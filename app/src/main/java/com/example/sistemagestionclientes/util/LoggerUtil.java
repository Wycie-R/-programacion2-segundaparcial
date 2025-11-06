package com.example.sistemagestionclientes.util;

import android.content.Context;
import android.util.Log;

import com.example.sistemagestionclientes.database.AppDatabase;
import com.example.sistemagestionclientes.database.LogApp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoggerUtil {

    private static final String TAG = "LoggerUtil";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static void registrarError(Context context, String descripcionError, String claseOrigen) {
        executor.execute(() -> {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                String fechaHora = sdf.format(new Date());

                LogApp logApp = new LogApp(fechaHora, descripcionError, claseOrigen);

                AppDatabase db = AppDatabase.getInstance(context);
                db.logAppDao().insert(logApp);

                Log.d(TAG, "Error registrado: " + descripcionError + " el " + fechaHora);
            } catch (Exception e) {
                Log.e(TAG, "Error al registrar en BD: " + e.getMessage());
            }
        });
    }
}