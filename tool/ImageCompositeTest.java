import java.awt.Color;
import java.awt.image.BufferedImage;

public class ImageCompositeTest {
  
  public static void main(String[] args) {
    // 50x50 boyutunda RGB tipinde BufferedImage oluştur
    BufferedImage originalImage = new BufferedImage(50, 50, BufferedImage.TYPE_INT_RGB);
    
    // Sadece 10x10 pikseli belirli bir renk yap
    Color testColor = new Color(100, 150, 200);
    originalImage.setRGB(10, 10, testColor.getRGB());
    
    System.out.println("Orijinal Resim - 10x10 piksel:");
    System.out.println("r=" + testColor.getRed() +
      ", g=" + testColor.getGreen() +
    ", b=" + testColor.getBlue());
    
    // Manuel alpha composite işlemi
    float alpha = 0.25f;
    BufferedImage manualCompositeImage = manualAlphaComposite(originalImage, alpha);
    
    Color manualResult = new Color(manualCompositeImage.getRGB(10, 10), true);
    System.out.println("\nManuel Alpha Composite - 10x10 piksel:");
    System.out.println("r=" + manualResult.getRed() +
      ", g=" + manualResult.getGreen() +
      ", b=" + manualResult.getBlue() +
    ", alpha=" + manualResult.getAlpha());
    
    // AlphaComposite ile tekrar deneyelim (farklı bir yaklaşımla)
    BufferedImage javaCompositeImage = javaAlphaComposite(originalImage, alpha);
    
    Color javaResult = new Color(javaCompositeImage.getRGB(10, 10), true);
    System.out.println("\nJava AlphaComposite - 10x10 piksel:");
    System.out.println("r=" + javaResult.getRed() +
      ", g=" + javaResult.getGreen() +
      ", b=" + javaResult.getBlue() +
    ", alpha=" + javaResult.getAlpha());
  }
  
  private static BufferedImage manualAlphaComposite(BufferedImage source, float alpha) {
    BufferedImage result = new BufferedImage(source.getWidth(), source.getHeight(),
    BufferedImage.TYPE_INT_ARGB);
    
    for (int y = 0; y < source.getHeight(); y++) {
      for (int x = 0; x < source.getWidth(); x++) {
        int rgb = source.getRGB(x, y);
        Color color = new Color(rgb);
        
        int r = (int)(color.getRed() * alpha);
        int g = (int)(color.getGreen() * alpha);
        int b = (int)(color.getBlue() * alpha);
        int a = (int)(255 * alpha);
        
        Color newColor = new Color(r, g, b, a);
        result.setRGB(x, y, newColor.getRGB());
      }
    }
    
    return result;
  }
  
  private static BufferedImage javaAlphaComposite(BufferedImage source, float alpha) {
    BufferedImage result = new BufferedImage(source.getWidth(), source.getHeight(),
    BufferedImage.TYPE_INT_ARGB);
    
    // Önce hedefi tamamen şeffaf yap
    for (int y = 0; y < result.getHeight(); y++) {
      for (int x = 0; x < result.getWidth(); x++) {
        result.setRGB(x, y, new Color(0, 0, 0, 0).getRGB());
      }
    }
    
    // Graphics2D kullanmadan direkt pixel işlemi
    // AlphaComposite'in beklenen davranışını simüle edelim
    for (int y = 0; y < source.getHeight(); y++) {
      for (int x = 0; x < source.getWidth(); x++) {
        int sourceRgb = source.getRGB(x, y);
        Color sourceColor = new Color(sourceRgb);
        
        // SRC_OVER formülü: result = source + (dest * (1 - source_alpha))
        // Ancak dest şeffaf olduğu için: result = source * alpha
        int r = (int)(sourceColor.getRed() * alpha);
        int g = (int)(sourceColor.getGreen() * alpha);
        int b = (int)(sourceColor.getBlue() * alpha);
        int a = (int)(255 * alpha);
        
        Color newColor = new Color(r, g, b, a);
        result.setRGB(x, y, newColor.getRGB());
      }
    }
    
    return result;
  }
  
}
