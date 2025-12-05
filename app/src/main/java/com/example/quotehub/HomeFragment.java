package com.example.quotehub;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class HomeFragment extends Fragment {

    private CardView wisdomCard, artCard, successCard, friendshipCard;
    private TextView viewAllText;
    private TextView userQuoteText;
    private ImageView editQuoteButton, deleteQuoteButton;
    private LinearLayout quoteActionsLayout;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private String userId;
    private String currentQuoteId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        initializeFirebase();
        initializeViews(view);
        setupCategoryCards();
        setupQuoteActions();
        loadUserCustomQuote();

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
        wisdomCard = view.findViewById(R.id.wisdomCard);
        artCard = view.findViewById(R.id.artCard);
        successCard = view.findViewById(R.id.successCard);
        friendshipCard = view.findViewById(R.id.friendshipCard);
        viewAllText = view.findViewById(R.id.viewAllText);
        userQuoteText = view.findViewById(R.id.userQuoteText);
        editQuoteButton = view.findViewById(R.id.editQuoteButton);
        deleteQuoteButton = view.findViewById(R.id.deleteQuoteButton);
        quoteActionsLayout = view.findViewById(R.id.quoteActionsLayout);
    }

    private void setupCategoryCards() {
        wisdomCard.setOnClickListener(v -> navigateToCategoryDetail("Wisdom"));
        artCard.setOnClickListener(v -> navigateToCategoryDetail("Art"));
        successCard.setOnClickListener(v -> navigateToCategoryDetail("Success"));
        friendshipCard.setOnClickListener(v -> navigateToCategoryDetail("Friendship"));

        viewAllText.setOnClickListener(v -> {
            if (getActivity() != null) {
                ((MainActivity) getActivity()).getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragmentContainer, new CategoriesFragment())
                        .commit();
                ((MainActivity) getActivity()).findViewById(R.id.bottomNavigation)
                        .findViewById(R.id.nav_categories).performClick();
            }
        });
    }

    private void setupQuoteActions() {
        editQuoteButton.setOnClickListener(v -> showEditQuoteDialog());
        deleteQuoteButton.setOnClickListener(v -> showDeleteQuoteDialog());
    }

    private void navigateToCategoryDetail(String category) {
        Intent intent = new Intent(getActivity(), CategoryDetailActivity.class);
        intent.putExtra("category", category);
        startActivity(intent);
    }

    private void loadUserCustomQuote() {
        if (userId == null) {
            userQuoteText.setText("Login to create your own quote");
            quoteActionsLayout.setVisibility(View.GONE);
            return;
        }

        mDatabase.child("users").child(userId).child("customQuotes")
                .orderByChild("timestamp")
                .limitToLast(1)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists() && snapshot.getChildrenCount() > 0) {
                            for (DataSnapshot quoteSnapshot : snapshot.getChildren()) {
                                Quote quote = quoteSnapshot.getValue(Quote.class);
                                if (quote != null) {
                                    currentQuoteId = quote.getId();
                                    String displayText = "\"" + quote.getText() + "\" - " + quote.getAuthor();
                                    userQuoteText.setText(displayText);
                                    quoteActionsLayout.setVisibility(View.VISIBLE);
                                }
                            }
                        } else {
                            userQuoteText.setText("Create your own quote in the Create tab");
                            quoteActionsLayout.setVisibility(View.GONE);
                            currentQuoteId = null;
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        userQuoteText.setText("Failed to load custom quote");
                        quoteActionsLayout.setVisibility(View.GONE);
                    }
                });
    }

    private void showEditQuoteDialog() {
        if (currentQuoteId == null || userId == null) return;

        mDatabase.child("users").child(userId).child("customQuotes").child(currentQuoteId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Quote quote = snapshot.getValue(Quote.class);
                        if (quote != null) {
                            showEditDialog(quote);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(getContext(), "Failed to load quote", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showEditDialog(Quote quote) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_quote, null);

        EditText quoteEditText = dialogView.findViewById(R.id.editUpdateQuoteText);
        EditText authorEditText = dialogView.findViewById(R.id.editAuthorText);

        quoteEditText.setText(quote.getText());
        authorEditText.setText(quote.getAuthor());

        builder.setView(dialogView)
                .setTitle("Edit Quote")
                .setPositiveButton("Save", (dialog, which) -> {
                    String newText = quoteEditText.getText().toString().trim();
                    String newAuthor = authorEditText.getText().toString().trim();

                    if (!newText.isEmpty() && !newAuthor.isEmpty()) {
                        updateQuote(quote.getId(), newText, newAuthor, quote.getCategory());
                    } else {
                        Toast.makeText(getContext(), "Fields cannot be empty", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateQuote(String quoteId, String text, String author, String category) {
        Quote updatedQuote = new Quote(quoteId, text, author, category, false, System.currentTimeMillis());

        mDatabase.child("quotes").child(quoteId).setValue(updatedQuote);

        mDatabase.child("users").child(userId).child("customQuotes").child(quoteId)
                .setValue(updatedQuote)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Quote updated successfully", Toast.LENGTH_SHORT).show();
                    loadUserCustomQuote();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to update quote", Toast.LENGTH_SHORT).show();
                });
    }

    private void showDeleteQuoteDialog() {
        if (currentQuoteId == null) return;

        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Quote")
                .setMessage("Are you sure you want to delete this quote?")
                .setPositiveButton("Delete", (dialog, which) -> deleteQuote())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteQuote() {
        if (currentQuoteId == null || userId == null) return;

        mDatabase.child("quotes").child(currentQuoteId).removeValue();

        mDatabase.child("users").child(userId).child("customQuotes").child(currentQuoteId)
                .removeValue()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Quote deleted successfully", Toast.LENGTH_SHORT).show();
                    currentQuoteId = null;
                    loadUserCustomQuote();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to delete quote", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserCustomQuote();
    }
}