package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.math.*;
import net.elena.murat.light.*;
import net.elena.murat.util.ColorUtil;

public class KilimRosemalingMaterial implements Material {
  private final Color kilimColor;
  private final Color rosemalingColor;
  private final Color accentColor;
  private final double patternIntensity;
  private Matrix4 objectTransform;
  
  private final double ambientCoeff = 0.5;
  private final double diffuseCoeff = 0.85;
  private final double specularCoeff = 0.1;
  private final double shininess = 15.0;
  private final double reflectivity = 0.06;
  private final double ior = 1.5;
  private final double transparency = 0.0;
  
  public KilimRosemalingMaterial() {
    this(new Color(0xC4, 0x00, 0x00), new Color(0x00, 0x64, 0x64), new Color(0xFF, 0xD7, 0x00), 0.7);
  }
  
  public KilimRosemalingMaterial(Color kilimColor, Color rosemalingColor, Color accentColor, double patternIntensity) {
    this.kilimColor = kilimColor;
    this.rosemalingColor = rosemalingColor;
    this.accentColor = accentColor;
    this.patternIntensity = Math.max(0, Math.min(1, patternIntensity));
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
    
    Color surfaceColor = calculateFusionPattern(objectPoint);
    
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
  
  private Color calculateFusionPattern(Point3 point) {
    double x = point.x * 12.0;
    double y = point.y * 12.0;
    double z = point.z * 12.0;
    
    // Kilim geometric patterns (Turkish)
    double kilimPattern1 = Math.abs(Math.sin(x * 2.0) + Math.cos(y * 2.0));
    double kilimPattern2 = (Math.floor(x * 1.2) + Math.floor(y * 1.2)) % 2.0;
    double kilimPattern3 = Math.abs(Math.sin(x * 3.0 + y * 2.0));
    
    // Rosemaling flower patterns (Norwegian)
    double rosePattern1 = Math.sin(x * 1.5) * Math.cos(y * 1.5 + Math.sin(z * 0.8));
    double rosePattern2 = Math.abs(Math.sin(x * 2.5 + y * 1.8) + Math.cos(y * 2.2));
    double rosePattern3 = Math.abs(Math.cos(x * 1.8 + y * 2.0 + z * 1.2));
    
    // Combine both cultural patterns
    double kilimWeight = 0.5 * patternIntensity;
    double roseWeight = 0.5 * patternIntensity;
    
    double combinedPattern = (kilimPattern1 * 0.2 + kilimPattern2 * 0.15 + kilimPattern3 * 0.15) * kilimWeight +
    (rosePattern1 * 0.2 + rosePattern2 * 0.15 + rosePattern3 * 0.15) * roseWeight;
    
    double normalizedPattern = (combinedPattern + 1.0) * 0.5;
    
    if (normalizedPattern < 0.3) {
      // Kilim base background
      return kilimColor;
      } else if (normalizedPattern < 0.6) {
      // Rosemaling flower elements
      double intensity = (normalizedPattern - 0.3) / 0.3;
      return ColorUtil.blendColors(rosemalingColor, ColorUtil.lightenColor(rosemalingColor, 0.2), intensity);
      } else if (normalizedPattern < 0.8) {
      // Accent details (shared cultural elements)
      return accentColor;
      } else {
      // Border and outline elements (fusion pattern)
      double intensity = (normalizedPattern - 0.8) / 0.2;
      Color borderColor = ColorUtil.blendColors(kilimColor, rosemalingColor, 0.5);
      return ColorUtil.darkenColor(borderColor, intensity * 0.4);
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
