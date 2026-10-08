package com.example.procrastinot.views;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.procrastinot.R;

public class PrincipalActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_principal);

        Button btnCrearTarea = findViewById(R.id.btnCrearTarea);
        Button btnVerTareas = findViewById(R.id.btnVerTareas);

        btnCrearTarea.setOnClickListener(v -> {
            Intent intent = new Intent(
                    PrincipalActivity.this,
                    CrearTareaActivity.class
            );

            startActivity(intent);
        });

        btnVerTareas.setOnClickListener(v -> {
            Intent intent = new Intent(
                    PrincipalActivity.this,
                    TareasUsuarioActivity.class
            );

            startActivity(intent);
        });





        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}