package net.elena.murat.util;

import net.elena.murat.math.*;
import net.elena.murat.shape.*;
import java.util.HashMap;
import java.util.Map;

// Java 8 uyumluluğu için yeni şekil örnekleri oluşturmak üzere özel arayüz
interface ShapeFactory {
  EMShape createShape();
}

public final class Font3DUtils {
  public static final float CHAR_HEIGHT = 2.0f;
  public static final float CHAR_WIDTH = 1.8f;
  private static final float DEPTH = 0.5f; // Harflerin Z eksenindeki kalınlığı
  private static final float STROKE_WIDTH = 0.3f; // Harf segmentlerinin genişliği/çapı
  
  // CHAR_CREATOR_MAP artık doğrudan şekil örneklerini değil, onları oluşturacak fabrikaları tutuyor.
  private static final Map<Character, ShapeFactory> CHAR_CREATOR_MAP = new HashMap<>();
  
  static {
    // TÜM BÜYÜK HARFLER
    CHAR_CREATOR_MAP.put('A', new ShapeFactory() { public EMShape createShape() { return createLetterA(); } });
    CHAR_CREATOR_MAP.put('B', new ShapeFactory() { public EMShape createShape() { return createLetterB(); } });
    CHAR_CREATOR_MAP.put('C', new ShapeFactory() { public EMShape createShape() { return createLetterC(); } });
    CHAR_CREATOR_MAP.put('D', new ShapeFactory() { public EMShape createShape() { return createLetterD(); } });
    CHAR_CREATOR_MAP.put('E', new ShapeFactory() { public EMShape createShape() { return createLetterE(); } });
    CHAR_CREATOR_MAP.put('F', new ShapeFactory() { public EMShape createShape() { return createLetterF(); } });
    CHAR_CREATOR_MAP.put('G', new ShapeFactory() { public EMShape createShape() { return createLetterG(); } });
    CHAR_CREATOR_MAP.put('H', new ShapeFactory() { public EMShape createShape() { return createLetterH(); } });
    CHAR_CREATOR_MAP.put('I', new ShapeFactory() { public EMShape createShape() { return createLetterI(); } });
    CHAR_CREATOR_MAP.put('J', new ShapeFactory() { public EMShape createShape() { return createLetterJ(); } });
    CHAR_CREATOR_MAP.put('K', new ShapeFactory() { public EMShape createShape() { return createLetterK(); } });
    CHAR_CREATOR_MAP.put('L', new ShapeFactory() { public EMShape createShape() { return createLetterL(); } });
    CHAR_CREATOR_MAP.put('M', new ShapeFactory() { public EMShape createShape() { return createLetterM(); } });
    CHAR_CREATOR_MAP.put('N', new ShapeFactory() { public EMShape createShape() { return createLetterN(); } });
    CHAR_CREATOR_MAP.put('O', new ShapeFactory() { public EMShape createShape() { return createLetterO(); } });
    CHAR_CREATOR_MAP.put('P', new ShapeFactory() { public EMShape createShape() { return createLetterP(); } });
    CHAR_CREATOR_MAP.put('Q', new ShapeFactory() { public EMShape createShape() { return createLetterQ(); } });
    CHAR_CREATOR_MAP.put('R', new ShapeFactory() { public EMShape createShape() { return createLetterR(); } });
    CHAR_CREATOR_MAP.put('S', new ShapeFactory() { public EMShape createShape() { return createLetterS(); } });
    CHAR_CREATOR_MAP.put('T', new ShapeFactory() { public EMShape createShape() { return createLetterT(); } });
    CHAR_CREATOR_MAP.put('U', new ShapeFactory() { public EMShape createShape() { return createLetterU(); } });
    CHAR_CREATOR_MAP.put('V', new ShapeFactory() { public EMShape createShape() { return createLetterV(); } });
    CHAR_CREATOR_MAP.put('W', new ShapeFactory() { public EMShape createShape() { return createLetterW(); } });
    CHAR_CREATOR_MAP.put('X', new ShapeFactory() { public EMShape createShape() { return createLetterX(); } });
    CHAR_CREATOR_MAP.put('Y', new ShapeFactory() { public EMShape createShape() { return createLetterY(); } });
    CHAR_CREATOR_MAP.put('Z', new ShapeFactory() { public EMShape createShape() { return createLetterZ(); } });
    
    // TÜM RAKAMLAR
    CHAR_CREATOR_MAP.put('0', new ShapeFactory() { public EMShape createShape() { return createNumber0(); } });
    CHAR_CREATOR_MAP.put('1', new ShapeFactory() { public EMShape createShape() { return createNumber1(); } });
    CHAR_CREATOR_MAP.put('2', new ShapeFactory() { public EMShape createShape() { return createNumber2(); } });
    CHAR_CREATOR_MAP.put('3', new ShapeFactory() { public EMShape createShape() { return createNumber3(); } });
    CHAR_CREATOR_MAP.put('4', new ShapeFactory() { public EMShape createShape() { return createNumber4(); } });
    CHAR_CREATOR_MAP.put('5', new ShapeFactory() { public EMShape createShape() { return createNumber5(); } });
    CHAR_CREATOR_MAP.put('6', new ShapeFactory() { public EMShape createShape() { return createNumber6(); } });
    CHAR_CREATOR_MAP.put('7', new ShapeFactory() { public EMShape createShape() { return createNumber7(); } });
    CHAR_CREATOR_MAP.put('8', new ShapeFactory() { public EMShape createShape() { return createNumber8(); } });
    CHAR_CREATOR_MAP.put('9', new ShapeFactory() { public EMShape createShape() { return createNumber9(); } });
  }
  
  public static EMShape getChar(char c) {
    ShapeFactory creator = CHAR_CREATOR_MAP.get(Character.toUpperCase(c));
    if (creator == null) throw new IllegalArgumentException("Desteklenmeyen karakter: " + c);
    return creator.createShape(); // Fabrika metodunu çağırarak yeni bir örnek oluşturulur
  }
  
  // ========== HARF OLUsTURMA METODLARI ========== //
  
  private static EMShape createLetterA() {
    CompositeShape s = new CompositeShape();
    // Sol çapraz bacak
    s.addShape(createDiagonalCylinder(0.0f, 0.0f, CHAR_WIDTH/2, CHAR_HEIGHT));
    // Sağ çapraz bacak
    s.addShape(createDiagonalCylinder(CHAR_WIDTH, 0.0f, CHAR_WIDTH/2, CHAR_HEIGHT));
    // Yatay çubuk: Y pozisyonunu daha hassas ayarlayalım
    s.addShape(createHorizontalBar(CHAR_WIDTH * 0.2f, CHAR_HEIGHT * 0.3f, CHAR_WIDTH * 0.6f, STROKE_WIDTH));
    return s;
  }
  
  private static EMShape createLetterB() {
    CompositeShape s = new CompositeShape();
    s.addShape(createVerticalBar(0, 0, STROKE_WIDTH, CHAR_HEIGHT));
    s.addShape(createHalfCircle(STROKE_WIDTH, CHAR_HEIGHT*0.75f, (CHAR_WIDTH-STROKE_WIDTH)/2, false));
    s.addShape(createHalfCircle(STROKE_WIDTH, CHAR_HEIGHT*0.25f, (CHAR_WIDTH-STROKE_WIDTH)/2, true));
    return s;
  }
  
  private static EMShape createLetterC() {
    CompositeShape s = new CompositeShape();
    // C harfi için standart bir yay:
    // Başlangıç açısı: 90 derece (yukarı)
    // Bitiş açısı: 270 derece (aşağı)
    // Bu, soldan sağa doğru bir yarım daire çizer, yani 'C' harfi gibi görünür.
    s.addShape(createArc(CHAR_WIDTH/2, CHAR_HEIGHT/2, CHAR_WIDTH/2 - STROKE_WIDTH/2, 90, 270));
    return s;
  }
  
  private static EMShape createLetterD() {
    CompositeShape s = new CompositeShape();
    s.addShape(createVerticalBar(0, 0, STROKE_WIDTH, CHAR_HEIGHT));
    s.addShape(createHalfCircle(STROKE_WIDTH, CHAR_HEIGHT/2, (CHAR_WIDTH-STROKE_WIDTH)/2, false));
    return s;
  }
  
  private static EMShape createLetterE() {
    CompositeShape s = new CompositeShape();
    s.addShape(createVerticalBar(0, 0, STROKE_WIDTH, CHAR_HEIGHT));
    s.addShape(createHorizontalBar(0, CHAR_HEIGHT-STROKE_WIDTH, CHAR_WIDTH, STROKE_WIDTH));
    s.addShape(createHorizontalBar(0, CHAR_HEIGHT/2, CHAR_WIDTH*0.7f, STROKE_WIDTH));
    s.addShape(createHorizontalBar(0, 0, CHAR_WIDTH, STROKE_WIDTH));
    return s;
  }
  
  private static EMShape createLetterF() {
    CompositeShape s = new CompositeShape();
    s.addShape(createVerticalBar(0, 0, STROKE_WIDTH, CHAR_HEIGHT));
    s.addShape(createHorizontalBar(0, CHAR_HEIGHT-STROKE_WIDTH, CHAR_WIDTH, STROKE_WIDTH));
    s.addShape(createHorizontalBar(0, CHAR_HEIGHT/2, CHAR_WIDTH*0.7f, STROKE_WIDTH));
    return s;
  }
  
  private static EMShape createLetterG() {
    CompositeShape s = new CompositeShape();
    s.addShape(createArc(CHAR_WIDTH/2, CHAR_HEIGHT/2, CHAR_WIDTH/2 - STROKE_WIDTH/2, 135, 360+45)); // C gibi
    s.addShape(createHorizontalBar(CHAR_WIDTH*0.5f, CHAR_HEIGHT*0.25f, CHAR_WIDTH*0.3f, STROKE_WIDTH));
    s.addShape(createVerticalBar(CHAR_WIDTH*0.8f, 0.0f, STROKE_WIDTH, CHAR_HEIGHT*0.25f));
    return s;
  }
  
  private static EMShape createLetterH() {
    CompositeShape s = new CompositeShape();
    s.addShape(createVerticalBar(0, 0, STROKE_WIDTH, CHAR_HEIGHT));
    s.addShape(createVerticalBar(CHAR_WIDTH-STROKE_WIDTH, 0, STROKE_WIDTH, CHAR_HEIGHT));
    s.addShape(createHorizontalBar(0, CHAR_HEIGHT/2, CHAR_WIDTH, STROKE_WIDTH));
    return s;
  }
  
  private static EMShape createLetterI() {
    return createVerticalBar(CHAR_WIDTH/2-STROKE_WIDTH/2, 0, STROKE_WIDTH, CHAR_HEIGHT);
  }
  
  private static EMShape createLetterJ() {
    CompositeShape s = new CompositeShape();
    // Üst yatay çubuk (isteğe bağlı, bazı fontlarda vardır)
    s.addShape(createHorizontalBar(CHAR_WIDTH * 0.2f, CHAR_HEIGHT - STROKE_WIDTH, CHAR_WIDTH * 0.6f, STROKE_WIDTH));
    // Dikey ana gövde
    s.addShape(createVerticalBar(CHAR_WIDTH * 0.5f - STROKE_WIDTH / 2, CHAR_HEIGHT * 0.1f, STROKE_WIDTH, CHAR_HEIGHT * 0.8f));
    // Kavisli alt kısım (yay)
    float arcRadius = CHAR_WIDTH * 0.3f; // Yarıçapı ayarlayarak kavisin şeklini değiştirebilirsiniz
    s.addShape(createArc(
        CHAR_WIDTH * 0.5f - STROKE_WIDTH / 2 - arcRadius, // Yay merkezinin X koordinatı
        CHAR_HEIGHT * 0.1f, // Yay merkezinin Y koordinatı (dikey çubuğun altıyla hizalı)
        arcRadius, // Yay yarıçapı
        90, // Başlangıç açısı (90 derece, yukarıdan başlar)
        180 // Bitiş açısı (180 derece, sola doğru kavis yapar)
    ));
    return s;
  }
  
  private static EMShape createLetterK() {
    CompositeShape s = new CompositeShape();
    s.addShape(createVerticalBar(0, 0, STROKE_WIDTH, CHAR_HEIGHT));
    s.addShape(createDiagonalCylinder(STROKE_WIDTH, CHAR_HEIGHT*0.7f, CHAR_WIDTH, CHAR_HEIGHT));
    s.addShape(createDiagonalCylinder(STROKE_WIDTH, CHAR_HEIGHT*0.3f, CHAR_WIDTH, 0));
    return s;
  }
  
  private static EMShape createLetterL() {
    CompositeShape s = new CompositeShape();
    s.addShape(createVerticalBar(0, 0, STROKE_WIDTH, CHAR_HEIGHT));
    s.addShape(createHorizontalBar(0, 0, CHAR_WIDTH, STROKE_WIDTH));
    return s;
  }
  
  private static EMShape createLetterM() {
    CompositeShape s = new CompositeShape();
    s.addShape(createVerticalBar(0, 0, STROKE_WIDTH, CHAR_HEIGHT));
    s.addShape(createVerticalBar(CHAR_WIDTH-STROKE_WIDTH, 0, STROKE_WIDTH, CHAR_HEIGHT));
    s.addShape(createDiagonalCylinder(0, CHAR_HEIGHT, CHAR_WIDTH/2, 0));
    s.addShape(createDiagonalCylinder(CHAR_WIDTH/2, 0, CHAR_WIDTH, CHAR_HEIGHT));
    return s;
  }
  
  private static EMShape createLetterN() {
    CompositeShape s = new CompositeShape();
    s.addShape(createVerticalBar(0, 0, STROKE_WIDTH, CHAR_HEIGHT));
    s.addShape(createVerticalBar(CHAR_WIDTH-STROKE_WIDTH, 0, STROKE_WIDTH, CHAR_HEIGHT));
    s.addShape(createDiagonalCylinder(0, CHAR_HEIGHT, CHAR_WIDTH, 0));
    return s;
  }
  
  private static EMShape createLetterO() {
    return createCircle(CHAR_WIDTH/2, CHAR_HEIGHT/2, Math.min(CHAR_WIDTH, CHAR_HEIGHT)/2 - STROKE_WIDTH/2);
  }
  
  private static EMShape createLetterP() {
    CompositeShape s = new CompositeShape();
    s.addShape(createVerticalBar(0, 0, STROKE_WIDTH, CHAR_HEIGHT));
    s.addShape(createHalfCircle(STROKE_WIDTH, CHAR_HEIGHT*0.75f, (CHAR_WIDTH-STROKE_WIDTH)/2, false));
    return s;
  }
  
  private static EMShape createLetterQ() {
    CompositeShape s = new CompositeShape();
    s.addShape(createCircle(CHAR_WIDTH/2, CHAR_HEIGHT/2, Math.min(CHAR_WIDTH, CHAR_HEIGHT)/2 - STROKE_WIDTH/2));
    s.addShape(createDiagonalCylinder(CHAR_WIDTH*0.6f, CHAR_HEIGHT*0.4f, CHAR_WIDTH, 0));
    return s;
  }
  
  private static EMShape createLetterR() {
    CompositeShape s = new CompositeShape();
    s.addShape(createVerticalBar(0, 0, STROKE_WIDTH, CHAR_HEIGHT));
    s.addShape(createHalfCircle(STROKE_WIDTH, CHAR_HEIGHT*0.75f, (CHAR_WIDTH-STROKE_WIDTH)/2, false));
    s.addShape(createDiagonalCylinder(CHAR_WIDTH/2, CHAR_HEIGHT/2, CHAR_WIDTH, 0));
    return s;
  }
  
  private static EMShape createLetterS() {
    CompositeShape s = new CompositeShape();
    s.addShape(createArc(CHAR_WIDTH/2, CHAR_HEIGHT*0.75f, (CHAR_WIDTH-STROKE_WIDTH)/2, 0, 180));
    s.addShape(createArc(CHAR_WIDTH/2, CHAR_HEIGHT*0.25f, (CHAR_WIDTH-STROKE_WIDTH)/2, 180, 360));
    return s;
  }
  
  private static EMShape createLetterT() {
    CompositeShape s = new CompositeShape();
    s.addShape(createHorizontalBar(0, CHAR_HEIGHT-STROKE_WIDTH, CHAR_WIDTH, STROKE_WIDTH));
    s.addShape(createVerticalBar(CHAR_WIDTH/2-STROKE_WIDTH/2, 0, STROKE_WIDTH, CHAR_HEIGHT));
    return s;
  }
  
  private static EMShape createLetterU() {
    CompositeShape s = new CompositeShape();
    s.addShape(createVerticalBar(0, 0, STROKE_WIDTH, CHAR_HEIGHT-STROKE_WIDTH));
    s.addShape(createVerticalBar(CHAR_WIDTH-STROKE_WIDTH, 0, STROKE_WIDTH, CHAR_HEIGHT-STROKE_WIDTH));
    s.addShape(createHorizontalBar(0, 0, CHAR_WIDTH, STROKE_WIDTH));
    return s;
  }
  
  private static EMShape createLetterV() {
    CompositeShape s = new CompositeShape();
    s.addShape(createDiagonalCylinder(0, CHAR_HEIGHT, CHAR_WIDTH/2, 0));
    s.addShape(createDiagonalCylinder(CHAR_WIDTH/2, 0, CHAR_WIDTH, CHAR_HEIGHT));
    return s;
  }
  
  private static EMShape createLetterW() {
    CompositeShape s = new CompositeShape();
    s.addShape(createDiagonalCylinder(0, CHAR_HEIGHT, CHAR_WIDTH/4, 0));
    s.addShape(createDiagonalCylinder(CHAR_WIDTH/4, 0, CHAR_WIDTH/2, CHAR_HEIGHT/2));
    s.addShape(createDiagonalCylinder(CHAR_WIDTH/2, CHAR_HEIGHT/2, CHAR_WIDTH*3/4, 0));
    s.addShape(createDiagonalCylinder(CHAR_WIDTH*3/4, 0, CHAR_WIDTH, CHAR_HEIGHT));
    return s;
  }
  
  private static EMShape createLetterX() {
    CompositeShape s = new CompositeShape();
    s.addShape(createDiagonalCylinder(0, CHAR_HEIGHT, CHAR_WIDTH, 0));
    s.addShape(createDiagonalCylinder(0, 0, CHAR_WIDTH, CHAR_HEIGHT));
    return s;
  }
  
  private static EMShape createLetterY() {
    CompositeShape s = new CompositeShape();
    s.addShape(createDiagonalCylinder(0, CHAR_HEIGHT, CHAR_WIDTH/2, CHAR_HEIGHT/2));
    s.addShape(createDiagonalCylinder(CHAR_WIDTH/2, CHAR_HEIGHT/2, CHAR_WIDTH, CHAR_HEIGHT));
    s.addShape(createVerticalBar(CHAR_WIDTH/2-STROKE_WIDTH/2, 0, STROKE_WIDTH, CHAR_HEIGHT/2));
    return s;
  }
  
  private static EMShape createLetterZ() {
    CompositeShape s = new CompositeShape();
    s.addShape(createHorizontalBar(0, CHAR_HEIGHT-STROKE_WIDTH, CHAR_WIDTH, STROKE_WIDTH));
    s.addShape(createDiagonalCylinder(0, CHAR_HEIGHT-STROKE_WIDTH, CHAR_WIDTH, STROKE_WIDTH));
    s.addShape(createHorizontalBar(0, 0, CHAR_WIDTH, STROKE_WIDTH));
    return s;
  }
  
  // ========== RAKAM OLUsTURMA METODLARI ========== //
  private static EMShape createNumber0() {
    CompositeShape s = new CompositeShape();
    // Dış halka
    s.addShape(createCircle(CHAR_WIDTH/2, CHAR_HEIGHT/2, Math.min(CHAR_WIDTH, CHAR_HEIGHT)/2 - STROKE_WIDTH/2));
    // İç boşluk (isteğe bağlı, daha çok Rectangle3D ile olurdu)
    // Eğer iç boşluk için bir kesme işlemi yapmak isterseniz Crescent kullanabilirsiniz.
    return s;
  }
  
  private static EMShape createNumber1() {
    return createVerticalBar(CHAR_WIDTH/2-STROKE_WIDTH/2, 0, STROKE_WIDTH, CHAR_HEIGHT);
  }
  
  private static EMShape createNumber2() {
    CompositeShape s = new CompositeShape();
    s.addShape(createArc(CHAR_WIDTH/2, CHAR_HEIGHT*0.75f, (CHAR_WIDTH-STROKE_WIDTH)/2, 0, 180));
    s.addShape(createHorizontalBar(0, CHAR_HEIGHT/2, CHAR_WIDTH, STROKE_WIDTH));
    s.addShape(createDiagonalCylinder(0, CHAR_HEIGHT/2, CHAR_WIDTH-STROKE_WIDTH, STROKE_WIDTH));
    s.addShape(createHorizontalBar(0, STROKE_WIDTH, CHAR_WIDTH, STROKE_WIDTH));
    return s;
  }
  
  private static EMShape createNumber3() {
    CompositeShape s = new CompositeShape();
    s.addShape(createArc(CHAR_WIDTH/2, CHAR_HEIGHT*0.75f, (CHAR_WIDTH-STROKE_WIDTH)/2, 0, 180));
    s.addShape(createArc(CHAR_WIDTH/2, CHAR_HEIGHT*0.25f, (CHAR_WIDTH-STROKE_WIDTH)/2, 0, 180));
    return s;
  }
  
  private static EMShape createNumber4() {
    CompositeShape s = new CompositeShape();
    s.addShape(createVerticalBar(CHAR_WIDTH-STROKE_WIDTH, 0, STROKE_WIDTH, CHAR_HEIGHT));
    s.addShape(createHorizontalBar(0, CHAR_HEIGHT/2, CHAR_WIDTH, STROKE_WIDTH));
    s.addShape(createDiagonalCylinder(0, CHAR_HEIGHT, CHAR_WIDTH-STROKE_WIDTH, STROKE_WIDTH));
    return s;
  }
  
  private static EMShape createNumber5() {
    CompositeShape s = new CompositeShape();
    s.addShape(createHorizontalBar(0, CHAR_HEIGHT-STROKE_WIDTH, CHAR_WIDTH, STROKE_WIDTH));
    s.addShape(createVerticalBar(0, CHAR_HEIGHT/2, STROKE_WIDTH, CHAR_HEIGHT/2));
    s.addShape(createArc(CHAR_WIDTH/2, CHAR_HEIGHT*0.25f, (CHAR_WIDTH-STROKE_WIDTH)/2, 0, 180));
    s.addShape(createHorizontalBar(CHAR_WIDTH/2, CHAR_HEIGHT/2, CHAR_WIDTH/2, STROKE_WIDTH));
    return s;
  }
  
  private static EMShape createNumber6() {
    CompositeShape s = new CompositeShape();
    s.addShape(createCircle(CHAR_WIDTH/2, CHAR_HEIGHT/2, (CHAR_WIDTH-STROKE_WIDTH)/2));
    s.addShape(createVerticalBar(0, CHAR_HEIGHT/2, STROKE_WIDTH, CHAR_HEIGHT/2));
    return s;
  }
  
  private static EMShape createNumber7() {
    CompositeShape s = new CompositeShape();
    s.addShape(createHorizontalBar(0, CHAR_HEIGHT-STROKE_WIDTH, CHAR_WIDTH, STROKE_WIDTH));
    s.addShape(createDiagonalCylinder(0, CHAR_HEIGHT-STROKE_WIDTH, CHAR_WIDTH, 0));
    return s;
  }
  
  private static EMShape createNumber8() {
    CompositeShape s = new CompositeShape();
    s.addShape(createCircle(CHAR_WIDTH/2, CHAR_HEIGHT*0.75f, (CHAR_WIDTH-STROKE_WIDTH)/2));
    s.addShape(createCircle(CHAR_WIDTH/2, CHAR_HEIGHT*0.25f, (CHAR_WIDTH-STROKE_WIDTH)/2));
    return s;
  }
  
  private static EMShape createNumber9() {
    CompositeShape s = new CompositeShape();
    s.addShape(createCircle(CHAR_WIDTH/2, CHAR_HEIGHT*0.75f, (CHAR_WIDTH-STROKE_WIDTH)/2));
    s.addShape(createVerticalBar(CHAR_WIDTH-STROKE_WIDTH, CHAR_HEIGHT/2, STROKE_WIDTH, CHAR_HEIGHT/2));
    s.addShape(createHorizontalBar(CHAR_WIDTH/2, CHAR_HEIGHT/2, CHAR_WIDTH/2, STROKE_WIDTH));
    return s;
  }
  
  // ========== TEMEL sekil OLUsTURUCULAR (Cylinder kullanır) ========== //
  
  private static EMShape createVerticalBar(float x, float y, float width, float height) {
    float radius = width / 2;
    Cylinder cylinder = new Cylinder(radius, height);
    Matrix4 transform = Matrix4.translate(new Vector3(x + radius, y + height / 2, 0));
    cylinder.setTransform(transform);
    return cylinder;
  }
  
  private static EMShape createHorizontalBar(float x, float y, float width, float height) {
    float radius = height / 2;
    Cylinder cylinder = new Cylinder(radius, width);
    Matrix4 transform = Matrix4.translate(new Vector3(x + width / 2, y + radius, 0))
    .multiply(Matrix4.rotateZ(90.0));
    cylinder.setTransform(transform);
    return cylinder;
  }
  
  private static EMShape createDiagonalCylinder(float x1, float y1, float x2, float y2) {
    float dx = x2 - x1;
    float dy = y2 - y1;
    float length = (float)Math.sqrt(dx * dx + dy * dy);
    float angle = (float)Math.toDegrees(Math.atan2(dy, dx));
    
    Cylinder cylinder = new Cylinder(STROKE_WIDTH / 2, length);
    Matrix4 transform = Matrix4.translate(new Vector3((x1 + x2) / 2, (y1 + y2) / 2, 0))
    .multiply(Matrix4.rotateZ(angle + 90.0f));
    cylinder.setTransform(transform);
    return cylinder;
  }
  
  // Çizgi segmenti oluşturur (createDiagonalCylinder ile aynı mantık, isim tutarlılığı için)
  private static EMShape createLineSegment(float x1, float y1, float x2, float y2) {
    return createDiagonalCylinder(x1, y1, x2, y2);
  }
  
  // Dairesel bir şekil oluşturur (silindir segmentlerinden oluşur)
  private static EMShape createCircle(float cx, float cy, float radius) {
    CompositeShape s = new CompositeShape();
    int segments = 36; // Daha pürüzsüz daire için segment sayısı
    for (int i = 0; i < segments; i++) {
      double angle1 = 2 * Math.PI * i / segments;
      double angle2 = 2 * Math.PI * (i + 1) / segments;
      
      float x1 = cx + radius * (float)Math.cos(angle1);
      float y1 = cy + radius * (float)Math.sin(angle1);
      float x2 = cx + radius * (float)Math.cos(angle2);
      float y2 = cy + radius * (float)Math.sin(angle2);
      
      s.addShape(createLineSegment(x1, y1, x2, y2));
    }
    return s;
  }
  
  // Yarım daire şekli oluşturur (silindir segmentlerinden oluşur)
  private static EMShape createHalfCircle(float cx, float cy, float radius, boolean lowerHalf) {
    CompositeShape s = new CompositeShape();
    int segments = 18;
    float startAngleDeg = lowerHalf ? 180 : 0;
    float endAngleDeg = lowerHalf ? 360 : 180;
    
    for (int i = 0; i < segments; i++) {
      double angle1 = Math.toRadians(startAngleDeg + i * (endAngleDeg - startAngleDeg) / segments);
      double angle2 = Math.toRadians(startAngleDeg + (i + 1) * (endAngleDeg - startAngleDeg) / segments);
      
      float x1 = cx + radius * (float)Math.cos(angle1);
      float y1 = cy + radius * (float)Math.sin(angle1);
      float x2 = cx + radius * (float)Math.cos(angle2);
      float y2 = cy + radius * (float)Math.sin(angle2);
      
      s.addShape(createLineSegment(x1, y1, x2, y2));
    }
    return s;
  }
  
  // Yay şekli oluşturur (silindir segmentlerinden oluşur)
  private static EMShape createArc(float cx, float cy, float radius, float startAngle, float endAngle) {
    CompositeShape s = new CompositeShape();
    int segments = (int)Math.ceil(Math.abs(endAngle - startAngle) / 10); // 10 derece segmentler
    if (segments == 0) segments = 1; // Çok küçük yaylar için en az bir segment
      
    for (int i = 0; i < segments; i++) {
      double angle1 = Math.toRadians(startAngle + i * (endAngle - startAngle) / segments);
      double angle2 = Math.toRadians(startAngle + (i + 1) * (endAngle - startAngle) / segments);
      
      float x1 = cx + radius * (float)Math.cos(angle1);
      float y1 = cy + radius * (float)Math.sin(angle1);
      float x2 = cx + radius * (float)Math.cos(angle2);
      float y2 = cy + radius * (float)Math.sin(angle2);
      
      s.addShape(createLineSegment(x1, y1, x2, y2));
    }
    return s;
  }
}
