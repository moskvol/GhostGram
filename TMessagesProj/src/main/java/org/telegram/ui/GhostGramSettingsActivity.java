package org.telegram.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/** Local GhostGram controls. No Telegram server state is changed here. */
public class GhostGramSettingsActivity extends BaseFragment {
    private static final String PREFS = "ghostgram_mod";
    private static final int BACKGROUND = 0xff111b26;
    private static final int CARD = 0xff1d2a39;
    private static final int ACCENT = 0xff73bde9;
    private static final int REQUEST_EXPORT = 731;
    private static final int REQUEST_IMPORT = 732;
    private static final String[] CATEGORIES = {
            "Основные", "Внешний вид", "Чаты", "Режим призрака", "Антиудаление",
            "Ghost Premium", "Плагины", "Приватность", "Другое"
    };
    private static final int[] ICONS = {
            R.drawable.settings_account, R.drawable.settings_chat, R.drawable.settings_folders,
            R.drawable.settings_privacy, R.drawable.settings_data, R.drawable.settings_premium,
            R.drawable.settings_power, R.drawable.settings_devices, R.drawable.settings_language
    };

    private final int category;
    private ScrollView scrollView;
    private TextView versionView;
    private TextView premiumView;
    private Switch videoSwitch;
    private Switch gifSwitch;
    private boolean refreshingNativeSwitches;

    public GhostGramSettingsActivity() {
        this(-1);
    }

    private GhostGramSettingsActivity(int category) {
        this.category = category;
    }

    @Override
    protected boolean useSimpleActionBar() {
        return true;
    }

    @Override
    public View createView(Context context) {
        setHasOwnBackground(true);
        simpleActionBar.setBackButtonImage(R.drawable.ic_ab_back);
        simpleActionBar.setTitle(category < 0 ? "GhostGram" : CATEGORIES[category]);
        simpleActionBar.setBackgroundColor(background(context));
        simpleActionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) finishFragment();
            }
        });

        scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(background(context));
        scrollView.setVerticalScrollBarEnabled(false);
        fragmentView = scrollView;
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(12), dp(16), dp(28));
        scrollView.addView(content);
        if (category < 0) {
            buildHome(context, content);
        } else {
            buildCategory(context, content);
        }
        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (scrollView != null && getParentActivity() != null) {
            scrollView.setBackgroundColor(background(getParentActivity()));
            simpleActionBar.setBackgroundColor(background(getParentActivity()));
            if (versionView != null) {
                versionView.setVisibility(prefs(getParentActivity()).getBoolean("show_version", true) ? View.VISIBLE : View.GONE);
            }
            if (premiumView != null) {
                premiumView.setVisibility(prefs(getParentActivity()).getBoolean("ghost_premium", false) ? View.VISIBLE : View.GONE);
            }
            refreshingNativeSwitches = true;
            if (videoSwitch != null) videoSwitch.setChecked(!SharedConfig.isAutoplayVideo());
            if (gifSwitch != null) gifSwitch.setChecked(!SharedConfig.isAutoplayGifs());
            refreshingNativeSwitches = false;
        }
    }

    private void buildHome(Context context, LinearLayout content) {
        ImageView logo = new ImageView(context);
        logo.setImageResource(R.drawable.ghostgram_header_icon);
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(dp(72), dp(72));
        logoParams.gravity = Gravity.CENTER_HORIZONTAL;
        logoParams.topMargin = dp(12);
        content.addView(logo, logoParams);

        TextView title = text(context, "GhostGram", 20, Color.WHITE);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, -2);
        titleParams.topMargin = dp(4);
        content.addView(title, titleParams);
        versionView = text(context, BuildVars.BUILD_VERSION_STRING, 12, 0xffa6b2c0);
        versionView.setGravity(Gravity.CENTER);
        content.addView(versionView);
        premiumView = text(context, "★ Ghost Premium", 12, ACCENT);
        premiumView.setGravity(Gravity.CENTER);
        content.addView(premiumView);
        versionView.setVisibility(prefs(context).getBoolean("show_version", true) ? View.VISIBLE : View.GONE);
        premiumView.setVisibility(prefs(context).getBoolean("ghost_premium", false) ? View.VISIBLE : View.GONE);

        TextView header = text(context, "Категории", 13, ACCENT);
        LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(-1, -2);
        headerParams.topMargin = dp(24);
        headerParams.bottomMargin = dp(8);
        content.addView(header, headerParams);

        LinearLayout card = card(context);
        content.addView(card);
        for (int i = 0; i < CATEGORIES.length; i++) {
            final int index = i;
            row(context, card, ICONS[i], CATEGORIES[i], () -> presentFragment(new GhostGramSettingsActivity(index)), i < CATEGORIES.length - 1);
        }
    }

    private void buildCategory(Context context, LinearLayout content) {
        TextView heading = text(context, CATEGORIES[category], 22, Color.WHITE);
        LinearLayout.LayoutParams headingParams = new LinearLayout.LayoutParams(-1, -2);
        headingParams.topMargin = dp(14);
        headingParams.bottomMargin = dp(18);
        content.addView(heading, headingParams);

        if (category == 0) {
            switchRow(context, content, "Показывать версию", "show_version", true);
            link(context, content, R.drawable.settings_power, "Энергосбережение и производительность", () -> presentFragment(new LiteModeSettingsActivity()));
            link(context, content, R.drawable.settings_data, "Экспорт настроек", this::exportSettings);
            link(context, content, R.drawable.settings_data, "Импорт настроек", this::importSettings);
        } else if (category == 1) {
            switchRow(context, content, "AMOLED-фон меню GhostGram", "amoled_menu", false);
            link(context, content, R.drawable.settings_chat, "Оформление Telegram", () -> presentFragment(new ThemeActivity(ThemeActivity.THEME_TYPE_BASIC)));
        } else if (category == 2) {
            link(context, content, R.drawable.settings_folders, "Папки чатов Telegram", () -> presentFragment(new FiltersSetupActivity()));
            info(context, content, "В меню каждого чата есть зашифрованная локальная заметка GhostGram.");
        } else if (category == 3) {
            videoSwitch = nativeSwitchRow(context, content, "Отключить автозапуск видео", !SharedConfig.isAutoplayVideo(), SharedConfig::toggleAutoplayVideo);
            gifSwitch = nativeSwitchRow(context, content, "Отключить автозапуск GIF", !SharedConfig.isAutoplayGifs(), SharedConfig::toggleAutoplayGifs);
            link(context, content, R.drawable.settings_data, "Другие настройки медиа и загрузок", () -> presentFragment(new DataSettingsActivity()));
            info(context, content, "Telegram API требует корректной отправки статусов набора текста и прочтения. Полный режим невидимки недоступен.");
        } else if (category == 5) {
            switchRow(context, content, "Локальный значок Ghost Premium", "ghost_premium", false);
            info(context, content, "Значок виден только в меню GhostGram и не меняет подписку Telegram.");
        } else if (category == 7) {
            switchRow(context, content, "Блокировать снимки экрана и превью недавних", "hide_recents", false);
            link(context, content, R.drawable.settings_privacy, "PIN-код, биометрия и автоблокировка", () -> presentFragment(PasscodeActivity.determineOpenFragment()));
            link(context, content, R.drawable.settings_privacy, "Настройки приватности Telegram", () -> presentFragment(new PrivacySettingsActivity()));
        } else if (category == 8) {
            info(context, content, "GhostGram · версия исходного клиента " + BuildVars.BUILD_VERSION_STRING);
            link(context, content, R.drawable.settings_data, "Управление локальным кэшем", () -> presentFragment(new CacheControlActivity()));
        }
    }

    private void switchRow(Context context, LinearLayout content, String label, String key, boolean defaultValue) {
        LinearLayout card = card(context);
        content.addView(card);
        LinearLayout row = new LinearLayout(context);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), 0, dp(12), 0);
        card.addView(row, new LinearLayout.LayoutParams(-1, dp(56)));
        TextView name = text(context, label, 15, Color.WHITE);
        row.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
        Switch control = new Switch(context);
        control.setChecked(prefs(context).getBoolean(key, defaultValue));
        control.setOnCheckedChangeListener((button, checked) -> {
            prefs(context).edit().putBoolean(key, checked).apply();
            if ("amoled_menu".equals(key)) {
                scrollView.setBackgroundColor(background(context));
                simpleActionBar.setBackgroundColor(background(context));
            } else if ("hide_recents".equals(key) && LaunchActivity.instance != null) {
                LaunchActivity.instance.updateGhostGramSecureFlag();
            }
        });
        row.addView(control);
        row.setOnClickListener(v -> control.setChecked(!control.isChecked()));
    }

    private Switch nativeSwitchRow(Context context, LinearLayout content, String label, boolean checked, Runnable toggle) {
        LinearLayout card = card(context);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(10);
        content.addView(card, params);
        LinearLayout line = new LinearLayout(context);
        line.setGravity(Gravity.CENTER_VERTICAL);
        line.setPadding(dp(16), 0, dp(12), 0);
        card.addView(line, new LinearLayout.LayoutParams(-1, dp(56)));
        TextView name = text(context, label, 15, Color.WHITE);
        line.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
        Switch control = new Switch(context);
        control.setChecked(checked);
        control.setOnCheckedChangeListener((button, value) -> {
            if (!refreshingNativeSwitches) toggle.run();
        });
        line.addView(control);
        line.setOnClickListener(v -> control.setChecked(!control.isChecked()));
        return control;
    }

    private void link(Context context, LinearLayout content, int icon, String label, Runnable action) {
        LinearLayout card = card(context);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(10);
        content.addView(card, params);
        row(context, card, icon, label, action, false);
    }

    private void row(Context context, LinearLayout card, int icon, String label, Runnable action, boolean divider) {
        LinearLayout line = new LinearLayout(context);
        line.setGravity(Gravity.CENTER_VERTICAL);
        line.setPadding(dp(14), 0, dp(14), 0);
        card.addView(line, new LinearLayout.LayoutParams(-1, dp(54)));
        ImageView image = new ImageView(context);
        image.setImageResource(icon);
        image.setColorFilter(0xff9da9b7);
        line.addView(image, new LinearLayout.LayoutParams(dp(22), dp(22)));
        TextView name = text(context, label, 15, Color.WHITE);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, -2, 1);
        nameParams.leftMargin = dp(16);
        line.addView(name, nameParams);
        TextView arrow = text(context, "›", 23, 0xff7d8a98);
        line.addView(arrow);
        line.setOnClickListener(v -> action.run());
        if (divider) {
            View border = new View(context);
            border.setBackgroundColor(0xff2b3948);
            LinearLayout.LayoutParams borderParams = new LinearLayout.LayoutParams(-1, dp(1));
            borderParams.leftMargin = dp(52);
            card.addView(border, borderParams);
        }
    }

    private void info(Context context, LinearLayout content, String message) {
        TextView view = text(context, message, 14, 0xffa6b2c0);
        view.setLineSpacing(dp(3), 1);
        content.addView(view);
    }

    private void exportSettings() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, "GhostGram-settings.json");
        startActivityForResult(intent, REQUEST_EXPORT);
    }

    private void importSettings() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        startActivityForResult(intent, REQUEST_IMPORT);
    }

    @Override
    public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK || data == null || data.getData() == null || getParentActivity() == null) return;
        Context context = getParentActivity();
        try {
            if (requestCode == REQUEST_EXPORT) {
                SharedPreferences settings = prefs(context);
                JSONObject json = new JSONObject();
                json.put("format", 1);
                json.put("show_version", settings.getBoolean("show_version", true));
                json.put("amoled_menu", settings.getBoolean("amoled_menu", false));
                json.put("ghost_premium", settings.getBoolean("ghost_premium", false));
                json.put("hide_recents", settings.getBoolean("hide_recents", false));
                json.put("autoplay_video", SharedConfig.isAutoplayVideo());
                json.put("autoplay_gifs", SharedConfig.isAutoplayGifs());
                try (OutputStream output = context.getContentResolver().openOutputStream(data.getData())) {
                    if (output == null) throw new IllegalStateException("Cannot open export file");
                    output.write(json.toString(2).getBytes(StandardCharsets.UTF_8));
                }
                Toast.makeText(context, "Настройки экспортированы", Toast.LENGTH_SHORT).show();
            } else if (requestCode == REQUEST_IMPORT) {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                try (InputStream input = context.getContentResolver().openInputStream(data.getData())) {
                    if (input == null) throw new IllegalStateException("Cannot open import file");
                    byte[] chunk = new byte[4096];
                    int length;
                    while ((length = input.read(chunk)) != -1) {
                        if (buffer.size() + length > 65536) throw new IllegalArgumentException("Settings file too large");
                        buffer.write(chunk, 0, length);
                    }
                }
                JSONObject json = new JSONObject(new String(buffer.toByteArray(), StandardCharsets.UTF_8));
                if (json.getInt("format") != 1 || !json.has("show_version") || !json.has("amoled_menu") || !json.has("ghost_premium")) {
                    throw new IllegalArgumentException("Unsupported settings file");
                }
                prefs(context).edit()
                        .putBoolean("show_version", json.getBoolean("show_version"))
                        .putBoolean("amoled_menu", json.getBoolean("amoled_menu"))
                        .putBoolean("ghost_premium", json.getBoolean("ghost_premium"))
                        .putBoolean("hide_recents", json.optBoolean("hide_recents", false))
                        .apply();
                if (LaunchActivity.instance != null) LaunchActivity.instance.updateGhostGramSecureFlag();
                if (json.has("autoplay_video") && SharedConfig.isAutoplayVideo() != json.getBoolean("autoplay_video")) SharedConfig.toggleAutoplayVideo();
                if (json.has("autoplay_gifs") && SharedConfig.isAutoplayGifs() != json.getBoolean("autoplay_gifs")) SharedConfig.toggleAutoplayGifs();
                Toast.makeText(context, "Настройки импортированы", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception error) {
            Toast.makeText(context, "Не удалось обработать файл настроек", Toast.LENGTH_LONG).show();
        }
    }

    private LinearLayout card(Context context) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(CARD);
        shape.setCornerRadius(dp(20));
        card.setBackground(shape);
        return card;
    }

    private int background(Context context) {
        return prefs(context).getBoolean("amoled_menu", false) ? Color.BLACK : BACKGROUND;
    }

    private SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private TextView text(Context context, String value, int sp, int color) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        return view;
    }

    private static int dp(float value) {
        return AndroidUtilities.dp(value);
    }
}
