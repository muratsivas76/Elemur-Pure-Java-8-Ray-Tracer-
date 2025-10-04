package net.elena.murat.shape;

import java.awt.image.BufferedImage;
import java.util.List;

import net.elena.murat.material.Material;
import net.elena.murat.math.*;

/**
 * A 2D quad in 3D space for displaying transparent images (e.g., emojis).
 * No UV passed to Intersection. Material must compute UV from point and transform.
 */
public class EmojiBillboard implements EMShape {
  
  private final double width;
  private final double height;
  private final boolean isRectangle;
  private final boolean isVisible;
  private final BufferedImage texture;
  
  private Matrix4 transform = Matrix4.identity();
  private Matrix4 inverseTransform;
  private Matrix4 inverseTransposeTransformForNormal;
  private Material material;
  
  public EmojiBillboard(double width, double height, 
         boolean isRectangle,
		 boolean isVisible,
		 BufferedImage texture) {
    this.width = width;
    this.height = height;
	this.isRectangle = isRectangle;
	this.isVisible = isVisible;
	this.texture = texture;
	
    updateTransforms();
  }
  
  public EmojiBillboard(double width, double height) {
    this (width, height, true, true, null);
  }
  
  public EmojiBillboard(double size) {
    this(size, size);
  }
  
  public EmojiBillboard() {
    this(1.0, 1.0);
  }
  
  // --- EMShape Methods ---
  
  @Override
  public void setTransform(Matrix4 transform) {
    this.transform = new Matrix4(transform);
    updateTransforms();
  }
  
  @Override
  public Matrix4 getTransform() {
    return this.transform;
  }
  
  @Override
  public Matrix4 getInverseTransform() {
    return this.inverseTransform;
  }
  
  private void updateTransforms() {
    this.inverseTransform = this.transform.inverse();
    this.inverseTransposeTransformForNormal = this.transform.inverse().transpose();
  }
  
  @Override
  public void setMaterial(Material material) {
    this.material = material;
  }
  
  @Override
  public Material getMaterial() {
    return this.material;
  }
  
  public boolean isVisible() {
    return isVisible;
  }
	
  @Override
  public double intersect(Ray ray) {
	  if (isRectangle) {
		return intersectR(ray);
	  } else {
		return intersectO(ray);
	  }
  }
  
  // --- Original intersect ---
  public double intersectR(Ray ray) {
    if (inverseTransform == null) return Double.POSITIVE_INFINITY;
    
    Ray localRay = ray.transform(inverseTransform);
    
    Vector3 dir = localRay.getDirection();
    if (Math.abs(dir.z) < Ray.EPSILON) {
      return Double.POSITIVE_INFINITY;
    }
    
    double t = -localRay.getOrigin().z / dir.z;
    if (t < Ray.EPSILON) return Double.POSITIVE_INFINITY;
    
    Point3 localHit = localRay.pointAtParameter(t);
    double x = localHit.x;
    double y = localHit.y;
    
    double halfWidth = width / 2.0;
    double halfHeight = height / 2.0;
    
	if (Math.abs(x) <= halfWidth && Math.abs(y) <= halfHeight) {
        return t;
    }
	
    return Double.POSITIVE_INFINITY;
  }

public double intersectO(Ray ray) {
    if (inverseTransform == null) return Double.POSITIVE_INFINITY;
    
    Ray localRay = ray.transform(inverseTransform);
    
    Vector3 dir = localRay.getDirection();
    if (Math.abs(dir.z) < Ray.EPSILON) {
        return Double.POSITIVE_INFINITY;
    }
    
    double t = -localRay.getOrigin().z / dir.z;
    if (t < Ray.EPSILON) return Double.POSITIVE_INFINITY;
    
    Point3 localHit = localRay.pointAtParameter(t);
    double x = localHit.x;
    double y = localHit.y;
    
    double radiusX = width / 2.0;
    double radiusY = height / 2.0;
    
    // Normalize coordinates to unit circle
    double nx = x / radiusX;
    double ny = y / radiusY;
    
    // Check if inside ellipse
    if (nx * nx + ny * ny <= 1.0) {
        return t;
    }
    
    return Double.POSITIVE_INFINITY;
}

  @Override
  public List<IntersectionInterval> intersectAll(Ray ray) {
    double t = intersect(ray);
    if (t == Double.POSITIVE_INFINITY) {
      return java.util.Collections.emptyList();
    }
    
    Point3 hitPoint = ray.pointAtParameter(t);
    Vector3 normal = getNormalAt(hitPoint);
    
    Intersection intersection = new Intersection(hitPoint, normal, t, this);
    
    return java.util.Arrays.asList(
      new IntersectionInterval(t, t, intersection, intersection)
    );
  }
  
  @Override
  public Vector3 getNormalAt(Point3 worldPoint) {
    if (inverseTransposeTransformForNormal == null) {
      return new Vector3(0, 0, 1);
    }
    
    Vector3 localNormal = new Vector3(0, 0, 1);
    return inverseTransposeTransformForNormal
    .transformDirection(localNormal)
    .normalize();
  }
  
  public double getWidth() {
    return width;
  }
  
  public double getHeight() {
    return height;
  }
  
}
