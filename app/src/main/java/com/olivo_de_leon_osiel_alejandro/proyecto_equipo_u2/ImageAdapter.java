package com.olivo_de_leon_osiel_alejandro.proyecto_equipo_u2;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.*;
import android.widget.ImageView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

/**
 * Adaptador para el RecyclerView que muestra imágenes en formato Base64
 */
public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {
    private Context contexto;
    private List<String> listaImagenes; // Lista de strings Base64
    public ImageAdapter(Context contexto, List<String> listaImagenes) {
        this.contexto = contexto;
        this.listaImagenes = listaImagenes;
    }

    /**
     * Contiene la vista de cada elemento
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView vistaImagen; // ImageView para mostrar la imagen

        public ViewHolder(View vistaItem) {
            super(vistaItem);
            vistaImagen = vistaItem.findViewById(R.id.itemImageView);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup padre, int viewType) {
        // diseño de como se muestra la imagen
        View vista = LayoutInflater.from(contexto)
                .inflate(R.layout.item_image, padre, false);
        return new ViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int posicion) {
        // Convertir Base64 a Bitmap y asignarlo al ImageView
        String imagenBase64 = listaImagenes.get(posicion);
        byte[] bytesImagen = Base64.decode(imagenBase64, Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytesImagen, 0, bytesImagen.length);
        holder.vistaImagen.setImageBitmap(bitmap);
    }

    @Override
    public int getItemCount() {
        return listaImagenes.size();
    }
}