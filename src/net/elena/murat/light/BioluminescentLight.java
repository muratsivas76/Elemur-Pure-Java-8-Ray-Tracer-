package net.elena.murat.light;

import java.awt.Color;
import java.util.Collections;
import java.util.List;

import net.elena.murat.math.Point3;
import net.elena.murat.math.Vector3;
import net.elena.murat.math.Ray;
import net.elena.murat.lovert.Scene;

public class BioluminescentLight implements Light {
  private final List<Point3> organismPositions;
  private final Color baseColor;
  private final double pulseSpeed;
  private final double baseIntensity;
  private final double attenuationFactor;
  private double currentTime;
  private static final double MIN_PULSE_INTENSITY = 0.7;
  private static final double PULSE_AMPLITUDE = 0.3;
  
  public BioluminescentLight(List<Point3> positions, Color color, double pulseSpeed) {
    this(positions, color, pulseSpeed, 0.8, 0.2);
  }
  
  public BioluminescentLight(List<Point3> positions, Color color,
    double pulseSpeed, double baseIntensity, double attenuationFactor) {
    if (positions == null || positions.isEmpty()) {
      throw new IllegalArgumentException("Organism positions cannot be null or empty");
    }
    this.organismPositions = Collections.unmodifiableList(positions);
    this.baseColor = color != null ? color : new Color(100, 255, 150);
    this.pulseSpeed = Math.max(0, pulseSpeed);
    this.baseIntensity = Math.max(0, baseIntensity);
    this.attenuationFactor = Math.max(0, attenuationFactor);
  }
  
  public void update(double deltaTime) {
    this.currentTime += deltaTime;
  }
  
  @Override
  public Point3 getPosition() {
    return organismPositions.get(0); // Reference position for the light
  }
  
  @Override
  public Color getColor() {
    double pulseFactor = MIN_PULSE_INTENSITY + PULSE_AMPLITUDE * Math.sin(currentTime * pulseSpeed);
    return new Color(
      clampColor(baseColor.getRed() * pulseFactor),
      clampColor(baseColor.getGreen() * pulseFactor),
      clampColor(baseColor.getBlue() * pulseFactor)
    );
  }
  
  @Override
  public double getIntensity() {
    return baseIntensity * (MIN_PULSE_INTENSITY + PULSE_AMPLITUDE * Math.sin(currentTime * pulseSpeed * 1.2));
  }
  
  @Override
  public Vector3 getDirectionAt(Point3 point) {
    Point3 closest = findClosestPosition(point);
    return closest.subtract(point).normalize();
  }
  
  @Override
  public Vector3 getDirectionTo(Point3 point) {
    Point3 closest = findClosestPosition(point);
    return point.subtract(closest).normalize();
  }
  
  @Override
  public double getAttenuatedIntensity(Point3 point) {
    double minDistance = findClosestPosition(point).distance(point);
    return getIntensity() / (1.0 + attenuationFactor * minDistance);
  }
  
  @Override
  public double getIntensityAt(Point3 point) {
    return getAttenuatedIntensity(point);
  }
  
  @Override
  public boolean isVisibleFrom(Point3 point, Scene scene) {
    Point3 closest = findClosestPosition(point);
    Vector3 lightDir = point.subtract(closest).normalize();
    double distance = closest.distance(point);
    
    Ray shadowRay = new Ray(
      point.add(lightDir.scale(Ray.EPSILON * 10)),
      lightDir
    );
    return !scene.intersects(shadowRay, distance - Ray.EPSILON);
  }
  
  public double getClosestDistance(Point3 point) {
    return findClosestPosition(point).distance(point);
  }
  
  private Point3 findClosestPosition(Point3 point) {
    return organismPositions.stream()
    .min((p1, p2) -> Double.compare(p1.distance(point), p2.distance(point)))
    .orElse(organismPositions.get(0));
  }
  
  private int clampColor(double value) {
    return (int) Math.max(0, Math.min(255, value));
  }
  
  // Utility methods
  public BioluminescentLight withPositions(List<Point3> newPositions) {
    return new BioluminescentLight(newPositions, baseColor, pulseSpeed, baseIntensity, attenuationFactor);
  }
  
  public BioluminescentLight withColor(Color newColor) {
    return new BioluminescentLight(organismPositions, newColor, pulseSpeed, baseIntensity, attenuationFactor);
  }
  
  public BioluminescentLight withPulseSpeed(double newSpeed) {
    return new BioluminescentLight(organismPositions, baseColor, newSpeed, baseIntensity, attenuationFactor);
  }
  
  public static BioluminescentLight createDefault() {
    return new BioluminescentLight(
      Collections.singletonList(new Point3(0, 0, 0)), // Tek elemanlı liste
      new Color(100, 255, 150),
      1.5
    );
  }
  
  @Override
  public String toString() {
    return String.format(
      "BioluminescentLight[positions=%d, color=%s, pulseSpeed=%.2f]",
      organismPositions.size(), baseColor, pulseSpeed
    );
  }
  
}
