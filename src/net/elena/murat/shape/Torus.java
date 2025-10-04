package net.elena.murat.shape;

import java.util.List;

import net.elena.murat.math.*;
import net.elena.murat.material.Material;

public class Torus implements EMShape {
  // Definition of torus in local coordinate system
  // Canonical Torus: centered at (0,0,0), aligned with Y axis
  private final double majorRadius; // R
  private final double minorRadius; // r
  private Material material;
  
  // Transformation matrices for EMShape interface
  private Matrix4 transform;        // From local space to world space
  private Matrix4 inverseTransform; // From world space to local space (inverse of transform)
  
  // Ray Marching Parameters
  private static final int MAX_MARCH_STEPS = 200;
  private static final double HIT_THRESHOLD = 0.001; // Threshold for distance function
  private static final double MAX_MARCH_DISTANCE = 100.0; // Maximum marching distance
  
  /**
   * Creates a Torus defined in local coordinate system.
   * Centered at (0,0,0). World space position and transformations are set via setTransform().
   * @param majorRadius Main radius of torus (from center to tube center)
   * @param minorRadius Secondary radius of torus (tube thickness)
   */
  public Torus(double majorRadius, double minorRadius) {
    this.majorRadius = majorRadius;
    this.minorRadius = minorRadius;
    this.material = null; // Material initially null
    
    // Initialize with identity transformation matrices by default
    this.transform = new Matrix4();
    this.inverseTransform = new Matrix4();
  }
  
  @Override
  public Material getMaterial() {
    return material;
  }
  
  @Override
  public void setMaterial(Material material) {
    this.material = material;
  }
  
  // --- EMShape Interface Implementations ---
  
  /**
   * Sets the transformation matrix from object's local space to world space.
   * When this matrix is set, the inverse transformation matrix is also calculated.
   * @param transform 4x4 transformation matrix (may contain translation, rotation, scaling).
   */
  @Override
  public void setTransform(Matrix4 transform) {
    this.transform = transform;
    this.inverseTransform = transform.inverse(); // Pre-calculate inverse for efficiency
    
    if (this.inverseTransform == null) {
      System.err.println("Warning: Could not compute inverse transform for Torus (determinant zero). Using identity matrix.");
      this.inverseTransform = new Matrix4();
    }
  }
  
  /**
   * Returns the transformation matrix from object's local space to world space.
   * @return 4x4 transformation matrix.
   */
  @Override
  public Matrix4 getTransform() {
    return this.transform;
  }
  
  /**
   * Returns the transformation matrix from world space to object's local space.
   * @return 4x4 inverse transformation matrix.
   */
  @Override
  public Matrix4 getInverseTransform() {
    return this.inverseTransform;
  }
  
  /**
   * Calculates the SDF distance of a given point in the torus's local coordinate system.
   * This method assumes the torus is centered at origin and aligned with Y axis.
   * @param p Point in object's local space.
   * @return SDF distance from point to torus.
   */
  private double calculateSDFLocal(Point3 p) {
    // q.x gives distance to torus's main circle (in XZ plane)
    // q.y gives distance along torus's Z axis (donut along Y axis)
    // Note: Previous code used Z as Y, which is a common approach in SDFs
    // (x, y, z) -> (x, z, y) or (sqrt(x^2+y^2)-R, z)
    // If torus's main circle is in XY plane:
    // double q_x = new Vector3(p.x, p.y, 0).length() - majorRadius; // sqrt(x^2 + y^2) - R
    // double q_y = p.z; // Z component
    
    // Following previous code's logic (majorRadius in XY plane, minorRadius in Z direction)
    // So, Torus rotates around Y axis and its main plane is XZ plane.
    double q_x_dist = new Vector3(p.x, p.z, 0).length() - majorRadius; // Circular distance in XZ plane - major radius
    double q_y_val = p.y; // Torus's own "height" axis (minorRadius direction)
    
    return Math.sqrt(q_x_dist * q_x_dist + q_y_val * q_y_val) - minorRadius;
  }
  
  /**
   * Checks if a ray intersects this Torus object using Ray Marching technique.
   *
   * @param ray Ray to test for intersection (in world space).
   * @return Distance from ray origin to intersection point (t) if exists, otherwise Double.POSITIVE_INFINITY.
   */
  @Override
  public double intersect(Ray ray) {
    // 1. Transform ray into object's local coordinate system
    Point3 localRayOrigin = inverseTransform.transformPoint(ray.getOrigin());
    Vector3 localRayDirection = inverseTransform.transformVector(ray.getDirection()).normalize();
    
    double totalDistanceMarched = 0.0;
    Point3 currentLocalRayPosition = localRayOrigin; // Ray's current position (in local space)
    
    for (int i = 0; i < MAX_MARCH_STEPS; i++) {
      // Calculate distance from current position to torus (in local space)
      double distanceToTorus = calculateSDFLocal(currentLocalRayPosition);
      
      // If distance is below threshold, we found an intersection
      if (distanceToTorus < HIT_THRESHOLD) {
        return totalDistanceMarched; // Return total marched distance
      }
      
      // Advance ray by distance to object (in local space)
      currentLocalRayPosition = currentLocalRayPosition.add(localRayDirection.scale(distanceToTorus));
      totalDistanceMarched += distanceToTorus;
      
      // If total marched distance exceeds maximum distance, no intersection
      if (totalDistanceMarched >= MAX_MARCH_DISTANCE) {
        return Double.POSITIVE_INFINITY; // No intersection
      }
    }
    // If maximum steps reached without finding intersection
    return Double.POSITIVE_INFINITY;
  }
  
  /**
   * Calculates all intersection intervals between a ray and this torus using ray marching.
   * Detects both entry (tIn) and exit (tOut) points by monitoring the SDF sign change.
   * @param ray The ray to test, in world coordinates.
   * @return A list of IntersectionInterval objects. Empty if no valid interval.
   */
  @Override
  public List<IntersectionInterval> intersectAll(Ray ray) {
    // 1. Transform the ray into local space
    Point3 localOrigin = inverseTransform.transformPoint(ray.getOrigin());
    Vector3 localDirection = inverseTransform.transformVector(ray.getDirection()).normalize();
    
    double totalDistance = 0.0;
    Point3 currentPosition = localOrigin;
    List<IntersectionInterval> intervals = new java.util.ArrayList<>();
    
    // Track if we are currently inside the shape
    boolean isInside = false;
    double tIn = -1;
    
    for (int i = 0; i < MAX_MARCH_STEPS; i++) {
      double sdf = calculateSDFLocal(currentPosition);
      
      boolean wasInside = isInside;
      isInside = sdf < HIT_THRESHOLD;
      
      // Entry: from outside to inside
      if (!wasInside && isInside && tIn < 0) {
        tIn = totalDistance;
      }
      
      // Exit: from inside to outside
      if (wasInside && !isInside && tIn >= 0) {
        double tOut = totalDistance;
        
        // Create Intersection objects
        Point3 pointIn = ray.pointAtParameter(tIn);
        Point3 pointOut = ray.pointAtParameter(tOut);
        Vector3 normalIn = getNormalAt(pointIn);
        Vector3 normalOut = getNormalAt(pointOut);
        Intersection in = new Intersection(pointIn, normalIn, tIn, this);
        Intersection out = new Intersection(pointOut, normalOut, tOut, this);
        
        intervals.add(new IntersectionInterval(tIn, tOut, in, out));
        tIn = -1; // Reset for next interval
      }
      
      // Move ray forward
      double step = Math.max(sdf, 0.01); // Avoid zero step
      currentPosition = currentPosition.add(localDirection.scale(step));
      totalDistance += step;
      
      if (totalDistance > MAX_MARCH_DISTANCE) {
        break;
      }
    }
    
    return intervals;
  }
  
  /**
   * Calculates the surface normal at intersection point.
   * Normal is approximated using gradient of SDF function (via finite differences method).
   *
   * @param worldPoint Intersection point (in world space).
   * @return Normalized surface normal at intersection point (in world space).
   */
  @Override
  public Vector3 getNormalAt(Point3 worldPoint) {
    // 1. Transform intersection point to object's local space
    Point3 localIntersectionPoint = inverseTransform.transformPoint(worldPoint);
    
    final double h = 0.0001; // Small perturbation (epsilon) for gradient calculation
    
    // Calculate normal in local space (numerical differentiation - approximate gradient)
    // Using SDF value changes with dx, dy, dz increments
    double nx = calculateSDFLocal(localIntersectionPoint.add(new Vector3(h, 0, 0))) - calculateSDFLocal(localIntersectionPoint.add(new Vector3(-h, 0, 0)));
    double ny = calculateSDFLocal(localIntersectionPoint.add(new Vector3(0, h, 0))) - calculateSDFLocal(localIntersectionPoint.add(new Vector3(0, -h, 0)));
    double nz = calculateSDFLocal(localIntersectionPoint.add(new Vector3(0, 0, h))) - calculateSDFLocal(localIntersectionPoint.add(new Vector3(0, 0, -h)));
    
    Vector3 localNormal = new Vector3(nx, ny, nz).normalize();
    
    // 2. Transform local normal back to world space
    // Normals are transformed using inverse transpose of model matrix
    Matrix4 normalTransformMatrix = this.inverseTransform.inverseTransposeForNormal(); // New method for normals
    return normalTransformMatrix.transformVector(localNormal).normalize(); // Normalize after transformation
  }
  
}
