package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.math.*;
import net.elena.murat.light.*;

/**
 * Material that creates a rectangular checkerboard pattern.
 * Rectangular version of the classic checkerboard.
 */
public class RectangleCheckerMaterial implements Material {
  private final Color color1;
  private final Color color2;
  private final double rectWidth;
  private final double rectHeight;
  private Matrix4 objectInverseTransform;
  
  // Lighting coefficients
  private final double ambientCoefficient;
  private final double diffuseCoefficient;
  private final double specularCoefficient;
  private final double shininess;
  private final double reflectivity;
  private final double ior;
  private final double transparency;
  private final Color specularColor;
  
  /**
   * Full constructor
   * @param color1 First color
   * @param color2 Second color
   * @param rectWidth Dikdörtgen genişliği (tekrar aralığı)
   * @param rectHeight Dikdörtgen yüksekliği (tekrar aralığı)
   * @param ambient Ambient coefficient
   * @param diffuse Diffuse coefficient
   * @param specular Specular coefficient
   * @param shininess Shininess exponent
   * @param reflectivity Reflectivity amount (0-1)
   * @param ior Index of refraction
   * @param transparency Transparency amount (0-1)
   * @param objectInverseTransform Object's inverse transform
   */
  public RectangleCheckerMaterial(Color color1, Color color2,
    double rectWidth, double rectHeight,
    double ambient, double diffuse, double specular,
    double shininess, double reflectivity,
    double ior, double transparency,
    Matrix4 objectInverseTransform) {
    this.color1 = color1;
    this.color2 = color2;
    this.rectWidth = rectWidth;
    this.rectHeight = rectHeight;
    this.ambientCoefficient = ambient;
    this.diffuseCoefficient = diffuse;
    this.specularCoefficient = specular;
    this.shininess = shininess;
    this.reflectivity = reflectivity;
    this.ior = ior;
    this.transparency = transparency;
    this.objectInverseTransform = objectInverseTransform;
    this.specularColor = Color.WHITE;
  }
  
  /**
   * Simplified constructor with default Phong parameters
   */
  public RectangleCheckerMaterial(Color color1, Color color2,
    double rectWidth, double rectHeight,
    Matrix4 objectInverseTransform) {
    this(color1, color2, rectWidth, rectHeight,
      0.1, 0.7, 0.2, 10.0, 0.0, 1.0, 0.0,
    objectInverseTransform);
  }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
    if (tm == null) tm = new Matrix4 ();
    this.objectInverseTransform = tm;
  }
  
  /**
   * Generates the checker pattern color
   */
  private Color getPatternColor(double u, double v) {
    // Adjust for rectangle dimensions
    double uScaled = u / rectWidth;
    double vScaled = v / rectHeight;
    
    // Handle negative coordinates correctly
    uScaled = (uScaled % 2.0 + 2.0) % 2.0;
    vScaled = (vScaled % 2.0 + 2.0) % 2.0;
    
    int xSeg = (int)Math.floor(uScaled);
    int ySeg = (int)Math.floor(vScaled);
    
    return (xSeg + ySeg) % 2 == 0 ? color1 : color2;
  }
  
  @Override
  public Color getColorAt(Point3 worldPoint, Vector3 worldNormal, Light light, Point3 viewerPos) {
    if (objectInverseTransform == null) {
      return Color.BLACK;
    }
    
    // Transform to object space
    Point3 localPoint = objectInverseTransform.transformPoint(worldPoint);
    Vector3 localNormal = objectInverseTransform.inverseTransposeForNormal().transformVector(worldNormal).normalize();
    
    // Determine dominant axis for UV mapping
    double absX = Math.abs(localNormal.x);
    double absY = Math.abs(localNormal.y);
    double absZ = Math.abs(localNormal.z);
    
    double u, v;
    if (absX > absY && absX > absZ) {
      u = localPoint.y;
      v = localPoint.z;
      } else if (absY > absX && absY > absZ) {
      u = localPoint.x;
      v = localPoint.z;
      } else {
      u = localPoint.x;
      v = localPoint.y;
    }
    
    // Get base pattern color
    Color baseColor = getPatternColor(u, v);
    
    // Lighting calculations (Phong model)
    Color lightColor = light.getColor();
    double attenuatedIntensity = 0.0;
    
    // Ambient component
    int rAmbient = (int)(baseColor.getRed() * ambientCoefficient * lightColor.getRed() / 255.0);
    int gAmbient = (int)(baseColor.getGreen() * ambientCoefficient * lightColor.getGreen() / 255.0);
    int bAmbient = (int)(baseColor.getBlue() * ambientCoefficient * lightColor.getBlue() / 255.0);
    
    if (light instanceof ElenaMuratAmbientLight) {
      return new Color(
        Math.min(255, rAmbient),
        Math.min(255, gAmbient),
        Math.min(255, bAmbient)
      );
    }
    
    // Handle different light types
    Vector3 lightDirection;
    if (light instanceof MuratPointLight) {
      MuratPointLight pLight = (MuratPointLight) light;
      lightDirection = pLight.getPosition().subtract(worldPoint).normalize();
      attenuatedIntensity = pLight.getAttenuatedIntensity(worldPoint);
      } else if (light instanceof ElenaDirectionalLight) {
      ElenaDirectionalLight dLight = (ElenaDirectionalLight) light;
      lightDirection = dLight.getDirection().negate().normalize();
      attenuatedIntensity = dLight.getIntensity();
      } else if (light instanceof PulsatingPointLight) {
      PulsatingPointLight ppLight = (PulsatingPointLight) light;
      lightDirection = ppLight.getPosition().subtract(worldPoint).normalize();
      attenuatedIntensity = ppLight.getAttenuatedIntensity(worldPoint);
      } else if (light instanceof SpotLight) {
      SpotLight sLight = (SpotLight) light;
      lightDirection = sLight.getDirectionAt(worldPoint);
      attenuatedIntensity = sLight.getAttenuatedIntensity(worldPoint);
      } else if (light instanceof BioluminescentLight) {
      BioluminescentLight bLight = (BioluminescentLight) light;
      lightDirection = bLight.getDirectionAt(worldPoint);
      attenuatedIntensity = bLight.getAttenuatedIntensity(worldPoint);
      } else if (light instanceof BlackHoleLight) {
      BlackHoleLight bhLight = (BlackHoleLight) light;
      lightDirection = bhLight.getDirectionAt(worldPoint);
      attenuatedIntensity = bhLight.getAttenuatedIntensity(worldPoint);
      } else if (light instanceof FractalLight) {
      FractalLight fLight = (FractalLight) light;
      lightDirection = fLight.getDirectionAt(worldPoint);
      attenuatedIntensity = fLight.getAttenuatedIntensity(worldPoint);
      } else {
      return Color.BLACK;
    }
    
    // Diffuse component
    double NdotL = Math.max(0, worldNormal.dot(lightDirection));
    int rDiffuse = (int)(baseColor.getRed() * diffuseCoefficient * lightColor.getRed() / 255.0 * attenuatedIntensity * NdotL);
    int gDiffuse = (int)(baseColor.getGreen() * diffuseCoefficient * lightColor.getGreen() / 255.0 * attenuatedIntensity * NdotL);
    int bDiffuse = (int)(baseColor.getBlue() * diffuseCoefficient * lightColor.getBlue() / 255.0 * attenuatedIntensity * NdotL);
    
    // Specular component
    Vector3 viewDir = viewerPos.subtract(worldPoint).normalize();
    Vector3 reflectionVec = lightDirection.negate().reflect(worldNormal);
    double RdotV = Math.max(0, reflectionVec.dot(viewDir));
    double specFactor = Math.pow(RdotV, shininess);
    
    int rSpecular = (int)(specularColor.getRed() * specularCoefficient * lightColor.getRed() / 255.0 * attenuatedIntensity * specFactor);
    int gSpecular = (int)(specularColor.getGreen() * specularCoefficient * lightColor.getGreen() / 255.0 * attenuatedIntensity * specFactor);
    int bSpecular = (int)(specularColor.getBlue() * specularCoefficient * lightColor.getBlue() / 255.0 * attenuatedIntensity * specFactor);
    
    // Combine components
    int finalR = Math.min(255, rAmbient + rDiffuse + rSpecular);
    int finalG = Math.min(255, gAmbient + gDiffuse + gSpecular);
    int finalB = Math.min(255, bAmbient + bDiffuse + bSpecular);
    
    return new Color(finalR, finalG, finalB);
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
  
}

/***
Material horizontalRects = new RectangleCheckerMaterial(
Color.WHITE, Color.BLACK,
2.0, 1.0, // Width 2.0, height 1.0
plane.getInverseTransform()
);

Material verticalRects = new RectangleCheckerMaterial(
Color.RED, Color.BLUE,
1.0, 3.0, // Width 1.0, height 3.0
plane.getInverseTransform()
);

Material customChecker = new RectangleCheckerMaterial(
new Color(200, 150, 50), // Beige
new Color(50, 100, 50),  // Dark green
1.5, 0.8,               // Dimensions
0.15, 0.8, 0.3,         // Ambient, Diffuse, Specular
25.0, 0.1,              // Shininess, Reflectivity
1.3, 0.02,              // IOR, Transparency
plane.getInverseTransform()
);
 */