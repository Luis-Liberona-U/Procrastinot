package com.example.procrastinot.repositories;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.procrastinot.database.ProcrastinotContract.RegistroTarea;
import com.example.procrastinot.database.ProcrastinotContract.Tareas;
import com.example.procrastinot.database.ProcrastinotDbHelper;

import java.util.ArrayList;
import java.util.List;

public class RegistroTareaRepository {

    private final ProcrastinotDbHelper helper;

    public RegistroTareaRepository(Context context) {
        helper = new ProcrastinotDbHelper(context);
    }

    public long iniciarRegistro(
            long tareaId,
            long usuarioId,
            long inicioProgramado,
            long inicioReal,
            String fotoAntes
    ) {
        SQLiteDatabase db = helper.getWritableDatabase();

        db.beginTransaction();

        try {
            // Comprueba que la tarea pertenece al usuario y sigue activa y no eliminada.
            String filtro = Tareas._ID + " = ? AND "
                    + Tareas.COLUMN_USUARIO_ID + " = ? AND "
                    + Tareas.COLUMN_ACTIVA + " = 1 AND "
                    + Tareas.COLUMN_ELIMINADA + " = 0";

            String[] argumentos = {
                    String.valueOf(tareaId),
                    String.valueOf(usuarioId)
            };

            try (Cursor cursor = db.query(
                    Tareas.TABLE_NAME,
                    new String[]{Tareas._ID},
                    filtro,
                    argumentos,
                    null,
                    null,
                    null
            )) {
                if (!cursor.moveToFirst()) {
                    throw new IllegalArgumentException("La tarea ya no está disponible");
                }
            }

            ContentValues valores = new ContentValues();

            valores.put(RegistroTarea.COLUMN_TAREA_ID, tareaId);
            valores.put(
                    RegistroTarea.COLUMN_INICIO_PROGRAMADO,
                    inicioProgramado
            );
            valores.put(RegistroTarea.COLUMN_INICIO_REAL, inicioReal);
            valores.put(
                    RegistroTarea.COLUMN_ESTADO,
                    com.example.procrastinot.models.RegistroTarea.EN_CURSO
            );
            valores.put(RegistroTarea.COLUMN_FOTO_ANTES, fotoAntes);

            long registroId = db.insertOrThrow(
                    RegistroTarea.TABLE_NAME,
                    null,
                    valores
            );

            db.setTransactionSuccessful();

            return registroId;

        } finally {
            db.endTransaction();
        }
    }
    public com.example.procrastinot.models.RegistroTarea obtenerRegistro(
            long registroId,
            long usuarioId
    ) {
        SQLiteDatabase db = helper.getReadableDatabase();

        String sql = "SELECT r.* FROM " + RegistroTarea.TABLE_NAME + " r "
                + "INNER JOIN " + Tareas.TABLE_NAME + " t "
                + "ON r." + RegistroTarea.COLUMN_TAREA_ID + " = t." + Tareas._ID
                + " WHERE r." + RegistroTarea._ID + " = ?"
                + " AND t." + Tareas.COLUMN_USUARIO_ID + " = ?";

        String[] argumentos = {
                String.valueOf(registroId),
                String.valueOf(usuarioId)
        };

        try (Cursor cursor = db.rawQuery(sql, argumentos)) {
            if (!cursor.moveToFirst()) {
                return null;
            }

            com.example.procrastinot.models.RegistroTarea registro =
                    new com.example.procrastinot.models.RegistroTarea();

            registro.setId(registroId);

            registro.setTareaId(cursor.getLong(
                    cursor.getColumnIndexOrThrow(
                            RegistroTarea.COLUMN_TAREA_ID
                    )
            ));

            registro.setInicioProgramado(cursor.getLong(
                    cursor.getColumnIndexOrThrow(
                            RegistroTarea.COLUMN_INICIO_PROGRAMADO
                    )
            ));

            int columnaInicio = cursor.getColumnIndexOrThrow(
                    RegistroTarea.COLUMN_INICIO_REAL
            );

            registro.setInicioReal(
                    cursor.isNull(columnaInicio)
                            ? null
                            : cursor.getLong(columnaInicio)
            );

            int columnaFin = cursor.getColumnIndexOrThrow(
                    RegistroTarea.COLUMN_FIN_REAL
            );

            registro.setFinReal(
                    cursor.isNull(columnaFin)
                            ? null
                            : cursor.getLong(columnaFin)
            );

            registro.setEstado(cursor.getString(
                    cursor.getColumnIndexOrThrow(
                            RegistroTarea.COLUMN_ESTADO
                    )
            ));

            registro.setFotoAntes(cursor.getString(
                    cursor.getColumnIndexOrThrow(
                            RegistroTarea.COLUMN_FOTO_ANTES
                    )
            ));

            registro.setFotoDespues(cursor.getString(
                    cursor.getColumnIndexOrThrow(
                            RegistroTarea.COLUMN_FOTO_DESPUES
                    )
            ));

            return registro;
        }
    }

    public boolean registrarFin(
            long registroId,
            long usuarioId,
            long finReal
    ) {
        SQLiteDatabase db = helper.getWritableDatabase();

        ContentValues valores = new ContentValues();
        valores.put(RegistroTarea.COLUMN_FIN_REAL, finReal);

        String filtro = RegistroTarea._ID + " = ? AND "
                + RegistroTarea.COLUMN_ESTADO + " = ? AND "
                + RegistroTarea.COLUMN_INICIO_REAL + " IS NOT NULL AND "
                + RegistroTarea.COLUMN_INICIO_REAL + " <= ? AND "
                + RegistroTarea.COLUMN_FIN_REAL + " IS NULL AND "
                + RegistroTarea.COLUMN_TAREA_ID + " IN (SELECT "
                + Tareas._ID + " FROM " + Tareas.TABLE_NAME
                + " WHERE " + Tareas.COLUMN_USUARIO_ID + " = ?)";

        String[] argumentos = {
                String.valueOf(registroId),
                com.example.procrastinot.models.RegistroTarea.EN_CURSO,
                String.valueOf(finReal),
                String.valueOf(usuarioId)
        };

        return db.update(
                RegistroTarea.TABLE_NAME,
                valores,
                filtro,
                argumentos
        ) == 1;
    }

    public boolean completarRegistro(
            long registroId,
            long usuarioId,
            String fotoDespues
    ) {
        SQLiteDatabase db = helper.getWritableDatabase();

        ContentValues valores = new ContentValues();
        valores.put(RegistroTarea.COLUMN_FOTO_DESPUES, fotoDespues);

        valores.put(
                RegistroTarea.COLUMN_ESTADO,
                com.example.procrastinot.models.RegistroTarea.COMPLETADA
        );

        String filtro = RegistroTarea._ID + " = ? AND "
                + RegistroTarea.COLUMN_ESTADO + " = ? AND "
                + RegistroTarea.COLUMN_FIN_REAL + " IS NOT NULL AND "
                + RegistroTarea.COLUMN_TAREA_ID + " IN (SELECT "
                + Tareas._ID + " FROM " + Tareas.TABLE_NAME
                + " WHERE " + Tareas.COLUMN_USUARIO_ID + " = ?)";

        String[] argumentos = {
                String.valueOf(registroId),
                com.example.procrastinot.models.RegistroTarea.EN_CURSO,
                String.valueOf(usuarioId)
        };

        return db.update(
                RegistroTarea.TABLE_NAME,
                valores,
                filtro,
                argumentos
        ) == 1;
    }

    public String obtenerNombreTarea(long tareaId, long usuarioId) {
        SQLiteDatabase db = helper.getReadableDatabase();

        String filtro = Tareas._ID + " = ? AND "
                + Tareas.COLUMN_USUARIO_ID + " = ?";

        String[] argumentos = {
                String.valueOf(tareaId),
                String.valueOf(usuarioId)
        };

        try (Cursor cursor = db.query(
                Tareas.TABLE_NAME,
                new String[]{Tareas.COLUMN_NOMBRE},
                filtro,
                argumentos,
                null,
                null,
                null
        )) {
            if (cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndexOrThrow(Tareas.COLUMN_NOMBRE));
            }

            return "Resultado de la tarea";
        }
    }

    public List<com.example.procrastinot.models.RegistroTarea>
    listarHistorial(long usuarioId) {

        SQLiteDatabase db = helper.getReadableDatabase();

        List<com.example.procrastinot.models.RegistroTarea> registros =
                new ArrayList<>();

        String sql = "SELECT "
                + "r." + RegistroTarea._ID + " AS registro_id, "
                + "r." + RegistroTarea.COLUMN_TAREA_ID + " AS tarea_id, "
                + "t." + Tareas.COLUMN_NOMBRE + " AS nombre_tarea, "
                + "r." + RegistroTarea.COLUMN_INICIO_REAL + " AS inicio_real, "
                + "r." + RegistroTarea.COLUMN_FIN_REAL + " AS fin_real "
                + "FROM " + RegistroTarea.TABLE_NAME + " r "
                + "INNER JOIN " + Tareas.TABLE_NAME + " t "
                + "ON r." + RegistroTarea.COLUMN_TAREA_ID + " = t." + Tareas._ID
                + " WHERE t." + Tareas.COLUMN_USUARIO_ID + " = ?"
                + " AND r." + RegistroTarea.COLUMN_ESTADO + " = ?"
                + " AND r." + RegistroTarea.COLUMN_INICIO_REAL + " IS NOT NULL"
                + " AND r." + RegistroTarea.COLUMN_FIN_REAL + " IS NOT NULL"
                + " ORDER BY r." + RegistroTarea.COLUMN_FIN_REAL + " DESC, "
                + "r." + RegistroTarea._ID + " DESC";

        String[] argumentos = {
                String.valueOf(usuarioId),
                com.example.procrastinot.models.RegistroTarea.COMPLETADA
        };

        try (Cursor cursor = db.rawQuery(sql, argumentos)) {
            while (cursor.moveToNext()) {
                com.example.procrastinot.models.RegistroTarea registro =
                        new com.example.procrastinot.models.RegistroTarea();

                registro.setId(cursor.getLong(
                        cursor.getColumnIndexOrThrow("registro_id")
                ));

                registro.setTareaId(cursor.getLong(
                        cursor.getColumnIndexOrThrow("tarea_id")
                ));

                registro.setNombreTarea(cursor.getString(
                        cursor.getColumnIndexOrThrow("nombre_tarea")
                ));

                registro.setInicioReal(cursor.getLong(
                        cursor.getColumnIndexOrThrow("inicio_real")
                ));

                registro.setFinReal(cursor.getLong(
                        cursor.getColumnIndexOrThrow("fin_real")
                ));

                registro.setEstado(
                        com.example.procrastinot.models.RegistroTarea.COMPLETADA
                );

                registros.add(registro);
            }
        }

        return registros;
    }

    public void cerrar() {
        helper.close();
    }
}