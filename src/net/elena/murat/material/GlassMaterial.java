package net.elena.murat.material;

import java.awt.Color;
import java.util.Random;

import net.elena.murat.light.Light;
import net.elena.murat.math.*;
import net.elena.murat.util.ColorUtil;

public class GlassMaterial implements Material {
  private final Color baseColor;
  private final double indexOfRefraction;
  private final double reflectivity;
  private final double transparency;
  private final Random random;
  
  private double currentReflectivity;
  private double currentTransparency;
  
  public GlassMaterial(Color baseColor, double ior,
    double reflectivity, double transparency) {
    this.baseColor = baseColor;
    this.indexOfRefraction = ior;
    this.reflectivity = reflectivity;
    this.transparency = transparency;
    this.random = new Random();
    
    this.currentReflectivity = this.reflectivity;
    this.currentTransparency = this.transparency;
  }
  
  public GlassMaterial(Color baseColor, double ior) {
    this(baseColor, ior, 0.08, 0.92);
  }
  
  public GlassMaterial(double ior) {
    this(new Color(200, 220, 240), ior);
  }
  
  public GlassMaterial() {
    this(1.5);
  }
  
  @Override
  public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPos) {
    Vector3 viewDir = viewerPos.subtract(point).normalize();
    Vector3 lightDir = light.getDirectionTo(point).normalize();
    
    double fresnel = Vector3.calculateFresnel(viewDir, normal, 1.0, indexOfRefraction);
    
    this.currentReflectivity = Math.min(0.95, reflectivity + (fresnel * 0.8));
    this.currentTransparency = Math.max(0.05, transparency * (1.0 - fresnel * 0.2));
    
    double NdotL = Math.max(0.3, normal.dot(lightDir));
    double intensity = light.getIntensityAt(point);
    
    Vector3 reflectDir = lightDir.reflect(normal);
    double specular = Math.pow(Math.max(0.0, viewDir.dot(reflectDir)), 128);
    
    Color glassTint = ColorUtil.multiplyColor(baseColor, 0.6);
    Color diffuse = ColorUtil.multiplyColor(glassTint, NdotL * 0.4 * intensity);
    Color specularHighlight = ColorUtil.multiplyColor(light.getColor(), specular * 1.2 * intensity);
    
    Color result = ColorUtil.addSafe(diffuse, specularHighlight);
    return ColorUtil.clampColor(result);
  }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
  }
  
  @Override
  public double getReflectivity() {
    return currentReflectivity;
  }
  
  @Override
  public double getTransparency() {
    return currentTransparency;
  }
  
  @Override
  public double getIndexOfRefraction() {
    return indexOfRefraction;
  }
  
  public Color getColorForRefraction() {
    return ColorUtil.multiplyColor(baseColor, 0.8);
  }
  
  public Color getGlassColor() {
    return baseColor;
  }
  
}
