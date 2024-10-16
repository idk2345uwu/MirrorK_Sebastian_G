package com.mirror.capsulas1;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class usuario_perfil extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.usuario_perfil);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button btnCargarVideo = findViewById(R.id.agregar_video_id);
        btnCargarVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(usuario_perfil.this);
                String videoUriString = sharedPreferences.getString("video_uri", null);
                String markedTimesJson = sharedPreferences.getString("marked_times", null);

                if (videoUriString != null && markedTimesJson != null) {
                    Intent intent = new Intent(usuario_perfil.this, Video.class);
                    intent.putExtra("video_uri", videoUriString);
                    intent.putExtra("marked_times", markedTimesJson);
                    startActivity(intent);
                } else {
                    Toast.makeText(usuario_perfil.this, "No hay video guardado", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}