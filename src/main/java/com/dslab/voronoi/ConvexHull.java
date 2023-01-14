package com.dslab.voronoi;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Vector;

import javax.annotation.OverridingMethodsMustInvokeSuper;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.mapred.RecordWriter;

/**
 * This Convex Hull class is specific to the voronoi algorithm.
 * It can only be constructed from 1 or 2 points. Any more points need to be
 * added through merges
 */
public class ConvexHull implements WritableComparable<ConvexHull> {

   // the list iterates counterclockwise through the
   // hull with the last point in the list being the next point clockwise from the
   // first
   private Vector<Point> points;

   public Vector<Point> getPoints() {
      return points;
   }

   public ConvexHull(ConvexHull ch) {
      this.points = new Vector<>();
      for (Point p : ch.getPoints()) {
         // shallow copy of each point currently
         points.add(p);
      }
   }

   private void setPoints(Vector<Point> points) {
      this.points = points;
   }

   public int getXValue() {
      return points.get(0).getX();
   }

   public ConvexHull(Vector<Point> points) {
      if (points.size() > 3) {
         this.points = divide(points, 0, points.size() - 1).getPoints();
      } else {
         this.points = points;
      }

   }

   public ConvexHull(Scanner input) {
      points = new Vector<>();

      while (input.hasNextInt()) {
         points.add(new Point(input));
      }

   }

   public static ConvexHull divide(Vector<Point> points, int left, int right) {
      if (points.isEmpty()) {
         return new ConvexHull();
      }
      int size = right - left + 1; // + 1 because converting last index to size

      if (size > 2) {
         int mid = left + size / 2;
         ConvexHull leftConvexHull = divide(points, left, mid - 1);
         ConvexHull rightConvexHull = divide(points, mid, right);
         return merge(leftConvexHull, rightConvexHull);

      } else {
         // base case
         if (size == 2) {

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

   // add points bottom to top. This ordering may not longer be necessary
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

   public ConvexHull() {
      points = new Vector<>();
   }

   public ConvexHull(Point a, Point b, Point c) {
      points = new Vector<>();
      points.add(a);
      points.add(b);
      points.add(c);
   }

   // serial form of CH: [size, point1, point2, ... , pointN]
   @Override
   public void write(DataOutput out) throws IOException {
      out.writeInt(points.size());
      for (Point p : points) {
         p.write(out);
      }
   }

   public String toString() {
      String res = "";
      for (Point p : points) {
         res += p.toString() + "; ";
      }
      return res;
   }

   public Text write() throws IOException {
      Text text = new Text();
      String ch = " ";
      for (Point p : points) {
         ch += p.toString() + " ";
      }
      text.set(ch);
      return text;
   }

   @Override
   public void readFields(DataInput in) throws IOException {
      // reset this object
      this.points = new Vector<>();
      int size = in.readInt();
      for (int i = 0; i < size; i++) {
         points.add(Point.read(in));
      }

   }

   public static ConvexHull read(DataInput in) throws IOException {
      ConvexHull ch = new ConvexHull();
      ch.readFields(in);
      return ch;
   }

   public Point getStartPoint() {
      return points.get(0);
   }

   private Point getLeftMostPoint() {
      Point leftmost = getStartPoint();
      for (Point p : points) {
         if (leftmost.getX() > p.getX()) {
            leftmost = p;
         }
      }
      return leftmost;
   }

   private Point getRightMostPoint() {
      Point rightmost = getStartPoint();
      for (Point p : points) {
         if (rightmost.getX() < p.getX()) {
            rightmost = p;
         }
      }
      return rightmost;
   }

   private static int orientation(Point a, Point b, Point c) {
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
   private static Vector<Integer> getBridge(ConvexHull left, ConvexHull right, int ia, int ib) {
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
   public static ConvexHull merge(ConvexHull left, ConvexHull right) {

      // merge based off this algorithm
      // https://iq.opengenus.org/divide-and-conquer-convex-hull/#:~:text=The%20key%20idea%20is%20that,results%20to%20a%20complete%20solution.
      // 1. find leftmost point b of right and rightmost point a of left
      Point leftmost = right.getLeftMostPoint();
      Point rightmost = left.getRightMostPoint();
      // 2. create a line segment from a to b
      // Vector<Point> topBridge = new Vector<>();
      // topBridge.add(leftmost);
      // topBridge.add(rightmost);
      // Vector<Point> botBridge = new Vector<>();
      // botBridge.add(leftmost);
      // botBridge.add(rightmost);

      int ia = left.getPoints().indexOf(rightmost);
      int ib = right.getPoints().indexOf(leftmost);

      // get upper bridge
      Vector<Integer> upperBridge = getBridge(left, right, ia, ib);

      // in the case that the starting points are a tanget, we want to ensure the
      // lower bridge will not be the same as the upper bridge
      if (upperBridge.get(0) == ib && upperBridge.get(1) == ia) {
         // this accounts for the case where left or right side is size 1,
         if (left.size() > right.size()) {
            ia = modulo((ia - 1), left.size());
         } else {
            ib = (ib + 1) % right.size();
         }
      }

      // get lower bridge (use same function with backwards inputs)
      Vector<Integer> lowerBridge = getBridge(right, left, ib, ia);

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

      for (int i = upperLeftIndex;; i = (i + 1) % left.size()) {
         newHull.add(left.getPoints().get(i));
         if (i == lowerLeftIndex) {
            break;
         }
      }

      ConvexHull res = new ConvexHull();
      res.setPoints(newHull);
      return res;

   }

   @Override
   public int compareTo(ConvexHull other) {
      return this.getStartPoint().getX() - other.getStartPoint().getX();

   }

}
