package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.light.Light;
import net.elena.murat.math.*;

public class SmartGlassMaterial implements Material {
  private final Color glassColor;
  private final double clarity;
  
  public SmartGlassMaterial() {
    this(new Color(200, 230, 255), 0.95); // Default settings
  }
  
  public SmartGlassMaterial(Color color, double clarity) {
    this.glassColor = color != null ? color : new Color(200, 230, 255);
    this.clarity = Math.max(0.1, Math.min(1.0, clarity));
  }
  
  @Override
  public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPos) {
    // 1. Null Guard Clauses
    if (point == null) point = new Point3(0, 0, 0);
    if (normal == null) normal = new Vector3(0, 1, 0);
    if (viewerPos == null) viewerPos = new Point3(0, 0, -1);
    
    // 2. Default color (if light is null)
    if (light == null || light.getPosition() == null) {
      return new Color(
        glassColor.getRed(),
        glassColor.getGreen(),
        glassColor.getBlue(),
        200 // 78% opaque
      );
    }
    
    // 3. Normalization guarantee
    Vector3 safeNormal = normal.length() > 0 ? normal.normalize() : new Vector3(0, 1, 0);
    Vector3 viewDir = viewerPos.subtract(point).normalizeSafe();
    Vector3 lightDir = light.getPosition().subtract(point).normalizeSafe();
    
    // 4. Glass effects
    double fresnel = Math.pow(1.0 - Math.abs(safeNormal.dot(viewDir)), 5);
    double specular = Math.pow(Math.max(0, safeNormal.dot(lightDir)), 256 * clarity);
    
    // 5. Color calculation (clamped)
    return new Color(
      clamp(glassColor.getRed() * (0.7 + 0.3 * specular)),
      clamp(glassColor.getGreen() * (0.8 + 0.2 * specular)),
      clamp(glassColor.getBlue() * (0.9 + 0.1 * specular)),
      (int)(50 + 205 * clarity) // Alpha: 50-255
    );
  }
  
  private int clamp(double value) {
    return (int) Math.max(0, Math.min(255, value));
  }
  
  @Override public double getReflectivity() { return 0.1; }
  @Override public double getIndexOfRefraction() { return 1.5; }
  @Override public double getTransparency() { return 0.9; }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
  }
  
}
