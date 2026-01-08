package com.example.idacardocr.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.idacardocr.model.RecognitionHistory;

import java.util.ArrayList;
import java.util.List;

public class RecognitionHistoryDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "ocr_history.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_HISTORY = "recognition_history";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_TYPE = "type"; // ID_CARD 或 BANK_CARD
    private static final String COLUMN_CARD_SIDE = "card_side"; // FRONT, BACK, DOUBLE
    private static final String COLUMN_RESULT_JSON = "result_json";
    private static final String COLUMN_SUMMARY = "summary"; // 简要信息，如姓名或卡号后四位
    private static final String COLUMN_TIMESTAMP = "timestamp";

    private static RecognitionHistoryDbHelper instance;

    public static synchronized RecognitionHistoryDbHelper getInstance(Context context) {
        if (instance == null) {
            instance = new RecognitionHistoryDbHelper(context.getApplicationContext());
        }
        return instance;
    }

    private RecognitionHistoryDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_HISTORY + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_TYPE + " TEXT NOT NULL, " +
                COLUMN_CARD_SIDE + " TEXT, " +
                COLUMN_RESULT_JSON + " TEXT NOT NULL, " +
                COLUMN_SUMMARY + " TEXT, " +
                COLUMN_TIMESTAMP + " INTEGER NOT NULL)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HISTORY);
        onCreate(db);
    }


    public long insertHistory(String type, String cardSide, String resultJson, String summary) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TYPE, type);
        values.put(COLUMN_CARD_SIDE, cardSide);
        values.put(COLUMN_RESULT_JSON, resultJson);
        values.put(COLUMN_SUMMARY, summary);
        values.put(COLUMN_TIMESTAMP, System.currentTimeMillis());
        return db.insert(TABLE_HISTORY, null, values);
    }

    public List<RecognitionHistory> getHistoryByType(String type, int limit) {
        List<RecognitionHistory> historyList = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        
        String selection = COLUMN_TYPE + " = ?";
        String[] selectionArgs = {type};
        String orderBy = COLUMN_TIMESTAMP + " DESC";
        String limitStr = String.valueOf(limit);

        Cursor cursor = db.query(TABLE_HISTORY, null, selection, selectionArgs, 
                null, null, orderBy, limitStr);

        while (cursor.moveToNext()) {
            RecognitionHistory history = new RecognitionHistory();
            history.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)));
            history.setType(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TYPE)));
            history.setCardSide(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CARD_SIDE)));
            history.setResultJson(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RESULT_JSON)));
            history.setSummary(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SUMMARY)));
            history.setTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP)));
            historyList.add(history);
        }
        cursor.close();
        return historyList;
    }

    public RecognitionHistory getHistoryById(long id) {
        SQLiteDatabase db = getReadableDatabase();
        String selection = COLUMN_ID + " = ?";
        String[] selectionArgs = {String.valueOf(id)};

        Cursor cursor = db.query(TABLE_HISTORY, null, selection, selectionArgs, 
                null, null, null);

        RecognitionHistory history = null;
        if (cursor.moveToFirst()) {
            history = new RecognitionHistory();
            history.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)));
            history.setType(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TYPE)));
            history.setCardSide(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CARD_SIDE)));
            history.setResultJson(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RESULT_JSON)));
            history.setSummary(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SUMMARY)));
            history.setTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP)));
        }
        cursor.close();
        return history;
    }

    public void deleteHistory(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_HISTORY, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public void clearHistoryByType(String type) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_HISTORY, COLUMN_TYPE + " = ?", new String[]{type});
    }
}
