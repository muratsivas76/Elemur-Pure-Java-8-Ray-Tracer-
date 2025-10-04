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

final public class YTracer extends Object {
  
  private YTracer() {
    super();
  }
  
  public String toString() {
    return "YTracer";
  }
  
  final private static void generateSaveRenderedImage(String [] args) throws IOException {
    //USAGE INFO
    System.out.println ("You can select recursiveDepthValue:");
    System.out.println ("\tjava -cp bin\\elenaRT.jar; YTracer 4\n");
    
    // 1. Create Scene
    Scene scene = new Scene();
    
    // 2. Create Ray Tracer
    int imageWidth = 800;
    int imageHeight = 600;
    Color rendererBackgroundColor = new Color(0.2f, 0.2f, 0.2f); // Dark gray background
    
    // Create ElenaMuratRayTracer
    ElenaMuratRayTracer rayTracer = new ElenaMuratRayTracer(scene, imageWidth, imageHeight, rendererBackgroundColor);
    
    // 3. Adjust values of ray tracer
    rayTracer.setCameraPosition(new Point3(0, 0, 5)); // Position camera at (0,0,5)
    rayTracer.setLookAt(new Point3(0, 0, 0)); // Look at (0,0,0) point
    rayTracer.setUpVector(new Vector3(0, 1, 0));
    rayTracer.setFov(60.0);
    
    if (args.length > 0) {
      int num=3;
      
      try {
        num=Integer.parseInt (args [0x0000]);
        } catch (NumberFormatException nfe) {
        num=3;
      }
      rayTracer.setMaxRecursionDepth (num);
      } else {
      rayTracer.setMaxRecursionDepth(3);
    }
    
    // 4. Create and add lights
    scene.addLight(new ElenaMuratAmbientLight(new Color(50, 55, 100), 0.5)); // Lower ambient light
    scene.addLight(new ElenaDirectionalLight(
        new Vector3(-0.5, -1, -0.3).normalize(),
        Color.WHITE, // White light
        1.0
    ));
    
    scene.addLight(new BlackHoleLight(
        new Point3(0, 0, -1),   // Black hole position (behind the spheres)
        0.5,                    // Event horizon radius
        new Color(200, 150, 255), // Accretion disk color (bluish purple)
        5.0                     // Base intensity
    ));
    
    // 5. Create shapes with materials and add them to the scene.
    // a. Reflective Sphere
    Sphere reflectiveSphere = new Sphere(0.7);
    PhongMaterial reflectiveMaterial = new PhongMaterial(
      new Color(200, 50, 50), // Reddish color
      Color.WHITE, // Specular color
      60.0,        // Shininess
      0.1, 0.3, 0.4, // Ambient, Diffuse (0.7->0.3), Specular (0.8->0.4) coefficients
      0.95,        // Reflectivity (increased from 0.7 to 0.95)
      1.0,         // Index of Refraction (like air)
      0.0          // Transparency (opaque)
    );
    reflectiveSphere.setMaterial(reflectiveMaterial);
    reflectiveSphere.setTransform(Matrix4.translate(new Vector3(0, 0.0, 0))); // Position near center
    scene.addShape(reflectiveSphere);
    
    // b. Transparent Sphere (for refraction)
    Sphere glassSphere = new Sphere(0.7);
    PhongMaterial glassMaterial = new PhongMaterial(
      new Color(150, 200, 255), // Light blue glass color
      Color.WHITE, // Specular color
      60.0,        // Shininess
      0.1, 0.7, 0.8, // Ambient, Diffuse, Specular coefficients
      0.1,         // Reflectivity (low reflection)
      1.5,         // Index of Refraction (glass refractive index)
      0.9          // Transparency (high transparency)
    );
    glassSphere.setMaterial(glassMaterial);
    glassSphere.setTransform(Matrix4.translate(new Vector3(1.5, 0.0, -1.0))); // Behind and to the right of reflective sphere
    scene.addShape(glassSphere);
    
    // c. Floor (Reflective)
    Plane floorPlane = new Plane(new Point3(0, 0, 0), new Vector3(0, 1, 0));
    floorPlane.setTransform(Matrix4.translate(new Vector3(0, -1.0, 0)));
    CheckerboardMaterial floorMaterial = new CheckerboardMaterial(
      new Color(100, 100, 100), // color1 (Dark gray)
      new Color(200, 200, 200), // color2 (Light gray)
      0.5, // size (Reduced for more frequent squares)
      0.1, 0.3, 0.4, 10.0, Color.WHITE, // Ambient, Diffuse (0.7->0.3), Specular (0.2->0.4) parameters
      0.95, // reflectivity (increased from 0.8 to 0.95)
      1.0, 0.0, // indexOfRefraction, transparency
      floorPlane.getInverseTransform()
    );
    floorPlane.setMaterial(floorMaterial);
    scene.addShape(floorPlane);
    
    // d. Back Wall (Reflective)
    Plane backWallPlane = new Plane(new Point3(0, 0, 0), new Vector3(0, 0, 1)); // Normal points inwards
    Matrix4 backWallTransform = Matrix4.translate(new Vector3(0, 0, -2.5)); // Closer wall
    backWallPlane.setTransform(backWallTransform);
    SquaredMaterial backWallMaterial = new SquaredMaterial(
      new Color(0.3f, 0.3f, 0.0f), // Dark yellow
      new Color(1.0f, 1.0f, 0.0f), // Bright yellow
      0.5,                       // Square size (frequency)
      0.1, 0.3, 0.4, 50.0, Color.WHITE, // Ambient, Diffuse (0.7->0.3), Specular (0.8->0.4) parameters
      0.95, 1.0, 0.0,             // reflectivity (0.8->0.95), ior, transparency
      backWallPlane.getInverseTransform()
    );
    backWallPlane.setMaterial(backWallMaterial);
    scene.addShape(backWallPlane);
    
    // 6. Render image
    System.out.println("Render process starting (Depth: " + rayTracer.getMaxRecursionDepth() + ")...");
    long startTime = System.currentTimeMillis();
    
    BufferedImage renderedImage = rayTracer.render();
    
    long endTime = System.currentTimeMillis();
    System.out.println("Render process completed. Time: " + (endTime - startTime) + " ms");
    
    // 7. Save image
    try {
      File outputFile = new File("..\\images\\rendered_scene_depth" + rayTracer.getMaxRecursionDepth () + ".png");
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
