package net.elena.murat.material;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import net.elena.murat.math.*;
import net.elena.murat.light.Light;

public class AmberMaterial implements Material {
  private Color baseColor;
  private double transparency;
  private double reflectivity;
  private double indexOfRefraction;
  private Matrix4 objectTransform;
  private Matrix4 inverseObjectTransform;
  
  public AmberMaterial() {
    this.baseColor = new Color(255, 176, 56);
    this.transparency = 0.5;
    this.reflectivity = 0.25;
    this.indexOfRefraction = 1.52;
    this.objectTransform = Matrix4.identity();
    this.inverseObjectTransform = Matrix4.identity();
  }
  
  @Override
  public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPoint) {
    Point3 localPoint = inverseObjectTransform.transformPoint(point);
    Vector3 localNormal = inverseObjectTransform.inverseTransposeForNormal().transformVector(normal).normalize();
    
    Vector3 lightDir = light.getDirectionTo(point).normalize();
    double diffuse = Math.max(0, localNormal.dot(lightDir));
    
    Vector3 viewDir = viewerPoint.subtract(localPoint).normalize();
    double rim = Math.pow(1.0 - Math.max(0, localNormal.dot(viewDir)), 2.0);
    
    double ambient = 0.1;
    double intensity = light.getIntensity();
    double totalLight = ambient + intensity * (0.7 * diffuse + 0.3 * rim);
    
    int r = (int) (baseColor.getRed() * totalLight);
    int g = (int) (baseColor.getGreen() * totalLight);
    int b = (int) (baseColor.getBlue() * totalLight);
    
    return new Color(
      Math.min(255, Math.max(0, r)),
      Math.min(255, Math.max(0, g)),
      Math.min(255, Math.max(0, b))
    );
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
    return indexOfRefraction;
  }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
    if (tm == null) {
      this.objectTransform = Matrix4.identity();
      } else {
      this.objectTransform = tm;
    }
    this.inverseObjectTransform = this.objectTransform.inverse();
  }
  
}
