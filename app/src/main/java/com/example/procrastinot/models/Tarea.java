package com.example.procrastinot.models;

public class Tarea {

    public static final String UNA_VEZ = "UNA_VEZ";
    public static final String DIARIA = "DIARIA";
    public static final String DIAS_SEMANA = "DIAS_SEMANA";

    private long id;
    private long usuarioId;
    private String nombre;
    private String descripcion;

    private int horaInicio;
    private int minutoInicio;

    private String repeticion;
    private String fechaUnica;

    private int duracionMinutos;
    private boolean activa;
    private boolean[] dias = new boolean[7];

    public Tarea() {
        repeticion = UNA_VEZ;
        activa = true;
    }

    public Tarea(long id,
            long usuarioId,
            String nombre,
            String descripcion,
            int horaInicio,
            int minutoInicio,
            String repeticion,
            String fechaUnica,
            int duracionMinutos,
            boolean activa
    ) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.horaInicio = horaInicio;
        this.minutoInicio = minutoInicio;
        this.repeticion = repeticion;
        this.fechaUnica = fechaUnica;
        this.duracionMinutos = duracionMinutos;
        this.activa = activa;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public int getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(int horaInicio) {
        this.horaInicio = horaInicio;
    }

    public int getMinutoInicio() {
        return minutoInicio;
    }

    public void setMinutoInicio(int minutoInicio) {
        this.minutoInicio = minutoInicio;
    }

    public String getRepeticion() {
        return repeticion;
    }

    public void setRepeticion(String repeticion) {
        this.repeticion = repeticion;
    }

    public String getFechaUnica() {
        return fechaUnica;
    }

    public void setFechaUnica(String fechaUnica) {
        this.fechaUnica = fechaUnica;
    }

    public int getDuracionMinutos() {
        return duracionMinutos;
    }

    public void setDuracionMinutos(int duracionMinutos) {
        this.duracionMinutos = duracionMinutos;
    }

    public boolean isActiva() {
        return activa;
    }

    public void setActiva(boolean activa) {
        this.activa = activa;
    }

    public boolean[] getDias() {
        return dias.clone();
    }

    public void setDias(boolean[] dias) {
        if (dias == null || dias.length != 7) {
            throw new IllegalArgumentException(
                    "Se necesitan siete posiciones para los días"
            );
        }

        this.dias = dias.clone();
    }

}