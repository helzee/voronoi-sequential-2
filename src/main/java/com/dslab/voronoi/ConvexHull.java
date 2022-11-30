package com.dslab.voronoi;

import java.util.Collections;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.SortedSet;
import java.util.Stack;
import java.util.Vector;

import org.locationtech.jts.algorithm.Angle;
import org.locationtech.jts.geom.LineSegment;

/**
 * This Convex Hull class is specific to the voronoi algorithm.
 * It can only be constructed from 1 or 2 points. Any more points need to be
 * added through merges
 */
public class ConvexHull {

   // head of list is
   // bottom-most point of convex hull. The list iterates clockwise through the
   // hull with the last point in the list being counter-clockwise next to the
   // bottom-most point
   private Vector<Point> points;

   public Vector<Point> getPoints() {
      return points;
   }

   public int size() {
      return points.size();
   }

   public ConvexHull(Point p) {

      points = new Vector<Point>();
      points.add(p);

   }

   // add points in correct order
   public ConvexHull(Point p1, Point p2) {
      points = new Vector<Point>();
      if (p1.above(p2)) {
         points.add(p2);
         points.add(p1);

      } else {
         points.add(p1);
         points.add(p2);

      }

   }

   public Point getBottomPoint() {
      return points.get(0);
   }

   /*
    * Merge 2 convex hulls that are left and right of eachother. . The merged CH is
    * the left convex hull. This method
    * returns two linked lists which represent
    * the points removed from each convex hull in order of low to high. Each list
    * also contains a point from the new convex hull representing entrance and exit
    * points into the stitching (Note: not
    * lowest to highest. sometimes the points removed can start higher than the
    * lowest point on the CH)
    * The merge utilizes grahams algorithm
    * 
    * ALWAYS called merge from left convex hull.
    */
   public Vector<Stack<Point>> merge(ConvexHull right) {

      // 1. find the lowest point of each convex hull. Choose the lowest one (and
      // leftmost if both are same height) as origin. remove the origin from its
      // original convex hull
      Point origin;
      if (this.getBottomPoint().getY() - right.getBottomPoint().getY() < 0.000001) {
         origin = this.getBottomPoint();

      } else {
         origin = right.getBottomPoint();

      }
      HashSet<Point> leftCH = new HashSet<>();
      HashSet<Point> rightCH = new HashSet<>();
      leftCH.addAll(this.points);
      rightCH.addAll(right.getPoints());

      // EDGE CASE: check if one of thte hulls is empty after getting origin

      // 2. Sort all points based on their polar angle from the origin point.
      // I created a comparator function in this function so that it can access the
      // ORIGIN
      PriorityQueue<Point> sortedPoints = new PriorityQueue<Point>((Point a, Point b) -> {
         double distA = a.distance(origin);
         double distB = b.distance(origin);
         double polarA = polar_angle(origin, a, distA);
         double polarB = polar_angle(origin, b, distB);
         if (Math.abs(polarA - polarB) < 0.00001) { // check if equal
            // if both points are left, then we want farthest one first (since we travers
            // counterclockwise)
            if (leftCH.contains(a) && leftCH.contains(b)) {
               return distA > distB ? -1 : 1;
            } else if (leftCH.contains(a) && rightCH.contains(b)) {
               return 1;
            } else if (rightCH.contains(a) && leftCH.contains(b)) {
               return -1;
            } else { // both in rightCH
               return distA > distB ? 1 : -1;
            }
         } else if (polarA > polarB) {
            return 1;
         } else { // polarA < polarB
            return -1;
         }
      });
      // As we sort points into priority queue, Track the leftmost and rightmost point
      // coords of each CH.

      for (Point p : this.points) {
         if (p != origin) {
            sortedPoints.add(p);

         }

      }

      for (Point p : right.points) {
         if (p != origin) {
            sortedPoints.add(p);
         }

      }

      // 3. Now that points are sorted. Run the graham algorithm.
      graham(sortedPoints, origin);
      // add the origin to this convex hull
      this.points.add(0, origin);
      // Within the graham algorithm, store discarded points from each CH in their
      // own priority queue ordered based on lowest y value. Return the new convex
      // hull as a vector
      // Replace this convex hull (the left one) with the new one. Return the two
      // priority queues

      // go through the convex hull. add the bridge points.. points at the top and
      // bottom of each stitching

      Stack<Point> leftBridge = new Stack<>();

      Stack<Point> rightBridge = new Stack<>();

      for (int i = 1; i < points.size() + 1; i++) {
         Point a = points.get(i - 1);
         Point b = points.get(i % points.size());
         if (leftCH.contains(a) && rightCH.contains(b)) {
            // point a is in left side and b is in right side
            if (leftBridge.isEmpty() || leftBridge.peek() != a) {
               leftBridge.add(a);
            }
            if (rightBridge.isEmpty() || rightBridge.peek() != b) {
               rightBridge.add(b);
            }

         } else if (rightCH.contains(a) && leftCH.contains(b)) {
            // point a is in right side and b is in left side
            if (leftBridge.isEmpty() || leftBridge.peek() != b) {
               leftBridge.add(b);
            }
            if (rightBridge.isEmpty() || rightBridge.peek() != a) {
               rightBridge.add(a);
            }

         }
      }

      organizeBridges(leftBridge, rightBridge);

      // check if any points are on the top bridge segment or bottom bridge segment
      checkIfPointsOnLine(leftBridge, rightBridge);
      // now the bridges are added to the end of the stitching. This represents the
      // top of the stitching. We now need to move the lower bridge to the bottom of
      // the stitching
      Vector<Stack<Point>> bridges = new Vector<>();
      bridges.add(leftBridge);
      bridges.add(rightBridge);

      return bridges;
   }

   // we want the bottommost entrance to the convex hull to be at position 0
   // can't use height to find this. must use angle. whichever bridge has an angle
   // closer to 90 degrees from x axis is better
   private static void organizeBridges(Stack<Point> leftBridge, Stack<Point> rightBridge) {

      if (allPointsOnLine(leftBridge, rightBridge)) {
         bridgeCaseParallel(leftBridge, rightBridge);
      } else if (leftBridge.size() == 1 && rightBridge.size() == 2) {
         // special case for when 1 bridge is size 1 and other is size 2
         bridgeCaseOfThree(leftBridge, rightBridge);
      } else if (leftBridge.size() == 2 && rightBridge.size() == 1) {
         bridgeCaseOfThree(rightBridge, leftBridge);
      } else { // 4 points, 2 bridges
         bridgeCaseOfFour(leftBridge, rightBridge);

      }
   }

   // find the closest points from each bridge. these are the only points we will
   // need for the merge so discard rest
   private static void bridgeCaseParallel(Stack<Point> leftBridge, Stack<Point> rightBridge) {

      Point closestLeftPoint = leftBridge.peek();
      Point closestRightPoint = rightBridge.peek();
      for (Point l : leftBridge) {
         for (Point r : rightBridge) {
            if (closestLeftPoint.distance(closestRightPoint) > l.distance(r)) {
               closestLeftPoint = l;
               closestRightPoint = r;
            }
         }
      }
      if (leftBridge.peek() == closestLeftPoint && leftBridge.size() == 2) {
         Collections.swap(leftBridge, 0, 1);
      }
      if (rightBridge.peek() == closestRightPoint && rightBridge.size() == 2) {
         Collections.swap(rightBridge, 0, 1);
      }

   }

   private static void checkIfPointsOnLine(Stack<Point> leftBridge, Stack<Point> rightBridge) {
      Line botBridge = new Line(leftBridge.get(0).getCoordinate(), rightBridge.get(0).getCoordinate());

      if (leftBridge.size() == 2 && botBridge.containsPoint(leftBridge.get(1))) {
         Collections.swap(leftBridge, 0, 1);
      }
      if (rightBridge.size() == 2 && botBridge.containsPoint(rightBridge.get(1))) {
         Collections.swap(rightBridge, 0, 1);
      }

   }

   private static boolean allPointsOnLine(Stack<Point> leftBridge, Stack<Point> rightBridge) {

      Line line = new Line(leftBridge.peek().getCoordinate(), rightBridge.peek().getCoordinate());
      for (Point p : leftBridge) {
         if (!line.containsPoint(p)) {
            return false;
         }
      }
      for (Point p : rightBridge) {
         if (!line.containsPoint(p)) {
            return false;
         }
      }
      return true;
   }

   private static void bridgeCaseOfFour(Stack<Point> leftBridge, Stack<Point> rightBridge) {
      Point leftBridgeA1 = leftBridge.pop();
      Point leftBridgeB1 = leftBridge.pop();
      Point rightBridgeA2 = rightBridge.pop();
      Point rightBridgeB2 = rightBridge.pop();
      LineSegment bridgeA = new LineSegment(leftBridgeA1.getCoordinate(), rightBridgeA2.getCoordinate());
      LineSegment bridgeB = new LineSegment(leftBridgeB1.getCoordinate(), rightBridgeB2.getCoordinate());

      // I oriented each bridge so that they start from left and end at right
      // this means that from the perspective of each bridge, the rightmost bridge of
      // the two will always be the lower one
      int orientationOfB = bridgeA.orientationIndex(bridgeB); // returns 1 if B is left of A
      if (orientationOfB == 1) { // bridge A is rightmost (bottom)
         leftBridge.add(leftBridgeA1);
         leftBridge.add(leftBridgeB1);
         rightBridge.add(rightBridgeA2);
         rightBridge.add(rightBridgeB2);
      } else {
         leftBridge.add(leftBridgeB1);
         leftBridge.add(leftBridgeA1);
         rightBridge.add(rightBridgeB2);
         rightBridge.add(rightBridgeA2);

      }

   }

   // in this special case, we want to find which of the two points from the larger
   // bridge is closer to 90 degrees with respect to the single bridge point
   // This is because it is already a given that the two large bridge points will
   // both either be on the right or the left of the small bridge point
   // The reason we need to do this calculation is because sometimes the top bridge
   // in the scenario actually contains the lower point of the large bridge (
   // because even though the point is lower, the bridge it creates is above that
   // of the other point that is higher up )

   // https://math.stackexchange.com/questions/707673/find-angle-in-degrees-from-one-point-to-another-in-2d-space

   private static void bridgeCaseOfThree(Stack<Point> smallBridge, Stack<Point> largeBridge) {
      Point origin = smallBridge.peek();
      PriorityQueue<Point> sortedPoints = new PriorityQueue<Point>((Point a, Point b) -> {
         double angleA = Math.atan2(a.getY() - origin.getY(), a.getX() - origin.getX());
         double angleB = Math.atan2(b.getY() - origin.getY(), b.getX() - origin.getX());
         double diffA = Math.abs(angleA - (Math.PI / 2));
         double diffB = Math.abs(angleB - (Math.PI / 2));
         return diffA > diffB ? -1 : 1;
      });
      sortedPoints.add(largeBridge.pop());
      sortedPoints.add(largeBridge.pop());
      largeBridge.add(sortedPoints.poll());
      largeBridge.add(sortedPoints.poll());
   }

   private static void swap(Stack<Point> stack) {
      if (stack.size() < 2) {
         return;
      }
      Point temp = stack.get(0);
      stack.set(0, stack.get(1));
      stack.set(1, temp);
   }

   /**
    * Performs the graham algorithm that scan all points from the bottom as
    * revolving a line leftward and eliminating non-convex points.
    *
    * @param q             a priority queue of points sorted by polar angle from
    *                      origin
    * @param rightBoundary the rightmost point's xvalue in the left side
    * @return two priority queues containing the discarded poitns from each side,
    *         sorted from lowest to highest
    */
   void graham(PriorityQueue<Point> q, Point origin) {

      Vector<Point> hull = new Vector<Point>();

      Point last2 = null, last1 = null, next = null;
      double prev_polar_angle = polar_angle(origin, q.peek(), origin.distance(q.peek()));

      // choose the first three points as convex-hull points.
      for (int i = 0; i < 2 && q.size() > 0; i++) {
         last2 = last1;
         last1 = q.poll();
         hull.add(last1);
         // q.remove(0);
      }

      while (q.size() > 0) {
         next = q.poll();

         while ((prev_polar_angle = leftturn(last2, last1, next, prev_polar_angle)) < 0) {
            // remove last1 that is no longer a convex point
            hull.remove(hull.size() - 1);

            if (hull.size() < 2) // check for excpetions
               break;
            // back-track to see previous points were actually concave.
            last1 = hull.elementAt(hull.size() - 1);
            last2 = hull.elementAt(hull.size() - 2);
         }

         // now advance to the next point.

         // if the second point is 180 degrees from the first point relative to the
         // previous polar angle
         // we need to figure out which point should get added to the hull first
         hull.add(next);
         if (prev_polar_angle == 0.0) {
            // if the next point is closer to our chosen origin point than the previous one,
            // we should swap them in the hull
            if (last2.distance(next) < last2.distance(last1)) {
               Collections.swap(hull, hull.size() - 1, hull.size() - 2);
            }

         }
         last2 = hull.get(hull.size() - 2);
         last1 = hull.get(hull.size() - 1);
      }

      q.addAll(hull);

      // replace the old left side's convex hull with the new one
      this.points = hull;

   }

   /**
    * Checks if B is hit by line that revolves left on A to B
    *
    * @param a the central point of a left-revovling line AC
    * @param b the point to check
    * @param c the destination to revolve line AC
    * @param p a's polar angle
    * @return b's polar angle if B is it by line AC that revolves left on A to B.
    *         Otherwise -1
    */
   double leftturn(Point a, Point b, Point c, double pa) {
      double distance = 0.0; // needed for polar_angle
      double pb = polar_angle(a, b, distance);
      double pc = polar_angle(a, c, distance);
      if (Math.abs(pb + pc) < 0.000001) {
         return 0;
      }
      // pb >= pa, pc >= pb
      return (pb - pa > -0.000001 && pc - pb > -0.000001) ? pb : -1;
   }

   boolean equals(double a, double b) {
      return Math.abs(a - b) < 0.000001;
   }

   /**
    * Checks if B is hit by line that revolves left on A to B
    *
    * @param a the central point of a left-revovling line AC
    * @param b the point to check
    * @param c the destination to revolve line AC
    * @param p a's polar angle
    * @return b's polar angle if B is it by line AC that revolves left on A to B.
    *         Otherwise -1
    */
   double correctTurn(Point a, Point b, Point c, double pa) {

      double pb = Angle.angle(a.getCoordinate(), b.getCoordinate());
      double pc = Angle.angle(a.getCoordinate(), c.getCoordinate());
      return (pb > pa && pc > pb) ? pb : -1;
   }

   public static boolean greaterThan(double a, double b) {
      return a - b > -0.000001;
   }

   /**
    * Computes the polar angle and distance between the origin o and the
    * the other point p.
    *
    * @param o        the orign point
    * @param p        the other point
    * @param distance the distance between o and p (to be returned)
    * @return the polar angle
    */
   public static double polar_angle(Point o, Point p, double distance) {
      double radian;
      double sin = 0.0;
      double arcsin = 0.0;
      if (o.getX() == p.getX() && o.getY() == p.getY()) {
         distance = 0;
         radian = 0;
      } else {
         int x = (int) (p.getX() - o.getX());
         int y = (int) (p.getY() - o.getY());

         distance = Math.sqrt(Math.pow(x, 2.0) +
               Math.pow(y, 2.0));
         sin = Math.abs(y / distance);
         arcsin = Math.asin(sin);
         if (x >= 0 && y >= 0) {
            // NE [+x, +y] (0 - 89)
            radian = arcsin;
         } else if (x < 0 && y >= 0) {
            // NW [-x, +y] (90 - 179)
            radian = Math.PI - arcsin;
         } else if (x < 0 && y < 0) {
            // SW [-x, -y] (180 - 269)
            radian = arcsin + Math.PI;
         } else {
            // SE [+x, -y] (270 - 359)
            radian = Math.PI * 2 - arcsin;
         }
      }

      return radian;
   }

}
