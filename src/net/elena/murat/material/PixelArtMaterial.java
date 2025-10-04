package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.math.*;
import net.elena.murat.light.*;

/**
 * Pixel art material with correct lights
 */
public class PixelArtMaterial implements Material {
  private final Color[] palette;
  private final double pixelSize;
  private Matrix4 objectInverseTransform;
  
  private final double ambientCoefficient;
  private final double diffuseCoefficient;
  private final double specularCoefficient;
  private final double shininess;
  private final double reflectivity;
  private final double transparency;
  private final double ior;
  
  public PixelArtMaterial(Color[] palette, double pixelSize,
    double ambient, double diffuse, double specular,
    double shininess, double reflectivity,
    double ior, double transparency,
    Matrix4 objectInverseTransform) {
    this.palette = palette.length >= 2 ? palette :
    new Color[]{Color.RED, Color.BLUE};
    this.pixelSize = Math.max(0.01, pixelSize);
    this.ambientCoefficient = clamp(ambient);
    this.diffuseCoefficient = clamp(diffuse);
    this.specularCoefficient = clamp(specular);
    this.shininess = Math.max(1, shininess);
    this.reflectivity = clamp(reflectivity);
    this.ior = ior;
    this.transparency = clamp(transparency);
    this.objectInverseTransform = objectInverseTransform;
  }
  
  public PixelArtMaterial(Color[] palette, double pixelSize,
    Matrix4 objectInverseTransform) {
    this(palette, pixelSize, 0.1, 0.7, 0.2, 10.0, 0.0, 1.0, 0.0, objectInverseTransform);
  }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
    if (tm == null) tm = new Matrix4 ();
    this.objectInverseTransform = tm;
  }
  
  private double clamp(double value) {
    return Math.max(0, Math.min(1, value));
  }
  
  private Color getPixelColor(double u, double v) {
    int x = (int)(u / pixelSize);
    int y = (int)(v / pixelSize);
    int hash = (x * 7919 + y * 7901) % palette.length;
    return palette[Math.abs(hash) % palette.length];
  }
  
  @Override
  public Color getColorAt(Point3 worldPoint, Vector3 worldNormal, Light light, Point3 viewerPos) {
    if (objectInverseTransform == null) {
      return Color.BLACK;
    }
    
    // Transform to object space for UV mapping
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
    
    // Base pixel color
    Color baseColor = getPixelColor(u, v);
    
    // Light calculations
    Color lightColor = light.getColor();
    double intensity = light.getIntensity();
    Vector3 lightDir = light.getDirectionAt(worldPoint).normalize();
    
    // 1. Ambient component (light color included)
    int r = (int)(baseColor.getRed() * ambientCoefficient * (lightColor.getRed()/255.0));
    int g = (int)(baseColor.getGreen() * ambientCoefficient * (lightColor.getGreen()/255.0));
    int b = (int)(baseColor.getBlue() * ambientCoefficient * (lightColor.getBlue()/255.0));
    
    // 2. Diffuse component (light color included)
    double NdotL = Math.max(0, worldNormal.dot(lightDir));
    if (NdotL > 0) {
      r += (int)(baseColor.getRed() * diffuseCoefficient * NdotL * (lightColor.getRed()/255.0) * intensity);
      g += (int)(baseColor.getGreen() * diffuseCoefficient * NdotL * (lightColor.getGreen()/255.0) * intensity);
      b += (int)(baseColor.getBlue() * diffuseCoefficient * NdotL * (lightColor.getBlue()/255.0) * intensity);
      
      // 3. Specular component (light color included)
      Vector3 viewDir = viewerPos.subtract(worldPoint).normalize();
      Vector3 reflectDir = lightDir.negate().reflect(worldNormal);
      double RdotV = Math.max(0, reflectDir.dot(viewDir));
      double specular = Math.pow(RdotV, shininess) * specularCoefficient * intensity;
      
      r += (int)(255 * specular * (lightColor.getRed()/255.0));
      g += (int)(255 * specular * (lightColor.getGreen()/255.0));
      b += (int)(255 * specular * (lightColor.getBlue()/255.0));
    }
    
    return new Color(
      Math.min(255, r),
      Math.min(255, g),
      Math.min(255, b)
    );
  }
  
  @Override public double getReflectivity() { return reflectivity; }
  @Override public double getIndexOfRefraction() { return ior; }
  @Override public double getTransparency() { return transparency; }
}

/***
Color[] retroPalette = {
new Color(255, 0, 0),    // Red
new Color(0, 255, 0),    // Green
new Color(0, 0, 255),    // Blue
new Color(255, 255, 0)   // Yellow
};

PixelArtMaterial pixelMat = new PixelArtMaterial(
retroPalette,
0.3, // Pixel size
object.getInverseTransform()
);

Color[] gameboyPalette = {
new Color(15, 56, 15),   // Dark green
new Color(48, 98, 48),   // Medium green
new Color(139, 172, 15), // Light green
new Color(155, 188, 15)  // Highlight green
};

PixelArtMaterial gbMat = new PixelArtMaterial(
gameboyPalette,
0.5, // Larger pixels
object.getInverseTransform()
);

// Directional Light (Sun)
Light sun = new DirectionalLight(
new Vector3(0, -1, 0), // From top to bottom
Color.WHITE,
1.5
);

// Point Light (Bulb)
Light bulb = new PointLight(
new Point3(2, 3, 0), // Position
Color.YELLOW,
2.0
);
 */
