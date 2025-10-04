public BufferedImage sharpenImage(BufferedImage image) {
  int width = image.getWidth();
  int height = image.getHeight();
  BufferedImage sharpened = new BufferedImage(width, height, image.getType());
  
  // Keskinleştirme matrisi
  float[] sharpenMatrix = {
    0.0f, -0.2f, 0.0f,
    -0.2f, 1.8f, -0.2f,
    0.0f, -0.2f, 0.0f
  };
  
  for (int y = 1; y < height - 1; y++) {
    for (int x = 1; x < width - 1; x++) {
      float red = 0, green = 0, blue = 0;
      int matrixIndex = 0;
      
      // 3x3 matris uygula
      for (int ky = -1; ky <= 1; ky++) {
        for (int kx = -1; kx <= 1; kx++) {
          Color pixel = new Color(image.getRGB(x + kx, y + ky));
          float factor = sharpenMatrix[matrixIndex++];
          
          red += pixel.getRed() * factor;
          green += pixel.getGreen() * factor;
          blue += pixel.getBlue() * factor;
        }
      }
      
      // Sınırları kontrol et
      int r = (int) Math.min(255, Math.max(0, red));
      int g = (int) Math.min(255, Math.max(0, green));
      int b = (int) Math.min(255, Math.max(0, blue));
      
      sharpened.setRGB(x, y, new Color(r, g, b).getRGB());
    }
  }
  
  return sharpened;
}