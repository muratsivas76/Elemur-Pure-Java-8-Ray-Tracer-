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

final public class XTracer extends Object {
  
  private XTracer() {
    super();
  }
  
  public String toString() {
    return "XTracer";
  }
  
  final private static void generateSaveRenderedImage() throws IOException {
    // 1. Create the Scene
    Scene scene = new Scene();
    
    // 2. Create the Ray Tracer object
    int imageWidth = 800;
    int imageHeight = 600;
    Color rendererBackgroundColor = new Color(0.99f, 0.99f, 0.99f);
    
    ElenaMuratRayTracer rayTracer = new ElenaMuratRayTracer(scene, imageWidth, imageHeight, rendererBackgroundColor);
    
    // 3. Set Ray Tracer settings
    rayTracer.setCameraPosition(new Point3(0, 0, 10));
    rayTracer.setLookAt(new Point3(0, 0, 0));
    rayTracer.setUpVector(new Vector3(0, 1, 0));
    rayTracer.setOrthographic(false);
    rayTracer.setFov(45.0);
    rayTracer.setMaxRecursionDepth(3);
    
    // Reduce ambient light (0.2-0.5 is ideal)
    Light ambientLight = new ElenaMuratAmbientLight(new Color(0.9f, 0.95f, 1.0f), 0.3);
    
    // Adjust point light intensity to reasonable level (1.0-2.0 range)
    Light pointLight = new MuratPointLight(new Point3(0, 2, 1), new Color(1.0f, 0.95f, 0.9f), 1.5);
    
    // Set directional light intensity
    Light dirLight = new ElenaDirectionalLight(new Vector3(-0.3, -1.0, -0.3).normalize(),
    new Color(0.95f, 0.95f, 1.0f), 1.0);
    
    scene.addLight(ambientLight);
    scene.addLight(pointLight);
    scene.addLight(dirLight);
    
    // --- Plane (Floor) ---
    Plane floorPlane = new Plane(new Point3(0, 0, 0), new Vector3(0, 1, 0));
    floorPlane.setTransform(Matrix4.translate(new Vector3(0, -1.0, 0)));
    CheckerboardMaterial floorMaterial = new CheckerboardMaterial(
      new Color(20, 20, 20),
      new Color(245, 245, 245),
      2.0,
      0.1, 0.7, 0.2, 10.0, Color.WHITE,
      0.0, 1.0, 0.0,
      floorPlane.getInverseTransform()
    );
    floorPlane.setMaterial(floorMaterial);
    
    // --- Rectangular Prism (Central Prism) for Rotation Test) ---
    RectangularPrism centralPrism = new RectangularPrism(2.0, 1.0, 1.5); // Width, Height, Depth
    // First, set the prism's transform
    double prismAngle = 45.0;
    centralPrism.setTransform(Matrix4.rotateZ(prismAngle)); // NOTE: Math.toRadians REMOVED
    
    // Initialize the material AFTER the prism's transform is set,
    // and pass the prism's ACTUAL inverse transform matrix to the material.
    Material prismMaterial = new TriangleMaterial(
      new Color(0, 100, 0),    // Color 1 (Dark Green)
      new Color(0, 255, 0),    // Color 2 (Bright Green)
      1.0,                     // triangleSize
      0.1,                     // ambientCoefficient
      0.7,                     // diffuseCoefficient
      0.8,                     // specularCoefficient
      50.0,                    // shininess
      0.0,                     // reflectivity (opaque)
      1.0,                     // ior
      0.0,                     // transparency (opaque)
      centralPrism.getInverseTransform() // Pass the prism's ACTUAL inverse transform
    );
    centralPrism.setMaterial(prismMaterial);
    
    // --- Torus (Rotation Test) ---
    Torus centralTorus = new Torus(1.0, 0.3); // Major Radius, Minor Radius
    double torusAngle = 45.0;
    // Apply both translation and rotation around Z-axis to the torus
    Matrix4 torusTransform = Matrix4.translate(new Vector3(0, 0.5, -3.0)).multiply(Matrix4.rotateY(torusAngle)); // NOTE: Math.toRadians REMOVED
    centralTorus.setTransform(torusTransform);
    
    // Torus material
    Material torusMaterial = new CheckerboardMaterial(
      new Color(255, 255, 0),    // Yellow
      new Color(0, 255, 255),    // Cyan
      0.5,                       // Square size
      0.1, 0.7, 0.8, 50.0, Color.WHITE,
      0.0, 1.0, 0.0,
      centralTorus.getInverseTransform() // Pass the torus's inverse transform
    );
    centralTorus.setMaterial(torusMaterial);
    
    // --- Cube (Rotation Test - In a Separate, Clear Location) ---
    Point3 cubeLocalMin = new Point3(-0.5, -0.5, -0.5);
    Point3 cubeLocalMax = new Point3(0.5, 0.5, 0.5);
    Cube centralCube = new Cube(cubeLocalMin, cubeLocalMax); // Cube definition with min/max as desired
    double cubeAngle = 45.0;
    // Move the cube to a different location from prism and torus, e.g., (3, 0.5, 3)
    Matrix4 cubeTransform = Matrix4.translate(new Vector3(3.0, 0.5, 3.0)).multiply(Matrix4.rotateX(cubeAngle)); // NOTE: Math.toRadians REMOVED
    centralCube.setTransform(cubeTransform);
    
    Material cubeMaterial = new CheckerboardMaterial(
      new Color(255, 0, 0),    // red
      new Color(0, 200, 0),    // green
      1.0,                     // Square size
      0.1, 0.7, 0.8, 50.0, Color.WHITE,
      0.0, 1.0, 0.0,
      centralCube.getInverseTransform()
    );
    centralCube.setMaterial(cubeMaterial);
    
    ////////////////
    Material shapeMaterial=null;
    
    Cone cone = new Cone(0.7, 1.5); // radius, height
    double coneAngle = 45.0;
    Matrix4 coneTransform = Matrix4.translate(new Vector3(0, 1.0, 3.0)).multiply(Matrix4.rotateZ(coneAngle));
    cone.setTransform(coneTransform);
    
    shapeMaterial = new CheckerboardMaterial(
      new Color(255, 0, 0),    // red
      new Color(0, 200, 0),    // green
      1.0,                     // Square size
      0.1, 0.7, 0.8, 50.0, Color.WHITE,
      0.0, 1.0, 0.0,
      cone.getInverseTransform() // Cone's own inverse transform used
    );
    cone.setMaterial(shapeMaterial);
    ////////////////
    EMShape newCylinder = new Cylinder(0.6, 2.0); // radius, height
    double cylinderAngle = 90.0; // Example rotation angle
    Matrix4 cylinderTransform = Matrix4.translate(new Vector3(-3.0, 0.0, -3.0))
    .multiply(Matrix4.rotateX(90)) // First rotate around X axis
    .multiply(Matrix4.rotateY(45));
    newCylinder.setTransform(cylinderTransform);
    
    shapeMaterial = new CheckerboardMaterial(
      new Color(255, 0, 0),    // red
      new Color(0, 200, 0),    // green
      1.0,                     // Square size
      0.1, 0.7, 0.8, 50.0, Color.WHITE,
      0.0, 1.0, 0.0,
      newCylinder.getInverseTransform() // Cone's own inverse transform used
    );
    newCylinder.setMaterial(shapeMaterial);
    //////////////////
    // Local vertices for a simple triangle (e.g., on XY plane)
    Point3 triV0 = new Point3(-2.0, 0.0, 0.0); // Wider base
    Point3 triV1 = new Point3(2.0, 0.0, 0.0);
    Point3 triV2 = new Point3(0.0, 4.0, 0.0); // Taller
    EMShape newTriangle = new Triangle(triV0, triV1, triV2);
    
    Matrix4 triangleTransform = Matrix4.translate(new Vector3(0.0, 1.0, -2.0))
    .multiply(Matrix4.rotateY(45.0));
    newTriangle.setTransform(triangleTransform);
    
    shapeMaterial = new CheckerboardMaterial(
      new Color(255, 0, 0),    // red
      new Color(0, 200, 0),    // green
      4,                    // Square size
      0.1, 0.7, 0.8, 50.0, Color.WHITE,
      0.0, 1.0, 0.0,
      new Matrix4()
    );
    newTriangle.setMaterial(shapeMaterial);
    
    //ELLIPSOID
    Material greenEllipsoidMaterial = new PhongMaterial(
      new Color(50, 205, 50), // Bright green color
      Color.WHITE, 60.0,      // White specular color, high shininess
      0.1, 0.9, 0.9,          // Ambient, Diffuse, Specular coefficients
      0.0, 1.0, 0.0           // Reflection, refractive index, transparency (opaque)
    );
    
    ////////////////////
    // a. Create ellipsoid center point
    Point3 center = new Point3(0, 0, 0);
    
    // b. Set semi-axis lengths (a: x-axis, b: y-axis, c: z-axis)
    double a = 2.0; // x-axis radius
    double b = 1.5; // y-axis radius
    double c = 1.0; // z-axis radius
    
    // c. Create transformation matrix (optional)
    Matrix4 transform = Matrix4.identity()
    .multiply(Matrix4.translate(new Vector3(1, 0, 0))) // Translate 1 unit on x-axis
    .multiply(Matrix4.rotateY(30)) // Rotate 30 degrees around y-axis
    .multiply(Matrix4.scale(1.2, 1.0, 0.8)); // Scale
    
    // d. Create material
    Material material = greenEllipsoidMaterial;
    
    // e. Create ellipsoid object
    EMShape ellipsoid = new Ellipsoid(center, a, b, c);
    ellipsoid.setTransform(transform);
    ellipsoid.setMaterial(material);
    
    ////////////////
    Sphere testSphere = new Sphere(3.5); // Sphere with radius from parameter
    //////////////
    Material simpleGlass = new CrystalClearMaterial(
      new Color(20, 20, 255), // Blue-green tone
      0.5,        // Frosted appearance
      2.0,        // Dense glass
      0.10,       // Pronounced color dispersion
      testSphere.getInverseTransform()
    );
    
    testSphere.setMaterial(simpleGlass);
    
    ////////////
    try {
      Material elenaMat = new ElenaTextureMaterial(
        "..\\textures\\elena.png",
        testSphere.getInverseTransform()
      );
      } catch (IOException e) {
      System.err.println("Texture loading error: " + e.getMessage());
    }
    
    ///////////////////
    Plane plane=null;
    
	Material elenaMat = new LambertMaterial (Color.GRAY);
      
	// Create plane (on XZ plane)
    plane = new Plane(new Point3(0,-1,0), new Vector3(0,1,0));
      
    plane.setMaterial(elenaMat);
    
    Material chessMat = new GradientChessMaterial(
      new Color(50, 50, 200),   // Blue
      new Color(200, 50, 50),   // Red
      1.0,                      // Square size
      plane.getInverseTransform()
    );
    
    Material pastelMat = new GradientChessMaterial(
      new Color(255, 0, 0), // Pink
      new Color(0, 0, 255), // Blue
      0.5,                      // Smaller squares
      plane.getInverseTransform()
    );
    
    Material horizontalRects = new RectangleCheckerMaterial(
      new Color(200, 150, 50), // Beige
      new Color(50, 100, 50),  // Dark green
      1.5, 0.8,               // Dimensions
      0.15, 0.8, 0.3,         // Ambient, Diffuse, Specular
      25.0, 0.1,              // Shininess, Reflectivity
      1.3, 0.02,              // IOR, Transparency
      plane.getInverseTransform()
    );
    
    Material wood = new WoodMaterial(
      new Color(139, 69, 19),    // baseColor: Brown (classic wood color)
      new Color(101, 67, 33),    // grainColor: Darker vein color
      0.5,                       // grainFrequency: Vein density (0.5 medium)
      0.3,                       // ringVariation: Annual ring variation
      Matrix4.scale(0.1, 0.1, 0.1) // objectInverseTransform: Scale down wood texture
      .multiply(Matrix4.rotateY(Math.toRadians(45))) // Rotate texture by 45°
    );
    
    Material platinum=new PlatinumMaterial(plane.getInverseTransform());
    
    Material marble = new MarbleMaterial(
      new Color(230, 22, 21), // baseColor (white marble)
      new Color(10, 180, 10), // veinColor (gray veins)
      0.05, // scale
      1.5,  // veinDensity
      3.0,  // turbulence
      0.3,  // ambient
      0.6,  // diffuse
      0.4,  // specular
      25.0, // shininess
      0.1,  // reflectivity
      1.5,  // ior
      0.0,  // transparency
      Matrix4.scale(0.1, 0.1, 0.1)
    );
    
    Material dewMat = new DewDropMaterial(
      new Color(50, 120, 50), // Leaf color
      new Color(200, 230, 255, 150), // Semi-transparent dew color
      0.7, // Density (70%)
      0.03, // Droplet size
      plane.getInverseTransform()
    );
    
    Material honeycombMat=new HexagonalHoneycombMaterial(
      new Color(255, 215, 0), // Gold yellow
      new Color(255, 255, 150), // Light yellow
      0.5,  // Cell size (scale factor)
      0.05  // Edge thickness
    );
    
    Material honeycomb = new HexagonalHoneycombMaterial(
      new Color(255, 239, 153), // Light yellow (honey color)
      new Color(255, 204, 51),  // Gold yellow
      new Color(50, 50, 50),    // Dark gray edges
      0.3,                      // Cell size
      0.05,                     // Edge thickness
      0.2,                      // Ambient intensity
      0.3,                      // Specular
      16.0                      // Shininess
    );
    
    Material illusionMat = new OpticalIllusionMaterial(
      Color.BLACK,
      Color.WHITE,
      5.0,  // Ring frequency
      0.2,  // Transition smoothness
      plane.getInverseTransform()
    );
    
    Material hypnoMat = new OpticalIllusionMaterial(
      new Color(0, 100, 255), // Blue
      new Color(255, 50, 0),   // Orange
      8.0,  // Denser rings
      0.1,  // Sharp transitions
      plane.getInverseTransform()
    );
    
    Material bluePhong = new PhongElenaMaterial(
      Color.BLUE, // Main color
      0.3,        // Reflection coefficient (0-1)
      50          // Shininess
    );
    
    Material redPhong = new PhongElenaMaterial(
      Color.RED,
      0.5,
      100,
      0.2 // Ambient coefficient
    );
    
    Material goldPhong = new PhongElenaMaterial(
      new Color(255, 215, 0), // Gold color
      0.8, // Strong reflection
      150  // High shininess
    );
    
    Color[] retroPalette = {
      new Color(255, 0, 0),    // Red
      new Color(0, 255, 0),    // Green
      new Color(0, 0, 255),    // Blue
      new Color(255, 255, 0)   // Yellow
    };
    
    Material pixelMat = new PixelArtMaterial(
      retroPalette,
      0.3, // Pixel size
      plane.getInverseTransform()
    );
    
    Color[] gameboyPalette = {
      new Color(15, 56, 15),   // Dark green
      new Color(48, 98, 48),    // Medium green
      new Color(139, 172, 15),  // Light green
      new Color(155, 188, 15)   // Highlight green
    };
    
    PixelArtMaterial gbMat = new PixelArtMaterial(
      gameboyPalette,
      0.5, // Larger pixels
      plane.getInverseTransform()
    );
    
    Material flowerMat = new ProceduralFlowerMaterial(
      5,            // 5 petals
      Color.RED,    // Petal color
      Color.YELLOW  // Center color
    );
    
    plane.setMaterial(flowerMat);
    
    scene.addShape(plane);
    
    Hyperboloid hpb=new Hyperboloid();
    hpb.setMaterial(elenaMat);
    
    ////////////////
    EMShape trefoil = new TorusKnot(2.0, 1, 3, 5);
    trefoil.setTransform(Matrix4.translate(0, 1, 0).multiply(Matrix4.scale(1.1, 1.1, 1.1)));
    trefoil.setMaterial(elenaMat);
    
    ///////////////
    EMShape crescent = new Crescent(
      2.0,    // Main radius
      1.8,    // Cut radius
      0.5     // Distance between centers
    );
    
    // Set material and transform as needed
    crescent.setMaterial(new LambertMaterial(Color.YELLOW));
    crescent.setTransform(Matrix4.rotateY(Math.PI/4));
    
    System.out.println("Render process starting...");
    long startTime = System.currentTimeMillis();
    
    // 6. Render the Ray Tracer
    BufferedImage renderedImage = rayTracer.render();
    
    long endTime = System.currentTimeMillis();
    System.out.println("Render process completed. Duration: " + (endTime - startTime) + " ms");
    
    // 7. Save the Image
    try {
      File outputFile = new File("..\\images\\xtracer_scene.png");
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
