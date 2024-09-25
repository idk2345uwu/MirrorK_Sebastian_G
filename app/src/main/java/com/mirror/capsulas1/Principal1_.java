package com.mirror.capsulas1;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Principal1_ extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_principal2);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    public void Login2(View V){

        EditText campo1 = this.findViewById(R.id.correo);
        String correo = campo1.getText().toString();
        EditText campo2 = this.findViewById(R.id.contrasenia);
        String contrasenia = campo2.getText().toString();
        System.out.println(correo+" "+contrasenia);


        if(correo.equals("c1") && contrasenia.equals("123")){
            Intent i = new Intent(this,MainActivity2.class);
            startActivity(i);
        }else{
            Toast.makeText(this,"Error", Toast.LENGTH_SHORT).show();
        }

    }
}