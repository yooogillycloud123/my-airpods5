package com.mxreturn.helper;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

abstract class SlotSwitchActivity extends Activity {
    protected abstract int logicalSlotIndex();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent command = new Intent(this, SlotCommandReceiver.class);
        command.setAction(SlotCommandReceiver.ACTION_SWITCH_SLOT);
        command.putExtra(SlotCommandReceiver.EXTRA_LOGICAL_SLOT, logicalSlotIndex());
        sendBroadcast(command);

        // OHO+ "Home screen shortcuts" launches an Activity.
        // Finish before a window is ever shown so the currently visible app is not replaced.
        finish();
        overridePendingTransition(0, 0);
    }
}
