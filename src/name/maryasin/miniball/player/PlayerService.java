package name.maryasin.miniball.player;

import name.maryasin.miniball.DanceListActivity;
import name.maryasin.miniball.R;

import android.app.*;
import android.content.Intent;
import android.os.*;
import android.util.Log;
import android.widget.Toast;

public class PlayerService extends Service {
	private NotificationManager notificationMgr;

	private static final int NOTIFICATION_ID = R.string.local_service_id;

	public static final String TAG = "PlayerService";

	@Override
	public void onCreate() {
		notificationMgr = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

		// show persistent notification
		showNotification();
	}

	@Override
	public void onDestroy() {
		// hide persistent notification
		notificationMgr.cancel(NOTIFICATION_ID);
		// TODO stop music
		// Debug, FIXME
		Toast.makeText(this, "PlayerService stopped", Toast.LENGTH_SHORT).show();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "Received start id "+startId+": "+intent);
		// TODO: handle intent, if needed
		if(intent == null) {
			// We were restarted by system. TODO: resume playback form stored position
		}

		// Keep running until explicit stop
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		// We don't support binding
		return null;
	}

	private void showNotification() {
		// TODO
		Notification n = new Notification.Builder(this)
				.setContentTitle(getText(R.string.app_name))
				.setContentText("Hello World")
				.setSmallIcon(R.drawable.ic_notif)
				//.setLargeIcon(R.drawable.ic_player_large) // TODO: current material's image, if present
				.setOngoing(true)
				.build();
		PendingIntent pi = PendingIntent.getActivity(
				this, 0,
				new Intent(this, DanceListActivity.class),
				0);
		notificationMgr.notify(NOTIFICATION_ID, n);
	}

	public void playbackStart() {
	}

	public void playbackStop() {
	}
}
