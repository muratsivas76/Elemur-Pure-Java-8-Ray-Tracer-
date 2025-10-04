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

final public class LetterTest extends Object {
  
  private LetterTest() {
    super();
  }
  
  @Override
  public String toString() {
    return "LetterTest";
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
    System.out.println("Usage: java -cp bin\\elenaRT.jar LetterTest [recursionDepth 1-5] [char]");
    System.out.println("Default recursion depth: 5\n");
    
    // ===== SCENE SETUP =====
    Scene scene = new Scene();
    System.out.println("[Scene] Scene instance created");
    
    // ===== RAY TRACER CONFIGURATION =====
    final int imageWidth = 640;
    final int imageHeight = 480;
    final Color backgroundColor = new Color(250, 250, 250);
    
    ElenaMuratRayTracer rayTracer = new ElenaMuratRayTracer(
      scene,
      imageWidth,
      imageHeight,
      backgroundColor
    );
    System.out.printf("[Renderer] Initialized %dx%d ray tracer\n", imageWidth, imageHeight);
    
    // ===== CAMERA CONFIGURATION =====
    Camera camera = new Camera();
    // Optimal camera positioning for glass objects
    camera.setCameraPosition(new Point3(2.5, 1.8, 4.0));
    camera.setLookAt(new Point3(0, 1.8, 0));
    camera.setUpVector(new Vector3(0, 1, 0).normalize());
    camera.setFov(30.0);
    camera.setOrthographic(false);
    
    // Ray tracing features
    camera.setReflective(true);
    camera.setRefractive(true);
    camera.setShadowsEnabled(true);
    
    // Set recursion depth (3-5 recommended for glass)
    int recursionDepth = 2;
    
    char character='L';
    
    if (args.length > 1) {
      try {
        recursionDepth = Math.min(5, Math.max(1, Integer.parseInt(args[0])));
        System.out.printf("[Config] Using recursion depth: %d\n", recursionDepth);
        } catch (NumberFormatException e) {
        System.err.println("[Warning] Invalid depth argument. Using default: 5");
      }
      
      character=args [1].charAt (0);
    }
    camera.setMaxRecursionDepth(recursionDepth);
    rayTracer.setCamera(camera);
    
    // ===== OPTIMIZED LIGHTING SETUP =====
    // Ambient Light - reduced intensity
    ElenaMuratAmbientLight ambientLight = new ElenaMuratAmbientLight(
      new Color(180, 195, 210),
      0.7
    );
    scene.addLight(ambientLight);
    
    scene.addLight(new MuratPointLight(
        new Point3(2, 5, 3),
        new Color(255, 240, 220),
        1.5
    ));
    
    scene.addLight(new ElenaDirectionalLight(
        new Vector3(-1, -1, -0.5).normalize(),
        new Color(255, 230, 210),
        0.7
    ));
    
    System.out.println("[Lighting] 3-point lighting setup complete");
    
    // ===== OPTIMIZED FLOOR =====
    EMShape ground = new Plane(new Point3(0, 0, 0), new Vector3(0, 1, 0));
    Matrix4 m=new Matrix4 ();
    m=m.translate(new Vector3(0, -1.7, 0));
    //m=m.multiply (m.rotateZ (45));
    ground.setTransform(m); // Position below objects
    
    Material material = new CeramicTilePBRMaterial(
      new Color(250, 250, 255), // Ice white
      new Color(60, 70, 80),    // Graphite grout
      0.5,                      // Medium size
      0.02,                     // Thin grout
      0.01,                     // Super shiny
      0.5                       // Matte grout
    );
    
    ground.setMaterial (material);
    scene.addShape(ground);
    System.out.println("[Geometry] Checkered floor added");
    
    // ===== LETTER =====
    
    // 1. Create material
    Material materialX = null;
    
    // 1. Create material (Gold PBR)
    materialX=new GoldPBRMaterial (0.0);
    //materialX=new DiffuseMaterial (Color.RED);
    
    // 1. Create letter L
    //Letter3D(char letter, int baseSize, double widthScale, double heightScale,
    //             double thickness, Font font)
    EMShape letter = new Letter3D(character, 32, 1.0, 1.0, 0.25,
    new java.awt.Font("Arial", 1, 32));
    
    // 2. Assign material
    letter.setMaterial(materialX);
    
    // 3. Set transformation
    // 1. First move pivot to center (Optional but critical)
    Matrix4 centerTransform = Matrix4.translate(-0.5, -0.5, -0.5);
    
    // 2. Apply scale
    Matrix4 scaleMatrix = Matrix4.scale(5, 5, 5); // 5x scaling
    
    // 3. Then move to position
    Matrix4 positionMatrix = Matrix4.translate(-1, 2, 0);
    
    // 4. Combine (Multiply from RIGHT to LEFT!)
    Matrix4 finalTransform = positionMatrix.multiply(scaleMatrix.multiply(centerTransform));
    
    // 5. Apply
    letter.setTransform(finalTransform);
    
    // 4. Add to scene
    scene.addShape(letter);
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
      "..%simages%sletter%s_render_%s_depth%d.png",
      File.separator, File.separator, Character.toString (character), timestamp, recursionDepth
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
