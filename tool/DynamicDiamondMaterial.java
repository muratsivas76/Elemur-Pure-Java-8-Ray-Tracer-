import java.awt.Color;

/**
 * DynamicDiamondMaterial - A physically based diamond material with fully dynamic properties
 * that are calculated per-pixel. Simulates diamond-specific properties like high dispersion (fire).
 */
public class DynamicDiamondMaterial implements Material {
  
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
   * Constructs a new DynamicDiamondMaterial with diamond's physical properties
   */
  public DynamicDiamondMaterial() {
    // Diamond-specific base properties
    this.baseIOR = 2.42f;          // Diamond has very high index of refraction
    this.baseReflectivity = 0.17f; // Higher natural reflectivity due to high IOR
    this.baseTransparency = 0.99f; // Diamond is exceptionally clear
    this.tintColor = new Color(255, 240, 225); // Very slight warm tint (optional)
    this.roughness = 0.01f;        // Diamond is polished to near perfection
    this.density = 0.1f;           // Lower density for less light absorption
    this.chromaticAberrationStrength = 0.03f; // STRONG for diamond "fire" effect
    
    // Initialize current values
    this.currentIOR = baseIOR;
    this.currentReflectivity = baseReflectivity;
    this.currentTransparency = baseTransparency;
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
    
    // Calculate optical components with spectral dispersion for diamond
    Color refractedColor = calculateDiamondRefraction(point, normal, light, viewerPoint, currentTransparency);
    Color reflectedColor = calculateReflection(point, normal, light, viewerPoint, currentReflectivity, fresnel);
    Color absorbedColor = calculateAbsorption(point, normal, viewerPoint);
    
    // Return combined result
    return combineOpticalComponents(refractedColor, reflectedColor, absorbedColor, fresnel, currentTransparency);
  }
  
  /**
   * Calculates dynamic Index of Refraction for diamond
   * Adds subtle variation based on viewing angle and surface position
   */
  private float calculateDynamicIOR(Point3 point, Vector3 normal, Point3 viewerPoint) {
    Vector3 viewDir = viewerPoint.subtract(point).normalize();
    float angleFactor = (float) Math.abs(viewDir.dot(normal));
    float angleVariation = 0.05f * (1.0f - angleFactor);
    float noise = (float) (0.005f * Math.sin(point.getX() * 15f) * Math.cos(point.getZ() * 15f));
    
    return baseIOR + angleVariation + noise;
  }
  
  /**
   * Calculates dynamic reflectivity for diamond
   * Higher base reflectivity and strong Fresnel effect
   */
  private float calculateDynamicReflectivity(Point3 point, Vector3 normal, Point3 viewerPoint) {
    Vector3 viewDir = viewerPoint.subtract(point).normalize();
    float fresnel = calculateFresnelEffect(viewDir, normal, baseIOR);
    float roughnessEffect = 1.0f - roughness;
    
    return baseReflectivity * fresnel * roughnessEffect;
  }
  
  /**
   * Calculates dynamic transparency for diamond
   * Diamond has exceptional clarity with minimal light absorption
   */
  private float calculateDynamicTransparency(Point3 point, Vector3 normal, Point3 viewerPoint) {
    Vector3 viewDir = viewerPoint.subtract(point).normalize();
    float angleFactor = (float) Math.abs(viewDir.dot(normal));
    float densityEffect = (float) Math.exp(-density * 1.5f);
    float angleEffect = 0.9f + 0.1f * angleFactor;
    
    return baseTransparency * densityEffect * angleEffect;
  }
  
  /**
   * Special refraction calculation for diamond with strong chromatic dispersion
   * Simulates diamond's "fire" effect by calculating different IOR for each color channel
   */
  private Color calculateDiamondRefraction(Point3 point, Vector3 normal, Light light, Point3 viewerPoint, float transparency) {
    Vector3 viewDir = viewerPoint.subtract(point).normalize();
    
    // Calculate different IOR values for each color channel (dispersion simulation)
    float iorRed = calculateDynamicIOR(point, normal, viewerPoint) - 0.03f;
    float iorGreen = calculateDynamicIOR(point, normal, viewerPoint);
    float iorBlue = calculateDynamicIOR(point, normal, viewerPoint) + 0.03f;
    
    // Calculate refraction for each color channel
    Vector3 refractedDirRed = refract(viewDir, normal, iorRed);
    Vector3 refractedDirGreen = refract(viewDir, normal, iorGreen);
    Vector3 refractedDirBlue = refract(viewDir, normal, iorBlue);
    
    // Calculate light transport for each channel
    Color redComponent = calculateLightTransport(point, refractedDirRed, light, transparency);
    Color greenComponent = calculateLightTransport(point, refractedDirGreen, light, transparency);
    Color blueComponent = calculateLightTransport(point, refractedDirBlue, light, transparency);
    
    // Combine channels with additional dispersion strength
    return new Color(
      Math.min(1.0f, Math.max(0.0f, redComponent.getRed() / 255.0f * (1.0f + chromaticAberrationStrength))),
      Math.min(1.0f, Math.max(0.0f, greenComponent.getGreen() / 255.0f)),
      Math.min(1.0f, Math.max(0.0f, blueComponent.getBlue() / 255.0f * (1.0f - chromaticAberrationStrength * 0.5f)))
    );
  }
  
  /**
   * Calculates reflection with diamond's high reflectivity characteristics
   */
  private Color calculateReflection(Point3 point, Vector3 normal, Light light, Point3 viewerPoint,
    float reflectivity, float fresnel) {
    Vector3 viewDir = viewerPoint.subtract(point).normalize();
    Vector3 reflectedDir = reflect(viewDir, normal);
    Vector3 roughReflectedDir = applyRoughness(reflectedDir, roughness);
    return calculateLightTransport(point, roughReflectedDir, light, reflectivity * fresnel);
  }
  
  /**
   * Calculates Fresnel effect using Schlick's approximation
   */
  private float calculateFresnelEffect(Vector3 viewDir, Vector3 normal, float ior) {
    float cosTheta = Math.abs(viewDir.dot(normal));
    float r0 = (1.0f - ior) / (1.0f + ior);
    r0 = r0 * r0;
    return r0 + (1.0f - r0) * (float) Math.pow(1.0f - cosTheta, 5.0f);
  }
  
  /**
   * Calculates light transport through the material
   */
  private Color calculateLightTransport(Point3 point, Vector3 direction, Light light, float intensity) {
    Vector3 toLight = light.getPosition().subtract(point).normalize();
    float lightFactor = Math.max(0.1f, direction.dot(toLight));
    return multiplyColor(tintColor, lightFactor * intensity * light.getIntensity());
  }
  
  /**
   * Calculates light absorption through the diamond
   */
  private Color calculateAbsorption(Point3 point, Vector3 normal, Point3 viewerPoint) {
    float distance = point.distanceTo(viewerPoint);
    float absorption = (float) Math.exp(-density * distance * 0.3f);
    return multiplyColor(tintColor, absorption);
  }
  
  /**
   * Combines all optical components (refraction, reflection, absorption)
   */
  private Color combineOpticalComponents(Color refraction, Color reflection, Color absorption,
    float fresnel, float transparency) {
    float reflectionWeight = fresnel;
    float refractionWeight = (1.0f - fresnel) * transparency;
    
    Color combined = addColors(
      multiplyColor(reflection, reflectionWeight),
      multiplyColor(refraction, refractionWeight)
    );
    
    return multiplyColors(combined, absorption);
  }
  
  /**
   * Returns the CURRENT dynamic IOR value (updated after last getColorAt call)
   */
  @Override
  public float getIOR() {
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
    this.baseIOR = Math.max(1.0f, Math.min(3.5f, ior));
    this.currentIOR = baseIOR;
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
  
  // --- Vector and Color Math Utilities ---
  
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
  
  private Vector3 applyRoughness(Vector3 direction, float roughness) {
    if (roughness <= 0) return direction;
    
    float theta = (float) (2.0f * Math.PI * Math.random());
    float phi = (float) (Math.acos(2.0f * Math.random() - 1.0f));
    float strength = roughness * 0.3f;
    
    Vector3 perturbation = new Vector3(
      (float) (strength * Math.sin(phi) * Math.cos(theta)),
      (float) (strength * Math.sin(phi) * Math.sin(theta)),
      (float) (strength * Math.cos(phi))
    );
    
    return direction.add(perturbation).normalize();
  }
  
  private Color multiplyColor(Color color, float factor) {
    float r = color.getRed() / 255.0f * factor;
    float g = color.getGreen() / 255.0f * factor;
    float b = color.getBlue() / 255.0f * factor;
    
    return new Color(
      Math.min(1.0f, Math.max(0.0f, r)),
      Math.min(1.0f, Math.max(0.0f, g)),
      Math.min(1.0f, Math.max(0.0f, b))
    );
  }
  
  private Color multiplyColors(Color color1, Color color2) {
    float r = color1.getRed() / 255.0f * color2.getRed() / 255.0f;
    float g = color1.getGreen() / 255.0f * color2.getGreen() / 255.0f;
    float b = color1.getBlue() / 255.0f * color2.getBlue() / 255.0f;
    
    return new Color(r, g, b);
  }
  
  private Color addColors(Color color1, Color color2) {
    float r = Math.min(1.0f, color1.getRed() / 255.0f + color2.getRed() / 255.0f);
    float g = Math.min(1.0f, color1.getGreen() / 255.0f + color2.getGreen() / 255.0f);
    float b = Math.min(1.0f, color1.getBlue() / 255.0f + color2.getBlue() / 255.0f);
    
    return new Color(r, g, b);
  }
}