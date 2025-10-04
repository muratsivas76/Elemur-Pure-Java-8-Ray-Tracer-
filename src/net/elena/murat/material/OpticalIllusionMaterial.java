package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.math.*;
import net.elena.murat.light.*;

/**
 * Optical Illusion Material
 */
public class OpticalIllusionMaterial implements Material {
  private final Color color1;
  private final Color color2;
  private final double frequency;
  private final double smoothness;
  private Matrix4 objectInverseTransform;
  
  private final double ambientCoefficient;
  private final double diffuseCoefficient;
  private final double specularCoefficient;
  private final double shininess;
  private final double reflectivity;
  private final double ior;
  private final double transparency;
  
  public OpticalIllusionMaterial(Color color1, Color color2,
    double frequency, double smoothness,
    double ambient, double diffuse, double specular,
    double shininess, double reflectivity,
    double ior, double transparency,
    Matrix4 objectInverseTransform) {
    this.color1 = color1;
    this.color2 = color2;
    this.frequency = Math.max(0.1, frequency);
    this.smoothness = Math.max(0, Math.min(1, smoothness));
    this.ambientCoefficient = Math.max(0, Math.min(1, ambient));
    this.diffuseCoefficient = Math.max(0, Math.min(1, diffuse));
    this.specularCoefficient = Math.max(0, Math.min(1, specular));
    this.shininess = Math.max(1, shininess);
    this.reflectivity = Math.max(0, Math.min(1, reflectivity));
    this.ior = ior;
    this.transparency = Math.max(0, Math.min(1, transparency));
    this.objectInverseTransform = objectInverseTransform;
  }
  
  public OpticalIllusionMaterial(Color color1, Color color2,
    double frequency, double smoothness,
    Matrix4 objectInverseTransform) {
    this(color1, color2, frequency, smoothness,
      0.15, 0.6, 0.25, 30.0, 0.05, 1.2, 0.02,
    objectInverseTransform);
  }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
    if (tm == null) tm = new Matrix4 ();
    this.objectInverseTransform = tm;
  }
  
  private Color getPatternColor(double u, double v) {
    double dist = Math.sqrt(u*u + v*v);
    double pattern = 0.5 * (1 + Math.sin(dist * frequency * 2 * Math.PI));
    
    if (smoothness > 0) {
      pattern = smoothstep(0.5-smoothness, 0.5+smoothness, pattern);
    }
    
    return blendColors(color1, color2, pattern);
  }
  
  private double smoothstep(double edge0, double edge1, double x) {
    x = Math.max(0, Math.min(1, (x - edge0) / (edge1 - edge0)));
    return x * x * (3 - 2 * x);
  }
  
  @Override
  public Color getColorAt(Point3 worldPoint, Vector3 worldNormal, Light light, Point3 viewerPos) {
    if (objectInverseTransform == null) {
      return Color.BLACK;
    }
    
    // UV mapping
    Point3 localPoint = objectInverseTransform.transformPoint(worldPoint);
    Vector3 localNormal = objectInverseTransform.inverseTransposeForNormal()
    .transformVector(worldNormal).normalize();
    
    double u, v;
    double absX = Math.abs(localNormal.x);
    double absY = Math.abs(localNormal.y);
    double absZ = Math.abs(localNormal.z);
    
    if (absX > absY && absX > absZ) {
      u = localPoint.y;
      v = localPoint.z;
      } else if (absY > absX && absY > absZ) {
      u = localPoint.x;
      v = localPoint.z;
      } else {
      u = localPoint.x;
      v = localPoint.y;
    }
    
    Color baseColor = getPatternColor(u, v);
    Color lightColor = light.getColor();
    double intensity = light.getIntensity();
    Vector3 lightDir = light.getDirectionAt(worldPoint).normalize();
    
    // 1. Ambient (light color included)
    int r = (int)(baseColor.getRed() * ambientCoefficient * (lightColor.getRed()/255.0));
    int g = (int)(baseColor.getGreen() * ambientCoefficient * (lightColor.getGreen()/255.0));
    int b = (int)(baseColor.getBlue() * ambientCoefficient * (lightColor.getBlue()/255.0));
    
    // 2. Diffuse (light color included)
    double NdotL = Math.max(0, worldNormal.dot(lightDir));
    if (NdotL > 0) {
      r += (int)(baseColor.getRed() * diffuseCoefficient * NdotL * (lightColor.getRed()/255.0) * intensity);
      g += (int)(baseColor.getGreen() * diffuseCoefficient * NdotL * (lightColor.getGreen()/255.0) * intensity);
      b += (int)(baseColor.getBlue() * diffuseCoefficient * NdotL * (lightColor.getBlue()/255.0) * intensity);
      
      // 3. Specular (light color included)
      Vector3 viewDir = viewerPos.subtract(worldPoint).normalize();
      Vector3 reflectDir = lightDir.negate().reflect(worldNormal);
      double RdotV = Math.max(0, reflectDir.dot(viewDir));
      double specular = Math.pow(RdotV, shininess) * specularCoefficient * intensity;
      
      r += (int)(255 * specular * (lightColor.getRed()/255.0));
      g += (int)(255 * specular * (lightColor.getGreen()/255.0));
      b += (int)(255 * specular * (lightColor.getBlue()/255.0));
    }
    
    return new Color(
      Math.min(255, r),
      Math.min(255, g),
      Math.min(255, b)
    );
  }
  
  @Override public double getReflectivity() { return reflectivity; }
  @Override public double getIndexOfRefraction() { return ior; }
  @Override public double getTransparency() { return transparency; }
  
  private Color blendColors(Color c1, Color c2, double ratio) {
    ratio = Math.max(0, Math.min(1, ratio));
    int r = (int)(c1.getRed() * (1-ratio) + c2.getRed() * ratio);
    int g = (int)(c1.getGreen() * (1-ratio) + c2.getGreen() * ratio);
    int b = (int)(c1.getBlue() * (1-ratio) + c2.getBlue() * ratio);
    return new Color(Math.min (255, r),
      Math.min (255, g),
    Math.min (255, b));
  }
}

/***
OpticalIllusionMaterial illusionMat = new OpticalIllusionMaterial(
Color.BLACK,
Color.WHITE,
5.0,  // Ring frequency
0.2,  // Transition smoothness
object.getInverseTransform()
);

OpticalIllusionMaterial hypnoMat = new OpticalIllusionMaterial(
new Color(0, 100, 255), // Blue
new Color(255, 50, 0),  // Orange
8.0,  // More frequent rings
0.1,  // Sharp transitions
object.getInverseTransform()
);
 */
