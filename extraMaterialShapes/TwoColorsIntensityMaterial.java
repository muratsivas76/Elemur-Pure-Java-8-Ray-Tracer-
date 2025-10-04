import java.awt.Color;

import net.elena.murat.material.Material;
import net.elena.murat.math.Point3;
import net.elena.murat.math.Vector3;
import net.elena.murat.light.Light;

public class TwoColorsIntensityMaterial implements Material {
  private final Color color1;
  private final Color color2;
  private final double intensity;
  
  // Default constructor
  public TwoColorsIntensityMaterial() {
    this.color1 = Color.RED;
    this.color2 = Color.BLUE;
    this.intensity = 0.5;
  }
  
  // Parameterized constructor
  public TwoColorsIntensityMaterial(Color color1, Color color2, double intensity) {
    this.color1 = color1;
    this.color2 = color2;
    this.intensity = intensity;
  }
  
  @Override
  public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPos) {
    // Renkleri topla ve intensity ile çarp
    int r = clamp((color1.getRed() + color2.getRed()) * intensity);
    int g = clamp((color1.getGreen() + color2.getGreen()) * intensity);
    int b = clamp((color1.getBlue() + color2.getBlue()) * intensity);
    
    return new Color(r, g, b);
  }
  
  // RGB değerlerini 0-255 arasına clamp et
  private int clamp(double value) {
    return (int) Math.max(0, Math.min(255, Math.round(value)));
  }
  
  @Override
  public double getReflectivity() {
    return 0.1;
  }
  
  @Override
  public double getIndexOfRefraction() {
    return 1.0;
  }
  
  @Override
  public double getTransparency() {
    return 0.0;
  }
  
  @Override
  public void setObjectTransform(net.elena.murat.math.Matrix4 tm) {
    // Not used in this material
  }
  
  @Override
  public String toString() {
    return "TwoColorsIntensityMaterial[color1=" + color1 + ", color2=" + color2 + ", intensity=" + intensity + "]";
  }
  
}
// javac -parameters -cp ..\bin\elenaRT.jar; TwoColorsIntensityMaterial.java
