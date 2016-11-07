
package org.totschnig.myexpenses.places;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Generated;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Generated("org.jsonschema2pojo")
public class Result {

    @SerializedName("geometry")
    @Expose
    private Geometry geometry;
    @SerializedName("icon")
    @Expose
    private String icon;
    @SerializedName("id")
    @Expose
    private String id;
    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("opening_hours")
    @Expose
    private OpeningHours openingHours;
    @SerializedName("photos")
    @Expose
    private List<Photo> photos = new ArrayList<Photo>();
    @SerializedName("place_id")
    @Expose
    private String placeId;
    @SerializedName("reference")
    @Expose
    private String reference;
    @SerializedName("scope")
    @Expose
    private String scope;
    @SerializedName("types")
    @Expose
    private List<String> types = new ArrayList<String>();
    @SerializedName("vicinity")
    @Expose
    private String vicinity;
    @SerializedName("rating")
    @Expose
    private Double rating;

    /**
     * 
     * @return
     *     The geometry
     */
    public Geometry getGeometry() {
        return geometry;
    }

    /**
     * 
     * @param geometry
     *     The geometry
     */
    public void setGeometry(Geometry geometry) {
        this.geometry = geometry;
    }

    /**
     * 
     * @return
     *     The icon
     */
    public String getIcon() {
        return icon;
    }

    /**
     * 
     * @param icon
     *     The icon
     */
    public void setIcon(String icon) {
        this.icon = icon;
    }

    /**
     * 
     * @return
     *     The id
     */
    public String getId() {
        return id;
    }

    /**
     * 
     * @param id
     *     The id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * 
     * @return
     *     The name
     */
    public String getName() {
        return name;
    }

    /**
     * 
     * @param name
     *     The name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 
     * @return
     *     The openingHours
     */
    public OpeningHours getOpeningHours() {
        return openingHours;
    }

    /**
     * 
     * @param openingHours
     *     The opening_hours
     */
    public void setOpeningHours(OpeningHours openingHours) {
        this.openingHours = openingHours;
    }

    /**
     * 
     * @return
     *     The photos
     */
    public List<Photo> getPhotos() {
        return photos;
    }

    /**
     * 
     * @param photos
     *     The photos
     */
    public void setPhotos(List<Photo> photos) {
        this.photos = photos;
    }

    /**
     * 
     * @return
     *     The placeId
     */
    public String getPlaceId() {
        return placeId;
    }

    /**
     * 
     * @param placeId
     *     The place_id
     */
    public void setPlaceId(String placeId) {
        this.placeId = placeId;
    }

    /**
     * 
     * @return
     *     The reference
     */
    public String getReference() {
        return reference;
    }

    /**
     * 
     * @param reference
     *     The reference
     */
    public void setReference(String reference) {
        this.reference = reference;
    }

    /**
     * 
     * @return
     *     The scope
     */
    public String getScope() {
        return scope;
    }

    /**
     * 
     * @param scope
     *     The scope
     */
    public void setScope(String scope) {
        this.scope = scope;
    }

    /**
     * 
     * @return
     *     The types
     */
    public List<String> getTypes() {
        return types;
    }

    /**
     * 
     * @param types
     *     The types
     */
    public void setTypes(List<String> types) {
        this.types = types;
    }

    /**
     * 
     * @return
     *     The vicinity
     */
    public String getVicinity() {
        return vicinity;
    }

    /**
     * 
     * @param vicinity
     *     The vicinity
     */
    public void setVicinity(String vicinity) {
        this.vicinity = vicinity;
    }

    /**
     * 
     * @return
     *     The rating
     */
    public Double getRating() {
        return rating;
    }

    /**
     * 
     * @param rating
     *     The rating
     */
    public void setRating(Double rating) {
        this.rating = rating;
    }

}
