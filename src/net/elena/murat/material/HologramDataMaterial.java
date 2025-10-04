package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.light.Light;
import net.elena.murat.math.*;

public class HologramDataMaterial implements Material {
  private final double dataDensity;
  private final int resolution;
  private final long startTime;
  
  public HologramDataMaterial(double dataDensity, int resolution) {
    this.dataDensity = Math.max(0.1, Math.min(1.0, dataDensity));
    this.resolution = Math.max(64, Math.min(512, resolution));
    this.startTime = System.currentTimeMillis();
  }
  
  @Override
  public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPos) {
    // 1. Grid position
    int gridX = (int)(point.x * resolution) % resolution;
    int gridY = (int)(point.z * resolution) % resolution;
    
    // 2. Time-based animation
    double time = (System.currentTimeMillis() - startTime) * 0.001;
    int animOffset = (int)(time * 10) % 10;
    
    // 3. Data pattern (ASCII art like)
    boolean isActive = (gridX + gridY + animOffset) % 4 == 0 &&
    Math.random() < dataDensity;
    
    // 4. Glitch effect
    double glitch = Math.sin(time * 3 + point.y * 10) * 0.1;
    
    return isActive ?
    new Color(
      (int)(100 + 155 * Math.abs(Math.sin(time + point.x))),
      (int)(200 + 55 * Math.abs(Math.cos(time + point.z))),
      255,
      180
    ) :
    new Color(0, 10, 20, 50); // Background color
  }
  
  @Override public double getReflectivity() { return 0.3; }
  @Override public double getIndexOfRefraction() { return 1.1; }
  @Override public double getTransparency() { return 0.8; }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
  }
  
}

/***
EMShape dataCube = new Cube()
.setMaterial(new HologramDataMaterial(0.7, 256));
 */
