package com.example.procrastinot.views;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.widget.Button;

import com.example.procrastinot.R;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import java.util.Locale;

public class CrearTareaActivity extends AppCompatActivity {
    private int horaInicio = -1;
    private int minutoInicio = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Button btnHoraInicio = findViewById(R.id.btnHoraInicio);

        btnHoraInicio.setOnClickListener(v -> {

            MaterialTimePicker reloj = new MaterialTimePicker.Builder()
                    .setTitleText("Hora de inicio")
                    .setTimeFormat(TimeFormat.CLOCK_24H)
                    .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
                    .setHour(horaInicio == -1 ? 8 : horaInicio)
                    .setMinute(minutoInicio == -1 ? 0 : minutoInicio)
                    .build();

            reloj.addOnPositiveButtonClickListener(view -> {
                horaInicio = reloj.getHour();
                minutoInicio = reloj.getMinute();

                btnHoraInicio.setText(
                        String.format(
                                Locale.getDefault(),
                                "%02d:%02d",
                                horaInicio,
                                minutoInicio
                        )
                );
            });

            reloj.show(getSupportFragmentManager(), "RELOJ_INICIO");
        });




        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_crear_tarea);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}