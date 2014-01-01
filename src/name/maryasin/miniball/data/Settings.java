package name.maryasin.miniball.data;

import java.io.IOException;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class Settings implements OnSharedPreferenceChangeListener {
	private static final String KEY_ROOTPATH = "cfg_rootPath";
	
	private static Settings instance = null;
	public static void init(Context context) {
		instance = new Settings(context);
	}
	public static Settings get() {
		if(instance == null)
			throw new IllegalStateException("Settings not initialized!");
		return instance;
	}

	private SharedPreferences sharedPrefs;
	private Context context;
	private Settings(Context context) {
		this.context = context;
		this.sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
	}
	
	public String getRootPath() {
		return sharedPrefs.getString(KEY_ROOTPATH, "");
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
		if(key.equals(KEY_ROOTPATH)) {
			try {
				DataManager.initDanceList();
			} catch (IOException e) {
				Log.e("DanceListFragment", "Ошибка инициализации DataManager", e);
				Toast.makeText(context, "Не удалось инициализировать данные!\nОшибка: "+e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
				return;
			}
			// TODO: update interface
		}
	}
}
