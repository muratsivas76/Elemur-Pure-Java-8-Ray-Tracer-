package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.math.*;
import net.elena.murat.light.Light;
import net.elena.murat.util.ColorUtil;

public class PolkaDotMaterial implements Material {
  private Color baseColor;
  private Color dotColor;
  private double dotSize;
  private double dotSpacing;
  private double transparency;
  
  public PolkaDotMaterial(Color baseColor, Color dotColor,
    double dotSize, double dotSpacing) {
    this.baseColor = baseColor;
    this.dotColor = dotColor;
    this.dotSize = dotSize;
    this.dotSpacing = dotSpacing;
    this.transparency = calculateTransparency(baseColor);
  }
  
  public PolkaDotMaterial(Color baseColor, Color dotColor) {
    this(baseColor, dotColor, 0.2, 0.5);
  }
  
  private double calculateTransparency(Color color) {
    int alpha = color.getAlpha();
    return 1.0 - ((double)alpha / 255.0);
  }
  
  @Override
  public double getTransparency() {
    return transparency;
  }
  
  @Override
  public double getReflectivity() {
    return 0.15;
  }
  
  @Override
  public double getIndexOfRefraction() {
    return 1.0;
  }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
    // Not needed for this material
  }
  
  @Override
  public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPoint) {
    // Calculate pattern based on point coordinates
    double pattern = calculatePolkaDotPattern(point);
    
    // Use dot pattern as blend factor
    return ColorUtil.blendColors(baseColor, dotColor, pattern);
  }
  
  private double calculatePolkaDotPattern(Point3 point) {
    // Map 3D point to 2D plane for pattern generation
    double u = (point.x + 1000) % dotSpacing / dotSpacing;
    double v = (point.y + 1000) % dotSpacing / dotSpacing;
    double w = (point.z + 1000) % dotSpacing / dotSpacing;
    
    // Use different planes for more interesting pattern
    double distance = Math.sqrt(u * u + v * v);
    
    // Create circular dots
    if (distance < dotSize) {
      return 1.0; // Full dot color
    }
    
    // Add some variation using z-coordinate
    double distance2 = Math.sqrt(v * v + w * w);
    if (distance2 < dotSize * 0.8) {
      return 0.7; // Lighter dot color
    }
    
    return 0.0; // Base color
  }
  
  public Color getBaseColor() {
    return baseColor;
  }
  
  public void setBaseColor(Color baseColor) {
    this.baseColor = baseColor;
    this.transparency = calculateTransparency(baseColor);
  }
  
  public Color getDotColor() {
    return dotColor;
  }
  
  public void setDotColor(Color dotColor) {
    this.dotColor = dotColor;
  }
  
  public double getDotSize() {
    return dotSize;
  }
  
  public void setDotSize(double dotSize) {
    this.dotSize = dotSize;
  }
  
  public double getDotSpacing() {
    return dotSpacing;
  }
  
  public void setDotSpacing(double dotSpacing) {
    this.dotSpacing = dotSpacing;
  }
}