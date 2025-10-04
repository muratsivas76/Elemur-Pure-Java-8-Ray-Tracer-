package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.math.*;
import net.elena.murat.light.*;
import net.elena.murat.util.ColorUtil;

public class FjordCrystalMaterial implements Material {
  private final Color waterColor;
  private final Color crystalColor;
  private final double clarity;
  private Matrix4 objectTransform;
  
  private final double ambientCoeff = 0.3;
  private final double diffuseCoeff = 0.6;
  private final double specularCoeff = 0.8;
  private final double shininess = 120.0;
  private final double reflectivity = 0.7;
  private final double ior = 1.33;
  private final double transparency = 0.4;
  
  public FjordCrystalMaterial() {
    this(new Color(0x00, 0x7F, 0xFF), new Color(0xAF, 0xEE, 0xEE), 0.75);
  }
  
  public FjordCrystalMaterial(Color waterColor, Color crystalColor, double clarity) {
    this.waterColor = waterColor;
    this.crystalColor = crystalColor;
    this.clarity = Math.max(0, Math.min(1, clarity));
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
    
    Color surfaceColor = calculateWaterEffect(objectPoint, worldNormal, viewerPos);
    
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
  
  private Color calculateWaterEffect(Point3 point, Vector3 normal, Point3 viewerPos) {
    double x = point.x * 10.0;
    double y = point.y * 10.0;
    double z = point.z * 10.0;
    
    // Water caustics and wave patterns
    double wave1 = Math.sin(x * 1.5 + Math.sin(y * 0.8) * 2.0);
    double wave2 = Math.cos(y * 1.2 + Math.sin(z * 1.0) * 1.5);
    double wave3 = Math.sin(x * 2.0 + y * 1.7 + z * 0.5);
    
    double waterPattern = (wave1 * 0.4 + wave2 * 0.3 + wave3 * 0.3);
    double normalizedPattern = (waterPattern + 1.0) * 0.5;
    
    // View-dependent transparency effect
    Vector3 viewDir = viewerPos.subtract(point).normalize();
    double viewAngle = Math.abs(viewDir.dot(normal));
    double depthEffect = Math.pow(viewAngle, 0.5);
    
    if (normalizedPattern < clarity) {
      // Crystal clear water areas
      double intensity = normalizedPattern / clarity * depthEffect;
      return ColorUtil.blendColors(waterColor, crystalColor, intensity);
      } else {
      // Deeper water with more color saturation
      double intensity = (normalizedPattern - clarity) / (1.0 - clarity);
      Color deepWater = ColorUtil.darkenColor(waterColor, intensity * 0.4);
      return ColorUtil.addColorVariation(deepWater, intensity);
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
