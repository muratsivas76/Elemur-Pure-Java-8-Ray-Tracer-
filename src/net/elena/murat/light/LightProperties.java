package net.elena.murat.light;

import java.awt.Color;

import net.elena.murat.math.Point3;
import net.elena.murat.math.Vector3;

public class LightProperties {
  public final Vector3 direction;
  public final Color color;
  public final double intensity;
  
  public LightProperties(Vector3 direction, Color color, double intensity) {
    this.direction = direction;
    this.color = color;
    this.intensity = intensity;
  }
  
  // Factory method for ambient light
  public static LightProperties createAmbient(Color color, double intensity) {
    return new LightProperties(new Vector3(0, 0, 0), color, intensity);
  }
  
  // Factory method for directional light
  public static LightProperties createDirectional(Vector3 direction, Color color, double intensity) {
    return new LightProperties(direction.negate().normalize(), color, intensity);
  }
  
  // Factory method for point light
  public static LightProperties createPointLight(Vector3 lightPos, Point3 surfacePoint, Color color, double intensity) {
    Vector3 dir = lightPos.subtract(surfacePoint).normalize();
    return new LightProperties(dir, color, intensity);
  }
  
  // Null object pattern for safety
  public static LightProperties nullProperties() {
    return new LightProperties(new Vector3(0, 1, 0), Color.BLACK, 0.0);
  }
  
  public static final LightProperties getLightProperties(Light light, Point3 point) {
    if (light == null) return nullProperties();
    
    if (light instanceof ElenaMuratAmbientLight) {
      return createAmbient(light.getColor(), light.getIntensity());
    }
    
    try {
      if (light instanceof MuratPointLight) {
        return createPointLight(
          light.getPosition().toVector(), point, light.getColor(), light.getAttenuatedIntensity(point)
        );
      }
      else if (light instanceof ElenaDirectionalLight) {
        return createDirectional(
          ((ElenaDirectionalLight)light).getDirection(), light.getColor(), light.getIntensity()
        );
      }
      else {
        return new LightProperties(
          new Vector3(0, 1, 0), light.getColor(), Math.min(light.getIntensity(), 1.0)
        );
      }
      } catch (Exception e) {
      return nullProperties();
    }
  }
  
}
