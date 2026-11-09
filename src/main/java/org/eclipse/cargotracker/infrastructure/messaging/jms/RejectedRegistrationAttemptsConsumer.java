package org.eclipse.cargotracker.infrastructure.messaging.jms;

import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;

import org.eclipse.cargotracker.interfaces.handling.HandlingEventRegistrationAttempt;

@ApplicationScoped
public class RejectedRegistrationAttemptsConsumer {

	@Inject
	private Logger logger;

	public void onRejectedAttempt(@ObservesAsync HandlingEventRegistrationAttempt attempt) {
		logger.log(Level.INFO, "Rejected registration attempt of cargo with tracking ID {0}.",
				attempt.getTrackingId());
	}
}
