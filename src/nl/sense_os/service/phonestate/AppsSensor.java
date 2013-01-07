package nl.sense_os.service.phonestate;

import android.content.Context;

public class AppsSensor {

    private static AppsSensor instance;

    public static AppsSensor getInstance(Context context) {
        if (null == instance) {
            instance = new AppsSensor(context);
        }
        return instance;
    }

    private AppsSensor(Context context) {

    }

    public void start() {

    }

    public void stop() {

    }

}
