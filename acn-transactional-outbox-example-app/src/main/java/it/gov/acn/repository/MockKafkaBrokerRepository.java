package it.gov.acn.repository;

import it.gov.acn.model.MockKafkaBrokerMessage;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MockKafkaBrokerRepository extends CrudRepository<MockKafkaBrokerMessage, UUID> { }
