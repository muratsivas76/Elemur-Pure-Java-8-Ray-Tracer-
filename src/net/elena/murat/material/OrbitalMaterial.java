package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.math.*;
import net.elena.murat.light.Light;
import net.elena.murat.util.ColorUtil;

public class OrbitalMaterial implements Material {
  private Color centerColor;
  private Color orbitColor;
  private double ringWidth;
  private int ringCount;
  private double transparency;
  
  public OrbitalMaterial(Color centerColor, Color orbitColor,
    double ringWidth, int ringCount) {
    this.centerColor = centerColor;
    this.orbitColor = orbitColor;
    this.ringWidth = ringWidth;
    this.ringCount = ringCount;
    this.transparency = calculateTransparency(centerColor);
  }
  
  public OrbitalMaterial(Color centerColor, Color orbitColor) {
    this(centerColor, orbitColor, 0.1, 5);
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
    return 0.2;
  }
  
  @Override
  public double getIndexOfRefraction() {
    return 1.4;
  }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
    // Not needed for this material
  }
  
  @Override
  public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPoint) {
    double pattern = calculateOrbitalPattern(point);
    return ColorUtil.blendColors(centerColor, orbitColor, pattern);
  }
  
  private double calculateOrbitalPattern(Point3 point) {
    double distanceFromCenter = Math.sqrt(
      point.x * point.x + point.y * point.y + point.z * point.z
    );
    
    // Calculate ring pattern based on distance
    double ringPattern = Math.sin(distanceFromCenter * ringCount * Math.PI);
    
    // Apply smooth step function for sharp rings
    double ringValue = smoothStep(0.5 - ringWidth, 0.5 + ringWidth, ringPattern * ringPattern);
    
    return ringValue;
  }
  
  private double smoothStep(double edge0, double edge1, double x) {
    x = clamp((x - edge0) / (edge1 - edge0), 0.0, 1.0);
    return x * x * (3.0 - 2.0 * x);
  }
  
  private double clamp(double value, double min, double max) {
    return Math.max(min, Math.min(max, value));
  }
  
  public Color getCenterColor() {
    return centerColor;
  }
  
  public void setCenterColor(Color centerColor) {
    this.centerColor = centerColor;
    this.transparency = calculateTransparency(centerColor);
  }
  
  public Color getOrbitColor() {
    return orbitColor;
  }
  
  public void setOrbitColor(Color orbitColor) {
    this.orbitColor = orbitColor;
  }
  
  public double getRingWidth() {
    return ringWidth;
  }
  
  public void setRingWidth(double ringWidth) {
    this.ringWidth = ringWidth;
  }
  
  public int getRingCount() {
    return ringCount;
  }
  
  public void setRingCount(int ringCount) {
    this.ringCount = ringCount;
  }
}