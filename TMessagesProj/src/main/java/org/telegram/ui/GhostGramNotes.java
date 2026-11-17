package org.telegram.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Device-only encrypted notes, scoped to a Telegram account and dialog. */
public final class GhostGramNotes {
    private static final String KEY_ALIAS = "ghostgram_notes_v1";
    private static final String STORE = "ghostgram_notes";

    private GhostGramNotes() {
    }

    private static String entry(int account, long dialog, long topic) {
        return account + "_" + dialog + "_" + topic;
    }

    private static SecretKey key() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        SecretKey existing = (SecretKey) store.getKey(KEY_ALIAS, null);
        if (existing != null) {
            return existing;
        }
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build());
        return generator.generateKey();
    }

    public static String read(Context context, int account, long dialog, long topic) throws Exception {
        SharedPreferences prefs = context.getSharedPreferences(STORE, Context.MODE_PRIVATE);
        String encoded = prefs.getString(entry(account, dialog, topic), null);
        if (encoded == null) {
            return "";
        }
        byte[] data = Base64.decode(encoded, Base64.NO_WRAP);
        if (data.length <= 12) {
            throw new IllegalArgumentException("Invalid note");
        }
        byte[] iv = new byte[12];
        System.arraycopy(data, 0, iv, 0, iv.length);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, iv));
        return new String(cipher.doFinal(data, iv.length, data.length - iv.length), StandardCharsets.UTF_8);
    }

    public static void write(Context context, int account, long dialog, long topic, String note) throws Exception {
        SharedPreferences prefs = context.getSharedPreferences(STORE, Context.MODE_PRIVATE);
        String name = entry(account, dialog, topic);
        if (note.isEmpty()) {
            prefs.edit().remove(name).apply();
            return;
        }
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key());
        byte[] iv = cipher.getIV();
        byte[] encrypted = cipher.doFinal(note.getBytes(StandardCharsets.UTF_8));
        byte[] data = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, data, 0, iv.length);
        System.arraycopy(encrypted, 0, data, iv.length, encrypted.length);
        prefs.edit().putString(name, Base64.encodeToString(data, Base64.NO_WRAP)).apply();
    }
}
