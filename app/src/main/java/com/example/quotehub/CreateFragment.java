package com.example.quotehub;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class CreateFragment extends Fragment {

    private EditText quoteEditText;
    private EditText authorEditText;
    private Spinner categorySpinner;
    private TextView charCountText;
    private Button createButton;
    private Button deleteButton;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private String userId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create, container, false);

        initializeFirebase();
        initializeViews(view);
        setupCategorySpinner();
        setupTextWatcher();
        setupButtons();

        return view;
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        if (mAuth.getCurrentUser() != null) {
            userId = mAuth.getCurrentUser().getUid();
        }
    }

    private void initializeViews(View view) {
        quoteEditText = view.findViewById(R.id.quoteEditText);
        authorEditText = view.findViewById(R.id.authorEditText);
        categorySpinner = view.findViewById(R.id.categorySpinner);
        charCountText = view.findViewById(R.id.charCountText);
        createButton = view.findViewById(R.id.createButton);
        deleteButton = view.findViewById(R.id.deleteButton);
    }

    private void setupCategorySpinner() {
        String[] categories = {
                "Select a Category...",
                "Wisdom",
                "Art",
                "Success",
                "Friendship",
                "Positive",
                "Life",
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                categories
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);
    }

    private void setupTextWatcher() {
        quoteEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int length = s.length();
                charCountText.setText(length + "/280");
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void setupButtons() {
        createButton.setOnClickListener(v -> createQuote());
        deleteButton.setOnClickListener(v -> clearFields());
    }

    private void createQuote() {
        String quoteText = quoteEditText.getText().toString().trim();
        String author = authorEditText.getText().toString().trim();
        String category = categorySpinner.getSelectedItem().toString();

        if (quoteText.isEmpty()) {
            Toast.makeText(getContext(), "Please enter a quote", Toast.LENGTH_SHORT).show();
            return;
        }

        if (author.isEmpty()) {
            Toast.makeText(getContext(), "Please enter an author name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (category.equals("Select a Category...")) {
            Toast.makeText(getContext(), "Please select a category", Toast.LENGTH_SHORT).show();
            return;
        }

        if (userId == null) {
            Toast.makeText(getContext(), "Please sign in first", Toast.LENGTH_SHORT).show();
            return;
        }

        createButton.setEnabled(false);
        createButton.setText("Creating...");

        String quoteId = mDatabase.child("quotes").push().getKey();
        if (quoteId != null) {
            long timestamp = System.currentTimeMillis();

            Quote quote = new Quote(quoteId, quoteText, author, category, false, timestamp, userId);

            mDatabase.child("quotes").child(quoteId).setValue(quote)
                    .addOnSuccessListener(aVoid -> {
                        // Save to user's custom quotes
                        mDatabase.child("users").child(userId).child("customQuotes").child(quoteId)
                                .setValue(quote)
                                .addOnSuccessListener(aVoid2 -> {

                                    addActivityLog("Quote Created", "Created quote in " + category + " category", quoteId, timestamp);

                                    Toast.makeText(getContext(), "✓ Quote created successfully!", Toast.LENGTH_SHORT).show();
                                    clearFields();
                                    createButton.setEnabled(true);
                                    createButton.setText("Create");


                                    if (getActivity() != null) {
                                        ((MainActivity) getActivity()).getSupportFragmentManager()
                                                .beginTransaction()
                                                .replace(R.id.fragmentContainer, new HomeFragment())
                                                .commit();
                                        ((MainActivity) getActivity()).findViewById(R.id.bottomNavigation)
                                                .findViewById(R.id.nav_home).performClick();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(getContext(), "Failed to save to your quotes", Toast.LENGTH_SHORT).show();
                                    createButton.setEnabled(true);
                                    createButton.setText("Create");
                                });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Failed to create quote: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        createButton.setEnabled(true);
                        createButton.setText("Create");
                    });
        }
    }

    private void addActivityLog(String action, String description, String quoteId, long timestamp) {
        if (userId == null) return;

        String activityId = mDatabase.child("users").child(userId).child("activityLog").push().getKey();
        if (activityId != null) {
            ActivityLog log = new ActivityLog(activityId, action, description, timestamp, quoteId);
            mDatabase.child("users").child(userId).child("activityLog").child(activityId)
                    .setValue(log);
        }
    }

    private void clearFields() {
        quoteEditText.setText("");
        authorEditText.setText("");
        categorySpinner.setSelection(0);
        charCountText.setText("0/280");
        Toast.makeText(getContext(), "✓ Fields cleared", Toast.LENGTH_SHORT).show();
    }
}