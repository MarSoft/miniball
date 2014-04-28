package name.maryasin.miniball.player;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.util.Log;
import android.view.KeyEvent;

public class MediaButtonsReceiver extends BroadcastReceiver {
	private static final String TAG = "MediaButtonsReceiver";

	private static boolean handleKey(Context ctx, KeyEvent evt) {
		Log.d(TAG, "Processing key: "+evt);

		if(evt == null)
			return false;

		String act = null;

		switch(evt.getKeyCode()) {
			case KeyEvent.KEYCODE_HEADSETHOOK:
			case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
				act = PlayerService.ACTION_PLAYPAUSE;
				break;
			case KeyEvent.KEYCODE_MEDIA_NEXT:
				//TODO act = PlayerService.ACTION_NEXT;
				break;
			case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
				act = PlayerService.ACTION_REPLAY;
				break;
			case KeyEvent.KEYCODE_MEDIA_PLAY:
				act = PlayerService.ACTION_PLAY;
				break;
			case KeyEvent.KEYCODE_MEDIA_PAUSE:
				act = PlayerService.ACTION_PAUSE;
				break;
			default:
				return false; // don't consume
		}
		if(act == null) // just for ...
			return false;
		if(evt.getAction() != KeyEvent.ACTION_DOWN)
			return true; // consume but not process

		Intent intent = new Intent(ctx, PlayerService.class);
		intent.setAction(act);
		ctx.startService(intent);
		return true;
	}

	public static void register(Context ctx) {
		AudioManager am = (AudioManager)ctx.getSystemService(Context.AUDIO_SERVICE);
		ComponentName receiver = new ComponentName(ctx.getPackageName(), MediaButtonsReceiver.class.getName());
		am.registerMediaButtonEventReceiver(receiver);
	}
	public static void unregister(Context ctx) {
		AudioManager am = (AudioManager)ctx.getSystemService(Context.AUDIO_SERVICE);
		ComponentName receiver = new ComponentName(ctx.getPackageName(), MediaButtonsReceiver.class.getName());
		am.unregisterMediaButtonEventReceiver(receiver);
	}

	@Override
	public void onReceive(Context ctx, Intent intent) {
		Log.d(TAG, "Received event: " + intent);
		if(intent != null && Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
			// yep, everything is alright, this is our intent
			KeyEvent evt = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
			if(handleKey(ctx, evt) && isOrderedBroadcast())
				abortBroadcast();
		}
	}
}
