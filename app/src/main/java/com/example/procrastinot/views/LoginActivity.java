package com.example.procrastinot.views;

import android.content.Intent;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.procrastinot.R;
import com.example.procrastinot.models.Usuario;
import com.example.procrastinot.repositories.UsuarioRepository;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Muestra la pantalla de inicio de sesión de Procrastinot.
 *
 * Comprueba que el usuario ingrese un correo y una contraseña y consulta las credenciales en SQLite a traves de la clase UsuarioRepository.
 * Si coinciden, abre PrincipalActivity con el ID y el nombre del usuario. También ofrece acceso RegistroActivity.
 */
public class LoginActivity extends AppCompatActivity {

    /** Ejecuta las consultas a SQLite fuera del hilo principal. */
    private final ExecutorService executor = Executors.newSingleThreadExecutor();


    /**
     * Carga el formulario, enlaza sus controles y configura las acciones para iniciar sesión o abrir el registro.
     * Los campos vacíos, el correo no registrado y la contraseña incorrecta se indican mediante mensajes en el campo correspondiente.
     *
     * @param savedInstanceState Estado guardado por Android al recrear la pantalla, o null si no existe.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        TextView txtIrARegistro = findViewById(R.id.txtIrARegistro);
        EditText edtEmail = findViewById(R.id.edtEmail);
        EditText edtPassword = findViewById(R.id.edtPassword);
        Button btnLogin = findViewById(R.id.btnIniciarSesionLogin);

        txtIrARegistro.setOnClickListener(v -> {
            Intent intent = new Intent(
                    LoginActivity.this,
                    RegistroActivity.class
            );
            startActivity(intent);
        });

        btnLogin.setOnClickListener(v -> {
            // Normaliza el correo igual que en el registro y conserva la contraseña exactamente como se ingresó.
            String correo = edtEmail.getText().toString().trim().toLowerCase(Locale.ROOT);

            String contrasena = edtPassword.getText().toString();

            if (correo.isEmpty()) {
                edtEmail.setError("Ingresa tu correo");
                edtEmail.requestFocus();
                return;
            }

            if (contrasena.isEmpty()) {
                edtPassword.setError("Ingresa tu contraseña");
                edtPassword.requestFocus();
                return;
            }

            // Evita otro intento mientras se consulta la base de datos
            btnLogin.setEnabled(false);

            executor.execute(() -> {
                UsuarioRepository repository = new UsuarioRepository(getApplicationContext());

                try {
                    if (!repository.existeCorreo(correo)) {
                        runOnUiThread(() -> {
                            if (isFinishing() || isDestroyed()) {
                                return;
                            }

                            btnLogin.setEnabled(true);

                            edtEmail.setError("No existe una cuenta con este correo");
                            edtEmail.requestFocus();
                        });

                        return;
                    }

                    Usuario usuario = repository.iniciarSesion(
                            correo,
                            contrasena
                    );

                    runOnUiThread(() -> {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        btnLogin.setEnabled(true);

                        if (usuario == null) {
                            edtPassword.setError("Contraseña incorrecta");
                            edtPassword.requestFocus();
                            return;
                        }

                        Intent intent = new Intent(
                                LoginActivity.this,
                                PrincipalActivity.class
                        );

                        // Envio de nombre e ID a la pantalla principal
                        intent.putExtra("usuario_id", usuario.getId());
                        intent.putExtra("usuario_nombre", usuario.getNombre());

                        intent.addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK
                        );

                        startActivity(intent);
                    });

                } catch (SQLiteException e) {
                    runOnUiThread(() -> {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        btnLogin.setEnabled(true);

                        Toast.makeText(LoginActivity.this, "No se pudo consultar la base de datos", Toast.LENGTH_SHORT).show();
                    });

                } finally {
                    repository.cerrar();
                }
            });
        });

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
     * Deja de aceptar nuevas consultas al destruirse la pantalla.
     * Las operaciones ya enviadas al ejecutor pueden terminar.
     */
    @Override
    protected void onDestroy() {
        executor.shutdown();
        super.onDestroy();
    }
}