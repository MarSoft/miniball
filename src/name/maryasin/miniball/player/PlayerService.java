package name.maryasin.miniball.player;

import name.maryasin.miniball.R;
import name.maryasin.miniball.data.*;

import android.app.*;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.*;
import android.util.Log;
import android.widget.Toast;

import java.io.File;

public class PlayerService extends Service {
	private NotificationManager notifMgr;

	private static final int NOTIF_ID = R.string.local_service_id;

	public static final String TAG = "PlayerService";

	@Override
	public void onCreate() {
		notifMgr = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

		// show persistent notification
		showNotification();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "Received start id "+startId+": "+intent);
		// Keep running until explicit stop
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		// hide persistent notification
		notifMgr.cancel(NOTIF_ID);
		// Debug, FIXME
		Toast.makeText(this, "PlayerService stopped", Toast.LENGTH_SHORT).show();
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO
		return null;
	}

	private void showNotification() {
		// TODO
	}
}
