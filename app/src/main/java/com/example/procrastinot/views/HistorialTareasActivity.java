package com.example.procrastinot.views;

import android.content.Intent;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.procrastinot.R;
import com.example.procrastinot.adapters.HistorialTareasAdapter;
import com.example.procrastinot.models.RegistroTarea;
import com.example.procrastinot.repositories.RegistroTareaRepository;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Muestra las realizaciones completadas del usuario.
 *
 * Al tocar una fila, abre ResultadoTareaActivity con el ID del usuario y el ID del registro seleccionado.
 */

public class HistorialTareasActivity extends AppCompatActivity {

    private long usuarioId = -1;
    private HistorialTareasAdapter adapter;

    /** Ejecuta las consultas a SQLite fuera del hilo principal. */
    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();


    /**
     * Carga el diseño, obtiene el ID del usuario y configura la lista.
     *
     * Si no se recibe el ID de usuario, muestra un mensaje de error y cierra la pantalla.
     *
     * @param savedInstanceState Estado guardado por Android al recrear la pantalla, o null si no existe.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_historial_tareas);

        usuarioId = getIntent().getLongExtra("usuario_id", -1);

        if (usuarioId == -1) {
            Toast.makeText(this, "No se recibió el usuario", Toast.LENGTH_LONG).show();

            finish();
            return;
        }

        RecyclerView rvHistorial = findViewById(R.id.rvHistorial);

        rvHistorial.setLayoutManager(new LinearLayoutManager(this));

        adapter = new HistorialTareasAdapter(registro -> {
            Intent intent = new Intent(
                    HistorialTareasActivity.this,
                    ResultadoTareaActivity.class
            );

            intent.putExtra("usuario_id", usuarioId);
            intent.putExtra("registro_id", registro.getId());

            startActivity(intent);
        });

        rvHistorial.setAdapter(adapter);

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

    /**
     * Carga el historial al entrar o volver a la pantalla, siempre que se haya recibido el ID del usuario y el adaptador esté inicializado.
     */
    @Override
    protected void onResume() {
        super.onResume();

        if (usuarioId != -1 && adapter != null) {
            cargarHistorial();
        }
    }

    /**
     * Consulta las realizaciones completadas en segundo plano y entrega los resultados al adaptador desde el hilo principal.
     *
     * Si la consulta devuelve una lista vacía, muestra "Todavía no tienes tareas completadas". Si se produce una SQLiteException, la captura y muestra
     * "No se pudo cargar el historial".
     */
    private void cargarHistorial() {
        executor.execute(() -> {
            RegistroTareaRepository repository = new RegistroTareaRepository(getApplicationContext());

            try {
                List<RegistroTarea> registros = repository.listarHistorial(usuarioId);

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    adapter.actualizarLista(registros);

                    if (registros.isEmpty()) {
                        Toast.makeText(this, "Todavía no tienes tareas completadas", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (SQLiteException e) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    Toast.makeText(this, "No se pudo cargar el historial", Toast.LENGTH_LONG).show();
                });

            } finally {
                repository.cerrar();
            }
        });
    }

    /**
     * Deja de aceptar nuevas consultas al destruirse la pantalla.
     * Las consultas ya enviadas al ejecutor pueden terminar.
     */
    @Override
    protected void onDestroy() {
        executor.shutdown();
        super.onDestroy();
    }
}