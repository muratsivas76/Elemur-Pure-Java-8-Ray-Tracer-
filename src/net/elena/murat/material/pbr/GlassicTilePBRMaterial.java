package net.elena.murat.material.pbr;

import java.awt.Color;

import net.elena.murat.math.*;
import net.elena.murat.light.*;
import net.elena.murat.util.ColorUtil;

public class GlassicTilePBRMaterial implements PBRCapableMaterial {
  protected final Color tileColor;
  private final Color groutColor;
  private final double tileSize;
  private final double groutWidth;
  private final double tileRoughness;
  private final double groutRoughness;

  public static final Color DEFAULT_TILE_COLOR = new Color(245, 245, 255);
  public static final Color DEFAULT_GROUT_COLOR = new Color(70, 70, 80);
  public static final double DEFAULT_TILE_SIZE = 0.6;
  public static final double DEFAULT_GROUT_WIDTH = 0.03;
  public static final double DEFAULT_TILE_ROUGHNESS = 0.1;
  public static final double DEFAULT_GROUT_ROUGHNESS = 0.6;

  public GlassicTilePBRMaterial() {
    this(DEFAULT_TILE_COLOR, DEFAULT_GROUT_COLOR,
         DEFAULT_TILE_SIZE, DEFAULT_GROUT_WIDTH,
         DEFAULT_TILE_ROUGHNESS, DEFAULT_GROUT_ROUGHNESS);
  }

  public GlassicTilePBRMaterial(Color tileColor, Color groutColor,
                               double tileSize, double groutWidth,
                               double tileRoughness, double groutRoughness) {
    this.tileColor = ColorUtil.adjustSaturation(tileColor, 1.2f);
    this.groutColor = groutColor;
    this.tileSize = Math.max(0.1, tileSize);
    this.groutWidth = Math.max(0.01, Math.min(0.1, groutWidth));
    this.tileRoughness = clamp(tileRoughness, 0.01, 1.0);
    this.groutRoughness = clamp(groutRoughness, 0.01, 1.0);
  }

 //Metallic nice like glass material. 
  @Override
  public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPos) {
    // 1. Tile pattern
    double u = (point.x + 1000) % tileSize / tileSize;
    double v = (point.z + 1000) % tileSize / tileSize;
    boolean isGroutLocal = (u < groutWidth/tileSize || u > 1 - groutWidth/tileSize ||
                            v < groutWidth/tileSize || v > 1 - groutWidth/tileSize);

    Color baseColor = isGroutLocal ? groutColor : tileColor;
    double roughness = isGroutLocal ? groutRoughness : tileRoughness;

    // Vectors
    Vector3 lightDir = light.getDirectionTo(point).normalize();
    Vector3 viewDir = viewerPos.subtract(point).normalize();
    Vector3 halfway = lightDir.add(viewDir).normalize();

    double NdotL = Math.max(0.0, normal.dot(lightDir));
    double NdotV = Math.max(0.0, normal.dot(viewDir));
    if (NdotL == 0.0) {
      return ColorUtil.multiply(baseColor, 0.1f); // Ambient
    }

    // Fresnel (Schlick)
    double HdotV = Math.max(0.0, halfway.dot(viewDir));
    double F0 = 0.04; // Dielectric
    double fresnel = F0 + (1.0 - F0) * Math.pow(1.0 - HdotV, 5);

    // --- GGX Distribution ---
    double alpha = roughness * roughness;
    double NdotH = Math.max(0.001, normal.dot(halfway));
    double denom = NdotH * NdotH * (alpha * alpha - 1.0) + 1.0; // Fix: alpha^2
    double D = alpha * alpha / (Math.PI * denom * denom);

    // --- Smith G (GGX) Shadowing ---
    double k = roughness * roughness / 2.0;
    double G1 = NdotL / (NdotL * (1 - k) + k);
    double G2 = NdotV / (NdotV * (1 - k) + k);
    double G = G1 * G2;

    // Specular BRDF
    double specular = (D * G * fresnel) / Math.max(0.001, 4.0 * NdotL * NdotV);
    specular = Math.min(specular, 10.0); // Prevent overflow

    // Diffuse (Lambert)
    double diffuse = NdotL;

    float[] baseRGB = ColorUtil.getFloatComponents(baseColor);

    // Albedo scale: (1 - F0)
    Color diffuseColor = ColorUtil.createColor(
        baseRGB[0] * (1.0 - F0) * 255,
        baseRGB[1] * (1.0 - F0) * 255,
        baseRGB[2] * (1.0 - F0) * 255
    );

    // Diffuse
    Color diffuseLight = ColorUtil.multiply(diffuseColor, (float) diffuse);

    float F0f = (float) F0;
    Color specularLight = ColorUtil.createColor(
        F0f * 255, F0f * 255, F0f * 255
    );
    specularLight = ColorUtil.multiply(specularLight, (float) specular);

    // Ambient
    Color ambient = ColorUtil.multiply(diffuseColor, 0.1f);

    Color total = ColorUtil.add(diffuseLight, specularLight);
    total = ColorUtil.add(total, ambient);

    // Tonemapping: Reinhard (HDR → LDR)
    float[] rgb = ColorUtil.getFloatComponents(total);
    float exposure = 0.6f; // Bu değeri ayarla: 0.4–1.0
    rgb[0] = rgb[0] * exposure;
    rgb[1] = rgb[1] * exposure;
    rgb[2] = rgb[2] * exposure;

    // Reinhard tonemapping
    rgb[0] = rgb[0] / (rgb[0] + 1.0f);
    rgb[1] = rgb[1] / (rgb[1] + 1.0f);
    rgb[2] = rgb[2] / (rgb[2] + 1.0f);

    return ColorUtil.createColor(rgb[0] * 255, rgb[1] * 255, rgb[2] * 255);
  }

  private double calculateSpecular(Vector3 normal, Vector3 halfway,
                                  double roughness, double fresnel) {
    // Bu metot artık kullanılmıyor, yukarıda inline yazdık
    return 0;
  }

  @Override 
  public Color getAlbedo() {
    return ColorUtil.blendColors(tileColor, groutColor, 0.7f);
  }

  @Override 
  public double getRoughness() {
    return (tileRoughness + groutRoughness) / 2.0;
  }
  
  @Override public double getMetalness() { return 0.0; }
  @Override public MaterialType getMaterialType() { return MaterialType.DIELECTRIC; }
  @Override public double getReflectivity() { return 0.4; }
  @Override public double getIndexOfRefraction() { return 1.52; }
  @Override public double getTransparency() { return 0.0; }

  @Override
  public void setObjectTransform(Matrix4 tm) {
    // Optional
  }
  
  private double clamp(double value, double min, double max) {
    return Math.max(min, Math.min(max, value));
  }
  
}
