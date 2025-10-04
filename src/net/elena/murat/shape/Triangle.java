package net.elena.murat.shape;

import java.util.List;

import net.elena.murat.material.Material;
import net.elena.murat.math.*;

/**
 * Triangle class represents a single triangle, defined by three vertices.
 * It implements the EMShape interface, supporting Matrix4 transformations.
 * The vertices are defined in the triangle's local coordinate system.
 */
public class Triangle implements EMShape {
  // Vertices defined in the triangle's LOCAL coordinate system
  private final Point3 localV0, localV1, localV2;
  private Material material;
  
  // Transformation matrices
  private Matrix4 transform;        // Local to World transformation matrix
  private Matrix4 inverseTransform; // World to Local transformation matrix
  
  // Precomputed local normal for optimization (recalculated if vertices change, not transform)
  private Vector3 precomputedLocalNormal;
  
  /**
   * Constructs a triangle using its three vertices in its LOCAL coordinate system.
   * The material must be set separately using setMaterial().
   * The transformation matrix is initialized to identity; use setTransform() to position.
   * @param v0 The first vertex in local space.
   * @param v1 The second vertex in local space.
   * @param v2 The third vertex in local space.
   */
  public Triangle(Point3 v0, Point3 v1, Point3 v2) {
    this.localV0 = v0;
    this.localV1 = v1;
    this.localV2 = v2;
    this.material = null;
    
    // Initialize with identity transform by default
    this.transform = new Matrix4();
    this.inverseTransform = new Matrix4();
    
    precomputeLocalNormal(); // Compute normal based on local vertices
  }
  
  // --- EMShape Interface Implementations ---
  
  @Override
  public void setMaterial(Material material) {
    this.material = material;
  }
  
  @Override
  public Material getMaterial() {
    return this.material;
  }
  
  /**
   * Sets the transformation matrix that converts points/vectors from the triangle's
   * local space to world space. When this is set, the inverse transform is also computed.
   * @param transform The 4x4 transformation matrix.
   */
  @Override
  public void setTransform(Matrix4 transform) {
    this.transform = transform;
    this.inverseTransform = transform.inverse(); // Pre-compute inverse for efficiency
    
    // You might want to add a check here if inverseTransform is null (determinant zero),
    // similar to your Matrix3 constructor, though Matrix4.inverse() should handle it.
    if (this.inverseTransform == null) {
      System.err.println("Warning: Could not compute inverse transform for Triangle (determinant zero). Using identity matrix.");
      this.inverseTransform = new Matrix4();
    }
  }
  
  /**
   * Returns the transformation matrix that converts points/vectors from the triangle's
   * local space to world space.
   * @return The 4x4 transformation matrix.
   */
  @Override
  public Matrix4 getTransform() {
    return this.transform;
  }
  
  /**
   * Returns the inverse of the transformation matrix, which converts points/vectors
   * from world space back to the triangle's local space.
   * @return The 4x4 inverse transformation matrix.
   */
  @Override
  public Matrix4 getInverseTransform() {
    return this.inverseTransform;
  }
  
  /**
   * Precomputes the normal vector of the triangle in its LOCAL coordinate system.
   * This normal assumes a counter-clockwise winding order of vertices (v0, v1, v2)
   * when viewed from the front face.
   */
  private void precomputeLocalNormal() {
    Vector3 edge1 = localV1.subtract(localV0);
    Vector3 edge2 = localV2.subtract(localV0);
    this.precomputedLocalNormal = edge1.cross(edge2).normalize();
  }
  
  /**
   * Calculates the intersection of a ray with this triangle.
   * The ray is first transformed into the triangle's local coordinate system for intersection testing.
   * Uses the Moller-Trumbore algorithm for efficient ray-triangle intersection.
   * @param ray The ray to test intersection, in world coordinates.
   * @return The t value where the ray intersects the triangle, or Double.POSITIVE_INFINITY if no intersection.
   */
  @Override
  public double intersect(Ray ray) {
    // 1. Transform the world ray into the triangle's local coordinate system
    Point3 localOrigin = inverseTransform.transformPoint(ray.getOrigin());
    // Normalize localDirection in case of non-uniform scaling affecting length,
    // although for Moller-Trumbore, the length of the direction vector affects 't' linearly.
    // It's crucial for normal transformations later if using transformVector with non-uniform scale.
    Vector3 localDirection = inverseTransform.transformVector(ray.getDirection()).normalize();
    
    // Create a local ray for intersection testing
    Ray localRay = new Ray(localOrigin, localDirection);
    
    // Vertices for intersection are already in local space (localV0, localV1, localV2)
    Vector3 edge1 = localV1.subtract(localV0);
    Vector3 edge2 = localV2.subtract(localV0);
    
    Vector3 pvec = localRay.getDirection().cross(edge2);
    double det = edge1.dot(pvec);
    
    // Check for parallel ray (determinant close to zero)
    if (det > -Ray.EPSILON && det < Ray.EPSILON) {
      return Double.POSITIVE_INFINITY;
    }
    
    double invDet = 1.0 / det;
    
    Vector3 tvec = localRay.getOrigin().subtract(localV0);
    double u = tvec.dot(pvec) * invDet;
    
    // Check barycentric coordinate U
    if (u < -Ray.EPSILON || u > 1.0 + Ray.EPSILON) { // Add epsilon for robustness
      return Double.POSITIVE_INFINITY;
    }
    
    Vector3 qvec = tvec.cross(edge1);
    double v = localRay.getDirection().dot(qvec) * invDet;
    
    // Check barycentric coordinate V
    if (v < -Ray.EPSILON || u + v > 1.0 + Ray.EPSILON) { // Add epsilon for robustness
      return Double.POSITIVE_INFINITY;
    }
    
    double t = edge2.dot(qvec) * invDet;
    
    // Check if the intersection point is in front of the ray origin
    if (t > Ray.EPSILON) {
      return t;
      } else {
      return Double.POSITIVE_INFINITY;
    }
  }
  
  /**
   * Calculates all intersection intervals between a ray and this triangle.
   * Since a triangle is a flat, infinitely thin surface, the entry and exit points are considered the same.
   * @param ray The ray to test, in world coordinates.
   * @return A list containing a single degenerate interval if intersected, otherwise empty list.
   */
  @Override
  public List<IntersectionInterval> intersectAll(Ray ray) {
    // 1. Transform the ray into the triangle's local coordinate system
    Point3 localOrigin = inverseTransform.transformPoint(ray.getOrigin());
    Vector3 localDirection = inverseTransform.transformVector(ray.getDirection()).normalize();
    Ray localRay = new Ray(localOrigin, localDirection);
    
    // 2. Use Moller-Trumbore algorithm to find intersection
    Vector3 edge1 = localV1.subtract(localV0);
    Vector3 edge2 = localV2.subtract(localV0);
    Vector3 pvec = localDirection.cross(edge2);
    double det = edge1.dot(pvec);
    
    // Check for parallel ray
    if (Math.abs(det) < Ray.EPSILON) {
      return java.util.Collections.emptyList();
    }
    
    double invDet = 1.0 / det;
    Vector3 tvec = localOrigin.subtract(localV0);
    double u = tvec.dot(pvec) * invDet;
    
    // Barycentric coordinate u check
    if (u < -Ray.EPSILON || u > 1.0 + Ray.EPSILON) {
      return java.util.Collections.emptyList();
    }
    
    Vector3 qvec = tvec.cross(edge1);
    double v = localDirection.dot(qvec) * invDet;
    
    // Barycentric coordinate v check
    if (v < -Ray.EPSILON || u + v > 1.0 + Ray.EPSILON) {
      return java.util.Collections.emptyList();
    }
    
    double t = edge2.dot(qvec) * invDet;
    
    // Check if intersection is in front of the ray
    if (t <= Ray.EPSILON) {
      return java.util.Collections.emptyList();
    }
    
    // 3. Create a single degenerate interval
    Point3 worldHit = ray.pointAtParameter(t);
    Vector3 worldNormal = getNormalAt(worldHit);
    Intersection hit = new Intersection(worldHit, worldNormal, t, this);
    
    IntersectionInterval interval = IntersectionInterval.point(t, hit);
    return java.util.Arrays.asList(interval);
  }
  
  /**
   * Calculates the normal vector at a given point on the Triangle's surface in WORLD coordinates.
   * For a triangle, the normal is constant across its surface (assuming it's flat).
   * This involves transforming the precomputed local normal to world space using the
   * inverse transpose of the model matrix.
   * @param worldPoint The point on the Triangle's surface in world coordinates (can be ignored for flat triangles).
   * @return The normalized normal vector at that point in world coordinates.
   */
  @Override
  public Vector3 getNormalAt(Point3 worldPoint) {
    // Normals transform with the inverse transpose of the model matrix.
    // For triangles, the normal is constant across the surface.
    //Matrix4 normalTransformMatrix = this.inverseTransform.transpose(); // M_normal = (M^-1)^T
    Matrix4 normalTransformMatrix = this.inverseTransform.inverseTransposeForNormal(); // Normaller için yeni metod
    return normalTransformMatrix.transformVector(precomputedLocalNormal).normalize(); // Ensure normalized after transform
  }
  
  // You might still want these getters for debugging or specific use cases,
  // but remember they return vertices in LOCAL space.
  public Point3 getLocalV0() {
    return localV0;
  }
  
  public Point3 getLocalV1() {
    return localV1;
  }
  
  public Point3 getLocalV2() {
    return localV2;
  }
}
