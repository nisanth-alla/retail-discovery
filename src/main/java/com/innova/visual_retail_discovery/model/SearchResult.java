package com.innova.visual_retail_discovery.model;


import java.util.List;

public class SearchResult {
    public Long productId;
    public String name;
    public String brand;
    public Double price;

    public String       imagePath;
    public List<String> detectedLabels;
    public float        score;

    public SearchResult(String imagePath, List<String> detectedLabels, float score, Long productId,String name,String brand,double price) {
        this.imagePath      = imagePath;
        this.detectedLabels = detectedLabels;
        this.score          = score;
        this.productId      = productId;
        this.name           = name;
        this.brand          = brand;
        this.price          = price;
    }


    public SearchResult() {

    }
}
