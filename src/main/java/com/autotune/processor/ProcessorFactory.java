package com.autotune.processor;

import com.autotune.em.utils.EMUtils.EMProcessorType;

public class ProcessorFactory {
	
	
	public static Processor getProcessor(EMProcessorType proc) {
		Processor processor= null;
		switch (proc) {
			case DAPROCESSOR: 
				processor = new DAProcessor();
				break;
			case MLPROCESSOR:
				processor = new MLProcessor();
				break;
			case EXPMGRPROCESSOR:
				processor = new ExpMgrProcessor();
				break;
			case RECMGRPROCESSOR:
				processor = new RecMgrProcessor();
				break;
			default :
				System.out.println("not a valid processor type provided.");
				break;
		}
		return processor;
	}
}
