package org.eclipse.cargotracker.infrastructure.messaging.jms;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.logging.Level;
import java.util.logging.Logger;



import jakarta.inject.Inject;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;

// TODO Step 5: wire JMS via ConnectionFactory (jms-quickstart pattern)
@ApplicationScoped

public class DeliveredCargoConsumer implements MessageListener {

    @Inject
    private Logger logger;

	@Override
	public void onMessage(Message message) {
		try {
			logger.log(Level.INFO, "Cargo with tracking ID {0} delivered.", message.getBody(String.class));
		} catch (JMSException ex) {
			logger.log(Level.WARNING, "Error processing message.", ex);
		}
	}
}
