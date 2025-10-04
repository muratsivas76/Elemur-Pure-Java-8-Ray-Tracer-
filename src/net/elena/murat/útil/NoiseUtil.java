package net.elena.murat.util;

import net.elena.murat.math.Point3;
import net.elena.murat.math.Vector3;

public final class NoiseUtil {
  
  private static final int[] PERMUTATION = new int[512]; // 512-element array
  
  static {
    // Base permutation table (0-255)
    int[] temp = { 151,160,137,91,90,15,131,13,201,95,96,53,194,233,7,225,
      140,36,103,30,69,142,8,99,37,240,21,10,23,190,6,148,247,120,234,75,0,26,197,62,94,252,219,203,117,
      35,11,32,57,177,33,88,237,149,56,87,174,20,125,136,171,168,68,175,74,165,71,134,139,48,27,166,77,
      146,158,231,83,111,229,122,60,211,133,230,220,105,92,41,55,46,245,40,244,102,143,54,65,25,63,161,
      1,216,80,73,209,76,132,187,208,89,18,169,200,196,135,130,116,188,159,86,164,100,109,198,173,186,3,
      64,52,217,226,250,124,123,5,202,38,147,118,126,255,82,85,212,207,206,59,227,47,16,58,17,182,189,
      28,42,223,183,170,213,119,248,152,2,44,154,163,70,221,153,101,155,167,43,172,9,129,22,39,253,19,98,
      108,110,79,113,224,232,178,185,112,104,218,246,97,228,251,34,242,193,238,210,144,12,191,179,162,241,
      81,51,145,235,249,14,239,107,49,192,214,31,181,199,106,157,184,84,204,176,115,121,50,45,127,4,150,
    254,138,236,205,93,222,114,67,29,24,72,243,141,128,195,78,66,215,61,156,180 };
    
    // Copy and repeat the array
    System.arraycopy(temp, 0, PERMUTATION, 0, 256);
    System.arraycopy(temp, 0, PERMUTATION, 256, 256);
  }
  
  /**
   * 3D Perlin noise (returns value between -1.0 and 1.0)
   */
  public static double noise(Point3 point) {
    int xi = (int)Math.floor(point.x) & 255;
    int yi = (int)Math.floor(point.y) & 255;
    int zi = (int)Math.floor(point.z) & 255;
    
    double xf = point.x - Math.floor(point.x);
    double yf = point.y - Math.floor(point.y);
    double zf = point.z - Math.floor(point.z);
    
    double u = fade(xf);
    double v = fade(yf);
    double w = fade(zf);
    
    int aaa = PERMUTATION[PERMUTATION[PERMUTATION[xi] + yi] + zi];
    int aba = PERMUTATION[PERMUTATION[PERMUTATION[xi] + yi + 1] + zi];
    int aab = PERMUTATION[PERMUTATION[PERMUTATION[xi] + yi] + zi + 1];
    int abb = PERMUTATION[PERMUTATION[PERMUTATION[xi] + yi + 1] + zi + 1];
    int baa = PERMUTATION[PERMUTATION[PERMUTATION[xi + 1] + yi] + zi];
    int bba = PERMUTATION[PERMUTATION[PERMUTATION[xi + 1] + yi + 1] + zi];
    int bab = PERMUTATION[PERMUTATION[PERMUTATION[xi + 1] + yi] + zi + 1];
    int bbb = PERMUTATION[PERMUTATION[PERMUTATION[xi + 1] + yi + 1] + zi + 1];
    
    double x1 = lerp(grad(aaa, xf, yf, zf), grad(baa, xf-1, yf, zf), u);
    double x2 = lerp(grad(aba, xf, yf-1, zf), grad(bba, xf-1, yf-1, zf), u);
    double y1 = lerp(x1, x2, v);
    
    x1 = lerp(grad(aab, xf, yf, zf-1), grad(bab, xf-1, yf, zf-1), u);
    x2 = lerp(grad(abb, xf, yf-1, zf-1), grad(bbb, xf-1, yf-1, zf-1), u);
    double y2 = lerp(x1, x2, v);
    
    return lerp(y1, y2, w);
  }
  
  /**
   * Turbulence effect (Fractal noise)
   * @param point 3D point
   * @param octaves Number of noise layers
   */
  public static double turbulence(Point3 point, int octaves) {
    double value = 0.0;
    double size = 1.0;
    double totalAmplitude = 0.0;
    double amplitude = 1.0;
    
    for (int i = 0; i < octaves; i++) {
      value += amplitude * Math.abs(noise(new Point3(
            point.x / size,
            point.y / size,
            point.z / size
      )));
      totalAmplitude += amplitude;
      amplitude *= 0.5;
      size *= 0.5;
    }
    
    return value / totalAmplitude;
  }
  
  private static double fade(double t) {
    return t * t * t * (t * (t * 6 - 15) + 10);
  }
  
  private static double lerp(double a, double b, double t) {
    return a + t * (b - a);
  }
  
  private static double grad(int hash, double x, double y, double z) {
    int h = hash & 15;
    double u = h < 8 ? x : y;
    double v = h < 4 ? y : h == 12 || h == 14 ? x : z;
    return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
  }
  
  /**
   * Planar noise (2D)
   */
  public static double noise(double x, double y) {
    return noise(new Point3(x, y, 0));
  }
  
}
