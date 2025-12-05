package com.example.quotehub;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

public class CategoriesFragment extends Fragment {

    private CardView wisdomCategoryCard, artCategoryCard, successCategoryCard;
    private CardView friendshipCategoryCard, positiveCategoryCard, lifeCategoryCard;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_categories, container, false);

        initializeViews(view);
        setupCategoryCards();

        return view;
    }

    private void initializeViews(View view) {
        wisdomCategoryCard = view.findViewById(R.id.wisdomCategoryCard);
        artCategoryCard = view.findViewById(R.id.artCategoryCard);
        successCategoryCard = view.findViewById(R.id.successCategoryCard);
        friendshipCategoryCard = view.findViewById(R.id.friendshipCategoryCard);
        positiveCategoryCard = view.findViewById(R.id.positiveCategoryCard);
        lifeCategoryCard = view.findViewById(R.id.lifeCategoryCard);
    }

    private void setupCategoryCards() {
        wisdomCategoryCard.setOnClickListener(v -> openCategoryDetail("Wisdom"));
        artCategoryCard.setOnClickListener(v -> openCategoryDetail("Art"));
        successCategoryCard.setOnClickListener(v -> openCategoryDetail("Success"));
        friendshipCategoryCard.setOnClickListener(v -> openCategoryDetail("Friendship"));
        positiveCategoryCard.setOnClickListener(v -> openCategoryDetail("Positive"));
        lifeCategoryCard.setOnClickListener(v -> openCategoryDetail("Life"));
    }

    private void openCategoryDetail(String category) {
        Intent intent = new Intent(getActivity(), CategoryDetailActivity.class);
        intent.putExtra("category", category);
        startActivity(intent);
    }
}