package com.example.testBT;



import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends Activity implements OnInitListener {

	Button b;
	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		b = (Button) findViewById(R.id.btnProceed);
	
		b.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				Intent ii = new Intent(arg0.getContext(),
						MyBlueToothClientActivity.class);
				startActivity(ii);
			}
		});
	}

	@Override
	public void onInit(int status) {
		// TODO Auto-generated method stub
		// mTts.speak("Welcome", TextToSpeech.QUEUE_FLUSH, null);

	}
}
