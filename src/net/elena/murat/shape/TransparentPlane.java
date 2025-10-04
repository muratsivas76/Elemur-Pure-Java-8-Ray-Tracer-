package net.elena.murat.shape;

import net.elena.murat.math.*;
import net.elena.murat.material.Material;

import java.util.List;

public class TransparentPlane implements EMShape {
  private final double width, height, thickness;
  private Material material;
  private Matrix4 transform = Matrix4.identity();
  private Matrix4 inverseTransform = Matrix4.identity();
  
  private static final Vector3 VEC_Y = new Vector3(0, 1, 0);
  private static final Vector3 VEC_X = new Vector3(1, 0, 0);
  
  public TransparentPlane(Point3 pointOnPlane, Vector3 normal, double thickness) {
    this.thickness = thickness;
    this.width = 2000.0;
    this.height = 2000.0;
    
    normal = normal.normalize();
    
    // Basis vectors
    Vector3 up = Math.abs(normal.dot(VEC_Y)) > 0.99 ? VEC_X : VEC_Y;
    Vector3 right = normal.cross(up).normalize();
    Vector3 forward = right.cross(normal).normalize();
    
    // Build transformation matrix: rotation + translation
    Matrix4 rotTrans = new Matrix4(
      right.x,    forward.x,    normal.x,    pointOnPlane.x,
      right.y,    forward.y,    normal.y,    pointOnPlane.y,
      right.z,    forward.z,    normal.z,    pointOnPlane.z,
      0,          0,            0,           1
    );
    
    this.setTransform(rotTrans);
  }
  
  @Override
  public void setMaterial(Material material) {
    this.material = material;
  }
  
  @Override
  public Material getMaterial() {
    return this.material;
  }
  
  @Override
  public void setTransform(Matrix4 transform) {
    this.transform = transform;
    this.inverseTransform = transform.inverse();
    if (this.inverseTransform == null) {
      System.err.println("TransparentPlane: Non-invertible transform!");
      this.inverseTransform = Matrix4.identity();
    }
  }
  
  @Override
  public Matrix4 getTransform() {
    return this.transform;
  }
  
  @Override
  public Matrix4 getInverseTransform() {
    return this.inverseTransform;
  }
  
  @Override
  public double intersect(Ray ray) {
    // Transform ray to local space
    Point3 localOrigin = inverseTransform.transformPoint(ray.getOrigin());
    Vector3 localDir = inverseTransform.transformVector(ray.getDirection()).normalize();
    Ray localRay = new Ray(localOrigin, localDir);
    
    // Local box bounds
    double w = width / 2.0;
    double h = height / 2.0;
    double t = thickness / 2.0;
    
    // Slab method
    double tmin = -Double.MAX_VALUE;
    double tmax = Double.MAX_VALUE;
    
    // X
    if (Math.abs(localRay.getDirection().x) > Ray.EPSILON) {
      double tx1 = (-w - localRay.getOrigin().x) / localRay.getDirection().x;
      double tx2 = (w - localRay.getOrigin().x) / localRay.getDirection().x;
      tmin = Math.max(tmin, Math.min(tx1, tx2));
      tmax = Math.min(tmax, Math.max(tx1, tx2));
      } else if (localRay.getOrigin().x < -w || localRay.getOrigin().x > w) {
      return -1;
    }
    
    // Y
    if (Math.abs(localRay.getDirection().y) > Ray.EPSILON) {
      double ty1 = (-h - localRay.getOrigin().y) / localRay.getDirection().y;
      double ty2 = (h - localRay.getOrigin().y) / localRay.getDirection().y;
      tmin = Math.max(tmin, Math.min(ty1, ty2));
      tmax = Math.min(tmax, Math.max(ty1, ty2));
      } else if (localRay.getOrigin().y < -h || localRay.getOrigin().y > h) {
      return -1;
    }
    
    // Z
    if (Math.abs(localRay.getDirection().z) > Ray.EPSILON) {
      double tz1 = (-t - localRay.getOrigin().z) / localRay.getDirection().z;
      double tz2 = (t - localRay.getOrigin().z) / localRay.getDirection().z;
      tmin = Math.max(tmin, Math.min(tz1, tz2));
      tmax = Math.min(tmax, Math.max(tz1, tz2));
      } else if (localRay.getOrigin().z < -t || localRay.getOrigin().z > t) {
      return -1;
    }
    
    if (tmax >= tmin && tmax > Ray.EPSILON) {
      return tmin > Ray.EPSILON ? tmin : tmax;
    }
    return -1;
  }
  
  @Override
  public List<IntersectionInterval> intersectAll(Ray ray) {
    double t = intersect(ray);
    if (t <= Ray.EPSILON) return java.util.Collections.emptyList();
    
    Point3 hit = ray.pointAtParameter(t);
    Vector3 norm = getNormalAt(hit);
    Intersection inter = new Intersection(hit, norm, t, this);
    return java.util.Arrays.asList(IntersectionInterval.point(t, inter));
  }
  
  @Override
  public Vector3 getNormalAt(Point3 worldPoint) {
    Point3 local = inverseTransform.transformPoint(worldPoint);
    double w = width / 2.0;
    double h = height / 2.0;
    double t = thickness / 2.0;
    
    if (Math.abs(local.x) > w - Ray.EPSILON) {
      return transform.transformVector(new Vector3(local.x > 0 ? 1 : -1, 0, 0)).normalize();
      } else if (Math.abs(local.y) > h - Ray.EPSILON) {
      return transform.transformVector(new Vector3(0, local.y > 0 ? 1 : -1, 0)).normalize();
      } else if (Math.abs(local.z) > t - Ray.EPSILON) {
      return transform.transformVector(new Vector3(0, 0, local.z > 0 ? 1 : -1)).normalize();
    }
    
    return transform.transformVector(new Vector3(0, 0, 1)).normalize();
  }
  
}
