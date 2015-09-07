package co.edu.unicauca.diabetescare.model;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.security.oauth2.common.OAuth2AccessToken;

import lombok.Data;

@Data
public class ClientTokenStorage {
	
	@Id
	private String id;
	
	private String clientId;
	private List<String> scope;
	private String principal; 
	private OAuth2AccessToken accessToken;
	
	public ClientTokenStorage(String clientId, List<String> scope, String principal,
			OAuth2AccessToken accessToken) {
		super();
		this.id = id;
		this.clientId = clientId;
		this.scope = scope;
		this.principal = principal;
		this.accessToken = accessToken;
	}
	
	
	
}
