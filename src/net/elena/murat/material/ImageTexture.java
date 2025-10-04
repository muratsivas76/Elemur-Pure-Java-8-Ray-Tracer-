package net.elena.murat.material;

import java.awt.Color;
import java.awt.image.BufferedImage;

import net.elena.murat.math.*;

/**
 * Represents an image-based texture. It can return a color based on
 * 3D point and normal (using spherical mapping for spheres) or direct UV coordinates.
 * This class does NOT implement the Material interface directly, but is used by Materials.
 */
public class ImageTexture {
  private final BufferedImage image;
  private final double scaleU;
  private final double scaleV;
  private final double offsetU;
  private final double offsetV;
  
  public ImageTexture(BufferedImage image, double scale) {
    this(image, scale, scale, 0.0, 0.0);
  }
  
  public ImageTexture(BufferedImage image, double scaleU, double scaleV, double offsetU, double offsetV) {
    this.image = image;
    this.scaleU = scaleU;
    this.scaleV = scaleV;
    this.offsetU = offsetU;
    this.offsetV = offsetV;
  }
  
  /**
   * Retrieves the color from the texture at the given UV coordinates.
   * This method handles texture tiling based on the scale factors.
   * @param u The U-coordinate (horizontal, expected 0.0 to 1.0, but can be outside for tiling).
   * @param v The V-coordinate (vertical, expected 0.0 to 1.0, but can be outside for tiling).
   * @return The Color at the specified texture coordinates.
   */
  public Color getColorFromUV(double u, double v) { // Renamed to avoid conflict with Material.getColorAt
    // Apply scaling and offset
    u = u * scaleU + offsetU;
    v = v * scaleV + offsetV;
    
    // Apply tiling using Math.floorMod for correct wrapping for negative values
    int imgX = Math.floorMod((int)(u * image.getWidth()), image.getWidth());
    int imgY = Math.floorMod((int)(v * image.getHeight()), image.getHeight());
    
    // Ensure indices are within bounds (should be handled by floorMod, but as a safeguard)
    imgX = Math.max(0, Math.min(image.getWidth() - 1, imgX));
    imgY = Math.max(0, Math.min(image.getHeight() - 1, imgY));
    
    return new Color(image.getRGB(imgX, imgY));
  }
  
  /**
   * Calculates UV coordinates from a 3D point and its normal using spherical mapping,
   * then retrieves the color from the texture.
   * This method is generally suitable for spheres centered at the origin.
   * For other shapes, this mapping might not be appropriate.
   *
   * @param point The 3D intersection point on the surface.
   * @param normal The surface normal at the intersection point.
   * @return The Color from the texture at the calculated UV coordinates.
   */
  public Color getColorFrom3DPoint(Point3 point, Vector3 normal) { // Renamed to clarify its purpose
    // Normalize normal to avoid issues with length
    Vector3 n = normal.normalize();
    
    // Spherical mapping from normal (latitude/longitude)
    // Theta: angle from +Y axis (pole), range [0, PI]
    double theta = Math.acos(Math.max(-1.0, Math.min(1.0, n.y))); // Clamp n.y for numerical stability
    
    // Phi: angle around Y axis, range [-PI, PI] then adjusted to [0, 2PI]
    double phi = Math.atan2(n.z, n.x);
    if (phi < 0) phi += 2 * Math.PI;
    
    // Convert to UV coordinates (normalized 0.0 to 1.0)
    double u = phi / (2 * Math.PI);
    double v = theta / Math.PI;
    
    // Get color using the more general getColorFromUV method
    return getColorFromUV(u, v);
  }
  
}
