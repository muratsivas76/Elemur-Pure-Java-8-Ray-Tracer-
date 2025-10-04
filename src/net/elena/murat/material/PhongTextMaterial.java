package net.elena.murat.material;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

import net.elena.murat.math.*;
import net.elena.murat.light.*;
import net.elena.murat.util.ColorUtil;

/**
 * PhongTextMaterial - Combines text/image rendering capability with Phong lighting model.
 * Renders text or images on a surface with ambient, diffuse, and specular lighting.
 */
public class PhongTextMaterial implements Material {
  
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
  
  // --- PHONG MATERIAL PROPERTIES ---
  private final Color diffuseColor;   // Base color (can be overridden by texture)
  private final Color specularColor;
  private final double shininess;
  private final double ambientCoefficient;
  private final double diffuseCoefficient;
  private final double specularCoefficient;
  private final double reflectivity;
  private final double ior;
  private final double transparency;
  private Matrix4 objectTransform;
  
  /**
   * Full constructor with all text and Phong properties
   */
  public PhongTextMaterial(String word, Color textColor, Color gradientColor,
    String gradientType, Color bgColor,
    String fontFamily, int fontStyle, int fontSize,
    int uOffset, int vOffset,
    BufferedImage imageObject, int imageWidth, int imageHeight,
    int imageUOffset, int imageVOffset,
    Color diffuseColor, Color specularColor, double shininess,
    double ambientCoefficient, double diffuseCoefficient, double specularCoefficient,
    double reflectivity, double ior, double transparency) {
    
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
    
    // Phong properties
    this.diffuseColor = diffuseColor;
    this.specularColor = specularColor;
    this.shininess = shininess;
    this.ambientCoefficient = ambientCoefficient;
    this.diffuseCoefficient = diffuseCoefficient;
    this.specularCoefficient = specularCoefficient;
    this.reflectivity = clamp01(reflectivity);
    this.ior = Math.max(1.0, ior);
    this.transparency = clamp01(transparency);
    this.objectTransform = Matrix4.identity();
    
    // Generate texture
    this.texture = createTexture();
  }
  
  /**
   * Simplified constructor with defaults
   */
  public PhongTextMaterial(String word, Color textColor, String fontFamily, int fontStyle, int fontSize) {
    this(word, textColor, null, "horizontal", new Color(0x00000000),
      fontFamily, fontStyle, fontSize, 0, 0,
      null, 0, 0, 0, 0,
      new Color(0.9f, 0.9f, 0.9f), Color.WHITE, 32.0,
      0.1, 0.7, 0.7,
    0.0, 1.0, 0.0);
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
  
  // --- PHONG LIGHTING WITH TEXTURE ---
  @Override
  public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPos) {
    // 1. Get texture color at this point
    Point3 localPoint = objectTransform.inverse().transformPoint(point);
    Color textureColor = getTextureColor(localPoint, normal);
    
    // 2. If texture has alpha=0, use diffuseColor (base)
    if (textureColor.getAlpha() == 0) {
      textureColor = diffuseColor;
    }
    
    // 3. Apply Phong lighting using textureColor as diffuse base
    Color lightColor = light.getColor();
    double attenuatedIntensity = 0.0;
    
    // Ambient component
    int rAmbient = (int) (textureColor.getRed()   * ambientCoefficient * lightColor.getRed()   / 255.0);
    int gAmbient = (int) (textureColor.getGreen() * ambientCoefficient * lightColor.getGreen() / 255.0);
    int bAmbient = (int) (textureColor.getBlue()  * ambientCoefficient * lightColor.getBlue()  / 255.0);
    
    // If light is ambient, return only ambient contribution
    if (light instanceof ElenaMuratAmbientLight) {
      return new Color(
        Math.min(255, rAmbient),
        Math.min(255, gAmbient),
        Math.min(255, bAmbient)
      );
    }
    
    // Get light direction
    Vector3 lightDir = getLightDirection(light, point);
    if (lightDir == null) return Color.BLACK;
    
    // Get attenuated intensity based on light type
    if (light instanceof MuratPointLight) {
      attenuatedIntensity = ((MuratPointLight) light).getAttenuatedIntensity(point);
      } else if (light instanceof ElenaDirectionalLight) {
      attenuatedIntensity = ((ElenaDirectionalLight) light).getIntensity();
      } else if (light instanceof PulsatingPointLight) {
      attenuatedIntensity = ((PulsatingPointLight) light).getAttenuatedIntensity(point);
      } else if (light instanceof SpotLight) {
      attenuatedIntensity = ((SpotLight) light).getAttenuatedIntensity(point);
      } else if (light instanceof BioluminescentLight) {
      attenuatedIntensity = ((BioluminescentLight) light).getAttenuatedIntensity(point);
      } else if (light instanceof BlackHoleLight) {
      attenuatedIntensity = ((BlackHoleLight) light).getAttenuatedIntensity(point);
      } else if (light instanceof FractalLight) {
      attenuatedIntensity = ((FractalLight) light).getAttenuatedIntensity(point);
      } else {
      System.err.println("Warning: Unsupported light type in PhongTextMaterial: " + light.getClass().getName());
      return Color.BLACK;
    }
    
    // Diffuse component
    double NdotL = Math.max(0, normal.dot(lightDir));
    int rDiffuse = (int) (textureColor.getRed()   * diffuseCoefficient * lightColor.getRed()   / 255.0 * attenuatedIntensity * NdotL);
    int gDiffuse = (int) (textureColor.getGreen() * diffuseCoefficient * lightColor.getGreen() / 255.0 * attenuatedIntensity * NdotL);
    int bDiffuse = (int) (textureColor.getBlue()  * diffuseCoefficient * lightColor.getBlue()  / 255.0 * attenuatedIntensity * NdotL);
    
    // Specular component
    Vector3 viewDir = viewerPos.subtract(point).normalize();
    Vector3 reflectDir = lightDir.negate().reflect(normal);
    double RdotV = Math.max(0, reflectDir.dot(viewDir));
    double specFactor = Math.pow(RdotV, shininess);
    
    int rSpecular = (int) (specularColor.getRed()   * specularCoefficient * lightColor.getRed()   / 255.0 * attenuatedIntensity * specFactor);
    int gSpecular = (int) (specularColor.getGreen() * specularCoefficient * lightColor.getGreen() / 255.0 * attenuatedIntensity * specFactor);
    int bSpecular = (int) (specularColor.getBlue()  * specularCoefficient * lightColor.getBlue()  / 255.0 * attenuatedIntensity * specFactor);
    
    // Combine components
    int finalR = Math.min(255, rAmbient + rDiffuse + rSpecular);
    int finalG = Math.min(255, gAmbient + gDiffuse + gSpecular);
    int finalB = Math.min(255, bAmbient + bDiffuse + bSpecular);
    
    return new Color(finalR, finalG, finalB);
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
  public Color getDiffuseColor() { return diffuseColor; }
  public Color getSpecularColor() { return specularColor; }
  public double getShininess() { return shininess; }
  public BufferedImage getTexture() { return texture; }
  
  private double clamp01(double val) {
    return Math.min(1.0, Math.max(0.0, val));
  }
  
  private Vector3 getLightDirection(Light light, Point3 point) {
    if (light instanceof MuratPointLight) {
      return ((MuratPointLight) light).getPosition().subtract(point).normalize();
      } else if (light instanceof ElenaDirectionalLight) {
      return ((ElenaDirectionalLight) light).getDirection().negate().normalize();
      } else if (light instanceof PulsatingPointLight) {
      return ((PulsatingPointLight) light).getPosition().subtract(point).normalize();
      } else if (light instanceof SpotLight) {
      return ((SpotLight) light).getDirectionAt(point).normalize();
      } else if (light instanceof BioluminescentLight) {
      return ((BioluminescentLight) light).getDirectionAt(point).normalize();
      } else if (light instanceof BlackHoleLight) {
      return ((BlackHoleLight) light).getDirectionAt(point).normalize();
      } else if (light instanceof FractalLight) {
      return ((FractalLight) light).getDirectionAt(point).normalize();
      } else {
      System.err.println("Warning: Unknown light type in PhongTextMaterial: " + light.getClass().getName());
      return new Vector3(0, 1, 0); // Fallback direction
    }
  }
  
  @Override
  public String toString() {
    return String.format("PhongTextMaterial[text='%s', diffuse=%s, shininess=%.1f]",
    word, diffuseColor, shininess);
  }
  
}
