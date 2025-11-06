package com.example.sistemagestionclientes.network;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface ApiService {

    //https://webhook.site/b9d13067-c0f0-477f-8757-8ff2f48d216b
    @Multipart
    @POST("/b9d13067-c0f0-477f-8757-8ff2f48d216b")
    Call<ResponseBody> enviarCliente(
            @Part("datos") RequestBody datosJson,
            @Part MultipartBody.Part fotoCasa1,
            @Part MultipartBody.Part fotoCasa2,
            @Part MultipartBody.Part fotoCasa3
    );

    @Multipart
    @POST("/b9d13067-c0f0-477f-8757-8ff2f48d216b")
    Call<ResponseBody> enviarArchivos(
            @Part("ci") RequestBody ci,
            @Part MultipartBody.Part archivoZip
    );

    @POST("/b9d13067-c0f0-477f-8757-8ff2f48d216b")
    Call<ResponseBody> enviarLogs(@Body List<LogApp> logs);

    class LogApp {
        public int id;
        public String fechaHora;
        public String descripcionError;
        public String claseOrigen;

        public LogApp(int id, String fechaHora, String descripcionError, String claseOrigen) {
            this.id = id;
            this.fechaHora = fechaHora;
            this.descripcionError = descripcionError;
            this.claseOrigen = claseOrigen;
        }
    }
}