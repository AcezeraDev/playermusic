package com.acezeradev.playermusic;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int REQUEST_LIBRARY = 713;
    private static final int TAB_LIBRARY = 0;
    private static final int TAB_PLAYLISTS = 1;
    private static final int TAB_QUEUE = 2;

    private static final int BG = 0xFF101116;
    private static final int SURFACE = 0xFF181A22;
    private static final int SURFACE_ALT = 0xFF222533;
    private static final int TEXT = 0xFFF5F7FA;
    private static final int MUTED = 0xFFA7ADB8;
    private static final int ACCENT = 0xFF3DDB9A;
    private static final int WARM = 0xFFFFB86C;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final MusicRepository repository = new MusicRepository();
    private PlaylistStore playlistStore;

    private TextView subtitleView;
    private TextView emptyView;
    private TextView tabLibrary;
    private TextView tabPlaylists;
    private TextView tabQueue;
    private TextView nowTitle;
    private TextView nowArtist;
    private TextView elapsedView;
    private TextView durationView;
    private EditText searchInput;
    private Button permissionButton;
    private ImageButton backPlaylistButton;
    private ImageButton addPlaylistButton;
    private ImageButton playButton;
    private ImageButton shuffleButton;
    private ImageButton repeatButton;
    private SeekBar seekBar;
    private ListView listView;

    private final ArrayList<Track> allTracks = new ArrayList<>();
    private final ArrayList<Track> visibleTracks = new ArrayList<>();
    private final TrackAdapter trackAdapter = new TrackAdapter();
    private final PlaylistAdapter playlistAdapter = new PlaylistAdapter();

    private MusicPlaybackService playbackService;
    private boolean bound = false;
    private boolean userSeeking = false;
    private boolean libraryLoading = false;
    private int currentTab = TAB_LIBRARY;
    private Playlist openPlaylist;
    private ArrayList<Track> pendingQueue;
    private int pendingIndex = 0;

    private final MusicPlaybackService.PlaybackListener playbackListener = () -> runOnUiThread(() -> {
        updatePlayer();
        if (currentTab == TAB_QUEUE) {
            showQueue();
        }
    });

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicPlaybackService.LocalBinder binder = (MusicPlaybackService.LocalBinder) service;
            playbackService = binder.getService();
            playbackService.addListener(playbackListener);
            if (pendingQueue != null) {
                playbackService.setQueue(pendingQueue, pendingIndex, true);
                pendingQueue = null;
            }
            updatePlayer();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            if (playbackService != null) {
                playbackService.removeListener(playbackListener);
            }
            playbackService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        playlistStore = new PlaylistStore(this);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        buildUi();
        bindService(new Intent(this, MusicPlaybackService.class), serviceConnection, BIND_AUTO_CREATE);
        bound = true;
        if (hasLibraryPermission()) {
            loadLibrary();
        } else {
            showPermissionState();
        }
        startTicker();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (bound) {
            if (playbackService != null) {
                playbackService.removeListener(playbackListener);
                playbackService.stopIfIdle();
            }
            unbindService(serviceConnection);
            bound = false;
        }
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LIBRARY) {
            if (hasLibraryPermission()) {
                loadLibrary();
            } else {
                showPermissionState();
            }
        }
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));
        setContentView(root);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(20), dp(18), dp(20), dp(10));
        root.addView(header, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(titleRow, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout titleBlock = new LinearLayout(this);
        titleBlock.setOrientation(LinearLayout.VERTICAL);
        titleRow.addView(titleBlock, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView title = text("PlayerMusic", 30, TEXT, Typeface.BOLD);
        titleBlock.addView(title);
        subtitleView = text("Biblioteca local", 14, MUTED, Typeface.NORMAL);
        titleBlock.addView(subtitleView);

        backPlaylistButton = iconButton(R.drawable.ic_arrow_back, "Voltar", SURFACE_ALT, TEXT, dp(42));
        backPlaylistButton.setVisibility(View.GONE);
        backPlaylistButton.setOnClickListener(view -> {
            openPlaylist = null;
            showPlaylists();
        });
        titleRow.addView(backPlaylistButton);

        addPlaylistButton = iconButton(R.drawable.ic_add, "Nova playlist", ACCENT, BG, dp(42));
        addPlaylistButton.setVisibility(View.GONE);
        addPlaylistButton.setOnClickListener(view -> showCreatePlaylistDialog(null));
        titleRow.addView(addPlaylistButton);

        permissionButton = new Button(this);
        permissionButton.setText("Permitir");
        permissionButton.setTextColor(BG);
        permissionButton.setTextSize(14);
        permissionButton.setAllCaps(false);
        permissionButton.setTypeface(Typeface.DEFAULT_BOLD);
        permissionButton.setBackground(rounded(ACCENT, dp(22), 0, 0));
        permissionButton.setVisibility(View.GONE);
        permissionButton.setOnClickListener(view -> requestLibraryPermission());
        titleRow.addView(permissionButton, new LinearLayout.LayoutParams(-2, dp(42)));

        searchInput = new EditText(this);
        searchInput.setSingleLine(true);
        searchInput.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        searchInput.setHint("Buscar musica, artista ou album");
        searchInput.setTextColor(TEXT);
        searchInput.setHintTextColor(0xFF757C8B);
        searchInput.setTextSize(15);
        searchInput.setPadding(dp(16), 0, dp(16), 0);
        searchInput.setBackground(rounded(SURFACE_ALT, dp(14), 1, 0xFF2E3343));
        searchInput.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_search, 0, 0, 0);
        searchInput.setCompoundDrawablePadding(dp(10));
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                refreshCurrentView();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(-1, dp(48));
        searchParams.setMargins(0, dp(16), 0, dp(12));
        header.addView(searchInput, searchParams);

        LinearLayout tabs = new LinearLayout(this);
        tabs.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(tabs, new LinearLayout.LayoutParams(-1, dp(42)));
        tabLibrary = tab("Musicas");
        tabPlaylists = tab("Playlists");
        tabQueue = tab("Fila");
        tabs.addView(tabLibrary, tabParams());
        tabs.addView(tabPlaylists, tabParams());
        tabs.addView(tabQueue, tabParams());
        tabLibrary.setOnClickListener(view -> {
            openPlaylist = null;
            currentTab = TAB_LIBRARY;
            refreshCurrentView();
        });
        tabPlaylists.setOnClickListener(view -> {
            currentTab = TAB_PLAYLISTS;
            showPlaylists();
        });
        tabQueue.setOnClickListener(view -> {
            openPlaylist = null;
            currentTab = TAB_QUEUE;
            refreshCurrentView();
        });

        FrameLayout content = new FrameLayout(this);
        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1f));
        listView = new ListView(this);
        listView.setDivider(null);
        listView.setSelector(android.R.color.transparent);
        listView.setCacheColorHint(Color.TRANSPARENT);
        listView.setClipToPadding(false);
        listView.setPadding(dp(14), dp(4), dp(14), dp(12));
        content.addView(listView, new FrameLayout.LayoutParams(-1, -1));

        emptyView = text("", 16, MUTED, Typeface.NORMAL);
        emptyView.setGravity(Gravity.CENTER);
        emptyView.setPadding(dp(28), dp(20), dp(28), dp(20));
        content.addView(emptyView, new FrameLayout.LayoutParams(-1, -1));
        listView.setEmptyView(emptyView);

        buildPlayer(root);
        refreshTabs();
    }

    private void buildPlayer(LinearLayout root) {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(18), dp(14), dp(18), dp(14));
        panel.setBackground(playerBackground());
        root.addView(panel, new LinearLayout.LayoutParams(-1, -2));

        nowTitle = text("Nada tocando", 17, TEXT, Typeface.BOLD);
        nowTitle.setSingleLine(true);
        nowTitle.setEllipsize(TextUtils.TruncateAt.END);
        panel.addView(nowTitle);

        nowArtist = text("Escolha uma musica da sua biblioteca", 13, MUTED, Typeface.NORMAL);
        nowArtist.setSingleLine(true);
        nowArtist.setEllipsize(TextUtils.TruncateAt.END);
        panel.addView(nowArtist);

        seekBar = new SeekBar(this);
        seekBar.setMax(1000);
        seekBar.setProgress(0);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    elapsedView.setText(formatMs(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                userSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                userSeeking = false;
                if (playbackService != null) {
                    playbackService.seekTo(seekBar.getProgress());
                }
            }
        });
        panel.addView(seekBar, new LinearLayout.LayoutParams(-1, dp(38)));

        LinearLayout timeRow = new LinearLayout(this);
        timeRow.setGravity(Gravity.CENTER_VERTICAL);
        panel.addView(timeRow, new LinearLayout.LayoutParams(-1, -2));
        elapsedView = text("0:00", 12, MUTED, Typeface.NORMAL);
        durationView = text("0:00", 12, MUTED, Typeface.NORMAL);
        timeRow.addView(elapsedView, new LinearLayout.LayoutParams(0, -2, 1f));
        timeRow.addView(durationView);

        LinearLayout controls = new LinearLayout(this);
        controls.setGravity(Gravity.CENTER);
        panel.addView(controls, new LinearLayout.LayoutParams(-1, dp(58)));

        shuffleButton = iconButton(R.drawable.ic_shuffle, "Aleatorio", Color.TRANSPARENT, MUTED, dp(44));
        ImageButton previousButton = iconButton(R.drawable.ic_skip_previous, "Anterior", Color.TRANSPARENT, TEXT, dp(48));
        playButton = iconButton(R.drawable.ic_play_arrow, "Tocar", ACCENT, BG, dp(56));
        ImageButton nextButton = iconButton(R.drawable.ic_skip_next, "Proxima", Color.TRANSPARENT, TEXT, dp(48));
        repeatButton = iconButton(R.drawable.ic_repeat, "Repetir", Color.TRANSPARENT, MUTED, dp(44));
        controls.addView(shuffleButton);
        controls.addView(previousButton);
        controls.addView(playButton);
        controls.addView(nextButton);
        controls.addView(repeatButton);

        playButton.setOnClickListener(view -> togglePlayback());
        previousButton.setOnClickListener(view -> {
            if (playbackService != null) {
                playbackService.previous();
            }
        });
        nextButton.setOnClickListener(view -> {
            if (playbackService != null) {
                playbackService.next();
            }
        });
        shuffleButton.setOnClickListener(view -> {
            boolean enabled = playbackService == null || !playbackService.isShuffleEnabled();
            if (playbackService != null) {
                playbackService.setShuffleEnabled(enabled);
            }
            updatePlayer();
        });
        repeatButton.setOnClickListener(view -> {
            int next = playbackService == null ? 1 : (playbackService.getRepeatMode() + 1) % 3;
            if (playbackService != null) {
                playbackService.setRepeatMode(next);
            }
            updatePlayer();
        });
    }

    private void loadLibrary() {
        libraryLoading = true;
        permissionButton.setVisibility(View.GONE);
        emptyView.setText("Carregando musicas...");
        new Thread(() -> {
            ArrayList<Track> loaded = repository.loadAudio(MainActivity.this);
            runOnUiThread(() -> {
                allTracks.clear();
                allTracks.addAll(loaded);
                libraryLoading = false;
                refreshCurrentView();
            });
        }).start();
    }

    private void showPermissionState() {
        permissionButton.setVisibility(View.VISIBLE);
        addPlaylistButton.setVisibility(View.GONE);
        backPlaylistButton.setVisibility(View.GONE);
        searchInput.setVisibility(View.GONE);
        visibleTracks.clear();
        listView.setAdapter(trackAdapter);
        trackAdapter.setTracks(visibleTracks);
        subtitleView.setText("Permissao necessaria");
        emptyView.setText("Permita acesso ao audio para tocar as musicas baixadas.");
    }

    private void refreshCurrentView() {
        if (!hasLibraryPermission()) {
            showPermissionState();
            return;
        }
        permissionButton.setVisibility(View.GONE);
        if (libraryLoading) {
            emptyView.setText("Carregando musicas...");
            return;
        }
        refreshTabs();
        if (currentTab == TAB_LIBRARY) {
            showLibrary();
        } else if (currentTab == TAB_PLAYLISTS) {
            if (openPlaylist == null) {
                showPlaylists();
            } else {
                showPlaylistTracks(openPlaylist);
            }
        } else {
            showQueue();
        }
    }

    private void showLibrary() {
        openPlaylist = null;
        searchInput.setVisibility(View.VISIBLE);
        addPlaylistButton.setVisibility(View.GONE);
        backPlaylistButton.setVisibility(View.GONE);
        filterInto(visibleTracks, allTracks);
        listView.setAdapter(trackAdapter);
        trackAdapter.setTracks(visibleTracks);
        listView.setOnItemClickListener((parent, view, position, id) -> playFrom(visibleTracks, position));
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            showTrackOptions(visibleTracks.get(position), null);
            return true;
        });
        subtitleView.setText(allTracks.size() + " musicas no aparelho");
        emptyView.setText(searchQuery().isEmpty() ? "Nenhuma musica baixada encontrada." : "Nenhum resultado para essa busca.");
    }

    private void showPlaylists() {
        currentTab = TAB_PLAYLISTS;
        openPlaylist = null;
        refreshTabs();
        searchInput.setVisibility(View.GONE);
        addPlaylistButton.setVisibility(View.VISIBLE);
        backPlaylistButton.setVisibility(View.GONE);
        ArrayList<Playlist> playlists = playlistStore.getPlaylists();
        listView.setAdapter(playlistAdapter);
        playlistAdapter.setPlaylists(playlists);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            openPlaylist = playlists.get(position);
            showPlaylistTracks(openPlaylist);
        });
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            showPlaylistOptions(playlists.get(position));
            return true;
        });
        subtitleView.setText(playlists.size() + " playlists");
        emptyView.setText("Crie uma playlist para organizar suas musicas.");
    }

    private void showPlaylistTracks(Playlist playlist) {
        currentTab = TAB_PLAYLISTS;
        openPlaylist = playlist;
        refreshTabs();
        searchInput.setVisibility(View.VISIBLE);
        addPlaylistButton.setVisibility(View.GONE);
        backPlaylistButton.setVisibility(View.VISIBLE);
        ArrayList<Track> resolved = resolvePlaylist(playlist);
        filterInto(visibleTracks, resolved);
        listView.setAdapter(trackAdapter);
        trackAdapter.setTracks(visibleTracks);
        listView.setOnItemClickListener((parent, view, position, id) -> playFrom(visibleTracks, position));
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            showTrackOptions(visibleTracks.get(position), playlist);
            return true;
        });
        subtitleView.setText(playlist.name + " - " + resolved.size() + " musicas");
        emptyView.setText(searchQuery().isEmpty() ? "Playlist vazia." : "Nenhuma musica nessa playlist para a busca.");
    }

    private void showQueue() {
        refreshTabs();
        searchInput.setVisibility(View.GONE);
        addPlaylistButton.setVisibility(View.GONE);
        backPlaylistButton.setVisibility(View.GONE);
        ArrayList<Track> queue = playbackService == null ? new ArrayList<>() : playbackService.getQueue();
        visibleTracks.clear();
        visibleTracks.addAll(queue);
        listView.setAdapter(trackAdapter);
        trackAdapter.setTracks(visibleTracks);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (playbackService != null) {
                playbackService.playQueueIndex(position);
            }
        });
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            showTrackOptions(visibleTracks.get(position), null);
            return true;
        });
        subtitleView.setText(queue.size() + " faixas na fila");
        emptyView.setText("A fila aparece quando uma musica comeca a tocar.");
    }

    private void playFrom(ArrayList<Track> source, int index) {
        if (source.isEmpty()) {
            return;
        }
        startService(new Intent(this, MusicPlaybackService.class));
        ArrayList<Track> queue = new ArrayList<>(source);
        if (playbackService == null) {
            pendingQueue = queue;
            pendingIndex = index;
        } else {
            playbackService.setQueue(queue, index, true);
        }
    }

    private void togglePlayback() {
        if (playbackService != null && playbackService.hasTrack()) {
            playbackService.toggle();
        } else if (!visibleTracks.isEmpty() && currentTab != TAB_PLAYLISTS || (currentTab == TAB_PLAYLISTS && openPlaylist != null && !visibleTracks.isEmpty())) {
            playFrom(visibleTracks, 0);
        } else if (!allTracks.isEmpty()) {
            playFrom(allTracks, 0);
        }
    }

    private void updatePlayer() {
        if (playbackService == null || !playbackService.hasTrack()) {
            nowTitle.setText("Nada tocando");
            nowArtist.setText("Escolha uma musica da sua biblioteca");
            playButton.setImageResource(R.drawable.ic_play_arrow);
            elapsedView.setText("0:00");
            durationView.setText("0:00");
            seekBar.setMax(1000);
            if (!userSeeking) {
                seekBar.setProgress(0);
            }
            shuffleButton.setColorFilter(MUTED);
            repeatButton.setColorFilter(MUTED);
            return;
        }
        Track track = playbackService.getCurrentTrack();
        nowTitle.setText(track.title);
        nowArtist.setText(track.subtitle());
        playButton.setImageResource(playbackService.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play_arrow);
        int duration = Math.max(0, playbackService.getDuration());
        int position = Math.max(0, playbackService.getPosition());
        seekBar.setMax(Math.max(1000, duration));
        if (!userSeeking) {
            seekBar.setProgress(Math.min(position, seekBar.getMax()));
        }
        elapsedView.setText(formatMs(position));
        durationView.setText(formatMs(duration));
        shuffleButton.setColorFilter(playbackService.isShuffleEnabled() ? ACCENT : MUTED);
        repeatButton.setColorFilter(playbackService.getRepeatMode() == 0 ? MUTED : WARM);
        repeatButton.setContentDescription(playbackService.getRepeatMode() == 2 ? "Repetir uma" : "Repetir");
    }

    private void showTrackOptions(Track track, Playlist playlistContext) {
        PopupMenu popup = new PopupMenu(this, listView);
        Menu menu = popup.getMenu();
        menu.add(0, 1, 0, "Adicionar a playlist");
        menu.add(0, 2, 1, "Tocar a seguir");
        menu.add(0, 3, 2, "Favoritar");
        if (playlistContext != null) {
            menu.add(0, 4, 3, "Remover da playlist");
        }
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                showAddToPlaylistDialog(track);
            } else if (item.getItemId() == 2) {
                if (playbackService != null) {
                    playbackService.enqueueNext(track);
                    toast("Adicionada a fila");
                }
            } else if (item.getItemId() == 3) {
                toast(playlistStore.toggleFavorite(track) ? "Adicionada as favoritas" : "Removida das favoritas");
                refreshCurrentView();
            } else if (item.getItemId() == 4 && playlistContext != null) {
                playlistStore.removeTrack(playlistContext.id, track);
                openPlaylist = findPlaylist(playlistContext.id);
                if (openPlaylist != null) {
                    showPlaylistTracks(openPlaylist);
                }
            }
            return true;
        });
        popup.show();
    }

    private void showAddToPlaylistDialog(Track track) {
        ArrayList<Playlist> playlists = playlistStore.getPlaylists();
        String[] names = new String[playlists.size()];
        for (int i = 0; i < playlists.size(); i++) {
            names[i] = playlists.get(i).name;
        }
        new AlertDialog.Builder(this)
                .setTitle("Adicionar a playlist")
                .setItems(names, (dialog, which) -> {
                    playlistStore.addTrack(playlists.get(which).id, track);
                    toast("Musica adicionada");
                    refreshCurrentView();
                })
                .setNegativeButton("Nova", (dialog, which) -> showCreatePlaylistDialog(track))
                .show();
    }

    private void showCreatePlaylistDialog(Track trackToAdd) {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("Nome da playlist");
        input.setTextColor(TEXT);
        input.setHintTextColor(MUTED);
        input.setPadding(dp(16), dp(8), dp(16), dp(8));
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Nova playlist")
                .setView(input)
                .setPositiveButton("Criar", null)
                .setNegativeButton("Cancelar", null)
                .create();
        dialog.setOnShowListener(view -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(button -> {
            String name = input.getText().toString().trim();
            if (name.isEmpty()) {
                input.setError("Digite um nome");
                return;
            }
            Playlist playlist = playlistStore.createPlaylist(name);
            if (trackToAdd != null) {
                playlistStore.addTrack(playlist.id, trackToAdd);
            }
            dialog.dismiss();
            toast("Playlist criada");
            showPlaylists();
        }));
        dialog.show();
    }

    private void showPlaylistOptions(Playlist playlist) {
        PopupMenu popup = new PopupMenu(this, listView);
        popup.getMenu().add(0, 1, 0, "Renomear");
        if (!Playlist.FAVORITES_ID.equals(playlist.id)) {
            popup.getMenu().add(0, 2, 1, "Apagar");
        }
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                showRenamePlaylistDialog(playlist);
            } else if (item.getItemId() == 2) {
                new AlertDialog.Builder(this)
                        .setTitle("Apagar playlist")
                        .setMessage("A playlist sera removida, mas suas musicas continuam no aparelho.")
                        .setPositiveButton("Apagar", (dialog, which) -> {
                            playlistStore.deletePlaylist(playlist.id);
                            showPlaylists();
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
            }
            return true;
        });
        popup.show();
    }

    private void showRenamePlaylistDialog(Playlist playlist) {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(playlist.name);
        input.setSelectAllOnFocus(true);
        new AlertDialog.Builder(this)
                .setTitle("Renomear playlist")
                .setView(input)
                .setPositiveButton("Salvar", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        playlistStore.renamePlaylist(playlist.id, name);
                        showPlaylists();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private ArrayList<Track> resolvePlaylist(Playlist playlist) {
        HashMap<String, Track> byUri = new HashMap<>();
        for (Track track : allTracks) {
            byUri.put(track.uri, track);
        }
        ArrayList<Track> resolved = new ArrayList<>();
        for (String uri : playlist.trackUris) {
            Track track = byUri.get(uri);
            if (track != null) {
                resolved.add(track);
            }
        }
        return resolved;
    }

    private Playlist findPlaylist(String id) {
        for (Playlist playlist : playlistStore.getPlaylists()) {
            if (playlist.id.equals(id)) {
                return playlist;
            }
        }
        return null;
    }

    private void filterInto(ArrayList<Track> target, ArrayList<Track> source) {
        target.clear();
        String query = searchQuery();
        for (Track track : source) {
            if (query.isEmpty() || track.searchableText().contains(query)) {
                target.add(track);
            }
        }
    }

    private String searchQuery() {
        return searchInput.getText().toString().trim().toLowerCase(Locale.ROOT);
    }

    private boolean hasLibraryPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            return checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED;
        }
        return checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLibraryPermission() {
        ArrayList<String> permissions = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= 33) {
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO);
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        requestPermissions(permissions.toArray(new String[0]), REQUEST_LIBRARY);
    }

    private void refreshTabs() {
        styleTab(tabLibrary, currentTab == TAB_LIBRARY);
        styleTab(tabPlaylists, currentTab == TAB_PLAYLISTS);
        styleTab(tabQueue, currentTab == TAB_QUEUE);
    }

    private void startTicker() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updatePlayer();
                handler.postDelayed(this, 1000);
            }
        }, 1000);
    }

    private TextView tab(String text) {
        TextView tab = text(text, 14, MUTED, Typeface.BOLD);
        tab.setGravity(Gravity.CENTER);
        tab.setClickable(true);
        return tab;
    }

    private LinearLayout.LayoutParams tabParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1f);
        params.setMargins(dp(3), 0, dp(3), 0);
        return params;
    }

    private void styleTab(TextView tab, boolean selected) {
        tab.setTextColor(selected ? BG : MUTED);
        tab.setBackground(rounded(selected ? ACCENT : SURFACE_ALT, dp(21), selected ? 0 : 1, selected ? 0 : 0xFF2E3343));
    }

    private TextView text(String value, int sp, int color, int style) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(sp);
        text.setTextColor(color);
        text.setTypeface(Typeface.DEFAULT, style);
        text.setIncludeFontPadding(true);
        return text;
    }

    private ImageButton iconButton(int icon, String label, int background, int tint, int size) {
        ImageButton button = new ImageButton(this);
        button.setImageResource(icon);
        button.setColorFilter(tint);
        button.setContentDescription(label);
        button.setScaleType(ImageButton.ScaleType.CENTER);
        button.setBackground(background == Color.TRANSPARENT ? null : oval(background));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
        params.setMargins(dp(4), 0, dp(4), 0);
        button.setLayoutParams(params);
        return button;
    }

    private GradientDrawable rounded(int color, int radius, int strokeWidth, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        if (strokeWidth > 0) {
            drawable.setStroke(dp(strokeWidth), strokeColor);
        }
        return drawable;
    }

    private GradientDrawable oval(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);
        return drawable;
    }

    private GradientDrawable playerBackground() {
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[] { SURFACE, 0xFF1E2630, 0xFF1E1D24 });
        drawable.setCornerRadii(new float[] { dp(24), dp(24), dp(24), dp(24), 0, 0, 0, 0 });
        drawable.setStroke(dp(1), 0xFF2B3140);
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String formatMs(int milliseconds) {
        int totalSeconds = Math.max(0, milliseconds / 1000);
        return String.format(Locale.ROOT, "%d:%02d", totalSeconds / 60, totalSeconds % 60);
    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    private class TrackAdapter extends BaseAdapter {
        private final ArrayList<Track> tracks = new ArrayList<>();

        void setTracks(ArrayList<Track> newTracks) {
            tracks.clear();
            tracks.addAll(newTracks);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return tracks.size();
        }

        @Override
        public Track getItem(int position) {
            return tracks.get(position);
        }

        @Override
        public long getItemId(int position) {
            return tracks.get(position).id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TrackRow row;
            if (convertView == null) {
                row = new TrackRow();
                convertView = row.root;
                convertView.setTag(row);
            } else {
                row = (TrackRow) convertView.getTag();
            }
            Track track = getItem(position);
            row.bind(track);
            return convertView;
        }
    }

    private class TrackRow {
        final LinearLayout root;
        final TextView badge;
        final TextView title;
        final TextView subtitle;
        final TextView duration;
        final ImageButton more;

        TrackRow() {
            root = new LinearLayout(MainActivity.this);
            root.setGravity(Gravity.CENTER_VERTICAL);
            root.setPadding(dp(12), dp(8), dp(8), dp(8));
            root.setBackground(rounded(SURFACE, dp(8), 1, 0xFF272C38));
            root.setLayoutParams(new AbsListView.LayoutParams(-1, dp(76)));

            badge = text("M", 18, BG, Typeface.BOLD);
            badge.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(dp(50), dp(50));
            root.addView(badge, badgeParams);

            LinearLayout labels = new LinearLayout(MainActivity.this);
            labels.setOrientation(LinearLayout.VERTICAL);
            labels.setPadding(dp(12), 0, dp(8), 0);
            root.addView(labels, new LinearLayout.LayoutParams(0, -2, 1f));

            title = text("", 16, TEXT, Typeface.BOLD);
            title.setSingleLine(true);
            title.setEllipsize(TextUtils.TruncateAt.END);
            labels.addView(title);

            subtitle = text("", 13, MUTED, Typeface.NORMAL);
            subtitle.setSingleLine(true);
            subtitle.setEllipsize(TextUtils.TruncateAt.END);
            labels.addView(subtitle);

            duration = text("", 12, MUTED, Typeface.BOLD);
            root.addView(duration);

            more = iconButton(R.drawable.ic_more_vert, "Opcoes", Color.TRANSPARENT, MUTED, dp(42));
            more.setFocusable(false);
            root.addView(more);
        }

        void bind(Track track) {
            title.setText(track.title);
            subtitle.setText(track.subtitle());
            duration.setText(track.formattedDuration());
            String initial = track.title.trim().isEmpty() ? "M" : track.title.substring(0, 1).toUpperCase(Locale.ROOT);
            badge.setText(initial);
            badge.setBackground(albumBadge(track.albumId));
            more.setOnClickListener(view -> showTrackOptions(track, currentTab == TAB_PLAYLISTS ? openPlaylist : null));
        }
    }

    private GradientDrawable albumBadge(long seed) {
        int[] colors = new int[] { ACCENT, WARM, 0xFF7CC7FF, 0xFFE86BA5, 0xFFA7F070 };
        int color = colors[(int) (Math.abs(seed) % colors.length)];
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[] { color, 0xFFFFFFFF });
        drawable.setCornerRadius(dp(8));
        return drawable;
    }

    private class PlaylistAdapter extends BaseAdapter {
        private final ArrayList<Playlist> playlists = new ArrayList<>();

        void setPlaylists(ArrayList<Playlist> newPlaylists) {
            playlists.clear();
            playlists.addAll(newPlaylists);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return playlists.size();
        }

        @Override
        public Playlist getItem(int position) {
            return playlists.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            PlaylistRow row;
            if (convertView == null) {
                row = new PlaylistRow();
                convertView = row.root;
                convertView.setTag(row);
            } else {
                row = (PlaylistRow) convertView.getTag();
            }
            row.bind(getItem(position));
            return convertView;
        }
    }

    private class PlaylistRow {
        final LinearLayout root;
        final TextView badge;
        final TextView title;
        final TextView count;
        final ImageButton more;

        PlaylistRow() {
            root = new LinearLayout(MainActivity.this);
            root.setGravity(Gravity.CENTER_VERTICAL);
            root.setPadding(dp(12), dp(8), dp(8), dp(8));
            root.setBackground(rounded(SURFACE, dp(8), 1, 0xFF272C38));
            root.setLayoutParams(new AbsListView.LayoutParams(-1, dp(78)));

            badge = text("", 18, BG, Typeface.BOLD);
            badge.setGravity(Gravity.CENTER);
            badge.setBackground(rounded(WARM, dp(8), 0, 0));
            badge.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_playlist, 0, 0, 0);
            root.addView(badge, new LinearLayout.LayoutParams(dp(50), dp(50)));

            LinearLayout labels = new LinearLayout(MainActivity.this);
            labels.setOrientation(LinearLayout.VERTICAL);
            labels.setPadding(dp(12), 0, dp(8), 0);
            root.addView(labels, new LinearLayout.LayoutParams(0, -2, 1f));

            title = text("", 17, TEXT, Typeface.BOLD);
            title.setSingleLine(true);
            title.setEllipsize(TextUtils.TruncateAt.END);
            labels.addView(title);

            count = text("", 13, MUTED, Typeface.NORMAL);
            labels.addView(count);

            more = iconButton(R.drawable.ic_more_vert, "Opcoes", Color.TRANSPARENT, MUTED, dp(42));
            more.setFocusable(false);
            root.addView(more);
        }

        void bind(Playlist playlist) {
            title.setText(playlist.name);
            count.setText(playlist.trackUris.size() + " musicas");
            more.setOnClickListener(view -> showPlaylistOptions(playlist));
        }
    }
}

