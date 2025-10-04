package net.elena.murat.shape;

import java.util.List;

import net.elena.murat.math.*;
import net.elena.murat.material.Material;

/**
 * Single-sheet hyperboloid shape (x²/a² + y²/b² - z²/c² = 1)
 * Fully implements the EMShape interface
 */
public class Hyperboloid implements EMShape {
  private final double a, b, c; // Hyperboloid parameters
  private final double height;  // Height limit along Z-axis (|z| <= height)
  private Material material;
  private Matrix4 transform = Matrix4.identity();
  private Matrix4 inverseTransform = Matrix4.identity();
  
  /**
   * Standard hyperboloid constructor (a=1, b=1, c=1, height=5)
   */
  public Hyperboloid() {
    this(1.0, 1.0, 1.0, 5.0);
  }
  
  /**
   * Parameterized hyperboloid constructor
   * @param a X-axis parameter
   * @param b Y-axis parameter
   * @param c Z-axis parameter
   * @param height Maximum height (taken as absolute value)
   */
  public Hyperboloid(double a, double b, double c, double height) {
    this.a = a;
    this.b = b;
    this.c = c;
    this.height = Math.abs(height);
  }
  
  @Override
  public double intersect(Ray ray) {
    // Transform ray to local coordinate system
    Point3 localOrigin = inverseTransform.transformPoint(ray.getOrigin());
    Vector3 localDirection = inverseTransform.transformVector(ray.getDirection()).normalize();
    Ray localRay = new Ray(localOrigin, localDirection);
    
    // Calculate quadratic equation coefficients: At² + Bt + C = 0
    double ox = localRay.getOrigin().x;
    double oy = localRay.getOrigin().y;
    double oz = localRay.getOrigin().z;
    double dx = localRay.getDirection().x;
    double dy = localRay.getDirection().y;
    double dz = localRay.getDirection().z;
    
    double a2 = a * a;
    double b2 = b * b;
    double c2 = c * c;
    
    double A = (dx*dx)/a2 + (dy*dy)/b2 - (dz*dz)/c2;
    double B = 2.0 * ((ox*dx)/a2 + (oy*dy)/b2 - (oz*dz)/c2);
    double C = (ox*ox)/a2 + (oy*oy)/b2 - (oz*oz)/c2 - 1.0;
    
    double discriminant = B*B - 4.0*A*C;
    
    if (discriminant < 0.0) {
      return -1.0; // No intersection
    }
    
    double sqrtDiscriminant = Math.sqrt(discriminant);
    double t1 = (-B - sqrtDiscriminant) / (2.0*A);
    double t2 = (-B + sqrtDiscriminant) / (2.0*A);
    
    // Check valid intersection points
    Point3 p1 = localRay.pointAtParameter(t1);
    Point3 p2 = localRay.pointAtParameter(t2);
    
    boolean validT1 = isValidIntersection(p1);
    boolean validT2 = isValidIntersection(p2);
    
    if (validT1 && validT2) {
      return Math.min(t1, t2);
      } else if (validT1) {
      return t1;
      } else if (validT2) {
      return t2;
    }
    
    return -1.0;
  }
  
  /**
   * Calculates all intersection intervals between a ray and this hyperboloid.
   * The ray is transformed into the hyperboloid's local space for calculation.
   * The method solves the quadratic equation for the hyperboloid and returns
   * intervals where the ray is inside the shape (between valid intersections).
   * @param ray The ray to test, in world coordinates.
   * @return A list of IntersectionInterval objects. Empty if no valid intersection.
   */
  @Override
  public List<IntersectionInterval> intersectAll(Ray ray) {
    // 1. Transform the ray into local space
    Point3 localOrigin = inverseTransform.transformPoint(ray.getOrigin());
    Vector3 localDirection = inverseTransform.transformVector(ray.getDirection()).normalize();
    Ray localRay = new Ray(localOrigin, localDirection);
    
    // 2. Coefficients for quadratic equation: At² + Bt + C = 0
    double ox = localOrigin.x;
    double oy = localOrigin.y;
    double oz = localOrigin.z;
    double dx = localDirection.x;
    double dy = localDirection.y;
    double dz = localDirection.z;
    
    double a2 = a * a;
    double b2 = b * b;
    double c2 = c * c;
    
    double A = (dx*dx)/a2 + (dy*dy)/b2 - (dz*dz)/c2;
    double B = 2.0 * ((ox*dx)/a2 + (oy*dy)/b2 - (oz*dz)/c2);
    double C = (ox*ox)/a2 + (oy*oy)/b2 - (oz*oz)/c2 - 1.0;
    
    double discriminant = B*B - 4.0*A*C;
    if (discriminant < 0.0) {
      return java.util.Collections.emptyList();
    }
    
    double sqrtDiscriminant = Math.sqrt(discriminant);
    double t1 = (-B - sqrtDiscriminant) / (2.0 * A);
    double t2 = (-B + sqrtDiscriminant) / (2.0 * A);
    
    // Ensure t1 is entry, t2 is exit
    if (t1 > t2) {
      double temp = t1;
      t1 = t2;
      t2 = temp;
    }
    
    // 3. Check validity (within height bounds)
    Point3 p1 = localRay.pointAtParameter(t1);
    Point3 p2 = localRay.pointAtParameter(t2);
    
    boolean validT1 = isValidIntersection(p1);
    boolean validT2 = isValidIntersection(p2);
    
    List<IntersectionInterval> intervals = new java.util.ArrayList<>();
    
    if (validT1 && validT2) {
      // Both intersections valid → full interval
      Point3 worldIn = ray.pointAtParameter(t1);
      Point3 worldOut = ray.pointAtParameter(t2);
      Vector3 normalIn = getNormalAt(worldIn);
      Vector3 normalOut = getNormalAt(worldOut);
      Intersection in = new Intersection(worldIn, normalIn, t1, this);
      Intersection out = new Intersection(worldOut, normalOut, t2, this);
      intervals.add(new IntersectionInterval(t1, t2, in, out));
      } else if (validT1) {
      // Only t1 valid → degenerate interval (e.g., ray ends inside)
      Point3 worldIn = ray.pointAtParameter(t1);
      Vector3 normalIn = getNormalAt(worldIn);
      Intersection hit = new Intersection(worldIn, normalIn, t1, this);
      intervals.add(IntersectionInterval.point(t1, hit));
      } else if (validT2) {
      // Only t2 valid → degenerate interval
      Point3 worldIn = ray.pointAtParameter(t2);
      Vector3 normalIn = getNormalAt(worldIn);
      Intersection hit = new Intersection(worldIn, normalIn, t2, this);
      intervals.add(IntersectionInterval.point(t2, hit));
    }
    
    return intervals;
  }
  
  private boolean isValidIntersection(Point3 localPoint) {
    // Height boundary check
    return Math.abs(localPoint.z) <= height;
  }
  
  @Override
  public Vector3 getNormalAt(Point3 worldPoint) {
    Point3 localPoint = inverseTransform.transformPoint(worldPoint);
    
    // Hyperboloid surface normal (gradient)
    Vector3 localNormal = new Vector3(
      2.0 * localPoint.x / (a * a),
      2.0 * localPoint.y / (b * b),
      -2.0 * localPoint.z / (c * c)
    ).normalize();
    
    // Transform normal to world coordinates (using inverse transpose)
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
  
  // Helper getter methods
  public double getA() { return a; }
  public double getB() { return b; }
  public double getC() { return c; }
  public double getHeight() { return height; }
}

/**
 * Usage examples:
 *
 * // Standard hyperboloid
 * Hyperboloid hyperboloid = new Hyperboloid();
 * hyperboloid.setMaterial(new GlossyMaterial(Color.RED, 0.3));
 *
 * // Customized hyperboloid
 * Hyperboloid customHyper = new Hyperboloid(1.5, 0.8, 1.2, 3.0);
 * customHyper.setTransform(Matrix4.rotationY(Math.PI/4));
 *
 * // Transformed hyperboloid
 * Hyperboloid transformed = new Hyperboloid();
 * transformed.setTransform(
 *     Matrix4.translate(2, 0, 0)
 *            .multiply(Matrix4.scale(1, 2, 1)) // Stretch along Y-axis
 * );
 */
