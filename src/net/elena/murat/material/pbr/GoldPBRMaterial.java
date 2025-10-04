package net.elena.murat.material.pbr;

import java.awt.Color;
import java.util.Random;

import net.elena.murat.math.*;
import net.elena.murat.light.*;
import net.elena.murat.material.Material;
import net.elena.murat.util.ColorUtil;

public class GoldPBRMaterial implements PBRCapableMaterial {
  private final Color albedo;
  private final double roughness;
  private final double metalness;
  private final Random random = new Random();
  
  public GoldPBRMaterial(double roughness) {
    this(new Color(255, 215, 0), // Standard gold color (RGB)
      roughness,
    1.0);                   // Full metal
  }
  
  public GoldPBRMaterial(Color albedo, double roughness, double metalness) {
    this.albedo = albedo;
    this.roughness = Math.max(0.0, Math.min(1.0, roughness));
    this.metalness = Math.max(0.0, Math.min(1.0, metalness));
  }

  @Override
  public void setObjectTransform(Matrix4 tm) {
  }
  
  @Override
  public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPos) {
    // 1. Vector calculations (optimized)
    Vector3 viewDir = new Vector3(point, viewerPos).normalize();
    Vector3 lightDir = light.getDirectionTo(point).normalize();
    
    // 2. Enhanced Fresnel (for more vibrant edge reflections)
    double cosTheta = Math.max(0.001, normal.dot(viewDir)); // Prevent division by zero
    double fresnel = Math.pow(1.0 - cosTheta, 5.0);
    fresnel = (0.98 * metalness) + (0.02 * fresnel); // Stronger base reflectivity in metals
    
    // 3. Reflection vector (optimized perturbation)
    Vector3 reflected = Vector3.reflect(viewDir.negate(), normal);
    if (roughness > 0.001) { // Skip small roughness values
      reflected = reflected.add(
        Vector3.randomInUnitSphere(random).scale(roughness * roughness) // roughness^2 looks more natural
      ).normalize();
    }
    
    // 4. Enhanced Specular (brighter highlights)
    Vector3 halfway = viewDir.add(lightDir).normalize();
    double specular = Math.max(0, normal.dot(halfway));
    double specularIntensity = Math.pow(specular,
      16 + 64 * (1.0 - roughness) // Dynamic shininess
    ) * (1.0 + metalness * 2.0); // Specular boost in metals
    
    // 5. Color components (for more vibrant colors)
    Color metallicColor = ColorUtil.multiply(
      albedo,
      (float)(specularIntensity * fresnel * 3.0) // 3x boost
    );
    
    // 6. Optimized Diffuse (preserve color saturation)
    double diffuseFactor = Math.max(0.1, normal.dot(lightDir)) * (1.0 - metalness * 0.8);
    Color diffuseColor = ColorUtil.scale(
      ColorUtil.gammaCorrect(albedo, 0.8F), // Saturation booster
      diffuseFactor * 1.2
    );
    
    // 7. Enhanced Ambient (preserve color temperature)
    Color ambientColor = ColorUtil.scale(
      ColorUtil.lerp(albedo, Color.WHITE, 0.2f), // Slightly whitened
      0.15 * (1.0 + metalness) // Brighter ambient in metals
    );
    
    // 8. Result (with tone mapping)
    Color finalColor = ColorUtil.add(
      ambientColor,
      ColorUtil.add(diffuseColor, metallicColor)
    );
    
    // Light intensity + contrast boost
    float intensity = (float)light.getIntensityAt(point) * 1.1f;
    return ColorUtil.adjustContrast(
      ColorUtil.multiply(finalColor, intensity),
      1.2f
    );
  }
  
  // --- PBRCapableMaterial Implementation ---
  @Override public Color getAlbedo() { return albedo; }
  @Override public double getRoughness() { return roughness; }
  @Override public double getMetalness() { return metalness; }
  @Override public MaterialType getMaterialType() { return MaterialType.METAL; }
  @Override public double getReflectivity() { return 0.9 * metalness; }
  @Override public double getIndexOfRefraction() { return 1.0; }
  @Override public double getTransparency() { return 0.0; }
  
}
