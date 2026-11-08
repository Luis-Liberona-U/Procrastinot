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

/**
 * Muestra la pantalla inicial de Procrastinot.
 *
 * El botón Comenzar abre LoginActivity donde el usuario puede ingresar sus credenciales.
 */
public class BienvenidaActivity extends AppCompatActivity {

    /**
     * Carga el diseño de bienvenida, ajusta el espacio para las barras del sistema y configura el botón que abre el inicio de sesión.
     *
     * @param savedInstanceState Estado guardado por Android al recrear la pantalla, o null si no existe.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_bienvenida);

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

        Button btnComenzar = findViewById(R.id.btnComenzar);


        btnComenzar.setOnClickListener(view -> {

            Intent intent = new Intent(
                    BienvenidaActivity.this,
                    LoginActivity.class
            );

            startActivity(intent);
        });


    }
}