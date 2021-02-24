import com.autotune.em.utils.EMUtils.EMProcessorType;
import com.autotune.processor.Processor;
import com.autotune.processor.ProcessorFactory;

public class TestProcessor {

	public static void main(String[] args) {
		Processor DAProcessor = ProcessorFactory.getProcessor(EMProcessorType.DAPROCESSOR);
		DAProcessor.process();

	}

}