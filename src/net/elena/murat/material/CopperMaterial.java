package net.elena.murat.material;

import java.awt.Color;

public class CopperMaterial extends MetallicMaterial {
  public CopperMaterial() {
    // Copper color (reddish-brown)
    // Reflectivity, shininess and other coefficients are specifically tuned for copper
    super(new Color(184, 115, 51), // Copper color (reddish-brown)
      new Color(220, 150, 100), // Slightly reddish specular color for copper (or Color.WHITE)
      0.85,  // Reflectivity strength (can be slightly less shiny than gold and silver)
      120.0, // Shininess (medium-high)
      0.1,   // Ambient light contribution
      0.07,  // Low diffuse contribution
      0.90   // High specular contribution
    );
  }
  
}
