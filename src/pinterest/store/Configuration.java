package pinterest.store;

import java.io.IOException;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import pinterest.util.Utils;

/** Constants and icons used by the standalone Pinterest MIDlet. */
public class Configuration {
  public static final int ALERT_TIMEOUT = 2000;

  public static final String STORAGE_SETTINGS = "pinterest.settings";

  public static final String DEFAULT_PINTEREST_API_URL = "http://opentty.fun/pinterest.php";
  public static final String DEFAULT_PINTEREST_IMAGE_PROXY = "http://opentty.fun/proxy.php?url=";
  public static final int DEFAULT_PINTEREST_PINS_PER_PAGE = 20;
  public static final int PINTEREST_PINS_PER_PAGE_MIN = 1;
  public static final int PINTEREST_PINS_PER_PAGE_MAX = 25;

  /** Where downloaded photos are saved on the memory card. */
  public static final String DEFAULT_PINTEREST_PATH = "file:///SDCard/Pinterest/";

  public static final int BLACKBERRY_WIFI_OFF = 0;
  public static final int BLACKBERRY_WIFI_ON = 1;

  public static final int THUMBNAILS_OFF = 0;
  public static final int THUMBNAILS_ON = 1;

  public static Image pinterestIcon;
  public static Image folderIcon;
  public static Image folderBadgeIcon;
  public static Image musicBadgeIcon;

  private static final int ICON_CELL = 42;
  private static final int ICON_COLS = 4;
  private static final int BADGE_SIZE = 24;
  private static Image iconSheet;

  public static void loadIcons() throws IOException {
    // res/icons.png is the shared MIDPlay icon sheet; only the folder glyph is used.
    iconSheet = Image.createImage("/icons.png");
    pinterestIcon = Utils.resizeImageToFit(Image.createImage("/pinterest.png"), 36, 36);
    folderIcon = region(0, 42, 40);
    folderBadgeIcon = Utils.resizeImageToFit(folderIcon, BADGE_SIZE, BADGE_SIZE);
    musicBadgeIcon = folderBadgeIcon;
  }

  /** Fallback used when the resource sheet is missing so the MIDlet still starts. */
  public static void loadFallbackIcons() {
    try {
      pinterestIcon = Utils.resizeImageToFit(Image.createImage("/pinterest.png"), 36, 36);
    } catch (Throwable t) {
      pinterestIcon = null;
    }
    try {
      Image image = Image.createImage(36, 36);
      Graphics g = image.getGraphics();
      g.setColor(0xE60023);
      g.fillRoundRect(2, 8, 32, 22, 5, 5);
      g.setColor(0xFFD6DC);
      g.fillRect(6, 13, 24, 3);
      g.fillRect(6, 20, 18, 3);
      folderIcon = image;
    } catch (Throwable t) {
      folderIcon = null;
    }
    folderBadgeIcon = folderIcon;
    musicBadgeIcon = folderIcon;
  }

  private static Image region(int index, int w, int h) {
    int x = (index % ICON_COLS) * ICON_CELL;
    int y = (index / ICON_COLS) * ICON_CELL;
    return Image.createImage(iconSheet, x, y, w, h, 0);
  }

  private Configuration() {}
}
