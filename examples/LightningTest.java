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

final public class LightningTest extends Object {
  
  private LightningTest() {
    super();
  }
  
  @Override
  public String toString() {
    return "LightningTest";
  }
  
  /**
   * Generates and renders a scene with lightning effect, saving the output as a PNG image.
   * Allows user to specify recursion depth for ray tracing quality.
   *
   * @param args Command line arguments (optional recursion depth [1-5])
   * @throws IOException If image saving fails
   */
  private static void generateSaveRenderedImage(String[] args) throws IOException {
    // ===== BASIC SETTINGS =====
    int width = 800;
    int height = 600;
    Color backgroundColor = new Color(20, 20, 40); // Dark blue background
    
    // Depth setting (default 2, can be specified by user)
    int depth = 2;
    if (args.length > 0) {
      try {
        depth = Math.max(1, Math.min(5, Integer.parseInt(args[0])));
        System.out.println("Depth level: " + depth);
        } catch (NumberFormatException e) {
        System.err.println("Invalid depth! Using default 2.");
      }
    }
    
    // ===== SCENE AND CAMERA SETUP =====
    Scene scene = new Scene();
    Camera camera = new Camera();
    
    // Camera positioning
    camera.setCameraPosition(new Point3(0, 3, 5));
    camera.setLookAt(new Point3(0, 2, 0));
    camera.setUpVector(new Vector3(0, 1, 0));
    camera.setFov(45.0);
    camera.setMaxRecursionDepth(depth); // User-specified depth
    
    // Lighting setup
    scene.addLight(new ElenaDirectionalLight(
        new Vector3(-1, -1, -1).normalize(),
        new Color(255, 250, 240),
        0.7
    ));
    scene.addLight(new ElenaMuratAmbientLight(
        new Color(100, 100, 120),
        0.3
    ));
    
    // ===== LIGHTNING EFFECT =====
    // 1. Ceiling (black)
    EMShape ceiling = new Plane(new Point3(0, 5, 0), new Vector3(0, -1, 0));
    ceiling.setMaterial(new SolidColorMaterial(Color.BLACK));
    scene.addShape(ceiling);
    
    // 2. Lightning plane
    Material lightningMat = new LightningMaterial(
      new Color(200, 230, 255), // Color
      3.0 // Intensity
    );
    
    EMShape lightning = new Plane(new Point3(0, 4.9, 0), new Vector3(0, -1, 0));
    lightning.setTransform(Matrix4.scale(15, 15, 15));
    lightning.setMaterial(lightningMat);
    scene.addShape(lightning);
    
    // 3. Test sphere (simple material instead of reflective)
    EMShape sphere = new Sphere(1.0);
    sphere.setTransform (new Matrix4 ().translate (0, 2, 0));
    //sphere.setMaterial(new DiffuseMaterial(new Color(200, 200, 255))); // Simple blue material
    sphere.setMaterial (new ReflectiveMaterial ());
    scene.addShape(sphere);
    
    // ===== RENDERING PROCESS =====
    ElenaMuratRayTracer renderer = new ElenaMuratRayTracer(scene, width, height, backgroundColor);
    renderer.setCamera(camera);
    
    System.out.println("Starting render...");
    long startTime = System.currentTimeMillis();
    BufferedImage image = renderer.render();
    long duration = System.currentTimeMillis() - startTime;
    
    // Save output
    String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
    String filename = "..\\images\\lightning_" + depth + "depth_" + timestamp + ".png";
    ImageIO.write(image, "png", new File(filename));
    System.out.println("Render completed! (" + (duration/1000.0) + " seconds)");
    System.out.println("Saved as: " + filename);
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
