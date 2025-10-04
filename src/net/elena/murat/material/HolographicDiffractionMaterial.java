package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.math.*;
import net.elena.murat.light.*;
import net.elena.murat.util.ColorUtil;

public class HolographicDiffractionMaterial implements Material {
  private Matrix4 objectInverseTransform;
  private final double reflectivity;
  
  public HolographicDiffractionMaterial(Matrix4 objectInverseTransform) {
    this(objectInverseTransform, 0.7);
  }
  
  public HolographicDiffractionMaterial(Matrix4 objectInverseTransform, double reflectivity) {
    this.objectInverseTransform = objectInverseTransform;
    this.reflectivity = clamp(reflectivity, 0.0, 1.0);
  }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
    if (tm == null) tm = new Matrix4 ();
    this.objectInverseTransform = tm;
  }
  
  @Override
  public Color getColorAt(Point3 worldPoint, Vector3 worldNormal, Light light, Point3 viewPos) {
    // Light information (guaranteed in 0-255 range)
    Color lightColor = light.getColor();
    double intensity = clamp(light.getIntensityAt(worldPoint), 0.0, 1.0);
    Vector3 lightDir = light.getDirectionAt(worldPoint).normalize();
    
    // Hologram pattern (guaranteed in 0.0-1.0 range)
    Point3 localPoint = objectInverseTransform.transformPoint(worldPoint);
    double r = 0.5 + 0.5 * Math.sin(10 * localPoint.x + clamp(lightColor.getRed()/255.0, 0.0, 1.0));
    double g = 0.5 + 0.5 * Math.sin(7 * localPoint.y + clamp(lightColor.getGreen()/255.0, 0.0, 1.0));
    double b = 0.5 + 0.5 * Math.cos(6 * localPoint.z + clamp(lightColor.getBlue()/255.0, 0.0, 1.0));
    
    // Light effect (guaranteed in 0.0-1.0 range)
    double NdotL = clamp(worldNormal.dot(lightDir), 0.1, 1.0);
    Color base = ColorUtil.createColor(r, g, b);
    
    return ColorUtil.blendColors(base, lightColor, NdotL * intensity * 0.5);
  }
  
  // General clamp method
  private static double clamp(double value, double min, double max) {
    return Math.max(min, Math.min(max, value));
  }
  
  private static int clamp(int value, int min, int max) {
    return Math.max(min, Math.min(max, value));
  }
  
  @Override public double getReflectivity() { return reflectivity; }
  @Override public double getIndexOfRefraction() { return clamp(1.3, 1.0, 2.5); }
  @Override public double getTransparency() { return clamp(0.8, 0.0, 1.0); }
  
}
