package nl.sense_os.service.phonestate;

import java.util.List;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import android.os.Handler;
import android.util.Log;

import nl.sense_os.service.MsgHandler;

public class PressureSensor implements SensorEventListener {
	@SuppressWarnings("unused")
	private static final String TAG = "Sense Pressure Sensor";
	private MsgHandler msgHandler;
	private long sampleDelay = 0; //in milliseconds    
	private long[] lastSampleTimes = new long[50];
	private Context context;
	private List<Sensor> sensors;
	private SensorManager smgr;
	private Handler PressureHandler = new Handler();
	private Runnable PressureThread = null;
	private boolean PressureSensingActive = false;
	public PressureSensor(MsgHandler handler, Context _context) {
		this.msgHandler = handler;
		this.context = _context;
		smgr = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);		
		sensors = smgr.getSensorList(Sensor.TYPE_ALL);
	}

	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		//        Log.d(TAG, "Accuracy changed...");
		//        Log.d(TAG, "Sensor: " + sensor.getName() + "(" + sensor.getType() + "), accuracy: " + accuracy);
	}

	public void onSensorChanged(SensorEvent event) {
		Sensor sensor = event.sensor;
		if(System.currentTimeMillis() > lastSampleTimes[sensor.getType()]+sampleDelay)
		{
			lastSampleTimes[sensor.getType()] = System.currentTimeMillis();	 

			String sensorName = "";
			if(sensor.getType()==Sensor.TYPE_PRESSURE)
			{
				sensorName = "pressure";
			}
			
			String jsonString = "{";	        
			int x = 0;
			for (float value: event.values) {
				if(x==0)
				{	
					if(sensor.getType()==Sensor.TYPE_PRESSURE)
						jsonString += "\"newton\":"+value;				
				}				
				x++;
			}
			jsonString += "}";
			this.msgHandler.sendSensorData(sensorName, jsonString, "json", sensor.getName()); 	       
		}
		if(sampleDelay > 500 && PressureSensingActive)
		{
			// unregister the listener and start again in sampleDelay seconds	
			stopPressureSensing();
			PressureHandler.postDelayed(PressureThread= new Runnable() {

				public void run() 
				{					
					startPressureSensing(sampleDelay);
				}
			},sampleDelay);
		}
	}

	public void setSampleDelay(long _sampleDelay)
	{
		sampleDelay = _sampleDelay;    	
	}

	public void startPressureSensing(long _sampleDelay)
	{
		PressureSensingActive = true;
		setSampleDelay(_sampleDelay);		
		for (Sensor sensor : sensors) 
		{		
			if (sensor.getType()==Sensor.TYPE_PRESSURE )
			{				
				//Log.d(TAG, "registering for sensor " + sensor.getName());
				smgr.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
			}
		}		
	}

	public void stopPressureSensing()
	{
		try{
			PressureSensingActive = false;
			smgr.unregisterListener(this);

			if(PressureThread != null)		
				PressureHandler.removeCallbacks(PressureThread);
			PressureThread = null;
			
		}catch(Exception e)
		{
			Log.e(TAG, e.getMessage());
		}

	}

	public long getSampleDelay()
	{
		return sampleDelay;
	}
}
