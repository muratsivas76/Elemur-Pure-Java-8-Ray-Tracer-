package net.elena.murat.shape;

import java.util.List;

// Custom imports
import net.elena.murat.material.Material;
import net.elena.murat.math.*;

/**
 * Represents a cube in the scene.
 * It can be defined by a side length (centered at origin) or by two corner points.
 * It implements EMShape to support transformations and materials.
 */
public class Cube implements EMShape {
  
  private Point3 minBounds; // Minimum corner of the cube in local space
  private Point3 maxBounds; // Maximum corner of the cube in local space
  private Material material;
  private Matrix4 transform;
  private Matrix4 inverseTransform;
  private Matrix4 inverseTransposeTransformForNormal; // For correct normal transformation
  
  /**
   * Constructs a Cube with the given side length, centered at the origin in its local space.
   * @param sideLength The length of each side of the cube.
   */
  public Cube(double sideLength) {
    double halfSide = sideLength / 2.0;
    this.minBounds = new Point3(-halfSide, -halfSide, -halfSide);
    this.maxBounds = new Point3(halfSide, halfSide, halfSide);
    this.transform = Matrix4.identity(); // Initialize with identity
    updateTransforms(); // Initial calculation of inverse transforms
  }
  
  /**
   * Constructs a Cube from two corner points in its local space.
   * The cube will be axis-aligned within its local coordinate system.
   * @param min The minimum corner of the cube (e.g., lower-left-back).
   * @param max The maximum corner of the cube (e.g., upper-right-front).
   */
  public Cube(Point3 min, Point3 max) {
    this.minBounds = min;
    this.maxBounds = max;
    this.transform = Matrix4.identity(); // Initialize with identity
    updateTransforms(); // Initial calculation of inverse transforms
  }
  
  @Override
  public void setMaterial(Material material) {
    this.material = material;
  }
  
  @Override
  public Material getMaterial() {
    return material;
  }
  
  @Override
  public void setTransform(Matrix4 transform) {
    // Create a deep copy of the incoming matrix to prevent external modifications
    this.transform = new Matrix4(transform);
    updateTransforms(); // Update inverse matrices when transform changes
  }
  
  @Override
  public Matrix4 getTransform() {
    return transform;
  }
  
  @Override
  public Matrix4 getInverseTransform() {
    return inverseTransform;
  }
  
  /**
   * Updates the inverse and inverse transpose transforms whenever the main transform changes.
   * This method is called internally by setTransform and the constructor.
   */
  private void updateTransforms() {
    this.inverseTransform = this.transform.inverse();
    this.inverseTransposeTransformForNormal = this.transform.inverseTransposeForNormal();
  }
  
  /**
   * Calculates the intersection of a ray with the cube.
   * This method transforms the ray into the object's local space,
   * performs the intersection test, and returns the 't' value.
   *
   * @param ray The ray to intersect with the cube (in world coordinates).
   * @return The distance 't' along the ray to the closest intersection point,
   * or Double.POSITIVE_INFINITY if no intersection.
   */
  @Override
  public double intersect(Ray ray) {
    // Ensure transforms are not null before proceeding
    if (inverseTransform == null || inverseTransposeTransformForNormal == null) {
      System.err.println("Error: Cube transforms are null. Cannot intersect.");
      return Double.POSITIVE_INFINITY; // Return infinity if transforms are invalid
    }
    
    // 1. Transform the ray into the cube's local space
    Ray localRay = new Ray(
      inverseTransform.transformPoint(ray.getOrigin()),
      inverseTransform.transformVector(ray.getDirection()).normalize() // Normalize direction after transformation
    );
    
    double tMin = Double.NEGATIVE_INFINITY;
    double tMax = Double.POSITIVE_INFINITY;
    
    // Kesişim hesaplamaları için minBounds ve maxBounds kullanılıyor
    // X-düzlemleriyle kesişim
    if (Math.abs(localRay.getDirection().x) < Ray.EPSILON) {
      if (localRay.getOrigin().x < minBounds.x || localRay.getOrigin().x > maxBounds.x) {
        return Double.POSITIVE_INFINITY;
      }
      } else {
      double t1 = (minBounds.x - localRay.getOrigin().x) / localRay.getDirection().x;
      double t2 = (maxBounds.x - localRay.getOrigin().x) / localRay.getDirection().x;
      if (t1 > t2) { double temp = t1; t1 = t2; t2 = temp; }
        tMin = Math.max(tMin, t1);
      tMax = Math.min(tMax, t2);
      if (tMin > tMax) return Double.POSITIVE_INFINITY;
    }
    
    // Y-düzlemleriyle kesişim
    if (Math.abs(localRay.getDirection().y) < Ray.EPSILON) {
      if (localRay.getOrigin().y < minBounds.y || localRay.getOrigin().y > maxBounds.y) {
        return Double.POSITIVE_INFINITY;
      }
      } else {
      double t1 = (minBounds.y - localRay.getOrigin().y) / localRay.getDirection().y;
      double t2 = (maxBounds.y - localRay.getOrigin().y) / localRay.getDirection().y;
      if (t1 > t2) { double temp = t1; t1 = t2; t2 = temp; }
        tMin = Math.max(tMin, t1);
      tMax = Math.min(tMax, t2);
      if (tMin > tMax) return Double.POSITIVE_INFINITY;
    }
    
    // Z-düzlemleriyle kesişim
    if (Math.abs(localRay.getDirection().z) < Ray.EPSILON) {
      if (localRay.getOrigin().z < minBounds.z || localRay.getOrigin().z > maxBounds.z) {
        return Double.POSITIVE_INFINITY;
      }
      } else {
      double t1 = (minBounds.z - localRay.getOrigin().z) / localRay.getDirection().z;
      double t2 = (maxBounds.z - localRay.getOrigin().z) / localRay.getDirection().z;
      if (t1 > t2) { double temp = t1; t1 = t2; t2 = temp; }
        tMin = Math.max(tMin, t1);
      tMax = Math.min(tMax, t2);
      if (tMin > tMax) return Double.POSITIVE_INFINITY;
    }
    
    double t = tMin;
    if (t < Ray.EPSILON) { // If closest intersection is behind or too close to origin
      t = tMax; // Try the farther intersection
      if (t < Ray.EPSILON) { // If farther intersection is also behind
        return Double.POSITIVE_INFINITY; // No valid intersection
      }
    }
    
    return t; // Return the valid intersection distance
  }
  
  /**
   * Calculates all intersection intervals between a ray and this cube.
   * Uses the slab method to find entry and exit points on the six faces.
   * @param ray The ray to test, in world coordinates.
   * @return A list of IntersectionInterval objects. Empty if no intersection.
   */
  @Override
  public List<IntersectionInterval> intersectAll(Ray ray) {
    // 1. Check for valid transforms
    if (inverseTransform == null || inverseTransposeTransformForNormal == null) {
      return java.util.Collections.emptyList();
    }
    
    // 2. Transform the ray into local space
    Ray localRay = new Ray(
      inverseTransform.transformPoint(ray.getOrigin()),
      inverseTransform.transformVector(ray.getDirection()).normalize()
    );
    
    double tMin = Double.NEGATIVE_INFINITY;
    double tMax = Double.POSITIVE_INFINITY;
    
    // Slab intersection on X, Y, Z axes
    double[][] slabs = {
      { minBounds.x, maxBounds.x, localRay.getDirection().x, localRay.getOrigin().x },
      { minBounds.y, maxBounds.y, localRay.getDirection().y, localRay.getOrigin().y },
      { minBounds.z, maxBounds.z, localRay.getDirection().z, localRay.getOrigin().z }
    };
    
    for (double[] slab : slabs) {
      double minBound = slab[0], maxBound = slab[1];
      double dir = slab[2], origin = slab[3];
      
      if (Math.abs(dir) < Ray.EPSILON) {
        if (origin < minBound || origin > maxBound) {
          return java.util.Collections.emptyList();
        }
        } else {
        double t1 = (minBound - origin) / dir;
        double t2 = (maxBound - origin) / dir;
        if (t1 > t2) { double temp = t1; t1 = t2; t2 = temp; }
          tMin = Math.max(tMin, t1);
        tMax = Math.min(tMax, t2);
        if (tMin > tMax) return java.util.Collections.emptyList();
      }
    }
    
    // 3. Now we have tMin (entry) and tMax (exit)
    if (tMax < Ray.EPSILON) return java.util.Collections.emptyList();
    if (tMin < Ray.EPSILON) tMin = tMax; // Ray is inside the cube
      
    // 4. Create Intersection objects
    Point3 pointIn = ray.pointAtParameter(tMin);
    Vector3 normalIn = getNormalAt(pointIn);
    Intersection in = new Intersection(pointIn, normalIn, tMin, this);
    
    Point3 pointOut = ray.pointAtParameter(tMax);
    Vector3 normalOut = getNormalAt(pointOut);
    Intersection out = new Intersection(pointOut, normalOut, tMax, this);
    
    // 5. Return the interval
    return java.util.Arrays.asList(new IntersectionInterval(tMin, tMax, in, out));
  }
  
  /**
   * Returns the surface normal at a given point on the cube's surface.
   * The point is in world coordinates. The normal is calculated in local space
   * and then transformed back to world space.
   *
   * @param worldPoint The point on the cube's surface in world coordinates.
   * @return The normalized normal vector at that point in world coordinates.
   */
  @Override
  public Vector3 getNormalAt(Point3 worldPoint) {
    // Ensure transforms are not null before proceeding
    if (inverseTransform == null || inverseTransposeTransformForNormal == null) {
      System.err.println("Error: Cube transforms are null. Cannot compute normal.");
      // Fallback: return a default normal or throw an exception
      return new Vector3(0, 1, 0);
    }
    
    // Transform the world point to the cube's local space
    Point3 localPoint = this.getInverseTransform().transformPoint(worldPoint);
    Vector3 localNormal = null;
    
    // Use a slightly larger epsilon for normal calculation to avoid ambiguity at edges/corners
    //double normalEpsilon = Ray.EPSILON * 10;
    double normalEpsilon = 1e-3;
    
    // Determine which face was hit to get the normal (based on min/max bounds)
    if (Math.abs(localPoint.x - maxBounds.x) < normalEpsilon) {
      localNormal = new Vector3(1, 0, 0);
      } else if (Math.abs(localPoint.x - minBounds.x) < normalEpsilon) {
      localNormal = new Vector3(-1, 0, 0);
      } else if (Math.abs(localPoint.y - maxBounds.y) < normalEpsilon) {
      localNormal = new Vector3(0, 1, 0);
      } else if (Math.abs(localPoint.y - minBounds.y) < normalEpsilon) {
      localNormal = new Vector3(0, -1, 0);
      } else if (Math.abs(localPoint.z - maxBounds.z) < normalEpsilon) {
      localNormal = new Vector3(0, 0, 1);
      } else if (Math.abs(localPoint.z - minBounds.z) < normalEpsilon) {
      localNormal = new Vector3(0, 0, -1);
      } else {
      // Fallback for floating point inaccuracies near edges/corners.
      // This attempts to find the closest face based on the local point's coordinates.
      double[] dists = {
        Math.abs(localPoint.x - maxBounds.x),
        Math.abs(localPoint.x - minBounds.x),
        Math.abs(localPoint.y - maxBounds.y),
        Math.abs(localPoint.y - minBounds.y),
        Math.abs(localPoint.z - maxBounds.z),
        Math.abs(localPoint.z - minBounds.z)
      };
      int minIdx = 0;
      for(int i = 1; i < 6; i++) {
        if (dists[i] < dists[minIdx]) {
          minIdx = i;
        }
      }
      switch(minIdx) {
        case 0: localNormal = new Vector3(1, 0, 0); break;
        case 1: localNormal = new Vector3(-1, 0, 0); break;
        case 2: localNormal = new Vector3(0, 1, 0); break;
        case 3: localNormal = new Vector3(0, -1, 0); break;
        case 4: localNormal = new Vector3(0, 0, 1); break;
        case 5: localNormal = new Vector3(0, 0, -1); break;
        default: localNormal = new Vector3(0, 1, 0); // Should not happen
        }
      //System.err.println("Warning: Cube normal fallback used due to floating point inaccuracy.");
    }
    
    // Transform the local normal back to world space
    // Use inverse transpose for correct normal transformation
    return this.inverseTransposeTransformForNormal.transformVector(localNormal).normalize();
  }
}
