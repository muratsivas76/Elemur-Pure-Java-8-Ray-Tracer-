package net.elena.murat.shape;

import java.util.List;

import net.elena.murat.math.*;
import net.elena.murat.material.Material;

/**
 * Finite cylinder aligned along the Y-axis in its local coordinate system.
 * Implements EMShape interface, now supporting Matrix4 transformations.
 */
public class Cylinder implements EMShape {
  private Material material;
  
  // Cylinder's definition in its LOCAL coordinate system.
  // Canonical cylinder: base center at (0,0,0), radius 'localRadius', height 'localHeight',
  // aligned along the positive Y-axis (localAxis = 0,1,0).
  private final Point3 localBaseCenter;
  private final double localRadius;
  private final double localHeight;
  private final Vector3 localAxis; // Always (0,1,0) in local space
  
  // Transformation matrices
  private Matrix4 transform;        // Local to World transformation matrix
  private Matrix4 inverseTransform; // World to Local transformation matrix
  
  // Using a class-level EPSILON for consistency
  private static final double EPSILON = 1e-5;
  
  /**
   * Constructs a cylinder with a given radius and height.
   * The cylinder's base is implicitly at (0,0,0) in its LOCAL coordinate system,
   * and it extends along the positive Y-axis up to (0, height, 0).
   * @param radius The radius of the cylinder.
   * @param height The height of the cylinder.
   */
  public Cylinder(double radius, double height) {
    this.localBaseCenter = new Point3(0, 0, 0);
    this.localRadius = radius;
    this.localHeight = height;
    this.localAxis = new Vector3(0, 1, 0);
    this.transform = new Matrix4();
    this.inverseTransform = new Matrix4();
  }
  
  /**
   * NEW CONSTRUCTOR - Creates a cylinder between two points in world space
   */
  public Cylinder(Point3 startPoint, Point3 endPoint, double radius, double height) {
    this(radius, height); // Call basic constructor first
    
    Vector3 direction = endPoint.subtract(startPoint);
    double actualHeight = direction.length();
    direction = direction.normalize();
    
    Vector3 midpoint = startPoint.toVector().add(endPoint.toVector()).scale(0.5);
    
    // Axis-angle rotation calculation
    Vector3 yAxis = new Vector3(0, 1, 0);
    Vector3 rotationAxis = yAxis.cross(direction);
    double rotationAngle = Math.acos(yAxis.dot(direction));
    
    // Create rotation matrix (Rodrigues' formula)
    double c = Math.cos(rotationAngle);
    double s = Math.sin(rotationAngle);
    double t = 1 - c;
    double x = rotationAxis.x;
    double y = rotationAxis.y;
    double z = rotationAxis.z;
    
    Matrix4 rotation = new Matrix4(
      t*x*x + c,    t*x*y - s*z,  t*x*z + s*y,  0,
      t*x*y + s*z,  t*y*y + c,    t*y*z - s*x,  0,
      t*x*z - s*y,  t*y*z + s*x,  t*z*z + c,    0,
      0,            0,            0,            1
    );
    
    Matrix4 translation = Matrix4.translate(midpoint.x, midpoint.y, midpoint.z);
    Matrix4 scale = Matrix4.scale(1, actualHeight/localHeight, 1);
    
    this.setTransform(translation.multiply(rotation).multiply(scale));
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
  public Vector3 getNormalAt(Point3 worldPoint) {
    Point3 localHitPoint = inverseTransform.transformPoint(worldPoint);
    Vector3 localNormal;
    
    double heightFromLocalBase = localHitPoint.y - localBaseCenter.y;
    
    if (Math.abs(heightFromLocalBase - localHeight) < Ray.EPSILON) {
      localNormal = localAxis;
      } else if (Math.abs(heightFromLocalBase) < Ray.EPSILON) {
      localNormal = localAxis.negate();
      } else {
      Point3 localProjectionOnAxis = new Point3(localBaseCenter.x, localHitPoint.y, localBaseCenter.z);
      localNormal = localHitPoint.subtract(localProjectionOnAxis).normalize();
    }
    
    Matrix4 normalTransformMatrix = this.inverseTransform.inverseTransposeForNormal();
    return normalTransformMatrix.transformVector(localNormal).normalize();
  }
  
  @Override
  public double intersect(Ray ray) {
    Point3 localOrigin = inverseTransform.transformPoint(ray.getOrigin());
    Vector3 localDirection = inverseTransform.transformVector(ray.getDirection()).normalize();
    
    double ox = localOrigin.x;
    double oy = localOrigin.y;
    double oz = localOrigin.z;
    double dx = localDirection.x;
    double dy = localDirection.y;
    double dz = localDirection.z;
    
    // Side surface intersection
    double a = dx * dx + dz * dz;
    double b = 2 * (ox * dx + oz * dz);
    double c = ox * ox + oz * oz - localRadius * localRadius;
    
    double discriminant = b * b - 4 * a * c;
    double tClosest = -1;
    
    if (discriminant >= Ray.EPSILON) {
      double sqrtDisc = Math.sqrt(discriminant);
      double t0 = (-b - sqrtDisc) / (2 * a);
      double t1 = (-b + sqrtDisc) / (2 * a);
      
      if (t0 > t1) {
        double temp = t0;
        t0 = t1;
        t1 = temp;
      }
      
      if (t0 > Ray.EPSILON) {
        double y0 = oy + t0 * dy;
        if (y0 >= -Ray.EPSILON && y0 <= localHeight + Ray.EPSILON) {
          tClosest = t0;
        }
      }
      
      if (t1 > Ray.EPSILON && (tClosest == -1 || t1 < tClosest)) {
        double y1 = oy + t1 * dy;
        if (y1 >= -Ray.EPSILON && y1 <= localHeight + Ray.EPSILON) {
          tClosest = t1;
        }
      }
    }
    
    // Cap intersections
    if (Math.abs(dy) > Ray.EPSILON) {
      // Bottom cap
      double tBottom = -oy / dy;
      if (tBottom > Ray.EPSILON) {
        double ix = ox + tBottom * dx;
        double iz = oz + tBottom * dz;
        if (ix * ix + iz * iz <= localRadius * localRadius + Ray.EPSILON) {
          if (tClosest == -1 || tBottom < tClosest) {
            tClosest = tBottom;
          }
        }
      }
      
      // Top cap
      double tTop = (localHeight - oy) / dy;
      if (tTop > Ray.EPSILON) {
        double ix = ox + tTop * dx;
        double iz = oz + tTop * dz;
        if (ix * ix + iz * iz <= localRadius * localRadius + Ray.EPSILON) {
          if (tClosest == -1 || tTop < tClosest) {
            tClosest = tTop;
          }
        }
      }
    }
    
    return tClosest;
  }
  
  /**
   * Calculates all intersection intervals between a ray and this finite cylinder.
   * The ray is transformed into the cylinder's local space for calculation.
   * The method checks for intersections with the side, top, and bottom caps.
   * All valid intersections are collected, sorted by t, and paired into intervals.
   * @param ray The ray to test, in world coordinates.
   * @return A list of IntersectionInterval objects. Empty if no intersections.
   */
  @Override
  public List<IntersectionInterval> intersectAll(Ray ray) {
    // 1. Transform the ray into the cylinder's local coordinate system
    Point3 localOrigin = inverseTransform.transformPoint(ray.getOrigin());
    Vector3 localDirection = inverseTransform.transformVector(ray.getDirection()).normalize();
    
    double ox = localOrigin.x;
    double oy = localOrigin.y;
    double oz = localOrigin.z;
    double dx = localDirection.x;
    double dy = localDirection.y;
    double dz = localDirection.z;
    
    // List to store all intersection t values and their corresponding data
    List<Intersection> hits = new java.util.ArrayList<>();
    
    // 2. Side surface intersection
    double a = dx * dx + dz * dz;
    double b = 2 * (ox * dx + oz * dz);
    double c = ox * ox + oz * oz - localRadius * localRadius;
    
    if (a > Ray.EPSILON) { // Avoid division by zero
      double discriminant = b * b - 4 * a * c;
      if (discriminant >= 0) {
        double sqrtDisc = Math.sqrt(discriminant);
        double t0 = (-b - sqrtDisc) / (2 * a);
        double t1 = (-b + sqrtDisc) / (2 * a);
        
        // Check if the intersection points are on the cylinder's height
        double y0 = oy + t0 * dy;
        double y1 = oy + t1 * dy;
        
        if (t0 > Ray.EPSILON && y0 >= -Ray.EPSILON && y0 <= localHeight + Ray.EPSILON) {
          Point3 point = ray.pointAtParameter(t0);
          Vector3 normal = getNormalAt(point);
          hits.add(new Intersection(point, normal, t0, this));
        }
        if (t1 > Ray.EPSILON && y1 >= -Ray.EPSILON && y1 <= localHeight + Ray.EPSILON) {
          Point3 point = ray.pointAtParameter(t1);
          Vector3 normal = getNormalAt(point);
          hits.add(new Intersection(point, normal, t1, this));
        }
      }
    }
    
    // 3. Bottom cap intersection
    if (Math.abs(dy) > Ray.EPSILON) {
      double tBottom = -oy / dy;
      if (tBottom > Ray.EPSILON) {
        double ix = ox + tBottom * dx;
        double iz = oz + tBottom * dz;
        if (ix * ix + iz * iz <= localRadius * localRadius + Ray.EPSILON) {
          Point3 point = ray.pointAtParameter(tBottom);
          Vector3 normal = getNormalAt(point);
          hits.add(new Intersection(point, normal, tBottom, this));
        }
      }
    }
    
    // 4. Top cap intersection
    if (Math.abs(dy) > Ray.EPSILON) {
      double tTop = (localHeight - oy) / dy;
      if (tTop > Ray.EPSILON) {
        double ix = ox + tTop * dx;
        double iz = oz + tTop * dz;
        if (ix * ix + iz * iz <= localRadius * localRadius + Ray.EPSILON) {
          Point3 point = ray.pointAtParameter(tTop);
          Vector3 normal = getNormalAt(point);
          hits.add(new Intersection(point, normal, tTop, this));
        }
      }
    }
    
    // 5. Sort intersections by t value
    java.util.Collections.sort(hits, (i1, i2) -> Double.compare(i1.getT(), i2.getT()));
    
    // 6. Pair intersections into intervals (in-out pairs)
    List<IntersectionInterval> intervals = new java.util.ArrayList<>();
    for (int i = 0; i < hits.size() - 1; i += 2) {
      Intersection in = hits.get(i);
      Intersection out = hits.get(i + 1);
      intervals.add(new IntersectionInterval(in.getT(), out.getT(), in, out));
    }
    
    return intervals;
  }
  
}
