package nl.sense_os.service.phonestate;

import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.util.Log;

public class AppsSensor {

    private static final String TAG = "Sense Apps Sensor";
    private static AppsSensor instance;

    public static AppsSensor getInstance(Context context) {
        if (null == instance) {
            instance = new AppsSensor(context);
        }
        return instance;
    }

    private Context context;

    private AppsSensor(Context context) {
        this.context = context;
    }

    private List<ResolveInfo> getInstalledApps() {
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> result = context.getPackageManager().queryIntentActivities(
                mainIntent, 0);

        return result;
    }

    public void start() {
        // installed activities
        List<ResolveInfo> installed = getInstalledApps();
        for (ResolveInfo appInfo : installed) {

            String label = appInfo.loadLabel(context.getPackageManager()).toString();
            String process = "";
            if (null != appInfo.activityInfo) {
                process = appInfo.activityInfo.processName;
            }
            Log.d(TAG, "Installed app: " + process + " (" + label + ")");
            // TODO
        }

        // get running activities
        List<RunningAppProcessInfo> running = getRunningApps();
        if (null != running) {
            for (RunningAppProcessInfo processInfo : running) {
                String process = processInfo.processName;
                Log.d(TAG, "Running process: " + process);
                // TODO
            }
        } else {
            // no running apps
        }
    }

    private List<RunningAppProcessInfo> getRunningApps() {
        ActivityManager actvityManager = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningAppProcessInfo> result = actvityManager.getRunningAppProcesses();

        return result;
    }

    public void stop() {

    }

}
