package net.elena.murat.material;

import java.awt.Color;

public class GoldMaterial extends MetallicMaterial {
  public GoldMaterial() {
    super(new Color(255, 215, 0), // Gold color
      new Color(255, 223, 186), // Light yellowish specular color for gold (or Color.WHITE)
      0.9,   // Reflectivity strength
      150.0, // Shininess
      0.1,   // Ambient light contribution
      0.05,  // Very low diffuse contribution
      0.95   // High specular contribution
    );
  }
  
}
