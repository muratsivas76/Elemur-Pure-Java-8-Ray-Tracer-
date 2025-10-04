package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.math.*;
import net.elena.murat.light.*;
import net.elena.murat.util.ColorUtil;

public class NordicWeaveMaterial implements Material {
  private final Color primaryColor;
  private final Color secondaryColor;
  private final Color accentColor;
  private final double patternScale;
  private Matrix4 objectTransform;
  
  private final double ambientCoeff = 0.5;
  private final double diffuseCoeff = 0.9;
  private final double specularCoeff = 0.1;
  private final double shininess = 10.0;
  private final double reflectivity = 0.05;
  private final double ior = 1.5;
  private final double transparency = 0.0;
  
  public NordicWeaveMaterial() {
    this(new Color(0x8B, 0x45, 0x13), new Color(0x00, 0x64, 0x64), new Color(0xDC, 0xDC, 0xDC), 4.0);
  }
  
  public NordicWeaveMaterial(Color primaryColor, Color secondaryColor, Color accentColor, double patternScale) {
    this.primaryColor = primaryColor;
    this.secondaryColor = secondaryColor;
    this.accentColor = accentColor;
    this.patternScale = patternScale;
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
    
    Color surfaceColor = calculateKilimPattern(objectPoint);
    
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
    Color specular = ColorUtil.multiplyColors(accentColor, props.color, specularCoeff * specFactor);
    
    return ColorUtil.combineColors(ambient, diffuse, specular);
  }
  
  private Color calculateKilimPattern(Point3 point) {
    double x = point.x * patternScale;
    double y = point.y * patternScale;
    double z = point.z * patternScale;
    
    // Viking rune patterns combined with Turkish geometric patterns
    double pattern1 = Math.sin(x * 2.0) * Math.cos(y * 2.0);
    double pattern2 = Math.abs(Math.sin(x * 3.0 + y * 3.0));
    double pattern3 = Math.floor(x * 0.5) + Math.floor(y * 0.5);
    
    double combinedPattern = (pattern1 + pattern2 + pattern3 % 2.0) % 3.0;
    
    if (combinedPattern < 1.0) {
      return primaryColor;
      } else if (combinedPattern < 2.0) {
      return secondaryColor;
      } else {
      return accentColor;
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
