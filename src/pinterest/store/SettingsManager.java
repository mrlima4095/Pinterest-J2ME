package pinterest.store;

import cc.nnproject.json.JSON;
import cc.nnproject.json.JSONException;
import cc.nnproject.json.JSONObject;
import javax.microedition.rms.RecordStoreException;
import pinterest.util.Lang;

/** Persistent settings for the standalone Pinterest MIDlet (RMS-backed). */
public class SettingsManager {
  private static final int SETTINGS_ID = 1;
  private static SettingsManager instance;

  private final JsonRecordStore storage;
  private JSONObject cachedSettings;

  private String currentLanguage;
  private int currentThumbnails;
  private String currentPinterestApiUrl;
  private String currentPinterestImageProxy;
  private int currentPinterestPinsPerPage;
  private String currentPinterestPath;

  public static synchronized SettingsManager getInstance() {
    if (instance == null) {
      instance = new SettingsManager();
    }
    return instance;
  }

  private SettingsManager() {
    storage =
        new JsonRecordStore(
            Configuration.STORAGE_SETTINGS, SETTINGS_ID, createDefaultSettings().toString());
  }

  private JSONObject settings() {
    if (cachedSettings == null) {
      cachedSettings = getSettingsJSON();
    }
    return cachedSettings;
  }

  private JSONObject getSettingsJSON() {
    try {
      return JSON.getObject(storage.load());
    } catch (JSONException e) {
    } catch (Exception e) {
    }
    return createDefaultSettings();
  }

  private static JSONObject createDefaultSettings() {
    JSONObject settings = new JSONObject();
    settings.put("language", defaultLanguage());
    settings.put("thumbnails", Configuration.THUMBNAILS_ON);
    settings.put("pinterestApiUrl", Configuration.DEFAULT_PINTEREST_API_URL);
    settings.put("pinterestImageProxy", Configuration.DEFAULT_PINTEREST_IMAGE_PROXY);
    settings.put("pinterestPinsPerPage", Configuration.DEFAULT_PINTEREST_PINS_PER_PAGE);
    settings.put("pinterestPath", Configuration.DEFAULT_PINTEREST_PATH);
    return settings;
  }

  /** Picks a bundled translation from the device locale; English is the fallback. */
  private static String defaultLanguage() {
    String locale = null;
    try {
      locale = System.getProperty("microedition.locale");
    } catch (Throwable t) {
    }
    if (locale == null) {
      return "en";
    }
    locale = locale.toLowerCase();
    String[] codes = {"pt", "pl", "tr", "vi", "he"};
    for (int i = 0; i < codes.length; i++) {
      if (locale.startsWith(codes[i])) {
        return codes[i];
      }
    }
    return "en";
  }

  public void loadSettings() {
    JSONObject settings = settings();
    setCurrentLanguage(settings.getString("language", "en"));
    currentThumbnails = settings.getInt("thumbnails", Configuration.THUMBNAILS_ON);
    currentPinterestApiUrl =
        settings.getString("pinterestApiUrl", Configuration.DEFAULT_PINTEREST_API_URL);
    currentPinterestImageProxy =
        settings.getString("pinterestImageProxy", Configuration.DEFAULT_PINTEREST_IMAGE_PROXY);
    currentPinterestPinsPerPage =
        settings.getInt("pinterestPinsPerPage", Configuration.DEFAULT_PINTEREST_PINS_PER_PAGE);
    currentPinterestPath =
        settings.getString("pinterestPath", Configuration.DEFAULT_PINTEREST_PATH);
  }

  private void persist(JSONObject settings) throws RecordStoreException {
    storage.save(settings.toString());
  }

  private void saveSetting(String key, String value) throws RecordStoreException {
    JSONObject settings = settings();
    if (settings.has(key) && value != null && value.equals(settings.getString(key, ""))) {
      return;
    }
    settings.put(key, value);
    persist(settings);
  }

  private void saveSetting(String key, int value) throws RecordStoreException {
    JSONObject settings = settings();
    if (settings.has(key) && value == settings.getInt(key, value)) {
      return;
    }
    settings.put(key, value);
    persist(settings);
  }

  public String getCurrentLanguage() {
    return currentLanguage;
  }

  public void setCurrentLanguage(String langCode) {
    currentLanguage = langCode;
    Lang.setLang(langCode);
  }

  public void saveLanguage(String langCode) throws RecordStoreException {
    saveSetting("language", langCode);
    setCurrentLanguage(langCode);
  }

  /** BlackBerry devices need the Wi-Fi transport hint; other devices ignore it. */
  public int getCurrentBlackberryWifi() {
    return Configuration.BLACKBERRY_WIFI_ON;
  }

  public int getCurrentThumbnails() {
    return currentThumbnails;
  }

  public void saveThumbnails(int thumbnails) throws RecordStoreException {
    saveSetting("thumbnails", thumbnails);
    currentThumbnails = thumbnails;
  }

  public String getPinterestApiUrl() {
    if (currentPinterestApiUrl == null || currentPinterestApiUrl.length() == 0) {
      currentPinterestApiUrl = Configuration.DEFAULT_PINTEREST_API_URL;
    }
    return currentPinterestApiUrl;
  }

  public void savePinterestApiUrl(String url) throws RecordStoreException {
    String value = url == null ? "" : url.trim();
    if (value.length() == 0) {
      value = Configuration.DEFAULT_PINTEREST_API_URL;
    }
    saveSetting("pinterestApiUrl", value);
    currentPinterestApiUrl = value;
  }

  public String getPinterestImageProxy() {
    if (currentPinterestImageProxy == null || currentPinterestImageProxy.length() == 0) {
      currentPinterestImageProxy = Configuration.DEFAULT_PINTEREST_IMAGE_PROXY;
    }
    return currentPinterestImageProxy;
  }

  public void savePinterestImageProxy(String url) throws RecordStoreException {
    String value = url == null ? "" : url.trim();
    if (value.length() == 0) {
      value = Configuration.DEFAULT_PINTEREST_IMAGE_PROXY;
    }
    saveSetting("pinterestImageProxy", value);
    currentPinterestImageProxy = value;
  }

  public int getPinterestPinsPerPage() {
    if (currentPinterestPinsPerPage < Configuration.PINTEREST_PINS_PER_PAGE_MIN
        || currentPinterestPinsPerPage > Configuration.PINTEREST_PINS_PER_PAGE_MAX) {
      currentPinterestPinsPerPage = Configuration.DEFAULT_PINTEREST_PINS_PER_PAGE;
    }
    return currentPinterestPinsPerPage;
  }

  public void savePinterestPinsPerPage(int count) throws RecordStoreException {
    int value = count;
    if (value < Configuration.PINTEREST_PINS_PER_PAGE_MIN) {
      value = Configuration.PINTEREST_PINS_PER_PAGE_MIN;
    } else if (value > Configuration.PINTEREST_PINS_PER_PAGE_MAX) {
      value = Configuration.PINTEREST_PINS_PER_PAGE_MAX;
    }
    saveSetting("pinterestPinsPerPage", value);
    currentPinterestPinsPerPage = value;
  }

  public String getPinterestPath() {
    if (currentPinterestPath == null || currentPinterestPath.length() == 0) {
      currentPinterestPath = Configuration.DEFAULT_PINTEREST_PATH;
    }
    return currentPinterestPath;
  }

  public void savePinterestPath(String path) throws RecordStoreException {
    String value = path == null ? "" : path.trim();
    if (value.length() == 0) {
      value = Configuration.DEFAULT_PINTEREST_PATH;
    }
    saveSetting("pinterestPath", value);
    currentPinterestPath = value;
  }

  public void flush() {
    JsonRecordStore.flushAll();
  }
}
