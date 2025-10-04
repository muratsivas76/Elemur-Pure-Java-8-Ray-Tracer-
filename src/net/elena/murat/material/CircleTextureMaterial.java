package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.light.*;
import net.elena.murat.math.Point3;
import net.elena.murat.math.Vector3;
import net.elena.murat.math.Matrix4;
import net.elena.murat.math.Ray;

/**
 * A material that creates a repeating circular (dot) pattern on a surface.
 * It applies a Phong-like lighting model (Ambient + Diffuse + Specular) to the pattern.
 * This material now fully implements the extended Material interface.
 */
public class CircleTextureMaterial implements Material {
  
  private final Color solidColor;
  private final Color patternColor;
  private final double patternSize; // Controls the density/spacing of the circles. Smaller value means more circles.
  private final double circleRadius; // Fixed radius for the circle within a [0,1) cell
  private Matrix4 objectInverseTransform; // The inverse transformation matrix of the object
  
  // Phong lighting model parameters
  private final double ambientCoefficient;
  private final double diffuseCoefficient;
  private final double specularCoefficient;
  private final double shininess;
  private final double reflectivity;
  private final double ior; // Index of Refraction
  private final double transparency;
  
  private final Color specularColor = Color.WHITE; // Default specular color for highlights
  
  /**
   * Full constructor for CircleTextureMaterial.
   * @param solidColor The background color of the material.
   * @param patternColor The color of the dots.
   * @param patternSize Controls the density/spacing of the circles. Smaller value means more circles.
   * @param ambientCoefficient Ambient light contribution.
   * @param diffuseCoefficient Diffuse light contribution.
   * @param specularCoefficient Specular light contribution.
   * @param shininess Shininess for specular highlights.
   * @param reflectivity Material's reflectivity (0.0 to 1.0).
   * @param ior Index of refraction for transparent materials.
   * @param transparency Material's transparency (0.0 to 1.0).
   * @param objectInverseTransform The full inverse transformation matrix of the object this material is applied to.
   */
  public CircleTextureMaterial(Color solidColor, Color patternColor, double patternSize,
    double ambientCoefficient, double diffuseCoefficient,
    double specularCoefficient, double shininess,
    double reflectivity, double ior, double transparency,
    Matrix4 objectInverseTransform) { // Added objectInverseTransform
    this.solidColor = solidColor;
    this.patternColor = patternColor;
    this.patternSize = patternSize;
    this.circleRadius = 0.35; // Corrected: Fixed radius for the circle within a [0,1) cell.
    // This value can be adjusted to make circles larger/smaller.
    this.objectInverseTransform = objectInverseTransform; // Store the inverse transform
    
    this.ambientCoefficient = ambientCoefficient;
    this.diffuseCoefficient = diffuseCoefficient;
    this.specularCoefficient = specularCoefficient;
    this.shininess = shininess;
    this.reflectivity = reflectivity;
    this.ior = ior;
    this.transparency = transparency;
  }
  
  /**
   * Simplified constructor with default Phong-like coefficients.
   * @param solidColor The background color of the material.
   * @param patternColor The color of the dots.
   * @param patternSize Controls the density/spacing of the circles. Smaller value means more circles.
   * @param objectInverseTransform The full inverse transformation matrix of the object.
   */
  public CircleTextureMaterial(Color solidColor, Color patternColor, double patternSize, Matrix4 objectInverseTransform) { // Added objectInverseTransform
    this(solidColor, patternColor, patternSize,
      0.1,  // ambientCoefficient
      0.8,  // diffuseCoefficient
      0.1,  // specularCoefficient
      10.0, // shininess
      0.0,  // reflectivity
      1.0,  // indexOfRefraction
      0.0,  // transparency
      objectInverseTransform // Pass the inverse transform
    );
  }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
    if (tm == null) tm = new Matrix4 ();
    this.objectInverseTransform = tm;
  }
  
  /**
   * Calculates the base pattern color (solid or circle) at a given local point.
   * This method now handles the pattern repetition based on patternSize.
   *
   * @param localPoint The 3D point in the object's local coordinates.
   * @param localNormal The surface normal at that point in local coordinates.
   * @return The pattern color (solidColor or patternColor).
   */
  private Color getPatternColor(Point3 localPoint, Vector3 localNormal) { // Changed parameters to Point3 and Vector3
    // Use normal-based planar mapping to get UV coordinates
    double u, v;
    double absNx = Math.abs(localNormal.x);
    double absNy = Math.abs(localNormal.y);
    double absNz = Math.abs(localNormal.z);
    
    // Project the 3D local point onto a 2D plane based on the dominant local normal axis.
    // Then scale by 'patternSize' to control repetition.
    if (absNx > absNy && absNx > absNz) { // Normal is mostly X-axis (local Y-Z plane)
      u = localPoint.y * this.patternSize;
      v = localPoint.z * this.patternSize;
      } else if (absNy > absNx && absNy > absNz) { // Normal is mostly Y-axis (local X-Z plane)
      u = localPoint.x * this.patternSize;
      v = localPoint.z * this.patternSize;
      } else { // Normal is mostly Z-axis (local X-Y plane) or equally dominant
      u = localPoint.x * this.patternSize;
      v = localPoint.y * this.patternSize;
    }
    
    // Normalize UV values to [0, 1) range within a single pattern cell
    // We use floor to get the integer part of the scaled coordinate, and subtract it
    // to get the fractional part, which represents the position within the current cell.
    double normalizedU = u - Math.floor(u + Ray.EPSILON);
    double normalizedV = v - Math.floor(v + Ray.EPSILON);
    
    // Calculate distance from the center of the current UV cell (0.5, 0.5)
    double dx = normalizedU - 0.5;
    double dy = normalizedV - 0.5;
    double distance = Math.sqrt(dx * dx + dy * dy);
    
    // If the point is within the circleRadius, return patternColor, otherwise solidColor.
    if (distance <= circleRadius) {
      return patternColor;
      } else {
      return solidColor;
    }
  }
  
  /**
   * Returns the final shaded color at a given 3D point on the surface,
   * applying the circular pattern and a Phong-like lighting model.
   * Uses normal-based planar UV mapping for better generalization across shapes.
   *
   * @param worldPoint The 3D point on the surface in world coordinates.
   * @param worldNormal The surface normal at that point in world coordinates (should be normalized).
   * @param light The light source.
   * @param viewerPos The position of the viewer/camera.
   * @return The final shaded color.
   */
  @Override
  public Color getColorAt(Point3 worldPoint, Vector3 worldNormal, Light light, Point3 viewerPos) {
    // Check if inverse transform is valid before proceeding
    if (objectInverseTransform == null) {
      System.err.println("Error: CircleTextureMaterial's inverse transform is null. Returning black.");
      return Color.BLACK;
    }
    
    // 1. Transform the intersection point from world space into the object's local space
    Point3 localPoint = objectInverseTransform.transformPoint(worldPoint);
    
    // 2. Transform the world normal to local space to determine the local face orientation
    Vector3 localNormal = objectInverseTransform.inverseTransposeForNormal().transformVector(worldNormal).normalize();
    // Check if the transformed normal is valid
    if (localNormal == null) {
      System.err.println("Error: CircleTextureMaterial's normal transform matrix is null or invalid. Returning black.");
      return Color.BLACK;
    }
    
    // Get the base pattern color from the circular procedural pattern.
    // Pass localPoint and localNormal to getPatternColor for UV calculation
    Color patternBaseColor = getPatternColor(localPoint, localNormal);
    
    // --- Lighting Calculation (Ambient + Diffuse + Specular Phong Model) ---
    // This part is consistent with your other material classes.
    
    Color lightColor = light.getColor();
    
    // Ambient component
    // Ensure calculations are done with doubles and then clamped
    double rAmbient = patternBaseColor.getRed() / 255.0 * ambientCoefficient * lightColor.getRed() / 255.0;
    double gAmbient = patternBaseColor.getGreen() / 255.0 * ambientCoefficient * lightColor.getGreen() / 255.0;
    double bAmbient = patternBaseColor.getBlue() / 255.0 * ambientCoefficient * lightColor.getBlue() / 255.0;
    
    if (light instanceof ElenaMuratAmbientLight) {
      return new Color(
        (float)Math.min(1.0, rAmbient),
        (float)Math.min(1.0, gAmbient),
        (float)Math.min(1.0, bAmbient)
      );
    }
    
    Vector3 lightDirection;
    if (light instanceof MuratPointLight) {
      lightDirection = ((MuratPointLight) light).getPosition().subtract(worldPoint).normalize();
      } else if (light instanceof ElenaDirectionalLight) {
      lightDirection = ((ElenaDirectionalLight) light).getDirection().negate().normalize();
      } else if (light instanceof PulsatingPointLight) {
      lightDirection = ((PulsatingPointLight) light).getPosition().subtract(worldPoint).normalize();
      } else if (light instanceof BioluminescentLight) {
      lightDirection = ((BioluminescentLight) light).getDirectionAt(worldPoint).normalize();
      } else if (light instanceof BlackHoleLight) {
      lightDirection = ((BlackHoleLight) light).getDirectionAt(worldPoint).normalize();
      } else if (light instanceof FractalLight) {
      lightDirection = ((FractalLight) light).getDirectionAt(worldPoint).normalize();
      } else {
      System.err.println("Warning: Unknown or unsupported light type for CircleTextureMaterial shading: " + light.getClass().getName());
      return Color.BLACK;
    }
    
    // Diffuse component
    double NdotL = Math.max(0, worldNormal.dot(lightDirection));
    double rDiffuse = patternBaseColor.getRed() / 255.0 * diffuseCoefficient * lightColor.getRed() / 255.0 * NdotL;
    double gDiffuse = patternBaseColor.getGreen() / 255.0 * diffuseCoefficient * lightColor.getGreen() / 255.0 * NdotL;
    double bDiffuse = patternBaseColor.getBlue() / 255.0 * diffuseCoefficient * lightColor.getBlue() / 255.0 * NdotL;
    
    // Specular component
    Vector3 viewDir = viewerPos.subtract(worldPoint).normalize();
    Vector3 reflectionVector = lightDirection.negate().reflect(worldNormal); // Use negate() before reflect() for correct reflection vector
    double RdotV = Math.max(0, reflectionVector.dot(viewDir));
    double specFactor = Math.pow(RdotV, shininess);
    
    double rSpecular = specularColor.getRed() / 255.0 * specularCoefficient * lightColor.getRed() / 255.0 * specFactor;
    double gSpecular = specularColor.getGreen() / 255.0 * specularCoefficient * lightColor.getGreen() / 255.0 * specFactor;
    double bSpecular = specularColor.getBlue() / 255.0 * specularCoefficient * lightColor.getBlue() / 255.0 * specFactor;
    
    // Sum up all components for this light source
    double finalR = rAmbient + rDiffuse + rSpecular;
    double finalG = gAmbient + gDiffuse + gSpecular;
    double finalB = bAmbient + bDiffuse + bSpecular;
    
    return new Color(
      (float)clamp01(finalR),
      (float)clamp01(finalG),
      (float)clamp01(finalB)
    );
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
  
  /**
   * Helper method to clamp a double value between 0.0 and 1.0.
   * @param val The value to clamp.
   * @return A value between 0.0 and 1.0.
   */
  private double clamp01(double val) {
    return Math.min(1.0, Math.max(0.0, val));
  }
  
  /**
   * Helper method to get the normalized light direction vector from a light source to a point.
   * @param light The light source.
   * @param worldPoint The point in world coordinates.
   * @return Normalized light direction vector, or null if light type is unsupported.
   */
  private Vector3 getLightDirection(Light light, Point3 worldPoint) {
    if (light instanceof MuratPointLight) {
      return ((MuratPointLight)light).getPosition().subtract(worldPoint).normalize();
      } else if (light instanceof ElenaDirectionalLight) {
      return ((ElenaDirectionalLight)light).getDirection().negate().normalize();
      } else if (light instanceof PulsatingPointLight) {
      return ((PulsatingPointLight)light).getPosition().subtract(worldPoint).normalize();
      } else if (light instanceof SpotLight) {
      return ((SpotLight)light).getDirectionAt(worldPoint).normalize();
      } else if (light instanceof BioluminescentLight) {
      return ((BioluminescentLight)light).getDirectionAt(worldPoint).normalize();
      } else if (light instanceof BlackHoleLight) {
      return ((BlackHoleLight)light).getDirectionAt(worldPoint).normalize();
      } else if (light instanceof FractalLight) {
      return ((FractalLight)light).getDirectionAt(worldPoint).normalize();
      } else {
      System.err.println("Warning: Unknown light type in PlaneFaceElenaTextureMaterial");
      return new Vector3(0, 1, 0); // Varsayılan yön
    }
  }
  
}
