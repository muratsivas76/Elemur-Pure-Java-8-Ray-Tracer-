// ElenaParser.java
// Compile: javac -source 1.8 -target 1.8 -parameters -encoding UTF-8 -sourcepath . -cp bin/elenaRT.jar -g -proc:none -nowarn -O -d . ElenaParser.java
// Example Usage: java -cp bin/elenaRT.jar:. ElenaParser scenes/scene.txt images/scene.png

// Java packages and classes
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

import java.io.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import javax.imageio.ImageIO;

import java.net.MalformedURLException;
import java.net.URLClassLoader;
import java.net.URL;

import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Stack;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Custom packages and classes
import net.elena.murat.light.*;
import net.elena.murat.lovert.*;
import net.elena.murat.math.*;
import net.elena.murat.material.*;
import net.elena.murat.material.pbr.*;
import net.elena.murat.shape.*;
import net.elena.murat.shape.letters.*;

public class ElenaParser {
  
  private static final Pattern COMMENT_PATTERN = Pattern.compile("#.*");
  private static final Pattern BLOCK_START = Pattern.compile("(\\w+)\\s+(\\w+)\\s*\\{");
  private static final Pattern ASSIGNMENT = Pattern.compile("^\\s*(\\w+)\\s*=\\s*([^;]*#?[^;]+?)\\s*;\\s*$");
  private static final Pattern COLOR_HEX = Pattern.compile("#([0-9A-Fa-f]{6})");
  private static final Pattern COLOR_RGB = Pattern.compile("rgb\\((\\d+),\\s*(\\d+),\\s*(\\d+)\\)");
  private static final Pattern POINT3 = Pattern.compile("P\\(([^)]+)\\)");
  private static final Pattern VECTOR3 = Pattern.compile("V\\(([^)]+)\\)");
  private static final Pattern TRANSFORM_OP = Pattern.compile("(translate|rotate|scale)\\s*\\(([^)]*)\\)");
  private static final Map<String, Material> materialCache = new ConcurrentHashMap<>();
  private static final Map<String, BufferedImage> IMAGE_CACHE = new ConcurrentHashMap<>();
    
  private static final BufferedImage DEFAULT_IMAGE = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
  
  private Map<String, Object> objects = new HashMap<>();
  private final Scene scene = new Scene();
  private Color backgroundColor = Color.BLACK;
  private int imageWidth = 800;
  private int imageHeight = 600;
  
  private ElenaMuratRayTracer tracer = new ElenaMuratRayTracer(scene, imageWidth, imageHeight, backgroundColor);
  
  public static void main(String[] args) {
    if (args.length != 2) {
      System.err.println("Usage: java ElenaParser <scene.txt> <output.png>");
      System.exit(1);
    }
    String sceneFile = args[0];
    String outputFile = args[1];
    ElenaParser parser = new ElenaParser();
    try {
      parser.parse(sceneFile);
      BufferedImage image = parser.render();
      File out = new File(outputFile);
      if (!out.getParentFile().exists()) out.getParentFile().mkdirs();
      ImageIO.write(image, "png", out);
      System.out.println("Render completed: " + outputFile);
      } catch (Exception e) {
      System.err.println("Error: " + e.getMessage());
      e.printStackTrace();
      System.exit(-1);
    }
  }
  
  ////////////// START SHAPE MATERAL EXTERNAL //////////
  public Material loadExternalMaterial(String classNameWithExtension, Map<String, String> params) {
    final Material defaultMaterial = new DiffuseMaterial(Color.BLUE);
    
    if (classNameWithExtension == null || classNameWithExtension.trim().isEmpty()) {
      System.err.println("Class name cannot be null or empty -> Returning default material");
      return defaultMaterial;
    }
    
    System.out.println("================\nLoading material: " + classNameWithExtension);
    System.out.println("Parameters: " + params);
    
    String className = classNameWithExtension.replace(".class", "").trim();
    String pathPart = "";
    String classPart = className;
    
    if (className.contains(File.separator) || className.contains("/") || className.contains("\\")) {
      File fullPath = new File(className);
      classPart = fullPath.getName().replace(".class", "");
      pathPart = fullPath.getParent();
      if (pathPart == null) pathPart = "";
    }
    
    try {
      File searchDir = !pathPart.isEmpty() ? new File(pathPart) : new File(".");
      URLClassLoader classLoader = new URLClassLoader(
        new URL[]{searchDir.toURI().toURL()},
        this.getClass().getClassLoader()
      );
      
      Class<?> loadedClass = classLoader.loadClass(classPart);
      
      if (!Material.class.isAssignableFrom(loadedClass)) {
        System.err.println("Class " + classPart + " does not implement Material interface");
        classLoader.close();
        return defaultMaterial;
      }
      
      Material material = null;
      
      System.out.println("Available constructors for " + classPart + ":");
      for (Constructor<?> ctor : loadedClass.getConstructors()) {
        System.out.println("  " + ctor);
      }
      
      // Convert Map to List<ParameterEntry> (type will be determined by constructor parameter types)
      List<ParameterEntry> paramEntries = new ArrayList<ParameterEntry>();
      for (Map.Entry<String, String> entry : params.entrySet()) {
        paramEntries.add(new ParameterEntry(entry.getKey(), entry.getValue(), null));
      }
      
      // Try to find matching constructor
      material = findMatchingConstructor(loadedClass, paramEntries);
      if (material != null) {
        System.out.println("✓ Successfully created material using parameterized constructor");
        classLoader.close();
        return material;
      }
      
      // Try with default constructor
      try {
        Constructor<?> defaultConstructor = loadedClass.getConstructor();
        material = (Material) defaultConstructor.newInstance();
        System.out.println("✓ Using default constructor");
        
        // Set properties via setters
        setPropertiesViaReflection(material, params);
        
        } catch (NoSuchMethodException e) {
        System.err.println("No default constructor found for " + classPart);
        classLoader.close();
        return defaultMaterial;
      }
      
      classLoader.close();
      return material;
      
      } catch (Exception e) {
      System.err.println("Error loading material: " + className + " - " + e.getMessage());
      e.printStackTrace();
    }
    
    return defaultMaterial;
  }
  
  public EMShape loadExternalShape(String classNameWithExtension, Map<String, String> params) {
    System.out.println("=== LOADING SHAPE ===");
    System.out.println("Class: " + classNameWithExtension);
    System.out.println("Parameters: " + params);
    
    if (classNameWithExtension == null || classNameWithExtension.trim().isEmpty()) {
      System.err.println("Class name cannot be null or empty");
      return null;
    }
    
    String className = classNameWithExtension.replace(".class", "").trim();
    String pathPart = "";
    String classPart = className;
    
    if (className.contains(File.separator) || className.contains("/") || className.contains("\\")) {
      File fullPath = new File(className);
      classPart = fullPath.getName().replace(".class", "");
      pathPart = fullPath.getParent();
      if (pathPart == null) pathPart = "";
    }
    
    try {
      File searchDir = !pathPart.isEmpty() ? new File(pathPart) : new File(".");
      URLClassLoader classLoader = new URLClassLoader(
        new URL[]{searchDir.toURI().toURL()},
        this.getClass().getClassLoader()
      );
      
      Class<?> loadedClass = classLoader.loadClass(classPart);
      
      if (!EMShape.class.isAssignableFrom(loadedClass)) {
        System.err.println("Class " + classPart + " does not extend EMShape");
        classLoader.close();
        return null;
      }
      
      EMShape shape = null;
      
      System.out.println("Available constructors for " + classPart + ":");
      for (Constructor<?> ctor : loadedClass.getConstructors()) {
        System.out.println("  " + ctor);
      }
      
      // Convert Map to List<ParameterEntry> (type will be determined by constructor parameter types)
      List<ParameterEntry> paramList = new ArrayList<ParameterEntry>();
      for (Map.Entry<String, String> entry : params.entrySet()) {
        paramList.add(new ParameterEntry(entry.getKey(), entry.getValue(), null));
      }
      
      // Find matching constructor
      shape = findMatchingConstructor(loadedClass, paramList);
      if (shape != null) {
        System.out.println("✓ Successfully created shape using parameterized constructor");
        classLoader.close();
        return shape;
      }
      
      // Try with default constructor
      Constructor<?> defaultConstructor = loadedClass.getConstructor();
      shape = (EMShape) defaultConstructor.newInstance();
      System.out.println("✓ Using default constructor");
      
      // Set properties via setters
      setPropertiesViaReflection(shape, params);
      
      classLoader.close();
      return shape;
      
      } catch (Exception e) {
      System.err.println("Error loading shape: " + className + " - " + e.getMessage());
      e.printStackTrace();
    }
    
    return null;
  }
  
  /**
   * Reflection ile property'leri set et
   */
  private void setPropertiesViaReflection(Object obj, Map<String, String> params) {
    Class<?> clazz = obj.getClass();
    boolean anyPropertySet = false;
    
    for (Map.Entry<String, String> entry : params.entrySet()) {
      String fieldName = entry.getKey();
      String value = entry.getValue();
      
      try {
        // Field'ı bul
        Field field;
        try {
          field = clazz.getDeclaredField(fieldName);
          } catch (NoSuchFieldException e) {
          System.out.println("Field not found: " + fieldName);
          continue;
        }
        
        // Erişimi aç
        field.setAccessible(true);
        
        // Değeri dönüştür ve set et
        Object convertedValue = convertStringToType(value, field.getType());
        if (convertedValue != null) {
          field.set(obj, convertedValue);
          System.out.println("✓ Set field: " + fieldName + " = " + convertedValue);
          anyPropertySet = true;
        }
        
        } catch (Exception e) {
        System.err.println("Error setting field " + fieldName + ": " + e.getMessage());
      }
    }
    
    if (!anyPropertySet) {
      System.out.println("No fields were set via reflection");
    }
  }
  
  /**
   * String'i primitive tiplere dönüştür (sadece geçerlilik kontrolü için)
   */
  private Object convertStringToPrimitiveType(String value) {
    if (value == null || value.trim().isEmpty()) return null;
    value = value.trim();
    
    try {
      // Boolean
      if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
        return Boolean.parseBoolean(value);
      }
      // Integer
      if (value.matches("-?\\d+")) {
        try {
          return Integer.parseInt(value);
          } catch (NumberFormatException e) {
          return Long.parseLong(value);
        }
      }
      // Double/Float
      if (value.matches("-?\\d*\\.\\d+([eE][-+]?\\d+)?")) {
        return Double.parseDouble(value);
      }
      // Character
      if (value.length() == 1) {
        return value.charAt(0);
      }
      // String (varsayılan)
      return value;
      
      } catch (Exception e) {
      return null;
    }
  }
  
  @SuppressWarnings("unchecked")
  private <T> T findMatchingConstructor(Class<?> clazz, List<ParameterEntry> params) {
    try {
      System.out.println("=== FINDING MATCHING CONSTRUCTOR ===");
      System.out.println("Parameter count: " + params.size());
      
      // Önce tüm constructor'ları al ve parametre sayısına göre sırala
      Constructor<?>[] constructors = clazz.getConstructors();
      Arrays.sort(constructors, new Comparator<Constructor<?>>() {
          @Override
          public int compare(Constructor<?> c1, Constructor<?> c2) {
            return Integer.compare(c2.getParameterTypes().length, c1.getParameterTypes().length);
          }
      });
      
      // Tüm constructor'ları dene
      for (Constructor<?> constructor : constructors) {
        Class<?>[] constructorParamTypes = constructor.getParameterTypes();
        System.out.println("Testing constructor: " + constructor);
        
        // Kullanılacak parametreleri takip etmek için
        List<ParameterEntry> availableParams = new ArrayList<ParameterEntry>(params);
        Object[] convertedParams = new Object[constructorParamTypes.length];
        boolean allParamsConverted = true;
        
        for (int i = 0; i < constructorParamTypes.length; i++) {
          boolean paramConverted = false;
          
          // Mevcut parametreler arasında uygun olanı ara
          for (int j = 0; j < availableParams.size(); j++) {
            ParameterEntry entry = availableParams.get(j);
            Object converted = convertStringToType(entry.value, constructorParamTypes[i]);
            if (converted != null) {
              convertedParams[i] = converted;
              paramConverted = true;
              System.out.println("  Using parameter '" + entry.name + "' = " + entry.value +
              " -> " + converted + " (" + constructorParamTypes[i].getSimpleName() + ")");
              // Bu parametreyi kullanıldı olarak işaretle (bir daha kullanılmasın)
              availableParams.remove(j);
              break;
            }
          }
          
          if (!paramConverted) {
            System.out.println("  No suitable parameter found for type: " +
            constructorParamTypes[i].getSimpleName());
            allParamsConverted = false;
            break;
          }
        }
        
        if (allParamsConverted) {
          System.out.println("Constructor matched: " + constructor);
          return (T) constructor.newInstance(convertedParams);
        }
      }
      
      System.out.println("No suitable constructor found");
      
      return null;
      
      } catch (Exception e) {
      System.err.println("Error in constructor matching: " + e.getMessage());
      e.printStackTrace();
      return null;
    }
  }
  
  private Color parseColor(String s) {
    if (s == null) return Color.WHITE;
    
    s = s.trim()
    .replaceAll(";", "")  // Remove any semicolons
    .replaceAll("\\s+", ""); // Remove whitespace
    
    Color cc=parseColorForExternal (s);
    
    return cc;
  }
  
  /**
   * From string to color
   * Supported formats:
   * - Hex: #FF0000, #FF00FF00
   * - Integer RGB: 255,0,0...
   * - Float RGB: 1.0f,0.5f,0.0f...
   */
  private Color parseColorForExternal(String colorStr) {
    if (colorStr == null || colorStr.trim().isEmpty()) {
      return null;
    }
    
    colorStr = colorStr.trim();
    
    try {
      // Hex format: #FF0000 veya #FF00FF00
      if (colorStr.startsWith("#")) {
        return hexToColor (colorStr);
      }
      
      // Float RGB format: 1.0f,0.5f,0.0f
      if (colorStr.toLowerCase().contains("f")) {
        String[] parts = colorStr.split(",");
        if (parts.length >= 3) {
          float r = Float.parseFloat(parts[0].trim());//.replace("f", ""));
          float g = Float.parseFloat(parts[1].trim());//.replace("f", ""));
          float b = Float.parseFloat(parts[2].trim());//.replace("f", ""));
          
          if (parts.length == 4) {
            float a = Float.parseFloat(parts[3].trim());//.replace("f", ""));
            return new Color(r, g, b, a);
          }
          return new Color(r, g, b);
        }
      }
      
      // Integer RGB format: 255,0,0
      if (colorStr.contains(",")) {
        String[] parts = colorStr.split(",");
        if (parts.length >= 3) {
          int r = Integer.parseInt(parts[0].trim());
          int g = Integer.parseInt(parts[1].trim());
          int b = Integer.parseInt(parts[2].trim());
          
          if (parts.length == 4) {
            int a = Integer.parseInt(parts[3].trim());
            return new Color(r, g, b, a);
          }
          return new Color(r, g, b);
        }
      }
      
      // Named colors (red, blue, green, etc.)
      try {
        Field field = Color.class.getField(colorStr.toUpperCase());
        return (Color) field.get(null);
        } catch (Exception e) {
        // Named color not found
      }
      
      } catch (Exception e) {
      System.err.println("Color parsing error for '" + colorStr + "': " + e.getMessage());
    }
    
    return null;
  }
  
  private Color hexToColor(String hex) {
    hex = hex.substring(1); //Remove #
    
    int hexlen = hex.length ();
    
    if ((hexlen != 6) && (hexlen != 8)) {
      throw new IllegalArgumentException("Color format must be #RRGGBB||#RRGGBBAA");
    }
    
    try {
      if (hexlen == 8) {
        int r = Integer.parseInt(hex.substring(0, 2), 16);
        int g = Integer.parseInt(hex.substring(2, 4), 16);
        int b = Integer.parseInt(hex.substring(4, 6), 16);
        int a = Integer.parseInt(hex.substring(6, 8), 16);
        
        return new Color(r, g, b, a);
        } else if (hexlen == 6) {
        int r = Integer.parseInt(hex.substring(0, 2), 16);
        int g = Integer.parseInt(hex.substring(2, 4), 16);
        int b = Integer.parseInt(hex.substring(4, 6), 16);
        
        return new Color(r, g, b);
        } else {
        System.out.println ("Return BLACK for an error...");
        return Color.BLACK;
      }
      } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid color code: " + hex);
    }
  }
  
  /**
   * String'den hedef tipe dönüşüm (primitive, String ve Color tipleri için)
   */
  private Object convertStringToType(String value, Class<?> targetType) {
    if (value == null || value.trim().isEmpty()) return null;
    value = value.trim();
    
    try {
      if (targetType == String.class) return value;
      if (targetType == int.class || targetType == Integer.class) return Integer.parseInt(value);
      if (targetType == double.class || targetType == Double.class) return Double.parseDouble(value);
      if (targetType == float.class || targetType == Float.class) return Float.parseFloat(value);
      if (targetType == boolean.class || targetType == Boolean.class) return Boolean.parseBoolean(value);
      if (targetType == char.class || targetType == Character.class) {
        if (value.length() == 1) return value.charAt(0);
        return null; // Geçersiz karakter
      }
      if (targetType == long.class || targetType == Long.class) return Long.parseLong(value);
      if (targetType == byte.class || targetType == Byte.class) return Byte.parseByte(value);
      if (targetType == short.class || targetType == Short.class) return Short.parseShort(value);
      
      // Color tipi desteği
      if (targetType == Color.class) {
        return parseColorForExternal(value);
      }
      
      return null; // Desteklenmeyen tip
      
      } catch (Exception e) {
      return null; // Dönüşüm hatası
    }
  }
  
  // Parametreleri saklamak için bir yardımcı sınıf
  private static class ParameterEntry {
    String name;
    String value;
    Class<?> type;
    
    ParameterEntry(String name, String value, Class<?> type) {
      this.name = name;
      this.value = value;
      this.type = type;
    }
  }
  ////////////// END SHAPE MATERAL EXTERNAL //////////
  
  /**
   * Parses a font string and creates a Font object
   * Format: "FontName, style, size" or "FontName, size" (style defaults to PLAIN)
   * Examples: "Arial,1,40", "Times New Roman,24", "Arial,BOLD,20"
   *
   * @param fontString String containing font information
   * @return Created Font object
   * @throws IllegalArgumentException if the format is invalid
   */
  private Font parseFont(String fontString) {
    if (fontString == null || fontString.trim().isEmpty()) {
      return new Font("Arial", Font.PLAIN, 24);
    }
    
    String[] parts = fontString.split(",");
    if (parts.length < 2 || parts.length > 3) {
      throw new IllegalArgumentException("Invalid font format. Use: 'FontName,size' or 'FontName,style,size'");
    }
    
    try {
      String fontName = parts[0].trim();
      int style = Font.PLAIN;
      int size;
      
      if (parts.length == 2) {
        // Format: "FontName, size"
        size = Integer.parseInt(parts[1].trim());
        } else {
        // Format: "FontName, style, size"
        String styleStr = parts[1].trim().toUpperCase();
        size = Integer.parseInt(parts[2].trim());
        
        switch (styleStr) {
          case "0": case "PLAIN": style = Font.PLAIN; break;
          case "1": case "BOLD": style = Font.BOLD; break;
          case "2": case "ITALIC": style = Font.ITALIC; break;
          case "3": case "BOLDITALIC": style = Font.BOLD | Font.ITALIC; break;
          default:
          throw new IllegalArgumentException("Invalid font style: " + styleStr +
          ". Use: PLAIN(0), BOLD(1), ITALIC(2), BOLDITALIC(3)");
        }
      }
      
      // Validate size
      if (size <= 0 || size > 1000) {
        throw new IllegalArgumentException("Font size must be between 1 and 1000");
      }
      
      return new Font(fontName, style, size);
      
      } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid number format in font string: " + fontString, e);
    }
  }
  
  private void parse(String filename) throws IOException {
    List<String> lines = readLines(filename);
    Stack<ObjectBuilder> stack = new Stack<ObjectBuilder>();
    int lineNumber = 0;
    
    for (String rawLine : lines) {
      lineNumber++;
      String line = rawLine.trim();
      
      // Skip empty lines and comment lines
      if (line.isEmpty() || line.startsWith("#") || line.startsWith ("//")) {
        continue;
      }
      
      int indo=line.lastIndexOf (";");
      if (indo >= 0x0000) {
        line = line.substring (0, indo+1);
        line = line.trim ();
      }
      
      // Handle block start (e.g., "Material matName {")
      Matcher blockStart = BLOCK_START.matcher(line);
      if (blockStart.matches()) {
        String type = blockStart.group(1);
        String id = blockStart.group(2);
        stack.push(new ObjectBuilder(type, id));
        continue;
      }
      
      // Handle block end
      if (line.equals("}")) {
        if (stack.isEmpty()) {
          throw new IOException("Line " + lineNumber + ": Unmatched '}' character. Attempting to close a block that wasn't opened.");
        }
        ObjectBuilder builder = stack.pop();
        Object obj = builder.build(this);
        if (obj != null) {
          objects.put(builder.id, obj);
        }
        continue;
      }
      
      // Handle property assignments (e.g., "width = 800;")
      if (!stack.isEmpty()) {
        Matcher assign = ASSIGNMENT.matcher(line);
        if (assign.matches()) {
          String key = assign.group(1).trim();
          String value = assign.group(2).trim();
          stack.peek().fields.put(key, value);
          } else {
          // Handle special cases like Renderer settings that aren't in blocks
          parseGlobal(line);
        }
      }
    }
    
    // Check for any blocks that weren't properly closed
    if (!stack.isEmpty()) {
      StringBuilder errorMsg = new StringBuilder("Unclosed blocks:\n");
      for (ObjectBuilder builder : stack) {
        errorMsg.append("- ").append(builder.type).append(" ").append(builder.id).append("\n");
      }
      throw new IOException(errorMsg.toString());
    }
  }
  
  private List<String> readLines(String filename) throws IOException {
    List<String> lines = new ArrayList<>();
    
    File fd = new File (filename);
    InputStream fis = new FileInputStream (fd);
    Reader isr = new InputStreamReader (fis, "UTF-8");
    BufferedReader br = new BufferedReader (isr);
    
    try {
      String line;
      while ((line = br.readLine()) != null) {
        // Satır sonu karakterlerini temizle
        line = line.replace("\r", "").replace("\n", "");
        lines.add(line);
      }
      
      br.close ();
      isr.close ();
      fis.close ();
      } catch (IOException ioe) {
      ioe.printStackTrace ();
    }
    
    return lines;
  }
  
  private void parseGlobal(String line) {
    if (line.contains("color")) {
      Matcher m = Pattern.compile("color\\s*=\\s*(.+?);?").matcher(line);
      if (m.find()) backgroundColor = parseColor(m.group(1));
    }
    if (line.contains("width")) {
      Matcher m = Pattern.compile("width\\s*=\\s*(\\d+)").matcher(line);
      if (m.find()) imageWidth = Integer.parseInt(m.group(1));
    }
    if (line.contains("height")) {
      Matcher m = Pattern.compile("height\\s*=\\s*(\\d+)").matcher(line);
      if (m.find()) imageHeight = Integer.parseInt(m.group(1));
    }
  }
  
  private Point3 parsePoint3(String s) {
    Matcher m = POINT3.matcher(s.trim());
    if (m.find()) {
      String[] parts = m.group(1).split(",");
      return new Point3(
        Double.parseDouble(parts[0].trim()),
        Double.parseDouble(parts[1].trim()),
        Double.parseDouble(parts[2].trim())
      );
    }
    return new Point3(0, 0, 0);
  }
  
  private Vector3 parseVector3(String s) {
    Matcher m = VECTOR3.matcher(s.trim());
    if (m.find()) {
      String[] parts = m.group(1).split(",");
      return new Vector3(
        Double.parseDouble(parts[0].trim()),
        Double.parseDouble(parts[1].trim()),
        Double.parseDouble(parts[2].trim())
      );
    }
    return new Vector3(0, 0, 0);
  }
  
  private List<Point3> parsePoint3List(String s) {
    List<Point3> list = new ArrayList<>();
    s = s.replaceAll("[\\[\\]]", "").trim();
    String[] items = s.split(",");
    for (int i = 0; i < items.length; i += 3) {
      list.add(new Point3(
          Double.parseDouble(items[i].trim().replaceAll("P\\(", "")),
          Double.parseDouble(items[i + 1].trim()),
          Double.parseDouble(items[i + 2].trim().replaceAll("\\)", ""))
      ));
    }
    return list;
  }
  
  private Matrix4 parseTransform(String s) {
    Matrix4 result = Matrix4.identity();
    
    // Split the string by '*' to separate transform operations
    String[] parts = s.split("\\*");
    List<Matrix4> transforms = new ArrayList<>();
    
    for (String part : parts) {
      part = part.trim();
      
      // Match operation name and parameters: e.g., rotate(0,90,-25)
      Matcher m = TRANSFORM_OP.matcher(part);
      if (!m.matches()) {
        throw new IllegalArgumentException("Invalid transform part: " + part);
      }
      
      String op = m.group(1).trim();
      String params = m.group(2).trim();
      String[] p = params.split(",");
      for (int i = 0; i < p.length; i++) p[i] = p[i].trim();
      if (p.length != 3) throw new IllegalArgumentException("Transform requires 3 params: " + part);
      
      double x = Double.parseDouble(p[0]);
      double y = Double.parseDouble(p[1]);
      double z = Double.parseDouble(p[2]);
      
      Matrix4 T = null;
      if ("translate".equals(op)) {
        T = Matrix4.translate(x, y, z);
        } else if ("rotate".equals(op)) {
        Matrix4 rx = Matrix4.rotateX(x);
        Matrix4 ry = Matrix4.rotateY(y);
        Matrix4 rz = Matrix4.rotateZ(z);
        T = rx.multiply(ry).multiply(rz);
        } else if ("scale".equals(op)) {
        T = Matrix4.scale(x, y, z);
        } else {
        throw new IllegalArgumentException("Unknown op: " + op);
      }
      
      transforms.add(T);
    }
    
    // Apply transformations in left-to-right order: first operation first
    for (Matrix4 T : transforms) {
      result = result.multiply(T);
    }
    
    return result;
  }
  
  private BufferedImage loadImage(String path) {
    // Path normalization
    String normalizedPath = path.replace("\"", "").trim();
    
    // Check cache first (fast path) - transparency value too add to cache key
    String cacheKey = normalizedPath;
    BufferedImage cachedImage = IMAGE_CACHE.get(cacheKey);
    if (cachedImage != null) {
      //IMAGE_CACHE.put(cacheKey, cachedImage.clone ());
      return cachedImage;
    }
    
    // Synchronized loading to prevent duplicate loads
    synchronized (IMAGE_CACHE) {
      // Double-check after synchronization
      cachedImage = IMAGE_CACHE.get(cacheKey);
      if (cachedImage != null) {
        //IMAGE_CACHE.put(cacheKey, cachedImage.clone ());
        return cachedImage;
      }
      
      // Load from disk
      try {
        File imageFile = new File(normalizedPath);
        if (!imageFile.exists()) {
          System.err.println("Image file not found: " + normalizedPath);
          return DEFAULT_IMAGE;
        }
        
        BufferedImage loadedImage = ImageIO.read(imageFile);
        if (loadedImage == null) {
          System.err.println("Failed to decode image: " + normalizedPath);
          return DEFAULT_IMAGE;
        }
        
        //BufferedImage bimg = new BufferedImage (
        // loadedImage.getWidth (), loadedImage.getHeight (),
        // BufferedImage.TYPE_INT_RGB);
        //Graphics2D gx = bimg.createGraphics ();
        //gx.drawImage (loadedImage, 0, 0, null);
        
        //loadedImage = bimg;
        //gx.dispose ();
        
        // Cache the successfully loaded image
        IMAGE_CACHE.put(cacheKey, loadedImage);
        return loadedImage;
        
        } catch (IOException e) {
        System.err.println("Error loading image '" + normalizedPath + "': " + e.getMessage());
        return DEFAULT_IMAGE;
      }
    }
  }
  
  private BufferedImage loadImageARGB(String path) {
    String normalizedPath = path.replace("\"", "").trim();
    String cacheKey = normalizedPath;
    
    BufferedImage cachedImage = IMAGE_CACHE.get(cacheKey);
    if (cachedImage != null) {
      return cachedImage;
    }
    
    synchronized (IMAGE_CACHE) {
      cachedImage = IMAGE_CACHE.get(cacheKey);
      if (cachedImage != null) {
        return cachedImage;
      }
      
      try {
        File imageFile = new File(normalizedPath);
        if (!imageFile.exists()) {
          System.err.println("Image file not found: " + normalizedPath);
          return DEFAULT_IMAGE;
        }
        
        BufferedImage loadedImage = ImageIO.read(imageFile);
        if (loadedImage == null) {
          System.err.println("Failed to decode image: " + normalizedPath);
          return DEFAULT_IMAGE;
        }
        
        final int lw = loadedImage.getWidth ();
        final int lh = loadedImage.getHeight ();
        
        BufferedImage compatibleImage = new BufferedImage(
          lw, lh, BufferedImage.TYPE_INT_ARGB
        );
        
        java.awt.Graphics2D g = compatibleImage.createGraphics();
        //g.setComposite(AlphaComposite.Clear);
        //g.fillRect(0, 0, lw, lh);
        //g.setBackground (new Color (250, 0, 0, 0));
        //g.clearRect (0, 0, lw, lh);
        //g.setComposite(AlphaComposite.SrcOver);
        
        g.drawImage(loadedImage, 0, 0, null);
        g.dispose();
        
        IMAGE_CACHE.put(cacheKey, compatibleImage);
        return compatibleImage;
        
        } catch (IOException e) {
        System.err.println("Error loading image '" + normalizedPath + "': " + e.getMessage());
        return DEFAULT_IMAGE;
      }
    }
  }
  
  private BufferedImage render() {
    //ElenaMuratRayTracer tracer = new ElenaMuratRayTracer(scene, imageWidth, imageHeight, backgroundColor);
    for (Object obj : objects.values()) {
      if (obj == null) {
        System.out.println ("NULL OBJECT!");
        continue;
      }
      
      if (obj instanceof Light) {
        scene.addLight ((Light) obj);
        System.out.println ("Added: "+obj.toString ()+"");
        } else if (obj instanceof EMShape) {
        scene.addShape ((EMShape) obj);
        System.out.println ("Added: "+obj.toString ()+"");
        } else if (obj instanceof Camera) {
        tracer.setCamera((Camera) obj);
        System.out.println ("Added: "+obj.toString ()+"");
        } else if (obj instanceof Material) {
        System.out.println ("Added: "+obj.toString ()+"");
      } else continue;
    }
    
    return tracer.render();
  }
  
  private final String convertToLatin(String text) {
    if (text == null) return "NullTextError!";
    if (text.length() < 1) return text;
    
    // Türkçe küçük harf dönüşümleri
    text = text.replaceAll("ccc", "\u00E7"); // ch
    text = text.replaceAll("sss", "\u015F"); // sh
    text = text.replaceAll("kikiki", "\u0131"); // kucuk I
    text = text.replaceAll("ggg", "\u011F"); // yumusak g
    text = text.replaceAll("kokoko", "\u00F6"); // ö
    text = text.replaceAll("kukuku", "\u00FC"); // ü
    
    // Türkçe büyük harf dönüşümleri
    text = text.replaceAll("CCC", "\u00C7"); // CH
    text = text.replaceAll("SSS", "\u015E"); // SH
    text = text.replaceAll("bibibi", "\u0130"); // Buyuk i
    text = text.replaceAll("GGG", "\u011E"); // Yumusak buyuk G
    text = text.replaceAll("bobobo", "\u00D6"); // Ö
    text = text.replaceAll("bububu", "\u00DC"); // Ü
    
    // Portekizce tilde (~) karakterleri
    text = text.replaceAll("AAAAAA", "\u00C3"); // Ã
    text = text.replaceAll("OOOOOO", "\u00D5"); // Õ
    text = text.replaceAll("aaaaaa", "\u00E3"); // ã
    text = text.replaceAll("oooooo", "\u00F5"); // õ
    
    // Shapka () aksanlı karakterler - 5'li
    text = text.replaceAll("AAAAA", "\u00C2"); // Â
    text = text.replaceAll("\u0130\u0130\u0130\u0130\u0130", "\u00CE"); // sapkali buyuk i
    text = text.replaceAll("UUUUU", "\u00DB"); // Û
    text = text.replaceAll("EEEEE", "\u00CA"); // Ê
    text = text.replaceAll("OOOOO", "\u00D4"); // Ô
    text = text.replaceAll("aaaaa", "\u00E2"); // â
    text = text.replaceAll("eeeee", "\u00EA"); // ê
    text = text.replaceAll("iiiii", "\u00EE"); // î
    text = text.replaceAll("ooooo", "\u00F4"); // ô
    text = text.replaceAll("uuuuu", "\u00FB"); // û
    
    // Grave (`) aksanlı karakterler - 4'lü
    text = text.replaceAll("AAAA", "\u00C0"); // À
    text = text.replaceAll("EEEE", "\u00C8"); // È
    text = text.replaceAll("\u0130\u0130\u0130\u0130", "\u00CC"); // Ì
    text = text.replaceAll("IIII", "\u00CC"); // Ì
    text = text.replaceAll("OOOO", "\u00D2"); // Ò
    text = text.replaceAll("UUUU", "\u00D9"); // Ù
    text = text.replaceAll("aaaa", "\u00E0"); // à
    text = text.replaceAll("eeee", "\u00E8"); // è
    text = text.replaceAll("iiii", "\u00EC"); // ì
    text = text.replaceAll("oooo", "\u00F2"); // ò
    text = text.replaceAll("uuuu", "\u00F9"); // ù
    
    // Acute (´) aksanlı karakterler - 3'lü
    text = text.replaceAll("AAA", "\u00C1"); // Tilde buyuk A
    text = text.replaceAll("EEE", "\u00C9"); // É
    text = text.replaceAll("III", "\u00CD"); // Tilde buyuk i
    text = text.replaceAll("OOO", "\u00D3"); // Ó
    text = text.replaceAll("UUU", "\u00DA"); // Ú
    text = text.replaceAll("NNN", "\u00D1"); // Ñ
    text = text.replaceAll("\\?\\?\\?", "\u00BF"); // ¿
    text = text.replaceAll("!!!", "\u00A1"); // ¡
    text = text.replaceAll("aaa", "\u00E1"); // á
    text = text.replaceAll("eee", "\u00E9"); // é
    text = text.replaceAll("iii", "\u00ED"); // í
    text = text.replaceAll("ooo", "\u00F3"); // ó
    text = text.replaceAll("uuu", "\u00FA"); // ú
    text = text.replaceAll("nnn", "\u00F1"); // ñ
    
    // Norveççe büyük harf dönüşümleri
    text = text.replaceAll("AAAEEE", "\u00C6"); // Æ
    text = text.replaceAll("OOO///", "\u00D8"); // Ø
    text = text.replaceAll("AAAooo", "\u00C5"); // Å
    
    // Norveççe küçük harf dönüşümleri
    text = text.replaceAll("aaaece", "\u00E6"); // æ
    text = text.replaceAll("ooo///", "\u00F8"); // ø
    text = text.replaceAll("aaaooo", "\u00E5"); // å
    
    return text;
  }
  
  private class ObjectBuilder {
    String type;
    String id;
    Map<String, String> fields = new HashMap<>();
    
    public ObjectBuilder(String type, String id) {
      this.type = type;
      this.id = id;
    }
    
    public Object build(ElenaParser parser) {
      try {
        switch (type) {
          case "Camera": return buildCamera(parser);
          case "Renderer": return buildTracerValues(parser);
          
          case "BioluminescentLight": return buildBioluminescentLight(parser);
          case "BlackHoleLight": return buildBlackHoleLight(parser);
          case "ElenaDirectionalLight": return buildElenaDirectionalLight(parser);
          case "ElenaMuratAmbientLight": return buildElenaMuratAmbientLight(parser);
          case "FractalLight": return buildFractalLight(parser);
          case "MuratPointLight": return buildMuratPointLight(parser);
          case "PulsatingPointLight": return buildPulsatingPointLight(parser);
          case "SpotLight": return buildSpotLight(parser);
          
          case "Box": return buildBox(parser);
          case "Cone": return buildCone(parser);
          case "Crescent": return buildCrescent (parser);
          case "Cube": return buildCube(parser);
          case "Cylinder": return buildCylinder(parser);
          case "EmojiBillboard": return buildEmojiBillboard (parser);
          case "Ellipsoid": return buildEllipsoid(parser);
          case "Hyperboloid": return buildHyperboloid(parser);
          case "Plane": return buildPlane(parser);
          case "TransparentPlane": return buildTransparentPlane(parser);
          case "Rectangle3D": return buildRectangle3D(parser);
          case "RectangularPrism": return buildRectangularPrism(parser);
          case "Sphere": return buildSphere(parser);
          case "Torus": return buildTorus(parser);
          case "TorusKnot": return buildTorusKnot(parser);
          case "Triangle": return buildTriangle(parser);
          case "UnionCSG": return buildUnionCSG(parser);
          case "IntersectionCSG": return buildIntersectionCSG(parser);
          case "DifferenceCSG": return buildDifferenceCSG(parser);
          
          case "CustomMaterial": return buildCustomMaterial(parser);
          case "CustomShape": return buildCustomShape(parser);
          
          case "AmberMaterial": return buildAmberMaterial(parser);
          case "AnodizedMetalMaterial": return buildAnodizedMetalMaterial(parser);
          case "AnodizedTextMaterial": return buildAnodizedTextMaterial(parser);
          case "AuroraCeramicMaterial": return buildAuroraCeramicMaterial(parser);
          case "BlackHoleMaterial": return buildBlackHoleMaterial(parser);
          case "BrightnessMaterial": return buildBrightnessMaterial(parser);
          case "BrunostCheeseMaterial": return buildBrunostCheeseMaterial (parser);
          case "BumpMaterial": return buildBumpMaterial(parser);
          case "CalligraphyRuneMaterial": return buildCalligraphyRuneMaterial (parser);
          case "CarpetTextureMaterial": return buildCarpetTextureMaterial (parser);
          case "CheckerboardMaterial": return buildCheckerboardMaterial(parser);
          case "CircleTextureMaterial": return buildCircleTextureMaterial(parser);
          case "ContrastMaterial": return buildContrastMaterial(parser);
          case "CopperMaterial": return buildCopperMaterial(parser);
          case "CrystalClearMaterial": return buildCrystalClearMaterial(parser);
          case "CrystalMaterial": return buildCrystalMaterial(parser);
          case "DamaskCeramicMaterial": return buildDamaskCeramicMaterial(parser);
          case "DewDropMaterial": return buildDewDropMaterial(parser);
          case "DiagonalCheckerMaterial": return buildDiagonalCheckerMaterial(parser);
          case "DielectricMaterial": return buildDielectricMaterial(parser);
          case "DiffuseMaterial": return buildDiffuseMaterial(parser);
          case "ElenaTextureMaterial": return buildElenaTextureMaterial(parser);
          case "EmissiveMaterial": return buildEmissiveMaterial(parser);
          case "FractalBarkMaterial": return buildFractalBarkMaterial(parser);
          case "FractalFireMaterial": return buildFractalFireMaterial(parser);
          case "FjordCrystalMaterial": return buildFjordCrystalMaterial (parser);
          case "GhostTextMaterial": return buildGhostTextMaterial(parser);
          case "GoldMaterial": return buildGoldMaterial(parser);
          case "GradientChessMaterial": return buildGradientChessMaterial(parser);
          case "GradientImageTextMaterial": return buildGradientImageTextMaterial(parser);
          case "GradientTextMaterial": return buildGradientTextMaterial(parser);
          case "HexagonalHoneycombMaterial": return buildHexagonalHoneycombMaterial(parser);
          case "HamamSaunaMaterial": return buildHamamSaunaMaterial(parser);
          case "HologramDataMaterial": return buildHologramDataMaterial(parser);
          case "HolographicDiffractionMaterial": return buildHolographicDiffractionMaterial(parser);
          case "HotCopperMaterial": return buildHotCopperMaterial(parser);
          case "HybridTextMaterial": return buildHybridTextMaterial(parser);
          case "ImageTexture": return buildImageTexture(parser);
          case "ImageTextureMaterial": return buildImageTextureMaterial(parser);
          case "InvertLightColorMaterial": return buildInvertLightColorMaterial(parser);
          case "KilimRosemalingMaterial": return buildKilimRosemalingMaterial (parser);
          case "LambertMaterial": return buildLambertMaterial(parser);
          case "LavaFlowMaterial": return buildLavaFlowMaterial(parser);
          case "LightningMaterial": return buildLightningMaterial(parser);
          case "MarbleMaterial": return buildMarbleMaterial(parser);
          case "MetallicMaterial": return buildMetallicMaterial(parser);
          case "MosaicMaterial": return buildMosaicMaterial(parser);
          case "NeutralMaterial": return buildNeutralMaterial(parser);
          case "NordicWoodMaterial": return buildNordicWoodMaterial (parser);
          case "NordicWeaveMaterial": return buildNordicWeaveMaterial (parser);
          case "NorthernLightMaterial": return buildNorthernLightMaterial (parser);
          case "OpticalIllusionMaterial": return buildOpticalIllusionMaterial(parser);
          case "CeramicTilePBRMaterial": return buildCeramicTilePBRMaterial(parser);
          case "ChromePBRMaterial": return buildChromePBRMaterial(parser);
          case "CoffeeFjordMaterial": return buildCoffeeFjordMaterial (parser);
          case "CopperPBRMaterial": return buildCopperPBRMaterial(parser);
          case "DiamondMaterial": return buildDiamondMaterial(parser);
          case "EdgeLightColorMaterial": return buildEdgeLightColorMaterial(parser);
          case "EmeraldMaterial": return buildEmeraldMaterial(parser);
          case "GlassicTilePBRMaterial": return buildGlassicTilePBRMaterial(parser);
          case "GlassMaterial": return buildGlassMaterial(parser);
          case "GoldPBRMaterial": return buildGoldPBRMaterial(parser);
          case "HolographicPBRMaterial": return buildHolographicPBRMaterial(parser);
          case "LinearGradientMaterial": return buildLinearGradientMaterial(parser);
          case "MarblePBRMaterial": return buildMarblePBRMaterial(parser);
          case "MultiMixMaterial": return buildMultiMixMaterial(parser);
          case "NonScaledTransparentPNGMaterial": return buildNonScaledTransparentPNGMaterial(parser);
          case "NorwegianRoseMaterial": return buildNorwegianRoseMaterial(parser);
          case "OrbitalMaterial": return buildOrbitalMaterial(parser);
          case "PolkaDotMaterial": return buildPolkaDotMaterial(parser);
          case "PlasticPBRMaterial": return buildPlasticPBRMaterial(parser);
          case "RadialGradientMaterial": return buildRadialGradientMaterial(parser);
          case "SilverPBRMaterial": return buildSilverPBRMaterial(parser);
          case "WaterPBRMaterial": return buildWaterPBRMaterial(parser);
          case "WoodPBRMaterial": return buildWoodPBRMaterial(parser);
          case "ObsidianMaterial": return buildObsidianMaterial(parser);
          case "PhongElenaMaterial": return buildPhongElenaMaterial(parser);
          case "PhongMaterial": return buildPhongMaterial(parser);
          case "PhongTextMaterial": return buildPhongTextMaterial(parser);
          case "PixelArtMaterial": return buildPixelArtMaterial(parser);
          case "PlatinumMaterial": return buildPlatinumMaterial(parser);
          case "ProceduralCloudMaterial": return buildProceduralCloudMaterial(parser);
          case "ProceduralFlowerMaterial": return buildProceduralFlowerMaterial(parser);
          case "PureWaterMaterial": return buildPureWaterMaterial(parser);
          case "QuantumFieldMaterial": return buildQuantumFieldMaterial(parser);
          case "RandomMaterial": return buildRandomMaterial(parser);
          case "RectangleCheckerMaterial": return buildRectangleCheckerMaterial(parser);
          case "ReflectiveMaterial": return buildReflectiveMaterial(parser);
          case "RosemalingMaterial": return buildRosemalingMaterial (parser);
          case "RoughMaterial": return buildRoughMaterial(parser);
          case "RubyMaterial": return buildRubyMaterial(parser);
          case "RuneStoneMaterial": return buildRuneStoneMaterial (parser);
          case "SilverMaterial": return buildSilverMaterial(parser);
          case "SmartGlassMaterial": return buildSmartGlassMaterial(parser);
          case "SolidCheckerboardMaterial": return buildSolidCheckerboardMaterial(parser);
          case "SolidColorMaterial": return buildSolidColorMaterial(parser);
          case "SphereWordTextureMaterial": return buildSphereWordTextureMaterial(parser);
          case "SquaredMaterial": return buildSquaredMaterial(parser);
          case "StainedGlassMaterial": return buildStainedGlassMaterial(parser);
          case "StarfieldMaterial": return buildStarfieldMaterial(parser);
          case "StripedMaterial": return buildStripedMaterial(parser);
          case "SultanKingMaterial": return buildSultanKingMaterial(parser);
          case "TelemarkPatternMaterial": return buildTelemarkPatternMaterial(parser);
          case "TextDielectricMaterial": return buildTextDielectricMaterial(parser);
          case "TexturedCheckerboardMaterial": return buildTexturedCheckerboardMaterial(parser);
          case "TexturedPhongMaterial": return buildTexturedPhongMaterial(parser);
          case "TextureMaterial": return buildTextureMaterial(parser);
          case "ThresholdMaterial": return buildThresholdMaterial(parser);
          case "TransparentColorMaterial": return buildTransparentColorMaterial(parser);
          case "TransparentEmojiMaterial": return buildTransparentEmojiMaterial (parser);
          case "TransparentPNGMaterial": return buildTransparentPNGMaterial (parser);
          case "TransparentEmissivePNGMaterial": return buildTransparentEmissivePNGMaterial (parser);
          case "TriangleMaterial": return buildTriangleMaterial(parser);
          case "TulipFjordMaterial": return buildTulipFjordMaterial (parser);
          case "TurkishTileMaterial": return buildTurkishTileMaterial (parser);
          case "VikingMetalMaterial": return buildVikingMetalMaterial (parser);
          case "VikingRuneMaterial": return buildVikingRuneMaterial (parser);
          case "WaterfallMaterial": return buildWaterfallMaterial(parser);
          case "WaterRippleMaterial": return buildWaterRippleMaterial(parser);
          case "WoodMaterial": return buildWoodMaterial(parser);
          case "WordMaterial": return buildWordMaterial (parser);
          case "XRayMaterial": return buildXRayMaterial(parser);
          case "Image3D": return buildImage3D(parser);
          case "Letter3D": return buildLetter3D(parser);
          default:
            throw new IllegalArgumentException("Unknown type: " + type);
        }
        } catch (Exception e) {
        throw new RuntimeException("Error building " + type + ": " + e.getMessage(), e);
      }
    }
    
    private Camera buildCamera(ElenaParser parser) {
      Point3 pos = parser.parsePoint3(fields.get("position"));
      Point3 lookAt = parser.parsePoint3(fields.get("lookAt"));
      Vector3 up = parser.parseVector3(fields.get("upVector"));
      double fov = Double.parseDouble(fields.get("fov"));
      boolean ortho = Boolean.parseBoolean(fields.get("orthographic"));
      int depth = Integer.parseInt(fields.get("maxRecursionDepth"));
      boolean isReflect = (Boolean.parseBoolean(fields.get("reflective")));
      boolean isRefract = (Boolean.parseBoolean(fields.get("refractive")));
      boolean isShadowed = (Boolean.parseBoolean(fields.get("shadowsEnabled")));
      
      Camera cam = new Camera();
      
      cam.setCameraPosition(pos);
      cam.setLookAt(lookAt);
      cam.setUpVector(up);
      cam.setFov(fov);
      cam.setOrthographic(ortho);
      cam.setMaxRecursionDepth(depth);
      cam.setReflective (isReflect);
      cam.setRefractive (isRefract);
      cam.setShadowsEnabled (isShadowed);
      
      return cam;
    }
    
    private ElenaMuratRayTracer buildTracerValues(ElenaParser parser) {
      // Renderer ayarlarını parse et
      if (fields.containsKey("width")) {
        imageWidth = Integer.parseInt(fields.get("width"));
      }
      
      if (fields.containsKey("height")) {
        imageHeight = Integer.parseInt(fields.get("height"));
      }
      
      if (fields.containsKey("backgroundColor")) {
        backgroundColor = parser.parseColor(fields.get("backgroundColor"));
      }
      
      tracer = new ElenaMuratRayTracer(scene, imageWidth, imageHeight, backgroundColor);
      
      if (fields.containsKey("shadowColor")) {
        Color cshadow = parser.parseColor(fields.get("shadowColor"));
        tracer.setShadowColor (cshadow);
      }
      
      return tracer;
    }
    
    private BioluminescentLight buildBioluminescentLight(ElenaParser parser) {
      List<Point3> positions = parser.parsePoint3List(fields.get("positions"));
      String colorStr = fields.get("color");
      if (colorStr == null) {
        throw new IllegalArgumentException("color is missing in BioluminescentLight");
      }
      Color color = parser.parseColor(colorStr);
      double pulseSpeed = Double.parseDouble(fields.get("pulseSpeed"));
      if (fields.containsKey("baseIntensity")) {
        double baseIntensity = Double.parseDouble(fields.get("baseIntensity"));
        double attenuationFactor = Double.parseDouble(fields.get("attenuationFactor"));
        return new BioluminescentLight(positions, color, pulseSpeed, baseIntensity, attenuationFactor);
      }
      return new BioluminescentLight(positions, color, pulseSpeed);
    }
    
    private BlackHoleLight buildBlackHoleLight(ElenaParser parser) {
      Point3 singularity = parser.parsePoint3(fields.get("singularity"));
      double radius = Double.parseDouble(fields.get("radius"));
      Color color = parser.parseColor(fields.get("color"));
      if (fields.containsKey("intensity")) {
        double intensity = Double.parseDouble(fields.get("intensity"));
        return new BlackHoleLight(singularity, radius, color, intensity);
      }
      return new BlackHoleLight(singularity, radius, color);
    }
    
    private ElenaDirectionalLight buildElenaDirectionalLight(ElenaParser parser) {
      Vector3 direction = parser.parseVector3(fields.get("direction"));
      Color color = parser.parseColor(fields.get("color"));
      double intensity = Double.parseDouble(fields.get("intensity"));
      return new ElenaDirectionalLight(direction, color, intensity);
    }
    
    private ElenaMuratAmbientLight buildElenaMuratAmbientLight(ElenaParser parser) {
      Color color = parser.parseColor(fields.get("color"));
      double intensity = Double.parseDouble(fields.get("intensity"));
      return new ElenaMuratAmbientLight(color, intensity);
    }
    
    private FractalLight buildFractalLight(ElenaParser parser) {
      Point3 position = parser.parsePoint3(fields.get("position"));
      Color color = parser.parseColor(fields.get("color"));
      double intensity = Double.parseDouble(fields.get("intensity"));
      if (fields.containsKey("octaves")) {
        int octaves = Integer.parseInt(fields.get("octaves"));
        double persistence = Double.parseDouble(fields.get("persistence"));
        double frequency = Double.parseDouble(fields.get("frequency"));
        return new FractalLight(position, color, intensity, octaves, persistence, frequency);
      }
      return new FractalLight(position, color, intensity);
    }
    
    private MuratPointLight buildMuratPointLight(ElenaParser parser) {
      Point3 position = parser.parsePoint3(fields.get("position"));
      Color color = parser.parseColor(fields.get("color"));
      double intensity = Double.parseDouble(fields.get("intensity"));
      if (fields.containsKey("constantAttenuation")) {
        double c = Double.parseDouble(fields.get("constantAttenuation"));
        double l = Double.parseDouble(fields.get("linearAttenuation"));
        double q = Double.parseDouble(fields.get("quadraticAttenuation"));
        return new MuratPointLight(position, color, intensity, c, l, q);
      }
      return new MuratPointLight(position, color, intensity);
    }
    
    private PulsatingPointLight buildPulsatingPointLight(ElenaParser parser) {
      Point3 initialPosition = parser.parsePoint3(fields.get("initialPosition"));
      Color baseColor = parser.parseColor(fields.get("baseColor"));
      double baseIntensity = Double.parseDouble(fields.get("baseIntensity"));
      double pulsationSpeed = Double.parseDouble(fields.get("pulsationSpeed"));
      double movementSpeed = Double.parseDouble(fields.get("movementSpeed"));
      double movementAmplitude = Double.parseDouble(fields.get("movementAmplitude"));
      if (fields.containsKey("constantAttenuation")) {
        double c = Double.parseDouble(fields.get("constantAttenuation"));
        double l = Double.parseDouble(fields.get("linearAttenuation"));
        double q = Double.parseDouble(fields.get("quadraticAttenuation"));
        return new PulsatingPointLight(initialPosition, baseColor, baseIntensity, pulsationSpeed, movementSpeed, movementAmplitude, c, l, q);
      }
      return new PulsatingPointLight(initialPosition, baseColor, baseIntensity, pulsationSpeed, movementSpeed, movementAmplitude);
    }
    
    private SpotLight buildSpotLight(ElenaParser parser) {
      Point3 position = parser.parsePoint3(fields.get("position"));
      Vector3 direction = parser.parseVector3(fields.get("direction"));
      Color color = parser.parseColor(fields.get("color"));
      double intensity = Double.parseDouble(fields.get("intensity"));
      double inner = Double.parseDouble(fields.get("innerConeAngle"));
      double outer = Double.parseDouble(fields.get("outerConeAngle"));
      if (fields.containsKey("constantAttenuation")) {
        double c = Double.parseDouble(fields.get("constantAttenuation"));
        double l = Double.parseDouble(fields.get("linearAttenuation"));
        double q = Double.parseDouble(fields.get("quadraticAttenuation"));
        return new SpotLight(position, direction, color, intensity, inner, outer, c, l, q);
      }
      return new SpotLight(position, direction, color, intensity, inner, outer);
    }
    
    
    //BUILD SHAPES
    private EMShape buildBox(ElenaParser parser) {
      double w = Double.parseDouble(fields.get("width"));
      double h = Double.parseDouble(fields.get("height"));
      double d = Double.parseDouble(fields.get("depth"));
      Box box = new Box(w, h, d);
      
      if (fields.containsKey("transform")) {
        box.setTransform(parser.parseTransform(fields.get("transform")));
      }
      
      if (fields.containsKey("material")) {
        Material mat=(Material) parser.objects.get(fields.get("material"));
        mat.setObjectTransform (box.getInverseTransform ());
        box.setMaterial(mat);
      }
      
      return box;
    }
    
    private EMShape buildCone(ElenaParser parser) {
      double radius = Double.parseDouble(fields.get("radius"));
      double height = Double.parseDouble(fields.get("height"));
      Cone cone = new Cone(radius, height);
      
      if (fields.containsKey("transform")) {
        cone.setTransform(parser.parseTransform(fields.get("transform")));
      }
      
      if (fields.containsKey("material")) {
        Material mat=(Material) parser.objects.get(fields.get("material"));
        mat.setObjectTransform (cone.getInverseTransform ());
        cone.setMaterial(mat);
      }
      
      return cone;
    }
    
    private EMShape buildCrescent(ElenaParser parser) {
      double radius = Double.parseDouble(fields.get("radius"));
      double cutRadius = Double.parseDouble(fields.get("cutRadius"));
      double cutDistance = Double.parseDouble(fields.get("cutDistance"));
      
      EMShape crescent = new Crescent(radius, cutRadius, cutDistance);
      
      if (fields.containsKey("transform")) {
        crescent.setTransform(parser.parseTransform(fields.get("transform")));
      }
      
      if (fields.containsKey("material")) {
        Material mat=(Material) parser.objects.get(fields.get("material"));
        mat.setObjectTransform (crescent.getInverseTransform ());
        crescent.setMaterial(mat);
      }
      
      return crescent;
    }
    
    private EMShape buildCube(ElenaParser parser) {
      if (fields.containsKey("sideLength")) {
        double side = Double.parseDouble(fields.get("sideLength"));
        Cube cube = new Cube(side);
        
        if (fields.containsKey("transform")) {
          cube.setTransform(parser.parseTransform(fields.get("transform")));
        }
        
        if (fields.containsKey("material")) {
          Material mat=(Material) parser.objects.get(fields.get("material"));
          mat.setObjectTransform (cube.getInverseTransform ());
          cube.setMaterial(mat);
        }
        
        return cube;
        } else {
        Point3 min = parser.parsePoint3(fields.get("min"));
        Point3 max = parser.parsePoint3(fields.get("max"));
        Cube cube = new Cube(min, max);
        
        if (fields.containsKey("transform")) {
          cube.setTransform(parser.parseTransform(fields.get("transform")));
        }
        
        if (fields.containsKey("material")) {
          Material mat=(Material) parser.objects.get(fields.get("material"));
          mat.setObjectTransform (cube.getInverseTransform ());
          cube.setMaterial(mat);
        }
        
        return cube;
      }
    }
    
    private EMShape buildCylinder(ElenaParser parser) {
      if (fields.containsKey("startPoint")) {
        Point3 start = parser.parsePoint3(fields.get("startPoint"));
        Point3 end = parser.parsePoint3(fields.get("endPoint"));
        double radius = Double.parseDouble(fields.get("radius"));
        double height = Double.parseDouble(fields.get("height"));
        Cylinder cylinder = new Cylinder(start, end, radius, height);
        
        if (fields.containsKey("transform")) {
          cylinder.setTransform(parser.parseTransform(fields.get("transform")));
        }
        
        if (fields.containsKey("material")) {
          Material mat=(Material) parser.objects.get(fields.get("material"));
          mat.setObjectTransform (cylinder.getInverseTransform ());
          cylinder.setMaterial(mat);
        }
        return cylinder;
        } else {
        double radius = Double.parseDouble(fields.get("radius"));
        double height = Double.parseDouble(fields.get("height"));
        Cylinder cylinder = new Cylinder(radius, height);
        
        if (fields.containsKey("transform")) {
          cylinder.setTransform(parser.parseTransform(fields.get("transform")));
        }
        
        if (fields.containsKey("material")) {
          Material mat=(Material) parser.objects.get(fields.get("material"));
          mat.setObjectTransform (cylinder.getInverseTransform ());
          cylinder.setMaterial(mat);
        }
        
        return cylinder;
      }
    }
    
    private EMShape buildDifferenceCSG(ElenaParser parser) {
      EMShape left = (EMShape) parser.objects.get(fields.get("left"));
      EMShape right = (EMShape) parser.objects.get(fields.get("right"));
      DifferenceCSG csg = new DifferenceCSG(left, right);
      
      if (fields.containsKey("transform")) {
        csg.setTransform(parser.parseTransform(fields.get("transform")));
      }
      
      if (fields.containsKey("material")) {
        Material mat=(Material) parser.objects.get(fields.get("material"));
        mat.setObjectTransform (csg.getInverseTransform ());
        csg.setMaterial(mat);
      }
      
      return csg;
    }
    
    private EMShape buildEllipsoid(ElenaParser parser) {
      Point3 center = parser.parsePoint3(fields.get("center"));
      double a = Double.parseDouble(fields.get("a"));
      double b = Double.parseDouble(fields.get("b"));
      double c = Double.parseDouble(fields.get("c"));
      Ellipsoid ellipsoid = new Ellipsoid(center, a, b, c);
      
      if (fields.containsKey("transform")) {
        ellipsoid.setTransform(parser.parseTransform(fields.get("transform")));
      }
      
      if (fields.containsKey("material")) {
        Material mat=(Material) parser.objects.get(fields.get("material"));
        mat.setObjectTransform (ellipsoid.getInverseTransform ());
        ellipsoid.setMaterial(mat);
      }
      
      return ellipsoid;
    }
    
    private EMShape buildHyperboloid(ElenaParser parser) {
      if (fields.size() < 1) {
        Hyperboloid h = new Hyperboloid();
        
        if (fields.containsKey("transform")) {
          h.setTransform(parser.parseTransform(fields.get("transform")));
        }
        
        if (fields.containsKey("material")) {
          Material mat=(Material) parser.objects.get(fields.get("material"));
          mat.setObjectTransform (h.getInverseTransform ());
          h.setMaterial(mat);
        }
        
        return h;
        } else {
        double a = Double.parseDouble(fields.get("a"));
        double b = Double.parseDouble(fields.get("b"));
        double c = Double.parseDouble(fields.get("c"));
        double height = Double.parseDouble(fields.get("height"));
        Hyperboloid h = new Hyperboloid(a, b, c, height);
        
        if (fields.containsKey("transform")) {
          h.setTransform(parser.parseTransform(fields.get("transform")));
        }
        
        if (fields.containsKey("material")) {
          Material mat=(Material) parser.objects.get(fields.get("material"));
          mat.setObjectTransform (h.getInverseTransform ());
          h.setMaterial(mat);
        }
        
        return h;
      }
    }
    
    private EMShape buildIntersectionCSG(ElenaParser parser) {
      EMShape left = (EMShape) parser.objects.get(fields.get("left"));
      EMShape right = (EMShape) parser.objects.get(fields.get("right"));
      IntersectionCSG csg = new IntersectionCSG(left, right);
      
      if (fields.containsKey("transform")) {
        csg.setTransform(parser.parseTransform(fields.get("transform")));
      }
      
      if (fields.containsKey("material")) {
        Material mat=(Material) parser.objects.get(fields.get("material"));
        mat.setObjectTransform (csg.getInverseTransform ());
        csg.setMaterial(mat);
      }
      
      return csg;
    }
    
    private EMShape buildPlane(ElenaParser parser) {
      Point3 point = parser.parsePoint3(fields.get("pointOnPlane"));
      Vector3 normal = parser.parseVector3(fields.get("normal"));
      Plane plane = new Plane(point, normal);
      
      if (fields.containsKey("transform")) {
        Matrix4 transform = parser.parseTransform(fields.get("transform"));
        plane.setTransform (transform);
      }
      
      if (fields.containsKey("material")) {
        Material mat = (Material) parser.objects.get(fields.get("material"));
        mat.setObjectTransform(plane.getInverseTransform());
        plane.setMaterial(mat);
      }
      return plane;
    }
    
    private EMShape buildEmojiBillboard(ElenaParser parser) {
      EMShape ebilbo = null;
      
      double width = 1.0;
      double height = 1.0;
      boolean isRectangle = true;
      boolean isVisible = true;
      BufferedImage textureImage = null;
      
      if (fields.containsKey("width")) {
        width = Double.parseDouble(fields.get("width"));
      }
      
      if (fields.containsKey("height")) {
        height = Double.parseDouble(fields.get("height"));
      }
      
      if (fields.containsKey("isRectangle")) {
        isRectangle = Boolean.parseBoolean(fields.get("isRectangle"));
      }
      
      if (fields.containsKey("isVisible")) {
        isVisible = Boolean.parseBoolean(fields.get("isVisible"));
      }
      
      if (fields.containsKey("imagePath")) {
        String imagePath = fields.get ("imagePath").replace ("\"", "");
        BufferedImage img = parser.loadImage(imagePath);
        textureImage = img;
      }
      
      ebilbo = new EmojiBillboard (width, height, isRectangle, isVisible, textureImage);
      
      if (fields.containsKey("transform")) {
        Matrix4 transform = parser.parseTransform(fields.get("transform"));
        ebilbo.setTransform (transform);
      }
      
      if (fields.containsKey("material")) {
        Material mat = (Material) parser.objects.get(fields.get("material"));
        mat.setObjectTransform(ebilbo.getInverseTransform());
        ebilbo.setMaterial(mat);
      }
      
      return ebilbo;
    }
    
    private EMShape buildTransparentPlane(ElenaParser parser) {
      Point3 point = parser.parsePoint3(fields.get("pointOnPlane"));
      Vector3 normal = parser.parseVector3(fields.get("normal"));
      double thickness = Double.parseDouble(fields.get("thickness"));
      
      EMShape plane = new TransparentPlane(point, normal, thickness);
      
      if (fields.containsKey("transform")) {
        Matrix4 transform = parser.parseTransform(fields.get("transform"));
        plane.setTransform (transform);
      }
      
      if (fields.containsKey("material")) {
        Material mat = (Material) parser.objects.get(fields.get("material"));
        mat.setObjectTransform(plane.getInverseTransform());
        plane.setMaterial(mat);
      }
      return plane;
    }
    
    private EMShape buildRectangle3D(ElenaParser parser) {
      Point3 p1 = parser.parsePoint3(fields.get("p1"));
      Point3 p2 = parser.parsePoint3(fields.get("p2"));
      float thickness = Float.parseFloat(fields.get("thickness"));
      Rectangle3D rect = new Rectangle3D(p1, p2, thickness);
      
      if (fields.containsKey("transform")) {
        rect.setTransform(parser.parseTransform(fields.get("transform")));
      }
      
      if (fields.containsKey("material")) {
        Material mat=(Material) parser.objects.get(fields.get("material"));
        mat.setObjectTransform (rect.getInverseTransform ());
        rect.setMaterial(mat);
      }
      
      return rect;
    }
    
    private EMShape buildRectangularPrism(ElenaParser parser) {
      double w = Double.parseDouble(fields.get("width"));
      double h = Double.parseDouble(fields.get("height"));
      double d = Double.parseDouble(fields.get("depth"));
      RectangularPrism prism = new RectangularPrism(w, h, d);
      
      if (fields.containsKey("transform")) {
        prism.setTransform(parser.parseTransform(fields.get("transform")));
      }
      
      if (fields.containsKey("material")) {
        Material mat=(Material) parser.objects.get(fields.get("material"));
        mat.setObjectTransform (prism.getInverseTransform ());
        prism.setMaterial(mat);
      }
      
      return prism;
    }
    
    private EMShape buildSphere(ElenaParser parser) {
      double radius = Double.parseDouble(fields.get("radius"));
      Sphere sphere = new Sphere(radius);
      
      if (fields.containsKey("transform")) {
        sphere.setTransform(parser.parseTransform(fields.get("transform")));
      }
      
      if (fields.containsKey("material")) {
        Material mat=(Material) parser.objects.get(fields.get("material"));
        mat.setObjectTransform (sphere.getInverseTransform ());
        sphere.setMaterial(mat);
      }
      
      return sphere;
    }
    
    private EMShape buildTorus(ElenaParser parser) {
      double major = Double.parseDouble(fields.get("majorRadius"));
      double minor = Double.parseDouble(fields.get("minorRadius"));
      Torus torus = new Torus(major, minor);
      
      if (fields.containsKey("transform")) {
        torus.setTransform(parser.parseTransform(fields.get("transform")));
      }
      
      if (fields.containsKey("material")) {
        Material mat=(Material) parser.objects.get(fields.get("material"));
        mat.setObjectTransform (torus.getInverseTransform ());
        torus.setMaterial(mat);
      }
      
      return torus;
    }
    
    private EMShape buildTorusKnot(ElenaParser parser) {
      double R = Double.parseDouble(fields.get("R"));
      double r = Double.parseDouble(fields.get("r"));
      int p = Integer.parseInt(fields.get("p"));
      int q = Integer.parseInt(fields.get("q"));
      TorusKnot knot = new TorusKnot(R, r, p, q);
      
      if (fields.containsKey("transform")) {
        knot.setTransform(parser.parseTransform(fields.get("transform")));
      }
      
      if (fields.containsKey("material")) {
        Material mat=(Material) parser.objects.get(fields.get("material"));
        mat.setObjectTransform (knot.getInverseTransform ());
        knot.setMaterial(mat);
      }
      
      return knot;
    }
    
    private EMShape buildTriangle(ElenaParser parser) {
      Point3 v0 = parser.parsePoint3(fields.get("v0"));
      Point3 v1 = parser.parsePoint3(fields.get("v1"));
      Point3 v2 = parser.parsePoint3(fields.get("v2"));
      Triangle triangle = new Triangle(v0, v1, v2);
      
      if (fields.containsKey("transform")) {
        triangle.setTransform(parser.parseTransform(fields.get("transform")));
      }
      
      if (fields.containsKey("material")) {
        Material mat=(Material) parser.objects.get(fields.get("material"));
        mat.setObjectTransform (triangle.getInverseTransform ());
        triangle.setMaterial(mat);
      }
      
      return triangle;
    }
    
    private EMShape buildUnionCSG(ElenaParser parser) {
      EMShape left = (EMShape) parser.objects.get(fields.get("left"));
      EMShape right = (EMShape) parser.objects.get(fields.get("right"));
      UnionCSG csg = new UnionCSG(left, right);
      
      if (fields.containsKey("transform")) {
        csg.setTransform(parser.parseTransform(fields.get("transform")));
      }
      
      if (fields.containsKey("material")) {
        Material mat=(Material) parser.objects.get(fields.get("material"));
        mat.setObjectTransform (csg.getInverseTransform ());
        csg.setMaterial(mat);
      }
      
      return csg;
    }
    
    // END OF SHAPES
    
    // START of MATERIALS
    private Material buildAnodizedMetalMaterial (ElenaParser parser) {
      Material material = null;
      
      if (fields.size () < 1) {
        material = new AnodizedMetalMaterial ();
        } else {
        Color baseColor = parser.parseColor(fields.get("baseColor"));
        material = new AnodizedMetalMaterial (baseColor);
      }
      parser.objects.put (id, material);
      return material;
    }
    
    private Material buildBlackHoleMaterial(ElenaParser parser) {
      Material material;
      if (fields.containsKey("singularity")) {
        Point3 singularity = parser.parsePoint3(fields.get("singularity"));
        material = new BlackHoleMaterial(singularity, Matrix4.identity());
        } else {
        material = new BlackHoleMaterial(Matrix4.identity());
      }
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildBumpMaterial(ElenaParser parser) {
      Material base = (Material) parser.objects.get(fields.get("baseMaterial"));
      ImageTexture normalMap = (ImageTexture) parser.objects.get(fields.get("normalMap"));
      double strength = Double.parseDouble(fields.get("strength"));
      double uvScale = Double.parseDouble(fields.get("uvScale"));
      Material material = new BumpMaterial(base, normalMap, strength, uvScale, Matrix4.identity());
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildAuroraCeramicMaterial (ElenaParser parser) {
      Material material = null;
      
      if (fields.size () < 1) {
        material = new AuroraCeramicMaterial ();
        } else {
        Color baseColor = parser.parseColor(fields.get("baseColor"));
        Color auroraColor = parser.parseColor (fields.get ("auroraColor"));
        double auroraIntensity = Double.parseDouble(fields.get("auroraIntensity"));
        material = new AuroraCeramicMaterial (baseColor, auroraColor, auroraIntensity);
      }
      parser.objects.put (id, material);
      return material;
    }
    
    private Material buildCarpetTextureMaterial (ElenaParser parser) {
      Material material = null;
      
      if (fields.size () < 1) {
        material = new CarpetTextureMaterial ();
        } else {
        Color baseColor = parser.parseColor(fields.get("baseColor"));
        Color patternColor = parser.parseColor (fields.get ("patternColor"));
        material = new CarpetTextureMaterial (baseColor, patternColor);
      }
      parser.objects.put (id, material);
      return material;
    }
    
    private Material buildSultanKingMaterial (ElenaParser parser) {
      Material material = null;
      
      if (fields.size () < 1) {
        material = new SultanKingMaterial ();
        } else {
        Color goldColor = parser.parseColor(fields.get("goldColor"));
        Color rubyColor = parser.parseColor (fields.get ("rubyColor"));
        Color sapphireColor = parser.parseColor (fields.get ("sapphireColor"));
        double royaltyIntensity = Double.parseDouble(fields.get("royaltyIntensity"));
        material = new SultanKingMaterial (goldColor, rubyColor, sapphireColor, royaltyIntensity);
      }
      parser.objects.put (id, material);
      return material;
    }
    
    private Material buildBrunostCheeseMaterial (ElenaParser parser) {
      Material material = null;
      
      if (fields.size () < 1) {
        material = new BrunostCheeseMaterial ();
        } else {
        Color cheeseColor = parser.parseColor(fields.get("cheeseColor"));
        Color caramelColor = parser.parseColor (fields.get ("caramelColor"));
        double caramelAmount = Double.parseDouble(fields.get("caramelAmount"));
        material = new BrunostCheeseMaterial(cheeseColor, caramelColor, caramelAmount);
      }
      parser.objects.put (id, material);
      return material;
    }
    
    private Material buildNorwegianRoseMaterial (ElenaParser parser) {
      Material material = null;
      
      if (fields.size () < 1) {
        material = new NorwegianRoseMaterial ();
        } else {
        Color woodColor = parser.parseColor(fields.get("woodColor"));
        Color roseColor = parser.parseColor (fields.get ("roseColor"));
        material = new NorwegianRoseMaterial(woodColor, roseColor);
      }
      parser.objects.put (id, material);
      return material;
    }
    
    private Material buildTurkishTileMaterial (ElenaParser parser) {
      Material material = null;
      
      if (fields.size () < 1) {
        material = new TurkishTileMaterial ();
        } else {
        Color baseColor = parser.parseColor(fields.get("baseColor"));
        Color patternColor = parser.parseColor (fields.get ("patternColor"));
        double tileSize = Double.parseDouble(fields.get("tileSize"));
        material = new TurkishTileMaterial(baseColor, patternColor, tileSize);
      }
      parser.objects.put (id, material);
      return material;
    }
    
    private Material buildHotCopperMaterial (ElenaParser parser) {
      Material material = null;
      
      if (fields.size () < 1) {
        material = new HotCopperMaterial ();
        } else {
        Color copperColor = parser.parseColor(fields.get("copperColor"));
        Color patinaColor = parser.parseColor (fields.get ("patinaColor"));
        double patinaAmount = Double.parseDouble(fields.get("patinaAmount"));
        material = new HotCopperMaterial(copperColor, patinaColor, patinaAmount);
      }
      parser.objects.put (id, material);
      return material;
    }
    
    private Material buildProceduralCloudMaterial (ElenaParser parser) {
      Material material = null;
      
      Color baseColor = parser.parseColor(fields.get("baseColor"));
      Color highlightColor = parser.parseColor (fields.get ("highlightColor"));
      
      material = new ProceduralCloudMaterial(baseColor, highlightColor);
      
      parser.objects.put (id, material);
      return material;
    }
    
    private Material buildLinearGradientMaterial (ElenaParser parser) {
      Material material = null;
      
      Color topColor = parser.parseColor(fields.get("topColor"));
      Color bottomColor = parser.parseColor (fields.get ("bottomColor"));
      
      material = new LinearGradientMaterial(topColor, bottomColor);
      
      parser.objects.put (id, material);
      return material;
    }
    
    private Material buildRadialGradientMaterial (ElenaParser parser) {
      Material material = null;
      
      Color centerColor = parser.parseColor(fields.get("centerColor"));
      Color edgeColor = parser.parseColor (fields.get ("edgeColor"));
      
      material = new RadialGradientMaterial(centerColor, edgeColor);
      
      parser.objects.put (id, material);
      return material;
    }
    
    private Material buildCalligraphyRuneMaterial (ElenaParser parser) {
      Material material = null;
      
      if (fields.size () < 1) {
        material = new CalligraphyRuneMaterial ();
        } else {
        Color parchmentColor = parser.parseColor(fields.get("parchmentColor"));
        Color inkColor = parser.parseColor (fields.get ("inkColor"));
        Color goldLeafColor = parser.parseColor (fields.get ("goldLeafColor"));
        double writingIntensity = Double.parseDouble(fields.get("writingIntensity"));
        material = new CalligraphyRuneMaterial(parchmentColor, inkColor, goldLeafColor, writingIntensity);
      }
      parser.objects.put (id, material);
      return material;
    }
    
    private Material buildVikingRuneMaterial (ElenaParser parser) {
      Material material = null;
      
      if (fields.size () < 1) {
        material = new VikingRuneMaterial ();
        } else {
        Color stoneColor = parser.parseColor(fields.get("stoneColor"));
        Color runeColor = parser.parseColor (fields.get ("runeColor"));
        double runeDepth = Double.parseDouble(fields.get("runeDepth"));
        material = new VikingRuneMaterial(stoneColor, runeColor, runeDepth);
      }
      parser.objects.put (id, material);
      return material;
    }
    
    private Material buildCustomMaterial(ElenaParser parser) {
      Material material = null;
      
      String className = fields.get("className");
      
      if (className == null || className.trim().isEmpty()) {
        throw new IllegalArgumentException("CustomMaterial requires className parameter");
      }
      
      // Create parameter map excluding className
      Map<String, String> params = new HashMap<>();
      for (Map.Entry<String, String> entry : fields.entrySet()) {
        if (!entry.getKey().equals("className")) {
          params.put(entry.getKey(), entry.getValue());
        }
      }
      
      // Yeni imzaya uygun şekilde çağırın
      material = loadExternalMaterial(className, params);
      
      parser.objects.put(id, material);
      return material;
    }
    
    private EMShape buildCustomShape(ElenaParser parser) {
      EMShape myshape = null;
      
      String className = fields.get("className");
      
      if (className == null || className.trim().isEmpty()) {
        throw new IllegalArgumentException("CustomShape requires className parameter");
      }
      
      // Create parameter map excluding className
      Map<String, String> params = new HashMap<>();
      for (Map.Entry<String, String> entry : fields.entrySet()) {
        if (!entry.getKey().equals("className")) {
          params.put(entry.getKey(), entry.getValue());
        }
      }
      
      // Load the external shape
      myshape = loadExternalShape(className, params);
      
      if (myshape == null) {
        myshape = new Sphere(0.5);
      }
      
      // Debugging: Check if transform exists
      if (fields.containsKey("transform")) {
        System.out.println("Applying transform: " + fields.get("transform"));
        myshape.setTransform(parser.parseTransform(fields.get("transform")));
        } else {
        System.out.println("No transform found for CustomShape.");
      }
      
      if (fields.containsKey("material")) {
        Material mat = (Material) parser.objects.get(fields.get("material"));
        mat.setObjectTransform(myshape.getInverseTransform());
        myshape.setMaterial(mat);
      }
      
      return myshape;
    }
    
    private Material buildRuneStoneMaterial (ElenaParser parser) {
      Material material = null;
      
      if (fields.size () < 1) {
        material = new RuneStoneMaterial ();
        } else {
        Color stoneColor = parser.parseColor(fields.get("stoneColor"));
        Color runeColor = parser.parseColor (fields.get ("runeColor"));
        double runeDensity = Double.parseDouble(fields.get("runeDensity"));
        material = new RuneStoneMaterial(stoneColor, runeColor, runeDensity);
      }
      parser.objects.put (id, material);
      return material;
    }
    
    private Material buildRosemalingMaterial (ElenaParser parser) {
      Material material = null;
      
      if (fields.size () < 1) {
        material = new RosemalingMaterial ();
        } else {
        Color backgroundColor = parser.parseColor(fields.get("backgroundColor"));
        Color flowerColor = parser.parseColor (fields.get ("flowerColor"));
        Color accentColor = parser.parseColor (fields.get ("accentColor"));
        double patternDensity = Double.parseDouble(fields.get("patternDensity"));
        material = new RosemalingMaterial(backgroundColor, flowerColor, accentColor, patternDensity);
      }
      parser.objects.put (id, material);
      return material;
    }
    
    private Material buildKilimRosemalingMaterial (ElenaParser parser) {
      Material material = null;
      
      if (fields.size () < 1) {
        material = new KilimRosemalingMaterial ();
        } else {
        Color kilimColor = parser.parseColor(fields.get("kilimColor"));
        Color rosemalingColor = parser.parseColor (fields.get ("rosemalingColor"));
        Color accentColor = parser.parseColor (fields.get ("accentColor"));
        double patternIntensity = Double.parseDouble(fields.get("patternIntensity"));
        material = new KilimRosemalingMaterial(kilimColor, rosemalingColor, accentColor, patternIntensity);
      }
      parser.objects.put (id, material);
      return material;
    }
    
    private Material buildHamamSaunaMaterial (ElenaParser parser) {
      Material material = null;
      
      if (fields.size () < 1) {
        material = new HamamSaunaMaterial ();
        } else {
        Color marbleColor = parser.parseColor(fields.get("marbleColor"));
        Color woodColor = parser.parseColor (fields.get ("woodColor"));
        Color steamColor = parser.parseColor (fields.get ("steamColor"));
        double steamIntensity = Double.parseDouble(fields.get("steamIntensity"));
        material = new HamamSaunaMaterial(marbleColor, woodColor, steamColor, steamIntensity);
      }
      parser.objects.put (id, material);
      return material;
    }
    
    private Material buildTransparentColorMaterial (ElenaParser parser) {
      Material material = null;
      
      double transparency = 0.5;
      double reflectivity = 0.0;
      double ior = 1.5;
      
      if (fields.containsKey("transparency")) {
        transparency = Double.parseDouble(fields.get("transparency"));
      }
      
      if (fields.containsKey("reflectivity")) {
        reflectivity = Double.parseDouble(fields.get("reflectivity"));
      }
      
      if (fields.containsKey("ior")) {
        ior = Double.parseDouble(fields.get("ior"));
      }
      
      material = new TransparentColorMaterial(transparency, reflectivity, ior);
      
      parser.objects.put (id, material);
      return material;
    }
    
    private Material buildFjordCrystalMaterial (ElenaParser parser) {
      Material material = null;
      
      if (fields.size () < 1) {
        material = new FjordCrystalMaterial ();
        } else {
        Color waterColor = parser.parseColor(fields.get("waterColor"));
        Color crystalColor = parser.parseColor (fields.get ("crystalColor"));
        double clarity = Double.parseDouble(fields.get("clarity"));
        material = new FjordCrystalMaterial(waterColor, crystalColor, clarity);
      }
      parser.objects.put (id, material);
      return material;
    }
    
    
    private Material buildNorthernLightMaterial (ElenaParser parser) {
      Material material = null;
      
      if (fields.size () < 1) {
        material = new NorthernLightMaterial ();
        } else {
        Color primaryAurora = parser.parseColor(fields.get("primaryAurora"));
        Color secondaryAurora = parser.parseColor (fields.get ("secondaryAurora"));
        double intensity = Double.parseDouble(fields.get("intensity"));
        material = new NorthernLightMaterial(primaryAurora, secondaryAurora, intensity);
      }
      parser.objects.put (id, material);
      return material;
    }
    
    private Material buildCoffeeFjordMaterial (ElenaParser parser) {
      Material material = null;
      
      if (fields.size () < 1) {
        material = new CoffeeFjordMaterial ();
        } else {
        Color coffeeColor = parser.parseColor(fields.get("coffeeColor"));
        Color fjordColor = parser.parseColor (fields.get ("fjordColor"));
        double blendIntensity = Double.parseDouble(fields.get("blendIntensity"));
        material = new CoffeeFjordMaterial(coffeeColor, fjordColor, blendIntensity);
      }
      parser.objects.put (id, material);
      return material;
    }
    
    private Material buildVikingMetalMaterial (ElenaParser parser) {
      Material material = null;
      
      if (fields.size () < 1) {
        material = new VikingMetalMaterial ();
        } else {
        Color baseColor = parser.parseColor(fields.get("baseColor"));
        Color rustColor = parser.parseColor (fields.get ("rustColor"));
        double rustDensity = Double.parseDouble(fields.get("rustDensity"));
        material = new VikingMetalMaterial(baseColor, rustColor, rustDensity);
      }
      parser.objects.put (id, material);
      return material;
    }
    
    private Material buildNordicWoodMaterial (ElenaParser parser) {
      Material material = null;
      
      if (fields.size () < 1) {
        material = new NordicWoodMaterial ();
        } else {
        Color woodColor = parser.parseColor(fields.get("woodColor"));
        Color grainColor = parser.parseColor (fields.get ("grainColor"));
        double grainIntensity = Double.parseDouble(fields.get("grainIntensity"));
        material = new NordicWoodMaterial(woodColor, grainColor, grainIntensity);
      }
      parser.objects.put (id, material);
      return material;
    }
    
    private Material buildNordicWeaveMaterial (ElenaParser parser) {
      Material material = null;
      
      if (fields.size () < 1) {
        material = new NordicWeaveMaterial ();
        } else {
        Color primaryColor = parser.parseColor(fields.get("primaryColor"));
        Color secondaryColor = parser.parseColor (fields.get ("secondaryColor"));
        Color accentColor = parser.parseColor (fields.get ("accentColor"));
        double patternScale = Double.parseDouble(fields.get("patternScale"));
        material = new NordicWeaveMaterial(primaryColor, secondaryColor, accentColor, patternScale);
      }
      parser.objects.put (id, material);
      return material;
    }
    
    private Material buildTulipFjordMaterial (ElenaParser parser) {
      Material material = null;
      
      if (fields.size () < 1) {
        material = new TulipFjordMaterial ();
        } else {
        Color tulipColor = parser.parseColor(fields.get("tulipColor"));
        Color fjordColor = parser.parseColor (fields.get ("fjordColor"));
        Color stemColor = parser.parseColor (fields.get ("stemColor"));
        double bloomIntensity = Double.parseDouble(fields.get("bloomIntensity"));
        material = new TulipFjordMaterial(tulipColor, fjordColor, stemColor, bloomIntensity);
      }
      parser.objects.put (id, material);
      return material;
    }
    
    private Material buildTelemarkPatternMaterial (ElenaParser parser) {
      Material material = null;
      
      if (fields.size () < 1) {
        material = new TelemarkPatternMaterial ();
        } else {
        Color baseColor = parser.parseColor(fields.get("baseColor"));
        Color patternColor = parser.parseColor (fields.get ("patternColor"));
        Color accentColor = parser.parseColor (fields.get ("accentColor"));
        double patternScale = Double.parseDouble(fields.get("patternScale"));
        material = new TelemarkPatternMaterial(baseColor, patternColor, accentColor, patternScale);
      }
      parser.objects.put (id, material);
      return material;
    }
    
    private Material buildCheckerboardMaterial(ElenaParser parser) {
      Color c1 = parser.parseColor(fields.get("color1"));
      Color c2 = parser.parseColor(fields.get("color2"));
      double size = Double.parseDouble(fields.get("size"));
      Material material;
      if (fields.containsKey("ambientCoeff")) {
        double amb = Double.parseDouble(fields.get("ambientCoeff"));
        double diff = Double.parseDouble(fields.get("diffuseCoeff"));
        double spec = Double.parseDouble(fields.get("specularCoeff"));
        double shin = Double.parseDouble(fields.get("shininess"));
        Color specColor = parser.parseColor(fields.get("specularColor"));
        double refl = Double.parseDouble(fields.get("reflectivity"));
        double ior = Double.parseDouble(fields.get("ior"));
        double trans = Double.parseDouble(fields.get("transparency"));
        material = new CheckerboardMaterial(c1, c2, size, amb, diff, spec, shin, specColor, refl, ior, trans, Matrix4.identity());
        } else {
        material = new CheckerboardMaterial(c1, c2, size, Matrix4.identity());
      }
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildCircleTextureMaterial(ElenaParser parser) {
      Color solid = parser.parseColor(fields.get("solidColor"));
      Color pattern = parser.parseColor(fields.get("patternColor"));
      double patternSize = Double.parseDouble(fields.get("patternSize"));
      Material material;
      if (fields.containsKey("ambientCoefficient")) {
        double amb = Double.parseDouble(fields.get("ambientCoefficient"));
        double diff = Double.parseDouble(fields.get("diffuseCoefficient"));
        double spec = Double.parseDouble(fields.get("specularCoefficient"));
        double shin = Double.parseDouble(fields.get("shininess"));
        double refl = Double.parseDouble(fields.get("reflectivity"));
        double ior = Double.parseDouble(fields.get("ior"));
        double trans = Double.parseDouble(fields.get("transparency"));
        material = new CircleTextureMaterial(solid, pattern, patternSize, amb, diff, spec, shin, refl, ior, trans, Matrix4.identity());
        } else {
        material = new CircleTextureMaterial(solid, pattern, patternSize, Matrix4.identity());
      }
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildCopperMaterial(ElenaParser parser) {
      Material material = new CopperMaterial();
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildCrystalClearMaterial(ElenaParser parser) {
      Color tint = parser.parseColor(fields.get("glassTint"));
      double clarity = Double.parseDouble(fields.get("clarity"));
      double ior = Double.parseDouble(fields.get("ior"));
      double dispersion = Double.parseDouble(fields.get("dispersion"));
      Material material = new CrystalClearMaterial(tint, clarity, ior, dispersion, Matrix4.identity());
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildDamaskCeramicMaterial(ElenaParser parser) {
      Color primary = parser.parseColor(fields.get("primary"));
      Color secondary = parser.parseColor(fields.get("secondary"));
      double shininess = Double.parseDouble(fields.get("shininess"));
      Material material;
      if (fields.containsKey("ambient")) {
        double ambient = Double.parseDouble(fields.get("ambient"));
        double specular = Double.parseDouble(fields.get("specular"));
        material = new DamaskCeramicMaterial(primary, secondary, shininess, ambient, specular, Matrix4.identity());
        } else {
        material = new DamaskCeramicMaterial(primary, secondary, shininess, Matrix4.identity());
      }
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildDewDropMaterial(ElenaParser parser) {
      Color base = parser.parseColor(fields.get("baseColor"));
      Color drop = parser.parseColor(fields.get("dropColor"));
      double density = Double.parseDouble(fields.get("dropDensity"));
      double size = Double.parseDouble(fields.get("dropSize"));
      Material material;
      if (fields.containsKey("ambient")) {
        double amb = Double.parseDouble(fields.get("ambient"));
        double diff = Double.parseDouble(fields.get("diffuse"));
        double spec = Double.parseDouble(fields.get("specular"));
        double shin = Double.parseDouble(fields.get("shininess"));
        double refl = Double.parseDouble(fields.get("reflectivity"));
        double ior = Double.parseDouble(fields.get("ior"));
        double trans = Double.parseDouble(fields.get("transparency"));
        material = new DewDropMaterial(base, drop, density, size, amb, diff, spec, shin, refl, ior, trans, Matrix4.identity());
        } else {
        material = new DewDropMaterial(base, drop, density, size, Matrix4.identity());
      }
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildDiagonalCheckerMaterial(ElenaParser parser) {
      Color c1 = parser.parseColor(fields.get("color1"));
      Color c2 = parser.parseColor(fields.get("color2"));
      double scale = Double.parseDouble(fields.get("scale"));
      Material material;
      if (fields.containsKey("ambient")) {
        double amb = Double.parseDouble(fields.get("ambient"));
        double diff = Double.parseDouble(fields.get("diffuse"));
        double spec = Double.parseDouble(fields.get("specular"));
        double shin = Double.parseDouble(fields.get("shininess"));
        Color specColor = parser.parseColor(fields.get("specularColor"));
        double refl = Double.parseDouble(fields.get("reflectivity"));
        double ior = Double.parseDouble(fields.get("ior"));
        double trans = Double.parseDouble(fields.get("transparency"));
        material = new DiagonalCheckerMaterial(c1, c2, scale, amb, diff, spec, shin, specColor, refl, ior, trans, Matrix4.identity());
        } else {
        material = new DiagonalCheckerMaterial(c1, c2, scale, Matrix4.identity());
      }
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildDiffuseMaterial(ElenaParser parser) {
      Color color = parser.parseColor(fields.get("color"));
      Material material;
      if (fields.containsKey("diffuseCoefficient")) {
        double diff = Double.parseDouble(fields.get("diffuseCoefficient"));
        double refl = Double.parseDouble(fields.get("reflectivity"));
        double ior = Double.parseDouble(fields.get("ior"));
        double trans = Double.parseDouble(fields.get("transparency"));
        material = new DiffuseMaterial(color, diff, refl, ior, trans);
        } else {
        material = new DiffuseMaterial(color);
      }
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildElenaTextureMaterial(ElenaParser parser) {
      String path = fields.get("imagePath").replace("\"", "");
      Material material = null;
      if (fields.containsKey("ambientCoeff")) {
        double amb = Double.parseDouble(fields.get("ambientCoeff"));
        double diff = Double.parseDouble(fields.get("diffuseCoeff"));
        double spec = Double.parseDouble(fields.get("specularCoeff"));
        double shin = Double.parseDouble(fields.get("shininess"));
        Color specColor = parser.parseColor(fields.get("specularColor"));
        double refl = Double.parseDouble(fields.get("reflectivity"));
        double ior = Double.parseDouble(fields.get("ior"));
        double trans = Double.parseDouble(fields.get("transparency"));
        try {
          material = new ElenaTextureMaterial(path, amb, diff, spec, shin, specColor, refl, ior, trans, Matrix4.identity());
        } catch (IOException ioe) {}
        } else {
        try {
          material = new ElenaTextureMaterial(path, Matrix4.identity());
        } catch (IOException ioe) {}
      }
      if (material != null) {
        parser.objects.put(id, material);
      }
      return material;
    }
    
    private Material buildEmissiveMaterial(ElenaParser parser) {
      Color color = parser.parseColor(fields.get("emissiveColor"));
      double strength = Double.parseDouble(fields.get("emissiveStrength"));
      Material material = new EmissiveMaterial(color, strength);
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildFractalBarkMaterial(ElenaParser parser) {
      Material material;
      if (fields.containsKey("roughness")) {
        double rough = Double.parseDouble(fields.get("roughness"));
        material = new FractalBarkMaterial(Matrix4.identity(), rough);
        } else {
        material = new FractalBarkMaterial(Matrix4.identity());
      }
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildFractalFireMaterial(ElenaParser parser) {
      int iter = Integer.parseInt(fields.get("iterations"));
      double chaos = Double.parseDouble(fields.get("chaos"));
      double scale = Double.parseDouble(fields.get("scale"));
      double speed = Double.parseDouble(fields.get("speed"));
      Material material = new FractalFireMaterial(iter, chaos, scale, speed);
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildDielectricMaterial(ElenaParser parser) {
      Material material;
      if (fields.size() < 1) {
        material = new DielectricMaterial();
        } else {
        Color diffuseColor = parser.parseColor(fields.get("diffuseColor"));
        Color filterColorInside = parser.parseColor(fields.get("filterColorInside"));
        Color filterColorOutside = parser.parseColor(fields.get("filterColorOutside"));
        double ior = Double.parseDouble(fields.get("ior"));
        double transparency = Double.parseDouble(fields.get("transparency"));
        double reflectivity = Double.parseDouble(fields.get("reflectivity"));
        material = new DielectricMaterial(diffuseColor, ior, transparency, reflectivity);
        ((DielectricMaterial)(material)).setFilterColorInside (filterColorInside);
        ((DielectricMaterial)(material)).setFilterColorOutside (filterColorOutside);
      }
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildGoldMaterial(ElenaParser parser) {
      Material material = new GoldMaterial();
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildGradientChessMaterial(ElenaParser parser) {
      Color c1 = parser.parseColor(fields.get("baseColor1"));
      Color c2 = parser.parseColor(fields.get("baseColor2"));
      double size = Double.parseDouble(fields.get("squareSize"));
      Material material;
      if (fields.containsKey("ambient")) {
        double amb = Double.parseDouble(fields.get("ambient"));
        double diff = Double.parseDouble(fields.get("diffuse"));
        double spec = Double.parseDouble(fields.get("specular"));
        double shin = Double.parseDouble(fields.get("shininess"));
        double refl = Double.parseDouble(fields.get("reflectivity"));
        double ior = Double.parseDouble(fields.get("ior"));
        double trans = Double.parseDouble(fields.get("transparency"));
        material = new GradientChessMaterial(c1, c2, size, amb, diff, spec, shin, refl, ior, trans, Matrix4.identity());
        } else {
        material = new GradientChessMaterial(c1, c2, size, Matrix4.identity());
      }
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildGradientImageTextMaterial(ElenaParser parser) {
      String text = fields.get("text").replace("\"", "");
      text = convertToLatin (text);
      String imagePath = fields.get ("imagePath").replace ("\"", "");
      double trans = Double.parseDouble(fields.get("transparency"));
      BufferedImage img = parser.loadImage (imagePath);
      Material material;
      if (fields.containsKey("bgStart")) {
        Color bgStart = parser.parseColor(fields.get("bgStart"));
        Color bgEnd = parser.parseColor(fields.get("bgEnd"));
        Color textStart = parser.parseColor(fields.get("textStart"));
        Color textEnd = parser.parseColor(fields.get("textEnd"));
        float bgAlpha = Float.parseFloat (fields.get ("bgAlpha"));
        if (bgAlpha < 0.01F) bgAlpha = 0.01F;
        if (bgAlpha > 1F) bgAlpha = 1F;
        float textAlpha = Float.parseFloat (fields.get ("textAlpha"));
        if (textAlpha < 0.01F) textAlpha = 0.01F;
        if (textAlpha > 1F) textAlpha = 1F;
        String fontName = fields.get("font").replace("\"", "");
        int fontSize = Integer.parseInt(fields.get("fontSize"));
        int fontStyle = Integer.parseInt(fields.get("fontStyle"));
        Font font = new Font(fontName, fontStyle, fontSize);
        StripeDirection dir = StripeDirection.valueOf(fields.get("direction"));
        double refl = Double.parseDouble(fields.get("reflectivity"));
        double ior = Double.parseDouble(fields.get("ior"));
        int xOffset = Integer.parseInt(fields.get("xOffset"));
        int yOffset = Integer.parseInt(fields.get("yOffset"));
        int imgOffsetX = Integer.parseInt(fields.get("imgOffsetX"));
        int imgOffsetY = Integer.parseInt(fields.get("imgOffsetY"));
        boolean isWrap = Boolean.parseBoolean (fields.get ("isWrap"));
        material = new GradientImageTextMaterial(bgStart, bgEnd, textStart, textEnd, getBufferedImage (imagePath), bgAlpha, textAlpha, text, font, dir, refl, ior, trans, Matrix4.identity(), xOffset, yOffset, imgOffsetX, imgOffsetY, isWrap);
        } else if (fields.containsKey("xOffset")) {
        int xOffset = Integer.parseInt(fields.get("xOffset"));
        int yOffset = Integer.parseInt(fields.get("yOffset"));
        material = new GradientImageTextMaterial(img, text, xOffset, yOffset);
        } else {
        material = new GradientImageTextMaterial(img, text);
      }
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildWordMaterial(ElenaParser parser) {
      String text = fields.get("text").replace("\"", "");
      text = convertToLatin(text);
      
      // Default values
      Color foregroundColor = Color.WHITE;
      Color backgroundColor = new Color(0x00000000, true); // Fully transparent
      Font font = new Font("Arial", Font.BOLD, 48);
      BufferedImage wordImage = null;
      boolean gradientEnabled = false;
      boolean isTriangleEtc = false;
      Color gradientColor = null;
      int width = 256;
      int height = 256;
      
      // Optional parameters
      if (fields.containsKey("foregroundColor")) {
        foregroundColor = parser.parseColor(fields.get("foregroundColor"));
      }
      
      if (fields.containsKey("backgroundColor")) {
        backgroundColor = parser.parseColor(fields.get("backgroundColor"));
      }
      
      if (fields.containsKey("fontName")) {
        String fontName = fields.get("fontName").replace("\"", "");
        int fontSize = fields.containsKey("fontSize") ?
        Integer.parseInt(fields.get("fontSize")) : 48;
        int fontStyle = fields.containsKey("fontStyle") ?
        Integer.parseInt(fields.get("fontStyle")) : Font.BOLD;
        font = new Font(fontName, fontStyle, fontSize);
      }
      
      if (fields.containsKey("wordImage")) {
        String imagePath = fields.get("wordImage").replace("\"", "");
        if (imagePath.trim().equals("null")) {
          wordImage = null;
          } else {
          wordImage = parser.loadImage(imagePath);
        }
      }
      
      if (fields.containsKey("gradientColor")) {
        gradientEnabled = true;
        gradientColor = parser.parseColor(fields.get("gradientColor"));
      }
      
      if (fields.containsKey("isTriangleEtc")) {
        isTriangleEtc = Boolean.parseBoolean(fields.get("isTriangleEtc"));
      }
      
      // Size parameters
      if (fields.containsKey("width")) {
        width = Integer.parseInt(fields.get("width"));
      }
      
      if (fields.containsKey("height")) {
        height = Integer.parseInt(fields.get("height"));
      }
      
      // UV parameters
      double uOffset = fields.containsKey("uOffset") ?
      Double.parseDouble(fields.get("uOffset")) : 0.0;
      double vOffset = fields.containsKey("vOffset") ?
      Double.parseDouble(fields.get("vOffset")) : 0.0;
      double uScale = fields.containsKey("uScale") ?
      Double.parseDouble(fields.get("uScale")) : 1.0;
      double vScale = fields.containsKey("vScale") ?
      Double.parseDouble(fields.get("vScale")) : 1.0;
      boolean isRepeatTexture = fields.containsKey("isRepeatTexture") ?
      Boolean.parseBoolean(fields.get("isRepeatTexture")) : false;
      
      // Create WordMaterial with size parameters
      WordMaterial material = new WordMaterial(text, foregroundColor, backgroundColor,
      font, gradientEnabled, gradientColor, wordImage, width, height);
      
      // Set UV parameters
      material.setUOffset(uOffset);
      material.setVOffset(vOffset);
      material.setUScale(uScale);
      material.setVScale(vScale);
      material.setRepeatTexture(isRepeatTexture);
      material.setTriangleEtc(isTriangleEtc);
      
      parser.objects.put(id, material);
      return material;
    }
    
    ///
    private final BufferedImage getBufferedImage (String pathimg) {
      BufferedImage timp=null;
      
      try {
        timp = ImageIO.read (new File (pathimg));
      }
      catch (IOException ioe) {
        ioe.printStackTrace ();
        timp = null;
      }
      
      return timp;
    }
    ///
    
    private Material buildGradientTextMaterial(ElenaParser parser) {
      String text = fields.get("text").replace("\"", "");
      text = convertToLatin (text);
      Material material;
      if (fields.containsKey("bgStart")) {
        Color bgStart = parser.parseColor(fields.get("bgStart"));
        Color bgEnd = parser.parseColor(fields.get("bgEnd"));
        Color textStart = parser.parseColor(fields.get("textStart"));
        Color textEnd = parser.parseColor(fields.get("textEnd"));
        String fontName = fields.get("font").replace("\"", "");
        int fontSize = Integer.parseInt(fields.get("fontSize"));
        int fontStyle = Integer.parseInt(fields.get("fontStyle"));
        Font font = new Font(fontName, fontStyle, fontSize);
        StripeDirection dir = StripeDirection.valueOf(fields.get("direction"));
        double refl = Double.parseDouble(fields.get("reflectivity"));
        double ior = Double.parseDouble(fields.get("ior"));
        double trans = Double.parseDouble(fields.get("transparency"));
        int xOffset = Integer.parseInt(fields.get("xOffset"));
        int yOffset = Integer.parseInt(fields.get("yOffset"));
        material = new GradientTextMaterial(bgStart, bgEnd, textStart, textEnd, text, font, dir, refl, ior, trans, Matrix4.identity(), xOffset, yOffset);
        } else if (fields.containsKey("xOffset")) {
        int xOffset = Integer.parseInt(fields.get("xOffset"));
        int yOffset = Integer.parseInt(fields.get("yOffset"));
        material = new GradientTextMaterial(text, xOffset, yOffset);
        } else {
        material = new GradientTextMaterial(text);
      }
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildHexagonalHoneycombMaterial(ElenaParser parser) {
      Color primary = parser.parseColor(fields.get("primary"));
      Color secondary = parser.parseColor(fields.get("secondary"));
      Material material;
      if (fields.containsKey("borderColor")) {
        Color border = parser.parseColor(fields.get("borderColor"));
        double cellSize = Double.parseDouble(fields.get("cellSize"));
        double borderWidth = Double.parseDouble(fields.get("borderWidth"));
        double amb = Double.parseDouble(fields.get("ambientStrength"));
        double spec = Double.parseDouble(fields.get("specularStrength"));
        double shin = Double.parseDouble(fields.get("shininess"));
        material = new HexagonalHoneycombMaterial(primary, secondary, border, cellSize, borderWidth, amb, spec, shin);
        } else {
        double cellSize = Double.parseDouble(fields.get("cellSize"));
        double borderWidth = Double.parseDouble(fields.get("borderWidth"));
        material = new HexagonalHoneycombMaterial(primary, secondary, cellSize, borderWidth);
      }
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildTransparentPNGMaterial(ElenaParser parser) {
      final int fsize = fields.size();
      
      Material material = null;
      
      if (fsize == 0x0000) {
        // No parameters: create default material
        material = new TransparentPNGMaterial();
        } else {
        // Parse image path if present
        String path = fields.get("imagePath");
        BufferedImage img = null;
        if (path != null) {
          path = path.replace("\"", "");
          img = parser.loadImage(path);
        }
        
        // Parse UV offset and scale parameters with defaults
        double uOffset = 0.0;
        double vOffset = 0.0;
        double uScale = 1.0;
        double vScale = 1.0;
        boolean isRepeatTexture = false;
        
        try {
          if (fields.containsKey("uOffset")) {
            uOffset = Double.parseDouble(fields.get("uOffset"));
          }
          if (fields.containsKey("vOffset")) {
            vOffset = Double.parseDouble(fields.get("vOffset"));
          }
          if (fields.containsKey("uScale")) {
            uScale = Double.parseDouble(fields.get("uScale"));
            if (uScale <= 0.0) uScale = 1.0; // Prevent invalid scale
            }
          if (fields.containsKey("vScale")) {
            vScale = Double.parseDouble(fields.get("vScale"));
            if (vScale <= 0.0) vScale = 1.0;
          }
          if (fields.containsKey("isRepeatTexture")) {
            String val = fields.get("isRepeatTexture").toLowerCase();
            isRepeatTexture = val.equals("true") || val.equals("1");
          }
          } catch (NumberFormatException e) {
          // Handle parse errors gracefully, fallback to defaults
          uOffset = 0.0;
          vOffset = 0.0;
          uScale = 1.0;
          vScale = 1.0;
          isRepeatTexture = false;
        }
        
        if (img != null) {
          // Create material with texture and UV parameters
          material = new TransparentPNGMaterial(img, uOffset, vOffset, uScale, vScale, isRepeatTexture);
          } else {
          // No valid image, create default material
          material = new TransparentPNGMaterial();
        }
      }
      
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildTransparentEmissivePNGMaterial(ElenaParser parser) {
      // Parse image path
      String path = fields.get("imagePath");
      BufferedImage img = null;
      if (path != null) {
        path = path.replace("\"", "");
        img = parser.loadImage(path);
      }
      
      // Parse UV parameters with defaults
      double uOffset = fields.containsKey("uOffset") ? Double.parseDouble(fields.get("uOffset")) : 0.0;
      double vOffset = fields.containsKey("vOffset") ? Double.parseDouble(fields.get("vOffset")) : 0.0;
      double uScale = fields.containsKey("uScale") ? Double.parseDouble(fields.get("uScale")) : 1.0;
      double vScale = fields.containsKey("vScale") ? Double.parseDouble(fields.get("vScale")) : 1.0;
      boolean isRepeatTexture = fields.containsKey("isRepeatTexture") ? Boolean.parseBoolean(fields.get("isRepeatTexture")) : false;
      
      // Parse emissive properties with DEFAULTS
      Color emissiveColor = Color.WHITE; // Default color
      if (fields.containsKey("emissiveColor")) {
        emissiveColor = parser.parseColor(fields.get("emissiveColor"));
      }
      
      double emissiveStrength = 0.0; // Default strength
      if (fields.containsKey("emissiveStrength")) {
        emissiveStrength = Double.parseDouble(fields.get("emissiveStrength"));
      }
      
      // Create material
      TransparentEmissivePNGMaterial material = new TransparentEmissivePNGMaterial(
        img, uOffset, vOffset, uScale, vScale, isRepeatTexture,
        emissiveColor, emissiveStrength
      );
      
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildThresholdMaterial(ElenaParser parser) {
      final int fsize = fields.size();
      
      Material material = null;
      
      if (fsize == 0x0000) {
        // No parameters: create default material with default values
        material = new ThresholdMaterial(Color.GRAY, 0.5);
        } else {
        // Parse base color with default
        Color baseColor = Color.GRAY;
        if (fields.containsKey("baseColor")) {
          baseColor = parseColor(fields.get("baseColor"));
        }
        
        // Parse threshold value with validation (0.0-1.0)
        double threshold = 0.5;
        try {
          if (fields.containsKey("threshold")) {
            threshold = Double.parseDouble(fields.get("threshold"));
            threshold = Math.max(0.0, Math.min(1.0, threshold)); // Clamp to valid range
          }
          } catch (NumberFormatException e) {
          threshold = 0.5; // Default on parse error
        }
        
        // Parse aboveColor with default
        Color aboveColor = Color.WHITE;
        if (fields.containsKey("aboveColor")) {
          aboveColor = parseColor(fields.get("aboveColor"));
        }
        
        // Parse belowColor with default
        Color belowColor = Color.BLACK;
        if (fields.containsKey("belowColor")) {
          belowColor = parseColor(fields.get("belowColor"));
        }
        
        // Parse boolean flags with defaults
        boolean useLightColor = false;
        boolean invertThreshold = false;
        
        if (fields.containsKey("useLightColor")) {
          String val = fields.get("useLightColor").toLowerCase();
          useLightColor = val.equals("true") || val.equals("1");
        }
        
        if (fields.containsKey("invertThreshold")) {
          String val = fields.get("invertThreshold").toLowerCase();
          invertThreshold = val.equals("true") || val.equals("1");
        }
        
        // Create material with all parsed parameters
        material = new ThresholdMaterial(baseColor, threshold, aboveColor, belowColor,
        useLightColor, invertThreshold);
      }
      
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildBrightnessMaterial(ElenaParser parser) {
      final int fsize = fields.size();
      
      Material material = null;
      
      if (fsize == 0x0000) {
        // No parameters: create default material with default values
        material = new BrightnessMaterial(Color.GRAY, 1.0, false);
        } else {
        // Parse base color with default
        Color baseColor = Color.GRAY;
        if (fields.containsKey("baseColor")) {
          baseColor = parseColor(fields.get("baseColor"));
        }
        
        // Parse brightness value with validation
        double brightness = 1.0;
        if (fields.containsKey("brightness")) {
          try {
            brightness = Double.parseDouble(fields.get("brightness"));
            // Clamp to reasonable range but allow extreme values for creative effects
            brightness = Math.max(0.0, Math.min(10.0, brightness));
            } catch (NumberFormatException e) {
            brightness = 1.0; // Default on parse error
          }
        }
        
        // Parse useLightColor flag with default
        boolean useLightColor = false;
        if (fields.containsKey("useLightColor")) {
          String val = fields.get("useLightColor").toLowerCase();
          useLightColor = val.equals("true") || val.equals("1");
        }
        
        // Create material with parsed parameters
        material = new BrightnessMaterial(baseColor, brightness, useLightColor);
      }
      
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildContrastMaterial(ElenaParser parser) {
      final int fsize = fields.size();
      
      Material material = null;
      
      if (fsize == 0x0000) {
        // No parameters: create default material with default values
        material = new ContrastMaterial(Color.GRAY, 1.0, false);
        } else {
        // Parse base color with default
        Color baseColor = Color.GRAY;
        if (fields.containsKey("baseColor")) {
          baseColor = parseColor(fields.get("baseColor"));
        }
        
        // Parse brightness value with validation
        double contrast = 1.0;
        if (fields.containsKey("contrast")) {
          try {
            contrast = Double.parseDouble(fields.get("contrast"));
            contrast = Math.max(0.0, Math.min(10.0, contrast));
            } catch (NumberFormatException e) {
            contrast = 1.0; // Default on parse error
          }
        }
        
        // Parse useLightColor flag with default
        boolean useLightColor = false;
        if (fields.containsKey("useLightColor")) {
          String val = fields.get("useLightColor").toLowerCase();
          useLightColor = val.equals("true") || val.equals("1");
        }
        
        // Create material with parsed parameters
        material = new ContrastMaterial(baseColor, contrast, useLightColor);
      }
      
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildNonScaledTransparentPNGMaterial(ElenaParser parser) {
      Material material = null;
      
      String path = fields.get("imagePath").replace("\"", "");
      BufferedImage imge = parser.loadImage(path);
      
      double billboardWidth = Double.parseDouble(fields.get("billboardWidth"));
      double billboardHeight = Double.parseDouble(fields.get("billboardHeight"));
      
      material = new NonScaledTransparentPNGMaterial(imge, billboardWidth, billboardHeight);
      
      // Shadow alpha threshold
      if (fields.containsKey("shadowAlphaThreshold")) {
        double threshold = Double.parseDouble(fields.get("shadowAlphaThreshold"));
        ((NonScaledTransparentPNGMaterial) material).setShadowAlphaThreshold(threshold);
      }
      
      if (fields.containsKey("gammaCorrection")) {
        float gammaCorrection = Float.parseFloat(fields.get("gammaCorrection"));
        ((NonScaledTransparentPNGMaterial) material).setGammaCorrection(gammaCorrection);
      }
      
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildXRayMaterial(ElenaParser parser) {
      Material material;
      if (fields.size () < 1) {
        material = new XRayMaterial();
        } else {
        Color baseColor = parser.parseColor(fields.get("baseColor"));
        double transparency = Double.parseDouble(fields.get("transparency"));
        double reflectivity = Double.parseDouble(fields.get("reflectivity"));
        material = new XRayMaterial (baseColor, transparency, reflectivity);
      }
      
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildRubyMaterial(ElenaParser parser) {
      Material material;
      if (fields.size () < 1) {
        material = new RubyMaterial();
        } else {
        Color baseColor = parser.parseColor(fields.get("baseColor"));
        double density = Double.parseDouble(fields.get("density"));
        double reflectivity = Double.parseDouble(fields.get("reflectivity"));
        material = new RubyMaterial (baseColor, density, reflectivity);
      }
      
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildObsidianMaterial(ElenaParser parser) {
      Material material;
      if (fields.size () < 1) {
        material = new ObsidianMaterial();
        } else {
        double edgeSharpness = Double.parseDouble(fields.get("edgeSharpness"));
        double reflectivity = Double.parseDouble(fields.get("reflectivity"));
        material = new ObsidianMaterial (edgeSharpness, reflectivity);
      }
      
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildEmeraldMaterial(ElenaParser parser) {
      Material material;
      if (fields.size () < 1) {
        material = new EmeraldMaterial();
        } else {
        Color baseColor = parser.parseColor(fields.get("baseColor"));
        double density = Double.parseDouble(fields.get("density"));
        double reflectivity = Double.parseDouble(fields.get("reflectivity"));
        material = new EmeraldMaterial (baseColor, density, reflectivity);
      }
      
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildAmberMaterial(ElenaParser parser) {
      Material material = new AmberMaterial();
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildHologramDataMaterial(ElenaParser parser) {
      double density = Double.parseDouble(fields.get("dataDensity"));
      int res = Integer.parseInt(fields.get("resolution"));
      Material material = new HologramDataMaterial(density, res);
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildHolographicDiffractionMaterial(ElenaParser parser) {
      Material material;
      if (fields.containsKey("reflectivity")) {
        double refl = Double.parseDouble(fields.get("reflectivity"));
        material = new HolographicDiffractionMaterial(Matrix4.identity(), refl);
        } else {
        material = new HolographicDiffractionMaterial(Matrix4.identity());
      }
      parser.objects.put(id, material);
      return material;
    }
    
    private ImageTexture buildImageTexture(ElenaParser parser) {
      String path = fields.get("imagePath").replace("\"", "");
      BufferedImage img = parser.loadImage(path);
      ImageTexture texture;
      if (fields.containsKey("uScale")) {
        double u = Double.parseDouble(fields.get("uScale"));
        double v = Double.parseDouble(fields.get("vScale"));
        double offU = Double.parseDouble(fields.get("uOffset"));
        double offV = Double.parseDouble(fields.get("vOffset"));
        texture = new ImageTexture(img, u, v, offU, offV);
        } else {
        double scale = Double.parseDouble(fields.get("scale"));
        texture = new ImageTexture(img, scale);
      }
      parser.objects.put(id, texture);
      return texture;
    }
    
    private Material buildImageTextureMaterial(ElenaParser parser) {
      String path = fields.get("imagePath").replace("\"", "");
      BufferedImage img = parser.loadImage(path);
      
      Material material;
      
      if (fields.containsKey("uScale") && fields.containsKey("vScale") &&
        fields.containsKey("uOffset") && fields.containsKey("vOffset")) {
        
        double u = Double.parseDouble(fields.get("uScale"));
        double v = Double.parseDouble(fields.get("vScale"));
        double offU = Double.parseDouble(fields.get("uOffset"));
        double offV = Double.parseDouble(fields.get("vOffset"));
        
        if (fields.containsKey("ambientCoefficient") && fields.containsKey("diffuseCoefficient") &&
          fields.containsKey("specularCoefficient") && fields.containsKey("shininess") &&
          fields.containsKey("reflectivity") && fields.containsKey("ior")) {
          
          double amb = Double.parseDouble(fields.get("ambientCoefficient"));
          double diff = Double.parseDouble(fields.get("diffuseCoefficient"));
          double spec = Double.parseDouble(fields.get("specularCoefficient"));
          double shin = Double.parseDouble(fields.get("shininess"));
          double refl = Double.parseDouble(fields.get("reflectivity"));
          double ior = Double.parseDouble(fields.get("ior"));
          
          material = new ImageTextureMaterial(
            img, u, v, offU, offV,
            amb, diff, spec,
            shin, refl, ior,
            Matrix4.identity()
          );
          } else {
          double amb = fields.containsKey("ambientCoefficient") ? Double.parseDouble(fields.get("ambientCoefficient")) : 0.1;
          double diff = fields.containsKey("diffuseCoefficient") ? Double.parseDouble(fields.get("diffuseCoefficient")) : 0.7;
          double spec = fields.containsKey("specularCoefficient") ? Double.parseDouble(fields.get("specularCoefficient")) : 0.7;
          double shin = fields.containsKey("shininess") ? Double.parseDouble(fields.get("shininess")) : 32.0;
          
          material = new ImageTextureMaterial(
            img, u, v, offU, offV,
            amb, diff, spec,
            shin, 0.0, 1.0,
            Matrix4.identity()
          );
        }
        } else if (fields.containsKey("scale")) {
        double scale = Double.parseDouble(fields.get("scale"));
        material = new ImageTextureMaterial(img, scale, Matrix4.identity());
        } else {
        material = new ImageTextureMaterial(img, Matrix4.identity());
      }
      
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildLambertMaterial(ElenaParser parser) {
      Color color = parser.parseColor(fields.get("color"));
      Material material;
      if (fields.containsKey("ambientCoeff")) {
        double amb = Double.parseDouble(fields.get("ambientCoeff"));
        double diff = Double.parseDouble(fields.get("diffuseCoeff"));
        material = new LambertMaterial(color, amb, diff);
        } else {
        material = new LambertMaterial(color);
      }
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildLavaFlowMaterial(ElenaParser parser) {
      Color hot = parser.parseColor(fields.get("hotColor"));
      Color cool = parser.parseColor(fields.get("coolColor"));
      double speed = Double.parseDouble(fields.get("flowSpeed"));
      Material material = new LavaFlowMaterial(hot, cool, speed, Matrix4.identity());
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildEdgeLightColorMaterial(ElenaParser parser) {
      Material material = new EdgeLightColorMaterial();
      
      if (fields.containsKey("baseColor")){
        ((EdgeLightColorMaterial) material).setBaseColor(parser.parseColor(fields.get("baseColor")));
      }
      
      if (fields.containsKey("edgeColor")){
        ((EdgeLightColorMaterial) material).setEdgeColor(parser.parseColor(fields.get("edgeColor")));
      }
      
      if (fields.containsKey("edgeThreshold")){
        ((EdgeLightColorMaterial) material).setEdgeThreshold(Float.parseFloat(fields.get("edgeThreshold")));
      }
      
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildInvertLightColorMaterial(ElenaParser parser) {
      Material material = new InvertLightColorMaterial();
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildLightningMaterial(ElenaParser parser) {
      Material material;
      if (fields.containsKey("baseColor")) {
        Color color = parser.parseColor(fields.get("baseColor"));
        double intensity = Double.parseDouble(fields.get("intensity"));
        material = new LightningMaterial(color, intensity);
        } else {
        material = new LightningMaterial();
      }
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildMarbleMaterial(ElenaParser parser) {
      Color base = parser.parseColor(fields.get("baseColor"));
      Color vein = parser.parseColor(fields.get("veinColor"));
      double scale = Double.parseDouble(fields.get("scale"));
      double density = Double.parseDouble(fields.get("veinDensity"));
      double turbulence = Double.parseDouble(fields.get("turbulence"));
      Material material;
      if (fields.containsKey("ambientCoefficient")) {
        double amb = Double.parseDouble(fields.get("ambientCoefficient"));
        double diff = Double.parseDouble(fields.get("diffuseCoefficient"));
        double spec = Double.parseDouble(fields.get("specularCoefficient"));
        double shin = Double.parseDouble(fields.get("shininess"));
        double refl = Double.parseDouble(fields.get("reflectivity"));
        double ior = Double.parseDouble(fields.get("ior"));
        double trans = Double.parseDouble(fields.get("transparency"));
        material = new MarbleMaterial(base, vein, scale, density, turbulence, amb, diff, spec, shin, refl, ior, trans, Matrix4.identity());
        } else {
        material = new MarbleMaterial(base, vein, scale, density, turbulence, Matrix4.identity());
      }
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildMetallicMaterial(ElenaParser parser) {
      Color metal = parser.parseColor(fields.get("metallicColor"));
      Color spec = parser.parseColor(fields.get("specularColor"));
      double refl = Double.parseDouble(fields.get("reflectivity"));
      double shin = Double.parseDouble(fields.get("shininess"));
      double amb = Double.parseDouble(fields.get("ambientCoefficient"));
      double diff = Double.parseDouble(fields.get("diffuseCoefficient"));
      double specCoeff = Double.parseDouble(fields.get("specularCoefficient"));
      Material material = new MetallicMaterial(metal, spec, refl, shin, amb, diff, specCoeff);
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildPolkaDotMaterial(ElenaParser parser) {
      Material material;
      
      final int fsize = fields.size ();
      
      if (fsize < 3) {
        Color baseColor = parser.parseColor(fields.get("baseColor"));
        Color dotColor = parser.parseColor(fields.get("dotColor"));
        material = new PolkaDotMaterial (baseColor, dotColor);
        } else {
        Color baseColor = parser.parseColor(fields.get("baseColor"));
        Color dotColor = parser.parseColor(fields.get("dotColor"));
        double dotSize = Double.parseDouble(fields.get("dotSize"));
        double dotSpacing = Double.parseDouble(fields.get("dotSpacing"));
        material = new PolkaDotMaterial (baseColor, dotColor, dotSize, dotSpacing);
      }
      
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildMosaicMaterial(ElenaParser parser) {
      Material material;
      
      final int fsize = fields.size ();
      
      if (fsize < 3) {
        Color baseColor = parser.parseColor(fields.get("baseColor"));
        Color tileColor = parser.parseColor(fields.get("tileColor"));
        material = new MosaicMaterial (baseColor, tileColor);
        } else {
        Color baseColor = parser.parseColor(fields.get("baseColor"));
        Color tileColor = parser.parseColor(fields.get("tileColor"));
        double tileSize = Double.parseDouble(fields.get("tileSize"));
        double groutWidth = Double.parseDouble(fields.get("groutWidth"));
        double randomness = Double.parseDouble(fields.get("randomness"));
        material = new MosaicMaterial (baseColor, tileColor, tileSize, groutWidth, randomness);
      }
      
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildCrystalMaterial(ElenaParser parser) {
      Material material;
      
      final int fsize = fields.size ();
      
      if (fsize < 3) {
        Color baseColor = parser.parseColor(fields.get("baseColor"));
        Color crystalColor = parser.parseColor(fields.get("crystalColor"));
        material = new CrystalMaterial (baseColor, crystalColor);
        } else {
        Color baseColor = parser.parseColor(fields.get("baseColor"));
        Color crystalColor = parser.parseColor(fields.get("crystalColor"));
        double rayDensity = Double.parseDouble(fields.get("rayDensity"));
        double raySharpness = Double.parseDouble(fields.get("raySharpness"));
        material = new CrystalMaterial (baseColor, crystalColor, rayDensity, raySharpness);
      }
      
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildOrbitalMaterial(ElenaParser parser) {
      Material material;
      
      final int fsize = fields.size ();
      
      if (fsize < 3) {
        Color centerColor = parser.parseColor(fields.get("centerColor"));
        Color orbitColor = parser.parseColor(fields.get("orbitColor"));
        material = new OrbitalMaterial (centerColor, orbitColor);
        } else {
        Color centerColor = parser.parseColor(fields.get("centerColor"));
        Color orbitColor = parser.parseColor(fields.get("orbitColor"));
        double ringWidth = Double.parseDouble(fields.get("ringWidth"));
        int ringCount = Integer.parseInt(fields.get("ringCount"));
        material = new OrbitalMaterial (centerColor, orbitColor, ringWidth, ringCount);
      }
      
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildNeutralMaterial(ElenaParser parser) {
      Color base = parser.parseColor(fields.get("baseColor"));
      Material material;
      if (fields.containsKey("reflectivity")) {
        double refl = Double.parseDouble(fields.get("reflectivity"));
        double trans = Double.parseDouble(fields.get("transparency"));
        double ior = Double.parseDouble(fields.get("indexOfRefraction"));
        material = new NeutralMaterial(base, refl, trans, ior);
        } else {
        material = new NeutralMaterial(base);
      }
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildOpticalIllusionMaterial(ElenaParser parser) {
      Color c1 = parser.parseColor(fields.get("color1"));
      Color c2 = parser.parseColor(fields.get("color2"));
      double freq = Double.parseDouble(fields.get("frequency"));
      double smooth = Double.parseDouble(fields.get("smoothness"));
      Material material;
      if (fields.containsKey("ambient")) {
        double amb = Double.parseDouble(fields.get("ambient"));
        double diff = Double.parseDouble(fields.get("diffuse"));
        double spec = Double.parseDouble(fields.get("specular"));
        double shin = Double.parseDouble(fields.get("shininess"));
        double refl = Double.parseDouble(fields.get("reflectivity"));
        double ior = Double.parseDouble(fields.get("ior"));
        double trans = Double.parseDouble(fields.get("transparency"));
        material = new OpticalIllusionMaterial(c1, c2, freq, smooth, amb, diff, spec, shin, refl, ior, trans, Matrix4.identity());
        } else {
        material = new OpticalIllusionMaterial(c1, c2, freq, smooth, Matrix4.identity());
      }
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildCeramicTilePBRMaterial(ElenaParser parser) {
      Material material;
      if (fields.size() < 1) {
        material = new CeramicTilePBRMaterial();
      }
      else {
        Color tile = parser.parseColor(fields.get("tileColor"));
        Color grout = parser.parseColor(fields.get("groutColor"));
        double tileSize = Double.parseDouble(fields.get("tileSize"));
        double groutWidth = Double.parseDouble(fields.get("groutWidth"));
        double tileRough = Double.parseDouble(fields.get("tileRoughness"));
        double groutRough = Double.parseDouble(fields.get("groutRoughness"));
        
        // Yeni parametreler için kontrol ve default değerler
        double tileSpecular = fields.containsKey("tileSpecular") ?
        Double.parseDouble(fields.get("tileSpecular")) :
        CeramicTilePBRMaterial.DEFAULT_TILE_SPECULAR;
        
        double groutSpecular = fields.containsKey("groutSpecular") ?
        Double.parseDouble(fields.get("groutSpecular")) :
        CeramicTilePBRMaterial.DEFAULT_GROUT_SPECULAR;
        
        double fresnelIntensity = fields.containsKey("fresnelIntensity") ?
        Double.parseDouble(fields.get("fresnelIntensity")) :
        CeramicTilePBRMaterial.DEFAULT_FRESNEL_INTENSITY;
        
        double normalMicroFacet = fields.containsKey("normalMicroFacet") ?
        Double.parseDouble(fields.get("normalMicroFacet")) :
        CeramicTilePBRMaterial.DEFAULT_NORMAL_MICRO_FACET;
        
        double reflectionSharpness = fields.containsKey("reflectionSharpness") ?
        Double.parseDouble(fields.get("reflectionSharpness")) :
        CeramicTilePBRMaterial.DEFAULT_REFLECTION_SHARPNESS;
        
        double energyConservation = fields.containsKey("energyConservation") ?
        Double.parseDouble(fields.get("energyConservation")) :
        CeramicTilePBRMaterial.DEFAULT_ENERGY_CONSERVATION;
        
        // Tüm parametrelerle yeni kurucuyu kullan
        material = new CeramicTilePBRMaterial(
          tile, grout, tileSize, groutWidth,
          tileRough, groutRough, tileSpecular, groutSpecular,
          fresnelIntensity, normalMicroFacet, reflectionSharpness, energyConservation
        );
      }
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildGlassicTilePBRMaterial(ElenaParser parser) {
      Material material;
      if (fields.size() < 1) {
        material = new GlassicTilePBRMaterial();
        } else {
        Color tile = parser.parseColor(fields.get("tileColor"));
        Color grout = parser.parseColor(fields.get("groutColor"));
        double tileSize = Double.parseDouble(fields.get("tileSize"));
        double groutWidth = Double.parseDouble(fields.get("groutWidth"));
        double tileRough = Double.parseDouble(fields.get("tileRoughness"));
        double groutRough = Double.parseDouble(fields.get("groutRoughness"));
        material = new GlassicTilePBRMaterial(tile, grout, tileSize, groutWidth, tileRough, groutRough);
      }
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildChromePBRMaterial(ElenaParser parser) {
      Material material;
      if (fields.size() < 1) {
        material = new ChromePBRMaterial();
        } else {
        Color base = parser.parseColor(fields.get("baseReflectance"));
        double rough = Double.parseDouble(fields.get("roughness"));
        double aniso = Double.parseDouble(fields.get("anisotropy"));
        double coat = Double.parseDouble(fields.get("clearCoat"));
        Color edge = parser.parseColor(fields.get("edgeTint"));
        material = new ChromePBRMaterial(base, rough, aniso, coat, edge);
      }
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildSphereWordTextureMaterial(ElenaParser parser) {
      final int fsize = fields.size();
      
      Material material = null;
      
      if (fsize == 0x0000) {
        // No parameters: create default material with default values
        material = new SphereWordTextureMaterial(
          "VANN",
          Color.WHITE,
          null, // gradientColor
          "horizontal", // gradientType
          new Color(0x00000000), // bgColor
          "Arial",
          Font.BOLD,
          100,
          0.3, // reflectivity
          1.0, // ior
          0.0, // opaque transparency
          0, // uOffset
          0, // vOffset
          null, // imageObject
          0, // imageWidth
          0, // imageHeight
          0, // imageUOffset
          0  // imageVOffset
        );
        } else {
        // Parse word with default
        String word = "VANN";
        if (fields.containsKey("word")) {
          word = fields.get("word").replace("\"", "");
        }
        
        // Parse text color with default
        Color textColor = Color.WHITE;
        if (fields.containsKey("textColor")) {
          textColor = parseColor(fields.get("textColor"));
        }
        
        // Parse gradient color (optional)
        Color gradientColor = null;
        if (fields.containsKey("gradientColor")) {
          gradientColor = parseColor(fields.get("gradientColor"));
        }
        
        // Parse gradient type with default
        String gradientType = "horizontal";
        if (fields.containsKey("gradientType")) {
          gradientType = fields.get("gradientType").toLowerCase();
          gradientType = gradientType.replace("\"", "");
          if (!gradientType.equals("horizontal") &&
            !gradientType.equals("vertical") &&
            !gradientType.equals("diagonal")) {
            gradientType = "horizontal";
          }
        }
        
        // Parse background color with default
        Color bgColor = new Color(0x00000000);
        if (fields.containsKey("bgColor")) {
          bgColor = parseColor(fields.get("bgColor"));
        }
        
        // Parse font family with default
        String fontFamily = "Arial";
        if (fields.containsKey("fontFamily")) {
          fontFamily = fields.get("fontFamily").replace("\"", "");
        }
        
        // Parse font style with default
        int fontStyle = Font.BOLD;
        if (fields.containsKey("fontStyle")) {
          String style = fields.get("fontStyle").toLowerCase();
          switch (style) {
            case "plain": fontStyle = Font.PLAIN; break;
            case "italic": fontStyle = Font.ITALIC; break;
            case "bolditalic": fontStyle = Font.BOLD | Font.ITALIC; break;
            default: fontStyle = Font.BOLD;
          }
        }
        
        // Parse font size with default
        int fontSize = 100;
        if (fields.containsKey("fontSize")) {
          try {
            fontSize = Integer.parseInt(fields.get("fontSize"));
            fontSize = Math.max(10, Math.min(500, fontSize)); // Reasonable range
            } catch (NumberFormatException e) {
            fontSize = 100;
          }
        }
        
        // Parse text offsets
        int uOffset = 0;
        if (fields.containsKey("uOffset")) {
          try {
            uOffset = Integer.parseInt(fields.get("uOffset"));
            } catch (NumberFormatException e) {
            uOffset = 0;
          }
        }
        
        int vOffset = 0;
        if (fields.containsKey("vOffset")) {
          try {
            vOffset = Integer.parseInt(fields.get("vOffset"));
            } catch (NumberFormatException e) {
            vOffset = 0;
          }
        }
        
        // Parse image parameters
        BufferedImage imageObject = null;
        int imageWidth = 0;
        int imageHeight = 0;
        int imageUOffset = 0;
        int imageVOffset = 0;
        
        if (fields.containsKey("imageObject")) {
          String imageName = fields.get("imageObject").replace("\"", "");
          if (imageName.equals ("null")) {
            imageObject = null;
            } else {
            // Look up the image in parser's image resources
            imageObject = parser.loadImage(imageName);
          }
        }
        
        if (fields.containsKey("imageWidth")) {
          try {
            imageWidth = Integer.parseInt(fields.get("imageWidth"));
            imageWidth = Math.max(0, imageWidth);
            } catch (NumberFormatException e) {
            imageWidth = 0;
          }
        }
        
        if (fields.containsKey("imageHeight")) {
          try {
            imageHeight = Integer.parseInt(fields.get("imageHeight"));
            imageHeight = Math.max(0, imageHeight);
            } catch (NumberFormatException e) {
            imageHeight = 0;
          }
        }
        
        if (fields.containsKey("imageUOffset")) {
          try {
            imageUOffset = Integer.parseInt(fields.get("imageUOffset"));
            } catch (NumberFormatException e) {
            imageUOffset = 0;
          }
        }
        
        if (fields.containsKey("imageVOffset")) {
          try {
            imageVOffset = Integer.parseInt(fields.get("imageVOffset"));
            } catch (NumberFormatException e) {
            imageVOffset = 0;
          }
        }
        
        // Parse optional material properties
        double reflectivity = 0.3;
        double ior = 1.0;
        double transparency = 0.0;
        
        if (fields.containsKey("reflectivity")) {
          try {
            reflectivity = Double.parseDouble(fields.get("reflectivity"));
            reflectivity = Math.max(0.0, Math.min(1.0, reflectivity));
            } catch (NumberFormatException e) {
            reflectivity = 0.3;
          }
        }
        
        if (fields.containsKey("ior")) {
          try {
            ior = Double.parseDouble(fields.get("ior"));
            ior = Math.max(1.0, ior);
            } catch (NumberFormatException e) {
            ior = 1.0;
          }
        }
        
        if (fields.containsKey("transparency")) {
          try {
            transparency = Double.parseDouble(fields.get("transparency"));
            transparency = Math.max(0.0, Math.min(1.0, transparency));
            } catch (NumberFormatException e) {
            transparency = 0.0;
          }
        }
        
        // Create material with all parsed parameters
        material = new SphereWordTextureMaterial(
          word,
          textColor,
          gradientColor,
          gradientType,
          bgColor,
          fontFamily,
          fontStyle,
          fontSize,
          reflectivity,
          ior,
          transparency,
          uOffset,
          vOffset,
          imageObject,
          imageWidth,
          imageHeight,
          imageUOffset,
          imageVOffset
        );
      }
      
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildTextDielectricMaterial(ElenaParser parser) {
      final int fsize = fields.size();
      
      Material material = null;
      
      if (fsize == 0x0000) {
        // No parameters: create default material with default values
        material = new TextDielectricMaterial(
          "VANN",
          Color.WHITE,
          null, // gradientColor
          "horizontal", // gradientType
          new Color(0x00000000), // bgColor
          "Arial",
          Font.BOLD,
          100,
          0, // uOffset
          0, // vOffset
          null, // imageObject
          0, // imageWidth
          0, // imageHeight
          0, // imageUOffset
          0, // imageVOffset
          new Color(0.9f, 0.9f, 0.9f), // diffuseColor
          1.5, // ior
          0.8, // transparency
          0.1, // reflectivity
          new Color(1.0f, 1.0f, 1.0f), // filterColorInside
          new Color(1.0f, 1.0f, 1.0f)  // filterColorOutside
        );
        } else {
        // Parse word with default
        String word = "VANN";
        if (fields.containsKey("word")) {
          word = fields.get("word").replace("\"", "");
        }
        
        // Parse text color with default
        Color textColor = Color.WHITE;
        if (fields.containsKey("textColor")) {
          textColor = parseColor(fields.get("textColor"));
        }
        
        // Parse gradient color (optional)
        Color gradientColor = null;
        if (fields.containsKey("gradientColor")) {
          gradientColor = parseColor(fields.get("gradientColor"));
        }
        
        // Parse gradient type with default
        String gradientType = "horizontal";
        if (fields.containsKey("gradientType")) {
          gradientType = fields.get("gradientType").toLowerCase();
          gradientType = gradientType.replace("\"", "");
          if (!gradientType.equals("horizontal") &&
            !gradientType.equals("vertical") &&
            !gradientType.equals("diagonal")) {
            gradientType = "horizontal";
          }
        }
        
        // Parse background color with default
        Color bgColor = new Color(0x00000000);
        if (fields.containsKey("bgColor")) {
          bgColor = parseColor(fields.get("bgColor"));
        }
        
        // Parse font family with default
        String fontFamily = "Arial";
        if (fields.containsKey("fontFamily")) {
          fontFamily = fields.get("fontFamily").replace("\"", "");
        }
        
        // Parse font style with default
        int fontStyle = Font.BOLD;
        if (fields.containsKey("fontStyle")) {
          String style = fields.get("fontStyle").toLowerCase();
          switch (style) {
            case "plain": fontStyle = Font.PLAIN; break;
            case "italic": fontStyle = Font.ITALIC; break;
            case "bolditalic": fontStyle = Font.BOLD | Font.ITALIC; break;
            default: fontStyle = Font.BOLD;
          }
        }
        
        // Parse font size with default
        int fontSize = 100;
        if (fields.containsKey("fontSize")) {
          try {
            fontSize = Integer.parseInt(fields.get("fontSize"));
            fontSize = Math.max(10, Math.min(500, fontSize)); // Reasonable range
            } catch (NumberFormatException e) {
            fontSize = 100;
          }
        }
        
        // Parse text offsets
        int uOffset = 0;
        if (fields.containsKey("uOffset")) {
          try {
            uOffset = Integer.parseInt(fields.get("uOffset"));
            } catch (NumberFormatException e) {
            uOffset = 0;
          }
        }
        
        int vOffset = 0;
        if (fields.containsKey("vOffset")) {
          try {
            vOffset = Integer.parseInt(fields.get("vOffset"));
            } catch (NumberFormatException e) {
            vOffset = 0;
          }
        }
        
        // Parse image parameters
        BufferedImage imageObject = null;
        int imageWidth = 0;
        int imageHeight = 0;
        int imageUOffset = 0;
        int imageVOffset = 0;
        
        if (fields.containsKey("imageObject")) {
          String imageName = fields.get("imageObject").replace("\"", "");
          if (imageName.equals("null")) {
            imageObject = null;
            } else {
            // Look up the image in parser's image resources
            imageObject = parser.loadImage(imageName);
          }
        }
        
        if (fields.containsKey("imageWidth")) {
          try {
            imageWidth = Integer.parseInt(fields.get("imageWidth"));
            imageWidth = Math.max(0, imageWidth);
            } catch (NumberFormatException e) {
            imageWidth = 0;
          }
        }
        
        if (fields.containsKey("imageHeight")) {
          try {
            imageHeight = Integer.parseInt(fields.get("imageHeight"));
            imageHeight = Math.max(0, imageHeight);
            } catch (NumberFormatException e) {
            imageHeight = 0;
          }
        }
        
        if (fields.containsKey("imageUOffset")) {
          try {
            imageUOffset = Integer.parseInt(fields.get("imageUOffset"));
            } catch (NumberFormatException e) {
            imageUOffset = 0;
          }
        }
        
        if (fields.containsKey("imageVOffset")) {
          try {
            imageVOffset = Integer.parseInt(fields.get("imageVOffset"));
            } catch (NumberFormatException e) {
            imageVOffset = 0;
          }
        }
        
        // Parse dielectric material properties
        Color diffuseColor = new Color(0.9f, 0.9f, 0.9f);
        if (fields.containsKey("diffuseColor")) {
          diffuseColor = parseColor(fields.get("diffuseColor"));
        }
        
        double ior = 1.5;
        if (fields.containsKey("ior")) {
          try {
            ior = Double.parseDouble(fields.get("ior"));
            ior = Math.max(1.0, ior);
            } catch (NumberFormatException e) {
            ior = 1.5;
          }
        }
        
        double transparency = 0.8;
        if (fields.containsKey("transparency")) {
          try {
            transparency = Double.parseDouble(fields.get("transparency"));
            transparency = Math.max(0.0, Math.min(1.0, transparency));
            } catch (NumberFormatException e) {
            transparency = 0.8;
          }
        }
        
        double reflectivity = 0.1;
        if (fields.containsKey("reflectivity")) {
          try {
            reflectivity = Double.parseDouble(fields.get("reflectivity"));
            reflectivity = Math.max(0.0, Math.min(1.0, reflectivity));
            } catch (NumberFormatException e) {
            reflectivity = 0.1;
          }
        }
        
        Color filterColorInside = new Color(1.0f, 1.0f, 1.0f);
        if (fields.containsKey("filterColorInside")) {
          filterColorInside = parseColor(fields.get("filterColorInside"));
        }
        
        Color filterColorOutside = new Color(1.0f, 1.0f, 1.0f);
        if (fields.containsKey("filterColorOutside")) {
          filterColorOutside = parseColor(fields.get("filterColorOutside"));
        }
        
        // Create material with all parsed parameters
        material = new TextDielectricMaterial(
          word,
          textColor,
          gradientColor,
          gradientType,
          bgColor,
          fontFamily,
          fontStyle,
          fontSize,
          uOffset,
          vOffset,
          imageObject,
          imageWidth,
          imageHeight,
          imageUOffset,
          imageVOffset,
          diffuseColor,
          ior,
          transparency,
          reflectivity,
          filterColorInside,
          filterColorOutside
        );
        
        ((TextDielectricMaterial)(material)).setFilterColorInside (filterColorInside);
        ((TextDielectricMaterial)(material)).setFilterColorOutside (filterColorOutside);
      }
      
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildCopperPBRMaterial(ElenaParser parser) {
      Material material;
      if (fields.size() < 1) {
        material = new CopperPBRMaterial();
        } else if (fields.size() == 3) {
        double rough = Double.parseDouble(fields.get("roughness"));
        double ox = Double.parseDouble(fields.get("oxidation"));
        material = new CopperPBRMaterial(rough, ox);
        } else {
        Color base = parser.parseColor(fields.get("baseColor"));
        double rough = Double.parseDouble(fields.get("roughness"));
        double ox = Double.parseDouble(fields.get("oxidation"));
        material = new CopperPBRMaterial(base, rough, ox);
      }
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildGhostTextMaterial(ElenaParser parser) {
      final int fsize = fields.size();
      
      Material material = null;
      
      if (fsize == 0x0000) {
        // No parameters: create default material with default values
        material = new GhostTextMaterial(
          "VANN",
          Color.WHITE,
          null, // gradientColor
          "horizontal", // gradientType
          "Arial",
          Font.BOLD,
          100,
          0, // uOffset
          0, // vOffset
          null, // imageObject
          0, // imageWidth
          0, // imageHeight
          0, // imageUOffset
          0, // imageVOffset
          0.95, // transparency
          0.4, // reflectivity
          1.52  // ior
        );
        } else {
        // Parse word with default
        String word = "VANN";
        if (fields.containsKey("word")) {
          word = fields.get("word").replace("\"", "");
        }
        
        // Parse text color with default
        Color textColor = Color.WHITE;
        if (fields.containsKey("textColor")) {
          textColor = parseColor(fields.get("textColor"));
        }
        
        // Parse gradient color (optional)
        Color gradientColor = null;
        if (fields.containsKey("gradientColor")) {
          gradientColor = parseColor(fields.get("gradientColor"));
        }
        
        // Parse gradient type with default
        String gradientType = "horizontal";
        if (fields.containsKey("gradientType")) {
          gradientType = fields.get("gradientType").toLowerCase();
          gradientType = gradientType.replace("\"", "");
          if (!gradientType.equals("horizontal") &&
            !gradientType.equals("vertical") &&
            !gradientType.equals("diagonal")) {
            gradientType = "horizontal";
          }
        }
        
        // Parse font family with default
        String fontFamily = "Arial";
        if (fields.containsKey("fontFamily")) {
          fontFamily = fields.get("fontFamily").replace("\"", "");
        }
        
        // Parse font style with default
        int fontStyle = Font.BOLD;
        if (fields.containsKey("fontStyle")) {
          String style = fields.get("fontStyle").toLowerCase();
          switch (style) {
            case "plain": fontStyle = Font.PLAIN; break;
            case "italic": fontStyle = Font.ITALIC; break;
            case "bolditalic": fontStyle = Font.BOLD | Font.ITALIC; break;
            default: fontStyle = Font.BOLD;
          }
        }
        
        // Parse font size with default
        int fontSize = 100;
        if (fields.containsKey("fontSize")) {
          try {
            fontSize = Integer.parseInt(fields.get("fontSize"));
            fontSize = Math.max(10, Math.min(500, fontSize)); // Reasonable range
            } catch (NumberFormatException e) {
            fontSize = 100;
          }
        }
        
        // Parse text offsets
        int uOffset = 0;
        if (fields.containsKey("uOffset")) {
          try {
            uOffset = Integer.parseInt(fields.get("uOffset"));
            } catch (NumberFormatException e) {
            uOffset = 0;
          }
        }
        
        int vOffset = 0;
        if (fields.containsKey("vOffset")) {
          try {
            vOffset = Integer.parseInt(fields.get("vOffset"));
            } catch (NumberFormatException e) {
            vOffset = 0;
          }
        }
        
        // Parse image parameters
        BufferedImage imageObject = null;
        int imageWidth = 0;
        int imageHeight = 0;
        int imageUOffset = 0;
        int imageVOffset = 0;
        
        if (fields.containsKey("imageObject")) {
          String imageName = fields.get("imageObject").replace("\"", "");
          if (imageName.equals("null")) {
            imageObject = null;
            } else {
            // Look up the image in parser's image resources
            imageObject = parser.loadImage(imageName);
          }
        }
        
        if (fields.containsKey("imageWidth")) {
          try {
            imageWidth = Integer.parseInt(fields.get("imageWidth"));
            imageWidth = Math.max(0, imageWidth);
            } catch (NumberFormatException e) {
            imageWidth = 0;
          }
        }
        
        if (fields.containsKey("imageHeight")) {
          try {
            imageHeight = Integer.parseInt(fields.get("imageHeight"));
            imageHeight = Math.max(0, imageHeight);
            } catch (NumberFormatException e) {
            imageHeight = 0;
          }
        }
        
        if (fields.containsKey("imageUOffset")) {
          try {
            imageUOffset = Integer.parseInt(fields.get("imageUOffset"));
            } catch (NumberFormatException e) {
            imageUOffset = 0;
          }
        }
        
        if (fields.containsKey("imageVOffset")) {
          try {
            imageVOffset = Integer.parseInt(fields.get("imageVOffset"));
            } catch (NumberFormatException e) {
            imageVOffset = 0;
          }
        }
        
        // Parse ghost material properties
        double transparency = 0.95;
        if (fields.containsKey("transparency")) {
          try {
            transparency = Double.parseDouble(fields.get("transparency"));
            transparency = Math.max(0.0, Math.min(1.0, transparency));
            } catch (NumberFormatException e) {
            transparency = 0.95;
          }
        }
        
        double reflectivity = 0.4;
        if (fields.containsKey("reflectivity")) {
          try {
            reflectivity = Double.parseDouble(fields.get("reflectivity"));
            reflectivity = Math.max(0.0, Math.min(1.0, reflectivity));
            } catch (NumberFormatException e) {
            reflectivity = 0.4;
          }
        }
        
        double ior = 1.52;
        if (fields.containsKey("ior")) {
          try {
            ior = Double.parseDouble(fields.get("ior"));
            ior = Math.max(1.0, ior);
            } catch (NumberFormatException e) {
            ior = 1.52;
          }
        }
        
        // Create material with all parsed parameters
        material = new GhostTextMaterial(
          word,
          textColor,
          gradientColor,
          gradientType,
          fontFamily,
          fontStyle,
          fontSize,
          uOffset,
          vOffset,
          imageObject,
          imageWidth,
          imageHeight,
          imageUOffset,
          imageVOffset,
          transparency,
          reflectivity,
          ior
        );
      }
      
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildMultiMixMaterial(ElenaParser parser) {
      Material material;
      
      // Parse materials
      if (!fields.containsKey("materials")) {
        throw new RuntimeException("Missing 'materials' field for MultiMixMaterial");
      }
      
      String materialsField = fields.get("materials");
      String[] materialIds;
      
      // Handle both comma-separated and array-like formats: [mat1, mat2, mat3]
      if (materialsField.startsWith("[") && materialsField.endsWith("]")) {
        materialsField = materialsField.substring(1, materialsField.length() - 1);
      }
      materialIds = materialsField.split(",");
      //materialIds = materialsField.split("\\s*,\\s*");
      
      Material[] materiales = new Material[materialIds.length];
      for (int i = 0; i < materialIds.length; i++) {
        String materialId = materialIds[i].trim();
        //System.out.println ("MIXED MATERIAL ID: "+materialId);
        
        if (parser.objects.containsKey(materialId)) {
          materiales[i] = (Material) parser.objects.get(materialId);
          } else {
          throw new RuntimeException("Material not found: " + materialId);
        }
      }
      
      // Parse ratios
      if (!fields.containsKey("ratios")) {
        throw new RuntimeException("Missing 'ratios' field for MultiMixMaterial");
      }
      
      String ratiosField = fields.get("ratios");
      String[] ratioStrings;
      
      // Handle both comma-separated and array-like formats: [0.3, 0.5, 0.2]
      if (ratiosField.startsWith("[") && ratiosField.endsWith("]")) {
        ratiosField = ratiosField.substring(1, ratiosField.length() - 1);
      }
      ratioStrings = ratiosField.split(",");
      
      double[] doubles = new double[ratioStrings.length];
      for (int i = 0; i < ratioStrings.length; i++) {
        doubles[i] = Double.parseDouble(ratioStrings[i].trim());
      }
      
      // Validation
      if (materiales.length != doubles.length) {
        throw new RuntimeException("Materials count (" + materiales.length +
        ") must match ratios count (" + doubles.length + ")");
      }
      
      material = new MultiMixMaterial(materiales, doubles);
      
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildGlassMaterial(ElenaParser parser) {
      Material material;
      if (fields.size() < 1) {
        material = new GlassMaterial();
        } else {
        Color baseColor = parser.parseColor(fields.get("baseColor"));
        double ior = Double.parseDouble(fields.get("ior"));
        double reflectivity = Double.parseDouble(fields.get("reflectivity"));
        double transparency = Double.parseDouble(fields.get("transparency"));
        material = new GlassMaterial(baseColor, ior, reflectivity, transparency);
      }
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildAnodizedTextMaterial(ElenaParser parser) {
      final int fsize = fields.size();
      
      Material material = null;
      
      if (fsize == 0x0000) {
        // No parameters: create default material with default values
        material = new AnodizedTextMaterial(
          "METAL",
          Color.WHITE,
          null, // gradientColor
          "horizontal", // gradientType
          new Color(0x00000000), // bgColor
          "Arial",
          Font.BOLD,
          100,
          0, // uOffset
          0, // vOffset
          null, // imageObject
          0, // imageWidth
          0, // imageHeight
          0, // imageUOffset
          0, // imageVOffset
          new Color(50, 50, 200) // baseColor (default anodized blue)
        );
        } else {
        // Parse word with default
        String word = "METAL";
        if (fields.containsKey("word")) {
          word = fields.get("word").replace("\"", "");
        }
        
        // Parse text color with default
        Color textColor = Color.WHITE;
        if (fields.containsKey("textColor")) {
          textColor = parseColor(fields.get("textColor"));
        }
        
        // Parse gradient color (optional)
        Color gradientColor = null;
        if (fields.containsKey("gradientColor")) {
          gradientColor = parseColor(fields.get("gradientColor"));
        }
        
        // Parse gradient type with default
        String gradientType = "horizontal";
        if (fields.containsKey("gradientType")) {
          gradientType = fields.get("gradientType").toLowerCase();
          gradientType = gradientType.replace("\"", "");
          if (!gradientType.equals("horizontal") &&
            !gradientType.equals("vertical") &&
            !gradientType.equals("diagonal")) {
            gradientType = "horizontal";
          }
        }
        
        // Parse background color with default
        Color bgColor = new Color(0x00000000);
        if (fields.containsKey("bgColor")) {
          bgColor = parseColor(fields.get("bgColor"));
        }
        
        // Parse font family with default
        String fontFamily = "Arial";
        if (fields.containsKey("fontFamily")) {
          fontFamily = fields.get("fontFamily").replace("\"", "");
        }
        
        // Parse font style with default
        int fontStyle = Font.BOLD;
        if (fields.containsKey("fontStyle")) {
          String style = fields.get("fontStyle").toLowerCase();
          switch (style) {
            case "plain": fontStyle = Font.PLAIN; break;
            case "italic": fontStyle = Font.ITALIC; break;
            case "bolditalic": fontStyle = Font.BOLD | Font.ITALIC; break;
            default: fontStyle = Font.BOLD;
          }
        }
        
        // Parse font size with default
        int fontSize = 100;
        if (fields.containsKey("fontSize")) {
          try {
            fontSize = Integer.parseInt(fields.get("fontSize"));
            fontSize = Math.max(10, Math.min(500, fontSize)); // Reasonable range
            } catch (NumberFormatException e) {
            fontSize = 100;
          }
        }
        
        // Parse text offsets
        int uOffset = 0;
        if (fields.containsKey("uOffset")) {
          try {
            uOffset = Integer.parseInt(fields.get("uOffset"));
            } catch (NumberFormatException e) {
            uOffset = 0;
          }
        }
        
        int vOffset = 0;
        if (fields.containsKey("vOffset")) {
          try {
            vOffset = Integer.parseInt(fields.get("vOffset"));
            } catch (NumberFormatException e) {
            vOffset = 0;
          }
        }
        
        // Parse image parameters
        BufferedImage imageObject = null;
        int imageWidth = 0;
        int imageHeight = 0;
        int imageUOffset = 0;
        int imageVOffset = 0;
        
        if (fields.containsKey("imageObject")) {
          String imageName = fields.get("imageObject").replace("\"", "");
          if (imageName.equals("null")) {
            imageObject = null;
            } else {
            // Look up the image in parser's image resources
            imageObject = parser.loadImage(imageName);
          }
        }
        
        if (fields.containsKey("imageWidth")) {
          try {
            imageWidth = Integer.parseInt(fields.get("imageWidth"));
            imageWidth = Math.max(0, imageWidth);
            } catch (NumberFormatException e) {
            imageWidth = 0;
          }
        }
        
        if (fields.containsKey("imageHeight")) {
          try {
            imageHeight = Integer.parseInt(fields.get("imageHeight"));
            imageHeight = Math.max(0, imageHeight);
            } catch (NumberFormatException e) {
            imageHeight = 0;
          }
        }
        
        if (fields.containsKey("imageUOffset")) {
          try {
            imageUOffset = Integer.parseInt(fields.get("imageUOffset"));
            } catch (NumberFormatException e) {
            imageUOffset = 0;
          }
        }
        
        if (fields.containsKey("imageVOffset")) {
          try {
            imageVOffset = Integer.parseInt(fields.get("imageVOffset"));
            } catch (NumberFormatException e) {
            imageVOffset = 0;
          }
        }
        
        // Parse base color for anodized effect — use parseColor directly
        Color baseColor = new Color(50, 50, 200); // Default: anodized blue
        if (fields.containsKey("baseColor")) {
          baseColor = parseColor(fields.get("baseColor"));
        }
        
        // Create material with all parsed parameters
        material = new AnodizedTextMaterial(
          word,
          textColor,
          gradientColor,
          gradientType,
          bgColor,
          fontFamily,
          fontStyle,
          fontSize,
          uOffset,
          vOffset,
          imageObject,
          imageWidth,
          imageHeight,
          imageUOffset,
          imageVOffset,
          baseColor
        );
      }
      
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildDiamondMaterial(ElenaParser parser) {
      Material material;
      if (fields.size() < 1) {
        material = new DiamondMaterial();
        } else {
        Color baseColor = parser.parseColor(fields.get("baseColor"));
        double ior = Double.parseDouble(fields.get("ior"));
        double reflectivity = Double.parseDouble(fields.get("reflectivity"));
        double transparency = Double.parseDouble(fields.get("transparency"));
        double dispersionStrength = Double.parseDouble(fields.get("dispersionStrength"));
        double fireEffect = Double.parseDouble(fields.get("fireEffect"));
        material = new DiamondMaterial(baseColor, ior, reflectivity, transparency, dispersionStrength, fireEffect);
      }
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildPhongTextMaterial(ElenaParser parser) {
      final int fsize = fields.size();
      
      Material material = null;
      
      if (fsize == 0x0000) {
        // No parameters: create default material with default values
        material = new PhongTextMaterial(
          "PHONG",
          Color.WHITE,
          null, // gradientColor
          "horizontal", // gradientType
          new Color(0x00000000), // bgColor
          "Arial",
          Font.BOLD,
          100,
          0, // uOffset
          0, // vOffset
          null, // imageObject
          0, // imageWidth
          0, // imageHeight
          0, // imageUOffset
          0, // imageVOffset
          new Color(0.9f, 0.9f, 0.9f), // diffuseColor
          Color.WHITE, // specularColor
          32.0, // shininess
          0.1, // ambientCoefficient
          0.7, // diffuseCoefficient
          0.7, // specularCoefficient
          0.0, // reflectivity
          1.0, // ior
          0.0  // transparency
        );
        } else {
        // Parse word with default
        String word = "PHONG";
        if (fields.containsKey("word")) {
          word = fields.get("word").replace("\"", "");
        }
        
        // Parse text color with default
        Color textColor = Color.WHITE;
        if (fields.containsKey("textColor")) {
          textColor = parseColor(fields.get("textColor"));
        }
        
        // Parse gradient color (optional)
        Color gradientColor = null;
        if (fields.containsKey("gradientColor")) {
          gradientColor = parseColor(fields.get("gradientColor"));
        }
        
        // Parse gradient type with default
        String gradientType = "horizontal";
        if (fields.containsKey("gradientType")) {
          gradientType = fields.get("gradientType").toLowerCase();
          gradientType = gradientType.replace("\"", "");
          if (!gradientType.equals("horizontal") &&
            !gradientType.equals("vertical") &&
            !gradientType.equals("diagonal")) {
            gradientType = "horizontal";
          }
        }
        
        // Parse background color with default
        Color bgColor = new Color(0x00000000);
        if (fields.containsKey("bgColor")) {
          bgColor = parseColor(fields.get("bgColor"));
        }
        
        // Parse font family with default
        String fontFamily = "Arial";
        if (fields.containsKey("fontFamily")) {
          fontFamily = fields.get("fontFamily").replace("\"", "");
        }
        
        // Parse font style with default
        int fontStyle = Font.BOLD;
        if (fields.containsKey("fontStyle")) {
          String style = fields.get("fontStyle").toLowerCase();
          switch (style) {
            case "plain": fontStyle = Font.PLAIN; break;
            case "italic": fontStyle = Font.ITALIC; break;
            case "bolditalic": fontStyle = Font.BOLD | Font.ITALIC; break;
            default: fontStyle = Font.BOLD;
          }
        }
        
        // Parse font size with default
        int fontSize = 100;
        if (fields.containsKey("fontSize")) {
          try {
            fontSize = Integer.parseInt(fields.get("fontSize"));
            fontSize = Math.max(10, Math.min(500, fontSize));
            } catch (NumberFormatException e) {
            fontSize = 100;
          }
        }
        
        // Parse text offsets
        int uOffset = 0;
        if (fields.containsKey("uOffset")) {
          try {
            uOffset = Integer.parseInt(fields.get("uOffset"));
            } catch (NumberFormatException e) {
            uOffset = 0;
          }
        }
        
        int vOffset = 0;
        if (fields.containsKey("vOffset")) {
          try {
            vOffset = Integer.parseInt(fields.get("vOffset"));
            } catch (NumberFormatException e) {
            vOffset = 0;
          }
        }
        
        // Parse image parameters
        BufferedImage imageObject = null;
        int imageWidth = 0;
        int imageHeight = 0;
        int imageUOffset = 0;
        int imageVOffset = 0;
        
        if (fields.containsKey("imageObject")) {
          String imageName = fields.get("imageObject").replace("\"", "");
          if (imageName.equals("null")) {
            imageObject = null;
            } else {
            imageObject = parser.loadImage(imageName);
          }
        }
        
        if (fields.containsKey("imageWidth")) {
          try {
            imageWidth = Integer.parseInt(fields.get("imageWidth"));
            imageWidth = Math.max(0, imageWidth);
            } catch (NumberFormatException e) {
            imageWidth = 0;
          }
        }
        
        if (fields.containsKey("imageHeight")) {
          try {
            imageHeight = Integer.parseInt(fields.get("imageHeight"));
            imageHeight = Math.max(0, imageHeight);
            } catch (NumberFormatException e) {
            imageHeight = 0;
          }
        }
        
        if (fields.containsKey("imageUOffset")) {
          try {
            imageUOffset = Integer.parseInt(fields.get("imageUOffset"));
            } catch (NumberFormatException e) {
            imageUOffset = 0;
          }
        }
        
        if (fields.containsKey("imageVOffset")) {
          try {
            imageVOffset = Integer.parseInt(fields.get("imageVOffset"));
            } catch (NumberFormatException e) {
            imageVOffset = 0;
          }
        }
        
        // Parse Phong material properties
        Color diffuseColor = new Color(0.9f, 0.9f, 0.9f);
        if (fields.containsKey("diffuseColor")) {
          diffuseColor = parseColor(fields.get("diffuseColor"));
        }
        
        Color specularColor = Color.WHITE;
        if (fields.containsKey("specularColor")) {
          specularColor = parseColor(fields.get("specularColor"));
        }
        
        double shininess = 32.0;
        if (fields.containsKey("shininess")) {
          try {
            shininess = Double.parseDouble(fields.get("shininess"));
            shininess = Math.max(1.0, shininess);
            } catch (NumberFormatException e) {
            shininess = 32.0;
          }
        }
        
        double ambientCoefficient = 0.1;
        if (fields.containsKey("ambientCoefficient")) {
          try {
            ambientCoefficient = Double.parseDouble(fields.get("ambientCoefficient"));
            ambientCoefficient = Math.max(0.0, Math.min(1.0, ambientCoefficient));
            } catch (NumberFormatException e) {
            ambientCoefficient = 0.1;
          }
        }
        
        double diffuseCoefficient = 0.7;
        if (fields.containsKey("diffuseCoefficient")) {
          try {
            diffuseCoefficient = Double.parseDouble(fields.get("diffuseCoefficient"));
            diffuseCoefficient = Math.max(0.0, Math.min(1.0, diffuseCoefficient));
            } catch (NumberFormatException e) {
            diffuseCoefficient = 0.7;
          }
        }
        
        double specularCoefficient = 0.7;
        if (fields.containsKey("specularCoefficient")) {
          try {
            specularCoefficient = Double.parseDouble(fields.get("specularCoefficient"));
            specularCoefficient = Math.max(0.0, Math.min(1.0, specularCoefficient));
            } catch (NumberFormatException e) {
            specularCoefficient = 0.7;
          }
        }
        
        double reflectivity = 0.0;
        if (fields.containsKey("reflectivity")) {
          try {
            reflectivity = Double.parseDouble(fields.get("reflectivity"));
            reflectivity = Math.max(0.0, Math.min(1.0, reflectivity));
            } catch (NumberFormatException e) {
            reflectivity = 0.0;
          }
        }
        
        double ior = 1.0;
        if (fields.containsKey("ior")) {
          try {
            ior = Double.parseDouble(fields.get("ior"));
            ior = Math.max(1.0, ior);
            } catch (NumberFormatException e) {
            ior = 1.0;
          }
        }
        
        double transparency = 0.0;
        if (fields.containsKey("transparency")) {
          try {
            transparency = Double.parseDouble(fields.get("transparency"));
            transparency = Math.max(0.0, Math.min(1.0, transparency));
            } catch (NumberFormatException e) {
            transparency = 0.0;
          }
        }
        
        // Create material with all parsed parameters
        material = new PhongTextMaterial(
          word,
          textColor,
          gradientColor,
          gradientType,
          bgColor,
          fontFamily,
          fontStyle,
          fontSize,
          uOffset,
          vOffset,
          imageObject,
          imageWidth,
          imageHeight,
          imageUOffset,
          imageVOffset,
          diffuseColor,
          specularColor,
          shininess,
          ambientCoefficient,
          diffuseCoefficient,
          specularCoefficient,
          reflectivity,
          ior,
          transparency
        );
      }
      
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildGoldPBRMaterial(ElenaParser parser) {
      Material material;
      if (fields.size () < 1) {
        material = new GoldPBRMaterial (0.1);
        } else if (fields.containsKey("roughness") && !fields.containsKey("albedo")) {
        double rough = Double.parseDouble(fields.get("roughness"));
        material = new GoldPBRMaterial(rough);
        } else {
        Color albedo = parser.parseColor(fields.get("albedo"));
        double rough = Double.parseDouble(fields.get("roughness"));
        double metal = Double.parseDouble(fields.get("metalness"));
        material = new GoldPBRMaterial(albedo, rough, metal);
      }
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildTexturedCheckerboardMaterial(ElenaParser parser) {
      final int fsize = fields.size();
      
      Material material = null;
      
      if (fsize == 0x0000) {
        // No parameters: create default material with default values
        material = new TexturedCheckerboardMaterial(
          Color.DARK_GRAY,           // color1
          Color.LIGHT_GRAY,          // color2
          1.0,                       // size
          "CHECKER",                 // text
          Color.WHITE,               // textColor
          null,                      // gradientColor
          "horizontal",              // gradientType
          new Color(0x00000000),     // bgColor (transparent)
          "Arial",                   // fontFamily
          Font.BOLD,                 // fontStyle
          72,                        // fontSize
          0,                         // textUOffset
          0,                         // textVOffset
          null,                      // imageObject
          0,                         // imageWidth
          0,                         // imageHeight
          0,                         // imageUOffset
          0,                         // imageVOffset
          0.1,                       // ambientCoefficient
          0.7,                       // diffuseCoefficient
          0.8,                       // specularCoefficient
          50.0,                      // shininess
          Color.WHITE,               // specularColor
          0.0,                       // reflectivity
          1.0,                       // ior
          0.0,                       // transparency
          new Matrix4()              // objectInverseTransform — NOT from scene file
        );
        } else {
        // Parse checkerboard color1 with default
        Color color1 = Color.DARK_GRAY;
        if (fields.containsKey("color1")) {
          color1 = parseColor(fields.get("color1"));
        }
        
        // Parse checkerboard color2 with default
        Color color2 = Color.LIGHT_GRAY;
        if (fields.containsKey("color2")) {
          color2 = parseColor(fields.get("color2"));
        }
        
        // Parse checkerboard size with default
        double size = 1.0;
        if (fields.containsKey("size")) {
          try {
            size = Double.parseDouble(fields.get("size"));
            size = Math.max(0.1, size); // prevent too small
            } catch (NumberFormatException e) {
            size = 1.0;
          }
        }
        
        // Parse overlay text with default
        String text = "CHECKER";
        if (fields.containsKey("text")) {
          text = fields.get("text").replace("\"", "");
        }
        
        // Parse text color with default
        Color textColor = Color.WHITE;
        if (fields.containsKey("textColor")) {
          textColor = parseColor(fields.get("textColor"));
        }
        
        // Parse gradient color (optional, default null)
        Color gradientColor = null;
        if (fields.containsKey("gradientColor")) {
          gradientColor = parseColor(fields.get("gradientColor"));
        }
        
        // Parse gradient type with default
        String gradientType = "horizontal";
        if (fields.containsKey("gradientType")) {
          gradientType = fields.get("gradientType").toLowerCase().replace("\"", "");
          if (!gradientType.equals("horizontal") &&
            !gradientType.equals("vertical") &&
            !gradientType.equals("diagonal")) {
            gradientType = "horizontal";
          }
        }
        
        // Parse background color with default (transparent)
        Color bgColor = new Color(0x00000000);
        if (fields.containsKey("bgColor")) {
          bgColor = parseColor(fields.get("bgColor"));
        }
        
        // Parse font family with default
        String fontFamily = "Arial";
        if (fields.containsKey("fontFamily")) {
          fontFamily = fields.get("fontFamily").replace("\"", "");
        }
        
        // Parse font style with default
        int fontStyle = Font.BOLD;
        if (fields.containsKey("fontStyle")) {
          String style = fields.get("fontStyle").toLowerCase();
          switch (style) {
            case "plain": fontStyle = Font.PLAIN; break;
            case "italic": fontStyle = Font.ITALIC; break;
            case "bolditalic": fontStyle = Font.BOLD | Font.ITALIC; break;
            default: fontStyle = Font.BOLD;
          }
        }
        
        // Parse font size with default
        int fontSize = 72;
        if (fields.containsKey("fontSize")) {
          try {
            fontSize = Integer.parseInt(fields.get("fontSize"));
            fontSize = Math.max(10, Math.min(500, fontSize));
            } catch (NumberFormatException e) {
            fontSize = 72;
          }
        }
        
        // Parse text offsets
        int textUOffset = 0;
        if (fields.containsKey("textUOffset")) {
          try {
            textUOffset = Integer.parseInt(fields.get("textUOffset"));
            } catch (NumberFormatException e) {
            textUOffset = 0;
          }
        }
        
        int textVOffset = 0;
        if (fields.containsKey("textVOffset")) {
          try {
            textVOffset = Integer.parseInt(fields.get("textVOffset"));
            } catch (NumberFormatException e) {
            textVOffset = 0;
          }
        }
        
        // Parse image parameters
        BufferedImage imageObject = null;
        int imageWidth = 0;
        int imageHeight = 0;
        int imageUOffset = 0;
        int imageVOffset = 0;
        
        if (fields.containsKey("imageObject")) {
          String imageName = fields.get("imageObject").replace("\"", "");
          if (!"null".equalsIgnoreCase(imageName)) {
            imageObject = parser.loadImage(imageName);
          }
        }
        
        if (fields.containsKey("imageWidth")) {
          try {
            imageWidth = Integer.parseInt(fields.get("imageWidth"));
            imageWidth = Math.max(0, imageWidth);
            } catch (NumberFormatException e) {
            imageWidth = 0;
          }
        }
        
        if (fields.containsKey("imageHeight")) {
          try {
            imageHeight = Integer.parseInt(fields.get("imageHeight"));
            imageHeight = Math.max(0, imageHeight);
            } catch (NumberFormatException e) {
            imageHeight = 0;
          }
        }
        
        if (fields.containsKey("imageUOffset")) {
          try {
            imageUOffset = Integer.parseInt(fields.get("imageUOffset"));
            } catch (NumberFormatException e) {
            imageUOffset = 0;
          }
        }
        
        if (fields.containsKey("imageVOffset")) {
          try {
            imageVOffset = Integer.parseInt(fields.get("imageVOffset"));
            } catch (NumberFormatException e) {
            imageVOffset = 0;
          }
        }
        
        // Parse Phong lighting properties
        double ambientCoefficient = 0.1;
        if (fields.containsKey("ambientCoefficient")) {
          try {
            ambientCoefficient = Double.parseDouble(fields.get("ambientCoefficient"));
            ambientCoefficient = Math.max(0.0, Math.min(1.0, ambientCoefficient));
            } catch (NumberFormatException e) {
            ambientCoefficient = 0.1;
          }
        }
        
        double diffuseCoefficient = 0.7;
        if (fields.containsKey("diffuseCoefficient")) {
          try {
            diffuseCoefficient = Double.parseDouble(fields.get("diffuseCoefficient"));
            diffuseCoefficient = Math.max(0.0, Math.min(1.0, diffuseCoefficient));
            } catch (NumberFormatException e) {
            diffuseCoefficient = 0.7;
          }
        }
        
        double specularCoefficient = 0.8;
        if (fields.containsKey("specularCoefficient")) {
          try {
            specularCoefficient = Double.parseDouble(fields.get("specularCoefficient"));
            specularCoefficient = Math.max(0.0, Math.min(1.0, specularCoefficient));
            } catch (NumberFormatException e) {
            specularCoefficient = 0.8;
          }
        }
        
        double shininess = 50.0;
        if (fields.containsKey("shininess")) {
          try {
            shininess = Double.parseDouble(fields.get("shininess"));
            shininess = Math.max(1.0, shininess);
            } catch (NumberFormatException e) {
            shininess = 50.0;
          }
        }
        
        Color specularColor = Color.WHITE;
        if (fields.containsKey("specularColor")) {
          specularColor = parseColor(fields.get("specularColor"));
        }
        
        // Parse material physical properties
        double reflectivity = 0.0;
        if (fields.containsKey("reflectivity")) {
          try {
            reflectivity = Double.parseDouble(fields.get("reflectivity"));
            reflectivity = Math.max(0.0, Math.min(1.0, reflectivity));
            } catch (NumberFormatException e) {
            reflectivity = 0.0;
          }
        }
        
        double ior = 1.0;
        if (fields.containsKey("ior")) {
          try {
            ior = Double.parseDouble(fields.get("ior"));
            ior = Math.max(1.0, ior);
            } catch (NumberFormatException e) {
            ior = 1.0;
          }
        }
        
        double transparency = 0.0;
        if (fields.containsKey("transparency")) {
          try {
            transparency = Double.parseDouble(fields.get("transparency"));
            transparency = Math.max(0.0, Math.min(1.0, transparency));
            } catch (NumberFormatException e) {
            transparency = 0.0;
          }
        }
        
        // Create material with all parsed parameters
        // Note: objectInverseTransform is NOT parsed from scene file — always default
        material = new TexturedCheckerboardMaterial(
          color1,
          color2,
          size,
          text,
          textColor,
          gradientColor,
          gradientType,
          bgColor,
          fontFamily,
          fontStyle,
          fontSize,
          textUOffset,
          textVOffset,
          imageObject,
          imageWidth,
          imageHeight,
          imageUOffset,
          imageVOffset,
          ambientCoefficient,
          diffuseCoefficient,
          specularCoefficient,
          shininess,
          specularColor,
          reflectivity,
          ior,
          transparency,
          new Matrix4()
        );
      }
      
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildHolographicPBRMaterial(ElenaParser parser) {
      Material material;
      if (fields.size() < 1) {
        material = new HolographicPBRMaterial();
        } else {
        Color base = parser.parseColor(fields.get("baseColor"));
        double rainbow = Double.parseDouble(fields.get("rainbowSpeed"));
        double scan = Double.parseDouble(fields.get("scanLineDensity"));
        double glitch = Double.parseDouble(fields.get("glitchIntensity"));
        double time = Double.parseDouble(fields.get("timeOffset"));
        double distort = Double.parseDouble(fields.get("distortionFactor"));
        double data = Double.parseDouble(fields.get("dataDensity"));
        material = new HolographicPBRMaterial(base, rainbow, scan, glitch, time, distort, data);
      }
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildHybridTextMaterial(ElenaParser parser) {
      final int fsize = fields.size();
      
      Material material = null;
      
      if (fsize == 0x0000) {
        // No parameters: create default hybrid material
        material = new HybridTextMaterial(
          "HYBRID",
          Color.WHITE,
          null, // gradientColor
          "horizontal", // gradientType
          new Color(0x00000000), // bgColor
          "Arial",
          Font.BOLD,
          100,
          0, // uOffset
          0, // vOffset
          null, // imageObject
          0, // imageWidth
          0, // imageHeight
          0, // imageUOffset
          0, // imageVOffset
          new Color(0.9f, 0.9f, 0.9f), // diffuseColor
          1.5, // indexOfRefraction (cam gibi)
          0.5, // transparency
          0.2, // reflectivity
          new Color(0.95f, 1.0f, 1.0f), // filterColorInside
          new Color(1.0f, 0.98f, 0.95f), // filterColorOutside
          Color.WHITE, // specularColor
          32.0, // shininess
          0.1, // ambientCoefficient
          0.7, // diffuseCoefficient
          0.7  // specularCoefficient
        );
        } else {
        // Parse word with default
        String word = "HYBRID";
        if (fields.containsKey("word")) {
          word = fields.get("word").replace("\"", "");
        }
        
        // Parse text color with default
        Color textColor = Color.WHITE;
        if (fields.containsKey("textColor")) {
          textColor = parseColor(fields.get("textColor"));
        }
        
        // Parse gradient color (optional)
        Color gradientColor = null;
        if (fields.containsKey("gradientColor")) {
          gradientColor = parseColor(fields.get("gradientColor"));
        }
        
        // Parse gradient type with default
        String gradientType = "horizontal";
        if (fields.containsKey("gradientType")) {
          gradientType = fields.get("gradientType").toLowerCase();
          gradientType = gradientType.replace("\"", "");
          if (!gradientType.equals("horizontal") &&
            !gradientType.equals("vertical") &&
            !gradientType.equals("diagonal")) {
            gradientType = "horizontal";
          }
        }
        
        // Parse background color with default
        Color bgColor = new Color(0x00000000);
        if (fields.containsKey("bgColor")) {
          bgColor = parseColor(fields.get("bgColor"));
        }
        
        // Parse font family with default
        String fontFamily = "Arial";
        if (fields.containsKey("fontFamily")) {
          fontFamily = fields.get("fontFamily").replace("\"", "");
        }
        
        // Parse font style with default
        int fontStyle = Font.BOLD;
        if (fields.containsKey("fontStyle")) {
          String style = fields.get("fontStyle").toLowerCase();
          switch (style) {
            case "plain": fontStyle = Font.PLAIN; break;
            case "italic": fontStyle = Font.ITALIC; break;
            case "bolditalic": fontStyle = Font.BOLD | Font.ITALIC; break;
            default: fontStyle = Font.BOLD;
          }
        }
        
        // Parse font size with default
        int fontSize = 100;
        if (fields.containsKey("fontSize")) {
          try {
            fontSize = Integer.parseInt(fields.get("fontSize"));
            fontSize = Math.max(10, Math.min(500, fontSize));
            } catch (NumberFormatException e) {
            fontSize = 100;
          }
        }
        
        // Parse text offsets
        int uOffset = 0;
        if (fields.containsKey("uOffset")) {
          try {
            uOffset = Integer.parseInt(fields.get("uOffset"));
            } catch (NumberFormatException e) {
            uOffset = 0;
          }
        }
        
        int vOffset = 0;
        if (fields.containsKey("vOffset")) {
          try {
            vOffset = Integer.parseInt(fields.get("vOffset"));
            } catch (NumberFormatException e) {
            vOffset = 0;
          }
        }
        
        // Parse image parameters
        BufferedImage imageObject = null;
        int imageWidth = 0;
        int imageHeight = 0;
        int imageUOffset = 0;
        int imageVOffset = 0;
        
        if (fields.containsKey("imageObject")) {
          String imageName = fields.get("imageObject").replace("\"", "");
          if (imageName.equals("null")) {
            imageObject = null;
            } else {
            imageObject = parser.loadImage(imageName);
          }
        }
        
        if (fields.containsKey("imageWidth")) {
          try {
            imageWidth = Integer.parseInt(fields.get("imageWidth"));
            imageWidth = Math.max(0, imageWidth);
            } catch (NumberFormatException e) {
            imageWidth = 0;
          }
        }
        
        if (fields.containsKey("imageHeight")) {
          try {
            imageHeight = Integer.parseInt(fields.get("imageHeight"));
            imageHeight = Math.max(0, imageHeight);
            } catch (NumberFormatException e) {
            imageHeight = 0;
          }
        }
        
        if (fields.containsKey("imageUOffset")) {
          try {
            imageUOffset = Integer.parseInt(fields.get("imageUOffset"));
            } catch (NumberFormatException e) {
            imageUOffset = 0;
          }
        }
        
        if (fields.containsKey("imageVOffset")) {
          try {
            imageVOffset = Integer.parseInt(fields.get("imageVOffset"));
            } catch (NumberFormatException e) {
            imageVOffset = 0;
          }
        }
        
        // Parse Phong material properties
        Color diffuseColor = new Color(0.9f, 0.9f, 0.9f);
        if (fields.containsKey("diffuseColor")) {
          diffuseColor = parseColor(fields.get("diffuseColor"));
        }
        
        Color specularColor = Color.WHITE;
        if (fields.containsKey("specularColor")) {
          specularColor = parseColor(fields.get("specularColor"));
        }
        
        double shininess = 32.0;
        if (fields.containsKey("shininess")) {
          try {
            shininess = Double.parseDouble(fields.get("shininess"));
            shininess = Math.max(1.0, shininess);
            } catch (NumberFormatException e) {
            shininess = 32.0;
          }
        }
        
        double ambientCoefficient = 0.1;
        if (fields.containsKey("ambientCoefficient")) {
          try {
            ambientCoefficient = Double.parseDouble(fields.get("ambientCoefficient"));
            ambientCoefficient = Math.max(0.0, Math.min(1.0, ambientCoefficient));
            } catch (NumberFormatException e) {
            ambientCoefficient = 0.1;
          }
        }
        
        double diffuseCoefficient = 0.7;
        if (fields.containsKey("diffuseCoefficient")) {
          try {
            diffuseCoefficient = Double.parseDouble(fields.get("diffuseCoefficient"));
            diffuseCoefficient = Math.max(0.0, Math.min(1.0, diffuseCoefficient));
            } catch (NumberFormatException e) {
            diffuseCoefficient = 0.7;
          }
        }
        
        double specularCoefficient = 0.7;
        if (fields.containsKey("specularCoefficient")) {
          try {
            specularCoefficient = Double.parseDouble(fields.get("specularCoefficient"));
            specularCoefficient = Math.max(0.0, Math.min(1.0, specularCoefficient));
            } catch (NumberFormatException e) {
            specularCoefficient = 0.7;
          }
        }
        
        // --- HYBRID-SPECIFIC: Dielectric Properties ---
        double indexOfRefraction = 1.5;
        if (fields.containsKey("indexOfRefraction")) {
          try {
            indexOfRefraction = Double.parseDouble(fields.get("indexOfRefraction"));
            indexOfRefraction = Math.max(1.0, indexOfRefraction);
            } catch (NumberFormatException e) {
            indexOfRefraction = 1.5;
          }
        }
        
        double transparency = 0.5;
        if (fields.containsKey("transparency")) {
          try {
            transparency = Double.parseDouble(fields.get("transparency"));
            transparency = Math.max(0.0, Math.min(1.0, transparency));
            } catch (NumberFormatException e) {
            transparency = 0.5;
          }
        }
        
        double reflectivity = 0.2;
        if (fields.containsKey("reflectivity")) {
          try {
            reflectivity = Double.parseDouble(fields.get("reflectivity"));
            reflectivity = Math.max(0.0, Math.min(1.0, reflectivity));
            } catch (NumberFormatException e) {
            reflectivity = 0.2;
          }
        }
        
        Color filterColorInside = new Color(0.95f, 1.0f, 1.0f);
        if (fields.containsKey("filterColorInside")) {
          filterColorInside = parseColor(fields.get("filterColorInside"));
        }
        
        Color filterColorOutside = new Color(1.0f, 0.98f, 0.95f);
        if (fields.containsKey("filterColorOutside")) {
          filterColorOutside = parseColor(fields.get("filterColorOutside"));
        }
        
        // Create material with all parsed parameters
        material = new HybridTextMaterial(
          word,
          textColor,
          gradientColor,
          gradientType,
          bgColor,
          fontFamily,
          fontStyle,
          fontSize,
          uOffset,
          vOffset,
          imageObject,
          imageWidth,
          imageHeight,
          imageUOffset,
          imageVOffset,
          diffuseColor,
          indexOfRefraction,
          transparency,
          reflectivity,
          filterColorInside,
          filterColorOutside,
          specularColor,
          shininess,
          ambientCoefficient,
          diffuseCoefficient,
          specularCoefficient
        );
      }
      
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildMarblePBRMaterial(ElenaParser parser) {
      Material material;
      if (fields.size() < 1) {
        material = new MarblePBRMaterial();
        } else {
        Color base = parser.parseColor(fields.get("baseColor"));
        Color vein = parser.parseColor(fields.get("veinColor"));
        double scale = Double.parseDouble(fields.get("veinScale"));
        double contrast = Double.parseDouble(fields.get("veinContrast"));
        double rough = Double.parseDouble(fields.get("roughness"));
        double refl = Double.parseDouble(fields.get("reflectivity"));
        double intensity = Double.parseDouble(fields.get("veinIntensity"));
        material = new MarblePBRMaterial(base, vein, scale, contrast, rough, refl, intensity);
      }
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildPlasticPBRMaterial(ElenaParser parser) {
      Color albedo = parser.parseColor(fields.get("albedo"));
      Material material;
      if (fields.containsKey("roughness")) {
        double rough = Double.parseDouble(fields.get("roughness"));
        double refl = Double.parseDouble(fields.get("reflectivity"));
        double ior = Double.parseDouble(fields.get("ior"));
        double trans = Double.parseDouble(fields.get("transparency"));
        material = new PlasticPBRMaterial(albedo, rough, refl, ior, trans);
        } else {
        material = new PlasticPBRMaterial(albedo);
      }
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildSilverPBRMaterial(ElenaParser parser) {
      Material material;
      if (fields.containsKey("roughness") && !fields.containsKey("albedo")) {
        double rough = Double.parseDouble(fields.get("roughness"));
        material = new SilverPBRMaterial(rough);
        } else {
        Color albedo = parser.parseColor(fields.get("albedo"));
        double rough = Double.parseDouble(fields.get("roughness"));
        double metal = Double.parseDouble(fields.get("metalness"));
        material = new SilverPBRMaterial(albedo, rough, metal);
      }
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildWaterPBRMaterial(ElenaParser parser) {
      Material material;
      if (fields.size() < 1) {
        material = new WaterPBRMaterial();
        } else {
        Color water = parser.parseColor(fields.get("waterColor"));
        double rough = Double.parseDouble(fields.get("roughness"));
        double wave = Double.parseDouble(fields.get("waveIntensity"));
        double murk = Double.parseDouble(fields.get("murkiness"));
        double foam = Double.parseDouble(fields.get("foamThreshold"));
        material = new WaterPBRMaterial(water, rough, wave, murk, foam);
      }
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildWoodPBRMaterial(ElenaParser parser) {
      Material material;
      if (fields.size() < 1) {
        material = new WoodPBRMaterial();
        } else {
        Color c1 = parser.parseColor(fields.get("color1"));
        Color c2 = parser.parseColor(fields.get("color2"));
        double tile = Double.parseDouble(fields.get("tileSize"));
        double rough = Double.parseDouble(fields.get("roughness"));
        double spec = Double.parseDouble(fields.get("specularScale"));
        material = new WoodPBRMaterial(c1, c2, tile, rough, spec);
      }
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildPhongElenaMaterial(ElenaParser parser) {
      Color diff = parser.parseColor(fields.get("diffuseColor"));
      double refl = Double.parseDouble(fields.get("reflectivity"));
      double shin = Double.parseDouble(fields.get("shininess"));
      Material material;
      if (fields.containsKey("ambientCoefficient")) {
        double amb = Double.parseDouble(fields.get("ambientCoefficient"));
        material = new PhongElenaMaterial(diff, refl, shin, amb);
        } else {
        material = new PhongElenaMaterial(diff, refl, shin);
      }
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildPhongMaterial(ElenaParser parser) {
      Color diff = parser.parseColor(fields.get("diffuseColor"));
      Material material;
      if (fields.containsKey("specularColor")) {
        Color spec = parser.parseColor(fields.get("specularColor"));
        double shin = Double.parseDouble(fields.get("shininess"));
        double amb = Double.parseDouble(fields.get("ambientCoefficient"));
        double diffCoeff = Double.parseDouble(fields.get("diffuseCoefficient"));
        double specCoeff = Double.parseDouble(fields.get("specularCoefficient"));
        double refl = Double.parseDouble(fields.get("reflectivity"));
        double ior = Double.parseDouble(fields.get("ior"));
        double trans = Double.parseDouble(fields.get("transparency"));
        material = new PhongMaterial(diff, spec, shin, amb, diffCoeff, specCoeff, refl, ior, trans);
        } else {
        material = new PhongMaterial(diff);
      }
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildPixelArtMaterial(ElenaParser parser) {
      // 1. Parse color palette
      Color[] palette;
      try {
        String paletteStr = fields.get("palette");
        // Remove brackets and # symbols then split by commas
        String[] colorStrs = paletteStr.replaceAll("[\\[\\]#]", "").split(",");
        palette = new Color[colorStrs.length];
        
        for (int i = 0; i < colorStrs.length; i++) {
          String hex = colorStrs[i].trim();
          
          // Convert 3-digit hex to 6-digit format (e.g., #FFF -> FFFFFF)
          if (hex.length() == 3) {
            hex = "" + hex.charAt(0) + hex.charAt(0)
            + hex.charAt(1) + hex.charAt(1)
            + hex.charAt(2) + hex.charAt(2);
          }
          // Parse 6-digit hex color
          int rgb = Integer.parseInt(hex, 16);
          palette[i] = new Color(rgb);
        }
        } catch (Exception e) {
        System.err.println("Palette parsing error: " + e.getMessage());
        // Fallback to default palette
        palette = new Color[]{Color.BLACK, Color.WHITE};
      }
      
      // 2. Safely parse numeric parameters with defaults
      double pixelSize = parseDoubleSafe("pixelSize", 0.05);
      
      Material material;
      // Check if using extended constructor (with additional parameters)
      if (fields.size() > 2) {
        double ambient = parseDoubleSafe("ambient", 0.1);
        double diffuse = parseDoubleSafe("diffuse", 0.7);
        double specular = parseDoubleSafe("specular", 0.3);
        double shininess = parseDoubleSafe("shininess", 30.0);
        double reflectivity = parseDoubleSafe("reflectivity", 0.2);
        double ior = parseDoubleSafe("ior", 1.0);
        double transparency = parseDoubleSafe("transparency", 0.0);
        
        material = new PixelArtMaterial(palette, pixelSize, ambient, diffuse,
          specular, shininess, reflectivity,
        ior, transparency, Matrix4.identity());
        } else {
        // Use basic constructor if only palette and pixelSize are provided
        material = new PixelArtMaterial(palette, pixelSize, Matrix4.identity());
      }
      
      parser.objects.put(id, material);
      return material;
    }
    
    /**
     * Safely parses a double value from fields map with fallback to default value
     * @param key The field name to look up
     * @param defaultValue Fallback value if parsing fails
     * @return Parsed double value or default if unavailable/invalid
     */
    private double parseDoubleSafe(String key, double defaultValue) {
      try {
        return fields.containsKey(key) ?
        Double.parseDouble(fields.get(key)) :
        defaultValue;
        } catch (NumberFormatException e) {
        System.err.println("Invalid value for " + key + ", using default: " + defaultValue);
        return defaultValue;
      }
    }
    
    private Material buildPlatinumMaterial(ElenaParser parser) {
      Material material;
      if (fields.containsKey("specularBalance")) {
        double balance = Double.parseDouble(fields.get("specularBalance"));
        material = new PlatinumMaterial(Matrix4.identity(), balance);
        } else {
        material = new PlatinumMaterial(Matrix4.identity());
      }
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildTransparentEmojiMaterial(ElenaParser parser) {
      Material material = null;
      final int fsize = fields.size();
      
      // Default values
      String path = null;
      Color checkerColor1 = new Color(200, 200, 200); // Light gray
      Color checkerColor2 = new Color(150, 150, 150); // Dark gray
      double checkerSize = 0.1;
      double uOffset = 0.0;
      double vOffset = 0.0;
      double uScale = 1.0;
      double vScale = 1.0;
      boolean isRepeatTexture = false;
      boolean isMessy = false;
      
      // Parse fields
      if (fields.containsKey("imagePath")) {
        path = fields.get("imagePath").replace("\"", "");
      }
      
      if (fields.containsKey("checkerColor1")) {
        checkerColor1 = parseColor(fields.get("checkerColor1"));
      }
      
      if (fields.containsKey("checkerColor2")) {
        checkerColor2 = parseColor(fields.get("checkerColor2"));
      }
      
      if (fields.containsKey("checkerSize")) {
        checkerSize = Double.parseDouble(fields.get("checkerSize"));
      }
      
      if (fields.containsKey("uOffset")) {
        uOffset = Double.parseDouble(fields.get("uOffset"));
      }
      
      if (fields.containsKey("vOffset")) {
        vOffset = Double.parseDouble(fields.get("vOffset"));
      }
      
      if (fields.containsKey ("isRepeatTexture")) {
        isRepeatTexture = Boolean.parseBoolean (fields.get ("isRepeatTexture"));
      }
      
      if (fields.containsKey ("isMessy")) {
        isMessy = Boolean.parseBoolean (fields.get ("isMessy"));
      }
      
      // Load image if path is provided
      BufferedImage img = null;
      if (path != null) {
        img = parser.loadImage(path);
      }
      
      // Parse scale parameters
      if (fields.containsKey("uScale")) {
        uScale = Double.parseDouble(fields.get("uScale"));
      }
      
      if (fields.containsKey("vScale")) {
        vScale = Double.parseDouble(fields.get("vScale"));
      }
      
      // Create material based on available parameters
      if (fsize == 0) {
        material = new TransparentEmojiMaterial();
      }
      else if (fsize == 1 && path != null) {
        material = new TransparentEmojiMaterial(img);
      }
      else {
        material = new TransparentEmojiMaterial(
          img,
          checkerColor1, checkerColor2,
          checkerSize, uOffset, vOffset,
          uScale, vScale, isRepeatTexture, isMessy
        );
      }
      
      if (material != null) {
        parser.objects.put(id, material);
      }
      return material;
    }
    
    private Material buildProceduralFlowerMaterial(ElenaParser parser) {
      double count = Double.parseDouble(fields.get("petalCount"));
      Color petal = parser.parseColor(fields.get("petalColor"));
      Color center = parser.parseColor(fields.get("centerColor"));
      Material material;
      if (fields.containsKey("ambientStrength")) {
        double amb = Double.parseDouble(fields.get("ambientStrength"));
        material = new ProceduralFlowerMaterial(count, petal, center, amb);
        } else {
        material = new ProceduralFlowerMaterial(count, petal, center);
      }
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildPureWaterMaterial(ElenaParser parser) {
      Material material;
      if (fields.size() < 1) {
        material = new PureWaterMaterial();
        } else {
        Color base = parser.parseColor(fields.get("baseColor"));
        double speed = Double.parseDouble(fields.get("flowSpeed"));
        material = new PureWaterMaterial(base, speed);
      }
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildQuantumFieldMaterial(ElenaParser parser) {
      Color primary = parser.parseColor(fields.get("primary"));
      Color secondary = parser.parseColor(fields.get("secondary"));
      double energy = Double.parseDouble(fields.get("energy"));
      Material material = new QuantumFieldMaterial(primary, secondary, energy, Matrix4.identity());
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildRandomMaterial(ElenaParser parser) {
      Material material = new RandomMaterial(Matrix4.identity());
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildRectangleCheckerMaterial(ElenaParser parser) {
      Color c1 = parser.parseColor(fields.get("color1"));
      Color c2 = parser.parseColor(fields.get("color2"));
      double w = Double.parseDouble(fields.get("rectWidth"));
      double h = Double.parseDouble(fields.get("rectHeight"));
      Material material;
      if (fields.containsKey("ambient")) {
        double amb = Double.parseDouble(fields.get("ambient"));
        double diff = Double.parseDouble(fields.get("diffuse"));
        double spec = Double.parseDouble(fields.get("specular"));
        double shin = Double.parseDouble(fields.get("shininess"));
        double refl = Double.parseDouble(fields.get("reflectivity"));
        double ior = Double.parseDouble(fields.get("ior"));
        double trans = Double.parseDouble(fields.get("transparency"));
        material = new RectangleCheckerMaterial(c1, c2, w, h, amb, diff, spec, shin, refl, ior, trans, Matrix4.identity());
        } else {
        material = new RectangleCheckerMaterial(c1, c2, w, h, Matrix4.identity());
      }
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildReflectiveMaterial(ElenaParser parser) {
      Material material;
      if (fields.size() < 1) {
        material = new ReflectiveMaterial();
        } else {
        Color base = parser.parseColor(fields.get("baseColor"));
        double refl = Double.parseDouble(fields.get("reflectivity"));
        double rough = Double.parseDouble(fields.get("roughness"));
        material = new ReflectiveMaterial(base, refl, rough);
      }
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildRoughMaterial(ElenaParser parser) {
      Color color = parser.parseColor(fields.get("color"));
      double rough = Double.parseDouble(fields.get("roughness"));
      Material material;
      if (fields.containsKey("diffuseCoefficient")) {
        double diff = Double.parseDouble(fields.get("diffuseCoefficient"));
        double refl = Double.parseDouble(fields.get("reflectivity"));
        material = new RoughMaterial(color, rough, diff, refl);
        } else {
        material = new RoughMaterial(color, rough);
      }
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildSilverMaterial(ElenaParser parser) {
      Material material = new SilverMaterial();
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildSmartGlassMaterial(ElenaParser parser) {
      Material material;
      if (fields.size() < 1) {
        material = new SmartGlassMaterial();
        } else {
        Color color = parser.parseColor(fields.get("color"));
        double clarity = Double.parseDouble(fields.get("clarity"));
        material = new SmartGlassMaterial(color, clarity);
      }
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildSolidCheckerboardMaterial(ElenaParser parser) {
      Color c1 = parser.parseColor(fields.get("color1"));
      Color c2 = parser.parseColor(fields.get("color2"));
      double size = Double.parseDouble(fields.get("size"));
      double amb = Double.parseDouble(fields.get("ambient"));
      double diff = Double.parseDouble(fields.get("diffuse"));
      Material material = new SolidCheckerboardMaterial(c1, c2, size, amb, diff, Matrix4.identity());
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildSolidColorMaterial(ElenaParser parser) {
      Color color = parser.parseColor(fields.get("color"));
      Material material = new SolidColorMaterial(color);
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildSquaredMaterial(ElenaParser parser) {
      Color c1 = parser.parseColor(fields.get("color1"));
      Color c2 = parser.parseColor(fields.get("color2"));
      double scale = Double.parseDouble(fields.get("scale"));
      Material material;
      if (fields.containsKey("ambient")) {
        double amb = Double.parseDouble(fields.get("ambient"));
        double diff = Double.parseDouble(fields.get("diffuse"));
        double spec = Double.parseDouble(fields.get("specular"));
        double shin = Double.parseDouble(fields.get("shininess"));
        Color specColor = parser.parseColor(fields.get("specularColor"));
        double refl = Double.parseDouble(fields.get("reflectivity"));
        double ior = Double.parseDouble(fields.get("ior"));
        double trans = Double.parseDouble(fields.get("transparency"));
        material = new SquaredMaterial(c1, c2, scale, amb, diff, spec, shin, specColor, refl, ior, trans, Matrix4.identity());
        } else {
        material = new SquaredMaterial(c1, c2, scale, Matrix4.identity());
      }
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildStainedGlassMaterial(ElenaParser parser) {
      Color tint = parser.parseColor(fields.get("tint"));
      double rough = Double.parseDouble(fields.get("roughness"));
      Material material = new StainedGlassMaterial(tint, rough, Matrix4.identity());
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildStarfieldMaterial(ElenaParser parser) {
      Material material;
      if (fields.size() < 1) {
        material = new StarfieldMaterial(Matrix4.identity());
        } else {
        Color nebula = parser.parseColor(fields.get("nebulaColor"));
        double size = Double.parseDouble(fields.get("starSize"));
        double density = Double.parseDouble(fields.get("starDensity"));
        double speed = Double.parseDouble(fields.get("twinkleSpeed"));
        material = new StarfieldMaterial(Matrix4.identity(), nebula, size, density, speed);
      }
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildStripedMaterial(ElenaParser parser) {
      Color c1 = parser.parseColor(fields.get("color1"));
      Color c2 = parser.parseColor(fields.get("color2"));
      double size = Double.parseDouble(fields.get("stripeSize"));
      StripeDirection dir=StripeDirection.HORIZONTAL;
      Material material;
      if (fields.containsKey("direction")) {
        dir = StripeDirection.valueOf(fields.get("direction"));
        material = new StripedMaterial(c1, c2, size, dir, Matrix4.identity());
        } else if (fields.containsKey("ambientCoefficient")) {
        double amb = Double.parseDouble(fields.get("ambientCoefficient"));
        double diff = Double.parseDouble(fields.get("diffuseCoefficient"));
        double spec = Double.parseDouble(fields.get("specularCoefficient"));
        double shin = Double.parseDouble(fields.get("shininess"));
        double refl = Double.parseDouble(fields.get("reflectivity"));
        double ior = Double.parseDouble(fields.get("ior"));
        double trans = Double.parseDouble(fields.get("transparency"));
        material = new StripedMaterial(c1, c2, size, dir, amb, diff, spec, shin, refl, ior, trans, Matrix4.identity());
        } else {
        material = new StripedMaterial(c1, c2, size, Matrix4.identity());
      }
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildTexturedPhongMaterial(ElenaParser parser) {
      Color base = parser.parseColor(fields.get("baseDiffuseColor"));
      String path = fields.get("imagePath").replace("\"", "");
      double trans = Double.parseDouble(fields.get("transparency"));
      BufferedImage img = parser.loadImageARGB(path);
      Material material;
      if (fields.containsKey("specularColor")) {
        Color spec = parser.parseColor(fields.get("specularColor"));
        double shin = Double.parseDouble(fields.get("shininess"));
        double amb = Double.parseDouble(fields.get("ambientCoefficient"));
        double diff = Double.parseDouble(fields.get("diffuseCoefficient"));
        double specCoeff = Double.parseDouble(fields.get("specularCoefficient"));
        double refl = Double.parseDouble(fields.get("reflectivity"));
        double ior = Double.parseDouble(fields.get("ior"));
        if (fields.containsKey("uOffset")) {
          double uOff = Double.parseDouble(fields.get("uOffset"));
          double vOff = Double.parseDouble(fields.get("vOffset"));
          double uScale = Double.parseDouble(fields.get("uScale"));
          double vScale = Double.parseDouble(fields.get("vScale"));
          material = new TexturedPhongMaterial(base, spec, shin, amb, diff, specCoeff, refl, ior, trans, img, uOff, vOff, uScale, vScale, Matrix4.identity());
          } else {
          material = new TexturedPhongMaterial(base, spec, shin, amb, diff, specCoeff, refl, ior, trans, img, Matrix4.identity());
        }
        } else if (fields.containsKey("diffuseColor")) {
        material = new TexturedPhongMaterial(parser.parseColor(fields.get("diffuseColor")), Matrix4.identity());
        } else {
        material = new TexturedPhongMaterial(base, img, Matrix4.identity());
      }
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildTextureMaterial(ElenaParser parser) {
      String path = fields.get("imagePath").replace("\"", "");
      BufferedImage img = parser.loadImageARGB(path);
      Material material = new TextureMaterial(img);
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildTriangleMaterial(ElenaParser parser) {
      Color c1 = parser.parseColor(fields.get("color1"));
      Color c2 = parser.parseColor(fields.get("color2"));
      double size = Double.parseDouble(fields.get("triangleSize"));
      Material material;
      if (fields.containsKey("ambientCoefficient")) {
        double amb = Double.parseDouble(fields.get("ambientCoefficient"));
        double diff = Double.parseDouble(fields.get("diffuseCoefficient"));
        double spec = Double.parseDouble(fields.get("specularCoefficient"));
        double shin = Double.parseDouble(fields.get("shininess"));
        double refl = Double.parseDouble(fields.get("reflectivity"));
        double ior = Double.parseDouble(fields.get("ior"));
        double trans = Double.parseDouble(fields.get("transparency"));
        material = new TriangleMaterial(c1, c2, size, amb, diff, spec, shin, refl, ior, trans, Matrix4.identity());
        } else {
        material = new TriangleMaterial(c1, c2, size, Matrix4.identity());
      }
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildWaterfallMaterial(ElenaParser parser) {
      Material material;
      if (fields.size() < 1) {
        material = new WaterfallMaterial();
        } else {
        Color base = parser.parseColor(fields.get("baseColor"));
        double speed = Double.parseDouble(fields.get("flowSpeed"));
        material = new WaterfallMaterial(base, speed);
      }
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildWaterRippleMaterial(ElenaParser parser) {
      Color water = parser.parseColor(fields.get("waterColor"));
      double speed = Double.parseDouble(fields.get("waveSpeed"));
      Material material;
      if (fields.containsKey("reflectivity")) {
        double refl = Double.parseDouble(fields.get("reflectivity"));
        material = new WaterRippleMaterial(water, speed, refl, Matrix4.identity());
        } else {
        material = new WaterRippleMaterial(water, speed, Matrix4.identity());
      }
      parser.objects.put(id, material);
      return material;
    }
    
    private Material buildWoodMaterial(ElenaParser parser) {
      Color base = parser.parseColor(fields.get("baseColor"));
      Color grain = parser.parseColor(fields.get("grainColor"));
      double freq = Double.parseDouble(fields.get("grainFrequency"));
      double ring = Double.parseDouble(fields.get("ringVariation"));
      Material material;
      if (fields.containsKey("ambientCoeff")) {
        double amb = Double.parseDouble(fields.get("ambientCoeff"));
        double diff = Double.parseDouble(fields.get("diffuseCoeff"));
        double spec = Double.parseDouble(fields.get("specularCoeff"));
        double shin = Double.parseDouble(fields.get("shininess"));
        double refl = Double.parseDouble(fields.get("reflectivity"));
        double ior = Double.parseDouble(fields.get("ior"));
        double trans = Double.parseDouble(fields.get("transparency"));
        material = new WoodMaterial(base, grain, freq, ring, amb, diff, spec, shin, refl, ior, trans, Matrix4.identity());
        } else {
        material = new WoodMaterial(base, grain, freq, ring, Matrix4.identity());
      }
      parser.objects.put(id, material);
      return material;
    }
    
    private EMShape buildImage3D(ElenaParser parser) {
      String path = fields.get("imagePath").replace("\"", "");
      BufferedImage img = parser.loadImageARGB(path);
      if (fields.containsKey("baseSize")) {
        int baseSize = Integer.parseInt(fields.get("baseSize"));
        double widthScale = Double.parseDouble(fields.get("widthScale"));
        double heightScale = Double.parseDouble(fields.get("heightScale"));
        double thickness = Double.parseDouble(fields.get("thickness"));
        Image3D shape = new Image3D(img, baseSize, widthScale, heightScale, thickness);
        if (fields.containsKey("material")) {
          shape.setMaterial((Material) parser.objects.get(fields.get("material")));
        }
        if (fields.containsKey("transform")) {
          shape.setTransform(parser.parseTransform(fields.get("transform")));
        }
        return shape;
        } else {
        Image3D shape = new Image3D(img);
        if (fields.containsKey("material")) {
          shape.setMaterial((Material) parser.objects.get(fields.get("material")));
        }
        if (fields.containsKey("transform")) {
          shape.setTransform(parser.parseTransform(fields.get("transform")));
        }
        return shape;
      }
    }
    
    private EMShape buildLetter3D(ElenaParser parser) {
      char letter = fields.get("letter").charAt(1);
      if (fields.containsKey("baseSize")) {
        int baseSize = Integer.parseInt(fields.get("baseSize"));
        double widthScale = Double.parseDouble(fields.get("widthScale"));
        double heightScale = Double.parseDouble(fields.get("heightScale"));
        double thickness = Double.parseDouble(fields.get("thickness"));
        String fontName = fields.get("font").replace("\"", "");
        int fontSizeVal = Integer.parseInt(fields.get("fontSize"));
        int fontStyleVal = Integer.parseInt(fields.get("fontStyle"));
        Font font = new Font(fontName, fontStyleVal, fontSizeVal);
        Letter3D shape = new Letter3D(letter, baseSize, widthScale, heightScale, thickness, font);
        if (fields.containsKey("material")) {
          shape.setMaterial((Material) parser.objects.get(fields.get("material")));
        }
        if (fields.containsKey("transform")) {
          shape.setTransform(parser.parseTransform(fields.get("transform")));
        }
        return shape;
        } else {
        Letter3D shape = new Letter3D(letter);
        if (fields.containsKey("material")) {
          shape.setMaterial((Material) parser.objects.get(fields.get("material")));
        }
        if (fields.containsKey("transform")) {
          shape.setTransform(parser.parseTransform(fields.get("transform")));
        }
        return shape;
      }
    }
  }
  
}//File ends
