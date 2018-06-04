package com.moping.appcrashreportlib;

import android.support.annotation.Nullable;

/**
 * 收集线程信息
 *
 */
public class ThreadCollector {

    @Nullable
    public static String collect(@Nullable Thread thread) {
        StringBuffer sb = new StringBuffer();
        if (thread != null) {
            sb.append("id=").append(thread.getId()).append("\n");
            sb.append("name=").append(thread.getName()).append("\n");
            sb.append("priority=").append(thread.getPriority()).append("\n");
            if (thread.getThreadGroup() != null) {
                sb.append("groupName=").append(thread.getThreadGroup().getName()).append("\n");
            }
        }
        return sb.toString();
    }

}
