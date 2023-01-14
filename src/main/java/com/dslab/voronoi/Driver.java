package com.dslab.voronoi;

import org.apache.commons.text.StringTokenizer;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.SequenceFile.Reader;
import org.apache.hadoop.mapred.OutputFormat;
import org.apache.hadoop.mapred.Reporter;

import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.MultipleInputs;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileAsTextRecordReader;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.thirdparty.com.google.common.io.PatternFilenameFilter;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.Scanner;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;
import java.util.regex.Pattern;
import java.io.File;

public class Driver {
   // for maximum performance an initial convex hull size that takes full advantage
   // of the heap space for a single threaded java program. Each job requires time
   // to create and destroy it, so minimizing the amount of "recursive calls" we
   // need to make (AKA jobs) saves us time. No point in creating a bunch of jobs
   // for merging convex hulls of small sizes
   public static int INITIAL_CH_SIZE = 100;

   // AKA number of jobs to complete the D and Q algorithm
   private static int RECURSIVE_DEPTH;
   private static Path input;
   private static Path output;
   private static int SIZE;
   private static int NUM_MACHINES = 1;

   // 4 CPUs * 2 cores * 2 threads
   private static final int NUM_THREADS = 1;

   // args: [Npoints, input path, output path, Nmachines]
   public static void main(String[] args) throws Exception {
      // number of recursions (mapreduce jobs) is equal to log_base_2(total_points /
      // initial_size_of_CH) rounded up
      SIZE = Integer.parseInt(args[0]);
      NUM_MACHINES = Integer.parseInt(args[3]);
      INITIAL_CH_SIZE = SIZE / (NUM_THREADS * NUM_MACHINES);
      RECURSIVE_DEPTH = (int) Math.ceil((Math.log((double) SIZE / INITIAL_CH_SIZE) / Math.log(2)));
      final int INITIAL_REDUCE_TASKS = NUM_THREADS * NUM_MACHINES;

      // create directory for intermediate files and output
      String dirPath = "/" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmssSSS"));
      new File(args[2] + dirPath).mkdirs();
      input = new Path(args[1]);
      output = new Path(args[2] + dirPath + "/output");

      // INITIAL JOB ---
      Configuration config = new Configuration();
      Job job = Job.getInstance(config, "Convex Hull Init");
      job.setJarByClass(Driver.class);

      // output <k,v> = <CH#, {CH points}>
      job.setOutputKeyClass(IntWritable.class);
      job.setOutputValueClass(ConvexHull.class);
      // job.setCombinerClass(PointReducer.class);
      job.setMapperClass(PointMapper.class);
      job.setReducerClass(PointReducer.class);
      job.setNumReduceTasks(INITIAL_REDUCE_TASKS);

      FileInputFormat.addInputPath(job, input);
      // intermediate output
      SequenceFileOutputFormat.setOutputPath(job, new Path(output.toString() + 0));

      job.setInputFormatClass(TextInputFormat.class);
      // job.setOutputFormatClass(SequenceFileOutputFormat.class);
      job.setOutputFormatClass(TextOutputFormat.class);
      job.waitForCompletion(true);

      // check output
      Scanner check = new Scanner(new File(output.toString() + "0/part-r-00000"));
      int x = 0;
      while (check.hasNextLine()) {
         Scanner line = new Scanner(check.nextLine());
         line.useDelimiter(Pattern.compile("[\\s,;]+"));
         line.nextInt();
         ConvexHullViewer.createImage(SIZE, line, new Path(output.toString() +
               "_StartingImage" + x + ".png"));
      }

      // RECURSIVE JOB ---
      int numReduceTasks = INITIAL_REDUCE_TASKS;

      // iterate jobs to replicate recursion for Divide and conquer
      for (int i = 0;; i++) {

         config = new Configuration();
         job = Job.getInstance(config, "Convex Hull Recursion" + i);
         job.setJarByClass(Driver.class);
         job.setMapperClass(ConvexHullMapper.class);
         job.setReducerClass(ConvexHullReducer.class);
         job.setCombinerClass(ConvexHullReducer.class);
         job.setNumReduceTasks(numReduceTasks);
         job.setOutputKeyClass(IntWritable.class);
         job.setOutputValueClass(ConvexHull.class);

         // intermediate output goes back and forth between a file ending in 0 to a file
         // ending 1. These files have the same path as the output
         for (int j = 0; j < numReduceTasks; j++) {
            SequenceFileInputFormat.addInputPath(job, new Path(output.toString() + i + "/part-r-0000" + j));
         }
         job.setInputFormatClass(SequenceFileInputFormat.class);

         for (Path p : SequenceFileInputFormat.getInputPaths(job)) {
            System.out.println(p.toString());
         }
         if (numReduceTasks == 1) { // last iteration. write output to specified file
            FileOutputFormat.setOutputPath(job, output);
            job.setOutputFormatClass(TextOutputFormat.class);
         } else {
            SequenceFileOutputFormat.setOutputPath(job, new Path(output.toString() + (i + 1)));
            job.setOutputFormatClass(SequenceFileOutputFormat.class);
         }

         job.waitForCompletion(true);
         if (numReduceTasks == 1) {
            break;
         }
         numReduceTasks = (int) Math.ceil((double) numReduceTasks / 2);
      }

      // SequenceFile.Reader reader = new SequenceFile.Reader(config,
      // SequenceFile.Reader
      // .file(new Path(SequenceFileOutputFormat.getOutputPath(job).toString() +
      // "/part-r-00000")));

      // IntWritable key = new IntWritable();
      // ConvexHull value = new ConvexHull();
      // while (reader.next(key, value)) {
      // System.out.println(key + "\t" + value);
      // }

      // CREATE IMAGE OF BEFORE AND AFTER
      Scanner start = new Scanner(new File(input.toString()));
      start.useDelimiter(Pattern.compile("[\\s,;]+"));

      ConvexHullViewer.createImage(SIZE, start, new Path(output.toString() +
            "_StartingImage.png"));

      Scanner result = new Scanner(new File(output.toString() + "/part-r-00000"));
      result.useDelimiter(Pattern.compile("[\\s,;]+"));

      // clear the key from the reduce output
      result.nextInt();
      ConvexHullViewer.createImage(SIZE, result, new Path(output.toString() +
            "_ResultImage.png"));

      // Reader reader = new Reader(config, Reader.file(output));

   }

   // turn list of x,y values sorted by x value into list of points.
   // keys (the group var) are a function of x value, where each key should have
   // 1-3 points
   public static class ConvexHullMapper extends Mapper<IntWritable, ConvexHull, IntWritable, ConvexHull> {

      private ConvexHull ch;
      private IntWritable group = new IntWritable();

      @Override
      public void map(IntWritable key, ConvexHull value, Context context) throws IOException, InterruptedException {

         // // get point from input file line of coords
         // Scanner reader = new Scanner(value.toString());
         // // get key first
         int oldKey = key.get();
         // ch = new ConvexHull(reader);
         ch = value;
         // group convex hulls into groups of 2 using key
         group.set((int) (oldKey / 2));
         context.write(group, ch);

      }
   }

   public static class ConvexHullReducer extends Reducer<IntWritable, ConvexHull, IntWritable, ConvexHull> {

      // reduce all CH into one CH from left to right
      @Override
      public void reduce(IntWritable key, Iterable<ConvexHull> values, Context context)
            throws IOException, InterruptedException {
         ConvexHull left = null;

         // This should only reduce two convex hulls at a time, but it is written to
         // allow for more
         int counter = 0;
         for (ConvexHull ch : values) {

            if (counter == 0) {

               left = ch;
               counter++;
               continue;
            }
            left = ConvexHull.merge(left, ch);
            counter++;
         }
         if (left != null) {
            context.write(key, left);
         }

      }

   }

   // as part of the first job, we need to group a bunch of points into convex
   // hulls using the initial CH size. This will save us many recursive steps and
   // time spent tearing down and creatin jobs
   public static class PointMapper extends Mapper<LongWritable, Text, IntWritable, ConvexHull> {

      private ConvexHull ch;
      private IntWritable group;

      @Override
      public void setup(Context context) {

      }

      @Override
      public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {

         // get point from input file line of coords
         Scanner reader = new Scanner(value.toString());
         System.out.println(value.toString());

         Point p = new Point();

         p = new Point(reader);

         ch = new ConvexHull(p);

         // group points into groups of a set size
         group = new IntWritable(p.getX() / INITIAL_CH_SIZE);
         context.write(group, ch);

      }
   }

   // here we take a bunch of points with the same key and create a convex hull
   // with the points.
   // This is the first job, as we don't want to waste time setting up and tearing
   // down jobs for creating a bunch of convex hulls of size 1 and 2. Afte this
   // initial creation, we will use a job for each recursive level of the D and Q
   // Convex Hull algorithm
   public static class PointReducer extends Reducer<IntWritable, ConvexHull, IntWritable, ConvexHull> {
      private ConvexHull whole = new ConvexHull();

      // reduce all CH into one CH from left to right
      @Override
      public void reduce(IntWritable key, Iterable<ConvexHull> values, Context context)
            throws IOException, InterruptedException {

         Vector<ConvexHull> hulls = new Vector<>();

         int count = 0;

         for (ConvexHull ch : values) {
            count++;

            // reset Iterable buffer (this cost me HOURS to figure out)
            hulls.add(new ConvexHull(ch));

         }

         System.out.println("********NUMBER OF CONVEX HULLS IN FIRST REDUCTION: " + count);

         Vector<Point> points = new Vector<>();

         for (ConvexHull ch : hulls) {
            points.addAll(ch.getPoints());
         }
         // sort points by x value
         Collections.sort(points);
         System.out.println(points);

         whole = new ConvexHull(points);

         context.write(key, whole);

      }

   }

}
