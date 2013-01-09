package nl.sense_os.service.phonestate;

import java.util.ArrayList;
import java.util.List;

import nl.sense_os.service.R;
import nl.sense_os.service.constants.SenseDataTypes;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.constants.SensorData.SensorNames;
import nl.sense_os.service.provider.SNTP;
import nl.sense_os.service.shared.PeriodicPollAlarmReceiver;
import nl.sense_os.service.shared.PeriodicPollingSensor;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;

public class AppsSensor implements PeriodicPollingSensor {

    /**
     * Action for the periodic poll alarm Intent
     */
    private static final String ACTION_SAMPLE = AppsSensor.class.getName() + ".SAMPLE";
    /**
     * Request code for the periodic poll alarm Intent
     */
    private static final int REQ_CODE = 0xf00d001d;
    private static final String TAG = "Sense Apps Sensor";
    private static AppsSensor instance;

    public static AppsSensor getInstance(Context context) {
        if (null == instance) {
            instance = new AppsSensor(context);
        }
        return instance;
    }

    private final Context context;
    private final BroadcastReceiver alarmReceiver;
    private boolean active;

    private AppsSensor(Context context) {
        this.context = context;
        this.alarmReceiver = new PeriodicPollAlarmReceiver(this);
    }

    @Override
    public void doSample() {
        Log.v(TAG, "do sample");

        // get app info from package manager
        List<ResolveInfo> installedApps = getInstalledApps();
        List<RunningAppProcessInfo> runningApps = getRunningApps();
        List<ResolveInfo> importantApps = getImportantApps(installedApps, runningApps);

        // send data about all installed apps
        PackageManager pm = context.getPackageManager();
        List<String> installedAppLabels = new ArrayList<String>();
        for (ResolveInfo installedApp : installedApps) {
            String app = installedApp.loadLabel(pm).toString();
            // TODO fix filthy way of filtering out an unparseable character ('tm')
            app = app.replaceAll("\u2122", "");
            installedAppLabels.add(app);
        }
        sendInstalledApps(installedAppLabels);

        // send data about important apps
        for (ResolveInfo importantApp : importantApps) {
            String app = importantApp.loadLabel(pm) + " (" + importantApp.activityInfo.processName
                    + ")";
            sendForegroundApp(app);
        }
    }

    private void sendInstalledApps(List<String> apps) {

        try {
            // create value JSON object
            JSONObject value = new JSONObject();
            value.put("installed", new JSONArray(apps));
            // TODO figure out a better way to send an array of objects

            // send data point
            Intent i = new Intent(context.getString(R.string.action_sense_new_data));
            i.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.JSON);
            i.putExtra(DataPoint.VALUE, value.toString());
            i.putExtra(DataPoint.SENSOR_NAME, SensorNames.APP_INSTALLED);
            i.putExtra(DataPoint.TIMESTAMP, SNTP.getInstance().getTime());
            context.startService(i);

        } catch (JSONException e) {
            Log.e(TAG, "Failed to create data point for installed apps sensor!", e);
        }
    }

    private void sendForegroundApp(String label) {
        Intent i = new Intent(context.getString(R.string.action_sense_new_data));
        i.putExtra(DataPoint.DATA_TYPE, SenseDataTypes.STRING);
        i.putExtra(DataPoint.VALUE, label);
        i.putExtra(DataPoint.SENSOR_NAME, SensorNames.APP_FOREGROUND);
        i.putExtra(DataPoint.TIMESTAMP, SNTP.getInstance().getTime());
        context.startService(i);
    }

    private List<ResolveInfo> getImportantApps(List<ResolveInfo> installedApps,
            List<RunningAppProcessInfo> runningApps) {

        // find the important apps
        List<ResolveInfo> result = new ArrayList<ResolveInfo>();
        if (null != runningApps) {
            for (RunningAppProcessInfo runningApp : runningApps) {

                // only check important apps
                int importance = runningApp.importance;
                if (importance >= RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                        && importance <= RunningAppProcessInfo.IMPORTANCE_VISIBLE) {

                    // see if this app was explicitly installed
                    ResolveInfo resolveInfo = null;
                    String ownPackage = context.getPackageName();
                    for (ResolveInfo installedApp : installedApps) {
                        if (runningApp.processName.equals(installedApp.activityInfo.processName)) {
                            if (installedApp.activityInfo.packageName.equals(ownPackage)) {
                                // ignore our own app, it is always foreground
                            } else {
                                resolveInfo = installedApp;
                            }
                            break;
                        }
                    }

                    // add to result list
                    if (null != resolveInfo) {
                        result.add(resolveInfo);
                    }
                }
            }
        } else {
            // no running apps
        }

        return result;
    }

    private List<ResolveInfo> getInstalledApps() {
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> result = context.getPackageManager().queryIntentActivities(mainIntent, 0);

        return result;
    }

    private List<RunningAppProcessInfo> getRunningApps() {
        ActivityManager actvityManager = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningAppProcessInfo> result = actvityManager.getRunningAppProcesses();

        return result;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    /**
     * Starts periodically sampling the lists of installed and running apps
     * 
     * @param interval
     *            Sample interval
     */
    public void start(long interval) {

        // start polling
        context.registerReceiver(alarmReceiver, new IntentFilter(ACTION_SAMPLE));
        Intent alarm = new Intent(ACTION_SAMPLE);
        PendingIntent alarmOperation = PendingIntent.getBroadcast(context, REQ_CODE, alarm, 0);
        AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        mgr.cancel(alarmOperation);
        mgr.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), interval,
                alarmOperation);

        // update active state
        active = true;
    }

    /**
     * Stops sampling
     */
    public void stop() {

        // stop the alarms
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarms.cancel(PendingIntent.getBroadcast(context, REQ_CODE, new Intent(ACTION_SAMPLE), 0));
        try {
            context.unregisterReceiver(alarmReceiver);
        } catch (IllegalArgumentException e) {
            // ignore
        }

        // update active state
        active = false;
    }

}
