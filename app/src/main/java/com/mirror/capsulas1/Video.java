package com.mirror.capsulas1;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class Video extends AppCompatActivity {

    private TextureView textureView;
    private MediaPlayer mediaPlayer;
    private SeekBar seekBar;
    private Map<Integer, String> markedTimes = new HashMap<>();
    private ArrayAdapter<String> adapter;
    private ArrayList<String> displayTimes = new ArrayList<>();
    private MediaPlayer beepSound;
    private Handler handler;
    private boolean isMirrored = false;
    private boolean isPlaying = false;
    private Uri selectedVideoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        textureView = findViewById(R.id.textureView);
        seekBar = findViewById(R.id.seekBar);
        Button btnMarkTime = findViewById(R.id.btnMarkTime);
        Button btnMirror = findViewById(R.id.btnMirror);
        Button btnPlayPause = findViewById(R.id.btnPlayPause);
        Button btnGuardar = findViewById(R.id.guardar);
        ListView timesListView = findViewById(R.id.timesListView);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, displayTimes);
        timesListView.setAdapter(adapter);

        beepSound = MediaPlayer.create(this, R.raw.beep);
        handler = new Handler();

        Intent intent = getIntent();
        String videoUriString = intent.getStringExtra("video_uri");
        String markedTimesJson = intent.getStringExtra("marked_times");

        if (videoUriString != null && markedTimesJson != null) {
            selectedVideoUri = Uri.parse(videoUriString);
            loadMarkedTimes(markedTimesJson);
            playVideo(selectedVideoUri);
        }

        btnMirror.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
                    AlertDialog.Builder builder = new AlertDialog.Builder(Video.this);
                    builder.setTitle("Describe el evento");

                    final EditText input = new EditText(Video.this);
                    input.setInputType(InputType.TYPE_CLASS_TEXT);
                    builder.setView(input);

                    builder.setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String description = input.getText().toString();
                            markedTimes.put(currentTime, description);
                            displayTimes.add(description);
                            adapter.notifyDataSetChanged();
                            saveMarkedTimes();
                            Toast.makeText(Video.this, "Tiempo marcado: " + description, Toast.LENGTH_SHORT).show();
                        }
                    });

                    builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });

                    builder.show();
                }
            }
        });

        btnPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mediaPlayer != null) {
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.pause();
                        isPlaying = false;
                        btnPlayPause.setText("Reproducir");
                        handler.removeCallbacksAndMessages(null);
                    } else {
                        mediaPlayer.start();
                        isPlaying = true;
                        btnPlayPause.setText("Pausar");
                        checkMarkedTimes();
                    }
                }
            }
        });

        btnGuardar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveMarkedTimes();
            }
        });

        timesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Integer selectedTime = (Integer) markedTimes.keySet().toArray()[position];
                if (mediaPlayer != null) {
                    mediaPlayer.seekTo(selectedTime);
                    seekBar.setProgress(selectedTime);
                }
            }
        });

        loadMarkedTimes(markedTimesJson);
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
            selectedVideoUri = selectedVideo;
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
                }
                handler.postDelayed(this, 1000);
            }
        }, 0);

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
                if (mediaPlayer != null && isPlaying) {
                    int currentTime = mediaPlayer.getCurrentPosition();

                    for (Map.Entry<Integer, String> entry : markedTimes.entrySet()) {
                        if (Math.abs(currentTime - entry.getKey()) < 200) {
                            playBeepSound();
                            break;
                        }
                    }
                }
                handler.postDelayed(this, 50);
            }
        }, 50);
    }

    private void playBeepSound() {
        if (beepSound != null) {
            beepSound.start();
        }
    }

    private String formatTime(int timeMs) {
        int milliseconds = (timeMs % 1000) / 10;
        int seconds = (timeMs / 1000) % 60;
        int minutes = (timeMs / 1000) / 60;
        return String.format("%02d:%02d.%02d", minutes, seconds, milliseconds);
    }

    private void saveMarkedTimes() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        JSONArray jsonArray = new JSONArray();
        for (Map.Entry<Integer, String> entry : markedTimes.entrySet()) {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("time", entry.getKey());
                jsonObject.put("description", entry.getValue());
                jsonArray.put(jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        editor.putString("marked_times", jsonArray.toString());
        editor.apply();

        Toast.makeText(this, "Tiempos guardados", Toast.LENGTH_SHORT).show();
    }

    private void loadMarkedTimes(String markedTimesJson) {
        if (markedTimesJson != null) {
            try {
                JSONArray jsonArray = new JSONArray(markedTimesJson);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    int time = jsonObject.getInt("time");
                    String description = jsonObject.getString("description");
                    markedTimes.put(time, description);
                    displayTimes.add(description);
                }
                adapter.notifyDataSetChanged();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (beepSound != null) {
            beepSound.release();
            beepSound = null;
        }
        handler.removeCallbacksAndMessages(null);
    }
}