package org.eclipse.cargotracker.infrastructure.messaging.jms;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

import org.eclipse.cargotracker.application.ApplicationEvents;
import org.eclipse.cargotracker.domain.model.cargo.Cargo;
import org.eclipse.cargotracker.domain.model.handling.HandlingEvent;
import org.eclipse.cargotracker.interfaces.handling.HandlingEventRegistrationAttempt;

@ApplicationScoped
public class JmsApplicationEvents implements ApplicationEvents, Serializable {

	private static final long serialVersionUID = 1L;

	@Inject
	Event<CargoHandledEvent> cargoHandledEvent;
	@Inject
	Event<CargoMisdirectedEvent> cargoMisdirectedEvent;
	@Inject
	Event<CargoDeliveredEvent> cargoDeliveredEvent;
	@Inject
	Event<HandlingEventRegistrationAttempt> handlingEventRegistrationAttempt;
	@Inject
	private Logger logger;

	@Override
	public void cargoWasHandled(HandlingEvent event) {
		Cargo cargo = event.getCargo();
		logger.log(Level.INFO, "Cargo was handled {0}", cargo);
		cargoHandledEvent.fireAsync(new CargoHandledEvent(cargo.getTrackingId().getIdString()));
	}

	@Override
	public void cargoWasMisdirected(Cargo cargo) {
		logger.log(Level.INFO, "Cargo was misdirected {0}", cargo);
		cargoMisdirectedEvent.fireAsync(new CargoMisdirectedEvent(cargo.getTrackingId().getIdString()));
	}

	@Override
	public void cargoHasArrived(Cargo cargo) {
		logger.log(Level.INFO, "Cargo has arrived {0}", cargo);
		cargoDeliveredEvent.fireAsync(new CargoDeliveredEvent(cargo.getTrackingId().getIdString()));
	}

	@Override
	public void receivedHandlingEventRegistrationAttempt(HandlingEventRegistrationAttempt attempt) {
		logger.log(Level.INFO, "Received handling event registration attempt {0}", attempt);
		handlingEventRegistrationAttempt.fireAsync(attempt);
	}
}
