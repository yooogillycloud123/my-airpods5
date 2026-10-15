package com.mxreturn.helper;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * ChatGPT Android physical-keyboard compatibility layer.
 * Scope is deliberately limited to com.openai.chatgpt.
 *
 * PC-style mapping inside ChatGPT only:
 *   Enter         -> send
 *   Shift + Enter -> newline
 *
 * Other apps keep their own native Enter behavior.
 */
public class ChatGPTKeyService extends AccessibilityService {
    private static final String CHATGPT_PACKAGE = "com.openai.chatgpt";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean chatGptActive = false;
    private boolean consumeShiftEnterUp = false;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        CharSequence pkg = event == null ? null : event.getPackageName();
        if (pkg != null) chatGptActive = CHATGPT_PACKAGE.contentEquals(pkg);
    }

    @Override
    public void onInterrupt() { }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        boolean enabled = getSharedPreferences("cfg", MODE_PRIVATE)
                .getBoolean("chatgpt_pc_enter", true);

        if (!enabled || !chatGptActive || event.getKeyCode() != KeyEvent.KEYCODE_ENTER) {
            return false;
        }
        if (event.isCtrlPressed() || event.isAltPressed() || event.isMetaPressed()) {
            return false;
        }

        if (event.isShiftPressed()) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                boolean inserted = insertNewlineAtCursor();
                consumeShiftEnterUp = inserted;
                return inserted;
            }
            if (event.getAction() == KeyEvent.ACTION_UP) {
                boolean consume = consumeShiftEnterUp;
                consumeShiftEnterUp = false;
                return consume;
            }
            return consumeShiftEnterUp;
        }

        // Plain Enter: consume the hardware key and press ChatGPT's Send button.
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            handler.postDelayed(() -> {
                if (!clickSendButton()) {
                    Toast.makeText(this,
                            "MX Return: ChatGPT 전송 버튼을 찾지 못했습니다.",
                            Toast.LENGTH_SHORT).show();
                }
            }, 25);
        }
        return true;
    }

    private boolean insertNewlineAtCursor() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return false;
        AccessibilityNodeInfo input = null;
        try {
            input = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
            if (input == null || !input.isEditable()) return false;

            CharSequence cs = input.getText();
            String text = cs == null ? "" : cs.toString();
            int start = input.getTextSelectionStart();
            int end = input.getTextSelectionEnd();
            if (start < 0 || end < 0) start = end = text.length();
            start = Math.max(0, Math.min(start, text.length()));
            end = Math.max(0, Math.min(end, text.length()));
            if (start > end) {
                int tmp = start;
                start = end;
                end = tmp;
            }

            String updated = text.substring(0, start) + "\n" + text.substring(end);
            Bundle setText = new Bundle();
            setText.putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    updated);
            if (!input.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, setText)) return false;

            int cursor = start + 1;
            Bundle selection = new Bundle();
            selection.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, cursor);
            selection.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, cursor);
            input.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, selection);
            return true;
        } catch (Throwable ignored) {
            return false;
        } finally {
            if (input != null) input.recycle();
            root.recycle();
        }
    }

    private boolean clickSendButton() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return false;
        try {
            AccessibilityNodeInfo labeled = findLabeledSend(root);
            if (labeled != null) {
                boolean ok = clickNodeOrParent(labeled);
                labeled.recycle();
                if (ok) return true;
            }
            return clickBestButtonNearInput(root);
        } finally {
            root.recycle();
        }
    }

    private AccessibilityNodeInfo findLabeledSend(AccessibilityNodeInfo node) {
        if (node == null) return null;
        String text = normalize(node.getText());
        String desc = normalize(node.getContentDescription());
        if (looksLikeSend(text) || looksLikeSend(desc)) {
            return AccessibilityNodeInfo.obtain(node);
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child == null) continue;
            AccessibilityNodeInfo found = findLabeledSend(child);
            child.recycle();
            if (found != null) return found;
        }
        return null;
    }

    private boolean looksLikeSend(String s) {
        if (s.isEmpty()) return false;
        return s.equals("send") || s.contains("send message") || s.contains("submit")
                || s.contains("보내기") || s.contains("전송") || s.contains("메시지 보내");
    }

    private boolean looksLikeExcluded(String s) {
        return s.contains("mic") || s.contains("microphone") || s.contains("voice")
                || s.contains("audio") || s.contains("camera") || s.contains("attach")
                || s.contains("파일") || s.contains("첨부") || s.contains("음성")
                || s.contains("마이크") || s.equals("+")
                || s.contains("cancel") || s.contains("취소");
    }

    private boolean clickNodeOrParent(AccessibilityNodeInfo node) {
        AccessibilityNodeInfo cur = node;
        for (int i = 0; cur != null && i < 4; i++) {
            if (cur.isClickable() && cur.isEnabled()) {
                boolean ok = cur.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                if (cur != node) cur.recycle();
                return ok;
            }
            AccessibilityNodeInfo parent = cur.getParent();
            if (cur != node) cur.recycle();
            cur = parent;
        }
        if (cur != null && cur != node) cur.recycle();
        return false;
    }

    private boolean clickBestButtonNearInput(AccessibilityNodeInfo root) {
        AccessibilityNodeInfo input = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        if (input == null) return false;

        Rect inputRect = new Rect();
        input.getBoundsInScreen(inputRect);
        List<AccessibilityNodeInfo> clickable = new ArrayList<>();
        collectClickable(root, clickable);

        AccessibilityNodeInfo best = null;
        long bestScore = Long.MAX_VALUE;
        int inputCenterY = inputRect.centerY();
        for (AccessibilityNodeInfo node : clickable) {
            String label = normalize(node.getContentDescription()) + " " + normalize(node.getText());
            if (looksLikeExcluded(label)) continue;

            Rect r = new Rect();
            node.getBoundsInScreen(r);
            if (r.width() <= 0 || r.height() <= 0 || r.centerX() < inputRect.centerX()) continue;

            int dx = Math.abs(r.centerX() - inputRect.right);
            int dy = Math.abs(r.centerY() - inputCenterY);
            long score = (long) dx * 2L + dy;
            if (score < bestScore) {
                bestScore = score;
                best = node;
            }
        }

        boolean ok = best != null && best.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        for (AccessibilityNodeInfo node : clickable) node.recycle();
        input.recycle();
        return ok;
    }

    private void collectClickable(AccessibilityNodeInfo node, List<AccessibilityNodeInfo> out) {
        if (node == null) return;
        if (node.isClickable() && node.isEnabled()) {
            out.add(AccessibilityNodeInfo.obtain(node));
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child == null) continue;
            collectClickable(child, out);
            child.recycle();
        }
    }

    private String normalize(CharSequence cs) {
        return cs == null ? "" : cs.toString().trim().toLowerCase(Locale.ROOT);
    }
}