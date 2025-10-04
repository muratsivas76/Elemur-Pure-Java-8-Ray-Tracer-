package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.math.*;
import net.elena.murat.light.*;
import net.elena.murat.util.ColorUtil;

public class VikingRuneMaterial implements Material {
  private final Color stoneColor;
  private final Color runeColor;
  private final double runeDepth;
  private Matrix4 objectTransform;
  
  private final double ambientCoeff = 0.4;
  private final double diffuseCoeff = 0.75;
  private final double specularCoeff = 0.1;
  private final double shininess = 12.0;
  private final double reflectivity = 0.05;
  private final double ior = 1.8;
  private final double transparency = 0.0;
  
  public VikingRuneMaterial() {
    this(new Color(0x60, 0x60, 0x60), new Color(0xE8, 0xD8, 0xC8), 0.35);
  }
  
  public VikingRuneMaterial(Color stoneColor, Color runeColor, double runeDepth) {
    this.stoneColor = stoneColor;
    this.runeColor = runeColor;
    this.runeDepth = Math.max(0, Math.min(1, runeDepth));
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
    
    Color surfaceColor = calculateRuneCarving(objectPoint, worldNormal);
    
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
    Color specular = ColorUtil.multiplyColors(new Color(0xCC, 0xCC, 0xCC), props.color, specularCoeff * specFactor);
    
    return ColorUtil.combineColors(ambient, diffuse, specular);
  }
  
  private Color calculateRuneCarving(Point3 point, Vector3 normal) {
    double x = point.x * 10.0;
    double y = point.y * 10.0;
    double z = point.z * 10.0;
    
    // Viking rune angular patterns
    double rune1 = Math.abs(Math.sin(x * 2.5) * Math.cos(y * 2.0));
    double rune2 = Math.abs(Math.sin(x * 3.0 + y * 1.7) + Math.cos(y * 2.3 + z * 1.2));
    double rune3 = (Math.floor(x * 0.8) + Math.floor(y * 0.8)) % 2.5;
    double ancient = Math.sin(x * 1.2 + y * 0.8 + z * 0.5) * 0.3 + 0.7;
    
    double combinedPattern = (rune1 * 0.35 + rune2 * 0.3 + rune3 * 0.25 + ancient * 0.1);
    double normalizedPattern = combinedPattern % 1.0;
    
    if (normalizedPattern < runeDepth) {
      // Deep rune carvings
      double depth = normalizedPattern / runeDepth;
      return ColorUtil.darkenColor(runeColor, depth * 0.6);
      } else {
      // Weathered stone surface
      double weathering = (normalizedPattern - runeDepth) / (1.0 - runeDepth);
      Color weatheredStone = ColorUtil.addColorVariation(stoneColor, weathering);
      return ColorUtil.darkenColor(weatheredStone, weathering * 0.2);
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
