package name.maryasin.miniball;

import android.app.Activity;
import android.os.Bundle;

public class ConfigActivity extends Activity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getFragmentManager().beginTransaction()
			.replace(android.R.id.content, new ConfigFragment())
			.commit();
	}
}
