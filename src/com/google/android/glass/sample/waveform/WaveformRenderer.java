package com.google.android.glass.sample.waveform;

import java.util.concurrent.TimeUnit;

import com.google.android.glass.timeline.DirectRenderingCallback;

import android.content.Context;
import android.graphics.Canvas;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

public class WaveformRenderer implements DirectRenderingCallback {

	/** The refresh rate, in frames per second, of the Live Card. */
	private static final int REFRESH_RATE_FPS = 33;

	/** The duration, in milliseconds, of one frame. */
	private static final long FRAME_TIME_MILLIS = TimeUnit.SECONDS.toMillis(1)
			/ REFRESH_RATE_FPS;

	private SurfaceHolder mHolder;
	// private RenderThread mRenderThread; --mayneed

	private int mSurfaceWidth;
	private int mSurfaceHeight;
	private boolean mRenderingPaused;

	private final FrameLayout mLayout;
	private final WaveformView mWaveformView;

	// The sampling rate for the audio recorder.
	private static final int SAMPLING_RATE = 44100;

	private TextView mDecibelView;

	//private RecordingThread mRecordingThread;
	private RenderThread mRenderThread;
	
	private Context mContext;

	public WaveformRenderer(Context context) {

		mContext = context;
		
		LayoutInflater inflater = LayoutInflater.from(context);

		mLayout = (FrameLayout) inflater
				.inflate(R.layout.layout_waveform, null);

		mWaveformView = (WaveformView) mLayout.findViewById(R.id.waveform_view);
		mDecibelView = (TextView) mLayout.findViewById(R.id.decibel_view);
		mDecibelView.setText("hey fucker");
		// Compute the minimum required audio buffer size and allocate the
		// buffer.
		
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// TODO Auto-generated method stub
		mSurfaceWidth = width;
		mSurfaceHeight = height;
		doLayout();
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		// mHolder = holder;

		mHolder = holder;
		
		mRenderThread = new RenderThread(mContext);
		mRenderThread.start();
		updateRenderingState();
		// mRecordingThread = new RecordingThread();
		// mRecordingThread.start();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		mHolder = null;
		updateRenderingState();
	}

	@Override
	public void renderingPaused(SurfaceHolder surfaceHolder, boolean paused) {
		mRenderingPaused = paused;
		Log.d("Rendering Paused", "paused");
		updateRenderingState();
	}

	/**
	 * Starts or stops rendering according to the {@link LiveCard}'s state.
	 */
	private void updateRenderingState() {
		boolean shouldRender = (mHolder != null) && !mRenderingPaused;
		boolean isRendering = (mRenderThread != null);

		if (shouldRender != isRendering) {
			if (shouldRender) {

				mRenderThread = new RenderThread(mContext);
				mRenderThread.start();
			} else {
				mRenderThread.quit();
				mRenderThread = null;

			}
		}
	}

	/**
	 * Requests that the views redo their layout. This must be called manually
	 * every time the tips view's text is updated because this layout doesn't
	 * exist in a GUI thread where those requests will be enqueued
	 * automatically.
	 */
	private void doLayout() {
		// Measure and update the layout so that it will take up the entire
		// surface space
		// when it is drawn.
		int measuredWidth = View.MeasureSpec.makeMeasureSpec(mSurfaceWidth,
				View.MeasureSpec.EXACTLY);
		int measuredHeight = View.MeasureSpec.makeMeasureSpec(mSurfaceHeight,
				View.MeasureSpec.EXACTLY);

		mLayout.measure(measuredWidth, measuredHeight);
		mLayout.layout(0, 0, mLayout.getMeasuredWidth(),
				mLayout.getMeasuredHeight());
	}

	/**
	 * A background thread that receives audio from the microphone and sends it
	 * to the waveform visualizing view.
	 */
	private synchronized void repaint() {
		Canvas canvas = null;

		try {
			canvas = mHolder.lockCanvas();
		} catch (RuntimeException e) {
			Log.d("o", "lockCanvas failed", e);
		}

		if (canvas != null) {

			// doLayout();
			mLayout.draw(canvas);

			try {
				mHolder.unlockCanvasAndPost(canvas);
			} catch (RuntimeException e) {
				Log.d("0", "unlockCanvasAndPost failed", e);
			}
		}
	}

	/**
	 * Redraws the Live Card in the background.
	 */
	private class RenderThread extends Thread {
		private boolean mShouldRun;

		/**
		 * Initializes the background rendering thread.
		 */
		public RenderThread(Context context) {
			mShouldRun = true;
			mBufferSize = AudioRecord.getMinBufferSize(SAMPLING_RATE,
					AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
			mAudioBuffer = new short[mBufferSize / 2];

			mDecibelFormat = context.getResources().getString(
					R.string.decibel_format);
		}

		/**
		 * Returns true if the rendering thread should continue to run.
		 * 
		 * @return true if the rendering thread should continue to run
		 */
		private synchronized boolean shouldRun() {
			return mShouldRun;
		}

		/**
		 * Requests that the rendering thread exit at the next opportunity.
		 */
		public synchronized void quit() {
			mShouldRun = false;
		}

		
		private int mBufferSize;
		private short[] mAudioBuffer;
		private String mDecibelFormat;
		
		@Override
		public void run() {

			android.os.Process
					.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);

			AudioRecord record = new AudioRecord(AudioSource.MIC,
					SAMPLING_RATE, AudioFormat.CHANNEL_IN_MONO,
					AudioFormat.ENCODING_PCM_16BIT, mBufferSize);
			record.startRecording();

			while (shouldRun()) {
				long frameStart = SystemClock.elapsedRealtime();

				record.read(mAudioBuffer, 0, mBufferSize / 2);
				mWaveformView.updateAudioData(mAudioBuffer);
				updateDecibelLevel();
				
				repaint();
				
				
				long frameLength = SystemClock.elapsedRealtime() - frameStart;

				long sleepTime = FRAME_TIME_MILLIS - frameLength;
				//mDecibelView.setText(String.valueOf(sleepTime));
				if (sleepTime > 0) {
					
					SystemClock.sleep(sleepTime);
					//Log.d("looper", "islooping");
					
				}

			}

		}

		private void updateDecibelLevel() {
			// Compute the root-mean-squared of the sound buffer and then apply
			// the formula for
			// computing the decibel level, 20 * log_10(rms). This is an
			// uncalibrated calculation
			// that assumes no noise in the samples; with 16-bit recording, it
			// can range from
			// -90 dB to 0 dB.
			double sum = 0;
			Log.d("running", "dbcalled");
			for (short rawSample : mAudioBuffer) {
				double sample = rawSample / 32768.0;
				sum += sample * sample;
			}

			double rms = Math.sqrt(sum / mAudioBuffer.length);
			final double db = 20 * Math.log10(rms);
			mDecibelView.setText(String.format(mDecibelFormat, db));
			// Update the text view on the main thread.
			
		}
	}

	

}
