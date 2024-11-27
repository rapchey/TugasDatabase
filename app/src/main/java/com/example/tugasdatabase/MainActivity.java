package com.example.tugasdatabase;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private MediaPlayer mediaPlayer;
    private SeekBar seekBar;
    private Handler handler = new Handler();
    private TextView songTitle, songDuration, songCurrentTime;
    private ImageView albumImage;
    private Button playPauseButton, nextButton, previousButton;
    private ListView historyListView;
    private Spinner songSpinner;

    private ArrayList<HistoryItem> history = new ArrayList<>();
    private HistoryAdapter historyAdapter;

    private int[] songs = {
            R.raw.alonica, R.raw.xxl, R.raw.malibu_nights, R.raw.you, R.raw.super_far
    };

    private String[] titles = {"Alonica", "XXL", "Malibu Nights", "you!", "Super Far"};
    private int[] albumImages = {
            R.drawable.album_alonica, R.drawable.album_xxl, R.drawable.album_malibu_nights,
            R.drawable.album_you, R.drawable.album_super_far
    };

    private int currentSongIndex = 0;
    private boolean isPlaying = false;

    private SQLiteHelper dbHelper;
    private SQLiteDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        getWindow().setStatusBarColor(Color.TRANSPARENT);

        setContentView(R.layout.activity_main);

        TextView loggedInUsername = findViewById(R.id.loggedInUsername);

        String username = getIntent().getStringExtra("USERNAME");

        if (username != null) {
            loggedInUsername.setText("Welcome, " + username);
        } else {
            loggedInUsername.setText("Welcome, User");
        }

        initViews();
        initDatabase();
        initMediaPlayer();
        initHistoryAdapter();
        initSpinnerAdapter();
        loadHistoryFromDatabase();
        setupListeners();
    }


    private void initViews() {
        seekBar = findViewById(R.id.seekBar);
        songTitle = findViewById(R.id.songTitle);
        songCurrentTime = findViewById(R.id.songCurrentTime);
        songDuration = findViewById(R.id.songDuration);
        albumImage = findViewById(R.id.albumImage);
        playPauseButton = findViewById(R.id.playPauseButton);
        nextButton = findViewById(R.id.nextButton);
        previousButton = findViewById(R.id.previousButton);
        historyListView = findViewById(R.id.historyListView);
        songSpinner = findViewById(R.id.songSpinner);
    }

    private void initDatabase() {
        dbHelper = new SQLiteHelper(this);
        database = dbHelper.getWritableDatabase();
    }

    private void initMediaPlayer() {
        mediaPlayer = MediaPlayer.create(this, songs[currentSongIndex]);
        updateUI();
        mediaPlayer.setOnCompletionListener(mp -> nextSong());
    }

    private void initHistoryAdapter() {
        historyAdapter = new HistoryAdapter(this, history);
        historyListView.setAdapter(historyAdapter);
    }

    private void initSpinnerAdapter() {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, titles) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView textView = (TextView) super.getView(position, convertView, parent);
                textView.setTextColor(Color.parseColor("#000001"));
                textView.setTypeface(ResourcesCompat.getFont(getContext(), R.font.gotham));
                textView.setBackgroundColor(Color.parseColor("#1DB954"));
                return textView;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView textView = (TextView) super.getDropDownView(position, convertView, parent);
                textView.setTextColor(Color.parseColor("#000001"));
                textView.setTypeface(ResourcesCompat.getFont(getContext(), R.font.gotham));
                textView.setBackgroundColor(Color.parseColor("#1DB954"));
                return textView;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        songSpinner.setAdapter(adapter);
    }

    private void setupListeners() {
        playPauseButton.setOnClickListener(v -> togglePlayPause());
        nextButton.setOnClickListener(v -> nextSong());
        previousButton.setOnClickListener(v -> previousSong());

        songSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != currentSongIndex) {
                    changeSong(position);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    mediaPlayer.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void togglePlayPause() {
        if (isPlaying) {
            pauseMedia();
            playPauseButton.setText("Play");
        } else {
            playMedia();
            playPauseButton.setText("Pause");
            updateHistory();
        }
        isPlaying = !isPlaying;
    }

    private void updateSeekBar() {
        seekBar.setMax(mediaPlayer.getDuration());
        handler.postDelayed(updateTimeTask, 100);
    }

    private Runnable updateTimeTask = new Runnable() {
        @Override
        public void run() {
            seekBar.setProgress(mediaPlayer.getCurrentPosition());
            songCurrentTime.setText(formatTime(mediaPlayer.getCurrentPosition()));
            handler.postDelayed(this, 100);
        }
    };

    private void updateHistory() {
        String title = titles[currentSongIndex];
        int albumImage = albumImages[currentSongIndex];
        saveToDatabase(title, albumImage);
        history.add(0, new HistoryItem(title, albumImage));
        historyAdapter.notifyDataSetChanged();
    }

    private void loadHistoryFromDatabase() {
        try (Cursor cursor = database.rawQuery("SELECT * FROM " + SQLiteHelper.TABLE_HISTORY + " ORDER BY id DESC", null)) {
            if (cursor != null) {
                int titleIndex = cursor.getColumnIndex(SQLiteHelper.COLUMN_TITLE);
                int albumImageIndex = cursor.getColumnIndex(SQLiteHelper.COLUMN_ALBUM_IMAGE);

                if (titleIndex >= 0 && albumImageIndex >= 0) {
                    history.clear();
                    while (cursor.moveToNext()) {
                        String title = cursor.getString(titleIndex);
                        int albumImage = cursor.getInt(albumImageIndex);
                        history.add(new HistoryItem(title, albumImage));
                    }
                }
            }
        } catch (Exception e) {
            Log.e("Database", "Error loading history: " + e.getMessage());
        }
        historyAdapter.notifyDataSetChanged();
    }

    private void saveToDatabase(String title, int albumImage) {
        try {
            database.execSQL("INSERT INTO " + SQLiteHelper.TABLE_HISTORY + " (" +
                            SQLiteHelper.COLUMN_TITLE + ", " +
                            SQLiteHelper.COLUMN_ALBUM_IMAGE + ") VALUES (?, ?)",
                    new Object[]{title, albumImage});
        } catch (Exception e) {
            Log.e("Database", "Error saving to database: " + e.getMessage());
        }
    }

    private void updateUI() {
        songTitle.setText(titles[currentSongIndex]);
        albumImage.setImageResource(albumImages[currentSongIndex]);
        songDuration.setText(formatTime(mediaPlayer.getDuration()));
        seekBar.setProgress(0);
        seekBar.setMax(mediaPlayer.getDuration());
        songSpinner.setSelection(currentSongIndex);
    }

    private void nextSong() {
        changeSong((currentSongIndex + 1) % songs.length);
    }

    private void previousSong() {
        changeSong((currentSongIndex - 1 + songs.length) % songs.length);
    }

    private void changeSong(int index) {
        mediaPlayer.reset();
        currentSongIndex = index;
        mediaPlayer = MediaPlayer.create(this, songs[currentSongIndex]);
        updateUI();
        mediaPlayer.start();
        updateSeekBar();
        updateHistory();
        isPlaying = true;
        playPauseButton.setText("Pause");
    }

    private void playMedia() {
        mediaPlayer.start();
        updateSeekBar();
    }

    private void pauseMedia() {
        mediaPlayer.pause();
    }

    private String formatTime(int milliseconds) {
        int minutes = (milliseconds / 1000) / 60;
        int seconds = (milliseconds / 1000) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    @Override
    protected void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        handler.removeCallbacks(updateTimeTask);
        super.onDestroy();
    }
}