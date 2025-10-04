package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.light.Light;
import net.elena.murat.math.*;

public class WaterfallMaterial implements Material {
  private final Color baseColor;
  private final double flowSpeed;
  private final long startTime;
  
  public WaterfallMaterial() {
    this(new Color(135, 206, 250), 0.1);
  }
  
  public WaterfallMaterial(Color baseColor, double flowSpeed) {
    this.baseColor = baseColor != null ? baseColor : new Color(135, 206, 250);
    this.flowSpeed = Math.max(0.01, Math.min(1.0, flowSpeed));
    this.startTime = System.currentTimeMillis();
  }
  
  @Override
  public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPos) {
    // 1. Time-based animation
    double time = (System.currentTimeMillis() - startTime) * 0.001 * flowSpeed;
    
    // 2. Base wave pattern
    double verticalWave = bound(Math.sin(point.y * 12 + time * 1.5) * 0.25, -0.25, 0.25);
    
    // 3. Cross turbulence
    double turbulence = Math.sin(point.x * 7 + time * 1.8) *
    Math.cos(point.z * 5 - time * 2.2) *
    0.15;
    
    // 4. Dynamic alpha
    double baseAlpha = 0.25 + 0.5 * (1 - bound(point.y, 0.1, 0.9));
    double alpha = bound(baseAlpha + verticalWave + turbulence, 0.15, 0.85);
    
    // 5. Foam density
    double foamInput = Math.sin(point.z * 8 - time * 4) * 0.6 +
    Math.sin(point.y * 20 + time * 2) * 0.4;
    double foamIntensity = bound(foamInput, 0, 1);
    
    // 6. Foam effect
    double foamThreshold = 0.45;
    if (alpha > foamThreshold) {
      double foamFactor = Math.pow((alpha - foamThreshold) / (1 - foamThreshold), 1.5);
      int foamValue = (int)(220 * foamFactor);
      
      return new Color(
        clamp(baseColor.getRed() * 0.6 + foamValue),
        clamp(baseColor.getGreen() * 0.7 + foamValue),
        clamp(baseColor.getBlue() * 0.8 + foamValue),
        (int)(alpha * 255)
      );
    }
    
    // 7. Non-foam area
    return new Color(
      clamp(baseColor.getRed() * (0.7 + turbulence * 0.3)),
      clamp(baseColor.getGreen() * (0.8 + turbulence * 0.2)),
      clamp(baseColor.getBlue() * (0.9 + turbulence * 0.1)),
      (int)(alpha * 255)
    );
  }
  
  @Override
  public double getReflectivity() {
    return 0.2; // Fixed reflectivity value
  }
  
  @Override
  public double getIndexOfRefraction() {
    return 1.33; // Water's refractive index
  }
  
  @Override
  public double getTransparency() {
    return 0.7; // Semi-transparent
  }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
  }
  
  // Helper methods
  private int clamp(double value) {
    return (int) Math.max(0, Math.min(255, value));
  }
  
  private double bound(double value, double min, double max) {
    return Math.max(min, Math.min(max, value));
  }
  
}
