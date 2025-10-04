package net.elena.murat.light;

import java.awt.Color;

import net.elena.murat.lovert.Scene;
import net.elena.murat.math.Point3;
import net.elena.murat.math.Vector3;

public interface Light {
  
  Point3 getPosition();
  
  Color getColor();
  
  double getIntensity();
  
  Vector3 getDirectionAt(Point3 point);
  
  double getAttenuatedIntensity(Point3 point);
  
  double getIntensityAt(Point3 point);
  
  Vector3 getDirectionTo(Point3 point);
  
  boolean isVisibleFrom(Point3 point, Scene scene);
  
}
///////////////////////
package net.elena.murat.light;

import java.awt.Color;

import net.elena.murat.math.Point3;
import net.elena.murat.math.Vector3;

public class LightProperties {
  public final Vector3 direction;
  public final Color color;
  public final double intensity;
  
  public LightProperties(Vector3 direction, Color color, double intensity) {
    this.direction = direction;
    this.color = color;
    this.intensity = intensity;
  }
  
  // Factory method for ambient light
  public static LightProperties createAmbient(Color color, double intensity) {
    return new LightProperties(new Vector3(0, 0, 0), color, intensity);
  }
  
  // Factory method for directional light
  public static LightProperties createDirectional(Vector3 direction, Color color, double intensity) {
    return new LightProperties(direction.negate().normalize(), color, intensity);
  }
  
  // Factory method for point light
  public static LightProperties createPointLight(Vector3 lightPos, Point3 surfacePoint, Color color, double intensity) {
    Vector3 dir = lightPos.subtract(surfacePoint).normalize();
    return new LightProperties(dir, color, intensity);
  }
  
  // Null object pattern for safety
  public static LightProperties nullProperties() {
    return new LightProperties(new Vector3(0, 1, 0), Color.BLACK, 0.0);
  }
  
  public static final LightProperties getLightProperties(Light light, Point3 point) {
    if (light == null) return nullProperties();
    
    if (light instanceof ElenaMuratAmbientLight) {
      return createAmbient(light.getColor(), light.getIntensity());
    }
    
    try {
      if (light instanceof MuratPointLight) {
        return createPointLight(
          light.getPosition().toVector(), point, light.getColor(), light.getAttenuatedIntensity(point)
        );
      }
      else if (light instanceof ElenaDirectionalLight) {
        return createDirectional(
          ((ElenaDirectionalLight)light).getDirection(), light.getColor(), light.getIntensity()
        );
      }
      else {
        return new LightProperties(
          new Vector3(0, 1, 0), light.getColor(), Math.min(light.getIntensity(), 1.0)
        );
      }
      } catch (Exception e) {
      return nullProperties();
    }
  }
  
}
////////////////////
package net.elena.murat.material;

import java.awt.Color;

//custom
import net.elena.murat.light.Light;
import net.elena.murat.math.*;

/**
 * Material interface defines the contract for all materials in the ray tracer.
 * It now includes methods for color calculation at a point,
 * as well as properties for reflectivity, index of refraction, and transparency.
 */
public interface Material {
  /**
   * Calculates the final color at a given point on the surface, considering
   * the material's properties and the light source.
   * @param point The point in 3D space (world coordinates) where the light hits.
   * @param normal The normal vector at the point (world coordinates).
   * @param light The single light source affecting this point.
   * @param viewerPos The position of the viewer/camera.
   * @return The color contribution from this specific light for the point.
   */
  Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPos);
  
  /**
   * Returns the reflectivity coefficient of the material.
   * This value determines how much light is reflected by the surface (0.0 for no reflection, 1.0 for full reflection).
   * @return The reflectivity value (0.0-1.0).
   */
  double getReflectivity();
  
  /**
   * Returns the index of refraction (IOR) of the material.
   * This value is used for calculating refraction (transparency).
   * @return The index of refraction (typically 1.0 for air, less than 1.0 for denser materials).
   */
  double getIndexOfRefraction();
  
  /**
   * Returns the transparency coefficient of the material.
   * This value determines how much light passes through the surface (0.0 for opaque, 1.0 for fully transparent).
   * @return The transparency value (0.0-1.0).
   */
  double getTransparency();
  
  void setObjectTransform(Matrix4 tm);
}
//////////////////
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
/////////////////
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
//////////////////
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
  
  public Optional<Vector3> refract(Vector3 normal, double n1, double n2) {
    double n = n1 / n2;
    double cosI = -this.dot(normal);
    double sinT2 = n * n * (1.0 - cosI * cosI);
    
    if (sinT2 > 1.0) return Optional.empty();
    double cosT = Math.sqrt(1.0 - sinT2);
    return Optional.of(this.scale(n).add(normal.scale(n * cosI - cosT)).normalize());
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
/////////////////
package net.elena.murat.math;

import net.elena.murat.math.Ray;

/**
 * Represents a 4x4 matrix for 3D transformations (translation, rotation, scaling).
 */
public class Matrix4 {
  private final double[][] m; // Matrix elements
  
  /**
   * Constructs an identity Matrix4.
   */
  public Matrix4() {
    m = new double[4][4];
    m[0][0] = 1.0; m[0][1] = 0.0; m[0][2] = 0.0; m[0][3] = 0.0;
    m[1][0] = 0.0; m[1][1] = 1.0; m[1][2] = 0.0; m[1][3] = 0.0;
    m[2][0] = 0.0; m[2][1] = 0.0; m[2][2] = 1.0; m[2][3] = 0.0;
    m[3][0] = 0.0; m[3][1] = 0.0; m[3][2] = 0.0; m[3][3] = 1.0;
  }
  
  /**
   * Constructs a Matrix4 with the specified elements.
   */
  public Matrix4(double m00, double m01, double m02, double m03,
    double m10, double m11, double m12, double m13,
    double m20, double m21, double m22, double m23,
    double m30, double m31, double m32, double m33) {
    m = new double[4][4];
    this.m[0][0] = m00; this.m[0][1] = m01; this.m[0][2] = m02; this.m[0][3] = m03;
    this.m[1][0] = m10; this.m[1][1] = m11; this.m[1][2] = m12; this.m[1][3] = m13;
    this.m[2][0] = m20; this.m[2][1] = m21; this.m[2][2] = m22; this.m[2][3] = m23;
    this.m[3][0] = m30; this.m[3][1] = m31; this.m[3][2] = m32; this.m[3][3] = m33;
  }
  
  /**
   * Constructs a new Matrix4 by copying an existing matrix.
   * @param other The Matrix4 object to copy.
   */
  public Matrix4(Matrix4 other) {
    this(other.m[0][0], other.m[0][1], other.m[0][2], other.m[0][3],
      other.m[1][0], other.m[1][1], other.m[1][2], other.m[1][3],
      other.m[2][0], other.m[2][1], other.m[2][2], other.m[2][3],
    other.m[3][0], other.m[3][1], other.m[3][2], other.m[3][3]);
  }
  
  /**
   * Returns an identity (unit) 4x4 matrix.
   * An identity matrix has 1s on the main diagonal and 0s elsewhere.
   * It represents no translation, rotation, or scaling.
   *
   * @return A new 4x4 identity matrix.
   */
  public static Matrix4 identity() {
    return new Matrix4(); // The default constructor creates an identity matrix
  }
  
  /**
   * Sets the value at the specified row and column.
   * @param row The row index (0-3)
   * @param col The column index (0-3)
   * @param value The value to set
   * @throws IndexOutOfBoundsException if row or col is not in [0, 3]
   */
  public void set(int row, int col, double value) {
    if (row < 0 || row >= 4 || col < 0 || col >= 4) {
      throw new IndexOutOfBoundsException("Matrix4 indices out of bounds: [" + row + "][" + col + "]");
    }
    
    this.m[row][col] = value;
  }
  
  /**
   * Gets the X-axis scale factor from this transformation matrix.
   * This is calculated as the magnitude of the X basis vector.
   * @return The X scale factor
   */
  public double getScaleX() {
    return Math.sqrt(m[0][0] * m[0][0] + m[1][0] * m[1][0] + m[2][0] * m[2][0]);
  }
  
  /**
   * Gets the Y-axis scale factor from this transformation matrix.
   * This is calculated as the magnitude of the Y basis vector.
   * @return The Y scale factor
   */
  public double getScaleY() {
    return Math.sqrt(m[0][1] * m[0][1] + m[1][1] * m[1][1] + m[2][1] * m[2][1]);
  }
  
  /**
   * Gets the Z-axis scale factor from this transformation matrix.
   * This is calculated as the magnitude of the Z basis vector.
   * @return The Z scale factor
   */
  public double getScaleZ() {
    return Math.sqrt(m[0][2] * m[0][2] + m[1][2] * m[1][2] + m[2][2] * m[2][2]);
  }
  
  public Ray transformRay(Ray ray) {
    Point3 newOrigin = this.transformPoint(ray.getOrigin());
    Vector3 newDirection = this.transformVector(ray.getDirection()).normalize();
    return new Ray(newOrigin, newDirection);
  }
  
  /**
   * Transforms a direction vector by this matrix.
   * Unlike points, vectors are not affected by translation.
   * Only the rotational and scaling components are applied.
   *
   * This is used for transforming normal vectors, ray directions, etc.
   *
   * @param v The direction vector to transform
   * @return A new transformed Vector3
   */
  public Vector3 transformDirection(Vector3 v) {
    double x = m[0][0] * v.x + m[0][1] * v.y + m[0][2] * v.z;
    double y = m[1][0] * v.x + m[1][1] * v.y + m[1][2] * v.z;
    double z = m[2][0] * v.x + m[2][1] * v.y + m[2][2] * v.z;
    return new Vector3(x, y, z);
  }
  
  /**
   * Provides access to a specific element of the matrix.
   * @param row The row index (0-3).
   * @param col The column index (0-3).
   * @return The matrix element at the specified position.
   * @throws IndexOutOfBoundsException If the row or column index is invalid.
   */
  public double get(int row, int col) {
    if (row < 0 || row >= 4 || col < 0 || col >= 4) {
      throw new IndexOutOfBoundsException("Matrix4 indices out of bounds: [" + row + "][" + col + "]");
    }
    return m[row][col];
  }
  
  /**
   * Normal vektörü dönüştürür (normal transformasyonu için).
   * Normal vektörlerin doğru dönüşümü için matrisin ters transpozu kullanılır.
   * @param normal Dönüştürülecek normal vektör
   * @return Dönüştürülmüş normal vektör (normalize edilmiş)
   */
  public Vector3 transformNormal(Vector3 normal) {
    // Matrisin ters transpozu alınır
    Matrix4 normalMatrix = this.inverseTransposeForNormal();
    
    if (normalMatrix == null) {
      return new Vector3(0, 0, 0); // Geçersiz dönüşüm durumu
    }
    
    // Vektörü dönüştür (w=0 varsayarak, sadece 3x3 kısım kullanılır)
    double x = normal.x;
    double y = normal.y;
    double z = normal.z;
    
    double newX = normalMatrix.m[0][0] * x + normalMatrix.m[0][1] * y + normalMatrix.m[0][2] * z;
    double newY = normalMatrix.m[1][0] * x + normalMatrix.m[1][1] * y + normalMatrix.m[1][2] * z;
    double newZ = normalMatrix.m[2][0] * x + normalMatrix.m[2][1] * y + normalMatrix.m[2][2] * z;
    
    return new Vector3(newX, newY, newZ).normalize();
  }
  
  /**
   * Multiplies this matrix by another matrix.
   * @param other The other Matrix4 to multiply with.
   * @return The resulting Matrix4.
   */
  public Matrix4 multiply(Matrix4 other) {
    Matrix4 result = new Matrix4(); // Start with an identity matrix
    for (int i = 0; i < 4; i++) {
      for (int j = 0; j < 4; j++) {
        result.m[i][j] = 0; // Reset element before summing
        for (int k = 0; k < 4; k++) {
          result.m[i][j] += this.m[i][k] * other.m[k][j];
        }
      }
    }
    return result;
  }
  
  /**
   * Transforms a Point3 by this matrix (includes translation).
   * For affine transformations, the W component of the transformed point should be 1.0.
   * @param point The Point3 to transform.
   * @return The transformed Point3.
   */
  public Point3 transformPoint(Point3 point) {
    double x = m[0][0] * point.x + m[0][1] * point.y + m[0][2] * point.z + m[0][3];
    double y = m[1][0] * point.x + m[1][1] * point.y + m[1][2] * point.z + m[1][3];
    double z = m[2][0] * point.x + m[2][1] * point.y + m[2][2] * point.z + m[2][3];
    return new Point3(x, y, z);
  }
  
  /**
   * Transforms a Vector3 by this matrix (only rotation and scaling, no translation).
   * @param vector The Vector3 to transform.
   * @return The transformed Vector3.
   */
  public Vector3 transformVector(Vector3 vector) {
    double x = m[0][0] * vector.x + m[0][1] * vector.y + m[0][2] * vector.z;
    double y = m[1][0] * vector.x + m[1][1] * vector.y + m[1][2] * vector.z;
    double z = m[2][0] * vector.x + m[2][1] * vector.y + m[2][2] * vector.z;
    return new Vector3(x, y, z);
  }
  
  /**
   * Returns the inverse of this matrix. Returns null if the matrix is non-invertible.
   * This method is designed for affine transformations (rotation, translation, uniform scaling).
   * Formula: [ R | t ]^-1 = [ R^-1 | -R^-1 * t ]
   * Where R is the upper-left 3x3 submatrix and t is the translation vector.
   * @return The inverse Matrix4 or null.
   */
  public Matrix4 inverse() {
    // Extract the upper 3x3 rotation/scale part
    Matrix3 upperLeft = new Matrix3(
      m[0][0], m[0][1], m[0][2],
      m[1][0], m[1][1], m[1][2],
      m[2][0], m[2][1], m[2][2]
    );
    Matrix3 invUpperLeft = upperLeft.inverse(); // This performs its own determinant check
    
    if (invUpperLeft == null) {
      System.err.println("Warning: Upper 3x3 part of Matrix4 is non-invertible, cannot compute inverse.");
      return null;
    }
    
    Matrix4 inv = new Matrix4(); // Resulting inverse matrix, initialized to identity
    
    // Set the upper-left 3x3 of the inverse matrix (R^-1)
    inv.m[0][0] = invUpperLeft.get(0,0); inv.m[0][1] = invUpperLeft.get(0,1); inv.m[0][2] = invUpperLeft.get(0,2);
    inv.m[1][0] = invUpperLeft.get(1,0); inv.m[1][1] = invUpperLeft.get(1,1); inv.m[1][2] = invUpperLeft.get(1,2);
    inv.m[2][0] = invUpperLeft.get(2,0); inv.m[2][1] = invUpperLeft.get(2,1); inv.m[2][2] = invUpperLeft.get(2,2);
    
    // Calculate the inverse translation part: -R^-1 * t
    Vector3 translation = new Vector3(m[0][3], m[1][3], m[2][3]);
    Vector3 invTranslation = invUpperLeft.transform(translation).negate();
    
    inv.m[0][3] = invTranslation.x;
    inv.m[1][3] = invTranslation.y;
    inv.m[2][3] = invTranslation.z;
    
    // Bottom row remains [0, 0, 0, 1] for affine transformations
    inv.m[3][0] = 0.0; inv.m[3][1] = 0.0; inv.m[3][2] = 0.0; inv.m[3][3] = 1.0;
    
    return inv;
  }
  
  /**
   * Computes the inverse transpose of the upper 3x3 part of this matrix.
   * This is typically used to transform normal vectors correctly when the
   * model matrix contains non-uniform scaling.
   * For pure rotations, the inverse is equal to the transpose.
   *
   * @return A new Matrix4 representing the inverse transpose of the 3x3 part,
   * with the translation components set to zero. Returns null if the
   * upper 3x3 part is non-invertible.
   */
  public Matrix4 inverseTransposeForNormal() {
    // Extract the upper 3x3 part
    Matrix3 upperLeft = new Matrix3(
      m[0][0], m[0][1], m[0][2],
      m[1][0], m[1][1], m[1][2],
      m[2][0], m[2][1], m[2][2]
    );
    
    // Compute its inverse
    Matrix3 invUpperLeft = upperLeft.inverse();
    
    if (invUpperLeft == null) {
      System.err.println("Warning: Upper 3x3 part of Matrix4 is non-invertible, cannot compute inverse transpose for normal.");
      return null;
    }
    
    // Transpose the inverse (this is the correct operation for normals)
    Matrix3 normalMatrix3 = invUpperLeft.transpose();
    
    // Construct a new Matrix4 from this 3x3, with translation part zeroed out
    return new Matrix4(
      normalMatrix3.get(0,0), normalMatrix3.get(0,1), normalMatrix3.get(0,2), 0,
      normalMatrix3.get(1,0), normalMatrix3.get(1,1), normalMatrix3.get(1,2), 0,
      normalMatrix3.get(2,0), normalMatrix3.get(2,1), normalMatrix3.get(2,2), 0,
      0, 0, 0, 1
    );
  }
  
  /**
   * Creates a translation matrix.
   * @param translation The translation vector.
   * @return The translation Matrix4.
   */
  public static Matrix4 translate(Vector3 translation) {
    return new Matrix4(
      1, 0, 0, translation.x,
      0, 1, 0, translation.y,
      0, 0, 1, translation.z,
      0, 0, 0, 1
    );
  }
  
  public static Matrix4 translate(double x, double y, double z) {
    return new Matrix4(
      1, 0, 0, x,
      0, 1, 0, y,
      0, 0, 1, z,
      0, 0, 0, 1
    );
  }
  
  /**
   * Creates a rotation matrix around the X-axis.
   * @param angleDegrees The rotation angle in degrees.
   * @return The rotation Matrix4.
   */
  public static Matrix4 rotateX(double angleDegrees) {
    double angleRad = Math.toRadians(angleDegrees);
    double cosA = Math.cos(angleRad);
    double sinA = Math.sin(angleRad);
    return new Matrix4(
      1,    0,     0, 0,
      0,  cosA, -sinA, 0,
      0,  sinA,  cosA, 0,
      0,    0,     0, 1
    );
  }
  
  /**
   * Creates a rotation matrix around the Y-axis.
   * @param angleDegrees The rotation angle in degrees.
   * @return The rotation Matrix4.
   */
  public static Matrix4 rotateY(double angleDegrees) {
    double angleRad = Math.toRadians(angleDegrees);
    double cosA = Math.cos(angleRad);
    double sinA = Math.sin(angleRad);
    return new Matrix4(
      cosA,  0, sinA, 0,
      0,     1,    0, 0,
      -sinA, 0, cosA, 0,
      0,     0,    0, 1
    );
  }
  
  /**
   * Creates a rotation matrix around the Z-axis.
   * @param angleDegrees The rotation angle in degrees.
   * @return The rotation Matrix4.
   */
  public static Matrix4 rotateZ(double angleDegrees) {
    double angleRad = Math.toRadians(angleDegrees);
    double cosA = Math.cos(angleRad);
    double sinA = Math.sin(angleRad);
    return new Matrix4(
      cosA, -sinA, 0, 0,
      sinA,  cosA, 0, 0,
      0,     0,    1, 0,
      0,     0,    0, 1
    );
  }
  
  /**
   * Creates a scaling matrix with the specified scale factors.
   * @param sx The X-axis scale factor.
   * @param sy The Y-axis scale factor.
   * @param sz The Z-axis scale factor.
   * @return The scaling Matrix4.
   */
  public static Matrix4 scale(double sx, double sy, double sz) {
    return new Matrix4(
      sx, 0,  0, 0,
      0, sy,  0, 0,
      0,  0, sz, 0,
      0,  0,  0, 1
    );
  }
  
  // Matrix4 sınıfına bu metodu ekleyin
  public Matrix4 transpose() {
    return new Matrix4(
      m[0][0], m[1][0], m[2][0], m[3][0],
      m[0][1], m[1][1], m[2][1], m[3][1],
      m[0][2], m[1][2], m[2][2], m[3][2],
      m[0][3], m[1][3], m[2][3], m[3][3]
    );
  }
  
  /**
   * Creates a Matrix4 from a Matrix3 (typically to extend rotation matrices to 4x4).
   * @param m3 The Matrix3 to extend.
   * @return The created Matrix4.
   */
  public static Matrix4 fromMatrix3(Matrix3 m3) {
    return new Matrix4(
      m3.get(0,0), m3.get(0,1), m3.get(0,2), 0,
      m3.get(1,0), m3.get(1,1), m3.get(1,2), 0,
      m3.get(2,0), m3.get(2,1), m3.get(2,2), 0,
      0, 0, 0, 1
    );
  }
  
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 4; i++) {
      sb.append("| ");
      for (int j = 0; j < 4; j++) {
        sb.append(String.format("%8.4f", m[i][j])).append(" ");
      }
      sb.append("|\n");
    }
    return sb.toString();
  }
  
}
///////////////
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
////////////////
package net.elena.murat.util;

import java.awt.Color;

/**
 * Utility class for RGB color operations.
 * All methods work exclusively with RGB components (alpha channel is ignored).
 * Results are always fully opaque (alpha=255).
 */
public final class ColorUtil {
  
  // Color constants (all fully opaque)
  public static final Color BLACK = new Color(0, 0, 0);
  public static final Color WHITE = new Color(255, 255, 255);
  public static final Color RED = new Color(255, 0, 0);
  public static final Color GREEN = new Color(0, 255, 0);
  public static final Color BLUE = new Color(0, 0, 255);
  
  /**
   * Linear interpolation between two RGB colors (t=0: c1, t=1: c2)
   * Only RGB components are interpolated, result is fully opaque
   */
  public static Color lerp(Color c1, Color c2, float t) {
    t = clamp(t, 0.0f, 1.0f);
    return new Color(
      c1.getRed() + (int)((c2.getRed() - c1.getRed()) * t),
      c1.getGreen() + (int)((c2.getGreen() - c1.getGreen()) * t),
      c1.getBlue() + (int)((c2.getBlue() - c1.getBlue()) * t)
    );
  }
  
  /**
   * Component-wise multiplication of two RGB colors
   * Result is always fully opaque (alpha=255)
   */
  public static Color multiply(Color c1, Color c2) {
    return new Color(
      (c1.getRed() * c2.getRed()) / 255,
      (c1.getGreen() * c2.getGreen()) / 255,
      (c1.getBlue() * c2.getBlue()) / 255
    );
  }
  
  /**
   * Multiplies RGB color by a scalar factor
   * Result is always fully opaque (alpha=255)
   */
  public static Color multiply(Color c, float scalar) {
    scalar = Math.max(0.0f, scalar);
    return new Color(
      clamp((int)(c.getRed() * scalar)),
      clamp((int)(c.getGreen() * scalar)),
      clamp((int)(c.getBlue() * scalar))
    );
  }
  
  /**
   * Reinhard Tone Mapping Operator
   */
  public static float reinhardToneMap(float color) {
    return color / (1.0f + color);
  }
  
  public static Color gammaCorrect(Color color, float gamma) {
    float invGamma = 1.0f / gamma;
    float r = (float) Math.pow(color.getRed() / 255.0, invGamma);
    float g = (float) Math.pow(color.getGreen() / 255.0, invGamma);
    float b = (float) Math.pow(color.getBlue() / 255.0, invGamma);
    float a = color.getAlpha() / 255.0f;
    
    // Garanti için clamp (NaN veya aşırı değerlere karşı)
    r = Math.max(0.0f, Math.min(1.0f, r));
    g = Math.max(0.0f, Math.min(1.0f, g));
    b = Math.max(0.0f, Math.min(1.0f, b));
    a = Math.max(0.0f, Math.min(1.0f, a));
    
    return new Color(r, g, b, a);
  }
  
  private static float linearToSrgb(float linear) {
    if (linear <= 0.0031308f) {
      return linear * 12.92f;
      } else {
      return (float) (1.055f * Math.pow(linear, 1.0/2.4) - 0.055f);
    }
  }
  
  // ColorUtil'e ekle - DOĞRU metod imzaları:
  /**
   * sRGB to linear conversion for a single channel
   */
  public static float srgbToLinear(float srgb) {
    if (srgb <= 0.04045f) {
      return srgb / 12.92f;
      } else {
      return (float) Math.pow((srgb + 0.055f) / 1.055f, 2.4f);
    }
  }
  
  public static Color sRGBToLinear(Color srgbColor, float gamma) {
    if (gamma == 1f) return srgbColor;
    
    float r = srgbColor.getRed() / 255.0f;
    float g = srgbColor.getGreen() / 255.0f;
    float b = srgbColor.getBlue() / 255.0f;
    float a = srgbColor.getAlpha() / 255.0f;
    
    r = clamp(r, 0.0f, 1.0f);
    g = clamp(g, 0.0f, 1.0f);
    b = clamp(b, 0.0f, 1.0f);
    
    r = (r <= 0.04045f) ? (r / 12.92f) : (float) Math.pow((r + 0.055f) / 1.055f, gamma);
    g = (g <= 0.04045f) ? (g / 12.92f) : (float) Math.pow((g + 0.055f) / 1.055f, gamma);
    b = (b <= 0.04045f) ? (b / 12.92f) : (float) Math.pow((b + 0.055f) / 1.055f, gamma);
    
    return new Color(r, g, b, a);
  }
  
  public static Color sRGBToLinearExtra(Color base, float gamma) {
    if (gamma == 1f) return base;
    
    if (gamma <= 0) gamma = 2.2f;
    
    float r = base.getRed() / 255.0f;
    float g = base.getGreen() / 255.0f;
    float b = base.getBlue() / 255.0f;
    float a = base.getAlpha() / 255.0f;
    
    r = (float) Math.pow(r, 1.0f / gamma);
    g = (float) Math.pow(g, 1.0f / gamma);
    b = (float) Math.pow(b, 1.0f / gamma);
    
    int red = (int) (Math.min(Math.max(r * 255, 0), 255));
    int green = (int) (Math.min(Math.max(g * 255, 0), 255));
    int blue = (int) (Math.min(Math.max(b * 255, 0), 255));
    int alpha = (int) (a * 255);
    
    return new Color(red, green, blue, alpha);
  }
  
  /**
   * Apply exposure and tone mapping to linear color
   */
  public static Color applyExposureAndToneMapping(Color linearColor, float exposure) {
    float r = clamp(linearColor.getRed(), 0.0f, 1.0f);
    float g = clamp(linearColor.getGreen(), 0.0f, 1.0f);
    float b = clamp(linearColor.getBlue(), 0.0f, 1.0f);
    float a = clamp(linearColor.getAlpha(), 0.0f, 1.0f);
    
    // Exposure adjustment
    r *= exposure;
    g *= exposure;
    b *= exposure;
    
    // ACES filmic tone mapping
    r = clamp(acesToneMap(r), 0.0f, 1.0f);
    g = clamp(acesToneMap(g), 0.0f, 1.0f);
    b = clamp(acesToneMap(b), 0.0f, 1.0f);
    
    return new Color(r, g, b, a);
  }
  
  /**
   * Apply tone mapping to a linear color
   */
  public static Color applyToneMapping(Color linearColor, float exposure) {
    float r = clamp(linearColor.getRed() / 255.0f, 0.0f, 1.0f);
    float g = clamp(linearColor.getGreen() / 255.0f, 0.0f, 1.0f);
    float b = clamp(linearColor.getBlue() / 255.0f, 0.0f, 1.0f);
    float a = clamp(linearColor.getAlpha() / 255.0f, 0.0f, 1.0f);
    
    // Exposure adjustment
    r *= exposure;
    g *= exposure;
    b *= exposure;
    
    // Reinhard tone mapping
    r = clamp(reinhardToneMap(r), 0.0f, 1.0f);
    g = clamp(reinhardToneMap(g), 0.0f, 1.0f);
    b = clamp(reinhardToneMap(b), 0.0f, 1.0f);
    
    return new Color(r, g, b, a);
  }
  
  public static Color linearToSRGB(Color linearColor) {
    float r = clamp(linearColor.getRed(), 0.0f, 1.0f);
    float g = clamp(linearColor.getGreen(), 0.0f, 1.0f);
    float b = clamp(linearColor.getBlue(), 0.0f, 1.0f);
    float a = clamp(linearColor.getAlpha(), 0.0f, 1.0f);
    
    // Linear to sRGB conversion
    r = clamp(linearToSrgb(r), 0.0f, 1.0f);
    g = clamp(linearToSrgb(g), 0.0f, 1.0f);
    b = clamp(linearToSrgb(b), 0.0f, 1.0f);
    
    return new Color(r, g, b, a);
  }
  
  private static float acesToneMap(float x) {
    float a = 2.51f;
    float b = 0.03f;
    float c = 2.43f;
    float d = 0.59f;
    float e = 0.14f;
    return Math.max(0.0f, Math.min(1.0f, (x * (a * x + b)) / (x * (c * x + d) + e)));
  }
  
  /**
   * Scales RGB components by a factor (0.0-1.0)
   * Result is always fully opaque (alpha=255)
   */
  public static Color multiplyColor(Color color, double factor) {
    factor = Math.max(0, Math.min(1, factor));
    return new Color(
      (int)(color.getRed() * factor),
      (int)(color.getGreen() * factor),
      (int)(color.getBlue() * factor)
    );
  }
  
  public static Color multiplyColorFloat(Color color, float factor) {
    factor = Math.max(0f, Math.min(1f, factor));
    return new Color(
      (int)(color.getRed() * factor),
      (int)(color.getGreen() * factor),
      (int)(color.getBlue() * factor)
    );
  }
  
  public static Color multiplyColors(Color color1, Color color2) {
    float r = color1.getRed() / 255.0f * color2.getRed() / 255.0f;
    float g = color1.getGreen() / 255.0f * color2.getGreen() / 255.0f;
    float b = color1.getBlue() / 255.0f * color2.getBlue() / 255.0f;
    
    return new Color(r, g, b);
  }
  
  /**
   * Multiplies two colors with a scaling factor
   * Result is always fully opaque (alpha=255)
   */
  public static Color multiplyColors(Color base, Color light, double factor) {
    int r = (int) Math.min(255, Math.max(0, base.getRed() * light.getRed() / 255.0 * factor));
    int g = (int) Math.min(255, Math.max(0, base.getGreen() * light.getGreen() / 255.0 * factor));
    int b = (int) Math.min(255, Math.max(0, base.getBlue() * light.getBlue() / 255.0 * factor));
    
    return new Color(r, g, b);
  }
  
  /**
   * Creates a Color object from double values with robust validation
   * Result is always fully opaque (alpha=255)
   */
  public static Color createColor(double r, double g, double b) {
    if (Double.isNaN(r) || Double.isNaN(g) || Double.isNaN(b)) {
      return BLACK;
    }
    
    return new Color(
      clamp((int)r),
      clamp((int)g),
      clamp((int)b)
    );
  }
  
  /**
   * Clamps double value to [0, 255] range and rounds to nearest integer
   */
  private static int clampAndRound(double value) {
    if (value > Double.MAX_VALUE / 2) return 255;
    if (value < -Double.MAX_VALUE / 2) return 0;
    
    double clamped = Math.max(0.0, Math.min(255.0, value));
    return (int) Math.round(clamped);
  }
  
  public static int clampColorValue(int value) {
    if (value < 0) {
      return 0;
    }
    if (value > 255) {
      return 255;
    }
    return value;
  }
  
  // Overload for float values
  public static Color createColor(float r, float g, float b) {
    return createColor((double) r, (double) g, (double) b);
  }
  
  // Overload for int values
  public static Color createColor(int r, int g, int b) {
    return new Color(
      Math.min(255, Math.max(0, r)),
      Math.min(255, Math.max(0, g)),
      Math.min(255, Math.max(0, b))
    );
  }
  
  // Interpolate between two colors
  public static Color interpolateColor(Color c1, Color c2, double t) {
    return blendColors(c1, c2, t);
  }
  
  // Combine multiple colors (additive blending)
  public static Color combineColors(Color... colors) {
    int r = 0, g = 0, b = 0;
    for (Color c : colors) {
      r = Math.min(255, r + c.getRed());
      g = Math.min(255, g + c.getGreen());
      b = Math.min(255, b + c.getBlue());
    }
    return new Color(r, g, b);
  }
  
  // Add noise/variation to a color
  public static Color addColorVariation(Color color, double variation) {
    double noise = 0.9 + Math.sin(variation * 15.0) * 0.1;
    int r = (int)(color.getRed() * noise);
    int g = (int)(color.getGreen() * noise);
    int b = (int)(color.getBlue() * noise);
    return new Color(clamp(r), clamp(g), clamp(b));
  }
  
  // Darken a color by specified amount (0.0 - 1.0)
  public static Color darkenColor(Color color, double amount) {
    amount = Math.max(0, Math.min(1, amount));
    int r = (int)(color.getRed() * (1 - amount));
    int g = (int)(color.getGreen() * (1 - amount));
    int b = (int)(color.getBlue() * (1 - amount));
    return new Color(clamp(r), clamp(g), clamp(b));
  }
  
  // Lighten a color by specified amount (0.0 - 1.0)
  public static Color lightenColor(Color color, double amount) {
    amount = Math.max(0, Math.min(1, amount));
    int r = (int)(color.getRed() + (255 - color.getRed()) * amount);
    int g = (int)(color.getGreen() + (255 - color.getGreen()) * amount);
    int b = (int)(color.getBlue() + (255 - color.getBlue()) * amount);
    return new Color(clamp(r), clamp(g), clamp(b));
  }
  
  /**
   * Color addition (RGB only)
   * Result is always fully opaque (alpha=255)
   */
  public static Color add(Color c1, Color c2) {
    return new Color(
      Math.min(255, c1.getRed() + c2.getRed()),
      Math.min(255, c1.getGreen() + c2.getGreen()),
      Math.min(255, c1.getBlue() + c2.getBlue())
    );
  }
  
  /**
   * Extracts float components [R,G,B] from AWT Color (0.0-1.0 range)
   */
  public static float[] getFloatComponents(Color color) {
    float[] comp = new float[3];
    comp[0] = color.getRed() / 255.0f;
    comp[1] = color.getGreen() / 255.0f;
    comp[2] = color.getBlue() / 255.0f;
    return comp;
  }
  
  /**
   * Adds specular highlight effect to a color based on intensity
   * Result is always fully opaque (alpha=255)
   */
  public static Color addSpecularHighlight(Color baseColor, double intensity) {
    intensity = Math.max(0, Math.min(1, intensity));
    int r = (int)(baseColor.getRed() + (255 - baseColor.getRed()) * intensity);
    int g = (int)(baseColor.getGreen() + (255 - baseColor.getGreen()) * intensity);
    int b = (int)(baseColor.getBlue() + (255 - baseColor.getBlue()) * intensity);
    return new Color(clamp(r), clamp(g), clamp(b));
  }
  
  /**
   * Adds specular highlight with custom highlight color
   * Result is always fully opaque (alpha=255)
   */
  public static Color addSpecularHighlight(Color baseColor, Color highlightColor, double intensity) {
    intensity = Math.max(0, Math.min(1, intensity));
    int r = (int)(baseColor.getRed() + (highlightColor.getRed() - baseColor.getRed()) * intensity);
    int g = (int)(baseColor.getGreen() + (highlightColor.getGreen() - baseColor.getGreen()) * intensity);
    int b = (int)(baseColor.getBlue() + (highlightColor.getBlue() - baseColor.getBlue()) * intensity);
    return new Color(clamp(r), clamp(g), clamp(b));
  }
  
  // Null-safe version of add
  public static Color addSafe(Color c1, Color c2) {
    if (c1 == null && c2 == null) return BLACK;
    if (c1 == null) return c2;
    if (c2 == null) return c1;
    return add(c1, c2);
  }
  
  /**
   * Clamps float value between [min, max]
   */
  public static float clamp(float value, float min, float max) {
    return Math.max(min, Math.min(max, value));
  }
  
  /**
   * Bilinear interpolation between four colors
   * Result is always fully opaque (alpha=255)
   */
  public static Color bilinearInterpolate(Color c00, Color c10, Color c01, Color c11, double tx, double ty) {
    int r = (int)((1-tx)*(1-ty)*c00.getRed() + tx*(1-ty)*c10.getRed() +
    (1-tx)*ty*c01.getRed() + tx*ty*c11.getRed());
    int g = (int)((1-tx)*(1-ty)*c00.getGreen() + tx*(1-ty)*c10.getGreen() +
    (1-tx)*ty*c01.getGreen() + tx*ty*c11.getGreen());
    int b = (int)((1-tx)*(1-ty)*c00.getBlue() + tx*(1-ty)*c10.getBlue() +
    (1-tx)*ty*c01.getBlue() + tx*ty*c11.getBlue());
    
    return new Color(clamp(r), clamp(g), clamp(b));
  }
  
  /**
   * Blends two colors with given ratio (0.0-1.0)
   * Result is always fully opaque (alpha=255)
   */
  public static Color blendColors(Color color1, Color color2, float ratio) {
    ratio = clamp(ratio, 0.0f, 1.0f);
    int r = (int)(color1.getRed() * (1 - ratio) + color2.getRed() * ratio);
    int g = (int)(color1.getGreen() * (1 - ratio) + color2.getGreen() * ratio);
    int b = (int)(color1.getBlue() * (1 - ratio) + color2.getBlue() * ratio);
    return new Color(clamp(r), clamp(g), clamp(b));
  }
  
  // Double ratio version
  public static Color blendColors(Color color1, Color color2, double ratio) {
    ratio = Math.max(0, Math.min(1, ratio));
    int r = (int)(color1.getRed() * (1-ratio) + color2.getRed() * ratio);
    int g = (int)(color1.getGreen() * (1-ratio) + color2.getGreen() * ratio);
    int b = (int)(color1.getBlue() * (1-ratio) + color2.getBlue() * ratio);
    return new Color(clamp(r), clamp(g), clamp(b));
  }
  
  /**
   * Smooth blending using smoothstep function
   * Result is always fully opaque (alpha=255)
   */
  public static Color smoothBlend(Color c1, Color c2, double ratio) {
    ratio = Math.max(0, Math.min(1, ratio));
    double smoothRatio = ratio * ratio * (3 - 2 * ratio);
    return blendColors(c1, c2, (float)smoothRatio);
  }
  
  /**
   * Calculates luminance (brightness) of color using ITU-R BT.709 standard
   */
  public static double luminance(Color color) {
    return (0.2126 * color.getRed() + 0.7152 * color.getGreen() + 0.0722 * color.getBlue()) / 255.0;
  }
  
  /**
   * Adjusts color contrast
   * Result is always fully opaque (alpha=255)
   */
  public static Color adjustContrast(Color color, float contrast) {
    float factor = (259f * (contrast + 255f)) / (255f * (259f - contrast));
    int red = adjustComponent(color.getRed(), factor);
    int green = adjustComponent(color.getGreen(), factor);
    int blue = adjustComponent(color.getBlue(), factor);
    return new Color(clamp(red), clamp(green), clamp(blue));
  }
  
  private static int adjustComponent(int component, float factor) {
    float normalized = component / 255f;
    float adjusted = 0.5f + factor * (normalized - 0.5f);
    return (int)(adjusted * 255f);
  }
  
  /**
   * Adjusts color brightness (exposure)
   * Result is always fully opaque (alpha=255)
   */
  public static Color adjustExposure(Color color, float exposure) {
    float[] rgb = getFloatComponents(color);
    return new Color(
      clamp(rgb[0] * exposure, 0f, 1f),
      clamp(rgb[1] * exposure, 0f, 1f),
      clamp(rgb[2] * exposure, 0f, 1f)
    );
  }
  
  /**
   * Adjusts color saturation
   * Result is always fully opaque (alpha=255)
   */
  public static Color adjustSaturation(Color color, float saturation) {
    float[] rgb = getFloatComponents(color);
    float luminance = 0.2126f * rgb[0] + 0.7152f * rgb[1] + 0.0722f * rgb[2];
    return new Color(
      clamp(luminance + (rgb[0] - luminance) * saturation, 0f, 1f),
      clamp(luminance + (rgb[1] - luminance) * saturation, 0f, 1f),
      clamp(luminance + (rgb[2] - luminance) * saturation, 0f, 1f)
    );
  }
  
  /**
   * Inverts color (negative)
   * Result is always fully opaque (alpha=255)
   */
  public static Color invert(Color color) {
    return new Color(
      255 - color.getRed(),
      255 - color.getGreen(),
      255 - color.getBlue()
    );
  }
  
  /**
   * Shifts hue in HSV color space
   * Result is always fully opaque (alpha=255)
   */
  public static Color shiftHue(Color color, float hueShift) {
    float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
    float newHue = (hsb[0] + hueShift/360f) % 1f;
    if (newHue < 0) newHue += 1f;
    return Color.getHSBColor(newHue, hsb[1], hsb[2]);
  }
  
  /**
   * Adjusts color temperature (warm/cool)
   * Result is always fully opaque (alpha=255)
   */
  public static Color adjustTemperature(Color color, float temperature) {
    temperature = clamp(temperature, -1f, 1f);
    float[] rgb = getFloatComponents(color);
    if (temperature > 0) {
      rgb[0] += temperature;
      rgb[1] += temperature * 0.5f;
      } else {
      rgb[2] -= temperature;
    }
    return new Color(
      clamp(rgb[0], 0f, 1f),
      clamp(rgb[1], 0f, 1f),
      clamp(rgb[2], 0f, 1f)
    );
  }
  
  /**
   * Converts to black and white based on threshold
   * Result is always fully opaque (alpha=255)
   */
  public static Color toBlackAndWhite(Color color, int threshold) {
    int luminance = (int)(luminance(color) * 255);
    return luminance > threshold ? WHITE : BLACK;
  }
  
  /**
   * Converts to sepia tone
   * Result is always fully opaque (alpha=255)
   */
  public static Color toSepia(Color color) {
    int r = color.getRed();
    int g = color.getGreen();
    int b = color.getBlue();
    int tr = (int)(0.393 * r + 0.769 * g + 0.189 * b);
    int tg = (int)(0.349 * r + 0.686 * g + 0.168 * b);
    int tb = (int)(0.272 * r + 0.534 * g + 0.131 * b);
    return new Color(clamp(tr), clamp(tg), clamp(tb));
  }
  
  /**
   * Calculates Euclidean distance between two colors
   */
  public static double colorDistance(Color c1, Color c2) {
    double rDiff = c1.getRed() - c2.getRed();
    double gDiff = c1.getGreen() - c2.getGreen();
    double bDiff = c1.getBlue() - c2.getBlue();
    return Math.sqrt(rDiff*rDiff + gDiff*gDiff + bDiff*bDiff);
  }
  
  /**
   * Sets new alpha value for existing color
   * This is the ONLY method that handles alpha - for compatibility
   */
  public static Color setAlpha(Color color, int alpha) {
    alpha = clamp(alpha, 0, 255);
    return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
  }
  
  /**
   * Sets new alpha value (float 0.0-1.0)
   * This is the ONLY method that handles alpha - for compatibility
   */
  public static Color setAlpha(Color color, float alpha) {
    alpha = clamp(alpha, 0.0f, 1.0f);
    return new Color(
      color.getRed() / 255f,
      color.getGreen() / 255f,
      color.getBlue() / 255f,
      alpha
    );
  }
  
  /**
   * Clamp integer value to [0,255]
   */
  public static int clamp(int value, int min, int max) {
    return Math.max(min, Math.min(max, value));
  }
  
  public static int clamp(int value) {
    return Math.max(0, Math.min(255, value));
  }
  
  /**
   * Scales color by factor (0.0-1.0)
   * Result is always fully opaque (alpha=255)
   */
  public static Color scale(Color color, double factor) {
    factor = Math.max(0.0, Math.min(1.0, factor));
    int r = (int)(color.getRed() * factor);
    int g = (int)(color.getGreen() * factor);
    int b = (int)(color.getBlue() * factor);
    
    return new Color(clamp(r), clamp(g), clamp(b));
  }
  
  /**
   * Applies lighting model (diffuse only)
   * Result is always fully opaque (alpha=255)
   */
  public static Color applyLighting(Color baseColor, Color lightColor, double intensity, double NdotL) {
    int r = (int)(baseColor.getRed() * lightColor.getRed() / 255.0 * intensity * NdotL);
    int g = (int)(baseColor.getGreen() * lightColor.getGreen() / 255.0 * intensity * NdotL);
    int b = (int)(baseColor.getBlue() * lightColor.getBlue() / 255.0 * intensity * NdotL);
    return new Color(clamp(r), clamp(g), clamp(b));
  }
  
  /**
   * Applies lighting with ambient and diffuse components
   * Result is always fully opaque (alpha=255)
   */
  public static Color applyLightingX(Color base, Color light, double diffuse, double ambient) {
    int r = (int)((base.getRed() * (ambient + diffuse * light.getRed()/255.0)));
    int g = (int)((base.getGreen() * (ambient + diffuse * light.getGreen()/255.0)));
    int b = (int)((base.getBlue() * (ambient + diffuse * light.getBlue()/255.0)));
    return new Color(clamp(r), clamp(g), clamp(b));
  }
  
}
////////////////
