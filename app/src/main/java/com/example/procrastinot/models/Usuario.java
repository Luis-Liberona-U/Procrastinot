package com.example.procrastinot.models;

public class Usuario {

    private long id;
    private String nombre;
    private String correo;
    private String passwordHash;

    public Usuario() {
    }

    public Usuario(long id, String nombre, String correo) {
        this.id = id;
        this.nombre = nombre;
        this.correo = correo;
    }

    public Usuario(
            long id,
            String nombre,
            String correo,
            String passwordHash
    ) {
        this(id, nombre, correo);
        this.passwordHash = passwordHash;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
}