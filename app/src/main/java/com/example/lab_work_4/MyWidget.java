package com.example.lab_work_4;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.util.TypedValue;
import android.widget.RemoteViews;

public class MyWidget extends AppWidgetProvider {
    public static final String ACTION_ALARM ="com.example.lab_work_4.SCHEDULED_ALARM";
    private static final String ACTION_UPDATE ="com.example.lab_work_4.SCHEDULED_UPDATE";
    private static final int DAY = 86400000;
    private static boolean restartFlag = false;
    private static int hourOfNotification = 9;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction()!= null) {
            switch (intent.getAction()) {
                case Intent.ACTION_BOOT_COMPLETED:
                    restartFlag = true;
                case Intent.ACTION_TIMEZONE_CHANGED:
                case Intent.ACTION_TIME_CHANGED:
                case ACTION_UPDATE:
                    AppWidgetManager manager = AppWidgetManager.getInstance(context);
                    int[] ids = manager.getAppWidgetIds(getComponentName(context));
                    onUpdate(context, manager, ids);
                    break;
                case ACTION_ALARM:
                    AppWidgetManager manager1 = AppWidgetManager.getInstance(context);
                    int id = (manager1.getAppWidgetIds(getComponentName(context)))[0];
                    ConfigActivity.setNotificationShown(context,id,true);
                    showNotification(context);
                    break;
            }
        }

        super.onReceive(context,intent);
    }
    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            ConfigActivity.deleteDatePref(context, appWidgetId);
        }
    }
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static NotificationChannel createNotificationChannel(String CHANNEL_ID) {
        NotificationChannel mChannel = new NotificationChannel(
                CHANNEL_ID,
                "Уведомление",
                NotificationManager.IMPORTANCE_DEFAULT);
        mChannel.enableLights(true);
        mChannel.setLightColor(Color.YELLOW);
        mChannel.enableVibration(true);
        mChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
        mChannel.setShowBadge(false);
        return mChannel;
    }

    private static String Days(int count) {
       if (count%10 > 1 && count%10 <= 4 && (count%100 <= 11 || count%100 > 14)) return " дня";
       else if (count%10 == 1 && count%100 != 11) return " день";
       else return " дней";
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        Intent intent = new Intent(context, ConfigActivity.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,PendingIntent.FLAG_UPDATE_CURRENT);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
        views.setOnClickPendingIntent(R.id.content, pendingIntent);

        if (!ConfigActivity.isDone(context,appWidgetId)) {
            Calendar calendar = Calendar.getInstance();
            String widgetDate = ConfigActivity.loadDatePref(context, appWidgetId);

            long timeInMilliseconds = 0l;
            if (!widgetDate.equals("")) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                try {
                    Date mDate = sdf.parse(widgetDate);
                    timeInMilliseconds = mDate.getTime();
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            double diffInDays = (double) (timeInMilliseconds - calendar.getTimeInMillis()) / DAY;

            int daysLeftCeil = (int) Math.ceil(diffInDays);
            views.setTextViewText(R.id.widget, Math.max(0, daysLeftCeil) + Days(daysLeftCeil));

            if (daysLeftCeil == 0) {
                Alarm(context, appWidgetId);
                ConfigActivity.setDone(context, appWidgetId, true);
            } else if (diffInDays > 0) {
                views.setTextViewTextSize(R.id.widget, TypedValue.COMPLEX_UNIT_SP, 30);
                views.setViewPadding(R.id.widget, 0, 0, 0, 0);
                NextUpdate(context, appWidgetId);
            }

        } else {
            views.setTextViewText(R.id.widget, "-");

            boolean alarmShown = ConfigActivity.wasNotificationShown(context, appWidgetId);
            if (restartFlag && !alarmShown) {
                Alarm(context, appWidgetId);
                restartFlag = false;
            }
        }

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private static void NextUpdate(Context context, int appWidgetId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(context, MyWidget.class).setAction(ACTION_UPDATE);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

        long midnightTime = getTimeTillHour(0) + DAY;
        alarmManager.cancel(pendingIntent);
        alarmManager.set(AlarmManager.RTC, midnightTime, pendingIntent);
    }

    private static void Alarm(Context context, int appWidgetId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(context, MyWidget.class).setAction(ACTION_ALARM);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        alarmManager.cancel(pendingIntent);

        long alarmTime = getTimeTillHour(hourOfNotification);

        alarmManager.setExact(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent);
    }

    private static long getTimeTillHour(int hour) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 1);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private static void showNotification(Context context) {
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        String CHANNEL_ID = "alarm_ch_1";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            mNotificationManager.createNotificationChannel( createNotificationChannel(CHANNEL_ID) );
        }
        NotificationCompat.Builder mBuilder =   new NotificationCompat.Builder(context,CHANNEL_ID)
                .setDefaults(Notification.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.icon)
                .setContentTitle("Сегодня день магии Java!")
                .setContentIntent(PendingIntent.getActivity(context, 0, new Intent(), 0))
                .setAutoCancel(true);

        mNotificationManager.notify(1, mBuilder.build());
    }

    public static ComponentName getComponentName(Context context) {
        return new ComponentName(context, MyWidget.class);
    }
}