package org.myorg;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.TreeSet;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;


public class Logs {
	public static class FileAccess implements Comparable<FileAccess> {

		private Text file, month;

		public FileAccess() {
			this.file = new Text();
			this.month = new Text();
		}

		public void set(Text file, Text month) {
			this.file = file;
			this.month = month;
		}

		@Override
		public int compareTo(FileAccess o) {
			if (file.compareTo(o.file) == 0) {
				return (month.compareTo(o.month));
			} else
				return (file.compareTo(o.file));
		}

		@Override
		public int hashCode() {
			return file.hashCode();
		}

		@Override
		public String toString() {
			return month.toString() + "\t" + file.toString();
		}
	}

	public static class Map extends
			Mapper<LongWritable, Text, FileAccess, Text> {

		private final FileAccess fileAccess = new FileAccess();

		private Text file = new Text();
		private Text ipAddress = new Text();
		private Text month = new Text();

		public void map(LongWritable key, Text value, Context context)
				throws IOException, InterruptedException {
			final String[] data = value.toString().trim().split(" ");
			if (data.length > 6) {
				ipAddress.set(data[0]);
				String[] datetime = data[3].trim().split(":");
				if (datetime.length > 0 && datetime[0].length()>1) {
					String date = datetime[0].substring(1);
					if (data[6].trim().length() > 1) {
						file.set(data[6]);
						String[] nums = date.split("/");
						if (nums.length > 1) {
							month.set(date.split("/")[1]);
							fileAccess.set(file, month);
							context.write(fileAccess, ipAddress);
						}
					}
				}
			}

		}
	}

	public static class Reduce extends
			Reducer<FileAccess, Text, FileAccess, Text> {

		@Override
		public void reduce(FileAccess key, Iterable<Text> values,
				Context context) throws IOException, InterruptedException {
			TreeSet<String> files = new TreeSet<String>();
			for (Text val : values) 
				files.add(val.toString());
			context.write(key, new Text(files.toString()));
		}
	}

	public static void main(String[] args) throws Exception {
		@SuppressWarnings("deprecation")
		Job job = new Job();
		job.setJobName("logs");
		job.setJarByClass(Logs.class);
		job.setMapperClass(Map.class);
		job.setReducerClass(Reduce.class);
		job.setOutputKeyClass(FileAccess.class);
		job.setOutputValueClass(Text.class);
		job.setMapOutputKeyClass(FileAccess.class);
		job.setMapOutputValueClass(Text.class);
		FileInputFormat.addInputPath(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));
		System.exit(job.waitForCompletion(true) ? 0 : 1);

	}
}