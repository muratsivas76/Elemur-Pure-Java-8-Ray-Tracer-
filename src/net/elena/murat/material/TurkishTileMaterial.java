package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.math.*;
import net.elena.murat.light.*;
import net.elena.murat.util.ColorUtil;

public class TurkishTileMaterial implements Material {
  private final Color baseColor;
  private final Color patternColor;
  private final double tileSize;
  private Matrix4 objectTransform;
  
  // Phong parameters
  private final double ambientCoeff = 0.4;
  private final double diffuseCoeff = 0.6;
  private final double specularCoeff = 0.8;
  private final double shininess = 80.0;
  private final Color specularColor = Color.WHITE;
  private final double reflectivity = 0.3;
  private final double ior = 1.5;
  private final double transparency = 0.0;
  
  public TurkishTileMaterial() {
    this(new Color(0, 102, 204), new Color(255, 255, 255), 2.0);
  }
  
  public TurkishTileMaterial(Color baseColor, Color patternColor, double tileSize) {
    this.baseColor = baseColor;
    this.patternColor = patternColor;
    this.tileSize = Math.max(0.5, tileSize);
    this.objectTransform = Matrix4.identity();
  }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
    if (tm == null) tm = new Matrix4 ();
    this.objectTransform = tm;
  }
  
  @Override
  public Color getColorAt(Point3 worldPoint, Vector3 worldNormal, Light light, Point3 viewerPos) {
    Color surfaceColor = calculatePatternColor(worldPoint, worldNormal);
    
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
    Color specular = ColorUtil.multiplyColors(specularColor, props.color, specularCoeff * specFactor);
    
    return ColorUtil.combineColors(ambient, diffuse, specular);
  }
  
  private Color calculatePatternColor(Point3 worldPoint, Vector3 normal) {
    Point3 localPoint = objectTransform.inverse().transformPoint(worldPoint);
    
    // Simple UV mapping based on dominant normal
    double u, v;
    double absX = Math.abs(normal.x);
    double absY = Math.abs(normal.y);
    double absZ = Math.abs(normal.z);
    
    if (absX > absY && absX > absZ) {
      u = localPoint.z * tileSize;
      v = localPoint.y * tileSize;
      } else if (absY > absX && absY > absZ) {
      u = localPoint.x * tileSize;
      v = localPoint.z * tileSize;
      } else {
      u = localPoint.x * tileSize;
      v = localPoint.y * tileSize;
    }
    
    // Clean tile repetition
    double tileU = u - Math.floor(u);
    double tileV = v - Math.floor(v);
    
    return createCleanTurkishPattern(tileU, tileV);
  }
  
  private Color createCleanTurkishPattern(double u, double v) {
    // 1. CRISP BORDER (10% border)
    double border = 0.1;
    if (u < border || u > 1.0 - border || v < border || v > 1.0 - border) {
      return patternColor;
    }
    
    // 2. CLEAN CENTRAL MEDALLION
    double centerX = 0.5;
    double centerY = 0.5;
    double dist = Math.sqrt(Math.pow(u - centerX, 2) + Math.pow(v - centerY, 2));
    
    // Central circle
    if (dist < 0.15) {
      // Sharp 8-petal flower
      double angle = Math.atan2(v - centerY, u - centerX);
      double petal = Math.abs(Math.sin(4 * angle)); // 8 petals (sin(4θ) gives 8 peaks)
      
      if (petal > 0.9 && dist > 0.05) {
        return patternColor;
      }
      
      // Solid center
      if (dist < 0.05) {
        return patternColor;
      }
      
      return baseColor;
    }
    
    // 3. SHARP GEOMETRIC LINES (no anti-aliasing)
    // Diagonals
    if (Math.abs(u - v) < 0.02 || Math.abs(u + v - 1) < 0.02) {
      return patternColor;
    }
    
    // Vertical/horizontal lines
    if (Math.abs(u - 0.5) < 0.01 || Math.abs(v - 0.5) < 0.01) {
      return patternColor;
    }
    
    // 4. CORNER ELEMENTS (sharp and clean)
    double[] cornerDists = {
      Math.sqrt(u*u + v*v),
      Math.sqrt((1-u)*(1-u) + v*v),
      Math.sqrt(u*u + (1-v)*(1-v)),
      Math.sqrt((1-u)*(1-u) + (1-v)*(1-v))
    };
    
    for (double cornerDist : cornerDists) {
      if (cornerDist < 0.3) {
        double cornerAngle = Math.atan2(
          v - (cornerDist < 0.3 ? 0 : 1),
          u - (cornerDist < 0.3 ? 0 : 1)
        );
        double star = Math.abs(Math.sin(5 * cornerAngle));
        
        if (star > 0.95 && cornerDist > 0.15) {
          return patternColor;
        }
      }
    }
    
    // 5. BASE COLOR (no texture noise)
    return baseColor;
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
