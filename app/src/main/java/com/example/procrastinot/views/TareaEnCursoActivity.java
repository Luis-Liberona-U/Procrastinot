package com.example.procrastinot.views;

import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.procrastinot.R;
import com.example.procrastinot.models.RegistroTarea;
import com.example.procrastinot.repositories.RegistroTareaRepository;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TareaEnCursoActivity extends AppCompatActivity {

    private long usuarioId;
    private long registroId;

    private Long inicioReal;
    private Long finReal;

    private TextView txtTiempoTranscurrido;
    private Button btnFinalizarTarea;

    private boolean pantallaVisible = false;

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private final Runnable actualizarTiempo = new Runnable() {
        @Override
        public void run() {
            if (!pantallaVisible || inicioReal == null) {
                return;
            }

            mostrarTiempo();

            if (finReal == null) {
                handler.postDelayed(this, 1000);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_tarea_en_curso);

        usuarioId = getIntent().getLongExtra("usuario_id", -1);
        registroId = getIntent().getLongExtra("registro_id", -1);

        if (usuarioId == -1 || registroId == -1) {
            Toast.makeText(this, "No se recibieron los datos de la realización", Toast.LENGTH_LONG).show();

            finish();
            return;
        }

        TextView txtNombre = findViewById(R.id.txtNombreTareaEnCurso);
        txtTiempoTranscurrido = findViewById(R.id.txtTiempoTranscurrido);
        btnFinalizarTarea = findViewById(R.id.btnFinalizarTarea);



        String nombre = getIntent().getStringExtra("tarea_nombre");

        txtNombre.setText(nombre == null ? "Tarea en curso" : nombre);

        txtTiempoTranscurrido.setText("00:00:00");
        btnFinalizarTarea.setEnabled(false);

        btnFinalizarTarea.setOnClickListener(v -> finalizarTarea());

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

        if (registroId == -1 || txtTiempoTranscurrido == null) {
            return;
        }

        pantallaVisible = true;
        cargarRegistro();
    }

    private void cargarRegistro() {
        btnFinalizarTarea.setEnabled(false);

        executor.execute(() -> {
            RegistroTareaRepository repository = new RegistroTareaRepository(getApplicationContext());

            try {
                RegistroTarea registro = repository.obtenerRegistro(registroId, usuarioId);

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()
                            || !pantallaVisible) {
                        return;
                    }

                    if (registro == null
                            || registro.getInicioReal() == null) {
                        Toast.makeText(this, "No se encontró una realización iniciada", Toast.LENGTH_LONG).show();

                        finish();
                        return;
                    }

                    inicioReal = registro.getInicioReal();
                    finReal = registro.getFinReal();

                    btnFinalizarTarea.setEnabled(
                            RegistroTarea.EN_CURSO.equals(registro.getEstado())
                                    && finReal == null
                    );

                    handler.removeCallbacks(actualizarTiempo);
                    handler.post(actualizarTiempo);
                });

            } catch (SQLiteException e) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()
                            || !pantallaVisible) {
                        return;
                    }

                    Toast.makeText(this, "No se pudo consultar la realización", Toast.LENGTH_LONG).show();
                });

            } finally {
                repository.cerrar();
            }
        });
    }

    private void mostrarTiempo() {
        long instanteFinal = finReal == null
                ? System.currentTimeMillis()
                : finReal;

        long segundosTotales = Math.max(0L, instanteFinal - inicioReal) / 1000;

        long horas = segundosTotales / 3600;
        long minutos = (segundosTotales % 3600) / 60;
        long segundos = segundosTotales % 60;

        txtTiempoTranscurrido.setText(
                String.format(
                        Locale.getDefault(),
                        "%02d:%02d:%02d",
                        horas,
                        minutos,
                        segundos
                )
        );
    }

    private void finalizarTarea() {
        final long instanteFin = System.currentTimeMillis();

        btnFinalizarTarea.setEnabled(false);

        executor.execute(() -> {
            RegistroTareaRepository repository = new RegistroTareaRepository(getApplicationContext());

            try {
                boolean guardado = repository.registrarFin(
                        registroId,
                        usuarioId,
                        instanteFin
                );

                if (!guardado) {
                    throw new IllegalStateException("No se pudo registrar la finalización");
                }

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    finReal = instanteFin;
                    handler.removeCallbacks(actualizarTiempo);
                    mostrarTiempo();

                    Intent intent = new Intent(
                            TareaEnCursoActivity.this,
                            FinalizarTareaActivity.class
                    );

                    intent.putExtra("usuario_id", usuarioId);
                    intent.putExtra("registro_id", registroId);
                    intent.putExtra("tarea_nombre", getIntent().getStringExtra("tarea_nombre")
                    );

                    startActivity(intent);
                    finish();
                });

            } catch (SQLiteException | IllegalStateException e) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    Toast.makeText(this, "No se pudo finalizar la tarea", Toast.LENGTH_LONG).show();
                    cargarRegistro();
                });

            } finally {
                repository.cerrar();
            }
        });
    }

    @Override
    protected void onPause() {
        pantallaVisible = false;
        handler.removeCallbacks(actualizarTiempo);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(actualizarTiempo);
        executor.shutdown();
        super.onDestroy();
    }
}