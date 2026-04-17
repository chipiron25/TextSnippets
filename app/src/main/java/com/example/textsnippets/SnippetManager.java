package com.example.textsnippets;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SnippetManager {

    private static final String PREFS_NAME = "textsnippets_prefs";
    private static final String KEY_SNIPPETS = "snippets_json";

    public static class Snippet {
        public String label;
        public String text;

        public Snippet(String label, String text) {
            this.label = label;
            this.text = text;
        }
    }

    public static List<Snippet> getSnippets(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_SNIPPETS, "[]");
        List<Snippet> snippets = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                Object item = arr.get(i);
                // Compatibilidad con versión anterior (solo texto)
                if (item instanceof String) {
                    String t = (String) item;
                    snippets.add(new Snippet(t.length() > 20 ? t.substring(0, 20) + "…" : t, t));
                } else {
                    JSONObject obj = arr.getJSONObject(i);
                    snippets.add(new Snippet(obj.getString("label"), obj.getString("text")));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return snippets;
    }

    public static void saveSnippets(Context context, List<Snippet> snippets) {
        JSONArray arr = new JSONArray();
        for (Snippet s : snippets) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("label", s.label);
                obj.put("text", s.text);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            arr.put(obj);
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_SNIPPETS, arr.toString()).apply();
    }
}
