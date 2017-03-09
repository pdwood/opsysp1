/* CSCI 4210 OpSys Project 1
 * 
 * Made By: 
 * Kyle Fawcett (fawcek)
 * Peter Wood (woodp)
 * Gavin Petilli (petilg)
 * 
 */

public class Process{
	//<proc-id>|<initial-arrival-time>|<cpu-burst-time>|<num-bursts>|<io-time>
	private final String ID;
	private final int arrival_time;
	private final int cpuBurstTime;
	private final int numBursts;
	private final int ioTime;
	private int remainingCPUBursts;
	private int timeRemaining;
	private int stateChangeTime;
	
	public Process(String id, int arrival, int burstTime, int bursts, int io) {
		ID = id;
		arrival_time = arrival;
		cpuBurstTime = burstTime;
		numBursts = bursts;
		ioTime = io;
		timeRemaining = burstTime;
		remainingCPUBursts= numBursts;
		stateChangeTime = arrival_time;
	}
	
	public String getID() {return ID;}
	public int getArrivalTime() {return arrival_time;}
	public int getCPUBurstTime() {return cpuBurstTime;}
	public int getNumberOfBursts() {return numBursts;}
	
	public void decrementTime(int time){ timeRemaining-=time; }
	public void decrementBursts() {remainingCPUBursts--;}
	public void setStateChangeTime(int t) {stateChangeTime = t;}
	
	public int getNextStateChange() {return stateChangeTime;}
	public int getRemainingCPUTime() {return timeRemaining;}
	public int getRemainingCPUBursts() {return remainingCPUBursts;}
	public String toString() {
		return ("Process "+ID+", "+remainingCPUBursts+" bursts remain. ");
	}
	
	
}
