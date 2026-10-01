package com.example.procrastinot.models;

public class RegistroTarea {
    public static final String PENDIENTE = "PENDIENTE";
    public static final String EN_CURSO = "EN_CURSO";
    public static final String COMPLETADA = "COMPLETADA";
    public static final String OMITIDA = "OMITIDA";

    private long id;
    private long tareaId;

    private long inicioProgramado;
    private Long inicioReal;
    private Long finReal;

    private String estado;
    private String fotoAntes;
    private String fotoDespues;

    public RegistroTarea() {
        estado = PENDIENTE;
    }

    public RegistroTarea(long id,
            long tareaId,
            long inicioProgramado,
            Long inicioReal,
            Long finReal,
            String estado,
            String fotoAntes,
            String fotoDespues
    ) {
        this.id = id;
        this.tareaId = tareaId;
        this.inicioProgramado = inicioProgramado;
        this.inicioReal = inicioReal;
        this.finReal = finReal;
        this.estado = estado;
        this.fotoAntes = fotoAntes;
        this.fotoDespues = fotoDespues;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getTareaId() {
        return tareaId;
    }

    public void setTareaId(long tareaId) {
        this.tareaId = tareaId;
    }

    public long getInicioProgramado() {
        return inicioProgramado;
    }

    public void setInicioProgramado(long inicioProgramado) {
        this.inicioProgramado = inicioProgramado;
    }

    public Long getInicioReal() {
        return inicioReal;
    }

    public void setInicioReal(Long inicioReal) {
        this.inicioReal = inicioReal;
    }

    public Long getFinReal() {
        return finReal;
    }

    public void setFinReal(Long finReal) {
        this.finReal = finReal;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getFotoAntes() {
        return fotoAntes;
    }

    public void setFotoAntes(String fotoAntes) {
        this.fotoAntes = fotoAntes;
    }

    public String getFotoDespues() {
        return fotoDespues;
    }

    public void setFotoDespues(String fotoDespues) {
        this.fotoDespues = fotoDespues;
    }
}
