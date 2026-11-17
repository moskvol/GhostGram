package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;

/** Client-side GhostGram options; these values never change Telegram account entitlements. */
public final class GhostGramFeatures {
    private static final String PREFS = "ghostgram_mod";

    private GhostGramFeatures() {
    }

    private static SharedPreferences preferences() {
        Context context = ApplicationLoader.applicationContext;
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean localVerified() {
        return preferences().getBoolean("local_verified", false);
    }

    public static boolean localPremium() {
        return preferences().getBoolean("ghost_premium", false);
    }

    public static boolean hideTyping() {
        return preferences().getBoolean("ghost_hide_typing", false);
    }
}
