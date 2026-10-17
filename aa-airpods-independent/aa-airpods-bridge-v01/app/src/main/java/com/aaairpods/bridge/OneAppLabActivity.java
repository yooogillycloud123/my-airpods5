package com.aaairpods.bridge;

import android.app.Activity;
import android.Manifest;
import android.graphics.Color;
import android.os.Build;
import android.content.Intent;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.view.View;
import android.widget.Button;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Side-by-side independent package shell for the SAME integrated source tree.
 * Until one-app controller/focus exclusivity has been proven, this screen
 * deliberately contains NO audio-policy/focus/daemon write button.
 * The Golden controller, original helpers and app data are not accessed.
 *
 * PRODUCT HANDOFF CONTRACT (2026-10-12): the developer must first port all
 * required source roles and deliver a compile/sign-verified functional APK
 * candidate. The user, not the developer, conducts parked-car audible tests.
 * Lack of physical access to a vehicle is NOT a reason to halt APK development.
 * This LAB screen is NOT that functional candidate; do not mislabel it FINAL.
 * Whenever a build or integration issue occurs, report the failing component,
 * its evidence, an original-preserving fix path, and the verified result.
 */
public final class OneAppLabActivity extends Activity {
    private TextView state;
    private TextView daemonHealth;
    private TextView coexistence;
    private TextView routeResult;
    private TextView cycleIndicator;
    private int localAirpodsRequests;
    private int localVehicleRequests;
    private String lastOwnA2dpAddress;
    private volatile boolean ownAirpodsPolicyRequested;
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private Button grantBluetooth;
    private OneApp2LiveOutputObserver independentOutputs;
    private static final int CONNECT_REQUEST=8461;
    private static final int SAVE_EVIDENCE=8462;
    private TextView add(LinearLayout p,String s,int size){
        TextView v=new TextView(this);
        v.setText(s);
        v.setTextColor(Color.WHITE);
        v.setTextSize(size);
        v.setPadding(0,14,0,14);
        p.addView(v);
        return v;
    }
    @Override protected void onCreate(Bundle saved){
        super.onCreate(saved);
        LinearLayout box=new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundColor(Color.rgb(15,21,30));
        int pad=(int)(20*getResources().getDisplayMetrics().density);
        box.setPadding(pad,pad,pad,pad);
        OneApp2CycleJournal.initialize(getApplicationContext());
        add(box,"AA AirPods / 통합 LAB",25);
        OneApp2CycleJournal.record("LIFECYCLE","LAB_ACTIVITY_CREATED");
        add(box,"기존 앱들과 나란히 설치되는 독립 검증 패키지",15);
        add(box,"Package: "+getPackageName(),13);
        android.content.SharedPreferences cycles=getPreferences(MODE_PRIVATE);
        localAirpodsRequests=cycles.getInt("lab_airpods_requests",0);
        localVehicleRequests=cycles.getInt("lab_vehicle_requests",0);
        cycleIndicator=add(box,"독립 통합본 반복전환 요청 횟수: AirPods="
            +localAirpodsRequests+" / 차량="+localVehicleRequests,13);
        add(box,"내부 소스 포함: Bridge · ManualEdge · DisconnectEdge · Golden B-prime (비활성)",14);
        add(box,"LAB 현재 모드: 독립 라우팅 시험 · Golden :47651 실행 중 오디오 변경 차단",15);
        add(box,"개발 책임: 검증된 원본 전 기능 소스 통합·컴파일·서명 APK 후보 생성",13);
        add(box,"실차 시험 책임: 사용자가 통합 후보를 받은 뒤 정차 상태에서 직접 수행",13);
        add(box,"실차 접근 불가를 개발 중단 사유로 삼지 않습니다. 현재 후보는 기능 통합 미완료.",13);
        add(box,"독립 ADB 인증 이후 자체 데몬·다이렉트·차량 복귀 시험 가능. 기존 보조 앱 호출 없음.",14);
        add(box,"Golden 작동환경과 인증·앱 데이터는 건드리지 않습니다.",14);
        // Local NEW-package code, not an external Golden helper dashboard:
        // source/UID/ownership/physical-proof are individually fail-closed.
        TextView parity=add(box,"독립 통합 기능별 연결 상태 확인 중",13);
        parity.setText(OneApp2IntegrationMatrix.summarize(getPackageName()));
        // The independent five-step media-cycle engine is bundled in THIS package.
        // Until genuine independent PCM/Focus events exist, its state is IDLE.
        add(box,new OneApp2IndependentCycleEngine().report(),12);
        state=add(box,"독립 통합본 출력장치 이벤트 대기 중",14);
        // Android live callback belongs to this package only; no original helper.
        independentOutputs=new OneApp2LiveOutputObserver(this);
        grantBluetooth=new Button(this);
        grantBluetooth.setText("독립 앱의 Bluetooth 연결 상태 확인 권한");
        grantBluetooth.setAllCaps(false);
        box.addView(grantBluetooth);
        grantBluetooth.setOnClickListener(v->{
            if(Build.VERSION.SDK_INT>=31 && !independentOutputs.hasPermission())
                requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                    CONNECT_REQUEST);
            else independentOutputs.refresh();
        });
        daemonHealth=add(box,"독립 데몬 :47652 인증 상태: 조회 전",14);
        routeResult=add(box,"독립 출력 엔진은 별도 ADB 인증과 독점 제어권이 필요합니다.",13);
        Button ownDiag=new Button(this);
        ownDiag.setText("원본/통합본 · 미디어/AA 진단 (읽기 전용)");
        box.addView(ownDiag);
        ownDiag.setOnClickListener(v->{
            OneApp2CycleJournal.record("DIAG","USER_REQUEST_LOCAL_PROBE");
            doInBackground(()->{
                String report=OneApp2ReadOnlyProbe.inspect(getApplicationContext());
                OneApp2CycleJournal.record("DIAG",
                    report.contains("GOLDEN_47651_LISTENER=true")
                       ?"ORIGINAL_PORT_LISTENING":"ORIGINAL_PORT_NOT_LISTENING");
                OneApp2CycleJournal.record("DIAG",
                    report.contains("OWN_47652_STATE=READY")
                       ?"OWN_DAEMON_AUTHENTICATED":"OWN_DAEMON_NOT_READY");
                OneApp2CycleJournal.record("DIAG",
                    report.contains("AA_ROUTE_MATCH=true")
                       ?"AA_MEDIA_ROUTE_MATCH":"AA_MEDIA_ROUTE_NOT_CONFIRMED");
                return report;
            });
        });
        Button exportLog=new Button(this);
        exportLog.setText("진단 로그 TXT 저장 · 기존본/통합본 구분");
        box.addView(exportLog);
        exportLog.setOnClickListener(v->{
            OneApp2CycleJournal.record("DIAG","USER_REQUEST_EXPORT");
            Intent save=new Intent(Intent.ACTION_CREATE_DOCUMENT);
            save.addCategory(Intent.CATEGORY_OPENABLE);
            save.setType("text/plain");
            save.putExtra(Intent.EXTRA_TITLE,"AA_OneApp2_LAB_log.txt");
            startActivityForResult(save,SAVE_EVIDENCE);
        });
        Button pair=new Button(this);
        pair.setText("① 이 통합앱의 ADB 자체 페어링");
        box.addView(pair);
        pair.setOnClickListener(v->{
            OneApp2CycleJournal.record("ADB","USER_PAIRING_REQUEST");
            routeResult.setText("설정 > 개발자 옵션 > 무선 디버깅 > 페어링 코드 화면을 연 뒤 알림에서 입력하세요.");
            Intent next=new Intent(this,PairingService.class);
            next.setAction(PairingService.ACTION_WAIT);
            try{startForegroundService(next);}catch(RuntimeException e){
                routeResult.setText("ADB 페어링 서비스 시작 실패: "+e.getClass().getSimpleName());
            }
        });
        Button startOwn=new Button(this);
        startOwn.setText("② 통합본 전용 데몬 시작 :47652 (원본 유지)");
        box.addView(startOwn);
        startOwn.setOnClickListener(v->{
            OneApp2CycleJournal.record("DAEMON","USER_OWN_START_REQUEST");
            doInBackground(
            ()->OneApp2Bootstrap.start(getApplicationContext()));
        });
        Button recover=new Button(this);
        recover.setText("자체 데몬 Self-Heal (누락 시만 복구 · Golden 유지)");
        box.addView(recover);
        recover.setOnClickListener(v->{
            OneApp2CycleJournal.record("RECOVERY","USER_REQUEST_SELFHEAL");
            doInBackground(()->{
                String outcome=OneApp2SelfHeal.restoreOnlyOwn(getApplicationContext());
                OneApp2CycleJournal.record("RECOVERY",
                    outcome.split("\\n",2)[0].replaceAll("[^A-Z0-9_]","_"));
                return outcome;
            });
        });
        Button airpods=new Button(this);
        airpods.setText("③ 독립 AirPods 다이렉트 시험 (Golden 데몬 실행 중 차단)");
        box.addView(airpods);
        airpods.setOnClickListener(v->{
            localAirpodsRequests++;
            getPreferences(MODE_PRIVATE).edit()
                .putInt("lab_airpods_requests",localAirpodsRequests).apply();
            cycleIndicator.setText("독립 통합본 반복전환 요청 횟수: AirPods="
                +localAirpodsRequests+" / 차량="+localVehicleRequests);
            OneApp2CycleJournal.record("ROUTE",
                "AIRPODS_REQUEST_ATTEMPT_"+localAirpodsRequests);
            String selected=lastOwnA2dpAddress;
            doInBackground(()->{
                OneApp2DirectController.Result result=
                    OneApp2DirectController.airpods(getApplicationContext(),selected);
                if(result.ok)ownAirpodsPolicyRequested=true;
                OneApp2CycleJournal.record("ROUTE",result.ok
                    ? "AIRPODS_POLICY_ACCEPTED_NOT_PHYSICAL_PASS"
                    : "AIRPODS_"+result.stage.replaceAll("[^A-Z0-9_]","_"));
                return result.report();
            });
        });
        Button vehicle=new Button(this);
        vehicle.setText("④ 독립 차량 복귀 시험 (AA 재연결 없음)");
        box.addView(vehicle);
        vehicle.setOnClickListener(v->{
            localVehicleRequests++;
            getPreferences(MODE_PRIVATE).edit()
                .putInt("lab_vehicle_requests",localVehicleRequests).apply();
            cycleIndicator.setText("독립 통합본 반복전환 요청 횟수: AirPods="
                +localAirpodsRequests+" / 차량="+localVehicleRequests);
            OneApp2CycleJournal.record("ROUTE",
                "VEHICLE_REQUEST_ATTEMPT_"+localVehicleRequests);
            String selected=lastOwnA2dpAddress;
            doInBackground(()->{
                OneApp2DirectController.Result result=
                    OneApp2DirectController.vehicle(getApplicationContext(),selected);
                OneApp2CycleJournal.record("ROUTE",result.ok
                    ? "VEHICLE_POLICY_ACCEPTED_NOT_PHYSICAL_PASS"
                    : "VEHICLE_"+result.stage.replaceAll("[^A-Z0-9_]","_"));
                if(result.ok){
                    ownAirpodsPolicyRequested=false;
                    runOnUiThread(()->startIndependentFocus(false));
                }
                return result.report();
            });
        });
        // This is a SOURCE/DESIGN assessment only. It does not inspect the
        // old uid=2000 process or claim Golden Focus ownership is measured.
        OneApp2DualDaemonCoexistenceGate.Ownership conservative=
            new OneApp2DualDaemonCoexistenceGate.Ownership(
                true,false,true,false,false);
        OneApp2DualDaemonCoexistenceGate.Decision decision=
            OneApp2DualDaemonCoexistenceGate.assess(
                OneApp2DualDaemonCoexistenceGate.goldenReference(),
                OneApp2DualDaemonCoexistenceGate.proposedOneApp2(),
                conservative);
        coexistence=add(box,
            "두 데몬 공존: 설계상 가능 / 실제 병렬 기동 미검증" +
            "\\nGolden :47651 = 기존 오디오 권한 유지" +
            "\\n통합2 :47652 = 자체 데몬, Golden 데몬 활성 중 쓰기 차단" +
            "\\n설계 판단: "+decision+
            "\\n전용 프로세스·로그·인증이 분리되지만 오디오 제어권 실차 검증은 미완료." +
            "\\n동시 DIRECT·FocusPolicy 쓰기 금지 / 자동 AA 재연결 없음" +
            "\\n제어권 독점·원복·차량 음향 실증: 미완료",13);
        ScrollView scroll=new ScrollView(this);
        scroll.addView(box);
        setContentView(scroll);
    }
    @Override protected void onResume(){
        super.onResume();
        if(state==null)return;
        new Thread(() -> {
            final OneApp2DaemonHealth.Snapshot snapshot=OneApp2DaemonHealth.inspect(this);
            runOnUiThread(() -> {
                if(!isFinishing() && daemonHealth!=null)
                    daemonHealth.setText("독립 데몬 :"+snapshot.port+" 확인: "+snapshot.state
                        +"\n"+snapshot.detail);
            });
        },"oneapp2-readonly-daemon-health").start();
        if(independentOutputs!=null)independentOutputs.start(snapshot->{
            if(isFinishing()||isDestroyed())return;
            state.setText(snapshot.report());
            OneApp2CycleJournal.record("DEVICES",snapshot.airpods.name());
            if(snapshot.verifiedAddress!=null)
                lastOwnA2dpAddress=snapshot.verifiedAddress;
            // Automatic DISCONNECT role is internal to THIS one APK and is
            // eligible only after THIS app's own policy was successfully set.
            if(snapshot.verifiedAirpodsRemoved && ownAirpodsPolicyRequested){
                OneApp2CycleJournal.record("DEVICES","VERIFIED_OWNER_DISCONNECT");
                ownAirpodsPolicyRequested=false;
                final String address=lastOwnA2dpAddress;
                doInBackground(()->{
                    OneApp2DirectController.Result result=
                        OneApp2DirectController.vehicle(getApplicationContext(),address);
                    if(result.ok)runOnUiThread(()->startIndependentFocus(true));
                    return "INDEPENDENT_PHYSICAL_DISCONNECT_RESTORE\n"+result.report();
                });
            }
            if(grantBluetooth!=null)grantBluetooth.setVisibility(
                independentOutputs.hasPermission()?View.GONE:View.VISIBLE);
        });
    }
    private void startIndependentFocus(boolean disconnect){
        // Services belong to this LAB package. Neither package UID nor
        // success is assumed equivalent to the original edge APK identities.
        try{
            Intent i=new Intent(this,disconnect
                ?IntegratedDisconnectEdgeService.class
                :IntegratedManualEdgeService.class);
            startForegroundService(i);
        }catch(RuntimeException e){
            if(routeResult!=null)
                routeResult.append("\nINTERNAL_EDGE_FOCUS_NOT_STARTED="+
                    e.getClass().getSimpleName());
        }
    }
    private interface Job{String run()throws Exception;}
    private void doInBackground(Job job){
        routeResult.setText("ONEAPP2_WORKING_NO_GOLDEN_CHANGES");
        io.execute(()->{
            String text;
            try{text=job.run();}
            catch(Exception e){text="ONEAPP2_REQUEST_FAILED="+e.getClass().getSimpleName();}
            final String finalText=text;
            runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())
                routeResult.setText(finalText);});
        });
    }
    @Override protected void onPause(){
        if(independentOutputs!=null)independentOutputs.stop();
        super.onPause();
    }
    @Override protected void onDestroy(){io.shutdownNow();super.onDestroy();}
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request!=SAVE_EVIDENCE || result!=RESULT_OK || data==null
           || data.getData()==null)return;
        try(OutputStream output=getContentResolver().openOutputStream(data.getData(),"w")){
            if(output==null)throw new java.io.IOException("empty document");
            String header="PACKAGE="+getPackageName()+"\\n"
                +"DAEMON_PORT=47652\\n"
                +"GOLDEN_PACKAGE=com.aaairpods.r25fix\\n"
                +"GOLDEN_DAEMON_PORT=47651\\n"
                +"GOLDEN_APP_DATA_ACCESSED=NO\\n";
            String body=header+OneApp2CycleJournal.report();
            output.write(body.getBytes(StandardCharsets.UTF_8));
            output.flush();
            if(routeResult!=null)routeResult.setText("LAB_DIAGNOSTIC_SAVED=YES");
        }catch(Exception failure){
            if(routeResult!=null)routeResult.setText(
                "LAB_DIAGNOSTIC_SAVE_FAILED="+failure.getClass().getSimpleName());
        }
    }
    @Override public void onRequestPermissionsResult(
            int requestCode,String[] permissions,int[] grants){
        super.onRequestPermissionsResult(requestCode,permissions,grants);
        if(requestCode==CONNECT_REQUEST && independentOutputs!=null)
            independentOutputs.refresh();
    }
}
