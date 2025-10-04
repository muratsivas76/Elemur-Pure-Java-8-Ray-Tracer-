package net.elena.murat.material;

import java.awt.Color;
import java.util.Random;

import net.elena.murat.light.Light;
import net.elena.murat.math.*;
import net.elena.murat.util.ColorUtil;

public class DiamondMaterial implements Material {
  private final Color baseColor;
  private final double indexOfRefraction;
  private final double baseReflectivity;
  private final double baseTransparency;
  private final Random random;
  
  private double currentReflectivity;
  private double currentTransparency;
  private final double dispersionStrength;
  private final double fireEffect;
  
  public DiamondMaterial(Color baseColor, double ior,
    double reflectivity, double transparency,
    double dispersionStrength, double fireEffect) {
    this.baseColor = baseColor;
    this.indexOfRefraction = ior;
    this.baseReflectivity = reflectivity;
    this.baseTransparency = transparency;
    this.dispersionStrength = dispersionStrength;
    this.fireEffect = fireEffect;
    this.random = new Random();
  }
  
  public DiamondMaterial(Color baseColor, double ior) {
    this(baseColor, ior, 0.15, 0.98, 0.3, 0.7);
  }
  
  public DiamondMaterial(double ior) {
    this(new Color(255, 250, 245), ior);
  }
  
  public DiamondMaterial() {
    this(2.42); // Diamond IOR
  }
  
  @Override
  public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPos) {
    Vector3 viewDir = viewerPos.subtract(point).normalize();
    Vector3 lightDir = light.getDirectionTo(point).normalize();
    
    double fresnel = calculateEnhancedFresnel(viewDir, normal, 1.0, indexOfRefraction);
    
    this.currentReflectivity = Math.min(0.97, baseReflectivity + (fresnel * 0.85));
    this.currentTransparency = Math.max(0.02, baseTransparency * (1.0 - fresnel * 0.1));
    
    double NdotL = Math.max(0.4, normal.dot(lightDir));
    double intensity = light.getIntensityAt(point);
    
    Vector3 reflectDir = lightDir.reflect(normal);
    double specular = Math.pow(Math.max(0.0, viewDir.dot(reflectDir)), 256);
    
    Color diamondTint = ColorUtil.multiplyColor(baseColor, 0.9);
    Color diffuse = ColorUtil.multiplyColor(diamondTint, NdotL * 0.3 * intensity);
    
    Color specularHighlight = ColorUtil.multiplyColor(light.getColor(), specular * 2.0 * intensity);
    
    Color dispersionEffect = applyDispersionEffect(specularHighlight, fresnel);
    
    Color result = ColorUtil.addSafe(diffuse, specularHighlight);
    result = ColorUtil.addSafe(result, dispersionEffect);
    
    return ColorUtil.clampColor(result);
  }
  
  private double calculateEnhancedFresnel(Vector3 viewDir, Vector3 normal,
    double ior1, double ior2) {
    double cosTheta = Math.abs(viewDir.dot(normal));
    cosTheta = Math.max(0.0, Math.min(1.0, cosTheta));
    
    double r0 = Math.pow((ior1 - ior2) / (ior1 + ior2), 2);
    double fresnel = r0 + (1.0 - r0) * Math.pow(1.0 - cosTheta, 3.5);
    
    return Math.max(0.0, Math.min(1.0, fresnel));
  }
  
  private Color applyDispersionEffect(Color baseColor, double fresnel) {
    if (dispersionStrength <= 0) return new Color(0, 0, 0, 0);
    
    double strength = dispersionStrength * fresnel * fireEffect;
    
    int r = (int) (strength * 180 * (0.8 + random.nextDouble() * 0.4));
    int g = (int) (strength * 120 * (0.7 + random.nextDouble() * 0.6));
    int b = (int) (strength * 200 * (0.9 + random.nextDouble() * 0.2));
    
    return new Color(
      Math.min(255, r),
      Math.min(255, g),
      Math.min(255, b),
      (int) (strength * 200)
    );
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
    int r = Math.min(255, (int)(baseColor.getRed() * 0.8 + fireEffect * 20));
    int g = Math.min(255, (int)(baseColor.getGreen() * 0.8 + fireEffect * 10));
    int b = Math.min(255, (int)(baseColor.getBlue() * 0.8 + fireEffect * 30));
    
    return new Color(r, g, b);
  }
  
  public Color getDiamondColor() {
    return baseColor;
  }
  
  public double getDispersionStrength() {
    return dispersionStrength;
  }
  
  public double getFireEffect() {
    return fireEffect;
  }
  
}
