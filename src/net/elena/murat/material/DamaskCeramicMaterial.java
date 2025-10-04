package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.math.*;
import net.elena.murat.light.*;
import net.elena.murat.util.ColorUtil;

public class DamaskCeramicMaterial implements Material {
  private final Color primaryColor;
  private final Color secondaryColor;
  private final double shininess;
  private final double ambientCoeff;
  private final double specularCoeff;
  private final double reflectivity=0.1;
  
  private Matrix4 objectInverseTransform;
  
  public DamaskCeramicMaterial(Color primary, Color secondary,
    double shininess, Matrix4 invTransform) {
    this(primary, secondary, shininess, 0.1, 0.8, invTransform);
  }
  
  public DamaskCeramicMaterial(Color primary, Color secondary,
    double shininess, double ambient,
    double specular, Matrix4 invTransform) {
    this.primaryColor = primary;
    this.secondaryColor = secondary;
    this.shininess = Math.max(1.0, shininess);
    this.ambientCoeff = ambient;
    this.specularCoeff = specular;
    this.objectInverseTransform = invTransform;
  }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
    if (tm == null) tm = new Matrix4 ();
    this.objectInverseTransform = tm;
  }
  
  @Override
  public Color getColorAt(Point3 worldPoint, Vector3 worldNormal, Light light, Point3 viewPos) {
    // 1. Coordinate transformation
    Point3 localPoint = objectInverseTransform.transformPoint(worldPoint);
    Vector3 normal = objectInverseTransform.transformNormal(worldNormal).normalize();
    
    // 2. Procedural damask pattern
    double pattern = Math.sin(localPoint.x * 5) *
    Math.cos(localPoint.y * 7) *
    Math.sin(localPoint.z * 3);
    Color baseColor = pattern > 0 ? primaryColor : secondaryColor;
    
    // 3. Light calculations
    Vector3 lightDir = light.getPosition().subtract(worldPoint).normalize();
    Vector3 viewDir = viewPos.subtract(worldPoint).normalize();
    Vector3 reflectDir = lightDir.negate().reflect(normal);
    
    // 4. Components
    double NdotL = Math.max(0, normal.dot(lightDir));
    double RdotV = Math.max(0, reflectDir.dot(viewDir));
    
    // 5. Light effect (supports colored light)
    Color lightColor = light.getColor();
    double intensity = light.getIntensityAt(worldPoint);
    
    // 6. Color mixing
    Color ambient = ColorUtil.multiplyColors(baseColor, lightColor, ambientCoeff);
    Color diffuse = ColorUtil.multiplyColors(baseColor, lightColor, NdotL * intensity);
    Color specular = ColorUtil.multiplyColors(lightColor,
      new Color(255, 255, 255),
    Math.pow(RdotV, shininess) * specularCoeff * intensity);
    
    return ColorUtil.combineColors(ambient, diffuse, specular);
  }
  
  @Override public double getReflectivity() { return reflectivity; }
  @Override public double getIndexOfRefraction() { return 1.4; }
  @Override public double getTransparency() { return 0.0; }
  
}

/***
// Scene setup
Scene scene = new Scene();

// 1. Ceramic sphere
Sphere ceramicSphere = new Sphere(1.0);
ceramicSphere.setTransform(Matrix4.translate(new Vector3(-2, 0, -5)));
ceramicSphere.setMaterial(new DamaskCeramicMaterial(
new Color(240, 240, 240), // White
new Color(70, 70, 70),    // Gray
50.0,                     // Shininess
ceramicSphere.getInverseTransform()
));
scene.addShape(ceramicSphere);

// 2. Water sphere
WaterRippleMaterial waterMat = new WaterRippleMaterial(
new Color(80, 180, 220), // Water blue
0.5,                     // Wave speed
Matrix4.translate(new Vector3(2, 0, -5)).inverse()
);
Sphere waterSphere = new Sphere(1.0);
waterSphere.setTransform(Matrix4.translate(new Vector3(2, 0, -5)));
waterSphere.setMaterial(waterMat);
scene.addShape(waterSphere);

// 3. Light source
scene.addLight(new MuratPointLight(
new Point3(0, 5, 0),
new Color(255, 220, 180), // Warm white
2.0
));

// In render loop
double deltaTime = 0.016; // ~60 FPS
waterMat.update(deltaTime);
 */
