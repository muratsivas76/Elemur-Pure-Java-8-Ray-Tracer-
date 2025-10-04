package net.elena.murat.util;

import java.util.Random;

import net.elena.murat.math.Vector3;
import net.elena.murat.math.Point3;

/**
 * Mathematical helper functions for 3D graphics and ray tracing.
 * All methods are thread-safe and deterministic.
 */
public final class MathUtil {
  
  // Mathematical constants
  public static final double PI = Math.PI;
  public static final double TWO_PI = 2.0 * PI;
  public static final double INV_PI = 1.0 / PI;
  public static final double EPSILON = 1e-8;
  public static final double GOLDEN_RATIO = 1.618033988749895;
  
  private static final Random RAND = new Random();
  private static final int[] PRIMES = {2, 3, 5, 7, 11, 13, 17, 19, 23, 29};
  
  private MathUtil() {} // Prevent instantiation
  
  // --- Basic Math Functions ---
  
  /**
   * Clamps value between [min, max] range.
   */
  public static double clamp(double value, double min, double max) {
    return Math.max(min, Math.min(max, value));
  }
  
  /**
   * Converts degrees to radians.
   */
  public static double radians(double degrees) {
    return degrees * PI / 180.0;
  }
  
  /**
   * Converts radians to degrees.
   */
  public static double degrees(double radians) {
    return radians * 180.0 / PI;
  }
  
  /**
   * Linear interpolation (lerp).
   */
  public static double lerp(double a, double b, double t) {
    return a + t * (b - a);
  }
  
  // --- Random Number Generation ---
  
  /**
   * Deterministic random number in [0,1) range.
   * @param seed Complex seed value
   */
  public static double random(double seed) {
    double x = Math.sin(seed * 12.9898 + 78.233) * 43758.5453;
    return x - Math.floor(x);
  }
  
  /**
   * Random point inside 3D unit sphere.
   */
  public static Vector3 randomInUnitSphere(double seed) {
    double phi = TWO_PI * random(seed + PRIMES[0]);
    double costheta = 2.0 * random(seed + PRIMES[1]) - 1.0;
    double theta = Math.acos(costheta);
    double r = Math.cbrt(random(seed + PRIMES[2]));
    
    return new Vector3(
      r * Math.sin(theta) * Math.cos(phi),
      r * Math.sin(theta) * Math.sin(phi),
      r * Math.cos(theta)
    );
  }
  
  // --- Vector and Point Operations ---
  
  /**
   * Calculates ray-plane intersection.
   * @param rayOrigin Ray starting point
   * @param rayDir Ray direction (normalized)
   * @param planePoint Point on the plane
   * @param planeNormal Plane normal (normalized)
   * @return Intersection distance (Double.POSITIVE_INFINITY if parallel)
   */
  public static double rayPlaneIntersect(
    Point3 rayOrigin, Vector3 rayDir,
    Point3 planePoint, Vector3 planeNormal) {
    
    double denom = planeNormal.dot(rayDir);
    if (Math.abs(denom) > EPSILON) {
      Vector3 diff = planePoint.subtract(rayOrigin);
      return diff.dot(planeNormal) / denom;
    }
    return Double.POSITIVE_INFINITY;
  }
  
  /**
   * Ray-triangle intersection (Möller-Trumbore algorithm).
   */
  public static Double rayTriangleIntersect(
    Point3 rayOrigin, Vector3 rayDir,
    Point3 v0, Point3 v1, Point3 v2) {
    
    Vector3 edge1 = v1.subtract(v0);
    Vector3 edge2 = v2.subtract(v0);
    Vector3 h = rayDir.cross(edge2);
    double a = edge1.dot(h);
    
    if (a > -EPSILON && a < EPSILON) {
      return null; // Ray parallel to plane
    }
    
    double f = 1.0 / a;
    Vector3 s = rayOrigin.subtract(v0);
    double u = f * s.dot(h);
    
    if (u < 0.0 || u > 1.0) {
      return null;
    }
    
    Vector3 q = s.cross(edge1);
    double v = f * rayDir.dot(q);
    
    if (v < 0.0 || u + v > 1.0) {
      return null;
    }
    
    double t = f * edge2.dot(q);
    return t > EPSILON ? t : null;
  }
  
  // --- Noise Functions ---
  
  /**
   * Hash function for Perlin noise.
   */
  public static int noiseHash(int x, int y, int z) {
    final int X_NOISE = 1619;
    final int Y_NOISE = 31337;
    final int Z_NOISE = 6971;
    final int SEED = 1013;
    
    int hash = (x * X_NOISE) ^ (y * Y_NOISE) ^ (z * Z_NOISE);
    hash = hash * hash * hash * SEED;
    return (hash >> 13) ^ hash;
  }
  
  /**
   * 3D Perlin noise (between -1 and 1).
   */
  public static double perlinNoise(double x, double y, double z) {
    // Simplified Perlin noise implementation
    int xi = (int)Math.floor(x) & 255;
    int yi = (int)Math.floor(y) & 255;
    int zi = (int)Math.floor(z) & 255;
    
    double xf = x - Math.floor(x);
    double yf = y - Math.floor(y);
    double zf = z - Math.floor(z);
    
    // Actual Perlin noise calculation would go here
    // For simplicity, returning random value
    return random(xi + yi * 256 + zi * 65536) * 2 - 1;
  }
  
  // --- Special Mathematical Functions ---
  
  /**
   * Filters near-zero values.
   */
  public static double nearZero(double value) {
    return Math.abs(value) < EPSILON ? 0.0 : value;
  }
  
  /**
   * Fresnel equation (Schlick approximation).
   * @param cosTheta Cosine of incident angle
   * @param refIdx Refractive index
   */
  public static double fresnelSchlick(double cosTheta, double refIdx) {
    double r0 = (1 - refIdx) / (1 + refIdx);
    r0 = r0 * r0;
    return r0 + (1 - r0) * Math.pow(1 - cosTheta, 5);
  }
  
  /**
   * GGX distribution function (PBR specular).
   */
  public static double ggxDistribution(double NdotH, double roughness) {
    double a = roughness * roughness;
    double a2 = a * a;
    double denom = (NdotH * NdotH * (a2 - 1.0) + 1.0);
    return a2 / (PI * denom * denom);
  }
  
  /**
   * Smith shadowing function.
   */
  public static double smithG1(double NdotV, double roughness) {
    double k = (roughness + 1.0) * (roughness + 1.0) / 8.0;
    return NdotV / (NdotV * (1.0 - k) + k);
  }
  
  // --- Coordinate Transformations ---
  
  /**
   * Spherical to Cartesian coordinate conversion.
   */
  public static Vector3 sphericalToCartesian(double r, double theta, double phi) {
    double sinTheta = Math.sin(theta);
    return new Vector3(
      r * sinTheta * Math.cos(phi),
      r * sinTheta * Math.sin(phi),
      r * Math.cos(theta)
    );
  }
  
  /**
   * Cartesian to spherical coordinate conversion.
   */
  public static double[] cartesianToSpherical(Vector3 v) {
    double r = v.length();
    return new double[] {
      r,
      Math.acos(v.z / r),
      Math.atan2(v.y, v.x)
    };
  }
  
}
