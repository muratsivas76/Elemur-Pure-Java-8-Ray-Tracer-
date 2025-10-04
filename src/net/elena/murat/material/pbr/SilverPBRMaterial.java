package net.elena.murat.material.pbr;

import java.awt.Color;
import java.util.Random;

import net.elena.murat.math.*;
import net.elena.murat.light.*;
import net.elena.murat.util.ColorUtil;

public class SilverPBRMaterial implements PBRCapableMaterial {
  private final Color albedo;
  private final double roughness;
  private final double metalness;
  private final Random random = new Random();
  
  // Silver color variations
  public static final Color PURE_SILVER = new Color(192, 192, 192);
  public static final Color POLISHED_SILVER = new Color(220, 220, 230);
  public static final Color ANTIQUE_SILVER = new Color(170, 170, 180);
  
  public SilverPBRMaterial(double roughness) {
    this(POLISHED_SILVER, roughness, 1.0);
  }
  
  public SilverPBRMaterial(Color albedo, double roughness, double metalness) {
    this.albedo = ColorUtil.adjustSaturation(albedo, 0.9f);
    this.roughness = Math.max(0.01, Math.min(1.0, roughness));
    this.metalness = Math.max(0.7, Math.min(1.0, metalness)); // Silver should have high metalness
  }
  
  @Override
  public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPos) {
    // 1. Vector calculations
    Vector3 viewDir = new Vector3(point, viewerPos).normalize();
    Vector3 lightDir = light.getDirectionTo(point).normalize();
    
    // 2. Optimized Fresnel for silver
    double cosTheta = Math.max(0.001, normal.dot(viewDir));
    double fresnel = 0.85 + 0.15 * Math.pow(1.0 - cosTheta, 5.0); // Silver has high reflectivity
    
    // 3. Reflection vector (less perturbation for silver)
    Vector3 reflected = Vector3.reflect(viewDir.negate(), normal);
    if (roughness > 0.001) {
      reflected = reflected.add(
        Vector3.randomInUnitSphere(random).scale(roughness * 0.5) // Less scattering than gold
      ).normalize();
    }
    
    // 4. Special specular for silver
    Vector3 halfway = viewDir.add(lightDir).normalize();
    double specular = Math.max(0, normal.dot(halfway));
    double specularIntensity = Math.pow(specular,
      20 + 80 * (1.0 - roughness) // Sharper reflections for silver
    ) * (1.0 + metalness * 1.8); // Less boost than gold
    
    // 5. Metallic color component (preserving silver color)
    Color metallicColor = ColorUtil.multiply(
      ColorUtil.lerp(albedo, Color.WHITE, 0.2f), // Slightly whitened
      (float)(specularIntensity * fresnel * 2.5) // Less boost than gold
    );
    
    // 6. Diffuse (very little for silver)
    double diffuseFactor = Math.max(0.05, normal.dot(lightDir)) * (1.0 - metalness * 0.9);
    Color diffuseColor = ColorUtil.scale(
      albedo,
      diffuseFactor * 0.8 // Less diffuse than gold
    );
    
    // 7. Ambient (cool-toned)
    Color ambientColor = ColorUtil.scale(
      ColorUtil.lerp(albedo, new Color(200, 210, 220), 0.3f), // Cool tone
      0.2 * (1.0 + metalness)
    );
    
    // 8. Final result
    Color finalColor = ColorUtil.add(
      ambientColor,
      ColorUtil.add(diffuseColor, metallicColor)
    );
    
    // Light intensity (less than gold)
    float intensity = (float)light.getIntensityAt(point) * 0.9f;
    return ColorUtil.adjustContrast(
      ColorUtil.multiply(finalColor, intensity),
      1.1f // Less contrast
    );
  }
  
  // --- PBR Properties ---
  @Override public Color getAlbedo() { return albedo; }
  @Override public double getRoughness() { return roughness; }
  @Override public double getMetalness() { return metalness; }
  @Override public MaterialType getMaterialType() { return MaterialType.METAL; }
  @Override public double getReflectivity() { return 0.85; } // Slightly less than gold
  @Override public double getIndexOfRefraction() { return 0.18; } // Metallic IOR
  @Override public double getTransparency() { return 0.0; }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
  }
  
}

/***
Material polishedSilver = new SilverPBRMaterial(0.1); // Polished silver
Material brushedSilver = new SilverPBRMaterial(0.3);  // Brushed silver

// Ideal lighting for silver
scene.addLight(new MuratPointLight(
new Point3(2, 5, 2),
new Color(255, 250, 245), // Warm white
2.0
));

// For cool reflections
scene.addLight(new MuratPointLight(
new Point3(-1, 3, -1),
new Color(200, 220, 255), // Blue tint
0.8
));

// Lighting setup
scene.clearLights();
scene.addLight(new ElenaDirectionalLight(
new Vector3(-1, -0.5, -0.5).normalize(), // Side lighting
new Color(230, 235, 240),
1.5
));
 */

/***
javac -cp ..\bin\elenaRT.jar; SilverTest.java

java -cp ..\bin\elenaRT.jar; SilverTest 3
 */
