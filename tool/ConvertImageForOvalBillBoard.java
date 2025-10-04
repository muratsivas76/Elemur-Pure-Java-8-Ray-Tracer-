import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class ConvertImageForOvalBillBoard {

    public static void main(String[] args) {
        if (args.length < 4 || args.length > 5) {
            System.out.println("Usage: java ConvertImageForOvalBillBoard src.png dst.png leftTopMargin rightBottomMargin [clip]");
            System.out.println("Example: java ConvertImageForOvalBillBoard input.png output.png 100 100 yes");
            System.out.println("Clip options: true, yes, y, e, evet, clip (case-insensitive)");
            return;
        }

        String srcPath = args[0];
        String dstPath = args[1];
        int marginX;
        int marginY;
        boolean useClip = false;

        try {
            marginX = Integer.parseInt(args[2]);
            marginY = Integer.parseInt(args[3]);
            if (marginX < 0 || marginY < 0) {
                System.err.println("Error: Margin values must be non-negative integers.");
                return;
            }
        } catch (NumberFormatException e) {
            System.err.println("Error: Margin values must be integers.");
            return;
        }

        if (args.length == 5) {
            String clipArg = args[4].toLowerCase();
            useClip = clipArg.equals("true") || clipArg.equals("yes") || clipArg.equals("y") ||
                      clipArg.equals("e") || clipArg.equals("evet") || clipArg.equals("clip");
        }

        try {
            BufferedImage srcImage = ImageIO.read(new File(srcPath));
            if (srcImage == null) {
                System.err.println("Error: Could not load image: " + srcPath);
                return;
            }

            int srcWidth = srcImage.getWidth();
            int srcHeight = srcImage.getHeight();
			
            int newWidth = srcWidth + marginX * 2;
            int newHeight = srcHeight + marginY * 2;

            BufferedImage dstImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);

            Graphics2D g2d = dstImage.createGraphics();

            // Enable high-quality rendering
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            // Clear background to fully transparent
            g2d.setComposite(AlphaComposite.Clear);
            g2d.fillRect(0, 0, newWidth, newHeight);

            // Reset composite for drawing
            g2d.setComposite(AlphaComposite.SrcOver);
			
if (useClip) {
    Ellipse2D oval = new Ellipse2D.Double(marginX, marginY, srcWidth, srcHeight);
    g2d.setClip(oval);

System.out.println ("Enter scale double value:");
java.io.BufferedReader br = new java.io.BufferedReader (new java.io.InputStreamReader (System.in));
String line = br.readLine ();
double scale = Double.parseDouble (line);

br.close ();

System.out.println ("Scale: "+scale+"");

    int scaledWidth = (int) (srcWidth * scale);
    int scaledHeight = (int) (srcHeight * scale);

    int offsetX = marginX + (srcWidth - scaledWidth) / 2;
    int offsetY = marginY + (srcHeight - scaledHeight) / 2;

    g2d.drawImage(srcImage, offsetX, offsetY, scaledWidth, scaledHeight, null);
} else {
    g2d.drawImage(srcImage, marginX, marginY, null);
}

            g2d.dispose();

            File outputFile = new File(dstPath);
            String format = "png";
            if (!dstPath.toLowerCase().endsWith(".png")) {
                System.out.println("Warning: Output file does not end with .png, using PNG format anyway.");
            }

            boolean success = ImageIO.write(dstImage, format, outputFile);
            if (success) {
                System.out.println("Success: Created " + dstPath);
                System.out.println("Original size: " + srcWidth + "x" + srcHeight);
                System.out.println("New size: " + newWidth + "x" + newHeight);
                System.out.println("Margins: left/top = " + marginX + "px, right/bottom = " + marginY + "px");
                if (useClip) {
                    System.out.println("Clip: OVAL applied.");
                } else {
                    System.out.println("Clip: None.");
                }
            } else {
                System.err.println("Error: Failed to write output file.");
            }

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } catch (NumberFormatException nfe) {
			System.err.println("Error: " + nfe.getMessage());
            nfe.printStackTrace();
		}
    }

}
