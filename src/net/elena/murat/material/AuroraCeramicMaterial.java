package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.math.*;
import net.elena.murat.light.*;
import net.elena.murat.util.ColorUtil;

public class AuroraCeramicMaterial implements Material {
  private final Color baseColor;
  private final Color auroraColor;
  private final double auroraIntensity;
  private Matrix4 objectTransform;
  
  private final double ambientCoeff = 0.5;
  private final double diffuseCoeff = 0.7;
  private final double specularCoeff = 0.35;
  private final double shininess = 55.0;
  private final double reflectivity = 0.22;
  private final double ior = 1.7;
  private final double transparency = 0.0;
  
  public AuroraCeramicMaterial() {
    this(new Color(0xF5, 0xF5, 0xDC), new Color(0x00, 0xFF, 0x7F), 0.45);
  }
  
  public AuroraCeramicMaterial(Color baseColor, Color auroraColor, double auroraIntensity) {
    this.baseColor = baseColor;
    this.auroraColor = auroraColor;
    this.auroraIntensity = Math.max(0, Math.min(1, auroraIntensity));
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
    
    Color surfaceColor = calculateAuroraEffect(objectPoint, worldNormal, viewerPos);
    
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
  
  private Color calculateAuroraEffect(Point3 point, Vector3 normal, Point3 viewerPos) {
    double x = point.x * 7.0;
    double y = point.y * 7.0;
    double z = point.z * 7.0;
    
    // Aurora-like flowing patterns
    double flow1 = Math.sin(x * 1.2 + Math.cos(y * 0.8) + Math.sin(z * 1.5));
    double flow2 = Math.cos(x * 0.7 + Math.sin(y * 1.3) + Math.cos(z * 0.9));
    double flow3 = Math.sin(x * 2.1 + y * 1.7 + z * 0.5);
    
    double auroraPattern = (flow1 * 0.4 + flow2 * 0.3 + flow3 * 0.3);
    double normalizedPattern = (auroraPattern + 1.0) * 0.5;
    
    // View-dependent effect for aurora
    Vector3 viewDir = viewerPos.subtract(point).normalize();
    double viewAngle = Math.abs(viewDir.dot(normal));
    double viewEffect = Math.pow(viewAngle, 0.7);
    
    if (normalizedPattern < auroraIntensity) {
      // Aurora glow effect
      double intensity = normalizedPattern / auroraIntensity * viewEffect;
      return createAuroraGlow(baseColor, auroraColor, intensity);
      } else {
      // Ceramic base with subtle glow
      double intensity = (normalizedPattern - auroraIntensity) / (1.0 - auroraIntensity);
      return addCeramicGlow(baseColor, auroraColor, intensity * 0.3 * viewEffect);
    }
  }
  
  private Color createAuroraGlow(Color base, Color glow, double intensity) {
    int r = (int)(base.getRed() * (1-intensity) + glow.getRed() * intensity);
    int g = (int)(base.getGreen() * (1-intensity) + glow.getGreen() * intensity);
    int b = (int)(base.getBlue() * (1-intensity) + glow.getBlue() * intensity);
    
    // Add extra glow effect
    double glowBoost = intensity * 0.5;
    r = Math.min(255, r + (int)(glow.getRed() * glowBoost));
    g = Math.min(255, g + (int)(glow.getGreen() * glowBoost));
    b = Math.min(255, b + (int)(glow.getBlue() * glowBoost));
    
    return new Color(r, g, b);
  }
  
  private Color addCeramicGlow(Color base, Color glow, double intensity) {
    int r = (int)(base.getRed() + glow.getRed() * intensity * 0.3);
    int g = (int)(base.getGreen() + glow.getGreen() * intensity * 0.4);
    int b = (int)(base.getBlue() + glow.getBlue() * intensity * 0.2);
    
    return new Color(
      Math.min(255, Math.max(0, r)),
      Math.min(255, Math.max(0, g)),
      Math.min(255, Math.max(0, b))
    );
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
