package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.math.*;
import net.elena.murat.light.*;
import net.elena.murat.util.ColorUtil;

public class CrystalClearMaterial implements Material {
  private final Color glassTint;
  private final double clarity;
  private final double ior;
  private final double dispersion;
  
  private Matrix4 objectInverseTransform;
  
  private static final double BASE_REFLECTIVITY = 0.05;
  private final double reflectivity=BASE_REFLECTIVITY;
  private static final double FRESNEL_BIAS = 0.1;
  
  public CrystalClearMaterial(Color glassTint, double clarity,
    double ior, double dispersion,
    Matrix4 objectInverseTransform) {
    this.glassTint = glassTint;
    this.clarity = Math.max(0, Math.min(1, clarity));
    this.ior = Math.max(1.3, Math.min(2.0, ior));
    this.dispersion = Math.max(0, Math.min(0.1, dispersion));
    this.objectInverseTransform = objectInverseTransform;
  }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
    if (tm == null) tm = new Matrix4 ();
    this.objectInverseTransform = tm;
  }
  
  @Override
  public Color getColorAt(Point3 worldPoint, Vector3 worldNormal, Light light, Point3 viewerPos) {
    // 1. Light properties getting
    LightProperties lightProps = LightProperties.getLightProperties(light, worldPoint);
    if (lightProps == null) return glassTint != null ? glassTint : Color.WHITE;
    
    // 2. Fresnel
    Vector3 viewDir = viewerPos.subtract(worldPoint).normalize();
    double fresnel = calculateFresnel(viewDir, worldNormal);
    
    // 3. Colors
    Color refractedColor = calculateDispersion(viewDir, worldNormal, lightProps);
    
    // 4. Combine optic
    return combineOptics(
      glassTint != null ? glassTint : Color.WHITE,
      refractedColor,
      fresnel,
      worldNormal,
      lightProps
    );
  }
  
  private double calculateFresnel(Vector3 viewDir, Vector3 normal) {
    double cosTheta = Math.abs(viewDir.dot(normal));
    return FRESNEL_BIAS + (1-FRESNEL_BIAS) * Math.pow(1 - cosTheta, 5);
  }
  
  private Color calculateDispersion(Vector3 viewDir, Vector3 normal, LightProperties light) {
    if (dispersion <= 0) return light.color;
    
    Vector3 refractedR = refract(viewDir, normal, 1.0, ior + dispersion * 0.1);
    Vector3 refractedG = refract(viewDir, normal, 1.0, ior + dispersion * 0.05);
    Vector3 refractedB = refract(viewDir, normal, 1.0, ior);
    
    return new Color(
      Math.min(255, (int)(light.color.getRed() * 0.9)),
      Math.min(255, (int)(light.color.getGreen() * 0.95)),
      light.color.getBlue()
    );
  }
  
  private Vector3 refract(Vector3 incoming, Vector3 normal, double n1, double n2) {
    double n = n1 / n2;
    double cosI = -normal.dot(incoming);
    double sinT2 = n * n * (1.0 - cosI * cosI);
    
    if (sinT2 > 1.0) return null; // Total internal reflection
      
    double cosT = Math.sqrt(1.0 - sinT2);
    return incoming.multiply(n).add(normal.multiply(n * cosI - cosT));
  }
  
  private Color combineOptics(Color base, Color refracted, double fresnel,
    Vector3 normal, LightProperties light) {
    // Reflection component
    Color reflection = new Color(
      Math.min(255, (int)(255 * fresnel * BASE_REFLECTIVITY)),
      Math.min(255, (int)(255 * fresnel * BASE_REFLECTIVITY)),
      Math.min(255, (int)(255 * fresnel * BASE_REFLECTIVITY))
    );
    
    // Refraction component
    Color refraction = ColorUtil.blendColors(base, refracted, clarity);
    
    // Light interaction
    double NdotL = Math.max(0, normal.dot(light.direction));
    
    return new Color(
      Math.min(255, (int)(refraction.getRed() * light.intensity * NdotL + reflection.getRed())),
      Math.min(255, (int)(refraction.getGreen() * light.intensity * NdotL + reflection.getGreen())),
      Math.min(255, (int)(refraction.getBlue() * light.intensity * NdotL + reflection.getBlue()))
    );
  }
  
  @Override public double getReflectivity() { return reflectivity; }
  @Override public double getIndexOfRefraction() { return ior; }
  @Override public double getTransparency() { return clarity; }
  
}

/***
Material simpleGlass = new CrystalClearMaterial(
null,       // Colorless
0.95,       // 95% transparency
1.5,        // Standard glass IOR
0.0,        // No color dispersion
testSphere.getInverseTransform()
);

Material leadCrystal = new CrystalClearMaterial(
new Color(240, 240, 255), // Light blue tint
0.99,       // Highly transparent
1.7,        // High IOR (lead crystal)
0.02,       // Slight color dispersion
testSphere.getInverseTransform()
);

Material artGlass = new CrystalClearMaterial(
new Color(200, 230, 255, 150), // Blue-green tint
0.8,        // Frosted appearance
1.6,        // Dense glass
0.05,       // Noticeable color dispersion
testSphere.getInverseTransform()
);
 */

