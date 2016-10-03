package arena.androidapp;

//This program is for smart watch (Galaxy Gear 1st generation)

//The camera supported resolution:  1280x720,  640x640,  640x480,  480x480, 320x320,  320x240

import android.app.Activity;
import android.content.*;
import android.content.pm.*;
import android.os.Bundle;
import android.os.Handler;
import android.os.Environment;
import android.widget.CheckBox;
import android.widget.RadioGroup;
import android.widget.RadioButton;
import android.widget.Button;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.Toast;
import android.view.KeyEvent;
import android.view.View;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.GeomagneticField;
import android.util.*;
import android.content.BroadcastReceiver;
import android.location.*;
import android.app.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Calendar;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.Menu;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.media.AudioFormat;
import android.media.AudioRecord;

public class GetSensorsActivity extends Activity implements SensorEventListener, Callback {
	private static final String TAG = "GetSensorsActivity";
	private static boolean m_blnRecordStatus = false; // true: Recording; false: Stopped 
	private static long m_lSensorDataFileInterval = 0; /* 0: Always record in one file;
														 Other value: in milliseconds, split file accordingly
														 */
	private static final int DATA_TYPE_SENSOR = 1;
	
	/* Interval to record to a new data file */
	private String[] m_arrsFileIntervalOptions = {
										"None",
										"10 s",
										"30 s",
										"1 min", 
										"3 mins", 
										"5 mins",
										"10 mins",
										"20 mins",
										"30 mins",
										"60 mins",
										"90 mins",
										"2 hours",
										"5 hours",
										"12 hours",
										"24 hours"};

	private long[] m_arrlFileIntervalValues = {
									0,
									10*1000,
									30*1000,
									60*1000,
									180*1000,
									300*1000,
									600*1000,
									1200*1000,
									1800*1000,
									3600*1000,
									5400*1000,
									3600*2*1000,
									3600*5*1000,
									3600*12*1000,
									3600*24*1000};
	
	private SensorManager m_smGSA = null;
	private CheckBox m_chkGyro,m_chkAccl,m_chkVideo,m_chkAudio;
	private RadioGroup m_rdgpSensor,m_rdgpSensorMode;
	private RadioButton m_rdSensorGyro,m_rdSensorAccl;
	private RadioButton m_rdSensorModeGame,m_rdSensorModeUI;
	private Spinner m_spnSelectFileInterval;
	
	private List<String> m_lstInterval = new ArrayList<String>();
	private ArrayAdapter<String> m_adpInterval;
	
	/* Sensor is available or not */
	private static boolean m_blnGyroPresent = false;
	private static boolean m_blnAcclPresent = false;
	
	/* Sensor is selected or not */
	private static boolean m_blnGyroEnabled = false;
	private static boolean m_blnAcclEnabled = false;
	private static boolean m_blnVideoEnabled = false;
	private static boolean m_blnAudioEnabled = false;
	
	
	//private static boolean m_blnGravityEnabled = false; //Don't Record gravity 
	private static boolean m_blnGravityEnabled = true;  

	//(A three dimensional vector indicating the direction and
	// magnitude of gravity)
			
	/* Default delay mode for sensors */
//	private static int m_nGyroMode = SensorManager.SENSOR_DELAY_NORMAL;
//	private static int m_nAcclMode = SensorManager.SENSOR_DELAY_NORMAL;
//	private static int m_nGravityMode = SensorManager.SENSOR_DELAY_NORMAL;
	private static int m_nGyroMode = SensorManager.SENSOR_DELAY_GAME;
	private static int m_nAcclMode = SensorManager.SENSOR_DELAY_GAME;
	
	private static int m_nGravityMode = SensorManager.SENSOR_DELAY_GAME;
	
	private Button m_btnRecord;
	private TextView m_tvShowInfo;
	private TextView m_tvShowRecord;
	private String m_sRecordFile; //Sensor record file
	private String m_sFullPathFile; //Sensor record full pathfile
	private FileWriter m_fwSensorRecord = null;
	private GetSensorsActivity m_actHome = this;
	private Date m_dtFileStart;	//The time sensor data file is created (for each data file intervally)
	private ResolveInfo m_riHome;
		
	private float[] m_arrfAcclValues = new float[3];	 // Store accelerometer values

	private float[] m_arrfGravityValues = new float[3];
	private float[] m_arrfLinearAcceleration  = new float[3];
	
	private AlarmReceiver m_receiverAlarm = null;
	
	static String m_sGyro = ",,,";
	static String m_sAccl = ",,,";
	
	static String m_sGravity = ",,,";
	
	static String m_sSensorAccuracy=",,";

	private String m_sRecordFullPathFile = "";
	
	///////////////
	private SurfaceHolder m_surfaceHolder;
    private SurfaceView m_surfaceView;
    public MediaRecorder m_videoRec = null;
    private Camera m_camera = null;	
    
    private MediaRecorder m_audioRec = null;
    
    private int m_nBufferSize = 0;
    private AudioRecord m_audioRecorder = null;
    private Thread m_recordingThread = null;
	///////////////
	
    
	private void stopRecord() {
		//Stop record. Close Sensor Record File
		if (m_fwSensorRecord != null) {
			try {
				m_fwSensorRecord.close();
				m_fwSensorRecord = null;
			} catch (IOException e) {
				//
			}
		}
				
		// Enable sensor setting when recording stops
		enableSensorSetting(true);
        m_tvShowInfo.setText(getString(R.string.defaultinfo));        			
        m_btnRecord.setText(getString(R.string.btn_start));
        m_tvShowRecord.setText("");
        resetValues();
		m_blnRecordStatus = false;
		
	}
	
	private void resetValues() {
		m_sGyro = ",,,";
		m_sAccl = ",,,";
	}
	
	
	private class AlarmReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
 			if (m_blnAudioEnabled) {
 				stopAudioRecording();
 			}
 			
 			if (m_blnVideoEnabled) {
 				stopVideoRecording();
 			}
			
			stopTimer();
			stopRecord();
		}
	}
	
	// Start a timer to stop recording when interval expires
	private void startTimer() {
		Intent intent;
		PendingIntent pIntent;
		AlarmManager am;
		Calendar cal;
		
		cal = Calendar.getInstance();
		cal.setTimeInMillis(System.currentTimeMillis());
		cal.add(Calendar.SECOND, (int)(m_lSensorDataFileInterval/1000));
		
		intent = new Intent(getString(R.string.myalarm));
		pIntent = PendingIntent.getBroadcast(GetSensorsActivity.this,0,intent,PendingIntent.FLAG_ONE_SHOT); 
		am = (AlarmManager)getSystemService(Activity.ALARM_SERVICE);
////new		am = (AlarmManager)getSystemService(Context.ALARM_SERVICE);

		am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pIntent);
	}
	
	private void stopTimer() {
		Intent intent;
		PendingIntent pIntent;
		AlarmManager am;

		intent = new Intent(getString(R.string.myalarm));
		pIntent = PendingIntent.getBroadcast(GetSensorsActivity.this,0,intent,PendingIntent.FLAG_ONE_SHOT);
		am = (AlarmManager)getSystemService(Activity.ALARM_SERVICE);
////new		am = (AlarmManager)getSystemService(Context.ALARM_SERVICE);

		am.cancel(pIntent);
		
	}
		
	
	/* Set file identification and type 
	 * Add "_" + "G", "A" to represent each enabled sensor recorded in the data file
	 * Sensor data is recorded as "CSV" file 
	 */
	private String getFileType() {
		String sFileType = "";
		boolean blnHasUnderscore = false; //Indicate the first "_" before the letters representing sensors
				
		if (m_blnGyroEnabled) {
			if (blnHasUnderscore == true) {
				sFileType = sFileType + "G";
			} else {
				sFileType = "_G";
				blnHasUnderscore = true;
			}
		}
		
		if (m_blnAcclEnabled) {
			if (blnHasUnderscore == true) {
				sFileType = sFileType + "A";
			} else {
				sFileType = "_A";
				blnHasUnderscore = true;
			}
		}

			
		sFileType = sFileType + ".csv";
		
		return sFileType;
		
	}
	
	/* Enable/Disable sensor setting 
	 * When recording is going on, sensor setting should be disabled 
	 */
	private void enableSensorSetting(boolean blnSettingEnabled) {		
		if (m_blnGyroPresent) {
			m_chkGyro.setEnabled(blnSettingEnabled);
			m_rdSensorGyro.setEnabled(blnSettingEnabled);
		} else {
			m_chkGyro.setEnabled(false);
			m_rdSensorGyro.setEnabled(false);
		}
		
		if (m_blnAcclPresent) {
			m_chkAccl.setEnabled(blnSettingEnabled);
			m_rdSensorAccl.setEnabled(blnSettingEnabled);
		} else {
			m_chkAccl.setEnabled(false);
			m_rdSensorAccl.setEnabled(false);
		}
		
		
		m_chkVideo.setEnabled(blnSettingEnabled);
		
		m_chkAudio.setEnabled(blnSettingEnabled);

				
		if (m_blnGyroPresent ||
			m_blnAcclPresent) {
			m_rdSensorModeGame.setEnabled(blnSettingEnabled);
			m_rdSensorModeUI.setEnabled(blnSettingEnabled);
			m_spnSelectFileInterval.setEnabled(blnSettingEnabled);
		} else {
			//No sensor is installed, disable mode selection
			m_rdSensorModeGame.setEnabled(false);
			m_rdSensorModeUI.setEnabled(false);
		}
		
		
	}


	/* Set default widget status and setting */
    private void setDefaultStatus() {
    	m_blnGyroEnabled = false;
    	m_blnAcclEnabled = false;
    	m_blnVideoEnabled = false;
    	m_blnAudioEnabled = false;
    	   	
    	m_nGyroMode = SensorManager.SENSOR_DELAY_GAME;
    	m_nAcclMode = SensorManager.SENSOR_DELAY_GAME;
    	
    	m_chkGyro.setChecked(false);
    	m_chkAccl.setChecked(false);
    	m_chkVideo.setChecked(false);
    	m_chkAudio.setChecked(false);
    	    	
    	m_lSensorDataFileInterval = 0;
    	m_spnSelectFileInterval.setSelection(0);
    	    	
    	m_tvShowInfo.setText(getString(R.string.defaultinfo));
    	m_tvShowRecord.setText("");
    	enableSensorSetting(true);
    	m_rdSensorModeGame.setChecked(true);
    	m_blnRecordStatus = false;
    	m_btnRecord.setText(getString(R.string.btn_start));
    }
	    
    
	/* Event for sensor enable/disable */
	private OnCheckedChangeListener m_chkSensorEnableListener = new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) 
			{
			// TODO Auto-generated method stub
				if (m_chkGyro.isChecked()) {
					m_blnGyroEnabled = true;
					m_smGSA.registerListener(m_actHome, m_smGSA.getDefaultSensor(Sensor.TYPE_GYROSCOPE),m_nGyroMode);					
				} else {
					m_blnGyroEnabled = false;
					m_smGSA.unregisterListener(m_actHome, m_smGSA.getDefaultSensor(Sensor.TYPE_GYROSCOPE));
				} 

if (false) {		//Old		
				if (m_chkAccl.isChecked()) {
					m_blnAcclEnabled = true;
					m_smGSA.registerListener(m_actHome, m_smGSA.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),m_nAcclMode);
				} else {
					m_blnAcclEnabled = false;
					m_smGSA.unregisterListener(m_actHome, m_smGSA.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION));
				}
} else {
				if (m_chkAccl.isChecked()) {
					m_blnAcclEnabled = true;
					m_smGSA.registerListener(m_actHome, m_smGSA.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),m_nAcclMode);
				} else {
					m_blnAcclEnabled = false;
					m_smGSA.unregisterListener(m_actHome, m_smGSA.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
				}
}


				if (m_chkVideo.isChecked()) {
					m_blnVideoEnabled = true;
				} else {
					m_blnVideoEnabled = false;	
				}

				if (m_chkAudio.isChecked()) {
					m_blnAudioEnabled = true;
				} else {
					m_blnAudioEnabled = false;	
				}
				
				
				if (m_blnGravityEnabled) {
					m_smGSA.registerListener(m_actHome, m_smGSA.getDefaultSensor(Sensor.TYPE_GRAVITY),m_nGravityMode);
				}
				
			}			
	};
	
	/* Event listener for record interval spinner */
    private Spinner.OnItemSelectedListener m_spnListenerFileInterval = new Spinner.OnItemSelectedListener() {
		public void onItemSelected(AdapterView arg0, View arg1, int arg2, long arg3) 
		{
			// TODO Auto-generated method stub
			int nIndex;
			nIndex = Long.valueOf(m_adpInterval.getItemId(arg2)).intValue();
			// Set file interval for creating new data file
			m_lSensorDataFileInterval = m_arrlFileIntervalValues[nIndex];
		}
		
		public void onNothingSelected(AdapterView arg0) {
			// TODO Auto-generated method stub
		}
		
    };

    
    /* Event listener for sensor radiogroup selection */
    private RadioGroup.OnCheckedChangeListener m_rdgpSensorListener = new RadioGroup.OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(RadioGroup group, int checkedId) 
		{
			
			// TODO Auto-generated method stub
			int iModeType = SensorManager.SENSOR_DELAY_GAME;
			//super.onCheckedChanged(group,checkedId);
			
			if (m_rdSensorGyro.isChecked()) {
				iModeType = m_nGyroMode;
			} else if (m_rdSensorAccl.isChecked()) {
				iModeType = m_nAcclMode;
			} 
			
			if (iModeType == SensorManager.SENSOR_DELAY_GAME) {
				m_rdSensorModeGame.setChecked(true);
			} else if (iModeType == SensorManager.SENSOR_DELAY_UI) {
				m_rdSensorModeUI.setChecked(true);
			}

		}
    };
    
    /* Event listener for sensor mode radiogroup selection */
    private RadioGroup.OnCheckedChangeListener m_rdgpSensorModeListener = new RadioGroup.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) 
			{
				
				// TODO Auto-generated method stub
				int iDelayMode = SensorManager.SENSOR_DELAY_GAME;
				
				if (m_rdSensorModeGame.isChecked()) {
					iDelayMode = SensorManager.SENSOR_DELAY_GAME;	
				} else if (m_rdSensorModeUI.isChecked()) {
					iDelayMode = SensorManager.SENSOR_DELAY_UI;
				}

				if (m_rdSensorGyro.isChecked()) {
					m_nGyroMode = iDelayMode;
				} else if (m_rdSensorAccl.isChecked()) {
					m_nAcclMode = iDelayMode;
				} 
				
				if (m_blnGyroEnabled) {
					m_smGSA.registerListener(m_actHome, m_smGSA.getDefaultSensor(Sensor.TYPE_GYROSCOPE),m_nGyroMode);
				}

if (false) {				
				if (m_blnAcclEnabled) {
					m_smGSA.registerListener(m_actHome, m_smGSA.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),m_nAcclMode);
				}
} else {
				if (m_blnAcclEnabled) {
					m_smGSA.registerListener(m_actHome, m_smGSA.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),m_nAcclMode);
				}
}

				
				if (m_blnGravityEnabled) {
					m_smGSA.registerListener(m_actHome, m_smGSA.getDefaultSensor(Sensor.TYPE_GRAVITY),m_nGravityMode);
				}
			}
     };

     
     
     /* Event listener for record button (Start/Stop) */
     private Button.OnClickListener m_btnRecordListener = new Button.OnClickListener() {
     	public void onClick(View v) 
     	{
     		String sDataDir;
     		File flDataFolder;
     		boolean blnSensorSelected = false;
     		boolean blnVideoSelected = false;
     		boolean blnAudioSelected = false;
     		String sShowInfo = "";
     		boolean blnRet = false;
     		
     		if (m_blnRecordStatus == false) {

     			if ((m_blnGyroEnabled == false) && 
     				(m_blnAcclEnabled == false)) {
     				// No sensor has been selected
     				blnSensorSelected = false;
     			} else {
     				blnSensorSelected = true;
     			}
     			
     			if (m_blnVideoEnabled == false) {
     				blnVideoSelected = false;
     			} else {
     				blnVideoSelected = true;
     			}

     			if (m_blnAudioEnabled == false) {
     				blnAudioSelected = false;
     			} else {
     				blnAudioSelected = true;
     			}
    			
     			if (blnSensorSelected == false && blnVideoSelected == false && blnAudioSelected == false) {
     				m_tvShowInfo.setText(getString(R.string.promptselect));
     				return;
     			}
     			
     			//Start to record
    /*
     			m_dtFileStart = new Date();
     	        final String DATE_FORMAT = "yyyyMMddHHmmss";
     			SimpleDateFormat spdCurrentTime = new SimpleDateFormat(DATE_FORMAT);
     			//m_sRecordFile = curDateTime.format(m_dtFileStart) + ".csv";
     			if (blnSensorSelected) {
     				m_sRecordFile = spdCurrentTime.format(m_dtFileStart);
     			}
   */  			     			        			
     			// Check whether SD Card has been plugged in
     			if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
     				sDataDir = Environment.getExternalStorageDirectory().getAbsolutePath() + 
     										File.separator + getString(R.string.activitydata_folder);
     				flDataFolder = new File(sDataDir);
     				//Check whether /mnt/sdcard/SensorData/ exists
     				if (!flDataFolder.exists()) {
     					//Does not exist, create it
     					if (flDataFolder.mkdir()) {
     						//if (blnSensorSelected) {
     						//	m_sFullPathFile = sDataDir + File.separator + m_sRecordFile;
     						//}
     						     						
     					} else {
     						//Failed to create
     						m_tvShowInfo.setText("Failed to record Sensor data on SD Card!");
     						return;
     					}
     					
     				} else {
     					//if (blnSensorSelected) {
     					//	m_sFullPathFile = sDataDir + File.separator + m_sRecordFile;
     					//}
     					     					
     				} 
     			} else {        				
     				//NO SD Card
     				m_tvShowInfo.setText("Please insert SD Card!");
     				return;
     			}
     			
     			makeRecordFullFilePath();
     			//m_sRecordFullPathFile = m_sFullPathFile;

     			if (blnSensorSelected) {
     				// Append file type information to the file name
     				//m_sFullPathFile = m_sFullPathFile + getFileType();
     				m_sFullPathFile = m_sRecordFullPathFile + getFileType();
     				
	     			try {
	     				m_fwSensorRecord = new FileWriter(m_sFullPathFile);
	      			} catch (IOException e) {
	      				m_tvShowInfo.setText("Failed to record Sensor data on SD Card!");
	      				m_fwSensorRecord = null;
	      				return;
	      			}
     			}
     			     			
     			if (m_blnAudioEnabled) {
     				startAudioRecording();
     			}
     			
     			if (m_blnVideoEnabled) {
     				blnRet = startVideoRecording();
     				if (blnRet == false) {
     					sShowInfo = "Failed to record video";
     					m_tvShowInfo.setText(sShowInfo);
     					return;
     				}
     				
	     			///try {
	     			//	startRecordingVideo();
	     			///} catch (Exception e) {
	                ///    String message = e.getMessage();
	                    //Log.i(null, "Problem " + message);
	                ///    m_MedRec.release();	
	     			///}
     			}
     			
    			//Disable setting for sensor when recording        			
     			enableSensorSetting(false);

     	        m_btnRecord.setText(getString(R.string.btn_stop));
     	        
     	        if (blnSensorSelected == true) {
     	        	if (blnVideoSelected == false && blnAudioSelected == false) {
     	        		sShowInfo = "Recording:" + m_sFullPathFile;
     	        	} else if (blnVideoSelected == true && blnAudioSelected == false) {
     	        		sShowInfo = "Recording Sensor & Video with sound";
     	        	} else if (blnVideoSelected == false && blnAudioSelected == true) {
     	        		sShowInfo = "Recording Sensor & Audio";     	        		
     	        	} else if (blnVideoSelected == true && blnAudioSelected == true) {
     	        		sShowInfo = "Recording Sensor & Audio & soundless Video"; 
     	        	}
     	        } else if (blnVideoSelected == true && blnAudioSelected == false) {
     	        	sShowInfo = "Recording Video with sound";
     	        } else if (blnVideoSelected == false && blnAudioSelected == true) {
     	        	sShowInfo = "Recording Audio";
     	        } else if (blnVideoSelected == true && blnAudioSelected == true) {
     	        	sShowInfo = "Recording Audio & soundless Video"; 
     	        }
     	             	             	        
     			m_tvShowInfo.setText(sShowInfo);
     			
     	        //Save to file once an interval
     			m_blnRecordStatus = true;
     			
     			if (m_lSensorDataFileInterval != 0) {
     				//Need to stop after an interval
     				m_receiverAlarm = new AlarmReceiver();
     				registerReceiver(m_receiverAlarm, new IntentFilter(getString(R.string.myalarm)));
     				startTimer();
     			}
     				     			
     		} else {
     			
     			//Stop record. Close Sensor Record File
     			if (m_fwSensorRecord != null) {
     				try {
     					m_fwSensorRecord.close();
         				m_fwSensorRecord = null;
     				} catch (IOException e) {
     					//
     				}
     			}
     			     			
     			// Enable sensor setting when recording stops
     			enableSensorSetting(true);
     	        m_tvShowInfo.setText(getString(R.string.defaultinfo));        			
     	        m_btnRecord.setText(getString(R.string.btn_start));
     	        m_tvShowRecord.setText("");
     	        resetValues();
     			m_blnRecordStatus = false;

     			if (m_blnAudioEnabled) {
     				stopAudioRecording();
     			}
     			
     			if (m_blnVideoEnabled) {
     				stopVideoRecording();
     			}
     			
     			if (m_lSensorDataFileInterval != 0) {
     				//Need to stop after an interval
     				stopTimer();
     				unregisterReceiver(m_receiverAlarm);
     			}   
 
     		}
     	}
     };

     /* Check the availability of sensors, disable relative widgets */
     private void checkSensorAvailability() {    	
    	 List<Sensor> lstSensor = m_smGSA.getSensorList(Sensor.TYPE_GYROSCOPE);
    	 if (lstSensor.size() > 0) {
    		 m_blnGyroPresent = true;
    	 } else {
    		 m_blnGyroPresent = false;
    		 m_blnGyroEnabled = false;
    		 m_chkGyro.setEnabled(false);
    		 m_rdSensorGyro.setEnabled(false);
    	 }

//    	 lstSensor = m_smGSA.getSensorList(Sensor.TYPE_LINEAR_ACCELERATION);
    	 lstSensor = m_smGSA.getSensorList(Sensor.TYPE_ACCELEROMETER);

    	 if (lstSensor.size() > 0) {
    		 m_blnAcclPresent = true;
    	 } else {
    		 m_blnAcclPresent = false;
    		 m_blnAcclEnabled = false;
    		 m_chkAccl.setEnabled(false);
    		 m_rdSensorAccl.setEnabled(false);
    	 }


    }
    
         
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
    	int i;
     	
    	super.onCreate(savedInstanceState);

        m_smGSA = (SensorManager) getSystemService(SENSOR_SERVICE);
                
        setContentView(R.layout.main);
        
        m_chkGyro = (CheckBox)findViewById(R.id.chkGyro);
        m_chkAccl = (CheckBox)findViewById(R.id.chkAccl);
        m_chkAudio = (CheckBox)findViewById(R.id.chkAudio);
        m_chkVideo = (CheckBox)findViewById(R.id.chkVideo);
                
        m_chkGyro.setOnCheckedChangeListener(m_chkSensorEnableListener);
        m_chkAccl.setOnCheckedChangeListener(m_chkSensorEnableListener);
        m_chkAudio.setOnCheckedChangeListener(m_chkSensorEnableListener);
        m_chkVideo.setOnCheckedChangeListener(m_chkSensorEnableListener);
                
        m_rdgpSensor = (RadioGroup)findViewById(R.id.RdGpSensor);
        m_rdSensorGyro = (RadioButton)findViewById(R.id.RdSensor_Gyro);
    	m_rdSensorAccl = (RadioButton)findViewById(R.id.RdSensor_Accl);
       	
        m_rdgpSensorMode = (RadioGroup)findViewById(R.id.RdGpSensorMode);
        m_rdSensorModeGame = (RadioButton)findViewById(R.id.RdSensorMode_Game);
        m_rdSensorModeUI = (RadioButton)findViewById(R.id.RdSensorMode_UI);
        
        m_spnSelectFileInterval = (Spinner)findViewById(R.id.spnSelectFileInterval);
                
        m_tvShowInfo = (TextView)findViewById(R.id.ShowInfo);  //Show Message to user
        m_tvShowInfo.setText(getString(R.string.defaultinfo));
        m_btnRecord = (Button)findViewById(R.id.Record);
        m_btnRecord.setText(getString(R.string.btn_start));
        m_tvShowRecord = (TextView)findViewById(R.id.ShowRecord); //Show sensor record
        
        
        for (i = 0; i < m_arrsFileIntervalOptions.length; i++) {
        	m_lstInterval.add(m_arrsFileIntervalOptions[i]);
        }
                
        m_adpInterval = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,m_lstInterval);
        m_adpInterval.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        m_spnSelectFileInterval.setAdapter(m_adpInterval);
               
        //setDefaultStatus();
      
        PackageManager pm = getPackageManager();
        m_riHome = pm.resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),0);

        m_rdgpSensor.setOnCheckedChangeListener(m_rdgpSensorListener);
        m_rdgpSensorMode.setOnCheckedChangeListener(m_rdgpSensorModeListener);
        m_spnSelectFileInterval.setOnItemSelectedListener(m_spnListenerFileInterval);
        
        m_btnRecord.setOnClickListener(m_btnRecordListener);
        
        checkSensorAvailability();
        				
        /* No sensor is installed, disable other widgets and show information to user */
		if ((m_blnGyroPresent == false) &&
			(m_blnAcclPresent == false)) {
			m_rdSensorModeGame.setEnabled(false);
			m_rdSensorModeUI.setEnabled(false);
			
		}
		
		//New added
		for (i=0; i<3; i++) {
			m_arrfGravityValues[i] = 0.0f;;
			m_arrfLinearAcceleration[i]  = 0.0f;
		}
		
        m_surfaceView = (SurfaceView) findViewById(R.id.surface_camera);
        m_surfaceHolder = m_surfaceView.getHolder();
        
        m_surfaceView.setBackgroundColor(Color.BLACK);
        
		//////////////
       // m_surfaceHolder.addCallback(this);
       // m_surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        //m_MedRec.setPreviewDisplay(m_surfaceHolder.getSurface());
        
        m_nBufferSize = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        
    }
    
    
    public void startActivitySafely(Intent intent) 
    {
    	intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    	try {
    		startActivity(intent);
    	} catch (ActivityNotFoundException e) {
    		Toast.makeText(this, "unable to open", Toast.LENGTH_SHORT).show();
    	} catch (SecurityException e) {
    		Toast.makeText(this, "unable to open", Toast.LENGTH_SHORT).show();
    	}
    	
    }
    
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	if (keyCode == KeyEvent.KEYCODE_BACK) {
    		//Show MAIN app without finishing current activity
    		ActivityInfo ai = m_riHome.activityInfo;
    		Intent startIntent = new Intent(Intent.ACTION_MAIN);
    		startIntent.addCategory(Intent.CATEGORY_LAUNCHER);
    		startIntent.setComponent(new ComponentName(ai.packageName,ai.name));
    		startActivitySafely(startIntent);
    		return true;
    	} else {
    		return super.onKeyDown(keyCode, event);
    	}
    }
    
    protected void onResume() 
    {
//    	int nModeType;
    	super.onResume();
    }
        
    protected void onStop() {
    	super.onStop();    	
    }
    
    @Override
    protected void onDestroy()
    {
        super.onDestroy();
    }
    
    public void onSensorChanged(SensorEvent event) {

		if ((m_blnGyroEnabled == false) && 
     		(m_blnAcclEnabled == false)) {
			return;
		} else {
			recordSensorGPS(DATA_TYPE_SENSOR, event, null);
		}
    }
    
    public void recordSensorGPS(int iType, SensorEvent event,Location location) {
    	String sRecordLine;
    	String sTimeField;
    	Date dtCurDate;
    	long lStartTime = 0;
    	long lCurrentTime = 0;
    ///	long lElapsedTime = 0;
		SimpleDateFormat spdRecordTime,spdCurDateTime;
        final String DATE_FORMAT = "yyyyMMddHHmmss";
		final String DATE_FORMAT_S = "yyMMddHHmmssSSS"; //"yyyyMMddHHmmssSSS"
        int nSensorReadingType = 0;  //1: Gyro   2:Accl
		
        final float alpha = 0.8f;
        
        if (m_blnRecordStatus == false) { //Stopped
        	return;
        }

        dtCurDate = new Date();
		
        if (m_lSensorDataFileInterval != 0) {
        	// Need to check whether to split file
        	lCurrentTime = dtCurDate.getTime();
			lStartTime = m_dtFileStart.getTime();
			
			//Check whether to record sensor data to a new file
			if (lCurrentTime - lStartTime >= m_lSensorDataFileInterval) {
				if (m_fwSensorRecord != null) {
					//Close current file
					try {
						m_fwSensorRecord.close();
						m_fwSensorRecord = null;
					} catch (IOException e) {
						
					}
				}
				
				return;
								
			}
        }
        
		// Timestamp for the record
        spdRecordTime = new SimpleDateFormat(DATE_FORMAT_S);
		sTimeField = spdRecordTime.format(dtCurDate);
		
////		lElapsedTime = System.nanoTime(); //New Added
		
		if (iType == DATA_TYPE_SENSOR) {
			synchronized(this) {
	    		switch (event.sensor.getType()){		
		    		case Sensor.TYPE_GYROSCOPE:
		    			//X,Y,Z
		    			m_sGyro = Float.toString(event.values[0]) + "," + 
		    					  Float.toString(event.values[1]) + "," + 
		    					  Float.toString(event.values[2]) + ",";
		    			
		    			nSensorReadingType = 1;
		
		    			break;
		    			
		    		case Sensor.TYPE_ACCELEROMETER:
		    			m_arrfGravityValues[0] = alpha * m_arrfGravityValues[0] + (1 - alpha) * event.values[0];
		    			m_arrfGravityValues[1] = alpha * m_arrfGravityValues[1] + (1 - alpha) * event.values[1];
		    			m_arrfGravityValues[2] = alpha * m_arrfGravityValues[2] + (1 - alpha) * event.values[2];

		    			m_arrfLinearAcceleration[0] = event.values[0] - m_arrfGravityValues[0];
		    			m_arrfLinearAcceleration[1] = event.values[1] - m_arrfGravityValues[1];
		    			m_arrfLinearAcceleration[2] = event.values[2] - m_arrfGravityValues[2];	
		    			
	    				m_sAccl = Float.toString(m_arrfLinearAcceleration[0]) + "," + 
	    						Float.toString(m_arrfLinearAcceleration[1]) + "," + 
	    						Float.toString(m_arrfLinearAcceleration[2]) + ",";
	    				
	    				nSensorReadingType = 2;
			
		    			break;
		    			
		    		case Sensor.TYPE_LINEAR_ACCELERATION:
		    			//X,Y,Z
		    			m_sAccl = Float.toString(event.values[0]) + "," + 
		    					  Float.toString(event.values[1]) + "," + 
		    					  Float.toString(event.values[2]) + ",";
		    			
		    			break;
		    					    				    		
		    		case Sensor.TYPE_GRAVITY:
		    			m_arrfGravityValues = event.values.clone();
		    			
	    				m_sGravity = Float.toString(event.values[0]) + "," + 
    								 Float.toString(event.values[1]) + "," + 
    								 Float.toString(event.values[2]) + ",";
	    				return;
	    			//	break;
	    		}
	    	}
		} 
		
		sRecordLine = sTimeField + ",";
////		sRecordLine = sRecordLine + Long.valueOf(lElapsedTime).toString() + ",";   //New Added
				
		
		//Add the timestamp from 1970.1.1
		if (iType == DATA_TYPE_SENSOR) {
			//Sensor Data, Timestamp in nanosecond
			sRecordLine = sRecordLine + Long.valueOf(event.timestamp).toString() + ",";
		} 
		
		sRecordLine = sRecordLine + Integer.valueOf(nSensorReadingType) + ",";   //Sensor Reading Type:  1--Gyro,  2--Accl
		
		if (m_blnGravityEnabled) {
			//sRecordLine = sRecordLine + m_sGravity;
		}
		
		if (m_blnGyroEnabled) {
			sRecordLine = sRecordLine + m_sGyro;
		}
		
		if (m_blnAcclEnabled) {
			sRecordLine = sRecordLine + m_sAccl;
		}
		
						
		/////sRecordLine = sRecordLine + m_sSensorAccuracy;
		
    	sRecordLine = sRecordLine + System.getProperty("line.separator");

    	if (m_blnGyroEnabled == false &&  m_blnAcclEnabled == false) {
    		//To avoid frequently update GUI, only when low-frequent data is presented, show on GUI
    		//Gyro and Accl are too frequent
    		m_tvShowRecord.setText(sRecordLine);
    	}
    	
    	if (m_fwSensorRecord != null) {
			//Write information into file
			//Compose information into recordLine
    		try {
    			m_fwSensorRecord.write(sRecordLine);
    		} catch (IOException e) {
    			
    		}
    	}

    }
    
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    	switch(sensor.getType()) {
    		case Sensor.TYPE_GYROSCOPE:
    			m_sSensorAccuracy = "1,"; //Gyro
    			break;

    		case Sensor.TYPE_ACCELEROMETER:
    			m_sSensorAccuracy = "2,"; //Accl
    			break;

    		case Sensor.TYPE_LINEAR_ACCELERATION:
    			m_sSensorAccuracy = "3,"; //LinearAccl
    			break;
    		
    		case Sensor.TYPE_GRAVITY:
    			m_sSensorAccuracy = "7,"; //Gravity		
    			break;
    			
    		default:
    			m_sSensorAccuracy = "8,"; //Other
    	}
    	
    	switch (accuracy) {
    		case SensorManager.SENSOR_STATUS_ACCURACY_HIGH:
    			m_sSensorAccuracy = m_sSensorAccuracy + "1,"; //H
    			break;

    		case SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM:
    			m_sSensorAccuracy = m_sSensorAccuracy + "2,"; //M
    			break;
    			
    		case SensorManager.SENSOR_STATUS_ACCURACY_LOW:
    			m_sSensorAccuracy = m_sSensorAccuracy + "3,"; //L
    			break;
    			
    		case SensorManager.SENSOR_STATUS_UNRELIABLE:
    			m_sSensorAccuracy = m_sSensorAccuracy + "4,"; //U
    			break;
    	}
    }

    

/*    
    protected void startRecording() throws IOException 
    {
    	Date dtCurDate;
		SimpleDateFormat spdCurDateTime;
        final String DATE_FORMAT = "yyyyMMddHHmmss";
		final String DATE_FORMAT_S = "yyMMddHHmmssSSS"; //"yyyyMMddHHmmssSSS"
        String sVideoFilename, sFullVideoPathFile;
       
        dtCurDate = new Date();
		spdCurDateTime = new SimpleDateFormat(DATE_FORMAT);
		sVideoFilename = spdCurDateTime.format(dtCurDate);
		sFullVideoPathFile = Environment.getExternalStorageDirectory().getAbsolutePath() + 
																		File.separator + 
																		getString(R.string.sensordata_folder) + 
																		File.separator + sVideoFilename;
		// Append file type information to the file name
		sFullVideoPathFile = sFullVideoPathFile + ".mp4";    	

		if (m_camera == null) {
			m_camera = Camera.open();
			m_camera.unlock();
		} else {
			m_camera.unlock();
		}
		
		if (m_rec == null)
			m_rec = new MediaRecorder(); 
 
        m_rec.setCamera(m_camera);

        m_rec.setPreviewDisplay(m_surfaceHolder.getSurface());
//        m_rec.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        m_rec.setVideoSource(MediaRecorder.VideoSource.DEFAULT);

        //        m_rec.setAudioSource(MediaRecorder.AudioSource.MIC); 
        m_rec.setAudioSource(MediaRecorder.AudioSource.DEFAULT);  //
        m_rec.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);  //

//        m_rec.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
        m_rec.setOutputFile(sFullVideoPathFile); 

        m_rec.setMaxDuration(50000);
        m_rec.setVideoFrameRate(24);
        m_rec.setVideoSize(1280, 720);
        m_rec.setVideoEncodingBitRate(3000000);
        m_rec.setAudioEncodingBitRate(8000);
        m_rec.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);
        m_rec.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        m_rec.prepare();
        m_rec.start();
        
                   
    }

    protected void stopRecording() {
    	if (m_rec != null) {
	    	m_rec.stop();
	    	m_rec.release();
	    	m_rec = null;
    	}
    	
    	if (m_camera != null) {
	    	m_camera.release();
	    	m_camera = null;
    	}
    }

    private void releaseMediaRecorder(){
        if (m_rec != null) {
        	m_rec.reset();   // clear recorder configuration
        	m_rec.release(); // release the recorder object
        	m_rec = null;
        	m_camera.lock();           // lock camera for later use
        }
    }

    private void releaseCamera(){
        if (m_camera != null){
        	m_camera.release();        // release the camera for other applications
        	m_camera = null;
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
            int height) {
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (m_camera != null){
            Parameters params = m_camera.getParameters();
            m_camera.setParameters(params);
        }
        else {
            Toast.makeText(getApplicationContext(), "Camera not available!", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    	m_camera.stopPreview();
    	m_camera.release();
    }
*/
	
    private void makeRecordFullFilePath()
    {
    	//Date dtCurDate;
		SimpleDateFormat spdCurDateTime;
        final String DATE_FORMAT = "yyyyMMddHHmmss";
		final String DATE_FORMAT_S = "yyMMddHHmmssSSS"; //"yyyyMMddHHmmssSSS"
		String sFilename;

		m_dtFileStart = new Date();
		spdCurDateTime = new SimpleDateFormat(DATE_FORMAT);
		sFilename = spdCurDateTime.format(m_dtFileStart);
		m_sRecordFullPathFile = Environment.getExternalStorageDirectory().getAbsolutePath() + 
																		File.separator + 
																		getString(R.string.activitydata_folder) + 
																		File.separator + sFilename;   
    }
    
    
    private void startAudioRecording() {
    	m_audioRecorder = new AudioRecord( MediaRecorder.AudioSource.MIC,
                44100, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT, m_nBufferSize);

    	if (m_audioRecorder == null) return;
    	
    	int i = m_audioRecorder.getState();
    	
    	if(i==1)
    		m_audioRecorder.startRecording();

    	//isRecording = true;

    	m_recordingThread = new Thread(new Runnable() {
    							public void run() {
    								writeAudioDataToFile();
    							}
    						},"AudioRecorder Thread");

    	m_recordingThread.start();   	
    }
 
    
    private void writeAudioDataToFile(){
        byte data[] = new byte[m_nBufferSize];
        String sFullAudioPathFileTmp = m_sRecordFullPathFile + ".wav.tmp"; 
        FileOutputStream os = null;
        
        try {
            os = new FileOutputStream(sFullAudioPathFileTmp);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        int read = 0;
        
        if (null != os){
            while(m_audioRecorder != null){
                read = m_audioRecorder.read(data, 0, m_nBufferSize);
                        
                if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                    try {
                        os.write(data);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
                
            try {
                 os.close();
            } catch (IOException e) {
                 e.printStackTrace();
            }
        }
    }
    

    private void stopAudioRecording() {
        if (null != m_audioRecorder) {
            int i = m_audioRecorder.getState();
            
            if (i==1) m_audioRecorder.stop();
            
            m_audioRecorder.release();
            
            m_audioRecorder = null;
            m_recordingThread = null;
        }
        
        copyWaveFile();
        deleteTempFile();
    }
    

    private void deleteTempFile() {
    	String sFullAudioPathFileTmp = m_sRecordFullPathFile + ".wav.tmp"; 
    	
        File file = new File(sFullAudioPathFileTmp);
        
        file.delete();
    }    
    
    
    private void copyWaveFile() {
        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen = 0;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = 44100;
        int channels = 2;
        long byteRate = 16 * 44100 * channels/8;
        String sFullAudioPathFileTmp = m_sRecordFullPathFile + ".wav.tmp"; 
        String sFullAudioPathFile = m_sRecordFullPathFile + ".wav"; 
        
        byte[] data = new byte[m_nBufferSize];
        
        try {
            in = new FileInputStream(sFullAudioPathFileTmp);
            out = new FileOutputStream(sFullAudioPathFile);
            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;
            
            WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
                            longSampleRate, channels, byteRate);
            
            while (in.read(data) != -1){
                out.write(data);
            }
                
            in.close();
            out.close();
        } catch (FileNotFoundException e) {
             e.printStackTrace();
        } catch (IOException e) {
             e.printStackTrace();
        }
    }    
   
    
    private void WriteWaveFileHeader(
            FileOutputStream out, long totalAudioLen,
            long totalDataLen, long longSampleRate, int channels,
            long byteRate) throws IOException {
    
	    byte[] header = new byte[44];
	    
	    header[0] = 'R';  // RIFF/WAVE header
	    header[1] = 'I';
	    header[2] = 'F';
	    header[3] = 'F';
	    header[4] = (byte) (totalDataLen & 0xff);
	    header[5] = (byte) ((totalDataLen >> 8) & 0xff);
	    header[6] = (byte) ((totalDataLen >> 16) & 0xff);
	    header[7] = (byte) ((totalDataLen >> 24) & 0xff);
	    header[8] = 'W';
	    header[9] = 'A';
	    header[10] = 'V';
	    header[11] = 'E';
	    header[12] = 'f';  // 'fmt ' chunk
	    header[13] = 'm';
	    header[14] = 't';
	    header[15] = ' ';
	    header[16] = 16;  // 4 bytes: size of 'fmt ' chunk
	    header[17] = 0;
	    header[18] = 0;
	    header[19] = 0;
	    header[20] = 1;  // format = 1
	    header[21] = 0;
	    header[22] = (byte) channels;
	    header[23] = 0;
	    header[24] = (byte) (longSampleRate & 0xff);
	    header[25] = (byte) ((longSampleRate >> 8) & 0xff);
	    header[26] = (byte) ((longSampleRate >> 16) & 0xff);
	    header[27] = (byte) ((longSampleRate >> 24) & 0xff);
	    header[28] = (byte) (byteRate & 0xff);
	    header[29] = (byte) ((byteRate >> 8) & 0xff);
	    header[30] = (byte) ((byteRate >> 16) & 0xff);
	    header[31] = (byte) ((byteRate >> 24) & 0xff);
	    header[32] = (byte) (2 * 16 / 8);  // block align
	    header[33] = 0;
	    header[34] = 16;  // bits per sample
	    header[35] = 0;
	    header[36] = 'd';
	    header[37] = 'a';
	    header[38] = 't';
	    header[39] = 'a';
	    header[40] = (byte) (totalAudioLen & 0xff);
	    header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
	    header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
	    header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
	
	    out.write(header, 0, 44);
	}
    
       
    private void startAudioRecording_3gp() {
        String sFullAudioPathFile = "";
       
    	m_audioRec = new MediaRecorder();
    	if (m_audioRec != null) {
    		// Append file type information to the file name
    		sFullAudioPathFile = m_sRecordFullPathFile + ".3gp";    	
//    		sFullAudioPathFile = m_sRecordFullPathFile + "_11.mp4";    	
    		
    		m_audioRec.setAudioSource(MediaRecorder.AudioSource.MIC);
    		m_audioRec.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
 //   		m_audioRec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

    		//m_audioRec.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_WB);
    		m_audioRec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
//    		m_audioRec.setAudioChannels(2);
//    		m_audioRec.setAudioSamplingRate(16000);  //AMR_WB
//    		m_audioRec.setAudioEncodingBitRate(23850);  //AMR_WB
    		m_audioRec.setAudioChannels(2);
    		m_audioRec.setAudioSamplingRate(44100);  //AAC
    		m_audioRec.setAudioEncodingBitRate(128000);  //AAC
    		
    		m_audioRec.setOutputFile(sFullAudioPathFile);
    		try {
    			m_audioRec.prepare();
    			m_audioRec.start();
    		} catch (Exception e) {
    			m_audioRec.reset();   // clear recorder configuration
    			m_audioRec.release(); // release the recorder object
    			m_audioRec = null;
    		}
    	}
    }
    
    private void stopAudioRecording_3gp() {
    	if (m_audioRec != null) {
	    	m_audioRec.stop();
	    	m_audioRec.reset();   // You can reuse the object by going back to setAudioSource() step
	    	m_audioRec.release(); // Now the object cannot be reused
	    	m_audioRec = null;
    	}
    }


    @Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		
	}

    private boolean prepareVideoRecording() 
    {
		m_camera = Camera.open();
		if (m_camera == null) return false;
		
    	try {    		
            m_camera.setPreviewDisplay(m_surfaceHolder);
            m_camera.startPreview();

    	} catch (Exception e) {
    		return false;
    	}
    
    	return true;
    }
    
    private boolean startVideoRecording()
    {
        String sFullVideoPathFile;
        boolean blnRet = false;
        
		blnRet = prepareVideoRecording();
		
		if (blnRet == false) return false;
        
		// Append file type information to the file name
		sFullVideoPathFile = m_sRecordFullPathFile + ".mp4";    	
    	
    	m_videoRec = new MediaRecorder();
    	
    	if (m_videoRec == null || m_camera == null) return false;

    	m_camera.unlock();

    	m_videoRec.setCamera(m_camera);

    	// store the quality profile required
       CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_480P); //# Setting for Galaxy Gear
//        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_720P); //# Setting for Galaxy S4
        
        // Step 2: Set sources
    	//m_videoRec.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        if (m_blnAudioEnabled == false) {  //#
        	m_videoRec.setAudioSource(MediaRecorder.AudioSource.MIC); //#
    	} //#
    
    	m_videoRec.setVideoSource(MediaRecorder.VideoSource.CAMERA); //#

       	m_videoRec.setOutputFormat(profile.fileFormat); //#

    	if (m_blnAudioEnabled == false) { //#
       		//m_videoRec.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT); //#
//       		m_videoRec.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_WB); 
       		m_videoRec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
//    		m_videoRec.setAudioChannels(2);
//    		m_videoRec.setAudioSamplingRate(16000);  //AMR_WB
//    		m_videoRec.setAudioEncodingBitRate(23850);  //AMR_WB
       		m_videoRec.setAudioChannels(2);
       		m_videoRec.setAudioSamplingRate(44100);  //AAC
       		m_videoRec.setAudioEncodingBitRate(128000);  //AAC
    	} //#
    	 
       	m_videoRec.setVideoEncoder(profile.videoCodec); //#
      	m_videoRec.setVideoEncodingBitRate(profile.videoBitRate); //#
   		m_videoRec.setVideoFrameRate(profile.videoFrameRate); //#
      	//m_videoRec.setVideoFrameRate(30);
      	//m_videoRec.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight); //# Setting for Galaxy S4
   		m_videoRec.setVideoSize(640, 640);  //# Setting for Galaxy Gear
    	//m_videoRec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);	
    	//m_videoRec.setVideoEncoder(MediaRecorder.VideoEncoder.H264);    	
    	//m_videoRec.setVideoEncodingBitRate(500000);
    	//m_videoRec.setVideoFrameRate(30);
//    	m_videoRec.setVideoSize(1280, 720); //work with FrameRate 30  1px*1px

    	//m_videoRec.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
//    	m_videoRec.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));  //QUALITY_480P=720x480 best, QUALITY_720P work, others (High, Low, 1080p) don't work
        
        // Step 4: Set output file
    	m_videoRec.setOutputFile(sFullVideoPathFile); //#

        // Step 5: Set the preview output
    	m_videoRec.setPreviewDisplay(m_surfaceView.getHolder().getSurface()); //#

        // Step 6: Prepare configured MediaRecorder
        try {
        	m_videoRec.prepare();
        	
        } catch (IllegalStateException e) {
            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        
        m_videoRec.start();
        
        return true;

    }
    
    
    private void releaseMediaRecorder()
    {
    	m_videoRec.release();
    	m_videoRec = null;
    	m_camera.lock();
    	
    	if (m_camera == null) return;

    	m_camera.stopPreview();
    	m_camera.release();
    	m_camera = null;
    	
    }
    
    private void stopVideoRecording()
    {
    	if (m_videoRec == null) return;
    	
    	m_videoRec.stop();
    //	m_videoRec.reset();
    	m_camera.stopPreview();
    	m_videoRec.release();
    	m_videoRec = null;
    	m_camera.lock();
    	
    	if (m_camera == null) return;
    	
    	m_camera.release();
    	m_camera = null;
    }

    
    //This function is a workable version of video recording
    private boolean WORK_VERSION_startVideoRecording()
    {
        String sFullVideoPathFile;
        boolean blnRet = false;
        
		blnRet = prepareVideoRecording();
		
		if (blnRet == false) return false;
        
		// Append file type information to the file name
		sFullVideoPathFile = m_sRecordFullPathFile + ".mp4";    	
    	
    	m_videoRec = new MediaRecorder();
    	
    	if (m_videoRec == null || m_camera == null) return false;

    	m_camera.unlock();

    	m_videoRec.setCamera(m_camera);

    	// store the quality profile required
        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_480P);
    	
        // Step 2: Set sources
        if (m_blnAudioEnabled == false) {
        	m_videoRec.setAudioSource(MediaRecorder.AudioSource.MIC);
    	}
    
    	m_videoRec.setVideoSource(MediaRecorder.VideoSource.CAMERA);

    	m_videoRec.setOutputFormat(profile.fileFormat);

    	if (m_blnAudioEnabled == false) {
    		m_videoRec.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
    	}
    	 
    	m_videoRec.setVideoEncoder(profile.videoCodec);
    	m_videoRec.setVideoEncodingBitRate(profile.videoBitRate);
    	m_videoRec.setVideoFrameRate(profile.videoFrameRate);
    	m_videoRec.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight);
    	
        // Step 4: Set output file
    	m_videoRec.setOutputFile(sFullVideoPathFile);

        // Step 5: Set the preview output
    	m_videoRec.setPreviewDisplay(m_surfaceView.getHolder().getSurface());

        // Step 6: Prepare configured MediaRecorder
        try {
        	m_videoRec.prepare();
        	
        } catch (IllegalStateException e) {
            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        
        m_videoRec.start();
        
        return true;

    }
    
	
}
