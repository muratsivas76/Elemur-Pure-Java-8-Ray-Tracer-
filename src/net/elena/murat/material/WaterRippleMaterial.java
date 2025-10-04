package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.math.*;
import net.elena.murat.light.*;
import net.elena.murat.util.ColorUtil;

public class WaterRippleMaterial implements Material {
  private final Color waterColor;
  private final double waveSpeed;
  private final double reflectivity;
  private Matrix4 objectInverseTransform;
  private double time;
  
  public WaterRippleMaterial(Color waterColor, double waveSpeed,
    Matrix4 invTransform) {
    this(waterColor, waveSpeed, 0.4, invTransform);
  }
  
  public WaterRippleMaterial(Color waterColor, double waveSpeed,
    double reflectivity, Matrix4 invTransform) {
    this.waterColor = waterColor;
    this.waveSpeed = waveSpeed;
    this.reflectivity = Math.max(0, Math.min(1, reflectivity));
    this.objectInverseTransform = invTransform;
  }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
    if (tm == null) tm = new Matrix4 ();
    this.objectInverseTransform = tm;
  }
  
  public void update(double deltaTime) {
    time += deltaTime * waveSpeed;
  }
  
  @Override
  public Color getColorAt(Point3 worldPoint, Vector3 worldNormal, Light light, Point3 viewPos) {
    // 1. Wave deformation
    Point3 localPoint = objectInverseTransform.transformPoint(worldPoint);
    double wave = calculateWaveEffect(localPoint);
    
    // 2. Normal perturbation
    Vector3 normal = perturbNormal(worldNormal, localPoint, wave);
    
    // 3. Light calculations
    Vector3 lightDir = light.getPosition().subtract(worldPoint).normalize();
    Vector3 viewDir = viewPos.subtract(worldPoint).normalize();
    Color lightColor = light.getColor();
    double intensity = light.getIntensityAt(worldPoint);
    
    // 4. Fresnel effect
    double fresnel = calculateFresnel(normal, viewDir);
    
    // 5. Specular reflection
    double specular = calculateSpecular(normal, lightDir, viewDir);
    
    // 6. Color blending
    Color base = ColorUtil.multiplyColor(waterColor, 0.7 + wave * 0.1);
    Color reflection = ColorUtil.multiplyColor(lightColor, fresnel * reflectivity * intensity);
    Color highlight = ColorUtil.multiplyColor(lightColor, specular * intensity);
    
    return ColorUtil.combineColors(base, reflection, highlight);
  }
  
  private double calculateWaveEffect(Point3 p) {
    return 0.3 * (
      Math.sin(p.x * 3 + time) +
      Math.cos(p.z * 4 + time * 1.3) +
      Math.sin(p.x * 5 + p.z * 7 + time * 0.7)
    ) / 3.0;
  }
  
  private Vector3 perturbNormal(Vector3 original, Point3 p, double wave) {
    double dx = 0.5 * Math.cos(p.x * 5 + time);
    double dz = 0.5 * Math.sin(p.z * 5 + time);
    return new Vector3(
      original.x + dx,
      original.y,
      original.z + dz
    ).normalize();
  }
  
  private double calculateFresnel(Vector3 normal, Vector3 viewDir) {
    return Math.pow(1.0 - Math.max(0, normal.dot(viewDir)), 5);
  }
  
  private double calculateSpecular(Vector3 normal, Vector3 lightDir, Vector3 viewDir) {
    Vector3 halfway = lightDir.add(viewDir).normalize();
    return Math.pow(Math.max(0, normal.dot(halfway)), 128);
  }
  
  @Override public double getReflectivity() { return reflectivity; }
  @Override public double getIndexOfRefraction() { return 1.33; } // Water's IOR
  @Override public double getTransparency() { return 0.3; }
}

/***
// Scene setup
Scene scene = new Scene();

// 1. Ceramic sphere
Sphere ceramicSphere = new Sphere(1.0);
ceramicSphere.setTransform(Matrix4.translate(new Vector3(-2, 0, -5)));
ceramicSphere.setMaterial(new DamaskCeramicMaterial(
new Color(240, 240, 240), // White
new Color(70, 70, 70),    // Gray
50.0,                     // Shine
ceramicSphere.getInverseTransform()
));
scene.addShape(ceramicSphere);

// 2. Water sphere
WaterRippleMaterial waterMat = new WaterRippleMaterial(
new Color(80, 180, 220), // Water blue
0.5,                     // Wave speed
Matrix4.translate(new Vector3(2, 0, -5)).inverse()
);
Sphere waterSphere = new Sphere(1.0);
waterSphere.setTransform(Matrix4.translate(new Vector3(2, 0, -5)));
waterSphere.setMaterial(waterMat);
scene.addShape(waterSphere);

// 3. Light source
scene.addLight(new MuratPointLight(
new Point3(0, 5, 0),
new Color(255, 220, 180), // Warm white
2.0
));

// In render loop
double deltaTime = 0.016; // ~60 FPS
waterMat.update(deltaTime);
 */
