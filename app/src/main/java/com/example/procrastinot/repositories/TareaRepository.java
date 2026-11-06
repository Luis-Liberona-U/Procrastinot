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

/**
 * Gestiona la configuración de las tareas y sus días de repetición en SQLite.
 *
 * Esta clase permite crear, consultar, modificar, activar, desactivar y eliminar las tareas de un usuario.
 *
 * Las realizaciones y sus fotografías se administran por separado mediante RegistroTareaRepository.
 */
public class TareaRepository {
    private final ProcrastinotDbHelper helper;

    public TareaRepository(Context context) {
        helper = new ProcrastinotDbHelper(context);
    }



    /**
     * Crea una tarea activa y no eliminada, junto con sus días semanales.
     *
     * La validación de días se aplica únicamente a DIAS_SEMANA.
     * Las demás restricciones se comprueban mediante el esquema de SQLite.
     *
     * @param usuarioId ID del usuario propietario de la tarea.
     * @param nombre Nombre de la tarea.
     * @param descripcion Descripción opcional.
     * @param hora Hora de inicio, entre 0 y 23.
     * @param minuto Minuto de inicio, entre 0 y 59.
     * @param repeticion Tipo: UNA_VEZ, DIARIA o DIAS_SEMANA.
     * @param fechaUnica Fecha yyyy-MM-dd, obligatoria para UNA_VEZ.
     * @param duracionMinutos Duración estimada, mayor que cero.
     * @param dias Siete posiciones de lunes a domingo; true indica selección. Este se ignora cuando la repetición no es semanal.
     * @return ID asignado a la tarea creada.
     * @throws IllegalArgumentException Si la repetición es semanal y los días no tienen siete posiciones o no hay ninguno seleccionado.
     * @throws android.database.sqlite.SQLiteException
     *         Sí falla el acceso a SQLite o alguna inserción.
     */
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

    /**
     * Consulta las tareas no eliminadas de un usuario.
     *
     * Incluye tareas activas y desactivadas. Las ordena por hora, minuto e ID, de menor a mayor, y carga sus días si son semanales.
     *
     * @param usuarioId ID del usuario propietario.
     * @return Lista de tareas encontradas, o una lista vacía si no hay ninguna.
     * @throws android.database.sqlite.SQLiteException
     *         Si falla el acceso o la consulta a SQLite.
     */
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

        // El Cursor se cierra automáticamente al salir del bloque.
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

                // Convierte cada fila de SQLite en un objeto Tarea.
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

    /**
     * Activa o desactiva una tarea no eliminada del usuario.
     *
     * Modifica únicamente el campo activa, conservando la configuración de la tarea y sus registros de realización o historial.
     *
     * @param tareaId ID de la tarea que se modificará.
     * @param usuarioId ID del usuario propietario.
     * @param activa true para activar; false para desactivar.
     * @return true si SQLite actualizó una fila y false si no encontró una tarea que cumpliera el filtro.
     * @throws android.database.sqlite.SQLiteException
     *         Sí falla el acceso o la actualización en SQLite.
     */
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

    /**
     * Elimina lógicamente una tarea del usuario indicado.
     *
     * Marca eliminada como 1 y activa como 0. Conserva la fila, sus días de repetición y los registros históricos asociados.
     *
     * @param tareaId ID de la tarea que se eliminará lógicamente.
     * @param usuarioId ID del usuario propietario.
     * @return true si se actualizó una fila y false si la tarea no existe, pertenece a otro usuario o ya estaba eliminada.
     * @throws android.database.sqlite.SQLiteException
     *         Sí falla el acceso o la actualización en SQLite.
     */
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

    /**
     * Recupera los días semanales asociados a una tarea.
     *
     * @param db Conexión abierta utilizada para realizar la consulta.
     * @param tareaId ID de la tarea cuyos días se consultan.
     * @return Arreglo de siete posiciones, de lunes a domingo, con true en los días guardados y false en los demás.
     * @throws android.database.sqlite.SQLiteException
     *         Sí falla la consulta de los días.
     */
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

    /**
     * Consulta una tarea no eliminada por su ID y propietario.
     *
     * Permite recuperar tareas activas o desactivadas.
     * Si su repetición es semanal, también captura sus días seleccionados.
     *
     * @param tareaId ID de la tarea que se desea recuperar.
     * @param usuarioId ID del usuario propietario.
     * @return Tarea encontrada, o null si no cumple el filtro.
     * @throws android.database.sqlite.SQLiteException
     *         Si falla el acceso o la consulta a SQLite.
     */
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

    /**
     * Actualiza la configuración de una tarea y reemplaza sus días semanales.
     *
     * Mantiene el propietario, el estado de activación y los registros históricos. La tarea debe pertenecer al usuario y no estar eliminada.
     *
     * Utiliza una transacción para actualizar los datos, quitar los días anteriores y guardar los nuevos. Si falla, se revierten los cambios.
     *
     * @param tareaId ID de la tarea que se modificará.
     * @param usuarioId ID del usuario propietario.
     * @param nombre Nuevo nombre de la tarea.
     * @param descripcion Nueva descripción opcional.
     * @param hora Nueva hora de inicio, entre 0 y 23.
     * @param minuto Nuevo minuto de inicio, entre 0 y 59.
     * @param repeticion Tipo: UNA_VEZ, DIARIA o DIAS_SEMANA.
     * @param fechaUnica Fecha yyyy-MM-dd, obligatoria para UNA_VEZ.
     * @param duracionMinutos Duración estimada, mayor que cero.
     * @param dias Siete posiciones de lunes a domingo; true indica selección. Se ignora cuando la repetición no es semanal.
     * @return true si se actualizó la tarea y su configuración de díasy false si ninguna tarea cumplió el filtro.
     * @throws IllegalArgumentException Si la repetición es semanal y los días no tienen siete posiciones o no hay ninguno seleccionado.
     * @throws android.database.sqlite.SQLiteException
     *         Sí falla el acceso o alguna operación en SQLite.
     */
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

    /**
     * Cierra el Helper y libera la conexión que administra.
     *
     * Debe llamarse cuando hayan terminado las operaciones del repositorio.
     */

    public void cerrar() {
        helper.close();
    }
}