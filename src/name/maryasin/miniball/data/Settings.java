package name.maryasin.miniball.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;

public class Settings implements OnSharedPreferenceChangeListener {
	private static final String KEY_ROOTPATH = "cfg_rootPath";
	
	private SharedPreferences sharedPrefs;
	
	private Settings(Context context) {
		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
	}
	
	private static Settings instance = null;
	public static void init(Context context) {
		instance = new Settings(context);
	}
	public static Settings get() {
		if(instance == null)
			throw new IllegalStateException("Settings not initialized!");
		return instance;
	}

	public String getRootPath() {
		return sharedPrefs.getString(KEY_ROOTPATH, "");
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences arg0, String arg1) {
		// TODO: update data when rootpath is changed
	}
}
