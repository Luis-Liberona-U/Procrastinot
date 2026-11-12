package com.example.procrastinot.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.procrastinot.database.ProcrastinotContract.RegistroTarea;
import com.example.procrastinot.database.ProcrastinotDbHelper;
import com.example.procrastinot.models.Tarea;
import com.example.procrastinot.repositories.TareaRepository;
import com.example.procrastinot.security.SesionManager;
import com.example.procrastinot.views.RealizarTareaActivity;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Recibe los avisos de inicio de tareas únicas y recurrentes.
 *
 * Comprueba que la tarea siga activa, pertenezca a la sesión actual y conserve el horario del aviso. Si es recurrente, programa su
 * próxima fecha aunque no corresponda mostrar la notificación actual.
 */
public class RecordatorioReceiver extends BroadcastReceiver {

    private static final String CANAL = "recordatorios_tareas";
    private static final String TAG = "Recordatorio";

    @Override
    public void onReceive(Context context, Intent intent) {
        long usuarioId = intent.getLongExtra("usuario_id", -1);
        long tareaId = intent.getLongExtra("tarea_id", -1);
        long inicio = intent.getLongExtra("inicio_programado", -1);

        Context app = context.getApplicationContext();
        SesionManager sesion = new SesionManager(app);

        if (usuarioId <= 0 || tareaId <= 0 || inicio <= 0 || sesion.obtenerUsuarioId() != usuarioId) {
            return;
        }

        PendingResult resultado = goAsync();

        new Thread(() -> {
            try {
                Tarea tarea;
                TareaRepository repositorio = new TareaRepository(app);

                try {
                    tarea = repositorio.obtenerPorId(tareaId, usuarioId);
                } finally {
                    repositorio.cerrar();
                }

                if (tarea == null || !tarea.isActiva()) {
                    return;
                }

                String repeticion = tarea.getRepeticion();
                boolean unica = Tarea.UNA_VEZ.equals(repeticion);
                boolean[] dias = tarea.getDias();

                // Usa la fecha del aviso, aunque Android lo entregue tarde.
                LocalDate fechaAviso = Instant.ofEpochMilli(inicio)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();

                if (unica) {
                    if (!fechaAviso.toString().equals(tarea.getFechaUnica())) {
                        return;
                    }
                } else if (!RecordatorioScheduler.correspondeAlDia(repeticion, fechaAviso, dias)) {
                    return;
                }

                long horarioActual = RecordatorioScheduler.calcularInicio(
                        fechaAviso.toString(),
                        tarea.getHoraInicio(),
                        tarea.getMinutoInicio()
                );

                if (horarioActual != inicio || sesion.obtenerUsuarioId() != usuarioId) {
                    return;
                }

                if (!unica) {
                    programarSiguiente(app, usuarioId, tarea, dias);
                }

                if (siguePendiente(app, tareaId, inicio) && sesion.obtenerUsuarioId() == usuarioId) {

                    mostrarAviso(app, usuarioId, tareaId, inicio, tarea.getNombre(), tarea.getDescripcion());
                }
            } catch (SQLiteException | DateTimeException | SecurityException e) {

                Log.e(TAG, "No se pudo procesar el aviso de la tarea", e);
            } finally {
                resultado.finish();
            }
        }).start();
    }

    /** Programa la siguiente fecha sin impedir el aviso actual si falla. */
    private void programarSiguiente(Context context, long usuarioId, Tarea tarea, boolean[] dias) {

        try {
            boolean programado = RecordatorioScheduler.programarProxima(
                    context,
                    usuarioId,
                    tarea.getId(),
                    tarea.getRepeticion(),
                    tarea.getFechaUnica(),
                    tarea.getHoraInicio(),
                    tarea.getMinutoInicio(),
                    dias
            );

            if (!programado) {
                Log.w(TAG, "No se pudo programar el siguiente aviso");
            }
        } catch (SecurityException | IllegalArgumentException | DateTimeException e) {
            Log.e(TAG, "Error al programar el siguiente aviso", e);
        }
    }

    /** Comprueba el estado de esta fecha concreta de la tarea. */
    private boolean siguePendiente(Context context, long tareaId, long inicio) {

        try (ProcrastinotDbHelper helper = new ProcrastinotDbHelper(context)) {
            SQLiteDatabase db = helper.getReadableDatabase();

            try (Cursor registro = db.query(
                    RegistroTarea.TABLE_NAME,
                    new String[]{RegistroTarea.COLUMN_ESTADO},
                    RegistroTarea.COLUMN_TAREA_ID + " = ? AND "
                            + RegistroTarea.COLUMN_INICIO_PROGRAMADO + " = ?",
                    new String[]{String.valueOf(tareaId), String.valueOf(inicio)},
                    null, null, null
            )) {
                return !registro.moveToFirst() || "PENDIENTE".equals(registro.getString(0));
            }
        }
    }

    /** Publica la notificación y configura la pantalla que abre al tocarla. */
    private void mostrarAviso(Context context, long usuarioId, long tareaId, long inicio, String nombre, String descripcion) {

        if (android.os.Build.VERSION.SDK_INT >= 33
                && context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return;
        }

        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            return;
        }

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) {
            return;
        }

        manager.createNotificationChannel(new NotificationChannel(CANAL, "Recordatorios de tareas", NotificationManager.IMPORTANCE_HIGH));

        Intent abrir = new Intent(context, RealizarTareaActivity.class);
        abrir.setData(Uri.parse("procrastinot://realizar/" + usuarioId + "/" + tareaId));
        abrir.putExtra("usuario_id", usuarioId);
        abrir.putExtra("tarea_id", tareaId);
        abrir.putExtra("inicio_programado", inicio);
        abrir.putExtra("tarea_nombre", nombre);
        abrir.putExtra("tarea_descripcion", descripcion);

        PendingIntent alTocar = PendingIntent.getActivity(context, 0, abrir,

                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        manager.notify(usuarioId + ":" + tareaId, 1,
                new NotificationCompat.Builder(context, CANAL)
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle("Es hora de comenzar tu tarea")
                        .setContentText(nombre)
                        .setContentIntent(alTocar)
                        .setAutoCancel(true)
                        .build());
    }
}