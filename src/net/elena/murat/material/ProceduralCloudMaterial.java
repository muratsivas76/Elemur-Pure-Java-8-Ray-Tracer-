package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.light.*;
import net.elena.murat.math.*;
import net.elena.murat.util.ColorUtil;

public class ProceduralCloudMaterial implements Material {
  private final Color baseColor;
  private final Color highlightColor;
  
  public ProceduralCloudMaterial(Color baseColor, Color highlightColor) {
    this.baseColor = baseColor;
    this.highlightColor = highlightColor;
  }
  
  @Override
  public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPos) {
    double value = Math.sin(point.x * 5.0) + Math.cos(point.y * 7.0);
    value = (value + 2.0) / 4.0; // normalize 0..1
    
    int r = (int)(baseColor.getRed() * (1 - value) + highlightColor.getRed() * value);
    int g = (int)(baseColor.getGreen() * (1 - value) + highlightColor.getGreen() * value);
    int b = (int)(baseColor.getBlue() * (1 - value) + highlightColor.getBlue() * value);
    
    r = ColorUtil.clampColorValue (r);
    g = ColorUtil.clampColorValue (g);
    b = ColorUtil.clampColorValue (b);
    
    return new Color(r, g, b);
  }
  
  @Override
  public double getReflectivity() {
    return 0.0;
  }
  
  @Override
  public double getIndexOfRefraction() {
    return 1.0;
  }
  
  @Override
  public double getTransparency() {
    return 0.0; //opaque
  }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
  }
  
}
