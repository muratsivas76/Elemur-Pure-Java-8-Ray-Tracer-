package net.elena.murat.light;

import java.awt.Color;

import net.elena.murat.math.*;
import net.elena.murat.lovert.Scene;

public class SpotLight implements Light {
  private final Point3 position;
  private final Vector3 direction;
  private final Color color;
  private final double intensity;
  private final double cosInnerCone;
  private final double cosOuterCone;
  private final double constantAttenuation;
  private final double linearAttenuation;
  private final double quadraticAttenuation;
  
  public SpotLight(Point3 position, Vector3 direction, Color color,
    double intensity, double innerConeAngle, double outerConeAngle) {
    this(position, direction, color, intensity, innerConeAngle, outerConeAngle,
    1.0, 0.1, 0.01);
  }
  
  public SpotLight(Point3 position, Vector3 direction, Color color,
    double intensity, double innerConeAngle, double outerConeAngle,
    double constantAttenuation, double linearAttenuation, double quadraticAttenuation) {
    this.position = position;
    this.direction = direction.normalize();
    this.color = color;
    this.intensity = intensity;
    this.cosInnerCone = Math.cos(Math.toRadians(innerConeAngle/2));
    this.cosOuterCone = Math.cos(Math.toRadians(outerConeAngle/2));
    this.constantAttenuation = constantAttenuation;
    this.linearAttenuation = linearAttenuation;
    this.quadraticAttenuation = quadraticAttenuation;
  }
  
  @Override
  public Point3 getPosition() {
    return position;
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
    return position.subtract(point).normalize();
  }
  
  @Override
  public Vector3 getDirectionTo(Point3 point) {
    return point.subtract(position).normalize();
  }
  
  @Override
  public double getAttenuatedIntensity(Point3 point) {
    double distance = position.distance(point);
    double attenuation = constantAttenuation +
    linearAttenuation * distance +
    quadraticAttenuation * distance * distance;
    double coneFactor = calculateConeFactor(point);
    return intensity * coneFactor / Math.max(attenuation, Ray.EPSILON);
  }
  
  @Override
  public double getIntensityAt(Point3 point) {
    return getAttenuatedIntensity(point);
  }
  
  @Override
  public boolean isVisibleFrom(Point3 point, Scene scene) {
    Vector3 lightDir = getDirectionTo(point);
    double distance = getDistanceTo(point);
    Ray shadowRay = new Ray(
      point.add(lightDir.scale(Ray.EPSILON * 10)),
      lightDir
    );
    return !scene.intersects(shadowRay, distance - Ray.EPSILON);
  }
  
  public double getDistanceTo(Point3 point) {
    return position.distance(point);
  }
  
  private double calculateConeFactor(Point3 point) {
    Vector3 lightToPoint = getDirectionTo(point);
    double dot = lightToPoint.dot(direction);
    
    if (dot >= cosInnerCone) return 1.0;
    if (dot <= cosOuterCone) return 0.0;
    
    return (dot - cosOuterCone) / (cosInnerCone - cosOuterCone);
  }
  
  public double getInnerConeAngle() {
    return Math.toDegrees(Math.acos(cosInnerCone)) * 2;
  }
  
  public double getOuterConeAngle() {
    return Math.toDegrees(Math.acos(cosOuterCone)) * 2;
  }
  
}
