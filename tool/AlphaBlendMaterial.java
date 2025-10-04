package net.elena.murat.material;

import net.elena.murat.math.*;
import net.elena.murat.light.Light;
import java.awt.Color;

public class AlphaBlendMaterial implements Material {
  
  private Color foregroundColor;
  private Color backgroundColor;
  private double alpha; // 0.0 (tamamen arkaplan) - 1.0 (tamamen önplan)
  private boolean useLightColorForBg;
  private boolean useLightColorForFg;
  
  public AlphaBlendMaterial(Color foregroundColor, Color backgroundColor, double alpha) {
    this(foregroundColor, backgroundColor, alpha, false, false);
  }
  
  public AlphaBlendMaterial(Color foregroundColor, Color backgroundColor, double alpha,
    boolean useLightColorForFg, boolean useLightColorForBg) {
    this.foregroundColor = foregroundColor;
    this.backgroundColor = backgroundColor;
    this.alpha = Math.max(0.0, Math.min(1.0, alpha));
    this.useLightColorForFg = useLightColorForFg;
    this.useLightColorForBg = useLightColorForBg;
  }
  
  @Override
  public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPoint) {
    Color fgColor = foregroundColor;
    Color bgColor = backgroundColor;
    
    // Işık rengini kullanma seçenekleri
    if (useLightColorForFg) {
      fgColor = light.getColor();
    }
    if (useLightColorForBg) {
      bgColor = light.getColor();
    }
    
    // Alpha blending uygula
    return alphaBlend(fgColor, bgColor, alpha);
  }
  
  private Color alphaBlend(Color foreground, Color background, double alpha) {
    float[] fg = foreground.getRGBColorComponents(null);
    float[] bg = background.getRGBColorComponents(null);
    
    // Alpha blending formülü: fg * alpha + bg * (1 - alpha)
    float r = (float) (fg[0] * alpha + bg[0] * (1 - alpha));
    float g = (float) (fg[1] * alpha + bg[1] * (1 - alpha));
    float b = (float) (fg[2] * alpha + bg[2] * (1 - alpha));
    
    return new Color(r, g, b);
  }
  
  @Override
  public double getReflectivity() {
    // Alpha'ya göre blend yapılabilir ama basit tutuyoruz
    return 0.0;
  }
  
  @Override
  public double getTransparency() {
    // Alpha değeri şeffaflık olarak da kullanılabilir
    return 1.0 - alpha;
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
  public Color getForegroundColor() {
    return foregroundColor;
  }
  
  public void setForegroundColor(Color foregroundColor) {
    this.foregroundColor = foregroundColor;
  }
  
  public Color getBackgroundColor() {
    return backgroundColor;
  }
  
  public void setBackgroundColor(Color backgroundColor) {
    this.backgroundColor = backgroundColor;
  }
  
  public double getAlpha() {
    return alpha;
  }
  
  public void setAlpha(double alpha) {
    this.alpha = Math.max(0.0, Math.min(1.0, alpha));
  }
  
  public boolean isUseLightColorForBg() {
    return useLightColorForBg;
  }
  
  public void setUseLightColorForBg(boolean useLightColorForBg) {
    this.useLightColorForBg = useLightColorForBg;
  }
  
  public boolean isUseLightColorForFg() {
    return useLightColorForFg;
  }
  
  public void setUseLightColorForFg(boolean useLightColorForFg) {
    this.useLightColorForFg = useLightColorForFg;
  }
  
  // Yardımcı metod: Premultiplied alpha blending (daha iyi sonuçlar için)
  private Color premultipliedAlphaBlend(Color foreground, Color background, double alpha) {
    float[] fg = foreground.getRGBColorComponents(null);
    float[] bg = background.getRGBColorComponents(null);
    
    float r = (float) (fg[0] * alpha + bg[0] * (1 - alpha));
    float g = (float) (fg[1] * alpha + bg[1] * (1 - alpha));
    float b = (float) (fg[2] * alpha + bg[2] * (1 - alpha));
    
    return new Color(r, g, b);
  }
}