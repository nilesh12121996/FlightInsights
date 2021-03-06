import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

class FlightProbMapper extends Mapper<Object, Text, Text, DoubleWritable>{

	@Override
	protected void map(Object key, Text value, Context context)
			throws IOException, InterruptedException {
		
		String file = value.toString();
		String[] entry = file.split(",");
		
		String carrier = entry[8];
		String arri_delay = entry[14];
		
		int delay_threshold = 5;
		
		if(!arri_delay.equalsIgnoreCase("ArrDelay") && !arri_delay.equalsIgnoreCase("NA")) {
			
				if(Integer.parseInt(arri_delay) > delay_threshold) {
					Text airline = new Text(carrier);
					context.write(airline, new DoubleWritable(0));
				}
			context.write(new Text(carrier), new DoubleWritable(1));
			
		}
		
		
		
	}
	
	

}

class FlightProbReducer extends Reducer<Text, DoubleWritable, Text, DoubleWritable>{

	public ArrayList<customDelay> listDelayMap = new ArrayList<customDelay>();
	
	@Override
	protected void reduce(Text key, Iterable<DoubleWritable> values,
			Context context) throws IOException, InterruptedException {
		
		
		int total_count = 0;
		int onTime = 0;
		
		Iterator<DoubleWritable> iterator = values.iterator();
		
		while(iterator.hasNext()) {
			
			if(iterator.next().get() == 1) {
				onTime++;
			}
			total_count++;
			
		}
		
		double prob_val =  (double)onTime / (double)total_count;
		
		listDelayMap.add(new customDelay(key.toString(), prob_val));
		
		
	}
	
	class customDelay{
		
		String airline;
		Double sch_prob;
		
		public customDelay(String airline, double sch_prob) {
			// TODO Auto-generated constructor stub
			
			this.airline = airline;
			this.sch_prob = sch_prob;
			
		}
		
	}
	
	@Override
	protected void cleanup(Reducer<Text, DoubleWritable, Text, DoubleWritable>.Context context)
			throws IOException, InterruptedException {
		
		Collections.sort(listDelayMap, new Comparator<customDelay>() {

			@Override
			public int compare(customDelay c1, customDelay c2) {
				// TODO Auto-generated method stub
				
				return c2.sch_prob.compareTo(c1.sch_prob);
				
			}
		});
		
		
		context.write(new Text("Highest 3 Probability"),new DoubleWritable(0.0));
		for(int i=0;i<3;i++) {
			customDelay c = listDelayMap.get(i);
			context.write(new Text(c.airline),new DoubleWritable(c.sch_prob));
		}
		
		context.write(new Text("Lowest 3 Probability"),new DoubleWritable(0.0));
		
		for(int i=(listDelayMap.size()-1) ; i > (listDelayMap.size()-4) ; i--) {
			customDelay c = listDelayMap.get(i);
			context.write(new Text(c.airline),new DoubleWritable(c.sch_prob));
		}
		
	}


}


public class FlightProb {
	public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException{
		
        Job conf=Job.getInstance(new Configuration(),"FlightProb");
        conf.setJarByClass(FlightProb.class);
        conf.setMapperClass(FlightProbMapper.class);
        conf.setReducerClass(FlightProbReducer.class);
        conf.setOutputKeyClass(Text.class);
        conf.setOutputValueClass(DoubleWritable.class);
        FileInputFormat.addInputPath(conf, new Path(args[0]));
        FileOutputFormat.setOutputPath(conf, new Path(args[1]));
        System.exit(conf.waitForCompletion(true) ? 0 : 1);
	}
}