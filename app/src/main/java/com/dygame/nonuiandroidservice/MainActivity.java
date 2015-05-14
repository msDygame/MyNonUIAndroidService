package com.dygame.nonuiandroidservice;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.ads.*;
import com.google.android.gms.ads.*;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;

/**
 隱式Intent傳遞
 Remote?AIDL?
 */
public class MainActivity extends ActionBarActivity
{
    protected MyService mBoundService ;
    protected boolean mIsBound = false ;
    protected AdView adView;//橫幅廣告
    protected InterstitialAd adInterstitial;//插頁廣告
    MyReceiver pReceiver ;
    protected static String TAG = "" ;
    //
    protected ServiceConnection mConnection = new ServiceConnection()
    {
        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            mBoundService = null;
            // Tell the user about this.
            Toast.makeText(MainActivity.this, "Service Disconnected", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            mBoundService = ((MyService.LocalBinder) service).getService() ;
            // Tell the user about this.
            Toast.makeText(MainActivity.this, "Service Connected", Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //UncaughtException處理類,當程序發生Uncaught異常的時候,有該類來接管程序,並記錄發送錯誤報告.
        MyCrashHandler pCrashHandler = MyCrashHandler.getInstance();
        pCrashHandler.init(getApplicationContext());
        TAG = MyCrashHandler.getTag() ;
        //在註冊廣播接收:
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.dygame.nonuiandroidservice.broadcast");//為BroadcastReceiver指定action，使之用於接收同action的廣播
        pReceiver = new MyReceiver();
        registerReceiver(pReceiver ,intentFilter);
        // 建立橫幅廣告 AdView。
        adView = new AdView(this);
        adView.setAdUnitId(this.getString(R.string.string_my_ad_unit_id));
        adView.setAdSize(AdSize.BANNER);
        // 假設 LinearLayout 已獲得 android:id="@+id/mainLayout" 屬性，
        // 查詢 LinearLayout。
        LinearLayout layout = (LinearLayout)findViewById(R.id.mainLayout);
        // 在其中加入 adView。
        layout.addView(adView);
        // 啟動一般請求。
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .build();
        // 以廣告請求載入 adView。
        adView.loadAd(adRequest);
        // 建立插頁式廣告 InterstitialAd
        adInterstitial = new InterstitialAd(this);
        adInterstitial.setAdUnitId(this.getString(R.string.string_my_in_unit_id));
        // 建立插頁廣告請求。
        AdRequest adRequestII = new AdRequest.Builder().build();
        // 開始載入插頁式廣告。
        adInterstitial.loadAd(adRequestII);
        //
        doBindService() ;
        //背景模式
        HideActivity() ;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings)
        {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    void doBindService()
    {
        // Establish a connection with the service.  We use an explicit
        // class name because there is no reason to be able to let other
        // applications replace our component.
        bindService(new Intent(MainActivity.this, MyService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
        // Tell the user about this.
        Toast.makeText(MainActivity.this, "Bind Service", Toast.LENGTH_SHORT).show();
    }

    void doUnbindService()
    {
        if (mIsBound)
        {
            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
            // Tell the user about this.
            Toast.makeText(MainActivity.this, "Unbinding Service", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPause()
    {
        adView.pause();
        super.onPause();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        adView.resume();
    }

    @Override
    public void onDestroy()
    {
        adView.destroy();
        doUnbindService();
        //註銷
        unregisterReceiver(pReceiver);
    }

    /**
     *  應用隱藏到後台，類似於按下Home鍵的效果
     */
    protected void HideActivity()
    {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addCategory(Intent.CATEGORY_HOME);
        startActivity(intent);
    }

    /**
     * 發放廣播
     */
    protected void SendBroadcastIntent()
    {
        //create a intent with an action
        String sActionString = "com.dygame.nonuiandroidservice.broadcast" ;
        Intent broadcastIntent = new Intent(sActionString) ;
        broadcastIntent.putExtra(TAG , "Burning Love! Poi!") ;
        sendBroadcast(broadcastIntent);
    }

    /**
     *  接收廣播
     */
    public class MyReceiver extends BroadcastReceiver
    {
        protected boolean IsCommonTag = false ;//it is a Tag , Log it and debug
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String sAction = intent.getAction();
            if ((sAction.equals("android.intent.action.BOOT_COMPLETED")) || (sAction.equals("Hello poi")))
            {
                  Log.i(TAG,"You've got mail") ;
            }
            // analyze broadcast by packagename
            if (sAction.equals("com.dygame.myandroidservice.broadcast")) { IsCommonTag = true ; }
            if (sAction.equals("com.dygame.nonuiandroidservice.broadcast")) { IsCommonTag = true ; }
            //
            if (IsCommonTag == true)
            {
                Bundle bundle = intent.getExtras();
                if(bundle != null)
                {
                    String sMessage = bundle.getString(TAG);
                    Log.i(TAG, "broadcast receiver action:" + sAction + "=" + sMessage);
                }
            }
        }
    }

    /**
     * 發放插頁式廣告
     */
    public void displayInterstitial()
    {
        //這個方法會在檢查 isLoaded() 並確認插頁式廣告載入完成後，呼叫 show() 來顯示插頁式廣告。
        if (adInterstitial.isLoaded())
        {
            // 當準備好顯示插頁式廣告時 (像是程式啟動時、影片播放前或載入遊戲關卡時)，叫用 displayInterstitial()。
            adInterstitial.show();
        }
    }

    /**
     *  判斷手機是否連上網路 (ConnectivityManager)
     *  需要權限 <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"></uses-permission>
     */
    protected void CheckConnectActivity()
    {
        ConnectivityManager CM = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = CM.getActiveNetworkInfo();
        info.getTypeName();             // 目前以何種方式連線 [WIFI]
        info.getState();                // 目前連線狀態 [CONNECTED]
        info.isAvailable();             // 目前網路是否可使用 [true]
        info.isConnected();             // 網路是否已連接 [true]
        info.isConnectedOrConnecting(); // 網路是否已連接 或 連線中 [true]
        info.isFailover();              // 網路目前是否有問題 [false]
        info.isRoaming();               // 網路目前是否在漫遊中 [false]
    }

    /**
     *  用法:?
     *  iLaunchRtn = launchGameZone("com.dygame.gamezone2","com.dygame.waverider");
     */
    private int LaunchGameZone(Context c)
    {
/*
        int iRtn = 0;
        if (checkMeRunning())
        {
            LogManager.ErrorLog(getClass(), this.GAME_ZONE_PACKAGE_NAME + "is running, ingore");
            return 0;
        }
        Log.i("DYService", "restart GameZone, sPackageName = " + this.GAME_ZONE_PACKAGE_NAME);

        PackageManager pkgMgt = c.getPackageManager();

        boolean bFind = false;
        Intent it = new Intent("android.intent.action.MAIN");
        it.addCategory("android.intent.category.LAUNCHER");
        List<ResolveInfo> ra = pkgMgt.queryIntentActivities(it, 0);
        String sCurPackageName = "";
        bFind = false;
        String sClassName = "com.dygame.gamezone2.Logo";
        for (int j = 0; j < ra.size(); j++)
        {
            ActivityInfo ai = ((ResolveInfo)ra.get(j)).activityInfo;
            sCurPackageName = ai.applicationInfo.packageName;
            if (this.GAME_ZONE_PACKAGE_NAME.equalsIgnoreCase(sCurPackageName))
            {
                sClassName = ai.name;
                bFind = true;
                break;
            }
        }
        if (!bFind) {
            Log.e("DYService", "not find PackageName:" + this.GAME_ZONE_PACKAGE_NAME);
        }
        Log.i("DYService", "Try launch GameZone, sPackageName = " + this.GAME_ZONE_PACKAGE_NAME + " , Class Name = " + sClassName);
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addFlags(268435456);
        intent.setComponent(new ComponentName(this.GAME_ZONE_PACKAGE_NAME, sClassName));
        c.startActivity(intent);
        return iRtn;
*/   return 0 ;
     }

    /*
    /**
* 獲取當前的手機號
* /
    public String getLocalNumber()
    {
        TelephonyManager tManager = (TelephonyManager) this.getSystemService(TELEPHONY_SERVICE);
        String number = tManager.getLine1Number();
        //獲取手機的IMSI碼/手機當前運營商
        System.out.println("-----"+telManager.getSubscriberId()); //2.-----460007423945575
        System.out.println("-----"+telManager.getSimSerialNumber()); //1.-----89860089281174245575
        System.out.println("-----"+telManager.getSimOperator());
        System.out.println("-----"+telManager.getSimCountryIso());
        System.out.println("-----"+telManager.getSimOperatorName());
        System.out.println("-----"+telManager.getSimState());
        //SubscriberId == 46001 // 中國聯通
        //SubscriberId == 46003 // 中國電信
        //
        return number;
    }
    /**
     *檢查是否有網絡連接
     * /
    public boolean checkInternet()
    {
        ConnectivityManager cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        if (info != null && info.isConnected())
        {
            // 能連接Internet
            return true;
        }
        else
        {
            // 不能連接到
            return false;
        }
    }
    /**
     * 判斷當前網絡連接狀態
     * /
    public static boolean isNetworkConnected(Context context)
    {
        NetworkInfo networkInfo = ((ConnectivityManager) context.getApplicationContext().getSystemService("connectivity")).getActiveNetworkInfo();
        if (networkInfo != null)
        {
            return networkInfo.isConnectedOrConnecting();
        }
        return false;
    }
    /**
     *get請求網絡數據
     * /
    public static String GetDate(String url)
    {
        HttpGet get = new HttpGet(url);
        HttpClient client = new DefaultHttpClient();
        try
        {
            HttpResponse response = client.execute(get);//
            return EntityUtils.toString(response.getEntity());
        }
        catch (Exception e)
        {
            return null;
        }
    }
    /**
     *獲取apk包的簽名信息
     * /
    private String getSign(Context context)
    {
        PackageManager pm = context.getPackageManager();
        List<PackageInfo> apps = pm.getInstalledPackages(PackageManager.GET_SIGNATURES);
        Iterator<PackageInfo> iter = apps.iterator();
        while(iter.hasNext())
        {
            PackageInfo packageinfo = iter.next();
            String packageName = packageinfo.packageName;

            return packageinfo.signatures[0].toCharsString();
            if (packageName.equals(instance.getPackageName()))
            {
                MediaApplication.logD(DownloadApk.class, packageinfo.signatures[0].toCharsString());
                return packageinfo.signatures[0].toCharsString();
            }
        }
        return null;
    }
    /**
     *調用系統瀏覽器
     * /
    Intent intent= new Intent();
    intent.setAction("android.intent.action.VIEW");
    Uri content_url = Uri.parse(exitUrl);
    intent.setData(content_url);
    startActivity(intent);
    /**
     *取得短信電話號碼  內容
     * /
    String number = actv_enter_number.getText().toString();
    String body = et_enter_msg_content.getText().toString();

    SmsManager smsManager = SmsManager.getDefault();
    ArrayList<String> parts = smsManager.divideMessage(body);

    for(String part : parts)
    {
        smsManager.sendTextMessage(number, null, part, null, null);

        Uri url = Sms.Sent.CONTENT_URI;
        ContentValues values = new ContentValues();
        values.put("address", number);
        values.put("body", part);
        getContentResolver().insert(url, values);
    }
/**
 * 手機橫屏
 * /
    <activity ...
    android:screenOrientation="landscape"  <!-- 橫屏 -->
/**
 * onkey測試
 * /
    c:\adb shell shell@android:/ $ monkey -p com.ooxxzzuu.yourapp -v 2000
    /**
     * 防止手機休眠，保持手機背光常亮
     * /
    getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
/**
 * 得到當前系統sdk版本/軟件當前的版本號
 */
/**
 private int getSdkVersion()
 {
 return android.os.Build.VERSION.SDK_INT;
 }
 //
 PackageManager pm = getPackageManager();
 PackageInfo pkinfo=pm.getPackageInfo(getPackageName(),0);
 return pkinfo.versionName;
 /**
 *判斷SD卡是否可用
 * /
    if (Environment.getExternalStorageState().equals( Environment.MEDIA_MOUNTED))
    /**
     *獲取SD/ROM可用空間
     * /
    private String getSize()
    {
        //得到ROM剩餘空間
        File path = Environment.getDataDirectory();
        //得到SD卡剩餘空間
        // Environment.getExternalStorageDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long availableBlocks = stat.getAvailableBlocks();
        long availsize = availableBlocks * blockSize;
        String size = Formatter.formatFileSize(this, availsize);
        return size;
    }
    /**
     * 獲取手機的剩餘可用ROM
     * /
    public static long getAvailableMem(Context context)
    {
        ActivityManager  am  = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        MemoryInfo outInfo = new MemoryInfo();
        am.getMemoryInfo(outInfo);
        long availMem = outInfo.availMem;
        return availMem;
    }
    /**
     *從網絡上下載文件
     * /
    URL url = new URL(path);
    HttpURLConnection conn = (HttpURLConnection)url.openConnection();
    conn.setRequestMethod("GET"); //
    conn.setConnectTimeout(5000); //超時時間
    int code = conn.getResponseCode(); // 狀態碼
    if (code == 200)
    {
        InputStream is = conn.getInputStream();
    }
    /**
     * 安裝下載完成的APK
     * /
    private void installAPK(File savedFile)
    {
        //調用系統的安裝方法
        Intent intent=new Intent();
        intent.setAction(intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(savedFile), "application/vnd.android.package-archive");
        startActivity(intent);
        finish();
    }
    /**
     * Sharedpreference 偏好設置
     * /
    private SharedPreferences sp;
    sp = getSharedPreferences("config", MODE_PRIVATE);
    // 向偏好設置內添加數據
    Editor editor = sp.edit();
    editor.putBoolean("update", false);
    editor.commit();
    // 從偏好設置內提取數據
    sp.getBoolean ("update", false);
    /**
     *內容提供者 獲取聯繫人姓名和電話 返回 infos對像
     * /
    public static List<ContactInfo> getContactInfo(Context context)
    {
        List<ContactInfo> infos = new ArrayList<ContactInfo>();
        Uri uri = Uri.parse("content://com.android.contacts/raw_contacts");
        Uri datauri = Uri.parse("content://com.android.contacts/data");
        Cursor cursor = context.getContentResolver().query(uri, null, null,null, null);
        while (cursor.moveToNext())
        {
            String id = cursor.getString((cursor.getColumnIndex("contact_id")));
            // 根據上面的ID查詢聯繫人的 信息
            ContactInfo info = new ContactInfo();
            Cursor datacursor = context.getContentResolver().query(datauri,null, "raw_contact_id=?", new String[] { id }, null);
            while (datacursor.moveToNext())
            {
                String data1 = datacursor.getString(cursor.getColumnIndex("data1"));
                String mimetype = datacursor.getString(cursor.getColumnIndex("mimetype"));
                if ("vnd.android.cursor.item/phone_v2".equals(mimetype))
                {
                    info.setNumber(data1);
                }
                if ("vnd.android.cursor.item/name".equals(mimetype))
                {
                    info.setName(data1);
                }
            }
            datacursor.close();
            infos.add(info);
        }
        cursor.close();
        return infos;
    }
/**
 *異步任務
 * /
    new AsyncTask<Void, Void, Void>()
    {
        @Override
        protected Void doInBackground(Void... params)
        {
            // 異步任務運行時執行
            return null;
        }
        @Override
        protected void onPreExecute()
        {
            //異步任務運行前執行 可以放置進度條 progressbar等
            super.onPreExecute();
        }
        @Override
        protected void onPostExecute(Void result)
        {
            //異步任務運行結束後執行
            super.onPostExecute(result);
        }
    }.execute();
    /**
     *進度條
     * /
    ProgressDialog pd;
    pd = new ProgressDialog(this);
    pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
    pd.setTitle("標題");
    pd.setMessage("進度條正文");
    pd.show();
    /**
     *生成6位隨機數
     * /
    int numcode = (int) ((Math.random() * 9 + 1) * 100000);
    String smstext = "你本次生成的6位安全驗證碼為：" + numcode;
    /**
     *獲取最大內存的方法
     * /
    ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
    am.getMemoryClass();
    */
}
