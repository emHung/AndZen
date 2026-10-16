package com.sanbong.app.model;

import java.io.Serializable;
import java.util.List;

/**
 * Đối tượng mô tả một cụm sân bóng
 */
public class Pitch implements Serializable {
    private final String id;
    private final String name;
    private final String district;
    private final String address;
    private final PitchType type;
    private final long minPrice;
    private final double rating;
    private final List<String> amenities;
    private final String description;
    private final int imageResId;
    private final String imageKey;

    public Pitch(String id, String name, String district, String address, 
                 PitchType type, long minPrice, double rating, 
                 List<String> amenities, String description, int imageResId) {
        this(id, name, district, address, type, minPrice, rating, amenities, description, imageResId, null);
    }

    public Pitch(String id, String name, String district, String address,
                 PitchType type, long minPrice, double rating,
                 List<String> amenities, String description, int imageResId, String imageKey) {
        this.id = id;
        this.name = name;
        this.district = district;
        this.address = address;
        this.type = type;
        this.minPrice = minPrice;
        this.rating = rating;
        this.amenities = amenities;
        this.description = description;
        this.imageResId = imageResId;
        this.imageKey = imageKey;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDistrict() { return district; }
    public String getAddress() { return address; }
    public PitchType getType() { return type; }
    public long getMinPrice() { return minPrice; }
    public double getRating() { return rating; }
    public List<String> getAmenities() { return amenities; }
    public String getDescription() { return description; }
    public int getImageResId() { return imageResId; }
    public String getImageKey() { return imageKey; }
}
