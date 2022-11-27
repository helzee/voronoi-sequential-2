package com.dslab.voronoi;

import java.util.Vector;

import org.locationtech.jts.algorithm.Angle;
import org.locationtech.jts.geom.Coordinate;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Stack;

public class VoronoiDiagram {
   private int size_x;
   private int size_y;

   private static final int RIGHT = 2;
   private static final int LEFT = 1;

   public VoronoiDiagram(int size_x, int size_y, Vector<Point> points) {
      this.size_x = size_x;
      this.size_y = size_y;
      divide(size_x, size_y, points, 0, points.size() - 1);
   }

   ConvexHull divide(int size_x, int size_y, Vector<Point> points, int lower, int upper) {
      int size = upper - lower + 1; // + 1 because converting last index to size?

      if (size > 2) {
         int mid = lower + size / 2;
         ConvexHull leftConvexHull = divide(size_x, size_y, points, lower, mid - 1);
         ConvexHull rightConvexHull = divide(size_x, size_y, points, mid, upper);
         return stitch(size_x, size_y, points, leftConvexHull, rightConvexHull);
         // if we comput convex hull to reduce time complexity, could do it after we get
         // each ConvexHull. Take right convex hull of left ConvexHull and left CV of
         // right
         // ConvexHull. then send those to stitch
      } else {
         // base case
         if (size == 2) {
            // draw a line
            Point p0 = points.elementAt(lower);
            Point p1 = points.elementAt(upper);

            Line bisector = bisectorLine(size_x, size_y, p0, p1);

            // lower point always gets the new line

            p0.insertLine(bisector);

            p1.insertLine(bisector);

            return new ConvexHull(p0, p1);

         }

      }

      return new ConvexHull(points.elementAt(lower));
   }

   // check the newest line for this point for an intersection
   // if point has no line or no intersection found then return null
   private double[] findItx(Point p0, Line bisector, Coordinate srcPoint,
         Line lastBisectedLine) {
      double[] itx = null;
      double dist = Double.MAX_VALUE;
      int i = 0;
      for (Line line : p0.getLines()) {

         Coordinate temp = line.intersects(bisector);
         if (temp != null && line != lastBisectedLine) {
            double distTemp = temp.distance(srcPoint);

            if (distTemp < dist) {
               itx = new double[3];
               itx[0] = temp.getX();
               itx[1] = temp.getY();
               itx[2] = i;
               dist = distTemp;

            }
         }

         i++;
      }

      return itx;
   }

   // p1 is left, p2 is right ALWAYS
   private static Line bisectorLine(int size_x, int size_y, Point p1, Point p2) {

      Coordinate midPoint = p1.midPoint(p2);

      double perpendicular_slope = -1.0 * ((double) p2.getX() - p1.getX()) / ((double) p2.getY() - p1.getY());

      if (Double.isInfinite(perpendicular_slope)) { // line is horizontal
         return new Line(midPoint.getX(), 0, midPoint.getX(), size_y, p1, p2);

      }
      if (perpendicular_slope == 0) {
         if (p2.above(p1)) { // right above left.. the line is traveling to left
            return new Line(size_x, midPoint.getY(), 0, midPoint.getY(), p1, p2);
         } else {
            return new Line(0, midPoint.getY(), size_x, midPoint.getY(), p1, p2);
         }
      }

      double intersect = midPoint.getY() - perpendicular_slope * midPoint.getX();

      // generate a bisector line
      // compute x1, y1
      double x1 = 0;
      double y1 = intersect;
      // // compute x2, y2
      double x2 = size_x;
      double y2 = perpendicular_slope * x2 + intersect;
      if (y1 < y2) {
         return new Line(x1, y1, x2, y2, p1, p2);
      } else {
         return new Line(x2, y2, x1, y1, p1, p2);
      }

   }

   // This does not handle case when starting bridge creates a bisector with slope
   // of 0! (need to determine if left side is above or below right side to
   // determine what direction to look for intersections then)
   public double[] findLowestIntersection(Point p, Line bisector) {
      double yVal = Double.POSITIVE_INFINITY;
      double[] res = null;
      for (int i = 0; i < p.getLines().size(); i++) {
         Line line = p.getLines().get(i);
         Coordinate itx = line.intersects(bisector);
         if (itx != null) {
            if (itx.getY() < yVal) {
               yVal = itx.getY();
               res = new double[3];
               res[0] = itx.getX();
               res[1] = itx.getY();
               res[2] = i;
            }
         }
      }
      return res;

   }

   // if dir is LEFT, right lefmost intersection. If RIGHT, rightmost
   public double[] findLeftRightMostIntersection(Point p, Line bisector, int dir) {
      double xVal = Double.NEGATIVE_INFINITY;
      if (dir == LEFT) {
         xVal = Double.POSITIVE_INFINITY;
      }
      double[] res = null;
      for (int i = 0; i < p.getLines().size(); i++) {
         Line line = p.getLines().get(i);
         Coordinate itx = line.intersects(bisector);
         if (itx != null) {
            if ((itx.getX() > xVal && dir == RIGHT) || (itx.getX() < xVal && dir == LEFT)) {
               xVal = itx.getX();
               res = new double[3];
               res[0] = itx.getX();
               res[1] = itx.getY();
               res[2] = i;
            }
         }
      }
      return res;

   }

   private ConvexHull stitch(int size_x, int size_y, Vector<Point> points,
         ConvexHull leftConvexHull, ConvexHull rightConvexHull) {

      // we need to run a convex hull merge algorithm to find the starting and ending
      // bridge
      Vector<Stack<Point>> bridges = leftConvexHull.merge(rightConvexHull);

      Stack<Point> leftBridge = bridges.get(0);
      Stack<Point> rightBridge = bridges.get(1);

      // choose the lowest point from left and right.
      Point p0 = leftBridge.remove(0);
      Point p1 = rightBridge.remove(0);

      Point bottomLeftBridge = p0;
      Point bottomRightBridge = p1;

      Coordinate srcPoint = null;
      Coordinate endPoint = null;

      Line lastBisectedLine = null;

      HashSet<Line> seenLines = new HashSet<>();
      HashSet<Point> seenPoints = new HashSet<>();
      seenPoints.add(p0);
      seenPoints.add(p1);
      Point upperLeftBridge = p0;
      Point upperRightBridge = p1;

      if (!leftBridge.isEmpty()) {

         upperLeftBridge = leftBridge.pop();
      }
      if (!rightBridge.isEmpty()) {

         upperRightBridge = rightBridge.pop();
      }

      Vector<Line> stitch = new Vector<>();
      // order removed lines from lowest upper Y value to highest
      PriorityQueue<Line> leftRemovedLines = new PriorityQueue<Line>(Comparator.comparing(Line::getUpperY));
      PriorityQueue<Line> rightRemovedLines = new PriorityQueue<Line>(Comparator.comparing(Line::getUpperY));

      do {
         // 1. get a bisector line between them.
         Line bisector = bisectorLine(size_x, size_y, p0, p1);
         // no longer necessary cuz of stitching
         seenLines.add(bisector);
         boolean isStartingStitch = false;
         Line l = null;
         boolean cutFromLeft = false;

         // 2. adjust the src point TODO
         // srcX = (endX == 0.0 && endY == 0.0) ? ((bisector.y1 < bisector.y2) ?
         // bisector.x1 : bisector.x2) : endX;
         // srcY = (endX == 0.0 && endY == 0.0) ? ((bisector.y1 < bisector.y2) ?
         // bisector.y1 : bisector.y2) : endY;
         srcPoint = endPoint;
         endPoint = null;

         if (p0 == upperLeftBridge && p1 == upperRightBridge) { // this means we have finished. extend the line to
                                                                // infinity

            bisector.setSrc(srcPoint);
            stitch.add(bisector);

            p0.insertLine(bisector);
            p1.insertLine(bisector);
            break;
         } else if (p0 == bottomLeftBridge && p1 == bottomRightBridge) { // we are starting. line starts at negative
                                                                         // infinity
                                                                         // this is a special case where we cant
                                                                         // measure distance from source, but have
                                                                         // to use lowest y value of intersection
            isStartingStitch = true;

         } else { // regular case not involving end or start. we will have srce and end points

            bisector.setSrc(srcPoint);

            // set and unbounded endpoint to help direct the bisector in the right direction
            // ( in case src is in an odd spot possibly horizontal) we know the bisector has
            // to pass through the midpoint of the two points
            // bisector.setEndUnbounded(p0.midPoint(p1));

         }

         // 3. compute the intersect with the bottom voronoi edges.
         if (!isStartingStitch) {
            double[] its1 = findItx(p0, bisector, srcPoint, lastBisectedLine);
            double[] its2 = findItx(p1, bisector, srcPoint, lastBisectedLine);

            if (its1 == null && its2 == null) { // RARE CASE when all points exist on same line
               System.err.println("No intersections FOUND before exiting top bridge.\n"
                     + " Either all the lines are parallel or this is an error!");
               stitch.add(bisector);

               p0.insertLine(bisector);
               p1.insertLine(bisector);
               break;
            }

            // 4. find which of the two intersects with the bisector line is closer to the
            // source point
            double dist1 = (its1 != null) ? srcPoint.distance(new Coordinate(its1[0], its1[1])) : Double.MAX_VALUE;
            double dist2 = (its2 != null) ? srcPoint.distance(new Coordinate(its2[0], its2[1])) : Double.MAX_VALUE;

            double endX = (dist1 < dist2) ? its1[0] : its2[0];
            double endY = (dist1 < dist2) ? its1[1] : its2[1];

            endPoint = new Coordinate(endX, endY);

            if (dist1 < dist2) {
               l = p0.getLines().elementAt((int) its1[2]);
               cutFromLeft = true;
            } else {
               l = p1.getLines().elementAt((int) its2[2]);
            }

         } else {
            if (bisector.isHorizontal()) {
               // cant find lowest intersection. ned to see which point is above. if left point
               // is above right point then we find the rightmost intersection and vice versa
               if (p0.above(p1)) {
                  double[] itx1 = findLeftRightMostIntersection(p0, bisector, RIGHT);
                  double[] itx2 = findLeftRightMostIntersection(p1, bisector, RIGHT);

                  if (itx1 == null && itx2 == null) { // RARE CASE when all points exist on same line
                     System.err.println("No intersections FOUND before exiting top bridge.\n"
                           + " Either all the lines are parallel or this is an error!");
                     stitch.add(bisector);

                     p0.insertLine(bisector);
                     p1.insertLine(bisector);
                     break;
                  } else if (itx1 == null) {
                     l = p1.getLines().get((int) itx2[2]);
                     endPoint = new Coordinate(itx2[0], itx2[1]);
                  } else if (itx2 == null) {
                     l = p0.getLines().get((int) itx1[2]);
                     endPoint = new Coordinate(itx1[0], itx1[1]);
                     cutFromLeft = true;
                  } else if (itx1[0] > itx2[0]) {
                     l = p0.getLines().get((int) itx1[2]);
                     endPoint = new Coordinate(itx1[0], itx1[1]);
                     cutFromLeft = true;
                  } else {
                     l = p1.getLines().get((int) itx2[2]);
                     endPoint = new Coordinate(itx2[0], itx2[1]);
                  }
               } else {
                  double[] itx1 = findLeftRightMostIntersection(p0, bisector, LEFT);
                  double[] itx2 = findLeftRightMostIntersection(p1, bisector, LEFT);
                  if (itx1 == null && itx2 == null) { // RARE CASE when all points exist on same line
                     System.err.println("No intersections FOUND before exiting top bridge.\n"
                           + " Either all the lines are parallel or this is an error!");
                     p0.insertLine(bisector);
                     p1.insertLine(bisector);
                     stitch.add(bisector);

                     break;
                  } else if (itx1 == null) {
                     l = p1.getLines().get((int) itx2[2]);
                     endPoint = new Coordinate(itx2[0], itx2[1]);
                  } else if (itx2 == null) {
                     l = p0.getLines().get((int) itx1[2]);
                     endPoint = new Coordinate(itx1[0], itx1[1]);
                     cutFromLeft = true;
                  } else if (itx1[0] < itx2[0]) {
                     l = p0.getLines().get((int) itx1[2]);
                     endPoint = new Coordinate(itx1[0], itx1[1]);
                     cutFromLeft = true;
                  } else {
                     l = p1.getLines().get((int) itx2[2]);
                     endPoint = new Coordinate(itx2[0], itx2[1]);
                  }

               }

            } else {
               double[] itx1 = findLowestIntersection(p0, bisector);
               double[] itx2 = findLowestIntersection(p1, bisector);

               if (itx1 == null && itx2 == null) { // RARE CASE when all points exist on same line
                  System.err.println("No intersections FOUND before exiting top bridge.\n"
                        + " Either all the lines are parallel or this is an error!");
                  p0.insertLine(bisector);
                  p1.insertLine(bisector);
                  stitch.add(bisector);

                  break;
               } else if (itx1 == null) {
                  l = p1.getLines().get((int) itx2[2]);
                  endPoint = new Coordinate(itx2[0], itx2[1]);
               } else if (itx2 == null) {
                  l = p0.getLines().get((int) itx1[2]);
                  endPoint = new Coordinate(itx1[0], itx1[1]);
                  cutFromLeft = true;
               } else if (itx1[1] < itx2[1]) {
                  l = p0.getLines().get((int) itx1[2]);
                  endPoint = new Coordinate(itx1[0], itx1[1]);
                  cutFromLeft = true;
               } else {
                  l = p1.getLines().get((int) itx2[2]);
                  endPoint = new Coordinate(itx2[0], itx2[1]);
               }
            }

         }

         // 6. add any edges that may get trimmed or deleted by this stitch line.
         // when the stitch line is complete, then we will look at all of these
         // candidates and determine if they should be deleted.
         // cut off bisector line at intersection
         bisector.setEnd(endPoint);

         // When we intersect multiple lines at the same spot. it takes multiple steps to
         // do so. Thus, multiple bisectors of length 0 end up getting created. To fix
         // this, we check if the end of this bisector is the same as the end of the last
         // one. If so, we remove the last one from the stitch and use it again. (it will
         // get put back in the stitch later)
         if (stitch.size() > 0) {
            Line lastBisector = stitch.get(stitch.size() - 1);
            if (Line.coordsEqual(bisector.getEnd(), lastBisector.getEnd())) {
               bisector.removeSelf();
               bisector = stitch.remove(stitch.size() - 1);
            }
         }

         if (cutFromLeft) {

            trim(l, bisector, endPoint, 2, leftRemovedLines);
         } else {

            trim(l, bisector, endPoint, 1, rightRemovedLines);
         }

         // give the new line to both points that share it

         p0.addStitch(bisector);

         p1.addStitch(bisector);

         // track the stitchings
         stitch.add(bisector);

         // 7. choose the next point from the same side that keeps the last voronoi edge
         // this point should be bisected by the line last intersected
         if (cutFromLeft) {

            seenLines.add(l);
            lastBisectedLine = l;
            if (l.getP0() != p0) {
               p0 = l.getP0();

            } else {
               p0 = l.getP1();

            }
            seenPoints.add(p0);

         } else {

            seenLines.add(l);
            lastBisectedLine = l;
            if (l.getP1() != p1) {
               p1 = l.getP1();

            } else {
               p1 = l.getP0();

            }
            seenPoints.add(p1);

         }

      } while (true);

      // delete any lines from right side to the left of the stitch
      checkForRemoval(stitch, leftRemovedLines, 2, seenLines);
      checkForRemoval(stitch, rightRemovedLines, 1, seenLines);

      for (Point p : seenPoints) {
         p.applyStitching(stitch);
      }

      return leftConvexHull;
   }

   // remove all lines in removedLines to the <right|left> of the stitch
   // right = 2. left = 1
   private void checkForRemoval(Vector<Line> stitching, PriorityQueue<Line> removedLines, int direction,
         HashSet<Line> seenLines) {

      Line candidate, stitch;
      int stitchIndex = 0;
      while (!removedLines.isEmpty()) {
         candidate = removedLines.poll();
         // if we saw the line that means it was intersected by a stitch and was already
         // taken care of
         if (seenLines.contains(candidate)) {
            continue;
         }
         stitch = stitching.get(stitchIndex);

         // get to the stitch section that is at the same level as the candidate line
         while (!stitch.inYBounds(candidate.getUpperY())) {
            stitchIndex++;
            stitch = stitching.get(stitchIndex);

         }

         // check if any point on the candidate line is to the left/right of the stitch
         // line
         if (direction == RIGHT && candidate.isRightOf(stitch)) {
            candidate.removeSelf();
         } else if (direction == LEFT && candidate.isLeftOf(stitch)) { // left
            candidate.removeSelf();
         }

      }

   }

   private void trim(Line l, Line bisector,
         Coordinate endPoint, int direction, PriorityQueue<Line> removedLines) {

      l.cutOffLines(direction, bisector, endPoint.getX(), removedLines);

      if (l.isHorizontal()) {
         // horizontal lines may not be oriented the correct way if they were just
         // instantiated so gotta check that
         // all horizontal lines start out oriented from right to left.
         // So, if we are cutting off the right side we should reorient from left to
         // right
         if (direction == RIGHT && l.unbounded()) {
            l.horizontalTowardsRight();
         }
      }
      // bisector is always traveling from src to end points. use this to determine
      // the side to trim
      // the right of the bisector should always be anywhere 180 degrees clockwise
      // from the line (if hand is from src to end)
      // the left will always be 180 deg CCW
      double angle = Angle.angleBetweenOriented(bisector.getSrc(), endPoint, l.getEnd());
      double angle2 = Angle.angleBetweenOriented(bisector.getSrc(), endPoint, l.getSrc());

      if (angle < 0 && angle2 < 0 || angle > 0 && angle2 > 0) {
         // if both end and start points are on one side... we need to slide an unbounded
         // side out to the intersection point then bind it

         double distSrc = l.getSrc().distance(endPoint);
         double distEnd = l.getEnd().distance(endPoint);
         if (distSrc < distEnd) { // if source point of line is closer to intersection point
            l.setSrc(endPoint);
         } else {
            l.setEnd(endPoint);
         }

      } else if (direction == RIGHT) { // trim right side
         // negative angle means that the endpoint of the line is CCW of the bisector.
         // since we reverse the bisector for this calcuation, CCW is on the right

         if (angle < 0) {
            l.setSrc(endPoint);
         } else {
            l.setEnd(endPoint);
         }

      } else { // trim left side
         if (angle > 0) {
            l.setSrc(endPoint);
         } else {
            l.setEnd(endPoint);
         }

      }

   }

}
