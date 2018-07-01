package com.example.testBT;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import LibPack.ValuesDB;
import android.app.Activity;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.os.Vibrator;

import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.testBT.CommonData.LongOperation;

public class MyBlueToothClientActivity extends Activity implements
TextToSpeech.OnInitListener {
	// Debugging
	private static final String TAG = "BluetoothChat";
	private static final boolean D = true;
	CommonData.LongOperation lo;

	// Message types sent from the BluetoothChatService Handler
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;
	public static final int TTS_INITIALISED = 50;
	Button btnSMS;
	boolean once = false;

	// Key names received from the BluetoothChatService Handler
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";
	private TextToSpeech mTts;
	

	// Intent request codes
	private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
	private static final int REQUEST_ENABLE_BT = 3;
	int cnt = 0;
	// Layout Views
	private TextView mTitle;
	float[] multiplier;

	// Button disIgnition, enbIgnition;
	Button start, stop;

	ProgressBar[] pgAdc;

	Dialog dialog;

	Context context;
	// String error = null, urlstr = "", ip;

	TextView[] showValues;

	int count = 0;
	// Name of the connected device
	private String mConnectedDeviceName = null;
	// Array adapter for the conversation thread
	private ArrayAdapter<String> mConversationArrayAdapter;
	// String buffer for outgoing messages
	private StringBuffer mOutStringBuffer;
	// Local Bluetooth adapter
	private BluetoothAdapter mBluetoothAdapter = null;
	// Member object for the chat services

	boolean mInitialised = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		try {
			Intent checkIntent = new Intent();
			checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
			startActivityForResult(checkIntent, 0);
			mTts = new TextToSpeech(this, this);
			if (D)
				Log.e(TAG, "+++ ON CREATE +++");

			// Set up the window layout
			requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
			setContentView(R.layout.activity_my_blue_tooth_client);
			getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
					R.layout.custom_title);
			if (Build.VERSION.SDK_INT >= 9) {
				StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
						.permitAll().build();
				StrictMode.setThreadPolicy(policy);
			}

			CommonData.vdb = new ValuesDB();

			// Set up the custom title
			mTitle = (TextView) findViewById(R.id.title_left_text);
			mTitle.setText(R.string.app_name);
			mTitle = (TextView) findViewById(R.id.title_right_text);

			context = this;
			showValues = new TextView[1];
			showValues[0] = (TextView) findViewById(R.id.valueView1);

			pgAdc = new ProgressBar[5];
			pgAdc[0] = (ProgressBar) findViewById(R.id.pgSensor1);

			CommonData.vdb.adc = new int[1];
			CommonData.vdb.prevAdc = new int[1];
			CommonData.vdb.opcode = 0;
			CommonData.vdb.opData = 0;
			CommonData.vdb.operand = 0;
			for (int i = 0; i < 1; i++) {
				pgAdc[i].setMax(255);
				CommonData.vdb.thr[i] = 128;
				CommonData.vdb.adc[i] = 0;
				CommonData.vdb.prevAdc[i] = 0;
			}

			start = (Button) findViewById(R.id.btnStart);
			stop = (Button) findViewById(R.id.btnStop);

			mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

			// If the adapter is null, then Bluetooth is not supported
			if (mBluetoothAdapter == null) {
				Toast.makeText(this, "Bluetooth is not available",
						Toast.LENGTH_LONG).show();
				finish();
				return;
			}

			Intent serverIntent = new Intent(this, DeviceListActivity.class);
			startActivityForResult(serverIntent,
					REQUEST_CONNECT_DEVICE_INSECURE);

		} catch (Exception e) {
			// TODO: handle exception
			Toast.makeText(this, "onCreate() Ended : " + e.getMessage(),
					Toast.LENGTH_SHORT).show();
		}

	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		// Stop the Bluetooth chat services
		if (CommonData.mChatService != null)
			CommonData.mChatService.stop();
		if (D)
			Log.e(TAG, "--- ON DESTROY ---");
	}

	@Override
	public void onStart() {
		super.onStart();
		// Toast.makeText(this, "onStart() Started", Toast.LENGTH_SHORT).show();
		if (D)
			Log.e(TAG, "++ ON START ++");

		// If BT is not on, request that it be enabled.
		// setupChat() will then be called during onActivityResult
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
			// Otherwise, setup the chat session
		} else {
			if (CommonData.mChatService == null)
				setupChat();
		}
		// Toast.makeText(this, "onStart() Ended", Toast.LENGTH_SHORT).show();
	}

	private void setupChat() {
		Log.d(TAG, "setupChat()");
		CommonData.context = this;

		// Initialize the array adapter for the conversation thread
		mConversationArrayAdapter = new ArrayAdapter<String>(this,
				R.layout.message);

		dialog = new Dialog(MyBlueToothClientActivity.this);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(R.layout.customdialog);
		// for reducing size of custom dialog
		Display display = ((WindowManager) getSystemService(context.WINDOW_SERVICE))
				.getDefaultDisplay();
		dialog.getWindow().setLayout(120, 120);

		start.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				CommonData.valueRequested = false;
				CommonData.vdb.opcode = 73;
				CommonData.vdb.currentChannel = 0;
				CommonData.vdb.operand = CommonData.vdb.currentChannel;
				CommonData.running = true;
				lo = new LongOperation();
				lo.execute("");

			}
		});

		stop.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				CommonData.running = false;
			}
		});

		// Initialize the BluetoothChatService to perform bluetooth connections
		CommonData.mChatService = new BluetoothClientService(this, mHandler, 1);

		// Initialize the buffer for outgoing messages
		mOutStringBuffer = new StringBuffer("");
	}

	void thresholdDialog(int threshold) {
		final TextView tv = (TextView) dialog.findViewById(R.id.textView1);
		tv.setText("" + threshold);
		dialog.show();

	}

	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		CommonData.cancel = true;
		super.onBackPressed();
	}

	// The Handler that gets information back from the BluetoothChatService
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case TTS_INITIALISED:

				break;
			case MESSAGE_STATE_CHANGE:
				if (D)
					Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
				switch (msg.arg1) {
				case BluetoothClientService.STATE_CONNECTED:
					mTitle.setText(R.string.title_connected_to);
					mTitle.append(mConnectedDeviceName);
					mConversationArrayAdapter.clear();
					break;
				case BluetoothClientService.STATE_CONNECTING:
					mTitle.setText(R.string.title_connecting);
					break;
				case BluetoothClientService.STATE_LISTEN:
				case BluetoothClientService.STATE_NONE:
					mTitle.setText(R.string.title_not_connected);
					break;
				}
				break;
			case MESSAGE_WRITE:
				byte[] writeBuf = (byte[]) msg.obj;
				// construct a string from the buffer

				String writeMessage = new String(writeBuf);
				mConversationArrayAdapter.add("Me:  " + writeMessage);
				break;
			case MESSAGE_READ:
				byte[] readBuf = (byte[]) msg.obj;
				// construct a string from the valid bytes in the buffer
				if (CommonData.valueRequested) {
					int inData = ((int) (readBuf[0] & 0xff));
					CommonData.valueRequested = false;
					if (CommonData.vdb.opcode == 73) {
						CommonData.vdb.adc[CommonData.vdb.currentChannel] = inData;
						if (CommonData.vdb.adc[CommonData.vdb.currentChannel] < 60) {
						
							if(mTts.isSpeaking())
							{
							
							Vibrator vibrator = (Vibrator) getApplication()
									.getSystemService(Service.VIBRATOR_SERVICE);
							vibrator.vibrate(1000);
							}else
							{
								mTts.speak("Intrusion Detected", TextToSpeech.QUEUE_FLUSH, null);
							}
						}
						pgAdc[CommonData.vdb.currentChannel]
								.setProgress(CommonData.vdb.adc[CommonData.vdb.currentChannel]);
						showValues[CommonData.vdb.currentChannel]
								.setText(" "
										+ CommonData.vdb.adc[CommonData.vdb.currentChannel]);
						CommonData.vdb.currentChannel++;
						cnt++;
						if (CommonData.vdb.currentChannel == 1) {
							CommonData.vdb.currentChannel = 0;
							CommonData.vdb.opcode = 34;

						}
						if (CommonData.vdb.opcode == 34) {
							CommonData.vdb.operand = CommonData.vdb.opData;
						} else {
							CommonData.vdb.operand = CommonData.vdb.currentChannel;
						}
					}
				}
				break;
			case MESSAGE_DEVICE_NAME:
				// save the connected device's name
				mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
				Toast.makeText(getApplicationContext(),
						"Connected to " + mConnectedDeviceName,
						Toast.LENGTH_SHORT).show();
				byte[] b = new byte[] { 65 };
				CommonData.mChatService.write(b);
				break;
			case MESSAGE_TOAST:
				Toast.makeText(getApplicationContext(),
						msg.getData().getString(TOAST), Toast.LENGTH_SHORT)
						.show();
				break;
			}
		}
	};

	private void connectDevice(Intent data, boolean secure) {
		// Get the device MAC address
		String address = data.getExtras().getString(
				DeviceListActivity.EXTRA_DEVICE_ADDRESS);

		// Get the BLuetoothDevice object
		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);

		// Attempt to connect to the device
		CommonData.mChatService.connect(device, secure);
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (D)
			Log.d(TAG, "onActivityResult " + resultCode);

		switch (requestCode) {

		case REQUEST_CONNECT_DEVICE_INSECURE:
			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {
				Toast.makeText(this, "COnnecting", Toast.LENGTH_SHORT).show();
				connectDevice(data, false);

			}
			break;
		case REQUEST_ENABLE_BT:
			// When the request to enable Bluetooth returns
			if (resultCode == Activity.RESULT_OK) {
				// Bluetooth is now enabled, so set up a chat session
				setupChat();

			} else {
				// User did not enable Bluetooth or an error occured
				Log.d(TAG, "BT not enabled");
				Toast.makeText(this, R.string.bt_not_enabled_leaving,
						Toast.LENGTH_SHORT).show();
				finish();
			}
		}
	}

	@Override
	public void onInit(int status) {
		// TODO Auto-generated method stub
		if (status == TextToSpeech.SUCCESS) {

			int result = mTts.setLanguage(Locale.US);

			if (result == TextToSpeech.LANG_MISSING_DATA
					|| result == TextToSpeech.LANG_NOT_SUPPORTED) {
				Log.e("TTS", "Language is not supported");
			} else {
				
			}

		} else {
			Log.e("TTS", "Initilization Failed");
		}
		
	}
}
