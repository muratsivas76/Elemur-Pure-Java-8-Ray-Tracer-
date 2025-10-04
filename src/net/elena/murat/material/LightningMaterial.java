package net.elena.murat.material;

import java.awt.Color;
import java.util.Random;

import net.elena.murat.light.Light;
import net.elena.murat.math.*;

public class LightningMaterial implements Material {
  private final Color baseColor;
  private final double intensity;
  private final Random random;
  private long lastStrikeTime;
  private double[][] lightningPath;
  
  public LightningMaterial() {
    this(new Color(135, 206, 250), 1.5); // Electric blue
  }
  
  public LightningMaterial(Color baseColor, double intensity) {
    this.baseColor = baseColor;
    this.intensity = Math.max(0.5, Math.min(5.0, intensity));
    this.random = new Random();
    this.lastStrikeTime = System.currentTimeMillis();
    generateLightningPath();
  }
  
  private void generateLightningPath() {
    // Lichtenberg figure algorithm (fractal lightning)
    int segments = 50;
    lightningPath = new double[segments][3]; // x,y,z
    
    double x = 0, y = 1, z = 0; // Start from ceiling
    lightningPath[0] = new double[]{x, y, z};
    
    for (int i = 1; i < segments; i++) {
      // Random direction change (fractal branching)
      x += (random.nextDouble() - 0.5) * 0.3;
      y -= random.nextDouble() * 0.2; // Downward
      z += (random.nextDouble() - 0.5) * 0.1;
      
      lightningPath[i] = new double[]{x, y, z};
    }
  }
  
  @Override
  public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPos) {
    // 1. Lightning refresh check (at random intervals)
    long currentTime = System.currentTimeMillis();
    if (currentTime - lastStrikeTime > 2000 + random.nextInt(3000)) {
      generateLightningPath();
      lastStrikeTime = currentTime;
    }
    
    // 2. Find closest lightning segment
    double minDist = Double.MAX_VALUE;
    for (int i = 0; i < lightningPath.length - 1; i++) {
      double dist = distanceToLineSegment(
        point,
        new Point3(lightningPath[i][0], lightningPath[i][1], lightningPath[i][2]),
        new Point3(lightningPath[i+1][0], lightningPath[i+1][1], lightningPath[i+1][2])
      );
      minDist = Math.min(minDist, dist);
    }
    
    // 3. Calculate brightness (inverse square law)
    double brightness = intensity / (1.0 + 100 * minDist * minDist);
    
    // 4. Flicker effect
    double flicker = 0.8 + 0.2 * Math.sin(currentTime * 0.05);
    
    return new Color(
      (int)(baseColor.getRed() * brightness * flicker),
      (int)(baseColor.getGreen() * brightness * flicker),
      (int)(baseColor.getBlue() * brightness * flicker),
      (int)(255 * Math.min(1, brightness * 2)) // Alpha
    );
  }
  
  private double distanceToLineSegment(Point3 p, Point3 a, Point3 b) {
    Vector3 ap = p.subtract(a);
    Vector3 ab = b.subtract(a);
    
    double projection = ap.dot(ab) / ab.dot(ab);
    projection = Math.max(0, Math.min(1, projection));
    
    Point3 closest = a.add(ab.scale(projection));
    return p.distance(closest);
  }
  
  @Override public double getReflectivity() { return 0.3; }
  @Override public double getIndexOfRefraction() { return 1.0; }
  @Override public double getTransparency() { return 0.8; }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
  }
  
}

/***
// 1. Create black ceiling
EMShape ceiling = new Plane(new Point3(0, 5, 0), new Vector3(0, -1, 0))
.setMaterial(new SolidColorMaterial(Color.BLACK));

// 2. Lightning material
LightningMaterial lightning = new LightningMaterial(
new Color(200, 230, 255), // Whiter lightning
2.5 // Intensity
);

// 3. Thin plane for visualization
EMShape lightningViz = new Plane()
.setTransform(Matrix4.scale(10, 10, 0.1).translate(0, 2.5, 0))
.setMaterial(lightning);

scene.add(ceiling);
scene.add(lightningViz);
 */
