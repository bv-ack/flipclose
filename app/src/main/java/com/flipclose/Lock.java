package com.flipclose;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Locale;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Purpose: the 6-digit PIN and a recovery code, kept only as salted hashes, with growing delays after wrong tries. */
final class Lock {
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int ROUNDS = 60000;

    private static SharedPreferences p(Context c) {
        return c.getSharedPreferences("flipclose_lock", Context.MODE_PRIVATE);
    }

    static boolean isSet(Context c) { return p(c).contains("pin"); }

    /** Sets the PIN. Returns a new recovery code, to be shown once. */
    static String set(Context c, String pin) {
        SecureRandom r = new SecureRandom();
        StringBuilder raw = new StringBuilder();
        for (int i = 0; i < 12; i++) raw.append(ALPHABET.charAt(r.nextInt(ALPHABET.length())));
        p(c).edit().putString("pin", store(pin)).putString("rec", store(raw.toString()))
                .putInt("fails", 0).putLong("until", 0).apply();
        String s = raw.toString();
        return s.substring(0, 4) + "-" + s.substring(4, 8) + "-" + s.substring(8, 12);
    }

    static boolean check(Context c, String pin) {
        if (waitMs(c) > 0) return false;
        boolean good = matches(pin, p(c).getString("pin", null));
        if (good) reset(c); else fail(c);
        return good;
    }

    /** A right recovery code removes the PIN. */
    static boolean useRecovery(Context c, String code) {
        if (waitMs(c) > 0) return false;
        String norm = code.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.US);
        boolean good = matches(norm, p(c).getString("rec", null));
        if (good) clear(c); else fail(c);
        return good;
    }

    private static final long EXIT_WAIT_MS = 3600000L; // 1 hour

    /** Starts the "lost the PIN and the code" exit timer. */
    static void startExit(Context c) { p(c).edit().putLong("exitat", System.currentTimeMillis() + EXIT_WAIT_MS).apply(); }

    /** -1 = not started, 0 = ready, otherwise milliseconds left. */
    static long exitLeftMs(Context c) {
        long at = p(c).getLong("exitat", 0L);
        return at == 0L ? -1L : Math.max(0L, at - System.currentTimeMillis());
    }

    static void clear(Context c) { p(c).edit().clear().apply(); }

    /** Milliseconds left before another try is allowed. */
    static long waitMs(Context c) {
        return Math.max(0L, p(c).getLong("until", 0L) - System.currentTimeMillis());
    }

    private static void reset(Context c) { p(c).edit().putInt("fails", 0).putLong("until", 0L).remove("exitat").apply(); }

    /** From the 5th wrong try on: 30 s, then 1 min, 2 min, ... up to 1 hour. */
    private static void fail(Context c) {
        int f = p(c).getInt("fails", 0) + 1;
        long delay = f < 5 ? 0L : Math.min(3600000L, 30000L << Math.min(f - 5, 7));
        p(c).edit().putInt("fails", f).putLong("until", System.currentTimeMillis() + delay).apply();
    }

    private static String hash(String secret, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(secret.toCharArray(), salt, ROUNDS, 256);
            byte[] h = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return Base64.encodeToString(h, Base64.NO_WRAP);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String store(String secret) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return Base64.encodeToString(salt, Base64.NO_WRAP) + ":" + hash(secret, salt);
    }

    private static boolean matches(String secret, String stored) {
        if (stored == null) return false;
        String[] parts = stored.split(":");
        if (parts.length != 2) return false;
        byte[] salt = Base64.decode(parts[0], Base64.NO_WRAP);
        return MessageDigest.isEqual(hash(secret, salt).getBytes(), parts[1].getBytes());
    }
}
