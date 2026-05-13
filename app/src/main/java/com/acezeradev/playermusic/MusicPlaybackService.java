package com.acezeradev.playermusic;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class MusicPlaybackService extends Service {
    public static final String ACTION_TOGGLE = "com.acezeradev.playermusic.TOGGLE";
    public static final String ACTION_NEXT = "com.acezeradev.playermusic.NEXT";
    public static final String ACTION_PREVIOUS = "com.acezeradev.playermusic.PREVIOUS";
    public static final String ACTION_STOP = "com.acezeradev.playermusic.STOP";

    private static final String CHANNEL_ID = "player_music_playback";
    private static final int NOTIFICATION_ID = 1907;

    private final IBinder binder = new LocalBinder();
    private final ArrayList<Track> queue = new ArrayList<>();
    private final Set<PlaybackListener> listeners = new HashSet<>();
    private final Random random = new Random();

    private MediaPlayer player;
    private MediaSession mediaSession;
    private AudioManager audioManager;
    private AudioFocusRequest focusRequest;
    private int currentIndex = -1;
    private int repeatMode = 0;
    private boolean shuffleEnabled = false;
    private boolean prepared = false;
    private boolean playWhenReady = false;
    private boolean pausedForFocusLoss = false;

    public interface PlaybackListener {
        void onPlaybackChanged();
    }

    public class LocalBinder extends Binder {
        MusicPlaybackService getService() {
            return MusicPlaybackService.this;
        }
    }

    private final BroadcastReceiver noisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                pause();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        AudioAttributes attributes = playbackAttributes();
        focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attributes)
                .setOnAudioFocusChangeListener(this::onAudioFocusChanged)
                .build();
        mediaSession = new MediaSession(this, "PlayerMusic");
        mediaSession.setCallback(new MediaSession.Callback() {
            @Override
            public void onPlay() {
                play();
            }

            @Override
            public void onPause() {
                pause();
            }

            @Override
            public void onSkipToNext() {
                next();
            }

            @Override
            public void onSkipToPrevious() {
                previous();
            }

            @Override
            public void onSeekTo(long pos) {
                seekTo((int) pos);
            }

            @Override
            public void onStop() {
                stopPlayback();
            }
        });
        mediaSession.setActive(true);
        registerReceiver(noisyReceiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
        createNotificationChannel();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case ACTION_TOGGLE:
                    toggle();
                    break;
                case ACTION_NEXT:
                    next();
                    break;
                case ACTION_PREVIOUS:
                    previous();
                    break;
                case ACTION_STOP:
                    stopPlayback();
                    break;
                default:
                    break;
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(noisyReceiver);
        releasePlayer();
        if (mediaSession != null) {
            mediaSession.setActive(false);
            mediaSession.release();
        }
        abandonAudioFocus();
        super.onDestroy();
    }

    public void addListener(PlaybackListener listener) {
        listeners.add(listener);
    }

    public void removeListener(PlaybackListener listener) {
        listeners.remove(listener);
    }

    public void setQueue(ArrayList<Track> tracks, int startIndex, boolean autoplay) {
        queue.clear();
        if (tracks != null) {
            queue.addAll(tracks);
        }
        if (queue.isEmpty()) {
            currentIndex = -1;
            releasePlayer();
            notifyPlaybackChanged();
            return;
        }
        currentIndex = Math.max(0, Math.min(startIndex, queue.size() - 1));
        prepareCurrent(autoplay);
    }

    public void playQueueIndex(int index) {
        if (index >= 0 && index < queue.size()) {
            currentIndex = index;
            prepareCurrent(true);
        }
    }

    public void enqueueNext(Track track) {
        if (track == null) {
            return;
        }
        if (queue.isEmpty()) {
            queue.add(track);
            currentIndex = 0;
            prepareCurrent(false);
        } else {
            int insertAt = Math.min(queue.size(), currentIndex + 1);
            queue.add(insertAt, track);
        }
        notifyPlaybackChanged();
    }

    public ArrayList<Track> getQueue() {
        return new ArrayList<>(queue);
    }

    public Track getCurrentTrack() {
        if (currentIndex < 0 || currentIndex >= queue.size()) {
            return null;
        }
        return queue.get(currentIndex);
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public boolean hasTrack() {
        return getCurrentTrack() != null;
    }

    public boolean isPlaying() {
        return player != null && prepared && player.isPlaying();
    }

    public int getDuration() {
        if (player == null || !prepared) {
            Track track = getCurrentTrack();
            return track == null ? 0 : (int) track.duration;
        }
        return player.getDuration();
    }

    public int getPosition() {
        if (player == null || !prepared) {
            return 0;
        }
        return player.getCurrentPosition();
    }

    public void toggle() {
        if (isPlaying()) {
            pause();
        } else {
            play();
        }
    }

    public void play() {
        if (player == null) {
            if (hasTrack()) {
                prepareCurrent(true);
            }
            return;
        }
        playWhenReady = true;
        if (!prepared) {
            notifyPlaybackChanged();
            return;
        }
        if (!requestAudioFocus()) {
            return;
        }
        player.start();
        updatePlaybackState();
        startForeground(NOTIFICATION_ID, buildNotification());
        notifyPlaybackChanged();
    }

    public void pause() {
        playWhenReady = false;
        if (player != null && prepared && player.isPlaying()) {
            player.pause();
        }
        updatePlaybackState();
        updateNotification();
        notifyPlaybackChanged();
    }

    public void next() {
        if (queue.isEmpty()) {
            return;
        }
        if (shuffleEnabled && queue.size() > 1) {
            int nextIndex = currentIndex;
            while (nextIndex == currentIndex) {
                nextIndex = random.nextInt(queue.size());
            }
            currentIndex = nextIndex;
        } else {
            currentIndex = currentIndex + 1;
            if (currentIndex >= queue.size()) {
                currentIndex = 0;
            }
        }
        prepareCurrent(true);
    }

    public void previous() {
        if (queue.isEmpty()) {
            return;
        }
        if (getPosition() > 3500) {
            seekTo(0);
            return;
        }
        currentIndex = currentIndex - 1;
        if (currentIndex < 0) {
            currentIndex = queue.size() - 1;
        }
        prepareCurrent(true);
    }

    public void seekTo(int positionMs) {
        if (player != null && prepared) {
            player.seekTo(Math.max(0, positionMs));
            updatePlaybackState();
            notifyPlaybackChanged();
        }
    }

    public void setShuffleEnabled(boolean enabled) {
        shuffleEnabled = enabled;
        notifyPlaybackChanged();
    }

    public boolean isShuffleEnabled() {
        return shuffleEnabled;
    }

    public void setRepeatMode(int mode) {
        repeatMode = mode;
        notifyPlaybackChanged();
    }

    public int getRepeatMode() {
        return repeatMode;
    }

    public void stopIfIdle() {
        if (!isPlaying()) {
            stopSelf();
        }
    }

    private void prepareCurrent(boolean autoplay) {
        Track track = getCurrentTrack();
        if (track == null) {
            return;
        }
        releasePlayer();
        prepared = false;
        playWhenReady = autoplay;
        player = new MediaPlayer();
        player.setAudioAttributes(playbackAttributes());
        player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        player.setOnPreparedListener(mediaPlayer -> {
            prepared = true;
            updateMediaMetadata();
            updatePlaybackState();
            if (playWhenReady) {
                play();
            } else {
                updateNotification();
                notifyPlaybackChanged();
            }
        });
        player.setOnCompletionListener(mediaPlayer -> handleCompletion());
        player.setOnErrorListener((mediaPlayer, what, extra) -> {
            handleCompletion();
            return true;
        });
        try {
            player.setDataSource(this, Uri.parse(track.uri));
            player.prepareAsync();
            updateMediaMetadata();
            updatePlaybackState();
            updateNotification();
            notifyPlaybackChanged();
        } catch (IOException | SecurityException error) {
            handleCompletion();
        }
    }

    private void handleCompletion() {
        if (queue.isEmpty()) {
            stopPlayback();
            return;
        }
        if (repeatMode == 2) {
            prepareCurrent(true);
            return;
        }
        if (shuffleEnabled && queue.size() > 1) {
            next();
            return;
        }
        if (currentIndex < queue.size() - 1) {
            currentIndex++;
            prepareCurrent(true);
        } else if (repeatMode == 1) {
            currentIndex = 0;
            prepareCurrent(true);
        } else {
            playWhenReady = false;
            seekTo(0);
            pause();
        }
    }

    private void stopPlayback() {
        playWhenReady = false;
        releasePlayer();
        abandonAudioFocus();
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
        notifyPlaybackChanged();
    }

    private void releasePlayer() {
        if (player != null) {
            try {
                player.reset();
                player.release();
            } catch (IllegalStateException ignored) {
            }
            player = null;
        }
        prepared = false;
    }

    private AudioAttributes playbackAttributes() {
        return new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
    }

    private boolean requestAudioFocus() {
        if (audioManager == null || focusRequest == null) {
            return true;
        }
        return audioManager.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    private void abandonAudioFocus() {
        if (audioManager != null && focusRequest != null) {
            audioManager.abandonAudioFocusRequest(focusRequest);
        }
    }

    private void onAudioFocusChanged(int focusChange) {
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
            pausedForFocusLoss = false;
            pause();
        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
            pausedForFocusLoss = isPlaying();
            pause();
        } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN && pausedForFocusLoss) {
            pausedForFocusLoss = false;
            play();
        }
    }

    private void updateMediaMetadata() {
        Track track = getCurrentTrack();
        if (track == null || mediaSession == null) {
            return;
        }
        MediaMetadata metadata = new MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, track.title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, track.artist)
                .putString(MediaMetadata.METADATA_KEY_ALBUM, track.album)
                .putLong(MediaMetadata.METADATA_KEY_DURATION, track.duration)
                .build();
        mediaSession.setMetadata(metadata);
    }

    private void updatePlaybackState() {
        if (mediaSession == null) {
            return;
        }
        int state = isPlaying() ? PlaybackState.STATE_PLAYING : PlaybackState.STATE_PAUSED;
        long actions = PlaybackState.ACTION_PLAY
                | PlaybackState.ACTION_PAUSE
                | PlaybackState.ACTION_PLAY_PAUSE
                | PlaybackState.ACTION_SKIP_TO_NEXT
                | PlaybackState.ACTION_SKIP_TO_PREVIOUS
                | PlaybackState.ACTION_SEEK_TO
                | PlaybackState.ACTION_STOP;
        PlaybackState playbackState = new PlaybackState.Builder()
                .setActions(actions)
                .setState(state, getPosition(), isPlaying() ? 1f : 0f)
                .build();
        mediaSession.setPlaybackState(playbackState);
    }

    private void updateNotification() {
        if (!hasTrack()) {
            return;
        }
        Notification notification = buildNotification();
        if (isPlaying() || prepared || playWhenReady) {
            startForeground(NOTIFICATION_ID, notification);
        } else {
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.notify(NOTIFICATION_ID, notification);
            }
        }
    }

    private Notification buildNotification() {
        Track track = getCurrentTrack();
        String title = track == null ? "PlayerMusic" : track.title;
        String artist = track == null ? "Musicas locais" : track.artist;
        PendingIntent contentIntent = PendingIntent.getActivity(
                this,
                0,
                new Intent(this, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        Notification.Action previous = action(R.drawable.ic_skip_previous, "Anterior", ACTION_PREVIOUS, 1);
        Notification.Action playPause = action(isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play_arrow, isPlaying() ? "Pausar" : "Tocar", ACTION_TOGGLE, 2);
        Notification.Action next = action(R.drawable.ic_skip_next, "Proxima", ACTION_NEXT, 3);
        return new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_music)
                .setContentTitle(title)
                .setContentText(artist)
                .setSubText("PlayerMusic")
                .setColor(0xFF3DDB9A)
                .setContentIntent(contentIntent)
                .setOngoing(isPlaying())
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .addAction(previous)
                .addAction(playPause)
                .addAction(next)
                .setStyle(new Notification.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2))
                .build();
    }

    private Notification.Action action(int icon, String title, String action, int requestCode) {
        Intent intent = new Intent(this, MusicPlaybackService.class).setAction(action);
        PendingIntent pendingIntent = PendingIntent.getService(this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Action.Builder(icon, title, pendingIntent).build();
    }

    private void createNotificationChannel() {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager == null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "PlayerMusic", NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Controles de reproducao em segundo plano");
        channel.setShowBadge(false);
        manager.createNotificationChannel(channel);
    }

    private void notifyPlaybackChanged() {
        updatePlaybackState();
        for (PlaybackListener listener : new HashSet<>(listeners)) {
            listener.onPlaybackChanged();
        }
    }
}

