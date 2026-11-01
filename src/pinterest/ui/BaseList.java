package pinterest.ui;

import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.List;
import pinterest.net.PinterestThumbnailOperation;
import pinterest.store.Configuration;
import pinterest.store.SettingsManager;
import pinterest.util.Utils;

/** Base for the Pinterest result/favorites lists, including lazy thumbnail loading. */
public abstract class BaseList extends List implements CommandListener {
  protected final Navigator navigator;

  private static final int ART_CELL = 36;

  private PinterestThumbnailOperation artOp;
  private int artKey = 0;
  private boolean artInFlight = false;
  private boolean artDone = false;
  private String[] artUrls;

  public BaseList(String title, Navigator navigator) {
    this(title, navigator, List.IMPLICIT);
  }

  protected BaseList(String title, Navigator navigator, int listType) {
    super(title, listType);
    this.navigator = navigator;
    setupCommands();
    setCommandListener(this);
  }

  private void setupCommands() {
    addCommand(Commands.back());
  }

  public void commandAction(Command c, Displayable d) {
    try {
      if (c == List.SELECT_COMMAND) {
        handleSelection();
      } else if (c == Commands.back()) {
        navigator.back();
      } else {
        handleCommand(c, d);
      }
    } catch (Exception e) {
      navigator.showAlert(e.toString(), AlertType.ERROR);
    }
  }

  protected abstract void populateItems();

  protected static boolean isValidSelection(int index, int size) {
    return index >= 0 && index < size;
  }

  protected void refresh() {
    int index = getSelectedIndex();
    this.deleteAll();
    populateItems();
    Utils.clampAndSelect(this, index);
  }

  /** Loads (or reloads) the thumbnails for every row; empty entries are skipped. */
  protected void loadArt(String[] urls) {
    cancelArt();
    if (SettingsManager.getInstance().getCurrentThumbnails() == Configuration.THUMBNAILS_OFF) {
      artUrls = null;
      artDone = true;
      return;
    }
    artUrls = urls;
    artDone = false;
    if (urls == null || urls.length == 0) {
      artDone = true;
      return;
    }
    String[] tmpUrls = new String[urls.length];
    int[] tmpIdx = new int[urls.length];
    int n = 0;
    for (int i = 0; i < urls.length; i++) {
      String u = urls[i];
      if (u != null && u.length() > 0) {
        tmpUrls[n] = u;
        tmpIdx[n] = i;
        n++;
      }
    }
    if (n == 0) {
      artDone = true;
      return;
    }
    final String[] reqUrls = new String[n];
    final int[] indices = new int[n];
    System.arraycopy(tmpUrls, 0, reqUrls, 0, n);
    System.arraycopy(tmpIdx, 0, indices, 0, n);
    artInFlight = true;
    fetchArt(reqUrls, indices, true);
  }

  /** Appends thumbnails for rows added later (pagination) without resetting the list. */
  protected void loadArtMore(int offset, String[] urls) {
    if (SettingsManager.getInstance().getCurrentThumbnails() == Configuration.THUMBNAILS_OFF) {
      return;
    }
    if (urls == null || urls.length == 0) {
      return;
    }
    String[] tmpUrls = new String[urls.length];
    int[] tmpIdx = new int[urls.length];
    int n = 0;
    for (int i = 0; i < urls.length; i++) {
      String u = urls[i];
      if (u != null && u.length() > 0) {
        tmpUrls[n] = u;
        tmpIdx[n] = offset + i;
        n++;
      }
    }
    if (n == 0) {
      return;
    }
    final String[] reqUrls = new String[n];
    final int[] indices = new int[n];
    System.arraycopy(tmpUrls, 0, reqUrls, 0, n);
    System.arraycopy(tmpIdx, 0, indices, 0, n);
    fetchArt(reqUrls, indices, false);
  }

  private void fetchArt(
      final String[] reqUrls, final int[] indices, final boolean trackCompletion) {
    if (artOp != null) {
      artOp.stop();
      artOp = null;
    }
    final int capturedKey = artKey;
    artOp =
        new PinterestThumbnailOperation(
            reqUrls,
            ART_CELL,
            new PinterestThumbnailOperation.Listener() {
              public void onImage(final int index, final Image image) {
                navigator.callSerially(
                    new Runnable() {
                      public void run() {
                        if (capturedKey != artKey) {
                          return;
                        }
                        int row = index >= 0 && index < indices.length ? indices[index] : -1;
                        if (row >= 0 && row < size() && image != null) {
                          set(row, getString(row), image);
                        }
                      }
                    });
              }

              public void onDone() {
                navigator.callSerially(
                    new Runnable() {
                      public void run() {
                        if (capturedKey != artKey) {
                          return;
                        }
                        if (trackCompletion) {
                          artInFlight = false;
                          artDone = true;
                        }
                      }
                    });
              }

              public void onError(Exception e) {
                navigator.callSerially(
                    new Runnable() {
                      public void run() {
                        if (capturedKey != artKey) {
                          return;
                        }
                        if (trackCompletion) {
                          artInFlight = false;
                          artDone = true;
                        }
                      }
                    });
              }
            });
    artOp.start();
  }

  protected void cancelArt() {
    if (artOp != null) {
      artOp.stop();
      artOp = null;
    }
    artInFlight = false;
    artKey++;
  }

  protected void showNotify() {
    if (artUrls != null && !artDone && !artInFlight) {
      loadArt(artUrls);
    }
  }

  protected void hideNotify() {
    cancelArt();
  }

  protected abstract void handleSelection();

  protected abstract void handleCommand(Command c, Displayable d);
}
