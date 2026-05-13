package com.acezeradev.playermusic;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.ContentUris;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int REQUEST_LIBRARY = 713;
    private static final int TAB_LIBRARY = 0;
    private static final int TAB_PLAYLISTS = 1;
    private static final int TAB_QUEUE = 2;
    private static final int TAB_EXTENSIONS = 3;
    private static final String EXTENSION_PREFS = "player_music_extensions";
    private static final String KEY_CUSTOM_EXTENSION_PACKAGE = "custom_extension_package";
    private static final String SNAPTUBE_PACKAGE = "com.snaptube.premium";
    private static final int MAX_REDIRECTS = 5;

    private static final int BG = 0xFF090B10;
    private static final int SURFACE = 0xFF171A21;
    private static final int SURFACE_ALT = 0xFF232836;
    private static final int SURFACE_RAISED = 0xFF20242F;
    private static final int TEXT = 0xFFF5F7FA;
    private static final int MUTED = 0xFFA7ADB8;
    private static final int ACCENT = 0xFF3DDB9A;
    private static final int WARM = 0xFFFFB86C;
    private static final int BLUE = 0xFF7CC7FF;
    private static final int ROSE = 0xFFE86BA5;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final MusicRepository repository = new MusicRepository();
    private PlaylistStore playlistStore;
    private SharedPreferences extensionPrefs;

    private TextView subtitleView;
    private TextView emptyView;
    private TextView tabLibrary;
    private TextView tabPlaylists;
    private TextView tabQueue;
    private TextView tabExtensions;
    private TextView librarySummary;
    private TextView durationSummary;
    private TextView playlistSummary;
    private ImageView nowArtwork;
    private TextView nowArtInitial;
    private TextView nowTitle;
    private TextView nowArtist;
    private TextView nowAlbum;
    private TextView queueInfo;
    private LinearLayout miniPlayerPanel;
    private FrameLayout expandedPlayer;
    private ImageView expandedArtwork;
    private TextView expandedArtInitial;
    private TextView expandedTitle;
    private TextView expandedArtist;
    private TextView expandedAlbum;
    private TextView expandedElapsedView;
    private TextView expandedDurationView;
    private TextView expandedQueueInfo;
    private TextView elapsedView;
    private TextView durationView;
    private EditText searchInput;
    private Button permissionButton;
    private ImageButton backPlaylistButton;
    private ImageButton addPlaylistButton;
    private ImageButton playButton;
    private ImageButton shuffleButton;
    private ImageButton repeatButton;
    private ImageButton expandedPlayButton;
    private ImageButton expandedShuffleButton;
    private ImageButton expandedRepeatButton;
    private SeekBar seekBar;
    private SeekBar expandedSeekBar;
    private ListView listView;

    private final ArrayList<Track> allTracks = new ArrayList<>();
    private final ArrayList<Track> visibleTracks = new ArrayList<>();
    private final TrackAdapter trackAdapter = new TrackAdapter();
    private final PlaylistAdapter playlistAdapter = new PlaylistAdapter();
    private final ExtensionAdapter extensionAdapter = new ExtensionAdapter();

    private MusicPlaybackService playbackService;
    private boolean bound = false;
    private boolean userSeeking = false;
    private boolean expandedUserSeeking = false;
    private boolean libraryLoading = false;
    private int currentTab = TAB_LIBRARY;
    private Playlist openPlaylist;
    private ArrayList<Track> pendingQueue;
    private int pendingIndex = 0;
    private String lastRenderedTrackUri = "";
    private boolean lastRenderedPlaying = false;
    private float miniPlayerTouchStartY;
    private float expandedTouchStartY;

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
        extensionPrefs = getSharedPreferences(EXTENSION_PREFS, MODE_PRIVATE);
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
    protected void onResume() {
        super.onResume();
        if (currentTab == TAB_EXTENSIONS) {
            extensionAdapter.notifyDataSetChanged();
        }
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

    @Override
    public void onBackPressed() {
        if (expandedPlayer != null && expandedPlayer.getVisibility() == View.VISIBLE) {
            hideExpandedPlayer();
            return;
        }
        super.onBackPressed();
    }

    private void buildUi() {
        FrameLayout shell = new FrameLayout(this);
        shell.setBackgroundColor(BG);
        shell.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));
        shell.addView(root, new FrameLayout.LayoutParams(-1, -1));
        setContentView(shell);

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

        TextView title = text("PlayerMusic", 29, TEXT, Typeface.BOLD);
        titleBlock.addView(title);
        subtitleView = text("Biblioteca local pronta para tocar", 14, MUTED, Typeface.NORMAL);
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

        LinearLayout summaryRow = new LinearLayout(this);
        summaryRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams summaryParams = new LinearLayout.LayoutParams(-1, dp(38));
        summaryParams.setMargins(0, dp(14), 0, 0);
        header.addView(summaryRow, summaryParams);
        librarySummary = statPill("0 musicas", ACCENT);
        durationSummary = statPill("0 min", BLUE);
        playlistSummary = statPill("0 playlists", WARM);
        summaryRow.addView(librarySummary, statParams());
        summaryRow.addView(durationSummary, statParams());
        summaryRow.addView(playlistSummary, statParams());

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
        searchParams.setMargins(0, dp(14), 0, dp(12));
        header.addView(searchInput, searchParams);

        LinearLayout tabs = new LinearLayout(this);
        tabs.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(tabs, new LinearLayout.LayoutParams(-1, dp(42)));
        tabLibrary = tab("Musicas");
        tabPlaylists = tab("Playlists");
        tabQueue = tab("Fila");
        tabExtensions = tab("Extensoes");
        tabs.addView(tabLibrary, tabParams());
        tabs.addView(tabPlaylists, tabParams());
        tabs.addView(tabQueue, tabParams());
        tabs.addView(tabExtensions, tabParams());
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
        tabExtensions.setOnClickListener(view -> {
            openPlaylist = null;
            currentTab = TAB_EXTENSIONS;
            refreshCurrentView();
        });

        FrameLayout content = new FrameLayout(this);
        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1f));
        listView = new ListView(this);
        listView.setDivider(new ColorDrawable(Color.TRANSPARENT));
        listView.setDividerHeight(dp(8));
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
        buildExpandedPlayer(shell);
        refreshTabs();
        updateLibrarySummary();
    }

    private void buildPlayer(LinearLayout root) {
        LinearLayout panel = new LinearLayout(this);
        miniPlayerPanel = panel;
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(18), dp(16), dp(18), dp(14));
        panel.setBackground(playerBackground());
        panel.setClickable(true);
        panel.setOnClickListener(view -> showExpandedPlayer());
        panel.setOnTouchListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                miniPlayerTouchStartY = event.getRawY();
            } else if (event.getAction() == MotionEvent.ACTION_UP && miniPlayerTouchStartY - event.getRawY() > dp(40)) {
                showExpandedPlayer();
                return true;
            }
            return false;
        });
        root.addView(panel, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout nowRow = new LinearLayout(this);
        nowRow.setGravity(Gravity.CENTER_VERTICAL);
        panel.addView(nowRow, new LinearLayout.LayoutParams(-1, -2));

        FrameLayout artworkFrame = new FrameLayout(this);
        artworkFrame.setBackground(rounded(SURFACE_RAISED, dp(18), 1, 0xFF303746));
        LinearLayout.LayoutParams artworkParams = new LinearLayout.LayoutParams(dp(76), dp(76));
        nowRow.addView(artworkFrame, artworkParams);

        nowArtwork = new ImageView(this);
        nowArtwork.setScaleType(ImageView.ScaleType.CENTER_CROP);
        nowArtwork.setVisibility(View.GONE);
        artworkFrame.addView(nowArtwork, new FrameLayout.LayoutParams(-1, -1));

        nowArtInitial = text("M", 30, BG, Typeface.BOLD);
        nowArtInitial.setGravity(Gravity.CENTER);
        nowArtInitial.setBackground(albumBadge(0));
        artworkFrame.addView(nowArtInitial, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout nowMeta = new LinearLayout(this);
        nowMeta.setOrientation(LinearLayout.VERTICAL);
        nowMeta.setPadding(dp(14), 0, dp(10), 0);
        nowRow.addView(nowMeta, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView nowLabel = text("TOCANDO AGORA", 11, ACCENT, Typeface.BOLD);
        nowMeta.addView(nowLabel);

        nowTitle = text("Nada tocando", 18, TEXT, Typeface.BOLD);
        nowTitle.setSingleLine(true);
        nowTitle.setEllipsize(TextUtils.TruncateAt.END);
        nowMeta.addView(nowTitle);

        nowArtist = text("Escolha uma musica da sua biblioteca", 13, MUTED, Typeface.NORMAL);
        nowArtist.setSingleLine(true);
        nowArtist.setEllipsize(TextUtils.TruncateAt.END);
        nowMeta.addView(nowArtist);

        nowAlbum = text("Player parado", 12, 0xFF858C99, Typeface.NORMAL);
        nowAlbum.setSingleLine(true);
        nowAlbum.setEllipsize(TextUtils.TruncateAt.END);
        nowMeta.addView(nowAlbum);

        queueInfo = text("Fila vazia", 12, TEXT, Typeface.BOLD);
        queueInfo.setGravity(Gravity.CENTER);
        queueInfo.setPadding(dp(10), 0, dp(10), 0);
        queueInfo.setBackground(rounded(0x263DDB9A, dp(16), 1, 0x443DDB9A));
        nowRow.addView(queueInfo, new LinearLayout.LayoutParams(-2, dp(32)));

        seekBar = new SeekBar(this);
        seekBar.setMax(1000);
        seekBar.setProgress(0);
        seekBar.setProgressTintList(ColorStateList.valueOf(ACCENT));
        seekBar.setProgressBackgroundTintList(ColorStateList.valueOf(0xFF333948));
        seekBar.setThumbTintList(ColorStateList.valueOf(TEXT));
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
        LinearLayout.LayoutParams seekParams = new LinearLayout.LayoutParams(-1, dp(38));
        seekParams.setMargins(0, dp(10), 0, 0);
        panel.addView(seekBar, seekParams);

        LinearLayout timeRow = new LinearLayout(this);
        timeRow.setGravity(Gravity.CENTER_VERTICAL);
        panel.addView(timeRow, new LinearLayout.LayoutParams(-1, -2));
        elapsedView = text("0:00", 12, MUTED, Typeface.NORMAL);
        durationView = text("0:00", 12, MUTED, Typeface.NORMAL);
        timeRow.addView(elapsedView, new LinearLayout.LayoutParams(0, -2, 1f));
        timeRow.addView(durationView);

        LinearLayout controls = new LinearLayout(this);
        controls.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams controlsParams = new LinearLayout.LayoutParams(-1, dp(60));
        controlsParams.setMargins(0, dp(2), 0, 0);
        panel.addView(controls, controlsParams);

        shuffleButton = iconButton(R.drawable.ic_shuffle, "Aleatorio", 0x1FFFFFFF, MUTED, dp(44));
        ImageButton previousButton = iconButton(R.drawable.ic_skip_previous, "Anterior", SURFACE_RAISED, TEXT, dp(48));
        playButton = iconButton(R.drawable.ic_play_arrow, "Tocar", ACCENT, BG, dp(58));
        ImageButton nextButton = iconButton(R.drawable.ic_skip_next, "Proxima", SURFACE_RAISED, TEXT, dp(48));
        repeatButton = iconButton(R.drawable.ic_repeat, "Repetir", 0x1FFFFFFF, MUTED, dp(44));
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

    private void buildExpandedPlayer(FrameLayout shell) {
        expandedPlayer = new FrameLayout(this);
        expandedPlayer.setBackgroundColor(BG);
        expandedPlayer.setVisibility(View.GONE);
        expandedPlayer.setClickable(true);
        expandedPlayer.setOnTouchListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                expandedTouchStartY = event.getRawY();
            } else if (event.getAction() == MotionEvent.ACTION_UP && event.getRawY() - expandedTouchStartY > dp(60)) {
                hideExpandedPlayer();
                return true;
            }
            return false;
        });
        shell.addView(expandedPlayer, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(18), dp(18), dp(18), dp(18));
        expandedPlayer.addView(page, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        page.addView(header, new LinearLayout.LayoutParams(-1, dp(48)));

        ImageButton closeButton = iconButton(R.drawable.ic_arrow_back, "Fechar player", SURFACE_ALT, TEXT, dp(42));
        closeButton.setRotation(-90f);
        closeButton.setOnClickListener(view -> hideExpandedPlayer());
        header.addView(closeButton);

        TextView headerTitle = text("PlayerMusic", 18, TEXT, Typeface.BOLD);
        headerTitle.setGravity(Gravity.CENTER);
        header.addView(headerTitle, new LinearLayout.LayoutParams(0, -2, 1f));

        expandedQueueInfo = text("Fila vazia", 12, TEXT, Typeface.BOLD);
        expandedQueueInfo.setGravity(Gravity.CENTER);
        expandedQueueInfo.setPadding(dp(10), 0, dp(10), 0);
        expandedQueueInfo.setBackground(rounded(0x263DDB9A, dp(16), 1, 0x443DDB9A));
        header.addView(expandedQueueInfo, new LinearLayout.LayoutParams(-2, dp(32)));

        FrameLayout artworkFrame = new FrameLayout(this);
        artworkFrame.setBackground(expandedArtworkBackground());
        LinearLayout.LayoutParams artworkParams = new LinearLayout.LayoutParams(dp(270), dp(270));
        artworkParams.gravity = Gravity.CENTER_HORIZONTAL;
        artworkParams.setMargins(0, dp(22), 0, dp(20));
        page.addView(artworkFrame, artworkParams);

        expandedArtwork = new ImageView(this);
        expandedArtwork.setScaleType(ImageView.ScaleType.CENTER_CROP);
        expandedArtwork.setVisibility(View.GONE);
        artworkFrame.addView(expandedArtwork, new FrameLayout.LayoutParams(-1, -1));

        expandedArtInitial = text("M", 64, BG, Typeface.BOLD);
        expandedArtInitial.setGravity(Gravity.CENTER);
        expandedArtInitial.setBackground(albumBadge(0));
        artworkFrame.addView(expandedArtInitial, new FrameLayout.LayoutParams(-1, -1));

        expandedTitle = text("Nada tocando", 24, TEXT, Typeface.BOLD);
        expandedTitle.setGravity(Gravity.CENTER);
        expandedTitle.setSingleLine(true);
        expandedTitle.setEllipsize(TextUtils.TruncateAt.END);
        page.addView(expandedTitle, new LinearLayout.LayoutParams(-1, -2));

        expandedArtist = text("Escolha uma musica da sua biblioteca", 15, MUTED, Typeface.NORMAL);
        expandedArtist.setGravity(Gravity.CENTER);
        expandedArtist.setSingleLine(true);
        expandedArtist.setEllipsize(TextUtils.TruncateAt.END);
        page.addView(expandedArtist, new LinearLayout.LayoutParams(-1, -2));

        expandedAlbum = text("Player parado", 13, 0xFF858C99, Typeface.NORMAL);
        expandedAlbum.setGravity(Gravity.CENTER);
        expandedAlbum.setSingleLine(true);
        expandedAlbum.setEllipsize(TextUtils.TruncateAt.END);
        page.addView(expandedAlbum, new LinearLayout.LayoutParams(-1, -2));

        expandedSeekBar = new SeekBar(this);
        expandedSeekBar.setMax(1000);
        expandedSeekBar.setProgress(0);
        expandedSeekBar.setProgressTintList(ColorStateList.valueOf(ACCENT));
        expandedSeekBar.setProgressBackgroundTintList(ColorStateList.valueOf(0xFF333948));
        expandedSeekBar.setThumbTintList(ColorStateList.valueOf(TEXT));
        expandedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    expandedElapsedView.setText(formatMs(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                expandedUserSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                expandedUserSeeking = false;
                if (playbackService != null) {
                    playbackService.seekTo(seekBar.getProgress());
                }
            }
        });
        LinearLayout.LayoutParams expandedSeekParams = new LinearLayout.LayoutParams(-1, dp(42));
        expandedSeekParams.setMargins(0, dp(26), 0, 0);
        page.addView(expandedSeekBar, expandedSeekParams);

        LinearLayout timeRow = new LinearLayout(this);
        timeRow.setGravity(Gravity.CENTER_VERTICAL);
        page.addView(timeRow, new LinearLayout.LayoutParams(-1, -2));
        expandedElapsedView = text("0:00", 12, MUTED, Typeface.NORMAL);
        expandedDurationView = text("0:00", 12, MUTED, Typeface.NORMAL);
        timeRow.addView(expandedElapsedView, new LinearLayout.LayoutParams(0, -2, 1f));
        timeRow.addView(expandedDurationView);

        LinearLayout controls = new LinearLayout(this);
        controls.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams controlsParams = new LinearLayout.LayoutParams(-1, dp(82));
        controlsParams.setMargins(0, dp(12), 0, 0);
        page.addView(controls, controlsParams);

        expandedShuffleButton = iconButton(R.drawable.ic_shuffle, "Aleatorio", 0x1FFFFFFF, MUTED, dp(46));
        ImageButton previousButton = iconButton(R.drawable.ic_skip_previous, "Anterior", SURFACE_RAISED, TEXT, dp(54));
        expandedPlayButton = iconButton(R.drawable.ic_play_arrow, "Tocar", ACCENT, BG, dp(68));
        ImageButton nextButton = iconButton(R.drawable.ic_skip_next, "Proxima", SURFACE_RAISED, TEXT, dp(54));
        expandedRepeatButton = iconButton(R.drawable.ic_repeat, "Repetir", 0x1FFFFFFF, MUTED, dp(46));
        controls.addView(expandedShuffleButton);
        controls.addView(previousButton);
        controls.addView(expandedPlayButton);
        controls.addView(nextButton);
        controls.addView(expandedRepeatButton);

        expandedPlayButton.setOnClickListener(view -> togglePlayback());
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
        expandedShuffleButton.setOnClickListener(view -> {
            boolean enabled = playbackService == null || !playbackService.isShuffleEnabled();
            if (playbackService != null) {
                playbackService.setShuffleEnabled(enabled);
            }
            updatePlayer();
        });
        expandedRepeatButton.setOnClickListener(view -> {
            int next = playbackService == null ? 1 : (playbackService.getRepeatMode() + 1) % 3;
            if (playbackService != null) {
                playbackService.setRepeatMode(next);
            }
            updatePlayer();
        });

        TextView sourceChip = text("Conteudo local importado pelo PlayerMusic", 12, MUTED, Typeface.BOLD);
        sourceChip.setGravity(Gravity.CENTER);
        sourceChip.setPadding(dp(14), 0, dp(14), 0);
        sourceChip.setBackground(rounded(0x1AFFFFFF, dp(17), 1, 0x22FFFFFF));
        LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(-2, dp(34));
        chipParams.gravity = Gravity.CENTER_HORIZONTAL;
        chipParams.setMargins(0, dp(6), 0, 0);
        page.addView(sourceChip, chipParams);
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
                updateLibrarySummary();
                refreshCurrentView();
            });
        }).start();
    }

    private void showPermissionState() {
        permissionButton.setVisibility(View.VISIBLE);
        addPlaylistButton.setVisibility(View.GONE);
        backPlaylistButton.setVisibility(View.GONE);
        searchInput.setVisibility(View.GONE);
        updateLibrarySummary();
        visibleTracks.clear();
        listView.setAdapter(trackAdapter);
        trackAdapter.setTracks(visibleTracks);
        subtitleView.setText("Permissao necessaria");
        emptyView.setText("Permita acesso ao audio para tocar as musicas baixadas.");
    }

    private void refreshCurrentView() {
        if (currentTab == TAB_EXTENSIONS) {
            showExtensions();
            return;
        }
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
        } else if (currentTab == TAB_QUEUE) {
            showQueue();
        } else {
            showExtensions();
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
        addPlaylistButton.setContentDescription("Nova playlist");
        addPlaylistButton.setOnClickListener(view -> showCreatePlaylistDialog(null));
        backPlaylistButton.setVisibility(View.GONE);
        ArrayList<Playlist> playlists = playlistStore.getPlaylists();
        updateLibrarySummary();
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

    private void showExtensions() {
        currentTab = TAB_EXTENSIONS;
        openPlaylist = null;
        refreshTabs();
        searchInput.setVisibility(View.GONE);
        addPlaylistButton.setVisibility(View.VISIBLE);
        addPlaylistButton.setContentDescription("Baixar por link");
        addPlaylistButton.setOnClickListener(view -> showImportLinkDialog());
        backPlaylistButton.setVisibility(View.GONE);
        listView.setAdapter(extensionAdapter);
        extensionAdapter.notifyDataSetChanged();
        listView.setOnItemClickListener((parent, view, position, id) -> handleExtensionAction(position));
        listView.setOnItemLongClickListener(null);
        subtitleView.setText("Extensoes e importacao local");
        emptyView.setText("");
    }

    private void handleExtensionAction(int position) {
        if (position == 0) {
            showImportLinkDialog();
        } else if (position == 1) {
            openExternalExtension(SNAPTUBE_PACKAGE, "Snaptube");
        } else if (position == 2) {
            String packageName = customExtensionPackage();
            if (packageName.isEmpty() || !isPackageAvailable(packageName)) {
                showExtensionPackageDialog();
            } else {
                openExternalExtension(packageName, packageName);
            }
        } else if (position == 3) {
            if (hasLibraryPermission()) {
                loadLibrary();
                toast("Biblioteca atualizando");
            } else {
                requestLibraryPermission();
            }
        } else if (position == 4) {
            showExtensionSafetyDialog();
        }
    }

    private void showImportLinkDialog() {
        EditText input = new EditText(this);
        input.setSingleLine(false);
        input.setMinLines(2);
        input.setMaxLines(4);
        input.setHint("https://site.com/musica.mp3");
        input.setTextColor(TEXT);
        input.setHintTextColor(MUTED);
        input.setPadding(dp(16), dp(8), dp(16), dp(8));
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Baixar audio por link")
                .setMessage("Cole um link direto de audio. Links do YouTube serao abertos no app oficial, sem download.")
                .setView(input)
                .setPositiveButton("Baixar", null)
                .setNegativeButton("Cancelar", null)
                .create();
        dialog.setOnShowListener(view -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(button -> {
            String link = input.getText().toString().trim();
            if (link.isEmpty()) {
                input.setError("Cole um link");
                return;
            }
            dialog.dismiss();
            importAudioLink(link);
        }));
        dialog.show();
    }

    private void importAudioLink(String link) {
        Uri uri = Uri.parse(link);
        String scheme = uri.getScheme();
        if (!"https".equalsIgnoreCase(scheme) && !"http".equalsIgnoreCase(scheme)) {
            toast("Use um link http ou https");
            return;
        }
        if (isYouTubeUrl(uri)) {
            openExternalUrl(link);
            toast("YouTube aberto sem download");
            return;
        }
        toast("Baixando audio...");
        new Thread(() -> {
            try {
                String savedName = downloadAudioFile(link);
                runOnUiThread(() -> {
                    toast("Audio salvo: " + savedName);
                    if (hasLibraryPermission()) {
                        loadLibrary();
                    } else {
                        requestLibraryPermission();
                    }
                });
            } catch (Exception error) {
                runOnUiThread(() -> showDownloadError(error.getMessage()));
            }
        }).start();
    }

    private boolean isYouTubeUrl(Uri uri) {
        String host = uri.getHost();
        if (host == null) {
            return false;
        }
        String normalized = host.toLowerCase(Locale.ROOT);
        return normalized.equals("youtu.be") || normalized.endsWith(".youtube.com") || normalized.equals("youtube.com");
    }

    private void openExternalUrl(String link) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link)));
        } catch (RuntimeException error) {
            toast("Nao foi possivel abrir o link");
        }
    }

    private void showDownloadError(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Download nao iniciado")
                .setMessage(message == null || message.isEmpty() ? "Use um link direto de audio, como mp3, m4a, wav, ogg ou flac." : message)
                .setPositiveButton("Entendi", null)
                .show();
    }

    private String downloadAudioFile(String link) throws Exception {
        HttpURLConnection connection = openConnectionFollowingRedirects(link);
        try {
            String contentType = cleanContentType(connection.getContentType());
            String fileName = fileNameFromLink(link, contentType);
            if (!isAudioResponse(fileName, contentType)) {
                throw new Exception("Esse link nao aponta diretamente para um arquivo de audio. Use mp3, m4a, wav, ogg, flac, aac, opus ou webm.");
            }
            String mimeType = mimeTypeFor(fileName, contentType);
            if (!isAudioFileName(fileName)) {
                fileName = fileName + "." + extensionForMime(mimeType);
            }
            try (InputStream input = connection.getInputStream()) {
                saveAudioStream(input, fileName, mimeType);
            }
            return fileName;
        } finally {
            connection.disconnect();
        }
    }

    private HttpURLConnection openConnectionFollowingRedirects(String link) throws Exception {
        URL url = new URL(link);
        for (int i = 0; i < MAX_REDIRECTS; i++) {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);
            connection.setRequestProperty("User-Agent", "PlayerMusic/1.3");
            int code = connection.getResponseCode();
            if (code >= 300 && code < 400) {
                String location = connection.getHeaderField("Location");
                connection.disconnect();
                if (location == null || location.trim().isEmpty()) {
                    throw new Exception("O link redirecionou sem informar o destino.");
                }
                url = new URL(url, location);
                continue;
            }
            if (code < 200 || code >= 300) {
                connection.disconnect();
                throw new Exception("O servidor respondeu com erro " + code + ".");
            }
            return connection;
        }
        throw new Exception("O link redirecionou muitas vezes.");
    }

    private void saveAudioStream(InputStream input, String fileName, String mimeType) throws Exception {
        if (Build.VERSION.SDK_INT >= 29) {
            saveAudioWithMediaStore(input, fileName, mimeType);
        } else {
            saveAudioLegacy(input, fileName, mimeType);
        }
    }

    private void saveAudioWithMediaStore(InputStream input, String fileName, String mimeType) throws Exception {
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/PlayerMusic");
        values.put(MediaStore.MediaColumns.IS_PENDING, 1);
        Uri audioUri = getContentResolver().insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values);
        if (audioUri == null) {
            throw new Exception("Nao foi possivel criar o arquivo de audio.");
        }
        boolean saved = false;
        try (OutputStream output = getContentResolver().openOutputStream(audioUri)) {
            if (output == null) {
                throw new Exception("Nao foi possivel abrir o arquivo de destino.");
            }
            copyStream(input, output);
            saved = true;
        } finally {
            values.clear();
            values.put(MediaStore.MediaColumns.IS_PENDING, 0);
            getContentResolver().update(audioUri, values, null, null);
            if (!saved) {
                getContentResolver().delete(audioUri, null, null);
            }
        }
    }

    private void saveAudioLegacy(InputStream input, String fileName, String mimeType) throws Exception {
        File baseDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        if (baseDir == null) {
            baseDir = getFilesDir();
        }
        File dir = new File(baseDir, "PlayerMusic");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new Exception("Nao foi possivel criar a pasta de musicas.");
        }
        File target = uniqueFile(dir, fileName);
        try (OutputStream output = new FileOutputStream(target)) {
            copyStream(input, output);
        }
        MediaScannerConnection.scanFile(this, new String[] { target.getAbsolutePath() }, new String[] { mimeType }, null);
    }

    private void copyStream(InputStream input, OutputStream output) throws Exception {
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        output.flush();
    }

    private File uniqueFile(File dir, String fileName) {
        File target = new File(dir, fileName);
        if (!target.exists()) {
            return target;
        }
        String base = fileName;
        String extension = "";
        int dot = fileName.lastIndexOf('.');
        if (dot > 0) {
            base = fileName.substring(0, dot);
            extension = fileName.substring(dot);
        }
        int index = 1;
        while (target.exists()) {
            target = new File(dir, base + "-" + index + extension);
            index++;
        }
        return target;
    }

    private String fileNameFromLink(String link, String contentType) {
        String name = Uri.parse(link).getLastPathSegment();
        if (name == null || name.trim().isEmpty()) {
            name = "playermusic-" + System.currentTimeMillis();
        }
        try {
            name = URLDecoder.decode(name, "UTF-8");
        } catch (Exception ignored) {
        }
        name = name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        if (name.isEmpty()) {
            name = "playermusic-" + System.currentTimeMillis();
        }
        if (!isAudioFileName(name) && contentType != null && contentType.startsWith("audio/")) {
            name = name + "." + extensionForMime(contentType);
        }
        return name;
    }

    private boolean isAudioResponse(String fileName, String contentType) {
        return (contentType != null && contentType.startsWith("audio/")) || isAudioFileName(fileName);
    }

    private boolean isAudioFileName(String fileName) {
        String lower = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        return lower.endsWith(".mp3")
                || lower.endsWith(".m4a")
                || lower.endsWith(".wav")
                || lower.endsWith(".ogg")
                || lower.endsWith(".flac")
                || lower.endsWith(".aac")
                || lower.endsWith(".opus")
                || lower.endsWith(".amr")
                || lower.endsWith(".3gp")
                || lower.endsWith(".webm");
    }

    private String cleanContentType(String contentType) {
        if (contentType == null) {
            return null;
        }
        return contentType.split(";")[0].trim().toLowerCase(Locale.ROOT);
    }

    private String mimeTypeFor(String fileName, String contentType) {
        if (contentType != null && contentType.startsWith("audio/")) {
            return contentType;
        }
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".m4a")) {
            return "audio/mp4";
        }
        if (lower.endsWith(".wav")) {
            return "audio/wav";
        }
        if (lower.endsWith(".ogg")) {
            return "audio/ogg";
        }
        if (lower.endsWith(".flac")) {
            return "audio/flac";
        }
        if (lower.endsWith(".aac")) {
            return "audio/aac";
        }
        if (lower.endsWith(".opus")) {
            return "audio/opus";
        }
        if (lower.endsWith(".amr")) {
            return "audio/amr";
        }
        if (lower.endsWith(".3gp")) {
            return "audio/3gpp";
        }
        if (lower.endsWith(".webm")) {
            return "audio/webm";
        }
        return "audio/mpeg";
    }

    private String extensionForMime(String mimeType) {
        if (mimeType == null) {
            return "mp3";
        }
        String normalized = mimeType.toLowerCase(Locale.ROOT);
        if (normalized.contains("mp4") || normalized.contains("m4a")) {
            return "m4a";
        }
        if (normalized.contains("wav")) {
            return "wav";
        }
        if (normalized.contains("ogg")) {
            return "ogg";
        }
        if (normalized.contains("flac")) {
            return "flac";
        }
        if (normalized.contains("aac")) {
            return "aac";
        }
        if (normalized.contains("opus")) {
            return "opus";
        }
        if (normalized.contains("webm")) {
            return "webm";
        }
        return "mp3";
    }

    private void openExternalExtension(String packageName, String label) {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
        if (launchIntent == null) {
            toast(label + " nao encontrado");
            return;
        }
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(launchIntent);
    }

    private void showExtensionPackageDialog() {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint(SNAPTUBE_PACKAGE);
        input.setText(customExtensionPackage());
        input.setSelectAllOnFocus(true);
        input.setTextColor(TEXT);
        input.setHintTextColor(MUTED);
        input.setPadding(dp(16), dp(8), dp(16), dp(8));
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Cadastrar extensao")
                .setView(input)
                .setPositiveButton("Salvar", null)
                .setNegativeButton("Cancelar", null)
                .setNeutralButton("Limpar", null)
                .create();
        dialog.setOnShowListener(view -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(button -> {
                String packageName = input.getText().toString().trim();
                if (packageName.isEmpty()) {
                    input.setError("Digite o pacote Android");
                    return;
                }
                extensionPrefs.edit().putString(KEY_CUSTOM_EXTENSION_PACKAGE, packageName).apply();
                dialog.dismiss();
                showExtensions();
            });
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(button -> {
                extensionPrefs.edit().remove(KEY_CUSTOM_EXTENSION_PACKAGE).apply();
                dialog.dismiss();
                showExtensions();
            });
        });
        dialog.show();
    }

    private void showExtensionSafetyDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Uso responsavel")
                .setMessage("Use extensoes externas apenas para baixar ou importar audio que voce tem direito de usar. O PlayerMusic atualiza e toca os arquivos locais encontrados no aparelho.")
                .setPositiveButton("Entendi", null)
                .show();
    }

    private String customExtensionPackage() {
        if (extensionPrefs == null) {
            return "";
        }
        return extensionPrefs.getString(KEY_CUSTOM_EXTENSION_PACKAGE, "");
    }

    private boolean isPackageAvailable(String packageName) {
        if (packageName == null || packageName.trim().isEmpty()) {
            return false;
        }
        return getPackageManager().getLaunchIntentForPackage(packageName) != null;
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
        } else if (!visibleTracks.isEmpty() && (currentTab != TAB_PLAYLISTS || openPlaylist != null)) {
            playFrom(visibleTracks, 0);
        } else if (!allTracks.isEmpty()) {
            playFrom(allTracks, 0);
        }
    }

    private void showExpandedPlayer() {
        if (expandedPlayer != null) {
            updatePlayer();
            expandedPlayer.setVisibility(View.VISIBLE);
        }
    }

    private void hideExpandedPlayer() {
        if (expandedPlayer != null) {
            expandedPlayer.setVisibility(View.GONE);
        }
    }

    private void updatePlayer() {
        if (playbackService == null || !playbackService.hasTrack()) {
            nowTitle.setText("Nada tocando");
            nowArtist.setText("Escolha uma musica da sua biblioteca");
            nowAlbum.setText("Player parado");
            queueInfo.setText("Fila vazia");
            expandedTitle.setText("Nada tocando");
            expandedArtist.setText("Escolha uma musica da sua biblioteca");
            expandedAlbum.setText("Player parado");
            expandedQueueInfo.setText("Fila vazia");
            updateArtwork(null);
            updateExpandedArtwork(null);
            playButton.setImageResource(R.drawable.ic_play_arrow);
            expandedPlayButton.setImageResource(R.drawable.ic_play_arrow);
            elapsedView.setText("0:00");
            durationView.setText("0:00");
            expandedElapsedView.setText("0:00");
            expandedDurationView.setText("0:00");
            seekBar.setMax(1000);
            expandedSeekBar.setMax(1000);
            if (!userSeeking) {
                seekBar.setProgress(0);
            }
            if (!expandedUserSeeking) {
                expandedSeekBar.setProgress(0);
            }
            shuffleButton.setColorFilter(MUTED);
            repeatButton.setColorFilter(MUTED);
            expandedShuffleButton.setColorFilter(MUTED);
            expandedRepeatButton.setColorFilter(MUTED);
            syncListPlaybackState(null, false);
            return;
        }
        Track track = playbackService.getCurrentTrack();
        boolean playing = playbackService.isPlaying();
        nowTitle.setText(track.title);
        nowArtist.setText(track.subtitle());
        nowAlbum.setText(track.album);
        queueInfo.setText(queueLabel());
        expandedTitle.setText(track.title);
        expandedArtist.setText(track.subtitle());
        expandedAlbum.setText(track.album);
        expandedQueueInfo.setText(queueLabel());
        updateArtwork(track);
        updateExpandedArtwork(track);
        playButton.setImageResource(playing ? R.drawable.ic_pause : R.drawable.ic_play_arrow);
        expandedPlayButton.setImageResource(playing ? R.drawable.ic_pause : R.drawable.ic_play_arrow);
        int duration = Math.max(0, playbackService.getDuration());
        int position = Math.max(0, playbackService.getPosition());
        seekBar.setMax(Math.max(1000, duration));
        expandedSeekBar.setMax(Math.max(1000, duration));
        if (!userSeeking) {
            seekBar.setProgress(Math.min(position, seekBar.getMax()));
        }
        if (!expandedUserSeeking) {
            expandedSeekBar.setProgress(Math.min(position, expandedSeekBar.getMax()));
        }
        elapsedView.setText(formatMs(position));
        durationView.setText(formatMs(duration));
        expandedElapsedView.setText(formatMs(position));
        expandedDurationView.setText(formatMs(duration));
        shuffleButton.setColorFilter(playbackService.isShuffleEnabled() ? ACCENT : MUTED);
        repeatButton.setColorFilter(playbackService.getRepeatMode() == 0 ? MUTED : WARM);
        repeatButton.setContentDescription(playbackService.getRepeatMode() == 2 ? "Repetir uma" : "Repetir");
        expandedShuffleButton.setColorFilter(playbackService.isShuffleEnabled() ? ACCENT : MUTED);
        expandedRepeatButton.setColorFilter(playbackService.getRepeatMode() == 0 ? MUTED : WARM);
        expandedRepeatButton.setContentDescription(playbackService.getRepeatMode() == 2 ? "Repetir uma" : "Repetir");
        syncListPlaybackState(track, playing);
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

    private void updateLibrarySummary() {
        if (librarySummary == null) {
            return;
        }
        long totalDuration = 0;
        for (Track track : allTracks) {
            totalDuration += Math.max(0, track.duration);
        }
        int playlistCount = playlistStore == null ? 0 : playlistStore.getPlaylists().size();
        librarySummary.setText(countLabel(allTracks.size(), "musica", "musicas"));
        durationSummary.setText(formatLibraryDuration(totalDuration));
        playlistSummary.setText(countLabel(playlistCount, "playlist", "playlists"));
    }

    private String countLabel(int count, String singular, String plural) {
        return count + " " + (count == 1 ? singular : plural);
    }

    private String formatLibraryDuration(long milliseconds) {
        long minutes = Math.max(0, milliseconds / 60000);
        if (minutes < 60) {
            return minutes + " min";
        }
        long hours = minutes / 60;
        long rest = minutes % 60;
        return rest == 0 ? hours + " h" : hours + " h " + rest + " min";
    }

    private String queueLabel() {
        if (playbackService == null) {
            return "Fila vazia";
        }
        ArrayList<Track> queue = playbackService.getQueue();
        if (queue.isEmpty()) {
            return "Fila vazia";
        }
        return (playbackService.getCurrentIndex() + 1) + "/" + queue.size();
    }

    private void updateArtwork(Track track) {
        setArtwork(nowArtwork, nowArtInitial, track);
    }

    private void updateExpandedArtwork(Track track) {
        setArtwork(expandedArtwork, expandedArtInitial, track);
    }

    private void setArtwork(ImageView artwork, TextView initialView, Track track) {
        if (track == null) {
            artwork.setImageDrawable(null);
            artwork.setVisibility(View.GONE);
            initialView.setText("M");
            initialView.setBackground(albumBadge(0));
            initialView.setVisibility(View.VISIBLE);
            return;
        }
        initialView.setText(trackInitial(track));
        initialView.setBackground(albumBadge(track.albumId));
        artwork.setImageDrawable(null);
        if (track.albumId > 0) {
            try {
                artwork.setImageURI(albumArtUri(track.albumId));
            } catch (RuntimeException ignored) {
                artwork.setImageDrawable(null);
            }
        }
        boolean hasArtwork = artwork.getDrawable() != null;
        artwork.setVisibility(hasArtwork ? View.VISIBLE : View.GONE);
        initialView.setVisibility(hasArtwork ? View.GONE : View.VISIBLE);
    }

    private Uri albumArtUri(long albumId) {
        return ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId);
    }

    private String trackInitial(Track track) {
        String title = track == null ? "" : track.title.trim();
        return title.isEmpty() ? "M" : title.substring(0, 1).toUpperCase(Locale.ROOT);
    }

    private void syncListPlaybackState(Track track, boolean playing) {
        String uri = track == null ? "" : track.uri;
        if (uri.equals(lastRenderedTrackUri) && playing == lastRenderedPlaying) {
            return;
        }
        lastRenderedTrackUri = uri;
        lastRenderedPlaying = playing;
        if (listView != null && listView.getAdapter() == trackAdapter) {
            trackAdapter.notifyDataSetChanged();
        }
    }

    private boolean isCurrentTrack(Track track) {
        Track current = playbackService == null ? null : playbackService.getCurrentTrack();
        return current != null && track != null && current.uri.equals(track.uri);
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
        styleTab(tabExtensions, currentTab == TAB_EXTENSIONS);
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

    private TextView statPill(String value, int tint) {
        TextView pill = text(value, 12, TEXT, Typeface.BOLD);
        pill.setGravity(Gravity.CENTER);
        pill.setSingleLine(true);
        pill.setEllipsize(TextUtils.TruncateAt.END);
        pill.setPadding(dp(8), 0, dp(8), 0);
        pill.setBackground(rounded(alphaColor(tint, 36), dp(15), 1, alphaColor(tint, 86)));
        return pill;
    }

    private LinearLayout.LayoutParams tabParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1f);
        params.setMargins(dp(3), 0, dp(3), 0);
        return params;
    }

    private LinearLayout.LayoutParams statParams() {
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

    private int alphaColor(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
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
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[] { 0xFF151B20, 0xFF202734, 0xFF211C25 });
        drawable.setCornerRadii(new float[] { dp(24), dp(24), dp(24), dp(24), 0, 0, 0, 0 });
        drawable.setStroke(dp(1), 0xFF303746);
        return drawable;
    }

    private GradientDrawable expandedArtworkBackground() {
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[] { 0xFF314048, 0xFF171A21, 0xFF3DDB9A });
        drawable.setCornerRadius(dp(22));
        drawable.setStroke(dp(1), 0xFF3A4352);
        return drawable;
    }

    private GradientDrawable rowBackground(boolean active) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(active ? 0xFF1D302D : SURFACE);
        drawable.setCornerRadius(dp(10));
        drawable.setStroke(dp(1), active ? ACCENT : 0xFF272C38);
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
            root.setBackground(rowBackground(false));
            root.setLayoutParams(new AbsListView.LayoutParams(-1, dp(78)));

            badge = text("M", 18, BG, Typeface.BOLD);
            badge.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(dp(52), dp(52));
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
            boolean active = isCurrentTrack(track);
            title.setText(track.title);
            title.setTextColor(active ? ACCENT : TEXT);
            subtitle.setText(active && playbackService != null && playbackService.isPlaying() ? "Tocando agora - " + track.subtitle() : track.subtitle());
            subtitle.setTextColor(active ? TEXT : MUTED);
            duration.setText(track.formattedDuration());
            duration.setTextColor(active ? ACCENT : MUTED);
            badge.setText(active && playbackService != null && playbackService.isPlaying() ? ">" : trackInitial(track));
            badge.setBackground(albumBadge(track.albumId));
            root.setBackground(rowBackground(active));
            more.setColorFilter(active ? ACCENT : MUTED);
            more.setOnClickListener(view -> showTrackOptions(track, currentTab == TAB_PLAYLISTS ? openPlaylist : null));
        }
    }

    private GradientDrawable albumBadge(long seed) {
        int[] colors = new int[] { ACCENT, WARM, BLUE, ROSE, 0xFFA7F070 };
        int color = colors[(int) (Math.abs(seed) % colors.length)];
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[] { color, 0xFFFFFFFF });
        drawable.setCornerRadius(dp(10));
        return drawable;
    }

    private ArrayList<ExtensionItem> extensionItems() {
        ArrayList<ExtensionItem> items = new ArrayList<>();
        boolean snaptubeReady = isPackageAvailable(SNAPTUBE_PACKAGE);
        String customPackage = customExtensionPackage();
        boolean customReady = isPackageAvailable(customPackage);
        items.add(new ExtensionItem(
                "L",
                "Baixar por link",
                "Aceita links diretos de audio: mp3, m4a, wav, ogg e flac",
                "Colar",
                ACCENT,
                true
        ));
        items.add(new ExtensionItem(
                "S",
                "Snaptube",
                snaptubeReady ? "Instalado - abrir app externo" : "Nao instalado neste aparelho",
                snaptubeReady ? "Abrir" : "Ausente",
                WARM,
                snaptubeReady
        ));
        items.add(new ExtensionItem(
                customPackage.isEmpty() ? "+" : "E",
                customPackage.isEmpty() ? "Adicionar extensao" : customPackage,
                customPackage.isEmpty() ? "Cadastre o pacote Android da extensao" : (customReady ? "Instalada - abrir app externo" : "Pacote salvo, mas nao encontrado"),
                customPackage.isEmpty() ? "Cadastrar" : (customReady ? "Abrir" : "Editar"),
                BLUE,
                customPackage.isEmpty() || customReady
        ));
        items.add(new ExtensionItem(
                "R",
                "Atualizar biblioteca",
                "Recarrega as musicas locais depois de baixar arquivos",
                "Atualizar",
                WARM,
                true
        ));
        items.add(new ExtensionItem(
                "!",
                "Uso responsavel",
                "Importe apenas audio que voce tem direito de usar",
                "Ver",
                ROSE,
                true
        ));
        return items;
    }

    private static class ExtensionItem {
        final String badge;
        final String title;
        final String subtitle;
        final String action;
        final int tint;
        final boolean active;

        ExtensionItem(String badge, String title, String subtitle, String action, int tint, boolean active) {
            this.badge = badge;
            this.title = title;
            this.subtitle = subtitle;
            this.action = action;
            this.tint = tint;
            this.active = active;
        }
    }

    private class ExtensionAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return extensionItems().size();
        }

        @Override
        public ExtensionItem getItem(int position) {
            return extensionItems().get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ExtensionRow row;
            if (convertView == null) {
                row = new ExtensionRow();
                convertView = row.root;
                convertView.setTag(row);
            } else {
                row = (ExtensionRow) convertView.getTag();
            }
            row.bind(getItem(position));
            return convertView;
        }
    }

    private class ExtensionRow {
        final LinearLayout root;
        final TextView badge;
        final TextView title;
        final TextView subtitle;
        final TextView action;

        ExtensionRow() {
            root = new LinearLayout(MainActivity.this);
            root.setGravity(Gravity.CENTER_VERTICAL);
            root.setPadding(dp(12), dp(8), dp(10), dp(8));
            root.setBackground(rowBackground(false));
            root.setLayoutParams(new AbsListView.LayoutParams(-1, dp(82)));

            badge = text("E", 18, BG, Typeface.BOLD);
            badge.setGravity(Gravity.CENTER);
            root.addView(badge, new LinearLayout.LayoutParams(dp(52), dp(52)));

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

            action = text("", 12, TEXT, Typeface.BOLD);
            action.setGravity(Gravity.CENTER);
            action.setPadding(dp(10), 0, dp(10), 0);
            root.addView(action, new LinearLayout.LayoutParams(-2, dp(32)));
        }

        void bind(ExtensionItem item) {
            badge.setText(item.badge);
            badge.setBackground(rounded(item.tint, dp(10), 0, 0));
            title.setText(item.title);
            title.setTextColor(item.active ? TEXT : MUTED);
            subtitle.setText(item.subtitle);
            action.setText(item.action);
            action.setBackground(rounded(alphaColor(item.tint, item.active ? 42 : 24), dp(16), 1, alphaColor(item.tint, item.active ? 110 : 60)));
            root.setBackground(rowBackground(false));
        }
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
            root.setBackground(rowBackground(false));
            root.setLayoutParams(new AbsListView.LayoutParams(-1, dp(80)));

            badge = text("", 18, BG, Typeface.BOLD);
            badge.setGravity(Gravity.CENTER);
            badge.setBackground(rounded(WARM, dp(10), 0, 0));
            badge.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_playlist, 0, 0, 0);
            root.addView(badge, new LinearLayout.LayoutParams(dp(52), dp(52)));

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

