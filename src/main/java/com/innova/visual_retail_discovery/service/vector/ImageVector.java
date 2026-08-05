package com.innova.visual_retail_discovery.service.vector;

import java.util.List;

public class ImageVector {
    public String       imagePath;
    public List<String> detectedLabels;
    public float[]      vector;
    public float[]      lableVector;
    public Long productId;

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public List<String> getDetectedLabels() {
        return detectedLabels;
    }

    public void setDetectedLabels(List<String> detectedLabels) {
        this.detectedLabels = detectedLabels;
    }

    public float[] getVector() {
        return vector;
    }

    public void setVector(float[] vector) {
        this.vector = vector;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String name;
    public String brand;
    public double price;

    public float[] getLableVector() {
        return lableVector;
    }

    public void setLableVector(float[] lableVector) {
        this.lableVector = lableVector;
    }

    public ImageVector() {}  // Jackson deserialization

    public ImageVector(String imagePath, List<String> detectedLabels, float[] vector, float[] labelVector, Long productId,String name,String brand,double price) {
        this.imagePath      = imagePath;
        this.detectedLabels = detectedLabels;
        this.vector         = vector;
        this.lableVector = labelVector;
        this.productId      = productId;
        this.name           = name;
        this.brand          = brand;
        this.price          = price;
    }
}
