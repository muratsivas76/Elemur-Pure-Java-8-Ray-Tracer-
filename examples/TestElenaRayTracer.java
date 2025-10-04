//package net.elena.murat.lovert;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.Optional;

//custom classes
import net.elena.murat.shape.*;
import net.elena.murat.lovert.*;
import net.elena.murat.material.*;
import net.elena.murat.math.*;
import net.elena.murat.light.*;
import net.elena.murat.util.*;

final
public class TestElenaRayTracer
extends Object
//implements java.io.Serializable // Uncomment if serialization is actually used
{
  
  /**
   * Private constructor to prevent instantiation of this utility class.
   */
  private TestElenaRayTracer ()
  {
    super ();
  }
  
  /**
   * Placeholder for a clone method if needed, currently commented out.
   * protected Object clone ()
   * {
   * return null;
   * }
   */
  
  /**
   * Returns a string representation of this object.
   * @return A string "TestElenaRayTracer".
   */
  public String toString ()
  {
    return "TestElenaRayTracer";
  }
  
  /**
   * Generates a ray-traced image and saves it to a file.
   * @throws IOException If there is an error during image file operations.
   */
  final
  private static void generateSaveRenderedImage ()
  throws IOException
  {
    // 1. Create Scene
    Scene scene = new Scene();
    
    // 2. Create ElenaMuratRayTracer object
    int imageWidth = 800;
    int imageHeight = 600;
    Color rendererBackgroundColor = new Color(0.99f, 0.99f, 0.99f); // A very light gray background
    
    ElenaMuratRayTracer rayTracer = new ElenaMuratRayTracer(scene, imageWidth, imageHeight, rendererBackgroundColor);
    
    // 3. Adjust ElenaMuratRayTracer settings
    rayTracer.setCameraPosition(new Point3(0, 0, 5));
    rayTracer.setLookAt(new Point3(0, 0, 0));
    rayTracer.setUpVector(new Vector3(0, 1, 0));
    rayTracer.setFov(60.0);
    rayTracer.setMaxRecursionDepth(5); // Set max recursion depth for reflections/refractions
    
    // 4. Create and add lights
    // You can add or change these lights and their values.
    ElenaMuratAmbientLight ambientLight = new ElenaMuratAmbientLight(Color.WHITE, 0.1);
    MuratPointLight pointLight1 = new MuratPointLight(new Point3(-3, 5, 2), Color.WHITE, 0.7);
    ElenaDirectionalLight dirLight = new ElenaDirectionalLight(new Vector3(-1,-1,-1), Color.WHITE, 0.5);
    
    scene.addLight(ambientLight);
    scene.addLight(pointLight1);
    scene.addLight(dirLight);
    
    ///////////////////////
    ///// IMPORTANT BLOCK ////
    ///// You add your own shapes here.
    // 5. Create and add shapes
    
    // --- Example Phong Material (Red Glossy) ---
    PhongMaterial redGlossy = new PhongMaterial(
      new Color(255, 0, 0),      // Diffuse color (Red)
      new Color(255, 255, 255),  // Specular color (White)
      50.0,                      // Shininess
      0.1,                       // Ambient coefficient
      0.7,                       // Diffuse coefficient
      0.8,                       // Specular coefficient
      1.0,                       // Reflectivity (fully reflective for testing)
      1.5,                       // Index of Refraction (IOR)
      0.0                        // Transparency (opaque)
    );
    
    // --- Example Phong Material (Blue Glass) ---
    PhongMaterial blueGlass = new PhongMaterial(
      new Color(100, 100, 255),  // Diffuse color (Light Blue)
      new Color(255, 255, 255),  // Specular color (White)
      150.0,                     // Shininess
      0.1,                       // Ambient coefficient
      0.3,                       // Diffuse coefficient
      0.8,                       // Specular coefficient
      1.0,                       // Reflectivity (fully reflective for testing)
      1.5,                       // Index of Refraction
      1.0                        // Transparency (fully transparent for testing)
    );
    
    // --- Sphere 1: Main Sphere (using red glossy material) ---
    Sphere sphere = new Sphere(1.0); // Create in local space (radius 1.0, center at 0,0,0)
    sphere.setTransform(Matrix4.translate(new Vector3(0, 0, 0))); // Position in world space
    sphere.setMaterial(redGlossy); // Set material after transform
    scene.addShape(sphere);
    
    // --- Sphere 2: Example Sphere (using TriangleMaterial) ---
    Sphere sphere2 = new Sphere(0.8);
    sphere2.setTransform(Matrix4.translate(new Vector3(-1.5, 0.5, -1))); // Set transform BEFORE material
    // Corrected TriangleMaterial constructor call
    sphere2.setMaterial(new TriangleMaterial (Color.RED, Color.GREEN, 0.1, sphere2.getInverseTransform()));
    scene.addShape(sphere2);
    
    // --- Cylinder ---
    Cylinder cylinder = new Cylinder(0.4, 1.0); // Create in local space (radius 0.4, height 1.0, base at 0,0,0)
    cylinder.setTransform(Matrix4.translate(new Vector3(-2, -2, 1))); // Position in world space
    // Corrected TriangleMaterial constructor call
    cylinder.setMaterial(new TriangleMaterial (Color.RED, Color.GREEN, 0.1, cylinder.getInverseTransform()));
    scene.addShape(cylinder);
    
    ////////////////
    // --- Cube with Squared Material and Transformations ---
    // 1. Define the cube in its local space (e.g., a unit cube)
    Point3 cubeLocalMin = new Point3(-0.5, -0.5, -0.5);
    Point3 cubeLocalMax = new Point3(0.5, 0.5, 0.5);
    Cube squaredCube = new Cube(cubeLocalMin, cubeLocalMax);
    
    // 2. Create the transformation matrix for the cube (e.g., translation, rotation, scaling)
    Vector3 translateVec = new Vector3(0.0, 0.0, 0.0); // Cube will be centered at (0,0,0) initially
    Vector3 scaleVec = new Vector3(1.0, 1.0, 1.0); // Unit cube
    double rotateXDegrees = 45.0;  // Rotate around X-axis to show the bottom (45 derece öne eğdik)
    double rotateYDegrees = 30.0;  // Rotate around Y-axis (yatay dönüşü koruduk)
    double rotateZDegrees = 0.0;   // Rotate around Z-axis
    
    Matrix4 translateMatrix = Matrix4.translate(translateVec);
    Matrix4 rotateXMatrix4 = Matrix4.rotateX(rotateXDegrees);
    Matrix4 rotateYMatrix4 = Matrix4.rotateY(rotateYDegrees);
    Matrix4 rotateZMatrix4 = Matrix4.rotateZ(rotateZDegrees);
    Matrix4 scaleMatrix = Matrix4.scale(scaleVec.x, scaleVec.y, scaleVec.z);
    
    // Transformation order: Scale -> Rotate -> Translate (T * R * S)
    // Dönüşüm sırası Z -> Y -> X olarak ayarlandı, bu genellikle daha öngörülebilir sonuçlar verir.
    Matrix4 cubeTransform = translateMatrix.multiply(rotateYMatrix4).multiply(rotateXMatrix4).multiply(rotateZMatrix4).multiply(scaleMatrix);
    
    // 4. Assign the transformation matrix to the cube
    squaredCube.setTransform(cubeTransform);
    
    // 3. Create the Squared Material with Phong parameters
    // Now pass the cube's INVERSE transformation matrix (Matrix4) to the material
    SquaredMaterial squaredMaterial = new SquaredMaterial(
      new Color(0.9f, 0.1f, 0.1f), // Color1 (Reddish)
      new Color(0.1f, 0.1f, 0.9f), // Color2 (Blueish)
      4.0,                         // Square size (frequency: 4.0 means 4 squares per unit length)
      0.1, 0.7, 0.8, 50.0, Color.WHITE, // ambient, diffuse, specular, shininess, specularColor (Phong parameters)
      0.0,                         // Reflectivity
      1.0,                         // IOR
      0.0,                         // Transparency
      squaredCube.getInverseTransform() // Pass the full inverse transform of the cube
    );
    squaredCube.setMaterial(squaredMaterial);
    
    // 5. Add the cube to the scene
    scene.addShape(squaredCube);
    /////////////////
    
    // --- Circle Texture Test Sphere ---
    Sphere circleTestSphere = new Sphere(0.7); // Create sphere first
    circleTestSphere.setTransform(Matrix4.translate(new Vector3(1.5, -0.5, -0.5))); // Set transform BEFORE material
    // Corrected CircleTextureMaterial constructor call
    circleTestSphere.setMaterial(new CircleTextureMaterial(
        Color.RED,         // Background color
        Color.GREEN,       // Circles color
        0.99,              // patternSize: Larger value means fewer, larger circles.
        circleTestSphere.getInverseTransform() // Add objectInverseTransform
    ));
    scene.addShape(circleTestSphere);
    
    // --- Striped Sphere ---
    Sphere stripedSphere = new Sphere(0.9); // Create sphere first
    stripedSphere.setTransform(Matrix4.translate(new Vector3(-3.0, 1.0, 0.0))); // Set transform BEFORE material
    // Corrected StripedMaterial constructor call
    stripedSphere.setMaterial(new StripedMaterial(
        Color.WHITE,
        Color.BLACK,
        0.3,
        StripeDirection.VERTICAL,
        stripedSphere.getInverseTransform() // Add objectInverseTransform
    ));
    scene.addShape(stripedSphere);
    
    // --- Cone ---
    double coneRadius = 0.5;
    double coneHeight = 1.5;
    Material coneMaterial = new LambertMaterial(new Color(50, 200, 50)); // Green color (RGB 0-255)
    Cone myCone = new Cone(coneRadius, coneHeight);
    myCone.setTransform(Matrix4.translate(new Vector3(0, -0.5, -0.5))); // Position in world space
    myCone.setMaterial (coneMaterial); // Set material after transform
    scene.addShape(myCone);
    
    // --- Zemin ---
    // Define Plane in local space (e.g., at (0,0,0) with (0,1,0) normal)
    Plane floorPlane = new Plane(new Point3(0, 0, 0), new Vector3(0, 1, 0));
    // Position the Plane in world space (y = -1.7)
    floorPlane.setTransform(Matrix4.translate(new Vector3(0, -1.7, 0)));
    CheckerboardMaterial floorMaterial = new CheckerboardMaterial(
      new Color(100, 100, 100), // color1 (Dark gray)
      new Color(200, 200, 200), // color2 (Light gray)
      4.0, // size (Scale: 4 squares per unit length) - Used 4.0 for more distinct squares
      0.1, // ambientCoefficient
      0.8, // diffuseCoefficient
      0.2, // specularCoefficient
      10.0, // shininess
      Color.WHITE, // specularColor
      0.0, // reflectivity (floor should not reflect)
      1.0, // indexOfRefraction
      0.0, // transparency
      floorPlane.getInverseTransform() // IMPORTANT: Materyale objenin TERS dönüşüm matrisini iletiyoruz
    );
    floorPlane.setMaterial(floorMaterial);
    scene.addShape(floorPlane);
    
    ////////////////
    // NEW: Right Wall Plane (X=5, normal (-1,0,0)) - A wall along the X-axis
    Plane rightWallPlane = new Plane(new Point3(0, 0, 0), new Vector3(-1, 0, 0)); // Normal points inwards
    Matrix4 rightWallTransform = Matrix4.translate(new Vector3(5, 0, 0));
    rightWallPlane.setTransform(rightWallTransform);
    SquaredMaterial rightWallMaterial = new SquaredMaterial(
      new Color(0.3f, 0.0f, 0.0f), // Dark red
      new Color(1.0f, 0.0f, 0.0f), // Bright red
      4.0,                         // Square size (frequency)
      0.1, 0.7, 0.8, 50.0, Color.WHITE, // Phong parameters
      0.0, 1.0, 0.0,
      rightWallPlane.getInverseTransform() // IMPORTANT: Pass the object's inverse transform
    );
    rightWallPlane.setMaterial(rightWallMaterial);
    scene.addShape(rightWallPlane);
    ////////////////
    
    // Left Wall Plane (X=-5, normal (1,0,0)) - Original left wall
    Plane leftWallPlane = new Plane(new Point3(0, 0, 0), new Vector3(1, 0, 0)); // Normal points inwards
    Matrix4 leftWallTransform = Matrix4.translate(new Vector3(-5, 0, 0));
    leftWallPlane.setTransform(leftWallTransform);
    SquaredMaterial leftWallMaterial = new SquaredMaterial(
      new Color(0.0f, 0.3f, 0.0f), // Dark green
      new Color(0.0f, 1.0f, 0.0f), // Bright green
      4.0,                         // Square size (frequency)
      0.1, 0.7, 0.8, 50.0, Color.WHITE, // Phong parameters
      0.0, 1.0, 0.0,
      leftWallPlane.getInverseTransform() // IMPORTANT: Pass the object's inverse transform
    );
    leftWallPlane.setMaterial(leftWallMaterial);
    scene.addShape(leftWallPlane);
    /////////////
    ////////////////////
    
    /////////////////////////
    //// End of blog 5 //////
    //////////////////////////
    
    System.out.println("Render process starting...");
    long startTime = System.currentTimeMillis();
    
    // 6. Render Ray Tracer
    BufferedImage renderedImage = rayTracer.render();
    
    long endTime = System.currentTimeMillis();
    System.out.println("Render process completed. Duration: " + (endTime - startTime) + " ms");
    
    // 7. Save it to file.
    try {
      File outputFile = new File("..\\images\\test_scene.png");
      ImageIO.write(renderedImage, "png", outputFile);
      System.out.println("Image saved successfully: " + outputFile.getAbsolutePath());
      } catch (IOException ioe) {
      System.err.println("An error occurred while saving the image: " + ioe.getMessage());
      ioe.printStackTrace();
    }
    
    return;
  }
  
  /**
   * The main method to run the ray tracer.
   * @param args Command line arguments (not used).
   */
  final
  public static void main (final String [] args)
  {
    try
    {
      generateSaveRenderedImage ();
      return;
    }
    catch (IOException ioe)
    {
      ioe.printStackTrace ();
      System.exit (-1);
    }
  }
  
}
