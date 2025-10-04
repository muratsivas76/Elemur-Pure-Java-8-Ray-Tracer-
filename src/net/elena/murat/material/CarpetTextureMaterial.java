// CarpetTextureMaterial.java
package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.math.*;
import net.elena.murat.light.*;
import net.elena.murat.util.ColorUtil;

public class CarpetTextureMaterial implements Material {
  private final Color baseColor;
  private final Color patternColor;
  private Matrix4 objectTransform;
  
  // Phong parameters optimized for carpet/wool surface
  private final double ambientCoeff = 0.5; // Higher ambient for fabric
  private final double diffuseCoeff = 0.7;
  private final double specularCoeff = 0.1; // Wool has very weak specular
  private final double shininess = 10.0; // Very rough surface
  private final Color specularColor = new Color(200, 200, 200); // Soft specular
  private final double reflectivity = 0.05;
  private final double ior = 1.2;
  private final double transparency = 0.0;
  
  public CarpetTextureMaterial() {
    this(new Color(170, 0, 0), new Color(40, 40, 40)); // Turkish red with dark pattern
  }
  
  public CarpetTextureMaterial(Color baseColor, Color patternColor) {
    this.baseColor = baseColor;
    this.patternColor = patternColor;
    this.objectTransform = Matrix4.identity();
  }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
    if (tm == null) tm = new Matrix4 ();
    this.objectTransform = tm;
  }
  
  @Override
  public Color getColorAt(Point3 worldPoint, Vector3 worldNormal, Light light, Point3 viewerPos) {
    // 1. Get carpet pattern color with pile effect
    Color surfaceColor = calculateCarpetColor(worldPoint, worldNormal);
    
    // 2. Handle light properties (EXACTLY like Checkerboard)
    LightProperties props = LightProperties.getLightProperties(light, worldPoint);
    if (props == null) return surfaceColor;
    
    // 3. Simulate carpet pile effect on normal
    Vector3 piledNormal = simulatePileEffect(worldPoint, worldNormal);
    
    // 4. Calculate Phong components with piled normal
    Color ambient = ColorUtil.multiplyColors(surfaceColor, props.color, ambientCoeff);
    
    if (light instanceof ElenaMuratAmbientLight) {
      return ambient;
    }
    
    double NdotL = Math.max(0, piledNormal.dot(props.direction));
    Color diffuse = ColorUtil.multiplyColors(surfaceColor, props.color, diffuseCoeff * NdotL * props.intensity);
    
    Vector3 viewDir = viewerPos.subtract(worldPoint).normalize();
    Vector3 reflectDir = props.direction.negate().reflect(piledNormal);
    double RdotV = Math.max(0, reflectDir.dot(viewDir));
    double specFactor = Math.pow(RdotV, shininess) * props.intensity;
    Color specular = ColorUtil.multiplyColors(specularColor, props.color, specularCoeff * specFactor);
    
    return ColorUtil.combineColors(ambient, diffuse, specular);
  }
  
  private Color calculateCarpetColor(Point3 worldPoint, Vector3 normal) {
    // Transform to object space for pattern calculation
    Point3 localPoint = objectTransform.inverse().transformPoint(worldPoint);
    
    // Calculate UV coordinates
    double u = localPoint.x * 6.0;
    double v = localPoint.z * 6.0;
    
    // Create traditional kilim pattern
    return createKilimPattern(u, v);
  }
  
  private Color createKilimPattern(double u, double v) {
    double tileU = (u % 1 + 1) % 1;
    double tileV = (v % 1 + 1) % 1;
    
    // Border pattern
    if (tileU < 0.08 || tileU > 0.92 || tileV < 0.08 || tileV > 0.92) {
      return patternColor;
    }
    
    // Diamond pattern in center
    if (Math.abs(tileU - 0.5) + Math.abs(tileV - 0.5) < 0.25) {
      return patternColor;
    }
    
    // Additional geometric elements
    if ((Math.abs(tileU - 0.25) < 0.06 && Math.abs(tileV - 0.25) < 0.06) ||
      (Math.abs(tileU - 0.75) < 0.06 && Math.abs(tileV - 0.75) < 0.06) ||
      (Math.abs(tileU - 0.25) < 0.06 && Math.abs(tileV - 0.75) < 0.06) ||
      (Math.abs(tileU - 0.75) < 0.06 && Math.abs(tileV - 0.25) < 0.06)) {
      return patternColor;
    }
    
    // Vertical and horizontal lines
    if (Math.abs(tileU - 0.5) < 0.02 || Math.abs(tileV - 0.5) < 0.02) {
      return patternColor;
    }
    
    return baseColor;
  }
  
  private Vector3 simulatePileEffect(Point3 point, Vector3 originalNormal) {
    // Simulate carpet pile using noise functions
    double pileU = point.x * 8.0;
    double pileV = point.z * 8.0;
    
    // Create gentle waves in the pile
    double pileX = Math.sin(pileU * 2.0) * 0.1;
    double pileY = 1.0; // Main pile direction (up)
    double pileZ = Math.cos(pileV * 2.0) * 0.1;
    
    Vector3 pileEffect = new Vector3(pileX, pileY, pileZ).normalize();
    return originalNormal.add(pileEffect).normalize();
  }
  
  @Override public double getReflectivity() { return reflectivity; }
  @Override public double getIndexOfRefraction() { return ior; }
  @Override public double getTransparency() { return transparency; }
  
}
