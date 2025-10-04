import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.util.*;
import java.util.List;
import java.io.*;
import javax.imageio.ImageIO;

/**
 * DynamicWordArtGenerator - Creates stylized word art images with various visual effects
 * Supports external configuration through infoDynamic.txt file
 */
public class DynamicWordArtGenerator {
  // Default configuration values
  private static final List<String> DEFAULT_FONT_NAMES = Arrays.asList(
    "Arial", "Helvetica", "Times New Roman", "Courier New",
    "Verdana", "Georgia", "Palatino", "Garamond", "Century Gothic",
    "Tahoma", "Trebuchet MS", "Impact", "Comic Sans MS", "Lucida Console",
    "Franklin Gothic Medium", "Century Schoolbook", "Bookman Old Style",
    "Copperplate Gothic Bold", "Arial Black", "Lucida Sans Unicode"
  );
  
  private static final int[][] DEFAULT_COLOR_PALETTES = {
    {0xFF00F5FF, 0xFF00DDFF, 0xFF0077FF, 0xFF0055AA}, // Blues
    {0xFFFF3366, 0xFFFF99CC, 0xFFFF0066, 0xFFCC0055}, // Pinks/Reds
    {0xFF66FF33, 0xFF99FF66, 0xFF33CC00, 0xFF229900}, // Greens
    {0xFFFFFF33, 0xFFFFFF99, 0xFFFFCC00, 0xFFFF9900}, // Yellows/Oranges
    {0xFF9966FF, 0xFFCC99FF, 0xFF6600CC, 0xFF440099}, // Purples
    {0xFFFF9933, 0xFFFFCC99, 0xFFFF6600, 0xFFCC4400}, // Oranges
    {0xFF33CCFF, 0xFF99FFFF, 0xFF0099CC, 0xFF006699}, // Light Blues
    {0xFFCC33FF, 0xFFEE99FF, 0xFF9900CC, 0xFF660099}  // Violets
  };
  
  private static final String CONFIG_FILE = "infoDynamic.txt";
  
  private List<String> fontNames;
  private int[][] colorPalettes;
  private List<TexturePaint> texturePaints;
  private int imageSize;
  private float minFontSize;
  private float maxFontSize;
  
  /**
   * Constructor - Initializes the generator with configuration
   */
  public DynamicWordArtGenerator() {
    loadConfiguration();
  }
  
  /**
   * Loads configuration from file or uses defaults
   */
  private void loadConfiguration() {
    fontNames = new ArrayList<>(DEFAULT_FONT_NAMES);
    colorPalettes = Arrays.copyOf(DEFAULT_COLOR_PALETTES, DEFAULT_COLOR_PALETTES.length);
    texturePaints = new ArrayList<>();
    imageSize = 256;
    minFontSize = 24f;
    maxFontSize = 60f;
    
    File configFile = new File(CONFIG_FILE);
    if (!configFile.exists()) {
      createDefaultConfigFile();
      return;
    }
    
    try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
      String line;
      while ((line = reader.readLine()) != null) {
        line = line.trim();
        if (line.isEmpty() || line.startsWith("#")) continue;
        
        if (line.startsWith("font:")) {
          String fontName = line.substring(5).trim();
          if (!fontNames.contains(fontName)) {
            fontNames.add(fontName);
          }
        }
        else if (line.startsWith("color_palette:")) {
          String[] colors = line.substring(14).trim().split("\\s*,\\s*");
          if (colors.length >= 2) {
            int[] palette = new int[colors.length];
            for (int i = 0; i < colors.length; i++) {
              try {
                palette[i] = (int) Long.parseLong(colors[i].replace("#", ""), 16);
                if (colors[i].length() <= 6) { // If no alpha specified
                  palette[i] = 0xFF000000 | palette[i]; // Add full alpha
                }
                } catch (NumberFormatException e) {
                System.err.println("Invalid color format: " + colors[i]);
                palette[i] = 0xFFFFFFFF; // Default to white
              }
            }
            // Add to palettes
            int[][] newPalettes = new int[colorPalettes.length + 1][];
            System.arraycopy(colorPalettes, 0, newPalettes, 0, colorPalettes.length);
            newPalettes[colorPalettes.length] = palette;
            colorPalettes = newPalettes;
          }
        }
        else if (line.startsWith("texture:")) {
          String texturePath = line.substring(8).trim();
          try {
            BufferedImage textureImg = ImageIO.read(new File(texturePath));
            if (textureImg != null) {
              TexturePaint texture = new TexturePaint(textureImg,
              new Rectangle(0, 0, textureImg.getWidth(), textureImg.getHeight()));
              texturePaints.add(texture);
            }
            } catch (IOException e) {
            System.err.println("Failed to load texture: " + texturePath);
          }
        }
        else if (line.startsWith("image_size:")) {
          try {
            imageSize = Integer.parseInt(line.substring(11).trim());
            } catch (NumberFormatException e) {
            System.err.println("Invalid image size: " + line);
          }
        }
        else if (line.startsWith("min_font_size:")) {
          try {
            minFontSize = Float.parseFloat(line.substring(14).trim());
            } catch (NumberFormatException e) {
            System.err.println("Invalid min font size: " + line);
          }
        }
        else if (line.startsWith("max_font_size:")) {
          try {
            maxFontSize = Float.parseFloat(line.substring(14).trim());
            } catch (NumberFormatException e) {
            System.err.println("Invalid max font size: " + line);
          }
        }
      }
      } catch (IOException e) {
      System.err.println("Error reading configuration file: " + e.getMessage());
    }
  }
  
  /**
   * Creates a default configuration file if none exists
   */
  private void createDefaultConfigFile() {
    try (PrintWriter writer = new PrintWriter(new FileWriter(CONFIG_FILE))) {
      writer.println("# Dynamic Word Art Generator Configuration");
      writer.println("# Add one setting per line using the format: key:value");
      writer.println();
      writer.println("# Fonts (add additional fonts available on your system)");
      writer.println("font:Algerian");
      writer.println("font:Bauhaus 93");
      writer.println("font:Berlin Sans FB");
      writer.println();
      writer.println("# Color palettes (comma-separated ARGB hex values)");
      writer.println("color_palette:#FFFFDD,#FFDD88,#FFBB33,#FF9900");
      writer.println("color_palette:#DDFFDD,#88FF88,#33FF33,#00CC00");
      writer.println("color_palette:#CCEEFF,#88CCFF,#3388FF,#0066CC");
      writer.println();
      writer.println("# Textures (path to image files)");
      writer.println("texture:metal_texture.jpg");
      writer.println("texture:paper_texture.png");
      writer.println("texture:wood_texture.jpg");
      writer.println();
      writer.println("# Sizing options");
      writer.println("image_size:256");
      writer.println("min_font_size:24");
      writer.println("max_font_size:60");
      
      } catch (IOException e) {
      System.err.println("Error creating default configuration file: " + e.getMessage());
    }
  }
  
  /**
   * Generates a word art image with the given text and background color
   */
  public BufferedImage generateWordArt(String word, int bgColor)
  throws IOException, NumberFormatException {
    int width = imageSize;
    int height = imageSize;
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2d = image.createGraphics();
    
    // Fill background
    g2d.setColor(new Color(bgColor, true));
    g2d.fillRect(0, 0, width, height);
    
    // Set rendering hints for quality
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
    g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    
    Random random = new Random();
    
    // Apply random background effect (10% chance)
    if (random.nextFloat() < 0.1f) {
      applyBackgroundEffect(g2d, width, height, random);
    }
    
    // Choose a random effect type
    int effectType = random.nextInt(6);
    
    switch(effectType) {
      case 0:
        applyGradientEffect(g2d, word, width, height, random);
      break;
      case 1:
        applyGlowEffect(g2d, word, width, height, random);
      break;
      case 2:
        apply3DEffect(g2d, word, width, height, random);
      break;
      case 3:
        applyOutlineEffect(g2d, word, width, height, random);
      break;
      case 4:
        applyTextureEffect(g2d, word, width, height, random);
      break;
      case 5:
        applyMixedEffect(g2d, word, width, height, random);
      break;
    }
    
    g2d.dispose();
    return image;
  }
  
  /**
   * Applies a random background effect
   */
  private void applyBackgroundEffect(Graphics2D g2d, int width, int height, Random random) {
    int effectType = random.nextInt(3);
    
    switch(effectType) {
      case 0: // Gradient background
        int[] palette = colorPalettes[random.nextInt(colorPalettes.length)];
      GradientPaint gradient = new GradientPaint(
        0, 0, new Color(palette[0], true),
        width, height, new Color(palette[1], true)
      );
      g2d.setPaint(gradient);
      g2d.fillRect(0, 0, width, height);
      break;
      
      case 1: // Texture background
      if (!texturePaints.isEmpty()) {
        TexturePaint texture = texturePaints.get(random.nextInt(texturePaints.size()));
        g2d.setPaint(texture);
        g2d.fillRect(0, 0, width, height);
      }
      break;
      
      case 2: // Radial gradient background
        int[] colors = colorPalettes[random.nextInt(colorPalettes.length)];
      RadialGradientPaint radialGradient = new RadialGradientPaint(
        new Point(width/2, height/2), width/2,
        new float[] {0.0f, 0.5f, 1.0f},
        new Color[] {
          new Color(colors[0], true),
          new Color(colors[1], true),
          new Color(colors[2], true)
        }
      );
      g2d.setPaint(radialGradient);
      g2d.fillRect(0, 0, width, height);
      break;
    }
  }
  
  /**
   * Applies a gradient effect to the text
   */
  private void applyGradientEffect(Graphics2D g2d, String word, int width, int height, Random random) {
    int[] palette = colorPalettes[random.nextInt(colorPalettes.length)];
    
    Point2D start = new Point2D.Float(
      random.nextInt(width/2),
      random.nextInt(height/2)
    );
    Point2D end = new Point2D.Float(
      (float)(start.getX() + width/2 + random.nextInt(width/2)),
      (float)(start.getY() + height/2 + random.nextInt(height/2))
    );
    
    GradientPaint gradient = new GradientPaint(
      start, new Color(palette[0], true),
      end, new Color(palette[1], true)
    );
    
    drawText(g2d, word, width, height, random, gradient, null, null);
  }
  
  /**
   * Applies a glow effect to the text
   */
  private void applyGlowEffect(Graphics2D g2d, String word, int width, int height, Random random) {
    int[] palette = colorPalettes[random.nextInt(colorPalettes.length)];
    
    // Draw glow (multiple layers with decreasing opacity)
    for (int i = 8; i > 0; i--) {
      Color glowColor = new Color(
        (palette[2] >> 16) & 0xFF,
        (palette[2] >> 8) & 0xFF,
        palette[2] & 0xFF,
        10 + i * 5
      );
      g2d.setColor(glowColor);
      drawText(g2d, word, width, height, random, null, null, new BasicStroke(1.5f * i));
    }
    
    // Draw main text
    drawText(g2d, word, width, height, random, new Color(palette[0], true), null, null);
  }
  
  /**
   * Applies a 3D effect to the text
   */
  private void apply3DEffect(Graphics2D g2d, String word, int width, int height, Random random) {
    int[] palette = colorPalettes[random.nextInt(colorPalettes.length)];
    
    // Draw shadow
    g2d.setColor(new Color(palette[2], true));
    for (int i = 0; i < 5; i++) {
      drawText(g2d, word, width - i, height - i, random, null, null, null);
    }
    
    // Draw main text
    drawText(g2d, word, width, height, random, new Color(palette[0], true), null, null);
    
    // Add highlight
    g2d.setColor(new Color(255, 255, 255, 80));
    drawText(g2d, word, width - 2, height - 2, random, null, null, null);
  }
  
  /**
   * Applies an outline effect to the text
   */
  private void applyOutlineEffect(Graphics2D g2d, String word, int width, int height, Random random) {
    int[] palette = colorPalettes[random.nextInt(colorPalettes.length)];
    
    // Draw outline
    g2d.setStroke(new BasicStroke(2.5f));
    g2d.setColor(new Color(palette[2], true));
    drawText(g2d, word, width, height, random, null, new Color(palette[2], true), null);
    
    // Draw main text
    g2d.setColor(new Color(palette[0], true));
    drawText(g2d, word, width, height, random, null, null, null);
  }
  
  /**
   * Applies a texture effect to the text
   */
  private void applyTextureEffect(Graphics2D g2d, String word, int width, int height, Random random) {
    if (!texturePaints.isEmpty()) {
      TexturePaint texture = texturePaints.get(random.nextInt(texturePaints.size()));
      drawText(g2d, word, width, height, random, texture, null, null);
      } else {
      // Fallback to gradient if no textures available
      applyGradientEffect(g2d, word, width, height, random);
    }
  }
  
  /**
   * Applies a mixed effect combining multiple techniques
   */
  private void applyMixedEffect(Graphics2D g2d, String word, int width, int height, Random random) {
    int[] palette = colorPalettes[random.nextInt(colorPalettes.length)];
    
    // Apply multiple random effects
    if (random.nextBoolean()) {
      // Add shadow
      g2d.setColor(new Color(palette[2], true));
      for (int i = 0; i < 3; i++) {
        drawText(g2d, word, width - i, height - i, random, null, null, null);
      }
    }
    
    if (random.nextBoolean() && !texturePaints.isEmpty()) {
      // Texture fill
      TexturePaint texture = texturePaints.get(random.nextInt(texturePaints.size()));
      drawText(g2d, word, width, height, random, texture, null, null);
      } else {
      // Gradient fill
      Point2D start = new Point2D.Float(
        random.nextInt(width/3),
        random.nextInt(height/3)
      );
      Point2D end = new Point2D.Float(
        (float)(start.getX() + width/2 + random.nextInt(width/2)),
        (float)(start.getY() + height/2 + random.nextInt(height/2))
      );
      
      GradientPaint gradient = new GradientPaint(
        start, new Color(palette[0], true),
        end, new Color(palette[1], true)
      );
      drawText(g2d, word, width, height, random, gradient, null, null);
    }
    
    if (random.nextBoolean()) {
      // Add outline
      g2d.setStroke(new BasicStroke(1.5f));
      g2d.setColor(new Color(palette[3], true));
      drawText(g2d, word, width, height, random, null, new Color(palette[3], true), null);
    }
  }
  
  /**
   * Draws the text with the specified properties
   */
  private void drawText(Graphics2D g2d, String word, int width, int height,
    Random random, Paint fillPaint, Color outlineColor, Stroke stroke) {
    
    // Select random font
    String fontName = fontNames.get(random.nextInt(fontNames.size()));
    int fontStyle = Font.PLAIN;
    
    if (random.nextBoolean()) fontStyle |= Font.BOLD;
    if (random.nextBoolean()) fontStyle |= Font.ITALIC;
    
    float fontSize = minFontSize + random.nextFloat() * (maxFontSize - minFontSize);
    Font font = new Font(fontName, fontStyle, (int) fontSize);
    g2d.setFont(font);
    
    // Center the text
    FontMetrics metrics = g2d.getFontMetrics();
    Rectangle2D bounds = metrics.getStringBounds(word, g2d);
    int x = (int) ((width - bounds.getWidth()) / 2);
    int y = (int) ((height - bounds.getHeight()) / 2 + metrics.getAscent());
    
    // Apply rotation (20% chance)
    if (random.nextFloat() < 0.2f) {
      double angle = Math.toRadians(random.nextInt(15) - 7.5);
      AffineTransform transform = AffineTransform.getRotateInstance(angle, width/2, height/2);
      g2d.setTransform(transform);
    }
    
    // Set fill paint
    if (fillPaint != null) {
      g2d.setPaint(fillPaint);
      } else if (outlineColor == null) {
      int[] palette = colorPalettes[random.nextInt(colorPalettes.length)];
      g2d.setColor(new Color(palette[random.nextInt(palette.length)], true));
    }
    
    // Set stroke if provided
    if (stroke != null) {
      g2d.setStroke(stroke);
    }
    
    // Draw outline if specified
    if (outlineColor != null) {
      g2d.setColor(outlineColor);
      g2d.drawString(word, x, y);
      } else {
      // Draw filled text
      g2d.drawString(word, x, y);
    }
    
    // Reset transformation
    g2d.setTransform(new AffineTransform());
  }
  
  /**
   * Main method for testing the generator
   */
  public static void main(String[] args) {
    if (args.length < 3) {
      System.out.println ("Usage:\n\t java DynamicWordArtGenerator [<str_str_...>] [<bgColor8hex>] [<dstFilePath>]");
      System.out.println ("Example:\n\t java DynamicWordArtGenerator Murat_iNAN 0x00000000 word.png");
      System.exit (-1);
    }
    try {
      String text = args [0].replaceAll ("_", " ");
      String numstr = args [1];
      if (numstr.startsWith ("0x")) {
        numstr = numstr.substring (2);
      }
      
      File dstfile = new File (args [2]);
      
      int hex = Integer.parseInt (numstr, 16);
      
      DynamicWordArtGenerator generator = new DynamicWordArtGenerator();
      
      BufferedImage image = generator.generateWordArt (text, hex);
      
      // Generate sample images
      //BufferedImage image1 = generator.generateWordArt("vann", 0x00000000); // Transparent background
      //BufferedImage image2 = generator.generateWordArt("su", 0xFFFFFFFF);   // White background
      //BufferedImage image3 = generator.generateWordArt("water", 0xFF336699); // Blue background
      
      // Save images
      ImageIO.write(image, "PNG", dstfile);
      System.out.println ("Saved successfully: "+dstfile.getName ()+"");
      } catch (IOException ioe) {
      ioe.printStackTrace ();
      System.exit (-1);
      } catch (NumberFormatException nfe) {
      nfe.printStackTrace ();
      System.exit (-1);
    }
    
    return;
  } // end of main
  
} // class end
/***
More efficient:
int color = (int) Long.parseLong("FFFF0000", 16);
// Result: -65536 (ARGB full red)
 */
