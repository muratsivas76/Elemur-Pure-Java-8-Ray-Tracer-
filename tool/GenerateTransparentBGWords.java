import java.awt.*;
import java.awt.image.*;
import java.awt.geom.Point2D;
import java.io.*;
import javax.imageio.*;
import java.util.Random;

public class GenerateTransparentBGWords {
  
  public static void main(String[] args) {
    if (args.length < 7) {
      System.out.println("Usage: java GenerateTransparentBGWords <width> <height> <bgColor> <textColor1> <textColor2> <font,style,size> <text>");
      System.out.println("Example: java GenerateTransparentBGWords 128 128 #00000000 #FF0000FF #0000FFFF Lucida_Calligraphy,1,40 Murat_iNAN");
      System.out.println("Color format: #AARRGGBB (Alpha, Red, Green, Blue)");
      System.out.println("Font style: 0=PLAIN, 1=BOLD, 2=ITALIC, 3=BOLD+ITALIC");
      System.exit(1);
    }
    
    try {
      // Parse command line arguments
      int width = Integer.parseInt(args[0]);
      int height = Integer.parseInt(args[1]);
      String bgColorHex = args[2];
      String textColor1Hex = args[3];
      String textColor2Hex = args[4];
      String fontInfo = args[5];
      String text = args[6].replace("_", " "); // Replace underscores with spaces
      
      // Parse font information
      String[] fontParts = fontInfo.split(",");
      if (fontParts.length != 3) {
        throw new IllegalArgumentException("Font format must be: fontName,style,size");
      }
      
      String fontName = fontParts[0].replace("_", " ");
      int fontStyle = Integer.parseInt(fontParts[1]);
      int fontSize = Integer.parseInt(fontParts[2]);
      
      // Parse colors
      Color backgroundColor = parseColor(bgColorHex);
      Color textColor1 = parseColor(textColor1Hex);
      Color textColor2 = parseColor(textColor2Hex);
      
      // Create transparent buffered image
      BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
      Graphics2D g2d = image.createGraphics();
      
      // Enable anti-aliasing for better quality
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      
      // Fill background with specified color (can be transparent)
      g2d.setColor(backgroundColor);
      g2d.fillRect(0, 0, width, height);
      
      // Set font
      Font font = new Font(fontName, fontStyle, fontSize);
      g2d.setFont(font);
      
      // Create GradientPaint for text color with random orientation and cycle=true
      Paint gradient = createRandomLinearGradient(width, height, textColor1, textColor2);
      g2d.setPaint(gradient);
      
      // Center the text
      FontMetrics metrics = g2d.getFontMetrics();
      int x = (width - metrics.stringWidth(text)) / 2;
      int y = ((height - metrics.getHeight()) / 2) + metrics.getAscent();
      
      // Draw the text with gradient paint
      g2d.drawString(text, x, y);
      
      g2d.dispose();
      
      // Save the file
      String outputFilename = generateOutputFilename(text);
      ImageIO.write(image, "PNG", new File(outputFilename));
      
      System.out.println("Generated: " + outputFilename);
      
      } catch (Exception e) {
      System.err.println("Error: " + e.getMessage());
      e.printStackTrace();
    }
  }
  
  /**
   * Parses a color string in #AARRGGBB or #RRGGBB format
   * @param hex the color hex string
   * @return Color object with specified alpha, red, green, blue components
   */
  private static Color parseColor(String hex) {
    if (hex.startsWith("#")) {
      hex = hex.substring(1);
    }
    
    if (hex.length() == 8) {
      // #AARRGGBB format
      int a = Integer.parseInt(hex.substring(0, 2), 16);
      int r = Integer.parseInt(hex.substring(2, 4), 16);
      int g = Integer.parseInt(hex.substring(4, 6), 16);
      int b = Integer.parseInt(hex.substring(6, 8), 16);
      return new Color(r, g, b, a);
      } else if (hex.length() == 6) {
      // #RRGGBB format (fully opaque if no alpha specified)
      int r = Integer.parseInt(hex.substring(0, 2), 16);
      int g = Integer.parseInt(hex.substring(2, 4), 16);
      int b = Integer.parseInt(hex.substring(4, 6), 16);
      return new Color(r, g, b, 255);
      } else {
      throw new IllegalArgumentException("Invalid color format: " + hex + ". Use #AARRGGBB or #RRGGBB");
    }
  }
  
  /**
   * Generates a safe filename by replacing special characters and spaces
   * @param text the input text
   * @return safe filename with PNG extension
   */
  private static String generateOutputFilename(String text) {
    // Replace Turkish characters with English equivalents using Unicode
    String filename = text.replace(" ", "_")
    .replace("\u0130", "I")  // (capital I with dot)
    .replace("\u0131", "i")  // (dotless i)
    .replace("\u011E", "G")  // (capital G with breve)
    .replace("\u011F", "g")  // (g with breve)
    .replace("\u00DC", "U")  // (capital U with diaeresis)
    .replace("\u00FC", "u")  // (u with diaeresis)
    .replace("\u015E", "S")  // (capital S with cedilla)
    .replace("\u015F", "s")  // (s with cedilla)
    .replace("\u00D6", "O")  // (capital O with diaeresis)
    .replace("\u00F6", "o")  // (o with diaeresis)
    .replace("\u00C7", "C")  // (capital C with cedilla)
    .replace("\u00E7", "c"); // (c with cedilla)
    filename = filename.toLowerCase ();
    return filename + ".png";
  }
  
  /**
   * Creates a GradientPaint with random orientation (vertical, horizontal, diagonal)
   * and cycle=true between two colors.
   * @param width image width
   * @param height image height
   * @param color1 first color
   * @param color2 second color
   * @return GradientPaint object
   */
  private static GradientPaint createRandomGradientPaint(int width, int height, Color color1, Color color2) {
    Random rand = new Random();
    int choice = rand.nextInt(3); // 0=vertical,1=horizontal,2=diagonal
    
    switch (choice) {
      case 0: // vertical gradient
        return new GradientPaint(0, 0, color1, 0, height/2, color2, true);
      case 1: // horizontal gradient
        return new GradientPaint(0, 0, color1, width/2, 0, color2, true);
      case 2: // diagonal gradient (top-left to bottom-right)
        return new GradientPaint(0, 0, color1, width/2, height/2, color2, true);
      default:
        // fallback vertical
      return new GradientPaint(0, 0, color1, 0, height/2, color2, true);
    }
  }
  
  private static Paint createRandomLinearGradient(int width, int height, Color color1, Color color2) {
    Random rand = new Random();
    int choice = rand.nextInt(3);
    Point2D start, end;
    
    switch (choice) {
      case 0: // vertical
        start = new Point2D.Float(0, 0);
      end = new Point2D.Float(0, height);
      break;
      case 1: // horizontal
        start = new Point2D.Float(0, 0);
      end = new Point2D.Float(width, 0);
      break;
      case 2: // diagonal
        start = new Point2D.Float(0, 0);
      end = new Point2D.Float(width, height);
      break;
      default:
        start = new Point2D.Float(0, 0);
      end = new Point2D.Float(0, height);
    }
    
    float[] fractions = {0.0f, 1.0f};
    Color[] colors = {color1, color2};
    
    return new LinearGradientPaint(start, end, fractions, colors, MultipleGradientPaint.CycleMethod.REPEAT);
  }
  
}
