package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.math.*;
import net.elena.murat.light.Light;

/**
 * Reflective metallic material class
 */
public class ReflectiveMaterial implements Material {
  private final Color baseColor;
  private final double reflectivity;
  private final double roughness;
  
  /**
   * Default reflective material (silver color, 70% reflectivity)
   */
  public ReflectiveMaterial() {
    this(new Color(200, 200, 200), 0.7, 0.1);
  }
  
  /**
   * Customizable reflective material
   * @param baseColor Base color
   * @param reflectivity Reflectivity ratio (0-1)
   * @param roughness Surface roughness (0-1, 0=perfect mirror)
   */
  public ReflectiveMaterial(Color baseColor, double reflectivity, double roughness) {
    this.baseColor = baseColor;
    this.reflectivity = Math.max(0, Math.min(1, reflectivity));
    this.roughness = Math.max(0, Math.min(1, roughness));
  }
  
  @Override
  public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPos) {
    // Corrected light direction calculation (using getDirectionTo)
    Vector3 lightDir = light.getDirectionTo(point).normalize();
    double diffuse = Math.max(0, normal.dot(lightDir));
    
    // Reflection brightness (Blinn-Phong)
    Vector3 viewDir = viewerPos.subtract(point).normalize();
    Vector3 halfDir = lightDir.add(viewDir).normalize();
    double specular = Math.pow(Math.max(0, normal.dot(halfDir)), 32 / (roughness + 0.01));
    
    // Color calculation
    int r = (int)(baseColor.getRed() * diffuse + 255 * specular);
    int g = (int)(baseColor.getGreen() * diffuse + 255 * specular);
    int b = (int)(baseColor.getBlue() * diffuse + 255 * specular);
    
    return new Color(
      Math.min(255, r),
      Math.min(255, g),
      Math.min(255, b)
    );
  }
  
  @Override
  public double getReflectivity() {
    return reflectivity;
  }
  
  @Override
  public double getIndexOfRefraction() {
    return 1.0; // IOR irrelevant for metals
  }
  
  @Override
  public double getTransparency() {
    return 0.0; // Opaque material
  }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
  }
  
  // Helper methods
  public static ReflectiveMaterial gold() {
    return new ReflectiveMaterial(new Color(255, 215, 0), 0.85, 0.15);
  }
  
  public static ReflectiveMaterial silver() {
    return new ReflectiveMaterial(new Color(192, 192, 192), 0.9, 0.1);
  }
  
  public static ReflectiveMaterial copper() {
    return new ReflectiveMaterial(new Color(184, 115, 51), 0.8, 0.2);
  }
  
}
