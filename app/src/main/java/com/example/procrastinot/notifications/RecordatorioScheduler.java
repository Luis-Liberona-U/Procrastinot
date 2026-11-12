package com.example.procrastinot.notifications;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import com.example.procrastinot.models.Tarea;
import com.example.procrastinot.views.BienvenidaActivity;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

/**
 * Programa el aviso de inicio de una tarea de tipo «Una vez».
 *
 * Si la aplicación tiene acceso a alarmas exactas, programa el aviso como una alarma de reloj. Si el acceso no está disponible o Android rechaza
 * la programación exacta, programa un aviso aproximado.
 *
 * Al dispararse la alarma, Android activa RecordatorioReceiver, que se encarga de publicar la notificación.
 */
public final class RecordatorioScheduler {

    private static final String TAG = "Recordatorio";

    private RecordatorioScheduler() {
    }

    /**
     * Convierte la fecha y hora de la tarea a milisegundos desde el inicio de la época Unix, usando la zona horaria del dispositivo.
     *
     * @param fecha fecha local en formato ISO, por ejemplo, 2026-11-12.
     * @param hora hora local de inicio, entre 0 y 23.
     * @param minuto minuto de inicio, entre 0 y 59.
     * @return instante de inicio en milisegundos.
     * @throws DateTimeParseException si la fecha no se puede interpretar.
     * @throws java.time.DateTimeException si la hora o el minuto están fuera de rango.
     */
    static long calcularInicio(String fecha, int hora, int minuto) {
        return LocalDate.parse(fecha)
                .atTime(hora, minuto)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
    }

    /**
     * Programa el aviso de inicio de una tarea para una fecha y hora futuras.
     *
     * @param context contexto para acceder al servicio de alarmas.
     * @param usuarioId identificador del usuario propietario de la tarea.
     * @param tareaId identificador de la tarea.
     * @param fecha fecha local en formato ISO, por ejemplo, 2026-11-12.
     * @param hora hora local de inicio, entre 0 y 23.
     * @param minuto minuto de inicio, entre 0 y 59.
     * @return true si se programó un aviso exacto o aproximado o false si los datos son inválidos, el inicio ya pasó o el servicio de alarmas no está disponible.
     */
    public static boolean programar(
            Context context,
            long usuarioId,
            long tareaId,
            String fecha,
            int hora,
            int minuto
    ) {
        if (fecha == null || usuarioId <= 0 || tareaId <= 0
                || hora < 0 || hora > 23 || minuto < 0 || minuto > 59) {
            return false;
        }

        final long inicio;
        try {
            inicio = calcularInicio(fecha, hora, minuto);
        } catch (DateTimeParseException e) {
            return false;
        }

        if (inicio <= System.currentTimeMillis()) {
            return false;
        }

        AlarmManager alarmas = context.getSystemService(AlarmManager.class);
        if (alarmas == null) {
            return false;
        }

        boolean accesoExacto = Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                || alarmas.canScheduleExactAlarms();

        Intent intent = new Intent(context, RecordatorioReceiver.class);
        // La URI evita que el aviso de una tarea reemplace al de otra.
        intent.setData(Uri.parse("procrastinot://recordatorio/" + usuarioId + "/" + tareaId));
        intent.putExtra("usuario_id", usuarioId);
        intent.putExtra("tarea_id", tareaId);
        intent.putExtra("inicio_programado", inicio);

        PendingIntent aviso = PendingIntent.getBroadcast(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (accesoExacto) {
            try {
                // Abre la app si el usuario toca la próxima alarma desde el sistema.
                Intent abrirApp = new Intent(context, BienvenidaActivity.class);
                PendingIntent mostrarAlarma = PendingIntent.getActivity(
                        context, 0, abrirApp, PendingIntent.FLAG_IMMUTABLE
                );

                alarmas.setAlarmClock(
                        new AlarmManager.AlarmClockInfo(inicio, mostrarAlarma),
                        aviso
                );
                return true;
            } catch (SecurityException e) {
                Log.w(TAG, "Falló la alarma tipo reloj; se usará la aproximada", e);
            }
        }

        alarmas.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, inicio, aviso);
        return true;
    }

    /**
     * Programa la próxima fecha futura que coincide con la repetición de la tarea.
     *
     * @return true si se programó un aviso; false si no hay una fecha válida o no se pudo programar.
     */
    public static boolean programarProxima(Context context, long usuarioId, long tareaId, String repeticion, String fechaUnica, int hora, int minuto, boolean[] dias) {
        if (Tarea.UNA_VEZ.equals(repeticion)) {
            return programar(context, usuarioId, tareaId, fechaUnica, hora, minuto);
        }

        long ahora = System.currentTimeMillis();
        LocalDate hoy = Instant.ofEpochMilli(ahora)
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

        // Incluye el mismo día de la semana siguiente si la hora de hoy ya pasó.
        for (int i = 0; i <= 7; i++) {
            LocalDate fecha = hoy.plusDays(i);
            if (correspondeAlDia(repeticion, fecha, dias) && calcularInicio(fecha.toString(), hora, minuto) > ahora) {

                return programar(context, usuarioId, tareaId, fecha.toString(), hora, minuto);
            }
        }

        return false;
    }

    /** Indica si una tarea recurrente está configurada para este día. */
    static boolean correspondeAlDia(String repeticion, LocalDate fecha, boolean[] dias) {
        return Tarea.DIARIA.equals(repeticion) || (Tarea.DIAS_SEMANA.equals(repeticion) && dias != null && dias.length == 7
                && dias[fecha.getDayOfWeek().getValue() - 1]);
    }
}