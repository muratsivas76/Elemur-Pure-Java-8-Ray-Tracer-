package net.elena.murat.math;

import net.elena.murat.math.Ray;

/**
 * Represents a 3x3 matrix for linear transformations in 3D space.
 */
public class Matrix3 {
  private final double[][] m;
  
  /**
   * Constructs an identity Matrix3.
   */
  public Matrix3() {
    m = new double[3][3];
    m[0][0] = 1.0; m[0][1] = 0.0; m[0][2] = 0.0;
    m[1][0] = 0.0; m[1][1] = 1.0; m[1][2] = 0.0;
    m[2][0] = 0.0; m[2][1] = 0.0; m[2][2] = 1.0;
  }
  
  /**
   * Constructs a Matrix3 with the specified elements.
   */
  public Matrix3(double m00, double m01, double m02,
    double m10, double m11, double m12,
    double m20, double m21, double m22) {
    m = new double[3][3];
    this.m[0][0] = m00; this.m[0][1] = m01; this.m[0][2] = m02;
    this.m[1][0] = m10; this.m[1][1] = m11; this.m[1][2] = m12;
    this.m[2][0] = m20; this.m[2][1] = m21; this.m[2][2] = m22;
  }
  
  /**
   * Constructs a new Matrix3 by copying an existing matrix.
   * @param other The Matrix3 object to copy.
   */
  public Matrix3(Matrix3 other) {
    this(other.m[0][0], other.m[0][1], other.m[0][2],
      other.m[1][0], other.m[1][1], other.m[1][2],
    other.m[2][0], other.m[2][1], other.m[2][2]);
  }
  
  /**
   * Provides access to a specific element of the matrix.
   * @param row The row index (0-2).
   * @param col The column index (0-2).
   * @return The matrix element at the specified position.
   * @throws IndexOutOfBoundsException If the row or column index is invalid.
   */
  public double get(int row, int col) {
    if (row < 0 || row >= 3 || col < 0 || col >= 3) {
      throw new IndexOutOfBoundsException("Matrix3 indices out of bounds: [" + row + "][" + col + "]");
    }
    return m[row][col];
  }
  
  /**
   * Multiplies this matrix by another matrix.
   * @param other The other Matrix3 to multiply with.
   * @return The resulting Matrix3.
   */
  public Matrix3 multiply(Matrix3 other) {
    Matrix3 result = new Matrix3();
    for (int i = 0; i < 3; i++) {
      for (int j = 0; j < 3; j++) {
        result.m[i][j] = 0;
        for (int k = 0; k < 3; k++) {
          result.m[i][j] += this.m[i][k] * other.m[k][j];
        }
      }
    }
    return result;
  }
  
  /**
   * Transforms a Vector3 by this matrix.
   * @param vector The Vector3 to transform.
   * @return The transformed Vector3.
   */
  public Vector3 transform(Vector3 vector) {
    double x = m[0][0] * vector.x + m[0][1] * vector.y + m[0][2] * vector.z;
    double y = m[1][0] * vector.x + m[1][1] * vector.y + m[1][2] * vector.z;
    double z = m[2][0] * vector.x + m[2][1] * vector.y + m[2][2] * vector.z;
    return new Vector3(x, y, z);
  }
  
  /**
   * Computes the determinant of this matrix.
   * @return The determinant value.
   */
  public double determinant() {
    return m[0][0] * (m[1][1] * m[2][2] - m[1][2] * m[2][1]) -
    m[0][1] * (m[1][0] * m[2][2] - m[1][2] * m[2][0]) +
    m[0][2] * (m[1][0] * m[2][1] - m[1][1] * m[2][0]);
  }
  
  /**
   * Computes the inverse of this matrix.
   * @return The inverse Matrix3, or null if the matrix is singular (non-invertible).
   */
  public Matrix3 inverse() {
    double det = determinant();
    if (Math.abs(det) < Ray.EPSILON) { // Using Ray.EPSILON for floating point comparison
      // System.err.println("Warning: Matrix3 is singular, cannot compute inverse.");
      return null;
    }
    
    double invDet = 1.0 / det;
    
    Matrix3 inv = new Matrix3();
    inv.m[0][0] = (m[1][1] * m[2][2] - m[1][2] * m[2][1]) * invDet;
    inv.m[0][1] = (m[0][2] * m[2][1] - m[0][1] * m[2][2]) * invDet;
    inv.m[0][2] = (m[0][1] * m[1][2] - m[0][2] * m[1][1]) * invDet;
    
    inv.m[1][0] = (m[1][2] * m[2][0] - m[1][0] * m[2][2]) * invDet;
    inv.m[1][1] = (m[0][0] * m[2][2] - m[0][2] * m[2][0]) * invDet;
    inv.m[1][2] = (m[0][2] * m[1][0] - m[0][0] * m[1][2]) * invDet;
    
    inv.m[2][0] = (m[1][0] * m[2][1] - m[1][1] * m[2][0]) * invDet;
    inv.m[2][1] = (m[0][1] * m[2][0] - m[0][0] * m[2][1]) * invDet;
    inv.m[2][2] = (m[0][0] * m[1][1] - m[0][1] * m[1][0]) * invDet;
    
    return inv;
  }
  
  /**
   * Computes the transpose of this matrix.
   * @return The transposed Matrix3.
   */
  public Matrix3 transpose() {
    Matrix3 result = new Matrix3();
    for (int i = 0; i < 3; i++) {
      for (int j = 0; j < 3; j++) {
        result.m[i][j] = this.m[j][i];
      }
    }
    return result;
  }
  
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 3; i++) {
      sb.append("| ");
      for (int j = 0; j < 3; j++) {
        sb.append(String.format("%8.4f", m[i][j])).append(" ");
      }
      sb.append("|\n");
    }
    return sb.toString();
  }
  
}
