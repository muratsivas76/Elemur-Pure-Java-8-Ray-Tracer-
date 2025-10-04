import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class SmartBackgroundRemover {
  private static double THRESHOLD = 30.0;
  
  public static void main(String[] args) {
    if (args.length < 3) {
      System.out.println("Usage: java SmartBackgroundRemover <inputFile> <outputFile> <thresholdValueLike10-20-30...>");
      System.exit(1);
    }
    
    try {
      THRESHOLD = Double.parseDouble (args [2]);
      BufferedImage inputImage = ImageIO.read(new File(args[0]));
      BufferedImage outputImage = removeBackground(inputImage);
      ImageIO.write(outputImage, "PNG", new File(args[1]));
      System.out.println("Successfully removed background: " + args[1]);
      } catch (IOException e) {
      System.err.println("Error: " + e.getMessage());
      } catch (NumberFormatException nfe) {
      System.err.println("Error: " + nfe.getMessage());
    }
  }
  
  public static BufferedImage removeBackground(BufferedImage image) {
    int width = image.getWidth();
    int height = image.getHeight();
    BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    
    // 1. Detect background color
    Color backgroundColor = detectBackgroundColor(image);
    System.out.println("Found backgroundColor: " + backgroundColor);
    
    // 2. Similar colors remove
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        int rgb = image.getRGB(x, y);
        Color currentColor = new Color(rgb);
        
        if (isBackgroundColor(currentColor, backgroundColor)) {
          result.setRGB(x, y, 0x00000000); // Fully transparent
          } else {
          result.setRGB(x, y, rgb); // Original color
        }
      }
    }
    
    // 3. Aanti-aliasing
    return applyEdgeSmoothing(result, backgroundColor);
  }
  
  private static Color detectBackgroundColor(BufferedImage image) {
    // Suppose backgroundColor
    int[] cornerPixels = {
      image.getRGB(0, 0),                      // left top
      image.getRGB(image.getWidth() - 1, 0),    // right top
      image.getRGB(0, image.getHeight() - 1),   // left bottom
      image.getRGB(image.getWidth() - 1, image.getHeight() - 1) // right bottom
    };
    
    // Find most used color
    Map<Integer, Integer> colorCount = new HashMap<>();
    for (int pixel : cornerPixels) {
      colorCount.put(pixel, colorCount.getOrDefault(pixel, 0) + 1);
    }
    
    return new Color(Collections.max(colorCount.entrySet(), Map.Entry.comparingByValue()).getKey());
  }
  
  private static boolean isBackgroundColor(Color color, Color bgColor) {
    // RGB distante
    double distance = colorDistance(color, bgColor);
    
    // threshold
    //double threshold = THRESHOLD;//30.0; // less value is more agresive; high value is more religious
    
    return distance < THRESHOLD;
  }
  
  private static double colorDistance(Color c1, Color c2) {
    // Euclidean distance in RGB space
    int rDiff = c1.getRed() - c2.getRed();
    int gDiff = c1.getGreen() - c2.getGreen();
    int bDiff = c1.getBlue() - c2.getBlue();
    
    return Math.sqrt(rDiff * rDiff + gDiff * gDiff + bDiff * bDiff);
  }
  
  private static BufferedImage applyEdgeSmoothing(BufferedImage image, Color bgColor) {
    // Anti alias
    BufferedImage smoothed = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
    
    for (int y = 1; y < image.getHeight() - 1; y++) {
      for (int x = 1; x < image.getWidth() - 1; x++) {
        int currentRGB = image.getRGB(x, y);
        
        if ((currentRGB >>> 24) == 0x00) { // Transparent pixel
          // Opaque pixels around tranparent pixel
          double avgAlpha = calculateAverageAlpha(image, x, y);
          if (avgAlpha > 0) {
            // Half transparent
            Color edgeColor = calculateEdgeColor(image, x, y, bgColor);
            smoothed.setRGB(x, y, edgeColor.getRGBWithAlpha((int)(avgAlpha * 0.7)));
            } else {
            smoothed.setRGB(x, y, currentRGB);
          }
          } else {
          smoothed.setRGB(x, y, currentRGB);
        }
      }
    }
    
    return smoothed;
  }
  
  private static double calculateAverageAlpha(BufferedImage image, int x, int y) {
    int transparentCount = 0;
    int total = 0;
    
    for (int dy = -1; dy <= 1; dy++) {
      for (int dx = -1; dx <= 1; dx++) {
        if (x + dx >= 0 && x + dx < image.getWidth() &&
          y + dy >= 0 && y + dy < image.getHeight()) {
          int rgb = image.getRGB(x + dx, y + dy);
          if ((rgb >>> 24) == 0x00) {
            transparentCount++;
          }
          total++;
        }
      }
    }
    
    return (double) transparentCount / total;
  }
  
  private static Color calculateEdgeColor(BufferedImage image, int x, int y, Color bgColor) {
    int r = 0, g = 0, b = 0;
    int count = 0;
    
    for (int dy = -1; dy <= 1; dy++) {
      for (int dx = -1; dx <= 1; dx++) {
        if (x + dx >= 0 && x + dx < image.getWidth() &&
          y + dy >= 0 && y + dy < image.getHeight()) {
          int rgb = image.getRGB(x + dx, y + dy);
          if ((rgb >>> 24) != 0x00) { // Opaque pixels
            Color color = new Color(rgb);
            r += color.getRed();
            g += color.getGreen();
            b += color.getBlue();
            count++;
          }
        }
      }
    }
    
    if (count > 0) {
      return new Color(r / count, g / count, b / count);
    }
    
    return bgColor; // Fallback
  }
  
  // Helper Color class
  private static class Color {
    private final int r, g, b;
    
    public Color(int rgb) {
      this.r = (rgb >> 16) & 0xFF;
      this.g = (rgb >> 8) & 0xFF;
      this.b = rgb & 0xFF;
    }
    
    public Color(int r, int g, int b) {
      this.r = r;
      this.g = g;
      this.b = b;
    }
    
    public int getRed() { return r; }
    public int getGreen() { return g; }
    public int getBlue() { return b; }
    
    public int getRGBWithAlpha(int alpha) {
      return (alpha << 24) | (r << 16) | (g << 8) | b;
    }
    
    @Override
    public String toString() {
      return String.format("RGB(%d, %d, %d)", r, g, b);
    }
  }
  
}
