package net.elena.murat.material;

import java.awt.Color;
import java.awt.image.BufferedImage;

import net.elena.murat.light.Light;
import net.elena.murat.math.*;

public class TextureMaterial implements Material {
  private final BufferedImage texture;
  private final int width;
  private final int height;
  
  public TextureMaterial(BufferedImage texture) {
    if (texture == null) {
      throw new IllegalArgumentException("Texture cannot be null");
    }
    this.texture = texture;
    this.width = texture.getWidth();
    this.height = texture.getHeight();
  }
  
  @Override
  public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPos) {
    // Calculate texture coordinates in [0,1] range
    double u = clamp(point.x - Math.floor(point.x)); // x mod 1
    double v = 1.0 - clamp(point.y - Math.floor(point.y)); // y mod 1 (flipped)
    
    // Calculate pixel position
    int x = (int)(u * (width - 1));
    int y = (int)(v * (height - 1));
    
    // Return RGB value directly (alpha ignored)
    return new Color(texture.getRGB(x, y));
  }
  
  private double clamp(double value) {
    return Math.max(0.0, Math.min(1.0, value));
  }
  
  @Override
  public double getReflectivity() {
    return 0.0; // No reflection
  }
  
  @Override
  public double getIndexOfRefraction() {
    return 1.0; // Air refractive index
  }
  
  @Override
  public double getTransparency() {
    return 0.0; // Fully opaque
  }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
  }
  
}
