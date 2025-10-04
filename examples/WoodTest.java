import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.text.SimpleDateFormat;

// Custom classes
import net.elena.murat.shape.*;
import net.elena.murat.lovert.*;
import net.elena.murat.material.*;
import net.elena.murat.material.pbr.*;
import net.elena.murat.math.*;
import net.elena.murat.light.*;
import net.elena.murat.util.*;

final public class WoodTest extends Object {
  
  private WoodTest() {
    super();
  }
  
  @Override
  public String toString() {
    return "WoodTest";
  }
  
  /**
   * Generates and renders a scene containing a glass sphere on a checkered floor
   * with optimized lighting and camera settings. Renders using ray tracing with
   * configurable recursion depth and saves the output to a PNG file.
   * @param args Command-line arguments (optional recursion depth [1-5])
   * @throws IOException If image saving fails
   */
  private static void generateSaveRenderedImage(String[] args) throws IOException {
    // ===== 1. INITIALIZATION AND USAGE INFO =====
    System.out.println("ElenaMurat Ray Tracer - Glass Sphere Demo\n");
    System.out.println("Usage: java -cp bin\\elenaRT.jar WoodTest [recursionDepth 1-5]");
    System.out.println("Default recursion depth: 5\n");
    
    // ===== 2. SCENE SETUP =====
    Scene scene = new Scene();
    System.out.println("[Scene] Scene instance created");
    
    // ===== 3. RAY TRACER CONFIGURATION =====
    final int imageWidth = 800;
    final int imageHeight = 600;
    final Color backgroundColor = new Color(250, 250, 250);
    
    ElenaMuratRayTracer rayTracer = new ElenaMuratRayTracer(
      scene,
      imageWidth,
      imageHeight,
      backgroundColor
    );
    System.out.printf("[Renderer] Initialized %dx%d ray tracer\n", imageWidth, imageHeight);
    
    // ===== 4. CAMERA CONFIGURATION =====
    Camera camera = new Camera();
    // Optimal camera positioning for glass objects
    camera.setCameraPosition(new Point3(3.0, 1.5, 4.0));
    camera.setLookAt(new Point3(0, 0, 0));
    camera.setUpVector(new Vector3(0, 1, 0).normalize());
    camera.setFov(35.0);
    camera.setOrthographic(false);
    
    // Ray tracing features
    camera.setReflective(true);
    camera.setRefractive(true);
    camera.setShadowsEnabled(true);
    
    // Set recursion depth (3-5 recommended for glass)
    int recursionDepth = 5;
    if (args.length > 0) {
      try {
        recursionDepth = Math.min(5, Math.max(1, Integer.parseInt(args[0])));
        System.out.printf("[Config] Using recursion depth: %d\n", recursionDepth);
        } catch (NumberFormatException e) {
        System.err.println("[Warning] Invalid depth argument. Using default: 5");
      }
    }
    camera.setMaxRecursionDepth(recursionDepth);
    rayTracer.setCamera(camera);
    
    // ===== 5. OPTIMIZED LIGHTING SETUP =====
    // 1. Main sunlight (Primary illumination)
    // 1. STRONG DIRECT light
    scene.addLight(new MuratPointLight(
        new Point3(2, 10, 3), // From above (reduces shadows)
        new Color(255, 250, 240), // Near white
        5.0 // Full power
    ));
    
    // 2. OMNIDIRECTIONAL light (Ambient++)
    scene.addLight(new ElenaMuratAmbientLight(
        new Color(255, 240, 230),
        1.5 // 1.5x intensity (higher than normal)
    ));
    
    // 3. GROUND REFLECTION light
    scene.addLight(new MuratPointLight(
        new Point3(0, -0.5, 0), // From ground upwards
        new Color(200, 190, 180),
        2.0
    ));
    
    System.out.println("[Lighting] 3-point lighting setup complete");
    
    // ===== 6. OPTIMIZED FLOOR =====
    EMShape ground = new Plane(new Point3(0, 0, 0), new Vector3(0, 1, 0));
    //Matrix4 m=new Matrix4 ();
    //m=m.multiply (m.rotateX (Math.toRadians (-90)));
    //m=m.translate(new Vector3(0, -1.7, 0));
    
    Matrix4 groundTransform =  new Matrix4()
    .rotateX(Math.toRadians(-90)) // HORIZONTAL
    .translate(0, -1, 0);       // Move down on Y axis
    
    ground.setTransform(groundTransform); // Position below objects
    
    Material material = new WoodPBRMaterial(
      new Color(120, 80, 50),    // Main brown color
      new Color(200, 170, 120),  // Honey-colored fibers
      4.0,                       // Wide rings
      0.8,                       // Very visible fibers
      0.3                        // Low roughness (shiny)
    )
    {
      public double getReflectivity () {
        return 0.4;
      }
    };
    
    ground.setMaterial (material);
    scene.addShape(ground);
    System.out.println("[Geometry] Checkered floor added");
    
    // ===== 7. GLASS SPHERE =====
    
    Material material2 = new DiffuseMaterial (Color.WHITE.darker ());
    
    Sphere sphere = new Sphere(1.0);
    Matrix4 m2=new Matrix4 ();
    m2=m2.translate(new Vector3(0, 2, 0));
    //m2=m2.multiply (m.rotateZ (45));
    //m2=m2.multiply (m.rotateX (35));
    //m2=m2.multiply (m.rotateY (25));
    sphere.setTransform(m2);
    
    sphere.setMaterial (material2);
    
    //scene.addShape(sphere);
    System.out.println("[Geometry] Sphere added");
    
    // ===== 8. RENDERING =====
    System.out.println("\n=== RENDERING STARTED ===");
    System.out.printf("Resolution: %dx%d\n", imageWidth, imageHeight);
    System.out.println("Max Recursion: " + camera.getMaxRecursionDepth());
    System.out.println("Active Lights: " + scene.getLights().size());
    System.out.println("Scene Objects: " + scene.getShapes().size());
    
    long startTime = System.nanoTime();
    BufferedImage renderedImage = rayTracer.render();
    long durationMs = (System.nanoTime() - startTime) / 1_000_000;
    
    System.out.printf("Render completed in %.2f seconds\n", durationMs/1000.0);
    
    // ===== 9. OUTPUT =====
    String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
    String filename = String.format(
      "..%simages%swood_render_%s_depth%d.png",
      File.separator, File.separator, timestamp, recursionDepth
    );
    
    File outputFile = new File(filename);
    if (!outputFile.getParentFile().exists()) {
      outputFile.getParentFile().mkdirs();
    }
    
    try {
      ImageIO.write(renderedImage, "png", outputFile);
      System.out.println("Image saved to: " + outputFile.getAbsolutePath());
      } catch (IOException e) {
      System.err.println("Save failed: " + e.getMessage());
      throw e;
    }
    
    System.out.println("=== RENDER COMPLETE ===\n");
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
