package net.elena.murat.light;

import java.awt.Color;

import net.elena.murat.lovert.Scene;
import net.elena.murat.math.Point3;
import net.elena.murat.math.Vector3;

public interface Light {
  
  Point3 getPosition();
  
  Color getColor();
  
  double getIntensity();
  
  Vector3 getDirectionAt(Point3 point);
  
  double getAttenuatedIntensity(Point3 point);
  
  double getIntensityAt(Point3 point);
  
  Vector3 getDirectionTo(Point3 point);
  
  boolean isVisibleFrom(Point3 point, Scene scene);
  
}
