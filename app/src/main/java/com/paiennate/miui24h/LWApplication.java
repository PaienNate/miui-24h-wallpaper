package com.paiennate.miui24h;

import android.app.Application;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

public class LWApplication extends Application {
    public static final String CRASH_FILE = "last_crash.txt";

    @Override
    public void onCreate() {
        super.onCreate();
        final Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                try {
                    StringWriter sw = new StringWriter();
                    throwable.printStackTrace(new PrintWriter(sw));
                    File f = new File(getFilesDir(), CRASH_FILE);
                    FileWriter fw = new FileWriter(f, false);
                    fw.write(new Date().toString() + "\n");
                    fw.write("Thread: " + thread.getName() + "\n\n");
                    fw.write(sw.toString());
                    fw.close();
                    Log.e("Miui24H", "CRASH on " + thread.getName(), throwable);
                } catch (Throwable ignored) {
                }
                if (previous != null) {
                    previous.uncaughtException(thread, throwable);
                }
            }
        });
    }
}
