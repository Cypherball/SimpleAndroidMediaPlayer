package com.nitishweb.app12;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.MediaController;
import android.widget.SeekBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;
import com.nitishweb.app12.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;

import static java.lang.Thread.sleep;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "MainActivity";
    ArrayAdapter<String> musicACTVAdapter;
    ArrayAdapter<String> videoACTVAdapter;
    private ActivityMainBinding binding;
    private ArrayList<String> musicList;    //List holds music values for AutoComplete TextView
    private LinkedHashMap<String, Music> musicMap;  //Holds list of Music objects with it's name as key
    private ArrayList<String> videoList;    //List holds video values for AutoComplete TextView
    private LinkedHashMap<String, Video> videoMap;  //Holds list of Video objects with it's name as key
    private Music nowPlaying;   //Current selected music object
    private Video videoPlaying;   //Current selected music object
    private MediaPlayer player;
    private MediaController controller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        controller = new MediaController(this);
        initViews();
    }

    private void initViews() {
        binding.musicRadio.setChecked(true);
        controller.setAnchorView(binding.videoView);
        binding.videoView.setMediaController(controller);

        musicList = new ArrayList<>();
        musicMap = new LinkedHashMap<>();

        videoList = new ArrayList<>();
        videoMap = new LinkedHashMap<>();

        populateMusicList();    //Generate list of predefined music from assets
        populateVideoList();

        //Set Adapter for AutoComplete TextView
        musicACTVAdapter = new ArrayAdapter<>(this, R.layout.simple_list_item, R.id.list_item, musicList);
        videoACTVAdapter = new ArrayAdapter<>(this, R.layout.simple_list_item, R.id.list_item, videoList);

        loadMusicLayout();

        setListeners();
    }

    private void setListeners() {
        binding.playerRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            binding.selectACTV.setText("");
            if (checkedId == R.id.musicRadio) {
                loadMusicLayout();
            } else if (checkedId == R.id.videoRadio) {
                loadVideoLayout();
            }
        });

        binding.selectACTV.setOnItemClickListener((parent, view, position, id) -> {
            if (binding.musicRadio.isChecked()) {
                nowPlaying = musicMap.get(musicList.get(position));
                Log.d(TAG, "onItemSelected: " + position);
                if (nowPlaying != null) {
                    //Set Image and Text of Music object
                    binding.nowPlayingIV.setImageDrawable(ContextCompat.getDrawable(this, nowPlaying.imageId));
                    binding.nowPlayingTV.setText(nowPlaying.name);
                    //Create MediaPlayer instance
                    if (player != null)
                        player.stop();
                    player = MediaPlayer.create(this, nowPlaying.resourceId);
                    //Reset SeekBar according to Music
                    binding.seekBar.setProgress(0);
                    binding.seekBar.setMax(player.getDuration() / 1000);
                    binding.seekBar.setEnabled(true);
                    //Update Duration Text View
                    int duration = player.getDuration() / 1000;
                    int minutes = (duration % 3600) / 60;
                    int seconds = duration % 60;
                    String durationTxt = String.format(Locale.ENGLISH, "%d:%02d", minutes, seconds);
                    binding.durationTV.setText(durationTxt);
                } else {
                    //Clear everything in case any unexpected condition occurs
                    binding.nowPlayingIV.setImageDrawable(null);
                    binding.nowPlayingTV.setText("");
                    Snackbar.make(binding.getRoot(), "Error! Music Not Found.", Snackbar.LENGTH_SHORT).show();
                }
            } else if (binding.videoRadio.isChecked()) {
                Log.d(TAG, "onItemSelected: " + position);
                if (binding.videoView.isPlaying())
                    binding.videoView.stopPlayback();
                videoPlaying = videoMap.get(videoList.get(position));
                binding.videoView.setVideoURI(videoPlaying.uri);
                binding.videoView.seekTo(0);
            }
        });

        binding.ibPlay.setOnClickListener(v -> {
            if (nowPlaying == null || player == null) {
                Snackbar.make(binding.getRoot(), "No Music Selected!", Snackbar.LENGTH_SHORT).show();
                return;
            }
            //Play music if not currently playing
            if (!player.isPlaying()) {
                player.start();
                activateSeekBar();
            }
            Snackbar.make(binding.getRoot(), "Now Playing: " + nowPlaying.name, Snackbar.LENGTH_SHORT).show();
        });

        binding.ibStop.setOnClickListener(v -> {
            if (nowPlaying == null || player == null) {
                Snackbar.make(binding.getRoot(), "No Music Selected!", Snackbar.LENGTH_SHORT).show();
                return;
            }
            //Stop playing and recreate media player instance
            player.stop();
            player = MediaPlayer.create(this, nowPlaying.resourceId);
            binding.seekBar.setProgress(0); //Reset SeekBar
            Snackbar.make(binding.getRoot(), "Music Stopped", Snackbar.LENGTH_SHORT).show();
        });

        binding.ibPause.setOnClickListener(v -> {
            if (nowPlaying == null || player == null) {
                Snackbar.make(binding.getRoot(), "No Music Selected!", Snackbar.LENGTH_SHORT).show();
                return;
            }
            if (player.isPlaying()) {
                player.pause();
                Snackbar.make(binding.getRoot(), "Music Paused", Snackbar.LENGTH_SHORT).show();
            }
        });

        binding.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //Seeks the player position to specified progress value if it is user input
                if (player != null && fromUser) {
                    player.seekTo(progress * 1000);
                }
                //Update Progress Text View
                int minutes = (progress % 3600) / 60;
                int seconds = progress % 60;
                String progressTxt = String.format(Locale.ENGLISH, "%d:%02d", minutes, seconds);
                binding.progressTV.setText(progressTxt);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    //Updates seekbar every second
    private void activateSeekBar() {
        Runnable mRunnable = () -> {
            while (player != null && player.isPlaying()) {
                Log.d(TAG, "setListeners: Position: " + player.getCurrentPosition() / 1000);
                binding.seekBar.setProgress(player.getCurrentPosition() / 1000);
                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        //Execute SeekBar Update on separate thread
        Thread thread = new Thread(mRunnable);
        thread.start();
    }

    private void loadMusicLayout() {
        if (binding.videoView.isPlaying())
            binding.videoView.stopPlayback();
        binding.titleTV.setText("Music Player");
        binding.selectTitleTV.setText("Select Music");
        binding.selectACTV.setAdapter(musicACTVAdapter);
        binding.seekBar.setVisibility(View.VISIBLE);
        binding.ibPlay.setVisibility(View.VISIBLE);
        binding.ibPause.setVisibility(View.VISIBLE);
        binding.ibStop.setVisibility(View.VISIBLE);
        binding.progressTV.setVisibility(View.VISIBLE);
        binding.durationTV.setVisibility(View.VISIBLE);
        binding.nowPlayingIV.setVisibility(View.VISIBLE);
        binding.nowPlayingTV.setVisibility(View.VISIBLE);
        binding.videoView.setVisibility(View.GONE);

        nowPlaying = null;
        binding.nowPlayingTV.setText("");
        binding.nowPlayingIV.setImageDrawable(null);
        player = null;
        binding.seekBar.setProgress(0);
        binding.seekBar.setEnabled(false);
    }

    private void loadVideoLayout() {
        if (player != null)
            player.stop();
        binding.titleTV.setText("Video Player");
        binding.selectTitleTV.setText("Select Video");
        binding.selectACTV.setAdapter(videoACTVAdapter);
        binding.seekBar.setVisibility(View.GONE);
        binding.ibPlay.setVisibility(View.GONE);
        binding.ibPause.setVisibility(View.GONE);
        binding.ibStop.setVisibility(View.GONE);
        binding.progressTV.setVisibility(View.GONE);
        binding.durationTV.setVisibility(View.GONE);
        binding.nowPlayingIV.setVisibility(View.GONE);
        binding.nowPlayingTV.setVisibility(View.GONE);
        binding.videoView.setVisibility(View.VISIBLE);
        controller.setAnchorView(binding.videoView);

        binding.videoView.stopPlayback();
        binding.videoView.seekTo(0);
    }

    private void populateMusicList() {
        musicMap.put("Bohemian Rhapsody",
                new Music("Bohemian Rhapsody", R.raw.bohemian_rhapsody, R.drawable.bohemian_rhapsody));

        musicMap.put("Can't Take My Eyes Off You",
                new Music("Can't Take My Eyes Off You", R.raw.cant_take_my_eyes_off_you, R.drawable.cant_take_my_eyes_off_you));

        musicMap.put("Dancing in the Moonlight",
                new Music("Dancing in the Moonlight", R.raw.dancing_in_the_moonlight, R.drawable.dancing_in_the_moonlight));

        musicMap.put("Eye of the Tiger",
                new Music("Eye of the Tiger", R.raw.eye_of_the_tiger, R.drawable.eye_of_the_tiger));

        musicMap.put("Karma Chameleon",
                new Music("Karma Chameleon", R.raw.karma_chameleon, R.drawable.karma_chameleon));

        for (LinkedHashMap.Entry<String, Music> pair : musicMap.entrySet()) {
            musicList.add(pair.getKey());
        }
    }

    private void populateVideoList() {
        videoMap.put("Electric Love",
                new Video("Electric Love", Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.electric_love)));

        videoMap.put("Hometown",
                new Video("Hometown", Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.hometown)));

        videoMap.put("House in LA",
                new Video("House in LA", Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.house_in_la)));

        videoMap.put("I was Wrong",
                new Video("I was Wrong", Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.i_was_wrong)));

        videoMap.put("Ride Slow",
                new Video("Ride Slow", Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.ride_slow)));

        for (LinkedHashMap.Entry<String, Video> pair : videoMap.entrySet()) {
            videoList.add(pair.getKey());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null)
            player.stop();
        if (binding.videoView.isPlaying())
            binding.videoView.stopPlayback();
    }

    static class Music {
        private String name;
        private int resourceId;
        private int imageId;

        public Music(String name, int resourceId, int imageId) {
            this.name = name;
            this.resourceId = resourceId;
            this.imageId = imageId;
        }
    }

    static class Video {
        private String name;
        private Uri uri;

        public Video(String name, Uri uri) {
            this.name = name;
            this.uri = uri;
        }
    }
}