package com.example.quotehub;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private String userId;
    private int currentFragmentId = R.id.nav_home;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeFirebase();
        checkUserAuthentication();
        initializeViews();
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    private void checkUserAuthentication() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getUid();
            initializeUserData();
        } else {
            navigateToLogin();
        }
    }

    private void navigateToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void initializeUserData() {
        DatabaseReference userRef = mDatabase.child("users").child(userId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    createUserProfile();
                } else {
                    updateLastLogin();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(MainActivity.this, "Failed to load user data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createUserProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String email = currentUser.getEmail();
            User user = new User(userId, email != null ? email : "");

            mDatabase.child("users").child(userId).setValue(user)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(MainActivity.this, "Welcome to QuoteHub!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(MainActivity.this, "Failed to create profile", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void updateLastLogin() {
        mDatabase.child("users").child(userId).child("lastLogin")
                .setValue(System.currentTimeMillis());
    }

    private void initializeViews() {
        bottomNavigationView = findViewById(R.id.bottomNavigation);
        setupBottomNavigation();
        loadFragment(new HomeFragment());
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (currentFragmentId == itemId) {
                return true;
            }

            Fragment fragment = null;

            if (itemId == R.id.nav_home) {
                fragment = new HomeFragment();
            } else if (itemId == R.id.nav_categories) {
                fragment = new CategoriesFragment();
            } else if (itemId == R.id.nav_create) {
                fragment = new CreateFragment();
            } else if (itemId == R.id.nav_liked) {
                fragment = new LikedFragment();
            } else if (itemId == R.id.nav_profile) {
                fragment = new ProfileFragment();
            }

            if (fragment != null) {
                currentFragmentId = itemId;
                return loadFragment(fragment);
            }

            return false;
        });
    }

    private boolean loadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .commit();
            return true;
        }
        return false;
    }

    public String getUserId() {
        return userId;
    }

    public DatabaseReference getDatabaseReference() {
        return mDatabase;
    }

    public FirebaseAuth getAuth() {
        return mAuth;
    }

    public void likeQuote(Quote quote) {
        if (userId == null) {
            Toast.makeText(this, "Please sign in first", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference quoteRef = mDatabase.child("users")
                .child(userId)
                .child("likedQuotes")
                .child(quote.getId());

        quote.setLiked(true);
        quote.setTimestamp(System.currentTimeMillis());

        quoteRef.setValue(quote)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(MainActivity.this, "Quote added to favorites", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Failed to add quote", Toast.LENGTH_SHORT).show();
                });
    }

    public void unlikeQuote(Quote quote) {
        if (userId == null) return;

        DatabaseReference quoteRef = mDatabase.child("users")
                .child(userId)
                .child("likedQuotes")
                .child(quote.getId());

        quoteRef.removeValue()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(MainActivity.this, "Quote removed from favorites", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Failed to remove quote", Toast.LENGTH_SHORT).show();
                });
    }

    public void addQuoteToDatabase(String text, String author, String category) {
        if (userId == null) {
            Toast.makeText(this, "Please sign in first", Toast.LENGTH_SHORT).show();
            return;
        }

        String quoteId = mDatabase.child("quotes").push().getKey();
        if (quoteId != null) {
            // FIXED: Include userId when creating quote
            Quote quote = new Quote(quoteId, text, author, category, false, System.currentTimeMillis(), userId);

            mDatabase.child("quotes")
                    .child(quoteId)
                    .setValue(quote)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(MainActivity.this, "Quote created successfully", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(MainActivity.this, "Failed to create quote", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    public void loadQuotesByCategory(String category, QuoteLoadCallback callback) {
        DatabaseReference quotesRef = mDatabase.child("quotes");
        quotesRef.orderByChild("category").equalTo(category)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        callback.onQuotesLoaded(dataSnapshot);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        callback.onError(databaseError.getMessage());
                    }
                });
    }

    public void loadAllQuotes(QuoteLoadCallback callback) {
        DatabaseReference quotesRef = mDatabase.child("quotes");
        quotesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                callback.onQuotesLoaded(dataSnapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                callback.onError(databaseError.getMessage());
            }
        });
    }

    public void checkIfQuoteLiked(String quoteId, QuoteLikeCheckCallback callback) {
        if (userId == null) {
            callback.onResult(false);
            return;
        }

        mDatabase.child("users")
                .child(userId)
                .child("likedQuotes")
                .child(quoteId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        callback.onResult(dataSnapshot.exists());
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        callback.onResult(false);
                    }
                });
    }

    public interface QuoteLoadCallback {
        void onQuotesLoaded(DataSnapshot dataSnapshot);
        void onError(String error);
    }

    public interface QuoteLikeCheckCallback {
        void onResult(boolean isLiked);
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            navigateToLogin();
        }
    }

    @Override
    public void onBackPressed() {
        if (currentFragmentId != R.id.nav_home) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        } else {
            super.onBackPressed();
        }
    }
}