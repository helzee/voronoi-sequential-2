package com.dslab.voronoi;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineSegment;
import java.io.Serializable;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Vector;

public class Line extends LineSegment {

   static int RIGHT = 1, LEFT = -1, ZERO = 0;

   private boolean p0Bound = false;
   private boolean p1Bound = false;

   // points that own this line (different from coords)
   private Point pA, pB;

   public Line(double x1, double y1, double x2, double y2) {
      super(x1, y1, x2, y2);

      // always want lines points upwards

      // a = (y2 - y1) / (x2 - x1);
      // b = y1 - a * x1;

   }

   public Coordinate intersects(Line line) {
      Coordinate itx = this.lineIntersection(line);
      if (itx == null) {
         return null;
      }
      // if this line is bound at this intersection point it means its already been
      // intersected
      if ((coordsEqual(itx, p0) && p0Bound) || (coordsEqual(itx, p1) && p1Bound)) {
         return null;
      }

      if (withinBounds(itx) && line.withinBounds(itx)) {
         return itx;
      }

      return null;

   }

   public static boolean coordsEqual(Coordinate a, Coordinate b) {
      if (Math.abs(a.getX() - b.getX()) < 1 && Math.abs(a.getY() - b.getY()) < 1) {
         return true;
      }
      return false;
   }

   /**
    * Tests whether the segment is horizontal.
    *
    * @return <code>true</code> if the segment is horizontal
    */
   @Override
   public boolean isHorizontal() {
      return Math.abs(p0.y - p1.y) < 1;
   }

   // this is used for HORIZONTAL lines ONLY
   public void horizontalTowardsRight() {
      if (p0.getX() > p1.getX()) {
         swapCoordinates();
      }
   }

   public void swapCoordinates() {
      Coordinate temp = p0;
      p0 = p1;
      p1 = temp;
   }

   public boolean fullyBounded() {
      return p0Bound && p1Bound;
   }

   public boolean unbounded() {
      return !p0Bound && !p1Bound;
   }

   /**
    * Check if we are in bounds by checking the distance between the bounds and the
    * distance from each bound to the point. If the distance from each bound to the
    * point is the same as the distance between bounds, we know it is within the
    * bounds. if the length from each bound to the point is greater than the length
    * between bounds, we are outside the bounds and can determine which bound we
    * are outside of by seeing which bound is closer to the point a
    * 
    * @param a Should be a point on the infinite Line represented by this line
    *          segment!!
    * @return true if within bounds
    */
   public boolean withinBounds(Coordinate a) {

      if (!p0Bound && !p1Bound) {
         return true;
      }
      double boundLength = p0.distance(p1);
      double distToP0 = a.distance(p0);
      double distToP1 = a.distance(p1);

      // if boundlenght = (distP0 + distP1) within 1 unit. It will never be negative
      if ((distToP0 + distToP1) - boundLength < 1) {
         // within both bounds
         return true;
      } else if (distToP0 < distToP1) {
         // outside of P0 bound
         return !p0Bound;
      } else {
         // outside of P1 bound
         return !p1Bound;
      }

   }

   public Point getP0() {
      return pA;
   }

   public Point getP1() {
      return pB;
   }

   public void setSrc(Coordinate p) {
      double oldAngle = this.angle();
      p0 = new Coordinate(p);
      p0Bound = true;
      double newAngle = this.angle();
      if (!this.isVertical()) { // vertical flip will result in similar radians but opposite sign
         oldAngle = Math.abs(oldAngle);
         newAngle = Math.abs(newAngle);
      }
      if (Math.abs(oldAngle - newAngle) > 0.00001) { // its flipped 180 degrees if the angle is different
         // if flipped.. shift p1 past p0 on the line
         p1 = pointAlong(-1.0);
      }
   }

   public boolean isSrcBound() {
      return p0Bound;
   }

   public boolean isEndBound() {
      return p1Bound;
   }

   public void setEnd(Coordinate p) {
      double oldAngle = this.angle();
      p1 = new Coordinate(p);
      p1Bound = true;
      double newAngle = this.angle();
      if (Math.abs(oldAngle - newAngle) > 0.00001) { // its flipped 180 degrees if the angle is different
         // if flipped.. shift p0 past p1 on the line
         p0 = pointAlong(1.0);
      }
   }

   public void setEndUnbounded(Coordinate p) {
      p1 = new Coordinate(p);
   }

   public void setSrcUnbounded(Coordinate p) {
      p0 = new Coordinate(p);
   }

   public double getX0() {
      return p0.getX();
   }

   public double getX1() {
      return p1.getX();
   }

   public double getY0() {
      return p0.getY();
   }

   public double getY1() {
      return p1.getY();
   }

   public Coordinate getSrc() {
      return p0;
   }

   public Coordinate getEnd() {
      return p1;
   }

   public void setX0(double x) {
      p0.setX(x);
   }

   public void setX1(double x) {
      p1.setX(x);
   }

   public void setY0(double y) {
      p0.setY(y);
   }

   public void setY1(double y) {
      p1.setY(y);
   }

   // p1 is left, p2 is right
   public Line(double x1, double y1, double x2, double y2, Point p1, Point p2) {
      this(x1, y1, x2, y2);
      this.pA = p1;
      this.pB = p2;

   }

   public Line(Point a, Point b) {
      this(a.getX(), a.getY(), b.getX(), b.getY());
      pA = a;
      pB = b;
   }

   // delete all references to this line
   public void removeSelf() {

      pA.removeLine(this);
      pB.removeLine(this);

   }

   public void cutOffLines(int direction, Line cut, double exactCut, PriorityQueue<Line> res) {

      pA.cutOffLines(direction, cut, exactCut, res);
      pB.cutOffLines(direction, cut, exactCut, res);

   }

   public double getLowerY() {
      return Math.min(p0.getY(), p1.getY());
   }

   public double getUpperY() {
      return Math.max(p0.getY(), p1.getY());
   }

   public boolean isRightOf(Line other) {
      return other.orientationIndex(this) != 1;
   }

   public boolean isLeftOf(Line other) {
      return other.orientationIndex(this) == 1;
   }

   public boolean inYBounds(double y) {
      double yLower = getLowerY();
      double yUpper = getUpperY();
      if (!p0Bound && p0.getY() == yLower || !p1Bound && p1.getY() == yLower) {
         yLower = Double.NEGATIVE_INFINITY;
      }
      if (!p1Bound && p1.getY() == yUpper || !p0Bound && p0.getY() == yUpper) {
         yUpper = Double.POSITIVE_INFINITY;
      }
      return yLower <= y && yUpper >= y;
   }

}
