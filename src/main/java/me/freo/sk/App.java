package me.freo.sk;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.Random;
import java.text.SimpleDateFormat;

import org.json.JSONObject;
// import io.siddhi.core.runtime;

import io.siddhi.core.SiddhiManager;
import io.siddhi.core.SiddhiAppRuntime;
import io.siddhi.core.event.Event;
import io.siddhi.query.api.definition.Attribute;
import io.siddhi.core.stream.input.InputHandler;
import io.siddhi.core.stream.output.StreamCallback;

/**
 * Hello world!
 *
 */
public class App {
	public static void main(String[] args) throws IOException {

		File inFile = null;
		if (0 < args.length) {
			inFile = new File(args[0]);
		} else {
			System.err.println("Invalid arguments count:" + args.length);
			System.exit(1);
		}
		
		String plan = "";
		
		BufferedReader br = new BufferedReader(new FileReader(inFile));
		String cl;
		while ((cl = br.readLine()) != null) {
			plan += cl ;
			plan += "\n";
        }
		br.close();
		
		int numConsumers = 1;

		String rand = Integer.toString((new Random()).nextInt());
		String groupId = "consumer-" + rand;
		List<String> topics = Arrays.asList("tfl");
		ExecutorService executor = Executors.newFixedThreadPool(numConsumers);
		SiddhiManager siddhiManager = new SiddhiManager();
		
		SiddhiAppRuntime runtime = siddhiManager.createSiddhiAppRuntime(plan);
		
		
		runtime.addCallback("outstream", new StreamCallback() {
			String[] sd = runtime.getStreamDefinitionMap().get("outstream").getAttributeNameArray();
			
			@Override
			public void receive(Event[] events) {

				
				
				JSONObject json = new JSONObject();
				for (int i=0; i < events.length; i++) {
					Object[] data = events[i].getData();
					for (int j=0; j< data.length; j++) {
						json.put(sd[j], data[j]);
					}
					json.put("timestamp", events[i].getTimestamp());
					System.out.println(json.toString(4));
				}
				
				
			}
		});
		runtime.start();
		InputHandler ih = runtime.getInputHandler("tflstream");

		final List<Consumer> consumers = new ArrayList<>();
		for (int i = 0; i < numConsumers; i++) {
			Consumer consumer = new Consumer(i, groupId, topics, ih);
			consumers.add(consumer);
			executor.submit(consumer);
		}

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				// print out query if available
				System.err.println();
				System.err.println("===================");
				System.err.println("aggregate query");
				Attribute[] atts = runtime.getStoreQueryOutputAttributes("from LateAggregation within \"2020-**-** **:**:** +00:00\" per \"minutes\" select *;");
				

				Event[] events = runtime.query("from LateAggregation within \"2020-**-** **:**:** +00:00\" per \"minutes\" select *;");
				
				if (events != null) {
					
					JSONObject json = new JSONObject();
					for (int i=0; i < events.length; i++) {
						Object[] data = events[i].getData();
						for (int j=0; j< data.length; j++) {
							if (atts[j].getName()=="AGG_TIMESTAMP") {
								Long ts = Long.parseLong(String.valueOf(data[j]));
								java.util.Date date = new java.util.Date(ts); 
								SimpleDateFormat fdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss z"); 
								String formattedDate = fdf.format(date);
								json.put(atts[j].getName(), formattedDate);
							} else {
								json.put(atts[j].getName(), data[j]);
							}
						}
						System.out.println(json.toString(4));
					}
				}	
				else {
					System.err.println("no result from aggregate query");
				}

				for (Consumer consumer : consumers) {
					consumer.shutdown();
				}
				executor.shutdown();
				try {
					executor.awaitTermination(5000, TimeUnit.MILLISECONDS);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
		
	}
}
