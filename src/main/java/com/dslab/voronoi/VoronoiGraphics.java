package com.dslab.voronoi;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Insets;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.util.Scanner;
import java.util.Vector;
import java.io.*;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.event.MouseWheelListener;
import java.awt.event.MouseWheelEvent;

public class VoronoiGraphics implements Runnable {
   static final int defaultX = 100; // the default system size
   static final Color bgColor = new Color(0, 0, 0);
   static final Color ptColor = new Color(255, 255, 255);
   static final Color lnColor = new Color(190, 255, 128);

   private JFrame gWin; // a graphics window

   private Insets theInsets; // the insets of the window
   private Vector<com.dslab.voronoi.Point> points;

   private int xShift = 500;
   private java.awt.Point mousePt;
   private final int W;
   private final int H;
   private java.awt.Point origin;

   private final int SCALE_DENOM = 100;
   private int scale = SCALE_DENOM;

   public VoronoiGraphics(int x, int y, Vector<com.dslab.voronoi.Point> points) {
      this.W = x;
      this.H = y;
      this.points = points;
      origin = new Point(0, 0);
      startGraphics(x, y);

   }

   public void run() {
      while (true) {
         writeToGraphics(points);
         long resumeTime = System.currentTimeMillis() + 500;
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
      MouseDragTest();
      MouseScrollTest();

      theInsets = gWin.getInsets();
      gWin.setSize(x + theInsets.left + theInsets.right,
            y + theInsets.top + theInsets.bottom);

      // wait for frame to get initialized
      long resumeTime = System.currentTimeMillis() + 100;
      do {
      } while (System.currentTimeMillis() < resumeTime);

   }

   public void MouseDragTest() {
      gWin.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      gWin.addMouseListener(new MouseAdapter() {
         @Override
         public void mousePressed(MouseEvent e) {
            mousePt = e.getPoint();

         }
      });
      gWin.addMouseMotionListener(new MouseMotionAdapter() {
         @Override
         public void mouseDragged(MouseEvent e) {
            int dx = e.getX() - mousePt.x;
            int dy = e.getY() - mousePt.y;
            origin.setLocation(origin.x + dx, origin.y + dy);
            mousePt = e.getPoint();

         }
      });
   }

   public void MouseScrollTest() {

      gWin.addMouseWheelListener(new MouseAdapter() {
         @Override
         public void mouseWheelMoved(MouseWheelEvent e) {
            scale -= e.getWheelRotation();

         }
      });

   }

   private void writeToGraphics(Vector<com.dslab.voronoi.Point> points) {

      Graphics g = gWin.getGraphics();
      // pop out the graphics

      g.setColor(bgColor);
      g.fillRect(theInsets.left,
            theInsets.top,
            W,
            H);

      for (com.dslab.voronoi.Point p : points) {

         double newScale = (double) scale / SCALE_DENOM;
         // draw this point
         g.setColor(ptColor);
         g.drawOval((int) (p.getX() * newScale) + origin.x, (int) (p.getY() * newScale) + origin.y,
               1, 1);

         // draw this point's lines

         for (Line l : p.getLines()) {
            g.setColor(lnColor);
            g.drawLine((int) (l.getCoordinate(0).getX() * newScale) + origin.x,
                  (int) (l.getCoordinate(0)
                        .getY() * newScale) + origin.y,
                  (int) (l.getCoordinate(1).getX() * newScale) + origin.x,
                  (int) (l.getCoordinate(1).getY() * newScale) + origin.y);
         }
      }

   }
}
