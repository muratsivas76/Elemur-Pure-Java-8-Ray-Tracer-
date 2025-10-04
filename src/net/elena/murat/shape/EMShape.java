package net.elena.murat.shape;

import java.util.List;

//custom
import net.elena.murat.math.*;
import net.elena.murat.material.Material;

public interface EMShape {
  
  //For CSG
  List<IntersectionInterval> intersectAll(Ray ray);
  
  //Old Methods
  double intersect(Ray ray);
  
  void setMaterial(Material material);
  void setTransform(Matrix4 transform);
  
  Vector3 getNormalAt(Point3 point);
  
  Material getMaterial();
  
  Matrix4 getTransform();
  Matrix4 getInverseTransform();
  
}
