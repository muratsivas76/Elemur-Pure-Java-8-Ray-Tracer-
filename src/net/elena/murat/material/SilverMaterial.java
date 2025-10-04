package net.elena.murat.material;

import java.awt.Color;

public class SilverMaterial extends MetallicMaterial {
  public SilverMaterial() {
    super(new Color(192, 192, 192), // Silver base color
      Color.WHITE,   // White specular highlight for silver
      0.95,  // Very high reflectivity
      200.0, // Very high shininess
      0.1,   // Ambient light contribution
      0.02,  // Very low diffuse contribution
      0.98   // Very high specular contribution
    );
  }
  
}
