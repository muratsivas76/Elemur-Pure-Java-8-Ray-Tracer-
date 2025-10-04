package net.elena.murat.lovert;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

//custom imports
import net.elena.murat.math.Intersection;
import net.elena.murat.math.Ray;
import net.elena.murat.shape.EMShape;
import net.elena.murat.light.Light;
import net.elena.murat.math.Point3;
import net.elena.murat.math.Vector3;

/**
 * Represents a 3D scene containing shapes and lights for ray tracing.
 * Handles intersection tests and light management.
 */
public class Scene {
  private final List<EMShape> shapes = new ArrayList<>();
  private final List<Light> lights = new ArrayList<>();
  
  /**
   * Adds a shape to the scene
   * @param shape The shape to add
   */
  public void addShape(EMShape shape) {
    shapes.add(shape);
  }
  
  /**
   * Adds a light source to the scene
   * @param light The light to add
   */
  public void addLight(Light light) {
    lights.add(light);
  }
  
  public List<EMShape> getShapes() {
    return new ArrayList<>(shapes); // Return copy for immutability
  }
  
  public List<Light> getLights() {
    return new ArrayList<>(lights); // Return copy for immutability
  }
  
  /**
   * Clears all shapes from the scene.
   */
  public void clearShapes() {
    shapes.clear();
  }
  
  /**
   * Clears all lights from the scene.
   */
  public void clearLights() {
    lights.clear();
  }
  
  /**
   * Finds the closest ray-object intersection in the scene
   * @param ray The ray to test
   * @return Optional containing closest intersection if found
   */
  public Optional<Intersection> intersect(Ray ray) {
    return intersect(ray, null); // No shape excluded by default
  }
  
  /**
   * Finds the closest ray-object intersection excluding a specific shape
   * @param ray The ray to test
   * @param excludeShape Shape to exclude from intersection tests
   * @return Optional containing closest intersection if found
   */
  public Optional<Intersection> intersect(Ray ray, EMShape excludeShape) {
    EMShape closestShape = null;
    double minDistance = Double.POSITIVE_INFINITY;
    Point3 closestHitPoint = null;
    
    for (EMShape shape : shapes) {
      if (shape == excludeShape) {
        continue;
      }
      
      double distance = shape.intersect(ray);
      
      if (distance < minDistance && distance > Ray.EPSILON) {
        minDistance = distance;
        closestShape = shape;
        closestHitPoint = ray.pointAtParameter(distance);
      }
    }
    
    if (closestShape != null) {
      Vector3 normal = closestShape.getNormalAt(closestHitPoint);
      // Normal orientation will be fixed during shading
      return Optional.of(new Intersection(
          closestHitPoint,
          normal,
          minDistance,
          closestShape
      ));
    }
    return Optional.empty();
  }
  
  public boolean intersects(Ray ray, double maxDistance) {
    for (EMShape shape : shapes) {
      double distance = shape.intersect(ray);
      if (distance > Ray.EPSILON && distance < maxDistance) {
        return true;
      }
    }
    return false;
  }
  
  /**
   * Checks if a point is visible from any light source (for soft shadows)
   * @param point The point to test
   * @return Number of lights visible from the point
   */
  public int getVisibleLightCount(Point3 point) {
    int count = 0;
    for (Light light : lights) {
      if (light.isVisibleFrom(point, this)) {
        count++;
      }
    }
    return count;
  }
}
