package it.gov.acn.repository;

import it.gov.acn.model.Constituency;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ConstituencyRepository extends CrudRepository<Constituency, UUID> {



}