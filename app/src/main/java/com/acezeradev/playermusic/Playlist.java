package com.acezeradev.playermusic;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class Playlist {
    public static final String FAVORITES_ID = "favorites";

    public final String id;
    public String name;
    public final long createdAt;
    public final ArrayList<String> trackUris;

    public Playlist(String id, String name, long createdAt, ArrayList<String> trackUris) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
        this.trackUris = trackUris == null ? new ArrayList<>() : trackUris;
    }

    public boolean contains(Track track) {
        return track != null && trackUris.contains(track.uri);
    }

    public JSONObject toJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("id", id);
        object.put("name", name);
        object.put("createdAt", createdAt);
        JSONArray tracks = new JSONArray();
        for (String uri : trackUris) {
            tracks.put(uri);
        }
        object.put("tracks", tracks);
        return object;
    }

    public static Playlist fromJson(JSONObject object) throws JSONException {
        JSONArray tracksJson = object.optJSONArray("tracks");
        ArrayList<String> tracks = new ArrayList<>();
        if (tracksJson != null) {
            for (int i = 0; i < tracksJson.length(); i++) {
                tracks.add(tracksJson.getString(i));
            }
        }
        return new Playlist(
                object.getString("id"),
                object.optString("name", "Playlist"),
                object.optLong("createdAt", System.currentTimeMillis()),
                tracks
        );
    }
}

