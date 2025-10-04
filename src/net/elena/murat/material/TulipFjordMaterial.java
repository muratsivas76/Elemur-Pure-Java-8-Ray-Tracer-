package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.math.*;
import net.elena.murat.light.*;
import net.elena.murat.util.ColorUtil;

public class TulipFjordMaterial implements Material {
  private final Color tulipColor;
  private final Color fjordColor;
  private final Color stemColor;
  private final double bloomIntensity;
  private Matrix4 objectTransform;
  
  private final double ambientCoeff = 0.4;
  private final double diffuseCoeff = 0.75;
  private final double specularCoeff = 0.35;
  private final double shininess = 50.0;
  private final double reflectivity = 0.2;
  private final double ior = 1.55;
  private final double transparency = 0.1;
  
  public TulipFjordMaterial() {
    this(new Color(0xFF, 0x00, 0x00), new Color(0x00, 0x7F, 0xFF), new Color(0x22, 0x8B, 0x22), 0.6);
  }
  
  public TulipFjordMaterial(Color tulipColor, Color fjordColor, Color stemColor, double bloomIntensity) {
    this.tulipColor = tulipColor;
    this.fjordColor = fjordColor;
    this.stemColor = stemColor;
    this.bloomIntensity = Math.max(0, Math.min(1, bloomIntensity));
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
    
    Color surfaceColor = calculateTulipFjordPattern(objectPoint, worldNormal, viewerPos);
    
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
    Color specular = ColorUtil.multiplyColors(new Color(0xFF, 0xF5, 0xEE), props.color, specularCoeff * specFactor);
    
    return ColorUtil.combineColors(ambient, diffuse, specular);
  }
  
  private Color calculateTulipFjordPattern(Point3 point, Vector3 normal, Point3 viewerPos) {
    double x = point.x * 10.0;
    double y = point.y * 10.0;
    double z = point.z * 10.0;
    
    // Tulip petal patterns (organic curves)
    double petal1 = Math.sin(x * 1.8 + Math.cos(y * 1.2) * 2.5);
    double petal2 = Math.abs(Math.cos(x * 2.0 + y * 1.5) + Math.sin(y * 1.8 + z * 1.0));
    double petal3 = Math.sin(x * 2.5 + y * 2.0) * Math.cos(y * 1.2 + z * 0.8);
    
    // Fjord water patterns (flowing waves)
    double fjord1 = Math.sin(x * 1.5 + Math.sin(y * 0.7) * 1.8);
    double fjord2 = Math.cos(y * 1.3 + Math.cos(x * 0.9) * 2.2);
    double fjord3 = Math.abs(Math.sin(x * 2.2 + y * 1.6 + z * 1.3));
    
    // Cultural fusion pattern
    double tulipWeight = bloomIntensity;
    double fjordWeight = 1.0 - bloomIntensity;
    
    double combinedPattern = (petal1 * 0.25 + petal2 * 0.2 + petal3 * 0.15) * tulipWeight +
    (fjord1 * 0.2 + fjord2 * 0.15 + fjord3 * 0.05) * fjordWeight;
    
    double normalizedPattern = (combinedPattern + 1.0) * 0.5;
    
    // View-dependent effects
    Vector3 viewDir = viewerPos.subtract(point).normalize();
    double viewAngle = Math.abs(viewDir.dot(normal));
    double viewEffect = Math.pow(viewAngle, 0.7);
    
    if (normalizedPattern < 0.3) {
      // Fjord water background
      double depth = normalizedPattern / 0.3;
      return ColorUtil.blendColors(fjordColor, ColorUtil.darkenColor(fjordColor, 0.4), depth);
      } else if (normalizedPattern < 0.6) {
      // Tulip petals with gradient
      double intensity = (normalizedPattern - 0.3) / 0.3;
      Color gradientTulip = ColorUtil.blendColors(tulipColor,
      ColorUtil.lightenColor(tulipColor, 0.3), intensity);
      return ColorUtil.multiplyColors(gradientTulip, Color.WHITE, viewEffect);
      } else if (normalizedPattern < 0.8) {
      // Stem and leaf elements
      return stemColor;
      } else {
      // Water reflections and highlights
      double intensity = (normalizedPattern - 0.8) / 0.2;
      Color reflection = ColorUtil.blendColors(fjordColor, Color.WHITE, intensity * 0.5);
      return ColorUtil.lightenColor(reflection, intensity * 0.7 * viewEffect);
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
