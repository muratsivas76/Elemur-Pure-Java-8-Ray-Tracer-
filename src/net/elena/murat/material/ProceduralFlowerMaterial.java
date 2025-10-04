package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.math.*;
import net.elena.murat.light.*;

public class ProceduralFlowerMaterial implements Material {
  private final double petalCount;
  private final Color petalColor;
  private final Color centerColor;
  private final double ambientStrength;
  private final double reflectivity = 0.1;
  
  public ProceduralFlowerMaterial(double petalCount, Color petalColor, Color centerColor) {
    this(petalCount, petalColor, centerColor, 0.2);
  }
  
  public ProceduralFlowerMaterial(double petalCount, Color petalColor,
    Color centerColor, double ambientStrength) {
    this.petalCount = petalCount;
    this.petalColor = petalColor;
    this.centerColor = centerColor;
    this.ambientStrength = Math.max(0, Math.min(1, ambientStrength));
  }
  
  @Override
  public Color getColorAt(Point3 worldPoint, Vector3 normal, Light light, Point3 viewPos) {
    // Light information
    Color lightColor = light.getColor();
    double intensity = light.getIntensityAt(worldPoint);
    Vector3 lightDir = light.getDirectionAt(worldPoint).normalize();
    
    // Diffuse shading
    double NdotL = Math.max(0, normal.dot(lightDir));
    double diffuseFactor = NdotL * intensity;
    
    // Polar coordinates
    double u = worldPoint.x / 2.0;
    double v = worldPoint.z / 2.0;
    double r = Math.sqrt(u*u + v*v);
    double theta = Math.atan2(v, u);
    double flowerR = 0.3 * Math.cos(petalCount * theta);
    
    // Color selection + light effect
    Color baseColor;
    if (r < 0.15) {
      baseColor = centerColor; // Center
    }
    else if (Math.abs(r - flowerR) < 0.05) {
      baseColor = petalColor; // Petals
    }
    else if (r < 0.6 && Math.random() < 0.3) {
      baseColor = new Color(0, 100 + (int)(155 * Math.random()), 0); // Green leaves
    }
    else {
      baseColor = Color.WHITE; // Background
    }
    
    // Light color blending
    return applyLight(baseColor, lightColor, diffuseFactor);
  }
  
  private Color applyLight(Color baseColor, Color lightColor, double diffuseFactor) {
    // Ambient component
    int ambientR = (int)(baseColor.getRed() * ambientStrength);
    int ambientG = (int)(baseColor.getGreen() * ambientStrength);
    int ambientB = (int)(baseColor.getBlue() * ambientStrength);
    
    // Diffuse + Specular (multiplied by light color)
    int r = (int)(baseColor.getRed() * (lightColor.getRed()/255.0) * diffuseFactor);
    int g = (int)(baseColor.getGreen() * (lightColor.getGreen()/255.0) * diffuseFactor);
    int b = (int)(baseColor.getBlue() * (lightColor.getBlue()/255.0) * diffuseFactor);
    
    // Final color (ambient + diffuse)
    r = Math.min(255, ambientR + r);
    g = Math.min(255, ambientG + g);
    b = Math.min(255, ambientB + b);
    
    return new Color(r, g, b);
  }
  
  @Override public double getReflectivity() { return reflectivity; }
  @Override public double getIndexOfRefraction() { return 1.0; }
  @Override public double getTransparency() { return 0.0; }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
  }
  
}

/***
// Adding to scene
Plane flowerPlane = new Plane(new Point3(0,0,0), new Vector3(0,1,0));
Material flowerMat = new ProceduralFlowerMaterial(
5,            // 5 petals
Color.RED,    // Petal color
Color.YELLOW  // Center color
);
flowerPlane.setMaterial(flowerMat);

// Camera setup
rayTracer.setCameraPosition(new Point3(0, 2, 3));
 */
