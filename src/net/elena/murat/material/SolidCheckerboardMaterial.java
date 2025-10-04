package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.math.*;
import net.elena.murat.light.*;
import net.elena.murat.util.ColorUtil;

public final class SolidCheckerboardMaterial implements Material {
  private final Color color1;
  private final Color color2;
  private final double size;
  private Matrix4 objectInverseTransform;
  private final double ambient;
  private final double diffuse;
  
  public SolidCheckerboardMaterial(Color color1, Color color2, double size,
    double ambient, double diffuse,
    Matrix4 objectInverseTransform) {
    // Null-safe initialization
    this.color1 = color1 != null ? color1 : new Color(100, 100, 100);
    this.color2 = color2 != null ? color2 : new Color(200, 200, 200);
    this.size = size > 0 ? size : 1.0;
    this.objectInverseTransform = objectInverseTransform != null ? objectInverseTransform : new Matrix4();
    this.ambient = Math.max(0, Math.min(1, ambient));
    this.diffuse = Math.max(0, Math.min(1, diffuse));
  }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
    if (tm == null) tm = new Matrix4 ();
    this.objectInverseTransform = tm;
  }
  
  @Override
  public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewPos) {
    // Null-safe point
    Point3 safePoint = point != null ? point : Point3.ORIGIN;
    
    Vector3 N = normal.normalize(); // normal zaten normalize edilmiş olmalı ama emin olalım
    
    // Spherical mapping: normal vektöründen u,v çıkar
    double u = (Math.atan2(N.z, N.x) + Math.PI) / (2 * Math.PI); // 0 to 1
    double v = (Math.asin(N.y) + Math.PI/2) / Math.PI;           // 0 to 1
    
    // Tiles size
    int tiles = (int) (10); // 10x10
    int x = (int) Math.floor(u * tiles);
    int y = (int) Math.floor(v * tiles);
    
    Color baseColor = ((x + y) % 2 == 0) ? color1 : color2;
    
    // Ambient if there is no light
    if (light == null) {
      return ColorUtil.scale(baseColor, ambient);
    }
    
    // Diffuse lighting
    Point3 lightPos = light.getPosition() != null ? light.getPosition() : new Point3(0, 10, 0);
    Vector3 lightDir = lightPos.subtract(safePoint).normalize();
    double NdotL = Math.max(0, normal.dot(lightDir));
    
    return ColorUtil.scale(baseColor, ambient + diffuse * NdotL * light.getIntensity());
  }
  
  // Reflection closed
  @Override public double getReflectivity() { return 0.0; }
  @Override public double getIndexOfRefraction() { return 1.0; }
  @Override public double getTransparency() { return 0.0; }
  
}
