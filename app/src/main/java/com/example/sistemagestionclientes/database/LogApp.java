package com.example.sistemagestionclientes.database;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "logs_app")
public class LogApp {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String fechaHora;
    private String descripcionError;
    private String claseOrigen;

    @Ignore
    public LogApp() {
    }

    public LogApp(String fechaHora, String descripcionError, String claseOrigen) {
        this.fechaHora = fechaHora;
        this.descripcionError = descripcionError;
        this.claseOrigen = claseOrigen;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(String fechaHora) {
        this.fechaHora = fechaHora;
    }

    public String getDescripcionError() {
        return descripcionError;
    }

    public void setDescripcionError(String descripcionError) {
        this.descripcionError = descripcionError;
    }

    public String getClaseOrigen() {
        return claseOrigen;
    }

    public void setClaseOrigen(String claseOrigen) {
        this.claseOrigen = claseOrigen;
    }
}