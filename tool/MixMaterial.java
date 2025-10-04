package net.elena.murat.material;

import net.elena.murat.math.*;
import net.elena.murat.light.Light;
import java.awt.Color;

public class MixMaterial implements Material {
  
  private Material material1;
  private Material material2;
  private double mixRatio;
  private Matrix4 objectTransform;
  private Matrix4 inverseObjectTransform;
  
  public MixMaterial(Material material1, Material material2, double mixRatio) {
    this.material1 = material1;
    this.material2 = material2;
    this.mixRatio = Math.max(0.0, Math.min(1.0, mixRatio));
    this.objectTransform = Matrix4.identity();
    this.inverseObjectTransform = Matrix4.identity();
  }
  
  @Override
  public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPoint) {
    // Her iki malzemenin rengini hesapla
    Color color1 = material1.getColorAt(point, normal, light, viewerPoint);
    Color color2 = material2.getColorAt(point, normal, light, viewerPoint);
    
    // Renkleri mix oranına göre karıştır
    return interpolateColors(color1, color2, mixRatio);
  }
  
  @Override
  public double getReflectivity() {
    // Her iki malzemenin reflectivity değerlerini karıştır
    return interpolate(material1.getReflectivity(), material2.getReflectivity(), mixRatio);
  }
  
  @Override
  public double getTransparency() {
    // Her iki malzemenin transparency değerlerini karıştır
    return interpolate(material1.getTransparency(), material2.getTransparency(), mixRatio);
  }
  
  @Override
  public double getIndexOfRefraction() {
    // Her iki malzemenin IOR değerlerini karıştır
    return interpolate(material1.getIndexOfRefraction(), material2.getIndexOfRefraction(), mixRatio);
  }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
    this.objectTransform = tm;
    this.inverseObjectTransform = tm.inverse();
    
    // Alt malzemelere transformu ilet
    material1.setObjectTransform(tm);
    material2.setObjectTransform(tm);
  }
  
  private Color interpolateColors(Color color1, Color color2, double ratio) {
    float[] comp1 = color1.getRGBColorComponents(null);
    float[] comp2 = color2.getRGBColorComponents(null);
    
    return new Color(
      (float) (comp1[0] * (1 - ratio) + comp2[0] * ratio),
      (float) (comp1[1] * (1 - ratio) + comp2[1] * ratio),
      (float) (comp1[2] * (1 - ratio) + comp2[2] * ratio)
    );
  }
  
  private double interpolate(double value1, double value2, double ratio) {
    return value1 * (1 - ratio) + value2 * ratio;
  }
  
  // Getter ve Setter metodları
  public Material getMaterial1() {
    return material1;
  }
  
  public void setMaterial1(Material material1) {
    this.material1 = material1;
    this.material1.setObjectTransform(objectTransform);
  }
  
  public Material getMaterial2() {
    return material2;
  }
  
  public void setMaterial2(Material material2) {
    this.material2 = material2;
    this.material2.setObjectTransform(objectTransform);
  }
  
  public double getMixRatio() {
    return mixRatio;
  }
  
  public void setMixRatio(double mixRatio) {
    this.mixRatio = Math.max(0.0, Math.min(1.0, mixRatio));
  }
}