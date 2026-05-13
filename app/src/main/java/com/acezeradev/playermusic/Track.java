package com.acezeradev.playermusic;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Locale;

public class Track implements Parcelable {
    public final long id;
    public final String title;
    public final String artist;
    public final String album;
    public final String uri;
    public final long duration;
    public final long albumId;
    public final long dateAdded;

    public Track(long id, String title, String artist, String album, String uri, long duration, long albumId, long dateAdded) {
        this.id = id;
        this.title = clean(title, "Musica sem titulo");
        this.artist = clean(artist, "Artista desconhecido");
        this.album = clean(album, "Album desconhecido");
        this.uri = uri;
        this.duration = duration;
        this.albumId = albumId;
        this.dateAdded = dateAdded;
    }

    protected Track(Parcel in) {
        id = in.readLong();
        title = in.readString();
        artist = in.readString();
        album = in.readString();
        uri = in.readString();
        duration = in.readLong();
        albumId = in.readLong();
        dateAdded = in.readLong();
    }

    public static final Creator<Track> CREATOR = new Creator<Track>() {
        @Override
        public Track createFromParcel(Parcel in) {
            return new Track(in);
        }

        @Override
        public Track[] newArray(int size) {
            return new Track[size];
        }
    };

    private static String clean(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty() || "<unknown>".equalsIgnoreCase(trimmed)) {
            return fallback;
        }
        return trimmed;
    }

    public String searchableText() {
        return (title + " " + artist + " " + album).toLowerCase(Locale.ROOT);
    }

    public String subtitle() {
        return artist + " - " + album;
    }

    public String formattedDuration() {
        long totalSeconds = Math.max(0, duration / 1000);
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format(Locale.ROOT, "%d:%02d", minutes, seconds);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(title);
        dest.writeString(artist);
        dest.writeString(album);
        dest.writeString(uri);
        dest.writeLong(duration);
        dest.writeLong(albumId);
        dest.writeLong(dateAdded);
    }
}

