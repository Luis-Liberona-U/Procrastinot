package com.example.procrastinot.repositories;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.procrastinot.database.ProcrastinotContract.Usuarios;
import com.example.procrastinot.database.ProcrastinotDbHelper;
import com.example.procrastinot.models.Usuario;

public class UsuarioRepository {

    private final ProcrastinotDbHelper helper;

    public UsuarioRepository(Context context) {
        helper = new ProcrastinotDbHelper(context);
    }

    // VERIFICACION CORREO EXISTENTE
    public boolean existeCorreo(String correo) {
        SQLiteDatabase db = helper.getReadableDatabase();

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

    // INSERT USUARIO (Registrar usuario)
    public long registrarUsuario(
            String nombre,
            String correo,
            String contrasena
    ) {
        SQLiteDatabase db = helper.getWritableDatabase();

        ContentValues valores = new ContentValues();
        valores.put(Usuarios.COLUMN_NOMBRE, nombre);
        valores.put(Usuarios.COLUMN_CORREO, correo);
        valores.put(Usuarios.COLUMN_PASSWORD_HASH, contrasena);

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
    public Usuario iniciarSesion(String correo, String contrasena) {
        SQLiteDatabase db = helper.getReadableDatabase();

        String filtro = Usuarios.COLUMN_CORREO + " = ? AND "
                + Usuarios.COLUMN_PASSWORD_HASH + " = ?";

        String[] argumentos = {correo, contrasena};

        try (Cursor cursor = db.query(
                Usuarios.TABLE_NAME,
                new String[]{
                        Usuarios._ID,
                        Usuarios.COLUMN_NOMBRE,
                        Usuarios.COLUMN_CORREO
                },
                filtro,
                argumentos,
                null,
                null,
                null
        )) {
            if (cursor.moveToFirst()) {
                Usuario usuario = new Usuario();

                usuario.setId(cursor.getLong(cursor.getColumnIndexOrThrow(Usuarios._ID)));

                usuario.setNombre(cursor.getString(cursor.getColumnIndexOrThrow(Usuarios.COLUMN_NOMBRE)));

                usuario.setCorreo(cursor.getString(cursor.getColumnIndexOrThrow(Usuarios.COLUMN_CORREO)));

                return usuario;
            }

            return null;
        }
    }
}