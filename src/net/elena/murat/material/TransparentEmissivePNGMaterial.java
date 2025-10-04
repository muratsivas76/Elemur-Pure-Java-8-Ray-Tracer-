package net.elena.murat.material;

import java.awt.Color;
import java.awt.image.BufferedImage;

import net.elena.murat.light.Light;
import net.elena.murat.math.Matrix4;
import net.elena.murat.math.Point3;
import net.elena.murat.math.Vector3;

/**
 * Material that combines transparent PNG texture with emissive properties.
 * The texture provides the base color and alpha channel, while emissive properties
 * add self-illumination effects. Perfect for glowing transparent objects like emojis.
 */
public class TransparentEmissivePNGMaterial implements Material {
  
  private BufferedImage texture;
  private Matrix4 objectInverseTransform = new Matrix4();
  
  // UV parameters
  private double uOffset = 0.0;
  private double vOffset = 0.0;
  private double uScale = 1.0;
  private double vScale = 1.0;
  private boolean isRepeatTexture = false;
  
  // Emissive properties
  private Color emissiveColor;
  private double emissiveStrength;
  private double transparency = 1.0;
  
  /**
   * Constructor with texture and emissive properties
   * @param texture BufferedImage with alpha channel (PNG)
   * @param emissiveColor Color of the emitted light
   * @param emissiveStrength Intensity of the emission (0.0 - 1.0 or higher)
   */
  public TransparentEmissivePNGMaterial(BufferedImage texture, Color emissiveColor, double emissiveStrength) {
    this.texture = texture;
    this.emissiveColor = new Color(
      emissiveColor.getRed(),
      emissiveColor.getGreen(),
      emissiveColor.getBlue()
    );
    this.emissiveStrength = Math.max(0, emissiveStrength);
  }
  
  /**
   * Full constructor with UV parameters and emissive properties
   */
  public TransparentEmissivePNGMaterial(BufferedImage texture, double uOffset, double vOffset,
    double uScale, double vScale, boolean isRepeatTexture,
    Color emissiveColor, double emissiveStrength) {
    this.texture = texture;
    this.uOffset = uOffset;
    this.vOffset = vOffset;
    this.uScale = (uScale > 0.0) ? uScale : 1.0;
    this.vScale = (vScale > 0.0) ? vScale : 1.0;
    this.isRepeatTexture = isRepeatTexture;
    this.emissiveColor = new Color(
      emissiveColor.getRed(),
      emissiveColor.getGreen(),
      emissiveColor.getBlue()
    );
    this.emissiveStrength = Math.max(0, emissiveStrength);
  }
  
  @Override
  public void setObjectTransform(Matrix4 inverseTransform) {
    if (inverseTransform != null) {
      this.objectInverseTransform = inverseTransform;
      } else {
      this.objectInverseTransform = new Matrix4();
    }
  }
  
  @Override
  public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPos) {
    if (texture == null) {
      setTransparency (1.0);
      return new Color(0, 0, 0, 0);
    }
    
    Point3 local = objectInverseTransform.transformPoint(point);
    
    // Base UV coordinates mapped from [-1,1] to [0,1]
    double u = (local.x + 1.0) * 0.5;
    double v = (1.0 - (local.y + 1.0) * 0.5);
    
    // Apply UV scale and offset
    double scaledU = u / uScale + uOffset;
    double scaledV = v / vScale + vOffset;
    
    double finalU, finalV;
    
    if (isRepeatTexture) {
      // Wrap UVs for tiling (repeat)
      finalU = scaledU - Math.floor(scaledU);
      finalV = scaledV - Math.floor(scaledV);
      } else {
      // No tiling: if UV outside [0,1], return fully transparent color
      if (scaledU < 0.0 || scaledU > 1.0 || scaledV < 0.0 || scaledV > 1.0) {
        setTransparency(1.0);
        return new Color(0, 0, 0, 0);
      }
      finalU = scaledU;
      finalV = scaledV;
    }
    
    int px = (int) (finalU * (texture.getWidth() - 1));
    int py = (int) (finalV * (texture.getHeight() - 1));
    
    int argb = texture.getRGB(px, py);
    
    int alpha = (argb >> 24) & 0xFF;
    int red = (argb >> 16) & 0xFF;
    int green = (argb >> 8) & 0xFF;
    int blue = argb & 0xFF;
    
    if (alpha > 5) {
      setTransparency(0.0);
      
      int emissiveRed = (int) (emissiveColor.getRed() * emissiveStrength);
      int emissiveGreen = (int) (emissiveColor.getGreen() * emissiveStrength);
      int emissiveBlue = (int) (emissiveColor.getBlue() * emissiveStrength);
      
      int finalRed = clampColorValue(red + emissiveRed);
      int finalGreen = clampColorValue(green + emissiveGreen);
      int finalBlue = clampColorValue(blue + emissiveBlue);
      
      return new Color(finalRed, finalGreen, finalBlue, 255);
    }
    
    setTransparency(1.0);
    return new Color(0, 0, 0, 0);
  }
  
  /**
   * Gets the color from the texture at the given point
   */
  private Color getTextureColor(Point3 point) {
    if (texture == null) {
      return new Color(0, 0, 0, 0);
    }
    
    Point3 local = objectInverseTransform.transformPoint(point);
    double u = (local.x + 1.0) * 0.5;
    double v = (1.0 - (local.y + 1.0) * 0.5);
    
    double scaledU = u / uScale + uOffset;
    double scaledV = v / vScale + vOffset;
    
    double finalU, finalV;
    
    if (isRepeatTexture) {
      finalU = scaledU - Math.floor(scaledU);
      finalV = scaledV - Math.floor(scaledV);
      } else {
      if (scaledU < 0.0 || scaledU > 1.0 || scaledV < 0.0 || scaledV > 1.0) {
        return new Color(0, 0, 0, 0);
      }
      finalU = scaledU;
      finalV = scaledV;
    }
    
    int px = (int) (finalU * (texture.getWidth() - 1));
    int py = (int) (finalV * (texture.getHeight() - 1));
    int argb = texture.getRGB(px, py);
    
    int alpha = (argb >> 24) & 0xFF;
    int red = (argb >> 16) & 0xFF;
    int green = (argb >> 8) & 0xFF;
    int blue = argb & 0xFF;
    
    return new Color(red, green, blue, alpha);
  }
  
  /**
   * Combines texture color with emissive light
   */
  private Color combineTextureWithEmission(Color textureColor) {
    // Get texture RGB components
    int r = textureColor.getRed();
    int g = textureColor.getGreen();
    int b = textureColor.getBlue();
    
    // Get emissive RGB components scaled by strength
    int er = (int) (emissiveColor.getRed() * emissiveStrength);
    int eg = (int) (emissiveColor.getGreen() * emissiveStrength);
    int eb = (int) (emissiveColor.getBlue() * emissiveStrength);
    
    // Add emissive light to texture color (with clamping)
    int finalR = clampColorValue(r + er);
    int finalG = clampColorValue(g + eg);
    int finalB = clampColorValue(b + eb);
    
    return new Color(finalR, finalG, finalB, textureColor.getAlpha());
  }
  
  private int clampColorValue(double value) {
    return (int) Math.min(255, Math.max(0, value));
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
  
  private void setTransparency (double tnw) {
    this.transparency = tnw;
  }
  
  // Getters and setters for emissive properties
  public Color getEmissiveColor() {
    return new Color(
      emissiveColor.getRed(),
      emissiveColor.getGreen(),
      emissiveColor.getBlue()
    );
  }
  
  public void setEmissiveColor(Color emissiveColor) {
    this.emissiveColor = new Color(
      emissiveColor.getRed(),
      emissiveColor.getGreen(),
      emissiveColor.getBlue()
    );
  }
  
  public double getEmissiveStrength() {
    return emissiveStrength;
  }
  
  public void setEmissiveStrength(double emissiveStrength) {
    this.emissiveStrength = Math.max(0, emissiveStrength);
  }
  
  // UV parameter getters and setters
  public double getUOffset() { return uOffset; }
  public void setUOffset(double uOffset) { this.uOffset = uOffset; }
  
  public double getVOffset() { return vOffset; }
  public void setVOffset(double vOffset) { this.vOffset = vOffset; }
  
  public double getUScale() { return uScale; }
  public void setUScale(double uScale) { this.uScale = uScale > 0.0 ? uScale : 1.0; }
  
  public double getVScale() { return vScale; }
  public void setVScale(double vScale) { this.vScale = vScale > 0.0 ? vScale : 1.0; }
  
  public boolean isRepeatTexture() { return isRepeatTexture; }
  public void setRepeatTexture(boolean repeat) { this.isRepeatTexture = repeat; }
  
}
