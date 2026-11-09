package org.eclipse.cargotracker.infrastructure.messaging.jms;

public record CargoDeliveredEvent(String trackingId) {
}
