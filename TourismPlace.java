package Hospitality_and_tourism_system;

public class TourismPlace {
    private int placeId;
    private String placeName;
    private String description;
    private String mapUrl;
    private String imagePath;

    public TourismPlace(int placeId, String placeName, String description, String mapUrl, String imagePath) {
        this.placeId = placeId;
        this.placeName = placeName;
        this.description = description;
        this.mapUrl = mapUrl;
        this.imagePath = imagePath;
    }

    public int getPlaceId() { return placeId; }
    public String getPlaceName() { return placeName; }
    public String getDescription() { return description; }
    public String getMapUrl() { return mapUrl; }
    public String getImagePath() { return imagePath; }
}