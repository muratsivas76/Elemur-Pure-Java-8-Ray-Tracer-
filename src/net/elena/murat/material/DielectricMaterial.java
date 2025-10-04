package net.elena.murat.material;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import net.elena.murat.math.*;
import net.elena.murat.light.Light;
import net.elena.murat.util.ColorUtil;

public class DielectricMaterial implements Material {
  
  // Material properties
  private Color diffuseColor;
  private double indexOfRefraction;
  private double transparency;
  private double reflectivity;
  
  // Filter colors for interior and exterior
  private Color filterColorInside;
  private Color filterColorOutside;
  
  private double currentReflectivity;
  private double currentTransparency;
  
  // Object transformation matrix
  private Matrix4 objectTransform;
  
  /**
   * Default constructor with glass-like properties
   */
  public DielectricMaterial() {
    this.diffuseColor = new Color(0.9f, 0.9f, 0.9f);
    this.indexOfRefraction = 1.5;
    this.transparency = 0.8;
    this.reflectivity = 0.1;
    this.filterColorInside = new Color(1.0f, 1.0f, 1.0f);
    this.filterColorOutside = new Color(1.0f, 1.0f, 1.0f);
    this.objectTransform = new Matrix4().identity();
    
    this.currentReflectivity = this.reflectivity;
    this.currentTransparency = this.transparency;
  }
  
  /**
   * Constructor with custom parameters
   */
  public DielectricMaterial(Color diffuseColor, double ior,
    double transparency, double reflectivity) {
    this.diffuseColor = diffuseColor;
    this.indexOfRefraction = ior;
    this.transparency = transparency;
    this.reflectivity = reflectivity;
    this.filterColorInside = new Color(1.0f, 1.0f, 1.0f);
    this.filterColorOutside = new Color(1.0f, 1.0f, 1.0f);
	
    this.objectTransform = new Matrix4().identity();
  }
  
  @Override
  public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPoint) {
    Vector3 lightDir = light.getDirectionTo(point).normalize();
    double diffuseFactor = Math.max(0, normal.dot(lightDir));
    
    // Calculate Fresnel reflection coefficient (for external use or debugging)
    Vector3 viewDir = viewerPoint.subtract(point).normalize();
    double fresnel = Vector3.calculateFresnel(viewDir, normal, 1.0, indexOfRefraction);
    
    this.currentReflectivity = Math.min(0.95, reflectivity + (fresnel * 0.8));
    this.currentTransparency = Math.max(0.05, transparency * (1.0 - fresnel * 0.2));
    
    // Basic diffuse color
    Color diffuse = ColorUtil.multiplyColor(diffuseColor, diffuseFactor * light.getIntensity());
    
	// Specular component
    Vector3 reflectDir = lightDir.reflect(normal);
    double specularFactor = Math.pow(Math.max(0, viewDir.dot(reflectDir)), 32);
    Color specular = ColorUtil.multiplyColor(light.getColor(), specularFactor * 0.5);
    
    // Combine diffuse and specular with light color
    Color c1 = ColorUtil.multiplyColors(light.getColor(), diffuse);
    Color result = ColorUtil.add(c1, specular);
    
    return ColorUtil.clampColor(result);
  }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
    if (tm == null) tm = new Matrix4();
    this.objectTransform = tm;
  }
  
  @Override
  public double getIndexOfRefraction() {
    return indexOfRefraction;
  }
  
  @Override
  public double getTransparency() {
    return currentTransparency;
  }
  
  @Override
  public double getReflectivity() {
    return currentReflectivity;
  }
 
  // Getters and setters for dielectric properties
  public Color getFilterColorInside() { return filterColorInside; }  
  public Color getFilterColorOutside() { return filterColorOutside; }
  
  public void setFilterColorInside(Color filterColorInside) {
    this.filterColorInside = filterColorInside;
  }
  
  public void setFilterColorOutside(Color filterColorOutside) {
    this.filterColorOutside = filterColorOutside;
  }
  
  public Color getDiffuseColor() {
    return diffuseColor;
  }
  
  public void setDiffuseColor(Color diffuseColor) {
    this.diffuseColor = diffuseColor;
  }
  
  public void setIndexOfRefraction(double indexOfRefraction) {
    this.indexOfRefraction = indexOfRefraction;
  }
  
  public void setTransparency(double transparency) {
    this.transparency = transparency;
  }
  
  public void setReflectivity(double reflectivity) {
    this.reflectivity = reflectivity;
  }
  
  @Override
  public String toString() {
    return String.format("DielectricMaterial[ior=%.2f, transparency=%.2f, reflectivity=%.2f]",
    indexOfRefraction, transparency, reflectivity);
  }
  
}
