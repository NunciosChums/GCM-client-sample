package kr.mint.testgcm;

import java.io.IOException;

import kr.mint.testgcm.util.PreferenceUtil;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

public class MainActivity extends Activity
{
  private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
  private static final String SENDER_ID = "388136674604";
  
  private GoogleCloudMessaging _gcm;
  private String _regId;
  
  private TextView _textStatus;
  
  
  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    
    _textStatus = (TextView) findViewById(R.id.textView1);
    
    // google play service가 사용가능한가
    if (checkPlayServices())
    {
      _gcm = GoogleCloudMessaging.getInstance(this);
      _regId = getRegistrationId();
      
      if (TextUtils.isEmpty(_regId))
        registerInBackground();
    }
    else
    {
      Log.i("MainActivity.java | onCreate", "|No valid Google Play Services APK found.|");
      _textStatus.append("\n No valid Google Play Services APK found.\n");
    }
    
    // display received msg
    String msg = getIntent().getStringExtra("msg");
    if (!TextUtils.isEmpty(msg))
      _textStatus.append("\n" + msg + "\n");
  }
  
  
  @Override
  protected void onNewIntent(Intent intent)
  {
    super.onNewIntent(intent);
    
    // display received msg
    String msg = intent.getStringExtra("msg");
    Log.i("MainActivity.java | onNewIntent", "|" + msg + "|");
    if (!TextUtils.isEmpty(msg))
      _textStatus.append("\n" + msg + "\n");
  }
  
  
  // google play service가 사용가능한가
  private boolean checkPlayServices()
  {
    int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
    if (resultCode != ConnectionResult.SUCCESS)
    {
      if (GooglePlayServicesUtil.isUserRecoverableError(resultCode))
      {
        GooglePlayServicesUtil.getErrorDialog(resultCode, this, PLAY_SERVICES_RESOLUTION_REQUEST).show();
      }
      else
      {
        Log.i("MainActivity.java | checkPlayService", "|This device is not supported.|");
        _textStatus.append("\n This device is not supported.\n");
        finish();
      }
      return false;
    }
    return true;
  }
  
  
  // registration  id를 가져온다.
  private String getRegistrationId()
  {
    String registrationId = PreferenceUtil.instance(getApplicationContext()).regId();
    if (TextUtils.isEmpty(registrationId))
    {
      Log.i("MainActivity.java | getRegistrationId", "|Registration not found.|");
      _textStatus.append("\n Registration not found.\n");
      return "";
    }
    int registeredVersion = PreferenceUtil.instance(getApplicationContext()).appVersion();
    int currentVersion = getAppVersion();
    if (registeredVersion != currentVersion)
    {
      Log.i("MainActivity.java | getRegistrationId", "|App version changed.|");
      _textStatus.append("\n App version changed.\n");
      return "";
    }
    return registrationId;
  }
  
  
  // app version을 가져온다. 뭐에 쓰는건지는 모르겠다.
  private int getAppVersion()
  {
    try
    {
      PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
      return packageInfo.versionCode;
    }
    catch (NameNotFoundException e)
    {
      // should never happen
      throw new RuntimeException("Could not get package name: " + e);
    }
  }
  
  
  // gcm 서버에 접속해서 registration id를 발급받는다.
  private void registerInBackground()
  {
    new AsyncTask<Void, Void, String>()
    {
      @Override
      protected String doInBackground(Void... params)
      {
        String msg = "";
        try
        {
          if (_gcm == null)
          {
            _gcm = GoogleCloudMessaging.getInstance(getApplicationContext());
          }
          _regId = _gcm.register(SENDER_ID);
          msg = "Device registered, registration ID=" + _regId;
          
          // For this demo: we don't need to send it because the device
          // will send upstream messages to a server that echo back the
          // message using the 'from' address in the message.
          
          // Persist the regID - no need to register again.
          storeRegistrationId(_regId);
        }
        catch (IOException ex)
        {
          msg = "Error :" + ex.getMessage();
          // If there is an error, don't just keep trying to register.
          // Require the user to click a button again, or perform
          // exponential back-off.
        }
        
        return msg;
      }
      
      
      @Override
      protected void onPostExecute(String msg)
      {
        Log.i("MainActivity.java | onPostExecute", "|" + msg + "|");
        _textStatus.append(msg);
      }
    }.execute(null, null, null);
  }
  
  
  // registraion id를 preference에 저장한다.
  private void storeRegistrationId(String regId)
  {
    int appVersion = getAppVersion();
    Log.i("MainActivity.java | storeRegistrationId", "|" + "Saving regId on app version " + appVersion + "|");
    PreferenceUtil.instance(getApplicationContext()).putRedId(regId);
    PreferenceUtil.instance(getApplicationContext()).putAppVersion(appVersion);
  }
}
