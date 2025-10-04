package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.math.*;
import net.elena.murat.light.Light;

/**
 * SolidColorMaterial represents a simple material that returns a solid color for any point,
 * without complex lighting calculations or patterns.
 * It now fully implements the extended Material interface.
 */
public class SolidColorMaterial implements Material {
  private final Color color;
  
  // Default values for new interface methods, as this is a simple solid color material
  private final double reflectivity = 0.0;
  private final double ior = 1.0; // Index of Refraction for air/vacuum
  private final double transparency;// = 0.0;
  
  /**
   * Constructs a SolidColorMaterial with a specified color.
   * @param color The solid color of the material.
   */
  public SolidColorMaterial(Color color) {
    this.color = color;
    
    int alfa = (this.color).getAlpha ();
    double alpha = ((double)(alfa))/255.0;
    
    this.transparency = (1.0-alpha);
  }
  
  /**
   * Calculates the color at a given point on the surface. For SolidColorMaterial,
   * it simply returns the predefined solid color, ignoring lighting and viewer position.
   * @param point The point in 3D space (world coordinates).
   * @param normal The normal vector at the point (world coordinates).
   * @param light The light source (ignored for solid color).
   * @param viewerPos The position of the viewer/camera (ignored for solid color).
   * @return The solid color of the material.
   */
  @Override
  public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPos) {
    return color; // SolidColor always returns the same color, no lighting effect
  }
  
  /**
   * Returns the reflectivity coefficient. For a solid color material, this is typically 0.0.
   * @return 0.0
   */
  @Override
  public double getReflectivity() {
    return reflectivity;
  }
  
  /**
   * Returns the index of refraction. For an opaque solid color material, this is typically 1.0.
   * @return 1.0
   */
  @Override
  public double getIndexOfRefraction() {
    return ior;
  }
  
  /**
   * Returns the transparency coefficient. For an opaque solid color material, this is typically 0.0.
   * @return 0.0
   */
  @Override
  public double getTransparency() {
    return transparency;
  }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
  }
  
}
