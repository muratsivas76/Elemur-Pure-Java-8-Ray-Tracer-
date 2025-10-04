package net.elena.murat.util;

import java.awt.Color;

/**
 * Utility class for RGB color operations.
 * All methods work exclusively with RGB components (alpha channel is ignored).
 * Results are always fully opaque (alpha=255).
 */
public final class ColorUtil {
  
  // Color constants (all fully opaque)
  public static final Color BLACK = new Color(0, 0, 0);
  public static final Color WHITE = new Color(255, 255, 255);
  public static final Color RED = new Color(255, 0, 0);
  public static final Color GREEN = new Color(0, 255, 0);
  public static final Color BLUE = new Color(0, 0, 255);
  
  /**
   * Linear interpolation between two RGB colors (t=0: c1, t=1: c2)
   * Only RGB components are interpolated, result is fully opaque
   */
  public static Color lerp(Color c1, Color c2, float t) {
    t = clamp(t, 0.0f, 1.0f);
    return new Color(
      c1.getRed() + (int)((c2.getRed() - c1.getRed()) * t),
      c1.getGreen() + (int)((c2.getGreen() - c1.getGreen()) * t),
      c1.getBlue() + (int)((c2.getBlue() - c1.getBlue()) * t)
    );
  }
  
  /**
   * Component-wise multiplication of two RGB colors
   * Result is always fully opaque (alpha=255)
   */
  public static Color multiply(Color c1, Color c2) {
    return new Color(
      (c1.getRed() * c2.getRed()) / 255,
      (c1.getGreen() * c2.getGreen()) / 255,
      (c1.getBlue() * c2.getBlue()) / 255
    );
  }
  
  /**
   * Multiplies RGB color by a scalar factor
   * Result is always fully opaque (alpha=255)
   */
  public static Color multiply(Color c, float scalar) {
    scalar = Math.max(0.0f, scalar);
    return new Color(
      clamp((int)(c.getRed() * scalar)),
      clamp((int)(c.getGreen() * scalar)),
      clamp((int)(c.getBlue() * scalar))
    );
  }
  
  /**
   * Reinhard Tone Mapping Operator
   */
  public static float reinhardToneMap(float color) {
    return color / (1.0f + color);
  }
  
  public static Color gammaCorrect(Color color, float gamma) {
    float invGamma = 1.0f / gamma;
    float r = (float) Math.pow(color.getRed() / 255.0, invGamma);
    float g = (float) Math.pow(color.getGreen() / 255.0, invGamma);
    float b = (float) Math.pow(color.getBlue() / 255.0, invGamma);
    float a = color.getAlpha() / 255.0f;
    
    // Garanti için clamp (NaN veya aşırı değerlere karşı)
    r = Math.max(0.0f, Math.min(1.0f, r));
    g = Math.max(0.0f, Math.min(1.0f, g));
    b = Math.max(0.0f, Math.min(1.0f, b));
    a = Math.max(0.0f, Math.min(1.0f, a));
    
    return new Color(r, g, b, a);
  }
  
  private static float linearToSrgb(float linear) {
    if (linear <= 0.0031308f) {
      return linear * 12.92f;
      } else {
      return (float) (1.055f * Math.pow(linear, 1.0/2.4) - 0.055f);
    }
  }
  
  /**
   * sRGB to linear conversion for a single channel
   */
  public static float srgbToLinear(float srgb) {
    if (srgb <= 0.04045f) {
      return srgb / 12.92f;
      } else {
      return (float) Math.pow((srgb + 0.055f) / 1.055f, 2.4f);
    }
  }
  
  public static Color sRGBToLinear(Color srgbColor, float gamma) {
    if (gamma == 1f) return srgbColor;
    
    float r = srgbColor.getRed() / 255.0f;
    float g = srgbColor.getGreen() / 255.0f;
    float b = srgbColor.getBlue() / 255.0f;
    float a = srgbColor.getAlpha() / 255.0f;
    
    r = clamp(r, 0.0f, 1.0f);
    g = clamp(g, 0.0f, 1.0f);
    b = clamp(b, 0.0f, 1.0f);
    
    r = (r <= 0.04045f) ? (r / 12.92f) : (float) Math.pow((r + 0.055f) / 1.055f, gamma);
    g = (g <= 0.04045f) ? (g / 12.92f) : (float) Math.pow((g + 0.055f) / 1.055f, gamma);
    b = (b <= 0.04045f) ? (b / 12.92f) : (float) Math.pow((b + 0.055f) / 1.055f, gamma);
    
    return new Color(r, g, b, a);
  }
  
  public static Color sRGBToLinearExtra(Color base, float gamma) {
    if (gamma == 1f) return base;
    
    if (gamma <= 0) gamma = 2.2f;
    
    float r = base.getRed() / 255.0f;
    float g = base.getGreen() / 255.0f;
    float b = base.getBlue() / 255.0f;
    float a = base.getAlpha() / 255.0f;
    
    r = (float) Math.pow(r, 1.0f / gamma);
    g = (float) Math.pow(g, 1.0f / gamma);
    b = (float) Math.pow(b, 1.0f / gamma);
    
    int red = (int) (Math.min(Math.max(r * 255, 0), 255));
    int green = (int) (Math.min(Math.max(g * 255, 0), 255));
    int blue = (int) (Math.min(Math.max(b * 255, 0), 255));
    int alpha = (int) (a * 255);
    
    return new Color(red, green, blue, alpha);
  }
  
  /**
   * Apply exposure and tone mapping to linear color
   */
  public static Color applyExposureAndToneMapping(Color linearColor, float exposure) {
    float r = clamp(linearColor.getRed(), 0.0f, 1.0f);
    float g = clamp(linearColor.getGreen(), 0.0f, 1.0f);
    float b = clamp(linearColor.getBlue(), 0.0f, 1.0f);
    float a = clamp(linearColor.getAlpha(), 0.0f, 1.0f);
    
    // Exposure adjustment
    r *= exposure;
    g *= exposure;
    b *= exposure;
    
    // ACES filmic tone mapping
    r = clamp(acesToneMap(r), 0.0f, 1.0f);
    g = clamp(acesToneMap(g), 0.0f, 1.0f);
    b = clamp(acesToneMap(b), 0.0f, 1.0f);
    
    return new Color(r, g, b, a);
  }
  
  /**
   * Apply tone mapping to a linear color
   */
  public static Color applyToneMapping(Color linearColor, float exposure) {
    float r = clamp(linearColor.getRed() / 255.0f, 0.0f, 1.0f);
    float g = clamp(linearColor.getGreen() / 255.0f, 0.0f, 1.0f);
    float b = clamp(linearColor.getBlue() / 255.0f, 0.0f, 1.0f);
    float a = clamp(linearColor.getAlpha() / 255.0f, 0.0f, 1.0f);
    
    // Exposure adjustment
    r *= exposure;
    g *= exposure;
    b *= exposure;
    
    // Reinhard tone mapping
    r = clamp(reinhardToneMap(r), 0.0f, 1.0f);
    g = clamp(reinhardToneMap(g), 0.0f, 1.0f);
    b = clamp(reinhardToneMap(b), 0.0f, 1.0f);
    
    return new Color(r, g, b, a);
  }
  
  public static Color linearToSRGB(Color linearColor) {
    float r = clamp(linearColor.getRed(), 0.0f, 1.0f);
    float g = clamp(linearColor.getGreen(), 0.0f, 1.0f);
    float b = clamp(linearColor.getBlue(), 0.0f, 1.0f);
    float a = clamp(linearColor.getAlpha(), 0.0f, 1.0f);
    
    // Linear to sRGB conversion
    r = clamp(linearToSrgb(r), 0.0f, 1.0f);
    g = clamp(linearToSrgb(g), 0.0f, 1.0f);
    b = clamp(linearToSrgb(b), 0.0f, 1.0f);
    
    return new Color(r, g, b, a);
  }
  
  private static float acesToneMap(float x) {
    float a = 2.51f;
    float b = 0.03f;
    float c = 2.43f;
    float d = 0.59f;
    float e = 0.14f;
    return Math.max(0.0f, Math.min(1.0f, (x * (a * x + b)) / (x * (c * x + d) + e)));
  }
  
  public static Color enhanceColorSaturation(Color color, float saturationFactor) {
    float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
    hsb[1] = Math.min(1.0f, hsb[1] * saturationFactor); // Increase saturation
    return new Color(Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]));
  }
  
  public static Color applyShadowColor(Color original, Color shadowColor) {
    float alpha = shadowColor.getAlpha() / 255.0f;
    return new Color(
        (int)(shadowColor.getRed() * alpha + original.getRed() * (1 - alpha)),
        (int)(shadowColor.getGreen() * alpha + original.getGreen() * (1 - alpha)),
        (int)(shadowColor.getBlue() * alpha + original.getBlue() * (1 - alpha))
    );
  }

  public static Color enhanceBrightnessAndContrast(Color color, float brightnessFactor, float contrastFactor) {
    int r = color.getRed();
    int g = color.getGreen();
    int b = color.getBlue();
    
    // Apply brightness
    r = Math.min(255, (int)(r * brightnessFactor));
    g = Math.min(255, (int)(g * brightnessFactor));
    b = Math.min(255, (int)(b * brightnessFactor));
    
    // Apply contrast
    float contrast = (contrastFactor - 1.0f) / 2.0f;
    r = (int)((r - 128) * contrastFactor + 128 + contrast * 255);
    g = (int)((g - 128) * contrastFactor + 128 + contrast * 255);
    b = (int)((b - 128) * contrastFactor + 128 + contrast * 255);
    
    // Clamp values
    r = Math.max(0, Math.min(255, r));
    g = Math.max(0, Math.min(255, g));
    b = Math.max(0, Math.min(255, b));
    
    return new Color(r, g, b, color.getAlpha());
  }

  /**
   * Scales RGB components by a factor (0.0-1.0)
   * Result is always fully opaque (alpha=255)
   */
  public static Color multiplyColor(Color color, double factor) {
    factor = Math.max(0, Math.min(1, factor));
    return new Color(
      (int)(color.getRed() * factor),
      (int)(color.getGreen() * factor),
      (int)(color.getBlue() * factor)
    );
  }
  
  public static Color multiplyColorFloat(Color color, float factor) {
    factor = Math.max(0f, Math.min(1f, factor));
    return new Color(
      (int)(color.getRed() * factor),
      (int)(color.getGreen() * factor),
      (int)(color.getBlue() * factor)
    );
  }
  
  public static Color multiplyColors(Color color1, Color color2) {
    float r = color1.getRed() / 255.0f * color2.getRed() / 255.0f;
    float g = color1.getGreen() / 255.0f * color2.getGreen() / 255.0f;
    float b = color1.getBlue() / 255.0f * color2.getBlue() / 255.0f;
    
    return new Color(r, g, b);
  }
  
  /**
   * Multiplies two colors with a scaling factor
   * Result is always fully opaque (alpha=255)
   */
  public static Color multiplyColors(Color base, Color light, double factor) {
    int r = (int) Math.min(255, Math.max(0, base.getRed() * light.getRed() / 255.0 * factor));
    int g = (int) Math.min(255, Math.max(0, base.getGreen() * light.getGreen() / 255.0 * factor));
    int b = (int) Math.min(255, Math.max(0, base.getBlue() * light.getBlue() / 255.0 * factor));
    
    return new Color(r, g, b);
  }
  
  /**
   * Creates a Color object from double values with robust validation
   * Result is always fully opaque (alpha=255)
   */
  public static Color createColor(double r, double g, double b) {
    if (Double.isNaN(r) || Double.isNaN(g) || Double.isNaN(b)) {
      return BLACK;
    }
    
    return new Color(
      clamp((int)r),
      clamp((int)g),
      clamp((int)b)
    );
  }
  
  /**
   * Clamps double value to [0, 255] range and rounds to nearest integer
   */
  private static int clampAndRound(double value) {
    if (value > Double.MAX_VALUE / 2) return 255;
    if (value < -Double.MAX_VALUE / 2) return 0;
    
    double clamped = Math.max(0.0, Math.min(255.0, value));
    return (int) Math.round(clamped);
  }
  
  public static int clampColorValue(int value) {
    if (value < 0) {
      return 0;
    }
    if (value > 255) {
      return 255;
    }
    return value;
  }
  
  public static float clampFloatColorValue(float value) {
    if (value < 0f) {
      return 0f;
    }
    if (value > 1f) {
      return 1f;
    }
    return value;
  }

  public static Color clampColor(Color color) {
    int r = Math.max(0, Math.min(255, color.getRed()));
    int g = Math.max(0, Math.min(255, color.getGreen()));
    int b = Math.max(0, Math.min(255, color.getBlue()));
    return new Color(r, g, b);
  }
  
  // Overload for float values
  public static Color createColor(float r, float g, float b) {
    return createColor((double) r, (double) g, (double) b);
  }
  
  // Overload for int values
  public static Color createColor(int r, int g, int b) {
    return new Color(
      Math.min(255, Math.max(0, r)),
      Math.min(255, Math.max(0, g)),
      Math.min(255, Math.max(0, b))
    );
  }
  
  // Interpolate between two colors
  public static Color interpolateColor(Color c1, Color c2, double t) {
    return blendColors(c1, c2, t);
  }
  
  // Combine multiple colors (additive blending)
  public static Color combineColors(Color... colors) {
    int r = 0, g = 0, b = 0;
    for (Color c : colors) {
      r = Math.min(255, r + c.getRed());
      g = Math.min(255, g + c.getGreen());
      b = Math.min(255, b + c.getBlue());
    }
    return new Color(r, g, b);
  }
  
  // Add noise/variation to a color
  public static Color addColorVariation(Color color, double variation) {
    double noise = 0.9 + Math.sin(variation * 15.0) * 0.1;
    int r = (int)(color.getRed() * noise);
    int g = (int)(color.getGreen() * noise);
    int b = (int)(color.getBlue() * noise);
    return new Color(clamp(r), clamp(g), clamp(b));
  }
  
  // Darken a color by specified amount (0.0 - 1.0)
  public static Color darkenColor(Color color, double amount) {
    amount = Math.max(0, Math.min(1, amount));
    int r = (int)(color.getRed() * (1 - amount));
    int g = (int)(color.getGreen() * (1 - amount));
    int b = (int)(color.getBlue() * (1 - amount));
    return new Color(clamp(r), clamp(g), clamp(b));
  }
  
  // Lighten a color by specified amount (0.0 - 1.0)
  public static Color lightenColor(Color color, double amount) {
    amount = Math.max(0, Math.min(1, amount));
    int r = (int)(color.getRed() + (255 - color.getRed()) * amount);
    int g = (int)(color.getGreen() + (255 - color.getGreen()) * amount);
    int b = (int)(color.getBlue() + (255 - color.getBlue()) * amount);
    return new Color(clamp(r), clamp(g), clamp(b));
  }
  
  /**
   * Color addition (RGB only)
   * Result is always fully opaque (alpha=255)
   */
  public static Color add(Color c1, Color c2) {
    return new Color(
      Math.min(255, c1.getRed() + c2.getRed()),
      Math.min(255, c1.getGreen() + c2.getGreen()),
      Math.min(255, c1.getBlue() + c2.getBlue())
    );
  }
  
  /**
   * Extracts float components [R,G,B] from AWT Color (0.0-1.0 range)
   */
  public static float[] getFloatComponents(Color color) {
    float[] comp = new float[3];
    comp[0] = color.getRed() / 255.0f;
    comp[1] = color.getGreen() / 255.0f;
    comp[2] = color.getBlue() / 255.0f;
    return comp;
  }
  
  /**
   * Adds specular highlight effect to a color based on intensity
   * Result is always fully opaque (alpha=255)
   */
  public static Color addSpecularHighlight(Color baseColor, double intensity) {
    intensity = Math.max(0, Math.min(1, intensity));
    int r = (int)(baseColor.getRed() + (255 - baseColor.getRed()) * intensity);
    int g = (int)(baseColor.getGreen() + (255 - baseColor.getGreen()) * intensity);
    int b = (int)(baseColor.getBlue() + (255 - baseColor.getBlue()) * intensity);
    return new Color(clamp(r), clamp(g), clamp(b));
  }
  
  /**
   * Adds specular highlight with custom highlight color
   * Result is always fully opaque (alpha=255)
   */
  public static Color addSpecularHighlight(Color baseColor, Color highlightColor, double intensity) {
    intensity = Math.max(0, Math.min(1, intensity));
    int r = (int)(baseColor.getRed() + (highlightColor.getRed() - baseColor.getRed()) * intensity);
    int g = (int)(baseColor.getGreen() + (highlightColor.getGreen() - baseColor.getGreen()) * intensity);
    int b = (int)(baseColor.getBlue() + (highlightColor.getBlue() - baseColor.getBlue()) * intensity);
    return new Color(clamp(r), clamp(g), clamp(b));
  }
  
  // Null-safe version of add
  public static Color addSafe(Color c1, Color c2) {
    if (c1 == null && c2 == null) return BLACK;
    if (c1 == null) return c2;
    if (c2 == null) return c1;
    return add(c1, c2);
  }
  
  /**
   * Clamps float value between [min, max]
   */
  public static float clamp(float value, float min, float max) {
    return Math.max(min, Math.min(max, value));
  }
  
  public static double clampDouble(double value, double min, double max) {
    return Math.max(min, Math.min(max, value));
  }
  
  /**
   * Bilinear interpolation between four colors
   * Result is always fully opaque (alpha=255)
   */
  public static Color bilinearInterpolate(Color c00, Color c10, Color c01, Color c11, double tx, double ty) {
    int r = (int)((1-tx)*(1-ty)*c00.getRed() + tx*(1-ty)*c10.getRed() +
    (1-tx)*ty*c01.getRed() + tx*ty*c11.getRed());
    int g = (int)((1-tx)*(1-ty)*c00.getGreen() + tx*(1-ty)*c10.getGreen() +
    (1-tx)*ty*c01.getGreen() + tx*ty*c11.getGreen());
    int b = (int)((1-tx)*(1-ty)*c00.getBlue() + tx*(1-ty)*c10.getBlue() +
    (1-tx)*ty*c01.getBlue() + tx*ty*c11.getBlue());
    
    return new Color(clamp(r), clamp(g), clamp(b));
  }
  
  /**
   * Blends two colors with given ratio (0.0-1.0)
   * Result is always fully opaque (alpha=255)
   */
  public static Color blendColors(Color color1, Color color2, float ratio) {
    ratio = clamp(ratio, 0.0f, 1.0f);
    int r = (int)(color1.getRed() * (1 - ratio) + color2.getRed() * ratio);
    int g = (int)(color1.getGreen() * (1 - ratio) + color2.getGreen() * ratio);
    int b = (int)(color1.getBlue() * (1 - ratio) + color2.getBlue() * ratio);
    return new Color(clamp(r), clamp(g), clamp(b));
  }
  
  // Double ratio version
  public static Color blendColors(Color color1, Color color2, double ratio) {
    ratio = Math.max(0, Math.min(1, ratio));
    int r = (int)(color1.getRed() * (1-ratio) + color2.getRed() * ratio);
    int g = (int)(color1.getGreen() * (1-ratio) + color2.getGreen() * ratio);
    int b = (int)(color1.getBlue() * (1-ratio) + color2.getBlue() * ratio);
    return new Color(clamp(r), clamp(g), clamp(b));
  }
  
  /**
   * Smooth blending using smoothstep function
   * Result is always fully opaque (alpha=255)
   */
  public static Color smoothBlend(Color c1, Color c2, double ratio) {
    ratio = Math.max(0, Math.min(1, ratio));
    double smoothRatio = ratio * ratio * (3 - 2 * ratio);
    return blendColors(c1, c2, (float)smoothRatio);
  }
  
  /**
   * Calculates luminance (brightness) of color using ITU-R BT.709 standard
   */
  public static double luminance(Color color) {
    return (0.2126 * color.getRed() + 0.7152 * color.getGreen() + 0.0722 * color.getBlue()) / 255.0;
  }
  
  /**
   * Adjusts color contrast
   * Result is always fully opaque (alpha=255)
   */
  public static Color adjustContrast(Color color, float contrast) {
    float factor = (259f * (contrast + 255f)) / (255f * (259f - contrast));
    int red = adjustComponent(color.getRed(), factor);
    int green = adjustComponent(color.getGreen(), factor);
    int blue = adjustComponent(color.getBlue(), factor);
    return new Color(clamp(red), clamp(green), clamp(blue));
  }
  
  private static int adjustComponent(int component, float factor) {
    float normalized = component / 255f;
    float adjusted = 0.5f + factor * (normalized - 0.5f);
    return (int)(adjusted * 255f);
  }
  
  /**
   * Adjusts color brightness (exposure)
   * Result is always fully opaque (alpha=255)
   */
  public static Color adjustExposure(Color color, float exposure) {
    float[] rgb = getFloatComponents(color);
    return new Color(
      clamp(rgb[0] * exposure, 0f, 1f),
      clamp(rgb[1] * exposure, 0f, 1f),
      clamp(rgb[2] * exposure, 0f, 1f)
    );
  }
  
  /**
   * Adjusts color saturation
   * Result is always fully opaque (alpha=255)
   */
  public static Color adjustSaturation(Color color, float saturation) {
    float[] rgb = getFloatComponents(color);
    float luminance = 0.2126f * rgb[0] + 0.7152f * rgb[1] + 0.0722f * rgb[2];
    return new Color(
      clamp(luminance + (rgb[0] - luminance) * saturation, 0f, 1f),
      clamp(luminance + (rgb[1] - luminance) * saturation, 0f, 1f),
      clamp(luminance + (rgb[2] - luminance) * saturation, 0f, 1f)
    );
  }
  
  /**
   * Inverts color (negative)
   * Result is always fully opaque (alpha=255)
   */
  public static Color invert(Color color) {
    return new Color(
      255 - color.getRed(),
      255 - color.getGreen(),
      255 - color.getBlue()
    );
  }
  
  /**
   * Shifts hue in HSV color space
   * Result is always fully opaque (alpha=255)
   */
  public static Color shiftHue(Color color, float hueShift) {
    float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
    float newHue = (hsb[0] + hueShift/360f) % 1f;
    if (newHue < 0) newHue += 1f;
    return Color.getHSBColor(newHue, hsb[1], hsb[2]);
  }
  
  /**
   * Adjusts color temperature (warm/cool)
   * Result is always fully opaque (alpha=255)
   */
  public static Color adjustTemperature(Color color, float temperature) {
    temperature = clamp(temperature, -1f, 1f);
    float[] rgb = getFloatComponents(color);
    if (temperature > 0) {
      rgb[0] += temperature;
      rgb[1] += temperature * 0.5f;
      } else {
      rgb[2] -= temperature;
    }
    return new Color(
      clamp(rgb[0], 0f, 1f),
      clamp(rgb[1], 0f, 1f),
      clamp(rgb[2], 0f, 1f)
    );
  }
  
  /**
   * Converts to black and white based on threshold
   * Result is always fully opaque (alpha=255)
   */
  public static Color toBlackAndWhite(Color color, int threshold) {
    int luminance = (int)(luminance(color) * 255);
    return luminance > threshold ? WHITE : BLACK;
  }
  
  /**
   * Converts to sepia tone
   * Result is always fully opaque (alpha=255)
   */
  public static Color toSepia(Color color) {
    int r = color.getRed();
    int g = color.getGreen();
    int b = color.getBlue();
    int tr = (int)(0.393 * r + 0.769 * g + 0.189 * b);
    int tg = (int)(0.349 * r + 0.686 * g + 0.168 * b);
    int tb = (int)(0.272 * r + 0.534 * g + 0.131 * b);
    return new Color(clamp(tr), clamp(tg), clamp(tb));
  }
  
  /**
   * Calculates Euclidean distance between two colors
   */
  public static double colorDistance(Color c1, Color c2) {
    double rDiff = c1.getRed() - c2.getRed();
    double gDiff = c1.getGreen() - c2.getGreen();
    double bDiff = c1.getBlue() - c2.getBlue();
    return Math.sqrt(rDiff*rDiff + gDiff*gDiff + bDiff*bDiff);
  }
  
  /**
   * Sets new alpha value for existing color
   * This is the ONLY method that handles alpha - for compatibility
   */
  public static Color setAlpha(Color color, int alpha) {
    alpha = clamp(alpha, 0, 255);
    return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
  }
  
  /**
   * Sets new alpha value (float 0.0-1.0)
   * This is the ONLY method that handles alpha - for compatibility
   */
  public static Color setAlpha(Color color, float alpha) {
    alpha = clamp(alpha, 0.0f, 1.0f);
    return new Color(
      color.getRed() / 255f,
      color.getGreen() / 255f,
      color.getBlue() / 255f,
      alpha
    );
  }
  
  /**
   * Clamp integer value to [0,255]
   */
  public static int clamp(int value, int min, int max) {
    return Math.max(min, Math.min(max, value));
  }
  
  public static int clamp(int value) {
    return Math.max(0, Math.min(255, value));
  }
  
  public static double clampDoubleColorValue(double value) {
    return Math.max(0.0, Math.min(1.0, value));
  }
  
  /**
   * Scales color by factor (0.0-1.0)
   * Result is always fully opaque (alpha=255)
   */
  public static Color scale(Color color, double factor) {
    factor = Math.max(0.0, Math.min(1.0, factor));
    int r = (int)(color.getRed() * factor);
    int g = (int)(color.getGreen() * factor);
    int b = (int)(color.getBlue() * factor);
    
    return new Color(clamp(r), clamp(g), clamp(b));
  }
  
  /**
   * Applies lighting model (diffuse only)
   * Result is always fully opaque (alpha=255)
   */
  public static Color applyLighting(Color baseColor, Color lightColor, double intensity, double NdotL) {
    int r = (int)(baseColor.getRed() * lightColor.getRed() / 255.0 * intensity * NdotL);
    int g = (int)(baseColor.getGreen() * lightColor.getGreen() / 255.0 * intensity * NdotL);
    int b = (int)(baseColor.getBlue() * lightColor.getBlue() / 255.0 * intensity * NdotL);
    return new Color(clamp(r), clamp(g), clamp(b));
  }
  
  /**
   * Applies lighting with ambient and diffuse components
   * Result is always fully opaque (alpha=255)
   */
  public static Color applyLightingX(Color base, Color light, double diffuse, double ambient) {
    int r = (int)((base.getRed() * (ambient + diffuse * light.getRed()/255.0)));
    int g = (int)((base.getGreen() * (ambient + diffuse * light.getGreen()/255.0)));
    int b = (int)((base.getBlue() * (ambient + diffuse * light.getBlue()/255.0)));
    return new Color(clamp(r), clamp(g), clamp(b));
  }
  
}
