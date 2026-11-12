package com.example.procrastinot.database;

import android.provider.BaseColumns;

/**
 * Centraliza los nombres de las tablas y columnas de SQLite.
 *
 * Los repositorios y ProcrastinotDbHelper utilizan estas constantes
 * para crear tablas y realizar consultas de forma consistente.
 *
 * Esta clase no crea la base de datos ni ejecuta operaciones SQL.
 */


public final class ProcrastinotContract {

    private ProcrastinotContract() {
    }

    /**
     * Define los nombres de la tabla de usuarios y sus columnas.
     *
     * Cada usuario tiene un nombre, un correo y una contraseña.
     * BaseColumns proporciona la constante _ID con el valor "_id".
     */

    public static final class Usuarios implements BaseColumns {

        public static final String TABLE_NAME = "usuarios";

        public static final String COLUMN_NOMBRE = "nombre";
        public static final String COLUMN_CORREO = "correo";
        public static final String COLUMN_PASSWORD_HASH = "password_hash";

        private Usuarios() {
        }
    }

    /**
     * Define los nombres de la tabla de tareas y sus columnas.
     *
     * Cada tarea pertenece a un usuario y contiene su configuración
     * de horario, repetición y duración estimada.
     */
    public static final class Tareas implements BaseColumns {

        public static final String TABLE_NAME = "tareas";

        /** Identificador del usuario propietario de la tarea. */
        public static final String COLUMN_USUARIO_ID = "usuario_id";
        public static final String COLUMN_NOMBRE = "nombre";
        public static final String COLUMN_DESCRIPCION = "descripcion";

        /** Hora de inicio, entre 0 y 23. */
        public static final String COLUMN_HORA_INICIO = "hora_inicio";

        /** Minuto de inicio, entre 0 y 59. */
        public static final String COLUMN_MINUTO_INICIO = "minuto_inicio";

        /** Tipo de repetición: UNA_VEZ, DIARIA o DIAS_SEMANA. */
        public static final String COLUMN_REPETICION = "repeticion";

        /** Fecha en formato yyyy-MM-dd para las tareas de una sola vez. */
        public static final String COLUMN_FECHA_UNICA = "fecha_unica";

        /** Duración estimada de la tarea, expresada en minutos. */
        public static final String COLUMN_DURACION = "duracion_minutos";

        /** Indica si la tarea está habilitada: 1 activa, 0 desactivada. */
        public static final String COLUMN_ACTIVA = "activa";

        /**
         * Indica la eliminación lógica: 1 eliminada, 0 no eliminada.
         * Permite ocultar la tarea sin borrar sus registros históricos.
         */
        public static final String COLUMN_ELIMINADA = "eliminada";


        private Tareas() {
        }
    }

    /**
     * Define la tabla que almacena los días de repetición semanal.
     *
     * Cada fila relaciona una tarea con uno de sus días seleccionados.
     */

    public static final class TareaDias {

        public static final String TABLE_NAME = "tarea_dias";

        /** Identificador de la tarea asociada. */
        public static final String COLUMN_TAREA_ID = "tarea_id";
        /** Día de la semana: 1 corresponde a lunes y 7 a domingo. */
        public static final String COLUMN_DIA_SEMANA = "dia_semana";

        private TareaDias() {
        }
    }

    /**
     * Define la tabla que almacena cada realización de una tarea.
     *
     * Una tarea puede tener varios registros a lo largo del tiempo.
     * Cada registro contiene el horario programado, los tiempos reales,
     * su estado y las rutas de las fotografías.
     *
     * Las fotografías se almacenan localmente como archivos  en el dispositivo;
     * esta tabla guarda sus rutas.
     */
    public static final class RegistroTarea implements BaseColumns {

        public static final String TABLE_NAME = "registro_tarea";

        /** Identificador de la tarea que se realiza. */
        public static final String COLUMN_TAREA_ID = "tarea_id";
        public static final String COLUMN_INICIO_PROGRAMADO =
                "inicio_programado";
        public static final String COLUMN_INICIO_REAL = "inicio_real";
        public static final String COLUMN_FIN_REAL = "fin_real";

        /** Estado: PENDIENTE, EN_CURSO, COMPLETADA u OMITIDA. */
        public static final String COLUMN_ESTADO = "estado";

        /** Ruta del archivo de la fotografía inicial. */
        public static final String COLUMN_FOTO_ANTES = "foto_antes";

        /** Ruta del archivo de la fotografía final. */
        public static final String COLUMN_FOTO_DESPUES = "foto_despues";

        private RegistroTarea() {
        }
    }
}