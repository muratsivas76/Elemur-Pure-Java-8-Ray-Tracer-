package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.math.*;
import net.elena.murat.light.*;
import net.elena.murat.util.ColorUtil;

public class NordicWoodMaterial implements Material {
  private final Color woodColor;
  private final Color grainColor;
  private final double grainIntensity;
  private Matrix4 objectTransform;
  
  private final double ambientCoeff = 0.45;
  private final double diffuseCoeff = 0.85;
  private final double specularCoeff = 0.18;
  private final double shininess = 25.0;
  private final double reflectivity = 0.09;
  private final double ior = 1.6;
  private final double transparency = 0.0;
  
  public NordicWoodMaterial() {
    this(new Color(0x8B, 0x5A, 0x2B), new Color(0x5D, 0x40, 0x35), 0.5);
  }
  
  public NordicWoodMaterial(Color woodColor, Color grainColor, double grainIntensity) {
    this.woodColor = woodColor;
    this.grainColor = grainColor;
    this.grainIntensity = Math.max(0, Math.min(1, grainIntensity));
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
    
    Color surfaceColor = calculateWoodGrain(objectPoint);
    
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
    Color specular = ColorUtil.multiplyColors(new Color(0xFF, 0xEC, 0xCB), props.color, specularCoeff * specFactor);
    
    return ColorUtil.combineColors(ambient, diffuse, specular);
  }
  
  private  Color calculateWoodGrain(Point3 point) {
    double x = point.x * 12.0;
    double y = point.y * 12.0;
    double z = point.z * 12.0;
    
    // Wood grain pattern simulation
    double ringPattern = Math.sin(x * 0.8 + Math.sin(y * 0.3) * 2.0);
    double linePattern = Math.abs(Math.sin(y * 4.0 + Math.cos(x * 1.2) * 0.5));
    double noisePattern = Math.sin(x * 5.0 + y * 3.0 + z * 2.0) * 0.3;
    
    double combinedPattern = (ringPattern * 0.5 + linePattern * 0.3 + noisePattern * 0.2);
    double normalizedPattern = (combinedPattern + 1.0) * 0.5;
    
    if (normalizedPattern < grainIntensity) {
      // Wood grain lines
      double intensity = normalizedPattern / grainIntensity;
      return ColorUtil.blendColors(woodColor, grainColor, intensity * 0.7);
      } else {
      // Base wood color with variation
      double intensity = (normalizedPattern - grainIntensity) / (1.0 - grainIntensity);
      return addWoodVariation(woodColor, intensity);
    }
  }
  
  private Color addWoodVariation(Color baseColor, double variation) {
    double warmth = 0.9 + Math.sin(variation * 15.0) * 0.1;
    double darkness = 0.85 + Math.cos(variation * 8.0) * 0.15;
    
    int r = (int)(baseColor.getRed() * warmth * darkness);
    int g = (int)(baseColor.getGreen() * warmth * darkness * 0.95);
    int b = (int)(baseColor.getBlue() * darkness * 0.9);
    
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
