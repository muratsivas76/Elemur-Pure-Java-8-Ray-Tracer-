package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.math.*;
import net.elena.murat.light.*;

/**
 * LambertMaterial represents a matte, non-glossy surface that reflects light
 * equally in all directions, using a simplified Phong-like model (ambient and diffuse components).
 * It now fully implements the extended Material interface.
 */
public class LambertMaterial implements Material {
  private final Color diffuseColor; // This is the main (diffuse) color of the material.
  
  // Material coefficients
  private final double ambientCoefficient;
  private final double diffuseCoefficient;
  
  // Default values for new interface methods, as this is a basic diffuse material
  private final double reflectivity = 0.0;
  private final double ior = 1.0; // Index of Refraction for air/vacuum
  private final double transparency = 0.0;
  
  /**
   * Default constructor for LambertMaterial. Uses default ambient (0.1) and diffuse (1.0) coefficients.
   * @param color The base color (diffuse color) of the material.
   */
  public LambertMaterial(Color color) {
    this.diffuseColor = color;
    this.ambientCoefficient = 0.1; // General ambient lighting contribution
    this.diffuseCoefficient = 1.0; // Use full diffuse lighting contribution
  }
  
  /**
   * Constructor to customize material coefficients.
   * @param color The base color of the material.
   * @param ambientCoeff How much the material reacts to ambient light (0.0-1.0).
   * @param diffuseCoeff How much the material reacts to diffuse light (0.0-1.0).
   */
  public LambertMaterial(Color color, double ambientCoeff, double diffuseCoeff) {
    this.diffuseColor = color;
    this.ambientCoefficient = ambientCoeff;
    this.diffuseCoefficient = diffuseCoeff;
  }
  
  /**
   * Calculates the material color at a specific point, considering the contribution of a single light source.
   * This method applies ambient and diffuse lighting components.
   *
   * @param point The intersection point in world coordinates.
   * @param normal The surface normal at the intersection point in world coordinates.
   * @param light The single light source to be used in the calculation.
   * @param viewerPos The position of the viewer (camera) in world coordinates (not directly used for Lambertian).
   * @return The calculated color contribution.
   */
  @Override
  public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPos) {
    Color lightColor = light.getColor();
    double attenuatedIntensity = 0.0; // Initialize for non-ambient lights
    
    // Ambient component
    int rAmbient = (int) (diffuseColor.getRed() * ambientCoefficient * lightColor.getRed() / 255.0);
    int gAmbient = (int) (diffuseColor.getGreen() * ambientCoefficient * lightColor.getGreen() / 255.0);
    int bAmbient = (int) (diffuseColor.getBlue() * ambientCoefficient * lightColor.getBlue() / 255.0);
    
    if (light instanceof ElenaMuratAmbientLight) {
      return new Color(
        Math.min(255, rAmbient),
        Math.min(255, gAmbient),
        Math.min(255, bAmbient)
      );
    }
    
    Vector3 lightDirection;
    if (light instanceof MuratPointLight) {
      MuratPointLight pLight = (MuratPointLight) light;
      lightDirection = pLight.getPosition().subtract(point).normalize();
      attenuatedIntensity = pLight.getAttenuatedIntensity(point);
      } else if (light instanceof ElenaDirectionalLight) {
      ElenaDirectionalLight dLight = (ElenaDirectionalLight) light;
      lightDirection = dLight.getDirection().negate().normalize();
      attenuatedIntensity = dLight.getIntensity(); // Directional lights don't attenuate with distance
      } else if (light instanceof PulsatingPointLight) {
      PulsatingPointLight ppLight = (PulsatingPointLight) light;
      lightDirection = ppLight.getPosition().subtract(point).normalize();
      attenuatedIntensity = ppLight.getAttenuatedIntensity(point);
      } else if (light instanceof SpotLight) {
      SpotLight sLight = (SpotLight) light;
      lightDirection = sLight.getDirectionAt(point);
      attenuatedIntensity = sLight.getAttenuatedIntensity(point);
      } else if (light instanceof BioluminescentLight) {
      BioluminescentLight bLight = (BioluminescentLight) light;
      lightDirection = bLight.getDirectionAt(point);
      attenuatedIntensity = bLight.getAttenuatedIntensity(point);
      } else if (light instanceof BlackHoleLight) {
      BlackHoleLight bhLight = (BlackHoleLight) light;
      lightDirection = bhLight.getDirectionAt(point);
      attenuatedIntensity = bhLight.getAttenuatedIntensity(point);
      } else if (light instanceof FractalLight) {
      FractalLight fLight = (FractalLight) light;
      lightDirection = fLight.getDirectionAt(point);
      attenuatedIntensity = fLight.getAttenuatedIntensity(point);
      } else {
      System.err.println("Warning: Unknown or unsupported light type for Lambertian shading: " + light.getClass().getName());
      return Color.BLACK;
    }
    
    // Diffuse component
    double NdotL = Math.max(0, normal.dot(lightDirection));
    int rDiffuse = (int) (diffuseColor.getRed() * diffuseCoefficient * lightColor.getRed() / 255.0 * attenuatedIntensity * NdotL);
    int gDiffuse = (int) (diffuseColor.getGreen() * diffuseCoefficient * lightColor.getGreen() / 255.0 * attenuatedIntensity * NdotL);
    int bDiffuse = (int) (diffuseColor.getBlue() * diffuseCoefficient * lightColor.getBlue() / 255.0 * attenuatedIntensity * NdotL);
    
    // Sum up ambient and diffuse components for this light source
    // Note: Ambient component is added only once per pixel in RayTracer's shade method,
    // so here we only return the diffuse contribution for non-ambient lights.
    // The rAmbient, gAmbient, bAmbient are calculated but not added to finalR/G/B here
    // if it's not an AmbientLight.
    int finalR = Math.min(255, rDiffuse);
    int finalG = Math.min(255, gDiffuse);
    int finalB = Math.min(255, bDiffuse);
    
    return new Color(finalR, finalG, finalB);
  }
  
  /**
   * Returns the reflectivity coefficient. For a Lambertian material, this is typically 0.0.
   * @return 0.0
   */
  @Override
  public double getReflectivity() {
    return reflectivity;
  }
  
  /**
   * Returns the index of refraction. For an opaque Lambertian material, this is typically 1.0.
   * @return 1.0
   */
  @Override
  public double getIndexOfRefraction() {
    return ior;
  }
  
  /**
   * Returns the transparency coefficient. For an opaque Lambertian material, this is typically 0.0.
   * @return 0.0
   */
  @Override
  public double getTransparency() {
    return transparency;
  }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
  }
  
}
