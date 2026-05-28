package com.example.mapp.model;

import com.google.gson.annotations.SerializedName;

public class BiologyDetection {
    @SerializedName(value = "bioName", alternate = {"bio_name"})
    private String bioName;
    @SerializedName("enName")
    private String enName;
    @SerializedName("intro")
    private String intro;
    @SerializedName(value = "className", alternate = {"class_name"})
    private String className;
    @SerializedName("confidence")
    private Float confidence;
    @SerializedName("imageUrl")
    private String imageUrl;

    public String getBioName() {
        return bioName;
    }

    public void setBioName(String bioName) {
        this.bioName = bioName;
    }

    public String getEnName() {
        return enName;
    }

    public void setEnName(String enName) {
        this.enName = enName;
    }

    public String getIntro() {
        return intro;
    }

    public void setIntro(String intro) {
        this.intro = intro;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public Float getConfidence() {
        return confidence;
    }

    public void setConfidence(Float confidence) {
        this.confidence = confidence;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
