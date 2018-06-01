package com.moping.appcrashreportlib;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.alibaba.fastjson.JSON;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 保存内容到文件
 */
public class CrashFileManager {

    private static final String TAG = CrashFileManager.class.getName();

    volatile private static CrashFileManager instance = null;

    private static final int SIZE = 4096;

    private Map<String, String> infos = new HashMap<String, String>();

    // 日期格式化
    private DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

    private CrashFileManager() {

    }

    public static CrashFileManager getInstance() {
        if (instance == null) {
            synchronized (CrashFileManager.class) {
                if (instance == null) {
                    instance = new CrashFileManager();
                }
            }
        }
        return instance;
    }

    /**
     * 收集错误信息
     *
     * @param context
     */
    public void collectDeviceInfo(Context context) {
        infos.clear();
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_ACTIVITIES);
            if (pi != null) {
                ApplicationInfo applicationInfo = pi.applicationInfo;
                String appName = pm.getApplicationLabel(applicationInfo).toString();
                String versionName = pi.versionName;
                String versionCode = pi.versionCode + "";
                infos.put("crashType", "1"); // 记录应用层崩溃信息
                infos.put("appName", appName);
                infos.put("versionName", versionName);
                infos.put("versionCode", versionCode);
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        Field[] fields = Build.class.getDeclaredFields();
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                if (field.get(null) instanceof String[]) {
                    StringBuffer sb = new StringBuffer();
                    String[] arrayValue = (String[])field.get(null);
                    for (int i = 0; i < arrayValue.length; i++) {
                        sb.append(arrayValue[i]);
                    }
                    infos.put(field.getName(), sb.toString());
                    Log.d(TAG, field.getName() + " : " + sb.toString());
                } else {
                    infos.put(field.getName(), field.get(null).toString());
                    Log.d(TAG, field.getName() + " : " + field.get(null).toString());
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 保存错误信息到文件
     *
     * @param ex
     */
    public void saveCrashInfo(Throwable ex) {

        StringBuffer sb = new StringBuffer();
        for (Map.Entry<String, String> entry : infos.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            sb.append(key + "=" + value + "\n");
        }

        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        ex.printStackTrace(printWriter);

        // 循环打印错误信息
        Throwable cause = ex.getCause();
        while (cause != null) {
            cause.printStackTrace(printWriter);
            cause = cause.getCause();
        }
        printWriter.close();

        String result = writer.toString();
        sb.append(result);
        infos.put("crashInfo", result);

        // 保存错误信息至网络
        if (!TextUtils.isEmpty(AppSettings.REPORT_IP)) {

            new Thread(new Runnable() {
                @Override
                public void run() {
                    uploadCrashInfo(infos);
                }
            }).start();
        }

        // 保存错误信息至本地文件
        try {
            String time = formatter.format(new Date());
            String fileName = time + ".txt";
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                String path = AppSettings.getGlobalCrashPath();
                File dir = new File(path);
                if (!dir.exists()) {
                    dir.mkdirs();
                }

                StringBuffer oldSb = new StringBuffer();
                try {
                    FileInputStream fis = new FileInputStream(path + fileName);
                    int len = 0;
                    byte[] buf = new byte[SIZE];
                    while ((len = fis.read(buf)) != -1) {
                        oldSb.append(new String(buf, 0, len));
                    }
                    oldSb.append("\n==================================\n\n");
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } finally {
                    FileOutputStream fos = new FileOutputStream(path + fileName);
                    oldSb.append(sb);
                    fos.write(oldSb.toString().getBytes("UTF-8"));
                    fos.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 删除文件
     *
     * @param autoClearDay 文件保存的天数
     */
    public void autoClear(final int autoClearDay) {
        if (autoClearDay < 0) {
            Log.e(TAG, "输入保存天数有误");
        }

        DecimalFormat df = new DecimalFormat("0.00");
        String[] extensions = new String[]{".txt"};
        Collection<File> files = FileUtils.listFiles(AppSettings.getGlobalCrashPath(), extensions);
        long currentTime = System.currentTimeMillis();
        for (File f : files) {
            long fileLastModified = f.lastModified();
            long timeGap = currentTime - fileLastModified;

            Log.i(TAG,"timeGap for file:" + f.getAbsolutePath() + " is " + df.format(timeGap / 1000.0 / 24 / 3600) + " day");

            if (timeGap > autoClearDay * 24L * 60 * 60 * 1000) {
                Log.i(TAG, "begin to delete file:" + f.getAbsolutePath());
                if(!f.delete()) {
                    Log.e(TAG, "delete file failed:" + f.getAbsolutePath());
                }
            }
        }
    }

    /**
     * 上传崩溃日志到后台
     *
     * @param infos
     */
    private void uploadCrashInfo(Map<String, String> infos) {
        if (infos == null) {
            return;
        }

        String jsonStr = JSON.toJSONString(infos);
        Log.i(TAG, "jsonStr : " + jsonStr);

        String result = "";
        BufferedReader reader = null;
        try {
            String reportUrl = "http://" + AppSettings.REPORT_IP + ":" + AppSettings.REPORT_PORT + AppSettings.REPORT_CONTROLLER;
            URL url = new URL(reportUrl);
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setRequestProperty("Charset", "UTF-8");
            // 设置文件类型:
            connection.setRequestProperty("Content-Type","application/x-www-form-urlencoded");

            if (jsonStr != null && !TextUtils.isEmpty(jsonStr)) {
                String param = "crashInfo=" + URLEncoder.encode(jsonStr, "utf-8");
                connection.connect();

                DataOutputStream out = new DataOutputStream(connection
                        .getOutputStream());
                out.writeBytes(param);
                //流用完记得关
                out.flush();
                out.close();

                Log.d(TAG, "doJsonPost: conn " + connection.getResponseCode());
            }

            if (connection.getResponseCode() == 200) {
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                result = reader.readLine();
                Log.i(TAG, "result str: " + result);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}