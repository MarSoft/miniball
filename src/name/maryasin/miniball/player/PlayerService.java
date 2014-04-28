package name.maryasin.miniball.player;

import name.maryasin.miniball.DanceListActivity;
import name.maryasin.miniball.R;
import name.maryasin.miniball.data.DataManager;
import name.maryasin.miniball.data.Material;

import android.app.*;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
		MediaPlayer.OnErrorListener,
		AudioManager.OnAudioFocusChangeListener,
		Handler.Callback {

	///////////////
	// Constants //

	public static final String ACTION_ENQUEUE = "name.maryasin.miniball.action.ENQUEUE";
	public static final String ACTION_PLAY = "name.maryasin.miniball.action.PLAY";
	public static final String ACTION_PAUSE = "name.maryasin.miniball.action.PAUSE";
	/** Stop means Pause and seek to beginning */
	public static final String ACTION_STOP = "name.maryasin.miniball.action.STOP";
	public static final String ACTION_REPLAY = "name.maryasin.miniball.action.REPLAY";

	private static final int NOTIFICATION_ID = 1;
	private static final int MSG_UPDATE_NOTIFICATION = 1;

	public static final String TAG = "PlayerService";

	////////////
	// Fields //

	private NotificationManager notificationMgr;

	/** For IPC: application-wide instance of the service */
	private static PlayerService sInstance;

	private List<Material> playbackQueue = new ArrayList<Material>();

	private AudioManager mAudioManager;
	private MediaPlayer mPlayer;

	private Handler mNotificationUpdateHandler = new Handler(this);

	///////////////////////
	// Service callbacks //

	@Override
	public void onCreate() {
		sInstance = this;
		notificationMgr = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

		mPlayer = createMediaPlayer();
		mAudioManager = (AudioManager)getSystemService(AUDIO_SERVICE);

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
					// FIXME: do enqueue, not just play
					playbackQueue.clear();
					playbackQueue.add(track);
					playTrack(track);
				} else {
					Toast.makeText(this, "Material has no audio: " + track, Toast.LENGTH_SHORT).show();
				}
			} else if(ACTION_PLAY.equals(action)) {
				playbackStart();
			} else if(ACTION_PAUSE.equals(action)) {
				playbackPause();
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

	@Override
	public boolean handleMessage(Message msg) {
		if(msg.what == MSG_UPDATE_NOTIFICATION) {
			updateNotification();
			return true;
		}
		return false;
	}

	@Override
	public void onAudioFocusChange(int type) {
		Log.d(TAG, "Audio focus change: "+type);
		switch(type) {
			case AudioManager.AUDIOFOCUS_LOSS:
				playbackPause();
				break;
			case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
			case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
				// ignore and keep playing
				break;
			case AudioManager.AUDIOFOCUS_GAIN:
				playbackStart(); // FIXME: only if was not paused
				break;
		}
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
		PendingIntent piPause = PendingIntent.getService(this, 0,
				new Intent(mPlayer.isPlaying() ? ACTION_PAUSE : ACTION_PLAY,
						null, this, PlayerService.class), 0);
		PendingIntent piStop = PendingIntent.getService(this, 0,
				new Intent(ACTION_STOP, null, this, PlayerService.class), 0);
		PendingIntent piReplay = PendingIntent.getService(this, 0,
				new Intent(ACTION_REPLAY, null, this, PlayerService.class), 0);

		Material ct = getCurrentTrack();
		String trackName = "<No track loaded>";
		String danceName = "<No dance loaded>";
		String duration = "0:00";
		if(ct != null) {
			trackName = ct.name;
			danceName = ct.dance.getName();
			duration = positionToStr(mPlayer.getCurrentPosition()) + " / " +
					positionToStr(mPlayer.getDuration());
		}
		// TODO: current material's image, if present
		Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
		Notification n = new NotificationCompat.Builder(this)
				.setContentTitle(danceName)
				.setContentText(trackName)
				.setContentInfo(duration)
				.setProgress(mPlayer.getDuration(), mPlayer.getCurrentPosition(), false)
				.setSmallIcon(R.drawable.ic_notif)
				.setLargeIcon(largeIcon)
				.setOngoing(true)
				.setContentIntent(piClick)
				.addAction(mPlayer.isPlaying() ? R.drawable.ic_action_pause : R.drawable.ic_action_play,
						getText(mPlayer.isPlaying() ? R.string.action_pause : R.string.action_play), piPause)
				.addAction(R.drawable.ic_action_stop, getText(R.string.action_stop), piStop)
				.addAction(R.drawable.ic_action_replay, getText(R.string.action_replay), piReplay)
				.build();

		mNotificationUpdateHandler.removeMessages(MSG_UPDATE_NOTIFICATION);
		if(mPlayer.isPlaying()) {
			// schedule notification update
			mNotificationUpdateHandler.sendEmptyMessageDelayed(MSG_UPDATE_NOTIFICATION,
					1050 - mPlayer.getCurrentPosition() % 1000); // update after position changes
		}

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
			mPlayer.reset();
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
		mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
		// ? mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
		// TODO: implement unmute
		//mAudioManager.setStreamMute(AudioManager.STREAM_RING, true);
		updateNotification();
	}
	public void playbackStop() {
		Log.i(TAG, "Stopping playback");
		mPlayer.pause();
		mPlayer.seekTo(0);
		updateNotification();
	}
	public void playbackPause() {
		Log.i(TAG, "Pausing playback");
		mPlayer.pause();
		updateNotification();
	}
	public void playbackRestart() {
		Log.i(TAG, "Restarting track");
		mPlayer.pause();
		mPlayer.seekTo(0);
		// TODO: configurable delay
		mPlayer.start();
		updateNotification();
	}

	//////////////////////
	// Helper functions //

	/**
	 * Convert milliseconds to "H:M:S" string
	 */
	public String positionToStr(int millis) {
		millis /= 1000;
		int s = millis % 60;
		millis /= 60;
		int m = millis % 60;
		int h = millis / 60;

		StringBuffer buf = new StringBuffer();
		if(h > 0) buf.append(String.format("%02d:", h));
		if(m > 0) buf.append(String.format("%02d:", m));
		buf.append(String.format("%02d", s));

		return buf.toString();
	}
}
