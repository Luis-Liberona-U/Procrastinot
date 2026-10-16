package com.example.procrastinot.repositories;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.procrastinot.database.ProcrastinotContract.Tareas;
import com.example.procrastinot.database.ProcrastinotContract.TareaDias;
import com.example.procrastinot.database.ProcrastinotDbHelper;
import com.example.procrastinot.models.Tarea;

import java.util.ArrayList;
import java.util.List;

public class TareaRepository {

    private final ProcrastinotDbHelper helper;

    public TareaRepository(Context context) {
        helper = new ProcrastinotDbHelper(context);
    }

    public long crearTarea(
            long usuarioId,
            String nombre,
            String descripcion,
            int hora,
            int minuto,
            String repeticion,
            String fechaUnica,
            int duracionMinutos,
            boolean[] dias
    ) {
        SQLiteDatabase db = helper.getWritableDatabase();

        db.beginTransaction();

        try {
            ContentValues valores = new ContentValues();

            valores.put(Tareas.COLUMN_USUARIO_ID, usuarioId);
            valores.put(Tareas.COLUMN_NOMBRE, nombre);
            valores.put(Tareas.COLUMN_DESCRIPCION, descripcion);
            valores.put(Tareas.COLUMN_HORA_INICIO, hora);
            valores.put(Tareas.COLUMN_MINUTO_INICIO, minuto);
            valores.put(Tareas.COLUMN_REPETICION, repeticion);
            valores.put(Tareas.COLUMN_FECHA_UNICA, fechaUnica);
            valores.put(Tareas.COLUMN_DURACION, duracionMinutos);
            valores.put(Tareas.COLUMN_ACTIVA, 1);
            valores.put(Tareas.COLUMN_ELIMINADA, 0);

            long tareaId = db.insertOrThrow(
                    Tareas.TABLE_NAME,
                    null,
                    valores
            );

            if (Tarea.DIAS_SEMANA.equals(repeticion)) {
                if (dias == null || dias.length != 7) {
                    throw new IllegalArgumentException("Debes proporcionar los siete días");
                }

                boolean hayDia = false;

                for (int i = 0; i < dias.length; i++) {
                    if (dias[i]) {
                        hayDia = true;

                        ContentValues valoresDia = new ContentValues();

                        valoresDia.put(
                                TareaDias.COLUMN_TAREA_ID,
                                tareaId
                        );

                        valoresDia.put(
                                TareaDias.COLUMN_DIA_SEMANA,
                                i + 1
                        );

                        db.insertOrThrow(
                                TareaDias.TABLE_NAME,
                                null,
                                valoresDia
                        );
                    }
                }

                if (!hayDia) {
                    throw new IllegalArgumentException("Selecciona al menos un día");
                }
            }

            db.setTransactionSuccessful();

            return tareaId;

        } finally {
            db.endTransaction();
        }
    }

    public List<Tarea> listarPorUsuario(long usuarioId) {
        SQLiteDatabase db = helper.getReadableDatabase();

        List<Tarea> tareas = new ArrayList<>();

        String filtro = Tareas.COLUMN_USUARIO_ID + " = ? AND "
                + Tareas.COLUMN_ELIMINADA + " = 0";

        String[] argumentos = {
                String.valueOf(usuarioId)
        };

        String orden = Tareas.COLUMN_HORA_INICIO + " ASC, "
                + Tareas.COLUMN_MINUTO_INICIO + " ASC, "
                + Tareas._ID + " ASC";

        try (Cursor cursor = db.query(
                Tareas.TABLE_NAME,
                null,
                filtro,
                argumentos,
                null,
                null,
                orden
        )) {
            while (cursor.moveToNext()) {
                Tarea tarea = new Tarea();

                tarea.setId(cursor.getLong(
                        cursor.getColumnIndexOrThrow(Tareas._ID)
                ));

                tarea.setUsuarioId(cursor.getLong(
                        cursor.getColumnIndexOrThrow(
                                Tareas.COLUMN_USUARIO_ID
                        )
                ));

                tarea.setNombre(cursor.getString(
                        cursor.getColumnIndexOrThrow(
                                Tareas.COLUMN_NOMBRE
                        )
                ));

                tarea.setDescripcion(cursor.getString(
                        cursor.getColumnIndexOrThrow(
                                Tareas.COLUMN_DESCRIPCION
                        )
                ));

                tarea.setHoraInicio(cursor.getInt(
                        cursor.getColumnIndexOrThrow(
                                Tareas.COLUMN_HORA_INICIO
                        )
                ));

                tarea.setMinutoInicio(cursor.getInt(
                        cursor.getColumnIndexOrThrow(
                                Tareas.COLUMN_MINUTO_INICIO
                        )
                ));

                tarea.setRepeticion(cursor.getString(
                        cursor.getColumnIndexOrThrow(
                                Tareas.COLUMN_REPETICION
                        )
                ));

                tarea.setFechaUnica(cursor.getString(
                        cursor.getColumnIndexOrThrow(
                                Tareas.COLUMN_FECHA_UNICA
                        )
                ));

                tarea.setDuracionMinutos(cursor.getInt(
                        cursor.getColumnIndexOrThrow(
                                Tareas.COLUMN_DURACION
                        )
                ));

                tarea.setActiva(cursor.getInt(
                        cursor.getColumnIndexOrThrow(
                                Tareas.COLUMN_ACTIVA
                        )
                ) == 1);

                tarea.setEliminada(cursor.getInt(
                        cursor.getColumnIndexOrThrow(
                                Tareas.COLUMN_ELIMINADA
                        )
                ) == 1);

                if (Tarea.DIAS_SEMANA.equals(tarea.getRepeticion())) {
                    tarea.setDias(
                            consultarDias(db, tarea.getId())
                    );
                }

                tareas.add(tarea);
            }
        }

        return tareas;
    }

    public boolean cambiarEstado(
            long tareaId,
            long usuarioId,
            boolean activa
    ) {
        SQLiteDatabase db = helper.getWritableDatabase();

        ContentValues valores = new ContentValues();
        valores.put(Tareas.COLUMN_ACTIVA, activa ? 1 : 0);

        String filtro = Tareas._ID + " = ? AND "
                + Tareas.COLUMN_USUARIO_ID + " = ? AND "
                + Tareas.COLUMN_ELIMINADA + " = 0";

        String[] argumentos = {
                String.valueOf(tareaId),
                String.valueOf(usuarioId)
        };

        int filas = db.update(
                Tareas.TABLE_NAME,
                valores,
                filtro,
                argumentos
        );

        return filas == 1;
    }

    public boolean eliminarTarea(
            long tareaId,
            long usuarioId
    ) {
        SQLiteDatabase db = helper.getWritableDatabase();

        ContentValues valores = new ContentValues();
        valores.put(Tareas.COLUMN_ELIMINADA, 1);
        valores.put(Tareas.COLUMN_ACTIVA, 0);

        String filtro = Tareas._ID + " = ? AND "
                + Tareas.COLUMN_USUARIO_ID + " = ? AND "
                + Tareas.COLUMN_ELIMINADA + " = 0";

        String[] argumentos = {
                String.valueOf(tareaId),
                String.valueOf(usuarioId)
        };

        int filas = db.update(
                Tareas.TABLE_NAME,
                valores,
                filtro,
                argumentos
        );

        return filas == 1;
    }

    private boolean[] consultarDias(
            SQLiteDatabase db,
            long tareaId
    ) {
        boolean[] dias = new boolean[7];

        try (Cursor cursor = db.query(
                TareaDias.TABLE_NAME,
                new String[]{TareaDias.COLUMN_DIA_SEMANA},
                TareaDias.COLUMN_TAREA_ID + " = ?",
                new String[]{String.valueOf(tareaId)},
                null,
                null,
                TareaDias.COLUMN_DIA_SEMANA + " ASC"
        )) {
            while (cursor.moveToNext()) {
                int dia = cursor.getInt(
                        cursor.getColumnIndexOrThrow(
                                TareaDias.COLUMN_DIA_SEMANA
                        )
                );

                if (dia >= 1 && dia <= 7) {
                    dias[dia - 1] = true;
                }
            }
        }

        return dias;
    }

    public Tarea obtenerPorId(long tareaId, long usuarioId) {
        SQLiteDatabase db = helper.getReadableDatabase();

        String filtro = Tareas._ID + " = ? AND "
                + Tareas.COLUMN_USUARIO_ID + " = ? AND "
                + Tareas.COLUMN_ELIMINADA + " = 0";

        String[] argumentos = {
                String.valueOf(tareaId),
                String.valueOf(usuarioId)
        };

        try (Cursor cursor = db.query(
                Tareas.TABLE_NAME,
                null,
                filtro,
                argumentos,
                null,
                null,
                null
        )) {
            if (!cursor.moveToFirst()) {
                return null;
            }

            Tarea tarea = new Tarea();

            tarea.setId(tareaId);
            tarea.setUsuarioId(usuarioId);

            tarea.setNombre(cursor.getString(
                    cursor.getColumnIndexOrThrow(Tareas.COLUMN_NOMBRE)
            ));

            tarea.setDescripcion(cursor.getString(
                    cursor.getColumnIndexOrThrow(Tareas.COLUMN_DESCRIPCION)
            ));

            tarea.setHoraInicio(cursor.getInt(
                    cursor.getColumnIndexOrThrow(Tareas.COLUMN_HORA_INICIO)
            ));

            tarea.setMinutoInicio(cursor.getInt(
                    cursor.getColumnIndexOrThrow(Tareas.COLUMN_MINUTO_INICIO)
            ));

            tarea.setRepeticion(cursor.getString(
                    cursor.getColumnIndexOrThrow(Tareas.COLUMN_REPETICION)
            ));

            tarea.setFechaUnica(cursor.getString(
                    cursor.getColumnIndexOrThrow(Tareas.COLUMN_FECHA_UNICA)
            ));

            tarea.setDuracionMinutos(cursor.getInt(
                    cursor.getColumnIndexOrThrow(Tareas.COLUMN_DURACION)
            ));

            tarea.setActiva(cursor.getInt(
                    cursor.getColumnIndexOrThrow(Tareas.COLUMN_ACTIVA)
            ) == 1);

            if (Tarea.DIAS_SEMANA.equals(tarea.getRepeticion())) {
                tarea.setDias(consultarDias(db, tareaId));
            }

            return tarea;
        }
    }

    public boolean actualizarTarea(
            long tareaId,
            long usuarioId,
            String nombre,
            String descripcion,
            int hora,
            int minuto,
            String repeticion,
            String fechaUnica,
            int duracionMinutos,
            boolean[] dias
    ) {
        // Validamos los días antes de modificar la base.
        if (Tarea.DIAS_SEMANA.equals(repeticion)) {
            if (dias == null || dias.length != 7) {
                throw new IllegalArgumentException(
                        "Debes proporcionar los siete días"
                );
            }

            boolean hayDia = false;

            for (boolean seleccionado : dias) {
                if (seleccionado) {
                    hayDia = true;
                    break;
                }
            }

            if (!hayDia) {
                throw new IllegalArgumentException(
                        "Selecciona al menos un día"
                );
            }
        }

        SQLiteDatabase db = helper.getWritableDatabase();

        db.beginTransaction();

        try {
            ContentValues valores = new ContentValues();

            valores.put(Tareas.COLUMN_NOMBRE, nombre);
            valores.put(Tareas.COLUMN_DESCRIPCION, descripcion);
            valores.put(Tareas.COLUMN_HORA_INICIO, hora);
            valores.put(Tareas.COLUMN_MINUTO_INICIO, minuto);
            valores.put(Tareas.COLUMN_REPETICION, repeticion);
            valores.put(Tareas.COLUMN_FECHA_UNICA, fechaUnica);
            valores.put(Tareas.COLUMN_DURACION, duracionMinutos);

            String filtro = Tareas._ID + " = ? AND "
                    + Tareas.COLUMN_USUARIO_ID + " = ? AND "
                    + Tareas.COLUMN_ELIMINADA + " = 0";

            String[] argumentos = {
                    String.valueOf(tareaId),
                    String.valueOf(usuarioId)
            };

            int filas = db.update(
                    Tareas.TABLE_NAME,
                    valores,
                    filtro,
                    argumentos
            );

            if (filas != 1) {
                return false;
            }

            db.delete(
                    TareaDias.TABLE_NAME,
                    TareaDias.COLUMN_TAREA_ID + " = ?",
                    new String[]{String.valueOf(tareaId)}
            );

            // Guardamos la nueva configuración, si es semanal.
            if (Tarea.DIAS_SEMANA.equals(repeticion)) {
                for (int i = 0; i < dias.length; i++) {
                    if (dias[i]) {
                        ContentValues valoresDia = new ContentValues();

                        valoresDia.put(TareaDias.COLUMN_TAREA_ID, tareaId);
                        valoresDia.put(TareaDias.COLUMN_DIA_SEMANA, i + 1);

                        db.insertOrThrow(
                                TareaDias.TABLE_NAME,
                                null,
                                valoresDia
                        );
                    }
                }
            }

            db.setTransactionSuccessful();

            return true;

        } finally {
            db.endTransaction();
        }
    }

    public void cerrar() {
        helper.close();
    }
}