#!/usr/bin/env python3
"""v1.0.7 focused MainActivity lifecycle fix, not a Bluetooth engine rewrite.

Incident: 2026-10-15 Galaxy screen recording: device state and battery briefly
render then activity exits. The pristine archived Main() invokes bindService()
in the Composable body. Its ServiceConnection calls AirPodsViewModel.init ->
mutable Compose isReady/UI state changes -> recomposition -> bindService again.
This builds repeated service connections, receivers, control observers, and
concurrent reads. Remove this proved feedback loop only.
"""
from pathlib import Path
import sys
root=Path(sys.argv[1])
base=root/"android/app/src/main/java/me/kavishdevar/librepods"
def repl(rel, before, after, label):
    f=base/rel
    s=f.read_text(encoding="utf-8")
    n=s.count(before)
    if n!=1:raise RuntimeError(f"{label}: anchor count={n}")
    f.write_text(s.replace(before,after,1),encoding="utf-8")
    print("V107_PATCH_PASS",label)
p="MainActivity.kt"
# The foreground service owns its own receivers. The Activity must not destroy
# them while being recreated by Android (e.g. launching recent-apps).
repl(p,'''        sendBroadcast(Intent(AirPodsNotifications.DISCONNECT_RECEIVERS))
        super.onDestroy()''',
    '''        // The Activity is not the owner of foreground-service receivers.
        // Do NOT send DISCONNECT_RECEIVERS on Activity re-creation.
        super.onDestroy()''',"activity_does_not_kill_background_receivers")

repl(p,'''    override fun onStop() {
        try {
            unbindService(serviceConnection)
            Log.d("MainActivity", "Unbound service")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error while unbinding service: $e")
        }
        try {
            unregisterReceiver(connectionStatusReceiver)
            Log.d("MainActivity", "Unregistered receiver")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error while unregistering receiver: $e")
        }
        super.onStop()
    }''','''    // Keep the service bound on onStop. Unbind only on Activity destruction.
    // onStop unbinding leaves the ViewModel pointing at a disconnected binder.
''',"stop_does_not_unbind_until_destroy")

repl(p,'''    val onboardingComplete = sharedPreferences.getBoolean("onboarding_complete", false)''',
     '''    val onboardingComplete = remember { mutableStateOf(sharedPreferences.getBoolean("onboarding_complete", false)) }
    val bindingAttempted = remember { mutableStateOf(false) }
    val viewModelInitialized = remember { mutableStateOf(false) }''',
     "remember_permission_and_binding_state")

repl(p,'''    fun bindService() {
        context.startForegroundService(Intent(context, AirPodsService::class.java))
        serviceConnection = object: ServiceConnection {''',
     '''    fun bindService() {
        if (bindingAttempted.value) return
        // Assign BEFORE notifying service: binding is asynchronous and may
        // otherwise race a Compose recomposition.
        bindingAttempted.value = true
        try {
            context.startForegroundService(Intent(context, AirPodsService::class.java))
        } catch (e: RuntimeException) {
            bindingAttempted.value = false
            Log.e("MainActivity", "Could not start AirPods foreground service", e)
            return
        }
        serviceConnection = object: ServiceConnection {''',
     "single_service_bind_guard")

repl(p,'''                airPodsViewModel.init(
                    service = service,
                    controlRepo = ControlCommandRepository(service.aacpManager),
                    sharedPreferences = context.getSharedPreferences("settings", MODE_PRIVATE),
                    appContext = context.applicationContext
                )''',
     '''                if (!viewModelInitialized.value) {
                    viewModelInitialized.value = true
                    try {
                        airPodsViewModel.init(
                            service = service,
                            controlRepo = ControlCommandRepository(service.aacpManager),
                            sharedPreferences = context.getSharedPreferences("settings", MODE_PRIVATE),
                            appContext = context.applicationContext
                        )
                    } catch (e: Exception) {
                        Log.e("MainActivity", "AirPodsViewModel initialization error", e)
                        // Do not crash launcher on malformed first AACP packet.
                        // Retain service; next Activity open allows a clean retry.
                    }
                }''',"single_viewmodel_init_guard")

repl(p,'''                airPodsService.value = null
            }
        }

        context.bindService(
            Intent(context, AirPodsService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    if (onboardingComplete) {
        bindService()
    }''',
     '''                airPodsService.value = null
                bindingAttempted.value = false
            }
        }

        try {
            val bound = context.bindService(
                Intent(context, AirPodsService::class.java),
                serviceConnection,
                Context.BIND_AUTO_CREATE
            )
            if (!bound) {
                bindingAttempted.value = false
                Log.e("MainActivity", "AirPodsService bind returned false")
            }
        } catch (e: RuntimeException) {
            bindingAttempted.value = false
            Log.e("MainActivity", "Could not bind AirPodsService", e)
        }
    }

    // Binding the service inside the Composable BODY created one extra service
    // binding on every battery/connection state recomposition. Bind only once.
    LaunchedEffect(onboardingComplete.value) {
        if (onboardingComplete.value) bindService()
    }''',"no_composable_recomposition_bind")

repl(p,'''showOnboarding = !onboardingComplete,
        onboardingComplete = {
            sharedPreferences.edit { putBoolean("onboarding_complete", true) }
            bindService()
        },''',
     '''showOnboarding = !onboardingComplete.value,
        onboardingComplete = {
            sharedPreferences.edit { putBoolean("onboarding_complete", true) }
            onboardingComplete.value = true
        },''',"onboarding_triggers_one_bind")

repl("presentation/viewmodel/AirPodsViewModel.kt",
     '''    fun init(service: AirPodsService, controlRepo: ControlCommandRepository, sharedPreferences: SharedPreferences, appContext: Context) {
        this.service = service''',
     '''    fun init(service: AirPodsService, controlRepo: ControlCommandRepository, sharedPreferences: SharedPreferences, appContext: Context) {
        if (isReady) return // do not register BroadcastReceivers and collectors twice
        this.service = service''',"viewmodel_no_duplicate_observers")

repl("presentation/viewmodel/AirPodsViewModel.kt",
     '''ancMode = controlRepo.getValue(ControlCommandIdentifiers.LISTENING_MODE)?.get(0)?.toInt() ?: 1,''',
     '''ancMode = controlRepo.getValue(ControlCommandIdentifiers.LISTENING_MODE)?.getOrNull(0)?.toInt() ?: 1,''',
     "empty_initial_listening_packet_not_fatal")

gradle=root/"android/app/build.gradle.kts"
g=gradle.read_text(encoding="utf-8")
assert g.count('versionCode = 10006')==1
assert g.count('versionName = "1.0.6-pristine-control-ko"')==1
g=g.replace('versionCode = 10006','versionCode = 10007')
g=g.replace('versionName = "1.0.6-pristine-control-ko"','versionName = "1.0.7-lifecycle-stable"')
gradle.write_text(g,encoding="utf-8")

main=(base/p).read_text(encoding="utf-8")
assert 'if (onboardingComplete) {\n        bindService()' not in main
assert 'LaunchedEffect(onboardingComplete.value)' in main
assert 'bindingAttempted.value = true' in main
assert 'sendBroadcast(Intent(AirPodsNotifications.DISCONNECT_RECEIVERS))' not in main
print("V107_SINGLE_BIND_SOURCE_GATE=PASS")
