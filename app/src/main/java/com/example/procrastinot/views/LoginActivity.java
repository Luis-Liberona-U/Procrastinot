package com.example.procrastinot.views;

import android.content.Intent;
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

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        TextView txtIrARegistro = findViewById(R.id.txtIrARegistro);
        EditText edtEmail = findViewById(R.id.edtEmail);
        EditText edtPassword = findViewById(R.id.edtPassword);
        Button btnLogin = findViewById(R.id.btnIniciarSesionLogin);

        btnLogin.setOnClickListener(V ->{

            String correo = edtEmail.getText().toString().trim();
            String contrasena = edtPassword.getText().toString().trim();

            if (correo.equalsIgnoreCase("luis@gmail.com") && contrasena.equals("123")){
                Intent intent = new Intent(
                        LoginActivity.this,
                        PrincipalActivity.class
                );
                intent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                startActivity(intent);
            } else {
                Toast.makeText(LoginActivity.this, "Correo o contraseña incorrectas", Toast.LENGTH_SHORT).show();
            }


        });









        txtIrARegistro.setOnClickListener(v -> {




            Intent intent = new Intent(LoginActivity.this, RegistroActivity.class);
            startActivity(intent);
        });





        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}