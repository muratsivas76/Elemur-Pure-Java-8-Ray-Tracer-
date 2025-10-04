import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.Optional;

// Custom classes
import net.elena.murat.shape.*;
import net.elena.murat.lovert.*;
import net.elena.murat.material.*;
import net.elena.murat.math.*;
import net.elena.murat.light.*;
import net.elena.murat.util.*;

final public class ReviseTracer extends Object {
  
  private ReviseTracer() {
    super();
  }
  
  public String toString() {
    return "ReviseTracer";
  }
  
  // This method renders the full scene and saves the image.
  // It is suitable for disabling detailed PolynomialSolver logs.
  final private static void generateSaveRenderedImage() throws IOException {
    // 1. Create the Scene
    Scene scene = new Scene();
    
    // 2. Create the Ray Tracer object
    int imageWidth = 800;
    int imageHeight = 600;
    // Background color for better contrast
    Color rendererBackgroundColor = new Color(0.9f, 0.9f, 0.9f);
    
    ElenaMuratRayTracer rayTracer = new ElenaMuratRayTracer(scene, imageWidth, imageHeight, rendererBackgroundColor);
    
    // 3. Set Ray Tracer settings
    rayTracer.setCameraPosition(new Point3(5, 5, 5)); // Camera positioned at (0, 5, 5)
    rayTracer.setLookAt(new Point3(0, 0, 0)); // Camera looking at plane at (0, -1, 0)
    rayTracer.setUpVector(new Vector3(0, 1, 0)); // Y axis still up
    //rayTracer.setOrthographic(true); // Set to perspective view (can be true for orthographic test)
    //rayTracer.setOrthographicScale(7.0); // Orthographic scale (only effective if isOrthographic is true)
    rayTracer.setFov(60.0); // Field of view
    rayTracer.setMaxRecursionDepth(5); // Recursion depth controlled by flag
    
    // 4. Create and Add Lights
    // Ambient light for general illumination
    ElenaMuratAmbientLight ambientLight = new ElenaMuratAmbientLight(Color.WHITE, 0.1); // Reduced ambient light
    MuratPointLight pointLight1 = new MuratPointLight(new Point3(-3, 5, 2), Color.WHITE, 0.8); // Main point light
    MuratPointLight pointLight2 = new MuratPointLight(new Point3(3, -2, -2), Color.WHITE, 0.6); // Second point light
    ElenaDirectionalLight dirLight = new ElenaDirectionalLight(new Vector3(-1, -1, -1), Color.WHITE, 0.4); // Directional light
    
    scene.addLight(ambientLight);
    scene.addLight(pointLight1);
    scene.addLight(pointLight2);
    scene.addLight(dirLight);
    
    // 5. Create and Add Shapes and Materials
    // --- Plane (Floor) ---
    Plane floorPlane = new Plane(new Point3(0, 0, 0), new Vector3(0, 1, 0)); // Local space definition
    floorPlane.setTransform(Matrix4.translate(new Vector3(0, -1.0, 0))); // Position in world space
    CheckerboardMaterial floorMaterial = new CheckerboardMaterial(
      new Color(80, 80, 80),   // Darker gray
      new Color(180, 180, 180), // Lighter gray
      4.0,                     // Size
      0.1, 0.7, 0.2, 10.0, Color.WHITE, // Phong params
      0.0, 1.0, 0.0,           // Reflectivity, IOR, Transparency
      floorPlane.getInverseTransform() // Pass inverse transform
    );
    floorPlane.setMaterial(floorMaterial);
    scene.addShape(floorPlane);
    
    // --- Sphere (Central Large Sphere) ---
    EMShape centralSphere = new Sphere(1.5); // A large sphere
    centralSphere.setTransform(Matrix4.translate(new Vector3(0, 0.5, 0))); // Slightly above floor, centered
    
    // Material Options (Enable one, comment out others)
    
    // 1. Checkerboard Material
    Material sphereMaterial = new CheckerboardMaterial(
      new Color(255, 0, 0),    // Red
      new Color(0, 0, 255),    // Blue
      2.0,                     // Size (larger squares)
      0.1, 0.7, 0.8, 50.0, Color.WHITE, // Phong params
      0.0, 1.0, 0.0,           // Reflectivity, IOR, Transparency
      centralSphere.getInverseTransform() // Pass inverse transform
    );
    
    // 2. Phong Material (Glass-like)
    sphereMaterial = new PhongMaterial(
      new Color(0, 0, 255),      // Diffuse color (Blue) - More distinct color
      new Color(255, 255, 255),  // Specular color (White)
      200.0,                     // Shininess
      0.1,                       // Ambient coefficient
      0.7,                       // Diffuse coefficient (more of its own color visible)
      0.8,                       // Specular coefficient
      0.1,                       // Reflectivity (reduced reflection)
      1.5,                       // Index of Refraction (glass-like)
      0.1                        // Transparency (reduced transparency)
    );
    
    // 3. Metallic Material (Gold)
    sphereMaterial = new GoldMaterial();
    
    // 4. Metallic Material (Silver)
    sphereMaterial = new SilverMaterial();
    
    // 5. Metallic Material (Copper)
    sphereMaterial = new CopperMaterial();
    
    // 6. Lambert Material (Solid Color)
    sphereMaterial = new LambertMaterial(new Color(0, 180, 0)); // Green
    
    // 7. Emissive Material (Light Source Sphere)
    sphereMaterial = new EmissiveMaterial(new Color(255, 100, 0), 5.0); // Orange light
    
    // 8. Squared Material
    sphereMaterial = new SquaredMaterial( // *** Corrected section here ***
      new Color(0.9f, 0.1f, 0.1f), // Color1 (Reddish)
      new Color(0.1f, 0.1f, 0.9f), // Color2 (Blueish)
      3.0,                         // Square size (frequency)
      0.1, 0.7, 0.8, 50.0, Color.WHITE, // Phong parameters
      0.0, 1.0, 0.0,               // Reflectivity, IOR, Transparency
      centralSphere.getInverseTransform() // *** ADDED SECTION: objectInverseTransform ***
    );
    
    // 9. Circle Texture Material
    sphereMaterial = new CircleTextureMaterial(
      new Color(100, 0, 100),    // Background color (Purple)
      new Color(255, 255, 0),    // Circles color (Yellow)
      0.5,                       // patternSize (smaller circles)
      centralSphere.getInverseTransform() // Pass inverse transform
    );
    
    // 10. Striped Material
    sphereMaterial = new StripedMaterial( // *** Corrected section here ***
      new Color(0, 0, 0),        // Color 1 (Black)
      new Color(255, 255, 255),  // Color 2 (White)
      20.0,                      // Stripe width - IMPORTANT: Increased from 0.2 to 20.0
      StripeDirection.DIAGONAL, // Direction
      centralSphere.getInverseTransform() // Pass inverse transform
    );
       
    // 11. Image Texture Material (Use your own image)
    BufferedImage customImage = null;
    try {
      customImage = ImageIO.read(new File("..\\textures\\elena.png"));
      System.out.println("Custom image loaded successfully for ImageTextureMaterial.");
      } catch (IOException e) {
      System.err.println("ERROR: Custom image could not be loaded for ImageTextureMaterial: " + e.getMessage());
      e.printStackTrace();
    }
    
    if (customImage != null) {
      sphereMaterial = new ImageTextureMaterial(
        customImage,
        1.0, // uScale
        1.0, // vScale
        0.0, // uOffset
        0.0, // vOffset
        0.1, // ambientCoefficient
        0.9, // diffuseCoefficient (Increased to make texture color more visible)
        0.1, // specularCoefficient (Reduced to decrease shininess)
        10.0, // shininess (Reduced to make highlights sharper)
        centralSphere.getInverseTransform() // Pass inverse transform
      );
      } else {
      sphereMaterial = new LambertMaterial(new Color(150, 150, 150)); // Fallback material
    }
    
    // 13. Bump Material (With normal map)
    BufferedImage bumpImage = null;
    ImageTexture bumpTexture = null;
    try {
      // Please replace this path with the ACTUAL path to your normal map file!
      bumpImage = ImageIO.read(new File("..\\textures\\elena.png")); // Example: textures\\brick_normal.png
      bumpTexture = new ImageTexture(bumpImage, 1.0); // ImageTexture constructor takes BufferedImage and uvScale
      System.out.println("Normal map loaded successfully for BumpMaterial.");
      } catch (IOException e) {
      System.err.println("ERROR: Normal map could not be loaded for BumpMaterial: " + e.getMessage());
      e.printStackTrace();
    }
    
    Material baseSphereMaterial = new PhongMaterial(
      new Color(150, 150, 150), // Diffuse color (Gray)
      new Color(255, 255, 255), // Specular color (White)
      50.0, 0.1, 0.7, 0.8, 0.0, 1.0, 0.0 // Phong params
    );
    
    //Material sphereMaterial;
    if (bumpTexture != null) {
      sphereMaterial = new BumpMaterial(
        baseSphereMaterial, // Base material for lighting
        bumpTexture,
        1.0, // Bump strength (1.0 for full effect)
        5.0, // UV scale for the bump texture (e.g., 5.0 for 5 repetitions per unit)
        centralSphere.getInverseTransform() // Pass the object's inverse transform
      );
      } else {
      sphereMaterial = baseSphereMaterial; // Fallback to base material
    }
    
    centralSphere.setMaterial(sphereMaterial);
    //scene.addShape(centralSphere);
    
    // --- Rectangular Prism (Central Prism) ---
    Material prismMaterial = null;
    
    RectangularPrism centralPrism = new RectangularPrism(2.0, 1.0, 1.5); // Width, Height, Depth
    centralPrism.setTransform(Matrix4.rotateX(Math.toRadians(45))); // Test Y-axis rotation
    
    prismMaterial=new TriangleMaterial(
      new Color(0, 100, 0),    // Color 1 (Dark Green)
      new Color(0, 255, 0),    // Color 2 (Bright Green)
      1.0,                     // triangleSize (Controls the size/frequency of triangles. Smaller value means more triangles.)
      0.1,                     // ambientCoefficient
      0.7,                     // diffuseCoefficient
      0.8,                     // specularCoefficient
      50.0,                    // shininess
      0.0,                     // reflectivity (opaque)
      1.0,                     // ior
      0.0,                     // transparency (opaque)
      centralPrism.getInverseTransform () // Inverse transform matrix of the sphere
    );
    
    centralPrism.setMaterial(prismMaterial); // Use the existing shapeMaterial
    scene.addShape(centralPrism);
    ////////////////////
    // --- Cylinder (Central Cylinder) ---
    Cylinder centralCylinder = new Cylinder(0.8, 2.0); // Radius, Height
    centralCylinder.setTransform(Matrix4.translate(new Vector3(0, 0.5, 0))); // Slightly above the floor, centered
    
    // Test with Checkerboard material
    Material shapeMaterial=null;
    shapeMaterial = new CheckerboardMaterial(
      new Color(255, 0, 0),    // Red
      new Color(0, 0, 255),    // Blue
      2.0,                     // Square size (frequency)
      0.1, 0.7, 0.8, 50.0, Color.WHITE, // Phong parameters
      0.0, 1.0, 0.0,           // Reflectivity, IOR, Transparency (opaque)
      centralCylinder.getInverseTransform() // Inverse transform matrix of the cylinder
    );
    
    centralCylinder.setMaterial(shapeMaterial);
    //scene.addShape(centralCylinder);
    ////////////////////
    Cone centralCone = new Cone(1.0, 2.0); // Radius, Height
    centralCone.setTransform(Matrix4.translate(new Vector3(0, 0.5, 0))); // Slightly above the floor, centered
    centralCone.setMaterial(shapeMaterial);
    //scene.addShape(centralCone);
    ///////////////////
    Triangle centralTriangle = new Triangle(
      new Point3(0.0, 1.0, 0.0),   // Top vertex
      new Point3(-1.0, -1.0, 0.0), // Bottom-left vertex
      new Point3(1.0, -1.0, 0.0)   // Bottom-right vertex
    );
    centralTriangle.setTransform(Matrix4.translate(new Vector3(0, 1.0, 0))); // Position the triangle in world space
    
    centralTriangle.setMaterial(shapeMaterial);
    //scene.addShape(centralTriangle);
    //////////////////
    Torus centralTorus = new Torus(1.0, 0.3); // Major Radius, Minor Radius
    centralTorus.setTransform(Matrix4.translate(new Vector3(0, 0.5, 0)).multiply(Matrix4.rotateZ(Math.toRadians(45))));
    
    centralTorus.setMaterial(shapeMaterial);
    //scene.addShape(centralTorus);
    //////////////////
    
    System.out.println("Render process starting...");
    long startTime = System.currentTimeMillis();
    
    // 6. Render the Ray Tracer
    BufferedImage renderedImage = rayTracer.render();
    
    long endTime = System.currentTimeMillis();
    System.out.println("Render process completed. Duration: " + (endTime - startTime) + " ms");
    
    // 7. Save the Image
    try {
      File outputFile = new File("..\\images\\revised_scene.png"); // Different filename used
      ImageIO.write(renderedImage, "png", outputFile);
      System.out.println("Image saved successfully: " + outputFile.getAbsolutePath());
      } catch (IOException e) {
      System.err.println("An error occurred while saving the image: " + e.getMessage());
      e.printStackTrace();
    }
  }
  
  final public static void main(final String[] args) {
    try {
      // For full render:
      generateSaveRenderedImage();
      
      } catch (IOException ioe) {
      ioe.printStackTrace();
      System.exit(-1);
    }
  }
  
}
