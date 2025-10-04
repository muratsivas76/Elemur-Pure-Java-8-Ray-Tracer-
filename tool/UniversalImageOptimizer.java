import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * TransparentBackgroundConverter - Converts images with transparent backgrounds
 * to have high-quality transparent PNGs that display correctly in galleries and viewers.
 * Preserves transparency while ensuring optimal visual quality.
 *
 * Usage: java TransparentBackgroundConverter <input> <output.png>
 * Example: java TransparentBackgroundConverter input.png output.png
 * Example: java TransparentBackgroundConverter input.jpg output.png
 */
public class UniversalImageOptimizer {
  
  // Alpha threshold for considering a pixel transparent (adjust as needed)
  private static final int ALPHA_THRESHOLD = 10;
  
  // Background color to replace fully transparent areas (black for better gallery viewing)
  private static final int TRANSPARENT_BG_COLOR = 0x00000000; // Fully transparent black
  
  public static void main(String[] args) {
    if (args.length != 2) {
      printUsage();
      System.exit(1);
    }
    
    String inputPath = args[0];
    String outputPath = args[1];
    
    try {
      System.out.println("Creating gallery-quality transparent image...");
      System.out.println("Input: " + inputPath);
      System.out.println("Output: " + outputPath);
      
      // Load source image
      BufferedImage sourceImage = loadImage(inputPath);
      if (sourceImage == null) {
        System.exit(1);
      }
      
      // Create high-quality transparent image
      BufferedImage transparentImage = createTransparentImage(sourceImage);
      
      // Save as PNG to preserve transparency
      boolean success = saveAsPNG(transparentImage, outputPath);
      
      if (success) {
        printSuccessStats(sourceImage, transparentImage, outputPath);
        System.out.println("Image ready for gallery viewing with perfect transparency!");
        } else {
        System.err.println("Failed to save transparent image");
        System.exit(1);
      }
      
      } catch (IOException e) {
      System.err.println("Error: " + e.getMessage());
      e.printStackTrace();
      System.exit(1);
    }
  }
  
  /**
   * Prints usage information
   */
  private static void printUsage() {
    System.out.println("Transparent Background Converter");
    System.out.println("================================");
    System.out.println("Creates high-quality PNG images with proper transparency for gallery viewing.");
    System.out.println("Transparent areas will appear black in viewers, colored areas will be preserved.");
    System.out.println();
    System.out.println("Usage: java UniversalImageOptimizer <input> <output.png>");
    System.out.println();
    System.out.println("Examples:");
    System.out.println("  java UniversalImageOptimizer texture.png gallery_texture.png");
    System.out.println("  java UniversalImageOptimizer input.jpg output.png");
    System.out.println("  java UniversalImageOptimizer image_with_alpha.png final_result.png");
  }
  
  /**
   * Loads an image from file path
   */
  private static BufferedImage loadImage(String filePath) throws IOException {
    File file = new File(filePath);
    if (!file.exists()) {
      System.err.println("File not found: " + filePath);
      return null;
    }
    
    BufferedImage image = ImageIO.read(file);
    if (image == null) {
      System.err.println("Unsupported image format or corrupt file: " + filePath);
      return null;
    }
    
    System.out.println(" Loaded image: " + image.getWidth() + "x" + image.getHeight() +
    " (type: " + getImageTypeName(image.getType()) + ")");
    return image;
  }
  
  /**
   * Creates a high-quality transparent image for gallery viewing
   */
  private static BufferedImage createTransparentImage(BufferedImage source) {
    int width = source.getWidth();
    int height = source.getHeight();
    
    // Always use ARGB to preserve transparency
    BufferedImage transparentImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    
    // Process all pixels to create the gallery effect
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        int rgb = source.getRGB(x, y);
        int processedRgb = processPixelForGallery(rgb);
        transparentImage.setRGB(x, y, processedRgb);
      }
    }
    
    return transparentImage;
  }
  
  /**
   * Processes pixel for gallery-quality transparent effect
   * - Fully transparent pixels become black transparent
   * - Colored pixels retain their colors with original alpha
   * - Semi-transparent pixels are preserved as-is
   */
  private static int processPixelForGallery(int rgb) {
    int alpha = (rgb >> 24) & 0xFF;
    int red = (rgb >> 16) & 0xFF;
    int green = (rgb >> 8) & 0xFF;
    int blue = rgb & 0xFF;
    
    // If pixel is fully or mostly transparent, make it black transparent
    if (alpha < ALPHA_THRESHOLD) {
      return TRANSPARENT_BG_COLOR; // Fully transparent black
    }
    
    // If pixel has color, preserve it with original alpha
    return (alpha << 24) | (red << 16) | (green << 8) | blue;
  }
  
  /**
   * Saves image as PNG with transparency support
   */
  private static boolean saveAsPNG(BufferedImage image, String filePath) throws IOException {
    File outputFile = new File(filePath);
    
    // Ensure output has .png extension
    if (!outputFile.getName().toLowerCase().endsWith(".png")) {
      outputFile = new File(outputFile.getParent(),
      outputFile.getName() + ".png");
    }
    
    // Create parent directories if they don't exist
    File parentDir = outputFile.getParentFile();
    if (parentDir != null && !parentDir.exists()) {
      parentDir.mkdirs();
    }
    
    return ImageIO.write(image, "png", outputFile);
  }
  
  /**
   * Returns human-readable name for BufferedImage type
   */
  private static String getImageTypeName(int type) {
    switch (type) {
      case BufferedImage.TYPE_INT_RGB: return "RGB (no alpha)";
      case BufferedImage.TYPE_INT_ARGB: return "ARGB (with alpha)";
      case BufferedImage.TYPE_4BYTE_ABGR: return "ABGR";
      case BufferedImage.TYPE_3BYTE_BGR: return "BGR";
      case BufferedImage.TYPE_BYTE_GRAY: return "GRAYSCALE";
      case BufferedImage.TYPE_INT_ARGB_PRE: return "ARGB_PRE";
      default: return "UNKNOWN (" + type + ")";
    }
  }
  
  /**
   * Prints success statistics after conversion
   */
  private static void printSuccessStats(BufferedImage source, BufferedImage result, String outputPath) {
    File outputFile = new File(outputPath);
    long fileSize = outputFile.length();
    
    System.out.println("\n Conversion completed successfully!");
    System.out.println("Gallery Quality Report:");
    System.out.println("   Dimensions: " + result.getWidth() + "x" + result.getHeight());
    System.out.println("   Format: PNG with Alpha Channel");
    System.out.println("   File size: " + (fileSize / 1024) + " KB");
    System.out.println("   Transparency: ENABLED");
    System.out.println("   Gallery effect: Transparent areas will appear black");
    System.out.println("   Color preservation: All colored pixels maintained");
    System.out.println();
    System.out.println(" Image is now perfect for gallery viewing!");
    System.out.println("   - Transparent background: Shows as black in viewers");
    System.out.println("   - Colored elements: Preserved with original quality");
    System.out.println("   - Alpha channel: Fully functional for texture mapping");
  }
  
}
