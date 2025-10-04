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
//import net.elena.murat.util.*;

final public class BlackHole extends Object {
  
  private BlackHole() {
    super();
  }
  
  public String toString() {
    return "";
  }
  
  final private static void generateSaveRenderedImage() throws IOException {
    // 1. Create the Scene
    Scene scene = new Scene();
    
    // 2. Create the Ray Tracer object
    int imageWidth = 800;
    int imageHeight = 600;
    Color rendererBackgroundColor = new Color(5, 5, 5); // Black background
    
    ElenaMuratRayTracer rayTracer = new ElenaMuratRayTracer(scene, imageWidth, imageHeight, rendererBackgroundColor);
    
    // 3. Set Ray Tracer settings (optimized for BlackHole)
    rayTracer.setCameraPosition(new Point3(0, 0, 5)); // Looking at black hole center
    rayTracer.setLookAt(new Point3(0, 0, 0));
    rayTracer.setUpVector(new Vector3(0, 1, 0));
    rayTracer.setOrthographic(false);
    rayTracer.setFov(30.0); // Narrow angle for capturing details
    rayTracer.setMaxRecursionDepth(12); // High because light bending is complex
    
    // 4. Create Lights (for BlackHole effect)
    Light accretionLight = new MuratPointLight(
      new Point3(1.5, 0, 0), // Beside the black hole
      new Color(255, 100, 50), // Orange-red accretion disk
      3.0
    );
    
    Light ambientGlow = new ElenaMuratAmbientLight(
      new Color(30, 10, 40), // Purple-ish ambient
      0.3
    );
    
    scene.addLight(accretionLight);
    scene.addLight(ambientGlow);
    
    // 5. Create BlackHole
    EMShape blackHole = new Sphere(1.0); // Schwarzschild radius
    
    Material blackHoleMat = new BlackHoleMaterial(
      new Point3(0, 0, 0), // Singularity position
      blackHole.getInverseTransform()
      //0.5  // Event horizon width
    );
    
    blackHole.setMaterial(blackHoleMat);
    scene.addShape(blackHole);
    
    // 6. Render the Ray Tracer
    System.out.println("Render process starting...");
    long startTime = System.currentTimeMillis();
    
    BufferedImage renderedImage = rayTracer.render();
    
    long endTime = System.currentTimeMillis();
    System.out.println("Render process completed. Duration: " + (endTime - startTime) + " ms");
    
    // 7. Save the Image
    try {
      File outputFile = new File("..\\images\\blackhole.png");
      ImageIO.write(renderedImage, "png", outputFile);
      System.out.println("Image saved successfully: " + outputFile.getAbsolutePath());
      } catch (IOException e) {
      System.err.println("An error occurred while saving the image: " + e.getMessage());
      e.printStackTrace();
    }
  }
  
  final public static void main(final String[] args) {
    try {
      generateSaveRenderedImage();
      } catch (IOException ioe) {
      ioe.printStackTrace();
      System.exit(-1);
    }
  }
  
}

/***
javac -cp ..\bin\elenaRT.jar; BlackHole.java
java -cp ..\bin\elenaRT.jar; BlackHole
..\images\blackhole.png
 */
