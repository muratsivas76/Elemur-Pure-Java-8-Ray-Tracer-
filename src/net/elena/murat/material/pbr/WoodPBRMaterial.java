package net.elena.murat.material.pbr;

import java.awt.Color;

import net.elena.murat.math.*;
import net.elena.murat.light.*;
import net.elena.murat.util.ColorUtil;

public class WoodPBRMaterial implements PBRCapableMaterial {
  private final Color woodColor1, woodColor2;
  private final double tileSize;
  private final double roughness;
  private final double specularScale;
  private boolean isAlternateTile = false;
  
  public WoodPBRMaterial() {
    this(new Color(160, 110, 60), new Color(130, 90, 50), 0.5, 0.3, 1.5);
  }
  
  public WoodPBRMaterial(Color color1, Color color2, double tileSize,
    double roughness, double specularScale) {
    this.woodColor1 = color1;
    this.woodColor2 = color2;
    this.tileSize = tileSize;
    this.roughness = roughness;
    this.specularScale = specularScale;
  }
  
  @Override
  public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPos) {
    // 1. Checkerboard pattern
    int tileX = (int)(point.x / tileSize) % 2;
    int tileZ = (int)(point.z / tileSize) % 2;
    isAlternateTile = (tileX + tileZ) % 2 == 0;
    
    // 2. Base color selection
    Color baseColor = isAlternateTile ? woodColor1 : woodColor2;
    
    // 3. Light calculations (Checkerboard's ambient/diffuse logic)
    Vector3 lightDir = light.getDirectionTo(point).normalize();
    double diffuse = Math.max(0.5, normal.dot(lightDir)); // Min 0.5 brightness guarantee
    
    // 4. Wood texture (grain effect)
    double grain = 1.0 + Math.sin(point.x * 30) * 0.2;
    Color woodColor = ColorUtil.multiply(baseColor, (float)grain);
    
    // 5. Specular (PBR)
    Vector3 viewDir = new Vector3(point, viewerPos).normalize();
    Vector3 halfway = viewDir.add(lightDir).normalize();
    double specular = Math.pow(Math.max(0, normal.dot(halfway)), 50) * specularScale;
    
    // 6. RESULT: Checkerboard's brightness guarantee + Wood texture
    return ColorUtil.add(
      ColorUtil.multiply(woodColor, (float)diffuse),
      ColorUtil.multiply(Color.WHITE, (float)(specular * 0.8))
    );
  }
  
  // PBR Properties
  @Override public Color getAlbedo() { return isAlternateTile ? woodColor1 : woodColor2; }
  @Override public double getRoughness() { return roughness; }
  @Override public double getMetalness() { return 0.0; }
  @Override public MaterialType getMaterialType() { return MaterialType.DIELECTRIC; }
  @Override public double getReflectivity() { return 0.3; }
  @Override public double getIndexOfRefraction() { return 1.53; }
  @Override public double getTransparency() { return 0.0; }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
  }
  
}

/***
// 1. STRONG DIRECT light
scene.addLight(new MuratPointLight(
new Point3(2, 10, 3), // FROM ABOVE (reduces shadows)
new Color(255, 250, 240), // Near white
5.0 // Full power
));

// 2. OMNIDIRECTIONAL light (Ambient++)
scene.addLight(new ElenaMuratAmbientLight(
new Color(255, 240, 230),
1.5 // 1.5x intensity (more than normal)
));

// 3. GROUND BOUNCE LIGHT
scene.addLight(new MuratPointLight(
new Point3(0, -0.5, 0), // From ground upwards
new Color(200, 190, 180),
2.0
));

EMShape ground = new Plane();
ground.setTransform(
new Matrix4()
.rotateX(Math.toRadians(-90)) // HORIZONTAL
.translate(0, -1, 0)         // Positioning
);
ground.setMaterial(new WoodPBRMaterial());

// CAMERA (Show full ground)
camera.setPosition(new Point3(3, 1.5, 4));

// Try these values in material constructor:
new WoodPBRMaterial(
new Color(180, 140, 90), // Light color
new Color(150, 100, 60), // Dark color
0.8,     // Large tiles
0.2,     // Very low roughness (shiny)
2.0      // High specular
);
 */
