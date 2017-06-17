package com.apackage.db;

/**
 * Created by thidu on 13/06/2017.
 */

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

import com.apackage.model.User;

import java.util.ArrayList;

public class DataBase extends SQLiteOpenHelper {

    private static int VERSAO_TABELA = 1;
    private static String NOME_TABELA_USERS = "USERS";
    private static String NOME_TABELA_DEVICES = "DEVICES";
    private static String NOME_TABELA_SETTINGS = "SETTINGS";

    private static String COLUNA_ID = "ID";
    private static String COLUNA_FK_USER = "USER_ID";
    private static String COLUNA_NAME = "NAME";
    private static String COLUNA_LOGIN = "EMAIL";
    private static String COLUNA_TOKEN = "ACCESS_TOKEN";
    private static String COLUNA_REFRESH_TOKEN = "REFRESH_TOKEN";
    private static String COLUNA_ACTIVE_USER = "ACTIVE";
    private static String COLUNA_MODEL = "MODEL";
    private static String COLUNA_OPTION = "OPTION";
    private static String COLUNA_OPTION_VALUE = "OPTION_VALUE";

    private static final String CREATE_TABLE_QUERY =
            "CREATE TABLE IF NOT EXISTS " + NOME_TABELA_USERS + " (" +
                    COLUNA_ID + " INTEGER PRIMARY KEY NOT NULL, " +
                    COLUNA_LOGIN + " TEXT, " +
                    COLUNA_NAME + " TEXT, " +
                    COLUNA_TOKEN + " TEXT, " +
                    COLUNA_ACTIVE_USER + " BOOLEAN NOT NULL DEFAULT 0 CHECK ("+COLUNA_ACTIVE_USER+" IN (0,1)), " +
                    COLUNA_REFRESH_TOKEN + " TEXT );";

    private static final String CREATE_DEVICES_TABLE_QUERY =
            "CREATE TABLE IF NOT EXISTS " + NOME_TABELA_DEVICES + " (" +
                    COLUNA_ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    COLUNA_MODEL + " TEXT, " +
                    COLUNA_FK_USER + " INTEGER,"+
                    " FOREIGN KEY ("+COLUNA_FK_USER+") REFERENCES "+NOME_TABELA_USERS+"("+COLUNA_ID+")"+
                    ");";

    private static final String CREATE_SETTINGS_TABLE_QUERY =
            "CREATE TABLE IF NOT EXISTS " + NOME_TABELA_DEVICES + " (" +
                    COLUNA_ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    COLUNA_OPTION + " TEXT, " +
                    COLUNA_OPTION_VALUE + " TEXT, " +
                    COLUNA_FK_USER + " INTEGER,"+
                    " FOREIGN KEY ("+COLUNA_FK_USER+") REFERENCES "+NOME_TABELA_USERS+"("+COLUNA_ID+")"+
                    ");";

    private String[] colunas = {
            COLUNA_LOGIN,COLUNA_TOKEN
    };

    private Context context;

    public DataBase(Context context){
        super(context,NOME_TABELA_USERS,null,VERSAO_TABELA);
        this.context = context;
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

    public ArrayList<User> getCurrentUser() {
        SQLiteDatabase db = getReadableDatabase();
        //SQLiteDatabase dwb = getWritableDatabase();
        ArrayList<User> users = new ArrayList<>();
        Cursor cursor = db.query(
                NOME_TABELA_USERS,
                colunas,
                null, // Claúsulas
                null, //
                null,
                null,
                null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            User user = convertCursorOnUsers(cursor);
            users.add(user);
            cursor.moveToNext();
        }
        // Importante
        cursor.close();
        return users;
    }


    private User convertCursorOnUsers(Cursor cursor){

        try{
            User user = new User();
            user.setId(cursor.getInt(
                    cursor.getColumnIndexOrThrow(COLUNA_ID)));
            user.setName(cursor.getString(
                    cursor.getColumnIndexOrThrow(COLUNA_NAME)));
            user.setLogin(cursor.getString(
                    cursor.getColumnIndexOrThrow(COLUNA_LOGIN)));
            user.setToken(cursor.getString(
                    cursor.getColumnIndexOrThrow(COLUNA_TOKEN)));
            user.setRefreshToken(cursor.getString(
                    cursor.getColumnIndexOrThrow(COLUNA_REFRESH_TOKEN)));
            return user;
        }catch (Exception e)
        {
            throw e;
        }
    }

    public boolean find(User user)
    {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(NOME_TABELA_USERS, new String[]{COLUNA_ID, COLUNA_LOGIN},  COLUNA_ID+" = ? and "+COLUNA_LOGIN+" = ?",new String[]{Integer.toString(user.getId()),user.getLogin()},null,null,null);
        if(cursor.getCount() > 0)
        {
            return true;
        }
        return false;
    }

    public boolean isActiveUser(int id)
    {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(NOME_TABELA_USERS, new String[]{COLUNA_ID},  COLUNA_ID+" = ? and "+COLUNA_ACTIVE_USER+" = 1",new String[]{Integer.toString(id)},null,null,null);
        if(cursor.getCount() > 0)
        {
            return true;
        }
        return false;
    }

    public int authenticate(int user)
    {
        SQLiteDatabase db = getWritableDatabase();
        //reset all users to inactive
        ContentValues resetValues = new ContentValues();
        resetValues.put(COLUNA_ACTIVE_USER, 0);
        db.update(NOME_TABELA_USERS, resetValues, "", null);
        //activate current user
        ContentValues values = new ContentValues();
        values.put(COLUNA_ACTIVE_USER, 1);
        return db.update(NOME_TABELA_USERS, values, COLUNA_ID +" = '"+ user + "'", null);
    }

    public int update(User user)
    {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUNA_LOGIN, user.getLogin());
        values.put(COLUNA_NAME, user.getName());
        values.put(COLUNA_TOKEN, user.getToken());
        values.put(COLUNA_REFRESH_TOKEN, user.getRefreshToken());
        int result = db.update(NOME_TABELA_USERS, values, COLUNA_ID +" = '"+ user.getId() + "'", null);
        return result;
    }

    public int save(User user){
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUNA_LOGIN, user.getLogin());
        values.put(COLUNA_NAME, user.getName());
        values.put(COLUNA_TOKEN, user.getToken());
        values.put(COLUNA_REFRESH_TOKEN, user.getRefreshToken());
        return (int) db.insert(NOME_TABELA_USERS,null,values);
    }

    public int saveOrUpdate(User user, boolean auth)
    {
        if(find(user))
        {
            if(!auth)
            {
                return update(user);
            }else{
                int updateResult = update(user);
                int authResult = authenticate(user.getId());
                return updateResult > 0 && authResult > 0 ? updateResult : 0;
            }
        }
        if(!auth)
        {
            return save(user);
        }else{
            int insertResult = save(user);
            int authResult = authenticate(user.getId());
            return insertResult > 0 && authResult > 0 ? insertResult : 0;
        }
    }


    public User getActiveUser()
    {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(NOME_TABELA_USERS, null,  COLUNA_ACTIVE_USER+" = ?",new String[]{"1"},null,null,null);
        try {
            if(cursor.getCount() > 0)
            {
                cursor.moveToFirst();
                User user = convertCursorOnUsers(cursor);
                cursor.close();
                return user;
            }
        }catch (Exception e)
        {
            Toast.makeText(this.context.getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
        }finally {
            cursor.close();
        }
        return null;
    }

}