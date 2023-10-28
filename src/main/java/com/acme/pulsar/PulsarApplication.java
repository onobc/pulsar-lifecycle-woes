package com.acme.pulsar;

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.Schema;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.pulsar.annotation.PulsarListener;
import org.springframework.pulsar.annotation.PulsarReader;
import org.springframework.pulsar.core.PulsarTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@SpringBootApplication
@EnableScheduling
public class PulsarApplication {

	public static void main(String[] args) {
		SpringApplication.run(PulsarApplication.class, args);
	}

	private static final String TOPIC_1 = "crac-demo-topic1";
	private static final String TOPIC_2 = "crac-demo-topic2";

	private AtomicBoolean running = new AtomicBoolean(false);

	@Autowired
	private PulsarTemplate<Greeting> pulsarTemplate;

	@Scheduled(initialDelay = 1_000, fixedDelay = 1_000)
	void sourceMessagesIntoPulsarTopics() throws PulsarClientException {
		if (!running.get()) {
			return;
		}
		Greeting message = new Greeting("Hello from CRaC!");
		pulsarTemplate.send(TOPIC_1, message, Schema.JSON(Greeting.class));
		System.out.println("Message Sent (@PulsarListener): " + message);
		pulsarTemplate.send(TOPIC_2, message, Schema.JSON(Greeting.class));
		System.out.println("Message Sent (@PulsarReader): " + message);
		System.out.println("*** DONE");
	}

	@EventListener
	public void applicationReady(ApplicationReadyEvent ignored) {
		this.running.compareAndSet(false, true);
	}

	@PulsarListener(topics = TOPIC_1, subscriptionName = "crac-demo-sub1")
	void receiveMessageFromTopic(Greeting message) {
		System.out.println("Message Received (@PulsarListener): " + message);
	}

	@PulsarReader(topics = TOPIC_2, subscriptionName = "crac-demo-sub2", startMessageId = "earliest")
	void readMessageFromTopic(Greeting message) {
		System.out.println("Message Received (@PulsarReader): " + message);
	}

	record Greeting(String message) {
	}
}
