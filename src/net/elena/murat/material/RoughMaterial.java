package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.light.*;
import net.elena.murat.math.*;

/**
 * RoughMaterial: Optimized material for rough surfaces.
 * Simulates micro-surface roughness unlike DiffuseMaterial.
 * Simple implementation following Physically Based Rendering (PBR) principles.
 */
public class RoughMaterial implements Material {
  private final Color color;
  private final double roughness; // 0.0 (smooth) - 1.0 (very rough)
  private final double diffuseCoefficient;
  private final double reflectivity; // 0.0 - 1.0
  
  /**
   * Full constructor.
   * @param color Material color
   * @param roughness Surface roughness (0.0-1.0)
   * @param diffuseCoefficient Diffuse reflection coefficient (0.0-1.0)
   * @param reflectivity Base reflectivity amount (0.0-1.0)
   */
  public RoughMaterial(Color color, double roughness,
    double diffuseCoefficient, double reflectivity) {
    this.color = color;
    this.roughness = clamp01(roughness);
    this.diffuseCoefficient = clamp01(diffuseCoefficient);
    this.reflectivity = clamp01(reflectivity);
  }
  
  /**
   * Simple constructor: takes only color and roughness.
   * @param color Material color
   * @param roughness Surface roughness (0.0-1.0)
   */
  public RoughMaterial(Color color, double roughness) {
    this(color, roughness, 0.9, 0.05 * roughness); // Reflectivity decreases as roughness increases
  }
  
  @Override
  public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPos) {
    if (light instanceof ElenaMuratAmbientLight) {
      return applyRoughnessToColor(color);
    }
    
    Vector3 lightDir = getLightDirection(light, point);
    if (lightDir == null) return Color.BLACK;
    
    double intensity = getLightIntensity(light, point);
    if (intensity <= 0) return Color.BLACK;
    
    // Diffuse calculation based on roughness
    double NdotL = Math.max(0.0, normal.dot(lightDir));
    double roughFactor = 1.0 - (roughness * 0.7); // Roughness effect
    double contribution = diffuseCoefficient * NdotL * intensity * roughFactor;
    
    Color baseColor = applyRoughnessToColor(color);
    int r = (int) (baseColor.getRed() * contribution);
    int g = (int) (baseColor.getGreen() * contribution);
    int b = (int) (baseColor.getBlue() * contribution);
    
    return new Color(
      Math.min(255, Math.max(0, r)),
      Math.min(255, Math.max(0, g)),
      Math.min(255, Math.max(0, b))
    );
  }
  
  /**
   * Darkens color based on roughness (simulates micro-shadows)
   */
  private Color applyRoughnessToColor(Color original) {
    float[] hsb = Color.RGBtoHSB(
      original.getRed(),
      original.getGreen(),
      original.getBlue(),
      null
    );
    // Value (brightness) decreases as roughness increases
    hsb[2] = (float) (hsb[2] * (1.0 - (roughness * 0.3)));
    return new Color(Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]));
  }
  
  // --- Material Interface Implementations ---
  @Override
  public double getReflectivity() {
    return reflectivity * (1.0 - roughness); // Roughness reduces reflectivity
  }
  
  @Override
  public double getIndexOfRefraction() {
    return 1.0; // IOR = 1.0 for solid opaque materials (no light refraction)
  }
  
  @Override
  public double getTransparency() {
    return 0.0; // Fully opaque
  }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
  }
  
  // --- Helper Methods ---
  
  private double getLightIntensity(Light light, Point3 point) {
    if (light instanceof MuratPointLight) {
      return ((MuratPointLight) light).getAttenuatedIntensity(point);
      } else if (light instanceof ElenaDirectionalLight) {
      return ((ElenaDirectionalLight) light).getIntensity();
      } else if (light instanceof PulsatingPointLight) {
      return ((PulsatingPointLight) light).getAttenuatedIntensity(point);
      } else if (light instanceof SpotLight) {
      return ((SpotLight) light).getAttenuatedIntensity(point);
      } else if (light instanceof BioluminescentLight) {
      return ((BioluminescentLight) light).getAttenuatedIntensity(point);
      } else if (light instanceof BlackHoleLight) {
      return ((BlackHoleLight) light).getAttenuatedIntensity(point);
      } else if (light instanceof FractalLight) {
      return ((FractalLight) light).getAttenuatedIntensity(point);
    }
    return 1.0; // Default
  }
  
  private Vector3 getLightDirection(Light light, Point3 point) {
    if (light instanceof MuratPointLight) {
      return ((MuratPointLight) light).getPosition().subtract(point).normalize();
      } else if (light instanceof ElenaDirectionalLight) {
      return ((ElenaDirectionalLight) light).getDirection().negate().normalize();
      } else if (light instanceof PulsatingPointLight) {
      return ((PulsatingPointLight) light).getPosition().subtract(point).normalize();
      } else if (light instanceof SpotLight) {
      return ((SpotLight) light).getDirectionAt(point).normalize();
      } else if (light instanceof BioluminescentLight) {
      return ((BioluminescentLight) light).getDirectionAt(point).normalize();
      } else if (light instanceof BlackHoleLight) {
      return ((BlackHoleLight) light).getDirectionAt(point).normalize();
      } else if (light instanceof FractalLight) {
      return ((FractalLight) light).getDirectionAt(point).normalize();
    }
    return null;
  }
  
  private double clamp01(double val) {
    return Math.min(1.0, Math.max(0.0, val));
  }
  
  // --- Getters ---
  public Color getColor() {
    return color;
  }
  
  public double getRoughness() {
    return roughness;
  }
  
  public double getDiffuseCoefficient() {
    return diffuseCoefficient;
  }
  
}
