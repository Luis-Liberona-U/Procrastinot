package com.example.procrastinot.models;

/**
 * Representa la relación entre una tarea y un día de repetición semanal.
 *
 * Cada objeto corresponde a una combinación de tareaId y diaSemana de la tabla tarea_dias.
 *
 * Los días se numeran del 1 al 7, desde lunes hasta domingo.
 */
public class TareaDia {

    public static final int LUNES = 1;
    public static final int MARTES = 2;
    public static final int MIERCOLES = 3;
    public static final int JUEVES = 4;
    public static final int VIERNES = 5;
    public static final int SABADO = 6;
    public static final int DOMINGO = 7;

    /** Identificador de la tarea asociada al día de repetición. */
    private long tareaId;

    /** Día de repetición: 1 corresponde a lunes y 7 a domingo. */
    private int diaSemana;


    /**
     * Crea un objeto sin datos asignados explícitamente.
     *
     * El día permanece en 0 hasta asignar un valor válido mediante setDiaSemana().
     */
    public TareaDia() {
    }

    /**
     * Crea una relación entre una tarea y un día de la semana.
     *
     * Utiliza setDiaSemana() para validar el día recibido.
     *
     * @param tareaId Identificador de la tarea asociada.
     * @param diaSemana Día de repetición, entre 1 y 7.
     * @throws IllegalArgumentException Si el día está fuera del rango permitido.
     */
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

    /**
     * Asigna el día de repetición después de validar su rango.
     *
     * @param diaSemana Día de la semana: 1 para lunes hasta 7 para domingo.
     * @throws IllegalArgumentException Si el valor es menor que 1 o mayor que 7.
     */
    public void setDiaSemana(int diaSemana) {
        if (diaSemana < LUNES || diaSemana > DOMINGO) {
            throw new IllegalArgumentException(
                    "El día debe estar entre 1 y 7"
            );
        }

        this.diaSemana = diaSemana;
    }
}