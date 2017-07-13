package me.freo.sk;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;











import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.log4j.helpers.ISO8601DateFormat;
import org.json.JSONException;
import org.json.JSONObject;
import org.wso2.siddhi.core.stream.input.InputHandler;

public class Consumer implements Runnable {
  private final KafkaConsumer<String, String> consumer;
  private final List<String> topics;
  private final int id;
  private final InputHandler inputHandler;

  public Consumer(int id,
                      String groupId, 
                      List<String> topics, InputHandler inputHandler) {
    this.id = id;
    this.topics = topics;
    Properties props = new Properties();
    props.put("bootstrap.servers", "kafka.freo.me:9092");
    props.put("group.id", groupId);
    props.put("enable.auto.commit", "true");
    props.put("auto.commit.interval.ms", "1000");
    props.put("auto.offset.reset", "latest");
    props.put("key.deserializer", StringDeserializer.class.getName());
    props.put("value.deserializer", StringDeserializer.class.getName());
    this.consumer = new KafkaConsumer<>(props);
    this.inputHandler = inputHandler;
  }
 
  @Override
  public void run() {
    try {
      
      consumer.subscribe(topics);
      
      while (true) {
        ConsumerRecords<String, String> records = consumer.poll(Long.MAX_VALUE);
        for (ConsumerRecord<String, String> record : records) {
        	
          JSONObject json = new JSONObject(record.value());
          String line = json.getString("line");
          String stationId = json.getString("stationId");
          String trainNumber = json.getString("trainNumber");
          String stationName = json.getString("stationName");
          Instant timestamp = Instant.parse(json.getString("timestamp"));
          Instant expArrival = Instant.parse(json.getString("expArrival"));
          long tts = json.getLong("tts");
          long ts = timestamp.getEpochSecond();
          long exp = expArrival.getEpochSecond();
          inputHandler.send(ts, new Object[] {line, stationId, stationName, trainNumber, exp, tts});
        }
      }
    } catch (WakeupException we) {
    	// exit quietly
    } catch ( Exception e) {
      e.printStackTrace();
    } finally {
      consumer.close();
    }
  }

  public void shutdown() {
    consumer.wakeup();
  }
}