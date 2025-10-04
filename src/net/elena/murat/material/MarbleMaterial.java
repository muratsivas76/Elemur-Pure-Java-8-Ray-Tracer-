package net.elena.murat.material;

import java.awt.Color;

//custom
import net.elena.murat.math.*;
import net.elena.murat.light.*;
import net.elena.murat.util.ColorUtil;

/**
 * A material that simulates marble with natural veining patterns.
 * Uses Perlin noise for realistic marble texture generation.
 */
public class MarbleMaterial implements Material {
  
  private final Color baseColor;       // Base color of the marble
  private final Color veinColor;      // Color of the veins
  private final double scale;         // Controls the size of the marble pattern
  private final double veinDensity;   // Controls how prominent the veins are (0.0 to 1.0)
  private final double turbulence;    // Controls the complexity of the veins
  private Matrix4 objectInverseTransform;
  
  // Lighting coefficients
  private final double ambientCoefficient;
  private final double diffuseCoefficient;
  private final double specularCoefficient;
  private final double shininess;
  private final double reflectivity;
  private final double ior;
  private final double transparency;
  
  // Specular color for marble (cool white)
  private final Color specularColor = new Color(240, 240, 255);
  
  /**
   * Full constructor for MarbleMaterial.
   * @param baseColor Base color of the marble.
   * @param veinColor Color of the veins.
   * @param scale Controls the size of the marble pattern.
   * @param veinDensity Controls vein prominence (0.0 to 1.0).
   * @param turbulence Controls vein complexity (0.0 to 1.0).
   * @param ambientCoefficient Ambient light contribution.
   * @param diffuseCoefficient Diffuse light contribution.
   * @param specularCoefficient Specular light contribution.
   * @param shininess Shininess for specular highlights.
   * @param reflectivity Material's reflectivity (0.0 to 1.0).
   * @param ior Index of refraction.
   * @param transparency Material's transparency (0.0 to 1.0).
   * @param objectInverseTransform Inverse transform matrix of the object.
   */
  public MarbleMaterial(Color baseColor, Color veinColor, double scale, double veinDensity, double turbulence,
    double ambientCoefficient, double diffuseCoefficient, double specularCoefficient,
    double shininess, double reflectivity, double ior, double transparency,
    Matrix4 objectInverseTransform) {
    this.baseColor = baseColor;
    this.veinColor = veinColor;
    this.scale = scale;
    this.veinDensity = Math.max(0, Math.min(1, veinDensity));
    this.turbulence = Math.max(0, Math.min(1, turbulence));
    this.objectInverseTransform = objectInverseTransform;
    
    this.ambientCoefficient = ambientCoefficient;
    this.diffuseCoefficient = diffuseCoefficient;
    this.specularCoefficient = specularCoefficient;
    this.shininess = shininess;
    this.reflectivity = reflectivity;
    this.ior = ior;
    this.transparency = transparency;
  }
  
  /**
   * Simplified constructor with default coefficients.
   * @param baseColor Base color of the marble.
   * @param veinColor Color of the veins.
   * @param scale Controls the size of the marble pattern.
   * @param veinDensity Controls vein prominence.
   * @param turbulence Controls vein complexity.
   * @param objectInverseTransform Inverse transform matrix of the object.
   */
  public MarbleMaterial(Color baseColor, Color veinColor, double scale, double veinDensity, double turbulence,
    Matrix4 objectInverseTransform) {
    this(baseColor, veinColor, scale, veinDensity, turbulence,
      0.1,   // ambientCoefficient
      0.7,   // diffuseCoefficient
      0.2,   // specularCoefficient
      50.0,  // shininess (marble is quite shiny)
      0.15,  // reflectivity (marble has some reflectivity)
      1.5,   // indexOfRefraction
      0.05, // slight transparency
    objectInverseTransform);
  }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
    if (tm == null) tm = new Matrix4 ();
    this.objectInverseTransform = tm;
  }
  
  /**
   * Generates marble pattern using simulated Perlin noise.
   * @param localPoint Point in object's local space.
   * @return Marble pattern color.
   */
  private Color getMarbleColor(Point3 localPoint) {
    // Scale coordinates
    double x = localPoint.x * scale;
    double y = localPoint.y * scale;
    double z = localPoint.z * scale;
    
    // Create turbulence pattern
    double noise = turbulence(x, y, z, turbulence);
    
    // Create sine wave pattern that will be distorted by noise
    double marblePattern = Math.sin(x + noise * 10.0) * 0.5 + 0.5;
    
    // Apply vein density to control how prominent veins are
    marblePattern = Math.pow(marblePattern, 1.0 + (veinDensity * 3.0));
    
    // Blend between base and vein color
    return ColorUtil.blendColors(baseColor, veinColor, marblePattern);
  }
  
  /**
   * Simple turbulence function to create natural-looking patterns.
   */
  private double turbulence(double x, double y, double z, double turbulenceFactor) {
    double t = 0.0;
    double size = 0.5;
    
    while (size >= 0.01) {
      t += Math.abs(improvedNoise(x/size, y/size, z/size)) * size;
      size /= 2.0;
    }
    
    return t * turbulenceFactor;
  }
  
  /**
   * Improved Perlin noise function for better pattern generation.
   */
  private double improvedNoise(double x, double y, double z) {
    // This is a simplified version of Perlin noise
    // In a full implementation, you'd want a proper noise function
    int xi = (int)Math.floor(x) & 255;
    int yi = (int)Math.floor(y) & 255;
    int zi = (int)Math.floor(z) & 255;
    
    x -= Math.floor(x);
    y -= Math.floor(y);
    z -= Math.floor(z);
    
    double u = fade(x);
    double v = fade(y);
    double w = fade(z);
    
    // Hash coordinates
    int a = xi+yi*256+zi*256*256;
    int b = a+1;
    int aa = a%256;
    int ab = (a+1)%256;
    int ba = (a+256)%256;
    int bb = (a+257)%256;
    
    // Blend everything
    double lerp1 = lerp(u, grad(aa, x, y, z), grad(ab, x-1, y, z));
    double lerp2 = lerp(u, grad(ba, x, y-1, z), grad(bb, x-1, y-1, z));
    double lerp3 = lerp(v, lerp1, lerp2);
    
    return lerp3;
  }
  
  private double fade(double t) { return t * t * t * (t * (t * 6 - 15) + 10); }
  private double lerp(double t, double a, double b) { return a + t * (b - a); }
  private double grad(int hash, double x, double y, double z) {
    int h = hash & 15;
    double u = h<8 ? x : y;
    double v = h<4 ? y : h==12||h==14 ? x : z;
    return ((h&1) == 0 ? u : -u) + ((h&2) == 0 ? v : -v);
  }
  
  @Override
  public Color getColorAt(Point3 worldPoint, Vector3 worldNormal, Light light, Point3 viewerPos) {
    if (objectInverseTransform == null) {
      System.err.println("Error: MarbleMaterial's inverse transform is null. Returning black.");
      return Color.BLACK;
    }
    
    // Transform to local space
    Point3 localPoint = objectInverseTransform.transformPoint(worldPoint);
    Vector3 localNormal = objectInverseTransform.inverseTransposeForNormal().transformVector(worldNormal).normalize();
    
    if (localNormal == null) {
      System.err.println("Error: MarbleMaterial's normal transform matrix is null or invalid. Returning black.");
      return Color.BLACK;
    }
    
    // Get base marble color
    Color marbleBaseColor = getMarbleColor(localPoint);
    
    // Lighting calculation (same structure as other materials)
    Color lightColor = light.getColor();
    double attenuatedIntensity = 0.0;
    
    // Ambient component
    int rAmbient = (int)(marbleBaseColor.getRed() * ambientCoefficient * lightColor.getRed() / 255.0);
    int gAmbient = (int)(marbleBaseColor.getGreen() * ambientCoefficient * lightColor.getGreen() / 255.0);
    int bAmbient = (int)(marbleBaseColor.getBlue() * ambientCoefficient * lightColor.getBlue() / 255.0);
    
    if (light instanceof ElenaMuratAmbientLight) {
      return new Color(
        Math.min(255, rAmbient),
        Math.min(255, gAmbient),
        Math.min(255, bAmbient)
      );
    }
    
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
      System.err.println("Warning: Unknown or unsupported light type for MarbleMaterial shading: " + light.getClass().getName());
      return Color.BLACK;
    }
    
    // Diffuse component
    double NdotL = Math.max(0, worldNormal.dot(lightDirection));
    int rDiffuse = (int)(marbleBaseColor.getRed() * diffuseCoefficient * lightColor.getRed() / 255.0 * attenuatedIntensity * NdotL);
    int gDiffuse = (int)(marbleBaseColor.getGreen() * diffuseCoefficient * lightColor.getGreen() / 255.0 * attenuatedIntensity * NdotL);
    int bDiffuse = (int)(marbleBaseColor.getBlue() * diffuseCoefficient * lightColor.getBlue() / 255.0 * attenuatedIntensity * NdotL);
    
    // Specular component
    Vector3 viewDir = viewerPos.subtract(worldPoint).normalize();
    Vector3 reflectionVector = lightDirection.negate().reflect(worldNormal);
    double RdotV = Math.max(0, reflectionVector.dot(viewDir));
    double specFactor = Math.pow(RdotV, shininess);
    
    int rSpecular = (int)(specularColor.getRed() * specularCoefficient * lightColor.getRed() / 255.0 * attenuatedIntensity * specFactor);
    int gSpecular = (int)(specularColor.getGreen() * specularCoefficient * lightColor.getGreen() / 255.0 * attenuatedIntensity * specFactor);
    int bSpecular = (int)(specularColor.getBlue() * specularCoefficient * lightColor.getBlue() / 255.0 * attenuatedIntensity * specFactor);
    
    // Sum up all components
    int finalR = Math.min(255, rAmbient + rDiffuse + rSpecular);
    int finalG = Math.min(255, gAmbient + gDiffuse + gSpecular);
    int finalB = Math.min(255, bAmbient + bDiffuse + bSpecular);
    
    return new Color(finalR, finalG, finalB);
  }
  
  @Override
  public double getReflectivity() { return reflectivity; }
  
  @Override
  public double getIndexOfRefraction() { return ior; }
  
  @Override
  public double getTransparency() { return transparency; }
  
}
