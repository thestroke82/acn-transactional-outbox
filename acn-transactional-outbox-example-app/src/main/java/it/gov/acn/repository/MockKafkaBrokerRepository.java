package it.gov.acn.repository;

import it.gov.acn.model.MockKafkaBrokerMessage;
import java.util.List;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MockKafkaBrokerRepository extends CrudRepository<MockKafkaBrokerMessage, UUID> { }
