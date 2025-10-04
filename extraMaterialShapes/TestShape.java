import java.util.ArrayList;
import java.util.List;

import net.elena.murat.math.*;
import net.elena.murat.material.Material;
import net.elena.murat.shape.EMShape;

public class TestShape implements EMShape {
  private Material material;
  private Matrix4 transform;
  private Matrix4 inverseTransform;
  private final double radius;
  
  // Default constructor
  public TestShape() {
    this(0.5);
  }
  
  // Parameterized constructor
  public TestShape(double radius) {
    this.radius = radius;
    this.transform = Matrix4.identity();
    this.inverseTransform = Matrix4.identity();
  }
  
  // For CSG: intersectAll method
  @Override
  public List<IntersectionInterval> intersectAll(Ray ray) {
    List<IntersectionInterval> intervals = new ArrayList<>();
    
    // Transform ray to object space using inverse transform
    Point3 transformedOrigin = inverseTransform.transformPoint(ray.getOrigin());
    Vector3 transformedDirection = inverseTransform.transformVector(ray.getDirection()).normalize();
    Ray transformedRay = new Ray(transformedOrigin, transformedDirection);
    
    // Basit sphere intersection
    Vector3 oc = transformedRay.getOrigin().subtract(Point3.ORIGIN);
    double a = transformedRay.getDirection().dot(transformedRay.getDirection());
    double b = 2.0 * oc.dot(transformedRay.getDirection());
    double c = oc.dot(oc) - radius * radius;
    
    double discriminant = b * b - 4 * a * c;
    
    if (discriminant >= 0) {
      double sqrtDisc = Math.sqrt(discriminant);
      double t1 = (-b - sqrtDisc) / (2 * a);
      double t2 = (-b + sqrtDisc) / (2 * a);
      
      if (t2 > Ray.EPSILON) {
        Point3 p1 = transformedRay.pointAtParameter(t1);
        Point3 p2 = transformedRay.pointAtParameter(t2);
        
        // Transform normals back to world space
        Vector3 n1 = getNormalAt(p1).transformNormal(inverseTransform).normalize();
        Vector3 n2 = getNormalAt(p2).transformNormal(inverseTransform).normalize();
        
        Intersection in = new Intersection(p1, n1, t1, this);
        Intersection out = new Intersection(p2, n2, t2, this);
        
        intervals.add(new IntersectionInterval(t1, t2, in, out));
      }
    }
    
    return intervals;
  }
  
  // Old intersect method
  @Override
  public double intersect(Ray ray) {
    List<IntersectionInterval> intervals = intersectAll(ray);
    return intervals.isEmpty() ? Double.POSITIVE_INFINITY : intervals.get(0).tIn;
  }
  
  // Get normal at point - basit sphere normal
  @Override
  public Vector3 getNormalAt(Point3 point) {
    return point.subtract(Point3.ORIGIN).normalize();
  }
  
  // Material methods
  @Override
  public void setMaterial(Material material) {
    this.material = material;
  }
  
  @Override
  public Material getMaterial() {
    return material;
  }
  
  // Transform methods
  @Override
  public void setTransform(Matrix4 transform) {
    this.transform = transform;
    this.inverseTransform = transform.inverse();
  }
  
  @Override
  public Matrix4 getTransform() {
    return transform;
  }
  
  @Override
  public Matrix4 getInverseTransform() {
    return inverseTransform;
  }
  
  // Getter for radius parameter
  public double getRadius() {
    return radius;
  }
  
  @Override
  public String toString() {
    return "TestShape[radius=" + radius + "]";
  }
  
}
