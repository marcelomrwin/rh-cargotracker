package org.eclipse.cargotracker.domain.model.cargo;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.eclipse.cargotracker.domain.model.handling.HandlingEvent;
import org.eclipse.cargotracker.domain.model.location.SampleLocations;
import org.eclipse.cargotracker.domain.model.voyage.Voyage;
import org.eclipse.cargotracker.domain.model.voyage.VoyageNumber;
import org.junit.jupiter.api.Test;

class ItineraryTest {

	private Voyage voyage = new Voyage.Builder(new VoyageNumber("0123"), SampleLocations.SHANGHAI)
			.addMovement(SampleLocations.ROTTERDAM, new Date(), new Date())
			.addMovement(SampleLocations.GOTHENBURG, new Date(), new Date()).build();
	private Voyage wrongVoyage = new Voyage.Builder(new VoyageNumber("666"), SampleLocations.NEWYORK)
			.addMovement(SampleLocations.STOCKHOLM, new Date(), new Date())
			.addMovement(SampleLocations.HELSINKI, new Date(), new Date()).build();

	@Test
	void testCargoOnTrack() {
		TrackingId trackingId = new TrackingId("CARGO1");
		RouteSpecification routeSpecification = new RouteSpecification(SampleLocations.SHANGHAI,
				SampleLocations.GOTHENBURG, new Date());
		Cargo cargo = new Cargo(trackingId, routeSpecification);

		Itinerary itinerary = new Itinerary(Arrays.asList(
				new Leg(voyage, SampleLocations.SHANGHAI, SampleLocations.ROTTERDAM, new Date(), new Date()),
				new Leg(voyage, SampleLocations.ROTTERDAM, SampleLocations.GOTHENBURG, new Date(), new Date())));

		HandlingEvent event = new HandlingEvent(cargo, new Date(), new Date(), HandlingEvent.Type.RECEIVE,
				SampleLocations.SHANGHAI);
		assertTrue(itinerary.isExpected(event));

		event = new HandlingEvent(cargo, new Date(), new Date(), HandlingEvent.Type.LOAD, SampleLocations.SHANGHAI,
				voyage);
		assertTrue(itinerary.isExpected(event));

		event = new HandlingEvent(cargo, new Date(), new Date(), HandlingEvent.Type.UNLOAD, SampleLocations.ROTTERDAM,
				voyage);
		assertTrue(itinerary.isExpected(event));

		event = new HandlingEvent(cargo, new Date(), new Date(), HandlingEvent.Type.LOAD, SampleLocations.ROTTERDAM,
				voyage);
		assertTrue(itinerary.isExpected(event));

		event = new HandlingEvent(cargo, new Date(), new Date(), HandlingEvent.Type.UNLOAD, SampleLocations.GOTHENBURG,
				voyage);
		assertTrue(itinerary.isExpected(event));

		event = new HandlingEvent(cargo, new Date(), new Date(), HandlingEvent.Type.CLAIM, SampleLocations.GOTHENBURG);
		assertTrue(itinerary.isExpected(event));

		event = new HandlingEvent(cargo, new Date(), new Date(), HandlingEvent.Type.CUSTOMS,
				SampleLocations.GOTHENBURG);
		assertTrue(itinerary.isExpected(event));

		event = new HandlingEvent(cargo, new Date(), new Date(), HandlingEvent.Type.RECEIVE, SampleLocations.HANGZOU);
		assertFalse(itinerary.isExpected(event));

		event = new HandlingEvent(cargo, new Date(), new Date(), HandlingEvent.Type.LOAD, SampleLocations.ROTTERDAM,
				wrongVoyage);
		assertFalse(itinerary.isExpected(event));

		event = new HandlingEvent(cargo, new Date(), new Date(), HandlingEvent.Type.UNLOAD, SampleLocations.HELSINKI,
				wrongVoyage);
		assertFalse(itinerary.isExpected(event));

		event = new HandlingEvent(cargo, new Date(), new Date(), HandlingEvent.Type.CLAIM, SampleLocations.ROTTERDAM);
		assertFalse(itinerary.isExpected(event));
	}

	@Test
	void testNextExpectedEvent() {
	}

	@Test
	void testCreateItinerary() {
		assertThrows(IllegalArgumentException.class, () -> new Itinerary(new ArrayList<Leg>()));

		assertThrows(NullPointerException.class, () -> {
			List<Leg> legs = null;
			new Itinerary(legs);
		});
	}
}
