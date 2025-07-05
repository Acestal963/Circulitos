package com.olivo_de_leon_osiel_alejandro.proyecto_equipo_u2;

import android.os.Bundle;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.*;
import com.google.firebase.database.*;
import java.util.*;

public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private ImageAdapter adaptador;
    private List<String> listaImagenes = new ArrayList<>();
    private DatabaseReference referenciaDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Configurar RecyclerView
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Inicializar adaptador
        adaptador = new ImageAdapter(this, listaImagenes);
        recyclerView.setAdapter(adaptador);

        // Referencia a la base de datos Firebase
        referenciaDB = FirebaseDatabase.getInstance().getReference("respuestas");

        // Escuchar cambios en Firebase
        referenciaDB.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listaImagenes.clear();
                for (DataSnapshot dato : snapshot.getChildren()) {
                    String imagenBase64 = dato.getValue(String.class);
                    listaImagenes.add(imagenBase64);
                }
                adaptador.notifyDataSetChanged(); // Actualizar vista
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Error al cargar imágenes", Toast.LENGTH_SHORT).show();
            }
        });

        // Botón para abrir actividad de subida
        findViewById(R.id.fabUpload).setOnClickListener(v -> {
            startActivity(new Intent(this, UploadActivity.class));
        });
    }
}