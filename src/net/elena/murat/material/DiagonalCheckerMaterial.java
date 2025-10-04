package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.light.*;
import net.elena.murat.math.*;

/**
 * DiagonalCheckerMaterial represents a surface with a two-color diagonal checkerboard pattern.
 * It fully implements the Material interface, including properties for reflectivity,
 * index of refraction, and transparency.
 */
public class DiagonalCheckerMaterial implements Material {
  private final Color color1; // First color for the checkerboard pattern
  private final Color color2; // Second color for the checkerboard pattern
  private final double scale; // Represents the frequency of squares (squares per unit)
  private Matrix4 objectInverseTransform; // The inverse transformation matrix of the object
  
  // Phong lighting model parameters
  private final double ambient; // Ambient reflection coefficient (0.0-1.0)
  private final double diffuse; // Diffuse reflection coefficient (0.0-1.0)
  private final double specular; // Specular reflection coefficient (0.0-1.0)
  private final double shininess; // Shininess exponent for specular highlights
  private final Color specularColor; // Color of the specular highlight
  private final double reflectivity; // Reflectivity coefficient (0.0-1.0)
  private final double ior; // Index of Refraction for transparent materials
  private final double transparency; // Transparency coefficient (0.0-1.0)
  
  /**
   * Constructs a DiagonalCheckerMaterial with two colors, a scale, Phong lighting model parameters,
   * and the object's inverse transformation matrix.
   *
   * @param color1              The first color for the checkerboard pattern.
   * @param color2              The second color for the checkerboard pattern.
   * @param scale               The frequency of the squares (e.g., 4.0 for 4 squares per unit length).
   * @param ambient             The ambient reflection coefficient (0.0-1.0).
   * @param diffuse             The diffuse reflection coefficient (0.0-1.0).
   * @param specular            The specular reflection coefficient (0.0-1.0).
   * @param shininess           The shininess exponent for specular highlights.
   * @param specularColor       The color of the specular highlight.
   * @param reflectivity        The reflectivity coefficient (0.0-1.0).
   * @param ior                 The Index of Refraction for transparent materials.
   * @param transparency        The transparency coefficient (0.0-1.0).
   * @param objectInverseTransform The full inverse transformation matrix of the object this material is applied to.
   */
  public DiagonalCheckerMaterial(Color color1, Color color2, double scale,
    double ambient, double diffuse, double specular,
    double shininess, Color specularColor,
    double reflectivity, double ior, double transparency,
    Matrix4 objectInverseTransform) {
    this.color1 = color1;
    this.color2 = color2;
    this.scale = scale;
    this.objectInverseTransform = objectInverseTransform;
    this.ambient = ambient;
    this.diffuse = diffuse;
    this.specular = specular;
    this.shininess = shininess;
    this.specularColor = specularColor;
    this.reflectivity = reflectivity;
    this.ior = ior;
    this.transparency = transparency;
  }
  
  /**
   * Simplified constructor for basic diagonal checkerboard pattern without full Phong parameters.
   * Uses default Phong values.
   *
   * @param color1              The first color for the checkerboard pattern.
   * @param color2              The second color for the checkerboard pattern.
   * @param scale               The frequency of the squares.
   * @param objectInverseTransform The full inverse transformation matrix of the object.
   */
  public DiagonalCheckerMaterial(Color color1, Color color2, double scale,
    Matrix4 objectInverseTransform) {
    this(color1, color2, scale,
      0.1, 0.7, 0.8, 50.0, Color.WHITE,
    0.0, 1.0, 0.0, objectInverseTransform);
  }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
    if (tm == null) tm = new Matrix4 ();
    this.objectInverseTransform = tm;
  }
  
  /**
   * Calculates the color contribution of a single light source at a given point on the surface,
   * taking into account surface properties and a diagonal checkerboard pattern using the Phong model.
   * The hit point is transformed into the material's local space before pattern calculation.
   *
   * @param worldPoint          The point in 3D space (world coordinates) where the light hits.
   * @param worldNormal         The normal vector at the point (world coordinates).
   * @param light               The single light source affecting this point.
   * @param viewerPos           The position of the viewer/camera.
   * @return                    The color contribution from this specific light for the point.
   */
  @Override
  public Color getColorAt(Point3 worldPoint, Vector3 worldNormal,
    Light light, Point3 viewerPos) {
    // Check if inverse transform is valid before proceeding
    if (objectInverseTransform == null) {
      System.err.println("Error: DiagonalCheckerMaterial's inverse transform is null. Returning black.");
      return Color.BLACK;
    }
    
    // 1. Transform point to object's local space
    Point3 localPoint = objectInverseTransform.transformPoint(worldPoint);
    
    // 2. Transform the world normal to local space to determine the local face orientation
    // Normals transform with the inverse transpose of the model matrix.
    Vector3 localNormal = objectInverseTransform.inverseTransposeForNormal().transformVector(worldNormal).normalize();
    
    // Check if the transformed normal is valid
    if (localNormal == null) {
      System.err.println("Error: DiagonalCheckerMaterial's normal transform matrix is null or invalid. Returning black.");
      return Color.BLACK;
    }
    
    // Determine the dominant axis of the *local normal* to decide 2D projection for pattern.
    // This ensures the square pattern aligns correctly with the object's local faces.
    double absNx = Math.abs(localNormal.x);
    double absNy = Math.abs(localNormal.y);
    double absNz = Math.abs(localNormal.z);
    
    // Project the 3D local point onto a 2D plane based on the dominant local normal axis.
    // Normalize coordinates from [-0.5, 0.5] to [0, 1] for a unit cube local space.
    // Then scale by 'this.scale' which represents squares per unit length.
    // Add a small epsilon to the coordinates before flooring to handle floating point inaccuracies at boundaries.
    double u, v; // 2D texture coordinates
    if (absNx > absNy && absNx > absNz) { // Normal is mostly X-axis (local Y-Z plane)
      u = (localPoint.y + 0.5 + Ray.EPSILON) * this.scale;
      v = (localPoint.z + 0.5 + Ray.EPSILON) * this.scale;
      } else if (absNy > absNx && absNy > absNz) { // Normal is mostly Y-axis (local X-Z plane)
      u = (localPoint.x + 0.5 + Ray.EPSILON) * this.scale;
      v = (localPoint.z + 0.5 + Ray.EPSILON) * this.scale;
      } else { // Normal is mostly Z-axis (local X-Y plane) or equally dominant
      u = (localPoint.x + 0.5 + Ray.EPSILON) * this.scale;
      v = (localPoint.y + 0.5 + Ray.EPSILON) * this.scale;
    }
    
    // Use diagonal checkerboard logic for all surfaces.
    // The parity of the sum of the integer parts determines the color.
    int checkU = (int) Math.floor(u);
    int checkV = (int) Math.floor(v);
    if ((checkU + checkV) % 2 == 0) { // Diagonal checkerboard pattern
      return this.color1;
      } else {
      return this.color2;
    }
  }
  
  /**
   * Returns the reflectivity coefficient of the material.
   *
   * @return The reflectivity value (0.0-1.0).
   */
  @Override
  public double getReflectivity() {
    return reflectivity;
  }
  
  /**
   * Returns the index of refraction (IOR) of the material.
   *
   * @return The index of refraction.
   */
  @Override
  public double getIndexOfRefraction() {
    return ior;
  }
  
  /**
   * Returns the transparency coefficient of the material.
   *
   * @return The transparency value (0.0-1.0).
   */
  @Override
  public double getTransparency() {
    return transparency;
  }
  
}
