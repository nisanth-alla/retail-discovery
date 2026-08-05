package com.innova.visual_retail_discovery.service.vector;

import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ImageUtil {

    public static File convertToJpeg(MultipartFile multipartFile, String outputPath) throws IOException {

        // Read image from multipart file
        BufferedImage bufferedImage = ImageIO.read(multipartFile.getInputStream());

        // Output file
        File jpegFile = new File(outputPath);

        // Write as JPEG
        ImageIO.write(bufferedImage, "jpg", jpegFile);

        return jpegFile;
    }
}
