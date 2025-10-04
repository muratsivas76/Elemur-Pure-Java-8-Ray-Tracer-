package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.math.*;
import net.elena.murat.light.*;
import net.elena.murat.util.ColorUtil;

public class PlatinumMaterial implements Material {
  private static final Color BASE_COLOR = new Color(160, 158, 155);
  private static final Color COLD_SPECULAR = new Color(180, 200, 255);
  private static final Color WARM_SPECULAR = new Color(255, 230, 190);
  private static final double IOR = 2.05;
  private static final double REFLECTIVITY = 0.6;
  private static final double SHININESS = 80.0;
  private static final double METALLIC_DIFFUSE = 0.25;
  
  private Matrix4 objectInverseTransform;
  private final double specularBalance;
  
  public PlatinumMaterial(Matrix4 objectInverseTransform) {
    this(objectInverseTransform, 0.35);
  }
  
  public PlatinumMaterial(Matrix4 objectInverseTransform, double specularBalance) {
    this.objectInverseTransform = new Matrix4(objectInverseTransform);
    this.specularBalance = Math.max(0, Math.min(1, specularBalance));
  }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
    if (tm == null) tm = new Matrix4 ();
    this.objectInverseTransform = tm;
  }
  
  @Override
  public Color getColorAt(Point3 worldPoint, Vector3 worldNormal, Light light, Point3 viewerPos) {
    // Transform normal to object space
    Vector3 normal = objectInverseTransform.inverseTransposeForNormal()
    .transformVector(worldNormal).normalize();
    
    // Get light properties
    LightProperties lightProps = LightProperties.getLightProperties(light, worldPoint);
    if (lightProps == null) return BASE_COLOR;
    
    Vector3 viewDir = viewerPos.subtract(worldPoint).normalize();
    double NdotL = Math.max(0.1, normal.dot(lightProps.direction));
    
    // Fresnel effect
    double fresnel = 0.04 + 0.96 * Math.pow(1 - Math.max(0, viewDir.dot(normal)), 5);
    
    // Specular calculations
    Color specularColor = ColorUtil.blendColors(COLD_SPECULAR, WARM_SPECULAR, specularBalance);
    Vector3 halfVec = lightProps.direction.add(viewDir).normalize();
    double NdotH = Math.max(0, normal.dot(halfVec));
    double specular = Math.pow(NdotH, SHININESS) * REFLECTIVITY * fresnel;
    
    // Apply light color and intensity
    Color lightColor = lightProps.color;
    double intensity = lightProps.intensity;
    
    // Base diffuse component
    int r = (int)(BASE_COLOR.getRed() * METALLIC_DIFFUSE * NdotL *
    (lightColor.getRed()/255.0) * intensity);
    int g = (int)(BASE_COLOR.getGreen() * METALLIC_DIFFUSE * NdotL *
    (lightColor.getGreen()/255.0) * intensity);
    int b = (int)(BASE_COLOR.getBlue() * METALLIC_DIFFUSE * NdotL *
    (lightColor.getBlue()/255.0) * intensity);
    
    // Add specular component
    r += (int)(specularColor.getRed() * specular *
    (lightColor.getRed()/255.0) * intensity);
    g += (int)(specularColor.getGreen() * specular *
    (lightColor.getGreen()/255.0) * intensity);
    b += (int)(specularColor.getBlue() * specular *
    (lightColor.getBlue()/255.0) * intensity);
    
    return new Color(
      clamp(r, 0, 255),
      clamp(g, 0, 255),
      clamp(b, 0, 255)
    );
  }
  
  private int clamp(int value, int min, int max) {
    return Math.max(min, Math.min(max, value));
  }
  
  @Override
  public double getReflectivity() {
    return REFLECTIVITY;
  }
  
  @Override
  public double getIndexOfRefraction() {
    return IOR;
  }
  
  @Override
  public double getTransparency() {
    return 0.0;
  }
  
}
