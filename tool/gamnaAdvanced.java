Color gammaCorrection(Color base, float gamma) {
  // Gamma değerini kontrol et (0'dan büyük olmalı)
  if (gamma <= 0) gamma = 2.2f;
  
  // Renk bileşenlerini al ve normalize et
  float r = base.getRed() / 255.0f;
  float g = base.getGreen() / 255.0f;
  float b = base.getBlue() / 255.0f;
  float a = base.getAlpha() / 255.0f;
  
  // Gamma düzeltmesi uygula
  r = (float) Math.pow(r, 1.0f / gamma);
  g = (float) Math.pow(g, 1.0f / gamma);
  b = (float) Math.pow(b, 1.0f / gamma);
  
  // Değerleri [0, 255] aralığına dönüştür ve kırp
  int red = (int) (Math.min(Math.max(r * 255, 0), 255));
  int green = (int) (Math.min(Math.max(g * 255, 0), 255));
  int blue = (int) (Math.min(Math.max(b * 255, 0), 255));
  int alpha = (int) (a * 255);
  
  return new Color(red, green, blue, alpha);
}