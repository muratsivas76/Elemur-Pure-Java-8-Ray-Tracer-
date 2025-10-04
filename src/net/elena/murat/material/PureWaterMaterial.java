package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.light.Light;
import net.elena.murat.math.*;

public class PureWaterMaterial implements Material {
  private final Color baseColor;
  private final double flowSpeed;
  private final long startTime;
  
  public PureWaterMaterial() {
    this(new Color(135, 206, 250), 0.1); // Default: Light blue, medium speed
  }
  
  public PureWaterMaterial(Color baseColor, double flowSpeed) {
    this.baseColor = baseColor != null ? baseColor : new Color(135, 206, 250);
    this.flowSpeed = Math.max(0.01, Math.min(1.0, flowSpeed));
    this.startTime = System.currentTimeMillis();
  }
  
  @Override
  public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPos) {
    // 1. Time-based animation
    double time = (System.currentTimeMillis() - startTime) * 0.001 * flowSpeed;
    
    // 2. Vertical wave pattern
    double verticalWave = bound(Math.sin(point.y * 12 + time * 1.5) * 0.25, -0.25, 0.25);
    
    // 3. Cross-wave turbulence
    double turbulence = Math.sin(point.x * 7 + time * 1.8) *
    Math.cos(point.z * 5 - time * 2.2) *
    0.15;
    
    // 4. Dynamic alpha
    double baseAlpha = 0.3 + 0.5 * (1 - bound(point.y, 0.1, 0.9));
    double alpha = bound(baseAlpha + verticalWave + turbulence, 0.15, 0.85);
    
    // 5. Pure water color (no foam)
    return new Color(
      clamp(baseColor.getRed() * (0.7 + turbulence * 0.3)),
      clamp(baseColor.getGreen() * (0.8 + turbulence * 0.2)),
      clamp(baseColor.getBlue() * (0.9 + turbulence * 0.1)),
      (int)(alpha * 255)
    );
  }
  
  @Override
  public double getReflectivity() {
    return 0.2; // Slight reflection
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

/***
EMShape calmPond = new Plane()
.setMaterial(new PureWaterMaterial(
new Color(180, 220, 255), // Light blue
0.02 // Very slow flow
));

EMShape slowRiver = new Plane()
.setTransform(Matrix4.rotateX(Math.toRadians(10)))
.setMaterial(new PureWaterMaterial(
new Color(120, 190, 255), // Medium blue
0.08 // Slow flow
));
 */
