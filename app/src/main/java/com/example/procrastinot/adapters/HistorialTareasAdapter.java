package com.example.procrastinot.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.procrastinot.R;
import com.example.procrastinot.models.RegistroTarea;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistorialTareasAdapter extends
        RecyclerView.Adapter<HistorialTareasAdapter.HistorialViewHolder> {

    private final List<RegistroTarea> registros = new ArrayList<>();
    private final AccionesHistorial acciones;

    public interface AccionesHistorial {
        void verResultado(RegistroTarea registro);
    }

    public HistorialTareasAdapter(AccionesHistorial acciones) {
        this.acciones = acciones;
    }

    public void actualizarLista(List<RegistroTarea> nuevaLista) {
        registros.clear();
        registros.addAll(nuevaLista);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HistorialViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View fila = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_historial, parent, false);

        return new HistorialViewHolder(fila);
    }

    @Override
    public void onBindViewHolder(
            @NonNull HistorialViewHolder holder,
            int position
    ) {
        RegistroTarea registro = registros.get(position);

        holder.nombre.setText(registro.getNombreTarea());

        SimpleDateFormat formato = new SimpleDateFormat(
                "dd/MM/yyyy HH:mm",
                Locale.getDefault()
        );

        holder.fecha.setText(
                "Realizada: " + formato.format(new Date(registro.getFinReal()))
        );

        long segundos = Math.max(
                0L,
                registro.getFinReal() - registro.getInicioReal()
        ) / 1000;

        String tiempo = String.format(
                Locale.getDefault(),
                "%02d:%02d:%02d",
                segundos / 3600,
                (segundos % 3600) / 60,
                segundos % 60
        );

        holder.tiempo.setText("Tiempo empleado: " + tiempo);

        holder.itemView.setOnClickListener(v ->
                acciones.verResultado(registro)
        );
    }

    @Override
    public int getItemCount() {
        return registros.size();
    }

    static class HistorialViewHolder extends RecyclerView.ViewHolder {

        final TextView nombre;
        final TextView fecha;
        final TextView tiempo;

        HistorialViewHolder(@NonNull View itemView) {
            super(itemView);

            nombre = itemView.findViewById(R.id.txtNombreHistorial);
            fecha = itemView.findViewById(R.id.txtFechaHistorial);
            tiempo = itemView.findViewById(R.id.txtTiempoHistorial);
        }
    }
}