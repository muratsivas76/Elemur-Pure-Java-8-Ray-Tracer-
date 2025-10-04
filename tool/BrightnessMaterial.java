package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.math.*;
import net.elena.murat.light.Light;

public class BrightnessMaterial implements Material {
  
  private Color baseColor;
  private double brightness;
  private boolean useLightColor;
  
  public BrightnessMaterial(Color baseColor, double brightness) {
    this(baseColor, brightness, false);
  }
  
  public BrightnessMaterial(Color baseColor, double brightness, boolean useLightColor) {
    this.baseColor = baseColor;
    this.brightness = brightness;
    this.useLightColor = useLightColor;
  }
  
  @Override
  public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPoint) {
    Color sourceColor = baseColor;
    
    if (useLightColor) {
      sourceColor = light.getColor();
    }
    
    return applyBrightness(sourceColor, brightness);
  }
  
  private Color applyBrightness(Color color, double brightnessFactor) {
    float[] rgb = color.getRGBColorComponents(null);
    
    float r = (float) Math.max(0.0, Math.min(1.0, rgb[0] * brightnessFactor));
    float g = (float) Math.max(0.0, Math.min(1.0, rgb[1] * brightnessFactor));
    float b = (float) Math.max(0.0, Math.min(1.0, rgb[2] * brightnessFactor));
    
    return new Color(r, g, b);
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
  
  public double getBrightness() {
    return brightness;
  }
  
  public void setBrightness(double brightness) {
    this.brightness = Math.max(0.0, brightness);
  }
  
  public boolean isUseLightColor() {
    return useLightColor;
  }
  
  public void setUseLightColor(boolean useLightColor) {
    this.useLightColor = useLightColor;
  }
  
}
