package in.ac.iiit.cvit.bequest;

/**
 * Created by HOME on 20-12-2016.
 */

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    /**
     * This class is first called from PackageLisActivity.
     * When user clicks ona heritage site on his list, This class sets opens that heritage site as session
     * So, whatever happens from now on is related to that particular session.
     */
    private static final int CONTEXT_MODE = 0;
    private static final String SESSION_PREFERENCES = "UserSession";


    public void setSessionPreferences(Context context, String key, String value) {
        SharedPreferences.Editor editor = context.getSharedPreferences(SESSION_PREFERENCES, CONTEXT_MODE).edit();
        editor.putString(key, value);
        editor.commit();
    }

    public void setSessionPreferences(Context context, String key, boolean value) {
        SharedPreferences.Editor editor = context.getSharedPreferences(SESSION_PREFERENCES, CONTEXT_MODE).edit();
        editor.putBoolean(key, value);
        editor.commit();
    }

    public String getStringSessionPreferences(Context context, String key, String default_value) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SESSION_PREFERENCES, CONTEXT_MODE);
        String preference = sharedPreferences.getString(key, default_value);
        return preference;
    }

    public boolean getBooleanSessionPreferences(Context context, String key, boolean default_value) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SESSION_PREFERENCES, CONTEXT_MODE);
        boolean preference = sharedPreferences.getBoolean(key, default_value);
        return preference;
    }

    public void clearSessionPreferences(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SESSION_PREFERENCES, CONTEXT_MODE);
        sharedPreferences.edit().clear().commit();
    }
}