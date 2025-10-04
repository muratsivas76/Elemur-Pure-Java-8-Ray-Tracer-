package net.elena.murat.material;

import java.awt.Color;

//custom
import net.elena.murat.math.Point3;
import net.elena.murat.math.Vector3;
import net.elena.murat.math.Matrix4;
import net.elena.murat.math.Ray;
import net.elena.murat.light.*;

/**
 * A material that creates a repeating triangular pattern on a surface.
 * It applies a Phong-like lighting model (Ambient + Diffuse + Specular) to the pattern.
 * This material now fully implements the extended Material interface.
 */
public class TriangleMaterial implements Material {
  
  private final Color color1; // Color for the first type of triangle/area
  private final Color color2; // Color for the second type of triangle/area
  private final double triangleSize; // Controls the size/frequency of the triangles
  private Matrix4 objectInverseTransform; // The inverse transformation matrix of the object
  
  // Lighting coefficients (Phong model)
  private final double ambientCoefficient;
  private final double diffuseCoefficient;
  private final double specularCoefficient;
  private final double shininess;
  private final double reflectivity;
  private final double ior; // Index of Refraction
  private final double transparency;
  
  // Default specular color (can be made a constructor parameter if needed)
  private final Color specularColor = Color.WHITE;
  
  /**
   * Full constructor for TriangleMaterial.
   * @param color1 First color for the triangle pattern.
   * @param color2 Second color for the triangle pattern.
   * @param triangleSize Controls the scale of the triangular pattern. Smaller values mean more, smaller triangles.
   * @param ambientCoefficient Ambient light contribution.
   * @param diffuseCoefficient Diffuse light contribution.
   * @param specularCoefficient Specular light contribution.
   * @param shininess Shininess for specular highlights.
   * @param reflectivity Material's reflectivity (0.0 to 1.0).
   * @param ior Index of refraction for transparent materials.
   * @param transparency Material's transparency (0.0 to 1.0).
   * @param objectInverseTransform The full inverse transformation matrix of the object this material is applied to.
   */
  public TriangleMaterial(Color color1, Color color2, double triangleSize,
    double ambientCoefficient, double diffuseCoefficient,
    double specularCoefficient, double shininess,
    double reflectivity, double ior, double transparency,
    Matrix4 objectInverseTransform) { // Added objectInverseTransform
    this.color1 = color1;
    this.color2 = color2;
    this.triangleSize = triangleSize;
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
   * @param color1 First color for the triangle pattern.
   * @param color2 Second color for the triangle pattern.
   * @param triangleSize Controls the scale of the triangular pattern.
   * @param objectInverseTransform The full inverse transformation matrix of the object.
   */
  public TriangleMaterial(Color color1, Color color2, double triangleSize, Matrix4 objectInverseTransform) { // Added objectInverseTransform
    // Default Phong parameters, adjusted to be similar to a common "diffuse" material.
    this(color1, color2, triangleSize,
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
   * Calculates the base pattern color at a given UV coordinate pair.
   * This method separates the pattern generation logic from lighting calculations.
   *
   * @param u U texture coordinate (can be any value, will be normalized internally).
   * @param v V texture coordinate (can be any value, will be normalized internally).
   * @return The pattern color (either color1 or color2).
   */
  private Color getPatternColor(double u, double v) {
    // Apply triangleSize as an inverse scale: smaller triangleSize = more repetitions (smaller triangles)
    // This scales the UV coordinates before the pattern logic.
    u = u / triangleSize;
    v = v / triangleSize;
    
    // Ensure positive values for modulo operations and make the pattern repeat seamlessly.
    // This handles negative u/v values correctly for wrapping.
    // Adding EPSILON to prevent floating point issues near integer boundaries.
    double u_normalized = (u % 1.0 + 1.0) % 1.0;
    double v_normalized = (v % 1.0 + 1.0) % 1.0;
    
    // Determine which repeating "square" (cell) we are in.
    // These `checkX` and `checkY` are used to alternate the pattern's orientation/colors.
    int checkX = (int) Math.floor(u); // Integer part of u
    int checkY = (int) Math.floor(v); // Integer part of v
    
    // --- Triangular Pattern Logic ---
    // This divides each conceptual "square" cell (from UV mapping) into two triangles
    // based on a diagonal. The colors are swapped in alternating cells to create
    // a continuous triangular grid appearance.
    
    Color patternColor;
    if ((checkX + checkY) % 2 == 0) { // For "even" cells (e.g., (0,0), (0,2), (1,1) etc.)
      if (u_normalized + v_normalized < 1.0) { // Top-left triangle (above the diagonal from bottom-left to top-right)
        patternColor = color1;
        } else { // Bottom-right triangle (below the diagonal)
        patternColor = color2;
      }
      } else { // For "odd" cells (e.g., (0,1), (1,0), (1,2) etc.)
      if (u_normalized + v_normalized < 1.0) { // Top-left triangle (colors swapped for contrast)
        patternColor = color2;
        } else { // Bottom-right triangle (colors swapped)
        patternColor = color1;
      }
    }
    return patternColor;
  }
  
  /**
   * Returns the final shaded color at a given 3D point on the surface,
   * applying the triangular pattern and a Phong-like lighting model.
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
      System.err.println("Error: TriangleMaterial's inverse transform is null. Returning black.");
      return Color.BLACK;
    }
    
    // 1. Transform the intersection point from world space into the object's local space
    // This is crucial for applying the pattern correctly regardless of object's position, rotation, or scale.
    Point3 localPoint = objectInverseTransform.transformPoint(worldPoint);
    
    // 2. Transform the world normal to local space to determine the local face orientation
    Vector3 localNormal = objectInverseTransform.inverseTransposeForNormal().transformVector(worldNormal).normalize();
    // Check if the transformed normal is valid
    if (localNormal == null) {
      System.err.println("Error: TriangleMaterial's normal transform matrix is null or invalid. Returning black.");
      return Color.BLACK;
    }
    
    // --- UV Coordinate Calculation (Normal-based Planar Mapping) ---
    // This approach selects a projection plane based on the dominant axis of the surface normal.
    // It's a common technique for procedural textures that need to wrap around arbitrary shapes.
    // The localPoint is used for UV calculation to ensure pattern is aligned with object's local axes.
    
    double u, v;
    
    double absNx = Math.abs(localNormal.x);
    double absNy = Math.abs(localNormal.y);
    double absNz = Math.abs(localNormal.z);
    
    // Project the 3D local point onto a 2D plane based on the dominant local normal axis.
    // The scaling factor will control the density/size of the pattern.
    // A value of 1.0 means 1 unit in world space corresponds to 1 unit in UV space (before triangleSize).
    // Add a small epsilon to the coordinates before flooring to handle floating point inaccuracies at boundaries.
    if (absNx > absNy && absNx > absNz) { // Normal is mostly X-axis (local Y-Z plane)
      u = localPoint.y + Ray.EPSILON;
      v = localPoint.z + Ray.EPSILON;
      } else if (absNy > absNx && absNy > absNz) { // Normal is mostly Y-axis (local X-Z plane)
      u = localPoint.x + Ray.EPSILON;
      v = localPoint.z + Ray.EPSILON;
      } else { // Normal is mostly Z-axis (local X-Y plane) or equally dominant
      u = localPoint.x + Ray.EPSILON;
      v = localPoint.y + Ray.EPSILON;
    }
    
    // Get the base color from the procedural triangular pattern.
    Color patternBaseColor = getPatternColor(u, v);
    
    // --- Lighting Calculation (Ambient + Diffuse + Specular Phong Model) ---
    // This part is consistent with your other material classes.
    
    Color lightColor = light.getColor();
    double attenuatedIntensity = 0.0; // Initialize attenuatedIntensity here
    
    // Ambient component
    int rAmbient = (int) (patternBaseColor.getRed() * ambientCoefficient * lightColor.getRed() / 255.0);
    int gAmbient = (int) (patternBaseColor.getGreen() * ambientCoefficient * lightColor.getGreen() / 255.0);
    int bAmbient = (int) (patternBaseColor.getBlue() * ambientCoefficient * lightColor.getBlue() / 255.0);
    
    if (light instanceof ElenaMuratAmbientLight) {
      return new Color(
        Math.min(255, rAmbient),
        Math.min(255, gAmbient),
        Math.min(255, bAmbient)
      );
    }
    
    Vector3 lightDirection;
    if (light instanceof MuratPointLight) {
      MuratPointLight pLight = (MuratPointLight) light;
      lightDirection = pLight.getPosition().subtract(worldPoint).normalize();
      attenuatedIntensity = pLight.getAttenuatedIntensity(worldPoint);
      } else if (light instanceof ElenaDirectionalLight) {
      ElenaDirectionalLight dLight = (ElenaDirectionalLight) light;
      lightDirection = dLight.getDirection().negate().normalize();
      attenuatedIntensity = dLight.getIntensity();
      } else if (light instanceof PulsatingPointLight) {
      PulsatingPointLight ppLight = (PulsatingPointLight) light;
      lightDirection = ppLight.getPosition().subtract(worldPoint).normalize();
      attenuatedIntensity = ppLight.getAttenuatedIntensity(worldPoint);
      } else if (light instanceof SpotLight) {
      SpotLight sLight = (SpotLight) light;
      lightDirection = sLight.getDirectionAt(worldPoint);
      attenuatedIntensity = sLight.getAttenuatedIntensity(worldPoint);
      } else if (light instanceof BioluminescentLight) {
      BioluminescentLight bLight = (BioluminescentLight) light;
      lightDirection = bLight.getDirectionAt(worldPoint);
      attenuatedIntensity = bLight.getAttenuatedIntensity(worldPoint);
      } else if (light instanceof BlackHoleLight) {
      BlackHoleLight bhLight = (BlackHoleLight) light;
      lightDirection = bhLight.getDirectionAt(worldPoint);
      attenuatedIntensity = bhLight.getAttenuatedIntensity(worldPoint);
      } else if (light instanceof FractalLight) {
      FractalLight fLight = (FractalLight) light;
      lightDirection = fLight.getDirectionAt(worldPoint);
      attenuatedIntensity = fLight.getAttenuatedIntensity(worldPoint);
      } else {
      System.err.println("Warning: Unknown or unsupported light type for TriangleMaterial shading: " + light.getClass().getName());
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
    // Ambient is handled separately for ambient lights. For other lights, combine diffuse and specular.
    int finalR = Math.min(255, rDiffuse + rSpecular);
    int finalG = Math.min(255, gDiffuse + gSpecular);
    int finalB = Math.min(255, bDiffuse + bSpecular);
    
    return new Color(finalR, finalG, finalB);
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
