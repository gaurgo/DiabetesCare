package co.edu.unicauca.diabetescare.config;


import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

import co.edu.unicauca.diabetescare.plan.PlanListener;
import co.edu.unicauca.diabetescare.plan.SendMessageByEmailPlan;

import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.ManagementService;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.ProcessEngineConfiguration;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.spring.SpringProcessEngineConfiguration;
import org.camunda.bpm.engine.spring.container.ManagedProcessEngineFactoryBean;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;

/**
 * @author gustavo
 *
 */
@Configuration
public class CamundaConfig {
	
	@Value("${spring.datasource.url}")
	private String datasourceURL;
	
	@Value("${spring.datasource.username}")
	private String datasourceUser;
	
	@Value("${spring.datasource.password}")
	private String datasourcePassword;
	
	@Value("${spring.datasource.driver-class-name}")
	private String datasourceDriverClass;
	
		
	@Bean
	public TransactionAwareDataSourceProxy dataSource(){
		return new TransactionAwareDataSourceProxy(targetDataSource());
	}

	private DataSource targetDataSource() {
		DriverManagerDataSource datasource = new DriverManagerDataSource(datasourceURL, datasourceUser, datasourcePassword);
		datasource.setDriverClassName(datasourceDriverClass);
		return datasource; 
	}
	
//	@Bean
//	public SpringServletProcessApplication processApplication()
//	{
//		return new SpringServletProcessApplication();
//	}
	
	@Bean
	public DataSourceTransactionManager transactionManager(DataSource dataSource) {
		
		return new DataSourceTransactionManager(dataSource);
	}
	
	@Bean
	public SpringProcessEngineConfiguration processEngineConfiguration(DataSource dataSource,DataSourceTransactionManager transactionManager,SendMessageByEmailPlan sendMessageByEmail, PlanListener planListener){
		SpringProcessEngineConfiguration configuration=new SpringProcessEngineConfiguration();
		configuration.setProcessEngineName("default");
		configuration.setDataSource(dataSource);
		configuration.setTransactionManager(transactionManager);
		configuration.setDatabaseSchemaUpdate("true");
		configuration.setJobExecutorActivate(false);
		configuration.setHistory(ProcessEngineConfiguration.HISTORY_FULL);
		Map<Object, Object> beans=new HashMap<>();
		//TODO change name of beans using the class name (change also in models)
		beans.put("sendMessageByEmailPlan", sendMessageByEmail);
		beans.put("type2DiabetesMellitusCarePlan", planListener);
		configuration.setBeans(beans);
		return configuration;
	}
	
	@Bean
	public ProcessEngine processEngine(SpringProcessEngineConfiguration processEngineConfiguration)  throws Exception
	{
		ManagedProcessEngineFactoryBean processEngineFactory=new ManagedProcessEngineFactoryBean();
		processEngineFactory.setProcessEngineConfiguration(processEngineConfiguration);
		ProcessEngine processEngine=processEngineFactory.getObject();
		return processEngine;
	}
	
	@Bean(name = "repositoryService")
	public RepositoryService createRepositoryService(ProcessEngine processEngine) {
	    return processEngine.getRepositoryService();
	}
	
	@Bean(name = "runtimeService")
	public RuntimeService createRuntimeService(ProcessEngine processEngine) {
	    return processEngine.getRuntimeService();
	}
	
	@Bean(name = "taskService")
	public TaskService createTaskService(ProcessEngine processEngine) {
	    return processEngine.getTaskService();
	}

	@Bean(name = "historyService")
	public HistoryService createHistoryService(ProcessEngine processEngine) {
	    return processEngine.getHistoryService();
	}
	
	@Bean(name = "managementService")
	public ManagementService createManagementService(ProcessEngine processEngine) {
	    return processEngine.getManagementService();
	}
		
	
	
	@Bean
	public HttpServletDispatcher cockpitApi()
	{
		return new HttpServletDispatcher();
	}
	
	@Bean
	public ServletRegistrationBean registrationCockpitApi(@Qualifier("cockpitApi")HttpServletDispatcher cokpitApi)
	{
		ServletRegistrationBean registration = new ServletRegistrationBean(cokpitApi,"/api/cockpit/*");
		HashMap<String, String> cockpitApiParameters = new HashMap<>();
        cockpitApiParameters.put("javax.ws.rs.Application", "org.camunda.bpm.cockpit.impl.web.CockpitApplication");
        cockpitApiParameters.put("resteasy.servlet.mapping.prefix", "/api/cockpit");
        registration.setInitParameters(cockpitApiParameters);
        registration.setName("Cockpit Api");
		return registration;
	}
	
	@Bean
	public HttpServletDispatcher adminApi()
	{
		return new HttpServletDispatcher();
	}
	
	@Bean
	public ServletRegistrationBean registrationAdminApi(@Qualifier("adminApi") HttpServletDispatcher adminApi)
	{
		ServletRegistrationBean registration = new ServletRegistrationBean(adminApi,"/api/admin/*");
		HashMap<String, String> adminApiParameters = new HashMap<>();
	    adminApiParameters.put("javax.ws.rs.Application", "org.camunda.bpm.admin.impl.web.AdminApplication");
	    adminApiParameters.put("resteasy.servlet.mapping.prefix", "/api/admin");
        registration.setInitParameters(adminApiParameters);
        registration.setName("Admin Api");
		return registration;
	}
	
	@Bean
	public HttpServletDispatcher engineApi()
	{
		return new HttpServletDispatcher();
	}
	
	@Bean
	public ServletRegistrationBean registrationEngineApi(@Qualifier("engineApi") HttpServletDispatcher engineApi)
	{
		ServletRegistrationBean registration = new ServletRegistrationBean(engineApi,"/api/engine/*");
		HashMap<String, String> engineApiParameters = new HashMap<>();
        engineApiParameters.put("javax.ws.rs.Application", "org.camunda.bpm.webapp.impl.engine" +
                ".EngineRestApplication");
        engineApiParameters.put("resteasy.servlet.mapping.prefix", "/api/engine");
        registration.setInitParameters(engineApiParameters);
        registration.setName("Engine Api");
		return registration;
	}
	
	@Bean
	public HttpServletDispatcher taskList()
	{
		return new HttpServletDispatcher();
	}
	
	@Bean
	public ServletRegistrationBean registrationTaskList(@Qualifier("taskList") HttpServletDispatcher taskList)
	{
		ServletRegistrationBean registration = new ServletRegistrationBean(taskList,"/api/taskList/*");
		HashMap<String, String> taskListParameters = new HashMap<>();
		taskListParameters.put("javax.ws.rs.Application", "org.camunda.bpm.webapp.impl.engine" +
                ".TaskListApplication");
		taskListParameters.put("resteasy.servlet.mapping.prefix", "/api/taskList");
        registration.setInitParameters(taskListParameters);
        registration.setName("TaskList Api");
		return registration;
	}
	
}
