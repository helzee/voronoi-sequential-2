package com.dslab.voronoi;

import java.util.Vector;
import java.util.Random;

public class Driver {
   public static void main(String[] args) {
      int N = 0;

      // validate args
      if (args.length > 0) {
         try {
            N = Integer.parseInt(args[0]);
         } catch (Exception e) {
         }
      }
      // if (N <= 0 || N > 1000000) {
      // System.err.println("usage: java Driver #points");
      // System.err.println(" where 0< #points <= 1000000"); // up to 1M
      // System.exit(-1);
      // }

      // generate N points
      Random rand = new Random(10); // 10
      Vector<Point> points = new Vector<Point>();

      // space should be always 1250 x 1250
      // int size = 3000;
      int size = N;
      int[][] map = new int[size][size];

      for (int i = 0; i < N; i++) {

         do {
            int x = i;

            int y = rand.nextInt(size);
            if (map[x][y] != 1) {
               map[x][y] = 1;
               break;
            }
         } while (true);
      }

      // TODO: get this case working
      int sides = 8;
      double r = 80;
      double offset = 0.5;
      // for (int i = 0; i < sides; i++) {
      // map[(int) (r * Math.cos(i * ((2 * Math.PI) / sides) + offset))
      // + 500][(int) (r * Math.sin(i * ((2 * Math.PI) / sides) + offset))
      // + 500] = 1;
      // }
      // map[10][10] = 1;
      // map[10][20] = 1;
      // map[20][10] = 1;
      // map[20][20] = 1;
      // map[15][7] = 1;
      // map[200][100] = 1;
      // map[200][700] = 1;
      // map[300][100] = 1;
      // map[400][200] = 1;
      // map[150][150] = 1;

      // map[100][800] = 1;
      // map[100][802] = 1;
      // map[200][200] = 1;
      // map[800][200] = 1;
      // map[800][300] = 1;
      // map[800][400] = 1;
      // map[800][500] = 1;
      // map[900][100] = 1;
      // map[700][100] = 1;
      // map[800][100] = 1;
      // map[100][100] = 1;
      // map[300][200] = 1;
      // generate points on map[x][y] == 1
      for (int x = 0; x < size; x++)
         for (int y = 0; y < size; y++)
            if (map[x][y] == 1) {
               points.add(new Point(x, y));
            }

      VoronoiGraphics vg = new VoronoiGraphics(size, size, points);
      Thread graphics = new Thread(vg);
      graphics.start();
      // they are sorted in x and then in y
      // visualize the diagram: max size = 1250 x 1250
      // generate a voronoi diagram
      VoronoiDiagram voronoi = new VoronoiDiagram(50000, 50000, points);

      System.out.println("COMPLETED");

   }
}
