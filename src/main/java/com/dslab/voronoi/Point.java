package com.dslab.voronoi;

import org.locationtech.jts.geom.Coordinate;

import java.util.PriorityQueue;
import java.util.Stack;
import java.util.Vector;

public class Point {
   private Stack<Line> lines;

   private Stack<Line> stitching;
   Coordinate coord;

   public Point(int i, int j) {
      coord = new Coordinate(i, j);
      lines = new Stack<Line>();
      stitching = new Stack<>();
   }

   public Point(double x, double y) {
      this((int) x, (int) y);
   }

   public void insertLine(Line line) {
      lines.push(line);
   }

   public void deleteLine(Line line) {
      lines.remove(line);
   }

   public double distance(Point p) {
      return p.getCoordinate().distance(coord);
   }

   public Line popLine() {
      if (lines.size() > 0)
         return lines.remove(0);
      else
         return null;
   }

   public boolean hasBisectorWith(Point other) {
      for (Line l : lines) {
         if (l.bisects(other)) {
            return true;
         }
      }
      return false;
   }

   public String print() {
      return "" + getX() + ", " + getY();
   }

   public void removeLine(Line l) {
      synchronized (this) {
         lines.remove(l);
      }

   }

   public Coordinate getCoordinate() {
      return coord;
   }

   public double getY() {
      return coord.y;
   }

   public double getX() {
      return coord.x;
   }

   public boolean above(Point p) {
      return coord.y > p.getY();
   }

   public Stack<Line> getLines() {
      return lines;
   }

   public Line peekLine() {
      if (lines.size() > 0)
         return lines.peek();
      else
         return null;
   }

   public Coordinate midPoint(Point buddy) {
      return Line.midPoint(this.coord, buddy.coord);
   }

   /**
    * delete all lines to the LEFT or RIGHT of the given line
    * 
    * @param direction the side of the line that we cut off (1 = left, 2 = right)
    * @param cut       the line to determein where we make the cut
    */
   public void cutOffLines(int direction, Line cut, double exactCut, PriorityQueue<Line> removedLines) {

      for (Line l : lines) {
         if (direction == 2) { // cutoff right side

            if (l.isRightOf(cut)) {

               removedLines.add(l);
            }
         } else {

            if (l.isLeftOf(cut)) {
               removedLines.add(l);
            }

         }

      }

   }

   public void applyStitching(Vector<Line> stitch) {

      lines.addAll(stitching);
      stitching = new Stack<>();
   }

   public void addStitch(Line l) {
      stitching.add(l);
   }

}
