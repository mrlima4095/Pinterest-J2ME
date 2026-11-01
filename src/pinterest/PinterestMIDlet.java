package pinterest;

import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Display;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;
import pinterest.net.NetworkOperation;
import pinterest.store.Configuration;
import pinterest.store.JsonRecordStore;
import pinterest.store.SettingsManager;
import pinterest.ui.Navigator;
import pinterest.ui.screen.PinterestScreen;
import pinterest.util.Lang;

/** Standalone Pinterest MIDlet (CLDC 1.1 / MIDP 2.0). */
public class PinterestMIDlet extends MIDlet {
  public static final String APP_VERSION = "1.0.0";

  private static PinterestMIDlet instance;
  private static NetworkOperation operation;

  private Navigator navigator;
  private boolean started;

  public PinterestMIDlet() {
    instance = this;
  }

  public static PinterestMIDlet getInstance() {
    return instance;
  }

  protected void startApp() throws MIDletStateChangeException {
    if (!started) {
      started = true;
      try {
        Configuration.loadIcons();
      } catch (Throwable t) {
        Configuration.loadFallbackIcons();
      }
      SettingsManager.getInstance().loadSettings();
      Lang.setLang(SettingsManager.getInstance().getCurrentLanguage());
      navigator = new Navigator(this);
      navigator.setRoot(new PinterestScreen(navigator));
    } else {
      Display.getDisplay(this).setCurrent(navigator.getCurrent());
    }
  }

  protected void pauseApp() {
    // Nothing to suspend: network operations either finish or are cancelled by the user.
  }

  protected void destroyApp(boolean unconditional) throws MIDletStateChangeException {
    cancelOperation();
    try {
      JsonRecordStore.flushAll();
    } catch (Throwable t) {
    }
  }

  /** Stops the previous network operation and starts a new one. */
  public static synchronized void startOperation(NetworkOperation op) {
    if (operation != null) {
      operation.stop();
    }
    operation = op;
    if (operation != null) {
      operation.start();
    }
  }

  /** Stops the running network operation, if any. */
  public static synchronized void cancelOperation() {
    if (operation != null) {
      operation.stop();
      operation = null;
    }
  }

  public void exitApp() {
    try {
      destroyApp(true);
    } catch (Exception e) {
    }
    notifyDestroyed();
  }

  /** Shows a short error on top of the current screen. */
  public void showError(String message) {
    if (navigator != null) {
      navigator.showAlert(message, AlertType.ERROR);
    }
  }
}
