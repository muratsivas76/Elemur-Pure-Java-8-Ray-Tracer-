package net.elena.murat.material;

import java.awt.Color;
import java.awt.image.BufferedImage;

import net.elena.murat.light.Light;
import net.elena.murat.math.Matrix4;
import net.elena.murat.math.Point3;
import net.elena.murat.math.Vector3;

/**
 * Material class that textures a surface with a transparent PNG image.
 * Supports alpha channel and returns fully transparent color for transparent pixels.
 * Uses planar UV mapping on XY plane (Z ignored).
 *
 * Assumes UV coordinates are derived from object local coordinates mapped from [-1,1] to [0,1].
 * The objectInverseTransform is used to convert world coordinates to local object space.
 *
 * Supports UV offset, scale, and optional repeating of the texture.
 * Includes strict alpha handling for complete transparency.
 */
public class TransparentPNGMaterial implements Material {
  
  private BufferedImage texture;
  private Matrix4 objectInverseTransform = new Matrix4(); // Identity by default
  
  private double transparency = 1.0;
  
  // UV offset and scale parameters with default values (no offset, scale=1)
  private double uOffset = 0.0;
  private double vOffset = 0.0;
  private double uScale = 1.0;
  private double vScale = 1.0;
  
  // Flag to enable repeating the texture outside [0,1] UV range
  private boolean isRepeatTexture = false;
  
  /**
   * Constructor with texture image.
   * @param texture BufferedImage with alpha channel (PNG)
   */
  public TransparentPNGMaterial(BufferedImage texture) {
    this.texture = texture;
  }
  
  /**
   * Constructor with texture image and UV offset/scale parameters.
   * @param texture BufferedImage with alpha channel (PNG)
   * @param uOffset Horizontal offset for texture coordinates (0.0 = no offset)
   * @param vOffset Vertical offset for texture coordinates (0.0 = no offset)
   * @param uScale Horizontal scale factor (1.0 = original size)
   * @param vScale Vertical scale factor (1.0 = original size)
   * @param isRepeatTexture Whether to repeat the texture outside [0,1] UV range
   */
  public TransparentPNGMaterial(BufferedImage texture, double uOffset, double vOffset, double uScale, double vScale, boolean isRepeatTexture) {
    this.texture = texture;
    this.uOffset = uOffset;
    this.vOffset = vOffset;
    this.uScale = (uScale > 0.0) ? uScale : 1.0; // Prevent zero or negative scale
    this.vScale = (vScale > 0.0) ? vScale : 1.0;
    this.isRepeatTexture = isRepeatTexture;
  }
  
  /**
   * Default constructor (no texture)
   */
  public TransparentPNGMaterial() {
    this.texture = null;
  }
  
  /**
   * Sets the inverse transform matrix of the object.
   * Used to convert world coordinates to local object space.
   * @param inverseTransform Matrix4 inverse transform
   */
  @Override
  public void setObjectTransform(Matrix4 inverseTransform) {
    if (inverseTransform != null) {
      this.objectInverseTransform = inverseTransform;
      } else {
      this.objectInverseTransform = new Matrix4(); // Identity fallback
    }
  }
  
  /**
   * Returns the color at the given world point on the surface.
   * Uses planar UV mapping on XY plane.
   * Fully transparent pixels return Color(0,0,0,0).
   * Applies UV offset, scale, and optional repeating.
   * Uses strict alpha checking: any alpha less than 255 returns full transparency.
   *
   * @param point World space point on surface
   * @param normal Surface normal (unused here)
   * @param light Light source (unused here)
   * @param viewerPos Viewer position (unused here)
   * @return Color with alpha channel
   */
  @Override
  public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPos) {
    if (texture == null) {
      setTransparency(1.0); // Fully transparent
      return new Color(0, 0, 0, 0);
    }
    
    // Transform world coordinates to local object space
    Point3 local = objectInverseTransform.transformPoint(point);
    
    // Base UV coordinates mapped from [-1,1] to [0,1]
    double u = (local.x + 1.0) * 0.5;
    double v = (1.0 - (local.y + 1.0) * 0.5);
    
    // Apply UV scale and offset
    double scaledU = u / uScale + uOffset;
    double scaledV = v / vScale + vOffset;
    
    // DEBUG
    //if (Math.random() < 0.0001) System.out.println("Local: " + local +
    //               ", U: " + u + ", V: " + v +
    //               ", ScaledU: " + scaledU + ", ScaledV: " + scaledV);
    
    double finalU, finalV;
    
    if (isRepeatTexture) {
      // Wrap UVs for tiling (repeat)
      finalU = scaledU - Math.floor(scaledU);
      finalV = scaledV - Math.floor(scaledV);
      } else {
      // No tiling: if UV outside [0,1], return fully transparent color
      if (scaledU < 0.0 || scaledU > 1.0 || scaledV < 0.0 || scaledV > 1.0) {
        setTransparency(1.0); // Fully transparent
        return new Color(0, 0, 0, 0);
        //setTransparency(0.0);
        //return new Color(1f, 0f, 0f, 1f);
      }
      finalU = scaledU;
      finalV = scaledV;
    }
    
    //double margin = 0.05;
    //finalU = margin + finalU * (1.0 - 2 * margin);
    //finalV = margin + finalV * (1.0 - 2 * margin);
    
    // Calculate texture coordinates
    int px = (int) (finalU * (texture.getWidth() - 1));
    int py = (int) (finalV * (texture.getHeight() - 1));
    
    // Ensure coordinates are within texture bounds
    px = Math.max(0, Math.min(texture.getWidth() - 1, px));
    py = Math.max(0, Math.min(texture.getHeight() - 1, py));
    
    // Get pixel color with alpha channel
    int argb = texture.getRGB(px, py);
    int alpha = (argb >> 24) & 0xFF;
    
    // Otherwise return fully transparent color
    if (alpha > 5) {
      int red = (argb >> 16) & 0xFF;
      int green = (argb >> 8) & 0xFF;
      int blue = argb & 0xFF;
      setTransparency(0.0); // Fully opaque
      return new Color(red, green, blue, 255);
    }
    
    // For any alpha value less than 255, return fully transparent
    setTransparency(1.0); // Fully transparent
    return new Color(0, 0, 0, 0);
  }
  
  /**
   * Returns reflectivity of the material.
   * @return 0.0 (non-reflective)
   */
  @Override
  public double getReflectivity() {
    return 0.0;
  }
  
  /**
   * Returns index of refraction.
   * @return 1.0 (no refraction)
   */
  @Override
  public double getIndexOfRefraction() {
    return 1.0;
  }
  
  /**
   * Returns transparency of the material.
   * @return transparency value (0.0 = opaque, 1.0 = fully transparent)
   */
  @Override
  public double getTransparency() {
    return transparency;
  }
  
  /**
   * Sets the transparency value.
   * @param transparency transparency value (0.0 = opaque, 1.0 = fully transparent)
   */
  private void setTransparency(double transparency) {
    this.transparency = transparency;
  }
  
  /**
   * Gets the horizontal texture offset.
   * @return U offset value
   */
  public double getUOffset() {
    return uOffset;
  }
  
  /**
   * Gets the vertical texture offset.
   * @return V offset value
   */
  public double getVOffset() {
    return vOffset;
  }
  
  /**
   * Gets the horizontal texture scale factor.
   * @return U scale factor
   */
  public double getUScale() {
    return uScale;
  }
  
  /**
   * Gets the vertical texture scale factor.
   * @return V scale factor
   */
  public double getVScale() {
    return vScale;
  }
  
  /**
   * Checks if texture repeating is enabled.
   * @return true if texture repeating is enabled, false otherwise
   */
  public boolean isRepeatTexture() {
    return isRepeatTexture;
  }
  
  /**
   * Sets whether texture repeating is enabled.
   * @param repeat true to enable repeating, false to disable
   */
  public void setRepeatTexture(boolean repeat) {
    this.isRepeatTexture = repeat;
  }
  
  /**
   * Sets the texture image.
   * @param texture BufferedImage with alpha channel
   */
  public void setTexture(BufferedImage texture) {
    this.texture = texture;
  }
  
  /**
   * Gets the current texture image.
   * @return current texture image
   */
  public BufferedImage getTexture() {
    return texture;
  }
  
}
