import javax.swing.*;
import java.awt.*;

public class EMEmojiWithShadow extends JPanel {
  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2 = (Graphics2D) g;
    
    // Antialiasing açalım ki yazılar pürüzsüz olsun
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    
    // Gölge rengi (yarı şeffaf siyah)
    Color shadowColor = new Color(0, 0, 0, 100);
    
    // Altın renk tonları
    Color gold1 = new Color(255, 215, 0);
    Color gold2 = new Color(255, 223, 70);
    GradientPaint gradient = new GradientPaint(10, 10, gold1, 50, 50, gold2, true);
    
    g2.setFont(new Font("Serif", Font.BOLD, 72));
    
    // Gölgeyi biraz sağa ve aşağı kaydırıyoruz (offset 3,3)
    g2.setColor(shadowColor);
    g2.drawString("E", 13, 63);
    g2.drawString("M", 33, 83);
    
    // Üstüne asıl altın renkli harfleri çiziyoruz
    g2.setPaint(gradient);
    g2.drawString("E", 10, 60);
    g2.drawString("M", 30, 80);
  }
  
  public static void main(String[] args) {
    JFrame frame = new JFrame("E ve M Emoji with Shadow");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(100, 100);
    frame.add(new EMEmojiWithShadow());
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }
}

/**
g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
 */
