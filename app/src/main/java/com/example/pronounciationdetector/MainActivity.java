package com.example.pronounciationdetector;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_MIC_PERMISSION = 100;
    private static final int REQUEST_SPEECH_INPUT = 1;

    private TextView resultText, correctedText, confidenceScore, ipaText;
    private AppCompatButton startButton, playCorrectPronunciation, saveProgressButton, viewHistoryButton;
    private Spinner difficultySpinner;
    private TextToSpeech textToSpeech;
    private HashMap<String, String> pronunciationDictionary;
    private DatabaseHelper databaseHelper;
    private String selectedDifficulty = "Easy";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI components
        resultText = findViewById(R.id.result_text);
        correctedText = findViewById(R.id.corrected_text);
        confidenceScore = findViewById(R.id.confidence_score);
        ipaText = findViewById(R.id.ipa_text);
        startButton = findViewById(R.id.start_button);
        playCorrectPronunciation = findViewById(R.id.play_correct_pronunciation);
        saveProgressButton = findViewById(R.id.save_progress_button);
        viewHistoryButton = findViewById(R.id.view_history_button);
        difficultySpinner = findViewById(R.id.difficulty_spinner);

        databaseHelper = new DatabaseHelper(this);

        // Initialize pronunciation dictionary
        setupPronunciationDictionary();

        // Initialize Text-to-Speech
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(Locale.US);
            }
        });

        // Set up difficulty spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.difficulty_levels, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        difficultySpinner.setAdapter(adapter);

        // Handle spinner selection
        difficultySpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                selectedDifficulty = parent.getItemAtPosition(position).toString();
                Toast.makeText(MainActivity.this, "Selected Difficulty: " + selectedDifficulty, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                selectedDifficulty = "Easy";
            }
        });

        // Button click listeners
        startButton.setOnClickListener(v -> checkMicPermission());
        playCorrectPronunciation.setOnClickListener(v -> speakCorrectPronunciation());
        saveProgressButton.setOnClickListener(v -> saveProgress());
        viewHistoryButton.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, HistoryActivity.class)));
    }

    /**
     * Check for microphone permission before starting speech recognition
     */
    private void checkMicPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_MIC_PERMISSION);
        } else {
            startSpeechRecognition();
        }
    }

    /**
     * Handle permission request results
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_MIC_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startSpeechRecognition();
            } else {
                Toast.makeText(this, "Microphone permission is required for speech recognition", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Start speech recognition using Google's built-in mic UI
     */
    private void startSpeechRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now..."); // Shows Google’s mic pop-up

        try {
            startActivityForResult(intent, REQUEST_SPEECH_INPUT);
        } catch (Exception e) {
            Toast.makeText(this, "Speech recognition is not supported on this device", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Handle speech recognition results
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_SPEECH_INPUT && resultCode == RESULT_OK && data != null) {
            ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            float[] confidenceScores = data.getFloatArrayExtra(RecognizerIntent.EXTRA_CONFIDENCE_SCORES);

            if (matches != null && !matches.isEmpty()) {
                String spokenText = matches.get(0);
                resultText.setText("You said: " + spokenText);
                highlightMistakes(spokenText);

                // Set confidence score if available
                if (confidenceScores != null && confidenceScores.length > 0) {
                    confidenceScore.setText("Confidence: " + (confidenceScores[0] * 100) + "%");
                } else {
                    confidenceScore.setText("Confidence: N/A");
                }
            }
        }
    }

    /**
     * Highlight pronunciation mistakes and provide IPA correction
     */
    private void highlightMistakes(String spokenText) {
        StringBuilder correctedSpeech = new StringBuilder();
        StringBuilder ipaBuilder = new StringBuilder();
        String[] words = spokenText.split(" ");

        for (String word : words) {
            if (pronunciationDictionary.containsKey(word.toLowerCase())) {
                correctedSpeech.append(word).append(" (✔) ");
                ipaBuilder.append(word).append(": ").append(pronunciationDictionary.get(word.toLowerCase())).append("\n");
            } else {
                correctedSpeech.append("(|)").append(word).append(" ");
            }
        }

        correctedText.setText("Correction: " + correctedSpeech.toString());
        ipaText.setText(ipaBuilder.toString());
    }

    /**
     * Speak the correct pronunciation of the recognized text
     */
    private void speakCorrectPronunciation() {
        String text = resultText.getText().toString().replace("You said: ", "");
        if (!text.isEmpty()) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    /**
     * Save progress to SQLite database
     */
    private void saveProgress() {
        String spokenText = resultText.getText().toString().replace("You said: ", "");
        String correction = correctedText.getText().toString();
        String ipa = ipaText.getText().toString();
        String confidence = confidenceScore.getText().toString();

        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("spoken_text", spokenText);
        values.put("correction", correction);
        values.put("ipa", ipa);
        values.put("confidence", confidence);

        long newRowId = db.insert("history", null, values);
        if (newRowId != -1) {
            Toast.makeText(this, "Progress Saved! Row ID: " + newRowId, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to save progress", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Setup a dictionary for correct IPA pronunciations
     */
    private void setupPronunciationDictionary() {
        pronunciationDictionary = new HashMap<>();
        pronunciationDictionary.put("hello", "həˈloʊ");
        pronunciationDictionary.put("world", "wɝːld");
        pronunciationDictionary.put("computer", "kəmˈpjuːtər");
        pronunciationDictionary.put("language", "ˈlæŋɡwɪdʒ");
        pronunciationDictionary.put("technology", "tɛkˈnɒlədʒi");
        pronunciationDictionary.put("science", "ˈsaɪəns");
        pronunciationDictionary.put("engineer", "ˌɛnʤɪˈnɪr");
        pronunciationDictionary.put("application", "ˌæplɪˈkeɪʃən");
        pronunciationDictionary.put("system", "ˈsɪstəm");
        pronunciationDictionary.put("network", "ˈnɛtwɜːrk");
        pronunciationDictionary.put("developer", "dɪˈvɛləpər");
        pronunciationDictionary.put("programming", "ˈproʊɡræmɪŋ");
        pronunciationDictionary.put("database", "ˈdeɪtəˌbeɪs");
        pronunciationDictionary.put("software", "ˈsɒftwɛər");
        pronunciationDictionary.put("hardware", "ˈhɑːrdwɛər");
        pronunciationDictionary.put("artificial", "ˌɑːrtɪˈfɪʃəl");
        pronunciationDictionary.put("intelligence", "ɪnˈtɛlɪdʒəns");
        pronunciationDictionary.put("algorithm", "ˈælɡəˌrɪðəm");
        pronunciationDictionary.put("machine", "məˈʃiːn");
        pronunciationDictionary.put("learning", "ˈlɜrnɪŋ");
        pronunciationDictionary.put("automation", "ˌɔːtəˈmeɪʃən");
        pronunciationDictionary.put("recognition", "ˌrɛkəɡˈnɪʃən");
        pronunciationDictionary.put("pronunciation", "prəˌnʌnsiˈeɪʃən");
        pronunciationDictionary.put("speech", "spiːʧ");
        pronunciationDictionary.put("audio", "ˈɔːdiˌoʊ");
        pronunciationDictionary.put("processing", "ˈprɒsɛsɪŋ");
        pronunciationDictionary.put("correct", "kəˈrɛkt");
        pronunciationDictionary.put("accent", "ˈæksɛnt");
        pronunciationDictionary.put("dictionary", "ˈdɪkʃəˌnɛri");
        pronunciationDictionary.put("phonetics", "fəˈnɛtɪks");
        pronunciationDictionary.put("vocabulary", "voʊˈkæbjəˌlɛri");
        pronunciationDictionary.put("education", "ˌɛdʒʊˈkeɪʃən");
        pronunciationDictionary.put("communication", "kəˌmjunɪˈkeɪʃən");
        pronunciationDictionary.put("accuracy", "ˈækjʊrəsi");
        pronunciationDictionary.put("evaluation", "ɪˌvæljuˈeɪʃən");
        pronunciationDictionary.put("feedback", "ˈfidˌbæk");

    }
}
