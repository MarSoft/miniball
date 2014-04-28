package name.maryasin.miniball.player;

import name.maryasin.miniball.DanceListActivity;
import name.maryasin.miniball.R;
import name.maryasin.miniball.data.DataManager;
import name.maryasin.miniball.data.Material;

import android.app.*;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.*;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class PlayerService extends Service implements
		MediaPlayer.OnCompletionListener {

	///////////////
	// Constants //

	public static final String ACTION_ENQUEUE = "name.maryasin.miniball.action.ENQUEUE";
	public static final String ACTION_STOP = "name.maryasin.miniball.action.STOP";

	private static final int NOTIFICATION_ID = 1;

	public static final String TAG = "PlayerService";

	////////////
	// Fields //

	private NotificationManager notificationMgr;

	/** For IPC: application-wide instance of the service */
	private static PlayerService sInstance;

	private List<Material> playbackQueue = new ArrayList<Material>();

	///////////////////////
	// Service callbacks //

	@Override
	public void onCreate() {
		sInstance = this;

		notificationMgr = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

		// start service as foreground, and show persistent notification
		startForeground(NOTIFICATION_ID, makeNotification());
	}

	@Override
	public void onDestroy() {
		// hide persistent notification
		notificationMgr.cancel(NOTIFICATION_ID);
		// and to really remove notification created by startForeground:
		stopForeground(true);

		// TODO: stop music if still playing

		// Debug, FIXME
		Toast.makeText(this, "PlayerService stopped", Toast.LENGTH_SHORT).show();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "Received start id "+startId+": "+intent);
		if(intent != null) {
			String action = intent.getAction();
			if(ACTION_ENQUEUE.equals(action)) {
				Uri trackUri = intent.getData();
				Material track = DataManager.getMaterialFromUri(trackUri);
				Log.i(TAG, "Enqueue track: "+track);
			} else if(ACTION_STOP.equals(action)) {
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

	////////////////////////
	// Listener callbacks //

	@Override
	public void onCompletion(MediaPlayer mp) {
		// TODO
	}

	/////////////////////////////////////////
	// Inter-process communication methods //

	public static PlayerService getInstance() {
		if(sInstance == null) {
			// TODO: Start service
		}
		return sInstance;
	}

	public Material getCurrentTrack() {
		if(playbackQueue.size() == 0)
			return null;
		return playbackQueue.get(0);
	}

	/////////////////////
	// Private methods //

	/**
	 * Construct a notification adequate with current state.
	 * @return Notification
	 */
	private Notification makeNotification() {
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
				.addAction(R.drawable.ic_action_next, getText(R.string.action_next), piNext)
				.build();
		return n;
	}
	private void updateNotification() {
		notificationMgr.notify(NOTIFICATION_ID, makeNotification());
	}

	public void playbackStart() {
	}

	public void playbackStop() {
	}
}
