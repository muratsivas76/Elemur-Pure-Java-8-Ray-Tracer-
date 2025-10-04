package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.math.*;
import net.elena.murat.light.*;
import net.elena.murat.util.ColorUtil;

public class BrunostCheeseMaterial implements Material {
  private final Color cheeseColor;
  private final Color caramelColor;
  private final double caramelAmount;
  private Matrix4 objectTransform;
  
  private final double ambientCoeff = 0.5;
  private final double diffuseCoeff = 0.9;
  private final double specularCoeff = 0.25;
  private final double shininess = 30.0;
  private final double reflectivity = 0.15;
  private final double ior = 1.55;
  private final double transparency = 0.0;
  
  public BrunostCheeseMaterial() {
    this(new Color(0xD2, 0x69, 0x1E), new Color(0x8B, 0x45, 0x13), 0.4);
  }
  
  public BrunostCheeseMaterial(Color cheeseColor, Color caramelColor, double caramelAmount) {
    this.cheeseColor = cheeseColor;
    this.caramelColor = caramelColor;
    this.caramelAmount = Math.max(0, Math.min(1, caramelAmount));
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
    
    Color surfaceColor = calculateCheeseTexture(objectPoint);
    
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
    Color specular = ColorUtil.multiplyColors(new Color(0xFF, 0xEC, 0x8B), props.color, specularCoeff * specFactor);
    
    return ColorUtil.combineColors(ambient, diffuse, specular);
  }
  
  private Color calculateCheeseTexture(Point3 point) {
    double x = point.x * 8.0;
    double y = point.y * 8.0;
    double z = point.z * 8.0;
    
    // Cheese texture with caramel veins
    double cheeseBase = Math.sin(x * 1.2 + Math.cos(y * 0.8)) * 0.5 + 0.5;
    double caramelVein1 = Math.abs(Math.sin(x * 3.0 + y * 2.0 + z * 1.5));
    double caramelVein2 = Math.abs(Math.cos(x * 2.5 + y * 1.8 + z * 2.0));
    double textureNoise = Math.sin(x * 5.0 + y * 4.0 + z * 3.0) * 0.2 + 0.8;
    
    double veinPattern = (caramelVein1 * 0.6 + caramelVein2 * 0.4);
    
    if (veinPattern < caramelAmount) {
      // Caramel veins
      double intensity = veinPattern / caramelAmount;
      Color veinColor = ColorUtil.blendColors(caramelColor,
      ColorUtil.darkenColor(caramelColor, 0.3), intensity);
      return ColorUtil.addColorVariation(veinColor, intensity);
      } else {
      // Cheese base with texture
      double intensity = (veinPattern - caramelAmount) / (1.0 - caramelAmount);
      Color texturedCheese = ColorUtil.multiplyColors(cheeseColor,
      ColorUtil.createColor(255, 255, 255), textureNoise);
      return ColorUtil.blendColors(texturedCheese,
      ColorUtil.lightenColor(cheeseColor, 0.1), intensity);
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
