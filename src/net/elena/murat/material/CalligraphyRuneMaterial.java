package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.math.*;
import net.elena.murat.light.*;
import net.elena.murat.util.ColorUtil;

public class CalligraphyRuneMaterial implements Material {
  private final Color parchmentColor;
  private final Color inkColor;
  private final Color goldLeafColor;
  private final double writingIntensity;
  private Matrix4 objectTransform;
  
  private final double ambientCoeff = 0.45;
  private final double diffuseCoeff = 0.8;
  private final double specularCoeff = 0.18;
  private final double shininess = 25.0;
  private final double reflectivity = 0.1;
  private final double ior = 1.6;
  private final double transparency = 0.0;
  
  public CalligraphyRuneMaterial() {
    this(new Color(0xF5, 0xDE, 0xB3), new Color(0x2F, 0x4F, 0x4F), new Color(0xFF, 0xD7, 0x00), 0.65);
  }
  
  public CalligraphyRuneMaterial(Color parchmentColor, Color inkColor, Color goldLeafColor, double writingIntensity) {
    this.parchmentColor = parchmentColor;
    this.inkColor = inkColor;
    this.goldLeafColor = goldLeafColor;
    this.writingIntensity = Math.max(0, Math.min(1, writingIntensity));
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
    
    Color surfaceColor = calculateWritingPattern(objectPoint, worldNormal);
    
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
    Color specular = ColorUtil.multiplyColors(goldLeafColor, props.color, specularCoeff * specFactor);
    
    return ColorUtil.combineColors(ambient, diffuse, specular);
  }
  
  private Color calculateWritingPattern(Point3 point, Vector3 normal) {
    double x = point.x * 15.0;
    double y = point.y * 15.0;
    double z = point.z * 15.0;
    
    // Islamic calligraphy flowing patterns
    double calligraphy1 = Math.sin(x * 1.2 + Math.cos(y * 0.8) * 2.0);
    double calligraphy2 = Math.abs(Math.cos(x * 1.5 + y * 1.0) + Math.sin(y * 1.2 + z * 0.7));
    double calligraphy3 = Math.sin(x * 2.0 + y * 1.5) * Math.cos(y * 1.0 + z * 0.5);
    
    // Viking rune angular patterns
    double rune1 = Math.abs(Math.sin(x * 2.5) * Math.cos(y * 2.0));
    double rune2 = (Math.floor(x * 0.8) + Math.floor(y * 0.8)) % 2.5;
    double rune3 = Math.abs(Math.sin(x * 3.0 + y * 1.7) + Math.cos(y * 2.3));
    
    // Cultural fusion writing pattern
    double calligraphyWeight = 0.5 * writingIntensity;
    double runeWeight = 0.5 * writingIntensity;
    
    double combinedPattern = (calligraphy1 * 0.2 + calligraphy2 * 0.15 + calligraphy3 * 0.15) * calligraphyWeight +
    (rune1 * 0.2 + rune2 * 0.15 + rune3 * 0.15) * runeWeight;
    
    double normalizedPattern = (combinedPattern + 1.0) * 0.5;
    
    // View-dependent effect for gold leaf
    Vector3 viewDir = new Vector3(0, 0, 1); // Simple view direction
    double viewEffect = Math.abs(viewDir.dot(normal)) * 0.5 + 0.5;
    
    if (normalizedPattern < 0.4) {
      // Parchment background with aging
      double ageEffect = normalizedPattern / 0.4;
      return ColorUtil.darkenColor(parchmentColor, ageEffect * 0.2);
      } else if (normalizedPattern < 0.7) {
      // Ink writing (both calligraphy and runes)
      double intensity = (normalizedPattern - 0.4) / 0.3;
      Color writingInk = ColorUtil.darkenColor(inkColor, intensity * 0.3);
      return ColorUtil.addColorVariation(writingInk, intensity);
      } else {
      // Gold leaf accents and decorations
      double intensity = (normalizedPattern - 0.7) / 0.3;
      Color gold = ColorUtil.blendColors(goldLeafColor,
      ColorUtil.lightenColor(goldLeafColor, 0.3), intensity);
      return ColorUtil.multiplyColors(gold, Color.WHITE, viewEffect);
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
