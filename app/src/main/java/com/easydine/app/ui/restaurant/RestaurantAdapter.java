package com.easydine.app.ui.restaurant;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.easydine.app.R;
import com.easydine.app.data.model.Restaurant;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.PhotoMetadata;
import com.google.android.libraries.places.api.net.FetchPhotoRequest;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class RestaurantAdapter extends RecyclerView.Adapter<RestaurantAdapter.VH> {

    public interface OnRestaurantClickListener {
        void onRestaurantClick(Restaurant r);
    }

    private final Context appContext;
    private final List<Restaurant> items;
    private final OnRestaurantClickListener onClickListener;

    // Favorites state (restaurantIds)
    private final Set<String> favoriteIds = new HashSet<>();

    // Places photos
    private final PlacesClient placesClient;
    private final LruCache<String, Bitmap> photoCache = new LruCache<>(40);

    public RestaurantAdapter(@NonNull Context context,
                             @NonNull List<Restaurant> items,
                             @NonNull OnRestaurantClickListener onClickListener) {
        this.appContext = context.getApplicationContext();
        this.items = items;
        this.onClickListener = onClickListener;

        if (!Places.isInitialized()) {
            Places.initialize(appContext, appContext.getString(R.string.google_maps_key));
        }
        placesClient = Places.createClient(appContext);
    }

    /** Call this from the Activity after you load favorites from Firestore. */
    public void setFavoriteIds(@NonNull Set<String> ids) {
        favoriteIds.clear();
        favoriteIds.addAll(ids);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_restaurant, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Restaurant r = items.get(position);

        holder.tvName.setText(r.getName());
        holder.tvAddress.setText(r.getAddress());
        holder.itemView.setOnClickListener(v -> onClickListener.onRestaurantClick(r));

        // Like icon state
        boolean isFav = favoriteIds.contains(r.getId());
        holder.ivLike.setImageResource(isFav ? R.drawable.ic_heart_filled : R.drawable.ic_heart);

        holder.btnLike.setOnClickListener(v -> toggleFavorite(r, holder));

        // Photo
        holder.ivRestaurantImage.setImageResource(R.drawable.ic_image_placeholder);

        String placeId = r.getPlaceId();
        if (placeId == null || placeId.trim().isEmpty()) return;

        holder.ivRestaurantImage.setTag(placeId);

        Bitmap cached = photoCache.get(placeId);
        if (cached != null) {
            holder.ivRestaurantImage.setImageBitmap(cached);
            return;
        }

        FetchPlaceRequest placeReq = FetchPlaceRequest.builder(
                placeId,
                Arrays.asList(Place.Field.PHOTO_METADATAS)
        ).build();

        placesClient.fetchPlace(placeReq)
                .addOnSuccessListener(placeResponse -> {
                    List<PhotoMetadata> photos = placeResponse.getPlace().getPhotoMetadatas();
                    if (photos == null || photos.isEmpty()) return;

                    FetchPhotoRequest photoReq = FetchPhotoRequest.builder(photos.get(0))
                            .setMaxWidth(900)
                            .setMaxHeight(600)
                            .build();

                    placesClient.fetchPhoto(photoReq)
                            .addOnSuccessListener(photoResponse -> {
                                Bitmap bmp = photoResponse.getBitmap();
                                if (bmp == null) return;

                                photoCache.put(placeId, bmp);

                                Object tag = holder.ivRestaurantImage.getTag();
                                if (placeId.equals(tag)) {
                                    holder.ivRestaurantImage.setImageBitmap(bmp);
                                }
                            });
                });
    }

    private void toggleFavorite(@NonNull Restaurant r, @NonNull VH holder) {
        String uid = (FirebaseAuth.getInstance().getCurrentUser() != null)
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (uid == null) {
            Toast.makeText(appContext, "User not signed in", Toast.LENGTH_SHORT).show();
            return;
        }

        String restaurantId = r.getId();
        boolean currentlyFav = favoriteIds.contains(restaurantId);

        var db = FirebaseFirestore.getInstance();
        var docRef = db.collection("favorites")
                .document(uid)
                .collection("items")
                .document(restaurantId);

        if (currentlyFav) {
            // Remove
            docRef.delete().addOnSuccessListener(unused -> {
                favoriteIds.remove(restaurantId);
                holder.ivLike.setImageResource(R.drawable.ic_heart);
            });
        } else {
            // Add
            docRef.set(new FavoriteItem(
                    restaurantId,
                    r.getPlaceId(),
                    r.getName(),
                    r.getAddress()
            )).addOnSuccessListener(unused -> {
                favoriteIds.add(restaurantId);
                holder.ivLike.setImageResource(R.drawable.ic_heart_filled);
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvAddress;
        ImageView ivRestaurantImage;
        View btnLike;
        ImageView ivLike;

        VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvAddress = itemView.findViewById(R.id.tvAddress);
            ivRestaurantImage = itemView.findViewById(R.id.ivRestaurantImage);

            // requires item_restaurant.xml from the Figma-like card
            btnLike = itemView.findViewById(R.id.btnLike);
            ivLike = itemView.findViewById(R.id.ivLike);
        }
    }

    /** Simple POJO for Firestore */
    public static class FavoriteItem {
        public String restaurantId;
        public String placeId;
        public String name;
        public String address;
        public FieldValue createdAt;

        public FavoriteItem() {}

        public FavoriteItem(String restaurantId, String placeId, String name, String address) {
            this.restaurantId = restaurantId;
            this.placeId = placeId;
            this.name = name;
            this.address = address;
            this.createdAt = FieldValue.serverTimestamp();
        }
    }
}