package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.math.*;
import net.elena.murat.light.*;
import net.elena.murat.util.ColorUtil;

public class StainedGlassMaterial implements Material {
  private final Color tint;
  private final double roughness;
  private Matrix4 objectInverseTransform;
  private final double reflectivity=0.05;
  
  public StainedGlassMaterial(Color tint, double roughness, Matrix4 invTransform) {
    // Start with clamping
    this.tint = new Color(
      clamp(tint.getRed(), 0, 255),
      clamp(tint.getGreen(), 0, 255),
      clamp(tint.getBlue(), 0, 255)
    );
    this.roughness = clamp(roughness, 0.01, 1.0);
    this.objectInverseTransform = invTransform;
  }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
    if (tm == null) tm = new Matrix4 ();
    this.objectInverseTransform = tm;
  }
  
  @Override
  public Color getColorAt(Point3 worldPoint, Vector3 worldNormal, Light light, Point3 viewPos) {
    // Refraction direction
    double eta = 1.0 / getIndexOfRefraction();
    Vector3 viewDir = viewPos.subtract(worldPoint).normalize();
    double cosTheta = clamp(-worldNormal.dot(viewDir), -1.0, 1.0);
    double k = 1.0 - eta * eta * (1.0 - cosTheta * cosTheta);
    
    Vector3 refractedDir = k < 0 ? viewDir.reflect(worldNormal) :
    viewDir.scale(eta).add(worldNormal.scale(eta * cosTheta - Math.sqrt(k)));
    
    // Background color (sky blue, clamped)
    Color bgColor = new Color(100, 150, 255);
    
    // Transmitted color (0-255 garantili)
    Color transmitted = ColorUtil.multiplyColors(
      bgColor,
      tint,
      0.7 // attenuation factor
    );
    
    // Specular highlights (0-255 garantili)
    Vector3 lightDir = light.getPosition().subtract(worldPoint).normalize();
    Vector3 halfway = lightDir.add(viewDir).normalize();
    double NdotH = clamp(worldNormal.dot(halfway), 0.0, 1.0);
    double specularIntensity = clamp(Math.pow(NdotH, 1.0/roughness) * 0.5, 0.0, 1.0);
    
    Color specularColor = ColorUtil.multiplyColors(
      light.getColor(),
      new Color(255, 255, 255), // white highlight
      specularIntensity * light.getIntensityAt(worldPoint)
    );
    
    // Combine with clamping (0-255 guaranteed)
    return ColorUtil.add(transmitted, specularColor);
  }
  
  // Helper methods
  private static int clamp(int value, int min, int max) {
    return Math.max(min, Math.min(max, value));
  }
  
  private static double clamp(double value, double min, double max) {
    return Math.max(min, Math.min(max, value));
  }
  
  @Override public double getReflectivity() { return reflectivity; }
  @Override public double getIndexOfRefraction() { return 1.52; }
  @Override public double getTransparency() { return 0.9; }
  
}

/***
// Camera setup (common for all materials)
rayTracer.setCameraPosition(new Point3(0, 0, 5));
rayTracer.setLookAt(new Point3(0, 0, 0));
rayTracer.setUpVector(new Vector3(0, 1, 0));
rayTracer.setFov(45.0);

// Ambient light (soft lighting for entire scene)
scene.addLight(new ElenaMuratAmbientLight(
new Color(200, 220, 255), // Bluish ambient
0.3                       // Intensity
));

// 1. Create plane instead of sphere (for glass panel effect)
Plane glassPane = new Plane();
glassPane.setTransform(Matrix4.rotateX(Math.PI/2).translate(0, 0, -7));

// 2. Create material (green glass)
StainedGlassMaterial glassMat = new StainedGlassMaterial(
new Color(100, 255, 150, 150), // Green color (RGBA)
0.1,                           // Surface roughness
glassPane.getInverseTransform()
);

// 3. Assign material
glassPane.setMaterial(glassMat);

// 4. Add to scene
scene.addShape(glassPane);

// 5. Backlight for illumination
scene.addLight(new ElenaDirectionalLight(
new Vector3(0, 0, 1),     // Light coming from behind
new Color(200, 220, 255),  // Bluish white
1.5
));
 */
