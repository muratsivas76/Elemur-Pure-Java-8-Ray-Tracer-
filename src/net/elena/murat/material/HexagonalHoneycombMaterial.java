package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.math.*;
import net.elena.murat.light.*;

public class HexagonalHoneycombMaterial implements Material {
  private final Color primaryColor;
  private final Color secondaryColor;
  private final Color borderColor;
  private final double cellSize;
  private final double borderWidth;
  private final double ambientStrength;
  private final double specularStrength;
  private final double shininess;
  private final double reflectivity = 0.01;
  
  public HexagonalHoneycombMaterial(Color primary, Color secondary, double cellSize, double borderWidth) {
    this(primary, secondary, Color.BLACK, cellSize, borderWidth, 0.2, 0.5, 32.0);
  }
  
  public HexagonalHoneycombMaterial(Color primary, Color secondary, Color borderColor,
    double cellSize, double borderWidth,
    double ambientStrength, double specularStrength,
    double shininess) {
    this.primaryColor = primary;
    this.secondaryColor = secondary;
    this.borderColor = borderColor;
    this.cellSize = Math.max(0.1, cellSize);
    this.borderWidth = Math.max(0, Math.min(0.5, borderWidth));
    this.ambientStrength = Math.max(0, Math.min(1, ambientStrength));
    this.specularStrength = Math.max(0, Math.min(1, specularStrength));
    this.shininess = Math.max(1, shininess);
  }
  
  @Override
  public Color getColorAt(Point3 worldPoint, Vector3 normal, Light light, Point3 viewPos) {
    // Hexagonal coordinate calculation
    double size = cellSize * 2.0;
    double x = (worldPoint.x + 1000) / size; // +1000 to avoid negative values
    double z = (worldPoint.z + 1000) / size;
    
    // Hexagonal grid coordinates
    double q = (x * Math.sqrt(3.0)/3.0 - z / 3.0);
    double r = z * 2.0/3.0;
    
    // Find hexagon center
    int q1 = (int)Math.round(q);
    int r1 = (int)Math.round(r);
    int s1 = (int)Math.round(-q - r);
    
    // Check if inside hexagon
    double dq = Math.abs(q - q1);
    double dr = Math.abs(r - r1);
    double ds = Math.abs(-q - r - s1);
    
    // Determine cell color
    boolean isBorder = (dq > (0.5 - borderWidth)) ||
    (dr > (0.5 - borderWidth)) ||
    (ds > (0.5 - borderWidth));
    
    if(isBorder) {
      return borderColor;
    }
    
    // Select cell color for pattern
    boolean isPrimary = (q1 + r1 + s1) % 2 == 0;
    Color baseColor = isPrimary ? primaryColor : secondaryColor;
    
    // Lighting calculations
    Vector3 lightDir = light.getDirectionAt(worldPoint).normalize();
    double NdotL = Math.max(0.1, normal.dot(lightDir));
    double intensity = light.getIntensityAt(worldPoint);
    
    // Ambient + Diffuse
    int red = (int)(baseColor.getRed() * (ambientStrength + NdotL * intensity * light.getColor().getRed()/255.0));
    int green = (int)(baseColor.getGreen() * (ambientStrength + NdotL * intensity * light.getColor().getGreen()/255.0));
    int blue = (int)(baseColor.getBlue() * (ambientStrength + NdotL * intensity * light.getColor().getBlue()/255.0));
    
    return new Color(
      Math.max(0, Math.min(255, red)),
      Math.max(0, Math.min(255, green)),
      Math.max(0, Math.min(255, blue))
    );
  }
  
  private double smoothstep(double edge0, double edge1, double x) {
    x = Math.max(0, Math.min(1, (x - edge0)/(edge1 - edge0)));
    return x * x * (3 - 2 * x);
  }
  
  private int clamp(int value) {
    return Math.max(0, Math.min(255, value));
  }
  
  @Override public double getReflectivity() { return reflectivity; }
  @Override public double getIndexOfRefraction() { return 1.0; }
  @Override public double getTransparency() { return 0.0; }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
  }
  
}

/***
Plane honeycombPlane = new Plane(new Point3(0,0,0), new Vector3(0,1,0));
Material honeycombMat = new HexagonalHoneycombMaterial(
new Color(255, 215, 0), // Gold yellow
new Color(255, 255, 150), // Light yellow
0.5,  // Cell size (scale factor)
0.05  // Border thickness
);
honeycombPlane.setMaterial(honeycombMat);

Material honeycomb = new HexagonalHoneycombMaterial(
new Color(255, 239, 153), // Light yellow (honey color)
new Color(255, 204, 51),  // Gold yellow
new Color(50, 50, 50),     // Dark gray borders
0.3,                      // Cell size
0.05,                     // Border thickness
0.2,                      // Ambient strength
0.3,                      // Specular
16.0                      // Shininess
);
 */
