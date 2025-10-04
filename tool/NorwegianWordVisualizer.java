import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class NorwegianWordVisualizer extends JFrame {
  private JTextField wordInput;
  private JComboBox<String> fontFamily;
  private JSlider fontSize, bevelSize;
  private JLabel sizeValue, bevelValue;
  private JPanel colorPreviewPanel;
  private Color textColor = Color.BLUE;
  private Color shadowColor = new Color(44, 62, 80);
  private Color bgColor = Color.WHITE;
  private DrawingPanel drawingPanel;
  
  public NorwegianWordVisualizer() {
    setTitle("3D Norwegian Word Visualizer - Java 8");
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setSize(1000, 800);
    setLocationRelativeTo(null);
    
    // Main panel
    JPanel container = new JPanel();
    container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
    container.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    container.setBackground(new Color(26, 42, 108));
    
    // Title
    JLabel title = new JLabel("3D Norwegian Word Visualizer");
    title.setFont(new Font("Segoe UI", Font.BOLD, 28));
    title.setForeground(new Color(253, 187, 45));
    title.setAlignmentX(Component.CENTER_ALIGNMENT);
    
    // Java 8 reference
    JLabel javaReference = new JLabel("With respect to the power of Java 8... - In the spirit of Sun Microsystems");
    javaReference.setFont(new Font("Segoe UI", Font.ITALIC, 14));
    javaReference.setForeground(new Color(52, 152, 219));
    javaReference.setAlignmentX(Component.CENTER_ALIGNMENT);
    
    // Word input
    JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    inputPanel.setOpaque(false);
    JLabel wordLabel = new JLabel("Norwegian Word:");
    wordLabel.setForeground(new Color(253, 187, 45));
    wordInput = new JTextField("BRØD", 20);
    wordInput.setFont(new Font("Arial", Font.PLAIN, 16));
    
    inputPanel.add(wordLabel);
    inputPanel.add(wordInput);
    
    // Example words
    JPanel examplePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
    examplePanel.setOpaque(false);
    String[] examples = {"BRØD (Bread)", "OST (Cheese)", "VANN (Water)",
    "KJÆRLIGHET (Love)", "SIVAS (Your City)"};
    
    for (String example : examples) {
      JButton exampleBtn = new JButton(example);
      exampleBtn.setBackground(new Color(253, 187, 45, 50));
      exampleBtn.setOpaque(true);
      exampleBtn.setBorderPainted(false);
      exampleBtn.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            wordInput.setText(example.split(" ")[0]);
            drawingPanel.repaint();
          }
      });
      examplePanel.add(exampleBtn);
    }
    
    // Settings panel
    JPanel settingsPanel = new JPanel(new GridLayout(2, 3, 10, 10));
    settingsPanel.setOpaque(false);
    
    // Font selection
    JPanel fontPanel = new JPanel(new BorderLayout());
    fontPanel.setOpaque(false);
    JLabel fontLabel = new JLabel("Font:");
    fontLabel.setForeground(new Color(253, 187, 45));
    String[] fonts = {"Arial", "Verdana", "Georgia", "Times New Roman", "Courier New", "Impact"};
    fontFamily = new JComboBox(fonts);
    fontPanel.add(fontLabel, BorderLayout.NORTH);
    fontPanel.add(fontFamily, BorderLayout.CENTER);
    
    // Font size
    JPanel sizePanel = new JPanel(new BorderLayout());
    sizePanel.setOpaque(false);
    JLabel sizeLabel = new JLabel("Font Size:");
    sizeLabel.setForeground(new Color(253, 187, 45));
    sizeValue = new JLabel("120");
    sizeValue.setForeground(new Color(52, 152, 219));
    
    JPanel sizeValuePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    sizeValuePanel.setOpaque(false);
    sizeValuePanel.add(sizeLabel);
    sizeValuePanel.add(sizeValue);
    
    fontSize = new JSlider(40, 200, 120);
    fontSize.addChangeListener(new ChangeListener() {
        public void stateChanged(ChangeEvent e) {
          sizeValue.setText(String.valueOf(fontSize.getValue()));
          drawingPanel.repaint();
        }
    });
    
    sizePanel.add(sizeValuePanel, BorderLayout.NORTH);
    sizePanel.add(fontSize, BorderLayout.CENTER);
    
    // 3D Depth
    JPanel bevelPanel = new JPanel(new BorderLayout());
    bevelPanel.setOpaque(false);
    JLabel bevelLabel = new JLabel("3D Depth:");
    bevelLabel.setForeground(new Color(253, 187, 45));
    bevelValue = new JLabel("4");
    bevelValue.setForeground(new Color(52, 152, 219));
    
    JPanel bevelValuePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    bevelValuePanel.setOpaque(false);
    bevelValuePanel.add(bevelLabel);
    bevelValuePanel.add(bevelValue);
    
    bevelSize = new JSlider(1, 10, 4);
    bevelSize.addChangeListener(new ChangeListener() {
        public void stateChanged(ChangeEvent e) {
          bevelValue.setText(String.valueOf(bevelSize.getValue()));
          drawingPanel.repaint();
        }
    });
    
    bevelPanel.add(bevelValuePanel, BorderLayout.NORTH);
    bevelPanel.add(bevelSize, BorderLayout.CENTER);
    
    // Color selections
    JPanel textColorPanel = createColorPanel("Text Color:", textColor, new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          textColor = JColorChooser.showDialog(NorwegianWordVisualizer.this, "Select Text Color", textColor);
          drawingPanel.repaint();
        }
    });
    
    JPanel shadowColorPanel = createColorPanel("Shadow Color:", shadowColor, new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          shadowColor = JColorChooser.showDialog(NorwegianWordVisualizer.this, "Select Shadow Color", shadowColor);
          drawingPanel.repaint();
        }
    });
    
    JPanel bgColorPanel = createColorPanel("Background Color:", bgColor, new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          bgColor = JColorChooser.showDialog(NorwegianWordVisualizer.this, "Select Background Color", bgColor);
          drawingPanel.repaint();
        }
    });
    
    // First row settings
    settingsPanel.add(fontPanel);
    settingsPanel.add(sizePanel);
    settingsPanel.add(bevelPanel);
    
    // Second row settings
    settingsPanel.add(textColorPanel);
    settingsPanel.add(shadowColorPanel);
    settingsPanel.add(bgColorPanel);
    
    // Buttons
    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    buttonPanel.setOpaque(false);
    
    JButton generateBtn = new JButton("Generate Visual");
    generateBtn.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          drawingPanel.repaint();
        }
    });
    
    JButton downloadBtn = new JButton("Download as PNG");
    downloadBtn.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          saveImage();
        }
    });
    
    buttonPanel.add(generateBtn);
    buttonPanel.add(downloadBtn);
    
    // Drawing panel
    drawingPanel = new DrawingPanel();
    drawingPanel.setPreferredSize(new Dimension(800, 400));
    
    // Instructions
    JTextArea instructions = new JTextArea();
    instructions.setEditable(false);
    instructions.setLineWrap(true);
    instructions.setWrapStyleWord(true);
    instructions.setOpaque(false);
    instructions.setForeground(Color.WHITE);
    instructions.setText("User Guide:\n" +
      "1. Type your desired Norwegian word or click on one of the examples\n" +
      "2. Adjust settings like font, size, 3D depth\n" +
      "3. Select text color, shadow color, and background color\n" +
      "4. Click the \"Generate Visual\" button\n" +
      "5. If you like the generated visual, you can download it as a PNG file with transparent background using the \"Download as PNG\" button\n\n" +
    "Note: The downloaded PNG file will have a completely transparent background.");
    
    // Add components
    container.add(title);
    container.add(Box.createVerticalStrut(10));
    container.add(javaReference);
    container.add(Box.createVerticalStrut(20));
    container.add(inputPanel);
    container.add(examplePanel);
    container.add(Box.createVerticalStrut(20));
    container.add(settingsPanel);
    container.add(Box.createVerticalStrut(20));
    container.add(buttonPanel);
    container.add(Box.createVerticalStrut(20));
    container.add(drawingPanel);
    container.add(Box.createVerticalStrut(20));
    container.add(instructions);
    
    add(container);
    setVisible(true);
  }
  
  private JPanel createColorPanel(String label, Color color, ActionListener action) {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setOpaque(false);
    
    JLabel colorLabel = new JLabel(label);
    colorLabel.setForeground(new Color(253, 187, 45));
    
    JButton colorButton = new JButton();
    colorButton.setBackground(color);
    colorButton.setPreferredSize(new Dimension(30, 30));
    colorButton.addActionListener(action);
    
    JPanel colorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    colorPanel.setOpaque(false);
    colorPanel.add(colorLabel);
    colorPanel.add(colorButton);
    
    panel.add(colorPanel, BorderLayout.NORTH);
    return panel;
  }
  
  private void saveImage() {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setDialogTitle("Save Image");
    fileChooser.setSelectedFile(new File("3d-norwegian-" + wordInput.getText().toLowerCase() + ".png"));
    
    int userSelection = fileChooser.showSaveDialog(this);
    if (userSelection == JFileChooser.APPROVE_OPTION) {
      File fileToSave = fileChooser.getSelectedFile();
      try {
        // Create image with transparent background
        BufferedImage image = new BufferedImage(
          drawingPanel.getWidth(),
          drawingPanel.getHeight(),
          BufferedImage.TYPE_INT_ARGB
        );
        
        Graphics2D g2d = image.createGraphics();
        drawingPanel.paintComponent(g2d, true);
        g2d.dispose();
        
        ImageIO.write(image, "PNG", fileToSave);
        JOptionPane.showMessageDialog(this, "Image saved successfully: " + fileToSave.getName());
        } catch (IOException ex) {
        JOptionPane.showMessageDialog(this, "Error saving image: " + ex.getMessage(),
        "Error", JOptionPane.ERROR_MESSAGE);
      }
    }
  }
  
  class DrawingPanel extends JPanel {
    @Override
    protected void paintComponent(Graphics g) {
      super.paintComponent(g);
      Graphics2D g2d = (Graphics2D) g;
      paintComponent(g2d, false);
    }
    
    public void paintComponent(Graphics2D g2d, boolean forSave) {
      // Draw background (transparent for saving, selected color for display)
      if (forSave) {
        g2d.setColor(new Color(0, 0, 0, 0)); // Completely transparent
        } else {
        g2d.setColor(bgColor);
      }
      g2d.fillRect(0, 0, getWidth(), getHeight());
      
      String text = wordInput.getText();
      if (text.isEmpty()) text = "BRØD";
      
      String fontName = (String) fontFamily.getSelectedItem();
      int size = fontSize.getValue();
      int depth = bevelSize.getValue();
      
      // Set font
      Font font = new Font(fontName, Font.BOLD, size);
      g2d.setFont(font);
      
      // Calculate text size for centering
      FontMetrics metrics = g2d.getFontMetrics();
      int x = getWidth() / 2;
      int y = getHeight() / 2 + metrics.getAscent() / 2 - metrics.getDescent();
      
      // Create 3D effect (shadows)
      for (int i = depth; i > 0; i--) {
        float alpha = 0.1f + (i / (float) depth) * 0.3f;
        Color shadowWithAlpha = new Color(
          shadowColor.getRed(),
          shadowColor.getGreen(),
          shadowColor.getBlue(),
          (int) (alpha * 255)
        );
        
        g2d.setColor(shadowWithAlpha);
        g2d.drawString(text, x + i, y + i);
      }
      
      // Draw front surface
      g2d.setColor(textColor);
      g2d.drawString(text, x, y);
      
      // Add a slight highlight effect
      g2d.setColor(new Color(255, 255, 255, 50));
      g2d.drawString(text, x - 2, y - 2);
    }
  }
  
  public static void main(String[] args) {
    SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          new NorwegianWordVisualizer();
        }
    });
  }
}