package net.elena.murat.material;

import java.awt.Color;
import java.util.Random;

import net.elena.murat.light.Light;
import net.elena.murat.math.*;
import net.elena.murat.util.ColorUtil;

public class RandomMaterial implements Material {
  private static final Random RAND = new Random();
  private final Color diffuseColor;
  private final Color specularColor;
  private final double ambientCoeff;
  private final double diffuseCoeff;
  private final double specularCoeff;
  private final double shininess;
  private final double reflectivity;
  private final double transparency;
  private Matrix4 objectInverseTransform;
  
  public RandomMaterial(Matrix4 invTransform) {
    this.diffuseColor = randomColor();
    this.specularColor = randomColor();
    this.ambientCoeff = randomInRange(0.1, 0.3);
    this.diffuseCoeff = randomInRange(0.5, 1.0);
    this.specularCoeff = randomInRange(0.1, 0.9);
    this.shininess = randomInRange(5, 150);
    this.reflectivity = randomInRange(0, 0.5);
    this.transparency = RAND.nextBoolean() ? randomInRange(0, 0.3) : 0;
    this.objectInverseTransform = invTransform;
  }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
    if (tm == null) tm = new Matrix4 ();
    this.objectInverseTransform = tm;
  }
  
  @Override
  public Color getColorAt(Point3 worldPoint, Vector3 normal, Light light, Point3 viewPos) {
    // Diffuse
    Vector3 lightDir = light.getPosition().subtract(worldPoint).normalize();
    double NdotL = Math.max(0, normal.dot(lightDir));
    Color diffuse = ColorUtil.multiplyColors(diffuseColor, light.getColor(), diffuseCoeff * NdotL);
    
    // Specular (Blinn-Phong)
    Vector3 viewDir = viewPos.subtract(worldPoint).normalize();
    Vector3 halfway = lightDir.add(viewDir).normalize();
    double specFactor = Math.pow(Math.max(0, normal.dot(halfway)), shininess);
    Color specular = ColorUtil.multiplyColors(specularColor, light.getColor(), specularCoeff * specFactor);
    
    // Ambient
    Color ambient = ColorUtil.multiplyColors(diffuseColor, light.getColor(), ambientCoeff);
    
    return ColorUtil.combineColors(ambient, diffuse, specular);
  }
  
  // Helper methods
  private Color randomColor() {
    return new Color(RAND.nextInt(256), RAND.nextInt(256), RAND.nextInt(256));
  }
  
  private double randomInRange(double min, double max) {
    return min + (max - min) * RAND.nextDouble();
  }
  
  @Override public double getReflectivity() { return reflectivity; }
  @Override public double getIndexOfRefraction() { return 1.0 + RAND.nextDouble(); }
  @Override public double getTransparency() { return transparency; }
  
}

/***
// Adding random material into the scene
Sphere sphere = new Sphere(1.0);
sphere.setMaterial(new RandomMaterial(sphere.getInverseTransform()));
scene.addShape(sphere);
 */
