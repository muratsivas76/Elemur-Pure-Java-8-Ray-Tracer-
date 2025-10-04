package net.elena.murat.material;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

import net.elena.murat.math.*;

/**
 * Material that applies a spherical texture with a word drawn on it.
 * The word is rendered on a texture mapped onto a sphere.
 * Supports transparency, reflectivity, gradient text, and background image parameters.
 */
public class SphereWordTextureMaterial implements Material {
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
  
  private final double reflectivity;
  private final double ior;
  private final double transparency;
  
  private Matrix4 objectInverseTransform;
  private BufferedImage texture;
  
  /**
   * Constructor with default background color (transparent black),
   * reflectivity=0.3, ior=1.0, transparency=0.0 (opaque), no offsets.
   * No gradient (uses solid textColor) and no background image.
   */
  public SphereWordTextureMaterial(String word, Color textColor,
    String fontFamily, int fontStyle, int fontSize) {
    this(word, textColor, null, "horizontal", new Color(0x00000000), fontFamily, fontStyle, fontSize,
    0.3, 1.0, 0.0, 0, 0, null, 0, 0, 0, 0);
  }
  
  /**
   * Full constructor with all parameters.
   *
   * @param word           The word to render on the sphere texture.
   * @param textColor      Primary text color (also used for gradient start/end depending on type).
   * @param gradientColor  Secondary color for gradient effect. If null, solid textColor is used.
   * @param gradientType   Type of gradient: "horizontal", "vertical", "diagonal".
   * @param bgColor        Background color of the texture.
   * @param fontFamily     Font family name.
   * @param fontStyle      Font style (Font.PLAIN, Font.BOLD, etc).
   * @param fontSize       Font size in points.
   * @param reflectivity   Reflectivity coefficient [0..1].
   * @param ior            Index of refraction (>=1.0).
   * @param uOffset        Horizontal pixel offset for text positioning.
   * @param vOffset        Vertical pixel offset for text positioning.
   * @param imageObject    BufferedImage to draw on the background, can be null.
   * @param imageWidth     Width to draw the image.
   * @param imageHeight    Height to draw the image.
   * @param imageUOffset   Horizontal pixel offset for image positioning.
   * @param imageVOffset   Vertical pixel offset for image positioning.
   */
  public SphereWordTextureMaterial(String word, Color textColor, Color gradientColor,
    String gradientType, Color bgColor,
    String fontFamily, int fontStyle, int fontSize,
    double reflectivity, double ior, double transparency,
    int uOffset, int vOffset,
    BufferedImage imageObject, int imageWidth, int imageHeight,
    int imageUOffset, int imageVOffset) {
    // Convert special English sequences to Norwegian characters and replace underscores with spaces
    this.word = (convertToNorwegianText(word)).replaceAll("_", " ");
    this.textColor = textColor;
    this.gradientColor = gradientColor;
    this.gradientType = (gradientType != null) ? gradientType : "horizontal";
    this.bgColor = bgColor;
    this.fontFamily = fontFamily.replaceAll("_", " ");
    this.fontStyle = fontStyle;
    this.fontSize = fontSize;
    this.reflectivity = Math.min(1.0, Math.max(0.0, reflectivity));
    this.ior = Math.max(1.0, ior);
    this.transparency=transparency;
    
    this.uOffset = uOffset;
    this.vOffset = vOffset;
    this.imageObject = imageObject;
    this.imageWidth = imageWidth;
    this.imageHeight = imageHeight;
    this.imageUOffset = imageUOffset;
    this.imageVOffset = imageVOffset;
    
    this.objectInverseTransform = new Matrix4();
    
    this.texture = createTexture();
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
    
    // Alpha composite setting for proper alpha channel handling
    //g2d.setComposite(AlphaComposite.SrcOver);
    
    // Enable anti-aliasing for smooth text and images
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    
    //g2d.setBackground(new Color(0, 0, 0, 0));
    //g2d.clearRect(0, 0, size, size);
    
    // Set background color with original alpha (transparency applied later in getColorAt)
    g2d.setBackground(bgColor);
    g2d.clearRect(0, 0, size, size);
    
    // Draw background image if provided
    if (imageObject != null) {
      int x = (size - imageWidth) / 2 + imageUOffset;
      int y = (size - imageHeight) / 2 - fontSize / 2;
      y -= (imageHeight/3);
      y += imageVOffset;
      g2d.drawImage(imageObject, x, y, imageWidth, imageHeight, null);
    }
    
    // Set font
    Font font = new Font(fontFamily, fontStyle, fontSize);
    g2d.setFont(font);
    
    // Calculate text position to center it, applying uOffset and vOffset
    FontMetrics fm = g2d.getFontMetrics();
    int textWidth = fm.stringWidth(word);
    int textHeight = fm.getHeight();
    int ascent = fm.getAscent();
    int descent = fm.getDescent ();
    
    int x = (size - textWidth) / 2 + uOffset;
    int y = (size - textHeight) / 2 + (textHeight/3) + ascent + vOffset;
    
    // GradientPaint
    if (gradientColor != null && gradientType != null) {
      java.awt.geom.Rectangle2D textBounds = fm.getStringBounds(word, g2d);
      
      float textX = x;
      float textY = y - fm.getAscent();
      
      float ftextWidth = (float) textBounds.getWidth();
      float ftextHeight = (float) textBounds.getHeight();
      
      GradientPaint gradient;
      
      switch (gradientType.toLowerCase()) {
        case "vertical":
        gradient = new GradientPaint(
          textX, textY, textColor,
          textX, textY + ftextHeight/2, gradientColor,
          true
        );
        break;
        
        case "diagonal":
        gradient = new GradientPaint(
          textX, textY, textColor,
          textX + ftextWidth/3, textY + ftextHeight/5, gradientColor,
          true
        );
        break;
        
        case "horizontal":
        default:
        gradient = new GradientPaint(
          textX, textY, textColor,
          textX + ftextWidth/3, textY, gradientColor,
          true
        );
        break;
      }
      
      g2d.setPaint(gradient);
      } else {
      g2d.setColor(textColor);
    }
    
    g2d.drawString(word, x, y);
    g2d.dispose();
    
    return texture;
  }
  
  /**
   * Converts English character sequences to Norwegian special characters.
   * For example, "AE" -> "Æ", "O/" -> "Ø", "A0" -> "Å", etc.
   *
   * @param input Input string possibly containing English sequences.
   * @return Converted string with Norwegian characters.
   */
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
   * Sets the inverse transform matrix of the object.
   * This matrix is used to convert world coordinates to local object coordinates.
   *
   * Note: The method keeps the original implementation as requested.
   *
   * @param tm The transformation matrix of the object.
   */
  @Override
  public void setObjectTransform(Matrix4 tm) {
    if (tm == null) tm = new Matrix4();
    this.objectInverseTransform = tm;
  }
  
  /**
   * Retrieves the color from the texture at the given local 3D point on the sphere.
   * Converts the 3D point to spherical coordinates and maps to 2D texture coordinates.
   *
   * @param localPoint Point in local object coordinates.
   * @return Color from the texture at the mapped UV coordinates.
   */
  private Color getTextureColor(Point3 localPoint) {
    if (texture == null) return textColor;
    
    // Normalize the point to get direction vector on unit sphere
    Vector3 dir = new Vector3(localPoint.x, localPoint.y, localPoint.z).normalize();
    
    // Spherical coordinates
    double phi = Math.atan2(dir.z, dir.x);   // azimuth angle [-pi, pi]
    double theta = Math.asin(dir.y);         // elevation angle [-pi/2, pi/2]
    
    // Convert spherical coordinates to UV texture coordinates [0..1]
    double u = 1.0 - (phi + Math.PI) / (2 * Math.PI);
    double v = (theta + Math.PI / 2) / Math.PI;
    v = 1.0 - v; // Flip vertically to match texture orientation
    
    // Apply fixed offset to u to align texture (can be adjusted or parameterized)
    u = (u + 0.25) % 1.0;
    
    // Convert UV to pixel coordinates
    int texX = (int) (u * texture.getWidth());
    texX = texX % texture.getWidth();
    if (texX < 0) texX += texture.getWidth();
    
    int texY = (int) (v * texture.getHeight());
    if (texY < 0 || texY >= texture.getHeight()) {
      // Outside texture bounds, return fully transparent
      return new Color(0, 0, 0, 0);
    }
    
    return new Color(texture.getRGB(texX, texY), true);
  }
  
  /**
   * Returns the color of the material at the given world point, considering lighting and viewer position.
   * Applies diffuse and specular lighting based on reflectivity and transparency.
   *
   * @param worldPoint  Point in world coordinates.
   * @param worldNormal Surface normal at the point.
   * @param light       Light source affecting the point.
   * @param viewerPos   Position of the viewer/camera.
   * @return Color of the material at the point.
   */
  @Override
  public Color getColorAt(Point3 worldPoint, Vector3 worldNormal, net.elena.murat.light.Light light, Point3 viewerPos) {
    Point3 localPoint = objectInverseTransform.transformPoint(worldPoint);
    Color textureColor = getTextureColor(localPoint);
    
    if (light instanceof net.elena.murat.light.ElenaMuratAmbientLight) {
      return textureColor;
    }
    
    Vector3 lightDir = getLightDirection(light, worldPoint);
    if (lightDir != null) {
      double diffuseFactor = Math.max(0, worldNormal.dot(lightDir));
      
      double specularFactor = 0.0;
      if (reflectivity > 0.0) {
        Vector3 viewDir = viewerPos.subtract(worldPoint).normalize();
        Vector3 reflectDir = lightDir.negate().reflect(worldNormal);
        specularFactor = Math.pow(Math.max(0, viewDir.dot(reflectDir)), 32) * reflectivity;
      }
      
      double totalFactor = Math.min(1.0, diffuseFactor + specularFactor);
      
      return new Color(
        (int)(textureColor.getRed() * totalFactor),
        (int)(textureColor.getGreen() * totalFactor),
        (int)(textureColor.getBlue() * totalFactor),
        textureColor.getAlpha()
      );
    }
    
    return textureColor;
  }
  
  /**
   * Helper method to get the light direction vector for various light types.
   *
   * @param light      Light source.
   * @param worldPoint Point on the surface in world coordinates.
   * @return Normalized direction vector from point to light or light direction.
   */
  private Vector3 getLightDirection(net.elena.murat.light.Light light, Point3 worldPoint) {
    if (light instanceof net.elena.murat.light.ElenaDirectionalLight) {
      return ((net.elena.murat.light.ElenaDirectionalLight) light).getDirection().normalize();
      } else if (light instanceof net.elena.murat.light.MuratPointLight) {
      return ((net.elena.murat.light.MuratPointLight) light).getPosition().subtract(worldPoint).normalize();
      } else if (light instanceof net.elena.murat.light.PulsatingPointLight) {
      return ((net.elena.murat.light.PulsatingPointLight) light).getPosition().subtract(worldPoint).normalize();
      } else if (light instanceof net.elena.murat.light.BioluminescentLight) {
      return ((net.elena.murat.light.BioluminescentLight) light).getDirectionAt(worldPoint).normalize();
      } else if (light instanceof net.elena.murat.light.BlackHoleLight) {
      return ((net.elena.murat.light.BlackHoleLight) light).getDirectionAt(worldPoint).normalize();
      } else if (light instanceof net.elena.murat.light.FractalLight) {
      return ((net.elena.murat.light.FractalLight) light).getDirectionAt(worldPoint).normalize();
      } else if (light instanceof net.elena.murat.light.SpotLight) {
      return ((net.elena.murat.light.SpotLight) light).getDirectionAt(worldPoint).normalize();
    }
    return null;
  }
  
  /**
   * Returns the reflectivity coefficient of the material.
   *
   * @return Reflectivity [0..1].
   */
  @Override
  public double getReflectivity() {
    return reflectivity;
  }
  
  /**
   * Returns the index of refraction of the material.
   *
   * @return Index of refraction (>=1.0).
   */
  @Override
  public double getIndexOfRefraction() {
    return ior;
  }
  
  /**
   * Returns the transparency coefficient of the material.
   *
   * @return Transparency [0..1], 1.0 = fully transparent.
   */
  @Override
  public double getTransparency() {
    return transparency;
  }
  
}
