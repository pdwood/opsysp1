/* CSCI 4210 OpSys Project 1
 * 
 * Made By: 
 * Kyle Fawcett (fawcek)
 * Peter Wood (woodp)
 * Gavin Petilli (petilg)
 * 
 */

public class Process {
	public
	Process(char id, int arrival, int burstTime, int bursts, int io) {
		ID = id;
		arrival_time = arrival;
		cpuBurstTime = burstTime;
		numBursts = bursts;
		ioTime = io;
		
		remainingCPUBursts= numBursts;
		cpuEntryTime = arrival;
	}
	
	char getID() {return ID;}
	int getArrivalTime() {return arrival_time;}
	int getCPUBurstTime() {return cpuBurstTime;}
	int getNumberOfBursts() {return numBursts;}
	int getIOTime() {return ioTime;}
	
	int getRemainingCPUTime() {return remainingCPUBursts*cpuBurstTime;}
	int getRemainingCPUBursts() {return remainingCPUBursts;}
	
	void executeCPUBurst() {remainingCPUBursts--;}
	int getIOEntryTime() {return ioEntryTime;}
	void setIOEntryTime(int s) {ioEntryTime = s;}
	int getCPUEntryTime() {return cpuEntryTime;}
	void setCPUEntryTime(int s) {cpuEntryTime = s;}
	
	
	
	//<proc-id>|<initial-arrival-time>|<cpu-burst-time>|<num-bursts>|<io-time>
	private final char ID;
	private final int arrival_time;
	private final int cpuBurstTime;
	private final int numBursts;
	private final int ioTime;
	
	private int remainingCPUBursts;
	private int ioEntryTime, cpuEntryTime;
	
	

	
};
