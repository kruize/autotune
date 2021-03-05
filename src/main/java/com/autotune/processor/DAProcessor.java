package com.autotune.processor;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import org.json.JSONObject;

import com.autotune.em.utils.EMUtils.QueueName;
import com.autotune.em.utils.FileResourceUtils;
import com.autotune.queue.AutotuneDTO;
import com.autotune.queue.AutotuneQueue;
import com.autotune.queue.AutotuneQueueFactory;
import com.autotune.queue.EventProducer;

public class DAProcessor implements Processor {

	@Override
	public void process() {
		
		System.out.println("Executing Dependency analyzer processor");
		AutotuneQueueFactory queueFactory = new AutotuneQueueFactory();
		AutotuneQueue recMgrQueue = queueFactory.getQueue(QueueName.RECMGRQUEUE.name());
		
		//TODO reading the input json 
		String jsonFile ="em_input.json";
	    JSONObject jsonObject = FileResourceUtils.getFileFromResourceAsStream(DAProcessor.class, jsonFile);
	    
	    AutotuneDTO newDTO = new AutotuneDTO();
	    newDTO.setId(1000);
	    newDTO.setName("DA-DTO");
	    newDTO.setInputObject(jsonObject);
	   
	    EventProducer eventProducer = new EventProducer(recMgrQueue, newDTO);
	    	
    	FutureTask<AutotuneDTO> task = new FutureTask<AutotuneDTO>(eventProducer);
    	Thread t = new Thread(task);
    	t.start();
    	
    	try {
    		while(task.isDone()) {
    			AutotuneDTO resultDTO = task.get();
    			System.out.println("resultDTO --->" + resultDTO);
    			break;
    		}
    		Thread.sleep(100);
			
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
    	
    	// Initializing Recommendation Manager 
		RecMgrProcessor recMgrProcess = new RecMgrProcessor();
		recMgrProcess.process();
	}

}
