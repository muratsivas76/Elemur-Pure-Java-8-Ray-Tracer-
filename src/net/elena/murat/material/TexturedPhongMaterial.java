package net.elena.murat.material;

import java.awt.Color;
import java.awt.image.BufferedImage;

import net.elena.murat.light.*;
import net.elena.murat.math.Point3;
import net.elena.murat.math.Vector3;
import net.elena.murat.math.Matrix4;
import net.elena.murat.math.Ray;

/**
 * Represents a Phong material with added texture mapping capabilities.
 * It extends the standard Phong illumination model by applying a BufferedImage as a texture.
 * This version allows for texture offsetting and scaling (tiling) and uses
 * object's inverse transform for correct UV mapping in local space.
 * This material now fully implements the extended Material interface.
 */
public class TexturedPhongMaterial implements Material {
  // Phong material properties
  private final Color baseDiffuseColor; // This color acts as a base if no texture, or for tinting
  private final Color specularColor;
  private final double shininess;
  private final double ambientCoefficient;
  private final double diffuseCoefficient;
  private final double specularCoefficient;
  private final double reflectivity;
  private final double ior; // Index of Refraction
  private final double transparency;
  
  // Texture property - now holding BufferedImage directly
  private final BufferedImage texture; // The image to be mapped onto the surface
  private final double uOffset;          // Horizontal texture offset
  private final double vOffset;          // Vertical texture offset
  private final double uScale;           // Horizontal texture tiling/scaling factor
  private final double vScale;           // Vertical texture tiling/scaling factor
  
  // The inverse transformation matrix of the object this material is applied to
  private Matrix4 objectInverseTransform;
  
  /**
   * Full constructor for TexturedPhongMaterial.
   * @param baseDiffuseColor The base diffuse color (used as a fallback or tint for texture).
   * @param specularColor The specular color.
   * @param shininess The shininess exponent for specular highlights.
   * @param ambientCoefficient The ambient light coefficient.
   * @param diffuseCoefficient The diffuse light coefficient.
   * @param specularCoefficient The specular light coefficient.
   * @param reflectivity The reflectivity of the material (0.0 to 1.0).
   * @param ior The index of refraction (for transparent materials).
   * @param transparency The transparency of the material (0.0 to 1.0).
   * @param texture The BufferedImage to be used as a texture. Can be null.
   * @param uOffset Horizontal offset for the texture (e.g., 0.5 to shift by half width).
   * @param vOffset Vertical offset for the texture (e.g., 0.5 to shift by half height).
   * @param uScale Horizontal tiling/scaling factor (e.g., 2.0 to repeat texture twice).
   * @param vScale Vertical tiling/scaling factor (e.g., 2.0 to repeat texture twice).
   * @param objectInverseTransform The full inverse transformation matrix of the object this material is applied to.
   */
  public TexturedPhongMaterial(Color baseDiffuseColor, Color specularColor, double shininess,
    double ambientCoefficient, double diffuseCoefficient, double specularCoefficient,
    double reflectivity, double ior, double transparency,
    BufferedImage texture,
    double uOffset, double vOffset, double uScale, double vScale,
    Matrix4 objectInverseTransform) { // Added objectInverseTransform
    this.baseDiffuseColor = baseDiffuseColor;
    this.specularColor = specularColor;
    this.shininess = shininess;
    this.ambientCoefficient = ambientCoefficient;
    this.diffuseCoefficient = diffuseCoefficient;
    this.specularCoefficient = specularCoefficient;
    this.reflectivity = clamp01(reflectivity);
    this.ior = Math.max(1.0, ior); // IOR should be at least 1.0 (for vacuum/air)
    this.transparency = clamp01(transparency);
    this.texture = texture;
    this.uOffset = uOffset;
    this.vOffset = vOffset;
    this.uScale = uScale;
    this.vScale = vScale;
    this.objectInverseTransform = objectInverseTransform; // Store the inverse transform
  }
  
  /**
   * Constructor for a textured material with default Phong properties and no texture transformations.
   * @param baseDiffuseColor The base diffuse color.
   * @param texture The BufferedImage to be used as a texture.
   * @param objectInverseTransform The full inverse transformation matrix of the object this material is applied to.
   */
  public TexturedPhongMaterial(Color baseDiffuseColor, BufferedImage texture, Matrix4 objectInverseTransform) { // Added objectInverseTransform
    // Call the full constructor with default values
    this(baseDiffuseColor, Color.WHITE, 32.0, 0.1, 0.7, 0.7, 0.0, 1.0, 0.0, texture, 0.0, 0.0, 1.0, 1.0, objectInverseTransform);
  }
  
  /**
   * Constructor for a textured material with full Phong properties but no texture transformations.
   * @param baseDiffuseColor The base diffuse color.
   * @param specularColor The specular color.
   * @param shininess The shininess exponent for specular highlights.
   * @param ambientCoefficient The ambient light coefficient.
   * @param diffuseCoefficient The diffuse light coefficient.
   * @param specularCoefficient The specular light coefficient.
   * @param reflectivity The reflectivity of the material (0.0 to 1.0).
   * @param ior The index of refraction (for transparent materials).
   * @param transparency The transparency of the material (0.0 to 1.0).
   * @param texture The BufferedImage to be used as a texture.
   * @param objectInverseTransform The full inverse transformation matrix of the object.
   */
  public TexturedPhongMaterial(Color baseDiffuseColor, Color specularColor, double shininess,
    double ambientCoefficient, double diffuseCoefficient, double specularCoefficient,
    double reflectivity, double ior, double transparency,
    BufferedImage texture, Matrix4 objectInverseTransform) { // Added objectInverseTransform
    // Call the full constructor with default transformation values
    this(baseDiffuseColor, specularColor, shininess, ambientCoefficient, diffuseCoefficient,
    specularCoefficient, reflectivity, ior, transparency, texture, 0.0, 0.0, 1.0, 1.0, objectInverseTransform);
  }
  
  /**
   * Constructor for a Phong material without texture (similar to original PhongMaterial).
   * Defaults to no texture and default texture transformations.
   * @param diffuseColor The diffuse color of the material.
   * @param objectInverseTransform The full inverse transformation matrix of the object this material is applied to.
   */
  public TexturedPhongMaterial(Color diffuseColor, Matrix4 objectInverseTransform) { // Added objectInverseTransform
    // Call the full constructor with default values (texture as null)
    this(diffuseColor, Color.WHITE, 32.0, 0.1, 0.7, 0.7, 0.0, 1.0, 0.0, null, 0.0, 0.0, 1.0, 1.0, objectInverseTransform);
  }
  
  // --- Getters for material properties ---
  public Color getBaseDiffuseColor() { return baseDiffuseColor; }
  public Color getSpecularColor() { return specularColor; }
  public double getShininess() { return shininess; } // Not part of Material interface, but useful internally
  public double getAmbientCoefficient() { return ambientCoefficient; } // Not part of Material interface
  public double getDiffuseCoefficient() { return diffuseCoefficient; } // Not part of Material interface
  public double getSpecularCoefficient() { return specularCoefficient; } // Not part of Material interface
  
  @Override
  public double getReflectivity() { return reflectivity; }
  
  @Override
  public double getIndexOfRefraction() { return ior; }
  
  @Override
  public double getTransparency() { return transparency; }
  
  public BufferedImage getTexture() { return texture; } // Returns BufferedImage
  public double getUOffset() { return uOffset; }
  public double getVOffset() { return vOffset; }
  public double getUScale() { return uScale; }
  public double getVScale() { return vScale; }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
    if (tm == null) tm = new Matrix4 ();
    this.objectInverseTransform = tm;
  }
  
  /**
   * Calculates the final color at a given point on the surface, considering light sources
   * and Phong illumination model with texture mapping.
   * This is the primary method used by the RayTracer's shading function.
   * @param worldPoint The intersection point on the surface in world coordinates.
   * @param worldNormal The normal vector at the intersection point in world coordinates (must be normalized).
   * @param light The light source currently being evaluated.
   * @param viewerPos The position of the viewer/camera.
   * @return The calculated Color at the point.
   */
  @Override
  public Color getColorAt(Point3 worldPoint, Vector3 worldNormal, Light light, Point3 viewerPos) {
    // Check if inverse transform is valid before proceeding
    if (objectInverseTransform == null) {
      System.err.println("Error: TexturedPhongMaterial's inverse transform is null. Returning black.");
      return Color.BLACK;
    }
    
    // 1. Transform world point and normal to object's local space for UV mapping
    Point3 localPoint = objectInverseTransform.transformPoint(worldPoint);
    Vector3 localNormal = objectInverseTransform.inverseTransposeForNormal().transformVector(worldNormal).normalize();
    
    // Check if the transformed normal is valid
    if (localNormal == null) {
      System.err.println("Error: TexturedPhongMaterial's local normal is null. Returning base diffuse color.");
      return baseDiffuseColor; // Fallback to base color if normal transformation fails
    }
    
    // Get the color from the texture at the intersection point using planar UV mapping.
    // If no texture is present or mapping fails, this will return the baseDiffuseColor.
    Color textureColor = getTextureColor(localPoint, localNormal);
    double texR = textureColor.getRed() / 255.0;
    double texG = textureColor.getGreen() / 255.0;
    double texB = textureColor.getBlue() / 255.0;
    
    double rCombined = 0.0;
    double gCombined = 0.0;
    double bCombined = 0.0;
    
    double specR = specularColor.getRed() / 255.0;
    double specG = specularColor.getGreen() / 255.0;
    double specB = specularColor.getBlue() / 255.0;
    
    // Ambient Light calculation
    if (light instanceof ElenaMuratAmbientLight) {
      double ambientIntensity = light.getIntensity();
      // Ambient component is scaled by the texture color
      rCombined = texR * ambientCoefficient * ambientIntensity * (light.getColor().getRed() / 255.0);
      gCombined = texG * ambientCoefficient * ambientIntensity * (light.getColor().getGreen() / 255.0);
      bCombined = texB * ambientCoefficient * ambientIntensity * (light.getColor().getBlue() / 255.0);
      } else { // Directional, Point, Pulsating, and Spot lights (non-ambient)
      Vector3 lightDir = getLightDirection(light, worldPoint); // Use worldPoint for light direction
      if (lightDir == null) { // Handle unsupported light types
        return Color.BLACK;
      }
      Color lightColorFromSource = light.getColor(); // Get color from the light source
      double attenuatedIntensity;
      
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
        // Ancak bir güvenlik önlemi olarak burada da bir uyarı basabiliriz.
        System.err.println("Warning: Unknown or unsupported light type for TexturedPhongMaterial shading (intensity): " + light.getClass().getName());
        return Color.BLACK;
      }
      
      // Diffuse component: scaled by the texture color
      double diffuseFactor = Math.max(0, worldNormal.dot(lightDir)); // Use worldNormal for lighting
      rCombined = texR * diffuseCoefficient * diffuseFactor * (lightColorFromSource.getRed() / 255.0) * attenuatedIntensity;
      gCombined = texG * diffuseCoefficient * diffuseFactor * (lightColorFromSource.getGreen() / 255.0) * attenuatedIntensity;
      bCombined = texB * diffuseCoefficient * diffuseFactor * (lightColorFromSource.getBlue() / 255.0) * attenuatedIntensity;
      
      // Specular component: not usually scaled by texture, uses material's specularColor
      Vector3 V = viewerPos.subtract(worldPoint).normalize();
      Vector3 R = lightDir.negate().reflect(worldNormal); // Reflect the light direction vector (negated for incoming light)
      
      double specularFactor = Math.max(0, R.dot(V));
      specularFactor = Math.pow(specularFactor, shininess);
      
      rCombined += specR * specularCoefficient * specularFactor * (lightColorFromSource.getRed() / 255.0) * attenuatedIntensity;
      gCombined += specG * specularCoefficient * specularFactor * (lightColorFromSource.getGreen() / 255.0) * attenuatedIntensity;
      bCombined += specB * specularCoefficient * specularFactor * (lightColorFromSource.getBlue() / 255.0) * attenuatedIntensity;
    }
    
    // Clamp final color values to [0, 1] range and convert to Color object
    return new Color((float)clamp01(rCombined), (float)clamp01(gCombined), (float)clamp01(bCombined));
  }
  
  /**
   * Helper method to clamp a double value between 0.0 and 1.0.
   * @param val The value to clamp.
   * @return The clamped value.
   */
  private double clamp01(double val) {
    return Math.min(1.0, Math.max(0.0, val));
  }
  
  /**
   * Retrieves the color from the texture image at the given local intersection point,
   * using planar UV mapping based on the provided local normal vector, and applies
   * offset and scale transformations.
   * @param localPoint The intersection point in the object's local coordinates.
   * @param localNormal The normalized normal vector at the intersection point in local coordinates.
   * @return The Color sampled from the texture, or the baseDiffuseColor if no texture is loaded.
   */
  private Color getTextureColor(Point3 localPoint, Vector3 localNormal) {
    if (texture == null) {
      return baseDiffuseColor; // If no texture is loaded, return the base diffuse color
    }
    if (localNormal == null) {
      System.err.println("Warning: getTextureColor called with a null local normal. Cannot perform planar UV mapping. Returning base diffuse color.");
      return baseDiffuseColor;
    }
    
    // Planar UV mapping based on the dominant axis of the local normal.
    // This is consistent with other textured materials (Squared, Checkerboard, Striped, Circle).
    double u, v;
    double absNx = Math.abs(localNormal.x);
    double absNy = Math.abs(localNormal.y);
    double absNz = Math.abs(localNormal.z);
    
    // Project the 3D local point onto a 2D plane based on the dominant local normal axis.
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
    
    // Apply scaling (tiling) and offset (shifting)
    u = (u * uScale) + uOffset;
    v = (v * vScale) + vOffset;
    
    // Wrap UV coordinates to [0,1) range for seamless tiling
    u = (u % 1.0 + 1.0) % 1.0;
    v = (v % 1.0 + 1.0) % 1.0;
    
    // Map UV coordinates to texture pixel coordinates
    int texX = (int) (u * (texture.getWidth() - 1));
    int texY = (int) (v * (texture.getHeight() - 1));
    
    // Clamp texture coordinates to ensure they are within bounds [0, width-1] and [0, height-1]
    texX = Math.min(Math.max(0, texX), texture.getWidth() - 1);
    texY = Math.min(Math.max(0, texY), texture.getHeight() - 1);
    
    // Get the RGB integer value from the texture
    int rgb = texture.getRGB(texX, texY);
    // Ensure alpha channel is fully opaque (255) to prevent unexpected transparency issues
    return new Color(rgb | 0xFF000000);
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
