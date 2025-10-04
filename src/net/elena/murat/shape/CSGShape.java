package net.elena.murat.shape;

import java.util.List;

import net.elena.murat.math.*;
import net.elena.murat.material.Material;

public abstract class CSGShape implements EMShape {
  protected final EMShape left;
  protected final EMShape right;
  
  private Matrix4 transform;
  private Matrix4 inverseTransform;
  
  public CSGShape(EMShape left, EMShape right) {
    this.left = left;
    this.right = right;
    this.transform = Matrix4.identity();
    this.inverseTransform = Matrix4.identity();
  }
  
  @Override
  public void setTransform(Matrix4 transform) {
    this.transform = new Matrix4(transform);
    this.inverseTransform = transform.inverse();
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
  public List<IntersectionInterval> intersectAll(Ray ray) {
    // 1. Ray to CSG's local space
    Point3 localOrigin = inverseTransform.transformPoint(ray.getOrigin());
    Vector3 localDirection = inverseTransform.transformVector(ray.getDirection()).normalize();
    Ray localRay = new Ray(localOrigin, localDirection);
    
    // 2. Intersects of inner shapes
    List<IntersectionInterval> a = left.intersectAll(localRay);
    List<IntersectionInterval> b = right.intersectAll(localRay);
    
    // 3. Combine
    return combine(a, b);
  }
  
  protected abstract List<IntersectionInterval> combine(
    List<IntersectionInterval> a,
    List<IntersectionInterval> b
  );
  
  @Override
  public double intersect(Ray ray) {
    List<IntersectionInterval> intervals = intersectAll(ray);
    return intervals.isEmpty() ? -1 : intervals.get(0).tIn;
  }
  
  /**
   * Calculates the normal vector at a given point on the CSG shape's surface in WORLD coordinates.
   * This is a placeholder implementation that uses the left operand's normal.
   * For accurate results, CSG operations should calculate normals based on the surface hit.
   * @param worldPoint The point on the CSG shape's surface in world coordinates.
   * @return The normalized normal vector at that point in world coordinates.
   */
  @Override
  public Vector3 getNormalAt(Point3 worldPoint) {
    // 1. Transform the world point to the CSG operation's local space
    Point3 localPoint = inverseTransform.transformPoint(worldPoint);
    
    // 2. Determine which operand was hit and use its normal
    // This is a simple heuristic: check the distance to the surfaces of left and right
    // A more accurate method would be to know which interval was hit in intersectAll
    Vector3 normalA = left.getNormalAt(localPoint);
    Vector3 normalB = right.getNormalAt(localPoint);
    
    // Calculate the distance from the localPoint to the surfaces of A and B
    // This is a crude approximation using the dot product with the normal
    // A point is "on" a surface if it's very close to it
    double distA = Math.abs(localPoint.subtract(left.getTransform().transformPoint(new Point3(0,0,0))).dot(normalA));
    double distB = Math.abs(localPoint.subtract(right.getTransform().transformPoint(new Point3(0,0,0))).dot(normalB));
    
    Vector3 localNormal;
    if (distA < distB) {
      // Hit on A's surface
      localNormal = normalA;
      } else {
      // Hit on B's surface
      // For DifferenceCSG, if we're on B's surface, the normal should point inwards
      // because B is being subtracted
      if (this instanceof DifferenceCSG) {
        localNormal = normalB.negate();
        } else {
        localNormal = normalB;
      }
    }
    
    // 3. Transform the local normal back to world space
    Matrix4 normalTransformMatrix = this.inverseTransform.inverseTransposeForNormal();
    return normalTransformMatrix.transformVector(localNormal).normalize();
  }
  
  @Override
  public Material getMaterial() {
    return left.getMaterial();
  }
  
  @Override
  public void setMaterial(Material material) {
    left.setMaterial(material);
    right.setMaterial(material);
  }
  
}
