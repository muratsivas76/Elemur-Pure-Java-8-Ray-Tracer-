package net.elena.murat.material.pbr;

import java.awt.Color;

import net.elena.murat.math.*;
import net.elena.murat.light.*;
import net.elena.murat.util.*;

public class ChromePBRMaterial implements PBRCapableMaterial {
  private final Color baseReflectance;
  private final double roughness;
  private final double anisotropy;
  private final double clearCoat;
  private final Color edgeTint;
  
  // Predefined chrome colors
  public static final Color MIRROR_CHROME = new Color(220, 230, 240);
  public static final Color BLACK_CHROME = new Color(70, 80, 90);
  public static final Color ROSE_CHROME = new Color(255, 200, 220);
  
  public ChromePBRMaterial() {
    this(MIRROR_CHROME, 0.02, 0.3, 1.0, new Color(150, 180, 255));
  }
  
  public ChromePBRMaterial(Color baseReflectance, double roughness,
    double anisotropy, double clearCoat, Color edgeTint) {
    this.baseReflectance = baseReflectance;
    this.roughness = MathUtil.clamp(roughness, 0.001, 0.5);
    this.anisotropy = MathUtil.clamp(anisotropy, 0.0, 1.0);
    this.clearCoat = MathUtil.clamp(clearCoat, 0.5, 1.5);
    this.edgeTint = edgeTint;
  }
  
  @Override
  public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPos) {
    // 1. Vector calculations
    Vector3 viewDir = new Vector3(point, viewerPos).normalize();
    Vector3 lightDir = light.getDirectionTo(point).normalize();
    Vector3 halfway = viewDir.add(lightDir).normalize();
    
    // 2. Fresnel effect (colored edges)
    double cosTheta = Math.max(0.001, normal.dot(viewDir));
    Color fresnelColor = calculateEdgeFresnel(cosTheta);
    
    // 3. Anisotropic reflection
    double specular = calculateAnisotropicSpecular(normal, halfway, lightDir, viewDir);
    
    // 4. ClearCoat layer
    double coatIntensity = calculateClearCoat(viewDir, normal);
    
    // 5. Color composition
    return composeFinalColor(fresnelColor, specular, coatIntensity);
  }
  
  private Color calculateEdgeFresnel(double cosTheta) {
    // Edge color with Schlick approximation
    double f0 = 0.8;
    double fresnel = f0 + (1 - f0) * Math.pow(1 - cosTheta, 5);
    
    return ColorUtil.lerp(
      baseReflectance,
      edgeTint,
    (float)(MathUtil.clamp((fresnel * 1.5), 0, 1)));
  }
  
  private double calculateAnisotropicSpecular(Vector3 normal, Vector3 halfway,
    Vector3 lightDir, Vector3 viewDir) {
    // GGX anisotropic distribution
    Vector3 tangent = new Vector3(1, 0, 0); // Surface texture direction
    Vector3 bitangent = normal.cross(tangent);
    
    double aspect = Math.sqrt(1 - anisotropy * 0.9);
    double ax = Math.max(0.001, roughness * roughness / aspect);
    double ay = Math.max(0.001, roughness * roughness * aspect);
    
    double NdotH = normal.dot(halfway);
    double TdotH = tangent.dot(halfway);
    double BdotH = bitangent.dot(halfway);
    
    // Anisotropic GGX
    double denominator = (TdotH * TdotH) / (ax * ax) +
    (BdotH * BdotH) / (ay * ay) +
    NdotH * NdotH;
    double distribution = 1.0 / (Math.PI * ax * ay * denominator * denominator);
    
    // Geometry function
    double NdotL = normal.dot(lightDir);
    double NdotV = normal.dot(viewDir);
    double G = MathUtil.smithG1(NdotL, roughness) * MathUtil.smithG1(NdotV, roughness);
    
    return distribution * G / (4 * NdotL * NdotV);
  }
  
  private double calculateClearCoat(Vector3 viewDir, Vector3 normal) {
    // Secondary reflection layer
    double coatRoughness = 0.1;
    double NdotV = Math.max(0.001, normal.dot(viewDir));
    return MathUtil.fresnelSchlick(NdotV, 1.5) * clearCoat;
  }
  
  private Color composeFinalColor(Color base, double specular, double coat) {
    // Base color (chrome reflects almost no diffuse)
    Color baseColor = ColorUtil.multiply(base, 0.1f);
    
    // Main reflection
    Color specColor = ColorUtil.scale(base, specular * 2.0);
    
    // ClearCoat layer (white reflection)
    Color coatColor = ColorUtil.scale(Color.WHITE, coat * 0.8);
    
    return ColorUtil.add(baseColor, ColorUtil.add(specColor, coatColor));
  }
  
  // --- PBR Properties ---
  @Override public Color getAlbedo() {
    return ColorUtil.scale(baseReflectance, 0.05f); // Chrome reflects very little albedo
  }
  @Override public double getRoughness() { return roughness; }
  @Override public double getMetalness() { return 1.0; } // Fully metallic
  @Override public MaterialType getMaterialType() { return MaterialType.ANISOTROPIC; }
  @Override public double getReflectivity() { return 0.95; }
  @Override public double getIndexOfRefraction() { return 2.5; } // High IOR for chrome
  @Override public double getTransparency() { return 0.0; }

  @Override
  public void setObjectTransform(Matrix4 tm) {
  }
  
  // --- Builder Pattern ---
  public static class Builder {
    private Color baseReflectance = MIRROR_CHROME;
    private double roughness = 0.02;
    private double anisotropy = 0.3;
    private double clearCoat = 1.0;
    private Color edgeTint = new Color(150, 180, 255);
    
    public Builder withRoughness(double roughness) {
      this.roughness = roughness;
      return this;
    }
    
    public Builder withAnisotropy(double anisotropy) {
      this.anisotropy = anisotropy;
      return this;
    }
    
    public Builder asBlackChrome() {
      this.baseReflectance = BLACK_CHROME;
      this.edgeTint = new Color(100, 120, 150);
      return this;
    }
    
    public ChromePBRMaterial build() {
      return new ChromePBRMaterial(baseReflectance, roughness,
      anisotropy, clearCoat, edgeTint);
    }
  }
  
  // --- Usage Examples ---
  public static void demo() {
    // Mirror-like chrome
    ChromePBRMaterial mirror = new ChromePBRMaterial();
    
    // Black chrome (custom settings)
    ChromePBRMaterial blackChrome = new Builder()
    .asBlackChrome()
    .withRoughness(0.05)
    .build();
    
    // Custom chrome (rose gold)
    ChromePBRMaterial roseGold = new ChromePBRMaterial(
      new Color(255, 200, 180), // Base color
      0.03,                     // Low roughness
      0.4,                      // Noticeable anisotropy
      1.2,                      // Thick clear coat
      new Color(255, 220, 180)  // Warm edge color
    );
  }
  
}
