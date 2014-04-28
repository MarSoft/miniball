package name.maryasin.miniball.player;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.util.Log;

public class MediaButtonsReceiver extends BroadcastReceiver {
	private static final String TAG = "MediaButtonsReceiver";

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
		Log.d(TAG, "Received button event: " + intent);
	}
}
