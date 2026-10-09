package com.example.procrastinot.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.procrastinot.R;
import com.example.procrastinot.models.Tarea;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ListaTareaAdapter extends RecyclerView.Adapter<ListaTareaAdapter.TareaViewHolder> {

    //Lista de tareas
    private final List<Tarea> tareas = new ArrayList<>();

    private final AccionesTarea acciones;

    public interface AccionesTarea {

        void editar(Tarea tarea);
        void eliminar(Tarea tarea);
        void cambiarActiva(Tarea tarea, boolean activa);
    }

    public ListaTareaAdapter(AccionesTarea acciones) {
        this.acciones = acciones;
    }

    // Remplaza la lista mostrada
    public void actualizarLista(List<Tarea> nuevaLista) {
        tareas.clear();
        tareas.addAll(nuevaLista);

        notifyDataSetChanged();
    }

    // Crea las filas
    @NonNull
    @Override
    public TareaViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View fila = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tarea, parent, false);

        return new TareaViewHolder(fila);
    }

    //Rellena con los daton las filasa
    @Override
    public void onBindViewHolder(
            @NonNull TareaViewHolder holder,
            int position
    ) {
        Tarea tarea = tareas.get(position);

        holder.nombre.setText(tarea.getNombre());

        holder.hora.setText(
                String.format(
                        Locale.getDefault(),
                        "%02d:%02d",
                        tarea.getHoraInicio(),
                        tarea.getMinutoInicio()
                )
        );

        holder.repeticion.setText(textoRepeticion(tarea));

        holder.activa.setOnCheckedChangeListener(null);
        holder.activa.setChecked(tarea.isActiva());

        holder.activa.setOnCheckedChangeListener(
                (button, checked) -> {
                    acciones.cambiarActiva(tarea, checked);
                }
        );

        holder.editar.setOnClickListener(v -> {
            acciones.editar(tarea);
        });

        holder.eliminar.setOnClickListener(v -> {
            acciones.eliminar(tarea);
        });
    }

    // Devuelve la cantidad de elementos
    @Override
    public int getItemCount() {
        return tareas.size();
    }

    // Convierte la repeticion en un texto
    private String textoRepeticion(Tarea tarea) {
        String repeticion = tarea.getRepeticion();

        if (Tarea.UNA_VEZ.equals(repeticion)) {
            return tarea.getFechaUnica() == null
                    ? "Una vez"
                    : tarea.getFechaUnica();
        }

        if (Tarea.DIARIA.equals(repeticion)) {
            return "Diaria";
        }

        if (Tarea.DIAS_SEMANA.equals(repeticion)) {
            boolean[] dias = tarea.getDias();

            boolean lunesAViernes =
                    dias[0] && dias[1] && dias[2]
                            && dias[3] && dias[4]
                            && !dias[5] && !dias[6];

            if (lunesAViernes) {
                return "Lun-Vie";
            }

            String[] nombres = {
                    "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"
            };

            StringBuilder resumen = new StringBuilder();

            for (int i = 0; i < dias.length; i++) {
                if (dias[i]) {
                    if (resumen.length() > 0) {
                        resumen.append(", ");
                    }

                    resumen.append(nombres[i]);
                }
            }

            return resumen.length() == 0
                    ? "Sin días"
                    : resumen.toString();
        }

        return "Sin repetición";
    }

    static class TareaViewHolder extends RecyclerView.ViewHolder {

        final TextView nombre;
        final TextView hora;
        final TextView repeticion;

        final Switch activa;
        final ImageButton editar;
        final Button eliminar;

        TareaViewHolder(@NonNull View itemView) {
            super(itemView);

            nombre = itemView.findViewById(R.id.txtNombreTareaLista);

            hora = itemView.findViewById(R.id.txtHoraTareaLista);

            repeticion = itemView.findViewById(R.id.txtRepeticionTatrea);

            activa = itemView.findViewById(R.id.switchActivaTarea);

            editar = itemView.findViewById(R.id.btnEditarTarea);

            eliminar = itemView.findViewById(R.id.btnEliminar);
        }
    }
}