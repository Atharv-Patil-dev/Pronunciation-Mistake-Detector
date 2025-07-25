package com.example.pronounciationdetector;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import androidx.appcompat.app.AppCompatActivity;

public class HistoryActivity extends AppCompatActivity {
    private ListView historyListView;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        historyListView = findViewById(R.id.history_list_view);
        databaseHelper = new DatabaseHelper(this);
        displayHistory();
    }

    private void displayHistory() {
        Cursor cursor = databaseHelper.getHistory();
        String[] fromColumns = {"spoken_text", "correction", "ipa", "confidence"};
        int[] toViews = {R.id.text_spoken, R.id.text_correction, R.id.text_ipa, R.id.text_confidence};

        SimpleCursorAdapter adapter = new SimpleCursorAdapter(
                this,
                R.layout.history_item,
                cursor,
                fromColumns,
                toViews,
                0
        );
        historyListView.setAdapter(adapter);
    }
}