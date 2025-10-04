package net.elena.murat.material;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import net.elena.murat.math.*;
import net.elena.murat.light.Light;

public class MultiMixMaterial implements Material {
  
  private List<Material> materials;
  private List<Double> mixRatios;
  private Matrix4 objectTransform;
  private Matrix4 inverseObjectTransform;
  
  public MultiMixMaterial() {
    this.materials = new ArrayList<>();
    this.mixRatios = new ArrayList<>();
    this.objectTransform = Matrix4.identity();
    this.inverseObjectTransform = Matrix4.identity();
  }
  
  public MultiMixMaterial(Material[] materials, double[] ratios) {
    this();
    if (materials.length != ratios.length) {
      throw new IllegalArgumentException("Materials and ratios arrays must have same length");
    }
    
    for (int i = 0; i < materials.length; i++) {
      addMaterial(materials[i], ratios[i]);
    }
  }
  
  public void addMaterial(Material material, double ratio) {
    materials.add(material);
    mixRatios.add(Math.max(0.0, ratio));
    material.setObjectTransform(objectTransform);
  }
  
  public void removeMaterial(int index) {
    if (index >= 0 && index < materials.size()) {
      materials.remove(index);
      mixRatios.remove(index);
    }
  }
  
  public void setMaterialRatio(int index, double ratio) {
    if (index >= 0 && index < mixRatios.size()) {
      mixRatios.set(index, Math.max(0.0, ratio));
    }
  }
  
  @Override
  public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPoint) {
    if (materials.isEmpty()) {
      return Color.BLACK;
    }
    
    // Toplam ratio'yu hesapla (normalizasyon için)
    double totalRatio = mixRatios.stream().mapToDouble(Double::doubleValue).sum();
    
    if (totalRatio == 0.0) {
      return Color.BLACK;
    }
    
    // İlk material ile başla
    Color resultColor = materials.get(0).getColorAt(point, normal, light, viewerPoint);
    double accumulatedRatio = mixRatios.get(0);
    
    // Diğer materialları sırayla karıştır
    for (int i = 1; i < materials.size(); i++) {
      Color currentColor = materials.get(i).getColorAt(point, normal, light, viewerPoint);
      double currentRatio = mixRatios.get(i);
      
      // Mevcut karışım oranını hesapla
      double blendRatio = currentRatio / (accumulatedRatio + currentRatio);
      
      // Renkleri karıştır
      resultColor = interpolateColors(resultColor, currentColor, blendRatio);
      accumulatedRatio += currentRatio;
    }
    
    return resultColor;
  }
  
  @Override
  public double getReflectivity() {
    if (materials.isEmpty()) return 0.0;
    
    double totalRatio = 0.0;
    double weightedSum = 0.0;
    
    for (int i = 0; i < materials.size(); i++) {
      double ratio = mixRatios.get(i);
      weightedSum += materials.get(i).getReflectivity() * ratio;
      totalRatio += ratio;
    }
    
    return totalRatio > 0 ? weightedSum / totalRatio : 0.0;
  }
  
  @Override
  public double getTransparency() {
    if (materials.isEmpty()) return 0.0;
    
    double totalRatio = 0.0;
    double weightedSum = 0.0;
    
    for (int i = 0; i < materials.size(); i++) {
      double ratio = mixRatios.get(i);
      weightedSum += materials.get(i).getTransparency() * ratio;
      totalRatio += ratio;
    }
    
    return totalRatio > 0 ? weightedSum / totalRatio : 0.0;
  }
  
  @Override
  public double getIndexOfRefraction() {
    if (materials.isEmpty()) return 1.0;
    
    double totalRatio = 0.0;
    double weightedSum = 0.0;
    
    for (int i = 0; i < materials.size(); i++) {
      double ratio = mixRatios.get(i);
      weightedSum += materials.get(i).getIndexOfRefraction() * ratio;
      totalRatio += ratio;
    }
    
    return totalRatio > 0 ? weightedSum / totalRatio : 1.0;
  }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
    this.objectTransform = tm;
    this.inverseObjectTransform = tm.inverse();
    
    // Tüm alt materiallara transformu ilet
    for (Material material : materials) {
      material.setObjectTransform(tm);
    }
  }
  
  private Color interpolateColors(Color color1, Color color2, double ratio) {
    ratio = Math.max(0.0, Math.min(1.0, ratio));
    float[] comp1 = color1.getRGBColorComponents(null);
    float[] comp2 = color2.getRGBColorComponents(null);
    
    return new Color(
      (float) (comp1[0] * (1 - ratio) + comp2[0] * ratio),
      (float) (comp1[1] * (1 - ratio) + comp2[1] * ratio),
      (float) (comp1[2] * (1 - ratio) + comp2[2] * ratio)
    );
  }
  
  // Getter ve utility metodları
  public int getMaterialCount() {
    return materials.size();
  }
  
  public Material getMaterial(int index) {
    return materials.get(index);
  }
  
  public double getRatio(int index) {
    return mixRatios.get(index);
  }
  
  public void normalizeRatios() {
    double total = mixRatios.stream().mapToDouble(Double::doubleValue).sum();
    if (total > 0) {
      for (int i = 0; i < mixRatios.size(); i++) {
        mixRatios.set(i, mixRatios.get(i) / total);
      }
    }
  }
  
  // Builder-style kullanım için yardımcı metod
  public MultiMixMaterial withMaterial(Material material, double ratio) {
    addMaterial(material, ratio);
    return this;
  }
  
}
