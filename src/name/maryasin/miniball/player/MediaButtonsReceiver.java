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

		switch(evt.getKeyCode()) {
			case KeyEvent.KEYCODE_HEADSETHOOK:
		}
		return false;
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
