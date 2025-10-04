package net.elena.murat.math;

public class Point3 {
  public final double x;
  public final double y;
  public final double z;
  
  public static final Point3 ORIGIN = new Point3(0, 0, 0);
  
  public Point3(double x, double y, double z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }
  
  public double get(int axis) {
    switch(axis) {
      case 0: return x;
      case 1: return y;
      case 2: return z;
      default: throw new IllegalArgumentException("Axis must be 0, 1 or 2");
    }
  }
  
  public Point3 add(Vector3 v) {
    return new Point3(x + v.x, y + v.y, z + v.z);
  }
  
  public Vector3 subtract(Point3 other) {
    return new Vector3(x - other.x, y - other.y, z - other.z);
  }
  
  public Point3 subtract(Vector3 v) {
    return new Point3(this.x - v.x, this.y - v.y, this.z - v.z);
  }
  
  public Vector3 toVector() {
    return new Vector3(x, y, z);
  }
  
  public Vector3 toVector3() {
    return new Vector3(x, y, z);
  }
  
  public Vector3 multiply(double scalar) {
    return new Vector3(
      this.x * scalar,
      this.y * scalar,
      this.z * scalar
    );
  }
  
  public double length() {
    return Math.sqrt(x * x + y * y + z * z);
  }
  
  /**
   * Calculates the Euclidean distance between this point and another Point3.
   * @param other The other point.
   * @return The distance between the two points.
   */
  public double distance(Point3 other) {
    double dx = this.x - other.x;
    double dy = this.y - other.y;
    double dz = this.z - other.z;
    return Math.sqrt(dx * dx + dy * dy + dz * dz);
  }
  
  /**
   * Calculates the dot product between this point and another point/vector.
   * @param other The other point or vector
   * @return The dot product (scalar value)
   */
  public double dot(Point3 other) {
    return this.x * other.x + this.y * other.y + this.z * other.z;
  }
  
  /**
   * Calculates the dot product between this point and a vector.
   * @param vector The vector to calculate dot product with
   * @return The dot product (scalar value)
   */
  public double dot(Vector3 vector) {
    return this.x * vector.x + this.y * vector.y + this.z * vector.z;
  }
  
  @Override
  public String toString() {
    return String.format("Point3(%.3f, %.3f, %.3f)", x, y, z);
  }
}
