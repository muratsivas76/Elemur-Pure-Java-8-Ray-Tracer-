import java.awt.Color;
import java.lang.reflect.Field;
import java.util.Arrays;

public class ColorParserTest {
  
  public static void main(String[] args) {
    // Test verileri
    String floatColor = "1.0F, 0.0F, 0.0F, 1F";
    String hexColor = "#FF0000";
    
    System.out.println("=== TEST BASLIYOR ===");
    System.out.println("Float Input: " + floatColor);
    System.out.println("Hex Input: " + hexColor);
    System.out.println();
    
    // Parse işlemleri
    Color color1 = parseColor(floatColor);
    Color color2 = parseColor(hexColor);
    
    System.out.println();
    System.out.println("=== SONUCLAR ===");
    System.out.println("Float Sonuç: " + color1 + " (RGB: " + color1.getRed() + "," + color1.getGreen() + "," + color1.getBlue() + ", Alpha: " + color1.getAlpha() + ")");
    System.out.println("Hex Sonuç: " + color2 + " (RGB: " + color2.getRed() + "," + color2.getGreen() + "," + color2.getBlue() + ", Alpha: " + color2.getAlpha() + ")");
    System.out.println("Eşit mi? " + color1.equals(color2));
    System.out.println("RGB Değerleri: " + color1.getRGB() + " vs " + color2.getRGB());
    System.out.println("Alpha Eşit mi? " + (color1.getAlpha() == color2.getAlpha()));
  }
  
  private static Color parseColor(String s) {
    if (s == null) return Color.WHITE;
    
    s = s.trim()
    .replaceAll(";", "")  // Remove any semicolons
    .replaceAll("\\s+", ""); // Remove whitespace
    
    Color cc=parseColorForExternal (s);
    
    return cc;
  }
  
  /**
   * From string to color
   * Supported formats:
   * - Hex: #FF0000, #FF00FF00
   * - Integer RGB: 255,0,0...
   * - Float RGB: 1.0f,0.5f,0.0f...
   */
  private static Color parseColorForExternal(String colorStr) {
    if (colorStr == null || colorStr.trim().isEmpty()) {
      return null;
    }
    
    colorStr = colorStr.trim();
    
    try {
      // Hex format: #FF0000 veya #FF00FF00
      if (colorStr.startsWith("#")) {
        return hexToColor (colorStr);
      }
      
      // Float RGB format: 1.0f,0.5f,0.0f
      if (colorStr.toLowerCase().contains("f")) {
        String[] parts = colorStr.split(",");
        if (parts.length >= 3) {
          float r = Float.parseFloat(parts[0].trim());//.replace("f", ""));
          float g = Float.parseFloat(parts[1].trim());//.replace("f", ""));
          float b = Float.parseFloat(parts[2].trim());//.replace("f", ""));
          
          if (parts.length == 4) {
            float a = Float.parseFloat(parts[3].trim());//.replace("f", ""));
            return new Color(r, g, b, a);
          }
          return new Color(r, g, b);
        }
      }
      
      // Integer RGB format: 255,0,0
      if (colorStr.contains(",")) {
        String[] parts = colorStr.split(",");
        if (parts.length >= 3) {
          int r = Integer.parseInt(parts[0].trim());
          int g = Integer.parseInt(parts[1].trim());
          int b = Integer.parseInt(parts[2].trim());
          
          if (parts.length == 4) {
            int a = Integer.parseInt(parts[3].trim());
            return new Color(r, g, b, a);
          }
          return new Color(r, g, b);
        }
      }
      
      // Named colors (red, blue, green, etc.)
      try {
        Field field = Color.class.getField(colorStr.toUpperCase());
        return (Color) field.get(null);
        } catch (Exception e) {
        // Named color not found
      }
      
      } catch (Exception e) {
      System.err.println("Color parsing error for '" + colorStr + "': " + e.getMessage());
    }
    
    return null;
  }
  
  private static Color hexToColor(String hex) {
    hex = hex.substring(1); //Remove #
    
    int hexlen = hex.length ();
    
    if ((hexlen != 6) && (hexlen != 8)) {
      throw new IllegalArgumentException("Color format must be #RRGGBB||#RRGGBBAA");
    }
    
    try {
      if (hexlen == 8) {
        int r = Integer.parseInt(hex.substring(0, 2), 16);
        int g = Integer.parseInt(hex.substring(2, 4), 16);
        int b = Integer.parseInt(hex.substring(4, 6), 16);
        int a = Integer.parseInt(hex.substring(6, 8), 16);
        
        return new Color(r, g, b, a);
        } else if (hexlen == 6) {
        int r = Integer.parseInt(hex.substring(0, 2), 16);
        int g = Integer.parseInt(hex.substring(2, 4), 16);
        int b = Integer.parseInt(hex.substring(4, 6), 16);
        
        return new Color(r, g, b);
        } else {
        System.out.println ("Return BLACK for an error...");
        return Color.BLACK;
      }
      } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid color code: " + hex);
    }
  }
  
}
