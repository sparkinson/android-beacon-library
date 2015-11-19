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
public class RunningAverageRssiFilter implements RssiFilter {

    private static final String TAG = "RunningAverageRssiFilter";
    public static final long DEFAULT_MAX_TRACKING_AGE = 5000; /* 5 Seconds */
    public static long maxTrackingAge = DEFAULT_MAX_TRACKING_AGE; /* 5 Seconds */
    public static final long DEFAULT_SAMPLE_EXPIRATION_MILLISECONDS = 20000; /* 20 seconds */
    private static long sampleExpirationMilliseconds = DEFAULT_SAMPLE_EXPIRATION_MILLISECONDS;
    public static final double DEFAULT_SAMPLE_QUANTILE = 0.5;
    private static double sampleQuantile = DEFAULT_SAMPLE_QUANTILE;
    public static final long DEFAULT_SAMPLE_RATE = 500; /* 0.5 seconds */
    private static long sampleRateMilliseconds = DEFAULT_SAMPLE_RATE;
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
        return mMeasurements.size() == 0;
    }

    @Override
    public double calculateRssi() {
        refreshMeasurements();

        int expected = (int) (sampleExpirationMilliseconds/sampleRateMilliseconds - 1);

        // fill missing samples
        ArrayList<Measurement> calcMeasurements = new ArrayList<>(mMeasurements);
        while (calcMeasurements.size() < expected) {
            Measurement m = new Measurement();
            m.rssi = -110;
            m.timestamp = System.currentTimeMillis();
            calcMeasurements.add(m);
        }
        Collections.sort(calcMeasurements);

        int startIndex = (int) (sampleQuantile * calcMeasurements.size());
        assert (startIndex > 0);
        assert (startIndex < calcMeasurements.size());

        double sum = 0;
        int n = 0;
        for (int i = startIndex; i < calcMeasurements.size(); i++) {
            sum += calcMeasurements.get(i).rssi;
            n++;
        }
        double runningAverage = sum/n;

        LogManager.i(TAG, "Running average mRssi based on %s measurements with %s dummy measurements: %s, max : %s",
                calcMeasurements.size(), calcMeasurements.size()-mMeasurements.size(), runningAverage, calcMeasurements.get(calcMeasurements.size()-1).rssi);
        return runningAverage;
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

    public static void setSampleRateMilliseconds(long newSampleRateMilliseconds) {
        sampleRateMilliseconds = newSampleRateMilliseconds;
    }

    public static void setSampleQuantile(double newSampleQuantile) {
        sampleQuantile = newSampleQuantile;
    }

}
