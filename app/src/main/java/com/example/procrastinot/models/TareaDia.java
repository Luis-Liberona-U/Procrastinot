package com.example.procrastinot.models;

public class TareaDia {

    public static final int LUNES = 1;
    public static final int MARTES = 2;
    public static final int MIERCOLES = 3;
    public static final int JUEVES = 4;
    public static final int VIERNES = 5;
    public static final int SABADO = 6;
    public static final int DOMINGO = 7;

    private long tareaId;
    private int diaSemana;

    public TareaDia() {
    }

    public TareaDia(long tareaId, int diaSemana) {
        this.tareaId = tareaId;
        setDiaSemana(diaSemana);
    }

    public long getTareaId() {
        return tareaId;
    }

    public void setTareaId(long tareaId) {
        this.tareaId = tareaId;
    }

    public int getDiaSemana() {
        return diaSemana;
    }

    public void setDiaSemana(int diaSemana) {
        if (diaSemana < LUNES || diaSemana > DOMINGO) {
            throw new IllegalArgumentException(
                    "El día debe estar entre 1 y 7"
            );
        }

        this.diaSemana = diaSemana;
    }
}