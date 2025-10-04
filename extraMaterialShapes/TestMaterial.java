import java.awt.Color;
import java.util.Random;

import net.elena.murat.material.Material;
import net.elena.murat.math.Point3;
import net.elena.murat.math.Vector3;
import net.elena.murat.light.Light;

public class TestMaterial implements Material {
  private final Random random = new Random();
  private final Color baseColor;
  private final double dotDensity;
  private final Color circleColor;
  private final double circleRadius;
  
  // Single empty constructor
  public TestMaterial() {
    this.baseColor = new Color(240, 240, 240); // Light gray base color
    this.dotDensity = 0.3;
    this.circleColor = new Color(255, 0, 0); // Red circles
    this.circleRadius = 0.15;
  }
  
  public TestMaterial(Color baseColor, double dotDensity) {
    this.baseColor = baseColor;
    this.dotDensity = dotDensity;
    this.circleColor = new Color(255, 0, 0); // Default red
    this.circleRadius = 0.15;
  }
  
  public TestMaterial(Color baseColor, double dotDensity, Color circleColor, double circleRadius) {
    this.baseColor = baseColor;
    this.dotDensity = dotDensity;
    this.circleColor = circleColor;
    this.circleRadius = circleRadius;
  }
  
  @Override
  public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPos) {
    // Create tile pattern based on world coordinates
    double scale = 8.0; // Pattern scale
    double u = point.x * scale;
    double v = point.z * scale;
    
    // Get fractional part for tile coordinates (0.0 to 1.0)
    double tileU = u - Math.floor(u);
    double tileV = v - Math.floor(v);
    
    // Check if current point is inside a circle
    if (isInsideCircle(tileU, tileV)) {
      return circleColor;
    }
    
    // Check for random dots
    if (hasRandomDot(tileU, tileV)) {
      return circleColor;
    }
    
    return baseColor;
  }
  
  /**
   * Checks if point is inside a centered circle
   */
  private boolean isInsideCircle(double u, double v) {
    double centerU = 0.5;
    double centerV = 0.5;
    
    // Calculate distance from center
    double dx = u - centerU;
    double dy = v - centerV;
    double distanceSquared = dx * dx + dy * dy;
    
    // Check if inside circle
    return distanceSquared <= (circleRadius * circleRadius);
  }
  
  /**
   * Generates random dots based on density
   */
  private boolean hasRandomDot(double u, double v) {
    // Use consistent random seed based on position
    long seed = (long) (Math.floor(u * 1000) + Math.floor(v * 1000) * 1000000);
    random.setSeed(seed);
    
    return random.nextDouble() < dotDensity;
  }
  
  /**
   * Alternative: Grid-based circles pattern
   */
  private Color getGridPattern(double u, double v) {
    // Create grid cells
    int gridSize = 4;
    double cellSize = 1.0 / gridSize;
    
    int cellX = (int) (u / cellSize);
    int cellY = (int) (v / cellSize);
    
    double cellCenterU = (cellX + 0.5) * cellSize;
    double cellCenterV = (cellY + 0.5) * cellSize;
    
    // Calculate distance from cell center
    double dx = u - cellCenterU;
    double dy = v - cellCenterV;
    double distance = Math.sqrt(dx * dx + dy * dy);
    
    // If within circle radius, return circle color
    if (distance <= circleRadius * cellSize) {
      return circleColor;
    }
    
    return baseColor;
  }
  
  @Override
  public double getReflectivity() {
    return 0.1;
  }
  
  @Override
  public double getIndexOfRefraction() {
    return 1.0;
  }
  
  @Override
  public double getTransparency() {
    return 0.0;
  }
  
  @Override
  public void setObjectTransform(net.elena.murat.math.Matrix4 tm) {
    // Not used in this material
  }
  
  // Getter methods for debugging
  public Color getBaseColor() {
    return baseColor;
  }
  
  public double getDotDensity() {
    return dotDensity;
  }
  
  public Color getCircleColor() {
    return circleColor;
  }
  
  public double getCircleRadius() {
    return circleRadius;
  }
  
}
// javac -parameters -cp ..\bin\elenaRT.jar; TestMaterial.java
