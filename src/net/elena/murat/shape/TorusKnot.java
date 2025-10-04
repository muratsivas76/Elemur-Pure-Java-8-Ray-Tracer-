package net.elena.murat.shape;

import java.util.List;

import net.elena.murat.math.*;
import net.elena.murat.material.Material;

/**
 * (p,q) type torus knot
 * EMShape arayüzünü tam olarak uygular
 */
public class TorusKnot implements EMShape {
  private final double R;  // Torus major radius
  private final double r;  // Knot tube radius
  private final int p;     // Knot p parametresi
  private final int q;     // Knot q parametresi
  private Material material;
  private Matrix4 transform = Matrix4.identity();
  private Matrix4 inverseTransform = Matrix4.identity();
  private static final double EPSILON = 1e-4;
  
  public TorusKnot(double R, double r, int p, int q) {
    this.R = Math.max(0.1, R);
    this.r = Math.max(0.05, r);
    this.p = p;
    this.q = q;
  }
  
  @Override
  public double intersect(Ray ray) {
    Ray localRay = new Ray(
      inverseTransform.transformPoint(ray.getOrigin()),
      inverseTransform.transformVector(ray.getDirection()).normalize()
    );
    
    // Sphere marching ile kesişim
    double t = 0.0;
    double stepSize = 0.05;
    int maxSteps = 200;
    double threshold = 0.005;
    
    for (int i = 0; i < maxSteps; i++) {
      Point3 point = localRay.pointAtParameter(t);
      double dist = signedDistanceFunction(point);
      
      if (dist < threshold) {
        if (t > Ray.EPSILON) {
          return t;
        }
        break;
      }
      
      if (t > 100.0) break;
      t += Math.max(dist * 0.5, stepSize);
    }
    
    return -1.0;
  }
  
  /**
   * Calculates all intersection intervals between a ray and this torus knot using ray marching.
   * Detects both entry (tIn) and exit (tOut) points by monitoring the SDF sign change.
   * @param ray The ray to test, in world coordinates.
   * @return A list of IntersectionInterval objects. Empty if no valid interval.
   */
  @Override
  public List<IntersectionInterval> intersectAll(Ray ray) {
    // 1. Transform the ray into local space
    Ray localRay = new Ray(
      inverseTransform.transformPoint(ray.getOrigin()),
      inverseTransform.transformVector(ray.getDirection()).normalize()
    );
    
    double t = 0.0;
    double stepSize = 0.05;
    int maxSteps = 200;
    double threshold = 0.005;
    double maxDistance = 100.0;
    
    List<IntersectionInterval> intervals = new java.util.ArrayList<>();
    boolean isInside = false;
    double tIn = -1;
    
    for (int i = 0; i < maxSteps; i++) {
      Point3 point = localRay.pointAtParameter(t);
      double dist = signedDistanceFunction(point);
      
      boolean wasInside = isInside;
      isInside = dist < threshold;
      
      // Entry: from outside to inside
      if (!wasInside && isInside && tIn < 0 && t > Ray.EPSILON) {
        tIn = t;
      }
      
      // Exit: from inside to outside
      if (wasInside && !isInside && tIn >= 0) {
        double tOut = t;
        
        // Create Intersection objects
        Point3 worldIn = ray.pointAtParameter(tIn);
        Point3 worldOut = ray.pointAtParameter(tOut);
        Vector3 normalIn = getNormalAt(worldIn);
        Vector3 normalOut = getNormalAt(worldOut);
        Intersection in = new Intersection(worldIn, normalIn, tIn, this);
        Intersection out = new Intersection(worldOut, normalOut, tOut, this);
        
        intervals.add(new IntersectionInterval(tIn, tOut, in, out));
        tIn = -1; // Reset for next interval
      }
      
      // Move forward
      t += Math.max(dist * 0.5, stepSize);
      
      if (t > maxDistance) {
        // Handle case where ray ends inside (optional)
        break;
      }
    }
    
    return intervals;
  }
  
  private double signedDistanceFunction(Point3 p) {
    double theta = Math.atan2(p.y, p.x);
    double phi = (this.q * theta) / this.p; // Düzeltme: this.p kullanıldı
    
    Point3 knotPos = new Point3(
      (R + r * Math.cos(phi)) * Math.cos(theta),
      (R + r * Math.cos(phi)) * Math.sin(theta),
      r * Math.sin(phi)
    );
    
    return p.subtract(knotPos).length() - (r * 0.3);
  }
  
  @Override
  public Vector3 getNormalAt(Point3 worldPoint) {
    Point3 localPoint = inverseTransform.transformPoint(worldPoint);
    
    double eps = 0.001;
    double dx = signedDistanceFunction(localPoint.add(new Vector3(eps, 0, 0))) -
    signedDistanceFunction(localPoint.add(new Vector3(-eps, 0, 0)));
    double dy = signedDistanceFunction(localPoint.add(new Vector3(0, eps, 0))) -
    signedDistanceFunction(localPoint.add(new Vector3(0, -eps, 0)));
    double dz = signedDistanceFunction(localPoint.add(new Vector3(0, 0, eps))) -
    signedDistanceFunction(localPoint.add(new Vector3(0, 0, -eps)));
    
    Vector3 localNormal = new Vector3(dx, dy, dz).normalize();
    return inverseTransform.inverseTransposeForNormal().transformVector(localNormal).normalize();
  }
  
  @Override
  public void setTransform(Matrix4 transform) {
    this.transform = new Matrix4(transform);
    this.inverseTransform = transform.inverse();
  }
  
  @Override
  public Matrix4 getTransform() {
    return new Matrix4(transform);
  }
  
  @Override
  public Matrix4 getInverseTransform() {
    return new Matrix4(inverseTransform);
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
/**
 * Usage examples:
 *
 * // Classic trefoil knot (2,3)
 * TorusKnot trefoil = new TorusKnot(1.5, 0.4, 2, 3);
 * trefoil.setMaterial(new GlossyMaterial(Color.CYAN, 0.3));
 * trefoil.setTransform(Matrix4.rotationX(Math.PI/2));
 *
 * // More complex (3,5) knot
 * TorusKnot complexKnot = new TorusKnot(2.0, 0.3, 3, 5);
 * complexKnot.setTransform(
 *     Matrix4.translate(0, 1, 0)
 *            .multiply(Matrix4.scale(1.2, 1.2, 1.2))
 * );
 */
