package com.dslab.voronoi;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Insets;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.util.Scanner;
import java.util.Vector;
import java.io.*;

public class VoronoiGraphics implements Runnable {
   static final int defaultX = 100; // the default system size
   static final Color bgColor = new Color(0, 0, 0);
   static final Color ptColor = new Color(255, 255, 255);
   static final Color lnColor = new Color(190, 255, 128);

   private JFrame gWin; // a graphics window

   private Insets theInsets; // the insets of the window
   private Vector<Point> points;
   private int x;
   private int y;

   public VoronoiGraphics(int x, int y, Vector<Point> points) {
      startGraphics(x, y);
      this.points = points;
      this.x = x;
      this.y = y;

   }

   public void run() {
      while (true) {
         writeToGraphics(points);
         long resumeTime = System.currentTimeMillis() + 2000;
         do {
         } while (System.currentTimeMillis() < resumeTime);

      }
   }

   private void startGraphics(int x, int y) {

      // initialize window and graphics:
      gWin = new JFrame("Voronoi Diagram");
      gWin.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      gWin.setLocation(50, 50); // screen coordinates of top left corner
      gWin.setResizable(true);
      gWin.setVisible(true); // show it!

      theInsets = gWin.getInsets();
      gWin.setSize(x + theInsets.left + theInsets.right,
            y + theInsets.top + theInsets.bottom);

      // wait for frame to get initialized
      long resumeTime = System.currentTimeMillis() + 100;
      do {
      } while (System.currentTimeMillis() < resumeTime);

   }

   private void writeToGraphics(Vector<Point> points) {

      Graphics g = gWin.getGraphics();
      // pop out the graphics

      g.setColor(bgColor);
      g.fillRect(theInsets.left,
            theInsets.top,
            x,
            y);

      for (Point p : points) {

         // draw this point
         g.setColor(ptColor);
         g.drawOval((int) p.getX(), (int) p.getY(),
               1, 1);

         // draw this point's lines

         for (Line l : p.getLines()) {
            g.setColor(lnColor);
            g.drawLine((int) (l.getCoordinate(0).getX()), (int) (l.getCoordinate(0)
                  .getY()), (int) (l.getCoordinate(1).getX()), (int) (l.getCoordinate(1).getY()));
         }
      }

   }
}
