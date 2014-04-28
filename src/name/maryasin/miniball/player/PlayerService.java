package name.maryasin.miniball.player;

import name.maryasin.miniball.DanceListActivity;
import name.maryasin.miniball.R;
import name.maryasin.miniball.data.DataManager;
import name.maryasin.miniball.data.Material;

import android.annotation.TargetApi;
import android.app.*;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.RemoteControlClient;
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
	public static final String ACTION_PLAYPAUSE = "name.maryasin.miniball.action.PLAYPAUSE";
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
	private RemoteControlClient mRemote;
	private boolean isTrackLoaded = false;

	private Handler mNotificationUpdateHandler = new Handler(this);

	///////////////////////
	// Service callbacks //

	@Override
	public void onCreate() {
		sInstance = this;
		notificationMgr = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

		mPlayer = createMediaPlayer();
		mAudioManager = (AudioManager)getSystemService(AUDIO_SERVICE);

		registerRemote();

		// TODO: restore saved track&position

		// start service as foreground, and show persistent notification
		startForeground(NOTIFICATION_ID, createNotification());
	}

	@Override
	public void onDestroy() {
		// hide persistent notification
		notificationMgr.cancel(NOTIFICATION_ID);
		// and to really remove notification created by startForeground:
		stopForeground(true);

		unregisterRemote();

		if(mPlayer != null) {
			// TODO: save position?
			playbackStop(); // release any audio locks
			mPlayer.release();
			mPlayer = null;
		}

		// Debug, FIXME
		Toast.makeText(this, "PlayerService stopped", Toast.LENGTH_SHORT).show();
	}

	private void handleIntent(Intent intent) {
		if(intent == null)
			return;
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
			return;
		}

		Log.d(TAG, "Track loaded? "+isTrackLoaded);
		if(!isTrackLoaded)
			return; // ignore all these intents if no track loaded

		if(ACTION_PLAY.equals(action)) {
			playbackStart();
		} else if(ACTION_PAUSE.equals(action)) {
			playbackPause();
		} else if(ACTION_PLAYPAUSE.equals(action)) {
			if(mPlayer.isPlaying())
				playbackPause();
			else
				playbackStart();
		} else if(ACTION_STOP.equals(action)) {
			playbackStop();
		} else if(ACTION_REPLAY.equals(action)) {
			playbackRestart();
		} else {
			Log.w(TAG, "Unknown action: "+action);
		}
	}
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "Received start id "+startId+": "+intent);
		handleIntent(intent);

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
		updateNotification();
		// TODO: go to next track if needed
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
				playbackStart(); // FIXME: only if was not paused by user
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
				new Intent(ACTION_PLAYPAUSE, null, this, PlayerService.class), 0);
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
		NotificationCompat.Builder b = new NotificationCompat.Builder(this)
				.setContentTitle(danceName)
				.setContentText(trackName)
				.setContentInfo(duration)
				.setSmallIcon(R.drawable.ic_notif)
				.setLargeIcon(largeIcon)
				.setOngoing(true)
				.setContentIntent(piClick);
		if(ct != null)
			b.setProgress(mPlayer.getDuration(), mPlayer.getCurrentPosition(), false)
					.addAction(mPlayer.isPlaying() ? R.drawable.ic_action_pause : R.drawable.ic_action_play,
							getText(mPlayer.isPlaying() ? R.string.action_pause : R.string.action_play), piPause)
					.addAction(R.drawable.ic_action_stop, getText(R.string.action_stop), piStop)
					.addAction(R.drawable.ic_action_replay, getText(R.string.action_replay), piReplay);
		Notification n = b.build();

		mNotificationUpdateHandler.removeMessages(MSG_UPDATE_NOTIFICATION);
		if(mPlayer.isPlaying()) {
			// schedule notification update
			mNotificationUpdateHandler.sendEmptyMessageDelayed(MSG_UPDATE_NOTIFICATION,
					1050 - mPlayer.getCurrentPosition() % 1000); // update after position changes
		}

		if(mRemote != null) {
			int state = mPlayer.isPlaying() ?
					RemoteControlClient.PLAYSTATE_PLAYING :
					mPlayer.getCurrentPosition() > 0 ?
							RemoteControlClient.PLAYSTATE_PAUSED :
							RemoteControlClient.PLAYSTATE_STOPPED;
			updateRemotePlayState(state,
					mPlayer.getCurrentPosition());
		}
		broadcastStockUpdatePlaystate();

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

	@TargetApi(14)
	private void registerRemote() {
		MediaButtonsReceiver.register(this);

		Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
		intent.setComponent(new ComponentName(getPackageName(), MediaButtonsReceiver.class.getName()));
		PendingIntent pi = PendingIntent.getBroadcast(this, 0, intent, 0);
		RemoteControlClient r = new RemoteControlClient(pi);
		r.setTransportControlFlags(RemoteControlClient.FLAG_KEY_MEDIA_NEXT
				| RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS
				| RemoteControlClient.FLAG_KEY_MEDIA_PAUSE
				| RemoteControlClient.FLAG_KEY_MEDIA_PLAY
				| RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE);
		mAudioManager.registerRemoteControlClient(r);
		mRemote = r;
	}
	@TargetApi(14)
	private void unregisterRemote() {
		mAudioManager.unregisterRemoteControlClient(mRemote);
		mRemote = null;
		MediaButtonsReceiver.unregister(this);
	}

	@TargetApi(14)
	private void updateRemote(Material track) {
		Log.d(TAG, "Updating media metadata with " + track);
		RemoteControlClient.MetadataEditor e = mRemote.editMetadata(true);

		e.putString(MediaMetadataRetriever.METADATA_KEY_ALBUM, track.dance.getName());
		e.putString(MediaMetadataRetriever.METADATA_KEY_TITLE, track.name);
		if(track.hasImage()) {
			Bitmap b = BitmapFactory.decodeFile(track.getImageFile().getPath());
			e.putBitmap(RemoteControlClient.MetadataEditor.BITMAP_KEY_ARTWORK, b);
		}

		e.apply();
	}
	@TargetApi(18)
	private void updateRemotePlayState(int newState, long time) {
		if(mRemote != null) {
			if (Build.VERSION.SDK_INT >= 18)
				mRemote.setPlaybackState(newState, time, 1);
			else
				mRemote.setPlaybackState(newState);
		}
	}

	private void broadcastStockUpdateMeta(Material track) {
		if(track == null)
			return;
		Intent intent = new Intent("com.android.music.metachanged");
		intent.putExtra("playing", mPlayer.isPlaying());
		intent.putExtra("track", track.name);
		intent.putExtra("album", track.dance.getName());
		intent.putExtra("duration", mPlayer.getDuration());
		sendBroadcast(intent);
	}
	private void broadcastStockUpdatePlaystate() {
		Intent intent = new Intent("com.android.music.playstatechanged");
		intent.putExtra("playing", mPlayer.isPlaying());
		intent.putExtra("duration", mPlayer.getDuration());
		sendBroadcast(intent);
	}
	private void broadcastStockPlaystateCompleted() {
		Intent intent = new Intent("com.android.music.playbackcomplete");
		sendBroadcast(intent);
	}

	////////////////
	// Operations //

	public void playTrack(Material track) {
		Log.d(TAG, "playTrack " + track);
		try {
			mPlayer.reset();
			mPlayer.setDataSource(track.getAudioFile().getPath());
			mPlayer.prepare();
			isTrackLoaded = true;
			playbackStart();
			// And now update remote control info
			if(mRemote == null) {
				Log.w(TAG, "No remote control client registered!");
			} else {
				updateRemote(track);
			}
			broadcastStockUpdateMeta(track);
		} catch(IOException ex) {
			Log.e(TAG, ""+ex);
		}
	}
	public void playbackStart() {
		Log.i(TAG, "Starting playback");
		int focusresult = mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
		if(focusresult != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
			Toast.makeText(this, "Audio focus request failed!", Toast.LENGTH_SHORT);
			return;
		}
		// ? mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
		mAudioManager.setStreamSolo(AudioManager.STREAM_MUSIC, true);
		mPlayer.start();
		updateNotification();
	}
	private void playbackHalt(boolean stop) {
		Log.i(TAG, "Halting playback");
		mPlayer.pause();
		if(stop) {
			mPlayer.seekTo(0);
		}
		mAudioManager.setStreamSolo(AudioManager.STREAM_MUSIC, false);
		mAudioManager.abandonAudioFocus(this);
		updateNotification();
		if(stop)
			broadcastStockPlaystateCompleted();
	}
	public void playbackStop() {
		playbackHalt(true);
	}
	public void playbackPause() {
		playbackHalt(false);
	}
	public void playbackRestart() {
		Log.i(TAG, "Restarting track");
		boolean wasPlaying = mPlayer.isPlaying();
		mPlayer.pause();
		mPlayer.seekTo(0);
		// TODO: configurable delay
		if(wasPlaying)
			mPlayer.start();
		else
			playbackStart(); // with all needed stream initializations
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
