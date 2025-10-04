import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.text.SimpleDateFormat;

// Custom classes
import net.elena.murat.shape.*;
import net.elena.murat.shape.letters.*;
import net.elena.murat.lovert.*;
import net.elena.murat.material.*;
import net.elena.murat.material.pbr.*;
import net.elena.murat.math.*;
import net.elena.murat.light.*;
import net.elena.murat.util.*;

final public class SmartGlassTest extends Object {
  
  private SmartGlassTest() {
    super();
  }
  
  @Override
  public String toString() {
    return "SmartGlassTest";
  }
  
  /**
   * Generates and renders a scene containing a glass sphere on a checkered floor
   * with optimized lighting and camera settings. Renders using ray tracing with
   * configurable recursion depth and saves the output to a PNG file.
   * @param args Command-line arguments (optional recursion depth [1-5])
   * @throws IOException If image saving fails
   */
  private static void generateSaveRenderedImage(String[] args) throws IOException {
    // ===== INITIALIZATION AND USAGE INFO =====
    System.out.println("ElenaMurat Ray Tracer - Demo\n");
    System.out.println("Usage: java -cp bin\\elenaRT.jar SmartGlassTest [recursionDepth 1-5] [char]");
    System.out.println("Default recursion depth: 5\n");
    
    // ===== SCENE SETUP =====
    Scene scene = new Scene();
    System.out.println("[Scene] Scene instance created");
    
    // ===== RAY TRACER CONFIGURATION =====
    final int imageWidth = 640;
    final int imageHeight = 480;
    final Color backgroundColor = new Color(230, 20, 20);
    
    ElenaMuratRayTracer rayTracer = new ElenaMuratRayTracer(
      scene,
      imageWidth,
      imageHeight,
      backgroundColor
    );
    System.out.printf("[Renderer] Initialized %dx%d ray tracer\n", imageWidth, imageHeight);
    
    // ===== CAMERA CONFIGURATION =====
    Camera camera = new Camera();
    
    // 1. Position and View Angle Optimization (Ideal for glass objects)
    camera.setCameraPosition(new Point3(2.5, 1.8, 4.0)); // 45 degree angle view from above
    camera.setLookAt(new Point3(0, 1.8, 0)); // Focus on center of objects
    camera.setUpVector(new Vector3(0, 1, 0).normalize()); // Vertical stability
    
    // 2. Field of View and Projection
    camera.setFov(30.0); // Narrow angle (35-45° can also be tried)
    camera.setOrthographic(false); // Perspective projection
    
    // 3. Ray Tracing Features
    camera.setReflective(true); // For reflections (critical for glass materials)
    camera.setRefractive(true); // Refraction effects (essential for glass)
    camera.setShadowsEnabled(true); // Realistic shadows
    
    // Set recursion depth (3-5 recommended for glass)
    int recursionDepth = 2;
    
    //char character='L';
    
    if (args.length > 0) {
      try {
        recursionDepth = Math.min(5, Math.max(1, Integer.parseInt(args[0])));
        System.out.printf("[Config] Using recursion depth: %d\n", recursionDepth);
        } catch (NumberFormatException e) {
        System.err.println("[Warning] Invalid depth argument. Using default: 2");
      }
      
      //character=args [1].charAt (0);
    }
    camera.setMaxRecursionDepth(recursionDepth);
    rayTracer.setCamera(camera);
    
    // ===== OPTIMIZED LIGHTING SETUP =====
    
    scene.addLight(new ElenaDirectionalLight(
        new Vector3(-1, -1, -1).normalize(),
        new Color(255, 250, 240),
        1.0
    ));
    
    System.out.println("[Lighting] 3-point lighting setup complete");
    
    // ===== OPTIMIZED FLOOR =====
    EMShape ground = new Plane(new Point3(0, 0, 0), new Vector3(0, 1, 0));
    Matrix4 m=new Matrix4 ();
    //m=m.rotateY (30);
    //m=m.multiply (m.rotateX (-15));
    //m=m.translate(new Vector3(0, -1.7, 0));
    m=m.multiply (m.translate(new Vector3(0, -1.7, 0)));
    ground.setTransform(m); // Position below objects
    
    Material material=null;
    
    //material=new FractalFireMaterial (20, 1.2, 1.5, 0.8);
    material=new HologramDataMaterial (0.7, 256);
    
    ground.setMaterial (material);
    scene.addShape(ground);
    System.out.println("[Geometry] Checkered floor added");
    
    // ===== LETTER =====
    System.out.println("Shape count added to scene: " + scene.getShapes().size());
    // ===== END LETTER =====
    
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
      "..%simages%ssmartGlass_render_%s_depth%d.png",
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
