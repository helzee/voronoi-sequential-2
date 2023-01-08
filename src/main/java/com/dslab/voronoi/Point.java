package com.dslab.voronoi;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Scanner;

public class Point implements Writable {

   private int x;
   private int y;

   public Point(int i, int j) {
      x = i;
      y = j;
   }

   public String toString() {
      return "" + x + " " + y;
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

   public void readFields(DataInput in) throws IOException {
      x = in.readInt();
      y = in.readInt();
   }

   public static Point read(DataInput in) throws IOException {
      Point p = new Point();
      p.readFields(in);
      return p;
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

   public boolean above(Point p) {
      return y > p.getY();
   }

}
