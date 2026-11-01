package pinterest.ui.screen;

import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.TextField;
import javax.microedition.rms.RecordStoreException;
import pinterest.store.Configuration;
import pinterest.store.SettingsManager;
import pinterest.ui.BaseForm;
import pinterest.ui.Commands;
import pinterest.ui.Navigator;
import pinterest.util.Lang;

/** Pinterest-only settings: language, proxy URLs, page size and the photo download folder. */
public final class PinterestSettingsScreen extends BaseForm {
  private static final String[] LANG_CODES = {"en", "pt", "pl", "tr", "vi", "he"};
  private static final String[] LANG_NAMES = {
    "English", "Português", "Polski", "Türkçe", "Tiếng Việt", "עברית"
  };

  private final SettingsManager settings = SettingsManager.getInstance();
  private final ChoiceGroup languageGroup;
  private final TextField apiUrlField;
  private final TextField imageProxyField;
  private final TextField pinsField;
  private final TextField pathField;

  private final String savedLanguage;
  private final String savedApiUrl;
  private final String savedImageProxy;
  private final int savedPins;
  private final String savedPath;

  public PinterestSettingsScreen(Navigator navigator) {
    super(Lang.tr("pinterest.settings_title"), navigator);
    savedLanguage = settings.getCurrentLanguage();
    savedApiUrl = settings.getPinterestApiUrl();
    savedImageProxy = settings.getPinterestImageProxy();
    savedPins = settings.getPinterestPinsPerPage();
    savedPath = settings.getPinterestPath();

    languageGroup =
        new ChoiceGroup(Lang.tr("settings.language"), Choice.POPUP, LANG_NAMES, null);
    int selected = 0;
    for (int i = 0; i < LANG_CODES.length; i++) {
      if (LANG_CODES[i].equals(savedLanguage)) {
        selected = i;
        break;
      }
    }
    languageGroup.setSelectedIndex(selected, true);
    append(languageGroup);

    apiUrlField =
        new TextField(Lang.tr("pinterest.settings_api_url"), savedApiUrl, 256, TextField.URL);
    append(apiUrlField);

    imageProxyField =
        new TextField(
            Lang.tr("pinterest.settings_image_proxy"), savedImageProxy, 256, TextField.URL);
    append(imageProxyField);

    pinsField =
        new TextField(
            Lang.tr("pinterest.settings_pins_per_page"),
            Integer.toString(savedPins),
            2,
            TextField.NUMERIC);
    append(pinsField);

    pathField =
        new TextField(Lang.tr("pinterest.settings_path"), savedPath, 256, TextField.URL);
    append(pathField);

    addCommand(Commands.formSave());
  }

  protected void handleCommand(Command c, Displayable d) {
    if (c == Commands.formSave()) {
      save();
    }
  }

  protected boolean onBackPressed() {
    if (!isDirty()) {
      return false;
    }
    navigator.showConfirmationAlert(
        Lang.tr("confirm.discard_changes"),
        new Runnable() {
          public void run() {
            navigator.back();
          }
        },
        AlertType.WARNING);
    return true;
  }

  private boolean isDirty() {
    return !savedLanguage.equals(selectedLanguage())
        || !savedApiUrl.equals(apiUrlField.getString().trim())
        || !savedImageProxy.equals(imageProxyField.getString().trim())
        || savedPins != parsePins(pinsField.getString())
        || !savedPath.equals(pathField.getString().trim());
  }

  private String selectedLanguage() {
    int index = languageGroup.getSelectedIndex();
    if (index < 0 || index >= LANG_CODES.length) {
      return savedLanguage;
    }
    return LANG_CODES[index];
  }

  private void save() {
    String language = selectedLanguage();
    String apiUrl = apiUrlField.getString().trim();
    String imageProxy = imageProxyField.getString().trim();
    int pins = parsePins(pinsField.getString());
    String path = pathField.getString().trim();
    try {
      boolean languageChanged = !language.equals(savedLanguage);
      if (languageChanged) {
        settings.saveLanguage(language);
        Commands.refresh();
      }
      if (!apiUrl.equals(savedApiUrl)) {
        settings.savePinterestApiUrl(apiUrl);
      }
      if (!imageProxy.equals(savedImageProxy)) {
        settings.savePinterestImageProxy(imageProxy);
      }
      if (pins != savedPins) {
        settings.savePinterestPinsPerPage(pins);
      }
      if (!path.equals(savedPath)) {
        settings.savePinterestPath(path);
      }
      if (languageChanged) {
        // Rebuild the root screen so every title/command picks up the new language.
        navigator.setRoot(new PinterestScreen(navigator));
      } else {
        navigator.back();
      }
    } catch (RecordStoreException e) {
      navigator.showAlert(Lang.tr("settings.error.save_failed"), AlertType.ERROR);
    }
  }

  private static int parsePins(String value) {
    try {
      int parsed = Integer.parseInt(value.trim());
      if (parsed < Configuration.PINTEREST_PINS_PER_PAGE_MIN) {
        return Configuration.PINTEREST_PINS_PER_PAGE_MIN;
      }
      if (parsed > Configuration.PINTEREST_PINS_PER_PAGE_MAX) {
        return Configuration.PINTEREST_PINS_PER_PAGE_MAX;
      }
      return parsed;
    } catch (Exception e) {
      return Configuration.DEFAULT_PINTEREST_PINS_PER_PAGE;
    }
  }
}
