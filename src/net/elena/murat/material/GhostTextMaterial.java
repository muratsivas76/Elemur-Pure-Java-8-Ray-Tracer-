package net.elena.murat.material;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

import net.elena.murat.math.*;
import net.elena.murat.light.Light;
import net.elena.murat.util.ColorUtil;

/**
 * GhostTextMaterial - Transparent ghost material with text and image texture support
 * Maintains original constructor signatures while adding dielectric properties
 */
public class GhostTextMaterial implements Material {
  
  // Original texture properties (DO NOT CHANGE NAMES)
  private final String word;
  private final Color textColor;
  private final Color gradientColor;
  private final String gradientType;
  private final String fontFamily;
  private final int fontStyle;
  private final int fontSize;
  private final int uOffset;
  private final int vOffset;
  private final BufferedImage imageObject;
  private final int imageWidth;
  private final int imageHeight;
  private final int imageUOffset;
  private final int imageVOffset;
  private BufferedImage texture;
  
  // New transparent material properties
  //private final double baseTransparency;
  private double transparency;
  private final double reflectivity;
  private final double indexOfRefraction;
  private final Color surfaceColor;
  
  private Matrix4 objectTransform;
  private final Random random;
  
  /**
   * ORIGINAL CONSTRUCTOR - 15 parameters
   */
  public GhostTextMaterial(String word, Color textColor, Color gradientColor,
    String gradientType,
    String fontFamily, int fontStyle, int fontSize,
    int uOffset, int vOffset,
    BufferedImage imageObject, int imageWidth, int imageHeight,
    int imageUOffset, int imageVOffset) {
    
    // Texture properties
    this.word = convertToNorwegianText(word).replaceAll("_", " ");
    this.textColor = textColor;
    this.gradientColor = gradientColor;
    this.gradientType = gradientType != null ? gradientType : "horizontal";
    this.fontFamily = fontFamily.replaceAll("_", " ");
    this.fontStyle = fontStyle;
    this.fontSize = fontSize;
    this.uOffset = uOffset;
    this.vOffset = vOffset;
    this.imageObject = imageObject;
    this.imageWidth = imageWidth;
    this.imageHeight = imageHeight;
    this.imageUOffset = imageUOffset;
    this.imageVOffset = imageVOffset;
    
    // Default transparent properties
    this.transparency = 0.8;
    this.reflectivity = 0.1;
    this.indexOfRefraction = 1.5;
    this.surfaceColor = new Color(0, 0, 0, (int)(this.transparency * 255));
    
    //this.baseTransparency = this.transparency;
    
    this.random = new Random();
    this.objectTransform = new Matrix4().identity();
    this.texture = createTexture();
  }
  
  /**
   * NEW CONSTRUCTOR - 18 parameters (with transparency properties)
   */
  public GhostTextMaterial(String word, Color textColor, Color gradientColor,
    String gradientType,
    String fontFamily, int fontStyle, int fontSize,
    int uOffset, int vOffset,
    BufferedImage imageObject, int imageWidth, int imageHeight,
    int imageUOffset, int imageVOffset,
    double transparency, double reflectivity, double ior) {
    
    // Texture properties
    this.word = convertToNorwegianText(word).replaceAll("_", " ");
    this.textColor = textColor;
    this.gradientColor = gradientColor;
    this.gradientType = gradientType != null ? gradientType : "horizontal";
    this.fontFamily = fontFamily.replaceAll("_", " ");
    this.fontStyle = fontStyle;
    this.fontSize = fontSize;
    this.uOffset = uOffset;
    this.vOffset = vOffset;
    this.imageObject = imageObject;
    this.imageWidth = imageWidth;
    this.imageHeight = imageHeight;
    this.imageUOffset = imageUOffset;
    this.imageVOffset = imageVOffset;
    
    // Transparent properties
    this.transparency = Math.max(0, Math.min(1, transparency));
    this.reflectivity = Math.max(0, Math.min(1, reflectivity));
    this.indexOfRefraction = Math.max(1.0, ior);
    this.surfaceColor = new Color(0, 0, 0, (int)(this.transparency * 255));
    
    //this.baseTransparency = this.transparency;
    
    this.random = new Random();
    this.objectTransform = new Matrix4().identity();
    this.texture = createTexture();
  }
  
  /**
   * SIMPLIFIED CONSTRUCTOR - 5 parameters
   */
  public GhostTextMaterial(String word, Color textColor,
    String fontFamily, int fontStyle, int fontSize) {
    this(word, textColor, null, "horizontal",
      fontFamily, fontStyle, fontSize, 0, 0,
    null, 0, 0, 0, 0);
  }
  
  /**
   * Creates the texture (ORIGINAL METHOD - DO NOT CHANGE)
   */
  private BufferedImage createTexture() {
    final int size = 1024;
    BufferedImage texture = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2d = texture.createGraphics();
    
    g2d.setComposite(AlphaComposite.Clear);
    g2d.clearRect(0, 0, size, size);
    g2d.setComposite(AlphaComposite.SrcOver);
    
    // Full transparent bg guarantized for eviting black circle
    // alrededor sphere
    //g2d.setBackground(new Color(0, 0, 0, 0));
    //g2d.clearRect(0, 0, size, size);
    
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    
    if (imageObject != null) {
      int imgX = ((size - imageWidth) / 2) + imageUOffset - (imageWidth * 2);
      int imgY = ((size - imageHeight) / 2) + imageVOffset;
      g2d.drawImage(imageObject, imgX, imgY, imageWidth, imageHeight, null);
    }
    
    Font font;
    try {
      font = new Font(fontFamily, fontStyle, fontSize);
      } catch (Exception e) {
      font = new Font("Arial", fontStyle, fontSize);
    }
    g2d.setFont(font);
    
    FontMetrics fm = g2d.getFontMetrics();
    int textWidth = fm.stringWidth(word);
    int textHeight = fm.getHeight();
    int ascent = fm.getAscent();
    
    int x = ((size - textWidth) / 2) + uOffset - (textWidth * 2);
    int y = ((size - textHeight) / 2) + (ascent * 2) + (textHeight / 3) + vOffset;
    
    if (gradientColor != null) {
      GradientPaint gradient = createGradient(x, y - ascent, textWidth, textHeight);
      g2d.setPaint(gradient);
      } else {
      g2d.setColor(textColor);
    }
    
    g2d.drawString(word, x, y);
    g2d.dispose();
    
    return texture;
  }
  
  private GradientPaint createGradient(float x, float y, float width, float height) {
    switch (gradientType.toLowerCase()) {
      case "vertical":
        return new GradientPaint(x, y, textColor, x, y + height/2, gradientColor, true);
      case "diagonal":
        return new GradientPaint(x, y, textColor, x + width/3, y + height/5, gradientColor, true);
      case "horizontal":
      default:
        return new GradientPaint(x, y, textColor, x + width/3, y, gradientColor, true);
    }
  }
  
  public static String convertToNorwegianText(String input) {
    if (input == null || input.isEmpty()) {
      return input;
    }
    
    String result = input;
    result = result.replace("AE", "\u00C6");
    result = result.replace("O/", "\u00D8");
    result = result.replace("A0", "\u00C5");
    result = result.replace("ae", "\u00E6");
    result = result.replace("o/", "\u00F8");
    result = result.replace("a0", "\u00E5");
    
    return result;
  }
  
  /**
   * NEW: getColorAt method with proper alpha blending for transparent material
   */
  @Override
  public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPoint) {
    Color backgroundColor = calculateBackgroundColor(point, normal, light, viewerPoint);
    
    Point3 localPoint = objectTransform.inverse().transformPoint(point);
    Color textureColor = getTextureColor(localPoint, normal);
    
    if (textureColor.getAlpha() == 0) {
      //this.transparency = this.transparency * 1.2; // %20
      return backgroundColor;
    }
    
    float textureAlpha = textureColor.getAlpha() / 255.0f;
    
    float r = (textureColor.getRed() / 255.0f * textureAlpha) +
    (backgroundColor.getRed() / 255.0f * (1 - textureAlpha));
    float g = (textureColor.getGreen() / 255.0f * textureAlpha) +
    (backgroundColor.getGreen() / 255.0f * (1 - textureAlpha));
    float b = (textureColor.getBlue() / 255.0f * textureAlpha) +
    (backgroundColor.getBlue() / 255.0f * (1 - textureAlpha));
    
    // Alpha: background transparency + texture visibility
    float a = Math.max(backgroundColor.getAlpha() / 255.0f, textureAlpha);
    
    //this.transparency = baseTransparency;
    
    return new Color(r, g, b, a);
  }
  
  private Color calculateBackgroundColor(Point3 point, Vector3 normal, Light light, Point3 viewerPoint) {
    int a = (int)(transparency * 255.0);
    a = ColorUtil.clampColorValue(a);
    return new Color(0, 0, 0, a);
  }
  
  private Color getTextureColor(Point3 localPoint, Vector3 worldNormal) {
    if (texture == null) return new Color(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), 255);
    
    Vector3 dir = worldNormal.normalize();
    
    double u = 0.5 + Math.atan2(dir.z, dir.x) / (2 * Math.PI);
    double v = 0.5 - Math.asin(dir.y) / Math.PI;
    
    u = (u % 1.0 + 1.0) % 1.0;
    v = (v % 1.0 + 1.0) % 1.0;
    
    // Kenarları atla - merkeze yakın pikselleri kullan
    double edgeMargin = 0.05;
    if (u < edgeMargin || u > 1.0 - edgeMargin || v < edgeMargin || v > 1.0 - edgeMargin) {
      return new Color(0, 0, 0, 0); // Kenarlar şeffaf
    }
    
    int texX = (int) (u * texture.getWidth());
    int texY = (int) (v * texture.getHeight());
    
    texX = Math.max(0, Math.min(texture.getWidth() - 1, texX));
    texY = Math.max(0, Math.min(texture.getHeight() - 1, texY));
    
    int rgb = texture.getRGB(texX, texY);
    return new Color(rgb, true);
  }
  
  // Material interface methods
  @Override
  public void setObjectTransform(Matrix4 tm) {
    this.objectTransform = (tm != null) ? tm : new Matrix4();
  }
  
  @Override
  public double getTransparency() {
    return transparency;
  }
  
  @Override
  public double getReflectivity() {
    return reflectivity;
  }
  
  @Override
  public double getIndexOfRefraction() {
    return indexOfRefraction;
  }
  
  // Getters for transparent properties
  public Color getSurfaceColor() {
    return surfaceColor;
  }
  
  @Override
  public String toString() {
    return String.format("GhostTextMaterial[text='%s', trans=%.2f, refl=%.2f, ior=%.2f]",
    word, transparency, reflectivity, indexOfRefraction);
  }
  
}
