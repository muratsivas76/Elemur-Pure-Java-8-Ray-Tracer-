public class TextureMaterial extends Material {
  private BufferedImage texture;
  private UVCalculator uvCalculator;
  
  public TextureMaterial(BufferedImage texture, UVCalculator uvCalculator) {
    this.texture = texture;
    this.uvCalculator = uvCalculator;
  }
  
  @Override
  public Color getColorAt(Point3D localPoint, Vec3 normal,
    Point3D light, Point3D viewerPoint) {
    
    // 1. UV Calculator strategy'sini kullan
    float[] uv = uvCalculator.calculateUV(localPoint);
    
    // 2. Texture'dan renk al
    int x = (int) (uv[0] * (texture.getWidth() - 1));
    int y = (int) ((1 - uv[1]) * (texture.getHeight() - 1));
    int rgb = texture.getRGB(x, y);
    
    // 3. Işıklandırma hesapla
    Color baseColor = new Color(rgb);
    return calculateLighting(baseColor, localPoint, normal, light, viewerPoint);
  }
  
  private Color calculateLighting(Color baseColor, Point3D point, Vec3 normal,
    Point3D light, Point3D viewerPoint) {
    // Işık hesaplamaları...
    return baseColor; // Simplified
  }
}