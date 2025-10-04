package net.elena.murat.material;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Random;

import net.elena.murat.math.*;
import net.elena.murat.light.Light;
import net.elena.murat.util.ColorUtil;

/**
 * TextDielectricMaterial - Combines text rendering capability with dielectric material properties
 * Creates transparent glass-like text on a sphere with refraction and reflection effects
 */
public class TextDielectricMaterial implements Material {
  
  // Text properties (from SphereWordTextureMaterial)
  private final String word;
  private final Color textColor;
  private final Color gradientColor;
  private final String gradientType;
  private final Color bgColor;
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
  
  // Dielectric properties (from DielectricMaterial)
  private Color diffuseColor;
  private double indexOfRefraction;
  private double transparency;
  private double reflectivity;
  private Color filterColorInside;
  private Color filterColorOutside;
  private Matrix4 objectTransform;
  private final Random random;
  private double currentReflectivity;
  private double currentTransparency;
  
  /**
   * Constructor with text and dielectric properties
   */
  public TextDielectricMaterial(String word, Color textColor, Color gradientColor,
    String gradientType, Color bgColor,
    String fontFamily, int fontStyle, int fontSize,
    int uOffset, int vOffset,
    BufferedImage imageObject, int imageWidth, int imageHeight,
    int imageUOffset, int imageVOffset,
    Color diffuseColor, double ior, double transparency, double reflectivity,
    Color filterColorInside, Color filterColorOutside) {
    
    // Text properties
    this.word = convertToNorwegianText(word).replaceAll("_", " ");
    this.textColor = textColor;
    this.gradientColor = gradientColor;
    this.gradientType = gradientType != null ? gradientType : "horizontal";
    this.bgColor = bgColor;
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
    
    // Dielectric properties
    this.diffuseColor = diffuseColor;
    this.indexOfRefraction = ior;
    this.transparency = transparency;
    this.reflectivity = reflectivity;
    this.filterColorInside = filterColorInside;
    this.filterColorOutside = filterColorOutside;
    
    this.random = new Random();
    this.currentReflectivity = reflectivity;
    this.currentTransparency = transparency;
    this.objectTransform = new Matrix4().identity();
    
    this.texture = createTexture();
  }
  
  /**
   * Simplified constructor with default dielectric properties
   */
  public TextDielectricMaterial(String word, Color textColor,
    String fontFamily, int fontStyle, int fontSize) {
    this(word, textColor, null, "horizontal", new Color(0x00000000),
      fontFamily, fontStyle, fontSize, 0, 0,
      null, 0, 0, 0, 0,
      new Color(0.9f, 0.9f, 0.9f), 1.5, 0.8, 0.1,
    new Color(1.0f, 1.0f, 1.0f), new Color(1.0f, 1.0f, 1.0f));
  }
  
  /**
   * Creates the texture image with the word drawn centered, optionally with a gradient and background image.
   * The texture size is fixed at 1024x1024 pixels.
   *
   * @return BufferedImage containing the rendered word texture.
   */
  private BufferedImage createTexture() {
    final int size = 1024;
    BufferedImage texture = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2d = texture.createGraphics();
    
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    
    g2d.setBackground(new Color(0, 0, 0, 0));
    g2d.clearRect(0, 0, size, size);
    
    if (imageObject != null) {
      int imgX = ((size - imageWidth) / 2) + imageUOffset;
      int imgY = ((size - imageHeight) / 2) + imageVOffset;
      g2d.drawImage(imageObject, imgX, imgY, imageWidth, imageHeight, null);
    }
    
    Font font;
    try {
      font = new Font(fontFamily, fontStyle, fontSize);
      } catch (Exception e) {
      font = new Font("Arial", fontStyle, fontSize); // Fallback font
    }
    g2d.setFont(font);
    
    FontMetrics fm = g2d.getFontMetrics();
    int textWidth = fm.stringWidth(word);
    int textHeight = fm.getHeight();
    int ascent = fm.getAscent();
    
    int x = ((size - textWidth) / 2) + uOffset;
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
  
  // Dielectric material methods
  @Override
  public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPoint) {
    // Get texture color first
    Point3 localPoint = objectTransform.inverse().transformPoint(point);
    Color textureColor = getTextureColor(localPoint, normal);
    
    // Apply dielectric lighting effects
    Vector3 lightDir = light.getDirectionTo(point).normalize();
    double diffuseFactor = Math.max(0.3, normal.dot(lightDir));
    
    // Fresnel effect for dynamic properties
    Vector3 viewDir = viewerPoint.subtract(point).normalize();
    double fresnel = Vector3.calculateFresnel(viewDir, normal, 1.0, indexOfRefraction);
    
    this.currentReflectivity = Math.min(0.95, reflectivity + (fresnel * 0.8));
    this.currentTransparency = Math.max(0.05, transparency * (1.0 - fresnel * 0.2));
    
    // Diffuse and specular components
    //Original is below line with textureColor:
    Color diffuse = ColorUtil.multiplyColor(textureColor, diffuseFactor * light.getIntensity());
    
    Vector3 reflectDir = lightDir.reflect(normal);
    double specularFactor = Math.pow(Math.max(0, viewDir.dot(reflectDir)), 128);
    Color specular = ColorUtil.multiplyColor(light.getColor(), specularFactor * 1.2 * light.getIntensity());
    
    // Combine with glass tint
    Color result = ColorUtil.add(diffuse, specular);
    //if (transparency > 0.3) {
    //Color glassTint = new Color(0.95f, 0.97f, 1.0f);
    //result = ColorUtil.multiplyColors(result, glassTint);
    //}
    
    return ColorUtil.clampColor(result);
  }
  
  private Color getTextureColor(Point3 localPoint, Vector3 worldNormal) {
    if (texture == null) return textColor;
    
    // Normal normalize
    Vector3 dir = worldNormal.normalize();
    
    double phi = Math.atan2(dir.z, dir.x);
    double theta = Math.asin(dir.y);
    
    double u = 1.0 - (phi + Math.PI) / (2 * Math.PI);
    double v = (theta + Math.PI / 2) / Math.PI;
    v = 1.0 - v;
    
    u = (u + 0.25) % 1.0; // Offset
    
    int texX = (int) (u * texture.getWidth());
    texX = texX % texture.getWidth();
    if (texX < 0) texX += texture.getWidth();
    
    int texY = (int) (v * texture.getHeight());
    if (texY < 0 || texY >= texture.getHeight()) {
      return new Color(0, 0, 0, 0);
    }
    return new Color(texture.getRGB(texX, texY), true);
  }
  
  // Material interface methods
  @Override
  public void setObjectTransform(Matrix4 tm) {
    this.objectTransform = (tm != null) ? tm : new Matrix4();
  }
  
  @Override
  public double getIndexOfRefraction() {
    return indexOfRefraction;
  }
  
  @Override
  public double getTransparency() {
    return currentTransparency;
  }
  
  @Override
  public double getReflectivity() {
    return currentReflectivity;
  }
  
  // Getters and setters for dielectric properties
  public Color getFilterColorInside() { return filterColorInside; }
  public Color getFilterColorOutside() { return filterColorOutside; }
  
  public void setFilterColorInside(Color filterInside) {
    this.filterColorInside = filterInside;
  }
  
  public void setFilterColorOutside(Color filterOutside) {
    this.filterColorOutside = filterOutside;
  }
  
  public Color getDiffuseColor() { return diffuseColor; }
  public void setDiffuseColor(Color color) { this.diffuseColor = color; }
  
  public void setIndexOfRefraction(double ior) { this.indexOfRefraction = ior; }
  public void setTransparency(double transparency) { this.transparency = transparency; }
  public void setReflectivity(double reflectivity) { this.reflectivity = reflectivity; }
  
  @Override
  public String toString() {
    return String.format("TextDielectricMaterial[text='%s', ior=%.2f, transparency=%.2f, reflectivity=%.2f]",
    word, indexOfRefraction, transparency, reflectivity);
  }
  
}
