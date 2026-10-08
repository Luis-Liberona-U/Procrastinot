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

public class LoginActivity extends AppCompatActivity {

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

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

            btnLogin.setEnabled(false);

            executor.execute(() -> {
                UsuarioRepository repository = new UsuarioRepository(getApplicationContext());

                try {
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
                            Toast.makeText(LoginActivity.this, "Correo o contraseña incorrectos", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Intent intent = new Intent(
                                LoginActivity.this,
                                PrincipalActivity.class
                        );

                        //Envio de nombre y Id a pantalla princiapal
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

    @Override
    protected void onDestroy() {
        executor.shutdown();
        super.onDestroy();
    }
}