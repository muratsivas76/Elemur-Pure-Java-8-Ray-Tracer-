package net.elena.murat.material;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Random;

import net.elena.murat.math.*;
import net.elena.murat.light.*;
import net.elena.murat.util.ColorUtil;

/**
 * AnodizedTextMaterial - Combines anodized metal material with text/image texture support.
 * Renders text or images on an iridescent, metallic anodized surface.
 */
public class AnodizedTextMaterial implements Material {
  
  // --- TEXTURE PROPERTIES (from TextDielectricMaterial) ---
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
  
  // --- ANODIZED METAL PROPERTIES (from AnodizedMetalMaterial) ---
  private final Color baseColor;
  private Matrix4 objectTransform;
  
  // Phong parameters optimized for metallic surface
  private final double ambientCoeff = 0.3;
  private final double diffuseCoeff = 0.2;  // Weak diffuse for metallic
  private final double specularCoeff = 1.0; // Strong specular
  private final double shininess = 100.0;
  private final Color specularColor = Color.WHITE;
  private final double reflectivity = 0.8;
  private final double ior = 2.4;
  private final double transparency = 0.0;
  
  /**
   * Full constructor with all text and anodized properties
   */
  public AnodizedTextMaterial(String word, Color textColor, Color gradientColor,
    String gradientType, Color bgColor,
    String fontFamily, int fontStyle, int fontSize,
    int uOffset, int vOffset,
    BufferedImage imageObject, int imageWidth, int imageHeight,
    int imageUOffset, int imageVOffset,
    Color baseColor) {
    
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
    
    // Anodized properties
    this.baseColor = baseColor != null ? baseColor : new Color(50, 50, 200);
    this.objectTransform = Matrix4.identity();
    
    // Generate texture
    this.texture = createTexture();
  }
  
  /**
   * Simplified constructor with defaults
   */
  public AnodizedTextMaterial(String word, Color textColor, String fontFamily, int fontStyle, int fontSize) {
    this(word, textColor, null, "horizontal", new Color(0, 0, 0, 0),
      fontFamily, fontStyle, fontSize, 0, 0,
      null, 0, 0, 0, 0,
    new Color(50, 50, 200));
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
      g2d.drawImage(imageObject, imgX, imgY, imageWidth, imageHeight, null);
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
  
  // --- ANODIZED RENDERING WITH TEXTURE ---
  @Override
  public Color getColorAt(Point3 worldPoint, Vector3 worldNormal, Light light, Point3 viewerPos) {
    // 1. Get texture color at this point
    Point3 localPoint = objectTransform.inverse().transformPoint(worldPoint);
    Color textureColor = getTextureColor(localPoint, worldNormal);
    
    // 2. If texture has alpha=0, use base anodized color only
    if (textureColor.getAlpha() == 0) {
      textureColor = baseColor;
    }
    
    // 3. Apply iridescence effect to the combined color
    Color surfaceColor = calculateIridescentColor(worldPoint, worldNormal, viewerPos, textureColor);
    
    // 4. Handle lighting (same as AnodizedMetalMaterial)
    LightProperties props = LightProperties.getLightProperties(light, worldPoint);
    if (props == null) return surfaceColor;
    
    Color ambient = ColorUtil.multiplyColors(surfaceColor, props.color, ambientCoeff);
    
    if (light instanceof ElenaMuratAmbientLight) {
      return ambient;
    }
    
    double NdotL = Math.max(0, worldNormal.dot(props.direction));
    Color diffuse = ColorUtil.multiplyColors(surfaceColor, props.color, diffuseCoeff * NdotL * props.intensity);
    
    Vector3 viewDir = viewerPos.subtract(worldPoint).normalize();
    Vector3 reflectDir = props.direction.negate().reflect(worldNormal);
    double RdotV = Math.max(0, reflectDir.dot(viewDir));
    double specFactor = Math.pow(RdotV, shininess) * props.intensity;
    Color specular = ColorUtil.multiplyColors(specularColor, props.color, specularCoeff * specFactor);
    
    return ColorUtil.combineColors(ambient, diffuse, specular);
  }
  
  /**
   * Modified iridescence that blends baseColor with textureColor based on view angle
   */
  private Color calculateIridescentColor(Point3 worldPoint, Vector3 normal, Point3 viewerPos, Color textureColor) {
    Vector3 viewDir = viewerPos.subtract(worldPoint).normalize();
    double viewAngle = Math.abs(viewDir.dot(normal));
    
    // Extract RGB from texture
    int r = textureColor.getRed();
    int g = textureColor.getGreen();
    int b = textureColor.getBlue();
    
    // Blend with iridescent shift based on view angle
    if (viewAngle < 0.3) {
      // Narrow angle - blue shift
      r = (int)(r * 0.7);
      g = (int)(g * 0.8);
      b = (int)(b * 1.2);
      } else if (viewAngle < 0.6) {
      // Medium angle - purple shift
      r = (int)(r * 1.1);
      g = (int)(g * 0.7);
      b = (int)(b * 1.0);
      } else {
      // Wide angle - pink/gold shift
      r = (int)(r * 1.3);
      g = (int)(g * 0.9);
      b = (int)(b * 0.8);
    }
    
    return new Color(
      Math.min(255, Math.max(0, r)),
      Math.min(255, Math.max(0, g)),
      Math.min(255, Math.max(0, b)),
      textureColor.getAlpha() // Preserve alpha
    );
  }
  
  private Color getTextureColor(Point3 localPoint, Vector3 worldNormal) {
    if (texture == null) return new Color(0, 0, 0, 0);
    
    Vector3 dir = worldNormal.normalize();
    double phi = Math.atan2(dir.z, dir.x);
    double theta = Math.asin(dir.y);
    
    double u = 1.0 - (phi + Math.PI) / (2 * Math.PI);
    double v = (theta + Math.PI / 2) / Math.PI;
    v = 1.0 - v;
    
    u = (u + 0.25) % 1.0; // Offset for better alignment
    
    int texX = (int) (u * texture.getWidth());
    texX = texX % texture.getWidth();
    if (texX < 0) texX += texture.getWidth();
    
    int texY = (int) (v * texture.getHeight());
    if (texY < 0 || texY >= texture.getHeight()) {
      return new Color(0, 0, 0, 0);
    }
    
    return new Color(texture.getRGB(texX, texY), true);
  }
  
  // --- MATERIAL INTERFACE ---
  @Override
  public void setObjectTransform(Matrix4 tm) {
    this.objectTransform = (tm != null) ? tm : new Matrix4();
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
  
  // --- GETTERS ---
  public Color getBaseColor() { return baseColor; }
  public BufferedImage getTexture() { return texture; }
  
  @Override
  public String toString() {
    return String.format("AnodizedTextMaterial[text='%s', baseColor=%s]", word, baseColor);
  }
  
}
