package net.elena.murat.light;

import java.awt.Color;

import net.elena.murat.math.Point3;
import net.elena.murat.math.Vector3;
import net.elena.murat.math.Ray;
import net.elena.murat.lovert.Scene;

public class ElenaDirectionalLight implements Light {
  private final Vector3 direction;
  private final Color color;
  private final double intensity;
  private static final double MIN_DIRECTION_LENGTH = 1e-6;
  
  public ElenaDirectionalLight(Vector3 direction, Color color, double intensity) {
    if (direction == null || direction.length() < MIN_DIRECTION_LENGTH) {
      throw new IllegalArgumentException("Direction cannot be null or zero-length vector");
    }
    this.direction = direction.normalize();
    this.color = color != null ? color : Color.WHITE;
    this.intensity = Math.max(0, intensity);
  }
  
  @Override
  public Point3 getPosition() {
    return null; // Directional lights have no position
  }
  
  @Override
  public Color getColor() {
    return color;
  }
  
  @Override
  public double getIntensity() {
    return intensity;
  }
  
  @Override
  public Vector3 getDirectionAt(Point3 point) {
    return direction.negate();
  }
  
  @Override
  public Vector3 getDirectionTo(Point3 point) {
    return direction;
  }
  
  @Override
  public double getAttenuatedIntensity(Point3 point) {
    return intensity; // No distance attenuation
  }
  
  @Override
  public double getIntensityAt(Point3 point) {
    return intensity; // Uniform intensity everywhere
  }
  
  @Override
  public boolean isVisibleFrom(Point3 point, Scene scene) {
    Ray shadowRay = new Ray(
      point.add(direction.scale(Ray.EPSILON * 10)),
      direction
    );
    return !scene.intersects(shadowRay, Double.POSITIVE_INFINITY);
  }
  
  // Additional utility methods
  public Vector3 getDirection() {
    return direction;
  }
  
  public ElenaDirectionalLight withDirection(Vector3 newDirection) {
    return new ElenaDirectionalLight(newDirection, color, intensity);
  }
  
  public ElenaDirectionalLight withColor(Color newColor) {
    return new ElenaDirectionalLight(direction, newColor, intensity);
  }
  
  public ElenaDirectionalLight withIntensity(double newIntensity) {
    return new ElenaDirectionalLight(direction, color, newIntensity);
  }
  
  public static ElenaDirectionalLight createDefault() {
    return new ElenaDirectionalLight(
      new Vector3(-1, -1, -1).normalize(),
      new Color(255, 255, 230),
      0.8
    );
  }
  
  @Override
  public String toString() {
    return String.format(
      "DirectionalLight[direction=%s, color=%s, intensity=%.2f]",
      direction, color, intensity
    );
  }
  
}

/***
// light like sun (from above)
ElenaDirectionalLight sunLight = new ElenaDirectionalLight(
new Vector3(0, -1, 0.2).normalize(),
new Color(255, 240, 220), // hot white
1.2
);

// Ligt morning (color more hot)
ElenaDirectionalLight morningLight = sunLight
.withColor(new Color(255, 220, 180))
.withIntensity(0.8);
 */
