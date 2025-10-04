package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.math.*;
import net.elena.murat.light.*;
import net.elena.murat.util.ColorUtil;

public class TelemarkPatternMaterial implements Material {
  private final Color baseColor;
  private final Color patternColor;
  private final Color accentColor;
  private final double patternScale;
  private Matrix4 objectTransform;
  
  private final double ambientCoeff = 0.45;
  private final double diffuseCoeff = 0.8;
  private final double specularCoeff = 0.12;
  private final double shininess = 18.0;
  private final double reflectivity = 0.07;
  private final double ior = 1.6;
  private final double transparency = 0.0;
  
  public TelemarkPatternMaterial() {
    this(new Color(0x8B, 0x00, 0x00), new Color(0xFF, 0xD7, 0x00), new Color(0x00, 0x64, 0x00), 5.0);
  }
  
  public TelemarkPatternMaterial(Color baseColor, Color patternColor, Color accentColor, double patternScale) {
    this.baseColor = baseColor;
    this.patternColor = patternColor;
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
    
    Color surfaceColor = calculateTelemarkPattern(objectPoint);
    
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
  
  private Color calculateTelemarkPattern(Point3 point) {
    double x = point.x * patternScale;
    double y = point.y * patternScale;
    double z = point.z * patternScale;
    
    // Traditional Telemark geometric patterns
    double diamond1 = Math.abs(Math.sin(x * 2.0) + Math.cos(y * 2.0));
    double diamond2 = Math.abs(Math.sin(x * 3.0 + y * 1.5) + Math.cos(y * 2.0 + x * 1.2));
    double cross = (Math.floor(x * 1.5) + Math.floor(y * 1.5)) % 3.0;
    double border = Math.abs(Math.sin(x * 4.0) * Math.cos(y * 4.0));
    
    double combinedPattern = (diamond1 * 0.3 + diamond2 * 0.25 + cross * 0.25 + border * 0.2);
    double normalizedPattern = combinedPattern % 1.0;
    
    if (normalizedPattern < 0.3) {
      // Base color with diamond pattern
      return baseColor;
      } else if (normalizedPattern < 0.6) {
      // Main geometric pattern
      return patternColor;
      } else if (normalizedPattern < 0.8) {
      // Accent details
      return accentColor;
      } else {
      // Border and outline elements
      return ColorUtil.darkenColor(patternColor, 0.4);
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
