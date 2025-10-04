package net.elena.murat.shape.letters;

import java.awt.Font;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import net.elena.murat.math.*;
import net.elena.murat.material.Material;
import net.elena.murat.shape.EMShape;
import net.elena.murat.util.LetterUtils3D;

public class Letter3D implements EMShape {
  private final char letter;
  private final double thickness;
  private Matrix4 transform;
  private Matrix4 inverseTransform;
  private Material material;
  private final LetterUtils3D.LetterMesh mesh;
  
  public Letter3D(char c) {
    this(c, 16, 1, 1, 0.1, new Font("Arial", Font.BOLD, 32));
  }
  
  public Letter3D(char letter, int baseSize,
    double widthScale, double heightScale,
    double thickness, Font font) {
    this.letter = letter;
    this.thickness = thickness;
    this.transform = Matrix4.identity();
    this.inverseTransform = Matrix4.identity();
    
    // Original render (slow but correct)
    BufferedImage img = LetterUtils3D.getLetterImage(letter, font, widthScale, heightScale, baseSize);
    
    // DEBUG
    try {
      javax.imageio.ImageIO.write(img, "PNG", new java.io.File("letterImage.png"));
      System.out.println ("Created letterImage.png file.");
    } catch (java.io.IOException ioe)
    {}
    
    boolean[][] pixelData = LetterUtils3D.getLetterPixelData(img);
    this.mesh = LetterUtils3D.getLetterMeshData(pixelData, thickness);
  }
  
  @Override
  public List<IntersectionInterval> intersectAll(Ray worldRay) {
    if (!intersectBoundingBox(worldRay)) {
      return Collections.emptyList();
    }
    
    Ray localRay = new Ray(
      inverseTransform.transformPoint(worldRay.getOrigin()),
      inverseTransform.transformVector(worldRay.getDirection())
    );
    
    List<IntersectionInterval> intervals = new ArrayList<>(16);
    
    for (LetterUtils3D.Face face : mesh.faces) {
      LetterUtils3D.Vertex v1 = mesh.vertices.get(face.v1);
      LetterUtils3D.Vertex v2 = mesh.vertices.get(face.v2);
      LetterUtils3D.Vertex v3 = mesh.vertices.get(face.v3);
      
      Point3 p1 = new Point3(v1.x, v1.y, v1.z);
      Point3 p2 = new Point3(v2.x, v2.y, v2.z);
      Point3 p3 = new Point3(v3.x, v3.y, v3.z);
      
      Vector3 edge1 = p2.subtract(p1);
      Vector3 edge2 = p3.subtract(p1);
      Vector3 normal = edge1.cross(edge2);
      double denom = normal.dot(localRay.getDirection());
      
      if (denom > -Ray.EPSILON && denom < Ray.EPSILON) continue;
      
      double t = normal.dot(p1.toVector().subtract(localRay.getOrigin().toVector())) / denom;
      if (t < Ray.EPSILON) continue;
      
      Point3 hitPoint = localRay.pointAtParameter(t);
      
      if (isPointInTriangle(hitPoint, p1, p2, p3)) {
        intervals.add(new IntersectionInterval(t, t,
            new Intersection(hitPoint, normal.normalize(), t, this),
        new Intersection(hitPoint, normal.normalize(), t, this)));
      }
    }
    
    intervals.sort((a, b) -> Double.compare(a.tIn, b.tIn));
    return intervals;
  }
  
  private boolean intersectBoundingBox(Ray worldRay) {
    Point3 boundsMin = new Point3(0, 0, -thickness/2);
    Point3 boundsMax = new Point3(1, 1, thickness/2);
    Point3 localOrigin = inverseTransform.transformPoint(worldRay.getOrigin());
    Vector3 localDir = inverseTransform.transformVector(worldRay.getDirection());
    double tMin = Double.NEGATIVE_INFINITY;
    double tMax = Double.POSITIVE_INFINITY;
    
    for (int i = 0; i < 3; i++) {
      double invD = 1.0 / localDir.get(i);
      double t0 = (boundsMin.get(i) - localOrigin.get(i)) * invD;
      double t1 = (boundsMax.get(i) - localOrigin.get(i)) * invD;
      
      if (invD < 0.0) {
        double temp = t0;
        t0 = t1;
        t1 = temp;
      }
      
      tMin = Math.max(t0, tMin);
      tMax = Math.min(t1, tMax);
      
      if (tMax <= tMin) return false;
    }
    return true;
  }
  
  private boolean isPointInTriangle(Point3 p, Point3 a, Point3 b, Point3 c) {
    Vector3 v0 = c.subtract(a);
    Vector3 v1 = b.subtract(a);
    Vector3 v2 = p.subtract(a);
    
    double dot00 = v0.dot(v0);
    double dot01 = v0.dot(v1);
    double dot02 = v0.dot(v2);
    double dot11 = v1.dot(v1);
    double dot12 = v1.dot(v2);
    
    double invDenom = 1.0 / (dot00 * dot11 - dot01 * dot01);
    double u = (dot11 * dot02 - dot01 * dot12) * invDenom;
    double v = (dot00 * dot12 - dot01 * dot02) * invDenom;
    
    return (u >= 0) && (v >= 0) && (u + v <= 1);
  }
  
  @Override public double intersect(Ray ray) {
    List<IntersectionInterval> intervals = intersectAll(ray);
    return intervals.isEmpty() ? Double.POSITIVE_INFINITY : intervals.get(0).tIn;
  }
  
  @Override
  public Vector3 getNormalAt(Point3 point) {
    Point3 localPoint = inverseTransform.transformPoint(point);
    double minDistance = Double.POSITIVE_INFINITY;
    Vector3 closestNormal = new Vector3(0, 1, 0);
    
    for (LetterUtils3D.Face face : mesh.faces) {
      LetterUtils3D.Vertex v1 = mesh.vertices.get(face.v1);
      LetterUtils3D.Vertex v2 = mesh.vertices.get(face.v2);
      LetterUtils3D.Vertex v3 = mesh.vertices.get(face.v3);
      
      Point3 p1 = new Point3(v1.x, v1.y, v1.z);
      Point3 p2 = new Point3(v2.x, v2.y, v2.z);
      Point3 p3 = new Point3(v3.x, v3.y, v3.z);
      
      Vector3 normal = p2.subtract(p1).cross(p3.subtract(p1)).normalize();
      double distance = Math.abs(normal.dot(localPoint.toVector().subtract(p1.toVector())));
      
      if (distance < minDistance) {
        minDistance = distance;
        closestNormal = normal;
      }
    }
    
    Vector3 scaledNormal = inverseTransform.transformNormal(closestNormal);
    return new Vector3(
      scaledNormal.x / transform.getScaleX(),
      scaledNormal.y / transform.getScaleY(),
      scaledNormal.z / transform.getScaleZ()
    ).normalize();
  }
  
  @Override public void setMaterial(Material material) { this.material = material; }
  @Override public Material getMaterial() { return material; }
  
  @Override
  public void setTransform(Matrix4 transform) {
    this.transform = new Matrix4(transform);
    this.inverseTransform = this.transform.inverse();
  }
  
  @Override public Matrix4 getTransform() { return new Matrix4(transform); }
  @Override public Matrix4 getInverseTransform() { return new Matrix4(inverseTransform); }
  
}
