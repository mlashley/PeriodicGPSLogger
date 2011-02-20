package com.mlashley.periodicgpslog;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

public class PeriodicGpsLog extends Activity {

	static boolean serviceRunning = false;
	static final int TIME_DIALOG_ID = 0;
	private int mHour = 0;
	private int mMinute = 15;
	SharedPreferences mPrefs;
//	private final DecimalFormat sevenSigDigits = new DecimalFormat("0.#######");
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	
		setContentView(R.layout.main);
		mPrefs = getSharedPreferences("malcgpslog",MODE_PRIVATE);
		mHour = mPrefs.getInt("gpsHour", 0);
		mMinute = mPrefs.getInt("gpsMinute", 15);
		
		myGpsService.setUpdateListener(new ServiceUpdateUIListener() {
			public void updateUI(final String newData) {
				// make sure this runs in the UI thread... since it's messing with views...
				PeriodicGpsLog.this.runOnUiThread(
						new Runnable() {
							public void run() {
//								Toast.makeText(PeriodicGpsLog.this, "Data updated :" + newData, Toast.LENGTH_SHORT).show();
								TextView tv = (TextView) findViewById(R.id.lastUpdate);
//								tv.setText(newData.replace(',', '\n'));
								final String[] sarr = newData.split(",");
								String output;
								if(sarr.length >5) {
									output = sarr[0] + " " + sarr[1] + "\n" // Date Time
									+ sarr[2].substring(0,8) + "," + sarr[3].substring(0,8) + "\n" // lat long
									+ sarr[5] + "\n"; // accuracy
								} else {
									output = newData;
								}
								tv.setText(output); 
							}
						});
			}
		});

		myGpsService.setMainActivity(this); 
		final Intent svc = new Intent(this, myGpsService.class); 
		final Button startbutton = (Button) findViewById(R.id.StartButton);
		final Button stopbutton = (Button) findViewById(R.id.StopButton);
		final Button logbutton = (Button) findViewById(R.id.ReadLogFileButton);
		final Button setintervalbutton = (Button) findViewById(R.id.SetIntervalButton);
		ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		List<RunningServiceInfo> svcInfo = am.getRunningServices(15);				
		((TextView) findViewById(R.id.svcStatus)).setText(getText(R.string.svcStopped));
		for(RunningServiceInfo ele : svcInfo) {
			if(ele.process.equals("com.mlashley.periodicgpslog")) {
				serviceRunning = true;
				mHour = myGpsService.getUpdateIntervalHours();
				mMinute = myGpsService.getUpdateIntervalMinutes();
				((TextView) findViewById(R.id.svcStatus)).setText(getText(R.string.svcRunning) + " " + Integer.toString(mHour) + ":" + pad(mMinute));
			}
		}
		startbutton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				startService(svc);
				serviceRunning = true;
//				myGpsService.setUpdateInterval(mHour, mMinute);
				((TextView) findViewById(R.id.svcStatus)).setText(getText(R.string.svcRunning) + " " + Integer.toString(mHour) + ":" + pad(mMinute));
			}
		});
		stopbutton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				stopService(svc);
				serviceRunning = false;
				((TextView) findViewById(R.id.svcStatus)).setText(getText(R.string.svcStopped));
			}
		});
		logbutton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String s;
			
				BufferedReader buf = null;
				if(serviceRunning) myGpsService.flushLogFile(); // Flush first - in case new file...
				try {
					//buf = new BufferedReader( new FileReader(getFilesDir().toString() + "/" + getText(R.string.logfile).toString()));
					buf = new BufferedReader( new FileReader(getFileStreamPath(getText(R.string.logfile).toString())));
				} catch (FileNotFoundException e) {
					Toast.makeText(PeriodicGpsLog.this, "ERROR: File not found", Toast.LENGTH_SHORT).show();
					e.printStackTrace();
					return; // We are done.
				}
				TextView tv = (TextView) findViewById(R.id.LogFileTextView);
				tv.setText(null);

				try {
					while((s = buf.readLine()) != null) {
						if (s.startsWith("LU:")) tv.append(s + "\n");
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					Toast.makeText(PeriodicGpsLog.this, "ERROR: Caught IO Exception reading file", Toast.LENGTH_SHORT).show();
					e.printStackTrace();
				}
			}
		});
		setintervalbutton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showDialog(TIME_DIALOG_ID);
			}
		});
	}
	@Override
	public void onDestroy() {
		super.onDestroy();
		myGpsService.setMainActivity(null);
		myGpsService.setUpdateListener(null);
	}
	@Override
	protected Dialog onCreateDialog(int id) {
	    switch (id) {
	    case TIME_DIALOG_ID:
	        return new TimePickerDialog(this,
	                mTimeSetListener, mHour, mMinute, true);
	    }
	    return null;
	}
	// the callback received when the user "sets" the time in the dialog 
	private TimePickerDialog.OnTimeSetListener mTimeSetListener = new TimePickerDialog.OnTimeSetListener() { 
		public void onTimeSet(TimePicker view, int hourOfDay, int minute) { 
			mHour = hourOfDay; 
			mMinute = minute; 
			SharedPreferences.Editor ed = mPrefs.edit();
		    ed.putInt("gpsHour", mHour );
		    ed.putInt("gpsMinute", mMinute);
		    ed.commit(); 
//			if(serviceRunning) myGpsService.setUpdateInterval(mHour, mMinute);
		} 
	};
	
	private String pad (int i) {
		if(i <10) return "0" + Integer.toString(i);
		return Integer.toString(i);
	}


}
