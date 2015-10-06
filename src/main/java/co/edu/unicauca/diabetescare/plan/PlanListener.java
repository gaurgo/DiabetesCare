package co.edu.unicauca.diabetescare.plan;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;

import javax.xml.bind.DatatypeConverter;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.engine.delegate.VariableScope;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import co.edu.unicauca.diabetescare.DataInterpreter;
import co.edu.unicauca.diabetescare.persistency.VirtuosoStorageRepository;

@Service
public class PlanListener implements ExecutionListener, TaskListener{
	
		
	private static final Logger log = LoggerFactory.getLogger(PlanListener.class);
	
	
	@Autowired
	private VirtuosoStorageRepository virtuosoStorageRepository;
	
	@Autowired
	protected DataInterpreter dataInterpreter;
	
	protected void startProcess(String planId, String planName)
	{
		log.debug("Starting  process of plan:"+planId);
		String processId=UUID.randomUUID().toString();
		String timeIntervalId=UUID.randomUUID().toString();
		String PointInTimeId=UUID.randomUUID().toString();
		String InformationObjectId=UUID.randomUUID().toString();
		
		String triples="dm2co:"+processId+" a btl2:Process. "+
				"dm2co:"+planId+" a dm2co:"+planName+". "+
				"dm2co:"+planId+" btl2:hasRealization dm2co:"+processId+". "+
				"dm2co:"+processId+" btl2:projectsOnto "+"dm2co:"+timeIntervalId+". "+
				"dm2co:"+timeIntervalId+" a btl2:TimeInterval. "+
				"dm2co:"+PointInTimeId+" a btl2:PointInTime. "+
				"dm2co:"+InformationObjectId+" a btl2:InformationObject. "+
				"dm2co:"+timeIntervalId+" btl2:hasBoundary dm2co:"+PointInTimeId+". "+
				"dm2co:"+InformationObjectId+" btl2:represents dm2co:"+PointInTimeId+". "+
				"dm2co:"+InformationObjectId+" rdfs:label \"start time\"@en . "+
				"dm2co:"+InformationObjectId+" rdfs:label \"tiempo de inicio\"@es . "+
				"dm2co:"+InformationObjectId+" dm2co:hasValue \""+DatatypeConverter.printDateTime(Calendar.getInstance())+"\"^^xsd:dateTime . ";
		virtuosoStorageRepository.insertTriples(triples);
		
	}
	
	protected void stopProcess(String planId)
	{
		log.debug("Stoping process of plan:"+planId);
		
		String variablesToSelect="?timeInterval";
		String wheretriples="dm2co:"+planId+" btl2:hasRealization ?processId. ?processId btl2:projectsOnto ?timeInterval";
		ObjectNode resultBody=virtuosoStorageRepository.select(variablesToSelect, wheretriples);
		String timeIntervalId=resultBody.findValue("timeInterval").get("value").asText();
		timeIntervalId=timeIntervalId.substring(timeIntervalId.indexOf("#")+1);
		log.debug("timeIntervalId:"+timeIntervalId);
		
		String PointInTimeId=UUID.randomUUID().toString();
		String InformationObjectId=UUID.randomUUID().toString();
		
		String triples= "dm2co:"+PointInTimeId+" a btl2:PointInTime. "+
				"dm2co:"+InformationObjectId+" a btl2:InformationObject. "+
				"dm2co:"+timeIntervalId+" btl2:hasBoundary dm2co:"+PointInTimeId+". "+
				"dm2co:"+InformationObjectId+" btl2:represents dm2co:"+PointInTimeId+". "+
				"dm2co:"+InformationObjectId+" rdfs:label \"end time\"@en . "+
				"dm2co:"+InformationObjectId+" rdfs:label \"tiempo de finalización\"@es . "+
				"dm2co:"+InformationObjectId+" dm2co:hasValue \""+DatatypeConverter.printDateTime(Calendar.getInstance())+"\"^^xsd:dateTime . ";
		virtuosoStorageRepository.insertTriples(triples);
	}
	
	protected void updateVariables(Map<String,Object> variables, String planId, String variablesLanguage)
	{		
		for (Iterator<Entry<String, Object>> iterator = variables.entrySet().iterator(); iterator.hasNext();) {
			Entry<String, Object> variable = (Entry<String, Object>) iterator.next();
			String variableName=variable.getKey();
			variableName=variableName.replace("_", " ");
			String variableValue=variable.getValue()+"";
			String variableClass = variable.getValue().getClass().getSimpleName();
			log.trace("Variable class:"+variableClass);
			
			String triples="dm2co:"+planId+" btl2:hasPart ?planPart."+
//						" ?planPart rdfs:label \""+variableName+"\"."+
						" ?planPart rdfs:label \""+variableName+"\"@"+variablesLanguage+"."+
						" ?planPart dm2co:hasValue ?prevVariableValue";
						
			
			
			String informationObjectId="";
			if(virtuosoStorageRepository.ask(triples))
			{
				log.debug("Variable "+variableName+" updated with value "+variableValue);
				ObjectNode result=virtuosoStorageRepository.select("?planPart ?prevVariableValue", triples);
				log.trace("?planPart(without parse)="+result);
				informationObjectId=result.findValue("planPart").get("value").asText();
				informationObjectId=informationObjectId.substring(informationObjectId.indexOf("#")+1);
				String prevVariableValue=result.findValue("prevVariableValue").get("value").asText();
				
				String variableTypeOrLanguage="";
				if(variableClass.equals("Date"))
				{
					variableTypeOrLanguage = "^^xsd:dateTime.";
				}
				else if(variableClass.equals("String"))
				{
					variableTypeOrLanguage="@"+variablesLanguage+". ";
				}
				else 
				{		
					variableTypeOrLanguage="^^xsd:"+variableClass.toLowerCase()+". ";
				}
				
				triples="dm2co:"+informationObjectId+" dm2co:hasValue \""+prevVariableValue+"\""+variableTypeOrLanguage;
				virtuosoStorageRepository.deleteTriples(triples);
			}
			else
			{
				log.debug("Variable "+variableName+" created with value "+variableValue);
				informationObjectId=UUID.randomUUID().toString();
			}
			

			String variableTypeOrLanguage="";
			
			if(variableClass.equals("Date"))
			{
				variableTypeOrLanguage="^^xsd:dateTime.";
				Calendar cal=Calendar.getInstance();
				cal.setTime((Date)variable.getValue());
				variableValue=DatatypeConverter.printDateTime(cal);
			}
			else if(variableClass.equals("String"))
			{
				variableTypeOrLanguage="@"+variablesLanguage+". ";
			}
			else 
			{		
				variableTypeOrLanguage="^^xsd:"+variableClass.toLowerCase()+". ";
			}
			
			triples="dm2co:"+planId+" btl2:hasPart dm2co:"+informationObjectId+". "+
					"dm2co:"+informationObjectId+" rdfs:label \""+variableName+"\"@"+variablesLanguage+". "+
					"dm2co:"+informationObjectId+" dm2co:hasValue \""+variableValue+"\""+variableTypeOrLanguage;
			
			virtuosoStorageRepository.insertTriples(triples);
		}
	}
	
	protected void updatePlanVariables(String planId, VariableScope execution) {
		String triples="dm2co:"+planId+" btl2:hasPart ?planPart."+
				" ?planPart rdfs:label ?variableName."+
				" ?planPart dm2co:hasValue ?variableValue. ";
		ObjectNode result=virtuosoStorageRepository.select("?variableName ?variableValue", triples);
		log.trace("Plan parts:"+result);
		List<JsonNode> variableNames = result.findValues("variableName");
		List<JsonNode> variableValues = result.findValues("variableValue");
		Iterator<JsonNode> iterator = variableValues.iterator();
		for (JsonNode variableName : variableNames) {
			JsonNode variableValue = iterator.next();
			if(variableValue.get("type").asText().equals("literal"))
			{
				//TODO how to have variable of many languages
				execution.setVariable(variableName.get("value").asText().replace(" ", "_"), variableValue.get("value").asText());
				log.debug("Literal process variable "+variableName.get("value").asText().replace(" ", "_")+" updated to "+variableValue.get("value").asText());
			}else
			{
				String datatype=variableValue.get("datatype").asText();
				datatype=datatype.substring(datatype.indexOf("#")+1);
				if(datatype.equals("dateTime"))
				{
					Calendar cal = DatatypeConverter.parseDateTime(variableValue.get("value").asText());
					execution.setVariable(variableName.get("value").asText().replace(" ", "_"), cal.getTime());
					log.debug("Date process variable "+variableName.get("value").asText()+" updated to "+cal.getTime());
				} else
				{
					//For Integer,Short, Long, Double and Float
					char[] tempChars = datatype.toCharArray();
					tempChars[0] = Character.toUpperCase(tempChars[0]);
					datatype=String.valueOf(tempChars);
					datatype="java.lang."+datatype;
					Constructor<?> constructor;
					try {
						constructor = Class.forName(datatype).getDeclaredConstructor(Class.forName("java.lang.String"));
						Object object = constructor.newInstance(variableValue.get("value").asText());
						execution.setVariable(variableName.get("value").asText().replace(" ", "_"), object );
						log.debug(datatype+" process variable "+variableName.get("value").asText().replace(" ", "_")+" updated to "+object.toString());
					} catch (NoSuchMethodException | SecurityException | ClassNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}   	
				}
					
			}
		}
	}
	
	@Override
	public void notify(DelegateExecution execution) throws Exception{
		log.debug("eventName:"+execution.getEventName());
		log.debug("currentActivityId:"+execution.getCurrentActivityId());
		ProcessDefinition processDefinition = execution.getProcessEngineServices().getRepositoryService().createProcessDefinitionQuery().processDefinitionId(execution.getProcessDefinitionId()).singleResult();
		log.debug("Plan class:"+processDefinition.getKey());
		String planId=""+execution.getVariable("planId");
		
		if(planId.equals("null"))
		{
			planId=UUID.randomUUID().toString();
			execution.setVariable("planId", planId);
		}
				
		if(execution.getEventName()==ExecutionListener.EVENTNAME_START && execution.getCurrentActivityId().startsWith("Start"))
		{	

			startProcess(planId,processDefinition.getKey());		
			
		} else if(execution.getEventName()==ExecutionListener.EVENTNAME_END && execution.getCurrentActivityId().startsWith("End"))
		{
			stopProcess(planId);
		} else {
			log.trace("Updating process variables"+execution.getVariables());
			updateVariables(execution.getVariables(),planId,"en");
			try {
				dataInterpreter.analyzeData("en");
				updatePlanVariables(planId,execution);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	@Override
	public void notify(DelegateTask delegateTask){
		log.debug("eventName:"+delegateTask.getEventName());
		log.debug("currentActivityId:"+delegateTask.getName());
		String planId=""+delegateTask.getVariable("planId");
		log.trace("Updating process variables"+delegateTask.getVariables());
		updateVariables(delegateTask.getVariables(),planId,"en");
		try {
			dataInterpreter.analyzeData("en");
			updatePlanVariables(planId,delegateTask);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
