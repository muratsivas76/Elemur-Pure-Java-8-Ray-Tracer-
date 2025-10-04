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

final public class Stars extends Object {
  
  private Stars() {
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
    Color rendererBackgroundColor = new Color(5, 5, 30);
    
    ElenaMuratRayTracer rayTracer = new ElenaMuratRayTracer(scene, imageWidth, imageHeight, rendererBackgroundColor);
    
    // 3. Set Ray Tracer settings
    rayTracer.setCameraPosition(new Point3(0, 0, 5));
    rayTracer.setLookAt(new Point3(0, 0, 0));
    rayTracer.setUpVector(new Vector3(0, 1, 0));
    rayTracer.setOrthographic(false);
    rayTracer.setFov(60.0);
    rayTracer.setMaxRecursionDepth(5);
    
    // 4. Create and Add Lights (Minimal setup)
    Light blueLight = new MuratPointLight(
      new Point3(0, 3, 0),
      new Color(150, 150, 255), // Mavi ton
      2.0
    );
    
    Light purpleLight = new ElenaDirectionalLight(
      new Vector3(-1, -1, -1).normalize(),
      new Color(200, 100, 255),
      0.5
    );
    
    scene.addLight(blueLight);
    scene.addLight (purpleLight);
    
    // 5. Create and Add Shapes and Materials
    EMShape sphere=new Sphere (3.0);
    
    Material starfield =  new StarfieldMaterial(
      sphere.getInverseTransform(),
      new Color(25, 20, 70),
      0.006,
      0.004,
      1.0
    );
    
    sphere.setMaterial (starfield);
    
    scene.addShape (sphere);
    
    // 6. Render the Ray Tracer
    System.out.println("Render process starting...");
    long startTime = System.currentTimeMillis();
    
    BufferedImage renderedImage = rayTracer.render();
    
    long endTime = System.currentTimeMillis();
    System.out.println("Render process completed. Duration: " + (endTime - startTime) + " ms");
    
    // 7. Save the Image
    try {
      File outputFile = new File("..\\images\\stars.png");
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
javac -cp ..\bin\elenaRT.jar; Stars.java
java -cp ..\bin\elenaRT.jar; Stars
..\images\stars.png
 */
