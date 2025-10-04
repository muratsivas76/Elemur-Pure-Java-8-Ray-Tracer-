package net.elena.murat.util;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public final class LetterUtils3D {
  private LetterUtils3D() {}
  
  // Cache mechanism with improved key generation
  private static final ConcurrentHashMap<String, LetterMesh> MESH_CACHE = new ConcurrentHashMap<>();
  
  public static BufferedImage getLetterImage(char c, Font font, double widthScale, double heightScale, int size) {
    final double baseSize = (double)(size);
    int width = (int)(baseSize * widthScale);
    int height = (int)(baseSize * heightScale);
    
    BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = img.createGraphics();
    
    try {
      // Setup graphics
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g.setBackground(new Color(0, 0, 0, 0)); // Transparent background
      g.clearRect(0, 0, width, height);
      g.setColor(Color.BLACK);
      g.setFont(font);
      
      // Center the letter properly with correct orientation
      FontMetrics fm = g.getFontMetrics();
      int xPos = (width - fm.charWidth(c)) / 2;
      int yPos = (height - fm.getHeight()) / 2 + fm.getAscent();
      
      // Flip Y-axis to match 3D coordinate system
      // g.translate(0, height);
      // g.scale(1, -1);
      
      g.drawString(String.valueOf(c), xPos, yPos);
      } finally {
      g.dispose();
    }
    
    return img;
  }
  
  public static boolean[][] getLetterPixelData(BufferedImage img) {
    int width = img.getWidth();
    int height = img.getHeight();
    boolean[][] pixels = new boolean[width][height];
    int[] rgb = new int[width * height];
    
    img.getRGB(0, 0, width, height, rgb, 0, width);
    
    final int rgblen=rgb.length;
    
    // Read pixels with proper Y orientation (flipped vertically)
    for (int i = 0; i < rgblen; i++) {
      int x = i % width;
      int y = height - 1 - (i / width); // Flip Y coordinate
      pixels[x][y] = (rgb[i] & 0xFF000000) != 0; // Check alpha channel
    }
    
    return pixels;
  }
  
  public static LetterMesh getLetterMeshData(boolean[][] pixels, double thickness) {
    int width = pixels.length;
    int height = pixels[0].length;
    
    String cacheKey = width + "x" + height + "t" + thickness;
    return MESH_CACHE.computeIfAbsent(cacheKey, k -> {
        List<Vertex> vertices = new ArrayList<>();
        List<Face> faces = new ArrayList<>();
        double halfThickness = thickness / 2;
        double scaleX = 1.0 / width;
        double scaleY = 1.0 / height;
        
        for (int x = 0; x < width; x++) {
          for (int y = 0; y < height; y++) {
            if (pixels[x][y]) {
              addOptimizedVoxel(vertices, faces, x, y, halfThickness, scaleX, scaleY);
            }
          }
        }
        
        return new LetterMesh(vertices, faces);
    });
  }
  
  private static void addOptimizedVoxel(List<Vertex> vertices, List<Face> faces,
    int x, int y, double halfThick, double scaleX, double scaleY) {
    double px = x * scaleX;
    double py = y * scaleY;
    int baseIndex = vertices.size();
    
    // Create 8 vertices for the voxel
    vertices.add(new Vertex(px,        py,        -halfThick)); // 0
    vertices.add(new Vertex(px + scaleX, py,        -halfThick)); // 1
    vertices.add(new Vertex(px + scaleX, py + scaleY, -halfThick)); // 2
    vertices.add(new Vertex(px,        py + scaleY, -halfThick)); // 3
    vertices.add(new Vertex(px,        py,         halfThick)); // 4
    vertices.add(new Vertex(px + scaleX, py,         halfThick)); // 5
    vertices.add(new Vertex(px + scaleX, py + scaleY,  halfThick)); // 6
    vertices.add(new Vertex(px,        py + scaleY,  halfThick)); // 7
    
    // Create 12 triangular faces (2 per cube face)
    int[] faceIndices = {
      // Front face
      baseIndex, baseIndex+1, baseIndex+2,
      baseIndex, baseIndex+2, baseIndex+3,
      // Back face
      baseIndex+4, baseIndex+6, baseIndex+5,
      baseIndex+4, baseIndex+7, baseIndex+6,
      // Top face
      baseIndex, baseIndex+4, baseIndex+5,
      baseIndex, baseIndex+5, baseIndex+1,
      // Bottom face
      baseIndex+3, baseIndex+2, baseIndex+6,
      baseIndex+3, baseIndex+6, baseIndex+7,
      // Right face
      baseIndex+1, baseIndex+5, baseIndex+6,
      baseIndex+1, baseIndex+6, baseIndex+2,
      // Left face
      baseIndex, baseIndex+3, baseIndex+7,
      baseIndex, baseIndex+7, baseIndex+4
    };
    
    for (int i = 0; i < faceIndices.length; i += 3) {
      faces.add(new Face(
          faceIndices[i],
          faceIndices[i+1],
          faceIndices[i+2]
      ));
    }
  }
  
  // Immutable vertex class
  public static final class Vertex {
    public final double x, y, z;
    public Vertex(double x, double y, double z) {
      this.x = x;
      this.y = y;
      this.z = z;
    }
  }
  
  // Immutable face class
  public static final class Face {
    public final int v1, v2, v3;
    public Face(int v1, int v2, int v3) {
      this.v1 = v1;
      this.v2 = v2;
      this.v3 = v3;
    }
  }
  
  // Immutable mesh container
  public static final class LetterMesh {
    public final List<Vertex> vertices;
    public final List<Face> faces;
    
    public LetterMesh(List<Vertex> vertices, List<Face> faces) {
      this.vertices = new ArrayList<>(vertices); // Java 6/8 uyumlu defensive copy
      this.faces = new ArrayList<>(faces);       // Java 6/8 uyumlu defensive copy
    }
  }
  
}
