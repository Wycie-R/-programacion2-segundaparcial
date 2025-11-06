package com.example.sistemagestionclientes.worker;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.sistemagestionclientes.database.AppDatabase;
import com.example.sistemagestionclientes.database.LogApp;
import com.example.sistemagestionclientes.network.ApiService;
import com.example.sistemagestionclientes.network.RetrofitClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Response;

public class SyncLogsWorker extends Worker {

    private static final String TAG = "SyncLogsWorker";

    public SyncLogsWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Iniciando sincronización de logs...");

        try {

            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            List<LogApp> logs = db.logAppDao().getAllLogs();

            if (logs == null || logs.isEmpty()) {
                Log.d(TAG, "No hay logs para sincronizar");
                return Result.success();
            }

            Log.d(TAG, "Logs a sincronizar: " + logs.size());


            List<ApiService.LogApp> apiLogs = new ArrayList<>();
            for (LogApp log : logs) {
                apiLogs.add(new ApiService.LogApp(
                        log.getId(),
                        log.getFechaHora(),
                        log.getDescripcionError(),
                        log.getClaseOrigen()
                ));
            }


            ApiService apiService = RetrofitClient.getApiService();
            Response<okhttp3.ResponseBody> response = apiService.enviarLogs(apiLogs).execute();

            if (response.isSuccessful()) {
                Log.d(TAG, "Logs enviados exitosamente");


                db.logAppDao().deleteAll();
                Log.d(TAG, "Logs eliminados de la base de datos local");

                return Result.success();
            } else {
                Log.e(TAG, "Error al enviar logs: " + response.code());
                return Result.retry();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error en sincronización: " + e.getMessage());
            e.printStackTrace();
            return Result.failure();
        }
    }
}