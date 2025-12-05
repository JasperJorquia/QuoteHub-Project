package com.example.quotehub;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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
import java.util.Collections;
import java.util.List;

public class ProfileFragment extends Fragment {

    private TextView profileAvatar;
    private TextView profileName;
    private TextView tvProfileEmail;
    private RecyclerView activityRecyclerView;
    private Button logoutButton;
    private Button clearAllQuotesButton;
    private ActivityAdapter activityAdapter;
    private List<ActivityItem> activityList;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private String userId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        initializeFirebase();
        initializeViews(view);
        setupRecyclerView();
        loadUserProfile();
        loadUserActivity();
        setupButtons();

        return view;
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    private void initializeViews(View view) {
        profileAvatar = view.findViewById(R.id.profileAvatar);
        profileName = view.findViewById(R.id.profileName);
        tvProfileEmail = view.findViewById(R.id.tvProfileEmail);
        activityRecyclerView = view.findViewById(R.id.activityRecyclerView);
        logoutButton = view.findViewById(R.id.logoutButton);
        clearAllQuotesButton = view.findViewById(R.id.clearAllQuotesButton);
        activityList = new ArrayList<>();
    }

    private void setupRecyclerView() {
        activityRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        activityAdapter = new ActivityAdapter(activityList);
        activityRecyclerView.setAdapter(activityAdapter);
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getUid();
            String email = currentUser.getEmail();

            if (email != null && !email.isEmpty()) {
                String avatarLetter = getAvatarLetter(email);
                profileAvatar.setText(avatarLetter);
                tvProfileEmail.setText(email);

                String displayName = email.split("@")[0];
                profileName.setText(displayName.toUpperCase());
            } else {
                profileAvatar.setText("A");
                tvProfileEmail.setText("Anonymous User");
                profileName.setText("ANONYMOUS");
            }
        }
    }

    private String getAvatarLetter(String email) {
        if (email != null && !email.isEmpty()) {
            return String.valueOf(email.charAt(0)).toUpperCase();
        }
        return "A";
    }

    private void loadUserActivity() {
        if (userId == null) return;

        activityList.clear();

        DatabaseReference likesRef = mDatabase.child("users").child(userId).child("likedQuotes");
        likesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int totalLiked = (int) dataSnapshot.getChildrenCount();

                activityList.clear();
                activityList.add(new ActivityItem("Total Quotes Liked", String.valueOf(totalLiked), "❤️"));

                long todayStart = getTodayStartTimestamp();
                int todayCount = 0;

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Long timestamp = snapshot.child("timestamp").getValue(Long.class);
                    if (timestamp != null && timestamp >= todayStart) {
                        todayCount++;
                    }
                }

                activityList.add(new ActivityItem("Quotes Liked Today", String.valueOf(todayCount), "📅"));
                activityList.add(new ActivityItem("Member Since", formatDate(getMemberSinceDate()), "🗓️"));

                loadActivityLogs();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getContext(), "Failed to load activity", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadActivityLogs() {
        if (userId == null) return;

        DatabaseReference activityRef = mDatabase.child("users").child(userId).child("activityLog");
        activityRef.orderByChild("timestamp").limitToLast(5).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<ActivityItem> logItems = new ArrayList<>();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    ActivityLog log = snapshot.getValue(ActivityLog.class);
                    if (log != null) {
                        String timeAgo = getTimeAgo(log.getTimestamp());
                        logItems.add(new ActivityItem(log.getAction(), log.getDescription() + " - " + timeAgo, "✨"));
                    }
                }

                Collections.reverse(logItems);
                activityList.addAll(logItems);
                activityAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getContext(), "Failed to load activity logs", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getTimeAgo(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + " day" + (days > 1 ? "s" : "") + " ago";
        } else if (hours > 0) {
            return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        } else if (minutes > 0) {
            return minutes + " minute" + (minutes > 1 ? "s" : "") + " ago";
        } else {
            return "Just now";
        }
    }

    private long getTodayStartTimestamp() {
        long now = System.currentTimeMillis();
        return now - (now % (24 * 60 * 60 * 1000));
    }

    private long getMemberSinceDate() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && currentUser.getMetadata() != null) {
            return currentUser.getMetadata().getCreationTimestamp();
        }
        return System.currentTimeMillis();
    }

    private String formatDate(long timestamp) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date(timestamp));
    }

    private void setupButtons() {
        logoutButton.setOnClickListener(v -> showLogoutDialog());
        clearAllQuotesButton.setOnClickListener(v -> showClearAllDialog());
    }

    private void showClearAllDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Clear All Custom Quotes")
                .setMessage("Are you sure you want to delete all your custom quotes? This action cannot be undone.")
                .setPositiveButton("Clear All", (dialog, which) -> clearAllCustomQuotes())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void clearAllCustomQuotes() {
        if (userId == null) return;

        DatabaseReference customQuotesRef = mDatabase.child("users").child(userId).child("customQuotes");
        customQuotesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                final int[] count = {0};
                for (DataSnapshot quoteSnapshot : dataSnapshot.getChildren()) {
                    String quoteId = quoteSnapshot.getKey();
                    if (quoteId != null) {
                        mDatabase.child("quotes").child(quoteId).removeValue();
                        count[0]++;
                    }
                }

                customQuotesRef.removeValue()
                        .addOnSuccessListener(aVoid -> {
                            mDatabase.child("users").child(userId).child("activityLog").removeValue();
                            Toast.makeText(getContext(), " Cleared " + count[0] + " custom quotes", Toast.LENGTH_SHORT).show();
                            loadUserActivity();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "Failed to clear quotes", Toast.LENGTH_SHORT).show();
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getContext(), "Failed to load quotes", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> performLogout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performLogout() {
        mAuth.signOut();

        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        if (getActivity() != null) {
            getActivity().finish();
        }

        Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (userId != null) {
            loadUserProfile();
            loadUserActivity();
        }
    }
}