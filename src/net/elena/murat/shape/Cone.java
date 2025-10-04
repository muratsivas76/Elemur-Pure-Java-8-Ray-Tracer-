package net.elena.murat.shape;

import java.util.List;

import net.elena.murat.math.*;
import net.elena.murat.material.Material;

/**
 * Cone class represents a finite cone in 3D space, aligned along a local axis.
 * Implements EMShape interface, now supporting Matrix4 transformations.
 */
public class Cone implements EMShape {
  private Material material;
  
  // Cone's definition in its local space.
  // Assuming a canonical cone: baseCenter at (0,0,0), axis along Y-axis (0,1,0),
  // and apex at (0, height, 0).
  private final Point3 localBaseCenter;
  private final double localRadius;
  private final double localHeight;
  private final Vector3 localAxis; // Canonical axis (0,1,0)
  private final Point3 localApex;  // Apex derived from localBaseCenter and localHeight
  
  // Transformation matrices
  private Matrix4 transform;        // Local to World transformation matrix
  private Matrix4 inverseTransform; // World to Local transformation matrix
  
  /**
   * Constructs a cone with a base center, radius, and height in its LOCAL coordinate system.
   * By default, the cone's base is at (0,0,0) and its axis extends along the Y-axis.
   * @param radius The radius of the cone's base.
   * @param height The height of the cone.
   */
  public Cone(double radius, double height) {
    // Define the cone in a canonical local space
    this.localBaseCenter = new Point3(0, 0, 0); // Base at origin
    this.localRadius = radius;
    this.localHeight = height;
    this.localAxis = new Vector3(0, 1, 0); // Axis along positive Y
    this.localApex = localBaseCenter.add(this.localAxis.scale(localHeight)); // Apex at (0, height, 0)
    
    // Initialize with identity transform by default
    this.transform = new Matrix4();
    this.inverseTransform = new Matrix4();
  }
  
  @Override
  public Material getMaterial() {
    return material;
  }
  
  @Override
  public void setMaterial(Material material) {
    this.material = material;
  }
  
  // --- New Methods for Transformation from EMShape interface ---
  
  /**
   * Sets the transformation matrix that converts points/vectors from the cone's
   * local space to world space. When this is set, the inverse transform is also computed.
   * @param transform The 4x4 transformation matrix.
   */
  @Override
  public void setTransform(Matrix4 transform) {
    this.transform = transform;
    this.inverseTransform = transform.inverse(); // Pre-compute inverse for efficiency
  }
  
  /**
   * Returns the transformation matrix that converts points/vectors from the cone's
   * local space to world space.
   * @return The 4x4 transformation matrix.
   */
  @Override
  public Matrix4 getTransform() {
    return this.transform;
  }
  
  /**
   * Returns the inverse of the transformation matrix, which converts points/vectors
   * from world space back to the cone's local space.
   * @return The 4x4 inverse transformation matrix.
   */
  @Override
  public Matrix4 getInverseTransform() {
    return this.inverseTransform;
  }
  
  /**
   * Calculates the normal vector at a given point on the cone's surface in WORLD coordinates.
   * The normal points outwards. This involves transforming the hit point to local space,
   * calculating the local normal, and then transforming it back to world space using the
   * inverse transpose of the model matrix.
   * @param worldPoint The point on the cone's surface in world coordinates.
   * @return The normalized normal vector at that point in world coordinates.
   */
  @Override
  public Vector3 getNormalAt(Point3 worldPoint) {
    // 1. Transform the world hit point to local space
    Point3 localHitPoint = inverseTransform.transformPoint(worldPoint);
    
    Vector3 localNormal;
    
    // Check if the hit point is on the base of the cone in local space
    // localBaseCenter is (0,0,0) and localAxis is (0,1,0)
    // So, base is at localHitPoint.y = 0
    if (Math.abs(localHitPoint.y - localBaseCenter.y) < Ray.EPSILON) {
      // Hit on the base
      // The base normal is simply the negative local axis (pointing downwards)
      localNormal = localAxis.negate();
      } else {
      // Hit on the lateral surface
      // Vector from local apex to local hit point
      Vector3 V = localHitPoint.subtract(localApex);
      
      // Projection of V onto the local axis
      double m = V.dot(localAxis);
      
      // Radius-to-height ratio squared
      double k_tan_sq = (localRadius * localRadius) / (localHeight * localHeight);
      
      // Calculate local normal using the general cone normal formula
      // N = (P_local - Apex_local) - ( (P_local - Apex_local) . Axis_local ) * (1 + k_tan_sq) * Axis_local
      // Simplified: Normal component perpendicular to axis is (P_local - Proj_on_Axis)
      // The component along the axis is -k_tan_sq * (dist_along_axis_from_apex) * Axis
      
      // Point on axis corresponding to the hit point's height
      Point3 projectionOnAxis = localApex.add(localAxis.scale(m / (1 + k_tan_sq))); // Corrected projection calculation for normal
      
      localNormal = localHitPoint.subtract(projectionOnAxis).normalize();
      
      // Ensure the normal points outwards. If the local axis is (0,1,0) and apex is above base,
      // the y-component of the normal should generally be positive or 0 for points near base
      // and negative for points on the side pointing "downwards" from apex's perspective.
      // A common heuristic is to check the dot product with a vector from apex to hit point
      // or simply ensure it faces away from the cone's interior.
      // For standard cone, (P_x, P_y, P_z) transformed to be (x,y,z) on cone centered at origin with apex on y axis:
      // N = (x, -R/H * sqrt(x^2+z^2), z) for cone open downwards
      // Or (x, R/H * sqrt(x^2+z^2), z) for cone open upwards.
      // Our formula (P - Proj) should usually give outward normal.
      // Let's ensure it points "upwards" relative to the cone's center in local space if it's pointing inwards.
      // The provided formula N = V - (1 + k) * m * axis is for a cone whose axis points from base to apex.
      // Our localAxis points from base to apex. So, V = P - Apex.
      // A more robust normal calculation for cone lateral surface:
      // Vector from apex to current point: V_apex_to_point = localHitPoint.subtract(localApex);
      // Height along axis: double current_height_along_axis = V_apex_to_point.dot(localAxis);
      // Radius at this height: double current_radius = localRadius * (1.0 - current_height_along_axis / localHeight);
      // Point on axis at this height: Point3 axis_point = localApex.add(localAxis.scale(current_height_along_axis));
      // Vector from axis to hit point: Vector3 radial_vec = localHitPoint.subtract(axis_point);
      // Normal = (radial_vec.normalize()).add(localAxis.scale(localRadius / localHeight)).normalize(); // This is for cone pointing upwards
      
      // Simpler calculation for Y-axis cone with apex at (0, height, 0) and base at (0,0,0):
      // The local normal points radially outwards and slightly upwards/downwards.
      // N_x = localHitPoint.x
      // N_z = localHitPoint.z
      // N_y = (localHitPoint.y - localApex.y) * (localRadius / localHeight) * (localRadius / localHeight) / localHitPoint.y.length()
      // No. It's:
      // Nx = (localHitPoint.x / localRadius) * localHeight
      // Ny = (localRadius / localHeight) * (localHitPoint.y - localApex.y) * -1.0
      // Nz = (localHitPoint.z / localRadius) * localHeight
      
      // A standard cone normal for a point (x,y,z) on the lateral surface of a cone with apex at (0,H,0) and base at (0,0,0)
      // is (x, -R/H * sqrt(x^2+z^2), z) normalized.
      // This simplifies to (x, -R/H * (current_radius at y), z)
      // Or, using the point-projection method:
      double r_sq_at_y = localHitPoint.subtract(localApex).lengthSquared() - Math.pow(localHitPoint.subtract(localApex).dot(localAxis), 2);
      double current_radius = Math.sqrt(r_sq_at_y);
      
      Vector3 vecFromApex = localHitPoint.subtract(localApex);
      Vector3 radialDir = vecFromApex.subtract(localAxis.scale(vecFromApex.dot(localAxis))).normalize();
      
      double cosAlpha = localHeight / Math.sqrt(localHeight * localHeight + localRadius * localRadius);
      double sinAlpha = localRadius / Math.sqrt(localHeight * localHeight + localRadius * localRadius);
      
      // Normal is composed of a radial component and an axial component
      // For a cone pointing along +Y, radial component is horizontal.
      // Axial component is typically upwards (negative if apex is above base and normal points inwards)
      localNormal = radialDir.add(localAxis.scale(cosAlpha / sinAlpha)).normalize();
      
      // If the cone is defined with apex at (0, height, 0) and base at (0,0,0),
      // and localAxis (0,1,0), then the lateral normal's Y component should be negative (pointing away from Y axis, "downwards")
      // Ensure the normal points "outwards" relative to the Y-axis projection:
      if (localNormal.dot(localAxis) > 0) { // If normal points "upwards" towards apex, flip it
        localNormal = localNormal.negate();
      }
    }
    
    // 2. Transform the local normal back to world space
    // Normals transform with the inverse transpose of the model matrix
    //Matrix4 normalTransformMatrix = this.inverseTransform.transpose(); // M_normal = (M^-1)^T
    Matrix4 normalTransformMatrix = this.inverseTransform.inverseTransposeForNormal(); // Normaller için yeni metod
    return normalTransformMatrix.transformVector(localNormal).normalize();
  }
  
  /**
   * Finds the intersection of a ray with the cone in its local coordinate system.
   * Returns the 't' value for the closest intersection point, or Double.POSITIVE_INFINITY if no intersection.
   * The ray is first transformed into the cone's local coordinate system.
   */
  @Override
  public double intersect(Ray ray) {
    // 1. Transform the ray into the cone's local coordinate system
    Point3 localOrigin = inverseTransform.transformPoint(ray.getOrigin());
    Vector3 localDirection = inverseTransform.transformVector(ray.getDirection()).normalize();
    
    // Create a local ray
    Ray localRay = new Ray(localOrigin, localDirection);
    
    // Parameters for cone equation in canonical form (base at origin, apex at (0, height, 0), axis along Y)
    // (x^2 + z^2) * H^2 = R^2 * (H - y)^2
    // Or if apex is origin: x^2 + z^2 = (R/H * y)^2 for y from 0 to H
    // Our cone has apex at (0, localHeight, 0) and base at (0,0,0).
    // The equation is x^2 + z^2 = (localRadius / localHeight)^2 * (localHeight - y)^2
    // Let k = (localRadius / localHeight)^2
    // x^2 + z^2 - k * (localHeight - y)^2 = 0
    
    // Ray in local space: P(t) = O_local + t * D_local
    // P_x = O_x + t*D_x
    // P_y = O_y + t*D_y
    // P_z = O_z + t*D_z
    
    // Substitute into cone equation:
    // (O_x + t*D_x)^2 + (O_z + t*D_z)^2 - k * (localHeight - (O_y + t*D_y))^2 = 0
    // (O_x + t*D_x)^2 + (O_z + t*D_z)^2 - k * ( (localHeight - O_y) - t*D_y )^2 = 0
    
    double k_cone = (localRadius * localRadius) / (localHeight * localHeight);
    
    // Coefficients for quadratic equation At^2 + Bt + C = 0
    // A = D_x^2 + D_z^2 - k * D_y^2
    double a = (localRay.getDirection().x * localRay.getDirection().x) +
    (localRay.getDirection().z * localRay.getDirection().z) -
    k_cone * (localRay.getDirection().y * localRay.getDirection().y);
    
    // B = 2 * (O_x*D_x + O_z*D_z - k * D_y * (O_y - localHeight))
    double b = 2 * ((localRay.getOrigin().x * localRay.getDirection().x) +
      (localRay.getOrigin().z * localRay.getDirection().z) -
    k_cone * localRay.getDirection().y * (localRay.getOrigin().y - localHeight));
    
    // C = O_x^2 + O_z^2 - k * (O_y - localHeight)^2
    double c = (localRay.getOrigin().x * localRay.getOrigin().x) +
    (localRay.getOrigin().z * localRay.getOrigin().z) -
    k_cone * Math.pow(localRay.getOrigin().y - localHeight, 2);
    
    double discriminant = b * b - 4 * a * c;
    
    double tLateral = Double.POSITIVE_INFINITY; // Intersection with lateral surface
    
    if (discriminant >= Ray.EPSILON) {
      double sqrtD = Math.sqrt(discriminant);
      double t0 = (-b - sqrtD) / (2 * a);
      double t1 = (-b + sqrtD) / (2 * a);
      
      if (t0 > t1) { // Ensure t0 is the smaller positive root
        double temp = t0;
        t0 = t1;
        t1 = temp;
      }
      
      // Check if t0 is a valid intersection on the lateral surface
      if (t0 > Ray.EPSILON) {
        Point3 hitPoint0 = localRay.pointAtParameter(t0);
        // Check if hitPoint0 is within the cone's height bounds [0, localHeight]
        boolean isWithinHeight0 = (hitPoint0.y <= (localHeight + Ray.EPSILON)) && (hitPoint0.y >= (localBaseCenter.y - Ray.EPSILON));
        if (isWithinHeight0) {
          tLateral = t0;
        }
      }
      
      // If t0 was not valid or t1 is closer and valid, check t1
      if (t1 > Ray.EPSILON && t1 < tLateral) { // Only check t1 if it's potentially closer than t0 or if t0 was invalid
        Point3 hitPoint1 = localRay.pointAtParameter(t1);
        boolean isWithinHeight1 = (hitPoint1.y <= (localHeight + Ray.EPSILON)) && (hitPoint1.y >= (localBaseCenter.y - Ray.EPSILON));
        if (isWithinHeight1) {
          tLateral = t1;
        }
      }
    }
    
    // --- Base Intersection Calculation (in local space) ---
    double tBase = Double.POSITIVE_INFINITY;
    // The base is a circle on the y=0 plane (localBaseCenter.y)
    // Normal of the base is (0, -1, 0) since localAxis is (0,1,0) and base is below apex.
    Vector3 baseNormal = localAxis.negate(); // Points downwards from base
    
    double denomBase = localRay.getDirection().dot(baseNormal);
    
    if (Math.abs(denomBase) > Ray.EPSILON) { // Ray is not parallel to the base plane
      // t = (P_base - O_ray_local) . N_base / (D_ray_local . N_base)
      tBase = (localBaseCenter.subtract(localRay.getOrigin())).dot(baseNormal) / denomBase;
      
      if (tBase > Ray.EPSILON) { // Intersection is in front of the ray
        Point3 hitPointBase = localRay.pointAtParameter(tBase);
        // Check if the intersection point is within the base circle's radius
        double distSqFromBaseCenter = hitPointBase.subtract(localBaseCenter).lengthSquared();
        if (distSqFromBaseCenter <= localRadius * localRadius + Ray.EPSILON) {
          // Valid base intersection
          } else {
          tBase = Double.POSITIVE_INFINITY; // Outside base circle
        }
        } else {
        tBase = Double.POSITIVE_INFINITY; // Intersection is behind the ray's origin
      }
    }
    
    // Return the closest valid intersection (between lateral surface and base)
    double finalT = Math.min(tLateral, tBase);
    return finalT == Double.POSITIVE_INFINITY ? -1 : finalT; // Return -1 if no intersection
  }
  
  /**
   * Calculates all intersection intervals between a ray and this finite cone.
   * The ray is transformed into the cone's local space for calculation.
   * The method checks for intersections with the lateral (conical) surface and the base.
   * All valid intersections are collected, sorted by t, and paired into intervals.
   * @param ray The ray to test, in world coordinates.
   * @return A list of IntersectionInterval objects. Empty if no intersections.
   */
  @Override
  public List<IntersectionInterval> intersectAll(Ray ray) {
    // 1. Transform the ray into the cone's local coordinate system
    Point3 localOrigin = inverseTransform.transformPoint(ray.getOrigin());
    Vector3 localDirection = inverseTransform.transformVector(ray.getDirection()).normalize();
    Ray localRay = new Ray(localOrigin, localDirection);
    
    // List to store all intersection points
    java.util.List<Intersection> hits = new java.util.ArrayList<>();
    
    // 2. Lateral (conical) surface intersection
    double k_cone = (localRadius * localRadius) / (localHeight * localHeight);
    double dx = localDirection.x, dy = localDirection.y, dz = localDirection.z;
    double ox = localOrigin.x, oy = localOrigin.y, oz = localOrigin.z;
    
    // Coefficients for quadratic equation: At^2 + Bt + C = 0
    double a = dx*dx + dz*dz - k_cone*dy*dy;
    double b = 2 * (ox*dx + oz*dz - k_cone*dy*(oy - localHeight));
    double c = ox*ox + oz*oz - k_cone*(oy - localHeight)*(oy - localHeight);
    
    if (Math.abs(a) > Ray.EPSILON) {
      double discriminant = b*b - 4*a*c;
      if (discriminant >= 0) {
        double sqrtD = Math.sqrt(discriminant);
        double t0 = (-b - sqrtD) / (2*a);
        double t1 = (-b + sqrtD) / (2*a);
        
        // Check t0
        if (t0 > Ray.EPSILON) {
          Point3 hit = localRay.pointAtParameter(t0);
          if (hit.y >= -Ray.EPSILON && hit.y <= localHeight + Ray.EPSILON) {
            Point3 worldHit = transform.transformPoint(hit);
            Vector3 worldNormal = getNormalAt(worldHit);
            hits.add(new Intersection(worldHit, worldNormal, t0, this));
          }
        }
        
        // Check t1
        if (t1 > Ray.EPSILON) {
          Point3 hit = localRay.pointAtParameter(t1);
          if (hit.y >= -Ray.EPSILON && hit.y <= localHeight + Ray.EPSILON) {
            Point3 worldHit = transform.transformPoint(hit);
            Vector3 worldNormal = getNormalAt(worldHit);
            hits.add(new Intersection(worldHit, worldNormal, t1, this));
          }
        }
      }
    }
    
    // 3. Base intersection (circle at y=0 in local space)
    Vector3 baseNormalLocal = localAxis.negate(); // (0, -1, 0)
    double denom = localDirection.dot(baseNormalLocal);
    if (Math.abs(denom) > Ray.EPSILON) {
      double t = (localBaseCenter.subtract(localOrigin)).dot(baseNormalLocal) / denom;
      if (t > Ray.EPSILON) {
        Point3 localHit = localRay.pointAtParameter(t);
        double distSq = localHit.subtract(localBaseCenter).lengthSquared();
        if (distSq <= localRadius * localRadius + Ray.EPSILON) {
          Point3 worldHit = transform.transformPoint(localHit);
          Vector3 worldNormal = getNormalAt(worldHit);
          hits.add(new Intersection(worldHit, worldNormal, t, this));
        }
      }
    }
    
    // 4. Sort all intersections by t
    java.util.Collections.sort(hits, (i1, i2) -> Double.compare(i1.getT(), i2.getT()));
    
    // 5. Pair into intervals
    List<IntersectionInterval> intervals = new java.util.ArrayList<>();
    for (int i = 0; i < hits.size() - 1; i += 2) {
      Intersection in = hits.get(i);
      Intersection out = hits.get(i + 1);
      intervals.add(new IntersectionInterval(in.getT(), out.getT(), in, out));
    }
    
    return intervals;
  }
  
}
