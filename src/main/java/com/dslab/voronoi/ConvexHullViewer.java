package com.dslab.voronoi;

import java.io.File;
import java.io.IOException;
import java.util.Vector;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import javax.imageio.ImageIO;

public class ConvexHullViewer {

   public static void createImage(int size, ConvexHull ch, File output) throws IOException {
      BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
      Graphics2D graphics = img.createGraphics();
      Vector<Point> points = ch.getPoints();
      Point prev = points.get(points.size() - 1);
      for (Point p : points) {
         graphics.draw(new Line2D.Double(prev.getX(), prev.getY(), p.getX(), p.getY()));
         prev = p;
      }

      ImageIO.write(img, "png", output);

   }
}