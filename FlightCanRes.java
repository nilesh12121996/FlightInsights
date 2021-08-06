	import java.io.IOException;
	import java.util.ArrayList;
	import java.util.Collections;
	import java.util.Comparator;
	import java.util.Iterator;

	import org.apache.hadoop.conf.Configuration;
	import org.apache.hadoop.fs.Path;
	import org.apache.hadoop.io.IntWritable;
	import org.apache.hadoop.io.Text;
	import org.apache.hadoop.mapreduce.Job;
	import org.apache.hadoop.mapreduce.Mapper;
	import org.apache.hadoop.mapreduce.Reducer;
	import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
	import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;


	class FlightCanResMapper extends Mapper<Object, Text, Text, IntWritable>{

		@Override
		protected void map(Object key, Text value, Context context)
				throws IOException, InterruptedException {
			
			String file = value.toString();
			String[] entry = file.split(",");
			
			String cancel_flag = entry[21];
			String cancel_code = entry[22];
			
			if(!cancel_flag.equalsIgnoreCase("Cancelled") && !cancel_flag.equalsIgnoreCase("NA")
					&& cancel_flag.equals("1") && !cancel_code.equalsIgnoreCase("CancellationCode")
					&& !cancel_code.equalsIgnoreCase("NA")) {
				
				context.write(new Text(cancel_code), new IntWritable(1));
				
			}
			
		}
		
	}

	class FlightCanResReducer extends Reducer<Text, IntWritable, Text, IntWritable>{

		public ArrayList<cancelModel> listCancelMap = new ArrayList<cancelModel>();
		
		@Override
		protected void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException {
			
			int total_count = 0;
			int cancel_count = 0;
			
			Iterator<IntWritable> iterator = values.iterator();
			
			while(iterator.hasNext()) {	
				cancel_count = cancel_count + iterator.next().get();
				total_count++;				
			}
			
			listCancelMap.add(new cancelModel(key.toString(), cancel_count));
			
		}
		
		@Override
		protected void cleanup(Context context)
				throws IOException, InterruptedException {
			
			Collections.sort(listCancelMap, new Comparator<cancelModel>() {

				@Override
				public int compare(cancelModel c1, cancelModel c2) {
					// TODO Auto-generated method stub
					
					return c2.can_count.compareTo(c1.can_count);
					
				}
			});
			if(listCancelMap.size() > 0) {
				cancelModel result_code = listCancelMap.get(0);
			
			switch(result_code.cancel_text){
				
				case "A":
					context.write(new Text(result_code.cancel_text+" : Carrier"), new IntWritable(result_code.can_count));
					break;
				
				case "B":
					context.write(new Text(result_code.cancel_text+" : Weather"), new IntWritable(result_code.can_count));
					break;
				
				case "C":
					context.write(new Text(result_code.cancel_text+" : NAS"), new IntWritable(result_code.can_count));
					break;
					
				case "D":
					context.write(new Text(result_code.cancel_text+" : Security"), new IntWritable(result_code.can_count));
					break;
					
				default:
					context.write(new Text(result_code.cancel_text+" : No Common Reason"), new IntWritable(result_code.can_count));
				
				
			}
			}
			else {
				context.write(new Text("No cancellation data available"), new IntWritable());
			}
			
		}

		
		class cancelModel{
			
			String cancel_text;
			Integer can_count;
			public cancelModel(String cancel_text, int can_count) {
				super();
				this.cancel_text = cancel_text;
				this.can_count = can_count;
			}
			
		}
		
	}

	public class FlightCanRes {
		
		public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException{
			
			Job conf=Job.getInstance(new Configuration(),"FlightCanRes");
			conf.setJarByClass(FlightCanRes.class);
			conf.setMapperClass(FlightCanResMapper.class);
			conf.setReducerClass(FlightCanResReducer.class);
			conf.setOutputKeyClass(Text.class);
			conf.setOutputValueClass(IntWritable.class);
	        FileInputFormat.setInputPaths(conf, new Path(args[0]));
	        FileOutputFormat.setOutputPath(conf, new Path(args[1]));
	        System.exit(conf.waitForCompletion(true) ? 0 : 1);		
		}

	}

