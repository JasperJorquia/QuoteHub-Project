package com.example.quotehub;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LikedFragment extends Fragment {

    private RecyclerView likedQuotesRecyclerView;
    private TextView emptyView;
    private CardView totalLikedCard;
    private CardView categoriesCard;
    private TextView totalLikedText;
    private TextView totalCategoriesText;
    private QuoteAdapter quoteAdapter;
    private List<Quote> likedQuoteList;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private String userId;
    private Map<String, Integer> categoryCount;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_liked, container, false);

        initializeFirebase();
        initializeViews(view);
        setupRecyclerView();
        setupCardClickListeners();
        loadLikedQuotes();

        return view;
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getUid();
        }
    }

    private void initializeViews(View view) {
        likedQuotesRecyclerView = view.findViewById(R.id.likedQuotesRecyclerView);
        emptyView = view.findViewById(R.id.emptyView);
        totalLikedCard = view.findViewById(R.id.totalLikedCard);
        categoriesCard = view.findViewById(R.id.categoriesCard);
        totalLikedText = view.findViewById(R.id.totalLikedCount);
        totalCategoriesText = view.findViewById(R.id.totalCategoriesCount);
        likedQuoteList = new ArrayList<>();
        categoryCount = new HashMap<>();
    }

    private void setupRecyclerView() {
        likedQuotesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        quoteAdapter = new QuoteAdapter(getContext(), likedQuoteList, new QuoteAdapter.OnQuoteActionListener() {
            @Override
            public void onLikeClick(Quote quote, int position) {
                unlikeQuote(quote, position);
            }

            @Override
            public void onDeleteClick(Quote quote, int position) {
                deleteQuote(quote, position);
            }
        });
        likedQuotesRecyclerView.setAdapter(quoteAdapter);
    }

    private void setupCardClickListeners() {
        totalLikedCard.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Total Liked: " + likedQuoteList.size(), Toast.LENGTH_SHORT).show();
        });

        categoriesCard.setOnClickListener(v -> {
            showCategoriesDialog();
        });
    }

    private void showCategoriesDialog() {
        if (categoryCount.isEmpty()) {
            Toast.makeText(getContext(), "No categories with quotes yet", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder message = new StringBuilder("Categories with Quotes:\n\n");
        for (Map.Entry<String, Integer> entry : categoryCount.entrySet()) {
            message.append(entry.getKey()).append(": ").append(entry.getValue()).append(" quote");
            if (entry.getValue() > 1) {
                message.append("s");
            }
            message.append("\n");
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Total Categories: " + categoryCount.size())
                .setMessage(message.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    private void loadLikedQuotes() {
        if (userId == null) {
            showEmptyView();
            return;
        }

        DatabaseReference likesRef = mDatabase.child("users").child(userId).child("likedQuotes");
        likesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                likedQuoteList.clear();
                categoryCount.clear();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Quote quote = snapshot.getValue(Quote.class);
                    if (quote != null) {
                        quote.setLiked(true);
                        likedQuoteList.add(quote);

                        String category = quote.getCategory();
                        if (category != null) {
                            categoryCount.put(category, categoryCount.getOrDefault(category, 0) + 1);
                        }
                    }
                }

                updateCardTexts();

                if (likedQuoteList.isEmpty()) {
                    showEmptyView();
                } else {
                    hideEmptyView();
                }

                quoteAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getContext(), "Failed to load liked quotes", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateCardTexts() {
        if (totalLikedText != null) {
            totalLikedText.setText(String.valueOf(likedQuoteList.size()));
        }
        if (totalCategoriesText != null) {
            totalCategoriesText.setText(String.valueOf(categoryCount.size()));
        }
    }

    private void unlikeQuote(Quote quote, int position) {
        if (userId == null) return;

        DatabaseReference quoteRef = mDatabase.child("users")
                .child(userId)
                .child("likedQuotes")
                .child(quote.getId());

        quoteRef.removeValue()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Quote removed from favorites", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to remove quote", Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteQuote(Quote quote, int position) {
        unlikeQuote(quote, position);
    }

    private void showEmptyView() {
        emptyView.setVisibility(View.VISIBLE);
        likedQuotesRecyclerView.setVisibility(View.GONE);
    }

    private void hideEmptyView() {
        emptyView.setVisibility(View.GONE);
        likedQuotesRecyclerView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (userId != null) {
            loadLikedQuotes();
        }
    }
}