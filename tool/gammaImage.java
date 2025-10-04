public BufferedImage applyGammaCorrection(BufferedImage image, float gamma) {
  int width = image.getWidth();
  int height = image.getHeight();
  BufferedImage corrected = new BufferedImage(width, height, image.getType());
  
  for (int y = 0; y < height; y++) {
    for (int x = 0; x < width; x++) {
      Color color = new Color(image.getRGB(x, y));
      
      // Gamma düzeltmesi uygula
      float r = color.getRed() / 255.0f;
      float g = color.getGreen() / 255.0f;
      float b = color.getBlue() / 255.0f;
      
      r = (float) Math.pow(r, 1.0f / gamma);
      g = (float) Math.pow(g, 1.0f / gamma);
      b = (float) Math.pow(b, 1.0f / gamma);
      
      // Yeni renk oluştur
      int red = (int) (r * 255);
      int green = (int) (g * 255);
      int blue = (int) (b * 255);
      
      // Sınırları kontrol et
      red = Math.min(255, Math.max(0, red));
      green = Math.min(255, Math.max(0, green));
      blue = Math.min(255, Math.max(0, blue));
      
      corrected.setRGB(x, y, new Color(red, green, blue).getRGB());
    }
  }
  
  return corrected;
}