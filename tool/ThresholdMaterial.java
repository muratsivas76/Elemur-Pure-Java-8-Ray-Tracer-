package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.math.*;
import net.elena.murat.light.Light;

public class ThresholdMaterial implements Material {
  
  private Color baseColor;
  private double threshold;
  private Color aboveColor;
  private Color belowColor;
  private boolean useLightColor;
  private boolean invertThreshold;
  
  public ThresholdMaterial(Color baseColor, double threshold) {
    this(baseColor, threshold, Color.WHITE, Color.BLACK, false, false);
  }
  
  public ThresholdMaterial(Color baseColor, double threshold, Color aboveColor, Color belowColor) {
    this(baseColor, threshold, aboveColor, belowColor, false, false);
  }
  
  public ThresholdMaterial(Color baseColor, double threshold, Color aboveColor,
    Color belowColor, boolean useLightColor, boolean invertThreshold) {
    this.baseColor = baseColor;
    this.threshold = Math.max(0.0, Math.min(1.0, threshold));
    this.aboveColor = aboveColor;
    this.belowColor = belowColor;
    this.useLightColor = useLightColor;
    this.invertThreshold = invertThreshold;
  }
  
  @Override
  public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPoint) {
    Color sourceColor = baseColor;
    
    if (useLightColor) {
      sourceColor = light.getColor();
    }
    
    return applyThreshold(sourceColor, threshold, aboveColor, belowColor, invertThreshold);
  }
  
  private Color applyThreshold(Color color, double thresholdValue, Color above, Color below, boolean invert) {
    float[] rgb = color.getRGBColorComponents(null);
    
    double intensity = 0.299 * rgb[0] + 0.587 * rgb[1] + 0.114 * rgb[2];
    
    if (invert) {
      intensity = 1.0 - intensity;
    }
    
    return intensity > thresholdValue ? above : below;
  }
  
  @Override
  public double getReflectivity() {
    return 0.0;
  }
  
  @Override
  public double getTransparency() {
    return 0.0;
  }
  
  @Override
  public double getIndexOfRefraction() {
    return 1.0;
  }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
  }
  
  // Getters Setters
  public Color getBaseColor() {
    return baseColor;
  }
  
  public void setBaseColor(Color baseColor) {
    this.baseColor = baseColor;
  }
  
  public double getThreshold() {
    return threshold;
  }
  
  public void setThreshold(double threshold) {
    this.threshold = Math.max(0.0, Math.min(1.0, threshold));
  }
  
  public Color getAboveColor() {
    return aboveColor;
  }
  
  public void setAboveColor(Color aboveColor) {
    this.aboveColor = aboveColor;
  }
  
  public Color getBelowColor() {
    return belowColor;
  }
  
  public void setBelowColor(Color belowColor) {
    this.belowColor = belowColor;
  }
  
  public boolean isUseLightColor() {
    return useLightColor;
  }
  
  public void setUseLightColor(boolean useLightColor) {
    this.useLightColor = useLightColor;
  }
  
  public boolean isInvertThreshold() {
    return invertThreshold;
  }
  
  public void setInvertThreshold(boolean invertThreshold) {
    this.invertThreshold = invertThreshold;
  }
  
  public static double calculateAutoThreshold(Color color) {
    float[] rgb = color.getRGBColorComponents(null);
    double intensity = 0.299 * rgb[0] + 0.587 * rgb[1] + 0.114 * rgb[2];
    return intensity;
  }
  
}
