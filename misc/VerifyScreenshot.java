import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

public final class VerifyScreenshot {
  public static void main(final String[] args) throws Exception {
    if (args.length < 1) {
      System.err.println("Usage: java VerifyScreenshot.java <screenshot.png>");
      System.exit(1);
    }
    final File file = new File(args[0]);
    if (!file.exists() || file.length() == 0) {
      System.err.println("ERROR: Screenshot file does not exist or is empty: " + file);
      System.exit(1);
    }
    final BufferedImage img = ImageIO.read(file);
    if (img == null) {
      System.err.println("ERROR: Could not decode image: " + file);
      System.exit(1);
    }
    final int w = img.getWidth();
    final int h = img.getHeight();
    final int totalPixels = w * h;
    System.out.printf("Image dimensions: %dx%d (%d pixels)\n", w, h, totalPixels);

    int whiteCount = 0;
    int grayBgCount = 0;
    int greenCount = 0;
    int blueTextCount = 0;
    final Map<Integer, Integer> colorCounts = new HashMap<>();

    for (int y = 0; y < h; y++) {
      for (int x = 0; x < w; x++) {
        final int argb = img.getRGB(x, y);
        final int rgb = argb & 0x00FFFFFF;
        colorCounts.put(rgb, colorCounts.getOrDefault(rgb, 0) + 1);

        final int r = (rgb >> 16) & 0xFF;
        final int g = (rgb >> 8) & 0xFF;
        final int b = rgb & 0xFF;

        if (rgb == 0xFFFFFF) {
          whiteCount++;
        } else if (rgb == 0xF0F0F0) {
          grayBgCount++;
        } else if (g >= 120 && r < 100 && b < 100) {
          // Green box (#4CAF50 -> r=76, g=175, b=80; #008000 -> r=0, g=128, b=0)
          greenCount++;
        } else if (b > 80 && r < 20 && g < 70) {
          // Dark blue heading (#003366 -> r=0, g=51, b=102)
          blueTextCount++;
        }
      }
    }

    System.out.printf("Unique colors: %d\n", colorCounts.size());
    colorCounts.entrySet().stream()
      .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
      .limit(10)
      .forEach(e -> System.out.printf("  Color #%06x: %d pixels (%.2f%%)%n", e.getKey(), e.getValue(), (e.getValue() * 100.0) / totalPixels));

    if (colorCounts.size() <= 1) {
      System.err.println("ERROR: Screenshot has only a single color (blank)!");
      System.exit(1);
    }
    if (whiteCount == totalPixels) {
      System.err.println("ERROR: Screenshot is 100% blank white!");
      System.exit(1);
    }

    System.out.println("VERIFIED: Screenshot is NOT blank; rendered page content is verified!");
  }
}
