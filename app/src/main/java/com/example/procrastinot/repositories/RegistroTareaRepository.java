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

/**
 * Gestiona los registros de realización de las tareas en SQLite.
 *
 * Permite registrar el inicio, y el final de la realización de una tarea, confirmar la fotografía final y consultar los resultados e historial.
 *
 * Cada registro representa una realización de una tarea y almacena tiempos y rutas de fotografías. Las fotografías se guardan localmente en el celular no en la base de datos.
 */

public class RegistroTareaRepository {

    private final ProcrastinotDbHelper helper;

    /**
     * Inicializa el repositorio y su administrador de base de datos.
     *
     * @param context Contexto utilizado para crear el Helper.
     */
    public RegistroTareaRepository(Context context) {
        helper = new ProcrastinotDbHelper(context);
    }


    /**
     * Crea un registro de realización con estado EN_CURSO.
     *
     * Comprueba que la tarea pertenece al usuario, está activa y no está eliminada.
     *
     * La restricción UNIQUE de SQLite impide registrar dos veces la misma tarea con el mismo inicio programado.
     *
     * Este método no comprueba la existencia del archivo de fotografía.
     *
     * @param tareaId ID de la tarea que se realizará.
     * @param usuarioId ID del usuario propietario de la tarea.
     * @param inicioProgramado Inicio previsto en milisegundos desde la época Unix.
     * @param inicioReal Inicio efectivo en milisegundos desde la época Unix.
     * @param fotoAntes Ruta del archivo de la fotografía inicial.
     * @return ID del registro creado.
     * @throws IllegalArgumentException
     *         Si la tarea no pertenece al usuario, está desactivada, está eliminada o no existe.
     * @throws android.database.sqlite.SQLiteException
     *         Si falla el acceso a SQLite o la inserción del registro.
     */
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

    /**
     * Recupera un registro comprobando el propietario de la tarea.
     *
     * La consulta une registro_tarea con tareas para verificar el usuario.
     *
     * Permite recuperar registros aunque la tarea esté desactivada o eliminada lógicamente.
     *
     * Carga los datos del registro, pero no el nombre de la tarea.
     *
     * @param registroId ID del registro que se desea consultar.
     * @param usuarioId ID del usuario propietario.
     * @return Registro encontrado, o null si no existe o pertenece a otro usuario.
     * @throws android.database.sqlite.SQLiteException
     *         Si falla el acceso o la consulta a SQLite.
     */
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

    /**
     * Registra el instante en que el usuario termina de realizar la tarea.
     *
     * Solo actualiza registros EN_CURSO del usuario que tengan inicio real, no tengan fin real y cuyo inicio no sea posterior al final recibido.
     *
     * Mantiene el estado EN_CURSO hasta confirmar la fotografía final.
     * El tiempo dedicado se puede calcular restando inicioReal de finReal.
     *
     * @param registroId ID del registro que se modificará.
     * @param usuarioId ID del usuario propietario.
     * @param finReal Instante de finalización en milisegundos desde la época Unix.
     * @return true si se actualizó una fila; false si no cumplía las condiciones.
     * @throws android.database.sqlite.SQLiteException
     *         Si falla el acceso o la actualización en SQLite.
     */
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

    /**
     * Guarda la ruta de la fotografía final y marca el registro como COMPLETADA.
     *
     * El registro debe pertenecer al usuario, estar EN_CURSO y tener un fin real guardado previamente.
     *
     * No modifica el fin real, por lo que el tiempo utilizado para tomar la fotografía final no se añade a la duración de la tarea.
     *
     * La Activity valida la fotografía antes de llamar a este método.
     *
     * @param registroId ID del registro que se completará.
     * @param usuarioId ID del usuario propietario.
     * @param fotoDespues Ruta del archivo de la fotografía final.
     * @return true si se actualizó una fila; false si no cumplía las condiciones.
     * @throws android.database.sqlite.SQLiteException
     *         Si falla el acceso o la actualización en SQLite.
     */
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

    /**
     * Consulta el nombre de una tarea del usuario indicado.
     *
     * Incluye tareas desactivadas o eliminadas lógicamente para permitir mostrar sus resultados históricos.
     *
     * @param tareaId ID de la tarea que se desea consultar.
     * @param usuarioId ID del usuario propietario.
     * @return Nombre de la tarea, o "Resultado de la tarea" si no se encuentra.
     * @throws android.database.sqlite.SQLiteException
     *         Si falla el acceso o la consulta a SQLite.
     */
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

    /**
     * Consulta el historial de realizaciones de tareas completadas de un usuario.
     *
     * Incluye únicamente registros COMPLETADA con inicio y fin reales.
     * Los ordena por finalización e ID, del más reciente al más antiguo.
     * Conserva las realizaciones de tareas eliminadas lógicamente.
     *
     * Devuelve los datos necesarios para la lista: ID del registro,
     * ID y nombre actual de la tarea, inicio, fin y estado.
     * Para obtener detalle completo se obtiene mediante obtenerRegistro().
     *
     * @param usuarioId ID del usuario cuyo historial se consultará.
     * @return Lista de registros resumidos, o una lista vacía si no hay resultados.
     * @throws android.database.sqlite.SQLiteException
     *         Si falla el acceso o la consulta a SQLite.
     */
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

    /**
     * Cierra el Helper y libera la conexión que administra.
     *
     * Debe llamarse cuando hayan terminado las operaciones del repositorio.
     */
    public void cerrar() {
        helper.close();
    }
}