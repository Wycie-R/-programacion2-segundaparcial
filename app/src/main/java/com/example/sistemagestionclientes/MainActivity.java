package com.example.sistemagestionclientes;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.gson.Gson;
import com.example.sistemagestionclientes.network.ApiService;
import com.example.sistemagestionclientes.network.RetrofitClient;
import com.example.sistemagestionclientes.util.LoggerUtil;
import com.example.sistemagestionclientes.worker.SyncLogsWorker;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private EditText etCI, etNombre, etDireccion, etTelefono;
    private ImageView ivFoto1, ivFoto2, ivFoto3;
    private Button btnFoto1, btnFoto2, btnFoto3, btnEnviar, btnCargaArchivos;

    private File photoFile1, photoFile2, photoFile3;
    private int currentPhotoIndex = 0;

    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> permissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inicializarVistas();
        configurarWorkManager();
        configurarLaunchers();
        configurarListeners();
    }

    private void inicializarVistas() {
        etCI = findViewById(R.id.etCI);
        etNombre = findViewById(R.id.etNombre);
        etDireccion = findViewById(R.id.etDireccion);
        etTelefono = findViewById(R.id.etTelefono);

        ivFoto1 = findViewById(R.id.ivFoto1);
        ivFoto2 = findViewById(R.id.ivFoto2);
        ivFoto3 = findViewById(R.id.ivFoto3);

        btnFoto1 = findViewById(R.id.btnFoto1);
        btnFoto2 = findViewById(R.id.btnFoto2);
        btnFoto3 = findViewById(R.id.btnFoto3);
        btnEnviar = findViewById(R.id.btnEnviar);
        btnCargaArchivos = findViewById(R.id.btnCargaArchivos);
    }

    private void configurarWorkManager() {
        Log.d("DEBUG: ", "WorkManager ejecutandose...");
        PeriodicWorkRequest syncWorkRequest = new PeriodicWorkRequest.Builder(
                SyncLogsWorker.class,
                1, // Mínimo permitido por Android
                TimeUnit.MINUTES
        ).build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "sync_logs",
                ExistingPeriodicWorkPolicy.KEEP,
                syncWorkRequest
        );

        Log.d(TAG, "WorkManager configurado");
    }

    private void configurarLaunchers() {
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                                    getContentResolver(),
                                    getCurrentPhotoUri()
                            );

                            switch (currentPhotoIndex) {
                                case 1:
                                    ivFoto1.setImageBitmap(bitmap);
                                    break;
                                case 2:
                                    ivFoto2.setImageBitmap(bitmap);
                                    break;
                                case 3:
                                    ivFoto3.setImageBitmap(bitmap);
                                    break;
                            }

                            Toast.makeText(this, "Foto capturada correctamente", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            LoggerUtil.registrarError(this, "Error al cargar foto: " + e.getMessage(), TAG);
                            Toast.makeText(this, "Error al cargar la foto", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        abrirCamara();
                    } else {
                        Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show();
                        LoggerUtil.registrarError(this, "Permiso de cámara denegado", TAG);
                    }
                }
        );
    }

    private void configurarListeners() {
        btnFoto1.setOnClickListener(v -> {
            currentPhotoIndex = 1;
            verificarPermisoYTomarFoto();
        });

        btnFoto2.setOnClickListener(v -> {
            currentPhotoIndex = 2;
            verificarPermisoYTomarFoto();
        });

        btnFoto3.setOnClickListener(v -> {
            currentPhotoIndex = 3;
            verificarPermisoYTomarFoto();
        });

        btnEnviar.setOnClickListener(v -> enviarDatos());

        btnCargaArchivos.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CargaArchivosActivity.class);
            startActivity(intent);
        });
    }

    private void verificarPermisoYTomarFoto() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            abrirCamara();
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void abrirCamara() {
        try {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            File photoFile = createImageFile();
            setCurrentPhotoFile(photoFile);

            Uri photoURI = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    photoFile
            );

            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            cameraLauncher.launch(takePictureIntent);

        } catch (Exception e) {
            LoggerUtil.registrarError(this, "Error al abrir cámara: " + e.getMessage(), TAG);
            Toast.makeText(this, "Error al abrir la cámara", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() {
        String fileName = "foto_" + System.currentTimeMillis() + ".jpg";
        File storageDir = getExternalFilesDir(null);
        return new File(storageDir, fileName);
    }

    private void setCurrentPhotoFile(File file) {
        switch (currentPhotoIndex) {
            case 1:
                photoFile1 = file;
                break;
            case 2:
                photoFile2 = file;
                break;
            case 3:
                photoFile3 = file;
                break;
        }
    }

    private Uri getCurrentPhotoUri() {
        File file = null;
        switch (currentPhotoIndex) {
            case 1:
                file = photoFile1;
                break;
            case 2:
                file = photoFile2;
                break;
            case 3:
                file = photoFile3;
                break;
        }
        return file != null ? Uri.fromFile(file) : null;
    }

    private void enviarDatos() {
        String ci = etCI.getText().toString().trim();
        String nombre = etNombre.getText().toString().trim();
        String direccion = etDireccion.getText().toString().trim();
        String telefono = etTelefono.getText().toString().trim();

        if (ci.isEmpty() || nombre.isEmpty() || direccion.isEmpty() || telefono.isEmpty()) {
            Toast.makeText(this, "Complete todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        if (photoFile1 == null || photoFile2 == null || photoFile3 == null) {
            Toast.makeText(this, "Debe capturar las 3 fotos", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Crear JSON con los datos
            Map<String, String> datosCliente = new HashMap<>();
            datosCliente.put("ci", ci);
            datosCliente.put("nombre", nombre);
            datosCliente.put("direccion", direccion);
            datosCliente.put("telefono", telefono);

            Gson gson = new Gson();
            String jsonDatos = gson.toJson(datosCliente);

            RequestBody datosBody = RequestBody.create(
                    MediaType.parse("application/json"),
                    jsonDatos
            );

            // Preparar las fotos
            MultipartBody.Part foto1Part = prepararFoto(photoFile1, "fotoCasa1");
            MultipartBody.Part foto2Part = prepararFoto(photoFile2, "fotoCasa2");
            MultipartBody.Part foto3Part = prepararFoto(photoFile3, "fotoCasa3");

            // Enviar
            ApiService apiService = RetrofitClient.getApiService();
            Call<ResponseBody> call = apiService.enviarCliente(datosBody, foto1Part, foto2Part, foto3Part);

            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(MainActivity.this, "Datos enviados correctamente", Toast.LENGTH_SHORT).show();
                        limpiarFormulario();
                    } else {
                        String error = "Error al enviar datos: " + response.code();
                        Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
                        LoggerUtil.registrarError(MainActivity.this, error, TAG);
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    String error = "Error de conexión: " + t.getMessage();
                    Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
                    LoggerUtil.registrarError(MainActivity.this, error, TAG);
                }
            });

        } catch (Exception e) {
            LoggerUtil.registrarError(this, "Error al enviar datos: " + e.getMessage(), TAG);
            Toast.makeText(this, "Error al preparar datos", Toast.LENGTH_SHORT).show();
        }
    }

    private MultipartBody.Part prepararFoto(File file, String partName) {
        RequestBody requestFile = RequestBody.create(
                MediaType.parse("image/jpeg"),
                file
        );
        return MultipartBody.Part.createFormData(partName, file.getName(), requestFile);
    }

    private void limpiarFormulario() {
        etCI.setText("");
        etNombre.setText("");
        etDireccion.setText("");
        etTelefono.setText("");

        ivFoto1.setImageResource(android.R.color.darker_gray);
        ivFoto2.setImageResource(android.R.color.darker_gray);
        ivFoto3.setImageResource(android.R.color.darker_gray);

        photoFile1 = null;
        photoFile2 = null;
        photoFile3 = null;
    }
}