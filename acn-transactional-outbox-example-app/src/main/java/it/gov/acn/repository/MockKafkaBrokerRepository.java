package it.gov.acn.repository;

import it.gov.acn.model.MockKafkaBroker;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MockKafkaBrokerRepository extends CrudRepository<MockKafkaBroker, UUID> {
}
