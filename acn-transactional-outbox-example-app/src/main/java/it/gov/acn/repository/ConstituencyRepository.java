package it.gov.acn.repository;

import it.gov.acn.model.Constituency;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConstituencyRepository extends CrudRepository<Constituency, UUID> {


}