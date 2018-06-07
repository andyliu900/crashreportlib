package com.moping.appcrashreportlib;


import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collection;

/**
 * 文件管理工具类
 */
public class FileUtils {

    private static final String TAG = FileUtils.class.getName();

    /**
     * 列出路径下文件列表
     *
     * @param path  文件路径
     * @return  路径下的文件列表
     */
    public static Collection<File> listFiles(String path) {
        return listFiles(path, null, false);
    }

    /**
     * 列出路径下文件列表
     *
     * @param path  文件路径
     * @param recursive  是否递归查询文件夹内的文件夹
     * @return  路径下的文件列表
     */
    public static Collection<File> listFiles(String path, boolean recursive) {
        return listFiles(path, null, recursive);
    }

    public static Collection<File> listFiles(String path, String[] extensions) {
        return listFiles(path, extensions, false);
    }

    /**
     * 列出路径下文件列表
     *
     * @param path  文件路径
     * @param extensions  文件格式后缀，只列出符合后缀的文件
     * @param recursive  是否递归查询文件夹内的文件夹
     * @return  路径下的文件列表
     */
    public static Collection<File> listFiles(String path, String[] extensions, boolean recursive) {
        if (path == null || TextUtils.isEmpty(path)) {
            Log.e(TAG, "路径不能为空");
            return null;
        }

        ArrayList<File> arrayList = new ArrayList();
        File file = new File(path);
        if (file.isFile()) {
            arrayList.add(file);
        } else {
            if (extensions != null) {
                for (int i = 0; i < extensions.length; i++) {
                    FileFlitter fileFlitter = new FileFlitter(extensions[i]);
                    File[] files = file.listFiles(fileFlitter);
                    if (files != null) {
                        for (int j = 0; j < files.length; j++) {
                            arrayList.add(files[j]);
                        }
                    }
                }
            } else {
                File[] files = file.listFiles();
                for (int j = 0; j < files.length; j++) {
                    arrayList.add(files[j]);
                }
            }
        }

        return arrayList;
    }

    static class FileFlitter implements FilenameFilter {

        private String type;

        public FileFlitter(String type) {
            this.type = type;
        }

        @Override
        public boolean accept(File dir, String name) {
            return name.endsWith(type);
        }
    }

}
