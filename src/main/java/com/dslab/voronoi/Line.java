package com.dslab.voronoi;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineSegment;

import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Vector;

public class Line extends LineSegment {

   static int RIGHT = 1, LEFT = -1, ZERO = 0;

   private boolean p0Bound = false;
   private boolean p1Bound = false;

   private HashSet<Line> pastIntersectedLines = new HashSet<>();

   // points that own this line (different from coords)
   private Point pA, pB;

   public Line(Coordinate a, Coordinate b) {
      super(a, b);
   }

   public Line(double x1, double y1, double x2, double y2) {
      super(x1, y1, x2, y2);

      // always want lines points upwards

      // a = (y2 - y1) / (x2 - x1);
      // b = y1 - a * x1;

   }

   public boolean isParallel(Line line) {
      if (line.isHorizontal() && this.isHorizontal() || line.isVertical() && this.isVertical()) {
         return true;
      }
      LineSegment l1, l2;
      if (pA == line.pA) {
         l1 = new LineSegment(pA.getCoordinate(), line.pB.getCoordinate());
      } else {
         l1 = new LineSegment(pA.getCoordinate(), line.pA.getCoordinate());
      }
      if (pB == line.pB) {
         l2 = new LineSegment(pB.getCoordinate(), line.pA.getCoordinate());
      } else {
         l2 = new LineSegment(pB.getCoordinate(), line.pB.getCoordinate());
      }

      if (l1.lineIntersection(l2) == null) {
         return true;
      }
      return false;
   }

   public Coordinate intersects(Line line) {
      // the same line cannot intersect this line more than once
      if (pastIntersectedLines.contains(line)) {
         return null;
      }
      if (this.isParallel(line)) {
         return null;
      }
      Coordinate itx = this.lineIntersection(line);
      if (itx == null) {
         return null;
      }
      // if this line is bound at this intersection point it means its already been
      // intersected
      // if ((coordsEqual(itx, p0) && p0Bound) || (coordsEqual(itx, p1) && p1Bound)) {
      // return null;
      // }

      if (withinBounds(itx) && line.withinBounds(itx)) {
         pastIntersectedLines.add(line);
         line.pastIntersectedLines.add(this);
         return itx;
      }

      return null;

   }

   public boolean bisects(Point other) {
      return pB == other || pA == other;

   }

   @Override
   public boolean equals(Object other) {
      if (other.getClass() == this.getClass()) {
         Line l = (Line) other;
         if (pA.equals(l.pA) && pB.equals(l.pB)) {
            return true;
         }
      }

      return false;
   }

   @Override
   public int hashCode() {
      return pA.hashCode() * pB.hashCode();
   }

   public static boolean coordsEqual(Coordinate a, Coordinate b) {
      if (Math.abs(a.getX() - b.getX()) < 0.1 && Math.abs(a.getY() - b.getY()) < 0.1) {
         return true;
      }
      return false;
   }

   /**
    * Tests whether the segment is horizontal. It uses the coords of the points the
    * line bisects NOT the coords of the line's src and end points
    *
    * @return <code>true</code> if the segment is horizontal
    */
   @Override
   public boolean isHorizontal() {
      return Math.abs(pA.getX() - pB.getX()) < 0.1;
   }

   /**
    * Tests whether the segment is horizontal. It uses the coords of the points the
    * line bisects NOT the coords of the line's src and end points
    *
    * @return <code>true</code> if the segment is horizontal
    */
   @Override
   public boolean isVertical() {
      return Math.abs(pA.getY() - pB.getY()) < 0.1;
   }

   // this is used for HORIZONTAL lines ONLY
   public void horizontalTowardsRight() {
      if (p0.getX() > p1.getX()) {
         swapCoordinates();
      }
   }

   // this is used for HORIZONTAL lines ONLY
   public void horizontalTowardsLeft() {
      if (p0.getX() < p1.getX()) {
         swapCoordinates();
      }
   }

   public boolean containsPoint(Point p) {
      return containsCoordinate(p.getCoordinate());
   }

   public boolean containsCoordinate(Coordinate p) {
      double lineLength = p0.distance(p1);
      double seg1Length = p0.distance(p);
      double seg2Length = p1.distance(p);
      if (Math.abs(seg1Length + seg2Length - lineLength) < 0.0000001) {
         return true;
      }
      return false;
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
      if ((distToP0 + distToP1) - boundLength < 0.01) {
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
      if (Math.abs(Math.abs(oldAngle) - Math.abs(newAngle)) > 0.00001) { // its flipped 180 degrees if the angle is
                                                                         // different
         // if flipped.. shift p0 past p1 on the line
         p0 = pointAlong(1.5);
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
      for (Line l : pastIntersectedLines) {
         l.pastIntersectedLines.remove(this);
      }

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

      return other.isCoordToRight(p1.getY() > p0.getY() ? p1 : p0);
   }

   public boolean isLeftOf(Line other) {
      return other.isCoordToLeft(p1.getY() > p0.getY() ? p1 : p0);
   }

   public boolean isCoordToRight(Coordinate p) {
      return getDirOfCoord(p) < 0.00001;

   }

   public boolean isCoordToLeft(Coordinate p) {
      return getDirOfCoord(p) > -0.00001;
   }

   public double getDirOfCoord(Coordinate p) {
      LineSegment line = new LineSegment(p, new Coordinate(100, p.getY()));
      Coordinate intersect = line.lineIntersection(this);
      if (intersect == null) {
         return Double.NaN;
      }
      return intersect.getX() - p.getX();
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
      // return yLower <= y && yUpper >= y;
      return y - yLower > -0.0001 && y - yUpper < 0.0001;
   }

}
