package net.elena.murat.material;

import java.lang.reflect.Method;

import java.awt.Color;

//custom imports
import net.elena.murat.light.*;
import net.elena.murat.math.*;
import net.elena.murat.util.ColorUtil;

/**
 * SquaredMaterial represents a surface with a two-color square pattern.
 * It now fully implements the extended Material interface, including properties
 * for reflectivity, index of refraction, and transparency.
 */
public class SquaredMaterial implements Material {
  
  private final Color color1;
  private final Color color2;
  private final double scale; // Represents the frequency of squares (squares per unit)
  private Matrix4 objectInverseTransform; // The inverse transformation matrix of the object
  
  // Phong lighting model parameters
  private final double ambient;
  private final double diffuse;
  private final double specular;
  private final double shininess;
  private final Color specularColor;
  private final double reflectivity;
  private final double ior;
  private final double transparency;
  
  /**
   * Constructs a SquaredMaterial with two colors, a scale, Phong lighting model parameters,
   * and the object's inverse transformation matrix.
   * @param color1 The first color for the square pattern.
   * @param color2 The second color for the square pattern.
   * @param scale The frequency of the squares (e.g., 4.0 for 4 squares per unit length).
   * @param ambient The ambient reflection coefficient (0.0-1.0).
   * @param diffuse The diffuse reflection coefficient (0.0-1.0).
   * @param specular The specular reflection coefficient (0.0-1.0).
   * @param shininess The shininess exponent for specular highlights.
   * @param specularColor The color of the specular highlight.
   * @param reflectivity The reflectivity coefficient (0.0-1.0).
   * @param ior The Index of Refraction for transparent materials.
   * @param transparency The transparency coefficient (0.0-1.0).
   * @param objectInverseTransform The full inverse transformation matrix of the object this material is applied to.
   */
  public SquaredMaterial(Color color1, Color color2, double scale,
    double ambient, double diffuse, double specular,
    double shininess, Color specularColor,
    double reflectivity, double ior, double transparency,
    Matrix4 objectInverseTransform) {
    this.color1 = color1;
    this.color2 = color2;
    this.scale = scale;
    this.objectInverseTransform = objectInverseTransform;
    
    this.ambient = ambient;
    this.diffuse = diffuse;
    this.specular = specular;
    this.shininess = shininess;
    this.specularColor = specularColor;
    this.reflectivity = reflectivity;
    this.ior = ior;
    this.transparency = transparency;
  }
  
  /**
   * Simplified constructor for basic squared pattern without full Phong parameters.
   * Uses default Phong values.
   * @param color1 The first color for the square pattern.
   * @param color2 The second color for the square pattern.
   * @param scale The frequency of the squares.
   * @param objectInverseTransform The full inverse transformation matrix of the object.
   */
  public SquaredMaterial(Color color1, Color color2, double scale,
    Matrix4 objectInverseTransform) {
    this(color1, color2, scale,
      0.1, 0.7, 0.8, 50.0, Color.WHITE,
    0.0, 1.0, 0.0, objectInverseTransform);
  }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
    if (tm == null) tm = new Matrix4 ();
    this.objectInverseTransform = tm;
  }
  
  /**
   * Calculates the color contribution of a single light source at a given point on the surface,
   * taking into account surface properties and a square pattern using the Phong model.
   * The hit point is transformed into the material's local space before pattern calculation.
   *
   * @param worldPoint The point in 3D space (world coordinates) where the light hits.
   * @param worldNormal The normal vector at the point (world coordinates).
   * @param light The single light source affecting this point.
   * @param viewerPos The position of the viewer/camera.
   * @return The color contribution from this specific light for the point.
   */
  @Override
  public Color getColorAt(Point3 worldPoint, Vector3 worldNormal,
    Light light, Point3 viewerPos) {
    // Check if inverse transform is valid before proceeding
    if (objectInverseTransform == null) {
      System.err.println("Error: SquaredMaterial's inverse transform is null. Returning black.");
      return Color.BLACK;
    }
    
    // 1. Transform point to object's local space
    Point3 localPoint = objectInverseTransform.transformPoint(worldPoint);
    
    // 2. Transform the world normal to local space to determine the local face orientation
    // Normals transform with the inverse transpose of the model matrix.
    Vector3 localNormal = objectInverseTransform.inverseTransposeForNormal().transformVector(worldNormal).normalize();
    // Check if the transformed normal is valid
    if (localNormal == null) {
      System.err.println("Error: SquaredMaterial's normal transform matrix is null or invalid. Returning black.");
      return Color.BLACK;
    }
    
    Color patternColor;
    double u, v; // 2D texture coordinates
    
    // Determine the dominant axis of the *local normal* to decide 2D projection for pattern.
    // This ensures the square pattern aligns correctly with the object's local faces.
    double absNx = Math.abs(localNormal.x);
    double absNy = Math.abs(localNormal.y);
    double absNz = Math.abs(localNormal.z);
    
    // Project the 3D local point onto a 2D plane based on the dominant local normal axis.
    // Normalize coordinates from [-0.5, 0.5] to [0, 1] for a unit cube local space.
    // Then scale by 'this.scale' which represents squares per unit length.
    // Add a small epsilon to the coordinates before flooring to handle floating point inaccuracies at boundaries.
    if (absNx > absNy && absNx > absNz) { // Normal is mostly X-axis (local Y-Z plane)
      u = (localPoint.y + 0.5 + Ray.EPSILON) * this.scale;
      v = (localPoint.z + 0.5 + Ray.EPSILON) * this.scale;
      } else if (absNy > absNx && absNy > absNz) { // Normal is mostly Y-axis (local X-Z plane)
      u = (localPoint.x + 0.5 + Ray.EPSILON) * this.scale;
      v = (localPoint.z + 0.5 + Ray.EPSILON) * this.scale;
      } else { // Normal is mostly Z-axis (local X-Y plane) or equally dominant
      u = (localPoint.x + 0.5 + Ray.EPSILON) * this.scale;
      v = (localPoint.y + 0.5 + Ray.EPSILON) * this.scale;
    }
    
    // Use 2D square pattern logic for all surfaces.
    // The parity of the sum of the integer parts determines the color.
    int checkU = (int) Math.floor(u);
    int checkV = (int) Math.floor(v);
    
    if ((checkU + checkV) % 2 == 0) { // Standard 2D checkerboard pattern
      patternColor = this.color1;
      } else {
      patternColor = this.color2;
    }
    
    // 3. Phong lighting calculations
    Color lightColor = light.getColor();
    double attenuatedIntensity = 0.0; // Initialize for non-ambient lights
    
    // Ambient component
    Color ambientColor = ColorUtil.multiplyColors(patternColor, lightColor, ambient);
    
    if (light instanceof ElenaMuratAmbientLight) {
      return ambientColor;
    }
    
    // Light direction calculation
    Vector3 lightDir = getLightDirection(light, worldPoint);
    if (lightDir == null) return Color.BLACK;
    
    // Get attenuated intensity based on light type
    if (light instanceof MuratPointLight) {
      attenuatedIntensity = ((MuratPointLight) light).getAttenuatedIntensity(worldPoint);
      } else if (light instanceof ElenaDirectionalLight) {
      attenuatedIntensity = ((ElenaDirectionalLight) light).getIntensity();
      } else if (light instanceof PulsatingPointLight) {
      attenuatedIntensity = ((PulsatingPointLight) light).getAttenuatedIntensity(worldPoint);
      } else if (light instanceof SpotLight) {
      attenuatedIntensity = ((SpotLight) light).getAttenuatedIntensity(worldPoint);
      } else if (light instanceof BioluminescentLight) {
      attenuatedIntensity = ((BioluminescentLight) light).getAttenuatedIntensity(worldPoint);
      } else if (light instanceof BlackHoleLight) {
      attenuatedIntensity = ((BlackHoleLight) light).getAttenuatedIntensity(worldPoint);
      } else if (light instanceof FractalLight) {
      attenuatedIntensity = ((FractalLight) light).getAttenuatedIntensity(worldPoint);
      } else {
      // Bu else bloğuna düşmemesi gerekiyor, çünkü getLightDirection zaten kontrol ediyor.
      System.err.println("Warning: Unknown or unsupported light type for SquaredMaterial shading (intensity): " + light.getClass().getName());
      return Color.BLACK;
    }
    
    // Diffuse component
    double NdotL = Math.max(0, worldNormal.dot(lightDir));
    Color diffuseColor = ColorUtil.multiplyColors(patternColor, lightColor, diffuse * NdotL * attenuatedIntensity);
    
    // Specular component
    Vector3 viewDir = viewerPos.subtract(worldPoint).normalize();
    Vector3 reflectDir = lightDir.negate().reflect(worldNormal);
    double RdotV = Math.max(0, reflectDir.dot(viewDir));
    double specFactor = Math.pow(RdotV, shininess);
    Color specularColor = ColorUtil.multiplyColors(this.specularColor, lightColor, specular * specFactor * attenuatedIntensity);
    
    // Combine all components
    return ColorUtil.combineColors(ambientColor, diffuseColor, specularColor);
  }
  
  private Vector3 getLightDirection(Light light, Point3 point) {
    if (light == null) {
      return new Vector3(0, 1, 0).normalize(); // Varsayılan yön
    }
    
    if (light instanceof MuratPointLight) {
      return ((MuratPointLight)light).getPosition().subtract(point).normalize();
      } else if (light instanceof ElenaDirectionalLight) {
      return ((ElenaDirectionalLight)light).getDirection().negate().normalize();
      } else if (light instanceof PulsatingPointLight) {
      return ((PulsatingPointLight)light).getPosition().subtract(point).normalize();
      } else if (light instanceof SpotLight) {
      return ((SpotLight)light).getDirectionAt(point).normalize();
      } else if (light instanceof BioluminescentLight) {
      return ((BioluminescentLight)light).getDirectionAt(point);
      } else if (light instanceof BlackHoleLight) {
      return ((BlackHoleLight)light).getDirectionAt(point);
      } else if (light instanceof FractalLight) {
      return ((FractalLight)light).getDirectionAt(point);
      } else {
      //return new Vector3(0, 1, 0).normalize();
    }
    
    // Reflection fallback
    try {
      Method getDirMethod = light.getClass().getMethod("getDirectionAt", Point3.class);
      return (Vector3) getDirMethod.invoke(light, point);
    }
    catch (Exception e) {
      System.err.println("Unsupported light type: " + light.getClass().getName());
      return new Vector3(0, 1, 0).normalize(); // Güvenli varsayılan
    }
  }
  
  /**
   * Returns the reflectivity coefficient of the material.
   * @return The reflectivity value (0.0-1.0).
   */
  @Override
  public double getReflectivity() { return reflectivity; }
  
  /**
   * Returns the index of refraction (IOR) of the material.
   * @return The index of refraction.
   */
  @Override
  public double getIndexOfRefraction() { return ior; }
  
  /**
   * Returns the transparency coefficient of the material.
   * @return The transparency value (0.0-1.0).
   */
  @Override
  public double getTransparency() { return transparency; }
  
}
