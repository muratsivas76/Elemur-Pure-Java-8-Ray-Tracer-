package net.elena.murat.math;

/**
 * Represents a ray in 3D space, defined by an origin point and a direction vector.
 * Includes energy tracking for ray tracing optimizations.
 */
public class Ray {
  private final Point3 origin;
  private final Vector3 direction;
  private double energy;  // New field for energy tracking
  
  /**
   * A small constant used to offset ray origins to prevent self-intersection.
   */
  public static final double EPSILON = 1e-4;//original is 1e-4
  
  /**
   * Constructs a new Ray with full energy (1.0).
   * The direction vector will be normalized automatically.
   * @param origin The origin point of the ray.
   * @param direction The direction vector of the ray.
   */
  public Ray(Point3 origin, Vector3 direction) {
    this(origin, direction, 1.0); // Default full energy
  }
  
  /**
   * Constructs a new Ray with specified energy.
   * @param origin The origin point of the ray.
   * @param direction The direction vector of the ray (will be normalized).
   * @param energy Initial energy of the ray (0.0 to 1.0).
   */
  public Ray(Point3 origin, Vector3 direction, double energy) {
    this.origin = origin;
    this.direction = direction.normalize();
    this.energy = Math.max(0, Math.min(1, energy)); // clamp
  }
  
  /**
   * Gets the current energy of the ray.
   * @return Energy value between 0.0 and 1.0.
   */
  public double getEnergy() {
    return energy;
  }
  
  /**
   * Sets the energy of the ray.
   * @param energy New energy value (will be clamped to [0.0, 1.0]).
   */
  public void setEnergy(double energy) {
    this.energy = Math.max(0.0, Math.min(1.0, energy));
  }
  
  /**
   * Creates a new ray with scaled energy (useful for reflections/refractions).
   * @param energyFactor Factor to multiply current energy by (0.0 to 1.0).
   * @return New Ray instance with adjusted energy.
   */
  public Ray createChildRay(double energyFactor) {
    return new Ray(
      this.origin,
      this.direction,
      this.energy * Math.max(0.0, Math.min(1.0, energyFactor))
    );
  }
  
  // Existing methods remain unchanged:
  public Point3 getOrigin() {
    return origin;
  }
  
  public Vector3 getDirection() {
    return direction;
  }
  
  public Point3 pointAtParameter(double t) {
    return origin.add(direction.scale(t));
  }
  
  /**
   * Transforms this ray by the given transformation matrix.
   * The origin point is transformed as a point (w=1).
   * The direction vector is transformed as a vector (w=0).
   *
   * This is commonly used to convert a world-space ray into object space
   * by applying the object's inverse transformation matrix.
   *
   * @param matrix The transformation matrix (usually inverse of object's transform)
   * @return A new Ray in the transformed space
  */
  public Ray transform(Matrix4 matrix) {
    // Transform the origin (treated as a point)
    Point3 newOrigin = matrix.transformPoint(getOrigin());
    
    // Transform the direction (treated as a vector)
    Vector3 newDirection = matrix.transformDirection(getDirection());
    
    return new Ray(newOrigin, newDirection);
  }

  @Override
  public String toString() {
    return String.format("Ray(origin=%s, direction=%s, energy=%.3f)",
    origin, direction, energy);
  }
  
}
