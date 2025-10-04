package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.light.Light;
import net.elena.murat.math.*;

/**
 * Neutral material that shows the original pixel colors without any lighting effects
 */
public class NeutralMaterial implements Material {
  private final Color baseColor;
  private final double reflectivity;
  private final double transparency;
  private final double indexOfRefraction;
  
  public NeutralMaterial(Color baseColor) {
    this(baseColor, 0.0, 0.0, 1.0);
  }
  
  public NeutralMaterial(Color baseColor, double reflectivity,
    double transparency, double indexOfRefraction) {
    this.baseColor = baseColor;
    this.reflectivity = reflectivity;
    this.transparency = transparency;
    this.indexOfRefraction = indexOfRefraction;
  }
  
  @Override
  public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPos) {
    // Return base color without any lighting calculations
    return baseColor;
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
  public double getTransparency() {
    return transparency;
  }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
  }
  
}
