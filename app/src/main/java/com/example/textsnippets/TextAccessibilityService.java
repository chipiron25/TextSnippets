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
import android.widget.TextView;

import java.util.List;

public class TextAccessibilityService extends AccessibilityService {

    private WindowManager windowManager;
    private View floatingView;
    private WindowManager.LayoutParams windowParams;
    private boolean isShowing = false;
    private AccessibilityNodeInfo lastFocusedNode = null;

    // For dragging the floating window
    private int dragInitialX, dragInitialY;
    private float dragInitialTouchX, dragInitialTouchY;

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

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

    // -----------------------------------------------------------------------
    // Accessibility events
    // -----------------------------------------------------------------------

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
            // Check if there's still any editable field with focus
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root != null) {
                AccessibilityNodeInfo inputFocus = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
                if (inputFocus == null || !inputFocus.isEditable()) {
                    // No editable field focused — hide the panel
                    hideFloatingWindow();
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Floating window management
    // -----------------------------------------------------------------------

    private void inflateFloatingView() {
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_window, null);

        // Initial window params: bottom-right, above keyboard area
        windowParams = new WindowManager.LayoutParams(
                dpToPx(220),
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
        );
        windowParams.gravity = Gravity.BOTTOM | Gravity.START;
        windowParams.x = dpToPx(8);
        windowParams.y = dpToPx(280); // above typical keyboard height

        // Drag handle
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
                    windowParams.x = dragInitialX + (int) (event.getRawX() - dragInitialTouchX);
                    windowParams.y = dragInitialY - (int) (event.getRawY() - dragInitialTouchY);
                    if (isShowing) {
                        windowManager.updateViewLayout(floatingView, windowParams);
                    }
                    return true;
            }
            return false;
        });

        // Close button
        floatingView.findViewById(R.id.btn_close).setOnClickListener(v -> hideFloatingWindow());
    }

    private void showFloatingWindow() {
        refreshButtons();
        if (!isShowing && floatingView != null) {
            try {
                windowManager.addView(floatingView, windowParams);
                isShowing = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void hideFloatingWindow() {
        if (isShowing && floatingView != null) {
            try {
                windowManager.removeView(floatingView);
                isShowing = false;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // -----------------------------------------------------------------------
    // Button population
    // -----------------------------------------------------------------------

    private void refreshButtons() {
        if (floatingView == null) return;
        LinearLayout container = floatingView.findViewById(R.id.buttons_container);
        container.removeAllViews();

        List<String> snippets = SnippetManager.getSnippets(this);

        if (snippets.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Sin textos configurados.\nAbre la app para añadirlos.");
            empty.setTextColor(0xFFCCCCCC);
            empty.setTextSize(11f);
            empty.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
            container.addView(empty);
            return;
        }

        for (String snippet : snippets) {
            Button btn = new Button(this);
            // Show truncated label; full text is in the tag
            String label = snippet.length() > 35 ? snippet.substring(0, 35) + "…" : snippet;
            btn.setText(label);
            btn.setTag(snippet);
            btn.setTextSize(12f);
            btn.setAllCaps(false);
            btn.setBackgroundColor(0xFF37474F);
            btn.setTextColor(0xFFFFFFFF);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            lp.setMargins(dpToPx(4), dpToPx(3), dpToPx(4), dpToPx(3));
            btn.setLayoutParams(lp);
            btn.setPadding(dpToPx(8), dpToPx(6), dpToPx(8), dpToPx(6));

            btn.setOnClickListener(v -> {
                insertText((String) v.getTag());
                // Visual feedback
                v.setBackgroundColor(0xFF1B5E20);
                new Handler(Looper.getMainLooper()).postDelayed(
                        () -> v.setBackgroundColor(0xFF37474F), 600);
            });

            container.addView(btn);
        }
    }

    // -----------------------------------------------------------------------
    // Text injection
    // -----------------------------------------------------------------------

    private void insertText(String text) {
        // Re-find the focused editable node (more reliable than caching)
        AccessibilityNodeInfo targetNode = null;
        try {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root != null) {
                AccessibilityNodeInfo focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
                if (focused != null && focused.isEditable()) {
                    targetNode = focused;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Fall back to the last known node
        if (targetNode == null) {
            targetNode = lastFocusedNode;
        }

        if (targetNode == null) return;

        // Primary method: paste via clipboard (inserts at cursor position)
        try {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("snippet", text);
            clipboard.setPrimaryClip(clip);
            boolean pasted = targetNode.performAction(AccessibilityNodeInfo.ACTION_PASTE);

            // Fallback: append with ACTION_SET_TEXT
            if (!pasted) {
                CharSequence current = targetNode.getText();
                String newText = (current != null ? current.toString() : "") + text;
                Bundle args = new Bundle();
                args.putCharSequence(
                        AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                        newText);
                targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // -----------------------------------------------------------------------
    // Utilities
    // -----------------------------------------------------------------------

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
