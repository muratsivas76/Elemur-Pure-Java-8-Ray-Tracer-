package net.elena.murat.material.pbr;

import java.awt.Color;

import net.elena.murat.math.*;
import net.elena.murat.light.*;
import net.elena.murat.util.ColorUtil;
import net.elena.murat.util.NoiseUtil;

public class MarblePBRMaterial implements PBRCapableMaterial {
  private final Color baseColor;
  private final Color veinColor;
  private final double veinScale;
  private final double veinContrast;
  private final double roughness;
  private final double reflectivity;
  private final double veinIntensity;
  
  public MarblePBRMaterial() {
    this(new Color(230, 226, 220), new Color(100, 100, 100),
    15.0, 0.7, 0.3, 0.1, 1.0);
  }
  
  public MarblePBRMaterial(Color baseColor, Color veinColor, double veinScale,
    double veinContrast, double roughness, double reflectivity,
    double veinIntensity) {
    this.baseColor = baseColor;
    this.veinColor = veinColor;
    this.veinScale = Math.max(1.0, veinScale);
    this.veinContrast = Math.max(0.1, Math.min(1.0, veinContrast));
    this.roughness = Math.max(0.01, Math.min(1.0, roughness));
    this.reflectivity = Math.max(0.0, Math.min(1.0, reflectivity));
    this.veinIntensity = Math.max(0.5, Math.min(2.0, veinIntensity));
  }
  
  @Override
  public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPos) {
    // 1. Advanced vein pattern (3D Perlin noise)
    Point3 scaledPoint = new Point3(
      point.x * veinScale,
      point.y * veinScale,
      point.z * veinScale * 0.5
    );
    
    double noise = NoiseUtil.turbulence(scaledPoint, 4);
    double veins = Math.pow(Math.sin(noise * Math.PI * 3) * 0.5 + 0.5, veinContrast * 10);
    veins *= veinIntensity;
    
    // 2. Color blending (with gamma correction)
    Color marbleColor = ColorUtil.lerp(
      ColorUtil.gammaCorrect(baseColor, 0.9f),
      ColorUtil.gammaCorrect(veinColor, 0.8f),
      (float)Math.min(0.9, veins) // Maximum vein intensity
    );
    
    // 3. Lighting calculations
    Vector3 lightDir = light.getDirectionTo(point).normalize();
    double NdotL = Math.max(0.4, normal.dot(lightDir)); // Minimum 0.4 brightness
    
    // 4. Diffuse
    Color diffuseColor = ColorUtil.multiply(marbleColor, (float)(NdotL * 1.3));
    
    // 5. Specular (GGX approximation)
    Vector3 viewDir = new Vector3(point, viewerPos).normalize();
    Vector3 halfway = viewDir.add(lightDir).normalize();
    double NdotH = Math.max(0.0, normal.dot(halfway));
    double alpha = roughness * roughness;
    double denominator = NdotH * NdotH * (alpha * alpha - 1.0) + 1.0;
    double specular = (alpha * alpha) / (Math.PI * denominator * denominator);
    
    // 6. Final result
    return ColorUtil.add(
      diffuseColor,
      ColorUtil.scale(Color.WHITE, specular * reflectivity * 1.5)
    );
  }
  
  // PBR Properties
  @Override public Color getAlbedo() { return baseColor; }
  @Override public double getRoughness() { return roughness; }
  @Override public double getMetalness() { return 0.0; }
  @Override public MaterialType getMaterialType() { return MaterialType.DIELECTRIC; }
  @Override public double getReflectivity() { return reflectivity; }
  @Override public double getIndexOfRefraction() { return 1.5; }
  @Override public double getTransparency() { return 0.0; }

  @Override
  public void setObjectTransform(Matrix4 tm) {
  }
  
  public MarblePBRMaterial withVeinIntensity(double intensity) {
    return new MarblePBRMaterial(baseColor, veinColor, veinScale,
    veinContrast, roughness, reflectivity, intensity);
  }
  
}

/***
Material material = new MarblePBRMaterial(
new Color(230, 226, 220), // Base color
new Color(100, 100, 100), // Vein color
15.0,  // Vein scale (larger = more frequent)
0.7,   // Vein contrast (0.1-1.0)
0.3,   // Roughness (0.01-1.0)
0.1,   // Reflectivity (0.0-1.0)
1.0    // Vein intensity (0.5-2.0)
);
 */
