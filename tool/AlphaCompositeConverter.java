import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class AlphaCompositeConverter {
  
  public static void main(String[] args) {
    if (args.length != 1) {
      System.out.println("Kullanım: java AlphaCompositeConverter <resim-dosyasi>");
      System.out.println("Örnek: java AlphaCompositeConverter resim.png");
      return;
    }
    
    String inputPath = args[0];
    
    try {
      // Resmi yükle
      File inputFile = new File(inputPath);
      BufferedImage originalImage = ImageIO.read(inputFile);
      
      if (originalImage == null) {
        System.out.println("Resim yüklenemedi: " + inputPath);
        return;
      }
      
      System.out.println("Resim yüklendi: " + originalImage.getWidth() + "x" + originalImage.getHeight());
      
      // Alpha composite uygula (%20 opaklık = %80 şeffaflık)
      BufferedImage transparentImage = applyAlphaComposite(originalImage, 0.8);
      
      // Çıktı dosya adını oluştur
      String outputPath = getOutputFilePath(inputPath);
      File outputFile = new File(outputPath);
      
      // Kaydet
      ImageIO.write(transparentImage, "png", outputFile);
      
      System.out.println("İşlem tamamlandı! Kaydedildi: " + outputPath);
      System.out.println("Orijinal alpha: " + getAlphaValue(originalImage, 10, 10));
      System.out.println("Yeni alpha: " + getAlphaValue(transparentImage, 10, 10));
      
      } catch (Exception e) {
      System.out.println("Hata: " + e.getMessage());
      e.printStackTrace();
    }
  }
  
  /**
   * Resme alpha composite uygular
   * @param srcImage Orijinal resim
   * @param transparency Seffaflık değeri (0.0 = opak, 1.0 = tamamen şeffaf)
   * @return Alpha composite uygulanmış resim
   */
  public static BufferedImage applyAlphaComposite(BufferedImage srcImage, double transparency) {
    final int width = srcImage.getWidth();
    final int height = srcImage.getHeight();
    
    // ARGB formatında yeni resim oluştur
    BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    
    // Tüm pikselleri işle
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        int originalRGB = srcImage.getRGB(x, y);
        
        // Alpha değerini ayarla (%20 opaklık = 255 * 0.2 = 51)
        int newAlpha = (int)(51); // %20 opaklık
        
        // RGB bileşenlerini al
        int red = (originalRGB >> 16) & 0xFF;
        int green = (originalRGB >> 8) & 0xFF;
        int blue = originalRGB & 0xFF;
        
        // Yeni ARGB değerini oluştur
        int newRGB = (newAlpha << 24) | (red << 16) | (green << 8) | blue;
        result.setRGB(x, y, newRGB);
      }
    }
    
    return result;
  }
  
  /**
   * Çıktı dosya yolunu oluşturur
   * @param inputPath Girdi dosya yolu
   * @return Çıktı dosya yolu
   */
  private static String getOutputFilePath(String inputPath) {
    File inputFile = new File(inputPath);
    String fileName = inputFile.getName();
    String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
    String extension = fileName.substring(fileName.lastIndexOf('.') + 1);
    
    // Orijinal dosya PNG değilse PNG'ye çevir
    if (!extension.equalsIgnoreCase("png")) {
      return inputFile.getParent() + File.separator + baseName + "_alpha.png";
    }
    
    return inputFile.getParent() + File.separator + baseName + "_transparent.png";
  }
  
  /**
   * Belirli bir pikselin alpha değerini alır
   */
  private static int getAlphaValue(BufferedImage image, int x, int y) {
    if (x >= image.getWidth() || y >= image.getHeight()) {
      x = Math.min(x, image.getWidth() - 1);
      y = Math.min(y, image.getHeight() - 1);
    }
    int rgb = image.getRGB(x, y);
    return (rgb >> 24) & 0xFF;
  }
  
}
