package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.math.*;
import net.elena.murat.light.Light;
import net.elena.murat.util.ColorUtil;

public class CrystalMaterial implements Material {
  private Color baseColor;
  private Color crystalColor;
  private double rayDensity;
  private double raySharpness;
  private double transparency;
  
  public CrystalMaterial(Color baseColor, Color crystalColor,
    double rayDensity, double raySharpness) {
    this.baseColor = baseColor;
    this.crystalColor = crystalColor;
    this.rayDensity = rayDensity;
    this.raySharpness = raySharpness;
    this.transparency = calculateTransparency(baseColor);
  }
  
  public CrystalMaterial(Color baseColor, Color crystalColor) {
    this(baseColor, crystalColor, 8.0, 0.7);
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
    return 0.3;
  }
  
  @Override
  public double getIndexOfRefraction() {
    return 1.55;
  }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
    // Not needed for this material
  }
  
  @Override
  public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPoint) {
    double pattern = calculateCrystalPattern(point);
    return ColorUtil.blendColors(baseColor, crystalColor, pattern);
  }
  
  private double calculateCrystalPattern(Point3 point) {
    double distanceFromCenter = Math.sqrt(
      point.x * point.x + point.y * point.y + point.z * point.z
    );
    
    double angle = Math.atan2(point.y, point.x);
    int numRays = (int) (rayDensity * 6);
    double rayPattern = Math.sin(angle * numRays);
    double pattern = rayPattern * Math.exp(-distanceFromCenter * raySharpness);
    
    return (pattern + 1.0) / 2.0;
  }
  
  public Color getBaseColor() {
    return baseColor;
  }
  
  public void setBaseColor(Color baseColor) {
    this.baseColor = baseColor;
    this.transparency = calculateTransparency(baseColor);
  }
  
  public Color getCrystalColor() {
    return crystalColor;
  }
  
  public void setCrystalColor(Color crystalColor) {
    this.crystalColor = crystalColor;
  }
  
  public double getRayDensity() {
    return rayDensity;
  }
  
  public void setRayDensity(double rayDensity) {
    this.rayDensity = rayDensity;
  }
  
  public double getRaySharpness() {
    return raySharpness;
  }
  
  public void setRaySharpness(double raySharpness) {
    this.raySharpness = raySharpness;
  }
  
}
