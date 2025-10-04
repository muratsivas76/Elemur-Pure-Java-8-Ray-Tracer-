package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.math.*;
import net.elena.murat.light.*;
import net.elena.murat.util.ColorUtil;

public class NorthernLightMaterial implements Material {
  private final Color primaryAurora;
  private final Color secondaryAurora;
  private final double intensity;
  private Matrix4 objectTransform;
  
  private final double ambientCoeff = 0.2;
  private final double diffuseCoeff = 0.4;
  private final double specularCoeff = 0.9;
  private final double shininess = 80.0;
  private final double reflectivity = 0.3;
  private final double ior = 1.45;
  private final double transparency = 0.6;
  
  public NorthernLightMaterial() {
    this(new Color(0x00, 0xFF, 0x7F), new Color(0x00, 0xBF, 0xFF), 0.85);
  }
  
  public NorthernLightMaterial(Color primaryAurora, Color secondaryAurora, double intensity) {
    this.primaryAurora = primaryAurora;
    this.secondaryAurora = secondaryAurora;
    this.intensity = Math.max(0, Math.min(1, intensity));
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
    double x = point.x * 8.0;
    double y = point.y * 12.0;
    double z = point.z * 8.0;
    
    // Aurora curtain wave patterns
    double curtain1 = Math.sin(x * 0.7 + Math.sin(y * 1.2) * 3.0 + z * 0.3);
    double curtain2 = Math.cos(y * 1.5 + Math.sin(x * 0.9) * 2.5 + z * 0.4);
    double curtain3 = Math.sin(x * 1.1 + y * 2.0 + Math.cos(z * 0.6) * 2.0);
    
    double auroraPattern = (curtain1 * 0.5 + curtain2 * 0.3 + curtain3 * 0.2);
    double normalizedPattern = (auroraPattern + 1.0) * 0.5;
    
    // Time-based animation simulation (using z-coordinate as time proxy)
    double timeEffect = Math.sin(z * 0.5 + System.currentTimeMillis() * 0.0001) * 0.3 + 0.7;
    
    // View-dependent intensity
    Vector3 viewDir = viewerPos.subtract(point).normalize();
    double viewEffect = Math.pow(Math.abs(viewDir.dot(normal)), 0.3);
    
    double finalIntensity = intensity * timeEffect * viewEffect;
    
    if (normalizedPattern < 0.6) {
      // Primary aurora green
      double ratio = normalizedPattern / 0.6;
      Color baseAurora = ColorUtil.blendColors(primaryAurora, secondaryAurora, ratio * 0.4);
      return ColorUtil.lightenColor(baseAurora, finalIntensity * 0.8);
      } else {
      // Secondary aurora blue with glow effect
      double ratio = (normalizedPattern - 0.6) / 0.4;
      Color glowingAurora = ColorUtil.blendColors(secondaryAurora, primaryAurora, ratio * 0.2);
      
      // Add emission glow
      int r = glowingAurora.getRed() + (int)(finalIntensity * 50);
      int g = glowingAurora.getGreen() + (int)(finalIntensity * 40);
      int b = glowingAurora.getBlue() + (int)(finalIntensity * 30);
      
      return ColorUtil.createColor(r, g, b);
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
