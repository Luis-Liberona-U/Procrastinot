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
import com.example.procrastinot.adapters.ListaTareaAdapter;
import com.example.procrastinot.models.Tarea;
import com.example.procrastinot.repositories.TareaRepository;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Muestra las tareas no eliminadas del usuario en la pantalla Mis tareas.
 *
 * Permite abrir el formulario de edición y solicitar la eliminación lógica de una tarea. La eliminación lógica la quita de la lista sin borrar sus registros del historial.
 */
public class TareasUsuarioActivity extends AppCompatActivity {

    private long usuarioId = -1;
    private ListaTareaAdapter adapter;

    /** Ejecuta las consultas y actualizaciones de SQLite fuera del hilo principal. */
    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();


    /**
     * Carga el diseño, configura la lista y conecta las acciones de cada fila.
     *
     * Si no se recibe el ID del usuario, muestra "No se recibió el usuario. Inicia sesión nuevamente." y cierra la pantalla.
     *
     * @param savedInstanceState Estado guardado por Android al recrear la pantalla, o null si no existe.
     */
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
                        Intent intent = new Intent(
                                TareasUsuarioActivity.this,
                                EditarTareaActivity.class
                        );

                        intent.putExtra("usuario_id", usuarioId);
                        intent.putExtra("tarea_id", tarea.getId());

                        startActivity(intent);
                    }

                    @Override
                    public void eliminar(Tarea tarea) {
                        new MaterialAlertDialogBuilder(TareasUsuarioActivity.this)
                                .setTitle("Eliminar tarea")
                                .setMessage("¿Quieres eliminar \"" + tarea.getNombre() + "\"?" + "\nSu historial se conservará."
                                )
                                .setNegativeButton("Cancelar", null)
                                .setPositiveButton("Eliminar", (dialog, which) -> {
                                    eliminarTarea(tarea.getId());
                                })
                                .show();
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

    /**
     * Recarga las tareas al entrar o volver a la pantalla.
     */
    @Override
    protected void onResume() {
        super.onResume();

        if (usuarioId != -1 && adapter != null) {
            cargarTareas();
        }
    }

    /**
     * Consulta en segundo plano las tareas no eliminadas del usuario y actualiza la lista desde el hilo principal.
     *
     * Si no hay tareas, muestra "Todavia no tienes tareas creadas".
     * Si la consulta falla con una SQLiteException, muestra "No se pudieron cargar las tareas".
     */
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
                        Toast.makeText(TareasUsuarioActivity.this, "Todavia no tienes tareas creadas", Toast.LENGTH_SHORT).show();
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

    /**
     * Solicita la eliminación lógica de una tarea del usuario actual.
     *
     * Si se devuelve un resultado, muestra "Tarea eliminada" o "No se pudo eliminar la tarea" y recarga la lista.
     * Si SQLite lanza una excepción, muestra "Error al eliminar la tarea".
     *
     * @param tareaId Identificador de la tarea seleccionada.
     */
    private void eliminarTarea(long tareaId) {
        executor.execute(() -> {
            TareaRepository repository = new TareaRepository(getApplicationContext());

            try {
                boolean eliminada = repository.eliminarTarea(
                        tareaId,
                        usuarioId
                );

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    Toast.makeText(TareasUsuarioActivity.this, eliminada ? "Tarea eliminada" : "No se pudo eliminar la tarea", Toast.LENGTH_SHORT).show();

                    cargarTareas();
                });

            } catch (SQLiteException e) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    Toast.makeText(TareasUsuarioActivity.this, "Error al eliminar la tarea", Toast.LENGTH_LONG).show();
                });

            } finally {
                repository.cerrar();
            }
        });
    }

    /**
     * Deja de aceptar nuevas operaciones al destruirse la pantalla.
     * Las operaciones ya enviadas al ejecutor pueden terminar.
     */
    @Override
    protected void onDestroy() {
        executor.shutdown();
        super.onDestroy();
    }
}