package pinterest.ui;

import java.util.Hashtable;
import javax.microedition.lcdui.Command;
import pinterest.util.Lang;

/** Cached, localized commands shared by every screen. */
public class Commands {
  private static final Hashtable cache = new Hashtable();

  private static Command get(String key, int type, int priority) {
    String cacheKey = key + "|" + type + "|" + priority;
    if (cache.containsKey(cacheKey)) {
      return (Command) cache.get(cacheKey);
    }
    Command cmd = new Command(Lang.tr(key), type, priority);
    cache.put(cacheKey, cmd);
    return cmd;
  }

  public static void refresh() {
    cache.clear();
  }

  public static Command ok() {
    return get("action.ok", Command.OK, 0);
  }

  public static Command cancel() {
    return get("action.cancel", Command.CANCEL, 1);
  }

  public static Command back() {
    return get("action.back", Command.BACK, 1);
  }

  public static Command exit() {
    return get("action.exit", Command.EXIT, 1);
  }

  public static Command formSave() {
    return get("action.save", Command.SCREEN, 1);
  }

  private Commands() {}
}
