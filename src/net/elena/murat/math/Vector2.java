package net.elena.murat.math;

/**
 * 2D vector class for graphics and mathematical operations.
 * Supports basic vector operations and transformations.
 */
public class Vector2 {
    public final double x;
    public final double y;
    
    /**
     * Constructor with x and y components
     * @param x X component
     * @param y Y component
     */
    public Vector2(double x, double y) {
        this.x = x;
        this.y = y;
    }
    
    /**
     * Default constructor (creates zero vector)
     */
    public Vector2() {
        this(0.0, 0.0);
    }
    
    /**
     * Copy constructor
     * @param other Vector to copy
     */
    public Vector2(Vector2 other) {
        this(other.x, other.y);
    }
    
    // --- Basic Arithmetic Operations ---
    
    /**
     * Vector addition
     * @param other Vector to add
     * @return New vector representing the sum
     */
    public Vector2 add(Vector2 other) {
        return new Vector2(this.x + other.x, this.y + other.y);
    }
    
    /**
     * Vector subtraction
     * @param other Vector to subtract
     * @return New vector representing the difference
     */
    public Vector2 subtract(Vector2 other) {
        return new Vector2(this.x - other.x, this.y - other.y);
    }
    
    /**
     * Scalar multiplication
     * @param scalar Scalar value to multiply
     * @return New scaled vector
     */
    public Vector2 multiply(double scalar) {
        return new Vector2(this.x * scalar, this.y * scalar);
    }
    
    /**
     * Scalar division
     * @param scalar Scalar value to divide by
     * @return New divided vector
     * @throws ArithmeticException if scalar is zero
     */
    public Vector2 divide(double scalar) {
        if (Math.abs(scalar) < 1e-10) {
            throw new ArithmeticException("Division by zero");
        }
        return new Vector2(this.x / scalar, this.y / scalar);
    }
    
    /**
     * Component-wise multiplication
     * @param other Vector to multiply with
     * @return New vector with component-wise product
     */
    public Vector2 multiply(Vector2 other) {
        return new Vector2(this.x * other.x, this.y * other.y);
    }
    
    /**
     * Component-wise division
     * @param other Vector to divide by
     * @return New vector with component-wise quotient
     * @throws ArithmeticException if any component of other is zero
     */
    public Vector2 divide(Vector2 other) {
        if (Math.abs(other.x) < 1e-10 || Math.abs(other.y) < 1e-10) {
            throw new ArithmeticException("Division by zero component");
        }
        return new Vector2(this.x / other.x, this.y / other.y);
    }
    
    // --- Vector Operations ---
    
    /**
     * Dot product of two vectors
     * @param other Other vector
     * @return Dot product value
     */
    public double dot(Vector2 other) {
        return this.x * other.x + this.y * other.y;
    }
    
    /**
     * Calculates the magnitude (length) of the vector
     * @return Magnitude of the vector
     */
    public double length() {
        return Math.sqrt(x * x + y * y);
    }
    
    /**
     * Calculates the squared magnitude of the vector
     * (faster than length() for comparison purposes)
     * @return Squared magnitude
     */
    public double lengthSquared() {
        return x * x + y * y;
    }
    
    /**
     * Normalizes the vector (makes it unit length)
     * @return New normalized vector
     * @throws ArithmeticException if vector length is zero
     */
    public Vector2 normalize() {
        double len = length();
        if (len < 1e-10) {
            throw new ArithmeticException("Cannot normalize zero vector");
        }
        return new Vector2(x / len, y / len);
    }
    
    /**
     * Calculates the distance between two vectors
     * @param other Other vector
     * @return Distance between vectors
     */
    public double distance(Vector2 other) {
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }
    
    /**
     * Calculates the squared distance between two vectors
     * @param other Other vector
     * @return Squared distance between vectors
     */
    public double distanceSquared(Vector2 other) {
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        return dx * dx + dy * dy;
    }
    
    /**
     * Linear interpolation between two vectors
     * @param other Target vector
     * @param t Interpolation factor (0.0 = this, 1.0 = other)
     * @return Interpolated vector
     */
    public Vector2 lerp(Vector2 other, double t) {
        t = Math.max(0.0, Math.min(1.0, t)); // Clamp t to [0,1]
        return new Vector2(
            this.x + (other.x - this.x) * t,
            this.y + (other.y - this.y) * t
        );
    }
    
    // --- Utility Methods ---
    
    /**
     * Returns the negated vector
     * @return New negated vector
     */
    public Vector2 negate() {
        return new Vector2(-x, -y);
    }
    
    /**
     * Returns the absolute value of each component
     * @return New vector with absolute components
     */
    public Vector2 abs() {
        return new Vector2(Math.abs(x), Math.abs(y));
    }
    
    /**
     * Clamps the vector components to specified range
     * @param min Minimum value
     * @param max Maximum value
     * @return New clamped vector
     */
    public Vector2 clamp(double min, double max) {
        return new Vector2(
            Math.max(min, Math.min(max, x)),
            Math.max(min, Math.min(max, y))
        );
    }
    
    /**
     * Checks if vector is approximately zero
     * @param epsilon Tolerance value
     * @return True if both components are near zero
     */
    public boolean isZero(double epsilon) {
        return Math.abs(x) < epsilon && Math.abs(y) < epsilon;
    }
    
    /**
     * Checks if vector is exactly zero
     * @return True if both components are zero
     */
    public boolean isZero() {
        return x == 0.0 && y == 0.0;
    }
    
    // --- Factory Methods ---
    
    /**
     * Creates a zero vector
     * @return Zero vector
     */
    public static Vector2 zero() {
        return new Vector2(0.0, 0.0);
    }
    
    /**
     * Creates a unit vector in X direction
     * @return Unit X vector
     */
    public static Vector2 unitX() {
        return new Vector2(1.0, 0.0);
    }
    
    /**
     * Creates a unit vector in Y direction
     * @return Unit Y vector
     */
    public static Vector2 unitY() {
        return new Vector2(0.0, 1.0);
    }
    
    /**
     * Creates a vector with both components set to value
     * @param value Component value
     * @return New vector
     */
    public static Vector2 fill(double value) {
        return new Vector2(value, value);
    }
    
    // --- Object Overrides ---
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Vector2 other = (Vector2) obj;
        return Double.compare(other.x, x) == 0 && Double.compare(other.y, y) == 0;
    }
    
    @Override
    public int hashCode() {
        long xBits = Double.doubleToLongBits(x);
        long yBits = Double.doubleToLongBits(y);
        return (int)(xBits ^ (xBits >>> 32)) ^ (int)(yBits ^ (yBits >>> 32));
    }
    
    @Override
    public String toString() {
        return String.format("Vector2(%.3f, %.3f)", x, y);
    }
    
    // --- Additional Methods for Graphics ---
    
    /**
     * Rotates the vector by specified angle (in radians)
     * @param angle Angle in radians
     * @return New rotated vector
     */
    public Vector2 rotate(double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Vector2(
            x * cos - y * sin,
            x * sin + y * cos
        );
    }
    
    /**
     * Returns the perpendicular vector (90 degree rotation)
     * @return New perpendicular vector
     */
    public Vector2 perpendicular() {
        return new Vector2(-y, x);
    }
    
    /**
     * Returns the angle of the vector in radians
     * @return Angle in radians [-π, π]
     */
    public double angle() {
        return Math.atan2(y, x);
    }
    
    /**
     * Returns the angle between two vectors in radians
     * @param other Other vector
     * @return Angle between vectors in radians [0, π]
     */
    public double angleBetween(Vector2 other) {
        double dot = this.dot(other);
        double len1 = this.length();
        double len2 = other.length();
        
        if (len1 < 1e-10 || len2 < 1e-10) {
            return 0.0;
        }
        
        return Math.acos(dot / (len1 * len2));
    }
    
    /**
     * Projects this vector onto another vector
     * @param other Vector to project onto
     * @return Projection vector
     */
    public Vector2 project(Vector2 other) {
        double lenSq = other.lengthSquared();
        if (lenSq < 1e-10) {
            return Vector2.zero();
        }
        double scale = this.dot(other) / lenSq;
        return other.multiply(scale);
    }
    
    /**
     * Reflects this vector across a normal vector
     * @param normal Normal vector (should be unit length)
     * @return Reflected vector
     */
    public Vector2 reflect(Vector2 normal) {
        double dot = this.dot(normal);
        return this.subtract(normal.multiply(2.0 * dot));
    }
	
}
