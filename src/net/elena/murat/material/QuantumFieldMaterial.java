package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.math.*;
import net.elena.murat.light.*;

public class QuantumFieldMaterial implements Material {
  private final Color primaryColor;
  private final Color secondaryColor;
  private final double energy;
  private Matrix4 objectInverseTransform;
  private double time;
  private final double reflectivity=0.3;
  
  public QuantumFieldMaterial(Color primary, Color secondary,
    double energy, Matrix4 invTransform) {
    this.primaryColor = primary;
    this.secondaryColor = secondary;
    this.energy = Math.max(0.1, Math.min(5.0, energy));
    this.objectInverseTransform = invTransform;
  }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
    if (tm == null) tm = new Matrix4 ();
    this.objectInverseTransform = tm;
  }
  
  public void update(double deltaTime) {
    time += deltaTime * energy * 0.1;
  }
  
  @Override
  public Color getColorAt(Point3 worldPoint, Vector3 worldNormal, Light light, Point3 viewPos) {
    Point3 localPoint = objectInverseTransform.transformPoint(worldPoint);
    Vector3 normal = objectInverseTransform.transformNormal(worldNormal).normalize();
    
    // Quantum noise patterns
    double nx = localPoint.x * 2 + time;
    double ny = localPoint.y * 3;
    double nz = localPoint.z + time;
    double pattern1 = (Math.sin(nx * 12.9898 + ny * 78.233 + nz * 144.7212) * 43758.5453) -
    Math.floor(Math.sin(nx * 12.9898 + ny * 78.233 + nz * 144.7212) * 43758.5453);
    
    double pattern2 = (Math.cos(nx * 9.1234 + ny * 45.678 + nz * 98.765) * 54321.987) -
    Math.floor(Math.cos(nx * 9.1234 + ny * 45.678 + nz * 98.765) * 54321.987);
    
    double pattern = Math.sin(pattern1 * Math.PI * 2) * Math.cos(pattern2 * Math.PI * 3);
    
    // Energy pulse
    double pulse = 0.5 + 0.5 * Math.sin(time * 2 + localPoint.length() * 5);
    
    // Base color
    Color baseColor = new Color(
      (int)(primaryColor.getRed() * (1-pattern) + secondaryColor.getRed() * pattern * pulse),
      (int)(primaryColor.getGreen() * (1-pattern) + secondaryColor.getGreen() * pattern * pulse),
      (int)(primaryColor.getBlue() * (1-pattern) + secondaryColor.getBlue() * pattern * pulse)
    );
    
    // Lighting
    Vector3 lightDir = light.getPosition().subtract(worldPoint).normalize();
    double NdotL = Math.max(0, normal.dot(lightDir));
    Color directLight = new Color(
      (int)(baseColor.getRed() * light.getColor().getRed() * NdotL * light.getIntensityAt(worldPoint) / 255.0),
      (int)(baseColor.getGreen() * light.getColor().getGreen() * NdotL * light.getIntensityAt(worldPoint) / 255.0),
      (int)(baseColor.getBlue() * light.getColor().getBlue() * NdotL * light.getIntensityAt(worldPoint) / 255.0)
    );
    
    // Energy glow
    double glow = Math.pow(pulse * 0.7 + 0.3, 2) * (0.5 + 0.5 * pattern);
    Color energyGlow = new Color(
      (int)(secondaryColor.getRed() * light.getColor().getRed() * glow * energy * 0.3 / 255.0),
      (int)(secondaryColor.getGreen() * light.getColor().getGreen() * glow * energy * 0.3 / 255.0),
      (int)(secondaryColor.getBlue() * light.getColor().getBlue() * glow * energy * 0.3 / 255.0)
    );
    
    // Combine
    int r = Math.min(255, directLight.getRed() + energyGlow.getRed());
    int g = Math.min(255, directLight.getGreen() + energyGlow.getGreen());
    int b = Math.min(255, directLight.getBlue() + energyGlow.getBlue());
    return new Color(r, g, b);
  }
  
  @Override public double getReflectivity() { return reflectivity; }
  @Override public double getIndexOfRefraction() { return 1.1; }
  @Override public double getTransparency() { return 0.2; }
  
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
Sphere quantumSphere = new Sphere(0.8);
quantumSphere.setTransform(Matrix4.translate(new Vector3(0, 1.5, -4)));

// 2. Create material (purple-effect quantum field)
QuantumFieldMaterial quantumMat = new QuantumFieldMaterial(
new Color(70, 0, 120),    // Dark purple
new Color(0, 200, 255),   // Cyan
3.5,                      // Energy level
quantumSphere.getInverseTransform()
);

// 3. Assign material
quantumSphere.setMaterial(quantumMat);

// 4. Add to scene
scene.addShape(quantumSphere);

// 5. Update for animation
void renderLoop() {
double deltaTime = 0.016;
quantumMat.update(deltaTime);
// ... rendering operations
}

// 6. Special lighting
scene.addLight(new PulsatingPointLight(
new Point3(0, 3, 2),
new Color(150, 0, 255),   // Purple light
2.0,
0.5                       // Pulse effect speed
));
 */
