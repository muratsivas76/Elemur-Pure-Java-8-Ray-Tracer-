package net.elena.murat.shape;

import java.awt.Color;
import java.util.List;

import net.elena.murat.math.*;

import net.elena.murat.material.Material;
import net.elena.murat.material.SolidColorMaterial;

public class Sphere implements EMShape {
  private Material material;
  
  // Sphere's definition in its LOCAL coordinate system.
  // Canonical sphere: center at (0,0,0) and radius 'localRadius'.
  private final Point3 localCenter;
  private final double localRadius;
  
  // Transformation matrices
  private Matrix4 transform;        // Local to World transformation matrix
  private Matrix4 inverseTransform; // World to Local transformation matrix
  
  public Sphere(double radius) {
    this(radius, new SolidColorMaterial(Color.BLUE));
  }
  
  public Sphere(double radius, Material material) {
    // Define the sphere in its canonical local space: centered at origin
    this.localCenter = new Point3(0, 0, 0);
    this.localRadius = radius;
    this.material = material;
    
    // Initialize with identity transform by default
    this.transform = new Matrix4();
    this.inverseTransform = new Matrix4();
  }
  
  // The getCenter() method is no longer directly applicable for the world position
  // if using transformations. You'd calculate it from the transform matrix.
  // However, it can still return the local center for internal use.
  public Point3 getLocalCenter() {
    return localCenter;
  }
  
  // --- EMShape Interface Implementations ---
  
  @Override
  public Material getMaterial() {
    return material;
  }
  
  @Override
  public void setMaterial(Material material) {
    this.material = material;
  }
  
  /**
   * Sets the transformation matrix that converts points/vectors from the sphere's
   * local space to world space. When this is set, the inverse transform is also computed.
   * @param transform The 4x4 transformation matrix.
   */
  @Override
  public void setTransform(Matrix4 transform) {
    this.transform = transform;
    this.inverseTransform = transform.inverse(); // Pre-compute inverse for efficiency
  }
  
  /**
   * Returns the transformation matrix that converts points/vectors from the sphere's
   * local space to world space.
   * @return The 4x4 transformation matrix.
   */
  @Override
  public Matrix4 getTransform() {
    return this.transform;
  }
  
  /**
   * Returns the inverse of the transformation matrix, which converts points/vectors
   * from world space back to the sphere's local space.
   * @return The 4x4 inverse transformation matrix.
   */
  @Override
  public Matrix4 getInverseTransform() {
    return this.inverseTransform;
  }
  
  /**
   * Finds the intersection of a ray with this Sphere.
   * The ray is first transformed into the sphere's local coordinate system for intersection testing.
   * @param ray The ray to test intersection, in world coordinates.
   * @return The t value where the ray intersects the sphere, or Double.POSITIVE_INFINITY if no intersection.
   */
  @Override
  public double intersect(Ray ray) {
    // 1. Transform the ray into the sphere's local coordinate system
    // This effectively transforms the problem from intersecting a transformed sphere
    // with a world-space ray, to intersecting a canonical sphere with a
    // locally-transformed ray.
    Point3 localOrigin = inverseTransform.transformPoint(ray.getOrigin());
    Vector3 localDirection = inverseTransform.transformVector(ray.getDirection()).normalize(); // Normalize after transform
    
    // Use local references for calculations to simplify
    Vector3 oc = localOrigin.subtract(localCenter); // localCenter is (0,0,0), so this is just localOrigin
    
    double a = localDirection.dot(localDirection);
    double b = 2.0 * oc.dot(localDirection);
    double c = oc.dot(oc) - localRadius * localRadius;
    double discriminant = b * b - 4 * a * c;
    
    if (discriminant < 0) {
      return Double.POSITIVE_INFINITY;
      } else {
      double sqrtDiscriminant = Math.sqrt(discriminant);
      
      // First intersection point
      double t1 = (-b - sqrtDiscriminant) / (2.0 * a);
      // Second intersection point
      double t2 = (-b + sqrtDiscriminant) / (2.0 * a);
      
      // Find the closest valid intersection (t > Ray.EPSILON)
      if (t1 > Ray.EPSILON && t2 > Ray.EPSILON) {
        return Math.min(t1, t2);
        } else if (t1 > Ray.EPSILON) {
        return t1;
        } else if (t2 > Ray.EPSILON) {
        return t2;
      }
      return Double.POSITIVE_INFINITY; // No valid intersection in front of the ray
    }
  }
  
  /**
   * Calculates all intersection intervals between a ray and this sphere.
   * The ray is transformed into the sphere's local space for calculation.
   * For a sphere, there are typically two intersection points (in and out),
   * forming a single interval where the ray is inside the sphere.
   * @param ray The ray to test, in world coordinates.
   * @return A list containing the intersection interval(s). Empty if no intersection.
   */
  @Override
  public List<IntersectionInterval> intersectAll(Ray ray) {
    // 1. Transform the ray into the sphere's local coordinate system
    Point3 localOrigin = inverseTransform.transformPoint(ray.getOrigin());
    Vector3 localDirection = inverseTransform.transformVector(ray.getDirection()).normalize();
    
    // Use local references
    Vector3 oc = localOrigin.subtract(localCenter); // localCenter is (0,0,0)
    
    double a = localDirection.dot(localDirection);
    double b = 2.0 * oc.dot(localDirection);
    double c = oc.dot(oc) - localRadius * localRadius;
    double discriminant = b * b - 4 * a * c;
    
    if (discriminant < 0) {
      return java.util.Collections.emptyList();
    }
    
    double sqrtDiscriminant = Math.sqrt(discriminant);
    double t1 = (-b - sqrtDiscriminant) / (2.0 * a);
    double t2 = (-b + sqrtDiscriminant) / (2.0 * a);
    
    // Ensure t1 is the entry, t2 is the exit
    if (t1 > t2) {
      double temp = t1;
      t1 = t2;
      t2 = temp;
    }
    
    // Only consider intersections in front of the ray
    boolean t1Valid = t1 > Ray.EPSILON;
    boolean t2Valid = t2 > Ray.EPSILON;
    
    if (!t1Valid && !t2Valid) {
      return java.util.Collections.emptyList();
    }
    
    // Create Intersection objects
    Point3 point1 = ray.pointAtParameter(t1);
    Vector3 normal1 = getNormalAt(point1);
    Intersection in = new Intersection(point1, normal1, t1, this);
    
    Point3 point2 = ray.pointAtParameter(t2);
    Vector3 normal2 = getNormalAt(point2);
    Intersection out = new Intersection(point2, normal2, t2, this);
    
    // Return a single interval
    return java.util.Arrays.asList(new IntersectionInterval(t1, t2, in, out));
  }
  
  /**
   * Calculates the normal vector at a given point on the Sphere's surface in WORLD coordinates.
   * This involves transforming the world hit point to local space, calculating the local normal,
   * and then transforming it back to world space using the inverse transpose of the model matrix.
   * @param worldPoint The point on the Sphere's surface in world coordinates.
   * @return The normalized normal vector at that point in world coordinates.
   */
  @Override
  public Vector3 getNormalAt(Point3 worldPoint) {
    // 1. Transform the world hit point to the sphere's local coordinate system
    Point3 localHitPoint = inverseTransform.transformPoint(worldPoint);
    
    // 2. Calculate the normal in local space. For a sphere centered at localCenter (0,0,0),
    // the normal is simply the normalized vector from the localCenter to the localHitPoint.
    Vector3 localNormal = localHitPoint.subtract(localCenter).normalize();
    
    // 3. Transform the local normal back to world space.
    // Normals transform with the inverse transpose of the model matrix.
    //Matrix4 normalTransformMatrix = this.inverseTransform.transpose(); // M_normal = (M^-1)^T
    Matrix4 normalTransformMatrix = this.inverseTransform.inverseTransposeForNormal(); // Normaller için yeni metod
    return normalTransformMatrix.transformVector(localNormal).normalize(); // Ensure normalized after transform
  }
}
