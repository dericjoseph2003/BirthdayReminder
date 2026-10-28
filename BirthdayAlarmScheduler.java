package com.example.birthdayreminder;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class BirthdayAlarmScheduler {

    private static final int NOTIFICATION_HOUR = 9;

    public static void scheduleBirthday(
            Context context, int id, String name, String birthDate) {

        // Remove any previous alarm for this birthday.
        cancel(context, id);

        Calendar birthday = getNextBirthday(birthDate);

        if (birthday == null) {
            return;
        }

        birthday.set(Calendar.HOUR_OF_DAY, NOTIFICATION_HOUR);
        birthday.set(Calendar.MINUTE, 0);
        birthday.set(Calendar.SECOND, 0);
        birthday.set(Calendar.MILLISECOND, 0);

        // If today's notification time has passed, schedule next year.
        if (birthday.getTimeInMillis() <= System.currentTimeMillis()) {
            birthday.add(Calendar.YEAR, 1);
        }

        Intent intent = new Intent(context, BirthdayAlarmReceiver.class);
        intent.putExtra("birthday_id", id);
        intent.putExtra("birthday_name", name);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        birthday.getTimeInMillis(),
                        pendingIntent
                );
            } else {
                alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        birthday.getTimeInMillis(),
                        pendingIntent
                );
            }
        }
    }

    public static void cancel(Context context, int id) {
        Intent intent = new Intent(context, BirthdayAlarmReceiver.class);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }

        pendingIntent.cancel();
    }

    private static Calendar getNextBirthday(String birthDate) {
        SimpleDateFormat format =
                new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        format.setLenient(false);

        try {
            Date date = format.parse(birthDate);

            if (date == null) {
                return null;
            }

            Calendar original = Calendar.getInstance();
            original.setTime(date);

            Calendar today = Calendar.getInstance();

            Calendar nextBirthday = Calendar.getInstance();
            nextBirthday.set(
                    today.get(Calendar.YEAR),
                    original.get(Calendar.MONTH),
                    original.get(Calendar.DAY_OF_MONTH),
                    0, 0, 0
            );
            nextBirthday.set(Calendar.MILLISECOND, 0);

            if (nextBirthday.before(today)) {
                nextBirthday.add(Calendar.YEAR, 1);
            }

            return nextBirthday;

        } catch (ParseException | IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void scheduleAll(Context context) {
        DatabaseHelper db = new DatabaseHelper(context);
        android.database.Cursor cursor = db.getAllBirthdays();

        try {
            while (cursor.moveToNext()) {
                int id = cursor.getInt(
                        cursor.getColumnIndexOrThrow("id"));
                String name = cursor.getString(
                        cursor.getColumnIndexOrThrow("name"));
                String date = cursor.getString(
                        cursor.getColumnIndexOrThrow("birth_date"));

                scheduleBirthday(context, id, name, date);
            }
        } finally {
            cursor.close();
            db.close();
        }
    }
}