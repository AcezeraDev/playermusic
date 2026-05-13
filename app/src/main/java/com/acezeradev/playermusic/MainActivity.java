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
import android.content.res.Configuration;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaScannerConnection;
import android.media.audiofx.AudioEffect;
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
import android.view.animation.AccelerateDecelerateInterpolator;
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
import android.widget.ScrollView;
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
    private static final String UI_PREFS = "player_music_ui";
    private static final String KEY_CUSTOM_EXTENSION_PACKAGE = "custom_extension_package";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_HISTORY = "history";
    private static final String KEY_LYRICS_PREFIX = "lyrics_";
    private static final String SNAPTUBE_PACKAGE = "com.snaptube.premium";
    private static final int MAX_REDIRECTS = 5;
    private static final int MAX_HISTORY = 40;
    private static final int EXT_DOWNLOAD = 1;
    private static final int EXT_REFRESH = 2;
    private static final int EXT_EQUALIZER = 3;
    private static final int EXT_HISTORY = 4;
    private static final int EXT_SNAPTUBE = 5;
    private static final int EXT_CUSTOM = 6;
    private static final int EXT_SAFETY = 7;

    private int BG;
    private int SURFACE;
    private int SURFACE_ALT;
    private int SURFACE_RAISED;
    private int TEXT;
    private int MUTED;
    private int SOFT_MUTED;
    private int ACCENT;
    private int WARM;
    private int BLUE;
    private int ROSE;
    private int BORDER;
    private int SEEK_BG;
    private int CONTROL_BG;
    private int ACTIVE_ROW;
    private int ON_ACCENT;
    private int BADGE_TEXT;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final MusicRepository repository = new MusicRepository();
    private PlaylistStore playlistStore;
    private SharedPreferences extensionPrefs;
    private SharedPreferences uiPrefs;
    private boolean darkMode = true;

    private TextView subtitleView;
    private TextView emptyView;
    private TextView tabLibrary;
    private TextView tabPlaylists;
    private TextView tabQueue;
    private TextView tabExtensions;
    private TextView librarySummary;
    private TextView durationSummary;
    private TextView playlistSummary;
    private TextView heroTitle;
    private TextView heroSubtitle;
    private TextView heroMetric;
    private TextView heroPrimaryAction;
    private TextView heroSecondaryAction;
    private TextView sectionTitle;
    private TextView sectionSubtitle;
    private ImageView nowArtwork;
    private TextView nowArtInitial;
    private TextView nowTitle;
    private TextView nowArtist;
    private TextView nowAlbum;
    private TextView queueInfo;
    private FrameLayout nowArtworkFrame;
    private LinearLayout miniPlayerPanel;
    private FrameLayout expandedPlayer;
    private FrameLayout expandedArtworkFrame;
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
    private ImageButton themeModeButton;
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
    private String lastAnimatedTrackUri = "";
    private String lastHistoryTrackUri = "";
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
        uiPrefs = getSharedPreferences(UI_PREFS, MODE_PRIVATE);
        darkMode = uiPrefs.getBoolean(KEY_DARK_MODE, isSystemDarkMode());
        applyThemePalette();
        applyWindowColors();
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
                if (!isChangingConfigurations()) {
                    playbackService.stopIfIdle();
                }
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

    private boolean isSystemDarkMode() {
        int mode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return mode == Configuration.UI_MODE_NIGHT_YES;
    }

    private void applyThemePalette() {
        if (darkMode) {
            BG = 0xFF090B10;
            SURFACE = 0xFF171A21;
            SURFACE_ALT = 0xFF232836;
            SURFACE_RAISED = 0xFF20242F;
            TEXT = 0xFFF5F7FA;
            MUTED = 0xFFA7ADB8;
            SOFT_MUTED = 0xFF858C99;
            ACCENT = 0xFF3DDB9A;
            WARM = 0xFFFFB86C;
            BLUE = 0xFF7CC7FF;
            ROSE = 0xFFE86BA5;
            BORDER = 0xFF2E3343;
            SEEK_BG = 0xFF333948;
            CONTROL_BG = 0x1FFFFFFF;
            ACTIVE_ROW = 0xFF1D302D;
            ON_ACCENT = 0xFF06100D;
            BADGE_TEXT = 0xFF06100D;
        } else {
            BG = 0xFFF6F8FB;
            SURFACE = 0xFFFFFFFF;
            SURFACE_ALT = 0xFFE9EEF5;
            SURFACE_RAISED = 0xFFFDFEFF;
            TEXT = 0xFF121826;
            MUTED = 0xFF667085;
            SOFT_MUTED = 0xFF7A8495;
            ACCENT = 0xFF0A8F68;
            WARM = 0xFFB86624;
            BLUE = 0xFF2563EB;
            ROSE = 0xFFC9437B;
            BORDER = 0xFFD7DFEA;
            SEEK_BG = 0xFFDDE5EF;
            CONTROL_BG = 0x180A8F68;
            ACTIVE_ROW = 0xFFE6F6F0;
            ON_ACCENT = 0xFFFFFFFF;
            BADGE_TEXT = 0xFF06100D;
        }
    }

    private void applyWindowColors() {
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        if (Build.VERSION.SDK_INT >= 23) {
            int flags = darkMode ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= 26 && !darkMode) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            getWindow().getDecorView().setSystemUiVisibility(flags);
        }
    }

    private void toggleThemeMode() {
        darkMode = !darkMode;
        if (uiPrefs != null) {
            uiPrefs.edit().putBoolean(KEY_DARK_MODE, darkMode).apply();
        }
        recreate();
    }

    private void buildUi() {
        FrameLayout shell = new FrameLayout(this);
        shell.setBackground(appBackground());
        shell.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.TRANSPARENT);
        root.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));
        shell.addView(root, new FrameLayout.LayoutParams(-1, -1));
        setContentView(shell);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(18), dp(16), dp(18), dp(10));
        root.addView(header, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(titleRow, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout titleBlock = new LinearLayout(this);
        titleBlock.setOrientation(LinearLayout.VERTICAL);
        titleRow.addView(titleBlock, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView title = text("PlayerMusic", 30, TEXT, Typeface.BOLD);
        titleBlock.addView(title);
        subtitleView = text("Musica local, offline e sem complicacao", 13, MUTED, Typeface.NORMAL);
        titleBlock.addView(subtitleView);

        themeModeButton = iconButton(darkMode ? R.drawable.ic_light_mode : R.drawable.ic_dark_mode, darkMode ? "Ativar modo claro" : "Ativar modo escuro", SURFACE_ALT, TEXT, dp(42));
        themeModeButton.setOnClickListener(view -> toggleThemeMode());
        titleRow.addView(themeModeButton);

        backPlaylistButton = iconButton(R.drawable.ic_arrow_back, "Voltar", SURFACE_ALT, TEXT, dp(42));
        backPlaylistButton.setVisibility(View.GONE);
        backPlaylistButton.setOnClickListener(view -> {
            openPlaylist = null;
            showPlaylists();
        });
        titleRow.addView(backPlaylistButton);

        addPlaylistButton = iconButton(R.drawable.ic_add, "Nova playlist", ACCENT, ON_ACCENT, dp(42));
        addPlaylistButton.setVisibility(View.GONE);
        addPlaylistButton.setOnClickListener(view -> showCreatePlaylistDialog(null));
        titleRow.addView(addPlaylistButton);

        permissionButton = new Button(this);
        permissionButton.setText("Permitir");
        permissionButton.setTextColor(ON_ACCENT);
        permissionButton.setTextSize(14);
        permissionButton.setAllCaps(false);
        permissionButton.setTypeface(Typeface.DEFAULT_BOLD);
        permissionButton.setBackground(rounded(ACCENT, dp(22), 0, 0));
        permissionButton.setVisibility(View.GONE);
        permissionButton.setOnClickListener(view -> requestLibraryPermission());
        titleRow.addView(permissionButton, new LinearLayout.LayoutParams(-2, dp(42)));

        header.addView(buildHeroPanel(), heroParams());

        LinearLayout summaryRow = new LinearLayout(this);
        summaryRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams summaryParams = new LinearLayout.LayoutParams(-1, dp(38));
        summaryParams.setMargins(0, dp(12), 0, 0);
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
        searchInput.setHintTextColor(MUTED);
        searchInput.setTextSize(15);
        searchInput.setPadding(dp(16), 0, dp(16), 0);
        searchInput.setBackground(rounded(SURFACE_ALT, dp(14), 1, BORDER));
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
        tabs.setPadding(dp(3), dp(3), dp(3), dp(3));
        tabs.setBackground(rounded(alphaBlend(SURFACE_ALT, ACCENT, darkMode ? 4 : 2), dp(23), 1, BORDER));
        header.addView(tabs, new LinearLayout.LayoutParams(-1, dp(48)));
        tabLibrary = tab("Musicas");
        tabPlaylists = tab("Listas");
        tabQueue = tab("Fila");
        tabExtensions = tab("Extras");
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

        LinearLayout sectionRow = new LinearLayout(this);
        sectionRow.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams sectionParams = new LinearLayout.LayoutParams(-1, -2);
        sectionParams.setMargins(dp(2), dp(12), dp(2), dp(2));
        header.addView(sectionRow, sectionParams);

        sectionTitle = text("Biblioteca", 18, TEXT, Typeface.BOLD);
        sectionRow.addView(sectionTitle, new LinearLayout.LayoutParams(-1, -2));
        sectionSubtitle = text("Todas as musicas do aparelho", 12, MUTED, Typeface.NORMAL);
        sectionRow.addView(sectionSubtitle, new LinearLayout.LayoutParams(-1, -2));

        FrameLayout content = new FrameLayout(this);
        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1f));
        listView = new ListView(this);
        listView.setDivider(new ColorDrawable(Color.TRANSPARENT));
        listView.setDividerHeight(dp(8));
        listView.setSelector(android.R.color.transparent);
        listView.setCacheColorHint(Color.TRANSPARENT);
        listView.setClipToPadding(false);
        listView.setPadding(dp(14), dp(6), dp(14), dp(18));
        content.addView(listView, new FrameLayout.LayoutParams(-1, -1));

        emptyView = text("", 15, MUTED, Typeface.NORMAL);
        emptyView.setGravity(Gravity.CENTER);
        emptyView.setPadding(dp(22), dp(20), dp(22), dp(20));
        emptyView.setBackground(emptyStateBackground());
        FrameLayout.LayoutParams emptyParams = new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER);
        emptyParams.setMargins(dp(22), 0, dp(22), 0);
        content.addView(emptyView, emptyParams);
        listView.setEmptyView(emptyView);

        buildPlayer(root);
        buildExpandedPlayer(shell);
        refreshTabs();
        updateLibrarySummary();
    }

    private LinearLayout buildHeroPanel() {
        LinearLayout hero = new LinearLayout(this);
        hero.setGravity(Gravity.CENTER_VERTICAL);
        hero.setPadding(dp(16), dp(14), dp(14), dp(14));
        hero.setBackground(heroBackground());

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        hero.addView(copy, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView eyebrow = text("PLAYERMUSIC", 11, ACCENT, Typeface.BOLD);
        eyebrow.setSingleLine(true);
        copy.addView(eyebrow, new LinearLayout.LayoutParams(-1, -2));

        heroTitle = text("Sua biblioteca", 22, TEXT, Typeface.BOLD);
        heroTitle.setSingleLine(true);
        heroTitle.setEllipsize(TextUtils.TruncateAt.END);
        copy.addView(heroTitle, new LinearLayout.LayoutParams(-1, -2));

        heroSubtitle = text("Tudo pronto para tocar offline.", 13, MUTED, Typeface.NORMAL);
        heroSubtitle.setMaxLines(2);
        heroSubtitle.setEllipsize(TextUtils.TruncateAt.END);
        copy.addView(heroSubtitle, new LinearLayout.LayoutParams(-1, -2));

        heroMetric = text("0 musicas", 12, TEXT, Typeface.BOLD);
        heroMetric.setSingleLine(true);
        heroMetric.setEllipsize(TextUtils.TruncateAt.END);
        heroMetric.setPadding(dp(10), 0, dp(10), 0);
        heroMetric.setBackground(rounded(alphaColor(ACCENT, darkMode ? 34 : 22), dp(14), 1, alphaColor(ACCENT, darkMode ? 78 : 58)));
        LinearLayout.LayoutParams metricParams = new LinearLayout.LayoutParams(-2, dp(30));
        metricParams.setMargins(0, dp(10), 0, 0);
        copy.addView(heroMetric, metricParams);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.VERTICAL);
        actions.setGravity(Gravity.CENTER);
        hero.addView(actions, new LinearLayout.LayoutParams(dp(108), -2));

        heroPrimaryAction = heroAction("Tocar", ACCENT, true, view -> quickPlayAll());
        actions.addView(heroPrimaryAction, new LinearLayout.LayoutParams(-1, dp(38)));

        heroSecondaryAction = heroAction("Baixar", BLUE, false, view -> showImportLinkDialog());
        LinearLayout.LayoutParams secondParams = new LinearLayout.LayoutParams(-1, dp(34));
        secondParams.setMargins(0, dp(8), 0, 0);
        actions.addView(heroSecondaryAction, secondParams);

        return hero;
    }

    private TextView heroAction(String label, int tint, boolean primary, View.OnClickListener listener) {
        TextView action = text(label, 12, primary ? ON_ACCENT : tint, Typeface.BOLD);
        action.setGravity(Gravity.CENTER);
        action.setSingleLine(true);
        action.setEllipsize(TextUtils.TruncateAt.END);
        action.setBackground(rounded(primary ? tint : alphaColor(tint, darkMode ? 30 : 18), dp(19), primary ? 0 : 1, alphaColor(tint, darkMode ? 92 : 70)));
        action.setClickable(true);
        action.setOnClickListener(listener);
        attachPressAnimation(action);
        return action;
    }

    private LinearLayout.LayoutParams heroParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, dp(14), 0, 0);
        return params;
    }

    private void quickPlayAll() {
        if (allTracks.isEmpty()) {
            if (hasLibraryPermission()) {
                toast("Nenhuma musica encontrada");
            } else {
                requestLibraryPermission();
            }
            return;
        }
        playFrom(allTracks, 0);
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

        View handle = new View(this);
        handle.setBackground(rounded(alphaColor(TEXT, darkMode ? 70 : 90), dp(2), 0, 0));
        LinearLayout.LayoutParams handleParams = new LinearLayout.LayoutParams(dp(42), dp(4));
        handleParams.gravity = Gravity.CENTER_HORIZONTAL;
        handleParams.setMargins(0, 0, 0, dp(12));
        panel.addView(handle, handleParams);

        LinearLayout nowRow = new LinearLayout(this);
        nowRow.setGravity(Gravity.CENTER_VERTICAL);
        panel.addView(nowRow, new LinearLayout.LayoutParams(-1, -2));

        FrameLayout artworkFrame = new FrameLayout(this);
        nowArtworkFrame = artworkFrame;
        artworkFrame.setBackground(rounded(SURFACE_RAISED, dp(18), 1, BORDER));
        LinearLayout.LayoutParams artworkParams = new LinearLayout.LayoutParams(dp(86), dp(86));
        nowRow.addView(artworkFrame, artworkParams);

        nowArtwork = new ImageView(this);
        nowArtwork.setScaleType(ImageView.ScaleType.CENTER_CROP);
        nowArtwork.setVisibility(View.GONE);
        artworkFrame.addView(nowArtwork, new FrameLayout.LayoutParams(-1, -1));

        nowArtInitial = text("M", 30, BADGE_TEXT, Typeface.BOLD);
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

        nowAlbum = text("Player parado", 12, SOFT_MUTED, Typeface.NORMAL);
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
        seekBar.setProgressBackgroundTintList(ColorStateList.valueOf(SEEK_BG));
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

        shuffleButton = iconButton(R.drawable.ic_shuffle, "Aleatorio", CONTROL_BG, MUTED, dp(46));
        ImageButton previousButton = iconButton(R.drawable.ic_skip_previous, "Anterior", SURFACE_RAISED, TEXT, dp(48));
        playButton = iconButton(R.drawable.ic_play_arrow, "Tocar", ACCENT, ON_ACCENT, dp(60));
        ImageButton nextButton = iconButton(R.drawable.ic_skip_next, "Proxima", SURFACE_RAISED, TEXT, dp(48));
        repeatButton = iconButton(R.drawable.ic_repeat, "Repetir", CONTROL_BG, MUTED, dp(46));
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
        expandedPlayer.setBackground(appBackground());
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

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setClipToPadding(false);
        expandedPlayer.addView(scrollView, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(18), dp(18), dp(18), dp(18));
        scrollView.addView(page, new ScrollView.LayoutParams(-1, -2));

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
        expandedArtworkFrame = artworkFrame;
        artworkFrame.setBackground(expandedArtworkBackground());
        int coverSize = Math.min(getResources().getDisplayMetrics().widthPixels - dp(48), dp(340));
        LinearLayout.LayoutParams artworkParams = new LinearLayout.LayoutParams(coverSize, coverSize);
        artworkParams.gravity = Gravity.CENTER_HORIZONTAL;
        artworkParams.setMargins(0, dp(22), 0, dp(20));
        page.addView(artworkFrame, artworkParams);

        expandedArtwork = new ImageView(this);
        expandedArtwork.setScaleType(ImageView.ScaleType.CENTER_CROP);
        expandedArtwork.setVisibility(View.GONE);
        artworkFrame.addView(expandedArtwork, new FrameLayout.LayoutParams(-1, -1));

        expandedArtInitial = text("M", 64, BADGE_TEXT, Typeface.BOLD);
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

        expandedAlbum = text("Player parado", 13, SOFT_MUTED, Typeface.NORMAL);
        expandedAlbum.setGravity(Gravity.CENTER);
        expandedAlbum.setSingleLine(true);
        expandedAlbum.setEllipsize(TextUtils.TruncateAt.END);
        page.addView(expandedAlbum, new LinearLayout.LayoutParams(-1, -2));

        expandedSeekBar = new SeekBar(this);
        expandedSeekBar.setMax(1000);
        expandedSeekBar.setProgress(0);
        expandedSeekBar.setProgressTintList(ColorStateList.valueOf(ACCENT));
        expandedSeekBar.setProgressBackgroundTintList(ColorStateList.valueOf(SEEK_BG));
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

        expandedShuffleButton = iconButton(R.drawable.ic_shuffle, "Aleatorio", CONTROL_BG, MUTED, dp(48));
        ImageButton previousButton = iconButton(R.drawable.ic_skip_previous, "Anterior", SURFACE_RAISED, TEXT, dp(54));
        expandedPlayButton = iconButton(R.drawable.ic_play_arrow, "Tocar", ACCENT, ON_ACCENT, dp(70));
        ImageButton nextButton = iconButton(R.drawable.ic_skip_next, "Proxima", SURFACE_RAISED, TEXT, dp(54));
        expandedRepeatButton = iconButton(R.drawable.ic_repeat, "Repetir", CONTROL_BG, MUTED, dp(48));
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

        LinearLayout extrasTop = new LinearLayout(this);
        extrasTop.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams extrasTopParams = new LinearLayout.LayoutParams(-1, dp(42));
        extrasTopParams.setMargins(0, dp(2), 0, 0);
        page.addView(extrasTop, extrasTopParams);
        extrasTop.addView(actionChip("EQ", ACCENT, view -> showEqualizerDialog()), chipParams(4));
        extrasTop.addView(actionChip("Letras", BLUE, view -> showLyricsDialog(currentTrackOrNull())), chipParams(4));
        extrasTop.addView(actionChip("Album", WARM, view -> showArtistAlbumDialog(currentTrackOrNull())), chipParams(4));
        extrasTop.addView(actionChip("Historico", ROSE, view -> showHistoryDialog()), chipParams(4));

        TextView sourceChip = text("Conteudo local e downloads offline", 12, MUTED, Typeface.BOLD);
        sourceChip.setGravity(Gravity.CENTER);
        sourceChip.setPadding(dp(14), 0, dp(14), 0);
        sourceChip.setBackground(rounded(alphaColor(ACCENT, darkMode ? 24 : 18), dp(17), 1, alphaColor(ACCENT, darkMode ? 52 : 42)));
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
        setSection("Permissao necessaria", "Autorize o acesso ao audio local");
        setHeroContext("Conecte sua biblioteca", "Permita acesso as musicas para o PlayerMusic organizar tudo automaticamente.", "Tocar", "Baixar");
        emptyView.setText("Acesso necessario\nPermita o audio para tocar as musicas baixadas.");
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
        setSection("Biblioteca", countLabel(allTracks.size(), "musica no aparelho", "musicas no aparelho"));
        setHeroContext("Sua biblioteca", allTracks.isEmpty() ? "Adicione musicas ou baixe audio direto para ouvir offline." : "Escolha uma musica, toque tudo ou use a busca.", "Tocar", "Baixar");
        emptyView.setText(searchQuery().isEmpty() ? "Biblioteca vazia\nBaixe audios ou permita acesso as suas musicas locais." : "Nada encontrado\nTente outro nome de musica, artista ou album.");
        animateListChange();
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
        setSection("Playlists", countLabel(playlists.size(), "lista criada", "listas criadas"));
        setHeroContext("Organize seu som", "Monte colecoes para treinos, estudos, festas ou favoritos.", "Nova lista", "Favoritos");
        emptyView.setText("Nenhuma playlist ainda\nCrie uma lista para separar suas musicas por clima.");
        animateListChange();
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
        setSection(playlist.name, countLabel(resolved.size(), "musica na playlist", "musicas na playlist"));
        setHeroContext(playlist.name, "Playlist pronta para tocar em sequencia.", "Tocar", "Buscar");
        emptyView.setText(searchQuery().isEmpty() ? "Playlist vazia\nAdicione musicas pelo menu de cada faixa." : "Nada encontrado\nEssa playlist nao tem resultado para a busca.");
        animateListChange();
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
        setSection("Fila de reproducao", countLabel(queue.size(), "faixa preparada", "faixas preparadas"));
        setHeroContext("Sua fila", queue.isEmpty() ? "Toque uma musica para montar uma fila automaticamente." : "Gerencie o que vem a seguir.", "Tocar", "Historico");
        emptyView.setText("Fila vazia\nComece uma musica para ver a sequencia aqui.");
        animateListChange();
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
        setSection("Extras", "Downloads, equalizador e ferramentas");
        setHeroContext("Central de ferramentas", "Baixe audio, ajuste o som e abra seus recursos profissionais.", "Baixar", "Historico");
        emptyView.setText("");
        animateListChange();
    }

    private void handleExtensionAction(int position) {
        ExtensionItem item = extensionAdapter.getItem(position);
        if (item.actionId == EXT_DOWNLOAD) {
            showImportLinkDialog();
        } else if (item.actionId == EXT_REFRESH) {
            refreshDownloadedLibrary();
        } else if (item.actionId == EXT_EQUALIZER) {
            showEqualizerDialog();
        } else if (item.actionId == EXT_HISTORY) {
            showHistoryDialog();
        } else if (item.actionId == EXT_SNAPTUBE) {
            openExternalExtension(SNAPTUBE_PACKAGE, "Snaptube");
        } else if (item.actionId == EXT_CUSTOM) {
            String packageName = customExtensionPackage();
            if (packageName.isEmpty() || !isPackageAvailable(packageName)) {
                showExtensionPackageDialog();
            } else {
                openExternalExtension(packageName, packageName);
            }
        } else if (item.actionId == EXT_SAFETY) {
            showExtensionSafetyDialog();
        }
    }

    private void refreshDownloadedLibrary() {
        if (hasLibraryPermission()) {
            loadLibrary();
            toast("Biblioteca atualizando");
        } else {
            requestLibraryPermission();
        }
    }

    private void setSection(String title, String subtitle) {
        subtitleView.setText(subtitle);
        if (sectionTitle != null) {
            sectionTitle.setText(title);
        }
        if (sectionSubtitle != null) {
            sectionSubtitle.setText(subtitle);
        }
    }

    private void setHeroContext(String title, String subtitle, String primaryLabel, String secondaryLabel) {
        if (heroTitle != null) {
            heroTitle.setText(title);
        }
        if (heroSubtitle != null) {
            heroSubtitle.setText(subtitle);
        }
        if (heroPrimaryAction != null) {
            heroPrimaryAction.setText(primaryLabel);
            heroPrimaryAction.setOnClickListener(view -> handleHeroAction(primaryLabel));
        }
        if (heroSecondaryAction != null) {
            heroSecondaryAction.setText(secondaryLabel);
            heroSecondaryAction.setOnClickListener(view -> handleHeroAction(secondaryLabel));
        }
    }

    private void handleHeroAction(String label) {
        if ("Nova lista".equals(label)) {
            showCreatePlaylistDialog(null);
        } else if ("Baixar".equals(label)) {
            showImportLinkDialog();
        } else if ("Historico".equals(label)) {
            showHistoryDialog();
        } else if ("Buscar".equals(label)) {
            searchInput.setVisibility(View.VISIBLE);
            searchInput.requestFocus();
        } else if ("Favoritos".equals(label)) {
            Playlist favorites = findPlaylist(Playlist.FAVORITES_ID);
            if (favorites != null) {
                showPlaylistTracks(favorites);
            }
        } else if ("Tocar".equals(label)) {
            if (currentTab == TAB_QUEUE && playbackService != null && playbackService.hasTrack()) {
                playbackService.toggle();
            } else if (!visibleTracks.isEmpty() && currentTab != TAB_PLAYLISTS) {
                playFrom(visibleTracks, 0);
            } else if (!visibleTracks.isEmpty() && openPlaylist != null) {
                playFrom(visibleTracks, 0);
            } else {
                quickPlayAll();
            }
        }
    }

    private void animateListChange() {
        if (listView == null) {
            return;
        }
        listView.animate().cancel();
        listView.setAlpha(0.72f);
        listView.setTranslationY(dp(8));
        listView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(180)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }

    private void showImportLinkDialog() {
        EditText input = new EditText(this);
        input.setSingleLine(false);
        input.setMinLines(2);
        input.setMaxLines(4);
        input.setHint("https://site.com/musica.mp3");
        input.setTextColor(TEXT);
        input.setHintTextColor(MUTED);
        input.setPadding(dp(16), dp(12), dp(16), dp(12));
        input.setBackground(rounded(SURFACE_ALT, dp(14), 1, BORDER));
        LinearLayout body = dialogBody();
        body.addView(infoCard("Offline", "Cole um link direto de audio. Links do YouTube serao abertos no app oficial, sem download.", ACCENT));
        body.addView(input, spacedParams(-1, dp(96), 12));
        showStyledDialog(
                "Baixar audio",
                "Importacao offline",
                body,
                new DialogAction("Cancelar", MUTED, false, AlertDialog::dismiss),
                new DialogAction("Baixar", ACCENT, true, dialog -> {
                    String link = input.getText().toString().trim();
                    if (link.isEmpty()) {
                        input.setError("Cole um link");
                        return;
                    }
                    dialog.dismiss();
                    importAudioLink(link);
                })
        );
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
        LinearLayout body = dialogBody();
        body.addView(infoCard("Link nao suportado", message == null || message.isEmpty() ? "Use um link direto de audio, como mp3, m4a, wav, ogg ou flac." : message, ROSE));
        showStyledDialog(
                "Download nao iniciado",
                "Nao deu para salvar este arquivo",
                body,
                new DialogAction("Entendi", ACCENT, true, AlertDialog::dismiss)
        );
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
        input.setPadding(dp(16), dp(12), dp(16), dp(12));
        input.setBackground(rounded(SURFACE_ALT, dp(14), 1, BORDER));
        LinearLayout body = dialogBody();
        body.addView(infoCard("Pacote Android", "Cadastre o nome do pacote de uma extensao instalada para abrir direto pelo PlayerMusic.", BLUE));
        body.addView(input, spacedParams(-1, dp(54), 12));
        showStyledDialog(
                "Cadastrar extensao",
                "Integre outro app instalado",
                body,
                new DialogAction("Limpar", ROSE, false, dialog -> {
                    extensionPrefs.edit().remove(KEY_CUSTOM_EXTENSION_PACKAGE).apply();
                    dialog.dismiss();
                    showExtensions();
                }),
                new DialogAction("Cancelar", MUTED, false, AlertDialog::dismiss),
                new DialogAction("Salvar", ACCENT, true, dialog -> {
                    String packageName = input.getText().toString().trim();
                    if (packageName.isEmpty()) {
                        input.setError("Digite o pacote Android");
                        return;
                    }
                    extensionPrefs.edit().putString(KEY_CUSTOM_EXTENSION_PACKAGE, packageName).apply();
                    dialog.dismiss();
                    showExtensions();
                })
        );
    }

    private void showExtensionSafetyDialog() {
        LinearLayout body = dialogBody();
        body.addView(infoCard("Biblioteca local", "Use extensoes externas apenas para baixar ou importar audio que voce tem direito de usar.", WARM));
        body.addView(infoCard("Atualizacao", "O PlayerMusic atualiza e toca os arquivos locais encontrados no aparelho.", ACCENT), spacedParams(-1, -2, 10));
        showStyledDialog(
                "Uso responsavel",
                "Downloads e extensoes",
                body,
                new DialogAction("Entendi", ACCENT, true, AlertDialog::dismiss)
        );
    }

    private void showEqualizerDialog() {
        if (playbackService == null || !playbackService.hasTrack()) {
            toast("Toque uma musica para abrir o equalizador");
            return;
        }
        if (!playbackService.hasEqualizer()) {
            if (openSystemEqualizer()) {
                return;
            }
            toast("Equalizador indisponivel neste aparelho");
            return;
        }
        short bandCount = playbackService.getEqualizerBandCount();
        short[] range = playbackService.getEqualizerBandLevelRange();
        if (bandCount <= 0 || range.length < 2 || range[0] == range[1]) {
            toast("Equalizador indisponivel neste aparelho");
            return;
        }

        LinearLayout panel = dialogBody();
        Track current = playbackService.getCurrentTrack();
        if (current != null) {
            panel.addView(infoCard("Faixa atual", current.title + "\n" + current.artist, ACCENT));
        }

        for (short band = 0; band < bandCount; band++) {
            final short currentBand = band;
            LinearLayout bandPanel = new LinearLayout(this);
            bandPanel.setOrientation(LinearLayout.VERTICAL);
            bandPanel.setPadding(dp(14), dp(12), dp(14), dp(10));
            bandPanel.setBackground(rounded(alphaBlend(SURFACE, ACCENT, darkMode ? 9 : 5), dp(14), 1, BORDER));
            panel.addView(bandPanel, spacedParams(-1, -2, 10));

            short level = playbackService.getEqualizerBandLevel(currentBand);
            LinearLayout labelRow = new LinearLayout(this);
            labelRow.setGravity(Gravity.CENTER_VERTICAL);
            bandPanel.addView(labelRow, new LinearLayout.LayoutParams(-1, -2));

            TextView frequency = text(formatBandFrequency(playbackService.getEqualizerCenterFreq(currentBand)), 13, TEXT, Typeface.BOLD);
            labelRow.addView(frequency, new LinearLayout.LayoutParams(0, -2, 1f));

            TextView label = text(formatDb(level), 12, ACCENT, Typeface.BOLD);
            label.setGravity(Gravity.CENTER);
            label.setPadding(dp(10), 0, dp(10), 0);
            label.setBackground(rounded(alphaColor(ACCENT, darkMode ? 34 : 22), dp(14), 1, alphaColor(ACCENT, darkMode ? 82 : 68)));
            labelRow.addView(label, new LinearLayout.LayoutParams(-2, dp(28)));

            SeekBar bandSeek = new SeekBar(this);
            bandSeek.setMax(range[1] - range[0]);
            bandSeek.setProgress(level - range[0]);
            bandSeek.setProgressTintList(ColorStateList.valueOf(ACCENT));
            bandSeek.setProgressBackgroundTintList(ColorStateList.valueOf(SEEK_BG));
            bandSeek.setThumbTintList(ColorStateList.valueOf(TEXT));
            bandSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    short newLevel = (short) (range[0] + progress);
                    label.setText(formatDb(newLevel));
                    if (fromUser) {
                        playbackService.setEqualizerBandLevel(currentBand, newLevel);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });
            bandPanel.addView(bandSeek, new LinearLayout.LayoutParams(-1, dp(42)));
        }

        ScrollView scrollView = new ScrollView(this);
        scrollView.setClipToPadding(false);
        scrollView.addView(panel);
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(-1, dialogScrollHeight(92 + bandCount * 86, 460)));
        showStyledDialog(
                "Equalizador",
                "Graves, medios e agudos",
                scrollView,
                new DialogAction("Zerar", WARM, false, dialog -> {
                    resetEqualizer();
                    dialog.dismiss();
                    showEqualizerDialog();
                }),
                new DialogAction("Pronto", ACCENT, true, AlertDialog::dismiss)
        );
    }

    private boolean openSystemEqualizer() {
        try {
            Intent intent = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
            intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, getPackageName());
            if (playbackService != null && playbackService.getAudioSessionId() > 0) {
                intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, playbackService.getAudioSessionId());
            }
            startActivity(intent);
            return true;
        } catch (RuntimeException error) {
            return false;
        }
    }

    private void resetEqualizer() {
        if (playbackService == null || !playbackService.hasEqualizer()) {
            return;
        }
        short bandCount = playbackService.getEqualizerBandCount();
        for (short band = 0; band < bandCount; band++) {
            playbackService.setEqualizerBandLevel(band, (short) 0);
        }
    }

    private void showLyricsDialog(Track track) {
        if (track == null) {
            toast("Toque uma musica para abrir as letras");
            return;
        }
        EditText input = new EditText(this);
        input.setSingleLine(false);
        input.setMinLines(8);
        input.setMaxLines(14);
        input.setGravity(Gravity.TOP);
        input.setHint("Cole ou edite a letra desta musica");
        input.setText(lyricsFor(track));
        input.setTextColor(TEXT);
        input.setHintTextColor(MUTED);
        input.setPadding(dp(16), dp(12), dp(16), dp(12));
        input.setBackground(rounded(SURFACE_ALT, dp(12), 1, BORDER));
        LinearLayout body = dialogBody();
        body.addView(infoCard("Musica", track.title + "\n" + track.artist, BLUE));
        body.addView(input, spacedParams(-1, dp(220), 12));
        showStyledDialog(
                "Letras",
                "Texto salvo por musica",
                body,
                new DialogAction("Buscar", BLUE, false, dialog -> openExternalUrl("https://www.google.com/search?q=" + Uri.encode(track.artist + " " + track.title + " letra"))),
                new DialogAction("Fechar", MUTED, false, AlertDialog::dismiss),
                new DialogAction("Salvar", ACCENT, true, dialog -> {
                    if (uiPrefs != null) {
                        uiPrefs.edit().putString(lyricsKey(track), input.getText().toString()).apply();
                    }
                    toast("Letra salva");
                    dialog.dismiss();
                })
        );
    }

    private void showArtistAlbumDialog(Track track) {
        if (track == null) {
            toast("Toque uma musica para abrir artista e album");
            return;
        }
        ArrayList<Track> artistTracks = tracksByArtist(track.artist);
        ArrayList<Track> albumTracks = tracksByAlbum(track.album, track.artist);
        LinearLayout body = dialogBody();
        body.addView(artistAlbumHeader(track));
        LinearLayout stats = new LinearLayout(this);
        stats.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams artistStatParams = new LinearLayout.LayoutParams(0, dp(74), 1f);
        artistStatParams.setMargins(0, 0, dp(5), 0);
        LinearLayout.LayoutParams albumStatParams = new LinearLayout.LayoutParams(0, dp(74), 1f);
        albumStatParams.setMargins(dp(5), 0, 0, 0);
        stats.addView(statBlock(countLabel(artistTracks.size(), "musica", "musicas"), "do artista", BLUE), artistStatParams);
        stats.addView(statBlock(countLabel(albumTracks.size(), "musica", "musicas"), "no album", WARM), albumStatParams);
        body.addView(stats, spacedParams(-1, dp(74), 12));
        body.addView(infoCard("Album", track.album, WARM), spacedParams(-1, -2, 10));
        showStyledDialog(
                "Artista e album",
                "Explore a biblioteca",
                body,
                new DialogAction("Fechar", MUTED, false, AlertDialog::dismiss),
                new DialogAction("Tocar artista", BLUE, false, dialog -> {
                    dialog.dismiss();
                    if (!artistTracks.isEmpty()) {
                        playFrom(artistTracks, 0);
                    }
                }),
                new DialogAction("Tocar album", ACCENT, true, dialog -> {
                    dialog.dismiss();
                    if (!albumTracks.isEmpty()) {
                        playFrom(albumTracks, 0);
                    }
                })
        );
    }

    private void showHistoryDialog() {
        ArrayList<Track> history = historyTracks();
        if (history.isEmpty()) {
            LinearLayout body = dialogBody();
            body.addView(infoCard("Nada tocado ainda", "As musicas tocadas vao aparecer aqui com acesso rapido para repetir.", ROSE));
            showStyledDialog(
                    "Historico",
                    "Ultimas reproducoes",
                    body,
                    new DialogAction("Entendi", ACCENT, true, AlertDialog::dismiss)
            );
            return;
        }
        LinearLayout list = dialogBody();
        final AlertDialog[] holder = new AlertDialog[1];
        for (int i = 0; i < history.size(); i++) {
            final int index = i;
            list.addView(historyRow(history.get(i), index, view -> {
                if (holder[0] != null) {
                    holder[0].dismiss();
                }
                playFrom(history, index);
            }), spacedParams(-1, dp(68), i == 0 ? 0 : 8));
        }
        ScrollView scrollView = new ScrollView(this);
        scrollView.setClipToPadding(false);
        scrollView.addView(list);
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(-1, dialogScrollHeight(68 * history.size(), 380)));
        holder[0] = showStyledDialog(
                "Historico",
                "Ultimas reproducoes",
                scrollView,
                new DialogAction("Limpar", ROSE, false, dialog -> {
                    if (uiPrefs != null) {
                        uiPrefs.edit().remove(KEY_HISTORY).apply();
                    }
                    toast("Historico limpo");
                    dialog.dismiss();
                }),
                new DialogAction("Fechar", ACCENT, true, AlertDialog::dismiss)
        );
    }

    private AlertDialog showStyledDialog(String title, String subtitle, View body, DialogAction... actions) {
        AlertDialog dialog = new AlertDialog.Builder(this).create();
        dialog.setCanceledOnTouchOutside(true);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(dialogBackground());

        View strip = new View(this);
        strip.setBackground(dialogAccentStrip());
        card.addView(strip, new LinearLayout.LayoutParams(-1, dp(5)));

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(dp(20), dp(18), dp(20), dp(16));
        card.addView(inner, new LinearLayout.LayoutParams(-1, -2));

        TextView titleView = text(title, 21, TEXT, Typeface.BOLD);
        titleView.setSingleLine(true);
        titleView.setEllipsize(TextUtils.TruncateAt.END);
        inner.addView(titleView, new LinearLayout.LayoutParams(-1, -2));

        if (subtitle != null && !subtitle.trim().isEmpty()) {
            TextView subtitleView = text(subtitle, 13, MUTED, Typeface.NORMAL);
            subtitleView.setSingleLine(true);
            subtitleView.setEllipsize(TextUtils.TruncateAt.END);
            inner.addView(subtitleView, new LinearLayout.LayoutParams(-1, -2));
        }

        if (body != null) {
            LinearLayout.LayoutParams bodyParams = spacedParams(-1, -2, 16);
            ViewGroup.LayoutParams requestedParams = body.getLayoutParams();
            if (requestedParams != null && requestedParams.height > 0) {
                bodyParams.height = requestedParams.height;
            }
            inner.addView(body, bodyParams);
        }

        LinearLayout buttonRow = new LinearLayout(this);
        buttonRow.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        buttonRow.setBaselineAligned(false);
        inner.addView(buttonRow, spacedParams(-1, dp(44), 16));
        if (actions != null) {
            for (DialogAction action : actions) {
                buttonRow.addView(dialogButton(dialog, action), dialogButtonParams());
            }
        }

        dialog.setView(card, 0, 0, 0, 0);
        dialog.setOnShowListener(view -> {
            android.view.Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                window.setDimAmount(darkMode ? 0.76f : 0.42f);
                int width = Math.min(getResources().getDisplayMetrics().widthPixels - dp(28), dp(430));
                window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
            animateDialogPanel(card);
        });
        dialog.show();
        return dialog;
    }

    private TextView dialogButton(AlertDialog dialog, DialogAction action) {
        TextView button = text(action.label, 12, action.primary ? ON_ACCENT : action.tint, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setSingleLine(true);
        button.setEllipsize(TextUtils.TruncateAt.END);
        button.setMinWidth(dp(70));
        button.setPadding(dp(12), 0, dp(12), 0);
        int fill = action.primary ? action.tint : alphaColor(action.tint, darkMode ? 30 : 18);
        int stroke = action.primary ? action.tint : alphaColor(action.tint, darkMode ? 92 : 70);
        button.setBackground(rounded(fill, dp(17), action.primary ? 0 : 1, stroke));
        button.setClickable(true);
        attachPressAnimation(button);
        button.setOnClickListener(view -> {
            if (action.callback == null) {
                dialog.dismiss();
            } else {
                action.callback.onClick(dialog);
            }
        });
        return button;
    }

    private LinearLayout dialogBody() {
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        return body;
    }

    private LinearLayout infoCard(String label, String message, int tint) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackground(rounded(alphaBlend(SURFACE, tint, darkMode ? 10 : 5), dp(14), 1, alphaColor(tint, darkMode ? 76 : 56)));

        TextView labelView = text(label, 12, tint, Typeface.BOLD);
        labelView.setSingleLine(true);
        labelView.setEllipsize(TextUtils.TruncateAt.END);
        card.addView(labelView, new LinearLayout.LayoutParams(-1, -2));

        TextView messageView = text(message, 13, TEXT, Typeface.NORMAL);
        messageView.setMaxLines(4);
        messageView.setEllipsize(TextUtils.TruncateAt.END);
        card.addView(messageView, new LinearLayout.LayoutParams(-1, -2));
        return card;
    }

    private LinearLayout historyRow(Track track, int index, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(8), dp(12), dp(8));
        row.setBackground(rounded(alphaBlend(SURFACE, index == 0 ? ACCENT : BLUE, darkMode ? 10 : 5), dp(14), 1, index == 0 ? alphaColor(ACCENT, 100) : BORDER));
        row.setClickable(true);
        row.setOnClickListener(listener);
        attachPressAnimation(row);

        TextView badge = text(index == 0 ? "1" : trackInitial(track), 16, BADGE_TEXT, Typeface.BOLD);
        badge.setGravity(Gravity.CENTER);
        badge.setBackground(albumBadge(track.albumId));
        row.addView(badge, new LinearLayout.LayoutParams(dp(46), dp(46)));

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setPadding(dp(12), 0, dp(8), 0);
        row.addView(labels, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView title = text(track.title, 15, TEXT, Typeface.BOLD);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        labels.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView subtitle = text(track.artist + " - " + track.album, 12, MUTED, Typeface.NORMAL);
        subtitle.setSingleLine(true);
        subtitle.setEllipsize(TextUtils.TruncateAt.END);
        labels.addView(subtitle, new LinearLayout.LayoutParams(-1, -2));

        TextView duration = text(track.formattedDuration(), 12, index == 0 ? ACCENT : MUTED, Typeface.BOLD);
        row.addView(duration, new LinearLayout.LayoutParams(-2, -2));
        return row;
    }

    private LinearLayout playlistChoiceRow(Playlist playlist, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(8), dp(12), dp(8));
        row.setBackground(rounded(alphaBlend(SURFACE, WARM, darkMode ? 9 : 5), dp(14), 1, BORDER));
        row.setClickable(true);
        row.setOnClickListener(listener);
        attachPressAnimation(row);

        TextView badge = text("", 16, BADGE_TEXT, Typeface.BOLD);
        badge.setGravity(Gravity.CENTER);
        badge.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_playlist, 0, 0, 0);
        badge.setBackground(rounded(WARM, dp(14), 0, 0));
        row.addView(badge, new LinearLayout.LayoutParams(dp(46), dp(46)));

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setPadding(dp(12), 0, 0, 0);
        row.addView(labels, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView title = text(playlist.name, 15, TEXT, Typeface.BOLD);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        labels.addView(title, new LinearLayout.LayoutParams(-1, -2));
        labels.addView(text(countLabel(playlist.trackUris.size(), "musica", "musicas"), 12, MUTED, Typeface.NORMAL), new LinearLayout.LayoutParams(-1, -2));
        return row;
    }

    private LinearLayout artistAlbumHeader(Track track) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        row.setBackground(rounded(alphaBlend(SURFACE, BLUE, darkMode ? 10 : 5), dp(16), 1, alphaColor(BLUE, darkMode ? 82 : 64)));

        TextView badge = text(trackInitial(track), 24, BADGE_TEXT, Typeface.BOLD);
        badge.setGravity(Gravity.CENTER);
        badge.setBackground(albumBadge(track.albumId));
        row.addView(badge, new LinearLayout.LayoutParams(dp(68), dp(68)));

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setPadding(dp(14), 0, 0, 0);
        row.addView(labels, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView artist = text(track.artist, 18, TEXT, Typeface.BOLD);
        artist.setSingleLine(true);
        artist.setEllipsize(TextUtils.TruncateAt.END);
        labels.addView(artist, new LinearLayout.LayoutParams(-1, -2));

        TextView title = text(track.title, 13, MUTED, Typeface.NORMAL);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        labels.addView(title, new LinearLayout.LayoutParams(-1, -2));
        return row;
    }

    private LinearLayout statBlock(String value, String label, int tint) {
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);
        block.setGravity(Gravity.CENTER);
        block.setPadding(dp(8), 0, dp(8), 0);
        block.setBackground(rounded(alphaBlend(SURFACE, tint, darkMode ? 12 : 6), dp(14), 1, alphaColor(tint, darkMode ? 84 : 60)));
        TextView valueView = text(value, 15, TEXT, Typeface.BOLD);
        valueView.setGravity(Gravity.CENTER);
        valueView.setSingleLine(true);
        valueView.setEllipsize(TextUtils.TruncateAt.END);
        block.addView(valueView, new LinearLayout.LayoutParams(-1, -2));
        TextView labelView = text(label, 12, tint, Typeface.BOLD);
        labelView.setGravity(Gravity.CENTER);
        block.addView(labelView, new LinearLayout.LayoutParams(-1, -2));
        return block;
    }

    private LinearLayout.LayoutParams dialogButtonParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, dp(36));
        params.setMargins(dp(5), 0, 0, 0);
        return params;
    }

    private LinearLayout.LayoutParams spacedParams(int width, int height, int topMarginDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
        params.setMargins(0, dp(topMarginDp), 0, 0);
        return params;
    }

    private int dialogScrollHeight(int desiredDp, int maxDp) {
        int screenLimit = Math.max(dp(180), getResources().getDisplayMetrics().heightPixels - dp(250));
        return Math.min(Math.min(dp(desiredDp), dp(maxDp)), screenLimit);
    }

    private void animateDialogPanel(View view) {
        view.setAlpha(0f);
        view.setTranslationY(dp(24));
        view.setScaleX(0.96f);
        view.setScaleY(0.96f);
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(220)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }

    private GradientDrawable dialogBackground() {
        int[] colors = darkMode
                ? new int[] { 0xFF2A2F35, 0xFF35363C }
                : new int[] { 0xFFFFFFFF, 0xFFF1F5FA };
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TL_BR, colors);
        drawable.setCornerRadius(dp(22));
        drawable.setStroke(dp(1), BORDER);
        return drawable;
    }

    private GradientDrawable dialogAccentStrip() {
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[] { ACCENT, BLUE, WARM });
        drawable.setCornerRadii(new float[] { dp(22), dp(22), dp(22), dp(22), 0, 0, 0, 0 });
        return drawable;
    }

    private interface DialogCallback {
        void onClick(AlertDialog dialog);
    }

    private static class DialogAction {
        final String label;
        final int tint;
        final boolean primary;
        final DialogCallback callback;

        DialogAction(String label, int tint, boolean primary, DialogCallback callback) {
            this.label = label;
            this.tint = tint;
            this.primary = primary;
            this.callback = callback;
        }
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
            expandedPlayer.animate().cancel();
            expandedPlayer.setAlpha(0f);
            expandedPlayer.setTranslationY(dp(36));
            expandedPlayer.setVisibility(View.VISIBLE);
            expandedPlayer.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(240)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
        }
    }

    private void hideExpandedPlayer() {
        if (expandedPlayer != null) {
            expandedPlayer.animate().cancel();
            expandedPlayer.animate()
                    .alpha(0f)
                    .translationY(dp(36))
                    .setDuration(200)
                    .withEndAction(() -> {
                        expandedPlayer.setVisibility(View.GONE);
                        expandedPlayer.setAlpha(1f);
                        expandedPlayer.setTranslationY(0f);
                    })
                    .start();
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
            lastAnimatedTrackUri = "";
            lastHistoryTrackUri = "";
            syncListPlaybackState(null, false);
            return;
        }
        Track track = playbackService.getCurrentTrack();
        boolean playing = playbackService.isPlaying();
        boolean trackChanged = !track.uri.equals(lastAnimatedTrackUri);
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
        if (trackChanged) {
            animateTrackChange(track);
            rememberPlayedTrack(track);
        }
        syncListPlaybackState(track, playing);
    }

    private Track currentTrackOrNull() {
        return playbackService == null ? null : playbackService.getCurrentTrack();
    }

    private void animateTrackChange(Track track) {
        if (track == null) {
            return;
        }
        lastAnimatedTrackUri = track.uri;
        animateSoftPop(nowArtworkFrame);
        animateSoftPop(nowTitle);
        animateSoftPop(expandedArtworkFrame);
        animateSoftPop(expandedTitle);
    }

    private void animateSoftPop(View view) {
        if (view == null) {
            return;
        }
        view.animate().cancel();
        view.setAlpha(0.45f);
        view.setScaleX(0.94f);
        view.setScaleY(0.94f);
        view.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(260)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }

    private void showTrackOptions(Track track, Playlist playlistContext) {
        PopupMenu popup = new PopupMenu(this, listView);
        Menu menu = popup.getMenu();
        menu.add(0, 1, 0, "Adicionar a playlist");
        menu.add(0, 2, 1, "Tocar a seguir");
        menu.add(0, 3, 2, "Favoritar");
        menu.add(0, 5, 3, "Letras");
        menu.add(0, 6, 4, "Artista e album");
        if (playlistContext != null) {
            menu.add(0, 4, 5, "Remover da playlist");
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
            } else if (item.getItemId() == 5) {
                showLyricsDialog(track);
            } else if (item.getItemId() == 6) {
                showArtistAlbumDialog(track);
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
        LinearLayout list = dialogBody();
        final AlertDialog[] holder = new AlertDialog[1];
        if (playlists.isEmpty()) {
            list.addView(infoCard("Nenhuma playlist", "Crie uma playlist para salvar esta musica.", WARM));
        } else {
            for (int i = 0; i < playlists.size(); i++) {
                Playlist playlist = playlists.get(i);
                list.addView(playlistChoiceRow(playlist, view -> {
                    if (holder[0] != null) {
                        holder[0].dismiss();
                    }
                    playlistStore.addTrack(playlist.id, track);
                    toast("Musica adicionada");
                    refreshCurrentView();
                }), spacedParams(-1, dp(66), i == 0 ? 0 : 8));
            }
        }
        holder[0] = showStyledDialog(
                "Adicionar a playlist",
                track.title,
                list,
                new DialogAction("Fechar", MUTED, false, AlertDialog::dismiss),
                new DialogAction("Nova", ACCENT, true, dialog -> {
                    dialog.dismiss();
                    showCreatePlaylistDialog(track);
                })
        );
    }

    private void showCreatePlaylistDialog(Track trackToAdd) {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("Nome da playlist");
        input.setTextColor(TEXT);
        input.setHintTextColor(MUTED);
        input.setPadding(dp(16), dp(12), dp(16), dp(12));
        input.setBackground(rounded(SURFACE_ALT, dp(14), 1, BORDER));
        LinearLayout body = dialogBody();
        body.addView(infoCard("Organizacao", trackToAdd == null ? "Crie uma playlist para reunir suas musicas favoritas." : "A musica selecionada sera adicionada automaticamente.", WARM));
        body.addView(input, spacedParams(-1, dp(54), 12));
        showStyledDialog(
                "Nova playlist",
                "Organize sua biblioteca",
                body,
                new DialogAction("Cancelar", MUTED, false, AlertDialog::dismiss),
                new DialogAction("Criar", ACCENT, true, dialog -> {
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
                })
        );
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
                LinearLayout body = dialogBody();
                body.addView(infoCard("Sem apagar musicas", "A playlist sera removida, mas suas musicas continuam no aparelho.", ROSE));
                showStyledDialog(
                        "Apagar playlist",
                        playlist.name,
                        body,
                        new DialogAction("Cancelar", MUTED, false, AlertDialog::dismiss),
                        new DialogAction("Apagar", ROSE, true, dialog -> {
                            playlistStore.deletePlaylist(playlist.id);
                            dialog.dismiss();
                            showPlaylists();
                        })
                );
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
        input.setTextColor(TEXT);
        input.setHintTextColor(MUTED);
        input.setPadding(dp(16), dp(12), dp(16), dp(12));
        input.setBackground(rounded(SURFACE_ALT, dp(14), 1, BORDER));
        LinearLayout body = dialogBody();
        body.addView(input, new LinearLayout.LayoutParams(-1, dp(54)));
        showStyledDialog(
                "Renomear playlist",
                "Atualize o nome da lista",
                body,
                new DialogAction("Cancelar", MUTED, false, AlertDialog::dismiss),
                new DialogAction("Salvar", ACCENT, true, dialog -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        playlistStore.renamePlaylist(playlist.id, name);
                        dialog.dismiss();
                        showPlaylists();
                    }
                })
        );
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

    private void rememberPlayedTrack(Track track) {
        if (track == null || uiPrefs == null || track.uri.equals(lastHistoryTrackUri)) {
            return;
        }
        lastHistoryTrackUri = track.uri;
        ArrayList<String> uris = new ArrayList<>();
        uris.add(track.uri);
        String saved = uiPrefs.getString(KEY_HISTORY, "");
        if (saved != null && !saved.isEmpty()) {
            String[] parts = saved.split("\n");
            for (String uri : parts) {
                if (!uri.trim().isEmpty() && !uri.equals(track.uri) && uris.size() < MAX_HISTORY) {
                    uris.add(uri);
                }
            }
        }
        uiPrefs.edit().putString(KEY_HISTORY, TextUtils.join("\n", uris)).apply();
    }

    private ArrayList<Track> historyTracks() {
        ArrayList<Track> tracks = new ArrayList<>();
        if (uiPrefs == null) {
            return tracks;
        }
        HashMap<String, Track> byUri = new HashMap<>();
        for (Track track : allTracks) {
            byUri.put(track.uri, track);
        }
        String saved = uiPrefs.getString(KEY_HISTORY, "");
        if (saved == null || saved.isEmpty()) {
            return tracks;
        }
        String[] parts = saved.split("\n");
        for (String uri : parts) {
            Track track = byUri.get(uri);
            if (track != null) {
                tracks.add(track);
            }
        }
        return tracks;
    }

    private String lyricsFor(Track track) {
        if (uiPrefs == null || track == null) {
            return "";
        }
        return uiPrefs.getString(lyricsKey(track), "");
    }

    private String lyricsKey(Track track) {
        return KEY_LYRICS_PREFIX + Integer.toHexString(track.uri.hashCode());
    }

    private ArrayList<Track> tracksByArtist(String artist) {
        ArrayList<Track> tracks = new ArrayList<>();
        for (Track item : allTracks) {
            if (sameText(item.artist, artist)) {
                tracks.add(item);
            }
        }
        return tracks;
    }

    private ArrayList<Track> tracksByAlbum(String album, String artist) {
        ArrayList<Track> tracks = new ArrayList<>();
        for (Track item : allTracks) {
            if (sameText(item.album, album) && sameText(item.artist, artist)) {
                tracks.add(item);
            }
        }
        return tracks;
    }

    private boolean sameText(String first, String second) {
        return first != null && second != null && first.trim().equalsIgnoreCase(second.trim());
    }

    private String trackLine(Track track) {
        return track.title + " - " + track.artist;
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
        if (heroMetric != null) {
            heroMetric.setText(countLabel(allTracks.size(), "musica", "musicas") + " - " + formatLibraryDuration(totalDuration));
        }
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
        pill.setBackground(rounded(alphaBlend(SURFACE, tint, darkMode ? 14 : 6), dp(15), 1, alphaColor(tint, darkMode ? 88 : 58)));
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
        tab.setTextColor(selected ? ON_ACCENT : MUTED);
        tab.setBackground(rounded(selected ? ACCENT : Color.TRANSPARENT, dp(20), 0, 0));
        tab.animate().scaleX(selected ? 1.02f : 1f).scaleY(selected ? 1.02f : 1f).setDuration(160).start();
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
        button.setMinimumWidth(size);
        button.setMinimumHeight(size);
        button.setPadding(dp(8), dp(8), dp(8), dp(8));
        attachPressAnimation(button);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
        params.setMargins(dp(4), 0, dp(4), 0);
        button.setLayoutParams(params);
        return button;
    }

    private TextView actionChip(String label, int tint, View.OnClickListener listener) {
        TextView chip = text(label, 12, TEXT, Typeface.BOLD);
        chip.setGravity(Gravity.CENTER);
        chip.setSingleLine(true);
        chip.setEllipsize(TextUtils.TruncateAt.END);
        chip.setPadding(dp(8), 0, dp(8), 0);
        chip.setClickable(true);
        chip.setBackground(rounded(alphaColor(tint, darkMode ? 34 : 22), dp(18), 1, alphaColor(tint, darkMode ? 92 : 70)));
        chip.setOnClickListener(listener);
        attachPressAnimation(chip);
        return chip;
    }

    private LinearLayout.LayoutParams chipParams(int columns) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1f);
        params.setMargins(dp(3), 0, dp(3), 0);
        return params;
    }

    private void attachPressAnimation(View view) {
        view.setOnTouchListener((pressedView, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                pressedView.animate().scaleX(0.95f).scaleY(0.95f).setDuration(90).start();
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                pressedView.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
            }
            return false;
        });
    }

    private int alphaColor(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    private int alphaBlend(int base, int overlay, int overlayPercent) {
        int percent = Math.max(0, Math.min(100, overlayPercent));
        int inverse = 100 - percent;
        int red = (((base >> 16) & 0xFF) * inverse + ((overlay >> 16) & 0xFF) * percent) / 100;
        int green = (((base >> 8) & 0xFF) * inverse + ((overlay >> 8) & 0xFF) * percent) / 100;
        int blue = ((base & 0xFF) * inverse + (overlay & 0xFF) * percent) / 100;
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
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

    private GradientDrawable appBackground() {
        int[] colors = darkMode
                ? new int[] { 0xFF07100D, 0xFF091018, 0xFF120E18 }
                : new int[] { 0xFFF2FBF8, 0xFFF7F9FC, 0xFFFFF8EF };
        return new GradientDrawable(GradientDrawable.Orientation.TL_BR, colors);
    }

    private GradientDrawable heroBackground() {
        int[] colors = darkMode
                ? new int[] { 0xFF13231F, 0xFF171E2A, 0xFF2A2027 }
                : new int[] { 0xFFFFFFFF, 0xFFE8F7F1, 0xFFFFF2E4 };
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TL_BR, colors);
        drawable.setCornerRadius(dp(22));
        drawable.setStroke(dp(1), alphaColor(ACCENT, darkMode ? 70 : 42));
        return drawable;
    }

    private GradientDrawable emptyStateBackground() {
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[] {
                alphaBlend(SURFACE, BLUE, darkMode ? 8 : 4),
                alphaBlend(SURFACE, ACCENT, darkMode ? 8 : 4)
        });
        drawable.setCornerRadius(dp(18));
        drawable.setStroke(dp(1), BORDER);
        return drawable;
    }

    private GradientDrawable playerBackground() {
        int[] colors = darkMode
                ? new int[] { 0xFF151B20, 0xFF202734, 0xFF211C25 }
                : new int[] { 0xFFFFFFFF, 0xFFEAF7F2, 0xFFF7F2EA };
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors);
        drawable.setCornerRadii(new float[] { dp(24), dp(24), dp(24), dp(24), 0, 0, 0, 0 });
        drawable.setStroke(dp(1), BORDER);
        return drawable;
    }

    private GradientDrawable expandedArtworkBackground() {
        int[] colors = darkMode
                ? new int[] { 0xFF314048, 0xFF171A21, 0xFF3DDB9A }
                : new int[] { 0xFFDCEFEA, 0xFFFFFFFF, 0xFF7DE0C0 };
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TL_BR, colors);
        drawable.setCornerRadius(dp(22));
        drawable.setStroke(dp(1), BORDER);
        return drawable;
    }

    private GradientDrawable rowBackground(boolean active) {
        int fill = active ? ACTIVE_ROW : alphaBlend(SURFACE, BLUE, darkMode ? 3 : 1);
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[] {
                fill,
                active ? alphaBlend(fill, ACCENT, darkMode ? 12 : 5) : SURFACE
        });
        drawable.setCornerRadius(dp(16));
        drawable.setStroke(dp(1), active ? ACCENT : BORDER);
        return drawable;
    }

    private GradientDrawable extensionRowBackground(boolean active, int tint) {
        int fill = active ? alphaBlend(SURFACE, tint, darkMode ? 18 : 10) : SURFACE;
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(14));
        drawable.setStroke(dp(1), active ? alphaColor(tint, darkMode ? 120 : 96) : BORDER);
        return drawable;
    }

    private GradientDrawable playlistRowBackground() {
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[] {
                alphaBlend(SURFACE, WARM, darkMode ? 10 : 5),
                alphaBlend(SURFACE, ACCENT, darkMode ? 5 : 2)
        });
        drawable.setCornerRadius(dp(16));
        drawable.setStroke(dp(1), alphaColor(WARM, darkMode ? 78 : 54));
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String formatMs(int milliseconds) {
        int totalSeconds = Math.max(0, milliseconds / 1000);
        return String.format(Locale.ROOT, "%d:%02d", totalSeconds / 60, totalSeconds % 60);
    }

    private String formatBandFrequency(int milliHz) {
        int hz = Math.max(0, milliHz / 1000);
        if (hz >= 1000) {
            return String.format(Locale.ROOT, "%.1f kHz", hz / 1000f);
        }
        return hz + " Hz";
    }

    private String formatDb(short milliBel) {
        return String.format(Locale.ROOT, "%+.1f dB", milliBel / 100f);
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
        final ImageView artwork;
        final TextView badge;
        final TextView title;
        final TextView subtitle;
        final TextView duration;
        final ImageButton more;

        TrackRow() {
            root = new LinearLayout(MainActivity.this);
            root.setGravity(Gravity.CENTER_VERTICAL);
            root.setPadding(dp(12), dp(10), dp(8), dp(10));
            root.setBackground(rowBackground(false));
            root.setLayoutParams(new AbsListView.LayoutParams(-1, dp(90)));
            attachPressAnimation(root);

            FrameLayout cover = new FrameLayout(MainActivity.this);
            cover.setBackground(rounded(SURFACE_RAISED, dp(16), 1, BORDER));
            root.addView(cover, new LinearLayout.LayoutParams(dp(58), dp(58)));

            artwork = new ImageView(MainActivity.this);
            artwork.setScaleType(ImageView.ScaleType.CENTER_CROP);
            artwork.setVisibility(View.GONE);
            cover.addView(artwork, new FrameLayout.LayoutParams(-1, -1));

            badge = text("M", 18, BADGE_TEXT, Typeface.BOLD);
            badge.setGravity(Gravity.CENTER);
            cover.addView(badge, new FrameLayout.LayoutParams(-1, -1));

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
            duration.setGravity(Gravity.CENTER);
            duration.setPadding(dp(8), 0, dp(8), 0);
            root.addView(duration, new LinearLayout.LayoutParams(-2, dp(28)));

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
            duration.setBackground(rounded(alphaColor(active ? ACCENT : MUTED, active ? 36 : (darkMode ? 22 : 14)), dp(13), 1, alphaColor(active ? ACCENT : MUTED, active ? 90 : (darkMode ? 44 : 30))));
            setArtwork(artwork, badge, track);
            if (active && playbackService != null && playbackService.isPlaying() && badge.getVisibility() == View.VISIBLE) {
                badge.setText(">");
            }
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
                EXT_DOWNLOAD,
                "D",
                "Download offline",
                "Salva links diretos de audio na pasta Music/PlayerMusic",
                "Colar link",
                ACCENT,
                true
        ));
        items.add(new ExtensionItem(
                EXT_REFRESH,
                "R",
                "Atualizar biblioteca",
                "Recarrega musicas locais depois de baixar arquivos",
                "Atualizar",
                WARM,
                true
        ));
        items.add(new ExtensionItem(
                EXT_EQUALIZER,
                "EQ",
                "Equalizador",
                playbackService != null && playbackService.hasTrack() ? "Ajuste graves, medios e agudos da faixa atual" : "Toque uma musica para liberar os controles",
                "Abrir",
                BLUE,
                true
        ));
        items.add(new ExtensionItem(
                EXT_HISTORY,
                "H",
                "Historico",
                "Veja e retome as ultimas musicas tocadas",
                "Ver",
                ROSE,
                true
        ));
        items.add(new ExtensionItem(
                EXT_SNAPTUBE,
                "S",
                "Snaptube",
                snaptubeReady ? "Instalado - abrir app externo" : "Nao instalado neste aparelho",
                snaptubeReady ? "Abrir" : "Ausente",
                WARM,
                snaptubeReady
        ));
        items.add(new ExtensionItem(
                EXT_CUSTOM,
                customPackage.isEmpty() ? "+" : "E",
                customPackage.isEmpty() ? "Adicionar extensao" : customPackage,
                customPackage.isEmpty() ? "Cadastre o pacote Android da extensao" : (customReady ? "Instalada - abrir app externo" : "Pacote salvo, mas nao encontrado"),
                customPackage.isEmpty() ? "Cadastrar" : (customReady ? "Abrir" : "Editar"),
                ACCENT,
                customPackage.isEmpty() || customReady
        ));
        items.add(new ExtensionItem(
                EXT_SAFETY,
                "!",
                "Uso responsavel",
                "Importe apenas audio que voce tem direito de usar",
                "Ver",
                BLUE,
                true
        ));
        return items;
    }

    private static class ExtensionItem {
        final int actionId;
        final String badge;
        final String title;
        final String subtitle;
        final String action;
        final int tint;
        final boolean active;

        ExtensionItem(int actionId, String badge, String title, String subtitle, String action, int tint, boolean active) {
            this.actionId = actionId;
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
            root.setPadding(dp(14), dp(10), dp(12), dp(10));
            root.setBackground(extensionRowBackground(false, ACCENT));
            root.setLayoutParams(new AbsListView.LayoutParams(-1, dp(100)));
            attachPressAnimation(root);

            badge = text("E", 18, BADGE_TEXT, Typeface.BOLD);
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
            subtitle.setMaxLines(2);
            subtitle.setEllipsize(TextUtils.TruncateAt.END);
            labels.addView(subtitle);

            action = text("", 12, TEXT, Typeface.BOLD);
            action.setGravity(Gravity.CENTER);
            action.setPadding(dp(12), 0, dp(12), 0);
            root.addView(action, new LinearLayout.LayoutParams(-2, dp(36)));
        }

        void bind(ExtensionItem item) {
            badge.setText(item.badge);
            badge.setBackground(rounded(item.tint, dp(14), 0, 0));
            title.setText(item.title);
            title.setTextColor(item.active ? TEXT : MUTED);
            subtitle.setText(item.subtitle);
            subtitle.setTextColor(item.active ? MUTED : SOFT_MUTED);
            action.setText(item.action);
            action.setTextColor(item.active ? TEXT : MUTED);
            action.setBackground(rounded(alphaColor(item.tint, item.active ? 42 : 24), dp(16), 1, alphaColor(item.tint, item.active ? 110 : 60)));
            root.setBackground(extensionRowBackground(item.active, item.tint));
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
            root.setPadding(dp(14), dp(10), dp(8), dp(10));
            root.setBackground(playlistRowBackground());
            root.setLayoutParams(new AbsListView.LayoutParams(-1, dp(92)));
            attachPressAnimation(root);

            badge = text("", 18, BADGE_TEXT, Typeface.BOLD);
            badge.setGravity(Gravity.CENTER);
            badge.setBackground(rounded(WARM, dp(16), 0, 0));
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
            count.setText(countLabel(playlist.trackUris.size(), "musica salva", "musicas salvas"));
            root.setBackground(playlistRowBackground());
            more.setOnClickListener(view -> showPlaylistOptions(playlist));
        }
    }
}

