package com.moping.appcrashreportlib;

import android.os.Environment;

import java.io.File;

public class AppSettings {

    /**
     * 获取崩溃文件保存路径
     *
     * @return
     */
    public static String getGlobalCrashPath() {
        return Environment.getExternalStorageDirectory().getAbsolutePath()
                + File.separator + "moping_lib_crash" + File.separator;
    }

    public static String REPORT_IP = "";

    public static String REPORT_PORT = "";

    public static String REPORT_CONTROLLER = "/concentratedcontrol/api/reportManager/crashReport";

}
