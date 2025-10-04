package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.light.*;
import net.elena.murat.math.*;

/**
 * DiffuseMaterial: A simple material that only performs diffuse (Lambertian) reflection.
 * Implements only the diffuse part of the Phong model. Suitable for ideal matte surfaces.
 * Does not include reflection, refraction or transparency.
 */
public class DiffuseMaterial implements Material {
  private final Color color;
  private final double diffuseCoefficient;
  private final double reflectivity;
  private final double ior;
  private final double transparency;
  
  /**
   * Full constructor method.
   *
   * @param color Diffuse surface color.
   * @param diffuseCoefficient Diffuse reflection coefficient (between 0.0 - 1.0).
   * @param reflectivity Reflection amount (0.0 = matte, 1.0 = mirror-like).
   * @param ior Index of refraction (e.g.: air=1.0, glass=1.5, water=1.33).
   * @param transparency Transparency level (0.0 = opaque, 1.0 = fully transparent).
   */
  public DiffuseMaterial(Color color, double diffuseCoefficient,
    double reflectivity, double ior, double transparency) {
    this.color = color;
    this.diffuseCoefficient = clamp01(diffuseCoefficient);
    this.reflectivity = clamp01(reflectivity);
    this.ior = Math.max(1.0, ior);
    this.transparency = clamp01(transparency);
  }
  
  /**
   * Simple constructor: takes only color, other values are default.
   * Default values: diffuseCoefficient = 0.8, reflectivity = 0.0, ior = 1.0, transparency = 0.0
   *
   * @param color Material's diffuse color.
   */
  public DiffuseMaterial(Color color) {
    this(color, 0.8, 0.0, 1.0, 0.0);
  }
  
  @Override
  public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPos) {
    // Ambient light is added separately by the ray tracer (only once in total)
    // So here we only calculate diffuse.
    
    // Ambient only returns for ElenaMuratAmbientLight, but usually handled separately
    if (light instanceof ElenaMuratAmbientLight) {
      // If ray tracer already adds ambient, just return the color here
      return color;
    }
    
    // Light direction
    Vector3 lightDir = getLightDirection(light, point);
    if (lightDir == null) return Color.BLACK;
    
    // Light intensity (including attenuation)
    double intensity = getLightIntensity(light, point);
    if (intensity <= 0) return Color.BLACK;
    
    // Diffuse: N · L
    double NdotL = Math.max(0.0, normal.dot(lightDir));
    double contribution = diffuseCoefficient * NdotL * intensity;
    
    int r = (int) (color.getRed() * contribution);
    int g = (int) (color.getGreen() * contribution);
    int b = (int) (color.getBlue() * contribution);
    
    r = Math.min(255, r);
    g = Math.min(255, g);
    b = Math.min(255, b);
    
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
  
  private double clamp01(double val) {
    return Math.min(1.0, Math.max(0.0, val));
  }
  
  // Getter method
  public Color getColor() {
    return color;
  }
  
  public double getDiffuseCoefficient() {
    return diffuseCoefficient;
  }
  
}
