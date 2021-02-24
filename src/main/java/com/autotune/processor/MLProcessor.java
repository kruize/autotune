package com.autotune.processor;

import com.autotune.queue.AutotuneDTO;

public class MLProcessor implements Processor {
	
	@Override
	public void process() {
		// TODO Auto-generated method stub
		System.out.println("In ML Processor");
//		System.out.println(dto.toString());
		
	}
	
	public AutotuneDTO process(AutotuneDTO mlInputDTO) {
		// ML business logic
		
		return mlInputDTO;
	}
}
