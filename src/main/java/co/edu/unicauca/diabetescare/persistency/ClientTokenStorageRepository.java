package co.edu.unicauca.diabetescare.persistency;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import co.edu.unicauca.diabetescare.model.ClientTokenStorage;

public interface ClientTokenStorageRepository extends MongoRepository<ClientTokenStorage, String>{
	public ClientTokenStorage findByClientIdAndScopeAndPrincipal(String clientId, List<String> scope, String principal);
}
