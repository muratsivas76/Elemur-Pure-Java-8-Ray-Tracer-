package net.elena.murat.material;

import java.awt.Color;

import net.elena.murat.math.*;
import net.elena.murat.light.Light;

public class EdgeLightColorMaterial implements Material {
  
  private float edgeThreshold = 0.2f;
  private Color edgeColor = new Color(30, 30, 30);
  private Color baseColor = new Color(200, 200, 200);
  
  private static final Vector3[] KERNEL_OFFSETS = {
    new Vector3(-1, -1, 0), new Vector3(0, -1, 0), new Vector3(1, -1, 0),
    new Vector3(-1,  0, 0), new Vector3(0,  0, 0), new Vector3(1,  0, 0),
    new Vector3(-1,  1, 0), new Vector3(0,  1, 0), new Vector3(1,  1, 0)
  };
  
  private static final float[] KERNEL_WEIGHTS = {
    0.0f, -1.0f,  0.0f,
    -1.0f,  4.0f, -1.0f,
    0.0f, -1.0f,  0.0f
  };
  
  @Override
  public Color getColorAt(Point3 point, Vector3 normal, Light light, Point3 viewerPoint) {
    // Kernel-based edge detection
    float edgeStrength = calculateKernelEdgeStrength(point, normal, viewerPoint);
    
    if (edgeStrength > edgeThreshold) {
      return edgeColor;
      } else {
      // Normal lighting calculation
      Vector3 lightDirection = light.getDirectionTo(point).normalize();
      double intensity = light.getIntensityAt(point);
      float dotProduct = (float) Math.max(0, normal.dot(lightDirection));
      
      // Calculate final color with lighting
      Color lightColor = light.getColor();
      float lightFactor = (float) (intensity * dotProduct);
      
      int r = (int) (baseColor.getRed() * lightFactor * lightColor.getRed() / 255);
      int g = (int) (baseColor.getGreen() * lightFactor * lightColor.getGreen() / 255);
      int b = (int) (baseColor.getBlue() * lightFactor * lightColor.getBlue() / 255);
      
      return new Color(
        Math.min(255, Math.max(0, r)),
        Math.min(255, Math.max(0, g)),
        Math.min(255, Math.max(0, b))
      );
    }
  }
  
  /**
   * Kernel-based edge detection using normal and view direction variations
   * Simulates the effect of a Laplacian kernel on the surface
   */
  private float calculateKernelEdgeStrength(Point3 point, Vector3 centerNormal, Point3 viewerPoint) {
    float totalEdgeValue = 0.0f;
    
    for (int i = 0; i < KERNEL_OFFSETS.length; i++) {
      // Create sample point using kernel offset
      Vector3 offset = KERNEL_OFFSETS[i].multiply(0.05); // Small offset for sampling
      Point3 samplePoint = point.add(offset);
      
      // Calculate normal variation at sample point
      // This simulates what a normal buffer would give us
      Vector3 sampleNormal = calculateSampleNormal(centerNormal, offset);
      
      // Calculate depth variation (simulated)
      float depthVariation = calculateDepthVariation(point, samplePoint, viewerPoint);
      
      // Combine normal and depth variations with kernel weight
      float variation = (float) (centerNormal.subtract(sampleNormal).length() * 0.7 + depthVariation * 0.3);
      totalEdgeValue += variation * KERNEL_WEIGHTS[i];
    }
    
    return Math.abs(totalEdgeValue);
  }
  
  /**
   * Simulates normal variation based on offset direction
   */
  private Vector3 calculateSampleNormal(Vector3 centerNormal, Vector3 offset) {
    // Add some noise/variation to the normal based on offset
    double variation = offset.length() * 0.3;
    return new Vector3(
      centerNormal.x + offset.x * variation,
      centerNormal.y + offset.y * variation,
      centerNormal.z + offset.z * variation
    ).normalize();
  }
  
  /**
   * Simulates depth variation for edge detection
   */
  private float calculateDepthVariation(Point3 center, Point3 sample, Point3 viewer) {
    // Calculate depth differences (simulated)
    double centerDepth = center.subtract(viewer).length();
    double sampleDepth = sample.subtract(viewer).length();
    return (float) Math.abs(centerDepth - sampleDepth) * 2.0f;
  }
  
  @Override
  public void setObjectTransform(Matrix4 tm) {
    // Transformation handling if needed
  }
  
  @Override
  public double getTransparency() {
    return 0.1;
  }
  
  @Override
  public double getReflectivity() {
    return 0.15;
  }
  
  @Override
  public double getIndexOfRefraction() {
    return 1.0;
  }
  
  public void setEdgeThreshold(float threshold) {
    this.edgeThreshold = Math.max(0.0f, Math.min(1.0f, threshold));
  }
  
  public void setEdgeColor(Color color) {
    this.edgeColor = color;
  }
  
  public void setBaseColor(Color color) {
    this.baseColor = color;
  }
  
}
