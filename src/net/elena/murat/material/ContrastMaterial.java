package net.elena.murat.material;

import net.elena.murat.math.*;
import net.elena.murat.light.Light;
import java.awt.Color;

public class ContrastMaterial implements Material {
  
  private Color baseColor;
  private double contrast;
  private boolean useLightColor;
  
  private double transparency = 0.0; //opaque
  
  public ContrastMaterial(Color baseColor, double contrast) {
    this(baseColor, contrast, false);
  }
  
  public ContrastMaterial(Color baseColor, double contrast, boolean useLightColor) {
    this.baseColor = baseColor;
    this.contrast = contrast;
    this.useLightColor = useLightColor;
  }
  
  @Override
  public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPoint) {
    Color sourceColor = baseColor;
    
    // Işık rengini kullanma seçeneği
    if (useLightColor) {
      sourceColor = light.getColor();
    }
    
    int alfa = sourceColor.getAlpha ();
    double alpha = ((double)(alfa))/255.0;
    
    setTransparency (1-alpha);
    
    // Kontrast uygula
    return applyContrast(sourceColor, contrast);
  }
  
  private Color applyContrast(Color color, double contrastFactor) {
    float[] rgb = color.getRGBColorComponents(null);
    
    // Kontrast formülü: (renk - 0.5) * factor + 0.5
    float r = (float) Math.max(0.0, Math.min(1.0, (rgb[0] - 0.5f) * contrastFactor + 0.5f));
    float g = (float) Math.max(0.0, Math.min(1.0, (rgb[1] - 0.5f) * contrastFactor + 0.5f));
    float b = (float) Math.max(0.0, Math.min(1.0, (rgb[2] - 0.5f) * contrastFactor + 0.5f));
    
    return new Color(r, g, b);
  }
  
  @Override
  public double getReflectivity() {
    return 0.0;
  }
  
  @Override
  public double getTransparency() {
    return transparency;
  }
  
  public void setTransparency (double tnw){
    this.transparency = tnw;
  }
  
  @Override
  public double getIndexOfRefraction() {
    return 1.0;
  }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
    // Transform'a ihtiyaç yok
  }
  
  // Getter ve Setter metodları
  public Color getBaseColor() {
    return baseColor;
  }
  
  public void setBaseColor(Color baseColor) {
    this.baseColor = baseColor;
  }
  
  public double getContrast() {
    return contrast;
  }
  
  public void setContrast(double contrast) {
    this.contrast = Math.max(0.0, contrast);
  }
  
  public boolean isUseLightColor() {
    return useLightColor;
  }
  
  public void setUseLightColor(boolean useLightColor) {
    this.useLightColor = useLightColor;
  }
  
  // Yardımcı metod: Otomatik kontrast için orta gri renk
  public static Color getMiddleGray() {
    return new Color(0.5f, 0.5f, 0.5f);
  }
}