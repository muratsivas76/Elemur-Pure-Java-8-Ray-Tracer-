package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.math.*;
import net.elena.murat.light.*;
import net.elena.murat.util.ColorUtil;

public class RuneStoneMaterial implements Material {
  private final Color stoneColor;
  private final Color runeColor;
  private final double runeDensity;
  private Matrix4 objectTransform;
  
  private final double ambientCoeff = 0.4;
  private final double diffuseCoeff = 0.75;
  private final double specularCoeff = 0.12;
  private final double shininess = 12.0;
  private final double reflectivity = 0.06;
  private final double ior = 1.8;
  private final double transparency = 0.0;
  
  public RuneStoneMaterial() {
    this(new Color(0x60, 0x60, 0x60), new Color(0xE8, 0xD8, 0xC8), 0.3);
  }
  
  public RuneStoneMaterial(Color stoneColor, Color runeColor, double runeDensity) {
    this.stoneColor = stoneColor;
    this.runeColor = runeColor;
    this.runeDensity = Math.max(0, Math.min(1, runeDensity));
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
    
    Color surfaceColor = calculateRunePattern(objectPoint);
    
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
  
  private Color calculateRunePattern(Point3 point) {
    double x = point.x * 8.0;
    double y = point.y * 8.0;
    double z = point.z * 8.0;
    
    // Create rune-like angular patterns
    double pattern1 = Math.abs(Math.sin(x * 1.5) * Math.cos(y * 2.0));
    double pattern2 = Math.abs(Math.sin(x * 3.0 + y * 1.7) + Math.cos(y * 2.3 + z * 1.2));
    double pattern3 = (Math.floor(x * 0.7) + Math.floor(y * 0.7)) % 2.0;
    
    double combinedPattern = (pattern1 * 0.4 + pattern2 * 0.3 + pattern3 * 0.3);
    double normalizedPattern = (combinedPattern % 1.0 + 1.0) % 1.0;
    
    if (normalizedPattern < runeDensity) {
      // Rune carving effect - slightly recessed
      double depth = normalizedPattern / runeDensity;
      return ColorUtil.darkenColor(runeColor, depth * 0.3);
      } else {
      // Stone surface
      double variation = (normalizedPattern - runeDensity) / (1.0 - runeDensity);
      return addStoneTexture(stoneColor, variation);
    }
  }
  
  private Color addStoneTexture(Color baseColor, double variation) {
    double noise = Math.sin(variation * 20.0) * 0.1 + 0.9;
    int r = (int)(baseColor.getRed() * noise);
    int g = (int)(baseColor.getGreen() * noise);
    int b = (int)(baseColor.getBlue() * noise);
    return new Color(r, g, b);
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
