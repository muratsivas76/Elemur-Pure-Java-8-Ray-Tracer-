package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.math.*;
import net.elena.murat.light.*;
import net.elena.murat.util.ColorUtil;

public class HotCopperMaterial implements Material {
  private final Color copperColor;
  private final Color patinaColor;
  private final double patinaAmount;
  private Matrix4 objectTransform;
  
  private final double ambientCoeff = 0.35;
  private final double diffuseCoeff = 0.65;
  private final double specularCoeff = 0.4;
  private final double shininess = 45.0;
  private final double reflectivity = 0.25;
  private final double ior = 2.6;
  private final double transparency = 0.0;
  
  public HotCopperMaterial() {
    this(new Color(0xB8, 0x73, 0x33), new Color(0x33, 0x99, 0x77), 0.25);
  }
  
  public HotCopperMaterial(Color copperColor, Color patinaColor, double patinaAmount) {
    this.copperColor = copperColor;
    this.patinaColor = patinaColor;
    this.patinaAmount = Math.max(0, Math.min(1, patinaAmount));
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
    
    Color surfaceColor = calculatePatinaPattern(objectPoint);
    
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
    Color specular = ColorUtil.multiplyColors(new Color(0xFF, 0xE6, 0xC9), props.color, specularCoeff * specFactor);
    
    return ColorUtil.combineColors(ambient, diffuse, specular);
  }
  
  private Color calculatePatinaPattern(Point3 point) {
    double noise1 = simplexNoise3D(point.x * 8, point.y * 8, point.z * 8);
    double noise2 = simplexNoise3D(point.x * 3 + 5.3, point.y * 3 + 2.7, point.z * 3 + 1.9);
    
    double pattern = (noise1 * 0.7 + noise2 * 0.3 + 1) * 0.5;
    
    if (pattern < patinaAmount) {
      double intensity = pattern / patinaAmount;
      return interpolateColor(patinaColor,
        new Color(Math.min(255, (int)(patinaColor.getRed() * 1.2)),
          Math.min(255, (int)(patinaColor.getGreen() * 0.9)),
        Math.min(255, (int)(patinaColor.getBlue() * 1.1))),
      intensity);
      } else {
      double intensity = (pattern - patinaAmount) / (1 - patinaAmount);
      return interpolateColor(copperColor,
        new Color(Math.min(255, (int)(copperColor.getRed() * 1.1)),
          Math.min(255, (int)(copperColor.getGreen() * 0.95)),
        Math.min(255, (int)(copperColor.getBlue() * 0.9))),
      intensity);
    }
  }
  
  private double simplexNoise3D(double x, double y, double z) {
    double value = Math.sin(x * 0.472) + Math.cos(y * 0.683) + Math.sin(z * 0.291);
    value += Math.cos(x * 1.732 + y * 0.846) * 0.6;
    value += Math.sin(y * 1.357 + z * 2.173) * 0.3;
    return value % 1.0;
  }
  
  private Color interpolateColor(Color c1, Color c2, double t) {
    t = Math.max(0, Math.min(1, t));
    int r = (int)(c1.getRed() * (1-t) + c2.getRed() * t);
    int g = (int)(c1.getGreen() * (1-t) + c2.getGreen() * t);
    int b = (int)(c1.getBlue() * (1-t) + c2.getBlue() * t);
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
