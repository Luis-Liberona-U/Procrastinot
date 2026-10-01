package com.example.procrastinot.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class ProcrastinotDb extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "procrastinot.db";
    private static final int DATABASE_VERSION = 1;

    public ProcrastinotDb(Context context) {
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

        // Comprobacion claves foraneas
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        //Tabla USUARIOS
        db.execSQL(
                "CREATE TABLE usuarios (" +
                        "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "nombre TEXT NOT NULL, " +
                        "correo TEXT NOT NULL UNIQUE, " +
                        "password_hash TEXT NOT NULL" +
                        ")"
        );

        //Tabla TAREAS
        db.execSQL(
                "CREATE TABLE tareas (" +
                        "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "usuario_id INTEGER NOT NULL, " +
                        "nombre TEXT NOT NULL, " +
                        "descripcion TEXT, " +
                        "hora_inicio INTEGER NOT NULL " +
                        "CHECK(hora_inicio BETWEEN 0 AND 23), " +
                        "minuto_inicio INTEGER NOT NULL " +
                        "CHECK(minuto_inicio BETWEEN 0 AND 59), " +
                        "repeticion TEXT NOT NULL " +
                        "CHECK(repeticion IN " +
                        "('UNA_VEZ', 'DIARIA', 'DIAS_SEMANA')), " +
                        "fecha_unica TEXT, " +
                        "duracion_minutos INTEGER NOT NULL " +
                        "CHECK(duracion_minutos > 0), " +
                        "activa INTEGER NOT NULL DEFAULT 1 " +
                        "CHECK(activa IN (0, 1)), " +

                        "CHECK(repeticion != 'UNA_VEZ' " +
                        "OR fecha_unica IS NOT NULL), " +

                        "FOREIGN KEY(usuario_id) " +
                        "REFERENCES usuarios(_id)" +
                        ")"
        );

            // Tabla TAREAS_DIAS
        db.execSQL(
                "CREATE TABLE tarea_dias (" +
                        "tarea_id INTEGER NOT NULL, " +
                        "dia_semana INTEGER NOT NULL " +
                        "CHECK(dia_semana BETWEEN 1 AND 7), " +

                        "PRIMARY KEY(tarea_id, dia_semana), " +

                        "FOREIGN KEY(tarea_id) " +
                        "REFERENCES tareas(_id) " +
                        "ON DELETE CASCADE" +
                        ")"
        );

        //Tabla REGISTRO_TAREA
        db.execSQL(
                "CREATE TABLE registro_tarea (" +
                        "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "tarea_id INTEGER NOT NULL, " +
                        "inicio_programado INTEGER NOT NULL, " +
                        "inicio_real INTEGER, " +
                        "fin_real INTEGER, " +
                        "estado TEXT NOT NULL DEFAULT 'PENDIENTE' " +
                        "CHECK(estado IN " +
                        "('PENDIENTE', 'EN_CURSO', " +
                        "'COMPLETADA', 'OMITIDA')), " +
                        "foto_antes TEXT, " +
                        "foto_despues TEXT, " +

                        "UNIQUE(tarea_id, inicio_programado), " +

                        "FOREIGN KEY(tarea_id) " +
                        "REFERENCES tareas(_id)" +
                        ")"
        );
    }

    @Override
    public void onUpgrade(
            SQLiteDatabase db,
            int oldVersion,
            int newVersion
    ){}
}