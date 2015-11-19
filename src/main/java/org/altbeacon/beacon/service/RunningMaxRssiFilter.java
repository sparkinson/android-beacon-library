package org.altbeacon.beacon.service;

import org.altbeacon.beacon.logging.LogManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;

/**
 * Calculate a RSSI value on base of an arbitrary list of measured RSSI values
 * The list is clipped by a certain length at start and end and the average
 * is calculate by simple arithmetic average
 */
public class RunningMaxRssiFilter implements RssiFilter {

    private static final String TAG = "RunningAverageRssiFilter";
    public static final long DEFAULT_SAMPLE_EXPIRATION_MILLISECONDS = 7500; /* 7.5 seconds */
    private static long sampleExpirationMilliseconds = DEFAULT_SAMPLE_EXPIRATION_MILLISECONDS;
    private ArrayList<Measurement> mMeasurements = new ArrayList<Measurement>();

    @Override
    public void addMeasurement(Integer rssi) {
        Measurement measurement = new Measurement();
        measurement.rssi = rssi;
        measurement.timestamp = new Date().getTime();
        mMeasurements.add(measurement);
        LogManager.i(TAG, "added measurement");
    }

    @Override
    public boolean noMeasurementsAvailable() {
        refreshMeasurements();
        return mMeasurements.size() == 0;
    }

    @Override
    public double calculateRssi() {
        refreshMeasurements();
        if (mMeasurements.size() == 0) return -1;
        LogManager.d(TAG, "Running max mRssi: %s", mMeasurements.get(0));
        return (double) mMeasurements.get(0).rssi;
    }

    private synchronized void refreshMeasurements() {
        Date now = new Date();
        ArrayList<Measurement> newMeasurements = new ArrayList<Measurement>();
        Iterator<Measurement> iterator = mMeasurements.iterator();
        while (iterator.hasNext()) {
            Measurement measurement = iterator.next();
            if (now.getTime() - measurement.timestamp < sampleExpirationMilliseconds ) {
                newMeasurements.add(measurement);
            }
        }
        mMeasurements = newMeasurements;
        Collections.sort(mMeasurements);
    }

    private class Measurement implements Comparable<Measurement> {
        Integer rssi;
        long timestamp;
        @Override
        public int compareTo(Measurement arg0) {
            return rssi.compareTo(arg0.rssi);
        }
    }

    public static void setSampleExpirationMilliseconds(long newSampleExpirationMilliseconds) {
        sampleExpirationMilliseconds = newSampleExpirationMilliseconds;
    }

}
