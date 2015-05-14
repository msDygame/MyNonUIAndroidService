package com.dygame.nonuiandroidservice;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by Administrator on 2015/4/17.
 * Todo : 做一個有StartService + BindServie 的 Service
 * 1.呼叫Context的bindService()會連結Service，若Service未開啟就會自動開啟。
 * 2.開啟Service後會呼叫onCreate()但是不會呼叫onStartCommand()
 * 3.呼叫onBind()並傳回IBinder物件
 * 4.呼叫ServiceConnection類別中的onServiceConnected()方法，並將步驟3的IBinder物件當作參數傳遞給onServiceConnected()，透過IBinder可以取得跟Service的聯繫。例如直接呼叫該Service內的公用方法。
 * 5.呼叫Context的unbindService()方法，則系統會呼叫該Service的onUnbind()以解除Context與Service的聯繫；隨後會自動呼叫Service的onDestroy()銷毀服務。
 * 6.實作必須注意的是：
 *    context.bindService()運用來綁定服務提供使用者允許Activity與Sevice互動；發送請求；獲得結果，甚至多行程互相執行這些操作。服務和另一個與之綁定的組件執行時間一樣長；若有多個組件也只能和同一個服務綁定一次。
 *    但所有組件取消綁定之後，服務就會自動銷毀。(這點跟context.startService()很不一樣！)
 *    但其實服務可以同時用以上兩種方式工作：startService()、bindService()，所以可以啟動之後無限期執行而且允許綁定Activity。這只取決你有沒有實現這兩種服務的回調介面: 「onBind()」。
 */
public class MyService extends Service
{
    private NotificationManager mNM;
    // Unique Identification Number for the Notification.
    // We use it on Notification start, and to cancel it.
    private int NOTIFICATION = 1234 ;//?
    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder
    {
        MyService getService()
        {
            return MyService.this;
        }
    }
    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();
    //
    @Override
    public IBinder onBind(Intent intent)
    {
        return mBinder;
    }
    //
    @Override
    public void onCreate()
    {
        super.onCreate();
        //follow android developer sample
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        // Display a notification about us starting.  We put an icon in the status bar.
        // Show a notification while this service is running.
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = "Message Incoming" ;
        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.mipmap.ic_launcher, text, System.currentTimeMillis());
        // The PendingIntent to launch our activity if the user selects this notification
        Intent notificationIntent = new Intent(this, MainActivity.class);//new Intent(this, LocalServiceActivities.Controller.class)
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent , 0);
        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(this, "title", "version1.0.0.1" , contentIntent);
        //前景Service ,
        startForeground(0, notification);
        // Send the notification.
        mNM.notify(NOTIFICATION, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.i("LocalService", "Received start id " + startId + ": " + intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    @Override
    public void onDestroy()
    {
        // Cancel the persistent notification.
        mNM.cancel(NOTIFICATION);
        // Tell the user we stopped.
        Toast.makeText(this, R.string.string_stop_service, Toast.LENGTH_SHORT).show();
    }
}
