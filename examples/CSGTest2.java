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

final public class CSGTest2 extends Object {
  
  private CSGTest2() {
    super();
  }
  
  public String toString() {
    return "CSGTest2";
  }
  
  final private static void generateSaveRenderedImage(String[] args) throws IOException {
    // USAGE INFO
    System.out.println("You can select recursiveDepthValue:");
    System.out.println("\tjava -cp bin\\elenaRT.jar; CSGTest2 numDepth\n");
    
    // 1. Create Scene
    Scene scene = new Scene();
    
    // 2. Create Ray Tracer
    int imageWidth = 800;
    int imageHeight = 600;
    Color rendererBackgroundColor = new Color(0.10f, 0.10f, 0.99f);
    
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
    scene.addLight(new ElenaMuratAmbientLight(new Color(50, 55, 100), 1));
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
      60.0,
      0.2, 0.7, 0.7,
      0.2, 1.0, 0.0
    );
    
    // === UNION (Left side) ===
    {
    }
    
    // === INTERSECTION (Center) ===
    {
      // 1. Cube (to be cut)
      Cube cube = new Cube(2.0); // 2x2x2 cube
      cube.setTransform(Matrix4.translate(0, 0, -5)); // Position near center
      
      // 2. Large sphere (cutter)
      Sphere sphere = new Sphere(2.4); // Large enough to cover cube corners
      sphere.setTransform(Matrix4.translate(0, 0, -5)); // Same center
      
      // 3. Intersection: only common area (rounded cube)
      EMShape roundedCube = new IntersectionCSG(cube, sphere);
      roundedCube.setMaterial(csgMaterial); // Same material (e.g. red-metallic)
      roundedCube.setTransform(Matrix4.translate(0, 0, 0)); // Place at exact center
      //scene.addShape(roundedCube);
    }
    
    // === DIFFERENCE (Right side) ===
    {
      // 1. Outer cylinder (large, pipe outer wall)
      Cylinder outer = new Cylinder(0.8, 2.0); // Radius 0.8, height 2.0
      outer.setTransform(Matrix4.translate(2.5, 0, 0)); // Place on right side
      
      // 2. Inner cylinder (small, for hollow space)
      Cylinder inner = new Cylinder(0.6, 2.0); // Radius 0.6, height 2.0
      inner.setTransform(Matrix4.translate(2.5, 0, 0)); // Same position
      
      // 3. Difference operation: outer - inner → pipe (tunnel)
      EMShape pipe = new DifferenceCSG(outer, inner);
      pipe.setMaterial(csgMaterial);
      scene.addShape(pipe);
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
