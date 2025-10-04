package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.light.*;
import net.elena.murat.math.*;
import net.elena.murat.util.ColorUtil;

/**
 * DynamicGlassMaterial - A physically based glass material with fully dynamic properties
 * that are calculated per-pixel and exposed through getter methods.
 */
public class DynamicGlassMaterial implements Material {
  
  private float baseIOR;
  private float baseReflectivity;
  private float baseTransparency;
  private Color tintColor;
  private float roughness;
  private float density;
  private float chromaticAberrationStrength;
  
  // Current dynamic values (updated per getColorAt call)
  private float currentIOR;
  private float currentReflectivity;
  private float currentTransparency;
  
  /**
   * Constructs a new DynamicGlassMaterial with default physical properties
   */
  public DynamicGlassMaterial() {
    this.baseIOR = 1.5f;
    this.baseReflectivity = 0.08f;
    this.baseTransparency = 0.95f;
    this.tintColor = new Color(220, 240, 255);
    this.roughness = 0.02f;
    this.density = 0.3f;
    this.chromaticAberrationStrength = 0.005f;
    
    // Initialize current values
    this.currentIOR = this.baseIOR;
    this.currentReflectivity = this.baseReflectivity;
    this.currentTransparency = this.baseTransparency;
  }
  
  public DynamicGlassMaterial (Color tintColor,
    float reflectivity,
    float transparency,
    float roughness,
    float density,
    float chromaticAberrationStrength) {
    this.tintColor = tintColor;
    this.baseReflectivity = reflectivity;
    this.baseTransparency = transparency;
    this.roughness = roughness;
    this.density = density;
    this.chromaticAberrationStrength = chromaticAberrationStrength;
    
    // Initialize current values
    this.baseIOR = 1.5f;
    this.currentIOR = this.baseIOR;
    this.currentReflectivity = this.baseReflectivity;
    this.currentTransparency = this.baseTransparency;
  }
  
  /**
   * Calculates the color at a specific point and updates dynamic properties
   */
  @Override
  public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPoint) {
    // Calculate dynamic properties
    this.currentIOR = calculateDynamicIOR(point, normal, viewerPoint);
    this.currentReflectivity = calculateDynamicReflectivity(point, normal, viewerPoint);
    this.currentTransparency = calculateDynamicTransparency(point, normal, viewerPoint);
    
    // Calculate Fresnel effect
    Vector3 viewDir = viewerPoint.subtract(point).normalize();
    float fresnel = calculateFresnelEffect(viewDir, normal, currentIOR);
    
    // Calculate optical components
    Color refractedColor = calculateRefraction(point, normal, light, viewerPoint, currentIOR, currentTransparency);
    Color reflectedColor = calculateReflection(point, normal, light, viewerPoint, currentReflectivity, fresnel);
    Color absorbedColor = calculateAbsorption(point, normal, viewerPoint);
    
    // Return combined result
    return combineOpticalComponents(refractedColor, reflectedColor, absorbedColor, fresnel, currentTransparency);
  }
  
  /**
   * Calculates dynamic Index of Refraction
   */
  private float calculateDynamicIOR(Point3 point, Vector3 normal, Point3 viewerPoint) {
    Vector3 viewDir = viewerPoint.subtract(point).normalize();
    float angleFactor = (float) Math.abs(viewDir.dot(normal));
    float angleVariation = 0.1f * (1.0f - angleFactor);
    float noise = (float) (0.01f * Math.sin(point.getX() * 10f) * Math.cos(point.getZ() * 10f));
    
    return baseIOR + angleVariation + noise;
  }
  
  /**
   * Calculates dynamic reflectivity
   */
  private float calculateDynamicReflectivity(Point3 point, Vector3 normal, Point3 viewerPoint) {
    Vector3 viewDir = viewerPoint.subtract(point).normalize();
    float fresnel = calculateFresnelEffect(viewDir, normal, baseIOR);
    float roughnessEffect = 1.0f - roughness;
    
    return baseReflectivity * fresnel * roughnessEffect;
  }
  
  /**
   * Calculates dynamic transparency
   */
  private float calculateDynamicTransparency(Point3 point, Vector3 normal, Point3 viewerPoint) {
    Vector3 viewDir = viewerPoint.subtract(point).normalize();
    float angleFactor = (float) Math.abs(viewDir.dot(normal));
    float densityEffect = (float) Math.exp(-density * 2.0f);
    float angleEffect = 0.8f + 0.2f * angleFactor;
    
    return baseTransparency * densityEffect * angleEffect;
  }
  
  /**
   * Returns the CURRENT dynamic IOR value (updated after last getColorAt call)
   */
  @Override
  public float getIndexOfRefraction() {
    return currentIOR;
  }
  
  /**
   * Returns the CURRENT dynamic reflectivity value
   */
  @Override
  public float getReflectivity() {
    return currentReflectivity;
  }
  
  /**
   * Returns the CURRENT dynamic transparency value
   */
  @Override
  public float getTransparency() {
    return currentTransparency;
  }
  
  // --- Property Setters (for base values) ---
  
  public void setBaseIOR(float ior) {
    this.baseIOR = Math.max(1.0f, Math.min(3.0f, ior));
    this.currentIOR = baseIOR; // Update current value
  }
  
  public void setBaseReflectivity(float reflectivity) {
    this.baseReflectivity = Math.max(0.0f, Math.min(1.0f, reflectivity));
    this.currentReflectivity = baseReflectivity;
  }
  
  public void setBaseTransparency(float transparency) {
    this.baseTransparency = Math.max(0.0f, Math.min(1.0f, transparency));
    this.currentTransparency = baseTransparency;
  }
  
  public void setTintColor(Color tintColor) {
    this.tintColor = tintColor;
  }
  
  public void setRoughness(float roughness) {
    this.roughness = Math.max(0.0f, Math.min(1.0f, roughness));
  }
  
  public void setDensity(float density) {
    this.density = Math.max(0.0f, Math.min(5.0f, density));
  }
  
  public void setChromaticAberrationStrength(float strength) {
    this.chromaticAberrationStrength = Math.max(0.0f, Math.min(0.1f, strength));
  }
  
  // --- Helper Methods (same as before) ---
  
  private float calculateFresnelEffect(Vector3 viewDir, Vector3 normal, float ior) {
    float cosTheta = Math.abs(viewDir.dot(normal));
    float r0 = (1.0f - ior) / (1.0f + ior);
    r0 = r0 * r0;
    return r0 + (1.0f - r0) * (float) Math.pow(1.0f - cosTheta, 5.0f);
  }
  
  private Color calculateRefraction(Point3 point, Vector3 normal, Light light, Point3 viewerPoint,
    float ior, float transparency) {
    Vector3 viewDir = viewerPoint.subtract(point).normalize();
    Vector3 refractedDir = refract(viewDir, normal, ior);
    Color baseRefracted = calculateLightTransport(point, refractedDir, light, transparency);
    return applyChromaticAberration(baseRefracted, refractedDir, normal);
  }
  
  private Color calculateReflection(Point3 point, Vector3 normal, Light light, Point3 viewerPoint,
    float reflectivity, float fresnel) {
    Vector3 viewDir = viewerPoint.subtract(point).normalize();
    Vector3 reflectedDir = reflect(viewDir, normal);
    Vector3 roughReflectedDir = applyRoughness(reflectedDir, roughness);
    return calculateLightTransport(point, roughReflectedDir, light, reflectivity * fresnel);
  }
  
  private Color calculateLightTransport(Point3 point, Vector3 direction, Light light, float intensity) {
    Vector3 toLight = light.getPosition().subtract(point).normalize();
    float lightFactor = Math.max(0.1f, direction.dot(toLight));
    return ColorUtil.multiplyColorFloat(tintColor, lightFactor * intensity * light.getIntensity());
  }
  
  private Color calculateAbsorption(Point3 point, Vector3 normal, Point3 viewerPoint) {
    float distance = point.distanceTo(viewerPoint);
    float absorption = (float) Math.exp(-density * distance * 0.5f);
    return ColorUtil.multiplyColorFloat(tintColor, absorption);
  }
  
  private Color applyChromaticAberration(Color baseColor, Vector3 direction, Vector3 normal) {
    if (chromaticAberrationStrength <= 0) return baseColor;
    
    float r = baseColor.getRed() / 255.0f;
    float g = baseColor.getGreen() / 255.0f;
    float b = baseColor.getBlue() / 255.0f;
    float aberration = chromaticAberrationStrength * direction.dot(normal);
    
    r *= (1.0f + aberration);
    g *= (1.0f + aberration * 0.5f);
    b *= (1.0f + aberration * 0.3f);
    
    return new Color(
      Math.min(1.0f, Math.max(0.0f, r)),
      Math.min(1.0f, Math.max(0.0f, g)),
      Math.min(1.0f, Math.max(0.0f, b))
    );
  }
  
  private Vector3 applyRoughness(Vector3 direction, float roughness) {
    if (roughness <= 0) return direction;
    
    float theta = (float) (2.0f * Math.PI * Math.random());
    float phi = (float) (Math.acos(2.0f * Math.random() - 1.0f));
    float strength = roughness * 0.5f;
    
    Vector3 perturbation = new Vector3(
      (float) (strength * Math.sin(phi) * Math.cos(theta)),
      (float) (strength * Math.sin(phi) * Math.sin(theta)),
      (float) (strength * Math.cos(phi))
    );
    
    return direction.add(perturbation).normalize();
  }
  
  private Color combineOpticalComponents(Color refraction, Color reflection, Color absorption,
    float fresnel, float transparency) {
    float reflectionWeight = fresnel;
    float refractionWeight = (1.0f - fresnel) * transparency;
    
    Color combined = ColorUtil.add(
      ColorUtil.multiplyColorFloat(reflection, reflectionWeight),
      ColorUtil.multiplyColorFloat(refraction, refractionWeight)
    );
    
    return ColorUtil.multiplyColors(combined, absorption);
  }
  
  private Vector3 refract(Vector3 incident, Vector3 normal, float ior) {
    float cosi = incident.dot(normal);
    float etai = 1.0f, etat = ior;
    Vector3 n = normal;
    
    if (cosi < 0) {
      cosi = -cosi;
      } else {
      float swap = etai;
      etai = etat;
      etat = swap;
      n = normal.negate();
    }
    
    float eta = etai / etat;
    float k = 1.0f - eta * eta * (1.0f - cosi * cosi);
    
    if (k < 0) {
      return new Vector3(0, 0, 0);
    }
    
    return incident.multiply(eta).add(n.multiply(eta * cosi - (float) Math.sqrt(k))).normalize();
  }
  
  private Vector3 reflect(Vector3 incident, Vector3 normal) {
    float dot = incident.dot(normal);
    return incident.subtract(normal.multiply(2.0f * dot)).normalize();
  }
  
}