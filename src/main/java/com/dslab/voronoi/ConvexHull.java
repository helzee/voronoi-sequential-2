package com.dslab.voronoi;

import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.SortedSet;
import java.util.Stack;
import java.util.Vector;

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
      if (this.getBottomPoint().getY() <= right.getBottomPoint().getY()) {
         origin = this.getBottomPoint();

      } else {
         origin = right.getBottomPoint();

      }

      // EDGE CASE: check if one of thte hulls is empty after getting origin

      // 2. Sort all points based on their polar angle from the origin point.
      // I created a comparator function in this function so that it can access the
      // ORIGIN
      PriorityQueue<Point> sortedPoints = new PriorityQueue<Point>((Point a, Point b) -> {
         double distA = 0.0;
         double distB = 0.0;
         double polarA = polar_angle(origin, a, distA);
         double polarB = polar_angle(origin, b, distB);
         if (polarA < polarB) {
            return -1;
         } else if (polarA > polarB) {
            return 1;
         } else {
            return distA < distB ? -1 : 1;
         }
      });
      // As we sort points into priority queue, Track the leftmost and rightmost point
      // coords of each CH.

      HashSet<Point> leftCH = new HashSet<>();
      for (Point p : this.points) {
         if (p != origin) {
            sortedPoints.add(p);

         }
         leftCH.add(p);

      }
      HashSet<Point> rightCH = new HashSet<>();
      for (Point p : right.points) {
         if (p != origin) {
            sortedPoints.add(p);
         }
         rightCH.add(p);
      }

      // 3. Now that points are sorted. Run the graham algorithm.
      graham(sortedPoints);
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

      // now the bridges are added to the end of the stitching. This represents the
      // top of the stitching. We now need to move the lower bridge to the bottom of
      // the stitching
      Vector<Stack<Point>> bridges = new Vector<>();
      bridges.add(leftBridge);
      bridges.add(rightBridge);

      return bridges;
   }

   // we want the bottommost entrance to the convex hull to be at position 0
   private static void organizeBridges(Stack<Point> leftBridge, Stack<Point> rightBridge) {
      Point botLeft = leftBridge.get(0);
      Point botRight = rightBridge.get(0);
      if (leftBridge.size() > 1) {
         if (leftBridge.get(0).getY() < leftBridge.get(1).getY()) {
            botLeft = leftBridge.get(0);
         } else {
            botLeft = leftBridge.get(1);
         }
      }

      if (rightBridge.size() > 1) {
         if (rightBridge.get(0).getY() < rightBridge.get(1).getY()) {
            botRight = rightBridge.get(0);
         } else {
            botRight = rightBridge.get(1);
         }
      }

      if (botLeft.getY() <= botRight.getY() && botLeft != leftBridge.get(0)) {
         swap(leftBridge);
         swap(rightBridge);

      } else if (botRight != rightBridge.get(0)) {
         swap(leftBridge);
         swap(rightBridge);
      }

      // special case for when 1 bridge is size 1 and other is size 2
      if (leftBridge.size() == 1 && rightBridge.size() == 2) {
         bridgeCaseOfThree(leftBridge, rightBridge);
      } else if (leftBridge.size() == 2 && rightBridge.size() == 1) {
         bridgeCaseOfThree(rightBridge, leftBridge);
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
   void graham(PriorityQueue<Point> q) {

      Vector<Point> hull = new Vector<Point>();

      Point last2 = null, last1 = null, next = null;

      // choose the first three points as convex-hull points.
      for (int i = 0; i < 2 && q.size() > 0; i++) {
         last2 = last1;
         last1 = q.poll();
         hull.add(last1);
         // q.remove(0);
      }

      double prev_polar_angle = 0.0;
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
         last2 = hull.lastElement();
         last1 = next;
         hull.add(next);
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
      return (pb > pa && pc > pb) ? pb : -1;
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
