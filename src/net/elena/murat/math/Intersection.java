package net.elena.murat.math;

import net.elena.murat.math.Point3;
import net.elena.murat.math.Vector3;
import net.elena.murat.shape.EMShape;

public class Intersection {
  private final Point3 point;     // Intersection point
  private final Vector3 normal;   // Normal vector at intersection point
  private final double distance;  // Distance from ray origin to intersection point (t parameter)
  private final EMShape shape;    // Intersected object
  
  public Intersection(Point3 point, Vector3 normal, double distance, EMShape shape) {
    this.point = point;
    this.normal = normal;
    this.distance = distance;
    this.shape = shape;
  }
  
  public Point3 getPoint() {
    return point;
  }
  
  public Vector3 getNormal() {
    return normal;
  }
  
  public double getDistance() {
    return distance;
  }
  
  /**
   * Returns the distance of the intersection point from the ray origin ('t' parameter).
   * Returns the same value as getDistance().
   * @return Intersection distance (t value).
   */
  public double getT() {
    return distance;
  }
  
  public EMShape getShape() {
    return shape;
  }
  
  @Override
  public String toString() {
    return "Intersection{" +
    "point=" + point +
    ", normal=" + normal +
    ", distance=" + distance +
    ", shape=" + (shape != null ? shape.getClass().getSimpleName() : "null") +
    '}';
  }
  
}
