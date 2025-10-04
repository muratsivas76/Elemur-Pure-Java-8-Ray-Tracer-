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

final public class CSGTest extends Object {
  
  private CSGTest() {
    super();
  }
  
  public String toString() {
    return "CSGTest";
  }
  
  final private static void generateSaveRenderedImage(String[] args) throws IOException {
    // USAGE INFO
    System.out.println("You can select recursiveDepthValue:");
    System.out.println("\tjava -cp bin\\elenaRT.jar; CSGTest numDepth\n");
    
    // 1. Create Scene
    Scene scene = new Scene();
    
    // 2. Create Ray Tracer
    int imageWidth = 800;
    int imageHeight = 600;
    Color rendererBackgroundColor = new Color(0.99f, 0.99f, 0.99f);
    
    // Create ElenaMuratRayTracer
    ElenaMuratRayTracer rayTracer = new ElenaMuratRayTracer(scene, imageWidth, imageHeight, rendererBackgroundColor);
    
    // 3. Adjust values of ray tracer
    // Set camera position and look-at point
    rayTracer.setCameraPosition(new Point3(0, 0.5, 6));
    rayTracer.setLookAt(new Point3(0, 0, 0));
    rayTracer.setUpVector(new Vector3(0, 1, 0));
    rayTracer.setFov(50.0); // Wider viewing angle (shows more scene)
    rayTracer.setOrthographic(false);
    //rayTracer.setOrthographicScale(0.75);
    
    if (args.length > 0) {
      int num = 3;
      
      try {
        num = Integer.parseInt(args[0x0000]);
        } catch (NumberFormatException nfe) {
        num = 3;
      }
      rayTracer.setMaxRecursionDepth(num);
      } else {
      rayTracer.setMaxRecursionDepth(3);
    }
    
    // 4. Create and add lights
    // ambient
    scene.addLight(new ElenaMuratAmbientLight(new Color(50, 55, 100), 0.5)); // Lower ambient
    scene.addLight(new ElenaDirectionalLight(
        new Vector3(-0.5, -1, -0.3).normalize(),
        Color.WHITE, // White light
        1.0
    ));//directional
    
    // 5. Create shapes with materials and add them to the scene.
    Plane floorPlane = new Plane(new Point3(0, 0, 0), new Vector3(0, 1, 0));
    floorPlane.setTransform(Matrix4.translate(new Vector3(0, -1.0, 0)));
    
    Material planeMaterial = new SquaredMaterial(
      new Color(0.0f, 0.0f, 0.0f), // First
      new Color(1.0f, 1.0f, 1.0f), // Second
      2,                       // Square size (frequency)
      0.1, 0.3, 0.4, 50.0, Color.RED, // Ambient, Diffuse, Specular parameters
      0.75, 1.0, 0.0,             // reflectivity, ior, transparency
      floorPlane.getInverseTransform()
    );
    floorPlane.setMaterial(planeMaterial);
    scene.addShape(floorPlane);
    
    /////////////////////////////////////
    // CSG Test: Union, Intersection, Difference
    // Common material
    final Material csgMaterial = new PhongMaterial(
      Color.RED,
      Color.BLUE,
      50.0,
      0.1, 0.7, 0.8,
      0.2, 1.0, 0.0
    );
    
    // Common spheres (centered locally)
    Sphere u1 = new Sphere(1);
    Sphere u2 = new Sphere(1.2);
    u2.setTransform(Matrix4.translate(1, 0, 0)); // u2 is sphere 1 unit right of u1
    
    // === UNION (Left side) ===
    {
      EMShape union = new UnionCSG(u1, u2);
      union.setMaterial(csgMaterial);
      union.setTransform(Matrix4.translate(-2.5, 0, 0)); // Shift to left side
      scene.addShape(union);
    }
    
    // === INTERSECTION (Center) ===
    {
      EMShape intersection = new IntersectionCSG(u1, u2);
      intersection.setMaterial(csgMaterial);
      intersection.setTransform(Matrix4.translate(0.0, 0, 0)); // Center (default position)
      scene.addShape(intersection);
    }
    // === DIFFERENCE (Right side) ===
    {
      EMShape difference = new DifferenceCSG(u1, u2);
      difference.setMaterial(csgMaterial);
      difference.setTransform(Matrix4.translate(2.5, 0, 0)); // Shift to right side
      scene.addShape(difference);
    }
    /////////////////////////
    
    //{
    // You can write code pieces between {}
    //}
    
    // 6. Render image
    System.out.println("Render process starting (Depth: " + rayTracer.getMaxRecursionDepth() + ")...");
    long startTime = System.currentTimeMillis();
    
    BufferedImage renderedImage = rayTracer.render();
    
    long endTime = System.currentTimeMillis();
    System.out.println("Render process completed. Time: " + (endTime - startTime) + " ms");
    
    // 7. Save image
    try {
      File outputFile = new File("..\\images\\csgTest_depth" + rayTracer.getMaxRecursionDepth() + ".png");
      ImageIO.write(renderedImage, "png", outputFile);
      System.out.println("Image successfully saved: " + outputFile.getAbsolutePath());
      
      } catch (IOException e) {
      System.err.println("An error occurred while saving the image: " + e.getMessage());
      e.printStackTrace();
    }
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
