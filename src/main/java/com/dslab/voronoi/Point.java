package com.dslab.voronoi;

import org.apache.commons.text.StringTokenizer;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;
import java.util.Scanner;

public class Point implements Serializable, Comparable<Point> {

   private int x;
   private int y;

   public Point(int i, int j) {
      x = i;
      y = j;
   }

   public Point(StringTokenizer itr) {
      x = Integer.parseInt(itr.nextToken());
      y = Integer.parseInt(itr.nextToken());
   }

   @Override
   public String toString() {
      return "" + x + ", " + y;
   }

   @Override
   public int compareTo(Point other) {

      return this.x - other.getX();
   }

   public Text write() {
      return new Text("" + x + " " + y + " ");
   }

   public Point() {

   }

   public Point(double x, double y) {
      this((int) x, (int) y);
   }

   public void write(DataOutput out) throws IOException {
      out.writeInt((int) getX());
      out.writeInt((int) getY());
   }

   public Point(Scanner input) {
      x = input.nextInt();
      y = input.nextInt();
   }

   public String print() {
      return "" + getX() + ", " + getY();
   }

   public int getY() {
      return y;
   }

   public int getX() {
      return x;
   }

   public void setY(int y) {
      this.y = y;
   }

   public void setX(int x) {
      this.x = x;
   }

   public boolean above(Point p) {
      return y > p.getY();
   }

}
