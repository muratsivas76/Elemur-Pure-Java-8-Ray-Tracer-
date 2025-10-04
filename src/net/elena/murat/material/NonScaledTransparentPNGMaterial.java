package net.elena.murat.material;

import java.awt.Color;
import java.awt.image.BufferedImage;

import net.elena.murat.light.Light;
import net.elena.murat.math.*;
import net.elena.murat.util.ColorUtil;

/**
 * Material that textures a surface with a transparent PNG image
 * using original pixel dimensions without scaling or tiling.
 * Assumes planar UV mapping on XY plane.
 */
public class NonScaledTransparentPNGMaterial implements Material {
  
  private BufferedImage texture;
  private Matrix4 objectInverseTransform = new Matrix4();
  private double transparency = 1.0;
  
  private final int originalWidth;
  private final int originalHeight;
  
  private final double billboardWidth;
  private final double billboardHeight;
  
  private float gammaCorrection = 2.4f;
  
  private double shadowAlphaThreshold = 0.1;
  
  /**
   * Constructor.
   * @param texture BufferedImage with alpha channel (PNG)
   * @param billboardWidth Example 6.6
   * @param billboardHeight Example 3.6
   */
  public NonScaledTransparentPNGMaterial(BufferedImage texture,
    double billboardWidth, double billboardHeight) {
    if (texture == null) {
      throw new IllegalArgumentException("Texture cannot be null");
    }
    this.texture = texture;
    this.originalWidth = texture.getWidth();
    this.originalHeight = texture.getHeight();
    
    this.billboardWidth = billboardWidth;
    this.billboardHeight = billboardHeight;
  }
  
  public void setGammaCorrection (float gamma) {
    this.gammaCorrection = gamma;
  }
  
  @Override
  public void setObjectTransform(Matrix4 inverseTransform) {
    if (inverseTransform != null) {
      this.objectInverseTransform = inverseTransform;
      } else {
      this.objectInverseTransform = new Matrix4();
    }
  }
  
  /**
   * Returns the color at the given world point on the surface.
   * Uses planar UV mapping on XY plane.
   * Fully transparent pixels return Color(0,0,0,0).
   * Applies UV offset.
   *
   * @param point World space point on surface
   * @param normal Surface normal (unused here)
   * @param light Light source (unused here)
   * @param viewerPos Viewer position (unused here)
   * @return Color with alpha channel
   */
  // Original
  @Override
  public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPos) {
    if (texture == null) {
      setTransparency(1.0);
      return new Color(0, 0, 0, 0);
    }
    
    Point3 local = objectInverseTransform.transformPoint(point);
    
    // Directly map local coordinates to [0, 1] without scaling
    double u = (local.x / billboardWidth) + 0.5; // [-0.5, 0.5] -> [0, 1]
    double v = (local.y / billboardHeight) + 0.5; // [-0.5, 0.5] -> [0, 1]
    v = 1.0 - v; // Flip V for image coordinates
    
    // Clamp to [0,1] to prevent sampling outside texture
    u = Math.max(0.0, Math.min(1.0, u));
    v = Math.max(0.0, Math.min(1.0, v));
    
    int px = (int) (u * (originalWidth - 1));
    int py = (int) (v * (originalHeight - 1));
    
    int argb = texture.getRGB(px, py);
    int alpha = (argb >> 24) & 0xFF;
    
    if (alpha > 5) {
      setTransparency(0.0);
      int red = (argb >> 16) & 0xFF;
      int green = (argb >> 8) & 0xFF;
      int blue = argb & 0xFF;
      Color linearColor = ColorUtil.sRGBToLinear(new Color(red, green, blue), gammaCorrection);
      return linearColor;
    }
    
    setTransparency(1.0);
    return new Color(0, 0, 0, 0);
  }
  
  public boolean hasShadowAt(Point3 point) {
    if (texture == null) {
      return false;
    }
    
    Point3 localPoint = objectInverseTransform.transformPoint(point);
    
    double nx = localPoint.x / (billboardWidth / 2.0);
    double ny = localPoint.y / (billboardHeight / 2.0);
    if (nx * nx + ny * ny > 1.0) {
      return false;
    }
    
    double u = (localPoint.x + 1.0) * 0.5;
    double v = (1.0 - (localPoint.y + 1.0) * 0.5);
    
    u = clamp(u, 0.0, 1.0);
    v = clamp(v, 0.0, 1.0);
    
    int xPixel = (int)(u * originalWidth);
    int yPixel = (int)(v * originalHeight);
    
    xPixel = Math.min(Math.max(xPixel, 0), originalWidth - 1);
    yPixel = Math.min(Math.max(yPixel, 0), originalHeight - 1);
    
    int argb = texture.getRGB(xPixel, yPixel);
    int alpha = (argb >> 24) & 0xFF;
    
    return (alpha / 255.0) >= shadowAlphaThreshold;
  }
  
  private double clamp(double value, double min, double max) {
    return Math.max(min, Math.min(max, value));
  }
  
  @Override
  public double getReflectivity() {
    return 0.0;
  }
  
  @Override
  public double getIndexOfRefraction() {
    return 1.0;
  }
  
  @Override
  public double getTransparency() {
    return transparency;
  }
  
  private void setTransparency(double t) {
    this.transparency = t;
  }
  
  public double getShadowAlphaThreshold() {
    return shadowAlphaThreshold;
  }
  
  public void setShadowAlphaThreshold(double threshold) {
    this.shadowAlphaThreshold = clamp(threshold, 0.0, 1.0);
  }
  
  public int getOriginalWidth() {
    return originalWidth;
  }
  
  public int getOriginalHeight() {
    return originalHeight;
  }
  
  public double getBillboardWidth() {
    return billboardWidth;
  }
  
  public double getBillboardHeight() {
    return billboardHeight;
  }
  
}
