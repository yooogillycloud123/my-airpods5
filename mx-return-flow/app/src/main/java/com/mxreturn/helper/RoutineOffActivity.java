package com.mxreturn.helper;
import android.app.Activity; import android.content.Intent; import android.os.Bundle; import android.widget.Toast;
public class RoutineOffActivity extends Activity {
 @Override protected void onCreate(Bundle b){ super.onCreate(b); getSharedPreferences("cfg",MODE_PRIVATE).edit().putBoolean("auto",false).apply(); stopService(new Intent(this,WatchService.class)); Toast.makeText(this,"MX Return 자동 복귀 OFF",Toast.LENGTH_SHORT).show(); finish(); }
}