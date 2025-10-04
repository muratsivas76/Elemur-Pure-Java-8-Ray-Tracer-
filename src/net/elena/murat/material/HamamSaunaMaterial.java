package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.math.*;
import net.elena.murat.light.*;
import net.elena.murat.util.ColorUtil;

public class HamamSaunaMaterial implements Material {
  private final Color marbleColor;
  private final Color woodColor;
  private final Color steamColor;
  private final double steamIntensity;
  private Matrix4 objectTransform;
  
  private final double ambientCoeff = 0.5;
  private final double diffuseCoeff = 0.8;
  private final double specularCoeff = 0.25;
  private final double shininess = 40.0;
  private final double reflectivity = 0.15;
  private final double ior = 1.55;
  private final double transparency = 0.12;
  
  public HamamSaunaMaterial() {
    this(new Color(0xE6, 0xE6, 0xFA), new Color(0x8B, 0x45, 0x13), new Color(0xF5, 0xF5, 0xF5, 150), 0.55);
  }
  
  public HamamSaunaMaterial(Color marbleColor, Color woodColor, Color steamColor, double steamIntensity) {
    this.marbleColor = marbleColor;
    this.woodColor = woodColor;
    this.steamColor = steamColor;
    this.steamIntensity = Math.max(0, Math.min(1, steamIntensity));
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
    
    Color surfaceColor = calculateHamamSaunaPattern(objectPoint, worldNormal, viewerPos);
    
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
    Color specular = ColorUtil.multiplyColors(new Color(0xFF, 0xFA, 0xF0), props.color, specularCoeff * specFactor);
    
    return ColorUtil.combineColors(ambient, diffuse, specular);
  }
  
  private Color calculateHamamSaunaPattern(Point3 point, Vector3 normal, Point3 viewerPos) {
    double x = point.x * 8.0;
    double y = point.y * 8.0;
    double z = point.z * 8.0;
    
    // Marble veining patterns (Hamam - Turkish)
    double marbleVein1 = Math.sin(x * 2.5 + Math.cos(y * 1.8) * 3.0);
    double marbleVein2 = Math.abs(Math.cos(x * 3.0 + y * 2.2) + Math.sin(y * 2.5 + z * 1.5));
    double marbleVein3 = Math.sin(x * 4.0 + y * 3.0) * Math.cos(y * 2.0 + z * 1.8);
    
    // Wood grain patterns (Sauna - Norwegian)
    double woodGrain1 = Math.sin(x * 1.2 + Math.sin(y * 0.5) * 2.0);
    double woodGrain2 = (Math.floor(x * 0.6) + Math.floor(y * 0.6)) % 1.8;
    double woodGrain3 = Math.abs(Math.sin(x * 2.8 + y * 1.4 + z * 1.0));
    
    // Steam/mist effect
    double steamEffect = Math.sin(x * 0.7 + y * 0.9 + z * 1.2) * Math.cos(y * 1.1 + z * 0.8);
    
    // Cultural fusion pattern
    double marbleWeight = 0.6;
    double woodWeight = 0.4;
    
    double materialPattern = (marbleVein1 * 0.3 + marbleVein2 * 0.2 + marbleVein3 * 0.2) * marbleWeight +
    (woodGrain1 * 0.25 + woodGrain2 * 0.15 + woodGrain3 * 0.1) * woodWeight;
    
    double normalizedPattern = (materialPattern + 1.0) * 0.5;
    double steamNormalized = (steamEffect + 1.0) * 0.5;
    
    // View-dependent effects for steam
    Vector3 viewDir = viewerPos.subtract(point).normalize();
    double viewEffect = Math.pow(Math.abs(viewDir.dot(normal)), 0.6);
    
    // Base material selection
    Color baseColor;
    if (normalizedPattern < 0.4) {
      // Marble background
      double veinIntensity = normalizedPattern / 0.4;
      baseColor = ColorUtil.blendColors(marbleColor,
      ColorUtil.darkenColor(marbleColor, 0.15), veinIntensity);
      } else if (normalizedPattern < 0.7) {
      // Wood grain details
      double woodIntensity = (normalizedPattern - 0.4) / 0.3;
      baseColor = ColorUtil.blendColors(woodColor,
      ColorUtil.lightenColor(woodColor, 0.25), woodIntensity);
      } else {
      // Marble-wood transition areas
      double blendIntensity = (normalizedPattern - 0.7) / 0.3;
      baseColor = ColorUtil.blendColors(marbleColor, woodColor, blendIntensity);
    }
    
    // Apply steam effect
    if (steamNormalized > (0.7 - steamIntensity * 0.3)) {
      double steamAlpha = (steamNormalized - (0.7 - steamIntensity * 0.3)) / (steamIntensity * 0.3 + 0.3);
      steamAlpha = Math.min(1.0, steamAlpha * 1.5);
      return ColorUtil.blendColors(baseColor, steamColor, steamAlpha * viewEffect);
    }
    
    return baseColor;
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
