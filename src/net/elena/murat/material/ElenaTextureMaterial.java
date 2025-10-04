package net.elena.murat.material;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

import net.elena.murat.math.*;
import net.elena.murat.light.*;
import net.elena.murat.util.ColorUtil;

public class ElenaTextureMaterial implements Material {
  private final BufferedImage texture;
  private Matrix4 objectInverseTransform;
  
  // Material properties
  private final double ambientCoeff;
  private final double diffuseCoeff;
  private final double specularCoeff;
  private final double shininess;
  private final Color specularColor;
  private final double reflectivity;
  private final double ior;
  private final double transparency;
  
  public ElenaTextureMaterial(String imagePath, Matrix4 objectInverseTransform) throws IOException {
    this(imagePath, 0.1, 0.7, 0.2, 10.0, Color.WHITE, 0.05, 1.0, 0.0, objectInverseTransform);
  }
  
  public ElenaTextureMaterial(String imagePath,
    double ambientCoeff, double diffuseCoeff, double specularCoeff,
    double shininess, Color specularColor,
    double reflectivity, double ior, double transparency,
    Matrix4 objectInverseTransform) throws IOException {
    this.texture = ImageIO.read(new File(imagePath));
    this.objectInverseTransform = objectInverseTransform;
    this.ambientCoeff = ambientCoeff;
    this.diffuseCoeff = diffuseCoeff;
    this.specularCoeff = specularCoeff;
    this.shininess = shininess;
    this.specularColor = specularColor;
    this.reflectivity = reflectivity;
    this.ior = ior;
    this.transparency = transparency;
  }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
    if (tm == null) tm = new Matrix4 ();
    this.objectInverseTransform = tm;
  }
  
  @Override
  public Color getColorAt(Point3 worldPoint, Vector3 worldNormal, Light light, Point3 viewerPos) {
    // 1. Get texture color
    Point3 localPoint = objectInverseTransform.transformPoint(worldPoint);
    Color textureColor = getSphericalTextureColor(localPoint);
    
    // 2. Handle light properties
    LightProperties props = LightProperties.getLightProperties(light, worldPoint);
    if (props == null) return textureColor;
    
    // 3. Calculate Phong components
    Color ambient = ColorUtil.multiplyColors(textureColor, props.color, ambientCoeff);
    
    if (light instanceof ElenaMuratAmbientLight) {
      return ambient;
    }
    
    double NdotL = Math.max(0, worldNormal.dot(props.direction));
    Color diffuse = ColorUtil.multiplyColors(textureColor, props.color, diffuseCoeff * NdotL * props.intensity);
    
    Vector3 viewDir = viewerPos.subtract(worldPoint).normalize();
    Vector3 reflectDir = props.direction.negate().reflect(worldNormal);
    double RdotV = Math.max(0, reflectDir.dot(viewDir));
    double specFactor = Math.pow(RdotV, shininess) * props.intensity;
    Color specular = ColorUtil.multiplyColors(specularColor, props.color, specularCoeff * specFactor);
    
    return ColorUtil.combineColors(ambient, diffuse, specular);
  }
  
  private Color getSphericalTextureColor(Point3 localPoint) {
    Vector3 normal = localPoint.toVector3().normalize();
    
    // Spherical mapping
    double u = 0.5 + Math.atan2(normal.z, normal.x) / (2 * Math.PI);
    double v = 0.5 - Math.asin(normal.y) / Math.PI;
    
    // Bilinear sampling
    return sampleTexture(u, v);
  }
  
  private Color sampleTexture(double u, double v) {
    double x = u * (texture.getWidth() - 1);
    double y = v * (texture.getHeight() - 1);
    
    int x0 = (int)Math.floor(x);
    int y0 = (int)Math.floor(y);
    int x1 = Math.min(x0 + 1, texture.getWidth() - 1);
    int y1 = Math.min(y0 + 1, texture.getHeight() - 1);
    
    double fracX = x - x0;
    double fracY = y - y0;
    
    Color c00 = new Color(texture.getRGB(x0, y0));
    Color c10 = new Color(texture.getRGB(x1, y0));
    Color c01 = new Color(texture.getRGB(x0, y1));
    Color c11 = new Color(texture.getRGB(x1, y1));
    
    return ColorUtil.bilinearInterpolate(c00, c10, c01, c11, fracX, fracY);
  }
  
  @Override public double getReflectivity() { return reflectivity; }
  @Override public double getIndexOfRefraction() { return ior; }
  @Override public double getTransparency() { return transparency; }
}

/***
try {
  Material elenaMat = new ElenaTextureMaterial(
"textures/elena.png",
sphere.getInverseTransform()
);
sphere.setMaterial(elenaMat);
} catch (IOException e) {
System.err.println("Texture loading error: " + e.getMessage());
sphere.setMaterial(new DiffuseMaterial(Color.PINK)); // Fallback
}
 */
