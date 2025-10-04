package net.elena.murat.material.pbr;

import java.awt.Color;

import net.elena.murat.math.*;
import net.elena.murat.light.*;
import net.elena.murat.util.ColorUtil;
import net.elena.murat.util.MathUtil;

public class HolographicPBRMaterial implements PBRCapableMaterial {
  private final Color baseColor;
  private final double rainbowSpeed;
  private final double scanLineDensity;
  private final double glitchIntensity;
  private final double timeOffset;
  private final double distortionFactor;
  private final double dataDensity;
  
  // Special effect modes
  public enum HologramMode {
    STANDARD, CYBERPUNK, MATRIX, QUANTUM
  }
  
  public HolographicPBRMaterial() {
    this(new Color(80, 255, 255, 150), 2.5, 25.0, 0.3, 0.0, 0.5, 10.0);
  }
  
  public HolographicPBRMaterial(Color baseColor, double rainbowSpeed, double scanLineDensity,
    double glitchIntensity, double timeOffset,
    double distortionFactor, double dataDensity) {
    this.baseColor = baseColor;
    this.rainbowSpeed = rainbowSpeed;
    this.scanLineDensity = scanLineDensity;
    this.glitchIntensity = glitchIntensity;
    this.timeOffset = timeOffset;
    this.distortionFactor = distortionFactor;
    this.dataDensity = dataDensity;
  }
  
  @Override
  public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPos) {
    double time = System.currentTimeMillis() * 0.001 + timeOffset;
    
    // 1. Space-Time Distortion
    Point3 distortedPoint = applySpaceTimeDistortion(point, time);
    
    // 2. Dynamic Color Spectrum
    Color spectralColor = calculateSpectralColor(distortedPoint, time);
    
    // 3. Holographic Scan Lines
    double scanLineEffect = calculateScanLines(distortedPoint, time);
    
    // 4. Quantum Noise
    double quantumNoise = calculateQuantumNoise(distortedPoint, time);
    
    // 5. Data Packet Effect (Matrix-style)
    Color dataEffect = calculateDataEffect(distortedPoint, time);
    
    // 6. Anisotropic Specular
    double anisotropicSpecular = calculateAnisotropicSpecular(point, normal, light, viewerPos, time);
    
    // 7. Glitch Effect
    Color glitchEffect = applyGlitchEffects(spectralColor, point, time);
    
    // 8. Final Color Composition
    return composeFinalColor(
      glitchEffect,
      dataEffect,
      scanLineEffect,
      quantumNoise,
      anisotropicSpecular
    );
  }
  
  // --- Special Effect Methods ---
  private Point3 applySpaceTimeDistortion(Point3 p, double time) {
    return new Point3(
      p.x + Math.sin(p.y * 0.5 + time) * distortionFactor * 0.1,
      p.y + Math.cos(p.x * 0.3 + time * 1.3) * distortionFactor * 0.1,
      p.z
    );
  }
  
  private Color calculateSpectralColor(Point3 p, double time) {
    double hue = (p.x * rainbowSpeed + time) % 1.0;
    double saturation = 0.7 + Math.sin(p.y * 3 + time) * 0.2;
    double brightness = 0.8 + Math.cos(p.z * 4 - time * 2) * 0.1;
    return Color.getHSBColor((float)hue, (float)saturation, (float)brightness);
  }
  
  private double calculateScanLines(Point3 p, double time) {
    double verticalLines = Math.sin(p.y * scanLineDensity + time) * 0.4 + 0.6;
    double horizontalLines = Math.pow(Math.sin(p.x * scanLineDensity * 0.3 + time * 0.7), 2);
    return verticalLines * horizontalLines;
  }
  
  private double calculateQuantumNoise(Point3 p, double time) {
    return MathUtil.random(p.x * 1000 + p.y * 100 + p.z * 10 + time) * 0.2;
  }
  
  private Color calculateDataEffect(Point3 p, double time) {
    if (MathUtil.random(p.z * 1000 + time) > 0.9) {
      double greenValue = 0.3 + MathUtil.random(p.x * 100) * 0.7;
      return new Color(0, (int)(greenValue * 255), 0, 100);
    }
    return new Color(0, 0, 0, 0);
  }
  
  private double calculateAnisotropicSpecular(Point3 p, Vector3 n, Light light, Point3 v, double time) {
    Vector3 h = light.getDirectionTo(p).add(new Vector3(v, p)).normalize();
    Vector3 tangent = new Vector3(
      Math.sin(p.y * 10 + time),
      0,
      Math.cos(p.y * 10 + time)
    ).normalize();
    double dotTH = tangent.dot(h);
    return Math.pow(1 - dotTH * dotTH, 10) * 2.0;
  }
  
  private Color applyGlitchEffects(Color base, Point3 p, double time) {
    if (MathUtil.random(p.x * 500 + p.y * 300 + time) < glitchIntensity * 0.1) {
      return ColorUtil.invert(base);
    }
    if (MathUtil.random(p.y * 400 + p.z * 200 + time * 2) < glitchIntensity * 0.05) {
      return new Color(
        base.getGreen(),
        base.getBlue(),
        base.getRed(),
        base.getAlpha()
      );
    }
    return base;
  }
  
  private Color composeFinalColor(Color base, Color data, double scanLine, double noise, double specular) {
    // Base color + scan lines
    Color c1 = ColorUtil.multiply(base, (float)(scanLine + noise));
    
    // Add data effect
    Color c2 = ColorUtil.add(c1, data);
    
    // Add specular (white highlight)
    return ColorUtil.add(c2, ColorUtil.scale(Color.WHITE, specular));
  }
  
  // --- PBR Properties (Customized for Holographic Material) ---
  @Override public Color getAlbedo() {
    return ColorUtil.setAlpha(baseColor, 100);
  }
  @Override public double getRoughness() { return 0.15; }
  @Override public double getMetalness() { return 0.7; }
  @Override public MaterialType getMaterialType() { return MaterialType.ANISOTROPIC; }
  @Override public double getReflectivity() { return 0.85; }
  @Override public double getIndexOfRefraction() { return 1.15; }
  @Override public double getTransparency() { return 0.4; }

  @Override
  public void setObjectTransform(Matrix4 tm) {
  }
  
  // --- Easy Configuration with Builder Pattern ---
  public static class Builder {
    private Color baseColor = new Color(100, 200, 255, 150);
    private double rainbowSpeed = 2.0;
    private double scanLineDensity = 20.0;
    private double glitchIntensity = 0.3;
    private double timeOffset = 0.0;
    private double distortionFactor = 0.5;
    private double dataDensity = 10.0;
    
    public Builder withBaseColor(Color color) {
      this.baseColor = color;
      return this;
    }
    
    public Builder withCyberpunkStyle() {
      this.rainbowSpeed = 3.5;
      this.scanLineDensity = 30.0;
      this.glitchIntensity = 0.7;
      return this;
    }
    
    public Builder withMatrixStyle() {
      this.baseColor = new Color(0, 255, 0, 100);
      this.dataDensity = 20.0;
      return this;
    }
    
    public HolographicPBRMaterial build() {
      return new HolographicPBRMaterial(
        baseColor, rainbowSpeed, scanLineDensity,
        glitchIntensity, timeOffset, distortionFactor, dataDensity
      );
    }
  }
  
  // --- Usage Examples ---
  public static void demo() {
    // Standard hologram
    HolographicPBRMaterial standard = new HolographicPBRMaterial();
    
    // Cyberpunk style
    HolographicPBRMaterial cyberpunk = new Builder()
    .withBaseColor(new Color(255, 50, 200, 180))
    .withCyberpunkStyle()
    .build();
    
    // Matrix style
    HolographicPBRMaterial matrix = new Builder()
    .withMatrixStyle()
    .build();
  }
  
}
