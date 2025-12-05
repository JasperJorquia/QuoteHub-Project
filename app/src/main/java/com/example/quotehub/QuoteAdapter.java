package com.example.quotehub;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class QuoteAdapter extends RecyclerView.Adapter<QuoteAdapter.QuoteViewHolder> {

    private Context context;
    private List<Quote> quotes;
    private OnQuoteInteractionListener interactionListener;
    private OnQuoteActionListener actionListener;

    public interface OnQuoteInteractionListener {
        void onLikeToggle(Quote quote, int position);
    }

    public interface OnQuoteActionListener {
        void onLikeClick(Quote quote, int position);
        void onDeleteClick(Quote quote, int position);
    }

    public QuoteAdapter(OnQuoteInteractionListener listener) {
        this.quotes = new ArrayList<>();
        this.interactionListener = listener;
    }

    public QuoteAdapter(Context context, List<Quote> quotes, OnQuoteActionListener actionListener) {
        this.context = context;
        this.quotes = quotes != null ? quotes : new ArrayList<>();
        this.actionListener = actionListener;
    }

    public QuoteAdapter(Context context) {
        this.context = context;
        this.quotes = new ArrayList<>();
    }

    @NonNull
    @Override
    public QuoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_quote, parent, false);
        if (context == null) {
            context = parent.getContext();
        }
        return new QuoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QuoteViewHolder holder, int position) {
        Quote quote = quotes.get(position);
        holder.bind(quote, position);
    }

    @Override
    public int getItemCount() {
        return quotes.size();
    }

    public void setQuotes(List<Quote> quotes) {
        this.quotes = quotes != null ? quotes : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void addQuote(Quote quote) {
        quotes.add(quote);
        notifyItemInserted(quotes.size() - 1);
    }

    public void removeQuote(int position) {
        if (position >= 0 && position < quotes.size()) {
            quotes.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, quotes.size());
        }
    }

    public void updateQuote(int position, Quote quote) {
        if (position >= 0 && position < quotes.size()) {
            quotes.set(position, quote);
            notifyItemChanged(position);
        }
    }

    class QuoteViewHolder extends RecyclerView.ViewHolder {
        TextView quoteText;
        TextView quoteAuthor;
        TextView categoryChip;
        ImageButton likeButton;
        ImageButton copyButton;

        public QuoteViewHolder(@NonNull View itemView) {
            super(itemView);
            quoteText = itemView.findViewById(R.id.quoteText);
            quoteAuthor = itemView.findViewById(R.id.quoteAuthor);
            categoryChip = itemView.findViewById(R.id.categoryChip);
            likeButton = itemView.findViewById(R.id.likeButton);
            copyButton = itemView.findViewById(R.id.copyButton);
        }

        public void bind(Quote quote, int position) {
            quoteText.setText(quote.getText());
            quoteAuthor.setText("- " + quote.getAuthor());
            categoryChip.setText(quote.getCategory());

            updateLikeButton(quote.isLiked());

            likeButton.setOnClickListener(v -> {
                if (interactionListener != null) {
                    interactionListener.onLikeToggle(quote, position);
                } else if (actionListener != null) {
                    actionListener.onLikeClick(quote, position);
                }
            });

            copyButton.setOnClickListener(v -> {
                copyQuoteToClipboard(quote);
            });

            itemView.setOnLongClickListener(v -> {
                copyQuoteToClipboard(quote);
                return true;
            });

            categoryChip.setOnClickListener(v -> {
                if (context != null) {
                    android.content.Intent intent = new android.content.Intent(
                            context, CategoryDetailActivity.class);
                    intent.putExtra("category", quote.getCategory());
                    context.startActivity(intent);
                }
            });
        }

        private void updateLikeButton(boolean isLiked) {
            if (isLiked) {
                likeButton.setImageResource(R.drawable.ic_heart_filled);
            } else {
                likeButton.setImageResource(R.drawable.ic_heart_outline);
            }
        }

        private void copyQuoteToClipboard(Quote quote) {
            if (context != null) {
                ClipboardManager clipboard = (ClipboardManager)
                        context.getSystemService(Context.CLIPBOARD_SERVICE);
                String quoteTextToCopy = quote.getText() + " - " + quote.getAuthor();
                ClipData clip = ClipData.newPlainText("quote", quoteTextToCopy);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(context, " Quote copied to clipboard", Toast.LENGTH_SHORT).show();
            }
        }
    }
}