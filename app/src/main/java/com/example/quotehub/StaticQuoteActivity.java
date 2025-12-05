package com.example.quotehub;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.ArrayList;
import java.util.List;

public class StaticQuoteActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private String userId;
    private String category;
    private List<StaticQuote> quotes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        category = getIntent().getStringExtra("category");
        if (category == null) {
            finish();
            return;
        }

        int layoutId = getLayoutForCategory(category);
        setContentView(layoutId);

        initializeFirebase();
        initializeQuotes();
        setupBackButton();
        setupQuoteCards();
    }

    private int getLayoutForCategory(String category) {
        switch (category) {
            case "Wisdom": return R.layout.page_wisdom;
            case "Art": return R.layout.page_art;
            case "Success": return R.layout.page_success;
            case "Friendship": return R.layout.page_friendship;
            case "Positive": return R.layout.page_positive;
            case "Life": return R.layout.page_life;
            default: return R.layout.page_wisdom;
        }
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        if (mAuth.getCurrentUser() != null) {
            userId = mAuth.getCurrentUser().getUid();
        }
    }

    private void initializeQuotes() {
        quotes = new ArrayList<>();

        switch (category) {
            case "Wisdom":
                quotes.add(new StaticQuote("Honesty is the first chapter in the book of wisdom.", "Thomas Jefferson"));
                quotes.add(new StaticQuote("The art of being wise is the art of knowing what to overlook.", "William James"));
                quotes.add(new StaticQuote("Look for the answer inside your question.", "Rumi"));
                quotes.add(new StaticQuote("The years teach much which the days never know.", "Ralph Waldo Emerson"));
                break;
            case "Art":
                quotes.add(new StaticQuote("Reason is powerless in the expression of Love.", "Rumi"));
                quotes.add(new StaticQuote("The secret of life is in art.", "Oscar Wilde"));
                quotes.add(new StaticQuote("Look for the answer inside your question.", "Rumi"));
                quotes.add(new StaticQuote("To create one's world in any of the arts take courage.", "Georgia O'Keeffe"));
                break;
            case "Success":
                quotes.add(new StaticQuote("When it looks impossible and you are ready to quit, victory is near.", "Tony Robbins"));
                quotes.add(new StaticQuote("Your time is limited, so don't waste it living someone else's life.", "Steve Jobs"));
                quotes.add(new StaticQuote("If you can dream it, you can do it.", "Walt Disney"));
                quotes.add(new StaticQuote("There is no elevator to success, you have to take stairs.", "Zig Ziglar"));
                break;
            case "Friendship":
                quotes.add(new StaticQuote("Friendship needs no words.", "Dag Hammarskjold"));
                quotes.add(new StaticQuote("We do not remember days, we remember moments.", "Cesare Pavese"));
                quotes.add(new StaticQuote("No friendship is an accident.", "O. Henry"));
                quotes.add(new StaticQuote("Once you pledge, don't hedge.", "Nikita Khrushchev"));
                break;
            case "Positive":
                quotes.add(new StaticQuote("Liberty means responsibility. That is why most people dread it.", "George Bernard Shaw"));
                quotes.add(new StaticQuote("Death is not the greatest loss in life. The greatest loss is what dies inside us while we live.", "Norman Cousins"));
                quotes.add(new StaticQuote("Life's most persistent and urgent question is, 'What are you doing for others?'", "Martin Luther King, Jr."));
                quotes.add(new StaticQuote("You cannot do kindness too soon, for you never know how soon it will be too late.", "Ralph Waldo Emerson"));
                break;
            case "Life":
                quotes.add(new StaticQuote("Life is divided into the horrible and the miserable.", "Woody Allen"));
                quotes.add(new StaticQuote("A stumble may prevent a fall.", "Thomas Fuller"));
                quotes.add(new StaticQuote("Nothing is an obstacles unless you say it is.", "Wally Amos"));
                quotes.add(new StaticQuote("We are what we repeatedly do. Excellence, then, is not an act, but a habit.", "Aristotle"));
                break;
        }
    }

    private void setupBackButton() {
        ImageView backButton = findViewById(R.id.backButton);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }
    }

    private void setupQuoteCards() {
        ViewGroup rootView = findViewById(android.R.id.content);
        setupCardsRecursively(rootView, 0);
    }

    private void setupCardsRecursively(ViewGroup viewGroup, int quoteIndex) {
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View child = viewGroup.getChildAt(i);

            if (child instanceof ViewGroup) {
                ViewGroup childGroup = (ViewGroup) child;

                if (childGroup.getChildCount() > 0 &&
                        childGroup.getChildAt(0) instanceof LinearLayout) {

                    LinearLayout cardContent = (LinearLayout) childGroup.getChildAt(0);

                    if (cardContent.getChildCount() >= 3) {
                        View lastChild = cardContent.getChildAt(cardContent.getChildCount() - 1);

                        if (lastChild instanceof LinearLayout) {
                            LinearLayout iconLayout = (LinearLayout) lastChild;

                            if (iconLayout.getChildCount() >= 2) {
                                ImageView favoriteIcon = (ImageView) iconLayout.getChildAt(0);
                                ImageView copyIcon = (ImageView) iconLayout.getChildAt(1);

                                int finalQuoteIndex = quoteIndex;
                                favoriteIcon.setOnClickListener(v -> toggleFavorite(finalQuoteIndex, favoriteIcon));
                                copyIcon.setOnClickListener(v -> copyToClipboard(finalQuoteIndex));

                                quoteIndex++;
                            }
                        }
                    }
                }

                setupCardsRecursively(childGroup, quoteIndex);
            }
        }
    }

    private void toggleFavorite(int index, ImageView icon) {
        if (userId == null) {
            Toast.makeText(this, "Please sign in to like quotes", Toast.LENGTH_SHORT).show();
            return;
        }

        if (index >= quotes.size()) return;

        StaticQuote staticQuote = quotes.get(index);
        String quoteId = mDatabase.child("quotes").push().getKey();

        if (quoteId != null) {
            Quote quote = new Quote(quoteId, staticQuote.text, staticQuote.author, category, true, System.currentTimeMillis());

            DatabaseReference quoteRef = mDatabase.child("users")
                    .child(userId)
                    .child("likedQuotes")
                    .child(quoteId);

            quoteRef.setValue(quote)
                    .addOnSuccessListener(aVoid -> {
                        icon.setImageResource(R.drawable.ic_heart_filled);
                        Toast.makeText(this, "Added to favorites", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to add to favorites", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void copyToClipboard(int index) {
        if (index >= quotes.size()) return;

        StaticQuote quote = quotes.get(index);
        String text = quote.text + " - " + quote.author;

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("quote", text);
        clipboard.setPrimaryClip(clip);

        Toast.makeText(this, "Quote copied to clipboard", Toast.LENGTH_SHORT).show();
    }

    private static class StaticQuote {
        String text;
        String author;

        StaticQuote(String text, String author) {
            this.text = text;
            this.author = author;
        }
    }
}