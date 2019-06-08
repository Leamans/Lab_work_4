package com.example.lab_work_4;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class ConfigActivity extends Activity {
    private static final String PREFS_NAME = "com.example.lab_work_4";
    private static final String PREF_PREFIX_KEY = "appwidget_";
    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private final Calendar mCalendar = Calendar.getInstance();
    private ConstraintLayout dateTv;
    private TextView textView;
    private String tmpDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.config);
        setTitle("Установка времени");

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_CANCELED,resultValue);

        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID){
            finish();
            return;
        }

        dateTv = findViewById(R.id.date);
        textView = findViewById(R.id.textView);
        tmpDate = loadDatePref(ConfigActivity.this, mAppWidgetId);

        final DatePickerDialog.OnDateSetListener date = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                mCalendar.set(Calendar.YEAR, year);
                mCalendar.set(Calendar.MONTH, monthOfYear);
                mCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                updateLabel();
            }
        };

        dateTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDateDialog(date);
            }
        });

        Button applyBtn = findViewById(R.id.btn);
        applyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setCalValue();

                Calendar today = Calendar.getInstance();
                today.set(Calendar.HOUR_OF_DAY,0);
                today.set(Calendar.MINUTE,0);
                today.set(Calendar.SECOND,0);
                today.set(Calendar.MILLISECOND,0);
                today.add(Calendar.DAY_OF_MONTH,1);

                if (!mCalendar.before(today)) {
                    final Context context = ConfigActivity.this;
                    saveDatePref(context, mAppWidgetId, tmpDate, false, false);
                    AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                    MyWidget.updateAppWidget(context, appWidgetManager, mAppWidgetId);
                    Intent resultValue = new Intent();
                    resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                    setResult(RESULT_OK, resultValue);
                    finish();
                } else {
                    Toast toast = Toast.makeText(getApplicationContext(),
                            "Java может многое, но отправлять уведомление в прошлое... это уже работа для Рика и Морти!", Toast.LENGTH_LONG);
                    toast.show();
                }
            }
        });
    }

    private void showDateDialog(DatePickerDialog.OnDateSetListener date) {
        new DatePickerDialog(ConfigActivity.this, date,
                mCalendar.get(Calendar.YEAR),
                mCalendar.get(Calendar.MONTH),
                mCalendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateLabel() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        tmpDate = sdf.format(mCalendar.getTime());
        textView.setText(tmpDate);
    }

    private void setCalValue() {
        if (!tmpDate.equals("")) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            try {
                mCalendar.setTime(sdf.parse(tmpDate));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }

    static void saveDatePref(Context context, int appWidgetId, String text, boolean done, boolean shown) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putString(PREF_PREFIX_KEY + appWidgetId, text);
        prefs.putBoolean(PREF_PREFIX_KEY + appWidgetId + "b", done);
        prefs.putBoolean(PREF_PREFIX_KEY + appWidgetId + "n", shown);
        prefs.apply();
    }

    static void deleteDatePref(Context context, int appWidgetId) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.remove(PREF_PREFIX_KEY + appWidgetId);
        prefs.remove(PREF_PREFIX_KEY + appWidgetId + "b");
        prefs.remove(PREF_PREFIX_KEY + appWidgetId + "n");
        prefs.apply();
    }

    static String loadDatePref(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        String titleValue = prefs.getString(PREF_PREFIX_KEY + appWidgetId, null);
        if (titleValue != null) {
            return titleValue;
        } else {
            return "";
        }
    }

    static Boolean isDone(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        return prefs.getBoolean(PREF_PREFIX_KEY + appWidgetId + "b", false);
    }

    static void setDone(Context context, int appWidgetId, boolean done) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putBoolean(PREF_PREFIX_KEY + appWidgetId + "b", done);
        prefs.apply();
    }

    static Boolean wasNotificationShown(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        return prefs.getBoolean(PREF_PREFIX_KEY + appWidgetId + "n", false);
    }

    static void setNotificationShown(Context context, int appWidgetId, boolean shown) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putBoolean(PREF_PREFIX_KEY + appWidgetId + "n", shown);
        prefs.apply();
    }
}
