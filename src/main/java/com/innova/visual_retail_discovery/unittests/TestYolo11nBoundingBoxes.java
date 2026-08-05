package com.innova.visual_retail_discovery.unittests;

import com.innova.visual_retail_discovery.utils.Utils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class TestYolo11nBoundingBoxes {

    public static void main(String[] args) throws IOException {
        // ---------------------- Configure ----------------------
        String imagePath = Utils.SAMPLE_IMAGE;
        String labelPath = Utils.SAMPLE_IMAGE_LABEL;
        String outputPath = Utils.OUTPUT_FILE_PATH;
        String[] classNames = Utils.SUPPORTED_CLASSES;

        // ---------------------- Load image ----------------------
        BufferedImage image = ImageIO.read(new File(imagePath));
        int imgWidth = image.getWidth();
        int imgHeight = image.getHeight();

        // ---------------------- Read labels ----------------------
        List<String> lines = Files.readAllLines(Paths.get(labelPath));

        Graphics2D g2d = image.createGraphics();
        g2d.setColor(Color.RED);
        g2d.setStroke(new BasicStroke(3)); // thick bounding boxes

        for (String line : lines) {
            String[] parts = line.trim().split("\\s+");
            if (parts.length != 5) continue; // skip invalid lines

            int classId = Integer.parseInt(parts[0]);
            double xCenterNorm = Double.parseDouble(parts[1]);
            double yCenterNorm = Double.parseDouble(parts[2]);
            double widthNorm   = Double.parseDouble(parts[3]);
            double heightNorm  = Double.parseDouble(parts[4]);

            // Convert normalized YOLO format to pixel coordinates
            int boxWidth = (int) (widthNorm * imgWidth);
            int boxHeight = (int) (heightNorm * imgHeight);
            int x = (int) (xCenterNorm * imgWidth - boxWidth / 2.0);
            int y = (int) (yCenterNorm * imgHeight - boxHeight / 2.0);

            // Clamp coordinates
            x = Math.max(0, x);
            y = Math.max(0, y);
            boxWidth = Math.min(boxWidth, imgWidth - x);
            boxHeight = Math.min(boxHeight, imgHeight - y);

            // Draw rectangle
            g2d.drawRect(x, y, boxWidth, boxHeight);

            // Draw label text
            String label = classId < classNames.length ? classNames[classId] : "class" + classId;
            System.out.println(label);
            System.out.println("x="+x+" y="+y+" width = "+imgWidth+" Height"+imgHeight);
            g2d.drawString(label, x, y - 5);
        }

        g2d.dispose();

        // ---------------------- Save output ----------------------
        ImageIO.write(image, "jpg", new File(outputPath));
        System.out.println("Saved output image with bounding boxes at: " + outputPath);
    }
}
