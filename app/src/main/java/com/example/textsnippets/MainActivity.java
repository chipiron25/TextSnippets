package com.example.textsnippets;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private LinearLayout snippetsContainer;
    private List<String> snippets = new ArrayList<>();
    private TextView tvAccessibilityStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        snippetsContainer = findViewById(R.id.snippets_container);
        tvAccessibilityStatus = findViewById(R.id.tv_accessibility_status);

        snippets = SnippetManager.getSnippets(this);
        renderSnippets();

        findViewById(R.id.btn_add_snippet).setOnClickListener(v -> showAddEditDialog(-1, ""));
        findViewById(R.id.btn_enable_accessibility).setOnClickListener(v -> openAccessibilitySettings());
        findViewById(R.id.btn_overlay_permission).setOnClickListener(v -> openOverlaySettings());
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatusIndicators();
    }

    // -----------------------------------------------------------------------
    // Status indicators
    // -----------------------------------------------------------------------

    private void updateStatusIndicators() {
        boolean accessibilityOn = isAccessibilityServiceEnabled();
        boolean overlayOn = Settings.canDrawOverlays(this);

        tvAccessibilityStatus.setText(
                "Accesibilidad: " + (accessibilityOn ? "✅ Activo" : "❌ Desactivado") +
                "   Superposición: " + (overlayOn ? "✅ OK" : "⚠️ Opcional")
        );

        Button btnAcc = findViewById(R.id.btn_enable_accessibility);
        btnAcc.setText(accessibilityOn ? "Accesibilidad: ACTIVA ✅" : "Activar Accesibilidad ⚠️");
    }

    private boolean isAccessibilityServiceEnabled() {
        AccessibilityManager am = (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> enabledServices =
                am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
        for (AccessibilityServiceInfo info : enabledServices) {
            if (info.getId() != null && info.getId().contains(getPackageName())) {
                return true;
            }
        }
        return false;
    }

    // -----------------------------------------------------------------------
    // Snippets rendering
    // -----------------------------------------------------------------------

    private void renderSnippets() {
        snippetsContainer.removeAllViews();

        if (snippets.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Aún no has añadido ningún texto rápido.\nPulsa el botón de abajo para empezar.");
            empty.setTextColor(0xFF888888);
            empty.setPadding(0, 32, 0, 32);
            snippetsContainer.addView(empty);
            return;
        }

        for (int i = 0; i < snippets.size(); i++) {
            final int index = i;
            View item = LayoutInflater.from(this).inflate(R.layout.item_snippet, snippetsContainer, false);

            TextView tvText = item.findViewById(R.id.tv_snippet_text);
            tvText.setText((i + 1) + ". " + snippets.get(i));

            item.findViewById(R.id.btn_edit).setOnClickListener(
                    v -> showAddEditDialog(index, snippets.get(index)));
            item.findViewById(R.id.btn_delete).setOnClickListener(
                    v -> confirmDelete(index));

            snippetsContainer.addView(item);
        }
    }

    // -----------------------------------------------------------------------
    // Add / Edit dialog
    // -----------------------------------------------------------------------

    private void showAddEditDialog(int index, String existingText) {
        boolean isEdit = index >= 0;

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_snippet, null);
        EditText editText = dialogView.findViewById(R.id.et_snippet_input);
        editText.setText(existingText);
        editText.setSelection(existingText.length());

        new AlertDialog.Builder(this)
                .setTitle(isEdit ? "Editar texto rápido" : "Nuevo texto rápido")
                .setView(dialogView)
                .setPositiveButton("Guardar", (d, w) -> {
                    String text = editText.getText().toString().trim();
                    if (text.isEmpty()) {
                        Toast.makeText(this, "El texto no puede estar vacío", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (isEdit) {
                        snippets.set(index, text);
                    } else {
                        snippets.add(text);
                    }
                    SnippetManager.saveSnippets(this, snippets);
                    renderSnippets();
                    Toast.makeText(this, isEdit ? "Guardado ✓" : "Añadido ✓", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void confirmDelete(int index) {
        new AlertDialog.Builder(this)
                .setTitle("Borrar texto")
                .setMessage("¿Eliminar «" + snippets.get(index) + "»?")
                .setPositiveButton("Borrar", (d, w) -> {
                    snippets.remove(index);
                    SnippetManager.saveSnippets(this, snippets);
                    renderSnippets();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // -----------------------------------------------------------------------
    // Permission navigation
    // -----------------------------------------------------------------------

    private void openAccessibilitySettings() {
        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        Toast.makeText(this,
                "Busca 'TextSnippets' y actívalo", Toast.LENGTH_LONG).show();
    }

    private void openOverlaySettings() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }
}
