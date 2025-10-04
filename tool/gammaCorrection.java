Color gammaCorrection(Color base, float gamma) {
  // Renk bileşenlerini [0, 1] aralığına normalize et
  float r = base.getRed() / 255.0f;
  float g = base.getGreen() / 255.0f;
  float b = base.getBlue() / 255.0f;
  
  // sRGB'den lineer RGB'ye dönüşüm
  r = (r <= 0.04045f) ? r / 12.92f : (float) Math.pow((r + 0.055f) / 1.055f, 2.4f);
  g = (g <= 0.04045f) ? g / 12.92f : (float) Math.pow((g + 0.055f) / 1.055f, 2.4f);
  b = (b <= 0.04045f) ? b / 12.92f : (float) Math.pow((b + 0.055f) / 1.055f, 2.4f);
  
  // Gamma düzeltmesi uygula
  r = (float) Math.pow(r, 1.0f / gamma);
  g = (float) Math.pow(g, 1.0f / gamma);
  b = (float) Math.pow(b, 1.0f / gamma);
  
  // Lineer RGB'den sRGB'ye geri dönüşüm
  r = (r <= 0.0031308f) ? 12.92f * r : (1.055f * (float) Math.pow(r, 1.0f / 2.4f)) - 0.055f;
  g = (g <= 0.0031308f) ? 12.92f * g : (1.055f * (float) Math.pow(g, 1.0f / 2.4f)) - 0.055f;
  b = (b <= 0.0031308f) ? 12.92f * b : (1.055f * (float) Math.pow(b, 1.0f / 2.4f)) - 0.055f;
  
  // Değerleri [0, 255] aralığına taşı ve renk oluştur
  return new Color(
    Math.min(255, (int) (r * 255 + 0.5f)),
    Math.min(255, (int) (g * 255 + 0.5f)),
    Math.min(255, (int) (b * 255 + 0.5f))
  );
}