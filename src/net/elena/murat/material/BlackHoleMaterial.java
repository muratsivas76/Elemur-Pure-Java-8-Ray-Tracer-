package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.math.*;
import net.elena.murat.light.*;

public class BlackHoleMaterial implements Material {
  private final Point3 singularity;
  private Matrix4 objectInverseTransform;
  private final double reflectivity=0.95;
  
  public BlackHoleMaterial(Matrix4 objectInverseTransform) {
    this(new Point3(0,0,0), objectInverseTransform);
  }
  
  public BlackHoleMaterial(Point3 singularity, Matrix4 objectInverseTransform) {
    this.singularity = singularity;
    this.objectInverseTransform = objectInverseTransform;
  }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
    if (tm == null) tm = new Matrix4 ();
    this.objectInverseTransform = tm;
  }
  
  @Override
  public Color getColorAt(Point3 worldPoint, Vector3 worldNormal, Light light, Point3 viewPos) {
    // Light informations
    Color lightColor = light.getColor();
    double intensity = light.getIntensityAt(worldPoint);
    
    // Distorsiyon
    Point3 localPoint = objectInverseTransform.transformPoint(worldPoint);
    double dist = localPoint.distance(singularity);
    if(dist < 0.5) return Color.BLACK;
    
    // Redding according to light color
    double warp = Math.min(1, 0.3/(dist*dist));
    int r = (int)(lightColor.getRed() * warp);
    int g = (int)(lightColor.getGreen() * warp * 0.3);
    int b = (int)(lightColor.getBlue() * warp * 0.1);
    
    return new Color(
      Math.min(255, r),
      Math.min(255, g),
      Math.min(255, b)
    );
  }
  
  @Override public double getReflectivity() { return reflectivity; }
  @Override public double getIndexOfRefraction() { return 2.5; }
  @Override public double getTransparency() { return 0.1; }
  
}
