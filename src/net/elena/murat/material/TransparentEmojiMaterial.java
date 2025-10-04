package net.elena.murat.material;

import java.awt.Color;
import java.awt.image.BufferedImage;

import net.elena.murat.light.Light;
import net.elena.murat.math.Matrix4;
import net.elena.murat.math.Point3;
import net.elena.murat.math.Vector3;

/**
 * Material that displays a transparent emoji/image texture on a surface with
 * a checkerboard background pattern that covers the entire surface uniformly.
 * The emoji/image texture respects UV scaling and offset parameters with optional repeating.
 * Designed for EmojiBillboard (quad in XY plane, Z=0).
 * Uses planar UV mapping on X and Y axes with practical scaling and offset.
 * Supports alpha channel, extra transparency, and checkerboard background.
 */
public class TransparentEmojiMaterial implements Material {
  private final BufferedImage image;
  private double transparency;
  private final Color checkerColor1;
  private final Color checkerColor2;
  private final double checkerSize;
  private final double uOffset;
  private final double vOffset;
  private final double uScale;
  private final double vScale;
  private final boolean isRepeatTexture;
  
  private Matrix4 objectInverseTransform;
  private double objectWidth = 2.0;
  private double objectHeight = 2.0;
  
  private final boolean isMessy;
  
  /**
   * Full constructor with all parameters including UV scaling and repeat option
   * @param image The RGBA BufferedImage (with alpha channel)
   * @param checkerColor1 First color of the checkerboard pattern
   * @param checkerColor2 Second color of the checkerboard pattern
   * @param checkerSize Size of each checkerboard tile in UV coordinates
   * @param uOffset Horizontal offset for texture coordinates (0.0-1.0 range)
   * @param vOffset Vertical offset for texture coordinates (0.0-1.0 range)
   * @param uScale Horizontal scaling factor (1.0 = original size, 0.5 = half size, 2.0 = double size)
   * @param vScale Vertical scaling factor (1.0 = original size, 0.5 = half size, 2.0 = double size)
   * @param isRepeatTexture Whether to repeat the texture outside [0,1] UV range
   */
  public TransparentEmojiMaterial(BufferedImage image,
    Color checkerColor1, Color checkerColor2,
    double checkerSize, double uOffset, double vOffset,
    double uScale, double vScale,
    boolean isRepeatTexture,
    boolean isMessy) {
    this.image = image;
    this.checkerColor1 = checkerColor1;
    this.checkerColor2 = checkerColor2;
    this.checkerSize = Math.max(0.01, checkerSize);
    this.uOffset = uOffset;
    this.vOffset = vOffset;
    this.uScale = Math.max(0.01, uScale);
    this.vScale = Math.max(0.01, vScale);
    this.isRepeatTexture = isRepeatTexture;
    this.isMessy = isMessy;
    this.objectInverseTransform = new Matrix4();
  }
  
  /**
   * Constructor with default checkerboard colors and UV scaling
   */
  public TransparentEmojiMaterial(BufferedImage image,
    double checkerSize, double uScale, double vScale) {
    this(image,
      new Color(200, 200, 200), new Color(150, 150, 150),
    checkerSize, 0.0, 0.0, uScale, vScale, false, false);
  }
  
  /**
   * Constructor with default checkerboard size and UV scaling
   */
  public TransparentEmojiMaterial(BufferedImage image,
    double uScale, double vScale) {
    this(image, 0.1, uScale, vScale);
  }
  
  /**
   * Constructor with default parameters (no scaling, no offset)
   */
  public TransparentEmojiMaterial(BufferedImage image) {
    this(image, 0.1, 1.0, 1.0);
  }
  
  /**
   * Empty constructor for later setup
   */
  public TransparentEmojiMaterial() {
    this(null, 0.1, 1.0, 1.0);
  }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
    if (tm == null) tm = new Matrix4 ();
    this.objectInverseTransform = tm;
    
    // Extract object dimensions from transform matrix for proper scaling
    // Assuming the object is a quad with original size 2x2 units (from -1 to +1)
    // The scale factors are in the columns of the transformation matrix
    this.objectWidth = 2.0 * Math.sqrt(
      tm.get(0, 0) * tm.get(0, 0) +
      tm.get(1, 0) * tm.get(1, 0) +
      tm.get(2, 0) * tm.get(2, 0)
    );
    
    this.objectHeight = 2.0 * Math.sqrt(
      tm.get(0, 1) * tm.get(0, 1) +
      tm.get(1, 1) * tm.get(1, 1) +
      tm.get(2, 1) * tm.get(2, 1)
    );
  }
  
  @Override
  public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPos) {
    // Transform world point to local object space
    Point3 localPoint = objectInverseTransform.transformPoint(point);
    
    // Base UV coordinates for checkerboard background (range [0,1])
    double u_bg = (localPoint.x + 1.0) * 0.5;
    double v_bg = (1.0 - localPoint.y) * 0.5;
    
    // Get checkerboard background color at these UVs
    Color backgroundColor = getBackgroundCheckerboardColor(u_bg, v_bg);
    
    if (isMessy) {
      if (backgroundColor.getAlpha () < 6) {
        setTransparency (1.0);
        return backgroundColor;
      }
    }
    
    // If no image, return background color immediately
    if (image == null) {
      setTransparency(0.0);
      return backgroundColor;
    }
    
    // Base UV for texture
    double u_tex = u_bg;
    double v_tex = v_bg;
    
    // Apply scale and offset (do NOT normalize offset)
    double scaledU = u_tex / uScale + uOffset;
    double scaledV = v_tex / vScale + vOffset;
    
    double finalU, finalV;
    
    if (isRepeatTexture) {
      // Wrap UVs for tiling
      finalU = scaledU - Math.floor(scaledU);
      finalV = scaledV - Math.floor(scaledV);
      } else {
      // No tiling: if UV outside [0,1], return background color immediately
      if (scaledU < 0.0 || scaledU > 1.0 || scaledV < 0.0 || scaledV > 1.0) {
        setTransparency(0.0);
        return backgroundColor;
      }
      finalU = scaledU;
      finalV = scaledV;
    }
    
    // Convert UV to pixel coordinates
    int x = (int) (finalU * (image.getWidth() - 1));
    int y = (int) (finalV * (image.getHeight() - 1));
    
    // Clamp pixel indices to valid range
    x = Math.max(0, Math.min(image.getWidth() - 1, x));
    y = Math.max(0, Math.min(image.getHeight() - 1, y));
    
    // Get pixel ARGB
    int argb = image.getRGB(x, y);
    int alpha = (argb >> 24) & 0xFF;
    
    // Extract RGB
    int r = (argb >> 16) & 0xFF;
    int g = (argb >> 8) & 0xFF;
    int b = argb & 0xFF;
    
    if (alpha == 0) {
      // Fully transparent pixel: show checkerboard background
      setTransparency(1.0);
      return backgroundColor;
      } else {
      // Opaque or semi-transparent pixel: blend PNG color with background based on alpha
      
      setTransparency(0.0);
      
      float alphaF = alpha / 255f;
      
      // Simple alpha blending: result = alpha * PNG + (1 - alpha) * background
      float bgR = backgroundColor.getRed() / 255f;
      float bgG = backgroundColor.getGreen() / 255f;
      float bgB = backgroundColor.getBlue() / 255f;
      
      float outR = alphaF * (r / 255f) + (1 - alphaF) * bgR;
      float outG = alphaF * (g / 255f) + (1 - alphaF) * bgG;
      float outB = alphaF * (b / 255f) + (1 - alphaF) * bgB;
      
      return new Color(outR, outG, outB, 1.0f);
    }
  }
  
  /**
   * Returns the checkerboard color at given UV coordinates.
   * Checkerboard pattern covers entire surface uniformly, ignoring scale and offset.
   * @param u Horizontal UV coordinate [0,1]
   * @param v Vertical UV coordinate [0,1]
   * @return Checkerboard color at UV
   */
  private Color getBackgroundCheckerboardColor(double u, double v) {
    int ix = (int) Math.floor(u / checkerSize);
    int iy = (int) Math.floor(v / checkerSize);
    return ((ix + iy) % 2 == 0) ? checkerColor1 : checkerColor2;
  }
  
  /**
   * Gets pixel color with bounds checking and alpha support
   * @param x X coordinate in image space
   * @param y Y coordinate in image space
   * @return Color with alpha channel, or transparent if out of bounds
   */
  private Color getPixelColor(int x, int y) {
    if (image == null) return new Color(0, 0, 0, 0);
    if (x < 0 || x >= image.getWidth() || y < 0 || y >= image.getHeight()) {
      return new Color(0, 0, 0, 0);
    }
    int rgb = image.getRGB(x, y);
    return new Color(
      (rgb >> 16) & 0xFF,
      (rgb >> 8) & 0xFF,
      rgb & 0xFF,
      (rgb >> 24) & 0xFF
    );
  }
  
  @Override
  public double getReflectivity() {
    return 0.0; // Non-reflective material
  }
  
  @Override
  public double getIndexOfRefraction() {
    return 1.0; // No refraction (same as air)
  }
  
  @Override
  public double getTransparency() {
    return transparency;
  }
  
  private void setTransparency(double tnw) {
    this.transparency = tnw;
  }
  
  /**
   * Gets the first checkerboard color
   * @return First checkerboard color
   */
  public Color getCheckerColor1() {
    return checkerColor1;
  }
  
  /**
   * Gets the second checkerboard color
   * @return Second checkerboard color
   */
  public Color getCheckerColor2() {
    return checkerColor2;
  }
  
  /**
   * Gets the checkerboard tile size
   * @return Checkerboard tile size in UV coordinates
   */
  public double getCheckerSize() {
    return checkerSize;
  }
  
  /**
   * Gets the horizontal texture offset
   * @return U offset value
   */
  public double getUOffset() {
    return uOffset;
  }
  
  /**
   * Gets the vertical texture offset
   * @return V offset value
   */
  public double getVOffset() {
    return vOffset;
  }
  
  /**
   * Gets the horizontal texture scale factor
   * @return U scale factor
   */
  public double getUScale() {
    return uScale;
  }
  
  /**
   * Gets the vertical texture scale factor
   * @return V scale factor
   */
  public double getVScale() {
    return vScale;
  }
  
  /**
   * Checks if texture repeating is enabled
   * @return true if texture repeating is enabled, false otherwise
   */
  public boolean isItRepeatTexture() {
    return true;
  }
  
  /**
   * Linear interpolation helper method
   * @param a Start value
   * @param b End value
   * @param t Interpolation factor [0,1]
   * @return Interpolated value between a and b
   */
  private double lerp(double a, double b, double t) {
    return a + t * (b - a);
  }
  
}
