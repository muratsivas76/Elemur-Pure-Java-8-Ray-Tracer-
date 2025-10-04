package net.elena.murat.material;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import net.elena.murat.light.Light;
import net.elena.murat.math.Matrix4;
import net.elena.murat.math.Point3;
import net.elena.murat.math.Vector3;

/**
 * Material class that generates textures with rendered text and optional image on the fly.
 * Supports custom text, fonts, colors, gradients, transparent backgrounds, and image integration.
 * Uses planar UV mapping on XY plane (Z ignored) similar to TransparentPNGMaterial.
 */
public class WordMaterial implements Material {
  
  private BufferedImage texture;
  private Matrix4 objectInverseTransform = new Matrix4();
  private double transparency = 1.0;
  
  // UV parameters
  private double uOffset = 0.0;
  private double vOffset = 0.0;
  private double uScale = 1.0;
  private double vScale = 1.0;
  private boolean isRepeatTexture = false;
  
  private boolean isTriangleEtc = false;
  
  // Text rendering parameters
  private String text;
  private Color foregroundColor;
  private Color backgroundColor;
  private Font font;
  private boolean gradientEnabled;
  private Color gradientColor;
  private BufferedImage wordImage;
  private int width;
  private int height;
  
  /**
   * Constructor with default styling (white text on transparent background, Arial Bold 48)
   * @param text The text to render on the material
   */
  public WordMaterial(String text) {
    this(text, Color.WHITE, new Color(0x00000000, true), new Font("Arial", Font.BOLD, 48),
    false, null, null, 256, 256);
  }
  
  /**
   * Constructor with custom colors and font
   * @param text The text to render
   * @param foregroundColor Text color (RGB or ARGB)
   * @param backgroundColor Background color (use 0x00000000 for transparent)
   * @param font The font to use for rendering
   */
  public WordMaterial(String text, Color foregroundColor, Color backgroundColor, Font font) {
    this(text, foregroundColor, backgroundColor, font, false, null, null, 256, 256);
  }
  
  /**
   * Constructor with gradient support
   * @param text The text to render
   * @param foregroundColor Starting gradient color
   * @param backgroundColor Background color
   * @param font The font to use
   * @param gradientColor Ending gradient color (if null, no gradient is applied)
   */
  public WordMaterial(String text, Color foregroundColor, Color backgroundColor,
    Font font, Color gradientColor) {
    this(text, foregroundColor, backgroundColor, font, true, gradientColor, null, 256, 256);
  }
  
  /**
   * Constructor with image support
   * @param text The text to render
   * @param foregroundColor Text color
   * @param backgroundColor Background color
   * @param font The font to use
   * @param wordImage Optional image to display above text (null for text only)
   */
  public WordMaterial(String text, Color foregroundColor, Color backgroundColor,
    Font font, BufferedImage wordImage) {
    this(text, foregroundColor, backgroundColor, font, false, null, wordImage,
    wordImage != null ? 384 : 256, wordImage != null ? 384 : 256);
  }
  
  /**
   * Constructor with custom size
   * @param text The text to render
   * @param foregroundColor Text color
   * @param backgroundColor Background color
   * @param font The font to use
   * @param width Texture width
   * @param height Texture height
   */
  public WordMaterial(String text, Color foregroundColor, Color backgroundColor,
    Font font, int width, int height) {
    this(text, foregroundColor, backgroundColor, font, false, null, null, width, height);
  }
  
  /**
   * Full constructor with all parameters
   * @param text The text to render
   * @param foregroundColor Text color
   * @param backgroundColor Background color
   * @param font The font to use
   * @param useGradient Whether to apply gradient effect
   * @param gradientColor Gradient end color (required if useGradient is true)
   * @param wordImage Optional image to display above text (null for text only)
   * @param width Texture width
   * @param height Texture height
   */
  public WordMaterial(String text, Color foregroundColor, Color backgroundColor,
    Font font, boolean useGradient, Color gradientColor, BufferedImage wordImage,
    int width, int height) {
    this.text = text;
    this.foregroundColor = foregroundColor;
    this.backgroundColor = backgroundColor;
    this.font = font;
    this.gradientEnabled = useGradient;
    this.gradientColor = gradientColor;
    this.wordImage = wordImage;
    this.width = width;
    this.height = height;
    
    this.texture = createTextImage(text, foregroundColor, backgroundColor, font,
    useGradient, gradientColor, wordImage, width, height);
  }
  
  /**
   * Creates a BufferedImage with the rendered text and optional image
   * @param text Text to render
   * @param fgColor Text color
   * @param bgColor Background color
   * @param font Font to use
   * @param useGradient Whether to use gradient
   * @param gradientColor Gradient end color
   * @param wordImage Optional image to display above text
   * @param width Image width
   * @param height Image height
   * @return BufferedImage with rendered content
   */
  private static BufferedImage createTextImage(String text, Color fgColor, Color bgColor,
    Font font, boolean useGradient, Color gradientColor,
    BufferedImage wordImage, int width, int height) {
    
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2d = image.createGraphics();
    
    // Clear background (transparent or colored)
    if (bgColor.getAlpha() == 0) {
      g2d.setComposite(AlphaComposite.Clear);
      g2d.fillRect(0, 0, width, height);
      g2d.setComposite(AlphaComposite.SrcOver);
      } else {
      g2d.setColor(bgColor);
      g2d.fillRect(0, 0, width, height);
    }
    
    // Enable anti-aliasing for smooth rendering
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    
    // Draw word image if provided
    if (wordImage != null) {
      int imageSize = Math.min(width, height) - 64;
      int imageX = (width - imageSize) / 2;
      int imageY = 32;
      
      g2d.drawImage(wordImage, imageX, imageY, imageSize, imageSize, null);
    }
    
    // Apply gradient or solid color for text
    if (useGradient && gradientColor != null) {
      int textY = (wordImage != null) ? (height * 2 / 3) : (height / 2);
      GradientPaint gradient = new GradientPaint(0, textY, fgColor, width, textY + 50, gradientColor);
      g2d.setPaint(gradient);
      } else {
      g2d.setColor(fgColor);
    }
    
    // Set font - NO AUTO-SCALING AT ALL
    g2d.setFont(font);
    
    // Center text horizontally
    FontMetrics metrics = g2d.getFontMetrics();
    int textWidth = metrics.stringWidth(text);
    int textX = (width - textWidth) / 2;
    
    // Calculate text Y position based on whether image is present
    int textY;
    if (wordImage != null) {
      textY = height * 3 / 4;
      } else {
      textY = (height - metrics.getHeight()) / 2 + metrics.getAscent();
    }
    
    // NO AUTO-SCALING - draw text as is, even if it goes outside bounds
    g2d.drawString(text, textX, textY);
    g2d.dispose();
    
    return image;
  }
  
  /**
   * Sets the inverse transform matrix of the object
   * @param inverseTransform Matrix4 inverse transform
   */
  @Override
  public void setObjectTransform(Matrix4 inverseTransform) {
    if (inverseTransform != null) {
      this.objectInverseTransform = inverseTransform;
      } else {
      this.objectInverseTransform = new Matrix4();
    }
  }
  
  public void setTriangleEtc(boolean nbool) {
    this.isTriangleEtc = nbool;
  }
  
  /**
   * Returns the color at the given world point on the surface
   * Uses planar UV mapping on XY plane similar to TransparentPNGMaterial
   * @param point World space point on surface
   * @param normal Surface normal
   * @param light Light source
   * @param viewerPos Viewer position
   * @return Color with alpha channel
   */
  @Override
  public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPos) {
    if (texture == null) {
      setTransparency(1.0);
      return new Color(0, 0, 0, 0);
    }
    
    Point3 local = objectInverseTransform.transformPoint(point);
    
    double u = 0.0;
    double v = 0.0;
    
    if (isTriangleEtc) {
      u = local.x + 0.5;
      v = local.z + 0.5;
      } else {
      u = (local.x + 1.0) * 0.5;
      v = (1.0 - (local.y + 1.0) * 0.5);
    }
    
    // Apply UV scale and offset
    double scaledU = u / uScale + uOffset;
    double scaledV = v / vScale + vOffset;
    
    double finalU, finalV;
    
    if (isRepeatTexture) {
      finalU = scaledU - Math.floor(scaledU);
      finalV = scaledV - Math.floor(scaledV);
      } else {
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
      return new Color(red, green, blue, 255);
    }
    
    setTransparency(1.0);
    return new Color(0, 0, 0, 0);
  }
  
  /**
   * Returns reflectivity of the material
   * @return 0.0 (non-reflective)
   */
  @Override
  public double getReflectivity() {
    return 0.0;
  }
  
  /**
   * Returns index of refraction
   * @return 1.0 (no refraction)
   */
  @Override
  public double getIndexOfRefraction() {
    return 1.0;
  }
  
  /**
   * Returns transparency of the material
   * @return transparency value (0.0 for opaque, 1.0 for fully transparent)
   */
  @Override
  public double getTransparency() {
    return transparency;
  }
  
  private void setTransparency(double tnw) {
    this.transparency = tnw;
  }
  
  /**
   * Gets the horizontal texture offset
   * @return U offset value
   */
  public double getUOffset() {
    return uOffset;
  }
  
  /**
   * Sets the horizontal texture offset
   * @param uOffset U offset value
   */
  public void setUOffset(double uOffset) {
    this.uOffset = uOffset;
  }
  
  /**
   * Gets the vertical texture offset
   * @return V offset value
   */
  public double getVOffset() {
    return vOffset;
  }
  
  /**
   * Sets the vertical texture offset
   * @param vOffset V offset value
   */
  public void setVOffset(double vOffset) {
    this.vOffset = vOffset;
  }
  
  /**
   * Gets the horizontal texture scale factor
   * @return U scale factor
   */
  public double getUScale() {
    return uScale;
  }
  
  /**
   * Sets the horizontal texture scale factor
   * @param uScale U scale factor
   */
  public void setUScale(double uScale) {
    this.uScale = (uScale > 0.0) ? uScale : 1.0;
  }
  
  /**
   * Gets the vertical texture scale factor
   * @return V scale factor
   */
  public double getVScale() {
    return vScale;
  }
  
  /**
   * Sets the vertical texture scale factor
   * @param vScale V scale factor
   */
  public void setVScale(double vScale) {
    this.vScale = (vScale > 0.0) ? vScale : 1.0;
  }
  
  /**
   * Checks if texture repeating is enabled
   * @return true if texture repeating is enabled, false otherwise
   */
  public boolean isRepeatTexture() {
    return isRepeatTexture;
  }
  
  /**
   * Sets whether texture repeating is enabled
   * @param repeat true to enable repeating, false to disable
   */
  public void setRepeatTexture(boolean repeat) {
    this.isRepeatTexture = repeat;
  }
  
  /**
   * Gets the rendered text
   * @return The text displayed on this material
   */
  public String getText() {
    return text;
  }
  
  /**
   * Gets the foreground color
   * @return Text color
   */
  public Color getForegroundColor() {
    return foregroundColor;
  }
  
  /**
   * Gets the background color
   * @return Background color
   */
  public Color getBackgroundColor() {
    return backgroundColor;
  }
  
  /**
   * Gets the font used for rendering
   * @return The font
   */
  public Font getFont() {
    return font;
  }
  
  /**
   * Checks if gradient is enabled
   * @return true if gradient is enabled
   */
  public boolean isGradientEnabled() {
    return gradientEnabled;
  }
  
  /**
   * Gets the gradient end color
   * @return Gradient color
   */
  public Color getGradientColor() {
    return gradientColor;
  }
  
  /**
   * Gets the optional word image
   * @return The word image or null if not set
   */
  public BufferedImage getWordImage() {
    return wordImage;
  }
  
  /**
   * Gets the texture width
   * @return Texture width in pixels
   */
  public int getWidth() {
    return width;
  }
  
  /**
   * Sets the texture width and regenerates the texture
   * @param width New texture width
   */
  public void setWidth(int width) {
    this.width = width;
    regenerateTexture();
  }
  
  /**
   * Gets the texture height
   * @return Texture height in pixels
   */
  public int getHeight() {
    return height;
  }
  
  /**
   * Sets the texture height and regenerates the texture
   * @param height New texture height
   */
  public void setHeight(int height) {
    this.height = height;
    regenerateTexture();
  }
  
  /**
   * Sets both texture dimensions and regenerates the texture
   * @param width New texture width
   * @param height New texture height
   */
  public void setSize(int width, int height) {
    this.width = width;
    this.height = height;
    regenerateTexture();
  }
  
  /**
   * Regenerates the texture with new text
   * @param newText New text to render
   */
  public void setText(String newText) {
    this.text = newText;
    regenerateTexture();
  }
  
  /**
   * Regenerates the texture with new colors
   * @param newForeground New text color
   * @param newBackground New background color
   */
  public void setColors(Color newForeground, Color newBackground) {
    this.foregroundColor = newForeground;
    this.backgroundColor = newBackground;
    regenerateTexture();
  }
  
  /**
   * Regenerates the texture with new font
   * @param newFont New font to use
   */
  public void setFont(Font newFont) {
    this.font = newFont;
    regenerateTexture();
  }
  
  /**
   * Regenerates the texture with gradient settings
   * @param useGradient Whether to use gradient
   * @param newGradientColor Gradient end color
   */
  public void setGradient(boolean useGradient, Color newGradientColor) {
    this.gradientEnabled = useGradient;
    this.gradientColor = newGradientColor;
    regenerateTexture();
  }
  
  /**
   * Regenerates the texture with new word image
   * @param newWordImage New word image to display (null for text only)
   */
  public void setWordImage(BufferedImage newWordImage) {
    this.wordImage = newWordImage;
    regenerateTexture();
  }
  
  /**
   * Regenerates the texture with all current settings
   * Useful when multiple properties change and you want to update once
   */
  public void regenerateTexture() {
    this.texture = createTextImage(text, foregroundColor, backgroundColor, font,
    gradientEnabled, gradientColor, wordImage, width, height);
  }
  
}

/***
// Only text
Material ekmekMaterial = new WordMaterial("Ekmek");

// Text + image
BufferedImage breadImage = ImageIO.read(new File("bread.png"));
Material ekmekMaterial = new WordMaterial("Ekmek", Color.BLACK,
new Color(0x00000000, true), new Font("Arial", Font.BOLD, 36), breadImage);

// Gradient + image
Material brodMaterial = new WordMaterial("Brød", Color.BLUE,
new Color(0x00000000, true), new Font("Arial", Font.BOLD, 42),
Color.CYAN, breadImage);
 */

/***
// Seffaflik icin daima ARGB formatinda yazin:
new Color(0x00000000, true); // Tam seffaf
new Color(0x80ffffff, true); // %50 seffaf beyaz

// RGB formatinda yazacaksaniz hasAlpha=false kullanin:
new Color(0xffffff); // Opak beyaz
new Color(0x000000); // Opak siyah
 */

/***
Triangle t1 {
point1 = P(-0.5, 0.0, -0.5);
point2 = P(0.5, 0.0, -0.5);
point3 = P(0.0, 2.0, 0.0);
material = lambert;
}

Triangle t2 {
point1 = P(0.5, 0.0, -0.5);
point2 = P(0.5, 0.0, 0.5);
point3 = P(0.0, 2.0, 0.0);
material = lambert;
}

Triangle t3 {
point1 = P(0.5, 0.0, 0.5);
point2 = P(-0.5, 0.0, 0.5);
point3 = P(0.0, 2.0, 0.0);
material = wordMaterial
}

Triangle t4 {
point1 = P(-0.5, 0.0, 0.5);
point2 = P(-0.5, 0.0, -0.5);
point3 = P(0.0, 2.0, 0.0);
material = lambert;
}
 */
