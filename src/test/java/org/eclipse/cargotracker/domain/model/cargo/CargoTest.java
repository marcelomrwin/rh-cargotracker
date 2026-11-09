package org.eclipse.cargotracker.domain.model.cargo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.eclipse.cargotracker.application.util.DateUtil;
import org.eclipse.cargotracker.domain.model.handling.HandlingEvent;
import org.eclipse.cargotracker.domain.model.handling.HandlingHistory;
import org.eclipse.cargotracker.domain.model.location.Location;
import org.eclipse.cargotracker.domain.model.location.SampleLocations;
import org.eclipse.cargotracker.domain.model.voyage.Voyage;
import org.eclipse.cargotracker.domain.model.voyage.VoyageNumber;
import org.junit.jupiter.api.Test;

public class CargoTest {

	private final List<HandlingEvent> events = new ArrayList<>();
	private final Voyage voyage = new Voyage.Builder(new VoyageNumber("0123"), SampleLocations.STOCKHOLM)
			.addMovement(SampleLocations.HAMBURG, new Date(), new Date())
			.addMovement(SampleLocations.HONGKONG, new Date(), new Date())
			.addMovement(SampleLocations.MELBOURNE, new Date(), new Date()).build();

	@Test
	void testConstruction() {
		TrackingId trackingId = new TrackingId("XYZ");
		Date arrivalDeadline = DateUtil.toDate("2009-03-13");
		RouteSpecification routeSpecification = new RouteSpecification(SampleLocations.STOCKHOLM,
				SampleLocations.MELBOURNE, arrivalDeadline);

		Cargo cargo = new Cargo(trackingId, routeSpecification);

		assertEquals(RoutingStatus.NOT_ROUTED, cargo.getDelivery().getRoutingStatus());
		assertEquals(TransportStatus.NOT_RECEIVED, cargo.getDelivery().getTransportStatus());
		assertEquals(Location.UNKNOWN, cargo.getDelivery().getLastKnownLocation());
		assertEquals(Voyage.NONE, cargo.getDelivery().getCurrentVoyage());
	}

	@Test
	void testRoutingStatus() {
		Cargo cargo = new Cargo(new TrackingId("XYZ"),
				new RouteSpecification(SampleLocations.STOCKHOLM, SampleLocations.MELBOURNE, new Date()));
		final Itinerary good = new Itinerary();
		Itinerary bad = new Itinerary();
		@SuppressWarnings("serial")
		RouteSpecification acceptOnlyGood = new RouteSpecification(cargo.getOrigin(),
				cargo.getRouteSpecification().getDestination(), new Date()) {

			@Override
			public boolean isSatisfiedBy(Itinerary itinerary) {
				return itinerary == good;
			}
		};

		cargo.specifyNewRoute(acceptOnlyGood);

		assertEquals(RoutingStatus.NOT_ROUTED, cargo.getDelivery().getRoutingStatus());

		cargo.assignToRoute(bad);
		assertEquals(RoutingStatus.MISROUTED, cargo.getDelivery().getRoutingStatus());

		cargo.assignToRoute(good);
		assertEquals(RoutingStatus.ROUTED, cargo.getDelivery().getRoutingStatus());
	}

	@Test
	void testLastKnownLocationUnknownWhenNoEvents() {
		Cargo cargo = new Cargo(new TrackingId("XYZ"),
				new RouteSpecification(SampleLocations.STOCKHOLM, SampleLocations.MELBOURNE, new Date()));

		assertEquals(Location.UNKNOWN, cargo.getDelivery().getLastKnownLocation());
	}

	@Test
	void testLastKnownLocationReceived() throws Exception {
		Cargo cargo = populateCargoReceivedStockholm();

		assertEquals(SampleLocations.STOCKHOLM, cargo.getDelivery().getLastKnownLocation());
	}

	@Test
	void testLastKnownLocationClaimed() throws Exception {
		Cargo cargo = populateCargoClaimedMelbourne();

		assertEquals(SampleLocations.MELBOURNE, cargo.getDelivery().getLastKnownLocation());
	}

	@Test
	void testLastKnownLocationUnloaded() throws Exception {
		Cargo cargo = populateCargoOffHongKong();

		assertEquals(SampleLocations.HONGKONG, cargo.getDelivery().getLastKnownLocation());
	}

	@Test
	void testLastKnownLocationloaded() throws Exception {
		Cargo cargo = populateCargoOnHamburg();

		assertEquals(SampleLocations.HAMBURG, cargo.getDelivery().getLastKnownLocation());
	}

	@Test
	void testIsUnloadedAtFinalDestination() {
		Cargo cargo = setUpCargoWithItinerary(SampleLocations.HANGZOU, SampleLocations.TOKYO, SampleLocations.NEWYORK);
		assertFalse(cargo.getDelivery().isUnloadedAtDestination());

		events.add(new HandlingEvent(cargo, new Date(10), new Date(), HandlingEvent.Type.RECEIVE,
				SampleLocations.HANGZOU));
		cargo.deriveDeliveryProgress(new HandlingHistory(events));
		assertFalse(cargo.getDelivery().isUnloadedAtDestination());

		Voyage voyage = new Voyage.Builder(new VoyageNumber("0123"), SampleLocations.HANGZOU)
				.addMovement(SampleLocations.NEWYORK, new Date(), new Date()).build();

		events.add(new HandlingEvent(cargo, new Date(20), new Date(), HandlingEvent.Type.UNLOAD, SampleLocations.TOKYO,
				voyage));
		cargo.deriveDeliveryProgress(new HandlingHistory(events));
		assertFalse(cargo.getDelivery().isUnloadedAtDestination());

		events.add(new HandlingEvent(cargo, new Date(30), new Date(), HandlingEvent.Type.CUSTOMS,
				SampleLocations.NEWYORK));
		cargo.deriveDeliveryProgress(new HandlingHistory(events));
		assertFalse(cargo.getDelivery().isUnloadedAtDestination());

		events.add(new HandlingEvent(cargo, new Date(40), new Date(), HandlingEvent.Type.UNLOAD,
				SampleLocations.NEWYORK, voyage));
		cargo.deriveDeliveryProgress(new HandlingHistory(events));
		assertTrue(cargo.getDelivery().isUnloadedAtDestination());
	}

	private Cargo populateCargoReceivedStockholm() throws Exception {
		Cargo cargo = new Cargo(new TrackingId("XYZ"),
				new RouteSpecification(SampleLocations.STOCKHOLM, SampleLocations.MELBOURNE, new Date()));

		HandlingEvent event = new HandlingEvent(cargo, getDate("2007-12-01"), new Date(), HandlingEvent.Type.RECEIVE,
				SampleLocations.STOCKHOLM);
		events.add(event);
		cargo.deriveDeliveryProgress(new HandlingHistory(events));

		return cargo;
	}

	private Cargo populateCargoClaimedMelbourne() throws Exception {
		Cargo cargo = populateCargoOffMelbourne();

		events.add(new HandlingEvent(cargo, getDate("2007-12-09"), new Date(), HandlingEvent.Type.CLAIM,
				SampleLocations.MELBOURNE));
		cargo.deriveDeliveryProgress(new HandlingHistory(events));

		return cargo;
	}

	private Cargo populateCargoOffHongKong() throws Exception {
		Cargo cargo = new Cargo(new TrackingId("XYZ"),
				new RouteSpecification(SampleLocations.STOCKHOLM, SampleLocations.MELBOURNE, new Date()));

		events.add(new HandlingEvent(cargo, getDate("2007-12-01"), new Date(), HandlingEvent.Type.LOAD,
				SampleLocations.STOCKHOLM, voyage));
		events.add(new HandlingEvent(cargo, getDate("2007-12-02"), new Date(), HandlingEvent.Type.UNLOAD,
				SampleLocations.HAMBURG, voyage));

		events.add(new HandlingEvent(cargo, getDate("2007-12-03"), new Date(), HandlingEvent.Type.LOAD,
				SampleLocations.HAMBURG, voyage));
		events.add(new HandlingEvent(cargo, getDate("2007-12-04"), new Date(), HandlingEvent.Type.UNLOAD,
				SampleLocations.HONGKONG, voyage));

		cargo.deriveDeliveryProgress(new HandlingHistory(events));

		return cargo;
	}

	private Cargo populateCargoOnHamburg() throws Exception {
		Cargo cargo = new Cargo(new TrackingId("XYZ"),
				new RouteSpecification(SampleLocations.STOCKHOLM, SampleLocations.MELBOURNE, new Date()));

		events.add(new HandlingEvent(cargo, getDate("2007-12-01"), new Date(), HandlingEvent.Type.LOAD,
				SampleLocations.STOCKHOLM, voyage));
		events.add(new HandlingEvent(cargo, getDate("2007-12-02"), new Date(), HandlingEvent.Type.UNLOAD,
				SampleLocations.HAMBURG, voyage));
		events.add(new HandlingEvent(cargo, getDate("2007-12-03"), new Date(), HandlingEvent.Type.LOAD,
				SampleLocations.HAMBURG, voyage));

		cargo.deriveDeliveryProgress(new HandlingHistory(events));

		return cargo;
	}

	private Cargo populateCargoOffMelbourne() throws Exception {
		Cargo cargo = new Cargo(new TrackingId("XYZ"),
				new RouteSpecification(SampleLocations.STOCKHOLM, SampleLocations.MELBOURNE, new Date()));

		events.add(new HandlingEvent(cargo, getDate("2007-12-01"), new Date(), HandlingEvent.Type.LOAD,
				SampleLocations.STOCKHOLM, voyage));
		events.add(new HandlingEvent(cargo, getDate("2007-12-02"), new Date(), HandlingEvent.Type.UNLOAD,
				SampleLocations.HAMBURG, voyage));

		events.add(new HandlingEvent(cargo, getDate("2007-12-03"), new Date(), HandlingEvent.Type.LOAD,
				SampleLocations.HAMBURG, voyage));
		events.add(new HandlingEvent(cargo, getDate("2007-12-04"), new Date(), HandlingEvent.Type.UNLOAD,
				SampleLocations.HONGKONG, voyage));

		events.add(new HandlingEvent(cargo, getDate("2007-12-05"), new Date(), HandlingEvent.Type.LOAD,
				SampleLocations.HONGKONG, voyage));
		events.add(new HandlingEvent(cargo, getDate("2007-12-07"), new Date(), HandlingEvent.Type.UNLOAD,
				SampleLocations.MELBOURNE, voyage));

		cargo.deriveDeliveryProgress(new HandlingHistory(events));

		return cargo;
	}

	@Test
	void testIsMisdirected() throws Exception {
		Cargo cargo = new Cargo(new TrackingId("TRKID"),
				new RouteSpecification(SampleLocations.SHANGHAI, SampleLocations.GOTHENBURG, new Date()));
		assertFalse(cargo.getDelivery().isMisdirected());

		cargo = setUpCargoWithItinerary(SampleLocations.SHANGHAI, SampleLocations.ROTTERDAM,
				SampleLocations.GOTHENBURG);

		assertFalse(cargo.getDelivery().isMisdirected());

		Collection<HandlingEvent> handlingEvents = new ArrayList<>();

		handlingEvents.add(new HandlingEvent(cargo, new Date(10), new Date(20), HandlingEvent.Type.RECEIVE,
				SampleLocations.SHANGHAI));
		handlingEvents.add(new HandlingEvent(cargo, new Date(30), new Date(40), HandlingEvent.Type.LOAD,
				SampleLocations.SHANGHAI, voyage));
		handlingEvents.add(new HandlingEvent(cargo, new Date(50), new Date(60), HandlingEvent.Type.UNLOAD,
				SampleLocations.ROTTERDAM, voyage));
		handlingEvents.add(new HandlingEvent(cargo, new Date(70), new Date(80), HandlingEvent.Type.LOAD,
				SampleLocations.ROTTERDAM, voyage));
		handlingEvents.add(new HandlingEvent(cargo, new Date(90), new Date(100), HandlingEvent.Type.UNLOAD,
				SampleLocations.GOTHENBURG, voyage));
		handlingEvents.add(new HandlingEvent(cargo, new Date(110), new Date(120), HandlingEvent.Type.CLAIM,
				SampleLocations.GOTHENBURG));
		handlingEvents.add(new HandlingEvent(cargo, new Date(130), new Date(140), HandlingEvent.Type.CUSTOMS,
				SampleLocations.GOTHENBURG));

		events.addAll(handlingEvents);
		cargo.deriveDeliveryProgress(new HandlingHistory(events));
		assertFalse(cargo.getDelivery().isMisdirected());

		cargo = setUpCargoWithItinerary(SampleLocations.SHANGHAI, SampleLocations.ROTTERDAM,
				SampleLocations.GOTHENBURG);
		handlingEvents = new ArrayList<>();

		handlingEvents.add(
				new HandlingEvent(cargo, new Date(), new Date(), HandlingEvent.Type.RECEIVE, SampleLocations.HANGZOU));
		events.addAll(handlingEvents);
		cargo.deriveDeliveryProgress(new HandlingHistory(events));

		assertTrue(cargo.getDelivery().isMisdirected());

		cargo = setUpCargoWithItinerary(SampleLocations.SHANGHAI, SampleLocations.ROTTERDAM,
				SampleLocations.GOTHENBURG);
		handlingEvents = new ArrayList<>();

		handlingEvents.add(new HandlingEvent(cargo, new Date(10), new Date(20), HandlingEvent.Type.RECEIVE,
				SampleLocations.SHANGHAI));
		handlingEvents.add(new HandlingEvent(cargo, new Date(30), new Date(40), HandlingEvent.Type.LOAD,
				SampleLocations.SHANGHAI, voyage));
		handlingEvents.add(new HandlingEvent(cargo, new Date(50), new Date(60), HandlingEvent.Type.UNLOAD,
				SampleLocations.ROTTERDAM, voyage));
		handlingEvents.add(new HandlingEvent(cargo, new Date(70), new Date(80), HandlingEvent.Type.LOAD,
				SampleLocations.ROTTERDAM, voyage));

		events.addAll(handlingEvents);
		cargo.deriveDeliveryProgress(new HandlingHistory(events));

		assertTrue(cargo.getDelivery().isMisdirected());

		cargo = setUpCargoWithItinerary(SampleLocations.SHANGHAI, SampleLocations.ROTTERDAM,
				SampleLocations.GOTHENBURG);
		handlingEvents = new ArrayList<>();

		handlingEvents.add(new HandlingEvent(cargo, new Date(10), new Date(20), HandlingEvent.Type.RECEIVE,
				SampleLocations.SHANGHAI));
		handlingEvents.add(new HandlingEvent(cargo, new Date(30), new Date(40), HandlingEvent.Type.LOAD,
				SampleLocations.SHANGHAI, voyage));
		handlingEvents.add(new HandlingEvent(cargo, new Date(50), new Date(60), HandlingEvent.Type.UNLOAD,
				SampleLocations.ROTTERDAM, voyage));
		handlingEvents.add(
				new HandlingEvent(cargo, new Date(), new Date(), HandlingEvent.Type.CLAIM, SampleLocations.ROTTERDAM));

		events.addAll(handlingEvents);
		cargo.deriveDeliveryProgress(new HandlingHistory(events));

		assertTrue(cargo.getDelivery().isMisdirected());
	}

	private Cargo setUpCargoWithItinerary(Location origin, Location midpoint, Location destination) {
		Cargo cargo = new Cargo(new TrackingId("CARGO1"), new RouteSpecification(origin, destination, new Date()));

		Itinerary itinerary = new Itinerary(Arrays.asList(new Leg(voyage, origin, midpoint, new Date(), new Date()),
				new Leg(voyage, midpoint, destination, new Date(), new Date())));

		cargo.assignToRoute(itinerary);
		return cargo;
	}

	private Date getDate(String isoFormat) throws ParseException {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		return dateFormat.parse(isoFormat);
	}
}
