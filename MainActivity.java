
package com.example.birthdayreminder;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import android.content.ContentValues;
import android.database.Cursor;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private DatabaseHelper database;
    private ListView listBirthdays;
    private ArrayAdapter<String> adapter;

    private final ArrayList<Birthday> birthdays = new ArrayList<>();
    private final ArrayList<String> displayItems = new ArrayList<>();

    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    private static class Birthday {
        int id;
        String name;
        String date;

        Birthday(int id, String name, String date) {
            this.id = id;
            this.name = name;
            this.date = date;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        database = new DatabaseHelper(this);
        listBirthdays = findViewById(R.id.listBirthdays);
        Button btnAdd = findViewById(R.id.btnAdd);

        dateFormat.setLenient(false);

        adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                displayItems
        );

        listBirthdays.setAdapter(adapter);

        btnAdd.setOnClickListener(v -> showBirthdayDialog(null));

        listBirthdays.setOnItemClickListener((parent, view, position, id) -> {
            showBirthdayDialog(birthdays.get(position));
        });

        listBirthdays.setOnItemLongClickListener(
                (parent, view, position, id) -> {
                    confirmDelete(birthdays.get(position));
                    return true;
                });

        loadBirthdays();
    }

    private void showBirthdayDialog(Birthday existing) {
        boolean editing = existing != null;

        EditText nameInput = new EditText(this);
        nameInput.setHint("Person's name");

        if (editing) {
            nameInput.setText(existing.name);
        }

        TextView dateLabel = new TextView(this);
        dateLabel.setPadding(0, 20, 0, 10);
        dateLabel.setTextSize(16);

        final Calendar selectedDate = Calendar.getInstance();

        if (editing) {
            try {
                Date parsed = dateFormat.parse(existing.date);
                if (parsed != null) {
                    selectedDate.setTime(parsed);
                }
            } catch (ParseException e) {
                Toast.makeText(this, "Invalid saved date",
                        Toast.LENGTH_SHORT).show();
            }
        }

        updateDateLabel(dateLabel, selectedDate);

        Button chooseDate = new Button(this);
        chooseDate.setText("Choose Birthday");

        chooseDate.setOnClickListener(v -> {
            DatePickerDialog picker = new DatePickerDialog(
                    MainActivity.this,
                    (datePicker, year, month, day) -> {
                        selectedDate.set(year, month, day);
                        updateDateLabel(dateLabel, selectedDate);
                    },
                    selectedDate.get(Calendar.YEAR),
                    selectedDate.get(Calendar.MONTH),
                    selectedDate.get(Calendar.DAY_OF_MONTH)
            );
            picker.show();
        });

        android.widget.LinearLayout layout =
                new android.widget.LinearLayout(this);

        layout.setOrientation(
                android.widget.LinearLayout.VERTICAL);
        layout.setPadding(40, 10, 40, 0);
        layout.addView(nameInput);
        layout.addView(dateLabel);
        layout.addView(chooseDate);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(editing ? "Edit Birthday" : "Add Birthday")
                .setView(layout)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", null)
                .create();

        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setOnClickListener(v -> {
                        String name = nameInput.getText()
                                .toString().trim();

                        if (name.isEmpty()) {
                            nameInput.setError("Enter a name");
                            return;
                        }

                        String date = dateFormat.format(
                                selectedDate.getTime());

                        ContentValues values = new ContentValues();
                        values.put("name", name);
                        values.put("birth_date", date);

                        long result;

                        if (editing) {
                            result = database.updateBirthday(
                                    existing.id, name, date);
                        } else {
                            result = database.addBirthday(name, date);
                        }

                        if (result >= 0) {
                            loadBirthdays();
                            dialog.dismiss();
                        } else {
                            Toast.makeText(this, "Save failed",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        dialog.show();
    }

    private void updateDateLabel(
            TextView label, Calendar selectedDate) {
        label.setText("Birthday: " +
                dateFormat.format(selectedDate.getTime()));
    }

    private void loadBirthdays() {
        birthdays.clear();
        displayItems.clear();

        try (Cursor cursor = database.getAllBirthdays()) {
            while (cursor.moveToNext()) {
                birthdays.add(new Birthday(
                        cursor.getInt(0),
                        cursor.getString(1),
                        cursor.getString(2)
                ));
            }
        }

        Collections.sort(birthdays, new Comparator<Birthday>() {
            @Override
            public int compare(Birthday a, Birthday b) {
                return Integer.compare(
                        sortKey(a.date), sortKey(b.date));
            }
        });

        for (Birthday birthday : birthdays) {
            displayItems.add(
                    birthday.name + "\n" +
                            birthday.date + "\n" +
                            daysUntil(birthday.date) + " days remaining"
            );
        }

        adapter.notifyDataSetChanged();
    }

    private int sortKey(String date) {
        Calendar birthday = Calendar.getInstance();

        try {
            Date parsed = dateFormat.parse(date);
            if (parsed != null) {
                birthday.setTime(parsed);
            }
        } catch (ParseException e) {
            return Integer.MAX_VALUE;
        }

        int monthDay =
                (birthday.get(Calendar.MONTH) + 1) * 100
                        + birthday.get(Calendar.DAY_OF_MONTH);

        Calendar today = Calendar.getInstance();
        int todayKey =
                (today.get(Calendar.MONTH) + 1) * 100
                        + today.get(Calendar.DAY_OF_MONTH);

        return monthDay >= todayKey
                ? monthDay : monthDay + 10000;
    }

    private int daysUntil(String date) {
        Calendar today = Calendar.getInstance();
        zeroTime(today);

        Calendar nextBirthday = Calendar.getInstance();

        try {
            Date parsed = dateFormat.parse(date);
            if (parsed == null) return 0;

            nextBirthday.setTime(parsed);
        } catch (ParseException e) {
            return 0;
        }

        nextBirthday.set(Calendar.YEAR,
                today.get(Calendar.YEAR));
        zeroTime(nextBirthday);

        if (nextBirthday.before(today)) {
            nextBirthday.add(Calendar.YEAR, 1);
        }

        long difference =
                nextBirthday.getTimeInMillis()
                        - today.getTimeInMillis();

        return (int) (difference /
                (24L * 60 * 60 * 1000));
    }

    private void zeroTime(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    private void confirmDelete(Birthday birthday) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Birthday")
                .setMessage("Delete " + birthday.name + "?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    database.deleteBirthday(birthday.id);
                    loadBirthdays();
                    Toast.makeText(this, "Birthday deleted",
                            Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    @Override
    protected void onDestroy() {
        database.close();
        super.onDestroy();
    }
}
