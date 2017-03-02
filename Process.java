/* CSCI 4210 OpSys Project 1
 * 
 * Made By: 
 * Kyle Fawcett (fawcek)
 * Peter Wood (woodp)
 * Gavin Petilli (petilg)
 * 
 */

public class Process {
	public Process(String id, int arrival, int burstTime, int bursts, int io) {
		ID = id;
		arrival_time = arrival;
		cpuBurstTime = burstTime;
		numBursts = bursts;
		ioTime = io;
		
		remainingCPUBursts= numBursts;
	}
	
	public String getID() {return ID;}
	public int getArrivalTime() {return arrival_time;}
	public int getCPUBurstTime() {return cpuBurstTime;}
	public int getNumberOfBursts() {return numBursts;}
	public int getIOTime() {return ioTime;}
	
	public int getRemainingCPUTime() {return remainingCPUBursts*cpuBurstTime;}
	public int getRemainingCPUBursts() {return remainingCPUBursts;}
	public String toString() {
		String ret = "Process "+ID+", "+remainingCPUBursts+" bursts remain. ";
		return ret;
	}
	
	public void executeCPUBurst() {remainingCPUBursts--;}
	
	//<proc-id>|<initial-arrival-time>|<cpu-burst-time>|<num-bursts>|<io-time>
	private final String ID;
	private final int arrival_time;
	private final int cpuBurstTime;
	private final int numBursts;
	private final int ioTime;
	
	private int remainingCPUBursts;
	
};
