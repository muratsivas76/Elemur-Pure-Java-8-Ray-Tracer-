package net.elena.murat.material;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import net.elena.murat.math.*;
import net.elena.murat.light.Light;
import net.elena.murat.util.ColorUtil;

public class InvertLightColorMaterial implements Material {
  
  @Override
  public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPoint) {
    Color originalColor = light.getColor();
    
    int invertedRed = 255 - originalColor.getRed();
    int invertedGreen = 255 - originalColor.getGreen();
    int invertedBlue = 255 - originalColor.getBlue();
    
    return new Color(invertedRed, invertedGreen, invertedBlue);
  }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
  }
  
  @Override
  public double getTransparency() {
    return 0.1;
  }
  
  @Override
  public double getReflectivity() {
    return 0.15;
  }
  
  @Override
  public double getIndexOfRefraction() {
    return 1.0;
  }
  
}
