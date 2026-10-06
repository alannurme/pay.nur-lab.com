package com.qube.piprapay_tool.Utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import android.util.Log;

import com.qube.piprapay_tool.Model.SmsItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class InboxSmsScanner {
    private static final String TAG = "InboxSmsScanner";

    public static List<SmsItem> scanInbox(Context context, long sinceTimestampMillis, int maxLimit) {
        List<SmsItem> result = new ArrayList<>();
        PrefManager pref = PrefManager.getInstance(context);
        Set<String> whitelisted = pref.getWhitelistedSenders();

        ContentResolver cr = context.getContentResolver();
        Uri uri = Uri.parse("content://sms/inbox");

        String selection = null;
        String[] selectionArgs = null;

        if (sinceTimestampMillis > 0) {
            selection = Telephony.Sms.DATE + " >= ?";
            selectionArgs = new String[]{String.valueOf(sinceTimestampMillis)};
        }

        String sortOrder = Telephony.Sms.DATE + " DESC";
        if (maxLimit > 0) {
            sortOrder += " LIMIT " + maxLimit;
        }

        Cursor cursor = null;
        try {
            cursor = cr.query(uri, null, selection, selectionArgs, sortOrder);
            if (cursor != null && cursor.moveToFirst()) {
                int colAddress = cursor.getColumnIndex(Telephony.Sms.ADDRESS);
                int colBody = cursor.getColumnIndex(Telephony.Sms.BODY);
                int colDate = cursor.getColumnIndex(Telephony.Sms.DATE);
                int colId = cursor.getColumnIndex(Telephony.Sms._ID);

                do {
                    String sender = colAddress != -1 ? cursor.getString(colAddress) : "";
                    String body = colBody != -1 ? cursor.getString(colBody) : "";
                    long dateMillis = colDate != -1 ? cursor.getLong(colDate) : System.currentTimeMillis();
                    String id = colId != -1 ? cursor.getString(colId) : String.valueOf(dateMillis);

                    if (pref.isSenderAllowed(sender)) {
                        String timestampSec = String.valueOf(dateMillis / 1000);
                        SmsItem item = new SmsItem(id, sender, body, "1", timestampSec);
                        result.add(item);
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error scanning inbox: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
        }

        return result;
    }
}