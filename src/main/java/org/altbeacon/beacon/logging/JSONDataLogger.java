package org.altbeacon.beacon.logging;

import android.content.Context;
import android.os.Environment;

import org.altbeacon.beacon.Beacon;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;

/**
 * Created by sp911 on 10/11/15.
 */
public class JSONDataLogger {

    private static final String TAG = "JSONDataLogger";
    private static final String FILENAME = "BeaconData.json";

    private static JSONDataLogger mInstance;
    private boolean mEnabled;
    private boolean mCrowded;
    private int mRange;

    private Context mContext;

    public boolean isEnabled() {
        return mEnabled;
    }

    public static void setEnabled(boolean mEnabled, Context context) {
        getInstance().mEnabled = mEnabled;
        getInstance().mContext = context;
    }

    public static void setCrowded(boolean crowded) {
        getInstance().mCrowded = crowded;
    }

    public static void setRange(int range) {
        getInstance().mRange = range;
    }

    public static JSONDataLogger getInstance() {
        if(mInstance == null) {
            mInstance = new JSONDataLogger();
        }
        return mInstance;
    }

    public void log(Beacon beacon, String component) {
        if (!mEnabled) return;

        JSONObject obj = beacon.toJSON();
        log(obj, component);
    }

    public void log(String message, String component) {
        if (!mEnabled) return;

        JSONObject obj = new JSONObject();
        try {
            obj.put("message", message);
        }
        catch (JSONException e) {
            LogManager.e(TAG, "Could not create JSON property");
            e.printStackTrace();
        }

        log(obj, component);
    }

    public void log(JSONObject obj, String component) {
        if (!mEnabled) return;

        try {
            obj.put("component", component);
            obj.put("timestamp", System.currentTimeMillis());
            obj.put("crowded", mCrowded);
            obj.put("in_range", mRange);
        }
        catch (JSONException e) {
            LogManager.e(TAG, "Could not create JSON property");
            e.printStackTrace();
        }

        PrintWriter printer;
        if (null != (printer = getPrinter())) {
            printer.println(obj.toString());
            printer.close();
        }

    }

    private PrintWriter getPrinter() {
        if (!(isExternalStorageWritable() & isExternalStorageReadable())) {
            LogManager.e(TAG, "External storage is unavailable");
            return null;
        }

        if (null == mContext) {
            LogManager.e(TAG, "Context is not set");
            return null;
        }

        // create file if it does not exist
        File file = new File(mContext.getExternalFilesDir(""), FILENAME);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                LogManager.e(TAG, "Could not create output file");
                e.printStackTrace();
            }
        }

        // get output stream
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file, true);
        } catch (FileNotFoundException e) {
            LogManager.e(TAG, "Could not find output file");
            e.printStackTrace();
        }

        PrintWriter printer = new PrintWriter(fos);

        return printer;
    }

    /* Checks if external storage is available for read and write */
    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    private boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

}
