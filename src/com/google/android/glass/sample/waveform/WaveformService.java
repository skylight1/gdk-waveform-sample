package com.google.android.glass.sample.waveform;

import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.TimelineManager;


import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class WaveformService extends Service {
	
	

	private static final String LIVE_CARD_TAG = "WaveformSample";

	// private ChronometerDrawer mCallback;

	private TimelineManager mTimelineManager;
	private LiveCard mLiveCard;
	private WaveformRenderer mWaveRenderer;
	
	public WaveformService() {
	}

	 @Override
	    public void onCreate() {
	        super.onCreate();

	        mTimelineManager = TimelineManager.from(this);
	    }

	    @Override
	    public IBinder onBind(Intent intent) {
	        return null;
	    }

	    @Override
	    public int onStartCommand(Intent intent, int flags, int startId) {
	        if (mLiveCard == null) {
	            
	            mLiveCard = mTimelineManager.createLiveCard(LIVE_CARD_TAG);
	            mWaveRenderer = new WaveformRenderer(this);

	            mLiveCard.setDirectRenderingEnabled(true);
	            mLiveCard.getSurfaceHolder().addCallback(mWaveRenderer);

	            // Display the options menu when the live card is tapped.
	            Intent menuIntent = new Intent(this, MenuActivity.class);
	            //menuIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

	            mLiveCard.setAction(PendingIntent.getActivity(this, 0, menuIntent, 0));

	            mLiveCard.publish(LiveCard.PublishMode.REVEAL);
	        }
	        return START_STICKY;
	    }
	
	
	@Override
    public void onDestroy() {
        if (mLiveCard != null && mLiveCard.isPublished()) {
            Log.d(LIVE_CARD_TAG, "Unpublishing LiveCard");
            if (mWaveRenderer != null) {
                mLiveCard.getSurfaceHolder().removeCallback(mWaveRenderer);
            }
            mLiveCard.unpublish();
            mLiveCard = null;
        }
        super.onDestroy();
    }
}
