package net.elena.murat.material;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Random;

import net.elena.murat.math.*;
import net.elena.murat.light.Light;
import net.elena.murat.util.ColorUtil;

/**
 * HybridTextMaterial — Combines dielectric material properties with text/image rendering.
 * Supports reflection, refraction, Fresnel effect, and textured text on curved surfaces.
 * Fully compatible with scene.txt loading and RayTracer integration.
 */
public class HybridTextMaterial implements Material {
  private final Color glassTint = new Color(0.95f, 0.97f, 1.0f, 1.0f);
  
  // --- TEXTURE PROPERTIES ---
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
  
  // --- DIELECTRIC PROPERTIES ---
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
  
  // --- PHONG LIGHTING PROPERTIES ---
  private Color specularColor;
  private double shininess;
  private double ambientCoefficient;
  private double diffuseCoefficient;
  private double specularCoefficient;
  
  /**
   * Full constructor — supports all text, dielectric, and lighting properties.
   */
  public HybridTextMaterial(String word, Color textColor, Color gradientColor,
    String gradientType, Color bgColor,
    String fontFamily, int fontStyle, int fontSize,
    int uOffset, int vOffset,
    BufferedImage imageObject, int imageWidth, int imageHeight,
    int imageUOffset, int imageVOffset,
    Color diffuseColor, double ior, double transparency, double reflectivity,
    Color filterColorInside, Color filterColorOutside,
    Color specularColor, double shininess,
    double ambientCoefficient, double diffuseCoefficient, double specularCoefficient) {
    
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
    
    // Phong lighting properties
    this.specularColor = specularColor;
    this.shininess = shininess;
    this.ambientCoefficient = ambientCoefficient;
    this.diffuseCoefficient = diffuseCoefficient;
    this.specularCoefficient = specularCoefficient;
    
    // Internal
    this.random = new Random();
    this.currentReflectivity = reflectivity;
    this.currentTransparency = transparency;
    this.objectTransform = new Matrix4().identity();
    
    // Generate texture with improved visibility
    this.texture = createTexture();
  }
  
  /**
   * Simplified constructor with defaults — ideal for scene.txt
   */
  public HybridTextMaterial(String word, Color textColor, String fontFamily, int fontStyle, int fontSize) {
    this(word, textColor, null, "horizontal", new Color(0x00000000),
      fontFamily, fontStyle, fontSize, 0, 0,
      null, 0, 0, 0, 0,
      new Color(0.9f, 0.9f, 0.9f), 1.5, 0.8, 0.1,
      new Color(1.0f, 1.0f, 1.0f), new Color(1.0f, 1.0f, 1.0f),
    Color.WHITE, 32.0, 0.1, 0.7, 0.7);
  }
  
  /**
   * Creates the texture image with the word drawn centered, optionally with a gradient and background image.
   * The texture size is fixed at 1024x1024 pixels.
   */
  private BufferedImage createTexture() {
    final int size = 1024;
    BufferedImage texture = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2d = texture.createGraphics();
    
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    
    g2d.setBackground(new Color(0, 0, 0, 0));
    g2d.clearRect(0, 0, size, size);
    
    // Optional background image
    if (imageObject != null) {
      int imgX = ((size - imageWidth) / 2) + imageUOffset;
      int imgY = ((size - imageHeight) / 2) + imageVOffset;
      
      AlphaComposite alphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.95f);
      g2d.setComposite(alphaComposite);
      g2d.drawImage(imageObject, imgX, imgY, imageWidth, imageHeight, null);
      g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER)); // Reset to default
    }
    
    // Font setup
    Font font;
    try {
      font = new Font(fontFamily, fontStyle, fontSize);
      } catch (Exception e) {
      font = new Font("Arial", fontStyle, fontSize); // Fallback
    }
    g2d.setFont(font);
    
    FontMetrics fm = g2d.getFontMetrics();
    int textWidth = fm.stringWidth(word);
    int textHeight = fm.getHeight();
    int ascent = fm.getAscent();
    
    int x = ((size - textWidth) / 2) + uOffset;
    int y = ((size - textHeight) / 2) + (ascent * 2) + (textHeight / 3) + vOffset;
    
    AlphaComposite textComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.98f);
    g2d.setComposite(textComposite);
    
    // Apply gradient or solid color
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
        return new GradientPaint(x, y, textColor, x, y + height / 2, gradientColor, true);
      case "diagonal":
        return new GradientPaint(x, y, textColor, x + width / 3, y + height / 5, gradientColor, true);
      case "horizontal":
      default:
        return new GradientPaint(x, y, textColor, x + width / 3, y, gradientColor, true);
    }
  }
  
  public static String convertToNorwegianText(String input) {
    if (input == null || input.isEmpty()) return input;
    String result = input;
    result = result.replace("AE", "\u00C6");
    result = result.replace("O/", "\u00D8");
    result = result.replace("A0", "\u00C5");
    result = result.replace("ae", "\u00E6");
    result = result.replace("o/", "\u00F8");
    result = result.replace("a0", "\u00E5");
    return result;
  }
  
  // --- MATERIAL INTERFACE: CORE LIGHTING + TEXTURE + DIELECTRIC ---
  @Override
  public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPoint) {
    // 1. Get texture color at this point
    Point3 localPoint = objectTransform.inverse().transformPoint(point);
    Color textureColor = getTextureColor(localPoint, normal);
    
    // 2. Check if this point is textured (text or image area)
    boolean isTexturedArea = textureColor.getAlpha() > 50;
    
    // 3. Fresnel effect for dynamic properties
    Vector3 viewDir = viewerPoint.subtract(point).normalize();
    double fresnel = Vector3.calculateFresnel(viewDir, normal, 1.0, indexOfRefraction);
    
    this.currentReflectivity = Math.min(0.95, reflectivity + (fresnel * 0.4));
    this.currentTransparency = Math.max(0.05, transparency * (1.0 - fresnel * 0.3));
    
    // 4. Different treatment for textured vs non-textured areas
    if (isTexturedArea) {
      // TEXTURED AREAS - Bright and vibrant with contrast boost
      Color brightColor = ColorUtil.enhanceBrightnessAndContrast(textureColor, 1.3f, 1.2f);
      
      // Ambient component - normal
      Color ambient = ColorUtil.multiplyColor(brightColor, ambientCoefficient * light.getIntensity());
      
      // Diffuse component - normal
      Vector3 lightDir = light.getDirectionTo(point).normalize();
      double NdotL = Math.max(0.0, normal.dot(lightDir));
      Color diffuse = ColorUtil.multiplyColor(brightColor, diffuseCoefficient * NdotL * light.getIntensity());
      
      // Specular component - normal
      Vector3 reflectDir = lightDir.reflect(normal);
      double RdotV = Math.max(0.0, reflectDir.dot(viewDir));
      double specFactor = Math.pow(RdotV, shininess);
      Color specular = ColorUtil.multiplyColor(specularColor, specularCoefficient * specFactor * light.getIntensity());
      
      // Combine components - prioritize texture
      Color result = ColorUtil.add(ambient, ColorUtil.add(diffuse, specular));
      return ColorUtil.clampColor(result);
    }
    else {
      // GLASS AREAS - Normal lighting but ensure brightness
      Color baseColor = diffuseColor;
      
      Vector3 lightDir = light.getDirectionTo(point).normalize();
      double diffuseFactor = Math.max(0.4, normal.dot(lightDir));
      
      Color diffuse = ColorUtil.multiplyColor(baseColor, diffuseFactor * light.getIntensity());
      
      // Normal specular for glass
      Vector3 reflectDir = lightDir.reflect(normal);
      double specularFactor = Math.pow(Math.max(0, viewDir.dot(reflectDir)), 40);
      Color specular = ColorUtil.multiplyColor(specularColor, specularFactor * 0.4 * light.getIntensity());
      
      // Combine with light glass tint (not too strong)
      Color result = ColorUtil.add(diffuse, specular);
      Color glassTint = new Color(0.98f, 0.99f, 1.0f); // Very subtle tint
      result = ColorUtil.multiplyColors(result, glassTint);
      
      return ColorUtil.clampColor(result);
    }
  }
  
  private Color getTextureColor(Point3 localPoint, Vector3 worldNormal) {
    if (texture == null) return textColor;
    
    Vector3 dir = worldNormal.normalize();
    double phi = Math.atan2(dir.z, dir.x);
    double theta = Math.asin(dir.y);
    
    double u = 1.0 - (phi + Math.PI) / (2 * Math.PI);
    double v = (theta + Math.PI / 2) / Math.PI;
    v = 1.0 - v;
    
    u = (u + 0.25) % 1.0; // Offset for alignment
    
    int texX = (int) (u * texture.getWidth());
    texX = texX % texture.getWidth();
    if (texX < 0) texX += texture.getWidth();
    
    int texY = (int) (v * texture.getHeight());
    if (texY < 0 || texY >= texture.getHeight()) {
      return new Color(0, 0, 0, 0);
    }
    
    return new Color(texture.getRGB(texX, texY), true);
  }
  
  // --- MATERIAL INTERFACE METHODS ---
  @Override
  public void setObjectTransform(Matrix4 tm) {
    this.objectTransform = (tm != null) ? tm : new Matrix4().identity();
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
  
  // --- GETTERS & SETTERS ---
  public Color getDiffuseColor() { return diffuseColor; }
  public void setDiffuseColor(Color color) { this.diffuseColor = color; }
  
  public Color getSpecularColor() { return specularColor; }
  public void setSpecularColor(Color color) { this.specularColor = color; }
  
  public double getShininess() { return shininess; }
  public void setShininess(double shininess) { this.shininess = shininess; }
  
  public Color getFilterColorInside() { return filterColorInside; }
  public void setFilterColorInside(Color color) { this.filterColorInside = color; }
  
  public Color getFilterColorOutside() { return filterColorOutside; }
  public void setFilterColorOutside(Color color) { this.filterColorOutside = color; }
  
  public void setIndexOfRefraction(double ior) { this.indexOfRefraction = ior; }
  public void setTransparency(double transparency) { this.transparency = transparency; }
  public void setReflectivity(double reflectivity) { this.reflectivity = reflectivity; }
  
  public BufferedImage getTexture() { return texture; }
  
  @Override
  public String toString() {
    return String.format(
      "HybridTextMaterial[word='%s', ior=%.2f, trans=%.2f, refl=%.2f, shininess=%.1f]",
      word, indexOfRefraction, transparency, reflectivity, shininess
    );
  }
  
}
