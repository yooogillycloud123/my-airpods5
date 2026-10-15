package com.mxreturn.helper;
import android.app.Activity; import android.content.Intent; import android.os.Build; import android.os.Bundle; import android.widget.Toast;
public class RoutineOnActivity extends Activity {
 @Override protected void onCreate(Bundle b){ super.onCreate(b); getSharedPreferences("cfg",MODE_PRIVATE).edit().putBoolean("auto",true).apply(); Intent i=new Intent(this,WatchService.class); try{ if(Build.VERSION.SDK_INT>=26) startForegroundService(i); else startService(i); Toast.makeText(this,"MX Return 자동 복귀 ON",Toast.LENGTH_SHORT).show(); }catch(Throwable t){ Toast.makeText(this,"MX Return ON 실패: "+t.getClass().getSimpleName(),Toast.LENGTH_LONG).show(); } finish(); }
}