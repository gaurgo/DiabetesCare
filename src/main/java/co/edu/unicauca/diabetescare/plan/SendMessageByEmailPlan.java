package co.edu.unicauca.diabetescare.plan;



import java.awt.Desktop;
import java.net.URI;
import java.util.Map;
import java.util.Map.Entry;


import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.UserRedirectRequiredException;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.node.ObjectNode;


@Service
public class SendMessageByEmailPlan extends PlanListener implements JavaDelegate {

	
	@Autowired
	private OAuth2RestTemplate emailRestTemplate;

		
	private static final Logger log = LoggerFactory.getLogger(SendMessageByEmailPlan.class);
	
	  public void execute(DelegateExecution delegate) throws Exception {
	       		  
		  	String userId="me";
			String uploadType="media";
			
			HttpHeaders requestHeaders = new HttpHeaders();
			requestHeaders.set("Content-Type", "message/rfc822");
			
								
			String body="Content-Type: text/plain; charset=\"UTF-8\"\n"+
					"MIME-Version: 1.0 Content-Transfer-Encoding: 8bit\n"+
					"to: "+delegate.getVariable("recipient")+"\n"+
					"from: "+delegate.getVariable("source")+"\n"+
					"subject: "+delegate.getVariable("subject")+"\n\n"+
					delegate.getVariable("message");
			
			log.debug("Sending email with body:"+body);
					
			HttpEntity<String> httpEntity = new HttpEntity<String>(body, requestHeaders);
			try
			{
				ResponseEntity<ObjectNode> result = emailRestTemplate.exchange("https://www.googleapis.com/upload/gmail/v1/users/"+userId+"/messages/send?uploadType="+uploadType, HttpMethod.POST, httpEntity,ObjectNode.class);
				log.debug("Result="+result);
			}
			catch(UserRedirectRequiredException exception)
			{
				//Open the browser to give permission
				String redirectUrl=exception.getRedirectUri();
				redirectUrl=redirectUrl+"?";
				
				Map<String, String> parameters = exception.getRequestParams();
				for (Entry<String, String> e: parameters.entrySet()) {
			        redirectUrl=redirectUrl+e.getKey()+"="+e.getValue()+"&";
			    }
				
				
				if(Desktop.isDesktopSupported())
				{
				  Desktop.getDesktop().browse(new URI(redirectUrl));
				}
				else
				{
					Runtime runtime = Runtime.getRuntime();
					runtime.exec("xdg-open " + redirectUrl);
					log.info("Created new window in existing browser session.");
				}
				
			}
			 
		 
		  }

}
