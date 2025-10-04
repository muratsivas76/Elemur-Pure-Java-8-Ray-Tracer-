package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.math.*;
import net.elena.murat.light.*;
import net.elena.murat.util.ColorUtil;

public class CoffeeFjordMaterial implements Material {
  private final Color coffeeColor;
  private final Color fjordColor;
  private final double blendIntensity;
  private Matrix4 objectTransform;
  
  private final double ambientCoeff = 0.45;
  private final double diffuseCoeff = 0.8;
  private final double specularCoeff = 0.25;
  private final double shininess = 35.0;
  private final double reflectivity = 0.15;
  private final double ior = 2.1;
  private final double transparency = 0.0;
  
  public CoffeeFjordMaterial() {
    this(new Color(0x4A, 0x2C, 0x1D), new Color(0x1E, 0x90, 0xFF), 0.4);
  }
  
  public CoffeeFjordMaterial(Color coffeeColor, Color fjordColor, double blendIntensity) {
    this.coffeeColor = coffeeColor;
    this.fjordColor = fjordColor;
    this.blendIntensity = Math.max(0, Math.min(1, blendIntensity));
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
    
    Color surfaceColor = calculateMarbleEffect(objectPoint);
    
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
    Color specular = ColorUtil.multiplyColors(new Color(0xFF, 0xF5, 0xE1), props.color, specularCoeff * specFactor);
    
    return ColorUtil.combineColors(ambient, diffuse, specular);
  }
  
  private Color calculateMarbleEffect(Point3 point) {
    double x = point.x * 6.0;
    double y = point.y * 6.0;
    double z = point.z * 6.0;
    
    // Marble-like turbulence effect
    double turbulence = Math.sin(x * 2.0 + Math.sin(y * 3.0) + Math.sin(z * 1.5 + Math.cos(x * 2.5)));
    double pattern = (turbulence + 2.0) / 4.0; // Normalize to 0-1
    
    if (pattern < blendIntensity) {
      double ratio = pattern / blendIntensity;
      return ColorUtil.blendColors(coffeeColor, fjordColor, ratio);
      } else {
      double ratio = (pattern - blendIntensity) / (1.0 - blendIntensity);
      Color darkCoffee = new Color(
        Math.max(0, coffeeColor.getRed() - 30),
        Math.max(0, coffeeColor.getGreen() - 20),
        Math.max(0, coffeeColor.getBlue() - 10)
      );
      return ColorUtil.blendColors(fjordColor, darkCoffee, ratio);
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
