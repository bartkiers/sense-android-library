package nl.sense_os.service.phonestate;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.util.Log;

import nl.sense_os.app.SenseSettings;
import nl.sense_os.service.MsgHandler;

import java.util.List;

public class ProximitySensor implements SensorEventListener {

    private static final String NAME = "proximity";
	private static final String TAG = "Sense Proximity Sensor";
	private long sampleDelay = 0; //in milliseconds    
	private long[] lastSampleTimes = new long[50];
	private Context context;
	private List<Sensor> sensors;
	private SensorManager smgr;
	private Handler ProximityHandler = new Handler();
	private Runnable ProximityThread = null;
	private boolean ProximitySensingActive = false;
	
	public ProximitySensor(Context context) {
		this.context = context;
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
			if (sensor.getType()==Sensor.TYPE_PROXIMITY) {
				sensorName = NAME;
			}
			
			String jsonString = "{";	        
			int x = 0;
			for (float value: event.values) {
				if(x==0)
				{	
					if(sensor.getType()==Sensor.TYPE_PROXIMITY)
						jsonString += "\"distance\":"+value;				
				}				
				x++;
			}
			jsonString += "}";
            
            // pass message to the MsgHandler
            Intent i = new Intent(this.context, MsgHandler.class);
            i.putExtra(MsgHandler.KEY_INTENT_TYPE, MsgHandler.TYPE_NEW_MSG);
            i.putExtra(MsgHandler.KEY_SENSOR_NAME, sensorName);
            i.putExtra(MsgHandler.KEY_VALUE, jsonString);
            i.putExtra(MsgHandler.KEY_DATA_TYPE, SenseSettings.SENSOR_DATA_TYPE_JSON);
            i.putExtra(MsgHandler.KEY_TIMESTAMP, System.currentTimeMillis());
            this.context.startService(i);       
		}
		if(sampleDelay > 500 && ProximitySensingActive)
		{
			// unregister the listener and start again in sampleDelay seconds	
			stopProximitySensing();
			ProximityHandler.postDelayed(ProximityThread= new Runnable() {

				public void run() 
				{					
					startProximitySensing(sampleDelay);
				}
			},sampleDelay);
		}
	}

	public void setSampleDelay(long _sampleDelay)
	{
		sampleDelay = _sampleDelay;    	
	}

	public void startProximitySensing(long _sampleDelay)
	{
		ProximitySensingActive = true;
		setSampleDelay(_sampleDelay);		
		for (Sensor sensor : sensors) {
			if (sensor.getType()==Sensor.TYPE_PROXIMITY )
			{
				//Log.d(TAG, "registering for sensor " + sensor.getName());
				smgr.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
			}
		}		
	}

	public void stopProximitySensing()
	{
		try{
			ProximitySensingActive = false;
			smgr.unregisterListener(this);

			if(ProximityThread != null)		
				ProximityHandler.removeCallbacks(ProximityThread);
			ProximityThread = null;
			
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
