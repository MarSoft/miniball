package name.maryasin.miniball;

import java.util.List;

import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;

public class ConfigActivity extends PreferenceActivity {
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			addPreferencesFromResource(R.xml.settings);
		}
	}
	
	@Override
	public void onBuildHeaders(List<Header> target) {
		//loadHeadersFromResource(R.xml.config_headers, target);
	}
}
