package net.elena.murat.util;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * Utility class for resizing BufferedImages while maintaining aspect ratio.
 */
public class ResizeImage {
  
  /**
   * Resizes a given BufferedImage to new dimensions (newWidth, newHeight)
   * while preserving its aspect ratio. The image will be scaled to fit
   * within the new dimensions and centered, potentially adding padding
   * (letterboxing or pillarboxing) if the aspect ratios differ.
   *
   * @param src The source BufferedImage to be resized.
   * @param newWidth The desired width for the resized image.
   * @param newHeight The desired height for the resized image.
   * @param backgroundColor The background color for padding areas. Pass null for transparent background (if image type supports it).
   * @return A new BufferedImage with the specified dimensions, containing the scaled source image.
   */
  public static BufferedImage getResizedImage(BufferedImage src, int newWidth, int newHeight, Color backgroundColor) {
    // Determine the type for the new buffered image.
    // Use TYPE_INT_ARGB if a transparent background is desired (backgroundColor is null),
    // otherwise TYPE_INT_RGB for opaque images.
    int imageType = BufferedImage.TYPE_INT_RGB;
    if (backgroundColor == null) {
      imageType = BufferedImage.TYPE_INT_ARGB;
    }
    
    BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, imageType);
    Graphics2D g2d = resizedImage.createGraphics();
    
    // Set rendering hints for high-quality scaling
    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    
    // Fill background if a color is provided
    if (backgroundColor != null) {
      g2d.setColor(backgroundColor);
      g2d.fillRect(0, 0, newWidth, newHeight);
    }
    
    // Calculate scaling factors and drawing dimensions to preserve aspect ratio
    double originalWidth = src.getWidth();
    double originalHeight = src.getHeight();
    
    double scaleX = newWidth / originalWidth;
    double scaleY = newHeight / originalHeight;
    
    // Use the smaller scale factor to ensure the entire image fits within the new bounds
    double scale = Math.min(scaleX, scaleY);
    
    int scaledWidth = (int) (originalWidth * scale);
    int scaledHeight = (int) (originalHeight * scale);
    
    // Calculate position to center the scaled image on the new canvas
    int x = (newWidth - scaledWidth) / 2;
    int y = (newHeight - scaledHeight) / 2;
    
    // Draw the scaled image onto the new buffered image
    g2d.drawImage(src, x, y, scaledWidth, scaledHeight, null);
    g2d.dispose(); // Release Graphics2D resources
    
    return resizedImage;
  }
  
  /**
   * Overload for getResizedImage that defaults to a black background for padding.
   * @param src The source BufferedImage to be resized.
   * @param newWidth The desired width for the resized image.
   * @param newHeight The desired height for the resized image.
   * @return A new BufferedImage with the specified dimensions, containing the scaled source image.
   */
  public static BufferedImage getResizedImage(BufferedImage src, int newWidth, int newHeight) {
    return getResizedImage(src, newWidth, newHeight, Color.BLACK); // Default to black background
  }
}
