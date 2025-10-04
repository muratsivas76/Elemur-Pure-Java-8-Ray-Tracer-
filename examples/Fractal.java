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

final public class Fractal extends Object {
  
  private Fractal() {
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
    Color rendererBackgroundColor = new Color(225, 225, 220);
    
    ElenaMuratRayTracer rayTracer = new ElenaMuratRayTracer(scene, imageWidth, imageHeight, rendererBackgroundColor);
    
    // 3. Set Ray Tracer settings
    rayTracer.setCameraPosition(new Point3(3, 2, 5));
    rayTracer.setLookAt(new Point3(0, 0, 0));
    rayTracer.setUpVector(new Vector3(0, 1, 0));
    rayTracer.setOrthographic(false);
    rayTracer.setFov(60.0);
    rayTracer.setMaxRecursionDepth(5);
    
    // 4. Create and Add Lights (Minimal setup)
    Light sunlight = new ElenaDirectionalLight(
      new Vector3(0.5, -1, 0.5).normalize(),
      new Color(255, 240, 220),
      1.2
    );
    
    scene.addLight(sunlight);
    
    // 5. Create and Add Shapes and Materials
    EMShape cylinder=new Cylinder (1.0, 3.0);
    
    Material bark = new FractalBarkMaterial(
      cylinder.getInverseTransform (),
      0.9 // Roughness (0-1 arası)
    );
    
    cylinder.setMaterial (bark);
    
    scene.addShape (cylinder);
    
    // 6. Render the Ray Tracer
    System.out.println("Render process starting...");
    long startTime = System.currentTimeMillis();
    
    BufferedImage renderedImage = rayTracer.render();
    
    long endTime = System.currentTimeMillis();
    System.out.println("Render process completed. Duration: " + (endTime - startTime) + " ms");
    
    // 7. Save the Image
    try {
      File outputFile = new File("..\\images\\fractal.png");
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
javac -cp ..\bin\elenaRT.jar; Fractal.java
java -cp ..\bin\elenaRT.jar; Fractal
..\images\fractal.png
 */
