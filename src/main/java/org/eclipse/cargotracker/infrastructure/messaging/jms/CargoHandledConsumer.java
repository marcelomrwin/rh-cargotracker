package org.eclipse.cargotracker.infrastructure.messaging.jms;

import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;

import org.eclipse.cargotracker.application.CargoInspectionService;
import org.eclipse.cargotracker.domain.model.cargo.TrackingId;

@ApplicationScoped
public class CargoHandledConsumer {

	@Inject
	private Logger logger;

	@Inject
	private CargoInspectionService cargoInspectionService;

	public void onCargoHandled(@ObservesAsync CargoHandledEvent event) {
		logger.log(Level.INFO, "Cargo handled event received for {0}", event.trackingId());
		cargoInspectionService.inspectCargo(new TrackingId(event.trackingId()));
	}
}
