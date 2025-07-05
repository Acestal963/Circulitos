package com.olivo_de_leon_osiel_alejandro.proyecto_equipo_u2;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.*;
import java.io.*;

public class UploadActivity extends AppCompatActivity {
    private ImageView vistaPrevia;
    private Button btnSelect, btnUpload;
    private ProgressBar progressBar;
    private Bitmap imagenSeleccionada;
    private DatabaseReference referenciaDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        // Inicializar vistas
        vistaPrevia = findViewById(R.id.imagePreview);
        btnSelect = findViewById(R.id.btnSelect);
        btnUpload = findViewById(R.id.btnUpload);
        progressBar = findViewById(R.id.progressUpload);

        // Referencia a Firebase
        referenciaDB = FirebaseDatabase.getInstance().getReference("respuestas");

        // Botón para seleccionar imagen
        btnSelect.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(intent, 100);
        });

        // Botón para subir imagen
        btnUpload.setOnClickListener(v -> {
            if (imagenSeleccionada != null) {
                subirImagen();
            } else {
                Toast.makeText(this, "Selecciona una imagen primero", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Procesar imagen seleccionada
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            try {
                Uri uriImagen = data.getData();
                imagenSeleccionada = MediaStore.Images.Media.getBitmap(getContentResolver(), uriImagen);
                vistaPrevia.setImageBitmap(imagenSeleccionada);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error al cargar imagen", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Convierte la imagen a Base64 y la sube a Firebase
     */
    private void subirImagen() {
        progressBar.setVisibility(View.VISIBLE);

        // Convertir a Base64
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        imagenSeleccionada.compress(Bitmap.CompressFormat.JPEG, 50, baos);
        String imagenBase64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);

        // Subir a Firebase
        String id = referenciaDB.push().getKey();
        referenciaDB.child(id).setValue(imagenBase64)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Imagen subida con éxito", Toast.LENGTH_SHORT).show();
                        finish(); // Volver a MainActivity
                    } else {
                        Toast.makeText(this, "Error al subir imagen", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}