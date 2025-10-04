package net.elena.murat.material;

import java.awt.Color;

//custom
import net.elena.murat.light.Light;
import net.elena.murat.math.*;

/**
 * Material interface defines the contract for all materials in the ray tracer.
 * It now includes methods for color calculation at a point,
 * as well as properties for reflectivity, index of refraction, and transparency.
 */
public interface Material {
  /**
   * Calculates the final color at a given point on the surface, considering
   * the material's properties and the light source.
   * @param point The point in 3D space (world coordinates) where the light hits.
   * @param normal The normal vector at the point (world coordinates).
   * @param light The single light source affecting this point.
   * @param viewerPos The position of the viewer/camera.
   * @return The color contribution from this specific light for the point.
   */
  Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPos);
  
  /**
   * Returns the reflectivity coefficient of the material.
   * This value determines how much light is reflected by the surface (0.0 for no reflection, 1.0 for full reflection).
   * @return The reflectivity value (0.0-1.0).
   */
  double getReflectivity();
  
  /**
   * Returns the index of refraction (IOR) of the material.
   * This value is used for calculating refraction (transparency).
   * @return The index of refraction (typically 1.0 for air, less than 1.0 for denser materials).
   */
  double getIndexOfRefraction();
  
  /**
   * Returns the transparency coefficient of the material.
   * This value determines how much light passes through the surface (0.0 for opaque, 1.0 for fully transparent).
   * @return The transparency value (0.0-1.0).
   */
  double getTransparency();
  
  void setObjectTransform(Matrix4 tm);
}
