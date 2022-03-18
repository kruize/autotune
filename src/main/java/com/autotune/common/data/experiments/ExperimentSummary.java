package com.autotune.common.data.experiments;

/**
 * This object should have the current summary of all the trials
 * of a given experiment
 */
public class ExperimentSummary {
	private final int totalTrials;
	private int currentTrial;
	private int trialsCompleted;
	private int trialsOngoing;
	private int trialsPassed;
	private int trialsFailed;
	private int bestTrial;

	public ExperimentSummary(int totalTrials,
							 int currentTrial,
							 int trialsCompleted,
							 int trialsOngoing,
							 int trialsPassed,
							 int trialsFailed,
							 int bestTrial) {
		this.totalTrials = totalTrials;
		this.currentTrial = currentTrial;
		this.trialsCompleted = trialsCompleted;
		this.trialsOngoing = trialsOngoing;
		this.trialsPassed = trialsPassed;
		this.trialsFailed = trialsFailed;
		this.bestTrial = bestTrial;
	}

	public int getTotalTrials() { return totalTrials; }

	public void setCurrentTrial(int currentTrial) {	this.currentTrial = currentTrial; }

	public int getCurrentTrial() { return currentTrial; }

	public int getTrialsCompleted() { return trialsCompleted; }

	public void setTrialsCompleted(int trialsCompleted) { this.trialsCompleted = trialsCompleted; }

	public int getTrialsOngoing() {	return trialsOngoing; }

	public void setTrialsOngoing(int trialsOngoing) { this.trialsOngoing = trialsOngoing; }

	public int getTrialsPassed() { return trialsPassed; }

	public void setTrialsPassed(int trialsPassed) { this.trialsPassed = trialsPassed; }

	public int getTrialsFailed() { return trialsFailed; }

	public void setTrialsFailed(int trialsFailed) { this.trialsFailed = trialsFailed; }

	public int getBestTrial() { return bestTrial; }

	public void setBestTrial(int bestTrial) { this.bestTrial = bestTrial; }
}
