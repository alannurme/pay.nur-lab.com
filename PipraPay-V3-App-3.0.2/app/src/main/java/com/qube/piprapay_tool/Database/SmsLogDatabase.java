package com.qube.piprapay_tool.Database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.qube.piprapay_tool.Model.SmsLogEntry;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmsLogDatabase extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "piprapay_sms_logs.db";
    private static final int DATABASE_VERSION = 2;

    public static final String TABLE_NAME = "sms_logs";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_SENDER = "sender";
    public static final String COLUMN_MESSAGE = "message";
    public static final String COLUMN_SIM_SLOT = "sim_slot";
    public static final String COLUMN_TIMESTAMP = "timestamp";
    public static final String COLUMN_TIME_MILLIS = "time_millis";
    public static final String COLUMN_STATUS = "status";
    public static final String COLUMN_RESPONSE = "response_message";

    private static SmsLogDatabase instance;

    public static synchronized SmsLogDatabase getInstance(Context context) {
        if (instance == null) {
            instance = new SmsLogDatabase(context.getApplicationContext());
        }
        return instance;
    }

    private SmsLogDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_SENDER + " TEXT, "
                + COLUMN_MESSAGE + " TEXT, "
                + COLUMN_SIM_SLOT + " TEXT, "
                + COLUMN_TIMESTAMP + " TEXT, "
                + COLUMN_TIME_MILLIS + " INTEGER, "
                + COLUMN_STATUS + " TEXT, "
                + COLUMN_RESPONSE + " TEXT)";
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + COLUMN_TIME_MILLIS + " INTEGER DEFAULT 0");
            } catch (Exception e) {
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
                onCreate(db);
            }
        }
    }

    public synchronized long insertLog(SmsLogEntry entry) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SENDER, entry.getSender());
        values.put(COLUMN_MESSAGE, entry.getMessage());
        values.put(COLUMN_SIM_SLOT, entry.getSimSlot());
        values.put(COLUMN_TIMESTAMP, entry.getTimestamp());
        values.put(COLUMN_TIME_MILLIS, System.currentTimeMillis());
        values.put(COLUMN_STATUS, entry.getStatus());
        values.put(COLUMN_RESPONSE, entry.getResponseMessage());

        long id = db.insert(TABLE_NAME, null, values);

        // Keep maximum 500 records
        db.execSQL("DELETE FROM " + TABLE_NAME + " WHERE " + COLUMN_ID + " NOT IN (SELECT " + COLUMN_ID + " FROM " + TABLE_NAME + " ORDER BY " + COLUMN_ID + " DESC LIMIT 500)");

        return id;
    }

    public synchronized List<SmsLogEntry> getAllLogs() {
        List<SmsLogEntry> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME + " ORDER BY " + COLUMN_ID + " DESC LIMIT 200", null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                SmsLogEntry entry = new SmsLogEntry();
                entry.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                entry.setSender(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SENDER)));
                entry.setMessage(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MESSAGE)));
                entry.setSimSlot(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SIM_SLOT)));
                entry.setTimestamp(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP)));
                entry.setStatus(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STATUS)));
                entry.setResponseMessage(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RESPONSE)));
                list.add(entry);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    public static class SummaryStats {
        public int todayCount = 0;
        public double todayAmount = 0.0;
        public int weekCount = 0;
        public double weekAmount = 0.0;
    }

    public synchronized SummaryStats getSummaryStats() {
        SummaryStats stats = new SummaryStats();
        SQLiteDatabase db = this.getReadableDatabase();

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long startOfToday = cal.getTimeInMillis();

        cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
        long startOfWeek = cal.getTimeInMillis();

        Cursor cursor = db.rawQuery("SELECT " + COLUMN_MESSAGE + ", " + COLUMN_TIME_MILLIS + ", " + COLUMN_STATUS + " FROM " + TABLE_NAME, null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String message = cursor.getString(0);
                long timeMillis = cursor.getLong(1);
                String status = cursor.getString(2);

                if ("SENT".equalsIgnoreCase(status) || "QUEUED".equalsIgnoreCase(status)) {
                    double amount = extractAmount(message);

                    if (timeMillis >= startOfToday) {
                        stats.todayCount++;
                        stats.todayAmount += amount;
                    }
                    if (timeMillis >= startOfWeek) {
                        stats.weekCount++;
                        stats.weekAmount += amount;
                    }
                }
            } while (cursor.moveToNext());
            cursor.close();
        }
        return stats;
    }

    private double extractAmount(String msg) {
        if (msg == null) return 0.0;
        Pattern pattern = Pattern.compile("(?i)(?:Tk|Amount|Tk\\.|BDT)[:\\s]+([0-9,]+(?:\\.[0-9]{1,2})?)");
        Matcher matcher = pattern.matcher(msg);
        if (matcher.find()) {
            try {
                String num = matcher.group(1).replace(",", "").trim();
                return Double.parseDouble(num);
            } catch (Exception ignored) {}
        }
        return 0.0;
    }

    public synchronized void clearLogs() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_NAME);
    }
}