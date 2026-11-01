package pinterest.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import javax.microedition.io.HttpConnection;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.List;

/** Small CLDC-safe helpers shared by the Pinterest screens and network code. */
public class Utils {
  public static final boolean isBlackberry;
  public static final boolean isSamsung;

  static {
    String platform = System.getProperty("microedition.platform");
    isBlackberry = (platform != null && platform.toLowerCase().startsWith("blackberry"));
    isSamsung =
        platform != null
            && (platform.toLowerCase().indexOf("samsung") != -1
                || platform.toLowerCase().indexOf("sgh") != -1);
  }

  private static final String HEX_DIGITS = "0123456789ABCDEF";

  private static boolean isUrlSafeCharacter(char c) {
    return (c >= 'A' && c <= 'Z')
        || (c >= 'a' && c <= 'z')
        || (c >= '0' && c <= '9')
        || c == '-'
        || c == '_'
        || c == '.'
        || c == '~';
  }

  public static boolean hasClass(String s) {
    try {
      Class.forName(s);
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  public static boolean hasProperty(String propertyName) {
    return System.getProperty(propertyName) != null;
  }

  public static boolean isJ2MELoader() {
    return hasClass("javax.microedition.shell.MicroActivity");
  }

  public static String replace(String text, String search, String replacement) {
    if (text == null || search == null) {
      return text;
    }
    StringBuffer sb = new StringBuffer();
    int pos = 0;
    int found;
    int searchLen = search.length();
    while ((found = text.indexOf(search, pos)) != -1) {
      sb.append(text.substring(pos, found)).append(replacement);
      pos = found + searchLen;
    }
    sb.append(text.substring(pos));
    return sb.toString();
  }

  public static byte[] utf8ToBytes(String s) {
    if (s == null) {
      return new byte[0];
    }
    try {
      return s.getBytes("UTF-8");
    } catch (UnsupportedEncodingException e) {
      return s.getBytes();
    }
  }

  public static String bytesToUtf8(byte[] data) {
    if (data == null || data.length == 0) {
      return "";
    }
    try {
      return new String(data, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      return new String(data);
    }
  }

  public static String urlEncode(String text) {
    if (text == null) {
      return "";
    }
    byte[] bytes = utf8ToBytes(text);
    StringBuffer result = new StringBuffer(bytes.length + (bytes.length >> 1));
    for (int i = 0; i < bytes.length; i++) {
      int b = bytes[i] & 0xFF;
      char c = (char) b;
      if (c == ' ') {
        result.append('+');
      } else if (isUrlSafeCharacter(c)) {
        result.append(c);
      } else {
        result
            .append('%')
            .append(HEX_DIGITS.charAt((b >> 4) & 0xF))
            .append(HEX_DIGITS.charAt(b & 0xF));
      }
    }
    return result.toString();
  }

  public static Image resizeImageToFit(Image src, int targetWidth, int targetHeight) {
    if (src == null) {
      return null;
    }
    int srcWidth = src.getWidth();
    int srcHeight = src.getHeight();
    if (srcWidth == targetWidth && srcHeight == targetHeight) {
      return src;
    }
    double scaleX = (double) targetWidth / srcWidth;
    double scaleY = (double) targetHeight / srcHeight;
    double scale = scaleX > scaleY ? scaleX : scaleY;
    int scaledWidth = (int) (srcWidth * scale);
    int scaledHeight = (int) (srcHeight * scale);
    int offsetX = (scaledWidth - targetWidth) / 2;
    int offsetY = (scaledHeight - targetHeight) / 2;
    int[] dst = new int[targetWidth * targetHeight];
    int[] srcRow = new int[srcWidth];
    int dstIndex = 0;
    for (int dy = 0; dy < targetHeight; dy++) {
      int srcY = ((offsetY + dy) * srcHeight) / scaledHeight;
      src.getRGB(srcRow, 0, srcWidth, 0, srcY, srcWidth, 1);
      for (int dx = 0; dx < targetWidth; dx++) {
        int srcX = ((offsetX + dx) * srcWidth) / scaledWidth;
        dst[dstIndex++] = srcRow[srcX];
      }
    }
    return Image.createRGBImage(dst, targetWidth, targetHeight, true);
  }

  /** Scales an image down/up while preserving its aspect ratio, fitting inside the box. */
  public static Image resizeImageToFitInside(Image src, int maxWidth, int maxHeight) {
    if (src == null) {
      return null;
    }
    int srcWidth = src.getWidth();
    int srcHeight = src.getHeight();
    if (srcWidth <= 0 || srcHeight <= 0 || maxWidth <= 0 || maxHeight <= 0) {
      return src;
    }
    double scaleX = (double) maxWidth / srcWidth;
    double scaleY = (double) maxHeight / srcHeight;
    double scale = scaleX < scaleY ? scaleX : scaleY;
    if (scale >= 1.0) {
      return src;
    }
    int targetWidth = (int) (srcWidth * scale);
    int targetHeight = (int) (srcHeight * scale);
    if (targetWidth < 1) {
      targetWidth = 1;
    }
    if (targetHeight < 1) {
      targetHeight = 1;
    }
    int[] dst = new int[targetWidth * targetHeight];
    int[] srcRow = new int[srcWidth];
    int dstIndex = 0;
    for (int dy = 0; dy < targetHeight; dy++) {
      int srcY = (dy * srcHeight) / targetHeight;
      src.getRGB(srcRow, 0, srcWidth, 0, srcY, srcWidth, 1);
      for (int dx = 0; dx < targetWidth; dx++) {
        int srcX = (dx * srcWidth) / targetWidth;
        dst[dstIndex++] = srcRow[srcX];
      }
    }
    return Image.createRGBImage(dst, targetWidth, targetHeight, true);
  }

  public static void clampAndSelect(List list, int index) {
    int size = list.size();
    if (size <= 0) {
      return;
    }
    if (index < 0) {
      index = 0;
    } else if (index >= size) {
      index = size - 1;
    }
    list.setSelectedIndex(index, true);
  }

  public static void closeQuietly(InputStream in) {
    if (in != null) {
      try {
        in.close();
      } catch (IOException e) {
      }
    }
  }

  public static void closeQuietly(OutputStream os) {
    if (os != null) {
      try {
        os.close();
      } catch (IOException e) {
      }
    }
  }

  public static void closeQuietly(HttpConnection connection) {
    if (connection != null) {
      try {
        connection.close();
      } catch (IOException e) {
      }
    }
  }

  private Utils() {}
}
