package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.math.*;
import net.elena.murat.light.*;

public class FractalBarkMaterial implements Material {
  private Matrix4 objectInverseTransform;
  private double roughness=0.1;
  private final double reflectivity=clamp(0.1 * roughness, 0.0, 1.0);
  
  public FractalBarkMaterial(Matrix4 objectInverseTransform) {
    this(objectInverseTransform, 0.7);
  }
  
  public FractalBarkMaterial(Matrix4 objectInverseTransform, double roughness) {
    this.objectInverseTransform = objectInverseTransform;
    this.roughness = clamp(roughness, 0.0, 1.0);
  }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
    if (tm == null) tm = new Matrix4 ();
    this.objectInverseTransform = tm;
  }
  
  // Fractal pattern between 0.0-1.0
  private double fractalNoise(Point3 p) {
    return clamp(Math.abs(Math.sin(30*p.x) * Math.cos(20*p.z) * Math.sin(5*p.y)), 0.0, 1.0);
  }
  
  @Override
  public Color getColorAt(Point3 worldPoint, Vector3 worldNormal, Light light, Point3 viewPos) {
    // Light information (guaranteed between 0-255 range)
    Color lightColor = light.getColor();
    double intensity = clamp(light.getIntensityAt(worldPoint), 0.0, 1.0);
    Vector3 lightDir = light.getDirectionAt(worldPoint).normalize();
    
    // Fractal pattern (guaranteed between 0.0-1.0 range)
    Point3 localPoint = objectInverseTransform.transformPoint(worldPoint);
    double n = fractalNoise(localPoint);
    
    // Base bark color (guaranteed between 0-255 range)
    int r = clamp((int)(100 + 100*n * (lightColor.getRed()/255.0)), 0, 255);
    int g = clamp((int)(70 + 50*n * (lightColor.getGreen()/255.0)), 0, 255);
    int b = clamp((int)(40 + 20*n * (lightColor.getBlue()/255.0)), 0, 255);
    Color base = new Color(r, g, b);
    
    // Diffuse lighting (guaranteed between 0-255 range)
    double NdotL = clamp(worldNormal.dot(lightDir), 0.1, 1.0);
    return new Color(
      clamp((int)(base.getRed() * NdotL * intensity), 0, 255),
      clamp((int)(base.getGreen() * NdotL * intensity), 0, 255),
      clamp((int)(base.getBlue() * NdotL * intensity), 0, 255)
    );
  }
  
  // Helper clamp methods
  private static double clamp(double value, double min, double max) {
    return Math.max(min, Math.min(max, value));
  }
  
  private static int clamp(int value, int min, int max) {
    return Math.max(min, Math.min(max, value));
  }
  
  @Override
  public double getReflectivity() {
    return reflectivity;
  }
  
  @Override
  public double getIndexOfRefraction() {
    return clamp(1.2, 1.0, 2.5);
  }
  
  @Override
  public double getTransparency() {
    return 0.0;
  }
  
}
