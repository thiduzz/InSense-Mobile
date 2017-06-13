package com.apackage.db;

/**
 * Created by thidu on 13/06/2017.
 */

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.apackage.model.User;

import java.util.ArrayList;

public class DataBase extends SQLiteOpenHelper {

    private static int VERSAO_TABELA = 1;
    private static String NOME_TABELA = "USERS";
    private static String NOME_TABELA_DEVICES = "DEVICES";
    private static String NOME_TABELA_SETTINGS = "SETTINGS";

    private static String COLUNA_ID = "ID";
    private static String COLUNA_FK_USER = "USER_ID";
    private static String COLUNA_NAME = "NAME";
    private static String COLUNA_LOGIN = "EMAIL";
    private static String COLUNA_TOKEN = "ACCESS_TOKEN";
    private static String COLUNA_REFRESH_TOKEN = "REFRESH_TOKEN";
    private static String COLUNA_MODEL = "MODEL";
    private static String COLUNA_OPTION = "OPTION";
    private static String COLUNA_OPTION_VALUE = "OPTION_VALUE";

    private static final String CREATE_TABLE_QUERY =
            "CREATE TABLE " + NOME_TABELA + " (" +
                    COLUNA_ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    COLUNA_LOGIN + " TEXT, " +
                    COLUNA_NAME + " TEXT, " +
                    COLUNA_TOKEN + " TEXT, " +
                    COLUNA_REFRESH_TOKEN + " TEXT );";

    private static final String CREATE_DEVICES_TABLE_QUERY =
            "CREATE TABLE " + NOME_TABELA_DEVICES + " (" +
                    COLUNA_ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    COLUNA_MODEL + " TEXT, " +
                    COLUNA_FK_USER + " INTEGER"+
                    " FOREIGN KEY ("+COLUNA_FK_USER+") REFERENCES "+NOME_TABELA+"("+COLUNA_ID+")"+
                    ");";

    private static final String CREATE_SETTINGS_TABLE_QUERY =
            "CREATE TABLE " + NOME_TABELA_DEVICES + " (" +
                    COLUNA_ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    COLUNA_OPTION + " TEXT, " +
                    COLUNA_OPTION_VALUE + " TEXT, " +
                    COLUNA_FK_USER + " INTEGER"+
                    " FOREIGN KEY ("+COLUNA_FK_USER+") REFERENCES "+NOME_TABELA+"("+COLUNA_ID+")"+
                    ");";

    private String[] colunas = {
            COLUNA_LOGIN,COLUNA_TOKEN
    };

    public DataBase(Context context){
        super(context,NOME_TABELA,null,VERSAO_TABELA);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_QUERY);
        db.execSQL(CREATE_SETTINGS_TABLE_QUERY);
        db.execSQL(CREATE_DEVICES_TABLE_QUERY);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //TODO REMOVE
    }

    public ArrayList<User> getUsers() {
        SQLiteDatabase db = getReadableDatabase();
        //SQLiteDatabase dwb = getWritableDatabase();
        ArrayList<User> users = new ArrayList<>();
        Cursor cursor = db.query(
                NOME_TABELA,
                colunas,
                null, // Claúsulas
                null, //
                null,
                null,
                null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            User user = convertCursorOnCliente(cursor);
            users.add(user);
            cursor.moveToNext();
        }
        // Importante
        cursor.close();
        return users;
    }


    private User convertCursorOnCliente(Cursor cursor){

        User user = new User();
        user.setLogin(cursor.getString(
                cursor.getColumnIndexOrThrow(COLUNA_LOGIN)));

        user.setToken(cursor.getString(
                cursor.getColumnIndexOrThrow(COLUNA_TOKEN)));


        user.setRefreshToken(cursor.getString(
                cursor.getColumnIndexOrThrow(COLUNA_REFRESH_TOKEN)));

        return user;
    }

    public boolean authenticateClient(String login,String pass)
    {
        SQLiteDatabase db = getReadableDatabase();
        User cliente = null;
        Cursor cursor = db.query(NOME_TABELA, new String[]{COLUNA_LOGIN, COLUNA_TOKEN}, COLUNA_LOGIN +" = ? and "+COLUNA_TOKEN+" = ?",new String[]{login,pass},null,null,null);
        if(cursor.getCount() > 0)
        {
            return true;
        }
        return false;
    }

    public boolean editClient(String old_login, String login,String token, String refresh_token)
    {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUNA_LOGIN, login);
        values.put(COLUNA_TOKEN, token);
        values.put(COLUNA_REFRESH_TOKEN, refresh_token);
        int result = db.update(NOME_TABELA, values, COLUNA_LOGIN +" = '"+ old_login + "'", null);
        if(result > 0)
        {
            return true;
        }
        return false;
    }

    public void saveCliente(User cliente){

        SQLiteDatabase db = getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUNA_LOGIN,cliente.getLogin());
        contentValues.put(COLUNA_TOKEN,cliente.getToken());
        contentValues.put(COLUNA_REFRESH_TOKEN,cliente.getRefreshToken());
        db.insert(NOME_TABELA,null,contentValues);

    }


}