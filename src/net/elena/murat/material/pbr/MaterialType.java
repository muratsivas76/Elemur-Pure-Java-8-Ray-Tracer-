package net.elena.murat.material.pbr;

/**
 * Advanced PBR material types with hologram-specific extensions.
 * Includes spectral, temporal and quantum rendering modes.
 */
public enum MaterialType {
  // Basic PBR Types
  METAL,          // Traditional metallic surfaces
  DIELECTRIC,     // Insulating materials (glass, plastic)
  PLASTIC,        // Micro-surface distributed plastic
  EMISSIVE,       // Self-illuminating surfaces
  TRANSPARENT,    // Light-transmitting materials
  
  // Holographic/Advanced Types
  HOLOGRAM,       // Basic holographic surface
  ANISOTROPIC,    // Directional reflection properties
  SPECTRAL,       // Wavelength-based color distribution
  VOLUMETRIC,     // Volumetric effects (fog, smoke)
  QUANTUM,        // Quantum wave function effects
  GLITCH,         // Digital distortion effects
  BIOLUMINESCENT, // Biological illumination
  PHANTOM         // Phantom image (partial intersection)
  ;
  
  /**
   * Checks if this material type requires temporal calculations.
   */
  public boolean isTimeDependent() {
    return this == HOLOGRAM ||
    this == SPECTRAL ||
    this == QUANTUM ||
    this == GLITCH;
  }
  
  /**
   * Checks if material has volumetric properties.
   */
  public boolean isVolumetric() {
    return this == VOLUMETRIC ||
    this == PHANTOM;
  }
  
  /**
   * Checks if material requires special light transport.
   */
  public boolean needsSpecialLightHandling() {
    return this == ANISOTROPIC ||
    this == BIOLUMINESCENT;
  }
  
  /**
   * Suggested roughness range for material type.
   */
  public double[] getSuggestedRoughnessRange() {
    switch(this) {
      case HOLOGRAM: return new double[]{0.05, 0.3};
      case ANISOTROPIC: return new double[]{0.1, 0.5};
      case GLITCH: return new double[]{0.2, 0.8};
      default: return new double[]{0.0, 1.0};
    }
  }
  
}
