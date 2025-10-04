/*
 * License GNU General Public License v3.0
 * @see <a href="https://www.gnu.org/licenses/gpl-3.0.en.html">GPL v3 License</a>
 */
package net.elena.murat.lovert;

// Java native imports
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Random;

// Custom imports
import net.elena.murat.shape.*;
import net.elena.murat.shape.letters.*;
import net.elena.murat.material.*;
import net.elena.murat.material.pbr.*;
import net.elena.murat.math.*;
import net.elena.murat.light.*;
import net.elena.murat.util.*;

/**
 * <h1>TestTracer - Complete Ray Tracing Demo</h1>
 *
 * <div class="block">
 * Full demonstration of the ray tracing engine including all setup steps.
 * </div>
 *
 * <h2>Compilation and Execution</h2>
 * <pre>
 * {@code
 * // Compile with:
 * javac -cp "bin\elenaRT.jar" TestTracer.java
 *
 * // Execute with:
 * java -cp "bin\elenaRT.jar"; TestTracer
 * }
 * </pre>
 *
 * <h2>Complete Implementation</h2>
 * <pre>
 * {@code
package net.elena.murat.lovert;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.io.File;
import java.io.IOException;

// Custom imports
import net.elena.murat.shape.*;
import net.elena.murat.lovert.*;
import net.elena.murat.material.*;
import net.elena.murat.material.pbr.*;
import net.elena.murat.math.*;
import net.elena.murat.light.*;
import net.elena.murat.util.*;

final public class TestTracer {

private TestTracer() {
super();
}

public String toString() {
return "TestTracer";
}

final private static void generateSaveRenderedImage(String[] args) throws IOException {
// 1. Scene creation
Scene scene = new Scene();

// 2. Ray tracer config (scene, width, height, backgroundColor)
ElenaMuratRayTracer rayTracer = new ElenaMuratRayTracer(scene, 800, 600, new Color(1f, 1f, 1f));

// 3. Camera setup
Camera camera = new Camera();
camera.setCameraPosition(new Point3(0, 0, 5));
camera.setLookAt(new Point3(0, 0, 0));
camera.setUpVector(new Vector3(0, 1, 0));
camera.setFov(60.0);
camera.setOrthographic(false);
camera.setReflective(true);
camera.setRefractive(true);
camera.setShadowsEnabled(true);
camera.setMaxRecursionDepth(2);

rayTracer.setCamera(camera);

// 4. Lighting
scene.addLight(new ElenaMuratAmbientLight(Color.WHITE, 0.5));
scene.addLight(new MuratPointLight(new Point3(-1, 1, 2), Color.WHITE, 1.0));
scene.addLight(new ElenaDirectionalLight(new Vector3(0,-1,0), Color.WHITE, 1.5));

// 5. Shapes/materials
EMShape ground = new Plane(new Point3(0, 0, 0), new Vector3(0, 1, 0));
ground.setTransform(Matrix4.translate(new Vector3(0, -1.70, 0)));
ground.setMaterial(new CheckerboardMaterial(
new Color(30, 30, 30), new Color(80, 80, 80), 0.4, 1.25, 0.5, 0.3, 8.0,
new Color(255, 230, 180), 0.25, 1.0, 0.0, ground.getInverseTransform()));
scene.addShape(ground);

// 6. Rendering
System.out.println("\n=== RENDERING STARTED ===");
long startTime = System.currentTimeMillis();
BufferedImage renderedImage = rayTracer.render();
long endTime = System.currentTimeMillis();
System.out.println("Rendering completed in " + (endTime - startTime) + " ms.");

// 7. Save image
String filename = "images\\example.png";
File outputFile = new File(filename);
ImageIO.write(renderedImage, "png", outputFile);
System.out.println("Image saved: " + outputFile.getAbsolutePath());
}

public static void main(String[] args) {
try {
  generateSaveRenderedImage(args);
} catch (IOException ioe) {
ioe.printStackTrace();
System.exit(-1);
}
}
}
 * }
 * </pre>
 *
 * @author Murat iNAN, muratsivas76@gmail.com
 * @version 1.0
 * @since 2023-11-15
 */

public class ElenaMuratRayTracer {
  private Scene scene;
  private int width;
  private int height;
  private Color backgroundColorAWT;
  private FloatColor backgroundColorFloat;
  private int maxRecursionDepth = 5;
  
  private Camera camera = new Camera();
  
  private Point3 cameraPosition;
  private Point3 lookAt;
  private Vector3 upVector;
  private double fov;
  private boolean isOrthographic;
  private boolean isReflective;
  private double orthographicScale = 2.0;
  
  public ElenaMuratRayTracer(Scene scene, int width, int height,
    Color backgroundColor) {
    this.scene = scene;
    this.width = width;
    this.height = height;
    this.backgroundColorAWT = backgroundColor;
    this.backgroundColorFloat = new FloatColor(backgroundColor.getRed() / 255.0,
      backgroundColor.getGreen() / 255.0,
    backgroundColor.getBlue() / 255.0);
    this.cameraPosition = new Point3(0, 0, 5);
    this.lookAt = new Point3(0, 0, 0);
    this.upVector = new Vector3(0, 1, 0);
    this.fov = 60.0;
    this.isOrthographic = false;
    this.isReflective = true;
  }
  
  public void setMaxRecursionDepth(int depth) {
    this.maxRecursionDepth = Math.max(1, Math.min(7, depth));
    camera.setMaxRecursionDepth(this.maxRecursionDepth);
  }
  
  public void setCameraPosition(Point3 position) {
    this.cameraPosition = position;
    camera.setCameraPosition(this.cameraPosition);
  }
  
  public void setLookAt(Point3 lookAt) {
    this.lookAt = lookAt;
    camera.setLookAt(this.lookAt);
  }
  
  public void setUpVector(Vector3 upVector) {
    this.upVector = upVector;
    camera.setUpVector(this.upVector);
  }
  
  public void setReflective(boolean isrf) {
    this.isReflective = isrf;
    camera.setReflective(this.isReflective);
  }
  
  public void setFov(double fov) {
    this.fov = fov;
    camera.setFov(this.fov);
  }
  
  public void setOrthographic(boolean ortho) {
    this.isOrthographic = ortho;
    camera.setOrthographic(this.isOrthographic);
  }
  
  public void setOrthographicScale(double scale) {
    this.orthographicScale = scale;
    camera.setOrthographicScale(this.orthographicScale);
  }
  
  public int getMaxRecursionDepth() {
    if (camera == null) {
      return maxRecursionDepth;
    }
    return camera.getMaxRecursionDepth();
  }
  
  public Camera getCamera() {
    return this.camera;
  }
  
  public void setCamera(Camera cmr) {
    if (cmr == null) {
      cmr = new Camera();
    }
    this.camera = cmr;
  }
  
  private Ray generateCameraRay(double screenX, double screenY) {
    Vector3 zAxis = (camera.getCameraPosition()).subtract(camera.getLookAt()).normalize();
    Vector3 xAxis = (camera.getUpVector()).cross(zAxis).normalize();
    Vector3 yAxis = zAxis.cross(xAxis).normalize();
    
    if (camera.isOrthographic()) {
      double worldScreenWidth = camera.getOrthographicScale();
      double worldScreenHeight = (camera.getOrthographicScale()) / ((double)width/height);
      Point3 rayOrigin = (camera.getCameraPosition())
      .add(xAxis.scale(screenX*worldScreenWidth/2.0))
      .add(yAxis.scale(screenY*worldScreenHeight/2.0));
      return new Ray(rayOrigin, zAxis.negate());
      } else {
      double aspectRatio = (double)width/height;
      double tanHalfFov = Math.tan(Math.toRadians(fov)/2);
      Vector3 rayDir = xAxis.scale(screenX*aspectRatio*tanHalfFov)
      .add(yAxis.scale(screenY*tanHalfFov))
      .subtract(zAxis)
      .normalize();
      return new Ray(camera.getCameraPosition(), rayDir);
    }
  }
  
  public BufferedImage render() {
    BufferedImage image = new BufferedImage(width, height,
    BufferedImage.TYPE_INT_ARGB);
    
    FloatColor cxx = FloatColor.BLACK;
    
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        double ndcX = (x + 0.5)/width;
        double ndcY = (y + 0.5)/height;
        double screenX = 2*ndcX - 1;
        double screenY = 1 - 2*ndcY;
        
        Ray ray = generateCameraRay(screenX, screenY);
        
        cxx = traceRay(ray, 0, 1.0);
        
        image.setRGB(x, y, cxx.toARGB());
      }
    }
    return image;
  }
  
  private FloatColor traceRay(Ray ray, int depth, double attenuationFactor) {
    // 1. Check depth and attenuation factor
    if ((depth > camera.getMaxRecursionDepth()) || attenuationFactor < 1e-30) {
      return depth == 0 ? backgroundColorFloat : FloatColor.BLACK;
    }
    
    // 2. Intersection test
    Optional<Intersection> hit = findClosestIntersection(ray);
    if (!hit.isPresent()) {
      return depth == 0 ? backgroundColorFloat : FloatColor.BLACK;
    }
    
    Intersection intersection = hit.get();
    EMShape shape = intersection.getShape();
    Material material = shape.getMaterial();
    Point3 hitPoint = intersection.getPoint();
    Vector3 normal = intersection.getNormal().normalize();
    
    if (material == null) {
      if (Math.random() < 0.001) {
        System.out.println("WARNING: NULL MATERIAL converted to DIFFUSEMATERIAL: " + shape.toString() + "");
      }
      material = new DiffuseMaterial(Color.RED);
    }
    
    // Normal direction correction
    boolean entering = ray.getDirection().dot(normal) < 0;
    Vector3 N = entering ? normal : normal.negate();
    
    // 3. Process based on material type
    if (material instanceof EmissiveMaterial) {
      // *** EMISSIVE MATERIAL CHECK ***
      EmissiveMaterial emat = (EmissiveMaterial) material;
      return new FloatColor(emat.getEmissiveColor()).multiply(emat.getEmissiveStrength());
      } else {
      // *** GENERAL MATERIALS (NON-PBR) ***
      FloatColor finalColor = FloatColor.BLACK;
      
      // Direct lighting
      Color directLightingColor = calculateDirectLighting(hitPoint, N, material, ray);
      FloatColor directLightingFloat = new FloatColor(directLightingColor);
      finalColor = finalColor.add(directLightingFloat);
      
      // Ambient light
      for (Light light : scene.getLights()) {
        if (light instanceof ElenaMuratAmbientLight) {
          Color ambientColor = material.getColorAt(hitPoint, N, light, ray.getOrigin());
          FloatColor ambientFloat = new FloatColor(ambientColor);
          finalColor = finalColor.add(ambientFloat);
        }
      }
      
      finalColor = finalColor.multiply(attenuationFactor);
      
      // Reflection
      if (shouldCalculateReflections(material)) {
        Vector3 reflectedDir = ray.getDirection().reflect(N).normalize();
        double newReflectedAttenuation = attenuationFactor * material.getReflectivity();
        Point3 offsetPoint = hitPoint.add(N.scale(Ray.EPSILON));
        Ray reflectedRay = new Ray(offsetPoint, reflectedDir);
        FloatColor reflectedColor = traceRay(reflectedRay, depth + 1, newReflectedAttenuation);
        finalColor = finalColor.add(reflectedColor);
      }
      
      // Refraction
      if (shouldCalculateRefractions(material)) {
        double n1 = entering ? 1.0 : material.getIndexOfRefraction();
        double n2 = entering ? material.getIndexOfRefraction() : 1.0;
        
        Optional<Vector3> refractedDir = ray.getDirection().refract(N, n1, n2);
        if (refractedDir.isPresent()) {
          Point3 refractedOffsetPoint = hitPoint.add(refractedDir.get().scale(Ray.EPSILON));
          double newRefractedAttenuation = attenuationFactor * material.getTransparency();
          Ray refractedRay = new Ray(refractedOffsetPoint, refractedDir.get());
          FloatColor refractedColor = traceRay(refractedRay, depth + 1, newRefractedAttenuation);
          
          if (material instanceof GlassMaterial) {
            Color glassColor = ((GlassMaterial)material).getColorForRefraction();
            FloatColor glassTint = new FloatColor(glassColor);
            refractedColor = refractedColor.multiply(glassTint);
          }
          
          if (material instanceof DiamondMaterial) {
            Color diamondColor = ((DiamondMaterial)material).getColorForRefraction();
            FloatColor diamondTint = new FloatColor(diamondColor);
            refractedColor = refractedColor.multiply(diamondTint);
          }
          
          finalColor = finalColor.add(refractedColor);
        }
      }
      
      finalColor = new FloatColor(finalColor.r, finalColor.g, finalColor.b, finalColor.a);
      
      return finalColor.clamp01();
    }
  }
  // end of traceRay
  
  private FloatColor calculateLightingForRGB(Point3 point, Vector3 normal, Material material,
    Ray ray, double attenuation) {
    FloatColor result = new FloatColor(0, 0, 0, 0);
    
    // Direct lighting
    try {
      Color direct = calculateDirectLighting(point, normal, material, ray);
      if (direct != null) {
        FloatColor directRGB = new FloatColor(
          direct.getRed() / 255.0,
          direct.getGreen() / 255.0,
          direct.getBlue() / 255.0,
          0.0
        );
        result = result.add(directRGB);
      }
    } catch (Exception e) {}
    
    // Ambient lights
    for (Light light : scene.getLights()) {
      try {
        if (light instanceof ElenaMuratAmbientLight) {
          Color ambient = material.getColorAt(point, normal, light, ray.getOrigin());
          if (ambient != null) {
            FloatColor ambientRGB = new FloatColor(
              ambient.getRed() / 255.0,
              ambient.getGreen() / 255.0,
              ambient.getBlue() / 255.0,
              0.0
            );
            result = result.add(ambientRGB);
          }
        }
      } catch (Exception e) {}
    }
    
    return result.multiply(attenuation);
  }
  
  /**
   * Applies colored absorption to a light color based on tint.
   * Preserves the alpha (transparency) of the original color.
   *
   * @param originalColor The incoming light color (with alpha)
   * @param tintColor The glass/material tint color (used for absorption)
   * @return Absorbed color with original alpha preserved
   */
  public FloatColor applyAbsorption(FloatColor originalColor, Color tintColor) {
    // 1. Convert tint color to FloatColor
    FloatColor tint = new FloatColor(tintColor);
    
    // 2. Absorption coefficients (higher tint → less absorption)
    //    - Red tint (high R) → absorbs less red
    double absorptionR = 1.0 - tint.r;
    double absorptionG = 1.0 - tint.g;
    double absorptionB = 1.0 - tint.b;
    //double absorptionA = 1.0 - tint.a;//originalColor.a;
    
    // 3. Base transmission (e.g., 97% light passes through)
    final double TRANSMISSION = 0.97; // 3% base absorption
    
    // 4. Apply absorption to RGB components
    double r = originalColor.r * (1.0 - absorptionR * (1.0 - TRANSMISSION));
    double g = originalColor.g * (1.0 - absorptionG * (1.0 - TRANSMISSION));
    double b = originalColor.b * (1.0 - absorptionB * (1.0 - TRANSMISSION));
    //double a = originalColor.a * (1.0 - absorptionA * (1.0 - TRANSMISSION));
    
    // 5. Preserve original alpha
    //double a = originalColor.a;
    
    return new FloatColor(r, g, b).clamp01();
  }
  
  // New helper methods (CAMERA CONTROLLED)
  private boolean shouldCalculateReflections(Material material) {
    return camera.isReflective()
    && material.getReflectivity() > Ray.EPSILON;
  }
  
  private boolean shouldCalculateRefractions(Material material) {
    return camera.isRefractive()
    && material.getTransparency() > Ray.EPSILON;
  }
  
  private boolean shouldCalculateShadows() {
    return camera.isShadowsEnabled();
  }
  
  private Color calculateDirectLighting(Point3 point, Vector3 normal,
    Material material, Ray ray) {
    Color directLightingColor = new Color(0, 0, 0);
    
    for (Light light : scene.getLights()) {
      if (light instanceof ElenaMuratAmbientLight) continue;
      
      Vector3 lightDir = null;
      double distance = Double.POSITIVE_INFINITY;
      
      if (light instanceof MuratPointLight) {
        MuratPointLight ptLight = (MuratPointLight) light;
        lightDir = ptLight.getPosition().subtract(point).normalize();
        distance = ptLight.getPosition().subtract(point).length();
        } else if (light instanceof PulsatingPointLight) {
        PulsatingPointLight pulsatingLight = (PulsatingPointLight) light;
        lightDir = pulsatingLight.getPosition().subtract(point).normalize();
        distance = pulsatingLight.getDistanceTo(point);
        } else if (light instanceof ElenaDirectionalLight) {
        ElenaDirectionalLight dl = (ElenaDirectionalLight) light;
        lightDir = dl.getDirection().negate().normalize();
        distance = Double.POSITIVE_INFINITY;
        } else if (light instanceof SpotLight) {
        SpotLight s = (SpotLight) light;
        lightDir = s.getPosition().subtract(point).normalize();
        distance = s.getPosition().subtract(point).length();
        } else if (light instanceof FractalLight) {
        FractalLight f = (FractalLight) light;
        lightDir = f.getPosition().subtract(point).normalize();
        distance = f.getPosition().subtract(point).length();
        } else if (light instanceof BlackHoleLight) {
        BlackHoleLight bh = (BlackHoleLight) light;
        lightDir = bh.getPosition().subtract(point).normalize();
        distance = bh.getPosition().subtract(point).length();
        } else if (light instanceof BioluminescentLight) {
        BioluminescentLight bio = (BioluminescentLight) light;
        lightDir = bio.getDirectionAt(point);
        distance = bio.getClosestDistance(point);
      }
      
      if (lightDir == null) continue;
      
      // NON-SCALED TRANSPARENT PNG ESPECIALLY
      if (material instanceof NonScaledTransparentPNGMaterial) {
        NonScaledTransparentPNGMaterial pngMaterial = (NonScaledTransparentPNGMaterial) material;
        
        // Continue for transparent pixels
        if (!pngMaterial.hasShadowAt(point)) {
          // Add irect light if it is passing here
          Color contribution = material.getColorAt(point, normal, light, ray.getOrigin());
          directLightingColor = ColorUtil.addSafe(directLightingColor, contribution);
          continue; // skip shadow control
        }
      }
      
      // Only calculate direct lighting if not in shadow
      if (!shouldCalculateShadows() ||
        !isInShadow(point.add(normal.scale(Ray.EPSILON)), lightDir, distance)) {
        Color contribution = material.getColorAt(point, normal, light, ray.getOrigin());
        directLightingColor = ColorUtil.addSafe(directLightingColor, contribution);
      }
    }
    
    return directLightingColor;
  }
  
  private Optional<Intersection> findClosestIntersection(Ray ray) {
    EMShape closestShape = null;
    double closestDist = Double.POSITIVE_INFINITY;
    
    for (EMShape shape : scene.getShapes()) {
      double dist = shape.intersect(ray);
      if (dist > Ray.EPSILON && dist < closestDist) {
        closestDist = dist;
        closestShape = shape;
      }
    }
    
    if (closestShape == null) return Optional.empty();
    
    Point3 hitPoint = ray.pointAtParameter(closestDist);
    Vector3 normal = closestShape.getNormalAt(hitPoint);
    return Optional.of(new Intersection(hitPoint, normal, closestDist, closestShape));
  }
  
  private boolean isInShadow(Point3 point, Vector3 lightDir, double lightDistance) {
    Ray shadowRay = new Ray(point, lightDir);
    Optional<Intersection> shadowHit = findClosestIntersection(shadowRay);
    return shadowHit.isPresent() && shadowHit.get().getDistance() < lightDistance - Ray.EPSILON;
  }
  
  public static void main(String[] args) {
    // Classes for compiling all packages easily
    UnionCSG ucsg = null;
    IntersectionCSG icsg = null;
    DifferenceCSG dcsg = null;
    PulsatingPointLight ppl = null;
    BioluminescentLight blm = null;
    BlackHoleLight bhl = null;
    FractalLight flt = null;
    SpotLight spli = null;
    CircleTextureMaterial ctm = null;
    SquaredMaterial sm = null;
    SolidColorMaterial scm = null;
    StripedMaterial stma = null;
    RectangularPrism rp = null;
    LambertMaterial lm = null;
    TriangleMaterial trm = null;
    CheckerboardMaterial cm = null;
    DiamondMaterial diamond = null;
    TexturedPhongMaterial tpma = null;
    SuperBrightDebugMaterial sbdm = null;
    ImageTextureMaterial itm = null;
    MetallicMaterial mmt = null;
    RoughMaterial romu = null;
    PixelArtMaterial pam = null;
    HolographicDiffractionMaterial hdm = null;
    HolographicPBRMaterial hcpm = null;
    ChromePBRMaterial chrpmt = null;
    WaterPBRMaterial wpmcv = null;
    BlackHoleMaterial bhm = null;
    FractalBarkMaterial fbm = null;
    StarfieldMaterial sfm = null;
    ProceduralFlowerMaterial pfmtr = null;
    DamaskCeramicMaterial dcm = null;
    LavaFlowMaterial lfm = null;
    WaterRippleMaterial wrm = null;
    QuantumFieldMaterial qfm = null;
    StainedGlassMaterial sgm = null;
    RandomMaterial rmat = null;
    PhongMaterial phm = null;
    PhongElenaMaterial pem = null;
    GlassMaterial glsmat = null;
    GoldPBRMaterial gpbrmat = null;
    CeramicTilePBRMaterial ctpm = null;
    GlassicTilePBRMaterial glptm = null;
    SilverPBRMaterial silvom = null;
    MarblePBRMaterial mbppp = null;
    SolidCheckerboardMaterial nrcbm = null;
    MarbleMaterial mbtt = null;
    DewDropMaterial ddm = null;
    HexagonalHoneycombMaterial hhcm = null;
    OpticalIllusionMaterial oim = null;
    Cube cube = null;
    Cone cone = null;
    TransparentPlane ptl = null;
    Ellipsoid els = null;
    Triangle tri = null;
    Plane plane = null;
    Cylinder cll = null;
    Rectangle3D r3d = null;
    Triangle tris = null;
    Torus torus = null;
    Box box = null;
    TorusKnot toruskn = null;
    Crescent ccr = null;
    MaterialUtils mut = null;
    ResizeImage rszz = null;
    Hyperboloid hypb = null;
    PBRCapableMaterial pbrcm = null;
    PlasticPBRMaterial plasmat = null;
    CopperPBRMaterial coppmat = null;
    WoodPBRMaterial wpmt = null;
    DiagonalCheckerMaterial dicem = null;
    RectangleCheckerMaterial rcm = null;
    DiffuseMaterial dmm = null;
    CrystalClearMaterial ccm = null;
    PlatinumMaterial plm = null;
    WoodMaterial wmt = null;
    DynamicAlphaColorMaterial dacm = null;
    ElenaTextureMaterial etm = null;
    GradientTextMaterial grtm = null;
    GradientImageTextMaterial gritm = null;
    TurkishTileMaterial trtilem = null;
    NorwegianRoseMaterial nwmat = null;
    NordicWoodMaterial nomat = null;
    CoffeeFjordMaterial cofij = null;
    NorthernLightMaterial nilon = null;
    CarpetTextureMaterial cetome = null;
    AnodizedMetalMaterial anotem = null;
    ProceduralCloudMaterial procemo = null;
    LinearGradientMaterial ligram = null;
    RadialGradientMaterial radimat = null;
    VikingMetalMaterial vkgmt = null;
    TransparentEmojiMaterial trnsEmoj = null;
    TransparentPNGMaterial tpng = null;
    NonScaledTransparentPNGMaterial nonsctrns = null;
    TransparentEmissivePNGMaterial tepng = null;
    WordMaterial womat = null;
    EmojiBillboard ebilbo = null;
    HotCopperMaterial hocop = null;
    NordicWeaveMaterial nqwmt = null;
    RuneStoneMaterial rosom = null;
    AuroraCeramicMaterial acome = null;
    FjordCrystalMaterial fjome = null;
    RosemalingMaterial risomel = null;
    TelemarkPatternMaterial telemol = null;
    BrunostCheeseMaterial buconi = null;
    VikingRuneMaterial viruni = null;
    KilimRosemalingMaterial kirose = null;
    CalligraphyRuneMaterial callimo = null;
    TulipFjordMaterial tjfo = null;
    HamamSaunaMaterial hasuna = null;
    SultanKingMaterial sukemat = null;
    GradientChessMaterial gcm = null;
    SmartGlassMaterial sglas = null;
    HologramDataMaterial holdam = null;
    WaterfallMaterial wemf = null;
    PureWaterMaterial pwm = null;
    ReflectiveMaterial refoma = null;
    LightningMaterial ligoma = null;
    FractalFireMaterial ffm = null;
    NeutralMaterial nnm = null;
    TextureMaterial timam = null;
    ColorUtil cutil = null;
    Letter3D l3d = null;
    Image3D i3d = null;
    
    // 1. Create Scene
    Scene scene = new Scene();
    
    // 2. Create Ray Tracer
    int imageWidth = 800;
    int imageHeight = 600;
    Color rendererBackgroundColor = new Color(0.2f, 0.2f, 0.2f);
    
    // Create ElenaMuratRayTracer
    ElenaMuratRayTracer rayTracer = new ElenaMuratRayTracer(scene, imageWidth, imageHeight, rendererBackgroundColor);
    
    // 3. Adjust ray tracer values
    Camera cmra = new Camera();
    
    cmra.setCameraPosition(new Point3(0, 1, 8));
    cmra.setLookAt(new Point3(0, 0, -3));
    cmra.setUpVector(new Vector3(0, 1, 0));
    cmra.setFov(60.0);
    cmra.setMaxRecursionDepth(2); // Max recursion depth
    cmra.setOrthographic(false);
    cmra.setReflective(true); // Reflections enabled
    cmra.setRefractive(true); // Refractions enabled
    cmra.setShadowsEnabled(true); // Shadows enabled
    
    rayTracer.setCamera(cmra);
    
    // 4. Create and add lights
    // Ambient (More bluish and stronger)
    scene.addLight(new ElenaMuratAmbientLight(new Color(220, 225, 255), 2.5));
    
    // Main light (Softer but stronger)
    scene.addLight(new ElenaDirectionalLight(
        new Vector3(-0.7, -1, -0.4).normalize(),
        new Color(255, 245, 235), // More neutral white
        3.8
    ));
    
    // Fill light (Wider area)
    scene.addLight(new MuratPointLight(
        new Point3(2, 3, 1),
        new Color(230, 235, 255),
        3.5
    ));
    
    // Back light (More pronounced)
    scene.addLight(new ElenaDirectionalLight(
        new Vector3(0.3, 0.3, 1).normalize(), // Direction adjustment
        new Color(255, 255, 255),
        2.0
    ));
    
    // Specular highlight light
    scene.addLight(new MuratPointLight(
        new Point3(-1, 2, 0.5),
        new Color(255, 255, 240),
        2.3
    ));
    
    // Global illumination (Subtle touch for entire scene)
    scene.addLight(new ElenaDirectionalLight(
        new Vector3(0.2, -0.3, 0.1).normalize(),
        new Color(210, 220, 255),
        1.5
    ));
    
    // 5. Create shapes with materials and add to scene
    // --- Four Basic Material Spheres ---
    // a. Gold Sphere
    Sphere goldSphere = new Sphere(0.7); // Create with radius only
    goldSphere.setMaterial(new GoldMaterial());
    goldSphere.setTransform(Matrix4.translate(new Vector3(-1.5, 0.5, 0))); // Set position
    scene.addShape(goldSphere);
    
    // b. Silver Sphere
    Sphere silverSphere = new Sphere(0.7);
    silverSphere.setMaterial(new SilverMaterial());
    silverSphere.setTransform(Matrix4.translate(new Vector3(1.5, 0.5, 0)));
    scene.addShape(silverSphere);
    
    // c. Copper Sphere
    CopperMaterial copperMat = new CopperMaterial();
    Sphere copperSphere = new Sphere(0.7);
    copperSphere.setMaterial(copperMat);
    copperSphere.setTransform(Matrix4.translate(new Vector3(-0.75, -1.0, -1.0)));
    scene.addShape(copperSphere);
    
    // d. Emissive Sphere
    Sphere emissiveSphere = new Sphere(0.7);
    emissiveSphere.setMaterial(new EmissiveMaterial(new Color(255, 100, 0), 3.0));
    emissiveSphere.setTransform(Matrix4.translate(new Vector3(0.75, -1.0, -1.0)));
    scene.addShape(emissiveSphere);
    
    // --- Bump Mapped Sphere ---
    BufferedImage bumpImage = null;
    ImageTexture bumpTexture = null;
    try {
      bumpImage = ImageIO.read(new File("textures\\elena.png"));
      bumpTexture = new ImageTexture(bumpImage, 1.0);
      System.out.println("Normal map loaded successfully.");
      } catch (IOException e) {
      System.err.println("ERROR: Normal map could not be loaded: " + e.getMessage());
      e.printStackTrace();
    }
    
    Material bumpyMaterial = null;
    Sphere bumpySphere = new Sphere(0.8);
    bumpySphere.setTransform(Matrix4.translate(new Vector3(0, 0, -2)));
    
    if (bumpTexture != null) {
      bumpyMaterial = new BumpMaterial(
        new LambertMaterial(new Color(100, 150, 200)), // Base material (blue Lambertian)
        bumpTexture,
        1.0, // Bump strength
        5.0, // UV scale
        bumpySphere.getInverseTransform()
      );
      } else {
      bumpyMaterial = new LambertMaterial(new Color(100, 150, 200));
    }
    bumpySphere.setMaterial(bumpyMaterial);
    scene.addShape(bumpySphere);
    
    // --- Floor ---
    Plane floorPlane = new Plane(new Point3(0, 0, 0), new Vector3(0, 1, 0));
    floorPlane.setTransform(Matrix4.translate(new Vector3(0, -1.7, 0)));
    CheckerboardMaterial floorMaterial = new CheckerboardMaterial(
      new Color(100, 100, 100), // Dark gray
      new Color(200, 200, 200), // Light gray
      4.0, // Scale: 4 squares per unit length
      0.1, // ambientCoefficient
      0.8, // diffuseCoefficient
      0.2, // specularCoefficient
      10.0, // shininess
      Color.WHITE, // specularColor
      0.0, // reflectivity
      1.0, // indexOfRefraction
      0.0, // transparency
      floorPlane.getInverseTransform()
    );
    floorPlane.setMaterial(floorMaterial);
    scene.addShape(floorPlane);
    
    // Right Wall Plane (X=5, normal (-1,0,0))
    Plane rightWallPlane = new Plane(new Point3(0, 0, 0), new Vector3(-1, 0, 0));
    Matrix4 rightWallTransform = Matrix4.translate(new Vector3(5, 0, 0));
    rightWallPlane.setTransform(rightWallTransform);
    SquaredMaterial rightWallMaterial = new SquaredMaterial(
      new Color(0.3f, 0.0f, 0.0f), // Dark red
      new Color(1.0f, 0.0f, 0.0f), // Bright red
      4.0, // Square size
      0.1, 0.7, 0.8, 50.0, Color.WHITE, // Phong parameters
      0.0, 1.0, 0.0,
      rightWallPlane.getInverseTransform()
    );
    rightWallPlane.setMaterial(rightWallMaterial);
    scene.addShape(rightWallPlane);
    
    // Left Wall Plane (X=-5, normal (1,0,0))
    Plane leftWallPlane = new Plane(new Point3(0, 0, 0), new Vector3(1, 0, 0));
    Matrix4 leftWallTransform = Matrix4.translate(new Vector3(-5, 0, 0));
    leftWallPlane.setTransform(leftWallTransform);
    SquaredMaterial leftWallMaterial = new SquaredMaterial(
      new Color(0.0f, 0.3f, 0.0f), // Dark green
      new Color(0.0f, 1.0f, 0.0f), // Bright green
      4.0, // Square size
      0.1, 0.7, 0.8, 50.0, Color.WHITE, // Phong parameters
      0.0, 1.0, 0.0,
      leftWallPlane.getInverseTransform()
    );
    leftWallPlane.setMaterial(leftWallMaterial);
    scene.addShape(leftWallPlane);
    
    // 6. Render image
    System.out.println("Render process starting...");
    long startTime = System.currentTimeMillis();
    
    BufferedImage renderedImage = rayTracer.render();
    
    long endTime = System.currentTimeMillis();
    System.out.println("Render process completed. Time: " + (endTime - startTime) + " ms");
    
    // 7. Save image
    try {
      File outputFile = new File("images\\rendered_scene.png");
      ImageIO.write(renderedImage, "png", outputFile);
      System.out.println("Image successfully saved: " + outputFile.getAbsolutePath());
      } catch (IOException e) {
      System.err.println("An error occurred while saving the image: " + e.getMessage());
      e.printStackTrace();
    }
  }
  
}
