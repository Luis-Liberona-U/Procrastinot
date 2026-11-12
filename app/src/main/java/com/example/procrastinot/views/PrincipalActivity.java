package com.example.procrastinot.views;

import android.content.Intent;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.procrastinot.R;
import com.example.procrastinot.models.Tarea;
import com.example.procrastinot.repositories.TareaRepository;
import com.example.procrastinot.security.SesionManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Muestra la pantalla principal del usuario y su próxima tarea activa.
 *
 * Permite crear tareas, abrir Mis tareas, consultar el historial y continuar a la pantalla para realizar la tarea proxima.
 */
public class PrincipalActivity extends AppCompatActivity {
    private long usuarioId; //Guarda id del usuario

    private TextView txtProximaTarea;
    private TextView txtHoraTarea;
    private Button btnComenzarTarea;

    private Tarea tareaProxima;
    private long inicioProgramado;

    private boolean pantallaVisible = false;
    private boolean cargando = false;

    /**
     * Ejecuta las consultas de SQLite fuera del hilo principal.
     */
    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    /**
     * Solicita actualizar la próxima tarea cada 30 segundos mientras la pantalla está visible.
     */
    private final Runnable actualizarPrincipal = new Runnable() {
        @Override
        public void run() {
            if (pantallaVisible) {
                cargarProximaTarea();
                handler.postDelayed(this, 30000);
            }
        }
    };


    /**
     * Recibe los datos del usuario y configura los botones de navegación.
     *
     * Si no se recibe el ID de usuario, muestra "Inicia sesión nuevamente" y cierra la pantalla.
     * El botón Comenzar tarea se habilita cuando se encuentra una próxima tarea.
     *
     * @param savedInstanceState Estado guardado por Android al recrear la pantalla, o null si no existe.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_principal);


        // Boton cerrar sesion
        findViewById(R.id.btnCerrarSesion).setOnClickListener(v -> {
            new SesionManager(this).cerrarSesion();

            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        // Asignar nombre al titulo
        usuarioId = getIntent().getLongExtra("usuario_id", -1);
        String nombreUsuario = getIntent().getStringExtra("usuario_nombre");

        if (usuarioId == -1) {
            Toast.makeText(this, "Inicia sesión nuevamente", Toast.LENGTH_LONG).show();
            finish();
            return;
        }


        TextView txtBienvenida = findViewById(R.id.txtTituloBienvenida);
        txtBienvenida.setText("Bienvenido: " + nombreUsuario);


        txtProximaTarea = findViewById(R.id.txtProximaTarea);
        txtHoraTarea = findViewById(R.id.txtHoraTarea);
        btnComenzarTarea = findViewById(R.id.btnComenzarTarea);

        btnComenzarTarea.setEnabled(false);


        Button btnCrearTarea = findViewById(R.id.btnCrearTarea);
        Button btnVerTareas = findViewById(R.id.btnVerTareas);

        // Boton crear una tarea
        btnCrearTarea.setOnClickListener(v -> {
            Intent intent = new Intent(
                    PrincipalActivity.this,
                    CrearTareaActivity.class
            );

            intent.putExtra("usuario_id", usuarioId);
            startActivity(intent);
        });

        // Boton ver historial de tareas
        btnVerTareas.setOnClickListener(v -> {
            Intent intent = new Intent(
                    PrincipalActivity.this,
                    TareasUsuarioActivity.class
            );

            intent.putExtra("usuario_id", usuarioId);
            startActivity(intent);
        });

        Button btnHistorial = findViewById(R.id.btnHistorial);

        btnHistorial.setOnClickListener(v -> {
            Intent intent = new Intent(
                    PrincipalActivity.this,
                    HistorialTareasActivity.class
            );

            intent.putExtra("usuario_id", usuarioId);
            startActivity(intent);
        });


// Botón abrir la próxima tarea
        btnComenzarTarea.setOnClickListener(v -> {
            if (tareaProxima == null) {
                return;
            }

            Intent intent = new Intent(
                    PrincipalActivity.this,
                    RealizarTareaActivity.class
            );

            intent.putExtra("usuario_id", usuarioId);
            intent.putExtra("tarea_id", tareaProxima.getId());
            intent.putExtra("tarea_nombre", tareaProxima.getNombre());
            intent.putExtra(
                    "tarea_descripcion",
                    tareaProxima.getDescripcion()
            );
            intent.putExtra("inicio_programado", inicioProgramado);

            startActivity(intent);
        });

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (v, insets) -> {
                    Insets systemBars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                    );

                    v.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );

                    return insets;
                }
        );
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (usuarioId == -1 || btnComenzarTarea == null) {
            return;
        }

        pantallaVisible = true;

        // Evita usar una selección antigua al regresar.
        tareaProxima = null;
        btnComenzarTarea.setEnabled(false);

        handler.removeCallbacks(actualizarPrincipal);
        handler.post(actualizarPrincipal);
    }

    /**
     * Suspende las actualizaciones periódicas al dejar la pantalla.
     */
    @Override
    protected void onPause() {
        pantallaVisible = false;
        handler.removeCallbacks(actualizarPrincipal);
        super.onPause();
    }

    /**
     * Consulta las tareas del usuario y selecciona el inicio más cercano.
     *
     * Ignora las tareas desactivadas, eliminadas o sin un próximo inicio.
     * Usa el horario y la repetición para elegir la tarea.
     *
     * Actualiza la interfaz desde el hilo principal. Si no hay una tarea próxima, muestra un aviso y deshabilita Comenzar tarea.
     * Si falla SQLite, muestra "No se pudo consultar la próxima tarea".
     */
    private void cargarProximaTarea() {
        // Evita iniciar otra consulta si la anterior aún no terminó.
        if (cargando) {
            return;
        }

        cargando = true;

        executor.execute(() -> {
            TareaRepository repository = new TareaRepository(getApplicationContext());

            try {

                List<Tarea> tareas = repository.listarPorUsuario(usuarioId);

                long ahora = System.currentTimeMillis();
                long fechaMasCercana = Long.MAX_VALUE;
                Tarea seleccionada = null;

                for (Tarea tarea : tareas) {
                    if (!tarea.isActiva() || tarea.isEliminada()) {
                        continue;
                    }

                    long fecha = calcularProximoInicio(tarea, ahora);

                    if (fecha != -1 && fecha < fechaMasCercana) {
                        fechaMasCercana = fecha;
                        seleccionada = tarea;
                    }
                }

                final Tarea resultado = seleccionada;
                final long fechaResultado = fechaMasCercana;

                runOnUiThread(() -> {
                    cargando = false;

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    if (!pantallaVisible) {
                        return;
                    }

                    tareaProxima = resultado;

                    if (resultado == null) {
                        txtProximaTarea.setText("No tienes tareas activas próximas");

                        txtHoraTarea.setText("Crea una tarea o activa una existente");

                        btnComenzarTarea.setEnabled(false);
                        return;
                    }

                    inicioProgramado = fechaResultado;

                    txtProximaTarea.setText(resultado.getNombre());

                    SimpleDateFormat formato =
                            new SimpleDateFormat(
                                    "dd/MM/yyyy 'a las' HH:mm",
                                    Locale.getDefault()
                            );

                    txtHoraTarea.setText(
                            formato.format(new Date(inicioProgramado))
                                    + " · "
                                    + resultado.getDuracionMinutos()
                                    + " minutos"
                    );

                    btnComenzarTarea.setEnabled(true);
                });

            } catch (SQLiteException e) {
                runOnUiThread(() -> {
                    cargando = false;

                    if (isFinishing() || isDestroyed()
                            || !pantallaVisible) {
                        return;
                    }

                    tareaProxima = null;
                    btnComenzarTarea.setEnabled(false);

                    txtProximaTarea.setText("No se pudo consultar la próxima tarea");

                    txtHoraTarea.setText("Vuelve a esta pantalla para intentar nuevamente");
                });

            } finally {
                repository.cerrar();
            }
        });
    }

    /**
     * Calcula el primer inicio igual o posterior al instante de referencia.
     *
     * Para una tarea de una sola vez, combina la fecha guardada con su hora.
     * Para una diaria, usa hoy o mañana según si la hora ya pasó.
     * Para una semanal, revisa desde hoy hasta siete días después y devuelve el primer día seleccionado cuyo horario no haya pasado.
     *
     * @param tarea Tarea cuyo horario y repetición se evaluarán.
     * @param ahora Instante de referencia en milisegundos desde la época Unix.
     * @return Próximo inicio en milisegundos desde la época Unix, o -1 si no se puede determinar.
     */
    private long calcularProximoInicio(Tarea tarea, long ahora) {
        Calendar inicio = Calendar.getInstance();
        inicio.setTimeInMillis(ahora);

        if (Tarea.UNA_VEZ.equals(tarea.getRepeticion())) {
            if (tarea.getFechaUnica() == null) {
                return -1;
            }

            SimpleDateFormat formato = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);

            formato.setLenient(false);

            try {
                Date fecha = formato.parse(tarea.getFechaUnica());

                if (fecha == null) {
                    return -1;
                }

                inicio.setTime(fecha);

            } catch (ParseException e) {
                return -1;
            }
        }

        inicio.set(Calendar.HOUR_OF_DAY, tarea.getHoraInicio());
        inicio.set(Calendar.MINUTE, tarea.getMinutoInicio());
        inicio.set(Calendar.SECOND, 0);
        inicio.set(Calendar.MILLISECOND, 0);

        if (Tarea.UNA_VEZ.equals(tarea.getRepeticion())) {
            return inicio.getTimeInMillis() >= ahora
                    ? inicio.getTimeInMillis()
                    : -1;
        }

        if (Tarea.DIARIA.equals(tarea.getRepeticion())) {
            if (inicio.getTimeInMillis() < ahora) {
                inicio.add(Calendar.DAY_OF_MONTH, 1);
            }

            return inicio.getTimeInMillis();
        }

        if (Tarea.DIAS_SEMANA.equals(tarea.getRepeticion())) {
            boolean[] dias = tarea.getDias();


            for (int i = 0; i <= 7; i++) {
                int diaCalendario = inicio.get(Calendar.DAY_OF_WEEK);

                // Calendar: domingo = 1.
                // Nuestro arreglo: lunes = 0, domingo = 6.
                int posicion = (diaCalendario + 5) % 7;

                if (dias[posicion]
                        && inicio.getTimeInMillis() >= ahora) {
                    return inicio.getTimeInMillis();
                }

                inicio.add(Calendar.DAY_OF_MONTH, 1);
            }
        }

        return -1;
    }

    /**
     * Retira las actualizaciones pendientes y deja de aceptar nuevas consultas.
     * Las consultas ya enviadas al ejecutor pueden terminar.
     */
    @Override
    protected void onDestroy() {
        handler.removeCallbacks(actualizarPrincipal);
        executor.shutdown();
        super.onDestroy();
    }
}