package com.example.procrastinot.repositories;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.procrastinot.database.ProcrastinotContract.Usuarios;
import com.example.procrastinot.database.ProcrastinotDbHelper;
import com.example.procrastinot.models.Usuario;
import com.example.procrastinot.security.PasswordUtils;

/**
 * Gestiona el registro y la consulta de usuarios en SQLite.
 */

public class UsuarioRepository {

    /** Administra la apertura, creación y actualización de la base de datos. */
    private final ProcrastinotDbHelper helper;

    /**
     * Inicializa el repositorio y su administrador de SQLite.
     *
     * @param context Contexto utilizado para crear el Helper.
     */
    public UsuarioRepository(Context context) {
        helper = new ProcrastinotDbHelper(context);
    }

    /**
     * Comprueba si existe un usuario con el correo indicado.
     *
     * El correo se consulta tal como se recibe, este método no elimina espacios ni convierte el texto a minúsculas.
     *
     * @param correo Correo electrónico que se desea consultar.
     * @return true si encuentra una cuenta y false si no existe.
     * @throws android.database.sqlite.SQLiteException
     *         Si ocurre un error al abrir o consultar la base de datos.
     */
    public boolean existeCorreo(String correo) {
        SQLiteDatabase db = helper.getReadableDatabase();

        // El Cursor se cierra automaticamente al salir del bloque try.
        try (Cursor cursor = db.query(
                Usuarios.TABLE_NAME,
                new String[]{Usuarios._ID},
                Usuarios.COLUMN_CORREO + " = ?",
                new String[]{correo},
                null,
                null,
                null
        )) {
            return cursor.moveToFirst();
        }
    }

    /**
     * Inserta una nueva cuenta en la tabla de usuarios.
     *
     * La Activity debe validar los datos antes de llamar a este metodo.
     * La restricción UNIQUE de la tabla impide repetir un correo.
     * La contraseña se transforma en un hash antes de almacenarse.
     *
     * @param nombre Nombre del usuario.
     * @param correo Correo electrónico de la cuenta.
     * @param contrasena Contraseña que se almacenará.
     * @return ID del usuario creado, o -1 si la inserción falla.
     * @throws android.database.sqlite.SQLiteException
     *         Sí ocurre un error al abrir la base de datos para escritura.
     */
    public long registrarUsuario(
            String nombre,
            String correo,
            String contrasena
    ) {
        SQLiteDatabase db = helper.getWritableDatabase();

        // Relaciona cada columna con el valor que se insertará.
        ContentValues valores = new ContentValues();
        valores.put(Usuarios.COLUMN_NOMBRE, nombre);
        valores.put(Usuarios.COLUMN_CORREO, correo);

        long inicio = android.os.SystemClock.elapsedRealtime();
        String hash = PasswordUtils.generarHash(contrasena);

        android.util.Log.d("Procrastinot", "Tiempo del hash: " + (android.os.SystemClock.elapsedRealtime() - inicio) + " ms");

        // Genera el hash antes de guardar la cuenta.
        valores.put(Usuarios.COLUMN_PASSWORD_HASH, PasswordUtils.generarHash(contrasena)
        );
        return db.insert(
                Usuarios.TABLE_NAME,
                null,
                valores
        );
    }

    public void cerrar() {
        helper.close();
    }

    // FUNCION INICIAR SESION
    /**
     * Busca una cuenta con correo y contraseña que coincida con los datos recibidos.
     *
     *
     * El objeto devuelto contiene el ID, nombre y correo del usuario no incluye su contraseña.
     *
     * @param correo Correo electrónico de la cuenta.
     * @param contrasena Contraseña que se comparará con la almacenada.
     * @return Usuario encontrado, o null si las credenciales no coinciden.
     * @throws android.database.sqlite.SQLiteException Si ocurre un error al abrir o consultar la base de datos.
     * @throws IllegalStateException Si no se puede calcular el hash.
     */
    public Usuario iniciarSesion(String correo, String contrasena) {
        SQLiteDatabase db = helper.getReadableDatabase();

        String filtro = Usuarios.COLUMN_CORREO + " = ?";

        String[] argumentos = {correo};

        try (Cursor cursor = db.query(
                Usuarios.TABLE_NAME,
                new String[]{
                        Usuarios._ID,
                        Usuarios.COLUMN_NOMBRE,
                        Usuarios.COLUMN_CORREO,
                        Usuarios.COLUMN_PASSWORD_HASH
                },
                filtro,
                argumentos,
                null,
                null,
                null
        )) {
            if (!cursor.moveToFirst()) {
                return null;
            }

            String hash = cursor.getString(cursor.getColumnIndexOrThrow(Usuarios.COLUMN_PASSWORD_HASH));

            if (!PasswordUtils.verificar(contrasena, hash)) {
                return null;
            }

                // Convierte la fila encontrada en un objeto del modelo Usuario.
                Usuario usuario = new Usuario();

                usuario.setId(cursor.getLong(cursor.getColumnIndexOrThrow(Usuarios._ID)));

                usuario.setNombre(cursor.getString(cursor.getColumnIndexOrThrow(Usuarios.COLUMN_NOMBRE)));

                usuario.setCorreo(cursor.getString(cursor.getColumnIndexOrThrow(Usuarios.COLUMN_CORREO)));

                return usuario;



        }
    }

    /**
     * Busca una cuenta por su ID sin consultar sus credenciales.
     *
     * @param usuarioId ID de la cuenta guardada en la sesión.
     * @return Usuario encontrado o null si ya no existe.
     */
    public Usuario buscarPorId(long usuarioId) {
        SQLiteDatabase db = helper.getReadableDatabase();

        try (Cursor cursor = db.query(
                Usuarios.TABLE_NAME,
                new String[]{Usuarios._ID, Usuarios.COLUMN_NOMBRE, Usuarios.COLUMN_CORREO},
                Usuarios._ID + " = ?",
                new String[]{String.valueOf(usuarioId)},
                null, null, null
        )) {
            if (!cursor.moveToFirst()) {
                return null;
            }

            return new Usuario(cursor.getLong(0), cursor.getString(1), cursor.getString(2));
        }
    }
}