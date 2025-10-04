package net.elena.murat.math;

import net.elena.murat.math.Ray;

/**
 * Represents a 4x4 matrix for 3D transformations (translation, rotation, scaling).
 */
public class Matrix4 {
  private final double[][] m; // Matrix elements
  
  /**
   * Constructs an identity Matrix4.
   */
  public Matrix4() {
    m = new double[4][4];
    m[0][0] = 1.0; m[0][1] = 0.0; m[0][2] = 0.0; m[0][3] = 0.0;
    m[1][0] = 0.0; m[1][1] = 1.0; m[1][2] = 0.0; m[1][3] = 0.0;
    m[2][0] = 0.0; m[2][1] = 0.0; m[2][2] = 1.0; m[2][3] = 0.0;
    m[3][0] = 0.0; m[3][1] = 0.0; m[3][2] = 0.0; m[3][3] = 1.0;
  }
  
  /**
   * Constructs a Matrix4 with the specified elements.
   */
  public Matrix4(double m00, double m01, double m02, double m03,
    double m10, double m11, double m12, double m13,
    double m20, double m21, double m22, double m23,
    double m30, double m31, double m32, double m33) {
    m = new double[4][4];
    this.m[0][0] = m00; this.m[0][1] = m01; this.m[0][2] = m02; this.m[0][3] = m03;
    this.m[1][0] = m10; this.m[1][1] = m11; this.m[1][2] = m12; this.m[1][3] = m13;
    this.m[2][0] = m20; this.m[2][1] = m21; this.m[2][2] = m22; this.m[2][3] = m23;
    this.m[3][0] = m30; this.m[3][1] = m31; this.m[3][2] = m32; this.m[3][3] = m33;
  }
  
  /**
   * Constructs a new Matrix4 by copying an existing matrix.
   * @param other The Matrix4 object to copy.
   */
  public Matrix4(Matrix4 other) {
    this(other.m[0][0], other.m[0][1], other.m[0][2], other.m[0][3],
      other.m[1][0], other.m[1][1], other.m[1][2], other.m[1][3],
      other.m[2][0], other.m[2][1], other.m[2][2], other.m[2][3],
    other.m[3][0], other.m[3][1], other.m[3][2], other.m[3][3]);
  }
  
  /**
   * Returns an identity (unit) 4x4 matrix.
   * An identity matrix has 1s on the main diagonal and 0s elsewhere.
   * It represents no translation, rotation, or scaling.
   *
   * @return A new 4x4 identity matrix.
   */
  public static Matrix4 identity() {
    return new Matrix4(); // The default constructor creates an identity matrix
  }
  
  /**
   * Sets the value at the specified row and column.
   * @param row The row index (0-3)
   * @param col The column index (0-3)
   * @param value The value to set
   * @throws IndexOutOfBoundsException if row or col is not in [0, 3]
   */
  public void set(int row, int col, double value) {
    if (row < 0 || row >= 4 || col < 0 || col >= 4) {
        throw new IndexOutOfBoundsException("Matrix4 indices out of bounds: [" + row + "][" + col + "]");
    }
	
    this.m[row][col] = value;
  }
  
  /**
   * Gets the X-axis scale factor from this transformation matrix.
   * This is calculated as the magnitude of the X basis vector.
   * @return The X scale factor
   */
  public double getScaleX() {
    return Math.sqrt(m[0][0] * m[0][0] + m[1][0] * m[1][0] + m[2][0] * m[2][0]);
  }
  
  /**
   * Gets the Y-axis scale factor from this transformation matrix.
   * This is calculated as the magnitude of the Y basis vector.
   * @return The Y scale factor
   */
  public double getScaleY() {
    return Math.sqrt(m[0][1] * m[0][1] + m[1][1] * m[1][1] + m[2][1] * m[2][1]);
  }
  
  /**
   * Gets the Z-axis scale factor from this transformation matrix.
   * This is calculated as the magnitude of the Z basis vector.
   * @return The Z scale factor
   */
  public double getScaleZ() {
    return Math.sqrt(m[0][2] * m[0][2] + m[1][2] * m[1][2] + m[2][2] * m[2][2]);
  }
  
  public Ray transformRay(Ray ray) {
    Point3 newOrigin = this.transformPoint(ray.getOrigin());
    Vector3 newDirection = this.transformVector(ray.getDirection()).normalize();
    return new Ray(newOrigin, newDirection);
  }

  /**
   * Transforms a direction vector by this matrix.
   * Unlike points, vectors are not affected by translation.
   * Only the rotational and scaling components are applied.
   *
   * This is used for transforming normal vectors, ray directions, etc.
   *
   * @param v The direction vector to transform
   * @return A new transformed Vector3
   */
  public Vector3 transformDirection(Vector3 v) {
    double x = m[0][0] * v.x + m[0][1] * v.y + m[0][2] * v.z;
    double y = m[1][0] * v.x + m[1][1] * v.y + m[1][2] * v.z;
    double z = m[2][0] * v.x + m[2][1] * v.y + m[2][2] * v.z;
    return new Vector3(x, y, z);
  }

  /**
   * Provides access to a specific element of the matrix.
   * @param row The row index (0-3).
   * @param col The column index (0-3).
   * @return The matrix element at the specified position.
   * @throws IndexOutOfBoundsException If the row or column index is invalid.
   */
  public double get(int row, int col) {
    if (row < 0 || row >= 4 || col < 0 || col >= 4) {
      throw new IndexOutOfBoundsException("Matrix4 indices out of bounds: [" + row + "][" + col + "]");
    }
    return m[row][col];
  }
  
  /**
   * Normal vektörü dönüştürür (normal transformasyonu için).
   * Normal vektörlerin doğru dönüşümü için matrisin ters transpozu kullanılır.
   * @param normal Dönüştürülecek normal vektör
   * @return Dönüştürülmüş normal vektör (normalize edilmiş)
   */
  public Vector3 transformNormal(Vector3 normal) {
    // Matrisin ters transpozu alınır
    Matrix4 normalMatrix = this.inverseTransposeForNormal();
    
    if (normalMatrix == null) {
      return new Vector3(0, 0, 0); // Geçersiz dönüşüm durumu
    }
    
    // Vektörü dönüştür (w=0 varsayarak, sadece 3x3 kısım kullanılır)
    double x = normal.x;
    double y = normal.y;
    double z = normal.z;
    
    double newX = normalMatrix.m[0][0] * x + normalMatrix.m[0][1] * y + normalMatrix.m[0][2] * z;
    double newY = normalMatrix.m[1][0] * x + normalMatrix.m[1][1] * y + normalMatrix.m[1][2] * z;
    double newZ = normalMatrix.m[2][0] * x + normalMatrix.m[2][1] * y + normalMatrix.m[2][2] * z;
    
    return new Vector3(newX, newY, newZ).normalize();
  }
  
  /**
   * Multiplies this matrix by another matrix.
   * @param other The other Matrix4 to multiply with.
   * @return The resulting Matrix4.
   */
  public Matrix4 multiply(Matrix4 other) {
    Matrix4 result = new Matrix4(); // Start with an identity matrix
    for (int i = 0; i < 4; i++) {
      for (int j = 0; j < 4; j++) {
        result.m[i][j] = 0; // Reset element before summing
        for (int k = 0; k < 4; k++) {
          result.m[i][j] += this.m[i][k] * other.m[k][j];
        }
      }
    }
    return result;
  }
  
  /**
   * Transforms a Point3 by this matrix (includes translation).
   * For affine transformations, the W component of the transformed point should be 1.0.
   * @param point The Point3 to transform.
   * @return The transformed Point3.
   */
  public Point3 transformPoint(Point3 point) {
    double x = m[0][0] * point.x + m[0][1] * point.y + m[0][2] * point.z + m[0][3];
    double y = m[1][0] * point.x + m[1][1] * point.y + m[1][2] * point.z + m[1][3];
    double z = m[2][0] * point.x + m[2][1] * point.y + m[2][2] * point.z + m[2][3];
    return new Point3(x, y, z);
  }
  
  /**
   * Transforms a Vector3 by this matrix (only rotation and scaling, no translation).
   * @param vector The Vector3 to transform.
   * @return The transformed Vector3.
   */
  public Vector3 transformVector(Vector3 vector) {
    double x = m[0][0] * vector.x + m[0][1] * vector.y + m[0][2] * vector.z;
    double y = m[1][0] * vector.x + m[1][1] * vector.y + m[1][2] * vector.z;
    double z = m[2][0] * vector.x + m[2][1] * vector.y + m[2][2] * vector.z;
    return new Vector3(x, y, z);
  }
  
  /**
   * Returns the inverse of this matrix. Returns null if the matrix is non-invertible.
   * This method is designed for affine transformations (rotation, translation, uniform scaling).
   * Formula: [ R | t ]^-1 = [ R^-1 | -R^-1 * t ]
   * Where R is the upper-left 3x3 submatrix and t is the translation vector.
   * @return The inverse Matrix4 or null.
   */
  public Matrix4 inverse() {
    // Extract the upper 3x3 rotation/scale part
    Matrix3 upperLeft = new Matrix3(
      m[0][0], m[0][1], m[0][2],
      m[1][0], m[1][1], m[1][2],
      m[2][0], m[2][1], m[2][2]
    );
    Matrix3 invUpperLeft = upperLeft.inverse(); // This performs its own determinant check
    
    if (invUpperLeft == null) {
      System.err.println("Warning: Upper 3x3 part of Matrix4 is non-invertible, cannot compute inverse.");
      return null;
    }
    
    Matrix4 inv = new Matrix4(); // Resulting inverse matrix, initialized to identity
    
    // Set the upper-left 3x3 of the inverse matrix (R^-1)
    inv.m[0][0] = invUpperLeft.get(0,0); inv.m[0][1] = invUpperLeft.get(0,1); inv.m[0][2] = invUpperLeft.get(0,2);
    inv.m[1][0] = invUpperLeft.get(1,0); inv.m[1][1] = invUpperLeft.get(1,1); inv.m[1][2] = invUpperLeft.get(1,2);
    inv.m[2][0] = invUpperLeft.get(2,0); inv.m[2][1] = invUpperLeft.get(2,1); inv.m[2][2] = invUpperLeft.get(2,2);
    
    // Calculate the inverse translation part: -R^-1 * t
    Vector3 translation = new Vector3(m[0][3], m[1][3], m[2][3]);
    Vector3 invTranslation = invUpperLeft.transform(translation).negate();
    
    inv.m[0][3] = invTranslation.x;
    inv.m[1][3] = invTranslation.y;
    inv.m[2][3] = invTranslation.z;
    
    // Bottom row remains [0, 0, 0, 1] for affine transformations
    inv.m[3][0] = 0.0; inv.m[3][1] = 0.0; inv.m[3][2] = 0.0; inv.m[3][3] = 1.0;
    
    return inv;
  }
  
  /**
   * Computes the inverse transpose of the upper 3x3 part of this matrix.
   * This is typically used to transform normal vectors correctly when the
   * model matrix contains non-uniform scaling.
   * For pure rotations, the inverse is equal to the transpose.
   *
   * @return A new Matrix4 representing the inverse transpose of the 3x3 part,
   * with the translation components set to zero. Returns null if the
   * upper 3x3 part is non-invertible.
   */
  public Matrix4 inverseTransposeForNormal() {
    // Extract the upper 3x3 part
    Matrix3 upperLeft = new Matrix3(
      m[0][0], m[0][1], m[0][2],
      m[1][0], m[1][1], m[1][2],
      m[2][0], m[2][1], m[2][2]
    );
    
    // Compute its inverse
    Matrix3 invUpperLeft = upperLeft.inverse();
    
    if (invUpperLeft == null) {
      System.err.println("Warning: Upper 3x3 part of Matrix4 is non-invertible, cannot compute inverse transpose for normal.");
      return null;
    }
    
    // Transpose the inverse (this is the correct operation for normals)
    Matrix3 normalMatrix3 = invUpperLeft.transpose();
    
    // Construct a new Matrix4 from this 3x3, with translation part zeroed out
    return new Matrix4(
      normalMatrix3.get(0,0), normalMatrix3.get(0,1), normalMatrix3.get(0,2), 0,
      normalMatrix3.get(1,0), normalMatrix3.get(1,1), normalMatrix3.get(1,2), 0,
      normalMatrix3.get(2,0), normalMatrix3.get(2,1), normalMatrix3.get(2,2), 0,
      0, 0, 0, 1
    );
  }
  
  /**
   * Creates a translation matrix.
   * @param translation The translation vector.
   * @return The translation Matrix4.
   */
  public static Matrix4 translate(Vector3 translation) {
    return new Matrix4(
      1, 0, 0, translation.x,
      0, 1, 0, translation.y,
      0, 0, 1, translation.z,
      0, 0, 0, 1
    );
  }
  
  public static Matrix4 translate(double x, double y, double z) {
    return new Matrix4(
      1, 0, 0, x,
      0, 1, 0, y,
      0, 0, 1, z,
      0, 0, 0, 1
    );
  }
  
  /**
   * Creates a rotation matrix around the X-axis.
   * @param angleDegrees The rotation angle in degrees.
   * @return The rotation Matrix4.
   */
  public static Matrix4 rotateX(double angleDegrees) {
    double angleRad = Math.toRadians(angleDegrees);
    double cosA = Math.cos(angleRad);
    double sinA = Math.sin(angleRad);
    return new Matrix4(
      1,    0,     0, 0,
      0,  cosA, -sinA, 0,
      0,  sinA,  cosA, 0,
      0,    0,     0, 1
    );
  }
  
  /**
   * Creates a rotation matrix around the Y-axis.
   * @param angleDegrees The rotation angle in degrees.
   * @return The rotation Matrix4.
   */
  public static Matrix4 rotateY(double angleDegrees) {
    double angleRad = Math.toRadians(angleDegrees);
    double cosA = Math.cos(angleRad);
    double sinA = Math.sin(angleRad);
    return new Matrix4(
      cosA,  0, sinA, 0,
      0,     1,    0, 0,
      -sinA, 0, cosA, 0,
      0,     0,    0, 1
    );
  }
  
  /**
   * Creates a rotation matrix around the Z-axis.
   * @param angleDegrees The rotation angle in degrees.
   * @return The rotation Matrix4.
   */
  public static Matrix4 rotateZ(double angleDegrees) {
    double angleRad = Math.toRadians(angleDegrees);
    double cosA = Math.cos(angleRad);
    double sinA = Math.sin(angleRad);
    return new Matrix4(
      cosA, -sinA, 0, 0,
      sinA,  cosA, 0, 0,
      0,     0,    1, 0,
      0,     0,    0, 1
    );
  }
  
  /**
   * Creates a scaling matrix with the specified scale factors.
   * @param sx The X-axis scale factor.
   * @param sy The Y-axis scale factor.
   * @param sz The Z-axis scale factor.
   * @return The scaling Matrix4.
   */
  public static Matrix4 scale(double sx, double sy, double sz) {
    return new Matrix4(
      sx, 0,  0, 0,
      0, sy,  0, 0,
      0,  0, sz, 0,
      0,  0,  0, 1
    );
  }
  
  // Matrix4 sınıfına bu metodu ekleyin
  public Matrix4 transpose() {
    return new Matrix4(
      m[0][0], m[1][0], m[2][0], m[3][0],
      m[0][1], m[1][1], m[2][1], m[3][1],
      m[0][2], m[1][2], m[2][2], m[3][2],
      m[0][3], m[1][3], m[2][3], m[3][3]
    );
  }
  
  /**
   * Creates a Matrix4 from a Matrix3 (typically to extend rotation matrices to 4x4).
   * @param m3 The Matrix3 to extend.
   * @return The created Matrix4.
   */
  public static Matrix4 fromMatrix3(Matrix3 m3) {
    return new Matrix4(
      m3.get(0,0), m3.get(0,1), m3.get(0,2), 0,
      m3.get(1,0), m3.get(1,1), m3.get(1,2), 0,
      m3.get(2,0), m3.get(2,1), m3.get(2,2), 0,
      0, 0, 0, 1
    );
  }
  
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 4; i++) {
      sb.append("| ");
      for (int j = 0; j < 4; j++) {
        sb.append(String.format("%8.4f", m[i][j])).append(" ");
      }
      sb.append("|\n");
    }
    return sb.toString();
  }
  
}
