package co.edu.unicauca.diabetescare.control;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.AccessTokenProvider;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unicauca.diabetescare.plan.SendMessageByEmailPlan;

@RestController
@RequestMapping("/oauth2/redirectUri")
public class Oauth2RedirectUriController {
	
	@Autowired
	private OAuth2ClientContext oauth2Context;
	
	@Autowired
	private AccessTokenProvider accessTokenProvider;
	
	@Autowired
	public OAuth2ProtectedResourceDetails gmailResource;
	
	private static final Logger log = LoggerFactory.getLogger(SendMessageByEmailPlan.class);
	
	@RequestMapping(method = RequestMethod.GET)
	public void continueFlow(HttpServletRequest request)
	{
		AccessTokenRequest accessTokenRequest = oauth2Context.getAccessTokenRequest();
		//TODO try to recover states
		//accessTokenRequest.setStateKey(null);
		log.debug(""+accessTokenProvider.obtainAccessToken(gmailResource, accessTokenRequest ));
		//TODO refresh token
		//TODO redirect the resource that need the token
	}
}
