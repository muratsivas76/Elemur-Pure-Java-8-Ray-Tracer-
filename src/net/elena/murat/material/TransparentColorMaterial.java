package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.math.*;
import net.elena.murat.light.Light;

public class TransparentColorMaterial implements Material {
  private final double transparency;
  private final double reflectivity;
  private final double indexOfRefraction;
  
  private final Color surfaceColor;
  
  public TransparentColorMaterial(double transparency,
    double reflectivity, double ior) {
    this.transparency = Math.max(0, Math.min(1, transparency));
    this.reflectivity = Math.max(0, Math.min(1, reflectivity));
    this.indexOfRefraction = Math.max(1.0, ior);
    
    int alpha = (int)(this.transparency * 255);
    this.surfaceColor = new Color(0, 0, 0, alpha); // RGB=0, alpha=transparency
  }
  
  // Basic constructor
  public TransparentColorMaterial(double transparency) {
    this(transparency, 0.0, 1.5);
  }
  
  @Override
  public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPoint) {
    return surfaceColor;
  }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
  }
  
  @Override
  public double getTransparency() {
    return transparency;
  }
  
  @Override
  public double getReflectivity() {
    return reflectivity;
  }
  
  @Override
  public double getIndexOfRefraction() {
    return indexOfRefraction;
  }
  
  @Override
  public String toString() {
    return String.format("TransparentColorMaterial[trans=%.2f, refl=%.2f, ior=%.2f]",
    transparency, reflectivity, indexOfRefraction);
  }
  
}
