import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * Minimal standalone client for gngr's local Grinder test interface.
 */
public final class GrinderTestClient {
  private static final int CONNECT_TIMEOUT_MS = 5000;
  private static final int READ_TIMEOUT_MS = 120000;
  private static final int MAX_SCREENSHOT_BYTES = 64 * 1024 * 1024;

  private GrinderTestClient() {
  }

  public static void main(final String[] args) throws Exception {
    if (args.length != 3 && args.length != 5) {
      usage();
      System.exit(2);
    }

    final String key = System.getenv("GRINDER_KEY");
    if (key == null || key.isEmpty()) {
      throw new IllegalArgumentException("Set GRINDER_KEY to the key used to launch gngr.");
    }

    final Path profile = Paths.get(args[0]);
    final String url = args[1];
    final Path screenshot = Paths.get(args[2]);
    final int width = args.length == 5 ? positiveInt(args[3], "width") : -1;
    final int height = args.length == 5 ? positiveInt(args[4], "height") : -1;

    final int reusePort = readPort(profile.resolve("port.dat"));
    final int grinderPort = startGrinder(reusePort, key);
    if (width > 0) {
      sendAcknowledgedCommand(grinderPort, "SET_SIZE " + width + " " + height);
    }
    sendAcknowledgedCommand(grinderPort, "TO " + url);
    final byte[] png = screenshot(grinderPort);
    Files.write(screenshot, png);
    System.out.println("Saved " + png.length + " screenshot bytes to " + screenshot);
    final Socket quitSocket = sendCommand(grinderPort, "QUIT");
    quitSocket.close();
  }

  private static void usage() {
    System.err.println("Usage: GRINDER_KEY=<key> java GrinderTestClient "
        + "<profile-dir> <url> <screenshot.png> [width height]");
    System.err.println("Example profile-dir: ~/.gngr/default");
  }

  private static int positiveInt(final String value, final String name) {
    final int result = Integer.parseInt(value);
    if (result <= 0) {
      throw new IllegalArgumentException(name + " must be positive.");
    }
    return result;
  }

  private static int readPort(final Path portFile) throws IOException {
    if (!Files.isRegularFile(portFile)) {
      throw new IOException("Cannot find gngr port file: " + portFile
          + " (start gngr with the same profile first).");
    }
    try (DataInputStream in = new DataInputStream(Files.newInputStream(portFile))) {
      final int port = in.readInt();
      if (port < 1 || port > 65535 || in.read() != -1) {
        throw new IOException("Invalid gngr port file: " + portFile);
      }
      return port;
    }
  }

  private static int startGrinder(final int reusePort, final String key) throws Exception {
    final MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
    final String passkey = Base64.getEncoder().encodeToString(
        sha256.digest((key + reusePort).getBytes(StandardCharsets.UTF_8)));

    try (Socket socket = connect(reusePort);
        DataInputStream in = new DataInputStream(socket.getInputStream());
        DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {
      out.write(("GRINDER " + passkey + "\r\n").getBytes(StandardCharsets.UTF_8));
      out.flush();
      final int grinderPort = in.readInt();
      if (grinderPort < 1 || grinderPort > 65535) {
        throw new IOException("gngr rejected the Grinder key or did not return a valid port.");
      }
      out.write("\r\n".getBytes(StandardCharsets.UTF_8));
      out.flush();
      return grinderPort;
    }
  }

  private static void sendAcknowledgedCommand(final int port, final String command) throws IOException {
    try (Socket socket = sendCommand(port, command);
        DataInputStream in = new DataInputStream(socket.getInputStream())) {
      final int status = in.readInt();
      if (status != 0) {
        throw new IOException("Grinder command failed with status " + status + ": " + command);
      }
      acknowledge(socket);
    }
  }

  private static byte[] screenshot(final int port) throws IOException {
    try (Socket socket = sendCommand(port, "SCREENSHOT");
        DataInputStream in = new DataInputStream(socket.getInputStream())) {
      final int length = in.readInt();
      if (length <= 0 || length > MAX_SCREENSHOT_BYTES) {
        throw new IOException("Invalid screenshot size returned by Grinder: " + length);
      }
      final byte[] png = new byte[length];
      in.readFully(png);
      acknowledge(socket);
      return png;
    }
  }

  private static Socket sendCommand(final int port, final String command) throws IOException {
    final Socket socket = connect(port);
    try {
      socket.getOutputStream().write((command + "\r\n").getBytes(StandardCharsets.UTF_8));
      socket.getOutputStream().flush();
      return socket;
    } catch (IOException e) {
      socket.close();
      throw e;
    }
  }

  private static Socket connect(final int port) throws IOException {
    final Socket socket = new Socket();
    try {
      socket.connect(new InetSocketAddress("127.0.0.1", port), CONNECT_TIMEOUT_MS);
      socket.setSoTimeout(READ_TIMEOUT_MS);
      return socket;
    } catch (IOException e) {
      socket.close();
      throw e;
    }
  }

  private static void acknowledge(final Socket socket) throws IOException {
    socket.getOutputStream().write("\r\n".getBytes(StandardCharsets.UTF_8));
    socket.getOutputStream().flush();
  }
}
