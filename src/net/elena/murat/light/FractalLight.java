package net.elena.murat.light;

import java.awt.Color;

import net.elena.murat.math.*;
import net.elena.murat.lovert.Scene;

public class FractalLight implements Light {
  private final Point3 position;
  private final Color baseColor;
  private final double baseIntensity;
  private final int octaves;
  private final double persistence;
  private final double frequency;
  private final double[] randoms;
  private final int[] permutations;
  
  public FractalLight(Point3 position, Color color, double intensity) {
    this(position, color, intensity, 4, 0.5, 0.1);
  }
  
  public FractalLight(Point3 position, Color color, double intensity,
    int octaves, double persistence, double frequency) {
    this.position = position;
    this.baseColor = color;
    this.baseIntensity = Math.max(0, intensity);
    this.octaves = Math.max(1, octaves);
    this.persistence = Math.max(0, Math.min(1, persistence));
    this.frequency = Math.max(0.001, frequency);
    this.randoms = new double[256];
    this.permutations = new int[512];
    
    initializeNoise();
  }
  
  private void initializeNoise() {
    for (int i = 0; i < 256; i++) {
      randoms[i] = Math.random() * 2 - 1;
      permutations[i] = i;
    }
    
    // Fisher-Yates shuffle
    for (int i = 255; i > 0; i--) {
      int index = (int)(Math.random() * (i + 1));
      int temp = permutations[i];
      permutations[i] = permutations[index];
      permutations[index] = temp;
    }
    
    // Duplicate for overflow
    System.arraycopy(permutations, 0, permutations, 256, 256);
  }
  
  @Override
  public Point3 getPosition() {
    return position;
  }
  
  @Override
  public Color getColor() {
    return baseColor;
  }
  
  @Override
  public double getIntensity() {
    return baseIntensity;
  }
  
  @Override
  public Vector3 getDirectionAt(Point3 point) {
    Vector3 direction = position.subtract(point);
    return direction.length() < Ray.EPSILON ? new Vector3(0,0,0) : direction.normalize();
  }
  
  @Override
  public double getAttenuatedIntensity(Point3 point) {
    double noise = fractalNoise(point.x, point.y, point.z);
    return baseIntensity * (0.3 + 0.7 * noise);
  }
  
  private double fractalNoise(double x, double y, double z) {
    double total = 0;
    double amplitude = 1.0;
    double maxAmplitude = 0;
    double freq = frequency;
    
    for (int i = 0; i < octaves; i++) {
      total += improvedNoise(x * freq, y * freq, z * freq) * amplitude;
      maxAmplitude += amplitude;
      amplitude *= persistence;
      freq *= 2.0;
    }
    
    return total / maxAmplitude;
  }
  
  private double improvedNoise(double x, double y, double z) {
    // Ken Perlin'in geliştirilmiş gürültü algoritması
    int xi = (int)Math.floor(x) & 255;
    int yi = (int)Math.floor(y) & 255;
    int zi = (int)Math.floor(z) & 255;
    
    double xf = x - Math.floor(x);
    double yf = y - Math.floor(y);
    double zf = z - Math.floor(z);
    
    double u = fade(xf);
    double v = fade(yf);
    double w = fade(zf);
    
    int aaa = permutations[permutations[permutations[xi] + yi] + zi];
    int aba = permutations[permutations[permutations[xi] + yi + 1] + zi];
    int aab = permutations[permutations[permutations[xi] + yi] + zi + 1];
    int abb = permutations[permutations[permutations[xi] + yi + 1] + zi + 1];
    int baa = permutations[permutations[permutations[xi + 1] + yi] + zi];
    int bba = permutations[permutations[permutations[xi + 1] + yi + 1] + zi];
    int bab = permutations[permutations[permutations[xi + 1] + yi] + zi + 1];
    int bbb = permutations[permutations[permutations[xi + 1] + yi + 1] + zi + 1];
    
    double x1 = lerp(u, grad(aaa, xf, yf, zf), grad(baa, xf-1, yf, zf));
    double x2 = lerp(u, grad(aba, xf, yf-1, zf), grad(bba, xf-1, yf-1, zf));
    double y1 = lerp(v, x1, x2);
    
    x1 = lerp(u, grad(aab, xf, yf, zf-1), grad(bab, xf-1, yf, zf-1));
    x2 = lerp(u, grad(abb, xf, yf-1, zf-1), grad(bbb, xf-1, yf-1, zf-1));
    double y2 = lerp(v, x1, x2);
    
    return (lerp(w, y1, y2) + 1) / 2; // [-1,1] -> [0,1] aralığına normalize
  }
  
  private double fade(double t) {
    return t * t * t * (t * (t * 6 - 15) + 10);
  }
  
  private double lerp(double t, double a, double b) {
    return a + t * (b - a);
  }
  
  private double grad(int hash, double x, double y, double z) {
    int h = hash & 15;
    double u = h < 8 ? x : y;
    double v = h < 4 ? y : (h == 12 || h == 14 ? x : z);
    return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
  }
  
  // Light interface diğer metodları
  @Override
  public Vector3 getDirectionTo(Point3 point) {
    Vector3 direction = point.subtract(position);
    return direction.length() < Ray.EPSILON ? new Vector3(0,0,0) : direction.normalize();
  }
  
  @Override
  public double getIntensityAt(Point3 point) {
    return getAttenuatedIntensity(point);
  }
  
  @Override
  public boolean isVisibleFrom(Point3 point, Scene scene) {
    Vector3 lightDir = getDirectionTo(point);
    double distance = position.distance(point);
    Ray shadowRay = new Ray(
      point.add(lightDir.scale(Ray.EPSILON * 10)),
      lightDir
    );
    return !scene.intersects(shadowRay, distance - Ray.EPSILON);
  }
  
}
