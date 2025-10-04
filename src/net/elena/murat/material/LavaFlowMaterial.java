package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.math.*;
import net.elena.murat.light.*;
import net.elena.murat.util.ColorUtil;

public class LavaFlowMaterial implements Material {
  private final Color hotColor;
  private final Color coolColor;
  private final double flowSpeed;
  private Matrix4 objectInverseTransform;
  private double time;
  private final double reflectivity=0.1;
  
  public LavaFlowMaterial(Color hotColor, Color coolColor,
    double flowSpeed, Matrix4 invTransform) {
    // Initialize colors by clamping values
    this.hotColor = new Color(
      clamp(hotColor.getRed(), 0, 255),
      clamp(hotColor.getGreen(), 0, 255),
      clamp(hotColor.getBlue(), 0, 255)
    );
    this.coolColor = new Color(
      clamp(coolColor.getRed(), 0, 255),
      clamp(coolColor.getGreen(), 0, 255),
      clamp(coolColor.getBlue(), 0, 255)
    );
    this.flowSpeed = flowSpeed;
    this.objectInverseTransform = invTransform;
  }
  
  public void update(double deltaTime) {
    time += deltaTime * flowSpeed;
  }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
    if (tm == null) tm = new Matrix4 ();
    this.objectInverseTransform = tm;
  }
  
  @Override
  public Color getColorAt(Point3 worldPoint, Vector3 worldNormal, Light light, Point3 viewPos) {
    Point3 localPoint = objectInverseTransform.transformPoint(worldPoint);
    Vector3 normal = objectInverseTransform.transformNormal(worldNormal).normalize();
    
    // Noise functions (guaranteed in 0-1 range)
    double nx = localPoint.x * 2 + time;
    double ny = localPoint.y * 3;
    double nz = time * 0.5;
    double noise1 = fract(Math.sin(nx) * 43758.5453);
    double noise2 = fract(Math.cos(ny) * 12578.1459);
    double pattern = clamp(Math.sin(noise1 * Math.PI * 3) * Math.cos(noise2 * Math.PI * 2), -1, 1) * 0.5 + 0.5;
    
    // Temperature variation (guaranteed in 0-1 range)
    double temp = clamp(0.5 + 0.5 * Math.sin(localPoint.y * 5 - time * 0.3), 0, 1);
    
    // Base color calculation (guaranteed in 0-255 range)
    Color baseColor = ColorUtil.blendColors(
      coolColor,
      hotColor,
      temp * (0.5 + 0.5 * pattern)
    );
    
    // Lighting calculations (guaranteed in 0-255 range)
    Vector3 lightDir = light.getPosition().subtract(worldPoint).normalize();
    double NdotL = clamp(normal.dot(lightDir), 0, 1);
    double intensity = clamp(light.getIntensityAt(worldPoint), 0, 1);
    
    Color directLight = ColorUtil.multiplyColors(
      baseColor,
      light.getColor(),
      NdotL * intensity
    );
    
    // Emissive glow (guaranteed in 0-255 range)
    double glow = clamp(Math.pow(temp * 0.8 + 0.2, 3) * (0.7 + 0.3 * pattern), 0, 1);
    Color emissive = ColorUtil.multiplyColors(
      hotColor,
      light.getColor(),
      glow * 0.5
    );
    
    // Combine with clamping (guaranteed in 0-255 range)
    return ColorUtil.add(directLight, emissive);
  }
  
  // Helper methods
  private static int clamp(int value, int min, int max) {
    return Math.max(min, Math.min(max, value));
  }
  
  private static double clamp(double value, double min, double max) {
    return Math.max(min, Math.min(max, value));
  }
  
  private static double fract(double value) {
    return value - Math.floor(value);
  }
  
  @Override public double getReflectivity() { return reflectivity; }
  @Override public double getIndexOfRefraction() { return 1.4; }
  @Override public double getTransparency() { return 0.0; }
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

// 1. Create sphere
Sphere lavaSphere = new Sphere(1.2);
lavaSphere.setTransform(Matrix4.translate(new Vector3(2, -0.5, -6)));

// 2. Create material
LavaFlowMaterial lavaMat = new LavaFlowMaterial(
new Color(255, 80, 0),    // Lava hot color
new Color(100, 0, 0),     // Cooled lava color
0.8,                      // Flow speed
lavaSphere.getInverseTransform()
);

// 3. Assign material
lavaSphere.setMaterial(lavaMat);

// 4. Add to scene
scene.addShape(lavaSphere);

// 5. Update for animation in render loop
void renderLoop() {
double deltaTime = 0.016; // ~60 FPS
lavaMat.update(deltaTime);
// ... rendering operations
}

// 6. Proper lighting
scene.addLight(new MuratPointLight(
new Point3(0, 3, 0),
new Color(255, 100, 50),  // Orange light
3.0
));
 */
