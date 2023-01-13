package com.dslab.voronoi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Scanner;
import java.util.Vector;
import java.io.Console;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

public class AppTest {

  // used for manual check to ensure the sequential version works
  @Test
  public void shouldAnswerWithTrue() {
    try {
      Scanner input = new Scanner(new File("input/1000.csv"));
      Vector<Point> points = new Vector<>();
      while (input.hasNextInt()) {
        points.add(new Point(input));
      }
      ConvexHull ch = new ConvexHull(points);
      File output = new File("output/test1000");
      FileWriter writer = new FileWriter(output);
      writer.write(ch.write().toString());

      writer.close();

      File imgOut = new File("output/test1000.png");
      ConvexHullViewer.createImage(1000, ch, imgOut);

    } catch (FileNotFoundException e) {
      System.err.println("test intput file not found.");

    } catch (IOException e) {
      System.err.println("Failed to write to output file");
    }

    assertTrue(true);
  }

}
