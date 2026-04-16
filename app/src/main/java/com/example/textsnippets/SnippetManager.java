package com.example.textsnippets;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class SnippetManager {

    private static final String PREFS_NAME = "textsnippets_prefs";
    private static final String KEY_SNIPPETS = "snippets_json";

    public static List<String> getSnippets(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_SNIPPETS, "[]");
        List<String> snippets = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                snippets.add(arr.getString(i));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return snippets;
    }

    public static void saveSnippets(Context context, List<String> snippets) {
        JSONArray arr = new JSONArray(snippets);
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_SNIPPETS, arr.toString()).apply();
    }
}
