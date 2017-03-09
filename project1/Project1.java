package project1;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Scanner;

public class Project1 {
	
	private static Process currentProcess;
	private static int cooldown; //time until the cpu can be used (because of context switching)
	private static Queue<Process> queue;
	private static PriorityQueue<Process> io;
	private static PriorityQueue<Process> outside;
	private static Queue<Process> finished;
	
	private static int csCount = 0;
	private static int preemptCount = 0;
	private static int contextSwitchTime = 6;
	private static int timeToNextEvent;
	private static int currentTime;

	private static boolean shouldPreempt(){ return false; } //TODO
	private static void contextSwitch() {} //TODO

	public static void main(String[] args) {

		//initialize local variables
		queue = new LinkedList<Process>(); //TODO this type should be dynamically decided
		io = new PriorityQueue<Process>(new Comparator<Process>(){
			public int compare(Process a, Process b){
				return a.getRemainingCPUTime() - b.getRemainingCPUTime();
			}
		});
		//Outside should be queue sorted by arrival time
		outside = new PriorityQueue<Process>(new Comparator<Process>(){
			public int compare(Process a, Process b){
				return a.getArrivalTime() - b.getArrivalTime();
			}
		});
		
		//retrieve process information from file
		parseFile(args[0]);

		currentTime=0;

		//running loop for the program, loops until all processes have completed
		while(true){
			//TODO Account for context switch!

			if(outside.peek()!=null && outside.peek().getArrivalTime() == currentTime)
				queue.add(outside.poll());		

			if(currentProcess == null || currentProcess.getRemainingCPUTime() == 0 || shouldPreempt())
				contextSwitch();

			int timeDelta = queryNextEvent();
			if(timeDelta == Integer.MAX_VALUE) break;
			updateTimestamps(timeDelta);
		}
	}
	
	/**
	 * Returns the timestamp of the next point the simulation should advance to
	 */
	private static int queryNextEvent(){
		//(May be difficult for SRT...)

		//Possible next events: Outside arrival, process finishing CPU, process finishing IO, others? ... SRT preemption, but that is weird.
		int timeDelta=Integer.MAX_VALUE;
		if(outside.size() > 0) timeDelta = outside.peek().getArrivalTime() - currentTime;
		if(currentProcess != null && currentProcess.getRemainingCPUTime() < timeDelta) timeDelta = currentProcess.getRemainingCPUTime();
		if(io.size() > 0 && io.peek().getRemainingCPUTime() < timeDelta) timeDelta = io.peek().getRemainingCPUTime();
		if(cooldown > 0 && cooldown < timeDelta) timeDelta = cooldown;
		
		return timeDelta;
	}

	/**
	 * Updates the remaining times of process in CPU
	 */
	private static void updateTimestamps(int timeDelta){
		if(currentProcess != null) currentProcess.decrementTime(timeDelta);
		currentTime += timeDelta;
		if(cooldown >= timeDelta) cooldown -= timeDelta;
	}

	/**
	 * Parse process data from a file, create new processes with the data.
	 * Push new processes to the Outside queue.
	 * Expected file format: <proc-id>|<initial-arrival-time>|<cpu-burst-time>|<num-bursts>|<io-time>
	 * @param filename the file to parse
	 */
	private static void parseFile(String filename){
		try{
			Scanner input = new Scanner(new File(filename));
			while(input.hasNextLine()){
				String line = input.nextLine();
				Process p;
				if(line.charAt(0)!='#'){
					p = parse(line);
					outside.add(p);
				}
			}
			input.close();
		}catch(IOException e){
			System.err.println("ERROR: Cannot read file "+filename+" ("+e.getMessage()+")");
		}
	}
	/* Helper function to parseFile(string).
	 * This will parse the particular line.
	 */
	private static Process parse(String in){
		String[] tokens = in.split("\\|");
		
		if(tokens.length != 5) throw new IllegalArgumentException("Invalid process description ("+tokens.length+"): "+in);
		return new Process(tokens[0], Integer.parseInt(tokens[1]), Integer.parseInt(tokens[2]), Integer.parseInt(tokens[3]), Integer.parseInt(tokens[4]));
	}
	
	//////////////////////Previously commented///////////////////////////////
	
	
	/**
	 * If the cpu is empty, do nothing.
	 * If the cpu is on cooldown, reduce cooldown.
	 * --If the cooldown is complete, clear the cooldown and move process or start the burst
	 * Else, reduce the current process's remaining burst time.
	 * --If the burst is finished, initiate a context switch.
	 * --If a process is leaving the CPU, add it to the finished queue, 
	 *    Set it's nextStateChange variable to be the time it left, +0.5 of a
	 *    context switch time.
	 * 
	 * Update timeToNextEvent if necessary.
	 * @param elapsedTime the amount of time since the last cpu update
	 */
	private static void updateCPU(int elapsedTime){
		
		
	}
	
	/**
	 * Reduce remaining IO time for all processes in IO.
	 * --If any processes are finished with IO, move them to the queue
	 * @param elapsedTime the amount of time since the last cpu update
	 */
	private static void updateIO(){
		Iterator<Process> iter;
		boolean debugging = false;
		
		if (debugging) {
			System.out.print(currentTime+": IO List before update: ");
			iter = io.iterator();
			while (iter.hasNext()) {
				Process p = iter.next();
				System.out.print(p.getNextStateChange()+", ");
			}
			System.out.println("");
		}
		
		iter = io.iterator();
		while (iter.hasNext()) {
			Process p = iter.next();
			if (p.getNextStateChange() <= currentTime) {
				queue.add(p);
				iter.remove();
			}
		}
		
		if (debugging) {
			System.out.print(currentTime+": IO List after update: ");
			iter = io.iterator();
			while (iter.hasNext()) {
				Process p = iter.next();
				System.out.print(p.getNextStateChange()+", ");
			}
			System.out.println("");
		}
		
	}
	
	/**
	 * Move processes to the queue if it is time for the process to enter.
	 * @param elapsedTime the amount of time since the last cpu update
	 */
	private static void updateOutside(){
		Iterator<Process> iter;
		boolean debugging = false;
		
		if (debugging) {
			System.out.print(currentTime+": Outside List before update: ");
			iter = outside.iterator();
			while (iter.hasNext()) {
				Process p = iter.next();
				System.out.print(p.getArrivalTime()+", ");
			}
			System.out.println("");
		}
		
		iter = io.iterator();
		while (iter.hasNext()) {
			Process p = iter.next();
			if (p.getArrivalTime() <= currentTime) {
				queue.add(p);
				iter.remove();
			}
		}
		
		if (debugging) {
			System.out.print(currentTime+": outside List after update: ");
			iter = io.iterator();
			while (iter.hasNext()) {
				Process p = iter.next();
				System.out.print(p.getArrivalTime()+", ");
			}
			System.out.println("");
		}
		
	}
	
	/**
	 * Check if cpu is empty
	 * Check for preemption
	 * -- If there was a preemption, increment the preemption counter.
	 * Initiate context switch if necessary
	 * -- If there was a context switch, increment the context switch counter.
	 * @param elapsedTime the amount of time since the last cpu update
	 */
	private static void updateQueue(){
		
	}
	
	private static String generateStatistics(String algo) {
		String retVal = "Algorithm "+algo+"\n";
		double avgBurst=0, avgWait=0, avgTurn=0;
		Iterator<Process> iter = finished.iterator();
		while (iter.hasNext()) {
			Process p = iter.next();
			
			avgBurst += p.getCPUBurstTime();
			avgWait += p.getNextStateChange()-p.getArrivalTime();
		}
		
		avgBurst = avgBurst/finished.size();
		
		retVal += "-- average CPU burst time: "+avgBurst+"\n";
		retVal += "-- average wait time: "+avgWait+"\n";
		retVal += "-- average turnaround time: "+avgTurn+"\n";
		retVal += "-- total number of context switches: "+csCount+"\n";
		retVal += "-- total number of preemptions: "+preemptCount+"\n";
		
		return retVal;
	}
	
	
	
	
};
