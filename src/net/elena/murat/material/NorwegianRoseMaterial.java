package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.math.*;
import net.elena.murat.light.*;
import net.elena.murat.util.ColorUtil;

public class NorwegianRoseMaterial implements Material {
  private final Color woodColor;
  private final Color roseColor;
  private Matrix4 objectTransform;
  private Matrix4 inverseTransform;
  private Matrix4 inverseTransposeTransform;
  
  // Phong parameters optimized for painted wood surface
  private final double ambientCoeff = 0.4;
  private final double diffuseCoeff = 0.7;
  private final double specularCoeff = 0.2;
  private final double shininess = 30.0;
  private final Color specularColor = new Color(220, 220, 220);
  private final double reflectivity = 0.1;
  private final double ior = 1.4;
  private final double transparency = 0.0;
  
  private static final double TWO_PI = Math.PI * 2;
  private static final double THREE_PI = Math.PI * 3;
  private static final double EIGHT = 8.0;
  
  public NorwegianRoseMaterial() {
    this(new Color(101, 67, 33), new Color(200, 50, 50));
  }
  
  public NorwegianRoseMaterial(Color woodColor, Color roseColor) {
    this.woodColor = woodColor;
    this.roseColor = roseColor;
    this.objectTransform = Matrix4.identity();
    this.inverseTransform = Matrix4.identity();
    this.inverseTransposeTransform = Matrix4.identity();
  }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
    if (tm == null) tm = new Matrix4 ();
    this.objectTransform = tm;
    this.inverseTransform = tm.inverse();
    this.inverseTransposeTransform = tm.inverseTransposeForNormal();
  }
  
  @Override
  public Color getColorAt(Point3 worldPoint, Vector3 worldNormal, Light light, Point3 viewerPos) {
    // 1. Get rosemal pattern color with wood grain
    Color surfaceColor = calculateRosemalColor(worldPoint, worldNormal);
    
    // 2. Handle light properties
    LightProperties props = LightProperties.getLightProperties(light, worldPoint);
    if (props == null) return surfaceColor;
    
    // 3. Calculate Phong components
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
  
  private Color calculateRosemalColor(Point3 worldPoint, Vector3 normal) {
    // Transform to object space for pattern calculation
    Point3 localPoint = inverseTransform.transformPoint(worldPoint);
    Vector3 localNormal = inverseTransposeTransform.transformVector(normal).normalize();
    
    // Calculate UV coordinates based on dominant normal axis
    double u, v;
    double absNx = Math.abs(localNormal.x);
    double absNy = Math.abs(localNormal.y);
    double absNz = Math.abs(localNormal.z);
    
    if (absNx > absNy && absNx > absNz) {
      u = localPoint.y * EIGHT;
      v = localPoint.z * EIGHT;
      } else if (absNy > absNx && absNy > absNz) {
      u = localPoint.x * EIGHT;
      v = localPoint.z * EIGHT;
      } else {
      u = localPoint.x * EIGHT;
      v = localPoint.y * EIGHT;
    }
    
    // Add wood grain texture to base
    Color woodBase = addWoodGrain(woodColor, u, v);
    
    // Apply rosemål pattern on top
    return createRosemalPattern(woodBase, u, v);
  }
  
  private Color addWoodGrain(Color baseWood, double u, double v) {
    // Simulate wood grain effect
    double grain = Math.sin(u * 3.0) * 0.15 + Math.sin(v * 1.5) * 0.1;
    
    int r = (int)(baseWood.getRed() * (0.85 + grain));
    int g = (int)(baseWood.getGreen() * (0.85 + grain));
    int b = (int)(baseWood.getBlue() * (0.85 + grain));
    
    r = r > 255 ? 255 : (r < 0 ? 0 : r);
    g = g > 255 ? 255 : (g < 0 ? 0 : g);
    b = b > 255 ? 255 : (b < 0 ? 0 : b);
    
    return new Color(r, g, b);
  }
  
  private Color createRosemalPattern(Color woodBase, double u, double v) {
    double tileU = (u % 1 + 1) % 1;
    double tileV = (v % 1 + 1) % 1;
    
    // Border pattern
    if (tileU < 0.1 || tileU > 0.9 || tileV < 0.1 || tileV > 0.9) {
      return roseColor;
    }
    
    // Central flower/medallion pattern
    double centerX = 0.5;
    double centerY = 0.5;
    double dx = tileU - centerX;
    double dy = tileV - centerY;
    double distToCenterSq = dx * dx + dy * dy;
    
    if (distToCenterSq < 0.04) { // 0.2^2 = 0.04
      // Flower pattern with petals
      double angle = Math.atan2(dy, dx);
      double petal = Math.sin(angle * 6) * 0.5 + 0.5;
      if (petal > 0.7 && distToCenterSq > 0.01) { // 0.1^2 = 0.01
        return roseColor;
      }
    }
    
    // Scrollwork patterns
    double diffUV = Math.abs(tileU - tileV);
    double sumUV = Math.abs(tileU + tileV - 1);
    
    if (diffUV < 0.03 ||
      sumUV < 0.03 ||
      Math.sin(tileU * THREE_PI) > 0.8 ||
      Math.sin(tileV * THREE_PI) > 0.8 ||
      Math.cos(tileU * TWO_PI) > 0.7 ||
      Math.cos(tileV * TWO_PI) > 0.7) {
      return roseColor;
    }
    
    // Additional decorative elements
    double tileU25 = Math.abs(tileU - 0.25);
    double tileU75 = Math.abs(tileU - 0.75);
    double tileV25 = Math.abs(tileV - 0.25);
    double tileV75 = Math.abs(tileV - 0.75);
    
    if ((tileU25 < 0.04 && tileV25 < 0.04) ||
      (tileU75 < 0.04 && tileV75 < 0.04) ||
      (tileU25 < 0.04 && tileV75 < 0.04) ||
      (tileU75 < 0.04 && tileV25 < 0.04)) {
      return roseColor;
    }
    
    return woodBase;
  }
  
  @Override public double getReflectivity() { return reflectivity; }
  @Override public double getIndexOfRefraction() { return ior; }
  @Override public double getTransparency() { return transparency; }
  
}
