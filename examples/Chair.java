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

final public class Chair extends Object {
  
  private Chair() {
    super();
  }
  
  public String toString() {
    return "Chair";
  }
  
  final private static void generateSaveRenderedImage(String [] args) throws IOException {
    //USAGE INFO
    System.out.println ("You can select recursiveDepthValue:");
    System.out.println ("\tjava -cp bin\\elenaRT.jar; Chair numDepth\n");
    
    // 1. Create Scene
    Scene scene = new Scene();
    
    // 2. Create Ray Tracer
    int imageWidth = 800;
    int imageHeight = 600;
    Color rendererBackgroundColor = new Color(0.10f, 0.10f, 0.99f);
    
    // Create ElenaMuratRayTracer
    ElenaMuratRayTracer rayTracer = new ElenaMuratRayTracer(scene, imageWidth, imageHeight, rendererBackgroundColor);
    
    // 3. Adjust values of ray tracer
    // Kamera pozisyonunu ve bakış noktasını ayarla
    rayTracer.setCameraPosition(new Point3(0, 0.5, 1.0)); // Yakın
    rayTracer.setLookAt(new Point3(0, -0.2, -2));         // Sandalyenin merkezine bak
    rayTracer.setUpVector(new Vector3(0, 1, 0));
    rayTracer.setFov(40.0);                               // Daha doğal perspektif
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
    // 1. Ana yönlü ışık (zaten var, koru)
    scene.addLight(new ElenaDirectionalLight(
        new Vector3(-0.5, -1, -0.3).normalize(),
        Color.WHITE,
        1.0
    ));
    
    // 2. Ambient ışık (zaten var, koru)
    scene.addLight(new ElenaMuratAmbientLight(new Color(0.2f, 0.2f, 0.2f), 0.5));
    
    // 3. MuratPointLight 1: Sağ üst köşeden hafif kırmızımsı ışık (ısıtıcı etki)
    scene.addLight(new MuratPointLight(
        new Point3(3, 2, -4),     // Sağ üst ön
        new Color(1.0f, 0.8f, 0.7f), // Sıcak beyaz (ampul etkisi)
        1.2                        // Biraz güçlü
    ));
    
    // 4. MuratPointLight 2: Sol arkadan mavi-turkuaz ışık (dekoratif, gölgeyi zenginleştirir)
    scene.addLight(new MuratPointLight(
        new Point3(-3, 1, -6),     // Sol arka
        new Color(0.7f, 0.8f, 1.0f), // Hafif mavi
        0.8                        // Daha yumuşak
    ));
    
    // 5. Create shapes with materials and add them to the scene.
    Plane floorPlane = new Plane(new Point3(0, 0, 0), new Vector3(0, 1, 0));
    floorPlane.setTransform(Matrix4.translate(new Vector3(0, -1.0, 0)));
    
    Material planeMaterial = new SquaredMaterial(
      new Color(0.0f, 0.0f, 0.0f),
      new Color(1.0f, 1.0f, 1.0f),
      2,
      0.1, 0.3, 0.4, 50.0, Color.RED,
      0.75, 1.0, 0.0,
      floorPlane.getInverseTransform()
    );
    
    floorPlane.setMaterial(planeMaterial);
    scene.addShape(floorPlane);
    
    final Material csgMaterial = new PhongMaterial(
      Color.RED,
      Color.BLUE,
      60.0,
      0.2, 0.7, 0.7,
      0.2, 1.0, 0.0
    );
    
    ////ADD CHAIR PIECES HERE //////////
    // 1. Seat (rectangular prism)
    Point3 seatMin = new Point3(-0.4, -0.05, -0.4); // width=0.8, height=0.1, depth=0.8
    Point3 seatMax = new Point3( 0.4,  0.05,  0.4);
    Cube seat = new Cube(seatMin, seatMax);
    seat.setTransform(Matrix4.translate(0, -0.4, -5));
    seat.setMaterial(csgMaterial);
    
    // 2. Backrest (vertical, thin rectangular prism)
    Point3 backMin = new Point3(-0.4, -0.3, -0.05); // width=0.8, height=0.6, depth=0.1
    Point3 backMax = new Point3( 0.4,  0.3,  0.05);
    Cube backrest = new Cube(backMin, backMax);
    backrest.setTransform(Matrix4.translate(0, 0.2, -4.8));
    backrest.setMaterial(csgMaterial);
    
    // 3. Legs (4 cylinders)
    Cylinder leg1 = new Cylinder(0.1, 0.3); // Yarıçap 0.1, yükseklik 0.3
    leg1.setTransform(Matrix4.translate(-0.3, -0.7, -4.7));
    leg1.setMaterial(csgMaterial);
    
    Cylinder leg2 = new Cylinder(0.1, 0.3);
    leg2.setTransform(Matrix4.translate(0.3, -0.7, -4.7));
    leg2.setMaterial(csgMaterial);
    
    Cylinder leg3 = new Cylinder(0.1, 0.3);
    leg3.setTransform(Matrix4.translate(-0.3, -0.7, -5.3));
    leg3.setMaterial(csgMaterial);
    
    Cylinder leg4 = new Cylinder(0.1, 0.3);
    leg4.setTransform(Matrix4.translate(0.3, -0.7, -5.3));
    leg4.setMaterial(csgMaterial);
    
    // === SCENE'E EKLE ===
    scene.addShape(seat);
    scene.addShape(backrest);
    scene.addShape(leg1);
    scene.addShape(leg2);
    scene.addShape(leg3);
    scene.addShape(leg4);
    ////ADDED CHAIR PIECES ////////////
    
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
      File outputFile = new File("..\\images\\chair_depth" + rayTracer.getMaxRecursionDepth() + ".png");
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
