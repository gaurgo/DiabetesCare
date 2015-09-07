package co.edu.unicauca.diabetescare;



import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestOperations;
import org.topbraid.spin.inference.DefaultSPINRuleComparator;
import org.topbraid.spin.inference.SPINInferences;
import org.topbraid.spin.inference.SPINRuleComparator;
import org.topbraid.spin.system.SPINModuleRegistry;
import org.topbraid.spin.util.CommandWrapper;
import org.topbraid.spin.util.JenaUtil;
import org.topbraid.spin.util.SPINQueryFinder;
import org.topbraid.spin.vocabulary.SPIN;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.compose.MultiUnion;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

import co.edu.unicauca.diabetescare.persistency.VirtuosoStorageRepository;
import co.edu.unicauca.diabetescare.plan.PlanComposer;

@Service
public class DataInterpreter {

	private static final Logger log = LoggerFactory.getLogger(DataInterpreter.class);
	
	@Autowired
	private RestOperations diabetesCareRestTemplate;
	
	@Autowired
	PlanComposer planComposer;
	
	@Autowired
	private VirtuosoStorageRepository virtuosoStorageRepository;
	
	@Value("${security.user.password}")
	private String defaultUserPassword;
	
	@Value("${server.port}")
	private String serverPort;
	
	@Value("${server.address}")
	private String serverAddress;
	
	@Value("${server.context-path}")
	private String serverContextPath;
	
	@Value("${diabetescare.server.email}")
	private String serverEmail;
	
	private String bpmnPlan;

	private OntModel owlrlModel;

	private OntModel queryModel;
	
	
	public void init() throws Exception
	{
		// Initialize system functions and templates
		SPINModuleRegistry.get().init();
		
		// Load OWL RL library from the web
		log.info("Loading OWL RL ontology...");
		owlrlModel = loadModelWithImports("http://topbraid.org/spin/owlrl-all");
		
		// Register any new functions defined in OWL RL
		SPINModuleRegistry.get().registerAll(owlrlModel, null);
		
		
		Model newTriples=runInferencesWithOWLRL();	
			
		insertNewTriples(newTriples);
	}
	
	private Model runInferencesWithOWLRL() throws Exception
	{
		log.info("Loading domain ontology...");
		queryModel = loadModelWithImports(virtuosoStorageRepository.getDomainOntologyInputStream());
		
		
		// Create and add Model for inferred triples
		Model newTriples = ModelFactory.createDefaultModel();
		queryModel.addSubModel(newTriples);			
		
		// Build one big union Model of everything
		MultiUnion multiUnion = JenaUtil.createMultiUnion(new Graph[] {
			queryModel.getGraph(),
			owlrlModel.getGraph()
		});
		Model unionModel = ModelFactory.createModelForGraph(multiUnion);
		
		// Collect rules (and template calls) defined in OWL RL
		Map<Resource,List<CommandWrapper>> cls2Query = SPINQueryFinder.getClass2QueryMap(unionModel, queryModel, SPIN.rule, true, false);
		Map<Resource,List<CommandWrapper>> cls2Constructor = SPINQueryFinder.getClass2QueryMap(queryModel, queryModel, SPIN.constructor, true, false);
		SPINRuleComparator comparator = new DefaultSPINRuleComparator(queryModel);

		// Run all inferences
		log.info("Running SPIN inferences...");
		SPINInferences.run(queryModel, newTriples, cls2Query, cls2Constructor, null, null, false, SPIN.rule, comparator, null);
		log.info("Inferred triples: " + newTriples.size());
		return newTriples;
	}
	
	private Model runInferences() throws Exception
	{
		//TODO:Only reload individuals
		// Load domain model with imports
		log.info("reloading domain ontology...");
		queryModel = loadModelWithImports(virtuosoStorageRepository.getDomainOntologyInputStream());
					
		
		// Create and add Model for inferred triples
		Model newTriples = ModelFactory.createDefaultModel();
		queryModel.addSubModel(newTriples);			

		
		// Run all inferences
		log.info("Running SPIN inferences...");
		SPINInferences.run(queryModel, newTriples, null, null, false,  null);
		log.info("Inferred triples: " + newTriples.size());
		return newTriples;
	}
	
	public void analyzeData() throws Exception {
				
		Model newTriples = runInferences();
		insertNewTriples(newTriples);
		
		Model temp_model = ModelFactory.createDefaultModel();
		Resource executablePlanClass = temp_model.createResource("http://purl.org/unicauca/dm2co#ExecutableInServerPlan");
		Property typeProperty = temp_model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		
		
		for (StmtIterator statements = queryModel.listStatements(null,typeProperty,executablePlanClass); statements.hasNext();) {
			Statement statement = (Statement) statements.next();
			String planId=statement.getSubject().toString();
			planId=planId.substring(planId.indexOf("dm2co")+6);
			log.debug("Plan id:"+planId);
			String planName=null;
			
			JsonNodeFactory nodeFactory = new JsonNodeFactory(false);

			ObjectNode variables = nodeFactory.objectNode();
						
			Property hasRealizationProperty = temp_model.createProperty("http://purl.org/biotop/btl2.owl#hasRealization");
			StmtIterator realizationProcess=queryModel.listStatements(statement.getSubject(),hasRealizationProperty,(RDFNode)null);
			//TODO: Ignore cancelled plans (cancel is an information object)
			if(!realizationProcess.hasNext())
			{
				for (StmtIterator properties = statement.getSubject().listProperties(typeProperty); properties.hasNext();) {
					Statement property = (Statement) properties.next();
					planName=null;
					log.trace(statement.getSubject()+" a "+property.getObject());
					if (property.getObject().toString().equals("http://purl.org/unicauca/dm2co#SendMessageByEmailPlan"))
					{
						log.debug("Plan type:"+property.getObject());
						String recipient=null;
						String message=null;
						planName="SendMessageByEmailPlan";
							
						for (StmtIterator properties2 = statement.getSubject().listProperties(temp_model.createProperty("http://purl.org/unicauca/dm2co#hasValue")); properties2.hasNext();) {
							Statement property2 = (Statement) properties2.next();
							String field=property2.getObject().toString();
							log.debug("Plan "+field.substring(0, field.indexOf(':')+1)+field.substring(field.indexOf(':')+1));
							if(field.substring(0, field.indexOf(':')).equals("recipient"))
							{
								recipient=field.substring(field.indexOf(':')+1);
							} else if(field.substring(0, field.indexOf(':')).equals("message")) {
								message=field.substring(field.indexOf(':')+1);
							}
								
						}
						//TODO: Allow multiple recipients. For example in emergency case: send to carer, family member and EPS
						//TODO: load subject from rdf
						variables.set("planId", nodeFactory.textNode(planId));
						variables.set("subject", nodeFactory.textNode("Message from Diabetes Care"));
						variables.set("message", nodeFactory.textNode(message));
						variables.set("recipient", nodeFactory.textNode(recipient));
						variables.set("source", nodeFactory.textNode(serverEmail));
						bpmnPlan=planComposer.composeBPMN(planName);
						log.trace(bpmnPlan);
					} else if(property.getObject().toString().equals("http://purl.org/unicauca/dm2co#Type2DiabetesMellitusCarePlan")){
						log.debug("Plan type:"+property.getObject());
						planName="Type2DiabetesMellitusCarePlan";
						//TODO:add variables
						bpmnPlan=planComposer.composeBPMN(planName);
						log.trace(bpmnPlan);
					} 
					
					if(planName!=null)
					{
						Runnable r = new planTrigger(planName, variables);
						new Thread(r).start();						
						break;
					}
				}
			}
			
		}
		
//				// Run all constraints
//				List<ConstraintViolation> cvs = SPINConstraints.check(ontModel, null);
//				System.out.println("Constraint violations:");
//				for(ConstraintViolation cv : cvs) {
//					System.out.println(" - at " + SPINLabels.get().getLabel(cv.getRoot()) + ": " + cv.getMessage());
//				}
//
//				// Run constraints on a single instance only
//				Resource person = cvs.get(0).getRoot();
//				List<ConstraintViolation> localCVS = SPINConstraints.check(person, null);
//				System.out.println("Constraint violations for " + SPINLabels.get().getLabel(person) + ": " + localCVS.size());
	
	}
	
	private void insertNewTriples(Model newTriples) {
		log.debug("Updating new triplets in virtuoso ...");
		String triples=" ";
		int count=0;
		for (StmtIterator statements = newTriples.listStatements(); statements.hasNext();) {
			Statement statement = (Statement) statements.next();
			if((!statement.getSubject().isAnon())&&(!statement.getObject().isAnon()))
			{
				if(!statement.getObject().isLiteral())
				{
					triples="<"+statement.getSubject()+"> <"+statement.getPredicate()+"> <"+statement.getObject()+">. ";
				}
				else
				{
					if(statement.getLiteral().getDatatypeURI()!=null)
					{
						triples="<"+statement.getSubject()+"> <"+statement.getPredicate()+"> \""+statement.getLiteral().getValue()+"\"^^<"+statement.getLiteral().getDatatypeURI()+">. ";
					}
					else
					{
						triples="<"+statement.getSubject()+"> <"+statement.getPredicate()+"> \""+statement.getLiteral().getValue()+"\". ";
					}
				}
				if(!virtuosoStorageRepository.ask(triples))
				{
					virtuosoStorageRepository.insertTriples(triples);
					count++;
				}
			}
		}
		log.debug("Updated in virtuoso "+count+" triplets");
	}

	private static OntModel loadModelWithImports(String url) {
		Model baseModel = ModelFactory.createDefaultModel();
		baseModel.read(url);
		return JenaUtil.createOntologyModel(OntModelSpec.OWL_MEM,baseModel);
	}
	
	private static OntModel loadModelWithImports(InputStream is) throws Exception {
		Model baseModel = ModelFactory.createDefaultModel();
		baseModel.read(is,null);
		return JenaUtil.createOntologyModel(OntModelSpec.OWL_MEM,baseModel);
	}
	
	protected class planTrigger implements Runnable{

		private String planName;
		private ObjectNode variables;
		
		public planTrigger(String planName,ObjectNode variables)
		{
			this.planName=planName;
			this.variables=variables;
		}
		
		@Override
		public void run() {
			HttpHeaders requestHeaders = new HttpHeaders();
			requestHeaders.set("Content-Type", "application/xml");
			String basicCredentials = Base64.getEncoder().encodeToString (("user:"+defaultUserPassword).getBytes());
			requestHeaders.set("Authorization", "Basic " + basicCredentials);
			
			String variablesString=variables.toString();
			try {
				variablesString=URLEncoder.encode(variablesString, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			String body=bpmnPlan;
																
			HttpEntity<String> httpEntity = new HttpEntity<String>(body, requestHeaders);
			
			diabetesCareRestTemplate.exchange("http://"+serverAddress+":"+serverPort+serverContextPath+"/plan?processName="+planName+"&variablesString="+variablesString, HttpMethod.POST, httpEntity,ObjectNode.class);
		}
		
	}	
}
