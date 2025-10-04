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

final public class GoldTest extends Object {
  
  private GoldTest() {
    super();
  }
  
  public String toString() {
    return "GoldTest";
  }
  
  /**
   * Generates a scene with a Box, applies lighting, renders the image
   * using ray tracing, and saves it to a file.
   * The recursion depth can be specified via command-line argument.
   * Example: java -cp bin\\elenaRT.jar; GoldTest 5
   */
  final private static void generateSaveRenderedImage(String[] args) throws IOException {
    
    // --- 1. USAGE INFORMATION ---
    System.out.println("Usage: You can specify recursion depth as an argument.");
    System.out.println("Example: java -cp bin\\elenaRT.jar; GoldTest number[1-5]\n");
    
    // --- 2. CREATE THE SCENE ---
    Scene scene = new Scene();
    System.out.println("Scene created.");
    
    // --- 3. CONFIGURE THE RAY TRACER ---
    int imageWidth = 800;
    int imageHeight = 600;
    Color backgroundColor = new Color(1f, 1f, 1f);
    
    ElenaMuratRayTracer rayTracer = new ElenaMuratRayTracer(scene, imageWidth, imageHeight, backgroundColor);
    System.out.println("Ray tracer initialized: " + imageWidth + "x" + imageHeight);
    
    // --- 4. OPTIMIZED CAMERA SETTINGS ---
    Camera camera = new Camera();
    camera.setCameraPosition(new Point3(0, 0, 5));  // Slightly elevated for better view
    camera.setLookAt(new Point3(0, 0, 0));         // Looking slightly downward
    camera.setUpVector(new Vector3(0, 1, 0));
    camera.setFov(60.0);  // Narrower FOV for less distortion
    camera.setOrthographic(false);
    
    camera.setReflective(true);  // Reflection on/off
    camera.setRefractive(true);  // Refraction on/off
    camera.setShadowsEnabled(true);  // Shadows on/off
    
    int recursionDepth = 3; // Default value
    if (args.length > 0) {
      try {
        recursionDepth = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
        System.err.println("Invalid depth argument. Using default depth: 5");
      }
    }
    camera.setMaxRecursionDepth(recursionDepth);
    rayTracer.setCamera(camera);
    
    // --- 5. OPTIMIZED LIGHTING SETUP ---
    ElenaMuratAmbientLight ambientLight = new ElenaMuratAmbientLight(Color.WHITE, 0.5);
    MuratPointLight pointLight = new MuratPointLight(new Point3(-1, 1, 2), Color.WHITE, 1.0);
    ElenaDirectionalLight dirLight = new ElenaDirectionalLight(new Vector3(0,-1,0), Color.WHITE, 1.5);
    
    scene.addLight(ambientLight);
    scene.addLight(pointLight);
    scene.addLight(dirLight);
    
    // --- 6. FLOOR MATERIAL ---
    EMShape ground = new Plane(new Point3(0, 0, 0), new Vector3(0, 1, 0));
    ground.setTransform(Matrix4.translate(new Vector3(0, -1.70, 0)));
    //Matrix4 rightWallTransform = Matrix4.translate(new Vector3(5, 0, 0));
    //ground.setTransform(rightWallTransform);
    Material floorMaterial = new CheckerboardMaterial(
      // Colors (compatible with gold reflections)
      new Color(30, 30, 30),    // Dark gray (for reflection contrast)
      new Color(80, 80, 80),    // Medium gray (complements gold color)
      
      // Scale and light settings
      0.4,                      // Larger squares (appropriate for gold size)
      0.25,                     // Ambient (for dark areas)
      0.5,                      // Diffuse (color saturation)
      
      // Reflection optimizations
      0.3,                      // Specular coefficient (soft reflection)
      8.0,                      // Shininess (slightly scattered reflection)
      new Color(255, 230, 180), // Warm specular color (complements gold)
      
      // PBR properties
      0.15,                     // Low reflectivity (avoid unnecessary reflections)
      1.0,                      // IOR (opaque floor)
      0.0,                      // Transparency (opaque)
      
      // Transform
      ground.getInverseTransform()
    );
    
    ground.setMaterial(floorMaterial);
    scene.addShape(ground);
    
    // --- 7. Gold SPHERE (OPTIMIZED) ---
    EMShape sphere = new Sphere(1.0);
    
    sphere.setTransform(Matrix4.translate(0, 1.2, 0));
    
    Material goldMaterial = new GoldPBRMaterial(
      new Color(245, 244, 25), // Color
      0.2,                     // Slightly rough
      0.9                      // High metallic
    );
    
    sphere.setMaterial(goldMaterial);
    
    scene.addShape(sphere);
    
    // --- 8. START RENDERING ---
    System.out.println("\n=== RENDERING STARTED ===");
    System.out.println("Image size: " + imageWidth + "x" + imageHeight);
    System.out.println("Recursion Depth: " + camera.getMaxRecursionDepth());
    System.out.println("Number of lights: " + scene.getLights().size());
    System.out.println("Number of shapes: " + scene.getShapes().size());
    
    long startTime = System.currentTimeMillis();
    BufferedImage renderedImage = rayTracer.render();
    long endTime = System.currentTimeMillis();
    
    System.out.println("Rendering completed in " + (endTime - startTime) + " ms.");
    
    // --- 9. SAVE THE IMAGE ---
    String filename = "..\\images\\goldSphere_depth" + camera.getMaxRecursionDepth() + ".png";
    File outputFile = new File(filename);
    
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
