package net.elena.murat.material;

import net.elena.murat.math.Vector3;
import net.elena.murat.math.Point3;
import net.elena.murat.math.Matrix4;
import net.elena.murat.math.Matrix3;
import net.elena.murat.math.Ray;
import net.elena.murat.light.Light;

import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * BumpMaterial applies a normal map to perturb the surface normal, creating the illusion of detail
 * without adding more geometry. It wraps a base material for actual color calculation.
 * This material now fully implements the extended Material interface.
 */
public class BumpMaterial implements Material {
  private Material baseMaterial; // The underlying material whose lighting will be perturbed
  private ImageTexture normalMap; // The normal map texture (uses ImageTexture class)
  private double strength; // The strength of the bump effect (0.0 = no effect, 1.0 = full effect)
  private Matrix4 objectInverseTransform; // Inverse transform of the object this material is applied to
  private double uvScale; // Scale factor for UV coordinates when sampling the normal map
  
  /**
   * Constructs a BumpMaterial with a base material, a normal map, and a strength.
   * @param baseMaterial The base material (e.g., PhongMaterial, LambertMaterial).
   * @param normalMap The ImageTexture object containing the normal map.
   * @param strength The strength of the bump effect (typically 0.0 to 1.0).
   * @param uvScale The scaling factor for UV coordinates when sampling the normal map (e.g., 1.0 for 1 repetition per unit).
   * @param objectInverseTransform The full inverse transformation matrix of the object this material is applied to.
   */
  public BumpMaterial(Material baseMaterial, ImageTexture normalMap, double strength, double uvScale, Matrix4 objectInverseTransform) {
    this.baseMaterial = baseMaterial;
    this.normalMap = normalMap;
    this.strength = strength;
    this.uvScale = uvScale;
    this.objectInverseTransform = objectInverseTransform;
  }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
    if (tm == null) tm = new Matrix4 ();
    this.objectInverseTransform = tm;
  }
  
  // Getter methods for BumpMaterial's specific properties
  public ImageTexture getNormalMap() {
    return normalMap;
  }
  
  public double getStrength() {
    return strength;
  }
  
  /**
   * Calculates the final shaded color at a given 3D point on the surface,
   * applying the bump mapping effect by perturbing the normal before
   * delegating to the base material for actual color calculation.
   *
   * @param worldPoint The 3D point on the surface in world coordinates.
   * @param worldNormal The geometric surface normal at that point in world coordinates (should be normalized).
   * @param light The light source.
   * @param viewerPos The position of the viewer/camera.
   * @return The final shaded color.
   */
  @Override
  public Color getColorAt(Point3 worldPoint, Vector3 worldNormal, Light light, Point3 viewerPos) {
    // Ensure inverse transform is valid
    if (objectInverseTransform == null) {
      System.err.println("Error: BumpMaterial's inverse transform is null. Returning black.");
      return Color.BLACK;
    }
    // If no normal map, just use the base material with the original normal.
    // We check if normalMap is null, as ImageTexture itself might be null.
    if (normalMap == null) {
      return baseMaterial.getColorAt(worldPoint, worldNormal, light, viewerPos);
    }
    
    // 1. Transform world point and geometric normal to object's local space for UV mapping
    Point3 localPoint = objectInverseTransform.transformPoint(worldPoint);
    Vector3 localGeometricNormal = objectInverseTransform.inverseTransposeForNormal().transformVector(worldNormal).normalize();
    
    if (localGeometricNormal == null) {
      System.err.println("Error: BumpMaterial's local geometric normal is null. Returning base material color with original normal.");
      return baseMaterial.getColorAt(worldPoint, worldNormal, light, viewerPos);
    }
    
    // 2. Calculate UV coordinates based on local point and local geometric normal
    double u, v;
    double absNx = Math.abs(localGeometricNormal.x);
    double absNy = Math.abs(localGeometricNormal.y);
    double absNz = Math.abs(localGeometricNormal.z);
    
    // Planar UV mapping: project onto the dominant plane of the local normal
    // Add a small epsilon to the coordinates before flooring to handle floating point inaccuracies at boundaries.
    if (absNx > absNy && absNx > absNz) { // Normal is mostly X-axis (local Y-Z plane)
      u = localPoint.y * uvScale + Ray.EPSILON;
      v = localPoint.z * uvScale + Ray.EPSILON;
      } else if (absNy > absNx && absNy > absNz) { // Normal is mostly Y-axis (local X-Z plane)
      u = localPoint.x * uvScale + Ray.EPSILON;
      v = localPoint.z * uvScale + Ray.EPSILON;
      } else { // Normal is mostly Z-axis (local X-Y plane) or equally dominant
      u = localPoint.x * uvScale + Ray.EPSILON;
      v = localPoint.y * uvScale + Ray.EPSILON;
    }
    
    // 3. Sample the normal map at the calculated UV coordinates using ImageTexture's method
    Color normalMapColor = normalMap.getColorFromUV(u, v); // *** Düzeltilen kısım burası ***
    
    // Convert RGB color from normal map to a tangent-space vector (range -1 to 1)
    // Normal maps typically store (X, Y, Z) components as (R, G, B) where R,G,B are 0-255.
    // Convert to -1 to 1 range: (value / 255.0) * 2.0 - 1.0
    Vector3 tangentSpaceNormal = new Vector3(
      (normalMapColor.getRed() / 255.0) * 2.0 - 1.0,
      (normalMapColor.getGreen() / 255.0) * 2.0 - 1.0,
      (normalMapColor.getBlue() / 255.0) * 2.0 - 1.0
    ).normalize();
    
    // 4. Calculate Tangent and Bitangent vectors in world space
    // These form the basis for the TBN matrix.
    // A robust way to get a tangent: cross with a non-parallel axis.
    Vector3 tangent;
    // Ensure worldNormal is normalized before cross product
    Vector3 normalizedWorldNormal = worldNormal.normalize();
    
    if (Math.abs(normalizedWorldNormal.x) < 0.9 && Math.abs(normalizedWorldNormal.y) < 0.9) {
      tangent = normalizedWorldNormal.cross(new Vector3(0, 0, 1)).normalize();
      } else {
      tangent = normalizedWorldNormal.cross(new Vector3(0, 1, 0)).normalize();
    }
    
    Vector3 bitangent = normalizedWorldNormal.cross(tangent).normalize();
    
    // 5. Construct the TBN (Tangent, Bitangent, Normal) matrix
    // This matrix transforms a vector from tangent space to world space.
    Matrix3 TBN = new Matrix3(
      tangent.x, bitangent.x, normalizedWorldNormal.x,
      tangent.y, bitangent.y, normalizedWorldNormal.y,
      tangent.z, bitangent.z, normalizedWorldNormal.z
    );
    
    // 6. Transform the sampled normal from tangent space to world space
    Vector3 worldSpacePerturbedNormal = TBN.transform(tangentSpaceNormal).normalize();
    
    // 7. Interpolate between the original geometric normal and the perturbed normal based on strength
    // Using Vector3.lerp method (assuming it's added to Vector3 class)
    Vector3 finalNormal = worldNormal.lerp(worldSpacePerturbedNormal, strength).normalize();
    
    // 8. Delegate to the base material for actual color calculation using the perturbed normal
    return baseMaterial.getColorAt(worldPoint, finalNormal, light, viewerPos);
  }
  
  /**
   * Returns the reflectivity coefficient, delegated to the base material.
   * @return The reflectivity value from the base material.
   */
  @Override
  public double getReflectivity() {
    return baseMaterial.getReflectivity();
  }
  
  /**
   * Returns the index of refraction (IOR), delegated to the base material.
   * @return The IOR value from the base material.
   */
  @Override
  public double getIndexOfRefraction() {
    return baseMaterial.getIndexOfRefraction();
  }
  
  /**
   * Returns the transparency coefficient, delegated to the base material.
   * @return The transparency value from the base material.
   */
  @Override
  public double getTransparency() {
    return baseMaterial.getTransparency();
  }
  
}
