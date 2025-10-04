package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.math.*;
import net.elena.murat.light.Light;

/**
 * EmissiveMaterial represents a surface that emits its own light, acting as a light source itself.
 * It does not reflect or refract light from other sources.
 * This material ignores all external lights since it's self-illuminating.
 */
public class EmissiveMaterial implements Material {
  private final Color emissiveColor;
  private final double emissiveStrength;
  
  // Material properties constants
  private static final double REFLECTIVITY = 0.0;
  private static final double IOR = 1.0;
  private static final double TRANSPARENCY = 0.0;
  
  public EmissiveMaterial(Color emissiveColor, double emissiveStrength) {
    this.emissiveColor = new Color(
      emissiveColor.getRed(),
      emissiveColor.getGreen(),
      emissiveColor.getBlue()
    );
    this.emissiveStrength = Math.max(0, emissiveStrength);
  }
  
  public Color getEmissiveColor() {
    return new Color(
      emissiveColor.getRed(),
      emissiveColor.getGreen(),
      emissiveColor.getBlue()
    );
  }
  
  public double getEmissiveStrength() {
    return emissiveStrength;
  }
  
  @Override
  public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPos) {
    // Emissive materials ignore all external lights and viewer position
    int r = clampColorValue(emissiveColor.getRed() * emissiveStrength);
    int g = clampColorValue(emissiveColor.getGreen() * emissiveStrength);
    int b = clampColorValue(emissiveColor.getBlue() * emissiveStrength);
    
    return new Color(r, g, b);
  }
  
  @Override
  public double getReflectivity() {
    return REFLECTIVITY;
  }
  
  @Override
  public double getIndexOfRefraction() {
    return IOR;
  }
  
  @Override
  public double getTransparency() {
    return TRANSPARENCY;
  }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
  }
  
  private int clampColorValue(double value) {
    return (int) Math.min(255, Math.max(0, value));
  }
  
}
