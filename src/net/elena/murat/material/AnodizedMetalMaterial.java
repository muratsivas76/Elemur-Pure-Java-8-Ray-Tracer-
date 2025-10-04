// AnodizedMetalMaterial.java - CHECKERBOARD STYLE
package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.math.*;
import net.elena.murat.light.*;
import net.elena.murat.util.ColorUtil;

public class AnodizedMetalMaterial implements Material {
  private final Color baseColor;
  private Matrix4 objectTransform;
  
  // Phong parameters optimized for metallic surface
  private final double ambientCoeff = 0.3;
  private final double diffuseCoeff = 0.2;  // Metallic surfaces have weak diffuse
  private final double specularCoeff = 1.0; // Strong specular for metallic
  private final double shininess = 100.0;
  private final Color specularColor = Color.WHITE;
  private final double reflectivity = 0.8;
  private final double ior = 2.4;
  private final double transparency = 0.0;
  
  public AnodizedMetalMaterial() {
    this(new Color(50, 50, 200));
  }
  
  public AnodizedMetalMaterial(Color baseColor) {
    this.baseColor = baseColor;
    this.objectTransform = Matrix4.identity();
  }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
    if (tm == null) tm = new Matrix4 ();
    this.objectTransform = tm;
  }
  
  @Override
  public Color getColorAt(Point3 worldPoint, Vector3 worldNormal, Light light, Point3 viewerPos) {
    // 1. Get base color with iridescence effect
    Color surfaceColor = calculateIridescentColor(worldPoint, worldNormal, viewerPos);
    
    // 2. Handle light properties (EXACTLY like Checkerboard)
    LightProperties props = LightProperties.getLightProperties(light, worldPoint);
    if (props == null) return surfaceColor;
    
    // 3. Calculate Phong components (EXACTLY like Checkerboard)
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
    Color specular = ColorUtil.multiplyColors(specularColor, props.color, specularCoeff * specFactor);
    
    return ColorUtil.combineColors(ambient, diffuse, specular);
  }
  
  private Color calculateIridescentColor(Point3 worldPoint, Vector3 normal, Point3 viewerPos) {
    // Calculate view angle for iridescence effect
    Vector3 viewDir = viewerPos.subtract(worldPoint).normalize();
    double viewAngle = Math.abs(viewDir.dot(normal));
    
    // Simple iridescence effect based on view angle
    double hueShift = viewAngle * 180.0;
    
    int r = baseColor.getRed();
    int g = baseColor.getGreen();
    int b = baseColor.getBlue();
    
    // Blue -> Purple -> Pink transition (anodized aluminum effect)
    if (viewAngle < 0.3) {
      // Narrow angle - blue tones
      r = (int)(r * 0.7);
      g = (int)(g * 0.8);
      b = (int)(b * 1.2);
      } else if (viewAngle < 0.6) {
      // Medium angle - purple tones
      r = (int)(r * 1.1);
      g = (int)(g * 0.7);
      b = (int)(b * 1.0);
      } else {
      // Wide angle - pink/gold tones
      r = (int)(r * 1.3);
      g = (int)(g * 0.9);
      b = (int)(b * 0.8);
    }
    
    return new Color(
      Math.min(255, Math.max(0, r)),
      Math.min(255, Math.max(0, g)),
      Math.min(255, Math.max(0, b))
    );
  }
  
  @Override public double getReflectivity() { return reflectivity; }
  @Override public double getIndexOfRefraction() { return ior; }
  @Override public double getTransparency() { return transparency; }
  
}
