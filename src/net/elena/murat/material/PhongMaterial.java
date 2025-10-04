package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.light.*;
import net.elena.murat.math.*;

/**
 * PhongMaterial implements the Phong reflection model, which includes
 * ambient, diffuse, and specular components. It also defines properties
 * for reflectivity, index of refraction, and transparency for advanced
 * ray tracing effects.
 * This material fully implements the extended Material interface.
 */
public class PhongMaterial implements Material {
  private final Color diffuseColor;
  private final Color specularColor;
  private final double shininess;
  private final double ambientCoefficient;
  private final double diffuseCoefficient;
  private final double specularCoefficient;
  private final double reflectivity;
  private final double ior; // Index of Refraction
  private final double transparency;
  
  /**
   * Full constructor for PhongMaterial.
   * @param diffuseColor The base color of the material (diffuse component).
   * @param specularColor The color of the specular highlight.
   * @param shininess The shininess exponent for specular highlights.
   *                    Higher values make highlights smaller and more intense.
   * @param ambientCoefficient The ambient light contribution coefficient (0.0 - 1.0).
   * @param diffuseCoefficient The diffuse light contribution coefficient (0.0 - 1.0).
   * @param specularCoefficient The specular light contribution coefficient (0.0 - 1.0).
   * @param reflectivity The reflectivity coefficient (0.0 - 1.0).
   * @param ior The Index of Refraction for transparent materials (igual or greater than 1.0).
   * @param transparency The transparency coefficient (0.0 - 1.0).
   */
  public PhongMaterial(Color diffuseColor, Color specularColor, double shininess,
    double ambientCoefficient, double diffuseCoefficient, double specularCoefficient,
    double reflectivity, double ior, double transparency) {
    this.diffuseColor = diffuseColor;
    this.specularColor = specularColor;
    this.shininess = shininess;
    this.ambientCoefficient = ambientCoefficient;
    this.diffuseCoefficient = diffuseCoefficient;
    this.specularCoefficient = specularCoefficient;
    this.reflectivity = clamp01(reflectivity);
    this.ior = Math.max(1.0, ior);
    this.transparency = clamp01(transparency);
  }
  
  /**
   * Simplified constructor with default parameters.
   * Uses white specular color, shininess of 32.0, and default coefficients.
   * Default reflectivity = 0.0, IOR = 1.0, transparency = 0.0.
   * @param diffuseColor The base color of the material.
   */
  public PhongMaterial(Color diffuseColor) {
    this(diffuseColor, Color.WHITE, 32.0,
      0.1, 0.7, 0.7,
    0.0, 1.0, 0.0);
  }
  
  // --- GETTERS (for internal use) ---
  public Color getDiffuseColor() { return diffuseColor; }
  public Color getSpecularColor() { return specularColor; }
  public double getShininess() { return shininess; }
  public double getAmbientCoefficient() { return ambientCoefficient; }
  public double getDiffuseCoefficient() { return diffuseCoefficient; }
  public double getSpecularCoefficient() { return specularCoefficient; }
  
  // --- MATERIAL INTERFACE IMPLEMENTATION ---
  
  @Override
  public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPos) {
    Color lightColor = light.getColor();
    double attenuatedIntensity = 0.0;
    
    // Ambient component
    int rAmbient = (int) (diffuseColor.getRed()   * ambientCoefficient * lightColor.getRed()   / 255.0);
    int gAmbient = (int) (diffuseColor.getGreen() * ambientCoefficient * lightColor.getGreen() / 255.0);
    int bAmbient = (int) (diffuseColor.getBlue()  * ambientCoefficient * lightColor.getBlue()  / 255.0);
    
    // If light is ambient, return only ambient contribution
    if (light instanceof ElenaMuratAmbientLight) {
      return new Color(
        Math.min(255, rAmbient),
        Math.min(255, gAmbient),
        Math.min(255, bAmbient)
      );
    }
    
    // Get light direction
    Vector3 lightDir = getLightDirection(light, point);
    if (lightDir == null) return Color.BLACK;
    
    // Get attenuated intensity based on light type
    if (light instanceof MuratPointLight) {
      attenuatedIntensity = ((MuratPointLight) light).getAttenuatedIntensity(point);
      } else if (light instanceof ElenaDirectionalLight) {
      attenuatedIntensity = ((ElenaDirectionalLight) light).getIntensity();
      } else if (light instanceof PulsatingPointLight) {
      attenuatedIntensity = ((PulsatingPointLight) light).getAttenuatedIntensity(point);
      } else if (light instanceof SpotLight) {
      attenuatedIntensity = ((SpotLight) light).getAttenuatedIntensity(point);
      } else if (light instanceof BioluminescentLight) {
      attenuatedIntensity = ((BioluminescentLight) light).getAttenuatedIntensity(point);
      } else if (light instanceof BlackHoleLight) {
      attenuatedIntensity = ((BlackHoleLight) light).getAttenuatedIntensity(point);
      } else if (light instanceof FractalLight) {
      attenuatedIntensity = ((FractalLight) light).getAttenuatedIntensity(point);
      } else {
      System.err.println("Warning: Unsupported light type in PhongMaterial: " + light.getClass().getName());
      return Color.BLACK;
    }
    
    // Diffuse component
    double NdotL = Math.max(0, normal.dot(lightDir));
    int rDiffuse = (int) (diffuseColor.getRed()   * diffuseCoefficient * lightColor.getRed()   / 255.0 * attenuatedIntensity * NdotL);
    int gDiffuse = (int) (diffuseColor.getGreen() * diffuseCoefficient * lightColor.getGreen() / 255.0 * attenuatedIntensity * NdotL);
    int bDiffuse = (int) (diffuseColor.getBlue()  * diffuseCoefficient * lightColor.getBlue()  / 255.0 * attenuatedIntensity * NdotL);
    
    // Specular component
    Vector3 viewDir = viewerPos.subtract(point).normalize();
    Vector3 reflectDir = lightDir.negate().reflect(normal);
    double RdotV = Math.max(0, reflectDir.dot(viewDir));
    double specFactor = Math.pow(RdotV, shininess);
    
    int rSpecular = (int) (specularColor.getRed()   * specularCoefficient * lightColor.getRed()   / 255.0 * attenuatedIntensity * specFactor);
    int gSpecular = (int) (specularColor.getGreen() * specularCoefficient * lightColor.getGreen() / 255.0 * attenuatedIntensity * specFactor);
    int bSpecular = (int) (specularColor.getBlue()  * specularCoefficient * lightColor.getBlue()  / 255.0 * attenuatedIntensity * specFactor);
    
    // Combine diffuse and specular (ambient added separately in RayTracer)
    int finalR = Math.min(255, rDiffuse + rSpecular);
    int finalG = Math.min(255, gDiffuse + gSpecular);
    int finalB = Math.min(255, bDiffuse + bSpecular);
    
    return new Color(finalR, finalG, finalB);
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
  
  // --- HELPER METHODS ---
  
  /**
   * Clamps a double value between 0.0 and 1.0.
   * @param val The value to clamp.
   * @return Clamped value in [0.0, 1.0].
   */
  private double clamp01(double val) {
    return Math.min(1.0, Math.max(0.0, val));
  }
  
  /**
   * Calculates the normalized direction from the light source to the given point.
   * @param light The light source.
   * @param point The point in world space.
   * @return Normalized light direction vector, or null if unsupported.
   */
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
      } else {
      System.err.println("Warning: Unknown light type in PhongMaterial: " + light.getClass().getName());
      return new Vector3(0, 1, 0); // Fallback direction
    }
  }
  
}
