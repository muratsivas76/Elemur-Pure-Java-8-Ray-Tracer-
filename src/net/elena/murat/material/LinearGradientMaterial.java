package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.light.*;
import net.elena.murat.math.*;
import net.elena.murat.util.ColorUtil;

public class LinearGradientMaterial implements Material {
    private final Color topColor;
    private final Color bottomColor;

    public LinearGradientMaterial(Color topColor, Color bottomColor) {
        this.topColor = topColor;
        this.bottomColor = bottomColor;
    }

    @Override
    public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPos) {
        double t = (point.y + 1.0) / 2.0; // y E [-1,1] -> [0,1]

        int r = (int)(topColor.getRed() * t + bottomColor.getRed() * (1 - t));
        int g = (int)(topColor.getGreen() * t + bottomColor.getGreen() * (1 - t));
        int b = (int)(topColor.getBlue() * t + bottomColor.getBlue() * (1 - t));

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
