package net.elena.murat.material.pbr;

import java.awt.Color;

import net.elena.murat.light.Light;
import net.elena.murat.math.Point3;
import net.elena.murat.math.Vector3;
import net.elena.murat.material.Material; // Temel arayüzü extend ediyor

/**
 * PBRCapableMaterial is an interface for materials that support Physically Based Rendering.
 * It extends the basic Material interface and adds PBR-specific properties.
 */
public interface PBRCapableMaterial extends Material {
  /**
   * Returns the base color (albedo) of the material.
   * @return The albedo color.
   */
  Color getAlbedo();
  
  /**
   * Returns the surface roughness [0.0 (smooth) to 1.0 (rough)].
   * @return The roughness value.
   */
  double getRoughness();
  
  /**
   * Returns the metalness factor [0.0 (dielectric) to 1.0 (metal)].
   * @return The metalness value.
   */
  double getMetalness();
  
  /**
   * Returns the type of PBR material (e.g., METAL, DIELECTRIC).
   * @return The material type.
   */
  MaterialType getMaterialType();
}
