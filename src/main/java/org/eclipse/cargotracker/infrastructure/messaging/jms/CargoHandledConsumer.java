package org.eclipse.cargotracker.infrastructure.messaging.jms;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.logging.Level;
import java.util.logging.Logger;



import jakarta.inject.Inject;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.TextMessage;

import org.eclipse.cargotracker.application.CargoInspectionService;
import org.eclipse.cargotracker.domain.model.cargo.TrackingId;

/**
 * Consumes JMS messages and delegates notification of misdirected cargo to the
 * tracking service.
 * <p>
 * This is a programmatic hook into the JMS infrastructure to make cargo
 * inspection message-driven.
 */
// TODO Step 5: wire JMS via ConnectionFactory (jms-quickstart pattern)
@ApplicationScoped

public class CargoHandledConsumer implements MessageListener {

	@Inject
	private Logger logger;
	
	@Inject
	private CargoInspectionService cargoInspectionService;

	@Override
	public void onMessage(Message message) {
		try {
			TextMessage textMessage = (TextMessage) message;
			String trackingIdString = textMessage.getText();

			cargoInspectionService.inspectCargo(new TrackingId(trackingIdString));
		} catch (JMSException e) {
			logger.log(Level.SEVERE, "Error procesing JMS message", e);
		}
	}
}
