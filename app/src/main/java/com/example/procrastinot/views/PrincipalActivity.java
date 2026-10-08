package com.example.procrastinot.views;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.procrastinot.R;

public class PrincipalActivity extends AppCompatActivity {
private long usuarioId; //Guarda id del usuario
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_principal);

        // Asignar nombre al titulo
        usuarioId = getIntent().getLongExtra("usuario_id", -1);
        String nombreUsuario = getIntent().getStringExtra("usuario_nombre");

        TextView txtBienvenida = findViewById(R.id.txtTituloBienvenida);
        txtBienvenida.setText("Bienvenido: " + nombreUsuario);



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



        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}