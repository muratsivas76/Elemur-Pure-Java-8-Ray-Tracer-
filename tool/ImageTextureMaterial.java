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
  
  //   @Override
  //  public Color getColorAt(Point3 point) {
  //     int u = Math.abs((int)(point.x * scale) % image.getWidth());
  //  )  int v = Math.abs((int)(point.z * scale) % image.getHeight());
  //
  // return new Color(image.getRGB(u, v));
  // }
  
  @Override
  public Color getColorAt(Point3 point) {
    // Kürenin merkezini (0,0,0) olarak varsayalım.
    double x = point.x;
    double y = point.y;
    double z = point.z;
    
    // Küresel koordinatlara dönüştürme (basitleştirilmiş)
    double theta = Math.atan2(z, x); // Açıyı hesapla
    double phi = Math.acos(y); // Açıyı hesapla
    
    // UV koordinatlarına dönüştürme
    double u = 0.5 + (theta / (2 * Math.PI));
    double v = phi / Math.PI;
    
    // U ve V değerlerini imaj boyutlarına göre ölçeklendirme
    int imageX = (int) ((u * image.getWidth()) % image.getWidth());
    int imageY = (int) ((v * image.getHeight()) % image.getHeight());
    
    // İmajdan rengi al
    return new Color(image.getRGB(imageX, imageY));
  }
  
}
