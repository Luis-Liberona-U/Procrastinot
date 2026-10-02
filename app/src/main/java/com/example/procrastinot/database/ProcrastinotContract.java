package com.example.procrastinot.database;

import android.provider.BaseColumns;

public final class ProcrastinotContract {

    private ProcrastinotContract() {
    }

    public static final class Usuarios implements BaseColumns {

        public static final String TABLE_NAME = "usuarios";

        public static final String COLUMN_NOMBRE = "nombre";
        public static final String COLUMN_CORREO = "correo";
        public static final String COLUMN_PASSWORD_HASH = "password_hash";

        private Usuarios() {
        }
    }

    public static final class Tareas implements BaseColumns {

        public static final String TABLE_NAME = "tareas";

        public static final String COLUMN_USUARIO_ID = "usuario_id";
        public static final String COLUMN_NOMBRE = "nombre";
        public static final String COLUMN_DESCRIPCION = "descripcion";
        public static final String COLUMN_HORA_INICIO = "hora_inicio";
        public static final String COLUMN_MINUTO_INICIO = "minuto_inicio";
        public static final String COLUMN_REPETICION = "repeticion";
        public static final String COLUMN_FECHA_UNICA = "fecha_unica";
        public static final String COLUMN_DURACION = "duracion_minutos";
        public static final String COLUMN_ACTIVA = "activa";

        private Tareas() {
        }
    }

    public static final class TareaDias {

        public static final String TABLE_NAME = "tarea_dias";

        public static final String COLUMN_TAREA_ID = "tarea_id";
        public static final String COLUMN_DIA_SEMANA = "dia_semana";

        private TareaDias() {
        }
    }

    public static final class RegistroTarea implements BaseColumns {

        public static final String TABLE_NAME = "registro_tarea";

        public static final String COLUMN_TAREA_ID = "tarea_id";
        public static final String COLUMN_INICIO_PROGRAMADO =
                "inicio_programado";
        public static final String COLUMN_INICIO_REAL = "inicio_real";
        public static final String COLUMN_FIN_REAL = "fin_real";
        public static final String COLUMN_ESTADO = "estado";
        public static final String COLUMN_FOTO_ANTES = "foto_antes";
        public static final String COLUMN_FOTO_DESPUES = "foto_despues";

        private RegistroTarea() {
        }
    }
}