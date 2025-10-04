import net.elena.murat.util.ColorUtil;
import java.awt.Color;

public class MosaicMaterial implements Material {
  private Color baseColor;
  private Color tileColor;
  private double tileSize;
  private double groutWidth;
  private double randomness;
  private double transparency;
  
  public MosaicMaterial(Color baseColor, Color tileColor,
    double tileSize, double groutWidth, double randomness) {
    this.baseColor = baseColor;
    this.tileColor = tileColor;
    this.tileSize = tileSize;
    this.groutWidth = groutWidth;
    this.randomness = randomness;
    this.transparency = calculateTransparency(baseColor);
  }
  
  public MosaicMaterial(Color baseColor, Color tileColor) {
    this(baseColor, tileColor, 0.3, 0.05, 0.2);
  }
  
  private double calculateTransparency(Color color) {
    int alpha = color.getAlpha();
    return 1.0 - ((double)alpha / 255.0);
  }
  
  @Override
  public double getTransparency() {
    return transparency;
  }
  
  @Override
  public double getReflectivity() {
    return 0.1;
  }
  
  @Override
  public double getIndexOfRefraction() {
    return 1.3;
  }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
    // Not needed for this material
  }
  
  @Override
  public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPoint) {
    double pattern = calculateMosaicPattern(point);
    return ColorUtil.blendColors(baseColor, tileColor, pattern);
  }
  
  private double calculateMosaicPattern(Point3 point) {
    // Add some randomness to prevent perfect alignment
    double randomOffset = (point.hashCode() % 1000) / 1000.0 * randomness;
    
    // Calculate tile coordinates with randomness
    double x = (point.x + randomOffset) / tileSize;
    double y = (point.y + randomOffset) / tileSize;
    double z = (point.z + randomOffset) / tileSize;
    
    // Get fractional parts to determine tile position
    double fracX = x - Math.floor(x);
    double fracY = y - Math.floor(y);
    double fracZ = z - Math.floor(z);
    
    // Check if we're in the grout area
    if (fracX < groutWidth || fracX > (1.0 - groutWidth) ||
      fracY < groutWidth || fracY > (1.0 - groutWidth) ||
      fracZ < groutWidth || fracZ > (1.0 - groutWidth)) {
      return 0.0; // Base color (grout)
    }
    
    // Add some variation to tile color based on position
    double variation = ((int)(Math.floor(x) + Math.floor(y) + Math.floor(z)) % 3) * 0.1;
    
    return 0.9 + variation; // Tile color with slight variation
  }
  
  public Color getBaseColor() {
    return baseColor;
  }
  
  public void setBaseColor(Color baseColor) {
    this.baseColor = baseColor;
    this.transparency = calculateTransparency(baseColor);
  }
  
  public Color getTileColor() {
    return tileColor;
  }
  
  public void setTileColor(Color tileColor) {
    this.tileColor = tileColor;
  }
  
  public double getTileSize() {
    return tileSize;
  }
  
  public void setTileSize(double tileSize) {
    this.tileSize = tileSize;
  }
  
  public double getGroutWidth() {
    return groutWidth;
  }
  
  public void setGroutWidth(double groutWidth) {
    this.groutWidth = groutWidth;
  }
  
  public double getRandomness() {
    return randomness;
  }
  
  public void setRandomness(double randomness) {
    this.randomness = randomness;
  }
}