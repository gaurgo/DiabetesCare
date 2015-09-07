package co.edu.unicauca.diabetescare.config;

import javax.ws.rs.ApplicationPath;

import org.camunda.bpm.engine.rest.impl.CamundaRestResources;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


@Component
@ApplicationPath("/rest")
public class JerseyConfig extends ResourceConfig{
	private static final Logger log = LoggerFactory.getLogger(JerseyConfig.class);

	
	public JerseyConfig() {
		registerClasses(CamundaRestResources.getResourceClasses());
		registerClasses(CamundaRestResources.getConfigurationClasses());
        log.debug("JAX-RS application loaded");
    }
	 

}
