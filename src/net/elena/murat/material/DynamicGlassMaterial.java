package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.light.*;
import net.elena.murat.math.*;
import net.elena.murat.util.ColorUtil;

/**
 * DynamicGlassMaterial - Glass material with optional per-pixel dynamic parameters.
 *
 * If per-pixel IOR, transparency or reflectivity are constant, set methods are not called inside getColorAt.
 * If per-pixel variation is needed, override getColorAt to update parameters via set methods.
 */
public class DynamicGlassMaterial implements Material {
  
  // Base physical properties (default values)
  private double baseIOR = 1.5;
  private double baseReflectivity = 0.04;
  private double baseTransparency = 0.92;
  private Color tintColor = new Color(200, 220, 240);
  private double roughness = 0.01;
  private double density = 0.1;
  private double chromaticAberrationStrength = 0.001;
  
  // Flag to enable per-pixel dynamic parameter update
  private boolean dynamicParametersEnabled = false;
  
  public DynamicGlassMaterial() {
  }
  
  public DynamicGlassMaterial(Color tintColor,
    double reflectivity,
    double transparency,
    double roughness,
    double density,
    double chromaticAberrationStrength) {
    this.tintColor = tintColor;
    this.baseReflectivity = clamp(reflectivity, 0.0, 1.0);
    this.baseTransparency = clamp(transparency, 0.0, 1.0);
    this.roughness = clamp(roughness, 0.0, 1.0);
    this.density = clamp(density, 0.0, 5.0);
    this.chromaticAberrationStrength = clamp(chromaticAberrationStrength, 0.0, 0.1);
  }
  
  /**
   * Enable or disable per-pixel dynamic parameter update.
   * If enabled, getColorAt will update IOR, transparency, reflectivity per pixel.
   */
  public void setDynamicParametersEnabled(boolean enabled) {
    this.dynamicParametersEnabled = enabled;
  }
  
  @Override
  public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPoint) {
    Vector3 viewDir = viewerPoint.subtract(point).normalize();
    
    if (dynamicParametersEnabled) {
      // Example dynamic update: vary parameters based on angle between normal and viewDir
      double angleFactor = Math.abs(normal.dot(viewDir));
      
      double dynamicIOR = 1.4 + 0.2 * angleFactor; // 1.4 - 1.6
      double dynamicTransparency = 0.8 + 0.2 * (1.0 - angleFactor); // 0.8 - 1.0
      double dynamicReflectivity = 0.05 + 0.1 * angleFactor; // 0.05 - 0.15
      
      setBaseIOR(dynamicIOR);
      setBaseTransparency(dynamicTransparency);
      setBaseReflectivity(dynamicReflectivity);
    }
    
    // Chromatic aberration per channel
    double iorR = clamp(baseIOR + chromaticAberrationStrength, 1.0, 3.0);
    double iorG = clamp(baseIOR, 1.0, 3.0);
    double iorB = clamp(baseIOR - chromaticAberrationStrength, 1.0, 3.0);
    
    Color colorR = calculateRefractReflectColor(point, normal, light, viewDir, iorR);
    Color colorG = calculateRefractReflectColor(point, normal, light, viewDir, iorG);
    Color colorB = calculateRefractReflectColor(point, normal, light, viewDir, iorB);
    
    int r = clamp(colorR.getRed(), 0, 255);
    int g = clamp(colorG.getGreen(), 0, 255);
    int b = clamp(colorB.getBlue(), 0, 255);
    
    return new Color(r, g, b);
  }
  
  private Color calculateRefractReflectColor(Point3 point, Vector3 normal, Light light, Vector3 viewDir, double ior) {
    double cosTheta = Math.abs(viewDir.dot(normal));
    double r0 = Math.pow((1.0 - ior) / (1.0 + ior), 2);
    double fresnel = r0 + (1.0 - r0) * Math.pow(1.0 - cosTheta, 5.0);
    
    Vector3 perturbedNormal = perturbNormal(normal, roughness);
    
    Vector3 refractedDir = refract(viewDir, perturbedNormal, ior);
    Color refractedColor = calculateLightTransport(point, refractedDir, light, baseTransparency * Math.exp(-density));
    
    Vector3 reflectedDir = reflect(viewDir, perturbedNormal);
    Color reflectedColor = calculateLightTransport(point, reflectedDir, light, baseReflectivity);
    
    return ColorUtil.add(
      ColorUtil.multiplyColorFloat(refractedColor, (float)((1.0 - fresnel) * baseTransparency)),
      ColorUtil.multiplyColorFloat(reflectedColor, (float)(fresnel * baseReflectivity))
    );
  }
  
  private Vector3 perturbNormal(Vector3 normal, double roughness) {
    if (roughness <= 0.0) return normal;
    
    Vector3 tangent = generateTangent(normal);
    Vector3 bitangent = normal.cross(tangent);
    
    double rand1 = (Math.random() - 0.5) * 2.0 * roughness;
    double rand2 = (Math.random() - 0.5) * 2.0 * roughness;
    
    Vector3 perturbed = normal
    .add(tangent.multiply((float)rand1))
    .add(bitangent.multiply((float)rand2))
    .normalize();
    
    return perturbed;
  }
  
  private Vector3 generateTangent(Vector3 normal) {
    Vector3 up = Math.abs(normal.z) < 0.999 ? new Vector3(0, 0, 1) : new Vector3(1, 0, 0);
    return normal.cross(up).normalize();
  }
  
  private Color calculateLightTransport(Point3 point, Vector3 direction, Light light, double intensity) {
    Vector3 lightDir = light.getDirectionAt(point);
    if (lightDir.lengthSquared() < 1e-10) {
      return ColorUtil.multiplyColorFloat(tintColor, (float)intensity);
    }
    
    lightDir = lightDir.normalize();
    double lightFactor = Math.max(0.1, direction.dot(lightDir));
    double attenuatedIntensity = intensity * light.getIntensity();
    
    attenuatedIntensity *= Math.exp(-density);
    
    return ColorUtil.multiplyColorFloat(tintColor, (float)(lightFactor * attenuatedIntensity));
  }
  
  private Vector3 refract(Vector3 incident, Vector3 normal, double ior) {
    double cosi = incident.dot(normal);
    double etai = 1.0, etat = ior;
    Vector3 n = normal;
    
    if (cosi < 0) {
      cosi = -cosi;
      } else {
      double temp = etai;
      etai = etat;
      etat = temp;
      n = normal.negate();
    }
    
    double eta = etai / etat;
    double k = 1.0 - eta * eta * (1.0 - cosi * cosi);
    
    if (k < 0) {
      return new Vector3(0, 0, 0);
    }
    
    return incident.multiply((float)eta)
    .add(n.multiply((float)(eta * cosi - Math.sqrt(k))))
    .normalize();
  }
  
  private Vector3 reflect(Vector3 incident, Vector3 normal) {
    double dot = incident.dot(normal);
    return incident.subtract(normal.multiply((float)(2.0 * dot))).normalize();
  }
  
  // Getters and setters
  
  @Override
  public double getIndexOfRefraction() {
    return baseIOR;
  }
  
  @Override
  public double getReflectivity() {
    return baseReflectivity;
  }
  
  @Override
  public double getTransparency() {
    return baseTransparency;
  }
  
  public void setBaseIOR(double ior) {
    this.baseIOR = clamp(ior, 1.0, 3.0);
  }
  
  public void setBaseReflectivity(double reflectivity) {
    this.baseReflectivity = clamp(reflectivity, 0.0, 1.0);
  }
  
  public void setBaseTransparency(double transparency) {
    this.baseTransparency = clamp(transparency, 0.0, 1.0);
  }
  
  public void setTintColor(Color tintColor) {
    if (tintColor != null) {
      this.tintColor = tintColor;
    }
  }
  
  public void setRoughness(double roughness) {
    this.roughness = clamp(roughness, 0.0, 1.0);
  }
  
  public void setDensity(double density) {
    this.density = clamp(density, 0.0, 5.0);
  }
  
  public void setChromaticAberrationStrength(double strength) {
    this.chromaticAberrationStrength = clamp(strength, 0.0, 0.1);
  }
  
  private static double clamp(double val, double min, double max) {
    return Math.max(min, Math.min(max, val));
  }
  
  private static int clamp(int val, int min, int max) {
    return Math.max(min, Math.min(max, val));
  }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
    // No transform needed for this material
  }
  
}
