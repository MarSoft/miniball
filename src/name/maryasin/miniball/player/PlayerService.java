package name.maryasin.miniball.player;

import name.maryasin.miniball.DanceListActivity;
import name.maryasin.miniball.R;

import android.app.*;
import android.content.Intent;
import android.net.Uri;
import android.os.*;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

public class PlayerService extends Service {
	public static final String ACTION_ENQUEUE = "name.maryasin.miniball.action.ENQUEUE";
	public static final String ACTION_STOP = "name.maryasin.miniball.action.STOP";

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
		// TODO: stop music if still playing
		// Debug, FIXME
		Toast.makeText(this, "PlayerService stopped", Toast.LENGTH_SHORT).show();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "Received start id "+startId+": "+intent);
		if(intent != null) {
			String action = intent.getAction();
			if(action.equals(ACTION_ENQUEUE)) {
				Uri track = intent.getData();
				Log.i(TAG, "Enqueue track: "+track);
			} else if(action.equals(ACTION_STOP)) {
				Log.i(TAG, "Stop playback");
			} else {
				Log.w(TAG, "Unknown action: "+action);
			}
		} else {
			// We were restarted by system. TODO: resume playback form stored position?
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
		PendingIntent piClick = PendingIntent.getActivity(
				this, 0,
				new Intent(this, DanceListActivity.class), // FIXME: use PlayerActivity
				0);
		PendingIntent piNext = PendingIntent.getActivity(
				this, 0,
				new Intent(this, DanceListActivity.class),
				0);
		Notification n = new NotificationCompat.Builder(this)
				.setContentTitle(getText(R.string.app_name))
				.setContentText("Hello World")
				.setSmallIcon(R.drawable.ic_notif)
				//.setLargeIcon(R.drawable.ic_player_large) // TODO: current material's image, if present
				.setOngoing(true)
				.setContentIntent(piClick)
				.addAction(R.drawable.ic_action_next, null, piNext)
				.build();
		notificationMgr.notify(NOTIFICATION_ID, n);
	}

	public void playbackStart() {
	}

	public void playbackStop() {
	}
}
