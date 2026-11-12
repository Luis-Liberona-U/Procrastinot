package com.example.procrastinot.security;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Conserva la sesión local de Procrastinot entre aperturas de la app.
 *
 * Guarda únicamente el ID del usuario en SharedPreferences.
 * La contraseña y su hash no se guardan en las preferencias.
 */
public final class SesionManager {

    /** Valor devuelto cuando no hay un ID de usuario guardado. */
    public static final long SIN_SESION = -1L;

    private static final String ARCHIVO = "sesion_procrastinot";
    private static final String USUARIO_ID = "usuario_id";

    private final SharedPreferences preferencias;

    /**
     * Abre las preferencias privadas que contienen la sesión.
     *
     * @param context Contexto de Android para acceder a las preferencias.
     */
    public SesionManager(Context context) {preferencias = context.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE);}


    /**
     * Guarda el ID de la cuenta después de un inicio de sesión correcto.
     *
     * @param usuarioId ID del usuario que inició sesión.
     */
    public void guardarSesion(long usuarioId) {preferencias.edit().putLong(USUARIO_ID, usuarioId).apply();}


    /**
     * Recupera el ID guardado en las preferencias.
     *
     * @return ID guardado, o {@link #SIN_SESION} si no hay ninguno.
     */
    public long obtenerUsuarioId() {return preferencias.getLong(USUARIO_ID, SIN_SESION);}


    /**
     * Elimina el ID guardado para impedir que se restaure la sesión al volver a abrir la app. La cuenta y sus datos siguen en SQLite.
     */
    public void cerrarSesion() {preferencias.edit().remove(USUARIO_ID).apply();}
}