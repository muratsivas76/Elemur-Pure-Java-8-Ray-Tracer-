package net.elena.murat.util;

import java.awt.Color;

/**
 * Utility class for common material-related operations,
 * such as color manipulation.
 */
public class MaterialUtils {
  
  /**
   * Multiplies a Color by a scalar factor.
   * Each RGB component is multiplied by the factor and clamped to the [0, 255] range.
   *
   * @param color The original Color to multiply.
   * @param factor The scalar factor to multiply by.
   * @return A new Color object resulting from the multiplication.
   */
  public static Color multiply(Color color, double factor) {
    int r = (int) (color.getRed() * factor);
    int g = (int) (color.getGreen() * factor);
    int b = (int) (color.getBlue() * factor);
    
    // Clamp values to the valid [0, 255] range
    r = Math.min(255, Math.max(0, r));
    g = Math.min(255, Math.max(0, g));
    b = Math.min(255, Math.max(0, b));
    
    return new Color(r, g, b);
  }
  
  /**
   * Adds two Color objects component-wise.
   * Each RGB component is added and clamped to the [0, 255] range.
   *
   * @param color1 The first Color.
   * @param color2 The second Color.
   * @return A new Color object resulting from the addition.
   */
  public static Color add(Color color1, Color color2) {
    int r = color1.getRed() + color2.getRed();
    int g = color1.getGreen() + color2.getGreen();
    int b = color1.getBlue() + color2.getBlue();
    
    // Clamp values to the valid [0, 255] range
    r = Math.min(255, Math.max(0, r));
    g = Math.min(255, Math.max(0, g));
    b = Math.min(255, Math.max(0, b));
    
    return new Color(r, g, b);
  }
  
  /**
   * Multiplies two Color objects component-wise (e.g., for texture blending).
   * Each RGB component is multiplied, normalized to [0, 1] for multiplication,
   * then scaled back to [0, 255] and clamped.
   *
   * @param color1 The first Color.
   * @param color2 The second Color.
   * @return A new Color object resulting from the component-wise multiplication.
   */
  public static Color blend(Color color1, Color color2) {
    double r = (color1.getRed() / 255.0) * (color2.getRed() / 255.0);
    double g = (color1.getGreen() / 255.0) * (color2.getGreen() / 255.0);
    double b = (color1.getBlue() / 255.0) * (color2.getBlue() / 255.0);
    
    int finalR = (int) (r * 255.0);
    int finalG = (int) (g * 255.0);
    int finalB = (int) (b * 255.0);
    
    // Clamp values to the valid [0, 255] range
    finalR = Math.min(255, Math.max(0, finalR));
    finalG = Math.min(255, Math.max(0, finalG));
    finalB = Math.min(255, Math.max(0, finalB));
    
    return new Color(finalR, finalG, finalB);
  }
  
}
