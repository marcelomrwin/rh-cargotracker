package org.eclipse.cargotracker.infrastructure.messaging.jms;

public record CargoMisdirectedEvent(String trackingId) {
}
