package com.autotune.processor;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import com.autotune.em.utils.EMUtils.QueueName;
import com.autotune.queue.AutotuneDTO;
import com.autotune.queue.AutotuneQueueFactory;
import com.autotune.queue.EventConsumer;
import com.autotune.queue.EventProducer;

public class RecMgrProcessor implements Processor {

	@Override
	public void process() {
		EventConsumer consumerEvnt = new EventConsumer(AutotuneQueueFactory.getQueue(QueueName.RECMGRQUEUE.name()));
		FutureTask<AutotuneDTO> futherTask = new FutureTask<AutotuneDTO>(consumerEvnt);
		AutotuneDTO recMgrInputDTO = null;
		
		try {
			
			while(futherTask.isDone()) {
				recMgrInputDTO = futherTask.get();
    			System.out.println("resultDTO --->" + recMgrInputDTO);
    			break;
    		}
    		Thread.sleep(100);
			
			// call the ML module for recommendation
			// business logic
			
			MLProcessor mlProcessor = new MLProcessor();
			AutotuneDTO mlProcessoedDTO = mlProcessor.process(recMgrInputDTO);
			// create a new DTO and send to the ExperimentManagerQueue
			EventProducer expMgrEventProducer = new EventProducer(AutotuneQueueFactory.getQueue(QueueName.EXPMGRQUEUE.name()), mlProcessoedDTO);
			FutureTask<AutotuneDTO> expMgrTask = new FutureTask<AutotuneDTO>(expMgrEventProducer);
			Thread t= new Thread(expMgrTask);
			t.start();
			
			AutotuneDTO returnRecmMgrDTO = expMgrTask.get(); 
			
			System.out.println("returnRecmMgrDTO returned value: " + returnRecmMgrDTO.toString());
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		ExpMgrProcessor expMgrProcessor = new ExpMgrProcessor();
		expMgrProcessor.process();
		
	}



}
