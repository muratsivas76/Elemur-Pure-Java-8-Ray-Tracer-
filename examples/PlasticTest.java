import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.io.File;
import java.io.IOException;

// Custom classes
import net.elena.murat.shape.*;
import net.elena.murat.lovert.*;
import net.elena.murat.material.*;
import net.elena.murat.material.pbr.*;
import net.elena.murat.math.*;
import net.elena.murat.light.*;
import net.elena.murat.util.*;

final public class PlasticTest extends Object {
  
  private PlasticTest() {
    super();
  }
  
  public String toString() {
    return "PlasticTest";
  }
  
  /**
   * Generates a scene with a Box, applies lighting, renders the image
   * using ray tracing, and saves it to a file.
   * The recursion depth can be specified via command-line argument.
   * Example: java -cp bin\\elenaRT.jar; PlasticTest 5
   */
  final private static void generateSaveRenderedImage(String[] args) throws IOException {
    
    // --- 1. USAGE INFORMATION ---
    System.out.println("Usage: You can specify recursion depth as an argument.");
    System.out.println("Example: java -cp bin\\elenaRT.jar; PlasticTest 5\n");
    
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
    Camera camera=new Camera ();
    
    camera.setCameraPosition(new Point3(5, 5, 5));   // Above and behind the box
    camera.setLookAt(new Point3(0, 0, 0));           // Looking at the center of the box
    camera.setUpVector(new Vector3(0, 1, 0));              // Y-axis is up
    camera.setFov(45.0);                                   // Natural field of view
    camera.setOrthographic(false);                         // Perspective projection
    
    int recursionDepth = 3; // Default value
    if (args.length > 0) {
      try {
        recursionDepth = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
        System.err.println("Invalid depth argument. Using default depth: 3");
      }
    }
    camera.setMaxRecursionDepth(recursionDepth);
    System.out.println("Maximum recursion depth set to: " + recursionDepth);
    
    rayTracer.setCamera (camera);
    
    // --- 5. ADD LIGHT SOURCES ---
    // 5.1. Directional Light: Main soft overhead light
    scene.addLight(new ElenaDirectionalLight(
        new Vector3(-0.5, -1.0, -0.3).normalize(), // Light direction
        Color.WHITE,                               // White light
        0.8                                        // Moderate intensity
    ));
    
    // 5.2. Ambient Light: Global low-level illumination
    scene.addLight(new ElenaMuratAmbientLight(
        new Color(0.2f, 0.2f, 0.2f), // Dim gray
        0.4                          // Low contribution
    ));
    
    // 5.3. Point Light 1: Warm light from front-right (creates highlights)
    scene.addLight(new MuratPointLight(
        new Point3(3, 3, -2),           // Position: right, high, front
        new Color(1.0f, 0.9f, 0.8f),    // Warm white (slightly yellow)
        1.2                             // Slightly strong
    ));
    
    // 5.4. Point Light 2: Cool light from back-left (adds depth and color contrast)
    scene.addLight(new MuratPointLight(
        new Point3(-4, 2, -6),          // Position: left, medium height, back
        new Color(0.7f, 0.8f, 1.0f),    // Cool blue-white
        0.7                             // Soft intensity
    ));
    
    System.out.println("Lights added: 1 directional, 1 ambient, 2 point lights.");
    
    // --- 6. CREATE AND ADD THE FLOOR ---
    // 1. Identity transform
    Matrix4 identity = new Matrix4();
    
    // 2. DiagonalCheckerMaterial
    Material diagonalCheckerMaterial = new DiagonalCheckerMaterial(
      new Color(30, 30, 30),
      new Color(220, 220, 230),
      1.2, //
      0.1, 0.7, 0.8, 50.0, new Color(200, 220, 255),
      0.1, 1.0, 0.0,
      identity
    );
    
    // 3. Zemine ata
    EMShape ground = new Plane(new Point3(0, 0, 0), new Vector3(0, 1, 0));
    ground.setMaterial(diagonalCheckerMaterial);
    scene.addShape(ground);
    
    // --- 8. CREATE AND ADD THE Dielectric objects ---
    // PLASTiK KÜP (PBR)
    EMShape plasticBox = new Box(1.5, 1.5, 1.5);
    Material plasticMaterial = new PlasticPBRMaterial(
      new Color(50, 150, 220), // color
      0.4,                     // roughness
      0.04,                    // Dielectric reflection
      1.5,                     // IOR
      0.0                      // Opaque
    );
    
    Matrix4 plasticTransform = Matrix4.translate(0, 2.5, -4);
    plasticTransform=plasticTransform.multiply (plasticTransform.rotateY (30));
    plasticTransform=plasticTransform.multiply (plasticTransform.rotateZ (30));
    plasticBox.setTransform(plasticTransform);
    plasticBox.setMaterial(plasticMaterial);
    scene.addShape(plasticBox);
    System.out.println("PBR Plastik küp eklendi.");
    
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
    String filename = "..\\images\\plastic_depth" + recursionDepth + ".png";
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
