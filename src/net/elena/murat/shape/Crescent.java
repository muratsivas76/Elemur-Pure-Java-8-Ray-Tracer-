package net.elena.murat.shape;

import java.util.List;

import net.elena.murat.math.*;
import net.elena.murat.material.Material;

/**
 * A 3D crescent shape (hilal) formed by the subtraction of one sphere from another.
 * Implements the EMShape interface for ray intersection and normal calculation.
 * This implementation uses Constructive Solid Geometry (CSG) for intersection.
 */
public class Crescent implements EMShape {
  private final double radius;        // Main sphere radius
  private final double cutRadius;     // Smaller sphere radius that cuts into the main sphere
  private final double cutDistance;   // Distance between sphere centers (from main sphere center to cut sphere center)
  private Material material;
  private Matrix4 transform = Matrix4.identity();
  private Matrix4 inverseTransform = Matrix4.identity();
  private Matrix4 inverseTransposeTransform = Matrix4.identity(); // For transforming normals
  
  // Sphere centers in local space (main sphere at origin, cut sphere along X-axis)
  private final Point3 mainSphereCenter = new Point3(0, 0, 0);
  private final Point3 cutSphereCenter;
  
  /**
   * Creates a crescent shape (hilal) using two spheres.
   * The crescent is formed by subtracting the 'cutSphere' from the 'mainSphere'.
   * @param radius Main sphere radius.
   * @param cutRadius Smaller sphere radius that cuts into the main sphere.
   * @param cutDistance Distance between the center of the main sphere (at origin)
   * and the center of the cut sphere (along X-axis).
   */
  public Crescent (double radius, double cutRadius, double cutDistance) {
    this.radius = radius;
    this.cutRadius = cutRadius;
    // Ensure cutDistance is valid for a visible crescent.
    // It must be greater than radius - cutRadius (otherwise cut sphere is fully inside main sphere)
    // and less than radius + cutRadius (otherwise spheres don't intersect).
    // We clamp it to a reasonable range.
    this.cutDistance = Math.max(Ray.EPSILON, Math.min(cutDistance, radius + cutRadius - Ray.EPSILON));
    
    // Initialize the cut sphere's center. Main sphere is at origin.
    this.cutSphereCenter = new Point3(this.cutDistance, 0, 0);
  }
  
  @Override
  public double intersect(Ray ray) {
    // Transform ray to local space
    Ray localRay = new Ray(
      inverseTransform.transformPoint(ray.getOrigin()),
      inverseTransform.transformVector(ray.getDirection())
    );
    
    // Find all intersections with both spheres
    // We need both entry and exit points for CSG
    double[] tMain = intersectSphereAll(localRay, mainSphereCenter, radius);
    double[] tCut = intersectSphereAll(localRay, cutSphereCenter, cutRadius);
    
    // No intersection with main sphere means no crescent
    if (tMain[0] < Ray.EPSILON && tMain[1] < Ray.EPSILON) {
      return -1;
    }
    
    double finalT = -1;
    
    // CSG Logic: A AND NOT B (Main Sphere AND NOT Cut Sphere)
    // Iterate through the intersection points of the main sphere
    for (int i = 0; i < 2; i++) {
      double t = tMain[i];
      if (t > Ray.EPSILON) { // Check for valid positive intersection
        Point3 hitPoint = localRay.pointAtParameter(t); // Use pointAtParameter
        // Check if this point is outside the cut sphere
        if (hitPoint.distance(cutSphereCenter) >= cutRadius - Ray.EPSILON) { // Use distance()
          // This is a valid entry point into the crescent
          finalT = t;
          break; // Found the closest valid intersection
        }
      }
    }
    
    // If no valid intersection found yet, check if ray starts inside the crescent
    // (i.e., inside main sphere but outside cut sphere)
    if (finalT < Ray.EPSILON) {
      boolean rayOriginInMain = localRay.getOrigin().distance(mainSphereCenter) < radius - Ray.EPSILON;
      boolean rayOriginOutCut = localRay.getOrigin().distance(cutSphereCenter) >= cutRadius - Ray.EPSILON;
      
      if (rayOriginInMain && rayOriginOutCut) {
        // Ray starts inside the crescent. Find the exit point from the main sphere.
        if (tMain[1] > Ray.EPSILON) {
          finalT = tMain[1];
        }
      }
    }
    
    return finalT;
  }
  
  /**
   * Calculates all intersection intervals between a ray and this crescent shape.
   * The crescent is defined as the region inside the main sphere and outside the cut sphere.
   * This method returns a list of intervals where the ray is inside the crescent.
   * @param ray The ray to test, in world coordinates.
   * @return A list of IntersectionInterval objects. Empty if no intersection.
   */
  @Override
  public List<IntersectionInterval> intersectAll(Ray ray) {
    // 1. Transform the ray into local space
    Ray localRay = new Ray(
      inverseTransform.transformPoint(ray.getOrigin()),
      inverseTransform.transformVector(ray.getDirection()).normalize()
    );
    
    // 2. Get all intersections with both spheres
    double[] tMain = intersectSphereAll(localRay, mainSphereCenter, radius);
    double[] tCut = intersectSphereAll(localRay, cutSphereCenter, cutRadius);
    
    // 3. Collect all valid t values with their "in/out" status
    List<TimedHit> hits = new java.util.ArrayList<>();
    
    // Add main sphere intersections (in: +1, out: -1)
    if (tMain[0] > Ray.EPSILON) hits.add(new TimedHit(tMain[0], 1, true));
    if (tMain[1] > Ray.EPSILON) hits.add(new TimedHit(tMain[1], -1, true));
    
    // Add cut sphere intersections (in: +1, out: -1)
    if (tCut[0] > Ray.EPSILON) hits.add(new TimedHit(tCut[0], 1, false));
    if (tCut[1] > Ray.EPSILON) hits.add(new TimedHit(tCut[1], -1, false));
    
    // 4. Sort by t
    java.util.Collections.sort(hits, (a, b) -> Double.compare(a.t, b.t));
    
    // 5. Track state: insideMain, insideCut
    boolean insideMain = false;
    boolean insideCut = false;
    boolean wasInsideCrescent = false;
    double currentIntervalStart = -1;
    
    List<IntersectionInterval> intervals = new java.util.ArrayList<>();
    
    for (TimedHit hit : hits) {
      // Update state
      if (hit.isMain) {
        insideMain = hit.delta > 0 ? true : false;
        } else {
        insideCut = hit.delta > 0 ? true : false;
      }
      
      boolean isInsideCrescent = insideMain && !insideCut;
      
      // Entering crescent
      if (!wasInsideCrescent && isInsideCrescent) {
        currentIntervalStart = hit.t;
      }
      
      // Exiting crescent
      if (wasInsideCrescent && !isInsideCrescent && currentIntervalStart >= 0) {
        // Create Intersection objects
        Point3 pointIn = ray.pointAtParameter(currentIntervalStart);
        Point3 pointOut = ray.pointAtParameter(hit.t);
        Vector3 normalIn = getNormalAt(pointIn);
        Vector3 normalOut = getNormalAt(pointOut);
        Intersection in = new Intersection(pointIn, normalIn, currentIntervalStart, this);
        Intersection out = new Intersection(pointOut, normalOut, hit.t, this);
        intervals.add(new IntersectionInterval(currentIntervalStart, hit.t, in, out));
        currentIntervalStart = -1;
      }
      
      wasInsideCrescent = isInsideCrescent;
    }
    
    // Handle case where ray ends inside crescent (no exit)
    // For rendering, we might ignore this, or treat it as going to infinity.
    // In a bounded scene, this is rare. We skip for now.
    
    return intervals;
  }
  
  // Helper class to track intersection events
  private static class TimedHit {
    final double t;
    final int delta; // +1 for entry, -1 for exit
    final boolean isMain; // true if from main sphere
    
    TimedHit(double t, int delta, boolean isMain) {
      this.t = t;
      this.delta = delta;
      this.isMain = isMain;
    }
  }
  
  /**
   * Helper method to intersect a ray with a sphere and return both t1 and t2.
   * @param ray The ray to test
   * @param center Sphere center
   * @param radius Sphere radius
   * @return An array containing t1 and t2. Returns {-1, -1} if no intersection.
   */
  private double[] intersectSphereAll(Ray ray, Point3 center, double radius) {
    Vector3 oc = ray.getOrigin().subtract(center);
    double a = ray.getDirection().dot(ray.getDirection());
    double b = 2.0 * oc.dot(ray.getDirection());
    double c = oc.dot(oc) - radius * radius;
    double discriminant = b * b - 4 * a * c;
    
    if (discriminant < 0) {
      return new double[]{-1, -1}; // No real roots
    }
    
    double sqrtDiscriminant = Math.sqrt(discriminant);
    double t1 = (-b - sqrtDiscriminant) / (2.0 * a);
    double t2 = (-b + sqrtDiscriminant) / (2.0 * a);
    
    // Ensure t1 is always the smaller (closer) positive intersection
    if (t1 > t2) {
      double temp = t1;
      t1 = t2;
      t2 = temp;
    }
    
    // Only return positive t values
    if (t1 < Ray.EPSILON) t1 = -1;
    if (t2 < Ray.EPSILON) t2 = -1;
    
    return new double[]{t1, t2};
  }
  
  @Override
  public Vector3 getNormalAt(Point3 worldPoint) {
    // Transform point to local space
    Point3 localPoint = inverseTransform.transformPoint(worldPoint);
    
    // Determine which surface the hit point is on
    // If the point is closer to the main sphere's surface (and outside the cut sphere)
    if (localPoint.distance(cutSphereCenter) >= cutRadius - Ray.EPSILON) { // Use distance()
      Vector3 normal = localPoint.subtract(mainSphereCenter).normalize();
      return inverseTransposeTransform.transformVector(normal).normalize();
    }
    // If the point is on the cut sphere's surface (inside the main sphere)
    else { // This implies localPoint.distance(cutSphereCenter) < cutRadius - Ray.EPSILON
      // Normal for the cut surface should point inwards to form the crescent
      Vector3 normal = cutSphereCenter.subtract(localPoint).normalize(); // Points towards cut sphere center
      return inverseTransposeTransform.transformVector(normal).normalize();
    }
  }
  
  @Override
  public void setTransform(Matrix4 transform) {
    this.transform = transform;
    this.inverseTransform = transform.inverse();
    // For normals, we need the inverse transpose of the 3x3 part of the transform matrix
    this.inverseTransposeTransform = transform.inverseTransposeForNormal();
  }
  
  @Override
  public Matrix4 getTransform() {
    return transform;
  }
  
  @Override
  public Matrix4 getInverseTransform() {
    return inverseTransform;
  }
  
  @Override
  public Material getMaterial() {
    return material;
  }
  
  @Override
  public void setMaterial(Material material) {
    this.material = material;
  }
}
/***
// Create a thin crescent (hilal)
CrescentShape crescent = new CrescentShape(
2.0,    // Main radius
1.8,    // Cut radius
0.5     // Distance between centers
);

// Set material and transform as needed
crescent.setMaterial(new DiffuseMaterial(Color.YELLOW));
crescent.setTransform(Matrix4.rotationY(Math.PI/4));
 */
