package net.elena.murat.material.pbr;

import java.awt.Color;
import java.util.Random;

import net.elena.murat.math.*;
import net.elena.murat.light.*;
import net.elena.murat.util.ColorUtil;

public class CopperPBRMaterial implements PBRCapableMaterial {
  private final Color albedo;
  private final double roughness;
  private final double oxidation;
  private final Random random = new Random();
  
  // Copper color constants
  public static final Color PURE_COPPER = new Color(184, 115, 51);
  public static final Color OXIDIZED_COPPER = new Color(100, 130, 90);
  
  // Constructors
  public CopperPBRMaterial() {
    this(0.0, 0.0); // Fully shiny, non-oxidized
  }
  
  public CopperPBRMaterial(double roughness, double oxidation) {
    this(PURE_COPPER, roughness, oxidation);
  }
  
  public CopperPBRMaterial(Color baseColor, double roughness, double oxidation) {
    this.albedo = baseColor;
    this.roughness = Math.max(0.0, Math.min(1.0, roughness));
    this.oxidation = Math.max(0.0, Math.min(1.0, oxidation));
  }
  
  @Override
  public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPos) {
    // 1. Oxidized color blend
    Color baseColor = ColorUtil.lerp(
      albedo,
      OXIDIZED_COPPER,
      (float)oxidation
    );
    
    // 2. Vector calculations
    Vector3 viewDir = new Vector3(point, viewerPos).normalize();
    Vector3 lightDir = light.getDirectionTo(point).normalize();
    
    // 3. Reflection vector (with roughness perturbation)
    Vector3 reflected = Vector3.reflect(viewDir.negate(), normal);
    if (roughness > 0) {
      Vector3 randomPerturbation = Vector3.randomInUnitSphere(random).scale(roughness);
      reflected = reflected.add(randomPerturbation).normalize();
    }
    
    // 4. Fresnel effect (special for metals)
    double cosTheta = Math.max(0.001, normal.dot(viewDir));
    double fresnel = 0.9 + 0.1 * Math.pow(1.0 - cosTheta, 5.0);
    
    // 5. Specular calculation (GGX)
    Vector3 halfway = viewDir.add(lightDir).normalize();
    double specular = calculateGGX(normal, halfway, roughness) * fresnel;
    
    // 6. Diffuse (reduced by oxidation)
    double diffuse = Math.max(0.05, normal.dot(lightDir)) * (1.0 - oxidation * 0.7);
    
    // 7. Color composition
    Color specularColor = ColorUtil.multiply(baseColor, (float)(specular * 2.0));
    Color diffuseColor = ColorUtil.multiply(baseColor, (float)diffuse);
    
    return ColorUtil.add(
      ColorUtil.scale(diffuseColor, 0.3),
      ColorUtil.scale(specularColor, 1.0 - oxidation * 0.5)
    );
  }
  
  private double calculateGGX(Vector3 normal, Vector3 halfway, double roughness) {
    double alpha = roughness * roughness;
    double NdotH = Math.max(0, normal.dot(halfway));
    double denom = NdotH * NdotH * (alpha - 1.0) + 1.0;
    return alpha / (Math.PI * denom * denom);
  }
  
  // PBR Properties
  @Override public Color getAlbedo() {
    return ColorUtil.lerp(albedo, OXIDIZED_COPPER, (float)oxidation);
  }
  @Override public double getRoughness() {
    return roughness + oxidation * 0.3; // Oxidation increases roughness
  }
  @Override public double getMetalness() {
    return 1.0 - oxidation * 0.5; // Oxidation reduces metallic property
  }
  @Override public MaterialType getMaterialType() {
    return oxidation > 0.7 ? MaterialType.DIELECTRIC : MaterialType.METAL;
  }
  @Override public double getReflectivity() {
    return 0.9 - oxidation * 0.6;
  }
  @Override public double getIndexOfRefraction() {
    return 1.0 + oxidation * 0.5;
  }
  @Override public double getTransparency() {
    return 0.0;
  }

  @Override
  public void setObjectTransform(Matrix4 tm) {
  }
  
}

/***
// 1. Shiny pure copper
Material pureCopper = new CopperPBRMaterial(0.05, 0.0);

// 2. Slightly oxidized (old copper pipe)
Material agedCopper = new CopperPBRMaterial(0.3, 0.4);

// 3. Green oxidized copper (statue)
Material oxidizedCopper = new CopperPBRMaterial(
new Color(80, 130, 80), // Greenish oxidation
0.6, // High roughness
0.9  // Full oxidation
);

// 4. Custom colored copper alloy
Material customCopper = new CopperPBRMaterial(
new Color(200, 120, 60), // Gold-copper blend
0.2,
0.1
);
 */
