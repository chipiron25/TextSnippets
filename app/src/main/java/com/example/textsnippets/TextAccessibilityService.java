package com.example.textsnippets;

import android.accessibilityservice.AccessibilityService;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

public class TextAccessibilityService extends AccessibilityService {

    private WindowManager windowManager;
    private View floatingView;
    private WindowManager.LayoutParams windowParams;
    private boolean isShowing = false;
    private boolean isExpanded = false;
    private AccessibilityNodeInfo lastFocusedNode = null;

    private int dragInitialX, dragInitialY;
    private float dragInitialTouchX, dragInitialTouchY;

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        inflateFloatingView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isShowing) {
            try { windowManager.removeView(floatingView); } catch (Exception ignored) {}
            isShowing = false;
        }
    }

    @Override
    public void onInterrupt() {}

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int type = event.getEventType();
        if (type == AccessibilityEvent.TYPE_VIEW_FOCUSED) {
            AccessibilityNodeInfo source = event.getSource();
            if (source != null && source.isEditable()) {
                lastFocusedNode = source;
                showFloatingWindow();
            }
        } else if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                || type == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root != null) {
                AccessibilityNodeInfo inputFocus = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
                if (inputFocus == null || !inputFocus.isEditable()) {
                    hideFloatingWindow();
                }
            }
        }
    }

    private void inflateFloatingView() {
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_window, null);

        windowParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
        );
        windowParams.gravity = Gravity.BOTTOM | Gravity.START;
        windowParams.x = dpToPx(8);
        windowParams.y = dpToPx(280);

        // Drag
        View dragHandle = floatingView.findViewById(R.id.drag_handle);
        dragHandle.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    dragInitialX = windowParams.x;
                    dragInitialY = windowParams.y;
                    dragInitialTouchX = event.getRawX();
                    dragInitialTouchY = event.getRawY();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    windowParams.x = dragInitialX + (int)(event.getRawX() - dragInitialTouchX);
                    windowParams.y = dragInitialY - (int)(event.getRawY() - dragInitialTouchY);
                    if (isShowing) windowManager.updateViewLayout(floatingView, windowParams);
                    return true;
            }
            return false;
        });

        // Botón "Atajos" — colapsa/expande
        Button btnToggle = floatingView.findViewById(R.id.btn_toggle);
        ScrollView scrollView = floatingView.findViewById(R.id.scroll_buttons);

        btnToggle.setOnClickListener(v -> {
            isExpanded = !isExpanded;
            if (isExpanded) {
                refreshButtons();
                scrollView.setVisibility(View.VISIBLE);
                btnToggle.setText("▲ Atajos");
            } else {
                scrollView.setVisibility(View.GONE);
                btnToggle.setText("▼ Atajos");
            }
            // Forzar redibujado del tamaño en el WindowManager
            floatingView.post(() -> {
                if (isShowing) {
                    try { windowManager.updateViewLayout(floatingView, windowParams); } catch (Exception ignored) {}
                }
            });
        });

        // Cerrar
        floatingView.findViewById(R.id.btn_close).setOnClickListener(v -> hideFloatingWindow());
    }

    private void showFloatingWindow() {
        // Al mostrar, siempre empieza colapsado
        isExpanded = false;
        Button btnToggle = floatingView.findViewById(R.id.btn_toggle);
        ScrollView scrollView = floatingView.findViewById(R.id.scroll_buttons);
        btnToggle.setText("▼ Atajos");
        scrollView.setVisibility(View.GONE);

        if (!isShowing && floatingView != null) {
            try {
                windowManager.addView(floatingView, windowParams);
                isShowing = true;
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private void hideFloatingWindow() {
        if (isShowing && floatingView != null) {
            try {
                windowManager.removeView(floatingView);
                isShowing = false;
                isExpanded = false;
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private void refreshButtons() {
        if (floatingView == null) return;
        LinearLayout container = floatingView.findViewById(R.id.buttons_container);
        container.removeAllViews();

        List<SnippetManager.Snippet> snippets = SnippetManager.getSnippets(this);

        if (snippets.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Sin atajos configurados");
            empty.setTextColor(0xFFCCCCCC);
            empty.setTextSize(11f);
            empty.setPadding(dpToPx(8), dpToPx(6), dpToPx(8), dpToPx(6));
            container.addView(empty);
            return;
        }

        for (SnippetManager.Snippet snippet : snippets) {
            Button btn = new Button(this);
            btn.setText(snippet.label);
            btn.setTag(snippet.text);
            btn.setTextSize(11f);
            btn.setAllCaps(false);
            btn.setBackgroundColor(0xFF37474F);
            btn.setTextColor(0xFFFFFFFF);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dpToPx(32)
            );
            lp.setMargins(dpToPx(3), dpToPx(2), dpToPx(3), dpToPx(2));
            btn.setLayoutParams(lp);
            btn.setPadding(dpToPx(6), 0, dpToPx(6), 0);

            btn.setOnClickListener(v -> {
                insertText((String) v.getTag());
                v.setBackgroundColor(0xFF1B5E20);
                new Handler(Looper.getMainLooper()).postDelayed(
                        () -> v.setBackgroundColor(0xFF37474F), 500);
            });

            container.addView(btn);
        }
    }

    private void insertText(String text) {
        AccessibilityNodeInfo targetNode = null;
        try {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root != null) {
                AccessibilityNodeInfo focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
                if (focused != null && focused.isEditable()) targetNode = focused;
            }
        } catch (Exception e) { e.printStackTrace(); }

        if (targetNode == null) targetNode = lastFocusedNode;
        if (targetNode == null) return;

        try {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("snippet", text));
            boolean pasted = targetNode.performAction(AccessibilityNodeInfo.ACTION_PASTE);
            if (!pasted) {
                CharSequence current = targetNode.getText();
                String newText = (current != null ? current.toString() : "") + text;
                Bundle args = new Bundle();
                args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, newText);
                targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
