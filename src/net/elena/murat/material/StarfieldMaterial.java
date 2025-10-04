package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.math.*;
import net.elena.murat.light.*;
import net.elena.murat.util.ColorUtil;

public class StarfieldMaterial implements Material {
  private Matrix4 objectInverseTransform;
  private final Color nebulaColor;
  private final double starSize; // Range 0.001-0.1
  private final double starDensity;
  private final double twinkleSpeed;
  private final double reflectivity = 0.02;
  
  // Star color palette
  private static final Color[] STAR_COLORS = {
    new Color(255, 255, 255), // White
    new Color(200, 200, 255), // Blue
    new Color(255, 200, 150), // Yellowish
    new Color(200, 255, 200)  // Greenish
  };
  
  public StarfieldMaterial(Matrix4 objectInverseTransform) {
    this(objectInverseTransform,
      new Color(10, 5, 40), // Nebula color
      0.015,   // Star size
      0.003,   // Star density
      1.0      // Twinkle speed
    );
  }
  
  public StarfieldMaterial(Matrix4 objectInverseTransform,
    Color nebulaColor,
    double starSize,
    double starDensity,
    double twinkleSpeed) {
    this.objectInverseTransform = objectInverseTransform;
    this.nebulaColor = nebulaColor;
    this.starSize = Math.min(0.1, Math.max(0.001, starSize));
    this.starDensity = Math.min(0.01, Math.max(0.0001, starDensity));
    this.twinkleSpeed = Math.max(0.1, twinkleSpeed);
  }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
    if (tm == null) tm = new Matrix4 ();
    this.objectInverseTransform = tm;
  }
  
  @Override
  public Color getColorAt(Point3 worldPoint, Vector3 normal, Light light, Point3 viewPos) {
    Point3 localPoint = objectInverseTransform.transformPoint(worldPoint);
    double dist = localPoint.distance(Point3.ORIGIN);
    
    // Star generation
    double starValue = calculateStarValue(localPoint, dist);
    
    if(starValue > 0) {
      // Star color and brightness
      Color starColor = getStarColor(localPoint);
      float brightness = (float)(starValue * (0.7 + 0.3*Math.sin(System.currentTimeMillis()*0.001*twinkleSpeed)));
      
      // Light effect
      double lightEffect = calculateLightEffect(light, worldPoint, normal);
      return ColorUtil.blendColors(starColor, light.getColor(), brightness * lightEffect);
    }
    
    // Nebula color (illuminated by light)
    return applyNebulaLight(nebulaColor, light, dist);
  }
  
  private double calculateStarValue(Point3 p, double dist) {
    // Make star positions deterministic
    long seed = (long)(p.x*1000) ^ (long)(p.y*1000) ^ (long)(p.z*1000);
    double random = (seed * 9301 + 49297) % 233280 / (double)233280;
    
    // Scale by distance
    double scaledDensity = starDensity * (1.0 + dist * 0.5);
    
    if(random < scaledDensity) {
      // Calculate star size
      double sizeFactor = 1.0 + (seed % 1000)/1000.0; // Range 1.0-2.0
      return starSize * sizeFactor;
    }
    return 0;
  }
  
  private Color getStarColor(Point3 p) {
    // Deterministic color selection
    int hash = (int)(p.x*100 + p.y*100 + p.z*100) % STAR_COLORS.length;
    return STAR_COLORS[Math.abs(hash)];
  }
  
  private double calculateLightEffect(Light light, Point3 worldPoint, Vector3 normal) {
    // Light direction and intensity
    Vector3 lightDir = light.getDirectionAt(worldPoint).normalize();
    double intensity = light.getIntensityAt(worldPoint);
    double NdotL = Math.max(0.1, normal.dot(lightDir));
    return NdotL * intensity;
  }
  
  private Color applyNebulaLight(Color nebula, Light light, double dist) {
    // Illuminate nebula with light
    double lightFactor = 0.2 + 0.1 * Math.sin(dist * 0.5) * light.getIntensity();
    int red = Math.min(255, ((int)(nebula.getRed() * lightFactor)));
    int green = Math.min(255, ((int)(nebula.getGreen() * lightFactor)));
    int blue = Math.min(255, ((int)(nebula.getBlue() * lightFactor)));
    
    return new Color(red, green, blue);
  }
  
  @Override public double getReflectivity() { return reflectivity; }
  @Override public double getIndexOfRefraction() { return 1.0; }
  @Override public double getTransparency() { return 0.95; }
}

/***
Material starfield = new StarfieldMaterial(
sphere.getInverseTransform(),
new Color(15, 10, 50), // Nebula color
0.05,  // Very large stars
0.002, // More sparse
0.8    // Slow twinkle
);
 */
