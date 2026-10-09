package com.example.procrastinot.views;

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
import com.example.procrastinot.adapters.ListaTareaAdapter;
import com.example.procrastinot.models.Tarea;
import com.example.procrastinot.repositories.TareaRepository;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TareasUsuarioActivity extends AppCompatActivity {

    private long usuarioId = -1;
    private ListaTareaAdapter adapter;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_tareas_usuario);

        usuarioId = getIntent().getLongExtra("usuario_id", -1);

        if (usuarioId == -1) {
            Toast.makeText(this, "No se recibió el usuario. Inicia sesión nuevamente.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        RecyclerView rvTareas = findViewById(R.id.rvTareas);
        rvTareas.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ListaTareaAdapter(
                new ListaTareaAdapter.AccionesTarea() {

                    @Override
                    public void editar(Tarea tarea) {
                        Toast.makeText(TareasUsuarioActivity.this, "Editar: " + tarea.getNombre(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void eliminar(Tarea tarea) {
                        Toast.makeText(TareasUsuarioActivity.this, "Eliminar: " + tarea.getNombre(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void cambiarActiva(Tarea tarea, boolean activa) {
                        cargarTareas();
                        Toast.makeText(TareasUsuarioActivity.this, "El cambio de estado aún está pendiente", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        rvTareas.setAdapter(adapter);

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

        if (usuarioId != -1 && adapter != null) {
            cargarTareas();
        }
    }

    private void cargarTareas() {
        executor.execute(() -> {
            TareaRepository repository = new TareaRepository(getApplicationContext());

            try {
                List<Tarea> tareas = repository.listarPorUsuario(usuarioId);

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    adapter.actualizarLista(tareas);

                    if (tareas.isEmpty()) {
                        Toast.makeText(TareasUsuarioActivity.this, "Todavía no tienes tareas creadas", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (SQLiteException e) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    Toast.makeText(TareasUsuarioActivity.this, "No se pudieron cargar las tareas", Toast.LENGTH_LONG).show();
                });

            } finally {
                repository.cerrar();
            }
        });
    }

    @Override
    protected void onDestroy() {
        executor.shutdown();
        super.onDestroy();
    }
}