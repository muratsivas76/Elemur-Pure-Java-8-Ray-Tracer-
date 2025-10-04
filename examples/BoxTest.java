import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.io.File;
import java.io.IOException;

// Custom classes
import net.elena.murat.shape.*;
import net.elena.murat.lovert.*;
import net.elena.murat.material.*;
import net.elena.murat.math.*;
import net.elena.murat.light.*;
import net.elena.murat.util.*;

final public class BoxTest extends Object {
  
  private BoxTest() {
    super();
  }
  
  public String toString() {
    return "BoxTest";
  }
  
  /**
   * Generates a scene with a Box, applies lighting, renders the image
   * using ray tracing, and saves it to a file.
   * The recursion depth can be specified via command-line argument.
   * Example: java -cp bin\\elenaRT.jar; BoxTest 5
   */
  final private static void generateSaveRenderedImage(String[] args) throws IOException {
    
    // --- 1. USAGE INFORMATION ---
    System.out.println("Usage: You can specify recursion depth as an argument.");
    System.out.println("Example: java -cp bin\\elenaRT.jar; BoxTest 5\n");
    
    // --- 2. CREATE THE SCENE ---
    Scene scene = new Scene();
    System.out.println("Scene created.");
    
    // --- 3. CONFIGURE THE RAY TRACER ---
    int imageWidth = 800;
    int imageHeight = 600;
    Color backgroundColor = new Color(0.10f, 0.10f, 0.99f); // Soft blue background
    
    ElenaMuratRayTracer rayTracer = new ElenaMuratRayTracer(scene, imageWidth, imageHeight, backgroundColor);
    System.out.println("Ray tracer initialized: " + imageWidth + "x" + imageHeight);
    
    // --- 4. SET CAMERA PARAMETERS ---
    // Position the camera to view the scene from a natural perspective
    rayTracer.setCameraPosition(new Point3(0, 2.0, 4.0));   // Above and behind the box
    rayTracer.setLookAt(new Point3(0, 1.0, -3.0));           // Looking at the center of the box
    rayTracer.setUpVector(new Vector3(0, 1, 0));              // Y-axis is up
    rayTracer.setFov(45.0);                                   // Natural field of view
    rayTracer.setOrthographic(false);                         // Perspective projection
    
    // --- 5. SET MAXIMUM RECURSION DEPTH ---
    int recursionDepth = 3; // Default value
    if (args.length > 0) {
      try {
        recursionDepth = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
        System.err.println("Invalid depth argument. Using default depth: 3");
      }
    }
    rayTracer.setMaxRecursionDepth(recursionDepth);
    System.out.println("Maximum recursion depth set to: " + recursionDepth);
    
    // --- 6. ADD LIGHT SOURCES ---
    // 6.1. Directional Light: Main soft overhead light
    scene.addLight(new ElenaDirectionalLight(
        new Vector3(-0.5, -1.0, -0.3).normalize(), // Light direction
        Color.WHITE,                               // White light
        0.8                                        // Moderate intensity
    ));
    
    // 6.2. Ambient Light: Global low-level illumination
    scene.addLight(new ElenaMuratAmbientLight(
        new Color(0.2f, 0.2f, 0.2f), // Dim gray
        0.4                          // Low contribution
    ));
    
    // 6.3. Point Light 1: Warm light from front-right (creates highlights)
    scene.addLight(new MuratPointLight(
        new Point3(3, 3, -2),           // Position: right, high, front
        new Color(1.0f, 0.9f, 0.8f),    // Warm white (slightly yellow)
        1.2                             // Slightly strong
    ));
    
    // 6.4. Point Light 2: Cool light from back-left (adds depth and color contrast)
    scene.addLight(new MuratPointLight(
        new Point3(-4, 2, -6),          // Position: left, medium height, back
        new Color(0.7f, 0.8f, 1.0f),    // Cool blue-white
        0.7                             // Soft intensity
    ));
    
    System.out.println("Lights added: 1 directional, 1 ambient, 2 point lights.");
    
    // --- 7. CREATE AND ADD THE FLOOR ---
    Plane floor = new Plane(new Point3(0, 0, 0), new Vector3(0, 1, 0));
    floor.setTransform(Matrix4.translate(0, -1.0, 0)); // Move floor down to Y = -1.0
    
    Material floorMaterial = new SquaredMaterial(
      new Color(0.0f, 0.0f, 0.0f),
      new Color(1.0f, 1.0f, 1.0f),
      2,
      0.1, 0.3, 0.4, 50.0, Color.RED,
      0.75, 1.0, 0.0,
      floor.getInverseTransform()
    );
    
    floor.setMaterial(floorMaterial);
    scene.addShape(floor);
    System.out.println("Checkerboard floor added at Y = -1.0.");
    
    // --- 8. CREATE AND ADD THE BOX ---
    // 8.1. Create a box with dimensions: width=2.0, height=3.0, depth=1.5
    EMShape box = new Box(2.0, 2.0, 1.5);
    System.out.println("Box created: 2.0 x 3.0 x 1.5");
    
    // 8.2. Define a red diffuse material with slight reflectivity
    Color boxColor = new Color(220, 50, 50); // Deep red
    Material boxMaterial = new DiffuseMaterial(
      boxColor,         // Base color
      0.8,              // Diffuse coefficient
      0.1,              // Slight reflectivity (for realism)
      1.0,              // Index of Refraction (air-like)
      0.0               // Not transparent
    );
    
    // 8.3. Assign the material to the box
    box.setMaterial(boxMaterial);
    
    // 8.4. Position the box: center at (0, 1.5, -3.0)
    // Y = 1.5 ensures the bottom of the box is at Y = 0 (sits on ground)
    // Z = -3.0 places it in front of the camera
    Matrix4 boxTransform = Matrix4.translate(0, 1.5, -3.0).rotateY (25);
    box.setTransform(boxTransform);
    System.out.println("Box positioned at (0, 1.5, -3.0).");
    
    // 8.5. Add the box to the scene
    scene.addShape(box);
    System.out.println("Box added to the scene.");
    
    // --- 9. START RENDERING ---
    System.out.println("\n=== RENDERING STARTED ===");
    System.out.println("Image size: " + imageWidth + "x" + imageHeight);
    System.out.println("Recursion Depth: " + rayTracer.getMaxRecursionDepth());
    System.out.println("Number of lights: " + scene.getLights().size());
    System.out.println("Number of shapes: " + scene.getShapes().size());
    
    long startTime = System.currentTimeMillis();
    BufferedImage renderedImage = rayTracer.render();
    long endTime = System.currentTimeMillis();
    
    System.out.println("Rendering completed in " + (endTime - startTime) + " ms.");
    
    // --- 10. SAVE THE IMAGE ---
    String filename = "..\\images\\box_depth" + recursionDepth + ".png";
    File outputFile = new File(filename);
    
    // Ensure the output directory exists
    if (!outputFile.getParentFile().exists()) {
      outputFile.getParentFile().mkdirs();
    }
    
    try {
      ImageIO.write(renderedImage, "png", outputFile);
      System.out.println("Image successfully saved: " + outputFile.getAbsolutePath());
      } catch (IOException e) {
      System.err.println("Failed to save image: " + e.getMessage());
      e.printStackTrace();
    }
    
    System.out.println("=== RENDERING PROCESS COMPLETED ===\n");
  }
  
  final public static void main(final String[] args) {
    try {
      generateSaveRenderedImage(args);
      } catch (IOException ioe) {
      ioe.printStackTrace();
      System.exit(-1);
    }
  }
  
}
