package net.elena.murat.math;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PolynomialSolver {
  public static final double EPS = 1e-10; // Tighter tolerance
  private static final double CUBIC_EPS = 1e-8; // Special tolerance for cubic equations
  
  // Helper method: Filters numbers close to zero
  private static boolean isApproxZero(double val, double epsilon) {
    return Math.abs(val) < epsilon;
  }
  
  // Linear equation solver (ax + b = 0)
  public static List<Double> solveLinear(double a, double b) {
    if (isApproxZero(a, EPS)) {
      return isApproxZero(b, EPS) ?
      Collections.singletonList(Double.POSITIVE_INFINITY) : // Infinite solutions
      Collections.emptyList();
    }
    return Collections.singletonList(-b / a);
  }
  
  // Quadratic equation solver (ax² + bx + c = 0)
  public static List<Double> solveQuadratic(double a, double b, double c) {
    if (isApproxZero(a, EPS)) return solveLinear(b, c);
    
    double discriminant = b*b - 4*a*c;
    List<Double> roots = new ArrayList<>();
    
    if (discriminant < -EPS) return roots;
    
    if (isApproxZero(discriminant, EPS)) {
      roots.add(-b / (2*a));
      } else {
      double sqrtDisc = Math.sqrt(discriminant);
      roots.add((-b + sqrtDisc) / (2*a));
      roots.add((-b - sqrtDisc) / (2*a));
    }
    return roots;
  }
  
  // Cubic equation solver (x³ + a2x² + a1x + a0 = 0)
  public static List<Double> solveCubic(double a2, double a1, double a0) {
    // Convert to depressed form: y³ + py + q = 0
    double p = a1 - a2*a2/3.0;
    double q = a0 - a2*a1/3.0 + 2*a2*a2*a2/27.0;
    
    // Special case: p ≈ 0
    if (isApproxZero(p, CUBIC_EPS)) {
      return solveLinear(1.0, q).stream()
      .map(y -> y - a2/3.0)
      .collect(Collectors.toList());
    }
    
    double discriminant = q*q/4.0 + p*p*p/27.0;
    List<Double> roots = new ArrayList<>();
    double offset = a2 / 3.0;
    
    if (discriminant > EPS) { // 1 real root
      double u = cbrt(-q/2.0 + Math.sqrt(discriminant));
      double v = cbrt(-q/2.0 - Math.sqrt(discriminant));
      roots.add(u + v - offset);
    }
    else if (discriminant < -EPS) { // 3 real roots
      double angle = Math.acos(3*q/(2*p)*Math.sqrt(-3/p));
      for (int k = 0; k < 3; k++) {
        roots.add(2*Math.sqrt(-p/3.0) *
        Math.cos((angle - 2*k*Math.PI)/3.0) - offset);
      }
    }
    else { // Coincident roots
      double root = cbrt(q/2.0) - offset;
      Collections.addAll(roots, root, root, root);
    }
    
    return roots.stream().distinct().collect(Collectors.toList());
  }
  
  public static List<Double> solveQuartic(double a3, double a2, double a1, double a0) {
    double p = a2 - 3*a3*a3/8.0;
    double q = a1 - a2*a3/2.0 + a3*a3*a3/8.0;
    double r = a0 - a1*a3/4.0 + a2*a3*a3/16.0 - 3*a3*a3*a3*a3/256.0;
    
    // Biquadratic case (q ≈ 0)
    if (isApproxZero(q, EPS*10)) {
      List<Double> roots = solveQuadratic(1.0, p, r).stream()
      .filter(z -> z >= -EPS)
      .flatMap(z -> {
          double sqrtZ = Math.sqrt(z);
          return isApproxZero(sqrtZ, EPS) ?
          Collections.singletonList(sqrtZ).stream() :
          Stream.of(sqrtZ, -sqrtZ);
      })
      .collect(Collectors.toList());
      
      return roots.stream()
      .map(y -> y - a3/4.0)
      .collect(Collectors.toList());
    }
    
    // Ferrari's method
    List<Double> cubicRoots = solveCubic(
      2*p,
      p*p - 4*r,
      -q*q
    ).stream()
    .filter(z -> z >= -EPS)
    .collect(Collectors.toList());
    
    if (cubicRoots.isEmpty()) return Collections.emptyList();
    
    double z = cubicRoots.get(0);
    double sqrt2z = Math.sqrt(2*z);
    
    // Two quadratic equations
    List<Double> roots = new ArrayList<>();
    double[] params = {
      sqrt2z, z + p/2.0 + q/(2*sqrt2z),
      -sqrt2z, z + p/2.0 - q/(2*sqrt2z)
    };
    
    for (int i = 0; i < 2; i++) {
      solveQuadratic(1.0, params[2*i], params[2*i+1]).stream()
      .map(y -> y - a3/4.0)
      .forEach(roots::add);
    }
    
    return roots.stream()
    .filter(t -> !Double.isNaN(t))
    .distinct()
    .collect(Collectors.toList());
  }
  
  // Cube root calculation (sign-preserving)
  private static double cbrt(double x) {
    return x < 0 ? -Math.pow(-x, 1.0/3.0) : Math.pow(x, 1.0/3.0);
  }
  
}
