package org.eclipse.cargotracker.infrastructure.persistence.jpa;

import java.io.Serializable;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;


import org.eclipse.cargotracker.domain.model.voyage.Voyage;
import org.eclipse.cargotracker.domain.model.voyage.VoyageNumber;
import org.eclipse.cargotracker.domain.model.voyage.VoyageRepository;

@ApplicationScoped
@Transactional
public class JpaVoyageRepository implements VoyageRepository, Serializable {

	private static final long serialVersionUID = 1L;

	@Inject
	private EntityManager entityManager;

	@Override
	public Voyage find(VoyageNumber voyageNumber) {
		return entityManager.createNamedQuery("Voyage.findByVoyageNumber", Voyage.class)
				.setParameter("voyageNumber", voyageNumber).getSingleResult();
	}

	@Override
	public List<Voyage> findAll() {
		return entityManager.createNamedQuery("Voyage.findAll", Voyage.class).getResultList();
	}

}
