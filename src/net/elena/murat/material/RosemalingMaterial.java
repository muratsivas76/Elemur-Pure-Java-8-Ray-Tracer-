package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.math.*;
import net.elena.murat.light.*;
import net.elena.murat.util.ColorUtil;

public class RosemalingMaterial implements Material {
  private final Color backgroundColor;
  private final Color flowerColor;
  private final Color accentColor;
  private final double patternDensity;
  private Matrix4 objectTransform;
  
  private final double ambientCoeff = 0.5;
  private final double diffuseCoeff = 0.85;
  private final double specularCoeff = 0.15;
  private final double shininess = 20.0;
  private final double reflectivity = 0.08;
  private final double ior = 1.5;
  private final double transparency = 0.0;
  
  public RosemalingMaterial() {
    this(new Color(0x2F, 0x4F, 0x4F), new Color(0xFF, 0x69, 0xB4), new Color(0xFF, 0xD7, 0x00), 0.65);
  }
  
  public RosemalingMaterial(Color backgroundColor, Color flowerColor, Color accentColor, double patternDensity) {
    this.backgroundColor = backgroundColor;
    this.flowerColor = flowerColor;
    this.accentColor = accentColor;
    this.patternDensity = Math.max(0, Math.min(1, patternDensity));
    this.objectTransform = Matrix4.identity();
  }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
    if (tm == null) tm = new Matrix4 ();
    this.objectTransform = tm;
  }
  
  @Override
  public Color getColorAt(Point3 worldPoint, Vector3 worldNormal, Light light, Point3 viewerPos) {
    Point3 objectPoint = objectTransform.inverse().transformPoint(worldPoint);
    
    Color surfaceColor = calculateRosemalingPattern(objectPoint);
    
    LightProperties props = LightProperties.getLightProperties(light, worldPoint);
    if (props == null) return surfaceColor;
    
    Color ambient = ColorUtil.multiplyColors(surfaceColor, props.color, ambientCoeff);
    
    if (light instanceof ElenaMuratAmbientLight) {
      return ambient;
    }
    
    double NdotL = Math.max(0, worldNormal.dot(props.direction));
    Color diffuse = ColorUtil.multiplyColors(surfaceColor, props.color, diffuseCoeff * NdotL * props.intensity);
    
    Vector3 viewDir = viewerPos.subtract(worldPoint).normalize();
    Vector3 reflectDir = props.direction.negate().reflect(worldNormal);
    double RdotV = Math.max(0, reflectDir.dot(viewDir));
    double specFactor = Math.pow(RdotV, shininess) * props.intensity;
    Color specular = ColorUtil.multiplyColors(Color.WHITE, props.color, specularCoeff * specFactor);
    
    return ColorUtil.combineColors(ambient, diffuse, specular);
  }
  
  private Color calculateRosemalingPattern(Point3 point) {
    double x = point.x * 15.0;
    double y = point.y * 15.0;
    double z = point.z * 15.0;
    
    // Rosemaling flower and scroll patterns
    double flower1 = Math.sin(x * 2.0) * Math.cos(y * 2.0 + Math.sin(z * 1.0));
    double flower2 = Math.abs(Math.sin(x * 3.0 + y * 2.0) + Math.cos(y * 2.5 + z * 1.5));
    double scroll = Math.sin(x * 1.5 + y * 1.2) * Math.cos(y * 0.8 + z * 0.7);
    double leaves = Math.abs(Math.cos(x * 2.2 + y * 1.8 + z * 1.0));
    
    double combinedPattern = (flower1 * 0.3 + flower2 * 0.25 + scroll * 0.25 + leaves * 0.2);
    double normalizedPattern = (combinedPattern + 1.0) * 0.5;
    
    if (normalizedPattern < patternDensity * 0.4) {
      // Flower centers
      return accentColor;
      } else if (normalizedPattern < patternDensity * 0.7) {
      // Flower petals
      double intensity = (normalizedPattern - patternDensity * 0.4) / (patternDensity * 0.3);
      return ColorUtil.blendColors(flowerColor, ColorUtil.lightenColor(flowerColor, 0.3), intensity);
      } else if (normalizedPattern < patternDensity) {
      // Scrollwork and leaves
      double intensity = (normalizedPattern - patternDensity * 0.7) / (patternDensity * 0.3);
      return ColorUtil.blendColors(accentColor, backgroundColor, intensity * 0.5);
      } else {
      // Background with subtle texture
      double intensity = (normalizedPattern - patternDensity) / (1.0 - patternDensity);
      return ColorUtil.addColorVariation(backgroundColor, intensity);
    }
  }
  
  @Override
  public double getReflectivity() {
    return reflectivity;
  }
  
  @Override
  public double getIndexOfRefraction() {
    return ior;
  }
  
  @Override
  public double getTransparency() {
    return transparency;
  }
  
}
