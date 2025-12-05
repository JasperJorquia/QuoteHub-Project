package com.example.quotehub;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CategoryDetailActivity extends AppCompatActivity implements QuoteAdapter.OnQuoteInteractionListener {

    private TextView categoryTitle;
    private TextView quoteCount;
    private ImageView backButton;
    private RecyclerView categoryQuotesRecyclerView;
    private ProgressBar loadingProgress;
    private TextView emptyState;
    private QuoteAdapter quoteAdapter;
    private List<Quote> categoryQuotesList;
    private Set<String> likedQuoteIds;
    private DatabaseReference databaseReference;
    private FirebaseAuth mAuth;
    private String category;
    private String userId;
    private boolean defaultQuotesLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_detail);

        category = getIntent().getStringExtra("category");

        if (category == null || category.isEmpty()) {
            Toast.makeText(this, "Invalid category", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeFirebase();
        initializeViews();
        setupRecyclerView();
        authenticateUser();
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();
    }

    private void initializeViews() {
        categoryTitle = findViewById(R.id.categoryTitle);
        quoteCount = findViewById(R.id.quoteCount);
        backButton = findViewById(R.id.backButton);
        categoryQuotesRecyclerView = findViewById(R.id.categoryQuotesRecyclerView);
        loadingProgress = findViewById(R.id.loadingProgress);
        emptyState = findViewById(R.id.emptyState);

        categoryTitle.setText(category);
        categoryQuotesList = new ArrayList<>();
        likedQuoteIds = new HashSet<>();

        backButton.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        categoryQuotesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        quoteAdapter = new QuoteAdapter((QuoteAdapter.OnQuoteInteractionListener) this);
        categoryQuotesRecyclerView.setAdapter(quoteAdapter);
    }

    private void authenticateUser() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getUid();
            loadLikedQuotes();
            loadCategoryQuotes();
        } else {
            signInAnonymously();
        }
    }

    private void signInAnonymously() {
        mAuth.signInAnonymously()
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            userId = user.getUid();
                            loadLikedQuotes();
                            loadCategoryQuotes();
                        }
                    } else {
                        Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show();
                        hideLoading();
                    }
                });
    }

    private void loadLikedQuotes() {
        if (userId == null) return;

        DatabaseReference likesRef = databaseReference.child("users").child(userId).child("likedQuotes");
        likesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                likedQuoteIds.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Quote quote = snapshot.getValue(Quote.class);
                    if (quote != null) {
                        likedQuoteIds.add(quote.getId());
                    }
                }
                updateQuotesLikedStatus();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(CategoryDetailActivity.this, "Failed to load liked quotes",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadCategoryQuotes() {
        showLoading();

        databaseReference.child("quotes")
                .orderByChild("category")
                .equalTo(category)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        categoryQuotesList.clear();

                        if (!snapshot.exists() && !defaultQuotesLoaded) {
                            loadDefaultQuotes();
                            return;
                        }

                        for (DataSnapshot quoteSnapshot : snapshot.getChildren()) {
                            Quote quote = quoteSnapshot.getValue(Quote.class);
                            if (quote != null) {
                                categoryQuotesList.add(quote);
                            }
                        }

                        updateQuotesLikedStatus();
                        updateUI();
                        hideLoading();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(CategoryDetailActivity.this,
                                "Failed to load quotes: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        hideLoading();
                    }
                });
    }

    private void loadDefaultQuotes() {
        defaultQuotesLoaded = true;
        List<Quote> defaultQuotes = getDefaultQuotesForCategory(category);

        if (defaultQuotes.isEmpty()) {
            hideLoading();
            updateUI();
            return;
        }

        int totalQuotes = defaultQuotes.size();
        final int[] addedCount = {0};

        for (Quote quote : defaultQuotes) {
            String quoteId = databaseReference.child("quotes").push().getKey();
            if (quoteId != null) {
                quote.setId(quoteId);
                quote.setCategory(category);
                quote.setTimestamp(System.currentTimeMillis());
                quote.setUserId(userId);  // FIXED: Add userId to default quotes

                databaseReference.child("quotes").child(quoteId).setValue(quote)
                        .addOnSuccessListener(aVoid -> {
                            addedCount[0]++;
                            if (addedCount[0] == totalQuotes) {
                                loadCategoryQuotes();
                            }
                        })
                        .addOnFailureListener(e -> {
                            addedCount[0]++;
                            if (addedCount[0] == totalQuotes) {
                                loadCategoryQuotes();
                            }
                        });
            }
        }
    }

    private void updateQuotesLikedStatus() {
        for (Quote quote : categoryQuotesList) {
            quote.setLiked(likedQuoteIds.contains(quote.getId()));
        }
        quoteAdapter.setQuotes(categoryQuotesList);
    }

    private void updateUI() {
        if (categoryQuotesList.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            categoryQuotesRecyclerView.setVisibility(View.GONE);
            quoteCount.setText("0 quotes");
        } else {
            emptyState.setVisibility(View.GONE);
            categoryQuotesRecyclerView.setVisibility(View.VISIBLE);
            quoteCount.setText(categoryQuotesList.size() + " quotes");
        }
    }

    private void showLoading() {
        loadingProgress.setVisibility(View.VISIBLE);
        categoryQuotesRecyclerView.setVisibility(View.GONE);
        emptyState.setVisibility(View.GONE);
    }

    private void hideLoading() {
        loadingProgress.setVisibility(View.GONE);
    }

    @Override
    public void onLikeToggle(Quote quote, int position) {
        if (userId == null) {
            Toast.makeText(this, "Please sign in first", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference quoteRef = databaseReference.child("users")
                .child(userId)
                .child("likedQuotes")
                .child(quote.getId());

        if (quote.isLiked()) {
            quoteRef.removeValue()
                    .addOnSuccessListener(aVoid -> {
                        quote.setLiked(false);
                        likedQuoteIds.remove(quote.getId());
                        quoteAdapter.notifyItemChanged(position);
                        Toast.makeText(this, "Removed from favorites", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to remove quote", Toast.LENGTH_SHORT).show();
                    });
        } else {
            quote.setLiked(true);
            quote.setTimestamp(System.currentTimeMillis());

            quoteRef.setValue(quote)
                    .addOnSuccessListener(aVoid -> {
                        likedQuoteIds.add(quote.getId());
                        quoteAdapter.notifyItemChanged(position);
                        Toast.makeText(this, "Added to favorites", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        quote.setLiked(false);
                        quoteAdapter.notifyItemChanged(position);
                        Toast.makeText(this, "Failed to add quote", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private List<Quote> getDefaultQuotesForCategory(String category) {
        List<Quote> quotes = new ArrayList<>();

        switch (category) {
            case "Wisdom":
                quotes.add(new Quote("", "Honesty is the first chapter in the book of wisdom.", "Thomas Jefferson", "Wisdom", false, 0, userId));
                quotes.add(new Quote("", "The art of being wise is the art of knowing what to overlook.", "William James", "Wisdom", false, 0, userId));
                quotes.add(new Quote("", "Look for the answer inside your question.", "Rumi", "Wisdom", false, 0, userId));
                quotes.add(new Quote("", "The years teach much which the days never know.", "Ralph Waldo Emerson", "Wisdom", false, 0, userId));
                quotes.add(new Quote("", "The only true wisdom is in knowing you know nothing.", "Socrates", "Wisdom", false, 0, userId));
                break;

            case "Art":
                quotes.add(new Quote("", "Reason is powerless in the expression of Love.", "Rumi", "Art", false, 0, userId));
                quotes.add(new Quote("", "The secret of life is in art.", "Oscar Wilde", "Art", false, 0, userId));
                quotes.add(new Quote("", "To create one's world in any of the arts takes courage.", "Georgia O'Keeffe", "Art", false, 0, userId));
                quotes.add(new Quote("", "Art is not what you see, but what you make others see.", "Edgar Degas", "Art", false, 0, userId));
                quotes.add(new Quote("", "Every artist was first an amateur.", "Ralph Waldo Emerson", "Art", false, 0, userId));
                break;

            case "Success":
                quotes.add(new Quote("", "When it looks impossible and you are ready to quit, victory is near.", "Tony Robbins", "Success", false, 0, userId));
                quotes.add(new Quote("", "Your time is limited, so don't waste it living someone else's life.", "Steve Jobs", "Success", false, 0, userId));
                quotes.add(new Quote("", "If you can dream it, you can do it.", "Walt Disney", "Success", false, 0, userId));
                quotes.add(new Quote("", "There is no elevator to success, you have to take the stairs.", "Zig Ziglar", "Success", false, 0, userId));
                quotes.add(new Quote("", "Success is not final, failure is not fatal: it is the courage to continue that counts.", "Winston Churchill", "Success", false, 0, userId));
                break;

            case "Friendship":
                quotes.add(new Quote("", "Friendship needs no words.", "Dag Hammarskjold", "Friendship", false, 0, userId));
                quotes.add(new Quote("", "We do not remember days, we remember moments.", "Cesare Pavese", "Friendship", false, 0, userId));
                quotes.add(new Quote("", "No friendship is an accident.", "O. Henry", "Friendship", false, 0, userId));
                quotes.add(new Quote("", "Once you pledge, don't hedge.", "Nikita Khrushchev", "Friendship", false, 0, userId));
                quotes.add(new Quote("", "A friend is someone who knows all about you and still loves you.", "Elbert Hubbard", "Friendship", false, 0, userId));
                break;

            case "Positive":
                quotes.add(new Quote("", "Liberty means responsibility. That is why most people dread it.", "George Bernard Shaw", "Positive", false, 0, userId));
                quotes.add(new Quote("", "Death is not the greatest loss in life. The greatest loss is what dies inside us while we live.", "Norman Cousins", "Positive", false, 0, userId));
                quotes.add(new Quote("", "Life's most persistent and urgent question is, 'What are you doing for others?'", "Martin Luther King, Jr.", "Positive", false, 0, userId));
                quotes.add(new Quote("", "You cannot do kindness too soon, for you never know how soon it will be too late.", "Ralph Waldo Emerson", "Positive", false, 0, userId));
                quotes.add(new Quote("", "Keep your face always toward the sunshine and shadows will fall behind you.", "Walt Whitman", "Positive", false, 0, userId));
                break;

            case "Life":
                quotes.add(new Quote("", "Life is divided into the horrible and the miserable.", "Woody Allen", "Life", false, 0, userId));
                quotes.add(new Quote("", "A stumble may prevent a fall.", "Thomas Fuller", "Life", false, 0, userId));
                quotes.add(new Quote("", "Nothing is an obstacle unless you say it is.", "Wally Amos", "Life", false, 0, userId));
                quotes.add(new Quote("", "We are what we repeatedly do. Excellence, then, is not an act, but a habit.", "Aristotle", "Life", false, 0, userId));
                quotes.add(new Quote("", "Life is what happens when you're busy making other plans.", "John Lennon", "Life", false, 0, userId));
                break;

            case "Motivation":
                quotes.add(new Quote("", "The only way to do great work is to love what you do.", "Steve Jobs", "Motivation", false, 0, userId));
                quotes.add(new Quote("", "Believe you can and you're halfway there.", "Theodore Roosevelt", "Motivation", false, 0, userId));
                quotes.add(new Quote("", "Don't watch the clock; do what it does. Keep going.", "Sam Levenson", "Motivation", false, 0, userId));
                quotes.add(new Quote("", "The future belongs to those who believe in the beauty of their dreams.", "Eleanor Roosevelt", "Motivation", false, 0, userId));
                quotes.add(new Quote("", "It does not matter how slowly you go as long as you do not stop.", "Confucius", "Motivation", false, 0, userId));
                break;

            case "Love":
                quotes.add(new Quote("", "Love is composed of a single soul inhabiting two bodies.", "Aristotle", "Love", false, 0, userId));
                quotes.add(new Quote("", "The best thing to hold onto in life is each other.", "Audrey Hepburn", "Love", false, 0, userId));
                quotes.add(new Quote("", "Love recognizes no barriers.", "Maya Angelou", "Love", false, 0, userId));
                quotes.add(new Quote("", "Where there is love there is life.", "Mahatma Gandhi", "Love", false, 0, userId));
                quotes.add(new Quote("", "Love is not only something you feel, it is something you do.", "David Wilkerson", "Love", false, 0, userId));
                break;
        }

        return quotes;
    }
}