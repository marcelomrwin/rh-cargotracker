package org.eclipse.cargotracker.infrastructure.messaging.jms;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;

import org.eclipse.cargotracker.application.HandlingEventService;
import org.eclipse.cargotracker.domain.model.handling.CannotCreateHandlingEventException;
import org.eclipse.cargotracker.interfaces.handling.HandlingEventRegistrationAttempt;

@ApplicationScoped
public class HandlingEventRegistrationAttemptConsumer {

	@Inject
	private HandlingEventService handlingEventService;

	public void onHandlingEventRegistrationAttempt(@ObservesAsync HandlingEventRegistrationAttempt attempt) {
		try {
			handlingEventService.registerHandlingEvent(attempt.getCompletionTime(), attempt.getTrackingId(),
					attempt.getVoyageNumber(), attempt.getUnLocode(), attempt.getType());
		} catch (CannotCreateHandlingEventException e) {
			throw new RuntimeException("Error occurred processing handling event registration", e);
		}
	}
}
