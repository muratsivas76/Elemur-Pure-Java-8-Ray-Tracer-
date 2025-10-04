package net.elena.murat.material;

/**
 * Enum to define the direction of the stripes.
 */
public enum StripeDirection {
  HORIZONTAL, // Stripes vary along the V-axis (or Y-axis in local space)
  VERTICAL,   // Stripes vary along the U-axis (or X-axis in local space)
  DIAGONAL,    // Stripes vary along a diagonal (U+V or U-V in local space)
  RANDOM
}
