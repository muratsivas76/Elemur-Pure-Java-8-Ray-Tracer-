package net.elena.murat.shape;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.elena.murat.math.*;

/**
 * Represents a Constructive Solid Geometry (CSG) Intersection operation.
 * The resulting shape is the intersection of two shapes: A ∩ B.
 * A point is inside the intersection only if it is inside both shape A and shape B.
 */
public class IntersectionCSG extends CSGShape {
  
  /**
   * Constructs an IntersectionCSG from two shapes.
   * @param left The first shape (left operand).
   * @param right The second shape (right operand).
   */
  public IntersectionCSG(EMShape left, EMShape right) {
    super(left, right);
  }
  
  /**
   * Combines the intersection intervals of two shapes using the Intersection operation.
   * The intersection is formed by finding intervals where the ray is inside both shapes.
   * @param a List of intervals from the left shape.
   * @param b List of intervals from the right shape.
   * @return The resulting list of intervals for the intersection.
   */
  @Override
  protected List<IntersectionInterval> combine(
    List<IntersectionInterval> a,
    List<IntersectionInterval> b) {
    
    // 1. If either list is empty, intersection is empty
    if (a.isEmpty() || b.isEmpty()) {
      return Collections.emptyList();
    }
    
    // 2. Sort both interval lists by tIn
    List<IntersectionInterval> sortedA = new ArrayList<>(a);
    List<IntersectionInterval> sortedB = new ArrayList<>(b);
    Collections.sort(sortedA, (ia, ib) -> Double.compare(ia.tIn, ib.tIn));
    Collections.sort(sortedB, (ia, ib) -> Double.compare(ia.tIn, ib.tIn));
    
    List<IntersectionInterval> result = new ArrayList<>();
    
    int i = 0, j = 0;
    while (i < sortedA.size() && j < sortedB.size()) {
      IntersectionInterval intervalA = sortedA.get(i);
      IntersectionInterval intervalB = sortedB.get(j);
      
      // Find overlap: max(tIn) to min(tOut)
      double overlapTIn = Math.max(intervalA.tIn, intervalB.tIn);
      double overlapTOut = Math.min(intervalA.tOut, intervalB.tOut);
      
      // If there is a valid overlap
      if (overlapTIn < overlapTOut - Ray.EPSILON) {
        // Calculate midpoint of the two entry points
        Point3 pointIn = new Point3(
          (intervalA.in.getPoint().x + intervalB.in.getPoint().x) * 0.5,
          (intervalA.in.getPoint().y + intervalB.in.getPoint().y) * 0.5,
          (intervalA.in.getPoint().z + intervalB.in.getPoint().z) * 0.5
        );
        Point3 pointOut = new Point3(
          (intervalA.out.getPoint().x + intervalB.out.getPoint().x) * 0.5,
          (intervalA.out.getPoint().y + intervalB.out.getPoint().y) * 0.5,
          (intervalA.out.getPoint().z + intervalB.out.getPoint().z) * 0.5
        );
        
        // Average the normals
        Vector3 normalIn = intervalA.in.getNormal()
        .add(intervalB.in.getNormal())
        .normalize();
        Vector3 normalOut = intervalA.out.getNormal()
        .add(intervalB.out.getNormal())
        .normalize();
        
        Intersection in = new Intersection(pointIn, normalIn, overlapTIn, this);
        Intersection out = new Intersection(pointOut, normalOut, overlapTOut, this);
        
        result.add(new IntersectionInterval(overlapTIn, overlapTOut, in, out));
      }
      
      // Advance the interval with the smaller tOut
      if (intervalA.tOut < intervalB.tOut) {
        i++;
        } else {
        j++;
      }
    }
    
    return result;
  }
  
}
