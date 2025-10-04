package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.light.Light;
import net.elena.murat.math.*;

public class FractalFireMaterial implements Material {
  private final int iterations;
  private final double chaos;
  private final long startTime;
  private final double scale;
  private final double speed;
  
  public FractalFireMaterial(int iterations, double chaos, double scale, double speed) {
    this.iterations = Math.max(5, Math.min(30, iterations));  // Increased iterations for more detail
    this.chaos = Math.max(0.1, Math.min(2.0, chaos));         // Chaos parameter has wider range
    this.scale = Math.max(0.5, Math.min(3.0, scale));         // Scale is better adjusted
    this.speed = Math.max(0.1, Math.min(2.0, speed));        // Animation speed added
    this.startTime = System.currentTimeMillis();
  }
  
  @Override
  public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPos) {
    // 1. Time-based animation (for smoother movement)
    double time = (System.currentTimeMillis() - startTime) * 0.0005 * speed;
    
    // 2. Scale the point and offset from center (for more interesting patterns)
    double x = (point.x - 0.5) * scale;
    double y = (point.y - 0.5) * scale;
    double z = (point.z - 0.5) * scale * 0.5;  // Z-axis for 3D effect
    
    // 3. Dynamic chaos parameters (for more lively fire effect)
    double cx = -0.7 + Math.sin(time * 0.7) * chaos;
    double cy = 0.27 + Math.cos(time * 0.5) * chaos;
    double cz = Math.sin(time * 0.3) * chaos * 0.5;
    
    // 4. 3D Fractal calculation (Julia Set + Perlin noise-like variation)
    int i;
    for (i = 0; i < iterations; i++) {
      double nx = x * x - y * y - z * z + cx;
      double ny = 2 * x * y + cy;
      double nz = 2 * x * z + cz;
      x = nx;
      y = ny;
      z = nz;
      if (x * x + y * y + z * z > 4) break;
    }
    
    // 5. Color palette (fire-like tones)
    double ratio = (double) i / iterations;
    int r = (int) (255 * Math.min(1, 0.3 + ratio * 3.0));  // Bright red/orange
    int g = (int) (255 * Math.min(1, ratio * 1.5));        // Yellow tones
    int b = (int) (255 * Math.min(1, ratio * 0.3));         // Dark red
    
    // 6. Lighting (more realistic reflection)
    if (light != null && light.getPosition() != null) {
      Vector3 lightDir = light.getPosition().subtract(point).normalize();
      double dot = Math.max(0.2, normal.dot(lightDir));  // Added minimum lighting
      r = (int) (r * dot);
      g = (int) (g * dot);
      b = (int) (b * dot);
    }
    
    // 7. Final color adjustments (more vibrant colors)
    r = Math.min(255, r + 20);  // Slightly brighter
    g = Math.min(255, g + 10);
    b = Math.max(0, b - 10);    // Reduce blue
    
    return new Color(r, g, b);
  }
  
  @Override
  public double getReflectivity() { return 0.1; }  // Slight reflection
  @Override
  public double getIndexOfRefraction() { return 1.0; }
  @Override
  public double getTransparency() { return 0.0; }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
  }
  
}

/***
// Example usage:
EMShape fireSphere = new Sphere()
.setMaterial(new FractalFireMaterial(
20,     // iterations (higher means more detailed)
1.2,    // chaos (higher means more "scattered" fire)
1.5,    // scale (smaller values give larger patterns)
0.8     // speed (1.0 = normal speed)
));

// Don't forget to add light!
scene.addLight(new PointLight(
new Point3(2, 5, 3),  // Light position
new Color(255, 200, 150),  // Warm color (white-yellow)
1.5  // Light intensity
));
 */
