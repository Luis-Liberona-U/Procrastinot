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

public class HistorialTareasActivity extends AppCompatActivity {

    private long usuarioId = -1;
    private HistorialTareasAdapter adapter;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

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

    @Override
    protected void onResume() {
        super.onResume();

        if (usuarioId != -1 && adapter != null) {
            cargarHistorial();
        }
    }

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

    @Override
    protected void onDestroy() {
        executor.shutdown();
        super.onDestroy();
    }
}