package com.nitishweb.app12;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;

import com.google.android.material.snackbar.Snackbar;
import com.nitishweb.app12.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;

import static java.lang.Thread.sleep;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private ArrayList<String> musicList;    //List holds music values for AutoComplete TextView
    private LinkedHashMap<String, Music> musicMap;  //Holds list of Music objects with it's name as key
    private Music nowPlaying;   //Current selected music object
    private MediaPlayer player;
    private final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initViews();
    }

    private void initViews() {
        binding.seekBar.setProgress(0);
        binding.seekBar.setEnabled(false);

        musicList = new ArrayList<>();
        musicMap = new LinkedHashMap<>();

        populateMusicList();    //Generate list of predefined music from assets

        //Set Adapter for AutoComplete TextView
        ArrayAdapter<String> musicACTVAdapter = new ArrayAdapter<>(this, R.layout.simple_list_item, R.id.list_item, musicList);
        binding.musicACTV.setAdapter(musicACTVAdapter);

        setListeners();
    }

    private void setListeners() {
        binding.musicACTV.setOnItemClickListener((parent, view, position, id) -> {
            nowPlaying = musicMap.get(musicList.get(position));
            Log.d(TAG, "onItemSelected: " + position);
            if (nowPlaying!=null) {
                //Set Image and Text of Music object
                binding.nowPlayingIV.setImageDrawable(ContextCompat.getDrawable(this, nowPlaying.imageId));
                binding.nowPlayingTV.setText(nowPlaying.name);
                //Create MediaPlayer instance
                if (player != null) player.stop();
                player = MediaPlayer.create(this, nowPlaying.resourceId);
                //Reset SeekBar according to Music
                binding.seekBar.setProgress(0);
                binding.seekBar.setMax(player.getDuration() / 1000);
                binding.seekBar.setEnabled(true);
                //Update Duration Text View
                int duration = player.getDuration() / 1000;
                int minutes = (duration % 3600) / 60;
                int seconds = duration % 60;
                String durationTxt = String.format(Locale.ENGLISH,"%d:%02d",minutes,seconds);
                binding.durationTV.setText(durationTxt);
            } else {
                //Clear everything in case any unexpected condition occurs
                binding.nowPlayingIV.setImageDrawable(null);
                binding.nowPlayingTV.setText("");
                Snackbar.make(binding.getRoot(), "Error! Music Not Found.", Snackbar.LENGTH_SHORT).show();
            }
        });

        binding.ibPlay.setOnClickListener(v-> {
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

        binding.ibStop.setOnClickListener(v-> {
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

        binding.ibPause.setOnClickListener(v-> {
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
                String progressTxt = String.format(Locale.ENGLISH,"%d:%02d",minutes,seconds);
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
            while (player!=null && player.isPlaying()) {
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

        for (LinkedHashMap.Entry<String, Music> pair: musicMap.entrySet()) {
            musicList.add(pair.getKey());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) player.stop();
    }

    class Music {
        private String name;
        private int resourceId;
        private int imageId;

        public Music(String name, int resourceId, int imageId) {
            this.name = name;
            this.resourceId = resourceId;
            this.imageId = imageId;
        }
    }
}