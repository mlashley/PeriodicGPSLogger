package com.mlashley.periodicgpslog;

//import java.io.BufferedWriter;
//import java.io.FileOutputStream;
//import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.Timer;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;


public class myGpsService extends Service implements OnSharedPreferenceChangeListener{
	private static int locUpdateIntervalHours = 0;
	private static int locUpdateIntervalMinutes = 30;
	private static int locUpdateInterval = 1800000;  
	private static float locUpdateDistance = (float) 10.0;

	public static ServiceUpdateUIListener UI_UPDATE_LISTENER;
	private static Activity MAIN_ACTIVITY;
	private static int lastStatus;
	private static DateFormat timestampFormat = new SimpleDateFormat(
			"yyyy/MM/dd,HH:mm:ss");

	//private static BufferedWriter buf;
	private static PrintStream buf;
	private Timer timer = new Timer();

	private static LocationManager lMgr;
	private static LocationListener locListener = new LocationListener() {
		public void onLocationChanged(Location loc) {
			String s = formatLocation(loc);
			Log.i("malcgps", s);
			if (UI_UPDATE_LISTENER != null) {
				UI_UPDATE_LISTENER.updateUI(s);
			}
			writeToLog("LU:" + s);
//			writeToLog("EX:" + loc.getExtras().describeContents());
		}

		public void onProviderDisabled(String arg0) {
			// TODO Auto-generated method stub

		}

		public void onProviderEnabled(String arg0) {
			// TODO Auto-generated method stub

		}

		public void onStatusChanged(String provider, int status, Bundle extra) {

			String s = null; // = "Location status changed for " + provider +
								// " ";
			switch (status) {
			case LocationProvider.AVAILABLE:
				s = "AVAILABLE";
				break;
			case LocationProvider.OUT_OF_SERVICE:
				s = "OUT OF SERVICE";
				break;
			case LocationProvider.TEMPORARILY_UNAVAILABLE:
				s = "TEMP UNAVAIL";
				break;
			}
			Log.i("malcgps", s);
			if (UI_UPDATE_LISTENER != null) {
				if (lastStatus != status) {
					UI_UPDATE_LISTENER.updateUI("Status " + s);
					writeToLog("LS:" + provider + ":" + s);
					if (status == LocationProvider.AVAILABLE)
						writeToLog("LUL:" + formatLocation(lMgr.getLastKnownLocation(LocationManager.GPS_PROVIDER)));
					lastStatus = status;
				}
			}
		}
	};

	public static void writeToLog(String s) {
		buf.println(s);
/*		try {
			// buf.write(DateFormat.getInstance().format(new Date()) + ":" + s +
			// "\n");
			buf.write(s + "\n");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			if (MAIN_ACTIVITY != null)
				Toast.makeText(MAIN_ACTIVITY, "Unable to write to file.",
						Toast.LENGTH_LONG).show();
			e.printStackTrace();
		}
*/
	}

	// XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
	// hooks into other activities
	// XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
	public static void setMainActivity(Activity activity) {
		MAIN_ACTIVITY = activity;
	}

	public static void setUpdateListener(ServiceUpdateUIListener l) {
		UI_UPDATE_LISTENER = l;
	}

	private static void setUpdateInterval() {
		int lastUpdateInterval = locUpdateInterval;
		setLocUpdateInterval(locUpdateIntervalHours,locUpdateIntervalMinutes);
		if (locUpdateInterval != lastUpdateInterval) {
			Log.i("malcgps", "Updating interval hours: " + locUpdateIntervalHours + " mins: "
					+ locUpdateIntervalMinutes + " = " + locUpdateInterval + " millis");
			if (lMgr != null && locListener != null) {
				lMgr.removeUpdates(locListener);
				lMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER,
						locUpdateInterval, locUpdateDistance, locListener);
			} else {
				Log.w("malcgps","Attempting to setUpdateInterval but found null lMgr or locListener");
			}
		}
	}

	public static int getUpdateIntervalHours() {
		return locUpdateIntervalHours;
	}

	public static int getUpdateIntervalMinutes() {
		return locUpdateIntervalMinutes;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		// init the service here
		_startService();
		if (MAIN_ACTIVITY != null)
			Toast.makeText(MAIN_ACTIVITY, "Service Started", 0).show();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		_shutdownService();
		if (MAIN_ACTIVITY != null)
			Toast.makeText(MAIN_ACTIVITY, "Service Stopped", 0).show();
	}

	// XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
	// service business logic
	// XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
	private void _startService() {
		// timer.scheduleAtFixedRate(
		// new TimerTask() {
		// public void run() {
		//		          
		// _getWeatherUpdate();
		// }
		// },
		// 0,
		// UPDATE_INTERVAL);
		// Log.i(getClass().getSimpleName(), "Timer started!!!");
		
		SharedPreferences mPrefs;
		
		timestampFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		try {
		
			//buf = new BufferedWriter(new FileWriter((getText(R.string.logfile)).toString(), true),1024);
		    buf = new PrintStream(openFileOutput((getText(R.string.logfile)).toString(), MODE_APPEND + MODE_WORLD_WRITEABLE + MODE_WORLD_READABLE));
			
			
		} catch (IOException e) {
			if (MAIN_ACTIVITY != null)
				Toast.makeText(MAIN_ACTIVITY, "Unable to open file for appending", Toast.LENGTH_LONG).show();
			e.printStackTrace();
			
		}
		;
		//try {
			buf.println("Logfile opened: "
					+ DateFormat.getInstance().format(new Date()));
/*		} catch (IOException e1) {
			if (MAIN_ACTIVITY != null)
				Toast.makeText(MAIN_ACTIVITY,
						"Unable to write header to file.", Toast.LENGTH_LONG)
						.show();
			e1.printStackTrace();
		}
*/
		mPrefs = getSharedPreferences("malcgpslog",MODE_PRIVATE);
		locUpdateIntervalHours = mPrefs.getInt("gpsHour", 0);
		locUpdateIntervalMinutes = mPrefs.getInt("gpsMinute", 15);
		setLocUpdateInterval(locUpdateIntervalHours,locUpdateIntervalMinutes); 

		mPrefs.registerOnSharedPreferenceChangeListener(this);
		
		lMgr = (LocationManager) this.getSystemService(LOCATION_SERVICE);
		lMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER,
				locUpdateInterval, locUpdateDistance, locListener);
		Log.i("malcgps","Service started " + Integer.toString(locUpdateIntervalHours) +":"+ Integer.toString(locUpdateIntervalMinutes));
	}
	
	private static void setLocUpdateInterval(int hours, int minutes) {
		locUpdateInterval = (locUpdateIntervalHours * 60 * 60 * 1000) + // Hours => Milliseconds
		locUpdateIntervalMinutes * 60 * 1000; // Minutes => Milliseconds
	}
	
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // Let's do something when a preference value changes
		int i;
		boolean updateRequired = false;
        if (key.equals("gpsHour")) {
        	i = sharedPreferences.getInt(key, 0);
        	if(i != locUpdateIntervalHours) { locUpdateIntervalHours = i ;  updateRequired = true; }
        } else if (key.equals("gpsMinute")) {
            i = sharedPreferences.getInt(key,0);
            if(i != locUpdateIntervalMinutes ) { locUpdateIntervalMinutes = i ; updateRequired = true; }
        }
        if(updateRequired) { 
        	setUpdateInterval();
            Log.i("malcgps","Preferences changed "+ Integer.toString(locUpdateIntervalHours) +":"+ Integer.toString(locUpdateIntervalMinutes));
        }
    }

	private static String formatLocation(Location loc) {
		return timestampFormat.format(loc.getTime()) + "," + loc.getLatitude()
				+ "," + loc.getLongitude() + ","
				+ (loc.hasAltitude() ? loc.getAltitude() : "NULL") + ","
				+ (loc.hasAccuracy() ? loc.getAccuracy() : "NULL") + ","
				+ (loc.hasSpeed() ? loc.getSpeed() : "NULL") + ","
				+ (loc.hasBearing() ? loc.getBearing() : "NULL");
	}

	private String gpsBabelUniCSVHeader() {
		return "date,time,lat,lon,ele,desc,speed,cour";
	}

	public static void flushLogFile() {
//		try {
			buf.flush();
/*		} catch (IOException e) {
			if (MAIN_ACTIVITY != null)
				Toast.makeText(MAIN_ACTIVITY, "Unable to flush logfile.",
						Toast.LENGTH_LONG).show();
			e.printStackTrace();
		}
*/
	}

	private void _shutdownService() {
		if (timer != null)
			timer.cancel();
		Log.i("malcgps", "_shutdownService called!!!");
		lMgr.removeUpdates(locListener);
//		try {
			buf.close();
/*		} catch (IOException e) {
			if (MAIN_ACTIVITY != null)
				Toast.makeText(MAIN_ACTIVITY, "Unable to close logfile.",
						Toast.LENGTH_LONG).show();
			e.printStackTrace();
		}
*/
	}

}
