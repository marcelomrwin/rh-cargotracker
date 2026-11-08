package org.eclipse.cargotracker.infrastructure.messaging.jms;

import jakarta.enterprise.context.ApplicationScoped;



import jakarta.inject.Inject;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.ObjectMessage;

import org.eclipse.cargotracker.application.HandlingEventService;
import org.eclipse.cargotracker.domain.model.handling.CannotCreateHandlingEventException;
import org.eclipse.cargotracker.interfaces.handling.HandlingEventRegistrationAttempt;

/**
 * Consumes handling event registration attempt messages and delegates to proper
 * registration.
 */
// TODO Step 5: wire JMS via ConnectionFactory (jms-quickstart pattern)
@ApplicationScoped

public class HandlingEventRegistrationAttemptConsumer implements MessageListener {

	@Inject
	private HandlingEventService handlingEventService;

	@Override
	public void onMessage(Message message) {
		try {
			ObjectMessage objectMessage = (ObjectMessage) message;
			HandlingEventRegistrationAttempt attempt = (HandlingEventRegistrationAttempt) objectMessage.getObject();
			handlingEventService.registerHandlingEvent(attempt.getCompletionTime(), attempt.getTrackingId(),
					attempt.getVoyageNumber(), attempt.getUnLocode(), attempt.getType());
		} catch (JMSException | CannotCreateHandlingEventException e) {
			// Poison messages will be placed on dead-letter queue.
			throw new RuntimeException("Error occurred processing message", e);
		}
	}
}
