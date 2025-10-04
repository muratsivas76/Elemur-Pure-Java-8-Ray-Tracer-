package net.elena.murat.shape;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.elena.murat.math.*;

/**
 * Represents a Constructive Solid Geometry (CSG) Union operation.
 * The resulting shape is the union of two shapes: A ∪ B.
 * A point is inside the union if it is inside shape A, shape B, or both.
 */
public class UnionCSG extends CSGShape {
  
  /**
   * Constructs a UnionCSG from two shapes.
   * @param left The first shape (left operand).
   * @param right The second shape (right operand).
   */
  public UnionCSG(EMShape left, EMShape right) {
    super(left, right);
  }
  
  /**
   * Combines the intersection intervals of two shapes using the Union operation.
   * The union is formed by merging overlapping intervals from both shapes.
   * @param a List of intervals from the left shape.
   * @param b List of intervals from the right shape.
   * @return The resulting list of non-overlapping intervals for the union.
   */
  @Override
  protected List<IntersectionInterval> combine(
    List<IntersectionInterval> a,
    List<IntersectionInterval> b) {
    
    // 1. Combine all intervals from both shapes
    List<IntersectionInterval> all = new ArrayList<>();
    all.addAll(a);
    all.addAll(b);
    
    // 2. Sort intervals by tIn (entry point)
    Collections.sort(all, (ia, ib) -> Double.compare(ia.tIn, ib.tIn));
    
    // 3. Merge overlapping intervals
    List<IntersectionInterval> merged = new ArrayList<>();
    if (all.isEmpty()) return merged;
    
    IntersectionInterval current = all.get(0);
    
    for (int i = 1; i < all.size(); i++) {
      IntersectionInterval next = all.get(i);
      
      // If current interval overlaps or touches the next one
      if (current.tOut >= next.tIn - Ray.EPSILON) {
        // Extend the current interval's tOut
        double newTOut = Math.max(current.tOut, next.tOut);
        current = new IntersectionInterval(
          current.tIn,
          newTOut,
          current.in, // Keep original in hit
          next.out  // Use new out hit
        );
        } else {
        // No overlap, finalize current interval
        merged.add(current);
        current = next;
      }
    }
    // Add the last interval
    merged.add(current);
    
    return merged;
  }
  
}
