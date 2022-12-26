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

   public ConvexHull(Vector<Point> points) {
      this.points = divide(points, 0, points.size() - 1).getPoints();
   }

   private static ConvexHull divide(Vector<Point> points, int left, int right) {
      int size = right - left + 1; // + 1 because converting last index to size

      if (size > 2) {
         int mid = left + size / 2;
         ConvexHull leftConvexHull = divide(points, left, mid - 1);
         ConvexHull rightConvexHull = divide(points, mid, right);
         leftConvexHull.merge(rightConvexHull);
         return leftConvexHull;
         // if we comput convex hull to reduce time complexity, could do it after we get
         // each ConvexHull. Take right convex hull of left ConvexHull and left CV of
         // right
         // ConvexHull. then send those to stitch
      } else {
         // base case
         if (size == 2) {
            // draw a line
            Point p0 = points.elementAt(left);
            Point p1 = points.elementAt(right);

            return new ConvexHull(p0, p1);

         }

      }

      return new ConvexHull(points.elementAt(left));

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

   public void draw() {
      Point prev = points.get(points.size() - 1);
      for (Point p : points) {
         prev.connect(p);
         prev = p;
      }
   }

   Point getLeftMostPoint() {
      Point leftmost = getBottomPoint();
      for (Point p : points) {
         if (leftmost.getX() > p.getX()) {
            leftmost = p;
         }
      }
      return leftmost;
   }

   Point getRightMostPoint() {
      Point rightmost = getBottomPoint();
      for (Point p : points) {
         if (rightmost.getX() < p.getX()) {
            rightmost = p;
         }
      }
      return rightmost;
   }

   int orientation(Point a, Point b, Point c) {
      double res = (b.getY() - a.getY()) * (c.getX() - b.getX()) - (c.getY() - b.getY()) * (b.getX() - a.getX());

      if (-0.000001 < res && res < 0.000001) {
         return 0;
      }
      if (res > 0) {
         return 1;
      }
      return -1;
   }

   private static int modulo(int a, int b) {
      if (a % b < 0) {
         return b + (a % b);
      } else {
         return a % b;
      }
   }

   /**
    * 
    * @param left
    * @param right
    * @param ia
    * @param ib
    * @return bridge points in counterclockwise direction (upper bridge is
    *         right->left and lower bridge is left->right)
    */
   private Vector<Integer> getBridge(ConvexHull left, ConvexHull right, int ia, int ib) {
      boolean done = false;
      while (!done) {
         done = true;
         // 3. move point b clockwise up the right convex hull until it is tangent to
         // right CH

         while (right.size() > 1 && 0 < orientation(right.getPoints().get(ib), left.points.get(ia),
               right.getPoints().get(modulo((ib - 1), right.size())))) {
            ib = modulo((ib - 1), right.size());
         }
         // 3. move point b clockwise up the right convex hull until it is tangent to
         // right CH

         // 4. move point a ccw up the left CH until it is tangent to left CH

         while (left.size() > 1 && 0 > orientation(
               left.points.get(ia), right.getPoints().get(ib),
               left.points.get((ia + 1) % left.size()))) {
            ia = (ia + 1) % left.size();
            // 5. if line segment now intersects the right CH, go back to step 3
            done = false;
         }

      }
      Vector<Integer> bridge = new Vector<>();
      bridge.add(ib);
      bridge.add(ia);

      return bridge;
   }

   /*
    * Merge 2 convex hulls that are left and right of eachother. . The merged CH is
    * the left convex hull.
    * 
    * ALWAYS called merge from left convex hull.
    */
   public Vector<Vector<Point>> merge(ConvexHull right) {

      // merge based off this algorithm
      // https://iq.opengenus.org/divide-and-conquer-convex-hull/#:~:text=The%20key%20idea%20is%20that,results%20to%20a%20complete%20solution.
      // 1. find leftmost point b of right and rightmost point a of left
      Point leftmost = right.getLeftMostPoint();
      Point rightmost = this.getRightMostPoint();
      // 2. create a line segment from a to b
      // Vector<Point> topBridge = new Vector<>();
      // topBridge.add(leftmost);
      // topBridge.add(rightmost);
      // Vector<Point> botBridge = new Vector<>();
      // botBridge.add(leftmost);
      // botBridge.add(rightmost);

      int ia = points.indexOf(rightmost);
      int ib = right.getPoints().indexOf(leftmost);

      // get upper bridge
      Vector<Integer> upperBridge = getBridge(this, right, ia, ib);

      // in the case that the starting points are a tanget, we want to ensure the
      // lower bridge will not be the same as the upper bridge
      if (upperBridge.get(0) == ib && upperBridge.get(1) == ia) {
         // this accounts for the case where left or right side is size 1,
         if (this.size() > right.size()) {
            ia = modulo((ia - 1), this.size());
         } else {
            ib = (ib + 1) % right.size();
         }
      }

      // get lower bridge (use same function with backwards inputs)
      Vector<Integer> lowerBridge = getBridge(right, this, ib, ia);

      // go through the convex hull. add the bridge points.. points at the top and
      // bottom of each stitching

      int lowerLeftIndex = lowerBridge.get(0);
      int upperLeftIndex = upperBridge.get(1);
      int lowerRightIndex = lowerBridge.get(1);
      int upperRightIndex = upperBridge.get(0);
      Vector<Point> newHull = new Vector<>();

      for (int i = lowerRightIndex;; i = (i + 1) % right.size()) {
         newHull.add(right.points.get(i));
         if (i == upperRightIndex) {
            break;
         }
      }

      for (int i = upperLeftIndex;; i = (i + 1) % this.size()) {
         newHull.add(points.get(i));
         if (i == lowerLeftIndex) {
            break;
         }
      }

      Vector<Point> leftBridge = new Stack<>();

      Vector<Point> rightBridge = new Stack<>();
      leftBridge.add(points.get(lowerLeftIndex));
      leftBridge.add(points.get(upperLeftIndex));
      rightBridge.add(right.points.get(lowerRightIndex));
      rightBridge.add(right.points.get(upperRightIndex));

      organizeBridges(leftBridge, rightBridge);
      checkIfPointsOnLine(leftBridge, rightBridge);

      Vector<Vector<Point>> bridges = new Vector<>();
      bridges.add(leftBridge);
      bridges.add(rightBridge);

      this.points = newHull;

      return bridges;
   }

   // we want the bottommost entrance to the convex hull to be at position 0
   // can't use height to find this. must use angle. whichever bridge has an angle
   // closer to 90 degrees from x axis is better
   private static void organizeBridges(Vector<Point> leftBridge, Vector<Point> rightBridge) {

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
   private static void bridgeCaseParallel(Vector<Point> leftBridge, Vector<Point> rightBridge) {

      Point closestLeftPoint = leftBridge.get(leftBridge.size() - 1);
      Point closestRightPoint = rightBridge.get(rightBridge.size() - 1);
      for (Point l : leftBridge) {
         for (Point r : rightBridge) {
            if (closestLeftPoint.distance(closestRightPoint) > l.distance(r)) {
               closestLeftPoint = l;
               closestRightPoint = r;
            }
         }
      }
      if (leftBridge.get(leftBridge.size() - 1) == closestLeftPoint && leftBridge.size() == 2) {
         Collections.swap(leftBridge, 0, 1);
      }
      if (rightBridge.get(rightBridge.size() - 1) == closestRightPoint && rightBridge.size() == 2) {
         Collections.swap(rightBridge, 0, 1);
      }

   }

   private static void checkIfPointsOnLine(Vector<Point> leftBridge, Vector<Point> rightBridge) {
      Line botBridge = new Line(leftBridge.get(0).getCoordinate(), rightBridge.get(0).getCoordinate());

      if (leftBridge.size() == 2 && botBridge.containsPoint(leftBridge.get(1))) {
         Collections.swap(leftBridge, 0, 1);
      }
      if (rightBridge.size() == 2 && botBridge.containsPoint(rightBridge.get(1))) {
         Collections.swap(rightBridge, 0, 1);
      }

   }

   private static boolean allPointsOnLine(Vector<Point> leftBridge, Vector<Point> rightBridge) {

      Line line = new Line(leftBridge.get(leftBridge.size() - 1).getCoordinate(), rightBridge.get(
            rightBridge.size() - 1).getCoordinate());
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

   private static void bridgeCaseOfFour(Vector<Point> leftBridge, Vector<Point> rightBridge) {
      Point leftBridgeA1 = leftBridge.remove(leftBridge.size() - 1);
      Point leftBridgeB1 = leftBridge.remove(leftBridge.size() - 1);
      Point rightBridgeA2 = rightBridge.remove(rightBridge.size() - 1);
      Point rightBridgeB2 = rightBridge.remove(rightBridge.size() - 1);
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

   private static void bridgeCaseOfThree(Vector<Point> smallBridge, Vector<Point> largeBridge) {
      Point origin = smallBridge.get(smallBridge.size() - 1);
      PriorityQueue<Point> sortedPoints = new PriorityQueue<Point>((Point a, Point b) -> {
         double angleA = Math.atan2(a.getY() - origin.getY(), a.getX() - origin.getX());
         double angleB = Math.atan2(b.getY() - origin.getY(), b.getX() - origin.getX());
         double diffA = Math.abs(angleA - (Math.PI / 2));
         double diffB = Math.abs(angleB - (Math.PI / 2));
         return diffA > diffB ? -1 : 1;
      });
      sortedPoints.add(largeBridge.remove(largeBridge.size() - 1));
      sortedPoints.add(largeBridge.remove(largeBridge.size() - 1));
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
