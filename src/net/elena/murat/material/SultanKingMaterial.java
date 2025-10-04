package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.math.*;
import net.elena.murat.light.*;
import net.elena.murat.util.ColorUtil;

public class SultanKingMaterial implements Material {
  private final Color goldColor;
  private final Color rubyColor;
  private final Color sapphireColor;
  private final double royaltyIntensity;
  private Matrix4 objectTransform;
  
  private final double ambientCoeff = 0.45;
  private final double diffuseCoeff = 0.7;
  private final double specularCoeff = 0.4;
  private final double shininess = 75.0;
  private final double reflectivity = 0.35;
  private final double ior = 1.8;
  private final double transparency = 0.05;
  
  public SultanKingMaterial() {
    this(new Color(0xFF, 0xD7, 0x00), new Color(0xDC, 0x14, 0x3C), new Color(0x00, 0x64, 0xCD), 0.75);
  }
  
  public SultanKingMaterial(Color goldColor, Color rubyColor, Color sapphireColor, double royaltyIntensity) {
    this.goldColor = goldColor;
    this.rubyColor = rubyColor;
    this.sapphireColor = sapphireColor;
    this.royaltyIntensity = Math.max(0, Math.min(1, royaltyIntensity));
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
    
    Color surfaceColor = calculateRoyalRegalia(objectPoint, worldNormal, viewerPos);
    
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
    Color specular = ColorUtil.multiplyColors(goldColor, props.color, specularCoeff * specFactor);
    
    return ColorUtil.combineColors(ambient, diffuse, specular);
  }
  
  private Color calculateRoyalRegalia(Point3 point, Vector3 normal, Point3 viewerPos) {
    double x = point.x * 12.0;
    double y = point.y * 12.0;
    double z = point.z * 12.0;
    
    // Ottoman Sultan patterns (crescent, tughra, geometric)
    double ottoman1 = Math.sin(x * 2.0 + Math.cos(y * 1.5) * 3.0);
    double ottoman2 = Math.abs(Math.cos(x * 2.5 + y * 2.0) + Math.sin(y * 2.2 + z * 1.8));
    double ottoman3 = (Math.floor(x * 0.8) + Math.floor(y * 0.8)) % 2.2;
    
    // Viking King patterns (crown, runes, animal motifs)
    double viking1 = Math.sin(x * 1.8 + Math.sin(y * 1.2) * 2.5);
    double viking2 = Math.abs(Math.cos(x * 3.0 + y * 1.7) + Math.sin(y * 2.5 + z * 1.3));
    double viking3 = Math.sin(x * 2.2 + y * 1.9) * Math.cos(y * 1.4 + z * 1.1);
    
    // Royal insignia and emblem patterns
    double emblem1 = Math.sin(x * 3.5 + y * 2.8) + Math.cos(y * 2.5 + z * 2.0);
    double emblem2 = Math.abs(Math.sin(x * 4.0 + y * 3.0) * Math.cos(y * 2.2 + z * 1.7));
    
    // Royal fusion pattern
    double ottomanWeight = 0.5 * royaltyIntensity;
    double vikingWeight = 0.5 * royaltyIntensity;
    double emblemWeight = 0.2 * royaltyIntensity;
    
    double combinedPattern = (ottoman1 * 0.2 + ottoman2 * 0.15 + ottoman3 * 0.1) * ottomanWeight +
    (viking1 * 0.2 + viking2 * 0.15 + viking3 * 0.1) * vikingWeight +
    (emblem1 * 0.05 + emblem2 * 0.05) * emblemWeight;
    
    double normalizedPattern = (combinedPattern + 1.0) * 0.5;
    
    // View-dependent metallic effects
    Vector3 viewDir = viewerPos.subtract(point).normalize();
    double viewEffect = Math.pow(Math.abs(viewDir.dot(normal)), 0.8);
    double metallicEffect = 0.3 + viewEffect * 0.7;
    
    // Royal material selection
    if (normalizedPattern < 0.3) {
      // Gold base with intricate engravings
      double detail = normalizedPattern / 0.3;
      return ColorUtil.blendColors(
        ColorUtil.darkenColor(goldColor, 0.2),
        ColorUtil.lightenColor(goldColor, 0.1),
        detail * metallicEffect
      );
      } else if (normalizedPattern < 0.5) {
      // Ruby inlays (Ottoman influence)
      double intensity = (normalizedPattern - 0.3) / 0.2;
      Color richRuby = ColorUtil.blendColors(
        rubyColor,
        ColorUtil.lightenColor(rubyColor, 0.3),
        intensity * viewEffect
      );
      return ColorUtil.addSpecularHighlight(richRuby, metallicEffect * 0.5);
      } else if (normalizedPattern < 0.7) {
      // Sapphire accents (Viking influence)
      double intensity = (normalizedPattern - 0.5) / 0.2;
      Color deepSapphire = ColorUtil.blendColors(
        sapphireColor,
        ColorUtil.lightenColor(sapphireColor, 0.25),
        intensity * viewEffect
      );
      return ColorUtil.addSpecularHighlight(deepSapphire, metallicEffect * 0.6);
      } else if (normalizedPattern < 0.85) {
      // Gold-sapphire fusion areas
      double intensity = (normalizedPattern - 0.7) / 0.15;
      return ColorUtil.blendColors(
        sapphireColor,
        goldColor,
        intensity * metallicEffect
      );
      } else {
      // Gold-ruby royal emblems
      double intensity = (normalizedPattern - 0.85) / 0.15;
      Color royalEmblem = ColorUtil.blendColors(
        goldColor,
        rubyColor,
        intensity * 0.7
      );
      return ColorUtil.addSpecularHighlight(royalEmblem, metallicEffect * 0.8);
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
