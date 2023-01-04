package com.dslab.voronoi;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.SequenceFileOutputFormat;
import org.apache.hadoop.mapred.lib.NLineInputFormat;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.thirdparty.com.google.common.util.concurrent.ClosingFuture.Combiner;

import java.io.IOException;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Vector;

public class Driver {
   // for maximum performance an initial convex hull size that takes full advantage
   // of the heap space for a single threaded java program. Each job requires time
   // to create and destroy it, so minimizing the amount of "recursive calls" we
   // need to make (AKA jobs) saves us time. No point in creating a bunch of jobs
   // for merging convex hulls of small sizes
   private static int INITIAL_CH_SIZE = 1024;

   // AKA number of jobs to complete the D and Q algorithm
   private static int RECURSIVE_DEPTH;
   private static Path input;
   private static Path output;
   private static int SIZE;
   private static int NUM_MACHINES = 1;

   // 4 CPUs * 2 cores * 2 threads
   private static final int NUM_THREADS = 16;

   // args: [Npoints, input path, output path, Nmachines]
   public static void main(String[] args) throws Exception {
      // number of recursions (mapreduce jobs) is equal to log_base_2(total_points /
      // initial_size_of_CH) rounded up
      SIZE = Integer.parseInt(args[0]);
      NUM_MACHINES = Integer.parseInt(args[3]);
      INITIAL_CH_SIZE = SIZE / (NUM_THREADS * NUM_MACHINES);
      RECURSIVE_DEPTH = (int) Math.ceil((Math.log((double) SIZE / INITIAL_CH_SIZE) / Math.log(2)));
      input = new Path(args[1]);
      output = new Path(args[2]);

      // INITIAL JOB ---
      Configuration config = new Configuration();
      Job job = Job.getInstance(config, "Convex Hull Init");
      job.setJarByClass(Driver.class);

      // output <k,v> = <CH#, {CH points}>
      job.setOutputKeyClass(IntWritable.class);
      job.setOutputValueClass(Point.class);

      job.setMapperClass(PointMapper.class);
      job.setReducerClass(PointReducer.class);
      FileInputFormat.addInputPath(job, input);
      // intermediate output
      FileOutputFormat.setOutputPath(job, new Path(output.toString() + 0));
      job.waitForCompletion(true);

      // RECURSIVE JOB ---

      // iterate jobs to replicate recursion for Divide and conquer
      for (int i = 0; i < RECURSIVE_DEPTH; i++) {
         config = new Configuration();
         job = Job.getInstance(config, "Convex Hull Recursion");
         job.setJarByClass(Driver.class);
         job.setMapperClass(ConvexHullMapper.class);
         job.setReducerClass(ConvexHullReducer.class);

         job.setOutputKeyClass(IntWritable.class);
         job.setOutputValueClass(ConvexHull.class);

         // intermediate output goes back and forth between a file ending in 0 to a file
         // ending 1. These files have the same path as the output
         FileInputFormat.addInputPath(job, new Path(output.toString() + (i % 2)));
         if (i == RECURSIVE_DEPTH - 1) { // last iteration. write output to specified file
            FileOutputFormat.setOutputPath(job, output);
         } else {
            FileOutputFormat.setOutputPath(job, new Path(output.toString() + ((i + 1) % 2)));
         }

         job.waitForCompletion(true);
      }

   }

   // turn list of x,y values sorted by x value into list of points.
   // keys (the group var) are a function of x value, where each key should have
   // 1-3 points
   public static class ConvexHullMapper extends Mapper<Object, Text, IntWritable, ConvexHull> {

      private ConvexHull ch;
      private IntWritable group;

      @Override
      public void map(Object key, Text value, Context context) throws IOException, InterruptedException {

         // get point from input file line of coords
         Scanner reader = new Scanner(value.toString());
         ch = new ConvexHull(reader);

         // group convex hulls into groups of 2 using key
         group = new IntWritable((int) (ch.getXValue() / 2));
         context.write(group, ch);

      }
   }

   public static class ConvexHullReducer extends Reducer<IntWritable, ConvexHull, IntWritable, ConvexHull> {
      private ConvexHull left;

      // reduce all CH into one CH from left to right
      @Override
      public void reduce(IntWritable key, Iterable<ConvexHull> values, Context context)
            throws IOException, InterruptedException {

         // This should only reduce two convex hulls at a time, but it is written to
         // allow for more
         int counter = 0;
         for (ConvexHull right : values) {
            if (counter == 0) {
               left = right;
               counter++;
               continue;
            }
            left = ConvexHull.merge(left, right);
            counter++;
         }

         context.write(key, left);

      }

   }

   // as part of the first job, we need to group a bunch of points into convex
   // hulls using the initial CH size. This will save us many recursive steps and
   // time spent tearing down and creatin jobs
   public static class PointMapper extends Mapper<Object, Text, IntWritable, Point> {
      private Point point;
      private IntWritable group;

      @Override
      public void map(Object key, Text value, Context context) throws IOException, InterruptedException {

         // get point from input file line of coords
         Scanner reader = new Scanner(value.toString());
         point = new Point(reader);

         // group points into groups of a set size
         group = new IntWritable((int) (point.getX() / INITIAL_CH_SIZE));
         context.write(group, point);

      }
   }

   // here we take a bunch of points with the same key and create a convex hull
   // with the points.
   // This is the first job, as we don't want to waste time setting up and tearing
   // down jobs for creating a bunch of convex hulls of size 1 and 2. Afte this
   // initial creation, we will use a job for each recursive level of the D and Q
   // Convex Hull algorithm
   public static class PointReducer extends Reducer<IntWritable, Point, IntWritable, ConvexHull> {
      private ConvexHull whole;
      private Vector<Point> points = new Vector<>();

      // reduce all CH into one CH from left to right
      @Override
      public void reduce(IntWritable key, Iterable<Point> values, Context context)
            throws IOException, InterruptedException {

         for (Point p : values) {
            points.add(p);
         }
         whole = new ConvexHull(points);

         context.write(key, whole);

      }

   }

}
