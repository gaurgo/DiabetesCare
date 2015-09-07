package co.edu.unicauca.diabetescare.control;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.repository.Deployment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


@RestController
@RequestMapping("/plan")
public class PlanController {
	
	@Autowired
    private RuntimeService runtimeService;
	
	@Autowired
    private ProcessEngine processEngine;
	
	private static final Logger log = LoggerFactory.getLogger(PlanController.class);
		
	@RequestMapping(method = RequestMethod.POST,consumes={"application/xml","text/xml","application/x-www-form-urlencoded"})
	public void createPlanWithSource(HttpServletResponse response,HttpServletRequest request, @RequestParam String processName, @RequestParam(required=false) String variablesString) throws IOException{
		//	response.setContentType("application/rdf+xml");
		response.setContentType("text/xml");
		
		//Load the plan in bpmn format
		List<Deployment> deployments = processEngine.getRepositoryService().createDeploymentQuery().deploymentName(processName).list();
		if(deployments.isEmpty())
		{
			if(request.getContentLength()>0)
			{
				processEngine.getRepositoryService().createDeployment().addInputStream("plan"+new Date().getTime()+".bpmn",request.getInputStream()).name(processName).deploy();	
			}
			else
			{
				processEngine.getRepositoryService().createDeployment().addClasspathResource("bpmn/"+processName+".bpmn").name(processName).deploy();
			}
			
		}
		
		Map<String,Object> variables=new HashMap<String,Object>();
		JsonNode variablesObject=null;
		
		if(variablesString!=null)
		{
			variablesString=URLDecoder.decode(variablesString, "UTF-8");
		
			ObjectMapper mapper = new ObjectMapper();
			variablesObject = mapper.readTree(variablesString);
		
			log.debug("Variables Object: "+variablesObject);
			for (Iterator<Entry<String, JsonNode>> iterator = variablesObject.fields(); iterator.hasNext();) {
				Entry<String, JsonNode> field = iterator.next();
				variables.put(field.getKey(), field.getValue().asText());
			}
		}
		
		
				
		
		//start plan
		runtimeService.startProcessInstanceByKey(processName,variables);
		
		
		//TODO return created code
    }
	
}
