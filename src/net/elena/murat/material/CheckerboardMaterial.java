package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.math.*;
import net.elena.murat.light.*;
import net.elena.murat.util.ColorUtil;

public class CheckerboardMaterial implements Material {
  private final Color color1;
  private final Color color2;
  private final double size;
  private Matrix4 objectInverseTransform;
  
  // Phong parameters
  private final double ambientCoeff;
  private final double diffuseCoeff;
  private final double specularCoeff;
  private final double shininess;
  private final Color specularColor;
  private final double reflectivity;
  private final double ior;
  private final double transparency;
  
  public CheckerboardMaterial(Color color1, Color color2, double size, Matrix4 objectInverseTransform) {
    this(color1, color2, size, 0.1, 0.7, 0.8, 50.0, Color.WHITE, 0.0, 1.0, 0.0, objectInverseTransform);
  }
  
  public CheckerboardMaterial(Color color1, Color color2, double size,
    double ambientCoeff, double diffuseCoeff,
    double specularCoeff, double shininess, Color specularColor,
    double reflectivity, double ior, double transparency,
    Matrix4 objectInverseTransform) {
    this.color1 = color1;
    this.color2 = color2;
    this.size = size;
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
    // 1. Get checkerboard pattern color
    Point3 localPoint = objectInverseTransform.transformPoint(worldPoint);
    Color patternColor = calculatePatternColor(localPoint, worldNormal);
    
    // 2. Handle light properties
    LightProperties props = getLightPropertiesX(light, worldPoint);
    if (props == null) return patternColor;
    
    // 3. Calculate Phong components
    Color ambient = ColorUtil.multiplyColors(patternColor, props.color, ambientCoeff);
    
    if (light instanceof ElenaMuratAmbientLight) {
      return ambient;
    }
    
    double NdotL = Math.max(0, worldNormal.dot(props.direction));
    Color diffuse = ColorUtil.multiplyColors(patternColor, props.color, diffuseCoeff * NdotL * props.intensity);
    
    Vector3 viewDir = viewerPos.subtract(worldPoint).normalize();
    Vector3 reflectDir = props.direction.negate().reflect(worldNormal);
    double RdotV = Math.max(0, reflectDir.dot(viewDir));
    double specFactor = Math.pow(RdotV, shininess) * props.intensity;
    Color specular = ColorUtil.multiplyColors(specularColor, props.color, specularCoeff * specFactor);
    
    return ColorUtil.combineColors(ambient, diffuse, specular);
  }
  
  private Color calculatePatternColor(Point3 localPoint, Vector3 worldNormal) {
    Vector3 localNormal = objectInverseTransform.inverseTransposeForNormal().transformVector(worldNormal).normalize();
    
    double u, v;
    double absNx = Math.abs(localNormal.x);
    double absNy = Math.abs(localNormal.y);
    double absNz = Math.abs(localNormal.z);
    
    if (absNx > absNy && absNx > absNz) {
      u = localPoint.y * size + Ray.EPSILON;
      v = localPoint.z * size + Ray.EPSILON;
      } else if (absNy > absNx && absNy > absNz) {
      u = localPoint.x * size + Ray.EPSILON;
      v = localPoint.z * size + Ray.EPSILON;
      } else {
      u = localPoint.x * size + Ray.EPSILON;
      v = localPoint.y * size + Ray.EPSILON;
    }
    
    return ((int)Math.floor(u) + (int)Math.floor(v)) % 2 == 0 ? color1 : color2;
  }
  
  private LightProperties getLightPropertiesX(Light light, Point3 point) {
    if (light == null) return null;
    
    if (light instanceof ElenaMuratAmbientLight) {
      return new LightProperties(
        new Vector3(0, 0, 0), // Dummy direction
        light.getColor(),
        light.getIntensity()
      );
    }
    
    Vector3 dir;
    double intensity;
    
    try {
      // Handle all light types consistently
      if (light instanceof MuratPointLight ||
        light instanceof PulsatingPointLight ||
        light instanceof BioluminescentLight) {
        dir = light.getPosition().subtract(point).normalize();
        intensity = light.getAttenuatedIntensity(point);
      }
      else if (light instanceof ElenaDirectionalLight) {
        dir = ((ElenaDirectionalLight)light).getDirection().negate().normalize();
        intensity = light.getIntensity();
      }
      else if (light instanceof SpotLight ||
        light instanceof BlackHoleLight ||
        light instanceof FractalLight) {
        // Use getDirectionAt() for advanced lights
        dir = light.getDirectionAt(point);
        intensity = light.getAttenuatedIntensity(point);
      }
      else {
        // Fallback for unknown lights
        dir = new Vector3(0, 1, 0); // Default upward direction
        intensity = light.getIntensity();
      }
      
      return new LightProperties(dir, light.getColor(), intensity);
      } catch (Exception e) {
      // Fallback if any calculation fails
      return new LightProperties(
        new Vector3(0, 1, 0),
        light.getColor(),
        Math.min(light.getIntensity(), 1.0)
      );
    }
  }
  
  @Override public double getReflectivity() { return reflectivity; }
  @Override public double getIndexOfRefraction() { return ior; }
  @Override public double getTransparency() { return transparency; }
  
}
