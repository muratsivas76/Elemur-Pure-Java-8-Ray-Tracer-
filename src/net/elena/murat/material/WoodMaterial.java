package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.math.*;
import net.elena.murat.light.*;
import net.elena.murat.util.ColorUtil;

public class WoodMaterial implements Material {
  private final Color baseColor;
  private final Color grainColor;
  private final double grainFrequency;
  private final double ringVariation;
  private Matrix4 objectInverseTransform;
  
  // Phong parameters
  private final double ambientCoeff;
  private final double diffuseCoeff;
  private final double specularCoeff;
  private final double shininess;
  private final double reflectivity;
  private final double ior;
  private final double transparency;
  private final Color specularColor = new Color(200, 200, 180);
  
  public WoodMaterial(Color baseColor, Color grainColor, double grainFrequency,
    double ringVariation, Matrix4 objectInverseTransform) {
    this(baseColor, grainColor, grainFrequency, ringVariation,
    0.15, 0.7, 0.15, 15.0, 0.05, 1.3, 0.0, objectInverseTransform);
  }
  
  public WoodMaterial(Color baseColor, Color grainColor, double grainFrequency,
    double ringVariation, double ambientCoeff, double diffuseCoeff,
    double specularCoeff, double shininess, double reflectivity,
    double ior, double transparency, Matrix4 objectInverseTransform) {
    this.baseColor = baseColor;
    this.grainColor = grainColor;
    this.grainFrequency = grainFrequency;
    this.ringVariation = Math.max(0, Math.min(1, ringVariation));
    this.objectInverseTransform = objectInverseTransform;
    this.ambientCoeff = ambientCoeff;
    this.diffuseCoeff = diffuseCoeff;
    this.specularCoeff = specularCoeff;
    this.shininess = shininess;
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
    // 1. Get wood texture color
    Point3 localPoint = objectInverseTransform.transformPoint(worldPoint);
    Color woodColor = calculateWoodColor(localPoint);
    
    // 2. Unified light handling
    LightProperties props = LightProperties.getLightProperties(light, worldPoint);
    if (props == null) return woodColor;
    
    // 3. Phong shading components
    Vector3 lightDir = props.direction;
    Color lightColor = props.color;
    double intensity = props.intensity;
    
    // Ambient
    Color ambient = ColorUtil.multiplyColors(woodColor, lightColor, ambientCoeff);
    
    // Diffuse
    double NdotL = Math.max(0, worldNormal.dot(lightDir));
    Color diffuse = ColorUtil.multiplyColors(woodColor, lightColor, diffuseCoeff * NdotL * intensity);
    
    // Specular
    Vector3 viewDir = viewerPos.subtract(worldPoint).normalize();
    Vector3 reflectDir = lightDir.negate().reflect(worldNormal);
    double RdotV = Math.max(0, reflectDir.dot(viewDir));
    double specFactor = Math.pow(RdotV, shininess) * intensity;
    Color specular = ColorUtil.multiplyColors(specularColor, lightColor, specularCoeff * specFactor);
    
    // Combine components
    return ColorUtil.combineColors(ambient, diffuse, specular);
  }
  
  private Color calculateWoodColor(Point3 localPoint) {
    double x = localPoint.x, y = localPoint.y, z = localPoint.z;
    double distance = Math.sqrt(x*x + z*z) * grainFrequency;
    double noise = Math.sin(y * 2.0) * ringVariation;
    double ringPattern = Math.pow(Math.sin(distance + noise) * 0.5 + 0.5, 3.0);
    return ColorUtil.blendColors(baseColor, grainColor, ringPattern);
  }
  
  @Override public double getReflectivity() { return reflectivity; }
  @Override public double getIndexOfRefraction() { return ior; }
  @Override public double getTransparency() { return transparency; }
}

/***
Material wood = new WoodMaterial(
new Color(139, 69, 19),    // baseColor: Brown (classic wood color)
new Color(101, 67, 33),     // grainColor: Darker vein color
0.5,                       // grainFrequency: Vein density (0.5 medium density)
0.3,                       // ringVariation: Annual ring variation
Matrix4.scale(0.1, 0.1, 0.1) // objectInverseTransform: Scale down wood texture
.multiply(Matrix4.rotateY(Math.toRadians(45))) // Rotate texture by 45°
);

// Alternative lighter colored oak wood:
Material oakWood = new WoodMaterial(
new Color(210, 180, 140),  // Light beige
new Color(160, 130, 90),    // Medium brown veins
0.7,                       // Denser veins
0.2,                       // Less ring variation
Matrix4.identity()          // Use texture as-is
);
 */

