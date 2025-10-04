package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.math.*;
import net.elena.murat.light.*;
import net.elena.murat.util.ColorUtil;

/**
 * Material that adds color gradients to a standard checkerboard pattern
 */
public class GradientChessMaterial implements Material {
  private final Color baseColor1;
  private final Color baseColor2;
  private final double squareSize;
  private Matrix4 objectInverseTransform;
  
  // Light properties
  private final double ambientCoefficient;
  private final double diffuseCoefficient;
  private final double specularCoefficient;
  private final double shininess;
  private final double reflectivity;
  private final double ior;
  private final double transparency;
  private final Color specularColor;
  
  public GradientChessMaterial(Color baseColor1, Color baseColor2,
    double squareSize,
    double ambient, double diffuse, double specular,
    double shininess, double reflectivity,
    double ior, double transparency,
    Matrix4 objectInverseTransform) {
    this.baseColor1 = baseColor1;
    this.baseColor2 = baseColor2;
    this.squareSize = Math.max(0.1, squareSize);
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
  
  // Simple constructor
  public GradientChessMaterial(Color baseColor1, Color baseColor2,
    double squareSize,
    Matrix4 objectInverseTransform) {
    this(baseColor1, baseColor2, squareSize,
      0.1, 0.7, 0.2, 15.0, 0.1, 1.3, 0.05,
    objectInverseTransform);
  }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
    if (tm == null) tm = new Matrix4 ();
    this.objectInverseTransform = tm;
  }
  
  /**
   * Checkerboard pattern with applied gradient
   */
  private Color getPatternColor(double u, double v) {
    // Normalize UV coordinates
    double normalizedU = (u / squareSize) % 2.0;
    double normalizedV = (v / squareSize) % 2.0;
    
    // Basic checkerboard pattern
    boolean isColor1 = ((int)normalizedU + (int)normalizedV) % 2 == 0;
    
    // Gradient factors (0-1 range)
    double uGradient = normalizedU % 1.0;
    double vGradient = normalizedV % 1.0;
    
    // Base color selection
    Color baseColor = isColor1 ? baseColor1 : baseColor2;
    Color targetColor = isColor1 ? baseColor2 : baseColor1;
    
    // Apply diagonal gradient
    double gradientFactor = (uGradient + vGradient) / 2.0;
    
    return ColorUtil.blendColors(baseColor, targetColor, gradientFactor);
  }
  
  @Override
  public Color getColorAt(Point3 worldPoint, Vector3 worldNormal, Light light, Point3 viewerPos) {
    if (objectInverseTransform == null) {
      return Color.BLACK;
    }
    
    // Transform to object space
    Point3 localPoint = objectInverseTransform.transformPoint(worldPoint);
    Vector3 localNormal = objectInverseTransform.inverseTransposeForNormal()
    .transformVector(worldNormal).normalize();
    
    // UV mapping by dominant normal axis
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
    
    // Get pattern color
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
    
    // Light direction handling
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
    }
    else {
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
  
  @Override public double getReflectivity() { return reflectivity; }
  @Override public double getIndexOfRefraction() { return ior; }
  @Override public double getTransparency() { return transparency; }
  
}

/***
Material chessMat = new GradientChessMaterial(
new Color(50, 50, 200),   // Blue
new Color(200, 50, 50),   // Red
1.0,                      // Square size
plane.getInverseTransform()
);

Material pastelMat = new GradientChessMaterial(
new Color(240, 180, 220), // Pink
new Color(180, 220, 240), // Blue
0.8,                      // Smaller squares
plane.getInverseTransform()
);
 */
