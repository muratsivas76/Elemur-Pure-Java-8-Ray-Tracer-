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

final public class ImageTest extends Object {
  
  private ImageTest() {
    super();
  }
  
  @Override
  public String toString() {
    return "ImageTest";
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
    System.out.println("Usage: java -cp bin\\elenaRT.jar ImageTest [recursionDepth 1-5] [char]");
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
    
    String imagePath = "";
    BufferedImage img = null;
    
    if (args.length > 1) {
      try {
        recursionDepth = Math.min(5, Math.max(1, Integer.parseInt(args[0])));
        System.out.printf("[Config] Using recursion depth: %d\n", recursionDepth);
        } catch (NumberFormatException e) {
        System.err.println("[Warning] Invalid depth argument. Using default: 5");
      }
      
      imagePath = args[1];
      
      try {
        img = ImageIO.read(new File(imagePath));
        } catch (IOException e) {
        img = null;
      }
    }
    
    if (img == null) {
      System.err.println("Null image error: " + imagePath);
      System.exit(-1);
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
    
    // For shiny metals:
    scene.addLight(new MuratPointLight(
        new Point3(2, 5, 3),
        new Color(255, 240, 220), // Warm white light
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
    Matrix4 m = new Matrix4();
    m = m.translate(new Vector3(0, -1.7, 0));
    //m = m.multiply(m.rotateZ(45));
    ground.setTransform(m); // Position below objects
    
    Material material = new CeramicTilePBRMaterial(
      new Color(250, 250, 255), // Ice white
      new Color(60, 70, 80),     // Graphite grout
      0.5,                       // Medium size
      0.02,                      // Thin grout
      0.01,                      // Super shiny
      0.5                        // Matte grout
    );
    
    ground.setMaterial(material);
    scene.addShape(ground);
    System.out.println("[Geometry] Checkered floor added");
    
    // ===== IMAGE 3D =====
    
    // Create material
    Material materialX = null;
    
    // 1. Load texture
    BufferedImage texture = img;
    
    // 2. Create Image3D object
    Image3D image3D = new Image3D(texture, 32, 1.0, 1.0, 0.1);
    
    // 3. Create and assign material
    image3D.setMaterial(material);
    
    // 4. Set transform
    Matrix4 transform = Matrix4.identity()
    .scale(3, 3, 1) // Scale 3x in X and Y
    .rotateY(30)    // Rotate 30 degrees
    .translate(0, 1, 0); // Move up
    
    image3D.setTransform(transform);
    
    // 5. Add to scene
    scene.addShape(image3D);
    
    System.out.println("Shape count added to scene: " + scene.getShapes().size());
    // ===== END IMAGE 3D =====
    
    // ===== RENDERING =====
    System.out.println("\n=== RENDERING STARTED ===");
    System.out.printf("Resolution: %dx%d\n", imageWidth, imageHeight);
    System.out.println("Max Recursion: " + camera.getMaxRecursionDepth());
    System.out.println("Active Lights: " + scene.getLights().size());
    System.out.println("Scene Objects: " + scene.getShapes().size());
    
    long startTime = System.nanoTime();
    BufferedImage renderedImage = rayTracer.render();
    long durationMs = (System.nanoTime() - startTime) / 1_000_000;
    
    System.out.printf("Render completed in %.2f seconds\n", durationMs/1000.0);
    
    // ===== OUTPUT =====
    String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
    String filename = String.format(
      "..%simages%simg%s_render_%s_depth%d.png",
      File.separator, File.separator, "3D", timestamp, recursionDepth
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
