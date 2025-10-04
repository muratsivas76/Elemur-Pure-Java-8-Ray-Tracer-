import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class NorwegianWordGenerator {
  
  public static void main(String[] args) {
    // Example usage
    if (args.length < 7) {
      System.out.println("Usage: java NorwegianWordGenerator <width> <height> <word> <font> <fgColor> <shadowColor> <shadowDepth>");
      System.out.println("\nExample: java NorwegianWordGenerator 800 400 BRO/D_EKMEK Arial_Black,1,40 #FF0000FF #00000080 4");
      
      StringBuffer result = new StringBuffer();
      result.append("\nConversion:\nAE: \u00C6; ");
      result.append("O/: \u00D8; ");
      result.append("A0: \u00C5; ");
      result.append("ae: \u00E6; ");
      result.append("o/: \u00F8; ");
      result.append("a0: \u00E5");
      System.out.println (result.toString ());
      
      System.exit(1);
    }
    
    try {
      int width = Integer.parseInt(args[0]);
      int height = Integer.parseInt(args[1]);
      String word = args[2].replaceAll ("_", " ");
      word = convertToNorwegianText (word);
      String fontParam = args[3];
      String fgColorHex = args[4];
      String shadowColorHex = args[5];
      int shadowDepth = Integer.parseInt(args[6]);
      
      generateImage(width, height, word, fontParam, fgColorHex, shadowColorHex, shadowDepth);
      System.out.println("Image successfully created: output.png");
      
      } catch (Exception e) {
      System.err.println("Error: " + e.getMessage());
      e.printStackTrace();
    }
  }
  
  /**
   * Generates an image with the specified Norwegian word and 3D effect
   * @param width Image width in pixels
   * @param height Image height in pixels
   * @param word The Norwegian word to render
   * @param fontParam Font parameters in format "name,style,size"
   * @param fgColorHex Foreground color in #RRGGBBAA format
   * @param shadowColorHex Shadow color in #RRGGBBAA format
   * @param shadowDepth Depth of the 3D shadow effect
   */
  public static void generateImage(int width, int height, String word,
    String fontParam, String fgColorHex,
    String shadowColorHex, int shadowDepth) {
    try {
      // Parse font parameters
      String[] fontParts = fontParam.split(",");
      String fontName = fontParts[0];
      fontName = fontName.replaceAll ("_", " ");
      int fontStyle = Integer.parseInt(fontParts[1]);
      int fontSize = Integer.parseInt(fontParts[2]);
      
      // Parse colors
      Color fgColor = hexToColor(fgColorHex);
      Color shadowColor = hexToColor(shadowColorHex);
      
      // Create image with transparent background
      BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
      Graphics2D g2d = image.createGraphics();
      
      // Enable anti-aliasing for better text quality
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      
      // Set font
      Font font = new Font(fontName, fontStyle, fontSize);
      g2d.setFont(font);
      
      // Calculate text position for center alignment
      FontMetrics metrics = g2d.getFontMetrics();
      int textWidth = metrics.stringWidth(word);
      int textHeight = metrics.getHeight();
      
      int x = (width - textWidth) / 2;
      int y = (height - textHeight) / 2 + metrics.getAscent();
      
      // Create 3D shadow effect
      for (int i = shadowDepth; i > 0; i--) {
        float alpha = 0.1f + (i / (float) shadowDepth) * 0.3f;
        Color shadowWithAlpha = new Color(
          shadowColor.getRed(),
          shadowColor.getGreen(),
          shadowColor.getBlue(),
          (int) (alpha * shadowColor.getAlpha())
        );
        
        g2d.setColor(shadowWithAlpha);
        g2d.drawString(word, x + i, y + i);
      }
      
      // Draw the main text
      g2d.setColor(fgColor);
      g2d.drawString(word, x, y);
      
      // Clean up
      g2d.dispose();
      
      // Save the image
      File output = new File("output.png");
      ImageIO.write(image, "PNG", output);
      
      } catch (Exception e) {
      System.err.println("Error generating image: " + e.getMessage());
      e.printStackTrace();
    }
  }
  
  /**
   * Converts a hexadecimal color string to a Color object
   * @param hex Color in #RRGGBB or #RRGGBBAA format
   * @return Color object
   */
  private static Color hexToColor(String hex) {
    if (hex.startsWith("#")) {
      hex = hex.substring(1);
    }
    
    if (hex.length() == 6) {
      // No alpha channel specified, use fully opaque
      return new Color(
        Integer.valueOf(hex.substring(0, 2), 16),
        Integer.valueOf(hex.substring(2, 4), 16),
        Integer.valueOf(hex.substring(4, 6), 16)
      );
      } else if (hex.length() == 8) {
      // With alpha channel
      return new Color(
        Integer.valueOf(hex.substring(0, 2), 16),
        Integer.valueOf(hex.substring(2, 4), 16),
        Integer.valueOf(hex.substring(4, 6), 16),
        Integer.valueOf(hex.substring(6, 8), 16)
      );
      } else {
      throw new IllegalArgumentException("Invalid color format: " + hex);
    }
  }
  
  /**
   * Converts English character sequences to Norwegian special characters
   * Converts: AE->Æ (\\u00C6), O/->Ø (\\u00D8), A0->Å (\\u00C5)
   * Also handles lowercase conversions: ae->æ (\\u00E6), o/->ø (\\u00F8), a0->å (\\u00E5)
   * @param input The text to convert to Norwegian characters
   * @return Text with Norwegian characters properly encoded
   */
  public static String convertToNorwegianText(String input) {
    if (input == null || input.isEmpty()) {
      return input;
    }
    
    // Replace character sequences with Norwegian equivalents
    String result = input;
    
    // Uppercase conversions
    result = result.replace("AE", "\u00C6");  // Æ
    result = result.replace("O/", "\u00D8");  // Ø
    result = result.replace("A0", "\u00C5");  // Å
    
    // Lowercase conversions
    result = result.replace("ae", "\u00E6");  // æ
    result = result.replace("o/", "\u00F8");  // ø
    result = result.replace("a0", "\u00E5");  // å
    
    return result;
  }
  
}
