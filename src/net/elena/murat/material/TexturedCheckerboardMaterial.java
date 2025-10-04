package net.elena.murat.material;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import net.elena.murat.math.Matrix4;
import net.elena.murat.math.Point3;
import net.elena.murat.math.Vector3;
import net.elena.murat.light.Light;
import net.elena.murat.light.ElenaMuratAmbientLight;
import net.elena.murat.util.ColorUtil;

public class TexturedCheckerboardMaterial implements Material {
  
  private final Color color1;
  private final Color color2;
  private final double size;
  
  private final String text;
  private final Color textColor;
  private final Color gradientColor;
  private final String gradientType;
  private final Color bgColor;
  private final String fontFamily;
  private final int fontStyle;
  private final int fontSize;
  private final int textUOffset;
  private final int textVOffset;
  
  private final BufferedImage imageObject;
  private final int imageWidth;
  private final int imageHeight;
  private final int imageUOffset;
  private final int imageVOffset;
  
  private final double ambientCoeff;
  private final double diffuseCoeff;
  private final double specularCoeff;
  private final double shininess;
  private final Color specularColor;
  
  private final double reflectivity;
  private final double ior;
  private final double transparency;
  
  private Matrix4 objectInverseTransform;
  private BufferedImage texture;
  
  public TexturedCheckerboardMaterial(
    Color color1, Color color2, double size,
    String text, Color textColor, Color gradientColor, String gradientType, Color bgColor,
    String fontFamily, int fontStyle, int fontSize,
    int textUOffset, int textVOffset,
    BufferedImage imageObject, int imageWidth, int imageHeight,
    int imageUOffset, int imageVOffset,
    double ambientCoeff, double diffuseCoeff, double specularCoeff, double shininess, Color specularColor,
    double reflectivity, double ior, double transparency,
    Matrix4 objectInverseTransform) {
    
    this.color1 = color1;
    this.color2 = color2;
    this.size = size;
    
    this.text = text != null ? text.replaceAll("_", " ") : null;
    this.textColor = textColor;
    this.gradientColor = gradientColor;
    this.gradientType = gradientType != null ? gradientType.toLowerCase() : "horizontal";
    this.bgColor = bgColor != null ? bgColor : new Color(0, 0, 0, 0);
    
    this.fontFamily = fontFamily != null ? fontFamily.replaceAll("_", " ") : "Arial";
    this.fontStyle = fontStyle;
    this.fontSize = fontSize;
    this.textUOffset = textUOffset;
    this.textVOffset = textVOffset;
    
    this.imageObject = imageObject;
    this.imageWidth = imageWidth;
    this.imageHeight = imageHeight;
    this.imageUOffset = imageUOffset;
    this.imageVOffset = imageVOffset;
    
    this.ambientCoeff = ambientCoeff;
    this.diffuseCoeff = diffuseCoeff;
    this.specularCoeff = specularCoeff;
    this.shininess = shininess;
    this.specularColor = specularColor;
    
    this.reflectivity = reflectivity;
    this.ior = ior;
    this.transparency = transparency;
    
    this.objectInverseTransform = objectInverseTransform != null ? objectInverseTransform : new Matrix4();
    
    this.texture = createTexture();
  }
  
  private BufferedImage createTexture() {
    final int TEX_SIZE = 1024;
    BufferedImage img = new BufferedImage(TEX_SIZE, TEX_SIZE, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2d = img.createGraphics();
    
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    
    g2d.setBackground(bgColor);
    g2d.clearRect(0, 0, TEX_SIZE, TEX_SIZE);
    
    int cellSize = (int)(TEX_SIZE * size / 10.0);
    if (cellSize < 1) cellSize = 1;
    
    for (int y = 0; y < TEX_SIZE; y += cellSize) {
      for (int x = 0; x < TEX_SIZE; x += cellSize) {
        boolean useColor1 = ((x / cellSize) + (y / cellSize)) % 2 == 0;
        g2d.setColor(useColor1 ? color1 : color2);
        g2d.fillRect(x, y, cellSize, cellSize);
      }
    }
    
    if (imageObject != null) {
      int imgX = ((TEX_SIZE - imageWidth) / 2) + imageUOffset - imageWidth - (imageWidth / 4);
      int imgY = ((TEX_SIZE - imageHeight) / 2) + imageVOffset -(imageHeight/2);
      g2d.drawImage(imageObject, imgX, imgY, imageWidth, imageHeight, null);
    }
    
    if (text != null && !text.trim().isEmpty()) {
      Font font;
      try {
        font = new Font(fontFamily, fontStyle, fontSize);
        } catch (Exception e) {
        font = new Font("Arial", fontStyle, fontSize);
      }
      g2d.setFont(font);
      
      FontMetrics fm = g2d.getFontMetrics();
      int textWidth = fm.stringWidth(text);
      int textHeight = fm.getHeight();
      int ascent = fm.getAscent();
      
      int x = ((TEX_SIZE - textWidth) / 2) + textUOffset - textWidth - (textWidth / 4);
      int y = ((TEX_SIZE - textHeight) / 2) + (ascent * 2) + textVOffset;
      
      if (gradientColor != null) {
        GradientPaint gradient = createGradient(x, y - ascent, textWidth, textHeight);
        g2d.setPaint(gradient);
        } else {
        g2d.setColor(textColor);
      }
      
      g2d.drawString(text, x, y);
    }
    
    g2d.dispose();
    return img;
  }
  
  private GradientPaint createGradient(float x, float y, float width, float height) {
    switch (gradientType) {
      case "vertical":
        return new GradientPaint(x, y, textColor, x, y + height/2, gradientColor, true);
      case "diagonal":
        return new GradientPaint(x, y, textColor, x + width/3, y + height/5, gradientColor, true);
      case "horizontal":
      default:
        return new GradientPaint(x, y, textColor, x + width/3, y, gradientColor, true);
    }
  }
  
  @Override
  public Color getColorAt(Point3 worldPoint, Vector3 worldNormal, Light light, Point3 viewerPos) {
    if (texture == null) {
      return color1;
    }
    
    Point3 localPoint = objectInverseTransform.transformPoint(worldPoint);
    Vector3 localNormal = objectInverseTransform.inverseTransposeForNormal().transformVector(worldNormal).normalize();
    
    double phi = Math.atan2(localNormal.z, localNormal.x);
    double theta = Math.asin(localNormal.y);
    
    double u = 1.0 - (phi + Math.PI) / (2 * Math.PI);
    double v = (theta + Math.PI/2) / Math.PI;
    v = 1.0 - v;
    
    int texX = (int)(u * texture.getWidth()) % texture.getWidth();
    int texY = (int)(v * texture.getHeight());
    if (texY < 0) texY = 0;
    if (texY >= texture.getHeight()) texY = texture.getHeight() - 1;
    
    Color baseColor = new Color(texture.getRGB(texX, texY), true);
    
    if (baseColor.getAlpha() == 0) {
      double scaledX = localPoint.x * size;
      double scaledY = localPoint.y * size;
      double scaledZ = localPoint.z * size;
      
      int ix = (int)Math.floor(scaledX);
      int iy = (int)Math.floor(scaledY);
      int iz = (int)Math.floor(scaledZ);
      
      boolean isColor1 = ((ix + iy + iz) % 2 == 0);
      baseColor = isColor1 ? color1 : color2;
    }
    
    if (light == null || light instanceof ElenaMuratAmbientLight) {
      return ColorUtil.multiplyColor(baseColor, ambientCoeff);
    }
    
    Vector3 lightDir = light.getDirectionAt(worldPoint).normalize();
    double NdotL = Math.max(0, worldNormal.dot(lightDir));
    
    Color ambient = ColorUtil.multiplyColor(baseColor, ambientCoeff);
    Color diffuse = ColorUtil.multiplyColors(baseColor, light.getColor(), diffuseCoeff * NdotL * light.getIntensity());
    
    Vector3 viewDir = viewerPos.subtract(worldPoint).normalize();
    Vector3 reflectDir = lightDir.reflect(worldNormal);
    double RdotV = Math.max(0, reflectDir.dot(viewDir));
    double specFactor = Math.pow(RdotV, shininess) * light.getIntensity();
    Color specular = ColorUtil.multiplyColors(specularColor, light.getColor(), specularCoeff * specFactor);
    
    return ColorUtil.combineColors(ambient, diffuse, specular);
  }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
    if (tm != null) {
      this.objectInverseTransform = tm;
    }
  }
  
  @Override
  public double getReflectivity() {
    return reflectivity;
  }
  
  @Override
  public double getIndexOfRefraction() {
    return ior;
  }
  
  @Override
  public double getTransparency() {
    return transparency;
  }
  
  @Override
  public String toString() {
    return "TexturedCheckerboardMaterial[text=" + text + ", size=" + size + "]";
  }
  
}
