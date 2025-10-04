package net.elena.murat.material;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import net.elena.murat.math.*;
import net.elena.murat.light.Light;

public class XRayMaterial implements Material {
  private Matrix4 objectTransform;
  private Matrix4 inverseObjectTransform;
  
  private final Color baseColor;
  private final double transparency;
  private final double reflectivity;
  
  private final float RF;
  private final float GF;
  private final float BF;
  private final float AF;
  
  public XRayMaterial() {
    this.baseColor = new Color (0.15f, 0.6f, 1.0f, 0.6f);
    this.transparency = 0.92;
    this.reflectivity = 0.05;
    
    this.RF = ((float)(this.baseColor.getRed ()))/(255.0f);
    this.GF = ((float)(this.baseColor.getGreen ()))/(255.0f);
    this.BF = ((float)(this.baseColor.getBlue ()))/(255.0f);
    this.AF = ((float)(this.baseColor.getAlpha ()))/(255.0f);
    
    this.objectTransform = Matrix4.identity();
    this.inverseObjectTransform = Matrix4.identity();
  }
  
  public XRayMaterial(Color baseColor, double transparency, double reflectivity) {
    this.baseColor = baseColor;
    this.transparency = transparency;
    this.reflectivity = reflectivity;
    
    this.RF = ((float)(this.baseColor.getRed ()))/(255.0f);
    this.GF = ((float)(this.baseColor.getGreen ()))/(255.0f);
    this.BF = ((float)(this.baseColor.getBlue ()))/(255.0f);
    this.AF = ((float)(this.baseColor.getAlpha ()))/(255.0f);
    
    this.objectTransform = Matrix4.identity();
    this.inverseObjectTransform = Matrix4.identity();
  }
  
  @Override
  public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPoint) {
    Point3 localPoint = inverseObjectTransform.transformPoint(point);
    
    double depth = calculateDepth(localPoint);
    float intensity = (float) Math.exp(-depth * 0.5);
    
    return new Color(
      RF,
      GF * intensity,
      BF * intensity,
      AF
    );
  }
  
  private double calculateDepth(Point3 point) {
    return point.length();
  }
  
  @Override
  public double getTransparency() {
    return transparency;
  }
  
  @Override
  public double getReflectivity() {
    return reflectivity;
  }
  
  @Override
  public double getIndexOfRefraction() {
    return 1.1;
  }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
    if (tm == null) tm = new Matrix4();
    this.objectTransform = tm;
    this.inverseObjectTransform = tm.inverse();
  }
  
}
