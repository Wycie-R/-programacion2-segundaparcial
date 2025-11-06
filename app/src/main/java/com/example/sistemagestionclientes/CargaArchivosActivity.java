package com.example.sistemagestionclientes;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.sistemagestionclientes.network.ApiService;
import com.example.sistemagestionclientes.network.RetrofitClient;
import com.example.sistemagestionclientes.util.LoggerUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CargaArchivosActivity extends AppCompatActivity {

    private static final String TAG = "CargaArchivosActivity";

    private EditText etCIArchivos;
    private TextView tvArchivosSeleccionados;
    private Button btnSeleccionarArchivos, btnEnviarArchivos;

    private List<Uri> archivosSeleccionados = new ArrayList<>();
    private ActivityResultLauncher<Intent> seleccionarArchivosLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_carga_archivos);

        inicializarVistas();
        configurarLaunchers();
        configurarListeners();
    }

    private void inicializarVistas() {
        etCIArchivos = findViewById(R.id.etCIArchivos);
        tvArchivosSeleccionados = findViewById(R.id.tvArchivosSeleccionados);
        btnSeleccionarArchivos = findViewById(R.id.btnSeleccionarArchivos);
        btnEnviarArchivos = findViewById(R.id.btnEnviarArchivos);
    }

    private void configurarLaunchers() {
        seleccionarArchivosLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        archivosSeleccionados.clear();

                        if (data.getClipData() != null) {
                            // Múltiples archivos
                            int count = data.getClipData().getItemCount();
                            for (int i = 0; i < count; i++) {
                                Uri uri = data.getClipData().getItemAt(i).getUri();
                                archivosSeleccionados.add(uri);
                            }
                        } else if (data.getData() != null) {
                            // Un solo archivo
                            archivosSeleccionados.add(data.getData());
                        }

                        actualizarTextoArchivos();
                    }
                }
        );
    }

    private void configurarListeners() {
        btnSeleccionarArchivos.setOnClickListener(v -> seleccionarArchivos());
        btnEnviarArchivos.setOnClickListener(v -> enviarArchivos());
    }

    private void seleccionarArchivos() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        seleccionarArchivosLauncher.launch(intent);
    }

    private void actualizarTextoArchivos() {
        if (archivosSeleccionados.isEmpty()) {
            tvArchivosSeleccionados.setText("No hay archivos seleccionados");
        } else {
            tvArchivosSeleccionados.setText("Archivos seleccionados: " + archivosSeleccionados.size());
        }
    }

    private void enviarArchivos() {
        String ci = etCIArchivos.getText().toString().trim();

        if (ci.isEmpty()) {
            Toast.makeText(this, "Ingrese el CI del cliente", Toast.LENGTH_SHORT).show();
            return;
        }

        if (archivosSeleccionados.isEmpty()) {
            Toast.makeText(this, "Seleccione al menos un archivo", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Crear archivo ZIP
            File zipFile = crearArchivoZip();

            if (zipFile == null) {
                Toast.makeText(this, "Error al crear archivo ZIP", Toast.LENGTH_SHORT).show();
                return;
            }

            // Preparar datos para enviar
            RequestBody ciBody = RequestBody.create(
                    MediaType.parse("text/plain"),
                    ci
            );

            RequestBody zipRequestBody = RequestBody.create(
                    MediaType.parse("application/zip"),
                    zipFile
            );

            MultipartBody.Part zipPart = MultipartBody.Part.createFormData(
                    "archivo_zip",
                    zipFile.getName(),
                    zipRequestBody
            );

            // Enviar al servidor
            ApiService apiService = RetrofitClient.getApiService();
            Call<ResponseBody> call = apiService.enviarArchivos(ciBody, zipPart);

            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(CargaArchivosActivity.this,
                                "Archivos enviados correctamente", Toast.LENGTH_SHORT).show();
                        limpiarFormulario();

                        // Eliminar archivo ZIP temporal
                        if (zipFile.exists()) {
                            zipFile.delete();
                        }
                    } else {
                        String error = "Error al enviar archivos: " + response.code();
                        Toast.makeText(CargaArchivosActivity.this, error, Toast.LENGTH_SHORT).show();
                        LoggerUtil.registrarError(CargaArchivosActivity.this, error, TAG);
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    String error = "Error de conexión: " + t.getMessage();
                    Toast.makeText(CargaArchivosActivity.this, error, Toast.LENGTH_SHORT).show();
                    LoggerUtil.registrarError(CargaArchivosActivity.this, error, TAG);
                }
            });

        } catch (Exception e) {
            LoggerUtil.registrarError(this, "Error al preparar archivos: " + e.getMessage(), TAG);
            Toast.makeText(this, "Error al preparar archivos", Toast.LENGTH_SHORT).show();
        }
    }

    private File crearArchivoZip() {
        try {
            File zipFile = new File(getExternalFilesDir(null),
                    "archivos_" + System.currentTimeMillis() + ".zip");

            FileOutputStream fos = new FileOutputStream(zipFile);
            ZipOutputStream zos = new ZipOutputStream(fos);

            byte[] buffer = new byte[1024];

            for (int i = 0; i < archivosSeleccionados.size(); i++) {
                Uri uri = archivosSeleccionados.get(i);

                try {
                    InputStream is = getContentResolver().openInputStream(uri);

                    String fileName = "archivo_" + i + "_" + System.currentTimeMillis();
                    ZipEntry zipEntry = new ZipEntry(fileName);
                    zos.putNextEntry(zipEntry);

                    int length;
                    while ((length = is.read(buffer)) > 0) {
                        zos.write(buffer, 0, length);
                    }

                    is.close();
                    zos.closeEntry();

                } catch (Exception e) {
                    Log.e(TAG, "Error al procesar archivo: " + e.getMessage());
                    LoggerUtil.registrarError(this,
                            "Error al procesar archivo: " + e.getMessage(), TAG);
                }
            }

            zos.close();
            fos.close();

            return zipFile;

        } catch (Exception e) {
            Log.e(TAG, "Error al crear ZIP: " + e.getMessage());
            LoggerUtil.registrarError(this, "Error al crear ZIP: " + e.getMessage(), TAG);
            return null;
        }
    }

    private void limpiarFormulario() {
        etCIArchivos.setText("");
        archivosSeleccionados.clear();
        actualizarTextoArchivos();
    }
}