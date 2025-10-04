package net.elena.murat.math;

import java.awt.Color;

/**
 * Represents a color with floating-point red, green, blue components.
 * Used for high-precision color calculations in ray tracing.
 * Values are typically in [0.0, 1.0] range.
 * Alpha channel is not supported - all operations are RGB-only.
 */
public class FloatColor {
  public final double r, g, b, a;
  
  /**
   * Constructs a FloatColor with red, green, and blue components.
   *
   * @param r Red component (0.0 - 1.0)
   * @param g Green component (0.0 - 1.0)
   * @param b Blue component (0.0 - 1.0)
   */
  public FloatColor(double r, double g, double b) {
    this.r = r;
    this.g = g;
    this.b = b;
    this.a = 1.0;
  }
  
  public FloatColor(double r, double g, double b, double a) {
    this.r = r;
    this.g = g;
    this.b = b;
    this.a = a;
  }
  
  /**
   * Constructs a FloatColor from a java.awt.Color.
   * Alpha channel is ignored.
   *
   * @param color The AWT Color to convert
   */
  public FloatColor(Color color) {
    this.r = color.getRed() / 255.0;
    this.g = color.getGreen() / 255.0;
    this.b = color.getBlue() / 255.0;
    this.a = color.getAlpha () / 255.0;
  }
  
  public double getR () {
    return this.r;
  }
  
  public double getG () {
    return this.g;
  }
  
  public double getB () {
    return this.b;
  }
  
  public double getA () {
    return this.a;
  }
  
  /**
   * Adds this color to another color component-wise.
   *
   * @param other The color to add
   * @return A new FloatColor representing the sum
   */
  public FloatColor add(FloatColor other) {
    return new FloatColor(
      this.r + other.r,
      this.g + other.g,
      this.b + other.b,
      Math.max(this.a, other.a)
    );
  }
  
  /**
   * Multiplies this color by a scalar factor.
   *
   * @param factor The multiplication factor
   * @return A new FloatColor with scaled components
   */
  public FloatColor multiply(double factor) {
    return new FloatColor(this.r * factor, this.g * factor, this.b * factor, this.a);
  }
  
  public FloatColor divide(double divisor) {
    if (divisor == 0) {
      return new FloatColor(0, 0, 0, this.a);
    }
    return new FloatColor(this.r / divisor, this.g / divisor, this.b / divisor, this.a);
  }
  
  /**
   * Scales this color by a factor (same as multiply).
   *
   * @param factor The scale factor
   * @return A new FloatColor
   */
  public FloatColor scale(double factor) {
    return this.multiply(factor);
  }
  
  /**
   * Clamps all components (r, g, b) to the [0.0, 1.0] range.
   *
   * @return A new clamped FloatColor
   */
  public FloatColor clamp01() {
    return new FloatColor(
      Math.max(0.0, Math.min(1.0, r)),
      Math.max(0.0, Math.min(1.0, g)),
      Math.max(0.0, Math.min(1.0, b)),
      Math.max(0.0, Math.min(1.0, a))
    );
  }
  /**
   * Checks if this color is nearly black.
   *
   * @param threshold The maximum value for each component to be considered "black"
   * @return true if all components are below the threshold
   */
  public boolean isBlack(double threshold) {
    return r < threshold && g < threshold && b < threshold;
  }
  
  /**
   * Converts this FloatColor to a java.awt.Color for rendering.
   * Components are clamped and scaled to 0-255 range.
   * Result is always fully opaque (alpha=255).
   *
   * @return A new Color object with RGB values
   */
  public Color toAWTColor() {
    FloatColor clamped = clamp01();
    int r = (int) (clamped.r * 255.0);
    int g = (int) (clamped.g * 255.0);
    int b = (int) (clamped.b * 255.0);
        
    return new Color(r, g, b); // RGB constructor (fully opaque)
  }
  
  public int toARGB() {
    int aInt = (int)(Math.max(0, Math.min(1, a)) * 255);
    
    int rInt = (int)(Math.max(0, Math.min(1, r)) * 255);
    int gInt = (int)(Math.max(0, Math.min(1, g)) * 255);
    int bInt = (int)(Math.max(0, Math.min(1, b)) * 255);
    
    return (aInt << 24) | (rInt << 16) | (gInt << 8) | bInt;
  }
  
  // Transparent pixels
  public int toARGBWithAlpha(int alpha) {
    int r = (int) (Math.max(0, Math.min(1, this.r)) * 255);
    int g = (int) (Math.max(0, Math.min(1, this.g)) * 255);
    int b = (int) (Math.max(0, Math.min(1, this.b)) * 255);
    int a = Math.max(0, Math.min(255, alpha));
    return (a << 24) | (r << 16) | (g << 8) | b;
  }
  
  /**
   * Performs component-wise multiplication (Hadamard product) with another color.
   *
   * @param other The other FloatColor
   * @return A new FloatColor with multiplied components
   */
  public FloatColor multiply(FloatColor other) {
    return new FloatColor(
      this.r * other.r,
      this.g * other.g,
      this.b * other.b,
      this.a * other.a
    );
  }
  
  /**
   * Static helper: component-wise multiplication.
   */
  public static FloatColor product(FloatColor a, FloatColor b) {
    return a.multiply(b);
  }
  
  /**
   * Static helper: multiply color by scalar.
   */
  public static FloatColor product(FloatColor a, double b) {
    return new FloatColor(a.r * b, a.g * b, a.b * b);
  }
  
  /**
   * Checks if this color is smaller than another in all components.
   *
   * @param other The other color
   * @return true if r less than other.r AND g less than other.g AND b less than other.b
   */
  public boolean isSmaller(FloatColor other) {
    return r < other.r && g < other.g && b < other.b;
  }
  
  /**
   * Returns the component-wise inverse (1/c).
   * Prevents division by zero.
   *
   * @return A new FloatColor
   */
  public FloatColor inverse() {
    return new FloatColor(
      r != 0.0 ? 1.0 / r : 0.0,
      g != 0.0 ? 1.0 / g : 0.0,
      b != 0.0 ? 1.0 / b : 0.0,
      this.a
    );
  }
  
  /**
   * Subtracts another color (absorption) component-wise.
   * Clamps result to 0.0 minimum.
   *
   * @param absorption The color to subtract
   * @return A new FloatColor
   */
  public FloatColor subtract(FloatColor absorption) {
    return new FloatColor(
      Math.max(0.0, this.r - absorption.r),
      Math.max(0.0, this.g - absorption.g),
      Math.max(0.0, this.b - absorption.b)
    );
  }
  
  /**
   * Subtracts a constant value from all components.
   *
   * @param value The value to subtract
   * @return A new FloatColor
   */
  public FloatColor subtract(double value) {
    return new FloatColor(
      Math.max(0.0, this.r - value),
      Math.max(0.0, this.g - value),
      Math.max(0.0, this.b - value)
    );
  }
  
  // Predefined constants
  public static final FloatColor BLACK = new FloatColor(0.0, 0.0, 0.0, 0.0);
  public static final FloatColor WHITE = new FloatColor(1.0, 1.0, 1.0, 0.0);
  
}
