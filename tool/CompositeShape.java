package net.elena.murat.shape;

import net.elena.murat.math.*;
import net.elena.murat.material.Material;
import java.util.ArrayList;
import java.util.List;
import java.awt.Color;
import net.elena.murat.material.SolidColorMaterial;

public class CompositeShape implements EMShape {
  private final List<EMShape> shapes = new ArrayList<>();
  private Material material;
  // CompositeShape'in kendi transformu artık çocukların transformlarını etkileyecek.
  // Bu transform, CompositeShape'in dünya uzayındaki konumunu belirler.
  private Matrix4 currentWorldTransform = Matrix4.identity();
  private Matrix4 currentInverseWorldTransform = Matrix4.identity();
  private Matrix4 currentInverseTransposeWorldTransform = Matrix4.identity();
  
  // Her çocuğun kendi yerel transformunu saklamak için bir liste
  private final List<Matrix4> childLocalTransforms = new ArrayList<>();
  
  // Vurulan son alt şekli saklamak için geçici değişken
  private transient EMShape lastHitSubShape = null;
  
  public CompositeShape() {
    this.material = new SolidColorMaterial(Color.GRAY);
  }
  
  public void addShape(EMShape shape) {
    shapes.add(shape);
    // Çocuğun mevcut transformunu (kendi yerel transformu) sakla
    childLocalTransforms.add(shape.getTransform());
    // Eğer alt şeklin materyali null ise, kompozitin materyalini ata.
    if (shape.getMaterial() == null) {
      shape.setMaterial(this.material);
    }
  }
  
  @Override
  public double intersect(Ray ray) {
    // Ray'i CompositeShape'in yerel uzayına DÖNÜsTÜRMEYİN.
    // Ray'i doğrudan çocuklara geçirin. Çocuklar kendi dönüşümlerini uygulayacak.
    double minT = Double.POSITIVE_INFINITY;
    lastHitSubShape = null;
    
    for (EMShape shape : shapes) {
      double t = shape.intersect(ray); // Ray'i doğrudan çocuğa ver
      if (t > Ray.EPSILON && t < minT) {
        minT = t;
        lastHitSubShape = shape;
      }
    }
    
    if (minT == Double.POSITIVE_INFINITY) {
      return -1;
    }
    return minT;
  }
  
  @Override
  public Vector3 getNormalAt(Point3 worldPoint) {
    if (lastHitSubShape != null) {
      // Vurulan alt şeklin dünya koordinatlarındaki normalini al
      // CompositeShape'in kendi transformu artık ray'i etkilemediği için
      // normalin de sadece alt şekilden gelmesi yeterli.
      return lastHitSubShape.getNormalAt(worldPoint);
    }
    return new Vector3(0, 0, 1);
  }
  
  @Override
  public void setTransform(Matrix4 transform) {
    this.currentWorldTransform = transform;
    this.currentInverseWorldTransform = transform.inverse();
    this.currentInverseTransposeWorldTransform = transform.inverseTransposeForNormal();
    
    // Her çocuğun transformunu güncelle:
    // Çocuğun yeni dünya transformu = CompositeShape'in dünya transformu * Çocuğun kendi yerel transformu
    for (int i = 0; i < shapes.size(); i++) {
      EMShape shape = shapes.get(i);
      Matrix4 localTransform = childLocalTransforms.get(i);
      // Çocuk şeklinin dönüşümünü, CompositeShape'in dönüşümü ile birleştir.
      // Bu, CompositeShape'in dönüşümünü çocukların üzerine uygular.
      shape.setTransform(this.currentWorldTransform.multiply(localTransform));
    }
  }
  
  @Override
  public Matrix4 getTransform() {
    return currentWorldTransform;
  }
  
  @Override
  public Matrix4 getInverseTransform() {
    return currentInverseWorldTransform;
  }
  
  @Override
  public Material getMaterial() {
    return material;
  }
  
  @Override
  public void setMaterial(Material material) {
    this.material = material;
    for (EMShape shape : shapes) {
      if (shape.getMaterial() == null) {
        shape.setMaterial(material);
      }
    }
  }
}
