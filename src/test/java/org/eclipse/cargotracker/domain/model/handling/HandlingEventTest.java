package org.eclipse.cargotracker.domain.model.handling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Date;

import org.eclipse.cargotracker.domain.model.cargo.Cargo;
import org.eclipse.cargotracker.domain.model.cargo.RouteSpecification;
import org.eclipse.cargotracker.domain.model.cargo.TrackingId;
import org.eclipse.cargotracker.domain.model.location.SampleLocations;
import org.eclipse.cargotracker.domain.model.voyage.SampleVoyages;
import org.junit.jupiter.api.Test;

class HandlingEventTest {

	private final TrackingId trackingId = new TrackingId("XYZ");
	private final RouteSpecification routeSpecification = new RouteSpecification(SampleLocations.HONGKONG,
			SampleLocations.NEWYORK, new Date());
	private final Cargo cargo = new Cargo(trackingId, routeSpecification);

	@Test
	void testNewWithCarrierMovement() {
		HandlingEvent event1 = new HandlingEvent(cargo, new Date(), new Date(), HandlingEvent.Type.LOAD,
				SampleLocations.HONGKONG, SampleVoyages.CM003);
		assertEquals(SampleLocations.HONGKONG, event1.getLocation());

		HandlingEvent event2 = new HandlingEvent(cargo, new Date(), new Date(), HandlingEvent.Type.UNLOAD,
				SampleLocations.NEWYORK, SampleVoyages.CM003);
		assertEquals(SampleLocations.NEWYORK, event2.getLocation());

		for (HandlingEvent.Type type : Arrays.asList(HandlingEvent.Type.CLAIM, HandlingEvent.Type.RECEIVE,
				HandlingEvent.Type.CUSTOMS)) {
			assertThrows(IllegalArgumentException.class,
					() -> new HandlingEvent(cargo, new Date(), new Date(), type, SampleLocations.HONGKONG,
							SampleVoyages.CM003));
		}

		for (HandlingEvent.Type type : Arrays.asList(HandlingEvent.Type.LOAD, HandlingEvent.Type.UNLOAD)) {
			assertThrows(NullPointerException.class,
					() -> new HandlingEvent(cargo, new Date(), new Date(), type, SampleLocations.HONGKONG, null));
		}
	}

	@Test
	void testNewWithLocation() {
		HandlingEvent event1 = new HandlingEvent(cargo, new Date(), new Date(), HandlingEvent.Type.CLAIM,
				SampleLocations.HELSINKI);
		assertEquals(SampleLocations.HELSINKI, event1.getLocation());
	}

	@Test
	void testCurrentLocationLoadEvent() {
		HandlingEvent event = new HandlingEvent(cargo, new Date(), new Date(), HandlingEvent.Type.LOAD,
				SampleLocations.CHICAGO, SampleVoyages.CM004);

		assertEquals(SampleLocations.CHICAGO, event.getLocation());
	}

	@Test
	void testCurrentLocationUnloadEvent() {
		HandlingEvent ev = new HandlingEvent(cargo, new Date(), new Date(), HandlingEvent.Type.UNLOAD,
				SampleLocations.HAMBURG, SampleVoyages.CM004);

		assertEquals(SampleLocations.HAMBURG, ev.getLocation());
	}

	@Test
	void testCurrentLocationReceivedEvent() {
		HandlingEvent event = new HandlingEvent(cargo, new Date(), new Date(), HandlingEvent.Type.RECEIVE,
				SampleLocations.CHICAGO);

		assertEquals(SampleLocations.CHICAGO, event.getLocation());
	}

	@Test
	void testCurrentLocationClaimedEvent() {
		HandlingEvent event = new HandlingEvent(cargo, new Date(), new Date(), HandlingEvent.Type.CLAIM,
				SampleLocations.CHICAGO);

		assertEquals(SampleLocations.CHICAGO, event.getLocation());
	}
}
