package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.math.*;
import net.elena.murat.light.*;
import net.elena.murat.util.ColorUtil;

public class VikingMetalMaterial implements Material {
  private final Color baseColor;
  private final Color rustColor;
  private final double rustDensity;
  private Matrix4 objectTransform;
  
  private final double ambientCoeff = 0.4;
  private final double diffuseCoeff = 0.7;
  private final double specularCoeff = 0.15;
  private final double shininess = 15.0;
  private final double reflectivity = 0.08;
  private final double ior = 2.8;
  private final double transparency = 0.0;
  
  public VikingMetalMaterial() {
    this(new Color(0x22, 0x22, 0x22), new Color(0xBB, 0x55, 0x22), 0.35);
  }
  
  public VikingMetalMaterial(Color baseColor, Color rustColor, double rustDensity) {
    this.baseColor = baseColor;
    this.rustColor = rustColor;
    this.rustDensity = Math.max(0, Math.min(1, rustDensity));
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
    
    Color surfaceColor = calculateRustPattern(objectPoint);
    
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
    Color specular = ColorUtil.multiplyColors(Color.LIGHT_GRAY, props.color, specularCoeff * specFactor);
    
    return ColorUtil.combineColors(ambient, diffuse, specular);
  }
  
  private Color calculateRustPattern(Point3 point) {
    double noise = simplexNoise3D(point.x * 5, point.y * 5, point.z * 5);
    
    double t = (noise + 1) * 0.5;
    
    if (t < rustDensity) {
      return interpolateColor(rustColor,
        new Color(Math.min(255, (int)(rustColor.getRed() * 0.8)),
          Math.min(255, (int)(rustColor.getGreen() * 0.9)),
        Math.min(255, (int)(rustColor.getBlue() * 1.1))),
      t / rustDensity);
      } else {
      return interpolateColor(baseColor,
        new Color(Math.min(255, (int)(baseColor.getRed() * 1.2)),
          Math.min(255, (int)(baseColor.getGreen() * 1.1)),
        Math.min(255, (int)(baseColor.getBlue() * 0.9))),
      (t - rustDensity) / (1 - rustDensity));
    }
  }
  
  private double simplexNoise3D(double x, double y, double z) {
    double value = Math.sin(x * 0.431) + Math.sin(y * 0.723) + Math.sin(z * 0.327);
    value += Math.sin(x * 1.531 + y * 0.927) * 0.5;
    value += Math.sin(y * 1.231 + z * 1.627) * 0.25;
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
