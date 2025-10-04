package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.light.*;
import net.elena.murat.math.*;

public class SuperBrightDebugMaterial implements Material {
  
  @Override
  public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPos) {
    return Color.WHITE; // Always WHITE
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
    return 0.0;
  }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
  }
  
}
