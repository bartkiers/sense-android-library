/**************************************************************************************************
 * Copyright (C) 2010 Sense Observation Systems, Rotterdam, the Netherlands. All rights reserved. *
 *************************************************************************************************/
package nl.sense_os.service.storage;

import java.nio.BufferOverflowException;

import nl.sense_os.service.R;
import nl.sense_os.service.constants.SensePrefs;
import nl.sense_os.service.constants.SensePrefs.Main;
import nl.sense_os.service.constants.SensorData.DataPoint;
import nl.sense_os.service.provider.SNTP;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

/**
 * Storage for recent sensor data. The data is initially stored in the device's RAM memory. In case
 * the memory becomes too full, the data is offloaded into a persistent database in the flash
 * memory. This process is hidden to the end user, so you do not have to worry about which data is
 * where.
 * 
 * @author Steven Mulder <steven@sense-os.nl>
 * 
 * @see ParserUtils
 * @see DataPoint
 */
public class LocalStorage {

    /**
     * Minimum time to retain data points. If data is not sent to CommonSense, it will be retained
     * longer.
     */
    private static final long RETENTION_TIME = 1000l * 60 * 60 * 24;

    /**
     * Default projection for rows of data points
     */
    private static final String[] DEFAULT_PROJECTION = new String[] { BaseColumns._ID,
            DataPoint.SENSOR_NAME, DataPoint.DISPLAY_NAME, DataPoint.SENSOR_DESCRIPTION,
            DataPoint.DATA_TYPE, DataPoint.VALUE, DataPoint.TIMESTAMP, DataPoint.DEVICE_UUID,
            DataPoint.TRANSMIT_STATE };

    private static final int LOCAL_VALUES_URI = 1;
    private static final int REMOTE_VALUES_URI = 2;

    private static final String TAG = "LocalStorage";

    private static LocalStorage instance;

    /**
     * @param context
     *            Context for lazy creating the LocalStorage.
     * @return Singleton instance of the LocalStorage
     */
    public static LocalStorage getInstance(Context context) {
        // Log.v(TAG, "Get local storage instance");
        if (null == instance) {
            instance = new LocalStorage(context.getApplicationContext());
        }
        return instance;
    }

    private final RemoteStorage commonSense;
    private final SQLiteStorage inMemory;
    private final SQLiteStorage persisted;
    private Context context;

    private LocalStorage(Context context) {
        Log.i(TAG, "Construct new local storage instance");
        this.context = context;
        persisted = new SQLiteStorage(context, true);
        inMemory = new SQLiteStorage(context, false);
        commonSense = new RemoteStorage(context);
    }

    public int delete(Uri uri, String where, String[] selectionArgs) {
        switch (matchUri(uri)) {
        case LOCAL_VALUES_URI:
            int nrDeleted = 0;
            nrDeleted += inMemory.delete(where, selectionArgs);
            nrDeleted += persisted.delete(where, selectionArgs);
            return nrDeleted;
        case REMOTE_VALUES_URI:
            throw new IllegalArgumentException("Cannot delete values from CommonSense!");
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    public String getType(Uri uri) {
        int uriType = matchUri(uri);
        if (uriType == LOCAL_VALUES_URI || uriType == REMOTE_VALUES_URI) {
            return DataPoint.CONTENT_TYPE;
        } else {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    public Uri insert(Uri uri, ContentValues values) {

        // check the URI
        switch (matchUri(uri)) {
        case LOCAL_VALUES_URI:
            // implementation below
            break;
        case REMOTE_VALUES_URI:
            throw new IllegalArgumentException(
                    "Cannot insert into CommonSense through this ContentProvider");
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        // insert in the in-memory database
        long rowId = 0;
        try {
            rowId = inMemory.insert(values);
        } catch (BufferOverflowException e) {
            // in-memory storage is full!
            persistAllUnsentData();

            // try again
            rowId = inMemory.insert(values);
        }

        // notify any listeners (does this work properly?)
        Uri contentUri = Uri.parse("content://"
                + context.getString(R.string.local_storage_authority) + DataPoint.CONTENT_URI_PATH);
        Uri rowUri = ContentUris.withAppendedId(contentUri, rowId);
        context.getContentResolver().notifyChange(rowUri, null);

        return rowUri;
    }

    private int matchUri(Uri uri) {
        if (DataPoint.CONTENT_URI_PATH.equals(uri.getPath())) {
            return LOCAL_VALUES_URI;
        } else if (DataPoint.CONTENT_REMOTE_URI_PATH.equals(uri.getPath())) {
            return REMOTE_VALUES_URI;
        } else {
            return -1;
        }
    }

    private int persistAllUnsentData() {
        Log.i(TAG, "Persist all unsent data points from in-memory storage");

        Cursor unsentPoints = null;
        int nrUnsentPoints = 0;
        try {
            // remove old data from the persistent storage
            long retentionLimit = SNTP.getInstance().getTime() - RETENTION_TIME;
            SharedPreferences prefs = context.getSharedPreferences(SensePrefs.MAIN_PREFS,
                    Context.MODE_PRIVATE);
            boolean useCommonSense = prefs.getBoolean(Main.Advanced.USE_COMMONSENSE, true);
            String selectOld = null;
            if (useCommonSense) {
                // delete data older than minimum retention time if it had been transmitted
                selectOld = DataPoint.TIMESTAMP + "<" + retentionLimit + " AND "
                        + DataPoint.TRANSMIT_STATE + "==1";
            } else {
                // not using CommonSense: delete all data older than minimum retention time
                selectOld = DataPoint.TIMESTAMP + "<" + retentionLimit;
            }
            persisted.delete(selectOld, null);

            // get unsent or very recent data from the memory
            String selectUnsent = DataPoint.TRANSMIT_STATE + "!=1" + " OR " + DataPoint.TIMESTAMP + ">"
                    + retentionLimit;
            unsentPoints = inMemory.query(DEFAULT_PROJECTION, selectUnsent, null, null);
            nrUnsentPoints = unsentPoints.getCount();

            // bulk insert the new data
            persisted.bulkInsert(unsentPoints);

            // remove all the in-memory data
            inMemory.delete(null, null);

        } finally {
            if (null != unsentPoints) {
                unsentPoints.close();
                unsentPoints = null;
            }
        }

        return nrUnsentPoints;
    }

    public Cursor query(Uri uri, String[] projection, String where, String[] selectionArgs,
            String sortOrder) {
        // Log.v(TAG, "Query data points in local storage");

        // check URI
        switch (matchUri(uri)) {
        case LOCAL_VALUES_URI:
            // implementation below
            break;
        case REMOTE_VALUES_URI:
            try {
                return commonSense.query(uri, projection, where, selectionArgs, sortOrder);
            } catch (Exception e) {
                Log.e(TAG, "Failed to query the CommonSense data points", e);
                return null;
            }
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        // use default projection if needed
        if (projection == null) {
            projection = DEFAULT_PROJECTION;
        }

        // query both databases
        Cursor inMemoryCursor = inMemory.query(projection, where, selectionArgs, sortOrder);
        Cursor persistedCursor = persisted.query(projection, where, selectionArgs, sortOrder);

        if (inMemoryCursor.getCount() > 0) {
            if (persistedCursor.getCount() > 0) {
                // merge cursors
                return new MergeCursor(new Cursor[] { persistedCursor, inMemoryCursor });
            } else {
                persistedCursor.close();
                return inMemoryCursor;
            }
        } else {
            inMemoryCursor.close();
            return persistedCursor;
        }
    }

    public int update(Uri uri, ContentValues newValues, String where, String[] selectionArgs) {

        // check URI
        switch (matchUri(uri)) {
        case LOCAL_VALUES_URI:
            // implementation below
            break;
        case REMOTE_VALUES_URI:
            throw new IllegalArgumentException("Cannot update data points in CommonSense");
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        // persist parameter is used to initiate persisting the volatile data
        boolean persist = "true".equals(uri.getQueryParameter("persist"));

        int result = 0;
        if (!persist) {
            // Log.v(TAG, "Update data points in local storage");
            int updated = 0;
            updated += inMemory.update(newValues, where, selectionArgs);
            updated += persisted.update(newValues, where, selectionArgs);
            return updated;

        } else {
            persistAllUnsentData();
        }

        // notify content observers
        context.getContentResolver().notifyChange(uri, null);

        return result;
    }
}
