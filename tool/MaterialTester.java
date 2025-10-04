public class MaterialTester {
  public static void main(String[] args) {
    // Test için basit bileşenler
    Point3 point = new Point3(0, 0, 0);
    Vector3 normal = new Vector3(0, 1, 0); // Yukarı doğru normal
    Point3 viewer = new Point3(0, 2, 3);   // İzleyici konumu
    Light light = new Light(new Point3(2, 5, 2), 1.0f); // Işık kaynağı
    
    // Cam materyalini test et
    Material glass = new AdvancedGlassMaterial(1.5f, 0.8f, new Color(200, 230, 255), 0.2f);
    
    Color result = glass.getColorAt(point, normal, light, viewer);
    System.out.println("Cam rengi: " + result);
    System.out.println("Şeffaflık: " + glass.getTransparency());
    System.out.println("Yansıma: " + glass.getReflectivity());
    System.out.println("Kırılma indisi: " + glass.getIOR());
  }
}