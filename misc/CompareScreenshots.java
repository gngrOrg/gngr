import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public final class CompareScreenshots {
  public static void main(final String[] args) throws Exception {
    if (args.length < 2) {
      System.err.println("Usage: java CompareScreenshots.java <test.png> <ref.png> [diff_output.png]");
      System.exit(1);
    }
    final File fileTest = new File(args[0]);
    final File fileRef = new File(args[1]);

    final BufferedImage imgTest = ImageIO.read(fileTest);
    final BufferedImage imgRef = ImageIO.read(fileRef);

    if (imgTest == null || imgRef == null) {
      System.err.println("Error reading images");
      System.exit(1);
    }

    final int w = imgTest.getWidth();
    final int h = imgTest.getHeight();

    if (w != imgRef.getWidth() || h != imgRef.getHeight()) {
      System.out.printf("DIMENSION MISMATCH: test=%dx%d, ref=%dx%d%n",
          w, h, imgRef.getWidth(), imgRef.getHeight());
      System.exit(1);
    }

    final BufferedImage diffImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    int diffCount = 0;
    final int total = w * h;

    for (int y = 0; y < h; y++) {
      for (int x = 0; x < w; x++) {
        final int cTest = imgTest.getRGB(x, y);
        final int cRef = imgRef.getRGB(x, y);

        if (cTest != cRef) {
          diffCount++;
          // Highlight diff in red
          diffImg.setRGB(x, y, 0xFFFF0000);
        } else {
          // Dim matching pixels
          final int r = ((cTest >> 16) & 0xFF) / 4;
          final int g = ((cTest >> 8) & 0xFF) / 4;
          final int b = (cTest & 0xFF) / 4;
          diffImg.setRGB(x, y, 0xFF000000 | (r << 16) | (g << 8) | b);
        }
      }
    }

    final double diffPct = (diffCount * 100.0) / total;
    System.out.printf("Comparison results: %d / %d differing pixels (%.4f%%)%n",
        diffCount, total, diffPct);

    if (args.length >= 3 && diffCount > 0) {
      ImageIO.write(diffImg, "png", new File(args[2]));
      System.out.println("Diff image saved to " + args[2]);
    }

    if (diffCount == 0) {
      System.out.println("MATCH: Exact 100% pixel match!");
    } else {
      System.out.printf("MISMATCH: %d differing pixels%n", diffCount);
    }
  }
}
