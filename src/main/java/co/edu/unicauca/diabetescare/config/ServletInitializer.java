package co.edu.unicauca.diabetescare.config;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.camunda.bpm.admin.impl.web.bootstrap.AdminContainerBootstrap;
import org.camunda.bpm.cockpit.impl.web.bootstrap.CockpitContainerBootstrap;
import org.camunda.bpm.engine.rest.filter.CacheControlFilter;
import org.camunda.bpm.tasklist.impl.web.bootstrap.TasklistContainerBootstrap;
import org.camunda.bpm.webapp.impl.engine.ProcessEnginesFilter;
import org.camunda.bpm.webapp.impl.security.auth.AuthenticationFilter;
import org.camunda.bpm.webapp.impl.security.filter.SecurityFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import co.edu.unicauca.diabetescare.DataInterpreter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.ServletContextInitializer;

@Component
public class ServletInitializer implements ServletContextInitializer {


	private ServletContext servletContext;
	
	@Autowired
	private DataInterpreter dataInterpreter;
	
	private static final Logger log = LoggerFactory.getLogger(ServletInitializer.class);
	
	private static final EnumSet<DispatcherType> DISPATCHER_TYPES = EnumSet.of(DispatcherType.REQUEST);

	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		this.servletContext=servletContext;
				
		servletContext.addListener(new CockpitContainerBootstrap());
		servletContext.addListener(new AdminContainerBootstrap());
		servletContext.addListener(new TasklistContainerBootstrap());
		
		
		registerFilter("Authentication Filter", AuthenticationFilter.class, null, "/*");
		
		HashMap<String, String> securityFilterParameters = new HashMap<>();
        securityFilterParameters.put("configFile", "/WEB-INF/securityFilterRules.json");
        registerFilter("Security Filter", SecurityFilter.class, securityFilterParameters, "/*");
		
		registerFilter("Engines Filter", ProcessEnginesFilter.class, null, "/app/*");
		registerFilter("CacheControlFilter", CacheControlFilter.class, null, "/api/*");
		
		try {
			dataInterpreter.init();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private FilterRegistration registerFilter(final String filterName,
             final Class<? extends Filter> filterClass,
             final Map<String, String> initParameters,
             final String... urlPatterns)
	{
		FilterRegistration filterRegistration = servletContext.getFilterRegistration(filterName);
		
		if (filterRegistration == null)
		{
			filterRegistration = servletContext.addFilter(filterName, filterClass);
			filterRegistration.addMappingForUrlPatterns(DISPATCHER_TYPES, true, urlPatterns);
		
			if (initParameters != null)
			{
				filterRegistration.setInitParameters(initParameters);
			}
			
			log.debug("Filter {} registered for URL path {} ", filterName, urlPatterns);
		}
		
		return filterRegistration;
	}

}
