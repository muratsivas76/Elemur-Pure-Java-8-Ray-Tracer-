package net.elena.murat.material;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.util.Random;
import net.elena.murat.light.*;
import net.elena.murat.math.*;

public class GradientTextMaterial implements Material {
  private final Color bgStartColor;
  private final Color bgEndColor;
  private final Color textStartColor;
  private final Color textEndColor;
  private final String text;
  private final Font font;
  private final StripeDirection direction;
  private final double reflectivity;
  private final double ior;
  private final double transparency;
  private Matrix4 objectInverseTransform;
  private final int xOffset;
  private final int yOffset;
  
  private BufferedImage texture;
  private Random random = new Random();
  
  // Main constructor with all parameters
  public GradientTextMaterial(Color bgStart, Color bgEnd,
    Color textStart, Color textEnd,
    String text, Font font, StripeDirection direction,
    double reflectivity, double ior, double transparency,
    Matrix4 objectInverseTransform,
    int xOffset, int yOffset) {
    this.bgStartColor = bgStart;
    this.bgEndColor = bgEnd;
    this.textStartColor = textStart;
    this.textEndColor = textEnd;
    this.text = text;
    this.font = font;
    this.direction = direction;
    this.reflectivity = Math.min(1.0, Math.max(0.0, reflectivity));
    this.ior = Math.max(1.0, ior);
    this.transparency = Math.min(1.0, Math.max(0.0, transparency));
    this.objectInverseTransform = objectInverseTransform;
    this.xOffset = xOffset;
    this.yOffset = yOffset;
    
    this.texture = createCompositeTexture();
  }
  
  // Simplified constructor with default parameters
  public GradientTextMaterial(String text) {
    this(text, 0, 0);
  }
  
  // Constructor with text and position offsets
  public GradientTextMaterial(String text, int xOffset, int yOffset) {
    this(
      generateRandomColor(),
      generateRandomColor(),
      Color.WHITE,
      Color.BLACK,
      text,
      new Font("Arial", Font.BOLD, 72),
      StripeDirection.RANDOM,
      0.3,
      1.0,
      0.1,
      new Matrix4 (),
      xOffset,
      yOffset
    );
  }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
    if (tm == null) tm = new Matrix4 ();
    this.objectInverseTransform = tm;
  }
  
  private static Color generateRandomColor() {
    Random rand = new Random();
    return new Color(rand.nextFloat(), rand.nextFloat(), rand.nextFloat());
  }
  
  /**
   * Creates a texture with cyclic (repeating) gradients for strong visual impact.
   * Uses GradientPaint with isCyclic=true to create wave-like color transitions
   * that remain visible even after spherical mapping and on low-resolution renders.
   *
   * @return A BufferedImage with repeating gradient patterns suitable for 3D materials.
   */
  private BufferedImage createCompositeTexture() {
    final int size = 512;  // Lower resolution for better performance
    BufferedImage texture = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2d = texture.createGraphics();
    
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    
    // === 1. BACKGROUND: CYCLIC GRADIENT ===
    StripeDirection bgDir = (direction == StripeDirection.RANDOM)
    ? StripeDirection.values()[random.nextInt(3)]
    : direction;
    
    Point2D bgStart = new Point2D.Float(0, 0);
    Point2D bgEnd;
    
    // Define a short vector to make gradient repeat frequently
    switch (bgDir) {
      case HORIZONTAL:
        bgEnd = new Point2D.Float((float)(size * 0.2), 0.0f);  // Repeat every 20% horizontally
      break;
      case VERTICAL:
        bgEnd = new Point2D.Float(0.0f, (float)(size * 0.2));  // Repeat every 20% vertically
      break;
      case DIAGONAL:
        bgEnd = new Point2D.Float((float)(size * 0.15), (float)(size * 0.15));  // Short diagonal
      break;
      default:
        bgEnd = new Point2D.Float((float)(size * 0.2), 0.0f);
    }
    
    // isCyclic = true → gradient repeats infinitely across the texture
    GradientPaint bgGradient = new GradientPaint(
      bgStart, bgStartColor,
      bgEnd, bgEndColor,
      true  // <<< THIS LINE MAKES THE GRADIENT REPEATING
    );
    g2d.setPaint(bgGradient);
    g2d.fillRect(0, 0, size, size);
    
    // === 2. TEXT: CYCLIC GRADIENT MASK ===
    if (text != null && !text.isEmpty()) {
      // Fix: Do not assign to 'font' if it's final
      // Use a local font or assume 'font' is already set
      Font renderFont = font != null ? font : new Font("Arial", Font.BOLD, size / 6);
      g2d.setFont(renderFont);
      
      FontMetrics fm = g2d.getFontMetrics();
      int textWidth = fm.stringWidth(text);
      int textHeight = fm.getHeight();
      int ascent = fm.getAscent();
      
      int x = (size - textWidth) / 2 + xOffset;
      int y = (size - textHeight) / 2 + ascent + yOffset;
      
      x = Math.max(0, Math.min(size - textWidth, x));
      y = Math.max(ascent, Math.min(size - fm.getDescent(), y));
      
      // Define cyclic gradient direction for the text
      Point2D textStart = new Point2D.Float((float)x, (float)y);
      
      Point2D textEnd;
      
      switch (direction) {
        case HORIZONTAL:
          textEnd = new Point2D.Float(x + textWidth, y);
        break;
        case VERTICAL:
          textEnd = new Point2D.Float(x, y + textHeight);
        break;
        case DIAGONAL:
          textEnd = new Point2D.Float(x + textWidth, y + textHeight);
        break;
        
        default:
          textEnd = new Point2D.Float(x + textWidth, y);
      }
      
      // Text gradient is also cyclic!
      GradientPaint textGradient = new GradientPaint(
        textStart, textStartColor,
        textEnd, textEndColor,
        true  // <<< REPEATING TEXT GRADIENT
      );
      g2d.setPaint(textGradient);
      g2d.drawString(text, x, y);
    }
    
    g2d.dispose();
    return texture;
  }
  
  private Point2D getEndPoint(int size, StripeDirection dir) {
    switch (dir) {
      case HORIZONTAL: return new Point2D.Float(size, 0);
      case VERTICAL: return new Point2D.Float(0, size);
      case DIAGONAL: return new Point2D.Float(size, size);
      default: return new Point2D.Float(size, 0);
    }
  }
  
  private Point2D getEndPoint(int width, int height, StripeDirection dir) {
    switch (dir) {
      case HORIZONTAL: return new Point2D.Float(width, 0);
      case VERTICAL: return new Point2D.Float(0, height);
      case DIAGONAL: return new Point2D.Float(width, height);
      default: return new Point2D.Float(width, 0);
    }
  }
  
  private Vector3 getLightDirection(Light light, Point3 worldPoint) {
    if (light instanceof ElenaDirectionalLight) {
      return ((ElenaDirectionalLight)light).getDirection().normalize();
      } else if (light instanceof MuratPointLight) {
      return ((MuratPointLight)light).getPosition().subtract(worldPoint).normalize();
      } else if (light instanceof PulsatingPointLight) {
      return ((PulsatingPointLight)light).getPosition().subtract(worldPoint).normalize();
      } else if (light instanceof BioluminescentLight) {
      return ((BioluminescentLight)light).getDirectionAt(worldPoint).normalize();
      } else if (light instanceof BlackHoleLight) {
      return ((BlackHoleLight)light).getDirectionAt(worldPoint).normalize();
      } else if (light instanceof FractalLight) {
      return ((FractalLight)light).getDirectionAt(worldPoint).normalize();
      } else {
      System.err.println("Warning: Unsupported light type: " + light.getClass().getName());
      return null;
    }
  }
  
  @Override
  public Color getColorAt(Point3 worldPoint, Vector3 worldNormal, Light light, Point3 viewerPos) {
    if (objectInverseTransform == null) {
      return bgStartColor;
    }
    
    Point3 localPoint = objectInverseTransform.transformPoint(worldPoint);
    Vector3 localNormal = objectInverseTransform.inverseTransposeForNormal().transformVector(worldNormal).normalize();
    
    Color textureColor = getTextureColor(localPoint, localNormal);
    if (textureColor.getAlpha() == 0) {
      return new Color(0, 0, 0, 0);
    }
    
    double texR = textureColor.getRed() / 255.0;
    double texG = textureColor.getGreen() / 255.0;
    double texB = textureColor.getBlue() / 255.0;
    
    double rCombined = 0.0;
    double gCombined = 0.0;
    double bCombined = 0.0;
    
    if (light instanceof ElenaMuratAmbientLight) {
      double ambientIntensity = light.getIntensity();
      rCombined = texR * ambientIntensity * (light.getColor().getRed() / 255.0);
      gCombined = texG * ambientIntensity * (light.getColor().getGreen() / 255.0);
      bCombined = texB * ambientIntensity * (light.getColor().getBlue() / 255.0);
      } else {
      Vector3 lightDir = getLightDirection(light, worldPoint);
      if (lightDir != null) {
        double diffuseFactor = Math.max(0, worldNormal.dot(lightDir));
        rCombined = texR * diffuseFactor * (light.getColor().getRed() / 255.0) * light.getIntensity();
        gCombined = texG * diffuseFactor * (light.getColor().getGreen() / 255.0) * light.getIntensity();
        bCombined = texB * diffuseFactor * (light.getColor().getBlue() / 255.0) * light.getIntensity();
        
        if (diffuseFactor > 0) {
          Vector3 viewDir = viewerPos.subtract(worldPoint).normalize();
          Vector3 reflectDir = lightDir.negate().reflect(worldNormal);
          double specFactor = Math.pow(Math.max(0, viewDir.dot(reflectDir)), 32);
          
          rCombined += specFactor * (light.getColor().getRed() / 255.0);
          gCombined += specFactor * (light.getColor().getGreen() / 255.0);
          bCombined += specFactor * (light.getColor().getBlue() / 255.0);
        }
      }
    }
    
    return new Color(
      (float)Math.min(1.0, Math.max(0.0, rCombined)),
      (float)Math.min(1.0, Math.max(0.0, gCombined)),
      (float)Math.min(1.0, Math.max(0.0, bCombined)),
      textureColor.getAlpha() / 255.0f
    );
  }
  
  /**
   * Maps a 3D point on the sphere to a 2D texture coordinate using spherical mapping.
   * The texture is sampled with proper orientation, ensuring text appears upright
   * when viewed from the front of the sphere.
   *
   * @param localPoint  The point on the surface in object space.
   * @param localNormal The surface normal (unused here, kept for interface).
   * @return The color sampled from the texture.
   */
  private Color getTextureColor(Point3 localPoint, Vector3 localNormal) {
    if (texture == null) return bgStartColor;
    
    // Normalize direction vector from center to point
    Vector3 dir = new Vector3(localPoint.x, localPoint.y, localPoint.z).normalize();
    
    // Convert to spherical coordinates
    double phi = Math.atan2(dir.z, dir.x);           // -π to π
    double theta = Math.asin(dir.y);                 // -π/2 to π/2
    
    // Map to UV [0,1]
    // U: Reverse the horizontal wrap so text appears correct
    double u = 1.0 - (phi + Math.PI) / (2 * Math.PI); // Flip U horizontally
    double v = (theta + Math.PI / 2) / Math.PI;      // V: top to bottom
    
    // Flip V because BufferedImage has Y-down
    v = 1.0 - v;
    
    // Wrap U for seamless tiling
    int texX = (int)(u * texture.getWidth());
    texX = texX % texture.getWidth();
    if (texX < 0) texX += texture.getWidth();
    
    // Clamp V
    int texY = (int)(v * texture.getHeight());
    if (texY < 0 || texY >= texture.getHeight()) {
      return new Color(0, 0, 0, 0);
    }
    
    return new Color(texture.getRGB(texX, texY), true);
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
  
}

/***
EMShape sphere = new Sphere(1.2);

sphere.setTransform(Matrix4.translate(0, 1.2, 0));

Material material = new GradientTextMaterial(
Color.GREEN, Color.WHITE.darker (), //BG Colors
Color.RED, Color.BLUE,           // Gradient colors
"Takk",                          // Norwegian text
new Font("Arial", Font.BOLD, 200),// Font
GradientTextMaterial.StripeDirection.DIAGONAL, // Gradient direction
0.2, 1.0, 0.0,                   // reflectivity, IOR, transparency
sphere.getInverseTransform(),       // object transform
0, 0 //x and y offset
);

sphere.setMaterial(material);
 */
