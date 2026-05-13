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
import android.view.animation.AccelerateDecelerateInterpolator;
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

public class PlayerActivity extends Activity {
    private static final int REQUEST_AUDIO = 713;
    private static final int TAB_LIBRARY = 0;
    private static final int TAB_PLAYLISTS = 1;
    private static final int TAB_QUEUE = 2;
    private static final int TAB_TOOLS = 3;
    private static final int MAX_REDIRECTS = 5;
    private static final int MAX_HISTORY = 50;
    private static final String UI_PREFS = "player_music_ui";
    private static final String EXTENSION_PREFS = "player_music_extensions";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_HISTORY = "history";
    private static final String KEY_CUSTOM_EXTENSION_PACKAGE = "custom_extension_package";
    private static final String KEY_LYRICS_PREFIX = "lyrics_";
    private static final String SNAPTUBE_PACKAGE = "com.snaptube.premium";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final MusicRepository repository = new MusicRepository();
    private final ArrayList<Track> allTracks = new ArrayList<>();
    private final ArrayList<Track> visibleTracks = new ArrayList<>();
    private final TrackAdapter trackAdapter = new TrackAdapter();
    private final PlaylistAdapter playlistAdapter = new PlaylistAdapter();
    private final ToolAdapter toolAdapter = new ToolAdapter();

    private PlaylistStore playlistStore;
    private SharedPreferences uiPrefs;
    private SharedPreferences extensionPrefs;
    private MusicPlaybackService playbackService;
    private boolean bound;
    private boolean darkMode;
    private boolean libraryLoading;
    private boolean miniSeeking;
    private boolean expandedSeeking;
    private int currentTab = TAB_LIBRARY;
    private Playlist openPlaylist;
    private ArrayList<Track> pendingQueue;
    private int pendingIndex;
    private String lastRenderedUri = "";
    private String lastHistoryUri = "";
    private float miniTouchStartY;
    private float expandedTouchStartY;

    private int bg;
    private int surface;
    private int surfaceAlt;
    private int raised;
    private int text;
    private int muted;
    private int soft;
    private int accent;
    private int warm;
    private int blue;
    private int rose;
    private int border;
    private int seekBg;
    private int active;
    private int onAccent;
    private int badgeText;

    private TextView subtitleView;
    private TextView heroTitle;
    private TextView heroSubtitle;
    private TextView heroMeta;
    private TextView heroPrimary;
    private TextView heroSecondary;
    private TextView sectionTitle;
    private TextView sectionSubtitle;
    private TextView emptyView;
    private TextView tabLibrary;
    private TextView tabPlaylists;
    private TextView tabQueue;
    private TextView tabTools;
    private TextView statTracks;
    private TextView statDuration;
    private TextView statPlaylists;
    private EditText searchInput;
    private Button permissionButton;
    private ImageButton backButton;
    private ImageButton addButton;
    private ImageButton themeButton;
    private ListView listView;
    private LinearLayout miniPlayer;
    private ImageView miniArtwork;
    private TextView miniInitial;
    private TextView miniTitle;
    private TextView miniArtist;
    private TextView miniQueue;
    private TextView miniElapsed;
    private TextView miniDuration;
    private SeekBar miniSeek;
    private ImageButton miniPlay;
    private ImageButton miniShuffle;
    private ImageButton miniRepeat;
    private FrameLayout expandedPlayer;
    private ImageView expandedArtwork;
    private TextView expandedInitial;
    private TextView expandedTitle;
    private TextView expandedArtist;
    private TextView expandedAlbum;
    private TextView expandedQueue;
    private TextView expandedElapsed;
    private TextView expandedDuration;
    private SeekBar expandedSeek;
    private ImageButton expandedPlay;
    private ImageButton expandedShuffle;
    private ImageButton expandedRepeat;

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
        uiPrefs = getSharedPreferences(UI_PREFS, MODE_PRIVATE);
        extensionPrefs = getSharedPreferences(EXTENSION_PREFS, MODE_PRIVATE);
        darkMode = uiPrefs.getBoolean(KEY_DARK_MODE, isSystemDarkMode());
        applyPalette();
        applyWindowColors();
        buildUi();
        bindService(new Intent(this, MusicPlaybackService.class), serviceConnection, BIND_AUTO_CREATE);
        bound = true;
        if (hasAudioPermission()) {
            loadLibrary();
        } else {
            showPermissionState();
        }
        startTicker();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentTab == TAB_TOOLS && toolAdapter != null) {
            toolAdapter.notifyDataSetChanged();
        }
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
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_AUDIO) {
            if (hasAudioPermission()) {
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
        if (openPlaylist != null) {
            openPlaylist = null;
            showPlaylists();
            return;
        }
        super.onBackPressed();
    }

    private void applyPalette() {
        if (darkMode) {
            bg = 0xFF070B10;
            surface = 0xFF121820;
            surfaceAlt = 0xFF1C2430;
            raised = 0xFF222B38;
            text = 0xFFF6F8FB;
            muted = 0xFFAAB2C0;
            soft = 0xFF7E8796;
            accent = 0xFF39E39B;
            warm = 0xFFFFB35B;
            blue = 0xFF68B8FF;
            rose = 0xFFFF6FA9;
            border = 0xFF2D3746;
            seekBg = 0xFF303A49;
            active = 0xFF18322B;
            onAccent = 0xFF06110D;
            badgeText = 0xFF06110D;
        } else {
            bg = 0xFFF5F8FC;
            surface = 0xFFFFFFFF;
            surfaceAlt = 0xFFE9EFF6;
            raised = 0xFFFDFEFF;
            text = 0xFF111827;
            muted = 0xFF637083;
            soft = 0xFF7D8796;
            accent = 0xFF0A936B;
            warm = 0xFFB86624;
            blue = 0xFF2563EB;
            rose = 0xFFC43C75;
            border = 0xFFD8E1EC;
            seekBg = 0xFFDDE6F0;
            active = 0xFFE2F5EF;
            onAccent = 0xFFFFFFFF;
            badgeText = 0xFF06110D;
        }
    }

    private void applyWindowColors() {
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        if (Build.VERSION.SDK_INT >= 23) {
            int flags = darkMode ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= 26 && !darkMode) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            getWindow().getDecorView().setSystemUiVisibility(flags);
        }
    }

    private boolean isSystemDarkMode() {
        int mode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return mode == Configuration.UI_MODE_NIGHT_YES;
    }

    private void buildUi() {
        FrameLayout shell = new FrameLayout(this);
        shell.setBackground(appBackground());
        setContentView(shell);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(0, 0, 0, 0);
        shell.addView(root, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(18), dp(14), dp(18), dp(10));
        root.addView(header, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(top, new LinearLayout.LayoutParams(-1, dp(48)));

        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        top.addView(brand, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView title = label("PlayerMusic", 30, text, Typeface.BOLD);
        title.setSingleLine(true);
        brand.addView(title);
        subtitleView = label("Player local limpo, rapido e offline", 13, muted, Typeface.NORMAL);
        subtitleView.setSingleLine(true);
        subtitleView.setEllipsize(TextUtils.TruncateAt.END);
        brand.addView(subtitleView);

        themeButton = iconButton(darkMode ? R.drawable.ic_light_mode : R.drawable.ic_dark_mode, "Tema", surfaceAlt, text, dp(42));
        themeButton.setOnClickListener(view -> toggleTheme());
        top.addView(themeButton);

        backButton = iconButton(R.drawable.ic_arrow_back, "Voltar", surfaceAlt, text, dp(42));
        backButton.setVisibility(View.GONE);
        backButton.setOnClickListener(view -> {
            openPlaylist = null;
            showPlaylists();
        });
        top.addView(backButton);

        addButton = iconButton(R.drawable.ic_add, "Adicionar", accent, onAccent, dp(42));
        addButton.setVisibility(View.GONE);
        top.addView(addButton);

        permissionButton = new Button(this);
        permissionButton.setAllCaps(false);
        permissionButton.setText("Permitir");
        permissionButton.setTextSize(13);
        permissionButton.setTypeface(Typeface.DEFAULT_BOLD);
        permissionButton.setTextColor(onAccent);
        permissionButton.setBackground(rounded(accent, dp(18), 0, 0));
        permissionButton.setVisibility(View.GONE);
        permissionButton.setOnClickListener(view -> requestAudioPermission());
        top.addView(permissionButton, new LinearLayout.LayoutParams(-2, dp(38)));

        header.addView(heroPanel(), marginParams(-1, -2, 14, 0, 0, 0));
        header.addView(statsRow(), marginParams(-1, dp(40), 12, 0, 0, 0));
        header.addView(searchBox(), marginParams(-1, dp(48), 14, 0, 0, 0));
        header.addView(tabs(), marginParams(-1, dp(48), 12, 0, 0, 0));
        header.addView(sectionHeader(), marginParams(-1, -2, 12, 2, 0, 2));

        FrameLayout content = new FrameLayout(this);
        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1f));

        listView = new ListView(this);
        listView.setDivider(new ColorDrawable(Color.TRANSPARENT));
        listView.setDividerHeight(dp(8));
        listView.setSelector(android.R.color.transparent);
        listView.setCacheColorHint(Color.TRANSPARENT);
        listView.setClipToPadding(false);
        listView.setPadding(dp(14), dp(4), dp(14), dp(18));
        content.addView(listView, new FrameLayout.LayoutParams(-1, -1));

        emptyView = label("", 15, muted, Typeface.NORMAL);
        emptyView.setGravity(Gravity.CENTER);
        emptyView.setPadding(dp(20), dp(18), dp(20), dp(18));
        emptyView.setBackground(emptyBackground());
        FrameLayout.LayoutParams emptyParams = new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER);
        emptyParams.setMargins(dp(22), 0, dp(22), 0);
        content.addView(emptyView, emptyParams);
        listView.setEmptyView(emptyView);

        buildMiniPlayer(root);
        buildExpandedPlayer(shell);
        refreshTabs();
        updateSummary();
    }

    private View heroPanel() {
        LinearLayout hero = new LinearLayout(this);
        hero.setGravity(Gravity.CENTER_VERTICAL);
        hero.setPadding(dp(16), dp(14), dp(14), dp(14));
        hero.setBackground(heroBackground());

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        hero.addView(copy, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView eyebrow = label("PLAYERMUSIC", 11, accent, Typeface.BOLD);
        copy.addView(eyebrow);

        heroTitle = label("Sua musica, do seu jeito", 22, text, Typeface.BOLD);
        heroTitle.setSingleLine(true);
        heroTitle.setEllipsize(TextUtils.TruncateAt.END);
        copy.addView(heroTitle);

        heroSubtitle = label("Biblioteca local organizada e pronta para tocar.", 13, muted, Typeface.NORMAL);
        heroSubtitle.setMaxLines(2);
        heroSubtitle.setEllipsize(TextUtils.TruncateAt.END);
        copy.addView(heroSubtitle);

        heroMeta = label("0 musicas", 12, text, Typeface.BOLD);
        heroMeta.setSingleLine(true);
        heroMeta.setPadding(dp(10), 0, dp(10), 0);
        heroMeta.setBackground(pillBg(accent));
        copy.addView(heroMeta, marginParams(-2, dp(30), 10, 0, 0, 0));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.VERTICAL);
        hero.addView(actions, new LinearLayout.LayoutParams(dp(112), -2));

        heroPrimary = actionChip("Tocar", accent, true, view -> playBestSource());
        actions.addView(heroPrimary, new LinearLayout.LayoutParams(-1, dp(38)));

        heroSecondary = actionChip("Baixar", blue, false, view -> showImportDialog());
        actions.addView(heroSecondary, marginParams(-1, dp(34), 8, 0, 0, 0));
        return hero;
    }

    private View statsRow() {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        statTracks = statPill("0 musicas", accent);
        statDuration = statPill("0 min", blue);
        statPlaylists = statPill("0 listas", warm);
        row.addView(statTracks, weightParams());
        row.addView(statDuration, weightParams());
        row.addView(statPlaylists, weightParams());
        return row;
    }

    private View searchBox() {
        searchInput = new EditText(this);
        searchInput.setSingleLine(true);
        searchInput.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        searchInput.setHint("Buscar musica, artista ou album");
        searchInput.setTextColor(text);
        searchInput.setHintTextColor(muted);
        searchInput.setTextSize(14);
        searchInput.setPadding(dp(16), 0, dp(16), 0);
        searchInput.setBackground(rounded(surfaceAlt, dp(16), 1, border));
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
        return searchInput;
    }

    private View tabs() {
        LinearLayout row = new LinearLayout(this);
        row.setPadding(dp(3), dp(3), dp(3), dp(3));
        row.setBackground(rounded(alphaBlend(surfaceAlt, accent, darkMode ? 5 : 2), dp(24), 1, border));
        tabLibrary = tab("Musicas", () -> {
            openPlaylist = null;
            currentTab = TAB_LIBRARY;
            refreshCurrentView();
        });
        tabPlaylists = tab("Listas", () -> {
            currentTab = TAB_PLAYLISTS;
            showPlaylists();
        });
        tabQueue = tab("Fila", () -> {
            openPlaylist = null;
            currentTab = TAB_QUEUE;
            showQueue();
        });
        tabTools = tab("Extras", () -> {
            openPlaylist = null;
            currentTab = TAB_TOOLS;
            showTools();
        });
        row.addView(tabLibrary, weightParams());
        row.addView(tabPlaylists, weightParams());
        row.addView(tabQueue, weightParams());
        row.addView(tabTools, weightParams());
        return row;
    }

    private View sectionHeader() {
        LinearLayout section = new LinearLayout(this);
        section.setOrientation(LinearLayout.VERTICAL);
        sectionTitle = label("Biblioteca", 18, text, Typeface.BOLD);
        sectionSubtitle = label("Todas as musicas locais", 12, muted, Typeface.NORMAL);
        section.addView(sectionTitle);
        section.addView(sectionSubtitle);
        return section;
    }

    private void buildMiniPlayer(LinearLayout root) {
        miniPlayer = new LinearLayout(this);
        miniPlayer.setOrientation(LinearLayout.VERTICAL);
        miniPlayer.setPadding(dp(16), dp(12), dp(16), dp(12));
        miniPlayer.setBackground(playerBackground());
        miniPlayer.setClickable(true);
        miniPlayer.setOnClickListener(view -> showExpandedPlayer());
        miniPlayer.setOnTouchListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                miniTouchStartY = event.getRawY();
            } else if (event.getAction() == MotionEvent.ACTION_UP && miniTouchStartY - event.getRawY() > dp(42)) {
                showExpandedPlayer();
                return true;
            }
            return false;
        });
        root.addView(miniPlayer, new LinearLayout.LayoutParams(-1, -2));

        View handle = new View(this);
        handle.setBackground(rounded(alphaColor(text, darkMode ? 70 : 90), dp(2), 0, 0));
        LinearLayout.LayoutParams handleParams = new LinearLayout.LayoutParams(dp(42), dp(4));
        handleParams.gravity = Gravity.CENTER_HORIZONTAL;
        handleParams.setMargins(0, 0, 0, dp(10));
        miniPlayer.addView(handle, handleParams);

        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        miniPlayer.addView(row, new LinearLayout.LayoutParams(-1, -2));

        FrameLayout cover = new FrameLayout(this);
        cover.setBackground(rounded(raised, dp(16), 1, border));
        row.addView(cover, new LinearLayout.LayoutParams(dp(66), dp(66)));

        miniArtwork = new ImageView(this);
        miniArtwork.setScaleType(ImageView.ScaleType.CENTER_CROP);
        miniArtwork.setVisibility(View.GONE);
        cover.addView(miniArtwork, new FrameLayout.LayoutParams(-1, -1));

        miniInitial = label("M", 28, badgeText, Typeface.BOLD);
        miniInitial.setGravity(Gravity.CENTER);
        miniInitial.setBackground(albumBadge(0));
        cover.addView(miniInitial, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(12), 0, dp(8), 0);
        row.addView(info, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView label = label("TOCANDO AGORA", 10, accent, Typeface.BOLD);
        info.addView(label);
        miniTitle = label("Nada tocando", 17, text, Typeface.BOLD);
        miniTitle.setSingleLine(true);
        miniTitle.setEllipsize(TextUtils.TruncateAt.END);
        info.addView(miniTitle);
        miniArtist = label("Escolha uma musica para comecar", 13, muted, Typeface.NORMAL);
        miniArtist.setSingleLine(true);
        miniArtist.setEllipsize(TextUtils.TruncateAt.END);
        info.addView(miniArtist);

        miniQueue = label("Fila vazia", 11, text, Typeface.BOLD);
        miniQueue.setGravity(Gravity.CENTER);
        miniQueue.setPadding(dp(8), 0, dp(8), 0);
        miniQueue.setBackground(pillBg(accent));
        row.addView(miniQueue, new LinearLayout.LayoutParams(-2, dp(30)));

        miniSeek = seekBar();
        miniSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    miniElapsed.setText(formatMs(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                miniSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                miniSeeking = false;
                if (playbackService != null) {
                    playbackService.seekTo(seekBar.getProgress());
                }
            }
        });
        miniPlayer.addView(miniSeek, marginParams(-1, dp(34), 10, 0, 0, 0));

        LinearLayout time = new LinearLayout(this);
        time.setGravity(Gravity.CENTER_VERTICAL);
        miniPlayer.addView(time, new LinearLayout.LayoutParams(-1, -2));
        miniElapsed = label("0:00", 11, muted, Typeface.NORMAL);
        miniDuration = label("0:00", 11, muted, Typeface.NORMAL);
        time.addView(miniElapsed, new LinearLayout.LayoutParams(0, -2, 1f));
        time.addView(miniDuration);

        LinearLayout controls = new LinearLayout(this);
        controls.setGravity(Gravity.CENTER);
        miniPlayer.addView(controls, marginParams(-1, dp(56), 2, 0, 0, 0));
        miniShuffle = iconButton(R.drawable.ic_shuffle, "Aleatorio", alphaColor(text, darkMode ? 20 : 12), muted, dp(44));
        ImageButton previous = iconButton(R.drawable.ic_skip_previous, "Anterior", raised, text, dp(46));
        miniPlay = iconButton(R.drawable.ic_play_arrow, "Tocar", accent, onAccent, dp(58));
        ImageButton next = iconButton(R.drawable.ic_skip_next, "Proxima", raised, text, dp(46));
        miniRepeat = iconButton(R.drawable.ic_repeat, "Repetir", alphaColor(text, darkMode ? 20 : 12), muted, dp(44));
        controls.addView(miniShuffle);
        controls.addView(previous);
        controls.addView(miniPlay);
        controls.addView(next);
        controls.addView(miniRepeat);

        miniPlay.setOnClickListener(view -> togglePlayback());
        previous.setOnClickListener(view -> {
            if (playbackService != null) {
                playbackService.previous();
            }
        });
        next.setOnClickListener(view -> {
            if (playbackService != null) {
                playbackService.next();
            }
        });
        miniShuffle.setOnClickListener(view -> toggleShuffle());
        miniRepeat.setOnClickListener(view -> cycleRepeat());
    }

    private void buildExpandedPlayer(FrameLayout shell) {
        expandedPlayer = new FrameLayout(this);
        expandedPlayer.setVisibility(View.GONE);
        expandedPlayer.setClickable(true);
        expandedPlayer.setBackground(appBackground());
        expandedPlayer.setOnTouchListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                expandedTouchStartY = event.getRawY();
            } else if (event.getAction() == MotionEvent.ACTION_UP && event.getRawY() - expandedTouchStartY > dp(70)) {
                hideExpandedPlayer();
                return true;
            }
            return false;
        });
        shell.addView(expandedPlayer, new FrameLayout.LayoutParams(-1, -1));

        ScrollView scroller = new ScrollView(this);
        scroller.setFillViewport(true);
        expandedPlayer.addView(scroller, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(18), dp(18), dp(18), dp(24));
        scroller.addView(page, new ScrollView.LayoutParams(-1, -2));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        page.addView(header, new LinearLayout.LayoutParams(-1, dp(48)));

        ImageButton close = iconButton(R.drawable.ic_arrow_back, "Fechar", surfaceAlt, text, dp(42));
        close.setRotation(-90f);
        close.setOnClickListener(view -> hideExpandedPlayer());
        header.addView(close);

        TextView title = label("Tocando agora", 18, text, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1f));

        expandedQueue = label("Fila vazia", 11, text, Typeface.BOLD);
        expandedQueue.setGravity(Gravity.CENTER);
        expandedQueue.setPadding(dp(10), 0, dp(10), 0);
        expandedQueue.setBackground(pillBg(accent));
        header.addView(expandedQueue, new LinearLayout.LayoutParams(-2, dp(30)));

        FrameLayout cover = new FrameLayout(this);
        cover.setBackground(expandedCoverBackground());
        int coverSize = Math.min(getResources().getDisplayMetrics().widthPixels - dp(48), dp(340));
        LinearLayout.LayoutParams coverParams = new LinearLayout.LayoutParams(coverSize, coverSize);
        coverParams.gravity = Gravity.CENTER_HORIZONTAL;
        coverParams.setMargins(0, dp(22), 0, dp(22));
        page.addView(cover, coverParams);

        expandedArtwork = new ImageView(this);
        expandedArtwork.setScaleType(ImageView.ScaleType.CENTER_CROP);
        expandedArtwork.setVisibility(View.GONE);
        cover.addView(expandedArtwork, new FrameLayout.LayoutParams(-1, -1));

        expandedInitial = label("M", 68, badgeText, Typeface.BOLD);
        expandedInitial.setGravity(Gravity.CENTER);
        expandedInitial.setBackground(albumBadge(0));
        cover.addView(expandedInitial, new FrameLayout.LayoutParams(-1, -1));

        expandedTitle = label("Nada tocando", 24, text, Typeface.BOLD);
        expandedTitle.setGravity(Gravity.CENTER);
        expandedTitle.setSingleLine(true);
        expandedTitle.setEllipsize(TextUtils.TruncateAt.END);
        page.addView(expandedTitle, new LinearLayout.LayoutParams(-1, -2));

        expandedArtist = label("Escolha uma musica da biblioteca", 15, muted, Typeface.NORMAL);
        expandedArtist.setGravity(Gravity.CENTER);
        expandedArtist.setSingleLine(true);
        expandedArtist.setEllipsize(TextUtils.TruncateAt.END);
        page.addView(expandedArtist);

        expandedAlbum = label("Player parado", 13, soft, Typeface.NORMAL);
        expandedAlbum.setGravity(Gravity.CENTER);
        expandedAlbum.setSingleLine(true);
        expandedAlbum.setEllipsize(TextUtils.TruncateAt.END);
        page.addView(expandedAlbum);

        expandedSeek = seekBar();
        expandedSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    expandedElapsed.setText(formatMs(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                expandedSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                expandedSeeking = false;
                if (playbackService != null) {
                    playbackService.seekTo(seekBar.getProgress());
                }
            }
        });
        page.addView(expandedSeek, marginParams(-1, dp(42), 24, 0, 0, 0));

        LinearLayout time = new LinearLayout(this);
        time.setGravity(Gravity.CENTER_VERTICAL);
        page.addView(time, new LinearLayout.LayoutParams(-1, -2));
        expandedElapsed = label("0:00", 12, muted, Typeface.NORMAL);
        expandedDuration = label("0:00", 12, muted, Typeface.NORMAL);
        time.addView(expandedElapsed, new LinearLayout.LayoutParams(0, -2, 1f));
        time.addView(expandedDuration);

        LinearLayout controls = new LinearLayout(this);
        controls.setGravity(Gravity.CENTER);
        page.addView(controls, marginParams(-1, dp(84), 10, 0, 0, 0));
        expandedShuffle = iconButton(R.drawable.ic_shuffle, "Aleatorio", alphaColor(text, darkMode ? 20 : 12), muted, dp(48));
        ImageButton previous = iconButton(R.drawable.ic_skip_previous, "Anterior", raised, text, dp(54));
        expandedPlay = iconButton(R.drawable.ic_play_arrow, "Tocar", accent, onAccent, dp(70));
        ImageButton next = iconButton(R.drawable.ic_skip_next, "Proxima", raised, text, dp(54));
        expandedRepeat = iconButton(R.drawable.ic_repeat, "Repetir", alphaColor(text, darkMode ? 20 : 12), muted, dp(48));
        controls.addView(expandedShuffle);
        controls.addView(previous);
        controls.addView(expandedPlay);
        controls.addView(next);
        controls.addView(expandedRepeat);

        expandedPlay.setOnClickListener(view -> togglePlayback());
        previous.setOnClickListener(view -> {
            if (playbackService != null) {
                playbackService.previous();
            }
        });
        next.setOnClickListener(view -> {
            if (playbackService != null) {
                playbackService.next();
            }
        });
        expandedShuffle.setOnClickListener(view -> toggleShuffle());
        expandedRepeat.setOnClickListener(view -> cycleRepeat());

        LinearLayout tools = new LinearLayout(this);
        tools.setGravity(Gravity.CENTER);
        page.addView(tools, marginParams(-1, dp(42), 4, 0, 0, 0));
        tools.addView(actionChip("EQ", accent, false, view -> showEqualizerPanel()), weightParams());
        tools.addView(actionChip("Letras", blue, false, view -> showLyricsPanel(currentTrack())), weightParams());
        tools.addView(actionChip("Album", warm, false, view -> showArtistPanel(currentTrack())), weightParams());
        tools.addView(actionChip("Historico", rose, false, view -> showHistoryPanel()), weightParams());
    }

    private void loadLibrary() {
        libraryLoading = true;
        permissionButton.setVisibility(View.GONE);
        emptyView.setText("Carregando musicas...");
        new Thread(() -> {
            ArrayList<Track> loaded = repository.loadAudio(PlayerActivity.this);
            runOnUiThread(() -> {
                allTracks.clear();
                allTracks.addAll(loaded);
                libraryLoading = false;
                updateSummary();
                refreshCurrentView();
            });
        }).start();
    }

    private void showPermissionState() {
        permissionButton.setVisibility(View.VISIBLE);
        searchInput.setVisibility(View.GONE);
        backButton.setVisibility(View.GONE);
        addButton.setVisibility(View.GONE);
        visibleTracks.clear();
        listView.setAdapter(trackAdapter);
        trackAdapter.setTracks(visibleTracks);
        setSection("Permissao necessaria", "Autorize o acesso ao audio local");
        setHero("Conecte sua biblioteca", "Permita acesso as musicas para organizar tudo automaticamente.", "Permitir", "Baixar");
        emptyView.setText("Acesso necessario\nToque em Permitir para carregar suas musicas.");
    }

    private void refreshCurrentView() {
        if (currentTab == TAB_TOOLS) {
            showTools();
            return;
        }
        if (!hasAudioPermission()) {
            showPermissionState();
            return;
        }
        permissionButton.setVisibility(View.GONE);
        if (libraryLoading) {
            emptyView.setText("Carregando musicas...");
            return;
        }
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
        }
    }

    private void showLibrary() {
        currentTab = TAB_LIBRARY;
        openPlaylist = null;
        refreshTabs();
        searchInput.setVisibility(View.VISIBLE);
        backButton.setVisibility(View.GONE);
        addButton.setVisibility(View.GONE);
        filterInto(visibleTracks, allTracks);
        listView.setAdapter(trackAdapter);
        trackAdapter.setTracks(visibleTracks);
        listView.setOnItemClickListener((parent, view, position, id) -> playFrom(visibleTracks, position));
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            showTrackMenu(visibleTracks.get(position), view, null);
            return true;
        });
        setSection("Biblioteca", countLabel(allTracks.size(), "musica no aparelho", "musicas no aparelho"));
        setHero("Sua biblioteca", allTracks.isEmpty() ? "Baixe audio ou permita acesso aos arquivos locais." : "Toque uma musica, busque por artista ou use o modo aleatorio.", "Tocar", "Baixar");
        emptyView.setText(searchText().isEmpty() ? "Biblioteca vazia\nBaixe audios ou coloque musicas no aparelho." : "Nenhum resultado\nTente outro nome, artista ou album.");
        animateList();
    }

    private void showPlaylists() {
        currentTab = TAB_PLAYLISTS;
        openPlaylist = null;
        refreshTabs();
        searchInput.setVisibility(View.GONE);
        backButton.setVisibility(View.GONE);
        addButton.setVisibility(View.VISIBLE);
        addButton.setContentDescription("Nova playlist");
        addButton.setOnClickListener(view -> showCreatePlaylistPanel(null));
        ArrayList<Playlist> playlists = playlistStore.getPlaylists();
        listView.setAdapter(playlistAdapter);
        playlistAdapter.setPlaylists(playlists);
        listView.setOnItemClickListener((parent, view, position, id) -> showPlaylistTracks(playlists.get(position)));
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            showPlaylistMenu(playlists.get(position), view);
            return true;
        });
        setSection("Playlists", countLabel(playlists.size(), "lista criada", "listas criadas"));
        setHero("Organize seu som", "Crie listas para favoritos, treino, estudo ou festa.", "Nova lista", "Tocar");
        emptyView.setText("Nenhuma playlist\nCrie uma lista para separar suas musicas por momento.");
        animateList();
    }

    private void showPlaylistTracks(Playlist playlist) {
        currentTab = TAB_PLAYLISTS;
        openPlaylist = playlist;
        refreshTabs();
        searchInput.setVisibility(View.VISIBLE);
        backButton.setVisibility(View.VISIBLE);
        addButton.setVisibility(View.GONE);
        ArrayList<Track> tracks = resolvePlaylist(playlist);
        filterInto(visibleTracks, tracks);
        listView.setAdapter(trackAdapter);
        trackAdapter.setTracks(visibleTracks);
        listView.setOnItemClickListener((parent, view, position, id) -> playFrom(visibleTracks, position));
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            showTrackMenu(visibleTracks.get(position), view, playlist);
            return true;
        });
        setSection(playlist.name, countLabel(tracks.size(), "musica", "musicas"));
        setHero(playlist.name, "Playlist pronta para tocar em sequencia.", "Tocar", "Buscar");
        emptyView.setText(searchText().isEmpty() ? "Playlist vazia\nAdicione musicas pelo menu de uma faixa." : "Nada encontrado\nEssa playlist nao tem resultado para a busca.");
        animateList();
    }

    private void showQueue() {
        currentTab = TAB_QUEUE;
        refreshTabs();
        searchInput.setVisibility(View.GONE);
        backButton.setVisibility(View.GONE);
        addButton.setVisibility(View.GONE);
        visibleTracks.clear();
        if (playbackService != null) {
            visibleTracks.addAll(playbackService.getQueue());
        }
        listView.setAdapter(trackAdapter);
        trackAdapter.setTracks(visibleTracks);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (playbackService != null) {
                playbackService.playQueueIndex(position);
            }
        });
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            showTrackMenu(visibleTracks.get(position), view, null);
            return true;
        });
        setSection("Fila", countLabel(visibleTracks.size(), "faixa preparada", "faixas preparadas"));
        setHero("Fila de reproducao", visibleTracks.isEmpty() ? "Toque uma musica para montar a sequencia." : "Veja e retome o que vem a seguir.", "Tocar", "Historico");
        emptyView.setText("Fila vazia\nComece uma musica para ver a sequencia aqui.");
        animateList();
    }

    private void showTools() {
        currentTab = TAB_TOOLS;
        openPlaylist = null;
        refreshTabs();
        searchInput.setVisibility(View.GONE);
        backButton.setVisibility(View.GONE);
        addButton.setVisibility(View.VISIBLE);
        addButton.setContentDescription("Baixar por link");
        addButton.setOnClickListener(view -> showImportDialog());
        listView.setAdapter(toolAdapter);
        toolAdapter.notifyDataSetChanged();
        listView.setOnItemClickListener((parent, view, position, id) -> handleTool(toolAdapter.getItem(position).id));
        listView.setOnItemLongClickListener(null);
        setSection("Extras", "Downloads, equalizador e ferramentas");
        setHero("Central profissional", "Baixe audio, ajuste o som e veja historico sem sair do app.", "Baixar", "Historico");
        emptyView.setText("");
        animateList();
    }

    private void setSection(String title, String subtitle) {
        subtitleView.setText(subtitle);
        sectionTitle.setText(title);
        sectionSubtitle.setText(subtitle);
    }

    private void setHero(String title, String subtitle, String primary, String secondary) {
        heroTitle.setText(title);
        heroSubtitle.setText(subtitle);
        heroPrimary.setText(primary);
        heroSecondary.setText(secondary);
        heroPrimary.setOnClickListener(view -> handleHero(primary));
        heroSecondary.setOnClickListener(view -> handleHero(secondary));
    }

    private void handleHero(String label) {
        if ("Permitir".equals(label)) {
            requestAudioPermission();
        } else if ("Baixar".equals(label)) {
            showImportDialog();
        } else if ("Nova lista".equals(label)) {
            showCreatePlaylistPanel(null);
        } else if ("Historico".equals(label)) {
            showHistoryPanel();
        } else if ("Buscar".equals(label)) {
            searchInput.setVisibility(View.VISIBLE);
            searchInput.requestFocus();
        } else if ("Tocar".equals(label)) {
            playBestSource();
        }
    }

    private void playBestSource() {
        if (!visibleTracks.isEmpty() && currentTab != TAB_TOOLS && currentTab != TAB_PLAYLISTS) {
            playFrom(visibleTracks, 0);
        } else if (!visibleTracks.isEmpty() && openPlaylist != null) {
            playFrom(visibleTracks, 0);
        } else if (!allTracks.isEmpty()) {
            playFrom(allTracks, 0);
        } else {
            toast(hasAudioPermission() ? "Nenhuma musica encontrada" : "Permita acesso ao audio");
        }
    }

    private void playFrom(ArrayList<Track> source, int index) {
        if (source == null || source.isEmpty()) {
            return;
        }
        startService(new Intent(this, MusicPlaybackService.class));
        ArrayList<Track> queue = new ArrayList<>(source);
        if (playbackService == null) {
            pendingQueue = queue;
            pendingIndex = Math.max(0, Math.min(index, queue.size() - 1));
        } else {
            playbackService.setQueue(queue, Math.max(0, Math.min(index, queue.size() - 1)), true);
        }
    }

    private void togglePlayback() {
        if (playbackService != null && playbackService.hasTrack()) {
            playbackService.toggle();
        } else {
            playBestSource();
        }
    }

    private void toggleShuffle() {
        if (playbackService != null) {
            playbackService.setShuffleEnabled(!playbackService.isShuffleEnabled());
        }
        updatePlayer();
    }

    private void cycleRepeat() {
        if (playbackService != null) {
            playbackService.setRepeatMode((playbackService.getRepeatMode() + 1) % 3);
        }
        updatePlayer();
    }

    private void updatePlayer() {
        if (miniTitle == null) {
            return;
        }
        if (playbackService == null || !playbackService.hasTrack()) {
            miniTitle.setText("Nada tocando");
            miniArtist.setText("Escolha uma musica para comecar");
            miniQueue.setText("Fila vazia");
            expandedTitle.setText("Nada tocando");
            expandedArtist.setText("Escolha uma musica da biblioteca");
            expandedAlbum.setText("Player parado");
            expandedQueue.setText("Fila vazia");
            setArtwork(miniArtwork, miniInitial, null);
            setArtwork(expandedArtwork, expandedInitial, null);
            miniPlay.setImageResource(R.drawable.ic_play_arrow);
            expandedPlay.setImageResource(R.drawable.ic_play_arrow);
            setSeekState(0, 0);
            colorPlaybackButtons(false, 0);
            syncListState(null, false);
            return;
        }
        Track track = playbackService.getCurrentTrack();
        boolean playing = playbackService.isPlaying();
        miniTitle.setText(track.title);
        miniArtist.setText(track.subtitle());
        miniQueue.setText(queueLabel());
        expandedTitle.setText(track.title);
        expandedArtist.setText(track.artist);
        expandedAlbum.setText(track.album);
        expandedQueue.setText(queueLabel());
        setArtwork(miniArtwork, miniInitial, track);
        setArtwork(expandedArtwork, expandedInitial, track);
        miniPlay.setImageResource(playing ? R.drawable.ic_pause : R.drawable.ic_play_arrow);
        expandedPlay.setImageResource(playing ? R.drawable.ic_pause : R.drawable.ic_play_arrow);
        setSeekState(playbackService.getPosition(), playbackService.getDuration());
        colorPlaybackButtons(playbackService.isShuffleEnabled(), playbackService.getRepeatMode());
        rememberHistory(track);
        syncListState(track, playing);
    }

    private void setSeekState(int position, int duration) {
        int safeDuration = Math.max(1000, duration);
        miniSeek.setMax(safeDuration);
        expandedSeek.setMax(safeDuration);
        if (!miniSeeking) {
            miniSeek.setProgress(Math.min(position, safeDuration));
        }
        if (!expandedSeeking) {
            expandedSeek.setProgress(Math.min(position, safeDuration));
        }
        miniElapsed.setText(formatMs(position));
        miniDuration.setText(formatMs(duration));
        expandedElapsed.setText(formatMs(position));
        expandedDuration.setText(formatMs(duration));
    }

    private void colorPlaybackButtons(boolean shuffle, int repeat) {
        miniShuffle.setColorFilter(shuffle ? accent : muted);
        expandedShuffle.setColorFilter(shuffle ? accent : muted);
        miniRepeat.setColorFilter(repeat == 0 ? muted : warm);
        expandedRepeat.setColorFilter(repeat == 0 ? muted : warm);
    }

    private void showExpandedPlayer() {
        updatePlayer();
        expandedPlayer.animate().cancel();
        expandedPlayer.setVisibility(View.VISIBLE);
        expandedPlayer.setAlpha(0f);
        expandedPlayer.setTranslationY(dp(36));
        expandedPlayer.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(220)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }

    private void hideExpandedPlayer() {
        expandedPlayer.animate().cancel();
        expandedPlayer.animate()
                .alpha(0f)
                .translationY(dp(36))
                .setDuration(180)
                .withEndAction(() -> {
                    expandedPlayer.setVisibility(View.GONE);
                    expandedPlayer.setAlpha(1f);
                    expandedPlayer.setTranslationY(0f);
                })
                .start();
    }

    private Track currentTrack() {
        return playbackService == null ? null : playbackService.getCurrentTrack();
    }

    private void showTrackMenu(Track track, View anchor, Playlist playlistContext) {
        PopupMenu menu = new PopupMenu(this, anchor == null ? listView : anchor);
        Menu items = menu.getMenu();
        items.add(0, 1, 0, "Adicionar a playlist");
        items.add(0, 2, 1, "Tocar a seguir");
        items.add(0, 3, 2, "Favoritar");
        items.add(0, 4, 3, "Letras");
        items.add(0, 5, 4, "Artista e album");
        if (playlistContext != null) {
            items.add(0, 6, 5, "Remover da playlist");
        }
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                showAddToPlaylistPanel(track);
            } else if (item.getItemId() == 2) {
                if (playbackService != null) {
                    playbackService.enqueueNext(track);
                    toast("Adicionada a fila");
                }
            } else if (item.getItemId() == 3) {
                toast(playlistStore.toggleFavorite(track) ? "Adicionada as favoritas" : "Removida das favoritas");
                refreshCurrentView();
            } else if (item.getItemId() == 4) {
                showLyricsPanel(track);
            } else if (item.getItemId() == 5) {
                showArtistPanel(track);
            } else if (item.getItemId() == 6 && playlistContext != null) {
                playlistStore.removeTrack(playlistContext.id, track);
                openPlaylist = findPlaylist(playlistContext.id);
                if (openPlaylist != null) {
                    showPlaylistTracks(openPlaylist);
                }
            }
            return true;
        });
        menu.show();
    }

    private void showPlaylistMenu(Playlist playlist, View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor == null ? listView : anchor);
        menu.getMenu().add(0, 1, 0, "Renomear");
        if (!Playlist.FAVORITES_ID.equals(playlist.id)) {
            menu.getMenu().add(0, 2, 1, "Apagar");
        }
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                showRenamePlaylistPanel(playlist);
            } else if (item.getItemId() == 2) {
                showConfirmPanel("Apagar playlist", "A playlist sera removida, mas as musicas continuam no aparelho.", rose, dialog -> {
                    playlistStore.deletePlaylist(playlist.id);
                    dialog.dismiss();
                    showPlaylists();
                });
            }
            return true;
        });
        menu.show();
    }

    private void showAddToPlaylistPanel(Track track) {
        LinearLayout body = dialogBody();
        ArrayList<Playlist> playlists = playlistStore.getPlaylists();
        final AlertDialog[] holder = new AlertDialog[1];
        for (int i = 0; i < playlists.size(); i++) {
            Playlist playlist = playlists.get(i);
            body.addView(choiceRow(R.drawable.ic_playlist, playlist.name, countLabel(playlist.trackUris.size(), "musica", "musicas"), warm, view -> {
                playlistStore.addTrack(playlist.id, track);
                toast("Musica adicionada");
                if (holder[0] != null) {
                    holder[0].dismiss();
                }
                refreshCurrentView();
            }), topMargin(-1, dp(66), i == 0 ? 0 : 8));
        }
        holder[0] = showPanel("Adicionar a playlist", track.title, body,
                new PanelAction("Fechar", muted, false, AlertDialog::dismiss),
                new PanelAction("Nova", accent, true, dialog -> {
                    dialog.dismiss();
                    showCreatePlaylistPanel(track);
                }));
    }

    private void showCreatePlaylistPanel(Track trackToAdd) {
        EditText input = input("Nome da playlist", "");
        LinearLayout body = dialogBody();
        body.addView(infoCard("Organizacao", trackToAdd == null ? "Crie uma lista para reunir suas musicas." : "A musica escolhida sera adicionada automaticamente.", warm));
        body.addView(input, topMargin(-1, dp(54), 12));
        showPanel("Nova playlist", "Organize a biblioteca", body,
                new PanelAction("Cancelar", muted, false, AlertDialog::dismiss),
                new PanelAction("Criar", accent, true, dialog -> {
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
    }

    private void showRenamePlaylistPanel(Playlist playlist) {
        EditText input = input("Nome da playlist", playlist.name);
        LinearLayout body = dialogBody();
        body.addView(input, new LinearLayout.LayoutParams(-1, dp(54)));
        showPanel("Renomear playlist", playlist.name, body,
                new PanelAction("Cancelar", muted, false, AlertDialog::dismiss),
                new PanelAction("Salvar", accent, true, dialog -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        playlistStore.renamePlaylist(playlist.id, name);
                        dialog.dismiss();
                        showPlaylists();
                    }
                }));
    }

    private void showImportDialog() {
        EditText input = input("https://site.com/musica.mp3", "");
        input.setSingleLine(false);
        input.setMinLines(2);
        LinearLayout body = dialogBody();
        body.addView(infoCard("Download offline", "Use links diretos de audio. Links do YouTube abrem no app externo.", accent));
        body.addView(input, topMargin(-1, dp(92), 12));
        showPanel("Baixar audio", "Salvar em Music/PlayerMusic", body,
                new PanelAction("Cancelar", muted, false, AlertDialog::dismiss),
                new PanelAction("Baixar", accent, true, dialog -> {
                    String link = input.getText().toString().trim();
                    if (link.isEmpty()) {
                        input.setError("Cole um link");
                        return;
                    }
                    dialog.dismiss();
                    importAudioLink(link);
                }));
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
                    if (hasAudioPermission()) {
                        loadLibrary();
                    } else {
                        requestAudioPermission();
                    }
                });
            } catch (Exception error) {
                runOnUiThread(() -> showDownloadError(error.getMessage()));
            }
        }).start();
    }

    private String downloadAudioFile(String link) throws Exception {
        HttpURLConnection connection = openConnection(link);
        try {
            String contentType = cleanContentType(connection.getContentType());
            String fileName = fileNameFromLink(link, contentType);
            if (!isAudioResponse(fileName, contentType)) {
                throw new Exception("Esse link nao aponta para um arquivo de audio direto.");
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

    private HttpURLConnection openConnection(String link) throws Exception {
        URL url = new URL(link);
        for (int i = 0; i < MAX_REDIRECTS; i++) {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);
            connection.setRequestProperty("User-Agent", "PlayerMusic/1.5");
            int code = connection.getResponseCode();
            if (code >= 300 && code < 400) {
                String location = connection.getHeaderField("Location");
                connection.disconnect();
                if (location == null || location.trim().isEmpty()) {
                    throw new Exception("O link redirecionou sem destino.");
                }
                url = new URL(url, location);
                continue;
            }
            if (code < 200 || code >= 300) {
                connection.disconnect();
                throw new Exception("Servidor respondeu com erro " + code + ".");
            }
            return connection;
        }
        throw new Exception("O link redirecionou muitas vezes.");
    }

    private void saveAudioStream(InputStream input, String fileName, String mimeType) throws Exception {
        if (Build.VERSION.SDK_INT >= 29) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/PlayerMusic");
            values.put(MediaStore.MediaColumns.IS_PENDING, 1);
            Uri audioUri = getContentResolver().insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values);
            if (audioUri == null) {
                throw new Exception("Nao foi possivel criar o arquivo.");
            }
            boolean saved = false;
            try (OutputStream output = getContentResolver().openOutputStream(audioUri)) {
                if (output == null) {
                    throw new Exception("Nao foi possivel abrir o destino.");
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
        } else {
            File base = getExternalFilesDir(Environment.DIRECTORY_MUSIC);
            if (base == null) {
                base = getFilesDir();
            }
            File dir = new File(base, "PlayerMusic");
            if (!dir.exists() && !dir.mkdirs()) {
                throw new Exception("Nao foi possivel criar a pasta de musicas.");
            }
            File target = uniqueFile(dir, fileName);
            try (OutputStream output = new FileOutputStream(target)) {
                copyStream(input, output);
            }
            MediaScannerConnection.scanFile(this, new String[] { target.getAbsolutePath() }, new String[] { mimeType }, null);
        }
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

    private void showDownloadError(String message) {
        LinearLayout body = dialogBody();
        body.addView(infoCard("Nao foi possivel baixar", message == null || message.isEmpty() ? "Use um link direto de audio: mp3, m4a, wav, ogg, flac, aac, opus ou webm." : message, rose));
        showPanel("Download nao iniciado", "Link invalido", body, new PanelAction("Entendi", accent, true, AlertDialog::dismiss));
    }

    private void handleTool(int id) {
        if (id == 1) {
            showImportDialog();
        } else if (id == 2) {
            if (hasAudioPermission()) {
                loadLibrary();
                toast("Biblioteca atualizando");
            } else {
                requestAudioPermission();
            }
        } else if (id == 3) {
            showEqualizerPanel();
        } else if (id == 4) {
            showHistoryPanel();
        } else if (id == 5) {
            String packageName = customExtensionPackage();
            if (packageName.isEmpty() || !openExternalPackage(packageName)) {
                showCustomExtensionPanel();
            }
        } else if (id == 6) {
            openExternalPackage(SNAPTUBE_PACKAGE);
        } else if (id == 7) {
            showSafetyPanel();
        }
    }

    private void showEqualizerPanel() {
        if (playbackService == null || !playbackService.hasTrack()) {
            toast("Toque uma musica para abrir o equalizador");
            return;
        }
        if (!playbackService.hasEqualizer()) {
            if (!openSystemEqualizer()) {
                toast("Equalizador indisponivel neste aparelho");
            }
            return;
        }
        short bands = playbackService.getEqualizerBandCount();
        short[] range = playbackService.getEqualizerBandLevelRange();
        if (bands <= 0 || range.length < 2 || range[0] == range[1]) {
            toast("Equalizador indisponivel neste aparelho");
            return;
        }
        LinearLayout body = dialogBody();
        body.addView(infoCard("Faixa atual", currentTrack().title + "\n" + currentTrack().artist, accent));
        for (short i = 0; i < bands; i++) {
            short band = i;
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(dp(14), dp(12), dp(14), dp(10));
            row.setBackground(rounded(alphaBlend(surface, accent, darkMode ? 9 : 4), dp(14), 1, border));
            body.addView(row, topMargin(-1, -2, 10));

            LinearLayout labels = new LinearLayout(this);
            labels.setGravity(Gravity.CENTER_VERTICAL);
            row.addView(labels, new LinearLayout.LayoutParams(-1, -2));
            labels.addView(label(formatBandFrequency(playbackService.getEqualizerCenterFreq(band)), 13, text, Typeface.BOLD), new LinearLayout.LayoutParams(0, -2, 1f));

            TextView levelView = label(formatDb(playbackService.getEqualizerBandLevel(band)), 12, accent, Typeface.BOLD);
            levelView.setGravity(Gravity.CENTER);
            levelView.setPadding(dp(10), 0, dp(10), 0);
            levelView.setBackground(pillBg(accent));
            labels.addView(levelView, new LinearLayout.LayoutParams(-2, dp(28)));

            SeekBar seek = seekBar();
            seek.setMax(range[1] - range[0]);
            seek.setProgress(playbackService.getEqualizerBandLevel(band) - range[0]);
            seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    short level = (short) (range[0] + progress);
                    levelView.setText(formatDb(level));
                    if (fromUser) {
                        playbackService.setEqualizerBandLevel(band, level);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });
            row.addView(seek, new LinearLayout.LayoutParams(-1, dp(42)));
        }
        ScrollView scroll = new ScrollView(this);
        scroll.addView(body);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(-1, Math.min(dp(460), getResources().getDisplayMetrics().heightPixels - dp(240))));
        showPanel("Equalizador", "Ajuste a faixa atual", scroll,
                new PanelAction("Zerar", warm, false, dialog -> {
                    resetEqualizer();
                    dialog.dismiss();
                    showEqualizerPanel();
                }),
                new PanelAction("Pronto", accent, true, AlertDialog::dismiss));
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
        for (short band = 0; band < playbackService.getEqualizerBandCount(); band++) {
            playbackService.setEqualizerBandLevel(band, (short) 0);
        }
    }

    private void showLyricsPanel(Track track) {
        if (track == null) {
            toast("Toque uma musica para abrir as letras");
            return;
        }
        EditText input = input("Cole ou edite a letra desta musica", lyricsFor(track));
        input.setSingleLine(false);
        input.setGravity(Gravity.TOP);
        input.setMinLines(8);
        LinearLayout body = dialogBody();
        body.addView(infoCard("Musica", track.title + "\n" + track.artist, blue));
        body.addView(input, topMargin(-1, dp(220), 12));
        showPanel("Letras", "Texto salvo por musica", body,
                new PanelAction("Buscar", blue, false, dialog -> openExternalUrl("https://www.google.com/search?q=" + Uri.encode(track.artist + " " + track.title + " letra"))),
                new PanelAction("Fechar", muted, false, AlertDialog::dismiss),
                new PanelAction("Salvar", accent, true, dialog -> {
                    uiPrefs.edit().putString(lyricsKey(track), input.getText().toString()).apply();
                    toast("Letra salva");
                    dialog.dismiss();
                }));
    }

    private void showArtistPanel(Track track) {
        if (track == null) {
            toast("Toque uma musica para abrir artista e album");
            return;
        }
        ArrayList<Track> artistTracks = tracksByArtist(track.artist);
        ArrayList<Track> albumTracks = tracksByAlbum(track.album, track.artist);
        LinearLayout body = dialogBody();
        body.addView(infoCard("Artista", track.artist + "\n" + track.title, blue));
        LinearLayout stats = new LinearLayout(this);
        stats.setGravity(Gravity.CENTER);
        stats.addView(infoCard(countLabel(artistTracks.size(), "musica", "musicas"), "do artista", blue), weightParams());
        stats.addView(infoCard(countLabel(albumTracks.size(), "musica", "musicas"), "no album", warm), weightParams());
        body.addView(stats, topMargin(-1, dp(82), 10));
        body.addView(infoCard("Album", track.album, warm), topMargin(-1, -2, 10));
        showPanel("Artista e album", "Explore a biblioteca", body,
                new PanelAction("Fechar", muted, false, AlertDialog::dismiss),
                new PanelAction("Tocar artista", blue, false, dialog -> {
                    dialog.dismiss();
                    if (!artistTracks.isEmpty()) {
                        playFrom(artistTracks, 0);
                    }
                }),
                new PanelAction("Tocar album", accent, true, dialog -> {
                    dialog.dismiss();
                    if (!albumTracks.isEmpty()) {
                        playFrom(albumTracks, 0);
                    }
                }));
    }

    private void showHistoryPanel() {
        ArrayList<Track> history = historyTracks();
        LinearLayout body = dialogBody();
        if (history.isEmpty()) {
            body.addView(infoCard("Nada tocado ainda", "As musicas tocadas vao aparecer aqui.", rose));
            showPanel("Historico", "Ultimas reproducoes", body, new PanelAction("Entendi", accent, true, AlertDialog::dismiss));
            return;
        }
        final AlertDialog[] holder = new AlertDialog[1];
        for (int i = 0; i < history.size(); i++) {
            int index = i;
            Track track = history.get(i);
            body.addView(choiceRow(0, track.title, track.artist + " - " + track.album, i == 0 ? accent : blue, view -> {
                if (holder[0] != null) {
                    holder[0].dismiss();
                }
                playFrom(history, index);
            }), topMargin(-1, dp(68), i == 0 ? 0 : 8));
        }
        ScrollView scroll = new ScrollView(this);
        scroll.addView(body);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(-1, Math.min(dp(380), getResources().getDisplayMetrics().heightPixels - dp(260))));
        holder[0] = showPanel("Historico", "Ultimas reproducoes", scroll,
                new PanelAction("Limpar", rose, false, dialog -> {
                    uiPrefs.edit().remove(KEY_HISTORY).apply();
                    toast("Historico limpo");
                    dialog.dismiss();
                }),
                new PanelAction("Fechar", accent, true, AlertDialog::dismiss));
    }

    private void showCustomExtensionPanel() {
        EditText input = input(SNAPTUBE_PACKAGE, customExtensionPackage());
        LinearLayout body = dialogBody();
        body.addView(infoCard("Pacote Android", "Cadastre o pacote de uma extensao instalada.", blue));
        body.addView(input, topMargin(-1, dp(54), 12));
        showPanel("Adicionar extensao", "App externo", body,
                new PanelAction("Limpar", rose, false, dialog -> {
                    extensionPrefs.edit().remove(KEY_CUSTOM_EXTENSION_PACKAGE).apply();
                    dialog.dismiss();
                    showTools();
                }),
                new PanelAction("Cancelar", muted, false, AlertDialog::dismiss),
                new PanelAction("Salvar", accent, true, dialog -> {
                    String packageName = input.getText().toString().trim();
                    if (packageName.isEmpty()) {
                        input.setError("Digite o pacote Android");
                        return;
                    }
                    extensionPrefs.edit().putString(KEY_CUSTOM_EXTENSION_PACKAGE, packageName).apply();
                    dialog.dismiss();
                    showTools();
                }));
    }

    private void showSafetyPanel() {
        LinearLayout body = dialogBody();
        body.addView(infoCard("Uso responsavel", "Importe apenas audio que voce tem direito de usar.", warm));
        body.addView(infoCard("Biblioteca local", "O PlayerMusic toca arquivos encontrados no aparelho e downloads salvos pelo app.", accent), topMargin(-1, -2, 10));
        showPanel("Downloads e extensoes", "Aviso", body, new PanelAction("Entendi", accent, true, AlertDialog::dismiss));
    }

    private void showConfirmPanel(String title, String message, int tint, PanelCallback confirm) {
        LinearLayout body = dialogBody();
        body.addView(infoCard("Confirmacao", message, tint));
        showPanel(title, "", body,
                new PanelAction("Cancelar", muted, false, AlertDialog::dismiss),
                new PanelAction("Confirmar", tint, true, confirm));
    }

    private AlertDialog showPanel(String title, String subtitle, View body, PanelAction... actions) {
        AlertDialog dialog = new AlertDialog.Builder(this).create();
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(panelBackground());

        View strip = new View(this);
        strip.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[] { accent, blue, warm }));
        card.addView(strip, new LinearLayout.LayoutParams(-1, dp(5)));

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(dp(20), dp(18), dp(20), dp(16));
        card.addView(inner);

        TextView titleView = label(title, 21, text, Typeface.BOLD);
        titleView.setSingleLine(true);
        titleView.setEllipsize(TextUtils.TruncateAt.END);
        inner.addView(titleView);
        if (subtitle != null && !subtitle.trim().isEmpty()) {
            TextView subtitleView = label(subtitle, 13, muted, Typeface.NORMAL);
            subtitleView.setSingleLine(true);
            subtitleView.setEllipsize(TextUtils.TruncateAt.END);
            inner.addView(subtitleView);
        }
        if (body != null) {
            LinearLayout.LayoutParams bodyParams = topMargin(-1, -2, 16);
            ViewGroup.LayoutParams requested = body.getLayoutParams();
            if (requested != null && requested.height > 0) {
                bodyParams.height = requested.height;
            }
            inner.addView(body, bodyParams);
        }

        LinearLayout buttons = new LinearLayout(this);
        buttons.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        inner.addView(buttons, topMargin(-1, dp(42), 16));
        if (actions != null) {
            for (PanelAction action : actions) {
                buttons.addView(panelButton(dialog, action), buttonParams());
            }
        }

        dialog.setView(card, 0, 0, 0, 0);
        dialog.setOnShowListener(view -> {
            android.view.Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                window.setDimAmount(darkMode ? 0.74f : 0.42f);
                window.setLayout(Math.min(getResources().getDisplayMetrics().widthPixels - dp(28), dp(430)), ViewGroup.LayoutParams.WRAP_CONTENT);
            }
            animateIn(card);
        });
        dialog.show();
        return dialog;
    }

    private TextView panelButton(AlertDialog dialog, PanelAction action) {
        TextView button = label(action.label, 12, action.primary ? onAccent : action.tint, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setSingleLine(true);
        button.setMinWidth(dp(70));
        button.setPadding(dp(12), 0, dp(12), 0);
        button.setBackground(rounded(action.primary ? action.tint : alphaColor(action.tint, darkMode ? 28 : 18), dp(17), action.primary ? 0 : 1, alphaColor(action.tint, darkMode ? 90 : 64)));
        button.setOnClickListener(view -> action.callback.onClick(dialog));
        attachPress(button);
        return button;
    }

    private LinearLayout dialogBody() {
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        return body;
    }

    private LinearLayout infoCard(String title, String message, int tint) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackground(rounded(alphaBlend(surface, tint, darkMode ? 10 : 5), dp(14), 1, alphaColor(tint, darkMode ? 80 : 56)));
        TextView titleView = label(title, 12, tint, Typeface.BOLD);
        titleView.setSingleLine(true);
        titleView.setEllipsize(TextUtils.TruncateAt.END);
        card.addView(titleView);
        TextView messageView = label(message, 13, text, Typeface.NORMAL);
        messageView.setMaxLines(4);
        messageView.setEllipsize(TextUtils.TruncateAt.END);
        card.addView(messageView);
        return card;
    }

    private LinearLayout choiceRow(int icon, String title, String subtitle, int tint, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(8), dp(12), dp(8));
        row.setBackground(rounded(alphaBlend(surface, tint, darkMode ? 10 : 5), dp(14), 1, alphaColor(tint, darkMode ? 78 : 56)));
        row.setOnClickListener(listener);
        attachPress(row);

        TextView badge = label(icon == 0 ? title.substring(0, 1).toUpperCase(Locale.ROOT) : "", 16, badgeText, Typeface.BOLD);
        badge.setGravity(Gravity.CENTER);
        badge.setBackground(albumBadge(tint));
        if (icon != 0) {
            badge.setCompoundDrawablesWithIntrinsicBounds(icon, 0, 0, 0);
        }
        row.addView(badge, new LinearLayout.LayoutParams(dp(46), dp(46)));

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setPadding(dp(12), 0, 0, 0);
        row.addView(labels, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView titleView = label(title, 15, text, Typeface.BOLD);
        titleView.setSingleLine(true);
        titleView.setEllipsize(TextUtils.TruncateAt.END);
        labels.addView(titleView);

        TextView subtitleView = label(subtitle, 12, muted, Typeface.NORMAL);
        subtitleView.setSingleLine(true);
        subtitleView.setEllipsize(TextUtils.TruncateAt.END);
        labels.addView(subtitleView);
        return row;
    }

    private EditText input(String hint, String value) {
        EditText input = new EditText(this);
        input.setText(value);
        input.setHint(hint);
        input.setTextColor(text);
        input.setHintTextColor(muted);
        input.setTextSize(14);
        input.setSingleLine(true);
        input.setSelectAllOnFocus(true);
        input.setPadding(dp(16), dp(10), dp(16), dp(10));
        input.setBackground(rounded(surfaceAlt, dp(14), 1, border));
        return input;
    }

    private void updateSummary() {
        long duration = 0;
        for (Track track : allTracks) {
            duration += Math.max(0, track.duration);
        }
        int playlistCount = playlistStore == null ? 0 : playlistStore.getPlaylists().size();
        statTracks.setText(countLabel(allTracks.size(), "musica", "musicas"));
        statDuration.setText(formatLibraryDuration(duration));
        statPlaylists.setText(countLabel(playlistCount, "lista", "listas"));
        heroMeta.setText(countLabel(allTracks.size(), "musica", "musicas") + " - " + formatLibraryDuration(duration));
    }

    private void filterInto(ArrayList<Track> target, ArrayList<Track> source) {
        target.clear();
        String query = searchText();
        for (Track track : source) {
            if (query.isEmpty() || track.searchableText().contains(query)) {
                target.add(track);
            }
        }
    }

    private String searchText() {
        return searchInput == null ? "" : searchInput.getText().toString().trim().toLowerCase(Locale.ROOT);
    }

    private ArrayList<Track> resolvePlaylist(Playlist playlist) {
        HashMap<String, Track> byUri = new HashMap<>();
        for (Track track : allTracks) {
            byUri.put(track.uri, track);
        }
        ArrayList<Track> tracks = new ArrayList<>();
        for (String uri : playlist.trackUris) {
            Track track = byUri.get(uri);
            if (track != null) {
                tracks.add(track);
            }
        }
        return tracks;
    }

    private Playlist findPlaylist(String id) {
        for (Playlist playlist : playlistStore.getPlaylists()) {
            if (playlist.id.equals(id)) {
                return playlist;
            }
        }
        return null;
    }

    private ArrayList<Track> tracksByArtist(String artist) {
        ArrayList<Track> tracks = new ArrayList<>();
        for (Track track : allTracks) {
            if (sameText(track.artist, artist)) {
                tracks.add(track);
            }
        }
        return tracks;
    }

    private ArrayList<Track> tracksByAlbum(String album, String artist) {
        ArrayList<Track> tracks = new ArrayList<>();
        for (Track track : allTracks) {
            if (sameText(track.album, album) && sameText(track.artist, artist)) {
                tracks.add(track);
            }
        }
        return tracks;
    }

    private boolean sameText(String a, String b) {
        return a != null && b != null && a.trim().equalsIgnoreCase(b.trim());
    }

    private void rememberHistory(Track track) {
        if (track == null || track.uri.equals(lastHistoryUri)) {
            return;
        }
        lastHistoryUri = track.uri;
        ArrayList<String> uris = new ArrayList<>();
        uris.add(track.uri);
        String saved = uiPrefs.getString(KEY_HISTORY, "");
        if (saved != null && !saved.isEmpty()) {
            for (String uri : saved.split("\n")) {
                if (!uri.trim().isEmpty() && !uri.equals(track.uri) && uris.size() < MAX_HISTORY) {
                    uris.add(uri);
                }
            }
        }
        uiPrefs.edit().putString(KEY_HISTORY, TextUtils.join("\n", uris)).apply();
    }

    private ArrayList<Track> historyTracks() {
        ArrayList<Track> tracks = new ArrayList<>();
        HashMap<String, Track> byUri = new HashMap<>();
        for (Track track : allTracks) {
            byUri.put(track.uri, track);
        }
        String saved = uiPrefs.getString(KEY_HISTORY, "");
        if (saved == null || saved.isEmpty()) {
            return tracks;
        }
        for (String uri : saved.split("\n")) {
            Track track = byUri.get(uri);
            if (track != null) {
                tracks.add(track);
            }
        }
        return tracks;
    }

    private void syncListState(Track track, boolean playing) {
        String uri = track == null ? "" : track.uri;
        if (uri.equals(lastRenderedUri)) {
            return;
        }
        lastRenderedUri = uri;
        if (listView != null && listView.getAdapter() == trackAdapter) {
            trackAdapter.notifyDataSetChanged();
        }
    }

    private boolean isCurrentTrack(Track track) {
        Track current = currentTrack();
        return current != null && track != null && current.uri.equals(track.uri);
    }

    private void setArtwork(ImageView image, TextView fallback, Track track) {
        if (track == null) {
            image.setImageDrawable(null);
            image.setVisibility(View.GONE);
            fallback.setText("M");
            fallback.setBackground(albumBadge(0));
            fallback.setVisibility(View.VISIBLE);
            return;
        }
        fallback.setText(trackInitial(track));
        fallback.setBackground(albumBadge(track.albumId));
        image.setImageDrawable(null);
        if (track.albumId > 0) {
            try {
                image.setImageURI(ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), track.albumId));
            } catch (RuntimeException ignored) {
                image.setImageDrawable(null);
            }
        }
        boolean hasArt = image.getDrawable() != null;
        image.setVisibility(hasArt ? View.VISIBLE : View.GONE);
        fallback.setVisibility(hasArt ? View.GONE : View.VISIBLE);
    }

    private String trackInitial(Track track) {
        String value = track == null ? "" : track.title.trim();
        return value.isEmpty() ? "M" : value.substring(0, 1).toUpperCase(Locale.ROOT);
    }

    private String queueLabel() {
        if (playbackService == null) {
            return "Fila vazia";
        }
        ArrayList<Track> queue = playbackService.getQueue();
        return queue.isEmpty() ? "Fila vazia" : (playbackService.getCurrentIndex() + 1) + "/" + queue.size();
    }

    private void animateList() {
        listView.animate().cancel();
        listView.setAlpha(0.78f);
        listView.setTranslationY(dp(8));
        listView.animate().alpha(1f).translationY(0f).setDuration(160).setInterpolator(new AccelerateDecelerateInterpolator()).start();
    }

    private void animateIn(View view) {
        view.setAlpha(0f);
        view.setTranslationY(dp(20));
        view.setScaleX(0.97f);
        view.setScaleY(0.97f);
        view.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f).setDuration(200).setInterpolator(new AccelerateDecelerateInterpolator()).start();
    }

    private void refreshTabs() {
        styleTab(tabLibrary, currentTab == TAB_LIBRARY);
        styleTab(tabPlaylists, currentTab == TAB_PLAYLISTS);
        styleTab(tabQueue, currentTab == TAB_QUEUE);
        styleTab(tabTools, currentTab == TAB_TOOLS);
    }

    private void styleTab(TextView tab, boolean selected) {
        tab.setTextColor(selected ? onAccent : muted);
        tab.setBackground(rounded(selected ? accent : Color.TRANSPARENT, dp(20), 0, 0));
    }

    private boolean hasAudioPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            return checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED;
        }
        return checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestAudioPermission() {
        ArrayList<String> permissions = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= 33) {
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO);
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        requestPermissions(permissions.toArray(new String[0]), REQUEST_AUDIO);
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

    private void toggleTheme() {
        darkMode = !darkMode;
        uiPrefs.edit().putBoolean(KEY_DARK_MODE, darkMode).apply();
        recreate();
    }

    private TextView tab(String title, Runnable action) {
        TextView tab = label(title, 13, muted, Typeface.BOLD);
        tab.setGravity(Gravity.CENTER);
        tab.setSingleLine(true);
        tab.setOnClickListener(view -> action.run());
        attachPress(tab);
        return tab;
    }

    private TextView statPill(String value, int tint) {
        TextView pill = label(value, 12, text, Typeface.BOLD);
        pill.setGravity(Gravity.CENTER);
        pill.setSingleLine(true);
        pill.setEllipsize(TextUtils.TruncateAt.END);
        pill.setPadding(dp(8), 0, dp(8), 0);
        pill.setBackground(pillBg(tint));
        return pill;
    }

    private TextView actionChip(String title, int tint, boolean primary, View.OnClickListener listener) {
        TextView chip = label(title, 12, primary ? onAccent : tint, Typeface.BOLD);
        chip.setGravity(Gravity.CENTER);
        chip.setSingleLine(true);
        chip.setEllipsize(TextUtils.TruncateAt.END);
        chip.setPadding(dp(8), 0, dp(8), 0);
        chip.setBackground(rounded(primary ? tint : alphaColor(tint, darkMode ? 30 : 18), dp(18), primary ? 0 : 1, alphaColor(tint, darkMode ? 90 : 64)));
        chip.setOnClickListener(listener);
        attachPress(chip);
        return chip;
    }

    private TextView label(String value, int sp, int color, int style) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setTypeface(Typeface.DEFAULT, style);
        view.setIncludeFontPadding(true);
        return view;
    }

    private ImageButton iconButton(int icon, String label, int fill, int tint, int size) {
        ImageButton button = new ImageButton(this);
        button.setImageResource(icon);
        button.setColorFilter(tint);
        button.setContentDescription(label);
        button.setScaleType(ImageButton.ScaleType.CENTER);
        button.setPadding(dp(8), dp(8), dp(8), dp(8));
        button.setBackground(fill == Color.TRANSPARENT ? null : oval(fill));
        button.setMinimumWidth(size);
        button.setMinimumHeight(size);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
        params.setMargins(dp(4), 0, dp(4), 0);
        button.setLayoutParams(params);
        attachPress(button);
        return button;
    }

    private SeekBar seekBar() {
        SeekBar seek = new SeekBar(this);
        seek.setMax(1000);
        seek.setProgressTintList(ColorStateList.valueOf(accent));
        seek.setProgressBackgroundTintList(ColorStateList.valueOf(seekBg));
        seek.setThumbTintList(ColorStateList.valueOf(text));
        return seek;
    }

    private void attachPress(View view) {
        view.setOnTouchListener((pressed, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                pressed.animate().scaleX(0.96f).scaleY(0.96f).setDuration(80).start();
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                pressed.animate().scaleX(1f).scaleY(1f).setDuration(110).start();
            }
            return false;
        });
    }

    private LinearLayout.LayoutParams weightParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1f);
        params.setMargins(dp(3), 0, dp(3), 0);
        return params;
    }

    private LinearLayout.LayoutParams marginParams(int width, int height, int top, int left, int bottom, int right) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
        params.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return params;
    }

    private LinearLayout.LayoutParams topMargin(int width, int height, int top) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
        params.setMargins(0, dp(top), 0, 0);
        return params;
    }

    private LinearLayout.LayoutParams buttonParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, dp(36));
        params.setMargins(dp(5), 0, 0, 0);
        return params;
    }

    private GradientDrawable appBackground() {
        return new GradientDrawable(GradientDrawable.Orientation.TL_BR, darkMode
                ? new int[] { 0xFF06100D, 0xFF08111B, 0xFF140E18 }
                : new int[] { 0xFFF1FBF7, 0xFFF7FAFE, 0xFFFFF8EF });
    }

    private GradientDrawable heroBackground() {
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TL_BR, darkMode
                ? new int[] { 0xFF132520, 0xFF151D2A, 0xFF2A2026 }
                : new int[] { 0xFFFFFFFF, 0xFFE6F8F1, 0xFFFFF1E2 });
        drawable.setCornerRadius(dp(22));
        drawable.setStroke(dp(1), alphaColor(accent, darkMode ? 80 : 44));
        return drawable;
    }

    private GradientDrawable playerBackground() {
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, darkMode
                ? new int[] { 0xFF121B20, 0xFF182333, 0xFF251D27 }
                : new int[] { 0xFFFFFFFF, 0xFFEAF7F2, 0xFFFFF3E8 });
        drawable.setCornerRadii(new float[] { dp(24), dp(24), dp(24), dp(24), 0, 0, 0, 0 });
        drawable.setStroke(dp(1), border);
        return drawable;
    }

    private GradientDrawable expandedCoverBackground() {
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TL_BR, darkMode
                ? new int[] { 0xFF263A41, 0xFF151C25, 0xFF39E39B }
                : new int[] { 0xFFDFF6EF, 0xFFFFFFFF, 0xFF8BE4C4 });
        drawable.setCornerRadius(dp(24));
        drawable.setStroke(dp(1), border);
        return drawable;
    }

    private GradientDrawable emptyBackground() {
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[] {
                alphaBlend(surface, blue, darkMode ? 8 : 4),
                alphaBlend(surface, accent, darkMode ? 8 : 4)
        });
        drawable.setCornerRadius(dp(18));
        drawable.setStroke(dp(1), border);
        return drawable;
    }

    private GradientDrawable rowBackground(boolean selected) {
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[] {
                selected ? active : alphaBlend(surface, blue, darkMode ? 3 : 1),
                selected ? alphaBlend(active, accent, darkMode ? 12 : 5) : surface
        });
        drawable.setCornerRadius(dp(16));
        drawable.setStroke(dp(1), selected ? accent : border);
        return drawable;
    }

    private GradientDrawable toolBackground(int tint, boolean enabled) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(enabled ? alphaBlend(surface, tint, darkMode ? 16 : 8) : alphaBlend(surface, muted, darkMode ? 8 : 4));
        drawable.setCornerRadius(dp(16));
        drawable.setStroke(dp(1), enabled ? alphaColor(tint, darkMode ? 115 : 78) : border);
        return drawable;
    }

    private GradientDrawable playlistBackground() {
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[] {
                alphaBlend(surface, warm, darkMode ? 11 : 5),
                alphaBlend(surface, accent, darkMode ? 5 : 2)
        });
        drawable.setCornerRadius(dp(16));
        drawable.setStroke(dp(1), alphaColor(warm, darkMode ? 82 : 58));
        return drawable;
    }

    private GradientDrawable panelBackground() {
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TL_BR, darkMode
                ? new int[] { 0xFF242B33, 0xFF30323A }
                : new int[] { 0xFFFFFFFF, 0xFFF2F6FB });
        drawable.setCornerRadius(dp(22));
        drawable.setStroke(dp(1), border);
        return drawable;
    }

    private GradientDrawable pillBg(int tint) {
        return rounded(alphaColor(tint, darkMode ? 34 : 22), dp(15), 1, alphaColor(tint, darkMode ? 86 : 60));
    }

    private GradientDrawable albumBadge(long seed) {
        int[] colors = new int[] { accent, warm, blue, rose, 0xFFA7F070 };
        int color = colors[(int) (Math.abs(seed) % colors.length)];
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[] { color, 0xFFFFFFFF });
        drawable.setCornerRadius(dp(14));
        return drawable;
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

    private int alphaColor(int color, int alpha) {
        return (color & 0x00FFFFFF) | (Math.max(0, Math.min(255, alpha)) << 24);
    }

    private int alphaBlend(int base, int overlay, int percent) {
        int p = Math.max(0, Math.min(100, percent));
        int inverse = 100 - p;
        int red = (((base >> 16) & 0xFF) * inverse + ((overlay >> 16) & 0xFF) * p) / 100;
        int green = (((base >> 8) & 0xFF) * inverse + ((overlay >> 8) & 0xFF) * p) / 100;
        int blueValue = ((base & 0xFF) * inverse + (overlay & 0xFF) * p) / 100;
        return 0xFF000000 | (red << 16) | (green << 8) | blueValue;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String countLabel(int count, String singular, String plural) {
        return count + " " + (count == 1 ? singular : plural);
    }

    private String formatLibraryDuration(long millis) {
        long minutes = Math.max(0, millis / 60000);
        if (minutes < 60) {
            return minutes + " min";
        }
        long hours = minutes / 60;
        long rest = minutes % 60;
        return rest == 0 ? hours + " h" : hours + " h " + rest + " min";
    }

    private String formatMs(int millis) {
        int seconds = Math.max(0, millis / 1000);
        return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
    }

    private String formatBandFrequency(int milliHz) {
        int hz = Math.max(0, milliHz / 1000);
        return hz >= 1000 ? String.format(Locale.ROOT, "%.1f kHz", hz / 1000f) : hz + " Hz";
    }

    private String formatDb(short milliBel) {
        return String.format(Locale.ROOT, "%+.1f dB", milliBel / 100f);
    }

    private String lyricsFor(Track track) {
        return uiPrefs.getString(lyricsKey(track), "");
    }

    private String lyricsKey(Track track) {
        return KEY_LYRICS_PREFIX + Integer.toHexString(track.uri.hashCode());
    }

    private boolean isYouTubeUrl(Uri uri) {
        String host = uri.getHost();
        if (host == null) {
            return false;
        }
        String normalized = host.toLowerCase(Locale.ROOT);
        return normalized.equals("youtu.be") || normalized.equals("youtube.com") || normalized.endsWith(".youtube.com");
    }

    private void openExternalUrl(String link) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link)));
        } catch (RuntimeException error) {
            toast("Nao foi possivel abrir o link");
        }
    }

    private boolean openExternalPackage(String packageName) {
        Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);
        if (intent == null) {
            toast("App nao encontrado");
            return false;
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        return true;
    }

    private String customExtensionPackage() {
        return extensionPrefs.getString(KEY_CUSTOM_EXTENSION_PACKAGE, "");
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
        return lower.endsWith(".mp3") || lower.endsWith(".m4a") || lower.endsWith(".wav")
                || lower.endsWith(".ogg") || lower.endsWith(".flac") || lower.endsWith(".aac")
                || lower.endsWith(".opus") || lower.endsWith(".amr") || lower.endsWith(".3gp")
                || lower.endsWith(".webm");
    }

    private String cleanContentType(String type) {
        return type == null ? null : type.split(";")[0].trim().toLowerCase(Locale.ROOT);
    }

    private String mimeTypeFor(String fileName, String contentType) {
        if (contentType != null && contentType.startsWith("audio/")) {
            return contentType;
        }
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".m4a")) return "audio/mp4";
        if (lower.endsWith(".wav")) return "audio/wav";
        if (lower.endsWith(".ogg")) return "audio/ogg";
        if (lower.endsWith(".flac")) return "audio/flac";
        if (lower.endsWith(".aac")) return "audio/aac";
        if (lower.endsWith(".opus")) return "audio/opus";
        if (lower.endsWith(".amr")) return "audio/amr";
        if (lower.endsWith(".3gp")) return "audio/3gpp";
        if (lower.endsWith(".webm")) return "audio/webm";
        return "audio/mpeg";
    }

    private String extensionForMime(String mimeType) {
        if (mimeType == null) return "mp3";
        String normalized = mimeType.toLowerCase(Locale.ROOT);
        if (normalized.contains("mp4") || normalized.contains("m4a")) return "m4a";
        if (normalized.contains("wav")) return "wav";
        if (normalized.contains("ogg")) return "ogg";
        if (normalized.contains("flac")) return "flac";
        if (normalized.contains("aac")) return "aac";
        if (normalized.contains("opus")) return "opus";
        if (normalized.contains("webm")) return "webm";
        return "mp3";
    }

    private void toast(String value) {
        Toast.makeText(this, value, Toast.LENGTH_SHORT).show();
    }

    private interface PanelCallback {
        void onClick(AlertDialog dialog);
    }

    private static class PanelAction {
        final String label;
        final int tint;
        final boolean primary;
        final PanelCallback callback;

        PanelAction(String label, int tint, boolean primary, PanelCallback callback) {
            this.label = label;
            this.tint = tint;
            this.primary = primary;
            this.callback = callback;
        }
    }

    private class TrackAdapter extends BaseAdapter {
        private final ArrayList<Track> tracks = new ArrayList<>();

        void setTracks(ArrayList<Track> nextTracks) {
            tracks.clear();
            tracks.addAll(nextTracks);
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
            row.bind(getItem(position));
            return convertView;
        }
    }

    private class TrackRow {
        final LinearLayout root;
        final ImageView artwork;
        final TextView fallback;
        final TextView title;
        final TextView subtitle;
        final TextView duration;
        final ImageButton more;

        TrackRow() {
            root = new LinearLayout(PlayerActivity.this);
            root.setGravity(Gravity.CENTER_VERTICAL);
            root.setPadding(dp(12), dp(10), dp(8), dp(10));
            root.setLayoutParams(new AbsListView.LayoutParams(-1, dp(90)));
            attachPress(root);

            FrameLayout cover = new FrameLayout(PlayerActivity.this);
            cover.setBackground(rounded(raised, dp(16), 1, border));
            root.addView(cover, new LinearLayout.LayoutParams(dp(58), dp(58)));

            artwork = new ImageView(PlayerActivity.this);
            artwork.setScaleType(ImageView.ScaleType.CENTER_CROP);
            artwork.setVisibility(View.GONE);
            cover.addView(artwork, new FrameLayout.LayoutParams(-1, -1));

            fallback = label("M", 18, badgeText, Typeface.BOLD);
            fallback.setGravity(Gravity.CENTER);
            cover.addView(fallback, new FrameLayout.LayoutParams(-1, -1));

            LinearLayout labels = new LinearLayout(PlayerActivity.this);
            labels.setOrientation(LinearLayout.VERTICAL);
            labels.setPadding(dp(12), 0, dp(8), 0);
            root.addView(labels, new LinearLayout.LayoutParams(0, -2, 1f));

            title = label("", 16, text, Typeface.BOLD);
            title.setSingleLine(true);
            title.setEllipsize(TextUtils.TruncateAt.END);
            labels.addView(title);

            subtitle = label("", 13, muted, Typeface.NORMAL);
            subtitle.setSingleLine(true);
            subtitle.setEllipsize(TextUtils.TruncateAt.END);
            labels.addView(subtitle);

            duration = label("", 12, muted, Typeface.BOLD);
            duration.setGravity(Gravity.CENTER);
            duration.setPadding(dp(8), 0, dp(8), 0);
            root.addView(duration, new LinearLayout.LayoutParams(-2, dp(28)));

            more = iconButton(R.drawable.ic_more_vert, "Opcoes", Color.TRANSPARENT, muted, dp(42));
            more.setFocusable(false);
            root.addView(more);
        }

        void bind(Track track) {
            boolean selected = isCurrentTrack(track);
            title.setText(track.title);
            title.setTextColor(selected ? accent : text);
            subtitle.setText(selected && playbackService != null && playbackService.isPlaying() ? "Tocando agora - " + track.subtitle() : track.subtitle());
            subtitle.setTextColor(selected ? text : muted);
            duration.setText(track.formattedDuration());
            duration.setTextColor(selected ? accent : muted);
            duration.setBackground(rounded(alphaColor(selected ? accent : muted, selected ? 36 : (darkMode ? 22 : 14)), dp(13), 1, alphaColor(selected ? accent : muted, selected ? 90 : (darkMode ? 44 : 30))));
            setArtwork(artwork, fallback, track);
            if (selected && playbackService != null && playbackService.isPlaying() && fallback.getVisibility() == View.VISIBLE) {
                fallback.setText(">");
            }
            root.setBackground(rowBackground(selected));
            more.setColorFilter(selected ? accent : muted);
            more.setOnClickListener(view -> showTrackMenu(track, more, currentTab == TAB_PLAYLISTS ? openPlaylist : null));
        }
    }

    private class PlaylistAdapter extends BaseAdapter {
        private final ArrayList<Playlist> playlists = new ArrayList<>();

        void setPlaylists(ArrayList<Playlist> nextPlaylists) {
            playlists.clear();
            playlists.addAll(nextPlaylists);
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
        final TextView subtitle;
        final ImageButton more;

        PlaylistRow() {
            root = new LinearLayout(PlayerActivity.this);
            root.setGravity(Gravity.CENTER_VERTICAL);
            root.setPadding(dp(14), dp(10), dp(8), dp(10));
            root.setLayoutParams(new AbsListView.LayoutParams(-1, dp(92)));
            root.setBackground(playlistBackground());
            attachPress(root);

            badge = label("", 18, badgeText, Typeface.BOLD);
            badge.setGravity(Gravity.CENTER);
            badge.setBackground(rounded(warm, dp(16), 0, 0));
            badge.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_playlist, 0, 0, 0);
            root.addView(badge, new LinearLayout.LayoutParams(dp(54), dp(54)));

            LinearLayout labels = new LinearLayout(PlayerActivity.this);
            labels.setOrientation(LinearLayout.VERTICAL);
            labels.setPadding(dp(12), 0, dp(8), 0);
            root.addView(labels, new LinearLayout.LayoutParams(0, -2, 1f));

            title = label("", 17, text, Typeface.BOLD);
            title.setSingleLine(true);
            title.setEllipsize(TextUtils.TruncateAt.END);
            labels.addView(title);

            subtitle = label("", 13, muted, Typeface.NORMAL);
            labels.addView(subtitle);

            more = iconButton(R.drawable.ic_more_vert, "Opcoes", Color.TRANSPARENT, muted, dp(42));
            more.setFocusable(false);
            root.addView(more);
        }

        void bind(Playlist playlist) {
            title.setText(playlist.name);
            subtitle.setText(countLabel(playlist.trackUris.size(), "musica salva", "musicas salvas"));
            more.setOnClickListener(view -> showPlaylistMenu(playlist, more));
        }
    }

    private ArrayList<ToolItem> toolItems() {
        ArrayList<ToolItem> items = new ArrayList<>();
        String customPackage = customExtensionPackage();
        items.add(new ToolItem(1, "D", "Download offline", "Salvar link direto de audio em Music/PlayerMusic", "Baixar", accent, true));
        items.add(new ToolItem(2, "R", "Atualizar biblioteca", "Recarregar musicas locais e downloads recentes", "Atualizar", warm, true));
        items.add(new ToolItem(3, "EQ", "Equalizador", playbackService != null && playbackService.hasTrack() ? "Ajustar a faixa atual em tempo real" : "Toque uma musica para liberar os controles", "Abrir", blue, true));
        items.add(new ToolItem(4, "H", "Historico", "Retomar as ultimas musicas tocadas", "Ver", rose, true));
        items.add(new ToolItem(5, customPackage.isEmpty() ? "+" : "E", customPackage.isEmpty() ? "Adicionar extensao" : customPackage, customPackage.isEmpty() ? "Cadastrar outro app externo" : "Abrir ou editar extensao cadastrada", customPackage.isEmpty() ? "Cadastrar" : "Abrir", accent, true));
        items.add(new ToolItem(6, "S", "Snaptube", isPackageAvailable(SNAPTUBE_PACKAGE) ? "Abrir app instalado" : "Nao instalado neste aparelho", isPackageAvailable(SNAPTUBE_PACKAGE) ? "Abrir" : "Ausente", warm, isPackageAvailable(SNAPTUBE_PACKAGE)));
        items.add(new ToolItem(7, "!", "Uso responsavel", "Importe apenas audio que voce tem direito de usar", "Ver", blue, true));
        return items;
    }

    private boolean isPackageAvailable(String packageName) {
        return packageName != null && !packageName.trim().isEmpty() && getPackageManager().getLaunchIntentForPackage(packageName) != null;
    }

    private static class ToolItem {
        final int id;
        final String badge;
        final String title;
        final String subtitle;
        final String action;
        final int tint;
        final boolean enabled;

        ToolItem(int id, String badge, String title, String subtitle, String action, int tint, boolean enabled) {
            this.id = id;
            this.badge = badge;
            this.title = title;
            this.subtitle = subtitle;
            this.action = action;
            this.tint = tint;
            this.enabled = enabled;
        }
    }

    private class ToolAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return toolItems().size();
        }

        @Override
        public ToolItem getItem(int position) {
            return toolItems().get(position);
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ToolRow row;
            if (convertView == null) {
                row = new ToolRow();
                convertView = row.root;
                convertView.setTag(row);
            } else {
                row = (ToolRow) convertView.getTag();
            }
            row.bind(getItem(position));
            return convertView;
        }
    }

    private class ToolRow {
        final LinearLayout root;
        final TextView badge;
        final TextView title;
        final TextView subtitle;
        final TextView action;

        ToolRow() {
            root = new LinearLayout(PlayerActivity.this);
            root.setGravity(Gravity.CENTER_VERTICAL);
            root.setPadding(dp(14), dp(10), dp(12), dp(10));
            root.setLayoutParams(new AbsListView.LayoutParams(-1, dp(100)));
            attachPress(root);

            badge = label("E", 18, badgeText, Typeface.BOLD);
            badge.setGravity(Gravity.CENTER);
            root.addView(badge, new LinearLayout.LayoutParams(dp(54), dp(54)));

            LinearLayout labels = new LinearLayout(PlayerActivity.this);
            labels.setOrientation(LinearLayout.VERTICAL);
            labels.setPadding(dp(12), 0, dp(8), 0);
            root.addView(labels, new LinearLayout.LayoutParams(0, -2, 1f));

            title = label("", 16, text, Typeface.BOLD);
            title.setSingleLine(true);
            title.setEllipsize(TextUtils.TruncateAt.END);
            labels.addView(title);

            subtitle = label("", 13, muted, Typeface.NORMAL);
            subtitle.setMaxLines(2);
            subtitle.setEllipsize(TextUtils.TruncateAt.END);
            labels.addView(subtitle);

            action = label("", 12, text, Typeface.BOLD);
            action.setGravity(Gravity.CENTER);
            action.setPadding(dp(12), 0, dp(12), 0);
            root.addView(action, new LinearLayout.LayoutParams(-2, dp(36)));
        }

        void bind(ToolItem item) {
            badge.setText(item.badge);
            badge.setBackground(rounded(item.tint, dp(16), 0, 0));
            title.setText(item.title);
            title.setTextColor(item.enabled ? text : muted);
            subtitle.setText(item.subtitle);
            subtitle.setTextColor(item.enabled ? muted : soft);
            action.setText(item.action);
            action.setTextColor(item.enabled ? text : muted);
            action.setBackground(rounded(alphaColor(item.tint, item.enabled ? 42 : 22), dp(16), 1, alphaColor(item.tint, item.enabled ? 110 : 58)));
            root.setBackground(toolBackground(item.tint, item.enabled));
        }
    }
}
