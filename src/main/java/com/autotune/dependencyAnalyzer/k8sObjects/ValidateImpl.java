package com.autotune.dependencyAnalyzer.k8sObjects;

import com.autotune.dependencyAnalyzer.util.AutotuneSupportedTypes;
import com.autotune.dependencyAnalyzer.util.DAConstants;
import com.autotune.dependencyAnalyzer.util.DAErrorConstants;

import java.util.HashMap;

public abstract class ValidateImpl implements Validate
{
	/**
	 * Check if the AutotuneConfig is valid
	 * @param map
	 * @return
	 */
	@Override
	public StringBuilder validateAutotuneConfig(HashMap<String, Object> map) {
		StringBuilder errorString = new StringBuilder();

		// Check if name is valid
		if (map.get(DAConstants.AutotuneConfigConstants.NAME) == null || ((String)map.get(DAConstants.AutotuneConfigConstants.NAME)).isEmpty()) {
			errorString.append(DAErrorConstants.AutotuneConfigErrors.AUTOTUNE_CONFIG_NAME_NULL);
		}

		// Check if either presence, layerPresenceQuery or layerPresenceLabel are set. presence field has highest priority.
		if (((String)map.get(DAConstants.AutotuneConfigConstants.PRESENCE)) == null ||
				!((String)map.get(DAConstants.AutotuneConfigConstants.PRESENCE)).equals(DAConstants.PRESENCE_ALWAYS)) {
			if ((map.get(DAConstants.AutotuneConfigConstants.LAYER_PRESENCE_LABEL) == null || map.get(DAConstants.AutotuneConfigConstants.LAYER_PRESENCE_LABEL_VALUE) == null) &&
					(map.get(DAConstants.AutotuneConfigConstants.LAYER_PRESENCE_QUERY) == null || map.get(DAConstants.AutotuneConfigConstants.LAYER_PRESENCE_KEY) == null)) {
				errorString.append(DAErrorConstants.AutotuneConfigErrors.LAYER_PRESENCE_MISSING);
			}
		}

		// Check if both layerPresenceQuery and layerPresenceLabel are set
		if (map.get(DAConstants.AutotuneConfigConstants.LAYER_PRESENCE_QUERY) != null && map.get(DAConstants.AutotuneConfigConstants.LAYER_PRESENCE_LABEL) != null) {
			errorString.append(DAErrorConstants.AutotuneConfigErrors.BOTH_LAYER_QUERY_AND_LABEL_SET);
		}

		// Check if level is valid
		if ((Integer)map.get(DAConstants.AutotuneConfigConstants.LAYER_LEVEL) < 0) {
			errorString.append(DAErrorConstants.AutotuneConfigErrors.LAYER_LEVEL_INVALID);
		}

		// Check if tunables is empty
		if ((map.get(DAConstants.AutotuneConfigConstants.TUNABLES)) == null) {
			errorString.append(DAErrorConstants.AutotuneConfigErrors.NO_TUNABLES);
		}
		return errorString;
	}

	/**
	 * Check if the AutotuneObject is valid
	 * @param map
	 * @return
	 */
	@Override
	public StringBuilder validateAutotuneObject(HashMap<String, Object> map) {
		StringBuilder errorString = new StringBuilder();

		// Check if name is valid
		if (map.get(DAConstants.AutotuneObjectConstants.NAME) == null || ((String)map.get(DAConstants.AutotuneObjectConstants.NAME)).isEmpty()) {
			errorString.append(DAErrorConstants.AutotuneObjectErrors.AUTOTUNE_OBJECT_NAME_MISSING);
		}

		// Check if mode is supported
		if (!AutotuneSupportedTypes.MODES_SUPPORTED.contains((String)map.get(DAConstants.AutotuneObjectConstants.MODE))) {
			errorString.append(DAErrorConstants.AutotuneObjectErrors.MODE_NOT_SUPPORTED);
		}

		// Check if matching label is set
		SelectorInfo selectorInfo = (SelectorInfo) map.get(DAConstants.AutotuneObjectConstants.SELECTOR);
		if (selectorInfo.getMatchLabel() == null || selectorInfo.getMatchLabel().isEmpty()) {
			errorString.append(DAErrorConstants.AutotuneObjectErrors.INVALID_MATCHLABEL);
		}

		if (selectorInfo.getMatchLabelValue() == null || selectorInfo.getMatchLabelValue().isEmpty()) {
			errorString.append(DAErrorConstants.AutotuneObjectErrors.INVALID_MATCHLABEL_VALUE);
		}

		// Check if sla_class is supported
		SlaInfo slaInfo = (SlaInfo) map.get(DAConstants.AutotuneObjectConstants.SLA);
		if (!AutotuneSupportedTypes.SLA_CLASSES_SUPPORTED.contains(slaInfo.getSlaClass())) {
			errorString.append(DAErrorConstants.AutotuneObjectErrors.SLA_CLASS_NOT_SUPPORTED);
		}

		// Check if direction is supported
		if (!AutotuneSupportedTypes.DIRECTIONS_SUPPORTED.contains(slaInfo.getDirection())) {
			errorString.append(DAErrorConstants.AutotuneObjectErrors.DIRECTION_NOT_SUPPORTED);
		}

		// Check if function_variables is empty
		if (slaInfo.getFunctionVariables().isEmpty()) {
			errorString.append(DAErrorConstants.AutotuneObjectErrors.FUNCTION_VARIABLES_EMPTY);
		}

		// Check if objective_function exists
		if (slaInfo.getObjectiveFunction() == null || slaInfo.getObjectiveFunction().isEmpty()) {
			errorString.append(DAErrorConstants.AutotuneObjectErrors.OBJECTIVE_FUNCTION_MISSING);
		}

		for (FunctionVariable functionVariable : slaInfo.getFunctionVariables()) {
			//Check if datasource is supported
			if (!AutotuneSupportedTypes.MONITORING_AGENTS_SUPPORTED.contains(functionVariable.getDatasource().toLowerCase())) {
				errorString.append("function_variable: ").append(functionVariable.getName()).append(" datatype not supported\n");
			}

			if (!AutotuneSupportedTypes.VALUE_TYPES_SUPPORTED.contains(functionVariable.getValueType().toLowerCase())) {
				errorString.append("function_variable: ").append(functionVariable.getName()).append(" value_type not supported\n");

			}
		}
		return errorString;
	}
}
