package com.example.testBT;

import java.io.IOException;
import java.util.Locale;



import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.view.Menu;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Toast;

public class RecognizeActivity extends Activity implements TextureView.SurfaceTextureListener, OnInitListener {

	private Camera mCamera;
	private TextureView mTextureView;
	private DrawOnTop mDrawOnTop;
	public static int corrX1;
	public static int corrY1;
	public static int WW, HH;
	public static int WW_SCR, HH_SCR;
	int sumR, sumG, sumB;
	public int tempRGBData[];
	int currentMotion = 0;
	private TextToSpeech mTts;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mTextureView = new TextureView(this);
		mTextureView.setSurfaceTextureListener(this);
		WW_SCR = getApplicationContext().getResources().getDisplayMetrics().widthPixels;
		HH_SCR = getApplicationContext().getResources().getDisplayMetrics().heightPixels;
		// initialise tts
		mTts = new TextToSpeech(this, this);
		tempRGBData = new int[240 * 320];
	
		mDrawOnTop = new DrawOnTop(this, this);
		setContentView(mTextureView);
		addContentView(mDrawOnTop, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
	}

	public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
		mCamera = Camera.open();

		try {
			mCamera.setPreviewTexture(surface);
			Camera.Parameters parameters = mCamera.getParameters();
			parameters.setPreviewSize(320, 240);
			mCamera.setParameters(parameters);
			mCamera.startPreview();
			mCamera.setPreviewCallback(new Camera.PreviewCallback() {

				@Override
				public void onPreviewFrame(byte[] data, Camera camera) {
					if ((mDrawOnTop == null) )
						return;

					if (mDrawOnTop.mBitmap == null) {
						// Initialize the draw-on-top companion
						Camera.Parameters params = camera.getParameters();
						mDrawOnTop.mActualImageWidth = params.getPreviewSize().width;
						mDrawOnTop.mActualImageHeight = params.getPreviewSize().height;

						mDrawOnTop.mBitmap = Bitmap.createBitmap(mDrawOnTop.mActualImageWidth, mDrawOnTop.mActualImageHeight, Bitmap.Config.RGB_565);

						mDrawOnTop.mRGBData = new int[mDrawOnTop.mActualImageWidth * mDrawOnTop.mActualImageHeight];
						mDrawOnTop.pixels = new int[mDrawOnTop.mActualImageWidth * mDrawOnTop.mActualImageHeight];
						mDrawOnTop.mYUVData = new byte[data.length];
						// System.out.println("MY DATA LENGTH :" + data.length);
					}

					// Pass YUV data to draw-on-top companion
					System.arraycopy(data, 0, mDrawOnTop.mYUVData, 0, data.length);
					mDrawOnTop.invalidate();
				}
			});
		} catch (IOException ioe) {
			// Something bad happened
		}
	}

	public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
		// Ignored, Camera does all the work for us
	}

	public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
		try {
			mCamera.stopPreview();

		} catch (Exception e) {
		}
		try {
			mCamera.release();
		} catch (Exception e) {
		}
		return true;
	}

	public void onSurfaceTextureUpdated(SurfaceTexture surface) {
		// Invoked every time there's a new Camera preview frame

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}


	@Override
	public void onInit(int status) {
		if (status == TextToSpeech.SUCCESS) {
		
			System.out.println("Success");
			int result = mTts.setLanguage(Locale.US);
		
			if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
				// Lanuage data is missing or the language is not supported.
				System.out.println("Lang Not Supported");
			}
		} else {
			// Initialization failed.
			System.out.println("Failed");
		}
	}
	void speak(String str) {
		if (!mTts.isSpeaking()) {
			mTts.speak(str, TextToSpeech.QUEUE_FLUSH, null);
			Toast.makeText(getApplicationContext(), "" + str, Toast.LENGTH_LONG).show();
		}
	}

	@Override
	protected void onDestroy() {
		if (mTts != null) {
			mTts.stop();
			mTts.shutdown();
		}
		super.onDestroy();
	}

}

class DrawOnTop extends View {
	Bitmap mBitmap;
	Paint mPaintBlack;
	Paint mPaintYellow;
	Paint mPaintRed;
	Paint mPaintGreen;
	Paint mPaintBlue;
	byte[] mYUVData;
	public static int[] mRGBData;
	int[] pixels;
	int mActualImageWidth, mActualImageHeight;

	public static int x1, y1, init1;
	public static int sumX1, sumY1;
	public static RecognizeActivity parent;
	Context context;
	static int col;
	Canvas locCanvas;
	Rect src, dst;

	public static int sumR, sumG, sumB;
	int currentMotion = 0, sensitivity = 40, currentMotionThreshold = 20;

	public DrawOnTop(Context context, RecognizeActivity parent) {
		super(context);
		this.parent = parent;
		locCanvas = new Canvas();

		mPaintBlack = new Paint();
		mPaintBlack.setStyle(Paint.Style.FILL);
		mPaintBlack.setColor(Color.WHITE);
		mPaintBlack.setTextSize(25);
		context = parent;

		mBitmap = null;
		mYUVData = null;
		mRGBData = null;
		pixels = null;

		init1 = 0;
		src = dst = null;

	}
//Motion Detection
	public void detectMotion() {
		currentMotion = 0;
		for (int y = 0; y < mActualImageHeight; y++) {
			for (int x = 0; x < mActualImageWidth; x++) {
				int col1 = mRGBData[(y * x) + x];
				int col2 = pixels[(y * x) + x];
				if (col2 != -1) {
					int r1 = (col1 >> 16) & 0xff;
					int g1 = (col1 >> 8) & 0xff;
					int b1 = (col1 >> 0) & 0xff;
					int r2 = (col2 >> 16) & 0xff;
					int g2 = (col2 >> 8) & 0xff;
					int b2 = (col2 >> 0) & 0xff;

					// grayscale values...
					r1 = (r1 + g1 + b1) / 3;
					r2 = (r2 + g2 + b2) / 3;

					if (Math.abs(r1 - r2) >= sensitivity) {
						currentMotion++;
					}
				}
				pixels[(y * x) + x] = mRGBData[(y * x) + x];
			}
		}
		// percentage calculation...
		currentMotion = (int) (((float) currentMotion / (mActualImageHeight * mActualImageWidth)) * 100);
		System.out.println("CurrentMotion: " + currentMotion);
		if (currentMotion >= currentMotionThreshold) {
			System.out.println("MOTION DETECTED");
			Toast.makeText(parent, "Motion Detected", Toast.LENGTH_SHORT).show();
			parent.speak("Motion has been detected..");
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		// System.out.println("on draw");
		if (mBitmap != null) {
			// System.out.println("In here");
			locCanvas = canvas;
			int canvasWidth = canvas.getWidth();
			int canvasHeight = canvas.getHeight();
			RecognizeActivity.WW = mActualImageWidth;
			RecognizeActivity.HH = mActualImageHeight;
			// Convert from YUV to RGB
			mRGBData = convertYUV420_NV21toARGB8888(mYUVData, mActualImageWidth, mActualImageHeight);
			// process all algorithms to detect blobs
			// Draw bitmap
			mBitmap.setPixels(mRGBData, 0, mActualImageWidth, 0, 0, mActualImageWidth, mActualImageHeight);
			if (src == null || dst == null) {
				src = new Rect(0, 0, mBitmap.getWidth(), mBitmap.getHeight());
				dst = new Rect(0, 0, canvasWidth, canvasHeight);
			}
			canvas.drawBitmap(mBitmap, src, dst, null);
			
				detectMotion();
		}
		 // end if statement
		super.onDraw(canvas);
	} // end onDraw method

	public static int[] convertYUV420_NV21toARGB8888(byte[] data, int width, int height) {
		int size = width * height;
		int offset = size;
		int[] pixels = new int[size];
		int u, v, y1, y2, y3, y4;

		// i along Y and the final pixels
		// k along pixels U and V
		for (int i = 0, k = 0; i < size; i += 2, k += 2) {
			y1 = data[i] & 0xff;
			y2 = data[i + 1] & 0xff;
			y3 = data[width + i] & 0xff;
			y4 = data[width + i + 1] & 0xff;

			v = data[offset + k] & 0xff;
			u = data[offset + k + 1] & 0xff;
			v = v - 128;
			u = u - 128;

			pixels[i] = convertYUVtoARGB(y1, u, v);
			pixels[i + 1] = convertYUVtoARGB(y2, u, v);
			pixels[width + i] = convertYUVtoARGB(y3, u, v);
			pixels[width + i + 1] = convertYUVtoARGB(y4, u, v);

			if (i != 0 && (i + 2) % width == 0)
				i += width;
		}

		return pixels;
	}

	private static int convertYUVtoARGB(int y, int u, int v) {
		int r = y + (int) (1.772f * v);
		int g = y - (int) (0.344f * v + 0.714f * u);
		int b = y + (int) (1.402f * u);
		r = r > 255 ? 255 : r < 0 ? 0 : r;
		g = g > 255 ? 255 : g < 0 ? 0 : g;
		b = b > 255 ? 255 : b < 0 ? 0 : b;
		return 0xff000000 | (r << 16) | (g << 8) | b;
	}


	
}
