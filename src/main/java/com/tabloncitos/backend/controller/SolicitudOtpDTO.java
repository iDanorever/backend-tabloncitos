package com.tabloncitos.backend.controller;

public class SolicitudOtpDTO {
    

    
    private String codigo;          // Ej: EMP-2044
    private String nombreEmpleado;  // Ej: Sara Jiménez
    private String otpGenerado;     // Ej: 849201

    // Getters y Setters
    public String getCodigo() {
        return codigo;
    }
    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNombreEmpleado() {
        return nombreEmpleado;
    }
    public void setNombreEmpleado(String nombreEmpleado) {
        this.nombreEmpleado = nombreEmpleado;
    }

    public String getOtpGenerado() {
        return otpGenerado;
    }
    public void setOtpGenerado(String otpGenerado) {
        this.otpGenerado = otpGenerado;
    }
}