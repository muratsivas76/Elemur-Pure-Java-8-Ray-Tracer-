public BufferedImage applyGammaCorrection(BufferedImage image, float gamma) {
  // Gamma 1.0 ise hiçbir işlem yapma (orijinal resmi döndür)
  if (gamma == 1.0f) {
    return image;
  }
  
  int width = image.getWidth();
  int height = image.getHeight();
  BufferedImage corrected = new BufferedImage(width, height, image.getType());
  
  for (int y = 0; y < height; y++) {
    for (int x = 0; x < width; x++) {
      Color color = new Color(image.getRGB(x, y));
      
      float r = color.getRed() / 255.0f;
      float g = color.getGreen() / 255.0f;
      float b = color.getBlue() / 255.0f;
      
      // Gamma düzeltmesi uygula (gamma=1 ise bu kısım atlanır)
      r = (float) Math.pow(r, 1.0f / gamma);
      g = (float) Math.pow(g, 1.0f / gamma);
      b = (float) Math.pow(b, 1.0f / gamma);
      
      int red = (int) (r * 255);
      int green = (int) (g * 255);
      int blue = (int) (b * 255);
      
      red = Math.min(255, Math.max(0, red));
      green = Math.min(255, Math.max(0, green));
      blue = Math.min(255, Math.max(0, blue));
      
      corrected.setRGB(x, y, new Color(red, green, blue).getRGB());
    }
  }
  
  return corrected;
}