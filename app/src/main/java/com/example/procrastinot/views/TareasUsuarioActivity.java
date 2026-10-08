package com.example.procrastinot.views;

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

import java.util.ArrayList;
import java.util.List;

public class TareasUsuarioActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_tareas_usuario);

        RecyclerView rvTareas = findViewById(R.id.rvTareas);

// Organiza las tareas una debajo de otra
        rvTareas.setLayoutManager(new LinearLayoutManager(this));

        // Define que dependiendo de la opcion
        ListaTareaAdapter adapter = new ListaTareaAdapter(
                new ListaTareaAdapter.AccionesTarea() {
                    @Override
                    public void editar(Tarea tarea) {Toast.makeText(TareasUsuarioActivity.this, "Editar: " + tarea.getNombre(),Toast.LENGTH_SHORT).show();
                    }
                    @Override
                    public void eliminar(Tarea tarea) {Toast.makeText(TareasUsuarioActivity.this, "Eliminar: " + tarea.getNombre(), Toast.LENGTH_SHORT).show();
                    }
                    @Override
                    public void cambiarActiva(Tarea tarea, boolean activa) {
                        tarea.setActiva(activa);Toast.makeText(TareasUsuarioActivity.this, activa ? "Tarea activada" : "Tarea desactivada", Toast.LENGTH_SHORT).show();
                    }
                }
        );

// Conecta el adaptador con la lista
        rvTareas.setAdapter(adapter);

// Datos de prueba
        Tarea ejemplo = new Tarea();
        ejemplo.setId(1L);
        ejemplo.setNombre("Estudiar Java");
        ejemplo.setHoraInicio(18);
        ejemplo.setMinutoInicio(30);
        ejemplo.setRepeticion(Tarea.DIARIA);
        ejemplo.setActiva(true);

        List<Tarea> tareasPrueba = new ArrayList<>();
        tareasPrueba.add(ejemplo);

        Tarea fortnite = new Tarea();
        ejemplo.setId(2L);
        ejemplo.setNombre("Jugar Fornite");
        ejemplo.setHoraInicio(20);
        ejemplo.setMinutoInicio(30);
        ejemplo.setRepeticion(Tarea.DIARIA);
        ejemplo.setActiva(true);


        tareasPrueba.add(fortnite);

// Entrega los datos al adaptador
        adapter.actualizarLista(tareasPrueba);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}