package me.third.right.worldDownloader.managers;

import java.util.Arrays;
import java.util.Collections;

//All created by Github Copilot :D
public class PerformanceTracker {
    private static boolean firstRun = true;
    private static int index = 0;
    private static final long[] times = new long[20];

    public static void addTime(long time) {
        times[index] = time;
        index++;
        if(index >= times.length) index = 0;
    }

    public static long getAverage() {
        if(firstRun) {
            Arrays.fill(times, 0L);
            firstRun = false;
        }

        long total = 0;
        for(long time : times) {
            total += time;
        }
        return total / times.length;
    }
}
