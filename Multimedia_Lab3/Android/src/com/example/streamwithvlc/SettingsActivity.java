package com.example.streamwithvlc;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class SettingsActivity extends Activity {

	private EditText hostNameField;
	private EditText portField;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
		setInterface();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	
	public void setInterface(){
		hostNameField = (EditText)findViewById(R.id.ServerhostNameField);
		portField = (EditText)findViewById(R.id.serverPortField);
		hostNameField.setText(MainActivity.HOSTNAME);
		portField.setText(""+MainActivity.PORT);
		
	}
	
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.save_settings_button:
			if(!hostNameField.getText().toString().equalsIgnoreCase("")){
				MainActivity.HOSTNAME = hostNameField.getText().toString();
				MainActivity.PORT = Integer.parseInt(portField.getText().toString());
				finish();
			}else{
				Toast.makeText(this, "One or more fields are empty", Toast.LENGTH_SHORT).show();
			}
			break;
		case R.id.settings_back_button:
			finish();
			break;
		}
	}
		

}
