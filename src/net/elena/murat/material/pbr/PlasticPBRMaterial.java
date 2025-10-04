package net.elena.murat.material.pbr;

import java.awt.Color;

import net.elena.murat.light.*;
import net.elena.murat.math.*;
import net.elena.murat.material.Material;

/**
 * PlasticPBRMaterial represents a non-metallic, dielectric material like plastic, wood, or painted surfaces.
 * It implements the PBRCapableMaterial interface for Physically Based Rendering.
 *
 * This material uses a simplified PBR approach:
 * - Albedo defines the base color (diffuse reflection).
 * - Roughness controls the size and spread of specular highlights.
 * - It assumes a fixed Index of Refraction (IOR ~ 1.5) for common plastics.
 * - No diffuse component for metals, but plastic always has one.
 *
 * The shading model combines Lambertian diffuse with a Phong-like specular term,
 * modulated by roughness. This is a step towards microfacet theory without full complexity.
 */
public class PlasticPBRMaterial implements PBRCapableMaterial {
  private final Color albedo;
  private final double roughness;
  private final double ior;
  private final double transparency;
  private final double reflectivity; // Base reflectivity, can be adjusted
  
  /**
   * Constructs a PlasticPBRMaterial with full parameters.
   *
   * @param albedo The base color of the material (diffuse response).
   * @param roughness The surface roughness (0.0 = smooth, 1.0 = rough).
   * @param reflectivity The base reflectivity coefficient (0.0-1.0).
   * @param ior The Index of Refraction (igual or greater than 1.0, typically 1.4-1.6 for plastics).
   * @param transparency The transparency level (0.0 = opaque, 1.0 = clear).
   */
  public PlasticPBRMaterial(Color albedo, double roughness, double reflectivity, double ior, double transparency) {
    this.albedo = albedo;
    this.roughness = clamp01(roughness);
    this.reflectivity = clamp01(reflectivity);
    this.ior = Math.max(1.0, ior);
    this.transparency = clamp01(transparency);
  }
  
  /**
   * Simplified constructor for common plastic materials.
   * Default: roughness=0.3, reflectivity=0.04 (Fresnel base), IOR=1.5, opaque.
   *
   * @param albedo The base color of the plastic.
   */
  public PlasticPBRMaterial(Color albedo) {
    this(albedo, 0.3, 0.04, 1.5, 0.0);
  }
  
  public PlasticPBRMaterial () {
    this(new Color (0.1F, 0.2F, 0.8F), 0.3, 0.04, 1.5, 0.0);
  }
  
  // --- PBRCapableMaterial Interface Implementation ---
  
  @Override
  public Color getAlbedo() {
    return albedo;
  }
  
  @Override
  public double getRoughness() {
    return roughness;
  }
  
  @Override
  public double getMetalness() {
    return 0.0; // Plastic is non-metallic
  }
  
  @Override
  public MaterialType getMaterialType() {
    return MaterialType.PLASTIC;
  }
  
  // --- Material Interface Implementation ---
  
  @Override
  public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPos) {
    // Ambient component is minimal
    if (light instanceof ElenaMuratAmbientLight) {
      int r = (int) (albedo.getRed()   * 0.05);
      int g = (int) (albedo.getGreen() * 0.05);
      int b = (int) (albedo.getBlue()  * 0.05);
      return new Color(Math.min(255, r), Math.min(255, g), Math.min(255, b));
    }
    
    // Get light direction and intensity
    Vector3 lightDir = getLightDirection(light, point);
    if (lightDir == null) return Color.BLACK;
    
    double intensity = getLightIntensity(light, point);
    if (intensity <= 0) return Color.BLACK;
    
    // Normalize vectors
    Vector3 N = normal.normalize();
    Vector3 L = lightDir.normalize();
    Vector3 V = viewerPos.subtract(point).normalize();
    Vector3 H = L.add(V).normalize(); // Half-vector
    
    // Diffuse component (Lambert)
    double NdotL = Math.max(0.0, N.dot(L));
    double diffuseFactor = NdotL;
    
    // Specular component (Blinn-Phong style, roughness affects shininess)
    double NdotH = Math.max(0.0, N.dot(H));
    double shininess = 1.0 / Math.max(0.001, roughness * roughness); // Higher roughness = lower shininess
    double specFactor = Math.pow(NdotH, shininess * 128.0); // Scale for visual plausibility
    
    // Fresnel approximation (Schlick) for reflectivity based on angle
    double F0 = (1.0 - ior) / (1.0 + ior);
    F0 = F0 * F0; // Base reflectivity at normal incidence
    double cosTheta = Math.max(0.0, V.dot(N));
    double fresnel = F0 + (1.0 - F0) * Math.pow(1.0 - cosTheta, 5);
    
    // Combine components
    Color lightColor = light.getColor();
    int rDiffuse = (int) (albedo.getRed()   * diffuseFactor * intensity * lightColor.getRed()   / 255.0);
    int gDiffuse = (int) (albedo.getGreen() * diffuseFactor * intensity * lightColor.getGreen() / 255.0);
    int bDiffuse = (int) (albedo.getBlue()  * diffuseFactor * intensity * lightColor.getBlue()  / 255.0);
    
    int rSpecular = (int) (lightColor.getRed()   * specFactor * fresnel * intensity * 1.5);
    int gSpecular = (int) (lightColor.getGreen() * specFactor * fresnel * intensity * 1.5);
    int bSpecular = (int) (lightColor.getBlue()  * specFactor * fresnel * intensity * 1.5);
    
    int r = Math.min(255, rDiffuse + rSpecular);
    int g = Math.min(255, gDiffuse + gSpecular);
    int b = Math.min(255, bDiffuse + bSpecular);
    
    return new Color(r, g, b);
  }
  
  @Override
  public double getReflectivity() {
    return reflectivity;
  }
  
  @Override
  public double getIndexOfRefraction() {
    return ior;
  }
  
  @Override
  public double getTransparency() {
    return transparency;
  }

  @Override
  public void setObjectTransform(Matrix4 tm) {
  }
  
  // --- Helper Methods ---
  
  private double clamp01(double val) {
    return Math.min(1.0, Math.max(0.0, val));
  }
  
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
    return 1.0;
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
  
}
