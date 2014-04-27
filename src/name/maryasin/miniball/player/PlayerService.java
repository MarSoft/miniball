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
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "Received start id "+startId+": "+intent);
		// TODO: handle intent, if needed

		// Keep running until explicit stop
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		// hide persistent notification
		notificationMgr.cancel(NOTIFICATION_ID);
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
		Notification n = new Notification.Builder(this)
				.setContentTitle(getText(R.string.app_name))
				.setContentText("Hello World")
				.setSmallIcon(R.drawable.ic_notif)
				.setLargeIcon(R.drawable.ic_player_large) // TODO: current material's image, if present
				.build();
		PendingIntent pi = PendingIntent.getActivity(
				this, 0,
				new Intent(this, DanceListActivity.class),
				0);
	}
}