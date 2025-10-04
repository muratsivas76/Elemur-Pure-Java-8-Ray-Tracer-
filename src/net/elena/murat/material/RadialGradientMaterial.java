package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.light.*;
import net.elena.murat.math.*;
import net.elena.murat.util.ColorUtil;

public class RadialGradientMaterial implements Material {
    private final Color centerColor;
    private final Color edgeColor;

    public RadialGradientMaterial(Color centerColor, Color edgeColor) {
        this.centerColor = centerColor;
        this.edgeColor = edgeColor;
    }

    @Override
    public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPos) {
        double dist = Math.sqrt(point.x * point.x + point.y * point.y);
        dist = Math.min(dist, 1.0);

        int r = (int)(centerColor.getRed() * (1 - dist) + edgeColor.getRed() * dist);
        int g = (int)(centerColor.getGreen() * (1 - dist) + edgeColor.getGreen() * dist);
        int b = (int)(centerColor.getBlue() * (1 - dist) + edgeColor.getBlue() * dist);
		
		r = ColorUtil.clampColorValue (r);
		g = ColorUtil.clampColorValue (g);
		b = ColorUtil.clampColorValue (b);
		
        return new Color(r, g, b);
    }
  
  @Override
  public double getReflectivity() {
    return 0.0;
  }
  
  @Override
  public double getIndexOfRefraction() {
    return 1.0;
  }
  
  @Override
  public double getTransparency() {
    return 0.0; //opaque
  }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
  }
  
}
