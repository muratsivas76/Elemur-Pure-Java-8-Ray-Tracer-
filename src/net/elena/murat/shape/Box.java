package net.elena.murat.shape;

import java.util.List;

// Custom imports
import net.elena.murat.material.Material;
import net.elena.murat.math.*;

/**
 * Represents an axis-aligned rectangular prism (or cuboid) in 3D space.
 * It implements EMShape to support transformations and materials.
 * The prism is defined by its width, height, and depth, centered at its local origin (0,0,0).
 */
public class Box implements EMShape {
  
  private final double width;
  private final double height;
  private final double depth;
  
  // EMShape interface transformation matrices and material
  private Matrix4 transform;
  private Matrix4 inverseTransform;
  private Matrix4 inverseTransposeTransformForNormal; // For correct normal transformation
  private Material material;
  
  /**
   * Constructs a Box with specified dimensions.
   * The prism is initially axis-aligned and centered at (0,0,0) in its local space.
   *
   * @param width The width of the prism along the local X-axis.
   * @param height The height of the prism along the local Y-axis.
   * @param depth The depth of the prism along the local Z-axis.
   */
  public Box(double width, double height, double depth) {
    this.width = width;
    this.height = height;
    this.depth = depth;
    // Default transform is identity.
    this.transform = Matrix4.identity();
    updateTransforms(); // Inverse transforms are calculated initially
  }
  
  /**
   * Constructs a Box with specified dimensions and a material.
   *
   * @param width The width of the prism along the local X-axis.
   * @param height The height of the prism along the local Y-axis.
   * @param depth The depth of the prism along the local Z-axis.
   * @param material The material applied to the prism.
   */
  public Box(double width, double height, double depth, Material material) {
    this(width, height, depth);
    this.setMaterial(material);
  }
  
  // --- EMShape Interface Methods Implementation ---
  
  @Override
  public void setTransform(Matrix4 transform) {
    // Create a deep copy of the incoming matrix to prevent external modifications
    this.transform = new Matrix4(transform);
    updateTransforms(); // Update inverse matrices when transform changes
  }
  
  @Override
  public Matrix4 getTransform() {
    return this.transform;
  }
  
  @Override
  public Matrix4 getInverseTransform() {
    return this.inverseTransform;
  }
  
  /**
   * Updates the inverse and inverse transpose transforms whenever the main transform changes.
   * This method is called internally by setTransform and the constructor.
   */
  private void updateTransforms() {
    this.inverseTransform = this.transform.inverse();
    this.inverseTransposeTransformForNormal = this.transform.inverseTransposeForNormal();
  }
  
  @Override
  public void setMaterial(Material material) {
    this.material = material;
  }
  
  @Override
  public Material getMaterial() {
    return this.material;
  }
  
  /**
   * Calculates the intersection of a ray with the rectangular prism.
   * This method transforms the ray into the object's local space,
   * performs the intersection test, and returns the 't' value.
   *
   * @param ray The ray to intersect with the prism (in world coordinates).
   * @return The distance 't' along the ray to the closest intersection point,
   * or Double.POSITIVE_INFINITY if no intersection.
   */
  @Override
  public double intersect(Ray ray) {
    // Ensure transforms are not null before proceeding
    if (inverseTransform == null || inverseTransposeTransformForNormal == null) {
      System.err.println("Error: Box transforms are null. Cannot intersect.");
      return Double.POSITIVE_INFINITY; // Return infinity if transforms are invalid
    }
    
    // 1. Transform the ray into the prism's local space
    Ray localRay = new Ray(
      inverseTransform.transformPoint(ray.getOrigin()),
      inverseTransform.transformVector(ray.getDirection()).normalize() // Normalize direction after transformation
    );
    
    double tMin = Double.NEGATIVE_INFINITY;
    double tMax = Double.POSITIVE_INFINITY;
    
    // Calculate half dimensions for easier calculations
    double halfWidth = width / 2.0;
    double halfHeight = height / 2.0;
    double halfDepth = depth / 2.0;
    
    // Intersection with X-planes (slab method)
    // Handle cases where ray direction component is zero to avoid division by zero
    if (Math.abs(localRay.getDirection().x) < Ray.EPSILON) {
      if (localRay.getOrigin().x < -halfWidth || localRay.getOrigin().x > halfWidth) {
        return Double.POSITIVE_INFINITY; // Ray is parallel and outside the slab
      }
      } else {
      double t1 = (-halfWidth - localRay.getOrigin().x) / localRay.getDirection().x;
      double t2 = (halfWidth - localRay.getOrigin().x) / localRay.getDirection().x;
      if (t1 > t2) { double temp = t1; t1 = t2; t2 = temp; } // Ensure t1 is min, t2 is max
        tMin = Math.max(tMin, t1);
      tMax = Math.min(tMax, t2);
      if (tMin > tMax) return Double.POSITIVE_INFINITY; // No intersection
      }
    
    // Intersection with Y-planes (slab method)
    if (Math.abs(localRay.getDirection().y) < Ray.EPSILON) {
      if (localRay.getOrigin().y < -halfHeight || localRay.getOrigin().y > halfHeight) {
        return Double.POSITIVE_INFINITY;
      }
      } else {
      double t1 = (-halfHeight - localRay.getOrigin().y) / localRay.getDirection().y;
      double t2 = (halfHeight - localRay.getOrigin().y) / localRay.getDirection().y;
      if (t1 > t2) { double temp = t1; t1 = t2; t2 = temp; }
        tMin = Math.max(tMin, t1);
      tMax = Math.min(tMax, t2);
      if (tMin > tMax) return Double.POSITIVE_INFINITY;
    }
    
    // Intersection with Z-planes (slab method)
    if (Math.abs(localRay.getDirection().z) < Ray.EPSILON) {
      if (localRay.getOrigin().z < -halfDepth || localRay.getOrigin().z > halfDepth) {
        return Double.POSITIVE_INFINITY;
      }
      } else {
      double t1 = (-halfDepth - localRay.getOrigin().z) / localRay.getDirection().z;
      double t2 = (halfDepth - localRay.getOrigin().z) / localRay.getDirection().z;
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
   * Calculates all intersection intervals between a ray and this rectangular prism.
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
    
    double halfWidth = width / 2.0;
    double halfHeight = height / 2.0;
    double halfDepth = depth / 2.0;
    
    // Slab intersection on X, Y, Z axes
    double[][] slabs = {
      { -halfWidth, halfWidth, localRay.getDirection().x, localRay.getOrigin().x },
      { -halfHeight, halfHeight, localRay.getDirection().y, localRay.getOrigin().y },
      { -halfDepth, halfDepth, localRay.getDirection().z, localRay.getOrigin().z }
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
    if (tMin < Ray.EPSILON) tMin = tMax; // Ray inside the box
      
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
   * Returns the surface normal at a given point on the prism's surface.
   * The point is in world coordinates. The normal is calculated in local space
   * and then transformed back to world space.
   *
   * @param worldPoint The point on the prism's surface in world coordinates.
   * @return The normalized normal vector at that point in world coordinates.
   */
  @Override
  public Vector3 getNormalAt(Point3 worldPoint) {
    // Ensure transforms are not null before proceeding
    if (inverseTransform == null || inverseTransposeTransformForNormal == null) {
      System.err.println("Error: Box transforms are null. Cannot compute normal.");
      // Fallback: return a default normal or throw an exception
      return new Vector3(0, 1, 0);
    }
    
    // Transform the world point to the prism's local space
    Point3 localPoint = this.getInverseTransform().transformPoint(worldPoint);
    
    Vector3 localNormal;
    
    // Use a slightly larger epsilon for normal calculation to avoid ambiguity at edges/corners
    double normalEpsilon = Ray.EPSILON * 10;
    
    double halfWidth = width / 2.0;
    double halfHeight = height / 2.0;
    double halfDepth = depth / 2.0;
    
    // Determine which face the local point is on by checking which coordinate is closest to the boundary
    if (Math.abs(localPoint.x - halfWidth) < normalEpsilon) {
      localNormal = new Vector3(1, 0, 0);
      } else if (Math.abs(localPoint.x + halfWidth) < normalEpsilon) {
      localNormal = new Vector3(-1, 0, 0);
    }
    else if (Math.abs(localPoint.y - halfHeight) < normalEpsilon) {
      localNormal = new Vector3(0, 1, 0);
      } else if (Math.abs(localPoint.y + halfHeight) < normalEpsilon) {
      localNormal = new Vector3(0, -1, 0);
    }
    else if (Math.abs(localPoint.z - halfDepth) < normalEpsilon) {
      localNormal = new Vector3(0, 0, 1);
      } else if (Math.abs(localPoint.z + halfDepth) < normalEpsilon) {
      localNormal = new Vector3(0, 0, -1);
      } else {
      // Fallback for floating point inaccuracies near edges/corners.
      // This attempts to find the closest face based on the local point's coordinates.
      double[] dists = {
        Math.abs(localPoint.x - halfWidth),
        Math.abs(localPoint.x + halfWidth),
        Math.abs(localPoint.y - halfHeight),
        Math.abs(localPoint.y + halfHeight),
        Math.abs(localPoint.z - halfDepth),
        Math.abs(localPoint.z + halfDepth)
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
      System.err.println("Warning: Box normal fallback used due to floating point inaccuracy.");
    }
    
    // Transform the local normal back to world space
    // Use inverse transpose for correct normal transformation
    return this.inverseTransposeTransformForNormal.transformVector(localNormal).normalize();
  }
  
}
