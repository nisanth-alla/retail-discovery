package com.innova.visual_retail_discovery.service.vector;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@Service
public class VendorService {


    @Value("${app.upload.dir}")
    private String uploadDir;

//    @Value("${app.base-url:http://localhost:8080}")
//    private String baseUrl;

    private static final List<String> ALLOWED_TYPES = List.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB

    public void registerProducts(List<MultipartFile> images) throws IOException {

        // Save images and collect paths
        for (MultipartFile img : images) {
            String[] names=img.getName().split("_");
            ImageVector imageVector = new ImageVector();
            if(names.length>0&&names[0]!=null){
                imageVector.setName(names[0]);
            }
            if(names.length>1&&names[1]!=null){
                imageVector.setBrand(names[1]);
            }
            if(names.length>2&&names[2]!=null){
                imageVector.setPrice(Double.valueOf(names[2]));
            }
            saveImage(imageVector, img);
        }
//        // Build image URLs for response
//        List<String> imageUrls = savedPaths.stream()
//                .map(p -> baseUrl + "/api/vendors/images/" + Paths.get(p).getFileName())
//                .collect(Collectors.toList());

//        return VendorResponse.builder()
//                .id(saved.getId())
//                .name(saved.getName())
//                .brand(saved.getBrand())
//                .price(saved.getPrice())
//                .email(saved.getEmail())
//                .phone(saved.getPhone())
//                .imageUrls(imageUrls)
//                .message("Vendor registered successfully")
//                .build();
    }

//    public VendorResponse getVendor(Long id) {
//        Vendor vendor = vendorRepository.findById(id)
//                .orElseThrow(() -> new NoSuchElementException("Vendor not found: " + id));
//
//        List<String> imageUrls = vendor.getImagePaths().stream()
//                .map(p -> baseUrl + "/api/vendors/images/" + Paths.get(p).getFileName())
//                .collect(Collectors.toList());
//
//        return VendorResponse.builder()
//                .id(vendor.getId())
//                .name(vendor.getName())
//                .brand(vendor.getBrand())
//                .price(vendor.getPrice())
//                .email(vendor.getEmail())
//                .phone(vendor.getPhone())
//                .imageUrls(imageUrls)
//                .build();
//    }

    // ── Private Helpers ──────────────────────────────

    private void saveImage(ImageVector imageVector, MultipartFile image) throws IOException {
        Path uploadPath = Paths.get(uploadDir);
        Files.createDirectories(uploadPath);
       // List<String> paths = new ArrayList<>();
        Path dest = uploadPath.resolve(imageVector.name);
        Files.copy(image.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);
        //paths.add(dest.toString());
    }
}
