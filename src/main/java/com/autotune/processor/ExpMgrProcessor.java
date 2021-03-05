package com.autotune.processor;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import com.autotune.em.fsm.api.EMAbstractEvent;
import com.autotune.em.fsm.api.EMFSMSimulator;
import com.autotune.em.fsm.api.EMFiniteStateMachine;
import com.autotune.em.fsm.events.RecommendedConfigReceiveEvent;
import com.autotune.em.utils.EMUtils.QueueName;
import com.autotune.queue.AutotuneDTO;
import com.autotune.queue.AutotuneExecutor;
import com.autotune.queue.AutotuneQueueFactory;
import com.autotune.queue.EventConsumer;

public class ExpMgrProcessor implements Processor {
	private AutotuneExecutor executorService;
	
	public ExpMgrProcessor() {
		executorService = new AutotuneExecutor();
	}
	
	@Override
	public void process() {
		AutotuneDTO recMgrOutputDTO = null;
		EventConsumer recMgrEventConsumer = new EventConsumer(AutotuneQueueFactory.getQueue(QueueName.EXPMGRQUEUE.name()));
		FutureTask<AutotuneDTO> expMgrInputTask = new FutureTask<AutotuneDTO>(recMgrEventConsumer);
		Thread t= new Thread(expMgrInputTask);
		t.start();
		
		try {
			AutotuneDTO returnRecmMgrDTO = expMgrInputTask.get(); 
			
			EMFSMSimulator fsmSimulator = new EMFSMSimulator();
			EMFiniteStateMachine fsm = fsmSimulator.getEMFiniteStateMachine();
			
			Callable<AutotuneDTO> configReceiveCall = new Callable<AutotuneDTO>() {
				@Override
				public AutotuneDTO call() throws Exception {
					EMAbstractEvent event = new RecommendedConfigReceiveEvent(returnRecmMgrDTO);
					fsm.fire(event);
					
					System.out.println("Currnet State: " +fsm.getCurrentState());
					return returnRecmMgrDTO;
				}
			};
			Future<AutotuneDTO> results = executorService.submitTask(configReceiveCall);
			
			if(results.isDone()) {
				
				recMgrOutputDTO = results.get();
			}
			
			
		} catch (InterruptedException ie) {
			ie.printStackTrace();
			
		} catch (ExecutionException ee) {
			ee.printStackTrace();
		}
		finally {
			
			System.out.println("recMgrOutputDTO :::" + recMgrOutputDTO);
			executorService.shutdown();
		}
	}
	
	
}
