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

final public class RecursiveTest extends Object {
  
  private RecursiveTest() {
    super();
  }
  
  public String toString() {
    return "RecursiveTest";
  }
  
  /**
   * Generates a scene with a Box, applies lighting, renders the image
   * using ray tracing, and saves it to a file.
   * The recursion depth can be specified via command-line argument.
   * Example: java -cp bin\\elenaRT.jar; RecursiveTest 5
   */
  final private static void generateSaveRenderedImage(String[] args) throws IOException {
    
    // --- 1. USAGE INFORMATION ---
    System.out.println("Usage: You can specify recursion depth as an argument.");
    System.out.println("Example: java -cp bin\\elenaRT.jar; RecursiveTest number[1-5]\n");
    
    // --- 2. CREATE THE SCENE ---
    Scene scene = new Scene();
    System.out.println("Scene created.");
    
    // --- 3. CONFIGURE THE RAY TRACER ---
    int imageWidth = 800;
    int imageHeight = 600;
    Color backgroundColor = new Color(170, 190, 210);
    
    ElenaMuratRayTracer rayTracer = new ElenaMuratRayTracer(scene, imageWidth, imageHeight, backgroundColor);
    System.out.println("Ray tracer initialized: " + imageWidth + "x" + imageHeight);
    
    // --- 4. OPTIMIZED CAMERA SETTINGS ---
    Camera camera = new Camera();
    camera.setCameraPosition(new Point3(1.2, 2.5, 3.0));  // Slightly elevated for better view
    camera.setLookAt(new Point3(0, 1.1, 0));      // Looking slightly downward
    camera.setUpVector(new Vector3(0, 1, 0));
    camera.setFov(38.0);  // Narrower FOV for less distortion
    camera.setOrthographic(false);
    
    camera.setReflective (false); // Reflection
    camera.setRefractive (true); // Refraction
    camera.setShadowsEnabled (false); // Shade
    
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
    // Ana ışık (key light)
    // 1. Ana ışık (Key Light)
    
    // 1. Main light
    MuratPointLight mainLight = new MuratPointLight(
      new Point3(1.5, 6.0, 2.0),
      new Color(255, 250, 240),
      2.2
    );
    
    // 2. Ambient point
    MuratPointLight fillLight = new MuratPointLight(
      new Point3(-2, 5, 1),
      new Color(230, 240, 255),    // Cold white
      1.0
    );
    
    // 3. ElenaMuratAmbientLight
    ElenaMuratAmbientLight ambientLight = new ElenaMuratAmbientLight(
      new Color(200, 210, 220),    // color
      0.6                          // grade
    );
    
    scene.addLight(mainLight);
    scene.addLight(fillLight);
    scene.addLight(ambientLight);
    
    // --- 6. FLOOR MATERIAL ---
    EMShape ground = new Plane(new Point3(0, 0, 0), new Vector3(0, 1, 0));
    ground.setTransform(Matrix4.translate(new Vector3(0, -1.70, 0)));
    //Matrix4 rightWallTransform = Matrix4.translate(new Vector3(5, 0, 0));
    //ground.setTransform(rightWallTransform);
    Material floorMaterial = new CheckerboardMaterial(
      new Color(10, 10, 10), // color1 (Dark gray)
      new Color(250, 250, 250), // color2 (Light gray)
      0.5,
      0.5, 0.5, 0.4, 50.0, Color.WHITE, // Ambient, Diffuse (0.7->0.3), Specular (0.2->0.4) parameters
      0.95, // reflectivity
      1.0, 0.0, // indexOfRefraction, transparency
      ground.getInverseTransform()
    );
    ground.setMaterial(floorMaterial);
    //ground.setMaterial (new LambertMaterial (new Color (24, 184, 24, 128)));
    //ground.setMaterial(new LambertMaterial(new Color(230, 230, 230)));
    //ground.setTransform(new Matrix4().rotateX(-90));
    scene.addShape(ground);
    
    // --- 7. GLASS SPHERE (OPTIMIZED) ---
    
    Material reflectiveMaterial1 = new PhongMaterial(
      new Color(200, 50, 50), // Renk
      Color.WHITE, // Specular color
      60.0,        // Shininess
      0.3, 0.4, 0.4, // Ambient, Diffuse (0.7->0.3), Specular (0.8->0.4) coefficients
      0.65,        // Reflectivity (0.7'den 0.95'e yükseltildi)
      1.0,         // Index of Refraction (hava gibi)
      0.2          // Transparency (opak)
    );
    
    Material reflectiveMaterial2 = new PhongMaterial(
      new Color(255, 255, 255, 128), // Renk
      Color.WHITE, // Specular color
      70.0,        // Shininess
      0.3, 0.4, 0.4, // Ambient, Diffuse (0.7->0.3), Specular (0.8->0.4) coefficients
      0.65,        // Reflectivity (0.7'den 0.95'e yükseltildi)
      1.0,         // Index of Refraction (hava gibi)
      0.2          // Transparency (opak)
    );
    
    Sphere s1=new Sphere (1.0);
    s1.setTransform (new Matrix4 ().translate (-1.75, 1.5, 0));
    s1.setMaterial (reflectiveMaterial1);
    scene.addShape (s1);
    
    Sphere s2=new Sphere (1.0);
    s2.setTransform (new Matrix4 ().translate (0.3, 1.8, -0.3));
    s2.setMaterial (reflectiveMaterial2);
    scene.addShape (s2);
    
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
    String filename = "..\\images\\recursiveSphere_depth" + camera.getMaxRecursionDepth() + ".png";
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
