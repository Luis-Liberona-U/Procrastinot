package com.example.procrastinot.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.procrastinot.database.ProcrastinotContract.Usuarios;
import com.example.procrastinot.database.ProcrastinotContract.Tareas;
import com.example.procrastinot.database.ProcrastinotContract.TareaDias;
import com.example.procrastinot.database.ProcrastinotContract.RegistroTarea;

public class ProcrastinotDbHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "procrastinot.db";
    public static final int DATABASE_VERSION = 1;

    private static final String SQL_CREATE_USUARIOS =
            "CREATE TABLE " + Usuarios.TABLE_NAME + " (" +
                    Usuarios._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    Usuarios.COLUMN_NOMBRE + " TEXT NOT NULL, " +
                    Usuarios.COLUMN_CORREO + " TEXT NOT NULL UNIQUE, " +
                    Usuarios.COLUMN_PASSWORD_HASH + " TEXT NOT NULL" +
                    ")";

    private static final String SQL_CREATE_TAREAS =
            "CREATE TABLE " + Tareas.TABLE_NAME + " (" +
                    Tareas._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    Tareas.COLUMN_USUARIO_ID + " INTEGER NOT NULL, " +
                    Tareas.COLUMN_NOMBRE + " TEXT NOT NULL, " +
                    Tareas.COLUMN_DESCRIPCION + " TEXT, " +

                    Tareas.COLUMN_HORA_INICIO + " INTEGER NOT NULL CHECK(" +
                    Tareas.COLUMN_HORA_INICIO + " BETWEEN 0 AND 23), " +

                    Tareas.COLUMN_MINUTO_INICIO + " INTEGER NOT NULL CHECK(" + Tareas.COLUMN_MINUTO_INICIO + " BETWEEN 0 AND 59), " +

                    Tareas.COLUMN_REPETICION + " TEXT NOT NULL CHECK(" + Tareas.COLUMN_REPETICION + " IN ('UNA_VEZ', 'DIARIA', 'DIAS_SEMANA')), " +

                    Tareas.COLUMN_FECHA_UNICA + " TEXT, " +

                    Tareas.COLUMN_DURACION +
                    " INTEGER NOT NULL CHECK(" +
                    Tareas.COLUMN_DURACION + " > 0), " +

                    Tareas.COLUMN_ACTIVA +
                    " INTEGER NOT NULL DEFAULT 1 CHECK(" +
                    Tareas.COLUMN_ACTIVA + " IN (0, 1)), " +

                    "CHECK(" + Tareas.COLUMN_REPETICION +
                    " != 'UNA_VEZ' OR " +
                    Tareas.COLUMN_FECHA_UNICA + " IS NOT NULL), " +

                    "FOREIGN KEY(" + Tareas.COLUMN_USUARIO_ID + ") " +
                    "REFERENCES " + Usuarios.TABLE_NAME +
                    "(" + Usuarios._ID + ")" +
                    ")";

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

    public ProcrastinotDbHelper(Context context) {
        super(
                context.getApplicationContext(),
                DATABASE_NAME,
                null,
                DATABASE_VERSION
        );
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_USUARIOS);
        db.execSQL(SQL_CREATE_TAREAS);
        db.execSQL(SQL_CREATE_TAREA_DIAS);
        db.execSQL(SQL_CREATE_REGISTRO_TAREA);
    }

    @Override
    public void onUpgrade(
            SQLiteDatabase db,
            int oldVersion,
            int newVersion
    ) {

    }
}