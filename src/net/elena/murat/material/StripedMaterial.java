package net.elena.murat.material;

import java.awt.Color;

//custom imports
import net.elena.murat.light.*;
import net.elena.murat.math.Matrix3;
import net.elena.murat.math.Matrix4;
import net.elena.murat.math.Point3;
import net.elena.murat.math.Vector3;
import net.elena.murat.math.Ray;

/**
 * A material that creates a repeating striped pattern on a surface.
 * It applies a Phong-like lighting model (Ambient + Diffuse + Specular) to the pattern.
 * This material now fully implements the extended Material interface.
 */
public class StripedMaterial implements Material {
  
  private final Color color1;     // Color for the first stripe
  private final Color color2;     // Color for the second stripe
  private final double stripeSize; // Controls the width/frequency of the stripes. Smaller value = thinner/more stripes.
  private final StripeDirection direction; // The direction of the stripes (e.g., Horizontal, Vertical, Diagonal)
  private Matrix4 objectInverseTransform; // The inverse transformation matrix of the object
  
  // Lighting coefficients (similar to Phong, as in other texture materials)
  private final double ambientCoefficient;
  private final double diffuseCoefficient;
  private final double specularCoefficient;
  private final double shininess;
  private final double reflectivity;
  private final double ior; // Index of Refraction
  private final double transparency;
  
  private final Color specularColor = Color.WHITE; // Default specular color for highlights
  
  /**
   * Full constructor for StripedMaterial.
   * @param color1 First color of the stripes.
   * @param color2 Second color of the stripes.
   * @param stripeSize Controls the width/frequency of the stripes. Smaller value means more, thinner stripes.
   * @param direction The orientation of the stripes (Horizontal, Vertical, Diagonal).
   * @param ambientCoefficient Ambient light contribution.
   * @param diffuseCoefficient Diffuse light contribution.
   * @param specularCoefficient Specular light contribution.
   * @param shininess Shininess for specular highlights.
   * @param reflectivity Material's reflectivity (0.0 to 1.0).
   * @param ior Index of refraction for transparent materials.
   * @param transparency Material's transparency (0.0 to 1.0).
   * @param objectInverseTransform The full inverse transformation matrix of the object this material is applied to.
   */
  public StripedMaterial(Color color1, Color color2, double stripeSize, StripeDirection direction,
    double ambientCoefficient, double diffuseCoefficient,
    double specularCoefficient, double shininess,
    double reflectivity, double ior, double transparency,
    Matrix4 objectInverseTransform) { // Added objectInverseTransform
    this.color1 = color1;
    this.color2 = color2;
    this.stripeSize = stripeSize;
    this.direction = direction;
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
   * Default direction is VERTICAL.
   * @param color1 First color of the stripes.
   * @param color2 Second color of the stripes.
   * @param stripeSize Controls the width/frequency of the stripes.
   * @param objectInverseTransform The full inverse transformation matrix of the object.
   */
  public StripedMaterial(Color color1, Color color2, double stripeSize, Matrix4 objectInverseTransform) { // Added objectInverseTransform
    this(color1, color2, stripeSize, StripeDirection.VERTICAL, objectInverseTransform); // Default to vertical stripes
  }
  
  /**
   * Simplified constructor with default Phong-like coefficients and specified direction.
   * @param color1 First color of the stripes.
   * @param color2 Second color of the stripes.
   * @param stripeSize Controls the width/frequency of the stripes.
   * @param direction The orientation of the stripes.
   * @param objectInverseTransform The full inverse transformation matrix of the object.
   */
  public StripedMaterial(Color color1, Color color2, double stripeSize, StripeDirection direction, Matrix4 objectInverseTransform) { // Added objectInverseTransform
    // Default Phong parameters, consistent with other texture materials
    this(color1, color2, stripeSize, direction,
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
   * Calculates the base pattern color (stripe color) at a given UV coordinate pair.
   * The input u and v coordinates are already scaled by stripeSize.
   *
   * @param u U texture coordinate.
   * @param v V texture coordinate.
   * @return The pattern color (color1 or color2).
   */
  private Color getPatternColor(double u, double v) {
    double value; // The coordinate value to check for stripes
    
    switch (direction) {
      case HORIZONTAL:
        value = v; // Stripes vary along the V-axis (latitude)
      break;
      case VERTICAL:
        value = u; // Stripes vary along the U-axis (longitude)
      break;
      case DIAGONAL:
        // Diagonal stripes can be based on u+v or u-v
      value = (u + v) / 2.0; // Average for diagonal, keeps scale consistent
      break;
      default:
        value = u; // Fallback to vertical
      break;
    }
    
    // Ensure value is within [0, 1) for consistent repetition check
    // Use Math.floor to get the integer part, then check parity
    // Add a small epsilon to handle floating point inaccuracies at boundaries.
    if (Math.floor(value + Ray.EPSILON) % 2 == 0) {
      return color1;
      } else {
      return color2;
    }
  }
  
  /**
   * Returns the final shaded color at a given 3D point on the surface,
   * applying the striped pattern and a Phong-like lighting model.
   * Uses spherical UV mapping for better generalization across shapes.
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
      System.err.println("Error: StripedMaterial's inverse transform is null. Returning black.");
      return Color.BLACK;
    }
    
    // 1. Transform the intersection point from world space into the object's local space
    Point3 localPoint = objectInverseTransform.transformPoint(worldPoint);
    
    // --- UV Coordinate Calculation (Spherical Mapping for general curved surfaces) ---
    // Convert localPoint to a Vector3 from the origin (0,0,0) to normalize it.
    // This is crucial for spherical mapping which treats the point as a direction from the center.
    Vector3 P_as_vector = new Vector3(localPoint.x, localPoint.y, localPoint.z);
    Vector3 P_normalized = P_as_vector.normalize();
    
    // Spherical mapping (standard latitude/longitude projection)
    // U: longitude, V: latitude
    // U ranges from 0 to 1, V ranges from 0 to 1
    double u = 0.5 + Math.atan2(P_normalized.z, P_normalized.x) / (2 * Math.PI);
    double v = 0.5 - Math.asin(P_normalized.y) / Math.PI;
    
    // Apply stripeSize as a frequency multiplier to the UV coordinates
    u *= this.stripeSize;
    v *= this.stripeSize;
    
    // Get the base pattern color from the striped procedural pattern.
    Color patternBaseColor = getPatternColor(u, v); // Pass scaled u,v
    
    // --- Lighting Calculation (Ambient + Diffuse + Specular Phong Model) ---
    // This part is consistent with your other material classes.
    
    Color lightColor = light.getColor();
    double attenuatedIntensity = 0.0; // Initialize attenuated intensity
    
    // Ambient Light calculation
    if (light instanceof ElenaMuratAmbientLight) {
      double ambientIntensity = light.getIntensity();
      // Ambient component is scaled by the pattern color
      int rAmbient = (int) (patternBaseColor.getRed() * ambientCoefficient * ambientIntensity * (lightColor.getRed() / 255.0));
      int gAmbient = (int) (patternBaseColor.getGreen() * ambientCoefficient * ambientIntensity * (lightColor.getGreen() / 255.0));
      int bAmbient = (int) (patternBaseColor.getBlue() * ambientCoefficient * ambientIntensity * (lightColor.getBlue() / 255.0));
      return new Color(
        Math.min(255, rAmbient),
        Math.min(255, gAmbient),
        Math.min(255, bAmbient)
      );
      } else { // Directional, Point, Pulsating, and Spot lights (non-ambient)
      Vector3 lightDirection = getLightDirection(light, worldPoint); // Use worldPoint for light direction
      if (lightDirection == null) { // Handle unsupported light types
        return Color.BLACK;
      }
      
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
        System.err.println("Warning: Unknown or unsupported light type for StripedMaterial shading (intensity): " + light.getClass().getName());
        return Color.BLACK;
      }
      
      // Diffuse component
      double NdotL = Math.max(0, worldNormal.dot(lightDirection));
      int rDiffuse = (int) (patternBaseColor.getRed() * diffuseCoefficient * lightColor.getRed() / 255.0 * attenuatedIntensity * NdotL);
      int gDiffuse = (int) (patternBaseColor.getGreen() * diffuseCoefficient * lightColor.getGreen() / 255.0 * attenuatedIntensity * NdotL);
      int bDiffuse = (int) (patternBaseColor.getBlue() * diffuseCoefficient * lightColor.getBlue() / 255.0 * attenuatedIntensity * NdotL);
      
      // Specular component
      Vector3 viewDir = viewerPos.subtract(worldPoint).normalize();
      Vector3 reflectionVector = lightDirection.negate().reflect(worldNormal); // Use negate() before reflect() for correct reflection vector
      double RdotV = Math.max(0, reflectionVector.dot(viewDir));
      double specFactor = Math.pow(RdotV, shininess);
      
      int rSpecular = (int) (specularColor.getRed() * specularCoefficient * lightColor.getRed() / 255.0 * attenuatedIntensity * specFactor);
      int gSpecular = (int) (specularColor.getGreen() * specularCoefficient * lightColor.getGreen() / 255.0 * attenuatedIntensity * specFactor);
      int bSpecular = (int) (specularColor.getBlue() * specularCoefficient * lightColor.getBlue() / 255.0 * attenuatedIntensity * specFactor);
      
      // Sum up all components for this light source
      int finalR = Math.min(255, rDiffuse + rSpecular); // Ambient is handled separately
      int finalG = Math.min(255, gDiffuse + gSpecular);
      int finalB = Math.min(255, bDiffuse + bSpecular);
      
      return new Color(finalR, finalG, finalB);
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
  
  // No need for getShininess() as it's not part of the Material interface and used internally.
  // public double getShininess() { return shininess; }
  
  /**
   * Helper method to clamp a double value between 0.0 and 1.0.
   * @param val The value to clamp.
   * @return The clamped value.
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
