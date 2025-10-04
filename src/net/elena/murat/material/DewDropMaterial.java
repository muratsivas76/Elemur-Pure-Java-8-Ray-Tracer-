package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.math.*;
import net.elena.murat.light.*;
import net.elena.murat.util.ColorUtil;

/**
 * Optimized dew drop material
 */
public class DewDropMaterial implements Material {
  private final Color baseColor;
  private final Color dropColor;
  private final double dropDensity; // [0.1, 0.9]
  private final double dropSize;    // [0.01, 0.1]
  private Matrix4 objectInverseTransform;
  
  private final double ambientCoefficient;
  private final double diffuseCoefficient;
  private final double specularCoefficient;
  private final double shininess;
  private final double reflectivity;
  private final double ior; // 1.33 (water)
  private final double transparency;
  
  public DewDropMaterial(Color baseColor, Color dropColor,
    double dropDensity, double dropSize,
    double ambient, double diffuse, double specular,
    double shininess, double reflectivity,
    double ior, double transparency,
    Matrix4 objectInverseTransform) {
    this.baseColor = baseColor;
    this.dropColor = dropColor;
    this.dropDensity = clamp(dropDensity, 0.1, 0.9);
    this.dropSize = clamp(dropSize, 0.01, 0.1);
    this.ambientCoefficient = clamp(ambient);
    this.diffuseCoefficient = clamp(diffuse);
    this.specularCoefficient = clamp(specular);
    this.shininess = Math.max(1, shininess);
    this.reflectivity = clamp(reflectivity);
    this.ior = ior;
    this.transparency = clamp(transparency);
    this.objectInverseTransform = objectInverseTransform;
  }
  
  // Simple constructor
  public DewDropMaterial(Color baseColor, Color dropColor,
    double dropDensity, double dropSize,
    Matrix4 objectInverseTransform) {
    this(baseColor, dropColor, dropDensity, dropSize,
      0.15, 0.6, 0.25, 50.0, 0.1, 1.33, 0.3,
    objectInverseTransform);
  }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
    if (tm == null) tm = new Matrix4 ();
    this.objectInverseTransform = tm;
  }
  
  @Override
  public Color getColorAt(Point3 worldPoint, Vector3 worldNormal, Light light, Point3 viewerPos) {
    if (objectInverseTransform == null) return Color.BLACK;
    
    // Transform to object space
    Point3 localPoint = objectInverseTransform.transformPoint(worldPoint);
    Color finalColor = calculateDewColor(baseColor, localPoint, worldNormal);
    
    // Light calculations
    Color lightColor = light.getColor();
    double intensity = light.getIntensityAt(worldPoint);
    Vector3 lightDir = light.getDirectionAt(worldPoint).normalize();
    
    // Phong lighting components
    int r = calculateAmbient(finalColor, lightColor);
    int g = r, b = r; // Same for all channels
    
    double NdotL = Math.max(0, worldNormal.dot(lightDir));
    if (NdotL > 0) {
      int[] diffuse = calculateDiffuse(finalColor, lightColor, intensity, NdotL);
      int[] specular = calculateSpecular(lightColor, intensity,
      lightDir, worldNormal, viewerPos);
      
      r = Math.min(255, r + diffuse[0] + specular[0]);
      g = Math.min(255, r + diffuse[1] + specular[1]);
      b = Math.min(255, r + diffuse[2] + specular[2]);
    }
    
    return new Color(r, g, b);
  }
  
  private Color calculateDewColor(Color base, Point3 localPoint, Vector3 normal) {
    if (!hasDewDrop(localPoint)) return base;
    
    double dist = calculateDropDistance(localPoint);
    double dropFactor = Math.max(0, 1 - (dist / dropSize));
    return ColorUtil.blendColors(base, dropColor, dropFactor * 0.8);
  }
  
  private boolean hasDewDrop(Point3 point) {
    Vector3 normal = point.toVector3().normalize();
    double noise = simpleNoise(normal.x * 100, normal.y * 100, normal.z * 100);
    return noise > (1.0 - dropDensity);
  }
  
  private double calculateDropDistance(Point3 point) {
    Vector3 normal = point.toVector3().normalize();
    return Math.sqrt(
      Math.pow(normal.x % (dropSize * 10), 2) +
      Math.pow(normal.y % (dropSize * 10), 2)
    );
  }
  
  private int calculateAmbient(Color color, Color lightColor) {
    return (int)(color.getRed() * ambientCoefficient * (lightColor.getRed()/255.0));
  }
  
  private int[] calculateDiffuse(Color color, Color lightColor,
    double intensity, double NdotL) {
    return new int[]{
      (int)(color.getRed() * diffuseCoefficient * NdotL * (lightColor.getRed()/255.0) * intensity),
      (int)(color.getGreen() * diffuseCoefficient * NdotL * (lightColor.getGreen()/255.0) * intensity),
      (int)(color.getBlue() * diffuseCoefficient * NdotL * (lightColor.getBlue()/255.0) * intensity)
    };
  }
  
  private int[] calculateSpecular(Color lightColor, double intensity,
    Vector3 lightDir, Vector3 normal, Point3 viewPos) {
    Vector3 viewDir = viewPos.subtract(viewPos).normalize();
    
    Vector3 reflectDir = lightDir.negate().reflect(normal);
    
    double RdotV = Math.max(0, reflectDir.dot(viewDir));
    double specular = Math.pow(RdotV, shininess * 1.5) * specularCoefficient * intensity;
    
    return new int[]{
      (int)(lightColor.getRed() * specular),
      (int)(lightColor.getGreen() * specular),
      (int)(lightColor.getBlue() * specular)
    };
  }
  
  // Helper methods
  private double clamp(double value, double min, double max) {
    return Math.max(min, Math.min(max, value));
  }
  
  private double clamp(double value) {
    return clamp(value, 0, 1);
  }
  
  private double simpleNoise(double x, double y, double z) {
    // Simple hash function
    int hash = (int)(x * 127 + y * 311 + z * 571);
    return (hash & 0x7FFFFFFF) / 2147483647.0;
  }
  
  @Override public double getReflectivity() { return reflectivity; }
  @Override public double getIndexOfRefraction() { return ior; }
  @Override public double getTransparency() { return transparency; }
}

/***
DewDropMaterial dewMat = new DewDropMaterial(
new Color(50, 120, 50), // Leaf color
new Color(200, 230, 255, 150), // Semi-transparent dew color
0.7, // Density (70%)
0.03, // Droplet size
sphere.getInverseTransform()
);
sphere.setMaterial(dewMat);

DewDropMaterial heavyDew = new DewDropMaterial(
Color.GRAY, // Rock color
new Color(220, 240, 255),
0.9, // Very dense
0.05, // Large droplets
object.getInverseTransform()
);

DewDropMaterial mistyDew = new DewDropMaterial(
new Color(70, 90, 110),
new Color(255, 255, 255, 180),
0.5,
0.02,
0.2, 0.5, 0.3, // Ambient, diffuse, specular
80.0, 0.15, 1.4, 0.4, // Shininess, reflectivity, IOR, transparency
rock.getInverseTransform()
);
 */
