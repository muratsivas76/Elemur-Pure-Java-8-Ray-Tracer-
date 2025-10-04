package net.elena.murat.math;

import java.util.Optional;
import java.util.Random;

public class Vector3 {
  public static final Vector3 ZERO = new Vector3(0, 0, 0);
  public static final Vector3 ONE = new Vector3(1, 1, 1);
  
  public final double x, y, z;
  
  public Vector3(double x, double y, double z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }
  
  /**
   * Creates a vector from point 'from' to point 'to'
   * @param from The starting point
   * @param to The ending point
   */
  public Vector3(Point3 from, Point3 to) {
    this.x = to.x - from.x;
    this.y = to.y - from.y;
    this.z = to.z - from.z;
  }
  
  public double get(int axis) {
    switch(axis) {
      case 0: return x;
      case 1: return y;
      case 2: return z;
      default: throw new IllegalArgumentException("Axis must be 0, 1 or 2");
    }
  }
  
  // Vector3.java'ya bu metodları ekleyin
  public Vector3 normalizeSafe() {
    double len = length();
    return len > 0 ? new Vector3(x/len, y/len, z/len) : new Vector3(0, 1, 0);
  }
  
  public double angleBetweenSafe(Vector3 other) {
    if (other == null) return Math.PI/2;
    double dot = this.dot(other);
    double magProduct = this.length() * other.length();
    return magProduct > 0 ? Math.acos(Math.max(-1, Math.min(1, dot/magProduct))) : Math.PI/2;
  }
  
  public double angleBetween(Vector3 other) {
    double dot = this.dot(other);
    double magProduct = this.length() * other.length();
    return Math.acos(dot / magProduct);
  }
  
  public double length() {
    return Math.sqrt(x * x + y * y + z * z);
  }
  
  public double lengthSquared() {
    return x * x + y * y + z * z;
  }
  
  public Vector3 multiply(double t) {
    return new Vector3(x * t, y * t, z * t);
  }
  
  public Vector3 lerp(Vector3 other, double t) {
    t = Math.max(0.0, Math.min(1.0, t));
    return new Vector3(
      x + (other.x - x) * t,
      y + (other.y - y) * t,
      z + (other.z - z) * t
    );
  }
  
  public Vector3 normalize() {
    double len = length();
    return len > 0 ? new Vector3(x/len, y/len, z/len) : new Vector3(0,0,0);
  }
  
  public Vector3 add(Vector3 other) {
    return new Vector3(x + other.x, y + other.y, z + other.z);
  }
  
  public Vector3 subtract(Vector3 other) {
    return new Vector3(x - other.x, y - other.y, z - other.z);
  }
  
  public Vector3 subtract(Point3 other) {
    return new Vector3(x - other.x, y - other.y, z - other.z);
  }
  
  public Vector3 scale(double scalar) {
    return new Vector3(x * scalar, y * scalar, z * scalar);
  }
  
  public double dot(Vector3 other) {
    return x * other.x + y * other.y + z * other.z;
  }
  
  public Vector3 negate() {
    return new Vector3(-x, -y, -z);
  }
  
  public Vector3 cross(Vector3 other) {
    return new Vector3(
      y * other.z - z * other.y,
      z * other.x - x * other.z,
      x * other.y - y * other.x
    );
  }
  
  public Vector3 reflect(Vector3 normal) {
    return this.subtract(normal.scale(2 * this.dot(normal)));
  }
  
  //Original
  public Optional<Vector3> refract(Vector3 normal, double n1, double n2) {
    double n = n1 / n2;
    //double cosI = -this.dot(normal); //Original
    double cosI = this.dot(normal);
    double sinT2 = n * n * (1.0 - (cosI * cosI));
    
    if (sinT2 > 1.0) return Optional.empty();
    double cosT = Math.sqrt(1.0 - sinT2);
    return Optional.of(this.scale(n).add(normal.scale((n * cosI) - cosT)).normalize());
  }
  
  public Optional<Vector3> refract(Vector3 normal, double eta) {
    return this.refract(normal, 1.0, eta);
  }
  
  public Vector3 refractSimple(Vector3 normal, double eta) {
    double cosI = -this.dot(normal);
    double sinT2 = eta * eta * (1.0 - cosI * cosI);
    
    if (sinT2 > 1.0) {
      return this.reflect(normal); // Total internal reflection
    }
    
    double cosT = Math.sqrt(1.0 - sinT2);
    return this.scale(eta).add(normal.scale(eta * cosI - cosT)).normalize();
  }
  
  /**
   * Calculates Fresnel reflection coefficient
   * @param viewDir View direction
   * @param normal The surface normal (normalized)
   * @param ior1 Index of refraction of first medium
   * @param ior2 Index of refraction of second medium
   * @return Fresnel reflection coefficient between 0 and 1
   */
  public static double calculateFresnel(Vector3 viewDir, Vector3 normal,
    double ior1, double ior2) {
    double cosTheta = Math.abs(viewDir.dot(normal));
    cosTheta = Math.max(0.0, Math.min(1.0, cosTheta));
    
    double r0 = Math.pow((ior1 - ior2) / (ior1 + ior2), 2);
    double fresnel = r0 + (1.0 - r0) * Math.pow(1.0 - cosTheta, 5);
    
    return Math.max(0.0, Math.min(1.0, fresnel));
  }
  
  public static Vector3 randomInUnitSphere(Random rand) {
    Vector3 p;
    do {
      p = new Vector3(
        rand.nextDouble() * 2 - 1,
        rand.nextDouble() * 2 - 1,
        rand.nextDouble() * 2 - 1
      );
    } while (p.lengthSquared() >= 1.0);
    return p;
  }
  
  /**
   * Verilen incident vektörünün, normal vektöre göre yansıma vektörünü hesaplar.
   *
   * @param incident Gelen vektör (ışın yönü).
   * @param normal Yansıma yüzeyinin normal vektörü.
   * @return Yansıma vektörü olarak yeni bir Vector3 nesnesi döner.
   */
  public static Vector3 reflect(Vector3 incident, Vector3 normal) {
    // R = I - 2 * (I . N) * N
    // I: incident vektör
    // N: normal vektör
    return incident.subtract(normal.scale(2 * incident.dot(normal)));
  }
  
  public Vector3 transformNormal(Matrix4 matrix) {
    Matrix4 inverseTranspose = matrix.inverseTransposeForNormal();
    if (inverseTranspose == null) {
      return new Vector3(0, 0, 0);
    }
    return new Vector3(
      inverseTranspose.get(0, 0) * x + inverseTranspose.get(0, 1) * y + inverseTranspose.get(0, 2) * z,
      inverseTranspose.get(1, 0) * x + inverseTranspose.get(1, 1) * y + inverseTranspose.get(1, 2) * z,
      inverseTranspose.get(2, 0) * x + inverseTranspose.get(2, 1) * y + inverseTranspose.get(2, 2) * z
    ).normalize();
  }
  
  @Override
  public String toString() {
    return String.format("(%.2f, %.2f, %.2f)", x, y, z);
  }
  
}
