package com.dslab.voronoi;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import javax.imageio.ImageIO;

import org.apache.hadoop.fs.Path;

public class ConvexHullViewer {

   // args: [size of map, file containing points, file path for image output]
   public static void createImage(int size, Scanner points, Path output) throws FileNotFoundException, IOException {

      BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);

      // char[][] pixels = new char[size][size];

      Graphics2D graphics = img.createGraphics();

      Point prev = new Point(points);
      while (points.hasNextInt()) {
         Point p = new Point(points);
         graphics.draw(new Line2D.Double(prev.getX(), prev.getY(), p.getX(), p.getY()));
         // pixels[p.getY()][p.getX()] = (char) 255;
         // img.setRGB(p.getY(), p.getX(), Integer.MAX_VALUE);
         prev = p;
      }

      File out = new File(output.toString());
      ImageIO.write(img, "png", out);

   }
}