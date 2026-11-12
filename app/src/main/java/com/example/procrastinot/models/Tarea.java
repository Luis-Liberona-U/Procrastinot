package com.example.procrastinot.models;


/**
 * Representa la configuración de una tarea perteneciente a un usuario.
 *
 * Contiene su nombre, descripción, horario, repetición, duración estimada y estados de activación y eliminación lógica.
 *
 * Las realizaciones, tiempos reales y fotografías se representan por separado mediante el modelo RegistroTarea.
 *
 */

public class Tarea {

    /** La tarea está programada para una única fecha. */
    public static final String UNA_VEZ = "UNA_VEZ";

    /** La tarea se repite todos los días. */
    public static final String DIARIA = "DIARIA";

    /** La tarea se repite los días de la semana seleccionados. */
    public static final String DIAS_SEMANA = "DIAS_SEMANA";

    /** Identificador de la tarea en SQLite. */
    private long id;

    /** Identificador del usuario propietario de la tarea. */
    private long usuarioId;

    /** Nombre que identifica la tarea. */
    private String nombre;

    /** Descripción opcional de la actividad que se realizará. */
    private String descripcion;

    /** Hora de inicio prevista, entre 0 y 23. */
    private int horaInicio;

    /** Minuto de inicio previsto, entre 0 y 59. */
    private int minutoInicio;

    /** Tipo de repetición: UNA_VEZ, DIARIA o DIAS_SEMANA. */
    private String repeticion;

    /** Fecha en formato yyyy-MM-dd utilizada para la repetición UNA_VEZ. */
    private String fechaUnica;

    /** Tiempo estimado para realizar la tarea, expresado en minutos. */
    private int duracionMinutos;

    /** Indica si la tarea está habilitada para considerarla al seleccionar tareas. */
    private boolean activa;

    /** Indica si la tarea fue eliminada lógicamente. */
    private boolean eliminada = false;


    /**
     * Días seleccionados para la repetición semanal.
     *
     * Las posiciones 0 a 6 corresponden a lunes a domingo.
     * Un valor true indica que el día está seleccionado.
     */
    private boolean[] dias = new boolean[7];



    /**
     * Crea una tarea activa con repetición UNA_VEZ.
     *
     * Inicialmente, no está eliminada y no tiene días seleccionados.
     * Los demás datos deben asignarse posteriormente.
     */
    public Tarea() {
        repeticion = UNA_VEZ;
        activa = true;
    }



    /**
     * Crea una tarea con los datos de configuración recibidos.
     *
     * Inicialmente, no está eliminada y no tiene días seleccionados.
     * Los días semanales pueden asignarse posteriormente con setDias().
     *
     * @param id Identificador de la tarea en SQLite.
     * @param usuarioId Identificador del usuario propietario.
     * @param nombre Nombre de la tarea.
     * @param descripcion Descripción opcional.
     * @param horaInicio Hora prevista de inicio, entre 0 y 23.
     * @param minutoInicio Minuto previsto de inicio, entre 0 y 59.
     * @param repeticion Tipo: UNA_VEZ, DIARIA o DIAS_SEMANA.
     * @param fechaUnica Fecha yyyy-MM-dd para las tareas de una sola vez.
     * @param duracionMinutos Duración estimada en minutos.
     * @param activa true si la tarea está habilitada; false si está desactivada.
     */
    public Tarea(
            long id,
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

    public boolean isEliminada() {
        return eliminada;
    }

    public void setEliminada(boolean eliminada) {
        this.eliminada = eliminada;
    }


    /**
     * Esta función devuelve una copia de los días seleccionados.
     *
     * La copia permite consultar o modificar el arreglo recibido.
     *
     * @return Arreglo independiente de siete posiciones, de lunes a domingo.
     */
    public boolean[] getDias() {
        return dias.clone();
    }


    /**
     * Asigna los días seleccionados mediante una copia del arreglo recibido.
     *
     * @param dias Arreglo de siete posiciones, de lunes a domingo.
     * @throws IllegalArgumentException Si el arreglo es null
     *         o no contiene exactamente siete posiciones.
     */
    public void setDias(boolean[] dias) {
        if (dias == null || dias.length != 7) {
            throw new IllegalArgumentException("Se necesitan siete posiciones para los días");
        }

        this.dias = dias.clone();
    }
}