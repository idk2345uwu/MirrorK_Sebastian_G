package com.mirror.capsulas1;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;

public class Video extends AppCompatActivity {

    private TextureView textureView;
    private MediaPlayer mediaPlayer;
    private SeekBar seekBar;
    private ArrayList<Integer> markedTimes = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private ArrayList<String> displayTimes = new ArrayList<>();
    private MediaPlayer beepSound;
    private Handler handler;
    private boolean isMirrored = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        textureView = findViewById(R.id.textureView);
        seekBar = findViewById(R.id.seekBar);
        Button btnMarkTime = findViewById(R.id.btnMarkTime);
        Button btnMirror = findViewById(R.id.btnMirror);
        Button btnPlayPause = findViewById(R.id.btnPlayPause);
        ListView timesListView = findViewById(R.id.timesListView);


        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, displayTimes);
        timesListView.setAdapter(adapter);


        beepSound = MediaPlayer.create(this, R.raw.beep);


        handler = new Handler();


        btnMirror.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(Video.this, "Botón espejo presionado", Toast.LENGTH_SHORT).show();
                if (isMirrored) {
                    textureView.setScaleX(1f);
                    isMirrored = false;
                } else {
                    textureView.setScaleX(-1f);
                    isMirrored = true;
                }
            }
        });


        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
                checkPermissionsAndOpenVideoPicker();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {}

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {}
        });


        btnMarkTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mediaPlayer != null) {
                    int currentTime = mediaPlayer.getCurrentPosition();
                    markedTimes.add(currentTime);
                    displayTimes.add(formatTime(currentTime));
                    adapter.notifyDataSetChanged();
                    Toast.makeText(Video.this, "Tiempo marcado: " + formatTime(currentTime), Toast.LENGTH_SHORT).show();
                }
            }
        });


        btnPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mediaPlayer != null) {
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.pause();
                        btnPlayPause.setText("Reproducir");
                    } else {
                        mediaPlayer.start();
                        btnPlayPause.setText("Pausar");
                    }
                }
            }
        });
    }


    private void checkPermissionsAndOpenVideoPicker() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_VIDEO}, 1);
            } else {
                openVideoPicker();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            } else {
                openVideoPicker();
            }
        }
    }


    private void openVideoPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, 1);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            Uri selectedVideo = data.getData();
            playVideo(selectedVideo);
        }
    }


    public void playVideo(Uri videoUri) {
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(this, videoUri);
            mediaPlayer.setSurface(new Surface(textureView.getSurfaceTexture()));
            mediaPlayer.prepare();
            mediaPlayer.start();


            seekBar.setMax(mediaPlayer.getDuration());
            updateSeekBar();


            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    checkMarkedTimes();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void updateSeekBar() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null) {
                    seekBar.setProgress(mediaPlayer.getCurrentPosition());
                    handler.postDelayed(this, 500);
                }
            }
        }, 500);


        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) {
                    mediaPlayer.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }


    private void checkMarkedTimes() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null) {
                    int currentTime = mediaPlayer.getCurrentPosition();


                    for (int time : markedTimes) {
                        if (Math.abs(currentTime - time) < 500) {  // Tolerancia de 500 ms
                            playBeepSound();
                            break;
                        }
                    }
                }


                handler.postDelayed(this, 500);
            }
        }, 500);
    }


    private void playBeepSound() {
        if (beepSound != null) {
            beepSound.start();
        }
    }


    private String formatTime(int timeMs) {
        int seconds = (timeMs / 1000) % 60;
        int minutes = (timeMs / 1000) / 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (beepSound != null) {
            beepSound.release();
            beepSound = null;
        }


        handler.removeCallbacksAndMessages(null);


        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}