package co.edu.unicauca.diabetescare.plan;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.springframework.stereotype.Component;

@Component
public class PlanComposer {
	public String composeBPMN(String planName)
	{
				ClassLoader classLoader = getClass().getClassLoader();
				File file = new File(classLoader.getResource("bpmn/"+planName+".bpmn").getFile());
				
				try (BufferedReader br = new BufferedReader(new FileReader(file)))
				{
		 
					String bpmnSource = "";
					String line;
					while ((line = br.readLine()) != null) {
						bpmnSource+=line+"\n";
					}
					
					return new String(bpmnSource.getBytes("UTF-8"), "ISO-8859-1");
					
				} catch (IOException e) {
					e.printStackTrace();
				} 
				return null;
	}

}
