package com.moping.appcrashreportlib;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

/**
 * 捕获异常
 */

@SuppressLint("SimpleDateFormat")
public class CrashHandler implements Thread.UncaughtExceptionHandler {

    public static String TAG = CrashHandler.class.getName();

    /**
     * 系统默认的UncaughtException处理类
     */

    private Thread.UncaughtExceptionHandler mDefaultHandler;

    private static CrashHandler instance = new CrashHandler();
    private Context mContext;

    /**
     * 屏蔽默认构造函数
     */
    private CrashHandler() {

    }

    public static CrashHandler getInstance() {
        return instance;
    }

    public void init(Context context, String ip, String port) {
        mContext = context;
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();

        AppSettings.REPORT_IP = ip;
        AppSettings.REPORT_PORT = port;

        Thread.setDefaultUncaughtExceptionHandler(this);
        CrashFileManager.getInstance().collectDeviceInfo(mContext);
        CrashFileManager.getInstance().autoClear(5);
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        if (!handleException(ex) && mDefaultHandler != null) {
            // 如果用户没有处理则让系统默认的异常处理器来处理
            mDefaultHandler.uncaughtException(thread, ex);
        } else {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Log.e(TAG, "error : ", e);
            }
            // 退出程序
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        }
    }

    /**
     * 自定义错误处理,收集错误信息 发送错误报告等操作均在此完成.
     *
     * @param ex
     * @return true:如果处理了该异常信息; 否则返回false.
     */
    private boolean handleException(Throwable ex) {
        if (ex == null) {
            return false;
        }

        try {
            new Thread() {
                @Override
                public void run() {
                    Looper.prepare();
                    Toast.makeText(mContext, "很抱歉，程序出现异常", Toast.LENGTH_LONG).show();
                    Looper.loop();
                }
            }.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 保存日志文件
        CrashFileManager.getInstance().saveCrashInfo(ex);

        return true;
    }

}
