package co.edu.unicauca.diabetescare.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.AccessTokenProvider;
import org.springframework.security.oauth2.client.token.AccessTokenProviderChain;
import org.springframework.security.oauth2.client.token.ClientTokenServices;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;
import org.springframework.security.oauth2.common.AuthenticationScheme;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import co.edu.unicauca.diabetescare.model.ClientTokenStorage;
import co.edu.unicauca.diabetescare.persistency.ClientTokenStorageRepository;

@Configuration
@EnableWebMvc
public class WebMvcConfig extends WebMvcConfigurerAdapter {
	
	@Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
            registry.addResourceHandler("/app/**")
                    .addResourceLocations("/app/");
            registry.addResourceHandler("/api/**")
            .addResourceLocations("/api/");
    }
	
				
	@Configuration
	@EnableOAuth2Client
	protected static class ResourceConfiguration {
		
		@Autowired
		private OAuth2ClientContext oauth2Context;

		@Autowired
		private ClientTokenStorageRepository clientTokenStorageRepository;
		
		
		@Value("${diabetescare.client.id}")
		private String clientId;
		
		@Value("${diabetescare.client.secret}")
		private String clientSecret;
		
		@Value("${server.context-path}")
		private String serverContextPath;
			
		@Value("${server.port}")
		private String serverPort;
		
		@Value("${server.address}")
		private String serverAddress;
		
		
		@Bean
		public OAuth2ProtectedResourceDetails gmailResource(){
			  AuthorizationCodeResourceDetails resource = new AuthorizationCodeResourceDetails(); 
			  resource.setClientId(clientId); 
			  resource.setId(clientId); 
			  resource.setClientSecret(clientSecret);
			  resource.setClientAuthenticationScheme(AuthenticationScheme.header);
			  resource.setAuthenticationScheme(AuthenticationScheme.header);
			  resource.setUserAuthorizationUri("https://accounts.google.com/o/oauth2/auth");
			  resource.setAccessTokenUri("https://www.googleapis.com/oauth2/v3/token");
			  resource.setPreEstablishedRedirectUri("http://"+serverAddress+":"+serverPort+serverContextPath+"/oauth2/redirectUri");
			  resource.setUseCurrentUri(false);
			  List<String> scope=new ArrayList<String>();
			  scope.add("https://www.googleapis.com/auth/gmail.modify");
			  resource.setScope(scope);
			  
			  return resource;
			}
		
		@Bean
		public AccessTokenProvider accessTokenProvider()
		{
			AccessTokenProviderChain provider = new AccessTokenProviderChain(Arrays.asList(new AuthorizationCodeAccessTokenProvider()));
		    provider.setClientTokenServices(clientTokenServices());
			return provider;
		}
		
		@Bean(name="emailRestTemplate")
//		@Scope(value = "session", proxyMode = ScopedProxyMode.INTERFACES)
	    public OAuth2RestTemplate emailRestTemplate(AccessTokenProvider accessTokenProvider) {
			//TODO select the email provider
		    OAuth2RestTemplate template =new OAuth2RestTemplate(gmailResource(), oauth2Context);
		    template.setAccessTokenProvider(accessTokenProvider);
	        return template;
	    }
		
		@Bean
	    public RestTemplate diabetesCareRestTemplate() {
		    RestTemplate template =new RestTemplate();
	        return template;
	    }
		
		@Bean
	    public RestTemplate virtuosoRestTemplate() {
		    RestTemplate template =new RestTemplate();
		    return template;
	    }
		
		@Bean
		public ClientTokenServices clientTokenServices() {
			ClientTokenServices clientTokenServices=new DiabetesCareClientTokenService();
			return clientTokenServices;
		}
		
		protected class DiabetesCareClientTokenService implements  ClientTokenServices
		{

			@Override
			public void saveAccessToken(OAuth2ProtectedResourceDetails resource, Authentication authentication,
					OAuth2AccessToken accessToken) {
				ClientTokenStorage clientTokenStorage=new ClientTokenStorage(resource.getClientId(),resource.getScope(),authentication.getPrincipal().toString(),accessToken);
				clientTokenStorageRepository.save(clientTokenStorage);
				System.out.println("Token saved: "+clientTokenStorage);
			}
			
			@Override
			public void removeAccessToken(OAuth2ProtectedResourceDetails resource, Authentication authentication) {
				ClientTokenStorage clientTokenStorage=clientTokenStorageRepository.findByClientIdAndScopeAndPrincipal(resource.getClientId(),resource.getScope(),authentication.getPrincipal().toString());
				if(clientTokenStorage!=null)clientTokenStorageRepository.delete(clientTokenStorage);
			}
			
			@Override
			public OAuth2AccessToken getAccessToken(OAuth2ProtectedResourceDetails resource, Authentication authentication) {
				ClientTokenStorage clientTokenStorage=clientTokenStorageRepository.findByClientIdAndScopeAndPrincipal(resource.getClientId(),resource.getScope(),authentication.getPrincipal().toString());
				if(clientTokenStorage!=null)
				{
					return clientTokenStorage.getAccessToken();
				}
				else
				{
					return null;
				}
				
			}
		}


	}
}
