package net.elena.murat.material;

import java.awt.Color;
import java.awt.image.BufferedImage;

import net.elena.murat.light.Light;
import net.elena.murat.light.LightProperties;
import net.elena.murat.math.*;
import net.elena.murat.util.ColorUtil;
//import net.elena.murat.util.ImageUtils3D;

/**
 * ImageTextureMaterial applies a loaded BufferedImage as a texture to a surface.
 * It uses planar UV mapping based on the object's local space and applies a Phong-like
 * lighting model (Ambient + Diffuse + Specular).
 * This material fully implements the extended Material interface with proper texture wrapping
 * to eliminate black gaps between texture tiles.
 */
public class ImageTextureMaterial implements Material {
  private final BufferedImage image;
  private final double uScale;
  private final double vScale;
  private final double uOffset;
  private final double vOffset;
  
  // Phong material properties
  private final Color specularColor;
  private final double shininess;
  private final double ambientCoefficient;
  private final double diffuseCoefficient;
  private final double specularCoefficient;
  private final double reflectivity;
  private final double ior;
  
  // Constants
  private static final double OPAQUE = 0.0;
  private static final double TRANSPARENT = 1.0;
  private static final Color TRANSPARENT_COLOR = new Color(0, 0, 0, 0);
  
  private double transparency = OPAQUE;
  private Matrix4 objectInverseTransform;
  
  /**
   * Full constructor for ImageTextureMaterial.
   */
  public ImageTextureMaterial(
    BufferedImage image,
    double uScale,
    double vScale,
    double uOffset,
    double vOffset,
    double ambientCoefficient,
    double diffuseCoefficient,
    double specularCoefficient,
    double shininess,
    double reflectivity,
    double ior,
    Matrix4 objectInverseTransform) {
    
    this.image = image;//ImageUtils3D.convertToTransparentImage(image, this.transparency);
    this.uScale = uScale;
    this.vScale = vScale;
    this.uOffset = uOffset;
    this.vOffset = vOffset;
    this.ambientCoefficient = ambientCoefficient;
    this.diffuseCoefficient = diffuseCoefficient;
    this.specularCoefficient = specularCoefficient;
    this.shininess = shininess;
    this.reflectivity = clamp01(reflectivity);
    this.ior = Math.max(1.0, ior);
    this.objectInverseTransform = objectInverseTransform;
    this.specularColor = Color.WHITE;
  }
  
  /**
   * Constructor for non-reflective and non-transparent textures.
   */
  public ImageTextureMaterial(
    BufferedImage image,
    double uScale,
    double vScale,
    double uOffset,
    double vOffset,
    double ambientCoefficient,
    double diffuseCoefficient,
    double specularCoefficient,
    double shininess,
    Matrix4 objectInverseTransform) {
    
    this(image, uScale, vScale, uOffset, vOffset,
      ambientCoefficient, diffuseCoefficient, specularCoefficient,
    shininess, 0.0, 1.0, objectInverseTransform);
  }
  
  /**
   * Simplified constructor with default Phong properties.
   */
  public ImageTextureMaterial(BufferedImage image, Matrix4 objectInverseTransform) {
    this(image, 1.0, 1.0, 0.0, 0.0,
    0.1, 0.7, 0.7, 32.0, objectInverseTransform);
  }
  
  /**
   * Simplified constructor with custom scale.
   */
  public ImageTextureMaterial(BufferedImage image, double scale, Matrix4 objectInverseTransform) {
    this(image, scale, scale, 0.0, 0.0,
    0.1, 0.7, 0.7, 32.0, objectInverseTransform);
  }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
    if (tm == null) tm = new Matrix4 ();
    this.objectInverseTransform = tm;
  }
  
  @Override
  public Color getColorAt(Point3 worldPoint, Vector3 worldNormal, Light light, Point3 viewerPos) {
    if (objectInverseTransform == null) {
      setTransparency (TRANSPARENT);
      return TRANSPARENT_COLOR;
    }
    
    // Transform to local coordinates
    Point3 localPoint = objectInverseTransform.transformPoint(worldPoint);
    Vector3 localNormal = objectInverseTransform.inverseTransposeForNormal().transformVector(worldNormal).normalize();
    
    if (localNormal == null) {
      setTransparency (TRANSPARENT);
      return TRANSPARENT_COLOR;
    }
    
    // Get texture color with improved sampling
    Color textureColor = getTextureColor(localPoint, localNormal);
    
    if (textureColor.getAlpha() < 6) {
      setTransparency(TRANSPARENT);
      return TRANSPARENT_COLOR;
      } else {
      setTransparency(OPAQUE);
    }
    
    // Get light properties using LightProperties utility
    LightProperties lightProps = LightProperties.getLightProperties(light, worldPoint);
    
    // Calculate lighting using ColorUtil for operations
    return calculateLighting(textureColor, worldNormal, lightProps, viewerPos, worldPoint);
  }
  
  /**
   * Calculates the final lighting contribution using Phong model.
   */
  private Color calculateLighting(Color textureColor, Vector3 worldNormal,
    LightProperties lightProps, Point3 viewerPos, Point3 worldPoint) {
    
    // Convert to float components for calculations
    float[] texRGB = ColorUtil.getFloatComponents(textureColor);
    float[] lightRGB = ColorUtil.getFloatComponents(lightProps.color);
    
    // Initialize result components
    float r = 0.0f;
    float g = 0.0f;
    float b = 0.0f;
    
    // Ambient component
    if (lightProps.direction.lengthSquared() == 0) { // Ambient light
      r = (float)(texRGB[0] * ambientCoefficient * lightProps.intensity * lightRGB[0]);
      g = (float)(texRGB[1] * ambientCoefficient * lightProps.intensity * lightRGB[1]);
      b = (float)(texRGB[2] * ambientCoefficient * lightProps.intensity * lightRGB[2]);
      } else {
      // Diffuse component
      double diffuseFactor = Math.max(0, worldNormal.dot(lightProps.direction));
      
      r = (float)(texRGB[0] * diffuseCoefficient * diffuseFactor * lightRGB[0] * lightProps.intensity);
      g = (float)(texRGB[1] * diffuseCoefficient * diffuseFactor * lightRGB[1] * lightProps.intensity);
      b = (float)(texRGB[2] * diffuseCoefficient * diffuseFactor * lightRGB[2] * lightProps.intensity);
      
      // Specular component
      Vector3 viewDir = viewerPos.subtract(worldPoint).normalize();
      Vector3 reflectDir = lightProps.direction.negate().reflect(worldNormal);
      double specularFactor = Math.pow(Math.max(0, reflectDir.dot(viewDir)), shininess);
      
      float[] specRGB = ColorUtil.getFloatComponents(specularColor);
      
      r += (float)(specRGB[0] * specularCoefficient * specularFactor * lightRGB[0] * lightProps.intensity);
      g += (float)(specRGB[1] * specularCoefficient * specularFactor * lightRGB[1] * lightProps.intensity);
      b += (float)(specRGB[2] * specularCoefficient * specularFactor * lightRGB[2] * lightProps.intensity);
    }
    
    float origAlfa = (float)(textureColor.getAlpha ());
    
    // Clamp and return final color
    return new Color(
      ColorUtil.clamp(r, 0.0f, 1.0f),
      ColorUtil.clamp(g, 0.0f, 1.0f),
      ColorUtil.clamp(b, 0.0f, 1.0f),
      ColorUtil.clamp(origAlfa / 255F, 0.0f, 1.0f)
    );
  }
  
  /**
   * Retrieves texture color with improved sampling to eliminate black lines.
   */
  private Color getTextureColor(Point3 localPoint, Vector3 localNormal) {
    if (image == null || localNormal == null) {
      return TRANSPARENT_COLOR;
    }
    
    final int imgWidth = image.getWidth();
    final int imgHeight = image.getHeight();
    
    if (imgWidth == 0 || imgHeight == 0) {
      return TRANSPARENT_COLOR;
    }
    
    // Calculate UV coordinates based on dominant normal axis
    double[] uv = calculateUVCoordinates(localPoint, localNormal);
    double u = uv[0];
    double v = uv[1];
    
    // Apply texture transformations with proper wrapping
    u = ((u * uScale) + uOffset) % 1.0;
    v = ((v * vScale) + vOffset) % 1.0;
    
    // Ensure positive coordinates
    if (u < 0) u += 1.0;
    if (v < 0) v += 1.0;
    
    // Flip V coordinate for image coordinate system
    v = 1.0 - v;
    
    // Use bilinear filtering to eliminate black lines
    return sampleTextureWithFiltering(u, v, imgWidth, imgHeight);
  }
  
  /**
   * Calculates UV coordinates based on dominant normal axis.
   */
  private double[] calculateUVCoordinates(Point3 localPoint, Vector3 localNormal) {
    double absX = Math.abs(localNormal.x);
    double absY = Math.abs(localNormal.y);
    double absZ = Math.abs(localNormal.z);
    
    double u, v;
    
    if (absX >= absY && absX >= absZ) {
      u = localPoint.y;
      v = localPoint.z;
      } else if (absY >= absX && absY >= absZ) {
      u = localPoint.x;
      v = localPoint.z;
      } else {
      u = localPoint.x;
      v = localPoint.y;
    }
    
    return new double[]{u, v};
  }
  
  /**
   * Samples texture with bilinear filtering to eliminate black lines.
   */
  private Color sampleTextureWithFiltering(double u, double v, int imgWidth, int imgHeight) {
    // Convert to pixel coordinates with sub-pixel precision
    double x = u * (imgWidth - 1);
    double y = v * (imgHeight - 1);
    
    // Get surrounding pixels for bilinear filtering
    int x0 = (int) Math.floor(x);
    int y0 = (int) Math.floor(y);
    int x1 = (int) Math.ceil(x);
    int y1 = (int) Math.ceil(y);
    
    // Ensure coordinates are within bounds with proper wrapping
    x0 = wrapCoordinate(x0, imgWidth);
    y0 = wrapCoordinate(y0, imgHeight);
    x1 = wrapCoordinate(x1, imgWidth);
    y1 = wrapCoordinate(y1, imgHeight);
    
    // Get colors of surrounding pixels
    Color c00 = new Color(image.getRGB(x0, y0), true);
    Color c10 = new Color(image.getRGB(x1, y0), true);
    Color c01 = new Color(image.getRGB(x0, y1), true);
    Color c11 = new Color(image.getRGB(x1, y1), true);
    
    // Calculate interpolation factors
    double tx = x - x0;
    double ty = y - y0;
    
    // Perform bilinear interpolation using ColorUtil
    return ColorUtil.bilinearInterpolate(c00, c10, c01, c11, tx, ty);
  }
  
  /**
   * Wraps coordinate to stay within texture bounds.
   */
  private int wrapCoordinate(int coord, int max) {
    if (coord < 0) return max - 1 - ((-coord - 1) % max);
    return coord % max;
  }
  
  @Override
  public double getReflectivity() {
    return reflectivity;
  }
  
  @Override
  public double getIndexOfRefraction() {
    return ior;
  }
  
  @Override
  public double getTransparency() {
    return transparency;
  }
  
  public void setTransparency(double transparency) {
    this.transparency = clamp01(transparency);
  }
  
  /**
   * Clamps value between 0.0 and 1.0.
   */
  private double clamp01(double value) {
    return Math.max(0.0, Math.min(1.0, value));
  }
  
}
