package net.elena.murat.shape;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.elena.murat.math.*;

/**
 * Represents a Constructive Solid Geometry (CSG) Difference operation.
 * The resulting shape is the difference of two shapes: A - B.
 * A point is inside the difference if it is inside shape A and outside shape B.
 */
public class DifferenceCSG extends CSGShape {
  
  /**
   * Constructs a DifferenceCSG from two shapes.
   * @param left The first shape (left operand, the one being subtracted from).
   * @param right The second shape (right operand, the one being subtracted).
   */
  public DifferenceCSG(EMShape left, EMShape right) {
    super(left, right);
  }
  
  /**
   * Combines the intersection intervals of two shapes using the Difference operation.
   * The difference is formed by finding intervals where the ray is inside 'left' and outside 'right'.
   * @param a List of intervals from the left shape (A).
   * @param b List of intervals from the right shape (B).
   * @return The resulting list of intervals for the difference (A - B).
   */
  @Override
  protected List<IntersectionInterval> combine(
    List<IntersectionInterval> a,
    List<IntersectionInterval> b) {
    
    // 1. If A has no intersections, result is empty
    if (a.isEmpty()) {
      return Collections.emptyList();
    }
    
    // 2. If B has no intersections, result is just A
    if (b.isEmpty()) {
      return new ArrayList<>(a);
    }
    
    // 3. Sort intervals by tIn
    List<IntersectionInterval> sortedA = new ArrayList<>(a);
    List<IntersectionInterval> sortedB = new ArrayList<>(b);
    Collections.sort(sortedA, (ia, ib) -> Double.compare(ia.tIn, ib.tIn));
    Collections.sort(sortedB, (ia, ib) -> Double.compare(ia.tIn, ib.tIn));
    
    List<IntersectionInterval> result = new ArrayList<>();
    
    for (IntersectionInterval intervalA : sortedA) {
      double currentTIn = intervalA.tIn;
      double currentTOut = intervalA.tOut;
      
      // For each interval in A, subtract all overlaps with B
      for (IntersectionInterval intervalB : sortedB) {
        // If B interval starts after A ends, no overlap
        if (intervalB.tIn >= currentTOut - Ray.EPSILON) {
          break;
        }
        
        // If B interval ends before A starts, no overlap
        if (intervalB.tOut <= currentTIn + Ray.EPSILON) {
          continue;
        }
        
        // There is an overlap
        double overlapTIn = Math.max(currentTIn, intervalB.tIn);
        double overlapTOut = Math.min(currentTOut, intervalB.tOut);
        
        // Add part before overlap (if exists)
        if (currentTIn < overlapTIn - Ray.EPSILON) {
          result.add(createInterval(intervalA, currentTIn, overlapTIn));
        }
        
        // Update current interval start
        currentTIn = overlapTOut;
      }
      
      // Add remaining part after all B intervals
      if (currentTIn < currentTOut - Ray.EPSILON) {
        result.add(createInterval(intervalA, currentTIn, currentTOut));
      }
    }
    
    return result;
  }
  
  /**
   * Helper method to create a new IntersectionInterval with correct hit data.
   * Uses linear interpolation to estimate point and normal at new t values.
   * @param original The original interval to copy data from.
   * @param tIn New tIn value.
   * @param tOut New tOut value.
   * @return A new IntersectionInterval.
   */
  private IntersectionInterval createInterval(IntersectionInterval original, double tIn, double tOut) {
    // Interpolate points
    Point3 pointIn = original.in.getPoint().add(
      original.out.getPoint().subtract(original.in.getPoint())
      .scale((tIn - original.tIn) / (original.tOut - original.tIn + Ray.EPSILON))
    );
    Point3 pointOut = original.in.getPoint().add(
      original.out.getPoint().subtract(original.in.getPoint())
      .scale((tOut - original.tIn) / (original.tOut - original.tIn + Ray.EPSILON))
    );
    
    // Use original normals (approximation)
    Vector3 normalIn = original.in.getNormal();
    Vector3 normalOut = original.out.getNormal();
    
    Intersection in = new Intersection(pointIn, normalIn, tIn, this);
    Intersection out = new Intersection(pointOut, normalOut, tOut, this);
    
    return new IntersectionInterval(tIn, tOut, in, out);
  }
  
}