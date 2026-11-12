package com.example.procrastinot.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.procrastinot.database.ProcrastinotContract.Usuarios;
import com.example.procrastinot.database.ProcrastinotContract.Tareas;
import com.example.procrastinot.database.ProcrastinotContract.TareaDias;
import com.example.procrastinot.database.ProcrastinotContract.RegistroTarea;

/**
 * Administra la creación y actualización de la base de datos local.
 *
 * Utiliza las constantes de ProcrastinotContract para definir las tablas
 * de la base de datos.
 *
 * La base de datos se abre o crea al solicitar una conexión mediante
 * getReadableDatabase() o getWritableDatabase().
 */
public class ProcrastinotDbHelper extends SQLiteOpenHelper {

    /** Nombre del archivo de base de datos almacenado en el dispositivo. */
    public static final String DATABASE_NAME = "procrastinot.db";

    /** Versión de la base de datos */
    public static final int DATABASE_VERSION = 2;

    /**
     * Crea la tabla de usuarios.
     *
     * El correo es obligatorio y único para evitar cuentas duplicadas.
     * Esta sentencia define el almacenamiento de la contraseña.
     */
    private static final String SQL_CREATE_USUARIOS =
            "CREATE TABLE " + Usuarios.TABLE_NAME + " (" +
                    Usuarios._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    Usuarios.COLUMN_NOMBRE + " TEXT NOT NULL, " +
                    Usuarios.COLUMN_CORREO + " TEXT NOT NULL UNIQUE, " +
                    Usuarios.COLUMN_PASSWORD_HASH + " TEXT NOT NULL" +
                    ")";

    /**
     * Crea la tabla de configuración de tareas.
     *
     * Cada tarea pertenece a un usuario. Las restricciones CHECK validan
     * la hora, el minuto, la repetición, la duración y los estados.
     * Las tareas de una sola vez deben tener una fecha asignada.
     *
     * Los campos activa y eliminada se almacenan como enteros 0 o 1.
     */
    private static final String SQL_CREATE_TAREAS =
            "CREATE TABLE " + Tareas.TABLE_NAME + " (" +

                    Tareas._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    Tareas.COLUMN_USUARIO_ID + " INTEGER NOT NULL, " +
                    Tareas.COLUMN_NOMBRE + " TEXT NOT NULL, " +
                    Tareas.COLUMN_DESCRIPCION + " TEXT, " +

                    Tareas.COLUMN_HORA_INICIO +
                    " INTEGER NOT NULL CHECK(" +
                    Tareas.COLUMN_HORA_INICIO +
                    " BETWEEN 0 AND 23), " +

                    Tareas.COLUMN_MINUTO_INICIO +
                    " INTEGER NOT NULL CHECK(" +
                    Tareas.COLUMN_MINUTO_INICIO +
                    " BETWEEN 0 AND 59), " +

                    Tareas.COLUMN_REPETICION +
                    " TEXT NOT NULL CHECK(" +
                    Tareas.COLUMN_REPETICION +
                    " IN ('UNA_VEZ', 'DIARIA', 'DIAS_SEMANA')), " +

                    Tareas.COLUMN_FECHA_UNICA + " TEXT, " +

                    Tareas.COLUMN_DURACION +
                    " INTEGER NOT NULL CHECK(" +
                    Tareas.COLUMN_DURACION + " > 0), " +

                    Tareas.COLUMN_ACTIVA +
                    " INTEGER NOT NULL DEFAULT 1 CHECK(" +
                    Tareas.COLUMN_ACTIVA + " IN (0, 1)), " +

                    Tareas.COLUMN_ELIMINADA +
                    " INTEGER NOT NULL DEFAULT 0 CHECK(" +
                    Tareas.COLUMN_ELIMINADA + " IN (0, 1)), " +

                    "CHECK(" + Tareas.COLUMN_REPETICION +
                    " != 'UNA_VEZ' OR " +
                    Tareas.COLUMN_FECHA_UNICA + " IS NOT NULL), " +

                    "FOREIGN KEY(" + Tareas.COLUMN_USUARIO_ID + ") " +
                    "REFERENCES " + Usuarios.TABLE_NAME +
                    "(" + Usuarios._ID + ")" +

                    ")";

    /**
     * Crea la tabla de días de repetición semanal.
     *
     * La clave primaria compuesta impide repetir el mismo día
     * para una tarea. Los días admitidos van del 1 al 7.
     *
     * ON DELETE CASCADE elimina estas filas si se borra físicamente
     * la tarea asociada.
     */
    private static final String SQL_CREATE_TAREA_DIAS =
            "CREATE TABLE " + TareaDias.TABLE_NAME + " (" +
                    TareaDias.COLUMN_TAREA_ID + " INTEGER NOT NULL, " +

                    TareaDias.COLUMN_DIA_SEMANA +
                    " INTEGER NOT NULL CHECK(" +
                    TareaDias.COLUMN_DIA_SEMANA + " BETWEEN 1 AND 7), " +

                    "PRIMARY KEY(" +
                    TareaDias.COLUMN_TAREA_ID + ", " +
                    TareaDias.COLUMN_DIA_SEMANA + "), " +

                    "FOREIGN KEY(" + TareaDias.COLUMN_TAREA_ID + ") " +
                    "REFERENCES " + Tareas.TABLE_NAME +
                    "(" + Tareas._ID + ") ON DELETE CASCADE" +
                    ")";

    /**
     * Crea la tabla de registros de realización de tareas.
     *
     * Guarda el inicio programado, los tiempos reales, el estado
     * y las rutas de las fotografías de cada tarea realizada.
     *
     * La restricción UNIQUE impide registrar dos veces una misma tarea
     * con el mismo inicio programado.
     *
     * ON DELETE CASCADE elimina los registros si se borra físicamente
     * la tarea. Marcarla como eliminada conserva su historial.
     */
    private static final String SQL_CREATE_REGISTRO_TAREA =
            "CREATE TABLE " + RegistroTarea.TABLE_NAME + " (" +
                    RegistroTarea._ID +
                    " INTEGER PRIMARY KEY AUTOINCREMENT, " +

                    RegistroTarea.COLUMN_TAREA_ID + " INTEGER NOT NULL, " +
                    RegistroTarea.COLUMN_INICIO_PROGRAMADO +
                    " INTEGER NOT NULL, " +
                    RegistroTarea.COLUMN_INICIO_REAL + " INTEGER, " +
                    RegistroTarea.COLUMN_FIN_REAL + " INTEGER, " +

                    RegistroTarea.COLUMN_ESTADO +
                    " TEXT NOT NULL DEFAULT 'PENDIENTE' CHECK(" +
                    RegistroTarea.COLUMN_ESTADO +
                    " IN ('PENDIENTE', 'EN_CURSO', " +
                    "'COMPLETADA', 'OMITIDA')), " +

                    RegistroTarea.COLUMN_FOTO_ANTES + " TEXT, " +
                    RegistroTarea.COLUMN_FOTO_DESPUES + " TEXT, " +

                    "UNIQUE(" +
                    RegistroTarea.COLUMN_TAREA_ID + ", " +
                    RegistroTarea.COLUMN_INICIO_PROGRAMADO + "), " +

                    "FOREIGN KEY(" + RegistroTarea.COLUMN_TAREA_ID + ") " +
                    "REFERENCES " + Tareas.TABLE_NAME +
                    "(" + Tareas._ID + ") ON DELETE CASCADE" +
                    ")";

    /**
     * Inicializa el administrador con el nombre y versión de la base.
     *
     * Utiliza el contexto de la aplicación para no conservar
     * una referencia al contexto de una Activity.
     *
     * @param context Contexto utilizado para acceder a la aplicación.
     */
    public ProcrastinotDbHelper(Context context) {
        super(
                context.getApplicationContext(),
                DATABASE_NAME,
                null,
                DATABASE_VERSION
        );
    }

    /**
     * Configura la conexión antes de crear o actualizar las tablas.
     *
     * Activa las claves foráneas para que SQLite compruebe
     * las relaciones entre los registros.
     *
     * @param db Conexión a la base de datos que se está configurando.
     */
    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    /**
     * Crea las cuatro tablas cuando la base de datos se crea por primera vez.
     *
     * Primero crea usuarios y tareas, y después las tablas
     * que dependen de ellas mediante claves foráneas.
     *
     * @param db Conexión donde se ejecutan las sentencias de creación.
     * @throws android.database.sqlite.SQLiteException
     *         Si falla la ejecución de una sentencia SQL.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_USUARIOS);
        db.execSQL(SQL_CREATE_TAREAS);
        db.execSQL(SQL_CREATE_TAREA_DIAS);
        db.execSQL(SQL_CREATE_REGISTRO_TAREA);
    }

    /**
     * Actualiza el esquema de una base de datos existente.
     *
     * Actualmente, se encuentra en la version 2 debido agregar la columna "eliminada"
     * a la tabla de tareas sin borrar los datos guardados.
     * Las tareas existentes reciben el valor 0: no eliminadas.
     *
     * @param db Conexión a la base de datos que se actualizará.
     * @param oldVersion Versión anterior del esquema.
     * @param newVersion Nueva versión solicitada por el Helper.
     * @throws android.database.sqlite.SQLiteException
     *         Si falla la modificación de la tabla.
     */
    @Override
    public void onUpgrade(
            SQLiteDatabase db,
            int oldVersion,
            int newVersion
    ) {
        if (oldVersion < 2) {
            db.execSQL(
                    "ALTER TABLE " + Tareas.TABLE_NAME
                            + " ADD COLUMN " + Tareas.COLUMN_ELIMINADA
                            + " INTEGER NOT NULL DEFAULT 0"
                            + " CHECK ("
                            + Tareas.COLUMN_ELIMINADA
                            + " IN (0, 1))"
            );
        }
    }
}