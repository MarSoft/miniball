package name.maryasin.miniball.player;

import name.maryasin.miniball.DanceListActivity;
import name.maryasin.miniball.R;
import name.maryasin.miniball.data.DataManager;
import name.maryasin.miniball.data.Material;

import android.app.*;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.*;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PlayerService extends Service implements
		MediaPlayer.OnCompletionListener,
		MediaPlayer.OnErrorListener {

	///////////////
	// Constants //

	public static final String ACTION_ENQUEUE = "name.maryasin.miniball.action.ENQUEUE";
	public static final String ACTION_STOP = "name.maryasin.miniball.action.STOP";
	public static final String ACTION_REPLAY = "name.maryasin.miniball.action.REPLAY";

	private static final int NOTIFICATION_ID = 1;

	public static final String TAG = "PlayerService";

	////////////
	// Fields //

	private NotificationManager notificationMgr;

	/** For IPC: application-wide instance of the service */
	private static PlayerService sInstance;

	private List<Material> playbackQueue = new ArrayList<Material>();

	private MediaPlayer mPlayer;

	///////////////////////
	// Service callbacks //

	@Override
	public void onCreate() {
		sInstance = this;

		notificationMgr = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

		mPlayer = createMediaPlayer();

		// start service as foreground, and show persistent notification
		startForeground(NOTIFICATION_ID, createNotification());
	}

	@Override
	public void onDestroy() {
		// hide persistent notification
		notificationMgr.cancel(NOTIFICATION_ID);
		// and to really remove notification created by startForeground:
		stopForeground(true);

		if(mPlayer != null) {
			// TODO: save position?
			mPlayer.release();
			mPlayer = null;
		}

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
				if(track.hasAudio()) {
					Log.i(TAG, "Enqueue track: " + track);
					playbackQueue.add(track);
					// FIXME: if already playing?
					playTrack(track);
				} else {
					Toast.makeText(this, "Material has no audio: " + track, Toast.LENGTH_SHORT).show();
				}
			} else if(ACTION_STOP.equals(action)) {
				playbackStop();
			} else if(ACTION_REPLAY.equals(action)) {
				playbackRestart();
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

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		Log.e(TAG, "MediaPlayer error: "+what+", "+extra);
		return true; // yes, we processed this error
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
	private Notification createNotification() {
		PendingIntent piClick = PendingIntent.getActivity(
				this, 0,
				new Intent(this, DanceListActivity.class), // FIXME: use PlayerActivity
				0);
		PendingIntent piStop = PendingIntent.getActivity(this, 0,
				new Intent(ACTION_STOP, null, this, PlayerService.class), 0);
		PendingIntent piReplay = PendingIntent.getActivity(this, 0,
				new Intent(ACTION_REPLAY, null, this, PlayerService.class), 0);

		Material ct = getCurrentTrack();
		String trackName = "<No track loaded>";
		if(ct != null)
			trackName = ct.name;
		Notification n = new NotificationCompat.Builder(this)
				.setContentTitle(getText(R.string.app_name))
				.setContentText(trackName)
				.setContentInfo(mPlayer.isPlaying() ? "|>" : "[]")
				.setSmallIcon(R.drawable.ic_notif)
				//.setLargeIcon(R.drawable.ic_player_large) // TODO: current material's image, if present
				.setOngoing(true)
				.setContentIntent(piClick)
				.addAction(R.drawable.ic_action_stop, getText(R.string.action_stop), piStop)
				.addAction(R.drawable.ic_action_replay, getText(R.string.action_replay), piReplay)
				.build();
		return n;
	}
	private void updateNotification() {
		notificationMgr.notify(NOTIFICATION_ID, createNotification());
	}

	private MediaPlayer createMediaPlayer() {
		MediaPlayer mp = new MediaPlayer();
		mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
		mp.setOnCompletionListener(this);
		mp.setOnErrorListener(this);
		return mp;
	}

	private void playTrack(Material track) {
		try {
			mPlayer.setDataSource(track.getAudioFile().getPath());
			mPlayer.prepare();
			playbackStart();
		} catch(IOException ex) {
			Log.e(TAG, ""+ex);
		}
	}

	public void playbackStart() {
		Log.i(TAG, "Starting playback");
		mPlayer.start();
		updateNotification();
	}

	public void playbackStop() {
		Log.i(TAG, "Stopping playback");
		mPlayer.stop();
		updateNotification();
	}

	public void playbackRestart() {
		Log.i(TAG, "Restarting track");
		mPlayer.stop();
		mPlayer.seekTo(0);
		// TODO: configurable delay
		mPlayer.start();
		updateNotification();
	}
}
