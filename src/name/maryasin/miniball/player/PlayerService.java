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
		// TODO: handle intent, if needed

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
		// FIXME: almost copied from http://developer.android.com/reference/android/app/Service.html
		Notification n = new Notification(
				R.drawable.ic_player,
				"Player Service Running", // getText(R.string.something)
				System.currentTimeMillis());
		PendingIntent pi = PendingIntent.getActivity(
				this, 0,
				new Intent(this, LocalServiceActivities.Controller.class),
				0);
		n.setLatestEvent
	}
}
