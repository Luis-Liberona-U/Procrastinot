package com.example.procrastinot.views;

import android.content.Intent;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.procrastinot.R;
import com.example.procrastinot.models.Usuario;
import com.example.procrastinot.repositories.UsuarioRepository;
import com.example.procrastinot.security.SesionManager;

/**
 * Muestra la pantalla inicial de Procrastinot.
 *
 * El botón Comenzar abre LoginActivity donde el usuario puede ingresar sus credenciales.
 */
public class BienvenidaActivity extends AppCompatActivity {

    /**
     * Carga el diseño de bienvenida, ajusta el espacio para las barras del sistema y configura el botón que abre el inicio de sesión.
     * Si la cuenta guardada sigue existiendo en SQLite, abre Principal.
     * Si no hay una sesión válida, el botón Comenzar abre el inicio de sesión.
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

        restaurarSesion(btnComenzar);

    }

    /**
     * Busca en segundo plano la cuenta asociada a la sesión guardada.
     *
     * Abre Principal si la cuenta existe. Si la cuenta ya no existe, elimina el ID guardado y vuelve a habilitar el botón Comenzar.
     *
     * @param btnComenzar Botón que permite abrir el inicio de sesión.
     */
    private void restaurarSesion(Button btnComenzar) {
        SesionManager sesion = new SesionManager(this);
        long usuarioId = sesion.obtenerUsuarioId();

        if (usuarioId == SesionManager.SIN_SESION) {
            return;
        }

        btnComenzar.setEnabled(false);

        new Thread(() -> {
            UsuarioRepository repository = new UsuarioRepository(getApplicationContext());

            try {
                Usuario usuario = repository.buscarPorId(usuarioId);

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    if (sesion.obtenerUsuarioId() != usuarioId) {
                        btnComenzar.setEnabled(true);
                        return;
                    }

                    if (usuario == null) {
                        sesion.cerrarSesion();
                        btnComenzar.setEnabled(true);
                        return;
                    }

                    Intent intent = new Intent(this, PrincipalActivity.class);
                    intent.putExtra("usuario_id", usuario.getId());
                    intent.putExtra("usuario_nombre", usuario.getNombre());
                    startActivity(intent);
                    finish();
                });
            } catch (SQLiteException e) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    btnComenzar.setEnabled(true);
                    Toast.makeText(this, "No se pudo recuperar la sesión", Toast.LENGTH_SHORT).show();
                });
            } finally {
                repository.cerrar();
            }
        }).start();
    }
}