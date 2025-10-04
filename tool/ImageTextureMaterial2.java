package net.elena.murat.material;

import java.awt.Color;
import java.awt.image.BufferedImage;
import net.elena.murat.math.Point3;

/**
 * Texture-mapped material using a loaded image.
 */
public class ImageTextureMaterial implements Material {
  private final BufferedImage image;
  private final double scale;
  
  public ImageTextureMaterial(BufferedImage image, double scale) {
    this.image = image;
    this.scale = scale;
  }
  
  @Override
  public Color getColorAt(Point3 point) {
    int u = Math.floorMod((int)(point.x * scale), image.getWidth());
    int v = Math.floorMod((int)(point.z * scale), image.getHeight());
    return new Color(image.getRGB(u, v));
  }
}
