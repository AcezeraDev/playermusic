package com.acezeradev.playermusic;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.UUID;

public class PlaylistStore {
    private static final String PREFS = "player_music_playlists";
    private static final String KEY_PLAYLISTS = "playlists";

    private final SharedPreferences preferences;

    public PlaylistStore(Context context) {
        preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public ArrayList<Playlist> getPlaylists() {
        String raw = preferences.getString(KEY_PLAYLISTS, null);
        ArrayList<Playlist> playlists = new ArrayList<>();
        if (raw != null) {
            try {
                JSONArray array = new JSONArray(raw);
                for (int i = 0; i < array.length(); i++) {
                    playlists.add(Playlist.fromJson(array.getJSONObject(i)));
                }
            } catch (JSONException ignored) {
                playlists.clear();
            }
        }
        ensureFavorites(playlists);
        save(playlists);
        return playlists;
    }

    public Playlist createPlaylist(String name) {
        ArrayList<Playlist> playlists = getPlaylists();
        Playlist playlist = new Playlist(UUID.randomUUID().toString(), name, System.currentTimeMillis(), new ArrayList<>());
        playlists.add(playlist);
        save(playlists);
        return playlist;
    }

    public void renamePlaylist(String playlistId, String name) {
        ArrayList<Playlist> playlists = getPlaylists();
        for (Playlist playlist : playlists) {
            if (playlist.id.equals(playlistId)) {
                playlist.name = name;
                break;
            }
        }
        save(playlists);
    }

    public void deletePlaylist(String playlistId) {
        if (Playlist.FAVORITES_ID.equals(playlistId)) {
            return;
        }
        ArrayList<Playlist> playlists = getPlaylists();
        for (int i = playlists.size() - 1; i >= 0; i--) {
            if (playlists.get(i).id.equals(playlistId)) {
                playlists.remove(i);
            }
        }
        save(playlists);
    }

    public void addTrack(String playlistId, Track track) {
        if (track == null) {
            return;
        }
        ArrayList<Playlist> playlists = getPlaylists();
        for (Playlist playlist : playlists) {
            if (playlist.id.equals(playlistId) && !playlist.trackUris.contains(track.uri)) {
                playlist.trackUris.add(track.uri);
                break;
            }
        }
        save(playlists);
    }

    public void removeTrack(String playlistId, Track track) {
        if (track == null) {
            return;
        }
        ArrayList<Playlist> playlists = getPlaylists();
        for (Playlist playlist : playlists) {
            if (playlist.id.equals(playlistId)) {
                playlist.trackUris.remove(track.uri);
                break;
            }
        }
        save(playlists);
    }

    public boolean toggleFavorite(Track track) {
        if (track == null) {
            return false;
        }
        ArrayList<Playlist> playlists = getPlaylists();
        Playlist favorites = null;
        for (Playlist playlist : playlists) {
            if (Playlist.FAVORITES_ID.equals(playlist.id)) {
                favorites = playlist;
                break;
            }
        }
        if (favorites == null) {
            favorites = createFavorites();
            playlists.add(0, favorites);
        }
        boolean added;
        if (favorites.trackUris.contains(track.uri)) {
            favorites.trackUris.remove(track.uri);
            added = false;
        } else {
            favorites.trackUris.add(track.uri);
            added = true;
        }
        save(playlists);
        return added;
    }

    private void ensureFavorites(ArrayList<Playlist> playlists) {
        for (Playlist playlist : playlists) {
            if (Playlist.FAVORITES_ID.equals(playlist.id)) {
                return;
            }
        }
        playlists.add(0, createFavorites());
    }

    private Playlist createFavorites() {
        return new Playlist(Playlist.FAVORITES_ID, "Favoritas", System.currentTimeMillis(), new ArrayList<>());
    }

    private void save(ArrayList<Playlist> playlists) {
        JSONArray array = new JSONArray();
        for (Playlist playlist : playlists) {
            try {
                array.put(playlist.toJson());
            } catch (JSONException ignored) {
                // A playlist with invalid JSON data is skipped instead of breaking all saved data.
            }
        }
        preferences.edit().putString(KEY_PLAYLISTS, array.toString()).apply();
    }
}

