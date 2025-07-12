package upvictoria.pm_may_ago_2024.iti_271415.pg2u2_eq02;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONObject;

public class ConfigActivity extends AppCompatActivity {

    private EditText etNumZonas;
    private EditText etNumCirculos;
    private Button btnGenerarCampos;
    private Button btnGuardar;
    private LinearLayout zonaInputs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        etNumZonas = findViewById(R.id.etNumZonas);
        etNumCirculos = findViewById(R.id.etNumCirculos);
        btnGenerarCampos = findViewById(R.id.btnGenerarCampos);
        btnGuardar = findViewById(R.id.btnSave);
        zonaInputs = findViewById(R.id.zonaInputs);

        btnGenerarCampos.setOnClickListener(v -> {
            zonaInputs.removeAllViews();
            int numZonas = Integer.parseInt(etNumZonas.getText().toString());
            int numCirculos = Integer.parseInt(etNumCirculos.getText().toString());

            for (int i = 0; i < numZonas; i++) {
                TextView label = new TextView(this);
                label.setText("Zona " + (i + 1));
                EditText input = new EditText(this);
                input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                input.setHint("Círculos esperados");
                input.setId(1000 + i);

                zonaInputs.addView(label);
                zonaInputs.addView(input);

                input.setText(String.valueOf(numCirculos));
            }
        });

        btnGuardar.setOnClickListener(v -> {
            JSONArray zonas = new JSONArray();
            int count = zonaInputs.getChildCount();
            int zonaId = 1;
            for (int i = 1; i < count; i += 2) {
                EditText input = (EditText) zonaInputs.getChildAt(i);
                String texto = input.getText().toString();
                int expected = texto.isEmpty() ? 0 : Integer.parseInt(texto);
                JSONObject zona = new JSONObject();
                try {
                    zona.put("id", zonaId++);
                    zona.put("expectedCount", expected);
                    zonas.put(zona);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            SharedPreferences prefs = getSharedPreferences("ZONES", MODE_PRIVATE);
            prefs.edit().putString("zonas_config", zonas.toString()).apply();
            Toast.makeText(this, "Zonas guardadas", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
