package com.cousteau;

import java.io.File;
import java.io.FileReader;
import java.util.Map;
import java.util.Properties;

import kafka.javaapi.producer.Producer;
import kafka.producer.ProducerConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KafkaNnector {

	private static final Logger _logger = LoggerFactory.getLogger(KafkaNnector.class);
	
	private static final String env_home = "COUSTEAU_HOME";
	
	private static final String config_file = new String("cousteau.properties");
	
	private final ProducerConfig conf;
	
	private final Producer<String, String> producer;
	
	private final String topic;
	
	private static final String DEF_TOPIC = "mailer";
	
	private static final KafkaNnector me = new KafkaNnector();
	
	private KafkaNnector() {
		Properties props = new Properties();
		try {
			System.out.println("Init Kafka");
			Map<String, String> envs = System.getenv();
			String env_path = (envs != null) ? envs.get(env_home) : null;
			String path = ((env_path != null) ? env_path : ".") + File.separatorChar + config_file;
			_logger.debug("Props are at: " + path);
			File cf = new File(path);
			
			if (cf.exists()) {
				_logger.debug("Loading props from path " + path);
				props.load(new FileReader(cf));
			} else {
				_logger.debug("Loading props from classpath");
				props.load(KafkaNnector.class.getClassLoader().getResourceAsStream(config_file));
			}
			_logger.info("Loaded properties from: " + config_file);
		} catch (Exception e) {
			_logger.error("can't load " + config_file + " loading defaults");
			System.out.println("Puttin props");
			props.put("metadata.broker.list", "localhost:9092");
			props.put("serializer.class", "kafka.serializer.StringEncoder");
			props.put("partitioner.class", "kafka.producer.DefaultPartitioner");
			props.put("request.required.acks", "1");
			e.printStackTrace();
		} finally {
			System.out.println("Still trying to configure");
			conf = new ProducerConfig(props);
			producer = new Producer<String, String>(conf);
			topic = props.getProperty("mail.topic", DEF_TOPIC);
		}
		_logger.info("Kafka producer created");
	}
	
	public static final KafkaNnector getInstance() {
		return me;
	}
	
	public Producer<String, String> getProducer() {
		return producer;
	}

	public String getTopic() {
		return topic;
	}
}
