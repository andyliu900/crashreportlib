package com.moping.appcrashreportlib;

import android.support.annotation.Nullable;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * 内存信息收集
 */
public class DumpSysCollector {

    private static final String LOG_TAG = DumpSysCollector.class.getName();

    private static final int DEFAULT_BUFFER_SIZE_IN_BYTES = 8192;

    @Nullable
    public static String collectMemInfo() {

        StringBuilder meminfo = new StringBuilder();
        try {
            final List<String> commandLine = new ArrayList<String>();
            commandLine.add("dumpsys");
            commandLine.add("meminfo");
            commandLine.add(Integer.toString(android.os.Process.myPid()));

            final Process process = Runtime.getRuntime().exec(commandLine.toArray(new String[commandLine.size()]));
            process.waitFor();


            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                    process.getInputStream()), DEFAULT_BUFFER_SIZE_IN_BYTES);
            while (true) {
                String line = bufferedReader.readLine();
                if (line == null) {
                    break;
                }
                meminfo.append(line);
                meminfo.append("\n");
            }

        } catch (IOException e) {
            Log.e(LOG_TAG, "BumpSysCollector.meminfo could not retrieve data", e);
        } catch (InterruptedException e) {
            Log.e(LOG_TAG, "BumpSysCollector.meminfo could not retrieve data", e);
        }

        return meminfo.toString();
    }

}
