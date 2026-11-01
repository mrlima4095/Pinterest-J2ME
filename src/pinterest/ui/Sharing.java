package pinterest.ui;

import javax.microedition.lcdui.AlertType;
import pinterest.PinterestMIDlet;
import pinterest.util.Lang;

/**
 * Opens a Pinterest link in the device browser. The standalone MIDlet has no embedded chat or
 * player to share into, so "Share" simply hands the public URL to the platform.
 */
public final class Sharing {
  private Sharing() {}

  public static void open(Navigator navigator, String url) {
    if (url == null || url.length() == 0) {
      return;
    }
    try {
      PinterestMIDlet.getInstance().platformRequest(url);
    } catch (Exception e) {
      navigator.showAlert(Lang.tr("pinterest.browser_error"), AlertType.ERROR);
    }
  }
}
