package net.elena.murat.shape;

import java.util.List;

import net.elena.murat.math.*;
import net.elena.murat.material.Material;

import static net.elena.murat.math.Vector3.*;

public class Rectangle3D implements EMShape {
  private final Point3 p1, p2;
  private final float thickness;
  private Material material;
  private Matrix4 transform = Matrix4.identity();
  private Matrix4 inverseTransform = Matrix4.identity();
  
  public Rectangle3D(Point3 p1, Point3 p2, float thickness) {
    this.p1 = p1;
    this.p2 = p2;
    this.thickness = thickness;
  }
  
  @Override
  public double intersect(Ray ray) {
    // Ray to local space
    Ray localRay = new Ray(
      inverseTransform.transformPoint(ray.getOrigin()),
      inverseTransform.transformVector(ray.getDirection())
    );
    
    // plane: normal = (0,0,1)
    if (Math.abs(localRay.getDirection().z) < Ray.EPSILON) return -1;
    
    double t = -localRay.getOrigin().z / localRay.getDirection().z;
    if (t < Ray.EPSILON) return -1;
    
    Point3 localHit = localRay.pointAtParameter(t);
    if (localHit.x < Math.min(p1.x, p2.x) || localHit.x > Math.max(p1.x, p2.x) ||
      localHit.y < Math.min(p1.y, p2.y) || localHit.y > Math.max(p1.y, p2.y)) {
      return -1;
    }
    
    return t;
  }
  
  /**
   * Calculates all intersection intervals between a ray and this Rectangle3D.
   * Since the rectangle is treated as a thin surface, the entry and exit points are the same.
   * @param ray The ray to test, in world coordinates.
   * @return A list containing a single degenerate interval if intersected, otherwise empty list.
   */
  @Override
  public List<IntersectionInterval> intersectAll(Ray ray) {
    // 1. Transform the ray into local space
    Ray localRay = new Ray(
      inverseTransform.transformPoint(ray.getOrigin()),
      inverseTransform.transformVector(ray.getDirection()).normalize()
    );
    
    // 2. Check for intersection with the plane (z=0 in local space)
    if (Math.abs(localRay.getDirection().z) < Ray.EPSILON) {
      return java.util.Collections.emptyList(); // Parallel to the plane
    }
    
    double t = -localRay.getOrigin().z / localRay.getDirection().z;
    if (t <= Ray.EPSILON) {
      return java.util.Collections.emptyList(); // Behind the ray
    }
    
    // 3. Check if the hit point is within the rectangle bounds
    Point3 localHit = localRay.pointAtParameter(t);
    double minX = Math.min(p1.x, p2.x);
    double maxX = Math.max(p1.x, p2.x);
    double minY = Math.min(p1.y, p2.y);
    double maxY = Math.max(p1.y, p2.y);
    
    if (localHit.x < minX || localHit.x > maxX || localHit.y < minY || localHit.y > maxY) {
      return java.util.Collections.emptyList();
    }
    
    // 4. Create a single degenerate interval
    Point3 worldHit = ray.pointAtParameter(t);
    Vector3 worldNormal = getNormalAt(worldHit);
    Intersection hit = new Intersection(worldHit, worldNormal, t, this);
    
    IntersectionInterval interval = IntersectionInterval.point(t, hit);
    return java.util.Arrays.asList(interval);
  }
  
  @Override
  public Vector3 getNormalAt(Point3 worldPoint) {
    Point3 localPoint = inverseTransform.transformPoint(worldPoint);
    Vector3 localNormal = new Vector3(0, 0, 1);
    return inverseTransform.transpose().transformVector(localNormal).normalize();
  }
  
  @Override
  public void setTransform(Matrix4 transform) {
    this.transform = transform;
    this.inverseTransform = transform.inverse();
  }
  
  @Override public Matrix4 getTransform() { return transform; }
  @Override public Matrix4 getInverseTransform() { return inverseTransform; }
  @Override public Material getMaterial() { return material; }
  @Override public void setMaterial(Material material) { this.material = material; }
}
