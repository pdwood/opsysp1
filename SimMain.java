package project1;

import java.util.Queue;

public class SimMain {
	
	private static Process CPU; 
	private static int cooldown; //time until the cpu can be used (because of context switching)
	private static Queue queue;
	private static Queue IO;
	private static Queue outside;
	private static int contextSwitchTime;
	private static int timeToNextEvent;

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
	
	/**
	 * Parse process data from a file, create new processes with the data.
	 * Push new processes to the Outside queue.
	 * Expected file format: <proc-id>|<initial-arrival-time>|<cpu-burst-time>|<num-bursts>|<io-time>
	 * @param filename the file to parse
	 */
	private static void parseFile(String filename){
		
	}
	
	/**
	 * If the cpu is empty, do nothing.
	 * If the cpu is on cooldown, reduce cooldown.
	 * --If the cooldown is complete, clear the cooldown and move process or start the burst
	 * Else, reduce the current process's remaining burst time.
	 * --If the burst is finished, initiate a context switch.
	 * 
	 * Update timeToNextEvent if necessary.
	 * @param elapsedTime the amount of time since the last cpu update
	 */
	private static void updateCPU(int elapsedTime){
		

	}
	
	/**
	 * Reduce remaining IO time for all processes in IO.
	 * --If any processes are finished with IO, move them to the Queue
	 * @param elapsedTime the amount of time since the last cpu update
	 */
	private static void updateIO(int elapsedTime){
		

	}
	
	/**
	 * Move processes to the queue if it is time for the process to enter.
	 * @param elapsedTime the amount of time since the last cpu update
	 */
	private static void updateOutside(int elapsedTime){
		

	}
	
	/**
	 * Check if cpu is empty
	 * Check for preemption
	 * --Initiate context switch if necessary
	 * @param elapsedTime the amount of time since the last cpu update
	 */
	private static void updateQueue(int elapsedTime){
		

	}

}
