import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.List;

public class WriteTextToImage {
  
  public static void main(String[] args) {
    if (args.length < 1) {
      System.out.println("Usage: java WriteTextToImage [input.jpg output.jpg] [STRING: \"text\" x y color fontSpec] [IMAGE: imagePath,x,y,scale] ...");
      System.out.println("Example: java WriteTextToImage [input.jpg output.jpg] [STRING: \"Murat_inan\" 100 50 #FF0000FF Arial,1,10] [IMAGE: overlay.png,50,60,1.2] [STRING: \"Hello\" 200 100 #00FF00FF Times_New_Roman,0,15]");
      System.out.println("bgColor format: #AARRGGBB (Alpha, Red, Green, Blue)");
      System.out.println("STRING format: [STRING: \"text\" x y color fontName,style,size]");
      System.out.println("IMAGE format: [IMAGE: imagePath,x,y,scale]");
      return;
    }
    
    try {
      // Parse all arguments that are enclosed in brackets
      List<String[]> argumentGroups = parseBracketArguments(args);
      
      if (argumentGroups.size() < 1) {
        System.out.println("No valid argument groups found. Use format: [group1] [group2] ...");
        return;
      }
      
      // First group should be [input.jpg output.jpg bgColor]
      String[] ioGroup = argumentGroups.get(0);
      if (ioGroup.length != 2) {
        System.out.println("First group must be: [input.jpg output.jpg]");
        return;
      }
      
      String srcImagePath = ioGroup[0];
      String dstImagePath = ioGroup[1];
      //String bgColorHex = ioGroup[2];
      
      // Create a new image or load existing one
      BufferedImage image;
      if (new File(srcImagePath).exists()) {
        // Load existing image
        image = ImageIO.read(new File(srcImagePath));
        if (image == null) {
          System.out.println("Could not load: " + srcImagePath);
          return;
        }
        } else {
        // Create new image
        System.out.println("Source image not found, creating new image.");
        image = createNewImage(800, 600); // Default size
      }
      
      // Graphics2D object create
      Graphics2D g2d = image.createGraphics();
      
      // Anti-aliasing
      g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
      RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON);
      
      // Process all operation groups (starting from index 1)
      for (int i = 1; i < argumentGroups.size(); i++) {
        String[] operationGroup = argumentGroups.get(i);
        
        if (operationGroup.length < 1) {
          System.out.println("Empty operation group, skipping...");
          continue;
        }
        
        String operationType = operationGroup[0];
        
        try {
          if ("STRING:".equals(operationType)) {
            // STRING operation: [STRING: "text" x y color fontSpec]
            if (operationGroup.length != 6) {
              System.out.println("STRING operation must have 6 elements: [STRING: \"text\" x y color fontSpec]");
              continue;
            }
            
            String text = operationGroup[1].replace("_", " ");
            int x = Integer.parseInt(operationGroup[2]);
            int y = Integer.parseInt(operationGroup[3]);
            String colorHex = operationGroup[4];
            String fontSpec = operationGroup[5];
            
            // Set color
            Color color = hexToColor(colorHex);
            g2d.setColor(color);
            
            // Font
            Font font = parseFontSpec(fontSpec);
            g2d.setFont(font);
            
            // Draw text
            g2d.drawString(text, x, y);
            
            } else if ("IMAGE:".equals(operationType)) {
            // IMAGE operation: [IMAGE: imagePath,x,y,scale]
            if (operationGroup.length != 2) {
              System.out.println("IMAGE operation must have 2 elements: [IMAGE: imagePath,x,y,scale]");
              continue;
            }
            
            String[] imageParts = operationGroup[1].split(",");
            if (imageParts.length != 4) {
              System.out.println("IMAGE parameters must be: imagePath,x,y,scale");
              continue;
            }
            
            String overlayPath = imageParts[0];
            int overlayX = Integer.parseInt(imageParts[1]);
            int overlayY = Integer.parseInt(imageParts[2]);
            double scale = Double.parseDouble(imageParts[3]);
            
            drawOverlayImage(g2d, overlayPath, overlayX, overlayY, scale);
            
            } else {
            System.out.println("Unknown operation type: " + operationType);
          }
          } catch (Exception e) {
          System.out.println("Error processing operation group " + i + ": " + e.getMessage());
        }
      }
      
      // Graphics dispose release
      g2d.dispose();
      
      // Save image to destination path
      ImageIO.write(image, getFormatName(dstImagePath), new File(dstImagePath));
      
      System.out.println("Successfully processed image. Output: " + dstImagePath);
      
      } catch (NumberFormatException e) {
      System.out.println("Number format exception: " + e.getMessage());
      } catch (IOException e) {
      System.out.println("IO Error: " + e.getMessage());
      } catch (IllegalArgumentException e) {
      System.out.println("Illegal argument: " + e.getMessage());
    }
  }
  
  /**
   * Create a new image with specified background color
   */
  private static BufferedImage createNewImage(int width, int height/*, String bgColorHex*/) {
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2d = image.createGraphics();
    
    // Fill with background color
    //Color bgColor = hexToColor(bgColorHex);
    //g2d.setColor(bgColor);
    //g2d.fillRect(0, 0, width, height);
    
    g2d.dispose();
    return image;
  }
  
  /**
   * Parse arguments that are enclosed in square brackets
   */
  private static List<String[]> parseBracketArguments(String[] args) {
    List<String[]> argumentGroups = new ArrayList<>();
    List<String> currentGroup = new ArrayList<>();
    boolean inGroup = false;
    
    for (String arg : args) {
      if (arg.startsWith("[")) {
        inGroup = true;
        // Remove opening bracket and add to current group
        String cleanArg = arg.substring(1);
        if (!cleanArg.isEmpty()) {
          currentGroup.add(cleanArg);
        }
        } else if (arg.endsWith("]")) {
        inGroup = false;
        // Remove closing bracket and add to current group
        String cleanArg = arg.substring(0, arg.length() - 1);
        if (!cleanArg.isEmpty()) {
          currentGroup.add(cleanArg);
        }
        // Add completed group to the list
        if (!currentGroup.isEmpty()) {
          argumentGroups.add(currentGroup.toArray(new String[0]));
          currentGroup.clear();
        }
        } else if (inGroup) {
        currentGroup.add(arg);
      }
    }
    
    // Handle case where there's an unclosed group
    if (!currentGroup.isEmpty()) {
      argumentGroups.add(currentGroup.toArray(new String[0]));
    }
    
    return argumentGroups;
  }
  
  /**
   * Draw overlay image with scaling
   */
  private static void drawOverlayImage(Graphics2D g2d, String overlayPath,
    int x, int y, double scale) throws IOException {
    BufferedImage overlayImage = ImageIO.read(new File(overlayPath));
    if (overlayImage == null) {
      System.out.println("Could not load overlay image: " + overlayPath);
      return;
    }
    
    // Calculate scaled dimensions
    int scaledWidth = (int) (overlayImage.getWidth() * scale);
    int scaledHeight = (int) (overlayImage.getHeight() * scale);
    
    // Draw the scaled overlay image
    g2d.drawImage(overlayImage, x, y, scaledWidth, scaledHeight, null);
  }
  
  /**
   * Convert hex string to Color object
   */
  private static Color hexToColor(String hex) {
    if (hex.startsWith("#")) {
      hex = hex.substring(1);
    }
    
    if (hex.length() != 8) {
      throw new IllegalArgumentException("Color format must be #AARRGGBB");
    }
    
    try {
      int a = Integer.parseInt(hex.substring(0, 2), 16);
      int r = Integer.parseInt(hex.substring(2, 4), 16);
      int g = Integer.parseInt(hex.substring(4, 6), 16);
      int b = Integer.parseInt(hex.substring(6, 8), 16);
      
      return new Color(r, g, b, a);
      } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid color code: " + hex);
    }
  }
  
  /**
   * Parse font specification string
   */
  private static Font parseFontSpec(String fontSpec) {
    String[] parts = fontSpec.split(",");
    if (parts.length != 3) {
      throw new IllegalArgumentException("Font format: fontName,style,size");
    }
    
    String fontName = parts[0].replaceAll("_", " ");
    int style;
    int size;
    
    try {
      style = Integer.parseInt(parts[1]);
      size = Integer.parseInt(parts[2]);
      } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Font style and size must be numbers!");
    }
    
    // Check style value
    if (style < 0 || style > 3) {
      throw new IllegalArgumentException("Font style must be between 0-3");
    }
    
    return new Font(fontName, style, size);
  }
  
  /**
   * Get image format name from file extension
   */
  private static String getFormatName(String filePath) {
    String extension = filePath.substring(filePath.lastIndexOf('.') + 1).toLowerCase();
    switch (extension) {
      case "jpg": case "jpeg": return "JPEG";
      case "png": return "PNG";
      case "gif": return "GIF";
      case "bmp": return "BMP";
      default: return "PNG";
    }
  }
  
}
