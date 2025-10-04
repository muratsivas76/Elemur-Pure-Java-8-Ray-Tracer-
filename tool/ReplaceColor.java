import java.io.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class ReplaceColor
{
  
  private static BufferedImage replaceColor(
    BufferedImage srcBuf,
    int cSearch,
    int cReplace) {
    int width = srcBuf.getWidth();
    int height = srcBuf.getHeight();
    
    // TYPE_INT_ARGB
    BufferedImage dstBuf = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        int pixel = srcBuf.getRGB(x, y);
        
        // Only RGB mix
        if ((pixel & 0x00FFFFFF) == (cSearch & 0x00FFFFFF)) {
          // Protect alpha, change rgb!
          int originalAlpha = (pixel >> 24) & 0xFF;
          int newColor = (originalAlpha << 24) | (cReplace & 0x00FFFFFF);
          dstBuf.setRGB(x, y, newColor);
          } else {
          dstBuf.setRGB(x, y, pixel);
        }
      }
    }
    
    return dstBuf;
  }
  
  public static void main (String [] args) {
    if (args.length < 4) {
      System.out.println ("Example from white to transparent black:\n\tjava ReplaceColor src.png dst.png ffffff 00000000");
      System.exit (-1);
    }
    
    try {
      BufferedImage source = ImageIO.read (new File (args [0]));
      
      File dest = new File (args [1]);
      
      int from = Integer.parseInt (args [2], 0x10);
      int to = Integer.parseInt (args [3], 0x10);
      
      BufferedImage hdf = replaceColor (source, from, to);
      ImageIO.write (hdf, "PNG", dest);
      
      System.out.println ("\nWrote: "+dest.getName ());
      } catch (IOException ioe) {
      ioe.printStackTrace ();
      System.exit (-1);
      } catch (NumberFormatException nfe) {
      nfe.printStackTrace ();
      System.exit (-1);
    }
    
    return;
  }
  
}
