package com.example.procrastinot.models;


/**
 * Representa una realización de una tarea.
 *
 * Una misma tarea puede tener varios registros correspondientes a distintos inicios programados.
 *
 * Contiene los tiempos, el estado y las rutas de las fotografías.
 * Esta clase no ejecuta consultas SQL, controla el cronómetro ni válida las transiciones entre estados.
 *
 * Los tiempos se expresan en milisegundos desde la época Unix.
 */

public class RegistroTarea {

    /** Estado que representa una realización pendiente de inicio. */
    public static final String PENDIENTE = "PENDIENTE";

    /**
     * Estado de una realización iniciada.
     * En el flujo actual se mantiene hasta confirmar la fotografía final, aunque ya se haya registrado el fin real.
     */
    public static final String EN_CURSO = "EN_CURSO";

    /** Estado asignado al confirmar y guardar el resultado final. */
    public static final String COMPLETADA = "COMPLETADA";

    /** Estado disponible para representar una realización omitida. */
    public static final String OMITIDA = "OMITIDA";

    /** Identificador Id de la realización en registro en SQLite. */
    private long id;

    /** Identificador de la tarea a la que pertenece esta realización. */
    private long tareaId;

    /** Instante previsto para comenzar la tarea, en milisegundos. */
    private long inicioProgramado;

    /**
     * Instante efectivo de inicio, en milisegundos.
     * Utiliza Long para permitir null cuando no se ha registrado.
     */
    private Long inicioReal;

    /**
     * Instante efectivo de finalización, en milisegundos.
     * Utiliza Long para permitir null cuando no se ha registrado.
     */
    private Long finReal;

    /** Estado de la realización. */
    private String estado;

    /** Ruta del archivo de la fotografía inicial. */
    private String fotoAntes;

    /** Ruta del archivo de la fotografía final. */
    private String fotoDespues;


    /**
     * Nombre de la tarea utilizado para mostrar el historial.
     *
     * Se obtiene al consultar la tabla tareas mediante un JOIN.
     * No corresponde a una columna de registro_tarea.
     */
    // Atributo usado para el Historial
    private String nombreTarea;


    /**
     * Crea un registro con estado PENDIENTE.
     *
     * Los tiempos reales y las rutas permanecen en null hasta que se asignen.
     * Los identificadores y el inicio programado conservan inicialmente el valor 0.
     */
    public RegistroTarea() {
        estado = PENDIENTE;
    }


    /**
     * Crea un registro con los datos recibidos, sin validarlos.
     *
     * El nombre de la tarea puede asignarse posteriormente mediante setNombreTarea().
     *
     * @param id Identificador del registro en SQLite.
     * @param tareaId Identificador de la tarea asociada.
     * @param inicioProgramado Inicio previsto en milisegundos desde la época Unix.
     * @param inicioReal Inicio efectivo en milisegundos, o null si no está registrado.
     * @param finReal Final efectivo en milisegundos, o null si no está registrado.
     * @param estado Estado de la realización.
     * @param fotoAntes Ruta de la fotografía inicial, o null si no está asignada.
     * @param fotoDespues Ruta de la fotografía final, o null si no está asignada.
     */
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

    // Acceso al nombre obtenido para presentar el historial.
    public String getNombreTarea() {return nombreTarea;}


    public void setNombreTarea(String nombreTarea) {this.nombreTarea = nombreTarea;}
}
