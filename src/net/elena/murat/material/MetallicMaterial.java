package net.elena.murat.material;

import java.lang.reflect.Method;

import java.awt.Color;

import net.elena.murat.math.*;
import net.elena.murat.light.*;

/**
 * MetallicMaterial represents a metallic surface with strong, colored specular highlights
 * and typically low diffuse reflection. It is designed to work with ray tracing
 * that handles reflections recursively.
 * This material now fully implements the extended Material interface.
 */
public class MetallicMaterial implements Material {
  private Color metallicColor; // The base color of the metal
  private Color specularColor; // The color of the specular highlight (can be metallicColor for true metals)
  private double reflectivity; // How much light is reflected (0.0 to 1.0)
  private double shininess;    // Shininess exponent for specular highlights
  private double ambientCoefficient;
  private double diffuseCoefficient;
  private double specularCoefficient;
  
  // Default values for Material interface methods, as this material is opaque and not refractive
  private final double ior = 1.0; // Index of Refraction for air/vacuum
  private final double transparency = 0.0; // Not transparent
  
  /**
   * Constructs a MetallicMaterial with specified properties.
   * @param metallicColor The base color of the metal.
   * @param specularColor The color of the specular highlights. For true metals, this is often the same as metallicColor.
   * @param reflectivity The reflectivity coefficient (0.0 to 1.0).
   * @param shininess The shininess exponent for specular highlights.
   * @param ambientCoefficient The ambient light contribution coefficient (0.0 to 1.0).
   * @param diffuseCoefficient The diffuse light contribution coefficient (0.0 to 1.0).
   * @param specularCoefficient The specular light contribution coefficient (0.0 to 1.0).
   */
  public MetallicMaterial(Color metallicColor, Color specularColor, double reflectivity, double shininess,
    double ambientCoefficient, double diffuseCoefficient, double specularCoefficient) {
    this.metallicColor = metallicColor;
    this.specularColor = specularColor;
    this.reflectivity = reflectivity;
    this.shininess = shininess;
    this.ambientCoefficient = ambientCoefficient;
    this.diffuseCoefficient = diffuseCoefficient;
    this.specularCoefficient = specularCoefficient;
  }
  
  // Getter methods for material properties
  public Color getMetallicColor() { return metallicColor; }
  public Color getSpecularColor() { return specularColor; }
  public double getShininess() { return shininess; } // Not part of Material interface, but useful internally
  public double getAmbientCoefficient() { return ambientCoefficient; } // Not part of Material interface
  public double getDiffuseCoefficient() { return diffuseCoefficient; } // Not part of Material interface
  public double getSpecularCoefficient() { return specularCoefficient; } // Not part of Material interface
  
  /**
   * Calculates the direct lighting color at a given point on the metallic surface.
   * This method computes the ambient, diffuse (typically low for metals), and specular components.
   * The actual reflection will be handled by the ray tracer's recursive shading process,
   * using the `getReflectivity()` value.
   *
   * @param point The intersection point in world coordinates.
   * @param normal The surface normal at the intersection point in world coordinates.
   * @param light The single light source to be used in the calculation.
   * @param viewerPos The position of the viewer (camera) in world coordinates.
   * @return The calculated color contribution from this light source.
   */
  @Override
  public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPos) {
    Color lightColor = light.getColor();
    double attenuatedIntensity = 0.0; // Initialize for non-ambient lights
    
    // Ambient component
    int rAmbient = (int) (metallicColor.getRed() * ambientCoefficient * lightColor.getRed() / 255.0);
    int gAmbient = (int) (metallicColor.getGreen() * ambientCoefficient * lightColor.getGreen() / 255.0);
    int bAmbient = (int) (metallicColor.getBlue() * ambientCoefficient * lightColor.getBlue() / 255.0);
    
    if (light instanceof ElenaMuratAmbientLight) {
      return new Color(
        Math.min(255, rAmbient),
        Math.min(255, gAmbient),
        Math.min(255, bAmbient)
      );
    }
    
    Vector3 lightDirection = getLightDirection(light, point);
    if (lightDirection == null) { // Handle unsupported light types
      return Color.BLACK;
    }
    
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
      System.err.println("Warning: Unknown or unsupported light type for MetallicMaterial shading (intensity): " + light.getClass().getName());
      return Color.BLACK;
    }
    
    // Diffuse component (typically very low for metals)
    double NdotL = Math.max(0, normal.dot(lightDirection));
    int rDiffuse = (int) (metallicColor.getRed() * diffuseCoefficient * lightColor.getRed() / 255.0 * attenuatedIntensity * NdotL);
    int gDiffuse = (int) (metallicColor.getGreen() * diffuseCoefficient * lightColor.getGreen() / 255.0 * attenuatedIntensity * NdotL);
    int bDiffuse = (int) (metallicColor.getBlue() * diffuseCoefficient * lightColor.getBlue() / 255.0 * attenuatedIntensity * NdotL);
    
    // Specular component (strong and colored for metals)
    Vector3 viewDir = viewerPos.subtract(point).normalize();
    Vector3 reflectDir = lightDirection.negate().reflect(normal); // Use negate() before reflect() for correct reflection vector
    double RdotV = Math.max(0, reflectDir.dot(viewDir));
    double specFactor = Math.pow(RdotV, shininess);
    
    int rSpecular = (int) (specularColor.getRed() * specularCoefficient * lightColor.getRed() / 255.0 * attenuatedIntensity * specFactor);
    int gSpecular = (int) (specularColor.getGreen() * specularCoefficient * lightColor.getGreen() / 255.0 * attenuatedIntensity * specFactor);
    int bSpecular = (int) (specularColor.getBlue() * specularCoefficient * lightColor.getBlue() / 255.0 * attenuatedIntensity * specFactor);
    
    // Sum up all components for this light source
    // Note: Ambient component is added only once per pixel in RayTracer's shade method,
    // so here we only return the diffuse and specular contribution for non-ambient lights.
    int finalR = Math.min(255, rDiffuse + rSpecular);
    int finalG = Math.min(255, gDiffuse + gSpecular);
    int finalB = Math.min(255, bDiffuse + bSpecular);
    
    return new Color(finalR, finalG, finalB);
  }
  
  /**
   * Returns the reflectivity coefficient of the material.
   * @return The reflectivity value (0.0-1.0).
   */
  @Override
  public double getReflectivity() { return reflectivity; }
  
  /**
   * Returns the index of refraction (IOR) of the material. For opaque metallic materials, this is typically 1.0.
   * @return 1.0
   */
  @Override
  public double getIndexOfRefraction() { return ior; }
  
  /**
   * Returns the transparency coefficient of the material. For opaque metallic materials, this is typically 0.0.
   * @return 0.0
   */
  @Override
  public double getTransparency() { return transparency; }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
  }
  
  /**
   * Helper method to get the normalized light direction vector from a light source to a point.
   * @param light The light source.
   * @param point The point in world coordinates.
   * @return Normalized light direction vector, or null if light type is unsupported.
   */
  
  private Vector3 getLightDirection(Light light, Point3 point) {
    final Vector3 safeVector=new Vector3 (0, 1, 0).normalize ();
    
    if (light == null) {
      return safeVector;// Varsayılan yön
    }
    
    if (light instanceof MuratPointLight) {
      return ((MuratPointLight)light).getPosition().subtract(point).normalize();
      } else if (light instanceof ElenaDirectionalLight) {
      return ((ElenaDirectionalLight)light).getDirection().negate().normalize();
      } else if (light instanceof PulsatingPointLight) {
      return ((PulsatingPointLight)light).getPosition().subtract(point).normalize();
      } else if (light instanceof SpotLight) {
      return ((SpotLight)light).getDirectionAt(point).normalize();
      } else if (light instanceof BioluminescentLight) {
      return ((BioluminescentLight)light).getDirectionAt(point);
      } else if (light instanceof BlackHoleLight) {
      return ((BlackHoleLight)light).getDirectionAt(point);
      } else if (light instanceof FractalLight) {
      return ((FractalLight)light).getDirectionAt(point);
      } else {
      //return safeVector;
    }
    
    try {
      Method getDirMethod = light.getClass().getMethod("getDirectionAt", Point3.class);
      return (Vector3) getDirMethod.invoke(light, point);
    }
    catch (Exception e) {
      System.err.println("Unsupported light type: " + light.getClass().getName());
      return new Vector3(0, 1, 0).normalize(); // Güvenli varsayılan
    }
  }
  
}
