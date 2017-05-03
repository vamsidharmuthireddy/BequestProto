package in.ac.iiit.cvit.bequest;

import android.content.Context;
import android.content.SharedPreferences;

public class LaunchPreferenceManager {

    private int PRIVATE_MODE = 0;

    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    private Context _context;

    private static final String PREF_NAME = "launch_preferences";
    private static final String IS_Downloaded = "IsDownloaded";

    public LaunchPreferenceManager(Context context) {
        this._context = context;
        preferences = _context.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
        editor = preferences.edit();
    }

    /**
     * After launching the app for the first time, this IS_FIRST_TIME_LAUNCH is set to be true
     * @param isDownloaded This is the boolean input. It is set to be false after opening the app for the
     *                    first time after installing. This is responsible for showing tajmahal and
     *                    other images on first opening of app.
     */
    public void setDownloaded(boolean isDownloaded) {
        editor.putBoolean(IS_Downloaded, isDownloaded);
        editor.commit();
        //       Log.v("setFirstTimeLaunch", "setting first time as "+ isFirstTime);
    }

    /**
     * This function is called when app is opened from the second time.
     * @return This is a boolean value telling that this app is not being opened for the first time.
     * It was already set to false when we opende the app for the first time.
     */
    public boolean isDownloaded() {
        //       Log.v("isFirstTimeLaunch", "setting first time as "+ preferences.getBoolean(IS_FIRST_TIME_LAUNCH, true));
        return preferences.getBoolean(IS_Downloaded, false);

    }
}