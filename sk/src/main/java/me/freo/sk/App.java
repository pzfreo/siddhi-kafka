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

import org.json.JSONObject;
import org.wso2.siddhi.core.ExecutionPlanRuntime;

import org.wso2.siddhi.core.SiddhiManager;
import org.wso2.siddhi.core.event.Event;
import org.wso2.siddhi.core.stream.input.InputHandler;
import org.wso2.siddhi.core.stream.output.StreamCallback;

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
            plan += cl;
        }
		br.close();
		
		int numConsumers = 1;

		String rand = Integer.toString((new Random()).nextInt());
		String groupId = "consumer-" + rand;
		List<String> topics = Arrays.asList("tfl");
		ExecutorService executor = Executors.newFixedThreadPool(numConsumers);
		SiddhiManager siddhiManager = new SiddhiManager();
		
	
		ExecutionPlanRuntime executionPlanRuntime = siddhiManager.createExecutionPlanRuntime(plan);
		
		executionPlanRuntime.addCallback("outstream", new StreamCallback() {
			String[] sd = executionPlanRuntime.getStreamDefinitionMap().get("outstream").getAttributeNameArray();
			
			@Override
			public void receive(Event[] events) {

				
				
				JSONObject json = new JSONObject();
				for (int i=0; i < events.length; i++) {
					Object[] data = events[i].getData();
					for (int j=0; j< data.length; j++) {
						json.put(sd[j], data[j]);
					}
					json.put("timestamp", events[i].getTimestamp());
					System.out.println(json.toString());
				}
				
				
			}
		});
		executionPlanRuntime.start();
		InputHandler ih = executionPlanRuntime.getInputHandler("tflstream");

		final List<Consumer> consumers = new ArrayList<>();
		for (int i = 0; i < numConsumers; i++) {
			Consumer consumer = new Consumer(i, groupId, topics, ih);
			consumers.add(consumer);
			executor.submit(consumer);
		}

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
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
