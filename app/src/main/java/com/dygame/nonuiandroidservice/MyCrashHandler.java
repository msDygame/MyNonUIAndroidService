package com.dygame.nonuiandroidservice;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.util.Log;

import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Administrator on 2015/5/11.
 * UncaughtExceptionHandler可以用來捕獲程序異常，比如NullPointerException空指針異常拋出時，用戶沒有try catch捕獲，那麼，Android系統會彈出對話框的「XXX程序異常退出」，給應用的用戶體驗造成不良影響。
 * 為了捕獲應用運行時異常並給出友好提示，便可繼承UncaughtExceptionHandler類來處理。
 */
public class MyCrashHandler implements Thread.UncaughtExceptionHandler
{
    private static final String TAG = MyCrashHandler.class.getSimpleName();
    private static MyCrashHandler instance; // 單例模式

    private Context mContext; // 程序Context對像
    private Thread.UncaughtExceptionHandler defalutHandler; // 系統默認的UncaughtException處理類
    //用來存儲設備信息和異常信息
    private Map<String, String> infos = new HashMap<String, String>();
    //用於格式化日期,作為日誌文件名的一部分
    private DateFormat formatter = new SimpleDateFormat( "yyyy-MM-dd_HH-mm-ss.SSS");

    /**
     * 保證只有一個CrashHandler實例
     * */
    private MyCrashHandler()
    {

    }
    //
    public static String getTag() { return TAG ; }

    /**
     * 獲取CrashHandler實例
     * @return CrashHandler
     */
    public static MyCrashHandler getInstance()
    {
        if (instance == null)
        {
            synchronized (MyCrashHandler.class)
            {
                if (instance == null)
                {
                    instance = new MyCrashHandler();
                }
            }
        }
        return instance;
    }
    /**
     * 異常處理初始化
     * @param context
     */
    public void init(Context context)
    {
        this.mContext = context;
        // 獲取系統默認的UncaughtException處理器
        defalutHandler = Thread.getDefaultUncaughtExceptionHandler();
        // 設置該CrashHandler為程序的默認處理器
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    /**
     * 當UncaughtException發生時會轉入該函數來處理
     */
    @Override
    public void uncaughtException(Thread thread, Throwable ex)
    {
        // 自定義錯誤處理
        boolean IsHandle = handleException(ex);
        if (!IsHandle && defalutHandler != null)
        {
            // 如果用戶沒有處理則讓系統默認的異常處理器來處理
            defalutHandler.uncaughtException(thread, ex);
        }
        else
        {
            try
            {
                Thread.sleep(3000);
            }
            catch (InterruptedException e)
            {
                Log.e(TAG, "error : ", e);
            }
            // 退出程序
            // 在這裡可以直接殺死應用，安全退出，也可以重起應用，也就是跳轉到app的一個activity。
            // 值得注意的事，由於此時本應用處於出錯狀態，正常的跳轉是沒法完成的。得開新的棧來存放新開起的activity，
            // 所以在跳轉activity是要加上intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);在此時，就相當於重新啟動了一個app。
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        }
    }
    /**
     * 自定義錯誤處理,收集錯誤信息 發送錯誤報告等操作均在此完成.
     * @param ex
     * @return true:如果處理了該異常信息;否則返回false.
     */
    private boolean handleException(final Throwable ex)
    {
        if (ex == null) return false;
        //收集設備參數信息
        collectDeviceInfo(mContext);
        // 通知用戶程序出現異常
        showCrashReportsDialog();
        //顯示異常信息
        new Thread()
        {
            @Override
            public void run()
            {
                Looper.prepare();

                ex.printStackTrace();
                String err = "[" + ex.getMessage() + "]";
                Toast.makeText(mContext, "程式出現異常,即将退出=" + err, Toast.LENGTH_LONG).show();

                Looper.loop();
            }
        }.start();

        // 保存日誌文件
        saveCrashInfoToFile(ex);
        return true;
    }
    /**
     * 彈出異常對話框，通知用戶
     * 失敗...出現error..?
     */
    public void showCrashReportsDialog()
    {
/*
        // 子線程彈出dialog 必須有looper.prepare(); Looper.loop();
        new Thread()
        {
            @Override
            public void run()
            {
                Looper.prepare();
                new AlertDialog.Builder(mContext)
                        .setTitle("Crash")
                        .setCancelable(false)
                        .setMessage("程式出現異常,即将退出...")
                        .setPositiveButton("Retry", new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                /*
                                //重啟
                                Intent intent = new Intent(mContext , MainActivity.class);
                                PendingIntent restartIntent = PendingIntent.getActivity(mContext, 0, intent, Intent.FLAG_ACTIVITY_NEW_TASK);// not work?
                                //退出程序
                                AlarmManager mgr = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
                                mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 1000, restartIntent); // 1秒鐘後重啟應用
                                application.finishActivity();// application?
                                                * /
                                //System.exit(0);
                                //dialog.dismiss();

                            }
                        })
                        .setNeutralButton("Quit", new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                System.exit(0);
                            }
                        })
//                                  .setCanceledOnTouchOutside(false);//?
                        .create().show();

                Looper.loop();
            }
        }.start();
*/
    }
    /**
     * 收集設備參數信息
     * @param
     */
    protected void collectDeviceInfo(Context context)
    {
        try
        {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_ACTIVITIES);
            if (pi != null)
            {
                String versionName = pi.versionName == null ? "null" : pi.versionName;
                String versionCode = pi.versionCode + "";
                infos.put("versionName", versionName);
                infos.put("versionCode", versionCode);
            }
        }
        catch (PackageManager.NameNotFoundException e)
        {
            Log.e(TAG, "an error occured when collect package info", e);
        }
        Field[] fields = Build.class.getDeclaredFields();
        for (Field field : fields)
        {
            try
            {
                field.setAccessible(true);
                infos.put(field.getName(), field.get(null).toString());
                Log.d(TAG, field.getName() + " : " + field.get(null));
            }
            catch (Exception e)
            {
                Log.e(TAG, "an error occured when collect crash info", e);
            }
        }
    }
    /**
     * 保存錯誤信息到文件中
     * @param ex
     * @return  返回文件名稱
     */
    protected String saveCrashInfoToFile(Throwable ex)
    {
        StringBuffer sb = new StringBuffer();
        for (Map.Entry<String, String> entry : infos.entrySet())
        {
            String key = entry.getKey();
            String value = entry.getValue();
            sb.append(key + "=" + value + "\n");
        }

        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        ex.printStackTrace(printWriter);
        Throwable cause = ex.getCause();
        while (cause != null)
        {
            cause.printStackTrace(printWriter);
            cause = cause.getCause();
        }
        printWriter.close();
        String result = writer.toString();
        sb.append(result);
        try
        {
            String time = formatter.format(new Date());
            String fileName = "crash-" + time + "-" + "-log.txt";
            // 判斷是否插入SD卡
            String APK_dir = "";
            String status = Environment.getExternalStorageDirectory().getAbsolutePath();
            if (status.equals(Environment.MEDIA_MOUNTED))
            {
                APK_dir = mContext.getFilesDir().getAbsolutePath() + "/baidu/";// 保存到app的包名路徑下
            }
            else
            {
                APK_dir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/debug/";// 保存到SD卡路徑下
            }
            File destDir = new File(APK_dir);
            if (!destDir.exists())// 判斷文件夾是否存在
            {
                destDir.mkdirs();
            }
            FileOutputStream fos = new FileOutputStream(APK_dir + fileName);
            fos.write(sb.toString().getBytes());
            //發送給開發人員Logcat
//                sendCrashLogCat(APK_dir + fileName);
            fos.close();
            //show CalledMethod
//                CalledMethodBringer(ex) ;
            return fileName;
        }
        catch (Exception e)
        {
            Log.e(TAG, "an error occured while writing file...", e);
        }
        return null;
    }
    /**
     * 將捕獲的錯誤信息輸出到LogCat中。
     */
    private void sendCrashLogCat(String fileName)
    {
        if(!new File(fileName).exists())
        {
            Toast.makeText(mContext, "日誌文件不存在！", Toast.LENGTH_SHORT).show();
            return;
        }
        FileInputStream fis = null;
        BufferedReader reader = null;
        String s = null;
        try
        {
            fis = new FileInputStream(fileName);
            reader = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
            while(true)
            {
                s = reader.readLine();
                if(s == null) break;
                //打出log日誌。
                Log.i(TAG, s.toString());
            }
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {   // 關閉流
            try
            {
                reader.close();
                fis.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }
    /**
     *  CalledMethod Bringer (誰叫的 出來!)
     */
    protected void CalledMethodBringer(Throwable ex)
    {
/*
        //取得呼叫當前方法之上一層函式
        for (StackTraceElement ste : Thread.currentThread().getStackTrace())
        {
            System.out.println(ste);
            Log.w(TAG, ste.toString());//然後會印出 CalledMethodBringer, saveCrashInfoToFile, handleException, uncaughtException, NativeStart.main...真是沒用的資訊..
        }
*/
        //another medth from stackoverflow
//         Thread.currentThread().getStackTrace();// if you don't care what the first element of the stack is. That returns an array of StackTraceElement
//         new Throwable().getStackTrace();//  will have a defined position for your current method, if that matters.

        //以下兩個效果與saveCrashInfoToFile裡的printStackTrace相同,會印出java.lang.NullPointerException, MainActivity$2.onClick, View.performClick , View$PerformClick.run , Handler.handleCallback , Handler.dispatchMessage, Looper.loop , ActivityThread.main , etc...
        //for android
        Log.e(TAG,"stackTrace by Throwable Exception :") ;
        String stackTrace = Log.getStackTraceString(ex);
        Log.e(TAG,stackTrace) ;
        //
        Log.e(TAG,"printStackTrace:") ;
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, true);
        ex.printStackTrace(pw);
        pw.flush();
        sw.flush();
        String sTemp = sw.toString();
        Log.e(TAG,sTemp) ;
    }
}
/**
 * 在Application或者Activity的onCreate方法中加入以下兩句調用即可：
 @Override
 protected void onCreate(Bundle savedInstanceState)
 {
 super.onCreate(savedInstanceState);
 setContentView(R.layout.activity_main);

 MyCrashHandler pCrashHandler = MyCrashHandler.getInstance();
 pCrashHandler.init(getApplicationContext());
 }
 和
 <uses-permission android:name="android.permission.INTERNET"/>
 <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
 <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
 */