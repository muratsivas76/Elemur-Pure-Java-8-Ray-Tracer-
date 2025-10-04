package net.elena.murat.light;

import java.awt.Color;

import net.elena.murat.math.Point3;
import net.elena.murat.math.Vector3;
import net.elena.murat.math.Ray;
import net.elena.murat.lovert.Scene;

public class BlackHoleLight implements Light {
  private final Point3 singularity;
  private final double eventHorizonRadius;
  private final Color accretionColor;
  private final double baseIntensity;
  private static final double GRAVITATIONAL_WARP_FACTOR = 2.0;
  
  public BlackHoleLight(Point3 singularity, double radius, Color color) {
    this(singularity, radius, color, 1.5);
  }
  
  public BlackHoleLight(Point3 singularity, double radius, Color color, double intensity) {
    if (singularity == null) {
      throw new IllegalArgumentException("Singularity point cannot be null");
    }
    this.singularity = singularity;
    this.eventHorizonRadius = Math.max(0.1, radius);
    this.accretionColor = color != null ? color : new Color(200, 150, 255);
    this.baseIntensity = Math.max(0, intensity);
  }
  
  @Override
  public Point3 getPosition() {
    return singularity;
  }
  
  @Override
  public Color getColor() {
    return accretionColor;
  }
  
  @Override
  public double getIntensity() {
    return baseIntensity;
  }
  
  @Override
  public Vector3 getDirectionAt(Point3 point) {
    Vector3 dir = singularity.subtract(point);
    double dist = dir.length();
    if (dist < Ray.EPSILON) {
      return new Vector3(0, 0, 0);
    }
    double warpFactor = GRAVITATIONAL_WARP_FACTOR / (1.0 - Math.exp(-dist/eventHorizonRadius));
    return dir.normalize().multiply(warpFactor);
  }
  
  @Override
  public Vector3 getDirectionTo(Point3 point) {
    Vector3 dir = point.subtract(singularity);
    double dist = dir.length();
    if (dist < Ray.EPSILON) {
      return new Vector3(0, 0, 0);
    }
    return dir.normalize();
  }
  
  @Override
  public double getAttenuatedIntensity(Point3 point) {
    double distance = singularity.distance(point);
    if (distance < eventHorizonRadius) {
      return 0.0;
    }
    return baseIntensity * (1.0 - eventHorizonRadius/distance);
  }
  
  @Override
  public double getIntensityAt(Point3 point) {
    return getAttenuatedIntensity(point);
  }
  
  @Override
  public boolean isVisibleFrom(Point3 point, Scene scene) {
    if (isPointBeyondEventHorizon(point)) {
      return false;
    }
    Vector3 lightDir = getDirectionTo(point);
    Ray shadowRay = new Ray(
      point.add(lightDir.scale(Ray.EPSILON * 10)),
      lightDir
    );
    return !scene.intersects(shadowRay, Double.POSITIVE_INFINITY);
  }
  
  public double getEventHorizonRadius() {
    return eventHorizonRadius;
  }
  
  public boolean isPointBeyondEventHorizon(Point3 point) {
    return singularity.distance(point) < eventHorizonRadius;
  }
  
  // Utility methods
  public BlackHoleLight withSingularity(Point3 newSingularity) {
    return new BlackHoleLight(newSingularity, eventHorizonRadius, accretionColor, baseIntensity);
  }
  
  public BlackHoleLight withRadius(double newRadius) {
    return new BlackHoleLight(singularity, newRadius, accretionColor, baseIntensity);
  }
  
  public BlackHoleLight withColor(Color newColor) {
    return new BlackHoleLight(singularity, eventHorizonRadius, newColor, baseIntensity);
  }
  
  public BlackHoleLight withIntensity(double newIntensity) {
    return new BlackHoleLight(singularity, eventHorizonRadius, accretionColor, newIntensity);
  }
  
  @Override
  public String toString() {
    return String.format(
      "BlackHoleLight[singularity=%s, radius=%.2f, color=%s, intensity=%.2f]",
      singularity, eventHorizonRadius, accretionColor, baseIntensity
    );
  }
  
}
