package net.elena.murat.material.pbr;

import java.awt.Color;

import net.elena.murat.math.*;
import net.elena.murat.light.*;
import net.elena.murat.util.*;

public class WaterPBRMaterial implements PBRCapableMaterial {
  private final Color waterColor;
  private final double roughness;
  private final double waveIntensity;
  private final double murkiness;
  private final double foamThreshold;
  private double time;
  
  // Predefined water types
  public static final Color OCEAN_BLUE = new Color(10, 90, 130, 150);
  public static final Color TROPICAL_TEAL = new Color(0, 180, 200, 120);
  public static final Color MURKY_RIVER = new Color(70, 100, 80, 200);
  
  public WaterPBRMaterial() {
    this(OCEAN_BLUE, 0.05, 0.3, 0.1, 0.7);
  }
  
  public WaterPBRMaterial(Color waterColor, double roughness,
    double waveIntensity, double murkiness,
    double foamThreshold) {
    this.waterColor = waterColor;
    this.roughness = MathUtil.clamp(roughness, 0.001, 0.5);
    this.waveIntensity = MathUtil.clamp(waveIntensity, 0.0, 1.0);
    this.murkiness = MathUtil.clamp(murkiness, 0.0, 1.0);
    this.foamThreshold = MathUtil.clamp(foamThreshold, 0.5, 1.0);
    this.time = 0.0;
  }
  
  public void update(double deltaTime) {
    this.time += deltaTime * 0.5; // Animation speed
  }
  
  @Override
  public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPos) {
    // 1. Wave deformation
    Vector3 waveNormal = calculateWaveNormal(point, normal);
    
    // 2. Light interactions
    Color lighting = calculateLighting(point, waveNormal, light, viewerPos);
    
    // 3. Foam formation
    Color foam = calculateFoam(point, waveNormal);
    
    // 4. Depth effect
    Color depthEffect = calculateDepthEffect(point);
    
    // 5. Result
    return ColorUtil.add(lighting, ColorUtil.add(foam, depthEffect));
  }
  
  private Vector3 calculateWaveNormal(Point3 p, Vector3 originalNormal) {
    // Gerstner waves
    double wave1 = Math.sin(p.x * 0.5 + p.z * 0.3 + time * 1.5) * waveIntensity;
    double wave2 = Math.cos(p.x * 0.3 - p.z * 0.4 + time * 2.0) * waveIntensity * 0.7;
    
    // Normal calculation
    double dx = 0.5 * Math.cos(p.x * 0.5 + p.z * 0.3 + time * 1.5) * waveIntensity;
    double dz = 0.3 * Math.sin(p.x * 0.3 - p.z * 0.4 + time * 2.0) * waveIntensity;
    
    return new Vector3(
      originalNormal.x - dx,
      originalNormal.y,
      originalNormal.z - dz
    ).normalize();
  }
  
  private Color calculateLighting(Point3 p, Vector3 normal, Light light, Point3 viewPos) {
    // Fresnel effect
    double NdotV = Math.max(0.001, normal.dot(new Vector3(p, viewPos).normalize()));
    double fresnel = MathUtil.fresnelSchlick(NdotV, 1.33); // Water IOR
    
    // Specular (GGX)
    Vector3 lightDir = light.getDirectionTo(p).normalize();
    Vector3 halfway = lightDir.add(new Vector3(p, viewPos)).normalize();
    double NdotH = normal.dot(halfway);
    double specular = MathUtil.ggxDistribution(NdotH, roughness) *
    MathUtil.fresnelSchlick(NdotH, 1.33);
    
    // Base color
    Color base = ColorUtil.scale(waterColor, 1.0 - murkiness * 0.7);
    
    // Reflection
    Color reflection = ColorUtil.scale(Color.WHITE, fresnel * 2.0);
    
    return ColorUtil.add(base, reflection);
  }
  
  private Color calculateFoam(Point3 p, Vector3 normal) {
    // Foam at wave peaks
    double foam = Math.sin(p.x * 2.0 + time * 3.0) *
    Math.cos(p.z * 1.5 + time * 2.5) *
    waveIntensity;
    
    if (foam > foamThreshold) {
      double intensity = (foam - foamThreshold) / (1.0 - foamThreshold);
      return ColorUtil.scale(Color.WHITE, intensity * 0.8);
    }
    return new Color(0, 0, 0, 0);
  }
  
  private Color calculateDepthEffect(Point3 p) {
    // Increasing murkiness with depth
    double depthFactor = MathUtil.clamp(-p.y * 0.5, 0.0, 1.0);
    return ColorUtil.scale(waterColor, depthFactor * murkiness);
  }
  
  // --- PBR Properties ---
  @Override public Color getAlbedo() {
    return ColorUtil.setAlpha(waterColor, 150);
  }
  @Override public double getRoughness() {
    return roughness + waveIntensity * 0.2; // Waves increase roughness
  }
  @Override public double getMetalness() { return 0.0; }
  @Override public MaterialType getMaterialType() { return MaterialType.TRANSPARENT; }
  @Override public double getReflectivity() { return 0.9; }
  @Override public double getIndexOfRefraction() { return 1.33; } // Water IOR
  @Override public double getTransparency() { return 0.8 - murkiness * 0.3; }

  @Override
  public void setObjectTransform(Matrix4 tm) {
  }
  
  // --- Builder Pattern ---
  public static class Builder {
    private Color waterColor = OCEAN_BLUE;
    private double roughness = 0.05;
    private double waveIntensity = 0.3;
    private double murkiness = 0.1;
    private double foamThreshold = 0.7;
    
    public Builder withTropicalColor() {
      this.waterColor = TROPICAL_TEAL;
      return this;
    }
    
    public Builder withStormyWaves() {
      this.waveIntensity = 0.8;
      this.roughness = 0.2;
      return this;
    }
    
    public WaterPBRMaterial build() {
      return new WaterPBRMaterial(
        waterColor, roughness,
        waveIntensity, murkiness,
        foamThreshold
      );
    }
  }
  
  // --- Usage Examples ---
  public static void demo() {
    // Clear ocean
    WaterPBRMaterial ocean = new WaterPBRMaterial();
    
    // Tropical lagoon
    WaterPBRMaterial lagoon = new Builder()
    .withTropicalColor()
    .build();
    
    // Stormy sea
    WaterPBRMaterial stormyOcean = new Builder()
    .withStormyWaves()
    .build();
  }
  
}
