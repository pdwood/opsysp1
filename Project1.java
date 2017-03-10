/*
 * CSCI 4210 Operating Systems Project 1
 * 
 * Made by:
 * Kyle Fawcett fawcek
 * Peter Wood woodp
 * Gavin Petilli petilg
*/
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.CookiePolicy;
import java.util.Comparator;
import java.util.Formatter;
import java.text.NumberFormat;
import java.text.DecimalFormat;
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
	private static boolean debugging = false;
	
	private static int csCount = 0;
	private static int preemptCount = 0;
	private static int contextSwitchTime = 6;
	private static int currentTime;
	
	private static BufferedWriter fileOut;
	
	//Enum for CPU state
	private enum State{
		WAITING, EMPTYING, INITIALIZING;
	}
	private static State preemptState = State.WAITING;
	
	//Enum for the different algorithms
	private enum Algorithm{
		FCFS, SRT, RR;
		//toString returns the declared name of the constant by default
	}
	private static Algorithm currentAlg;
	
	public static void main(String[] args) {
		
		//Ensure files are correct before starting
		if (args.length == 0) {
			System.err.println("ERROR: No input file given.");
			return;
		}else if(args.length == 1){
			System.err.println("ERROR: No output file given.");
		}
		
		//Open the output file
		try {
			fileOut = new BufferedWriter(new FileWriter(new File(args[1])));
		} catch (IOException e) {
			System.err.println("ERROR: Cannot open output file for writing.");
		}
		
		//loop through each of the algorithms
		for (Algorithm alg: Algorithm.values()){
			currentAlg = alg;
			//initialize local variables
			finished = new LinkedList<Process>();
			
			//select the queue type based on the current algorithm
			switch(currentAlg){
			case FCFS:
				queue = new LinkedList<Process>();
				break;
			case SRT:
				queue = new PriorityQueue<Process>(new Comparator<Process>(){
					public int compare(Process a, Process b){
						if (a.getRemainingCPUTime() == b.getRemainingCPUTime())
							return a.getID().compareTo(b.getID());
						return a.getRemainingCPUTime() - b.getRemainingCPUTime();
					}
				});
				break;
			case RR:
				queue = new LinkedList<Process>();
				break;
			}
			
			io = new PriorityQueue<Process>(new Comparator<Process>(){
				public int compare(Process a, Process b){
					return a.getNextStateChange() - b.getNextStateChange();
				}
			});
			//Outside should be queue sorted by arrival time
			outside = new PriorityQueue<Process>(new Comparator<Process>(){
				public int compare(Process a, Process b){
					if (a.getArrivalTime() == b.getArrivalTime())
						return a.getID().compareTo(b.getID());
					return a.getArrivalTime() - b.getArrivalTime();
				}
			});
			
			//retrieve process information from file
			if (!parseFile(args[0])){
				System.err.println("ERROR: Could not read given input file.");
				return;
			}
			
			currentTime=0;
			
			int i=0;//used for debug
			
			//running loop for the program, loops until all processes have completed
			
			System.out.print("time "+currentTime+"ms: Simulator started for " + currentAlg);
			System.out.println(" ["+queueStatus()+"]");
			
			while(true){
				if (debugging) {
					System.out.println("\nWhile Loop Status: "+currentTime+"ms. (Step "+i+").");
					System.out.println("\tCooldown: "+cooldown);
					System.out.println("\tOutside Size: "+outside.size());
					System.out.println("\tQueue Size: "+queue.size());
					System.out.println("\tIO Size: "+io.size());
					if (currentProcess != null) System.out.println("\tCPU Status: "+currentProcess);
					else System.out.println("\tCPU Status: Empty");
					System.out.println("\tFinished Size: "+finished.size());
				}
				
				int timeDelta = queryNextEvent();
				if (debugging) System.out.println("\tNext event at time: "+currentTime+"+"+timeDelta);
				if(timeDelta == Integer.MAX_VALUE) break;
				updateTimestamps(timeDelta);
				
				/* Go through all the sectors and update/check their statuses. 
				 * Yes, order does matter, see Piazza "Several Questions". */
				updateCPU(timeDelta);
				updateIO();
				updateOutside();
				updateQueue(timeDelta);
				if (checkPreemption(currentAlg)){
					if (emptyCPU())
						preemptCount++;
				}
				
				i++;//used for debug
			}
			
			System.out.println("time "+currentTime+"ms: Simulator ended for " + currentAlg);
			if (currentAlg != Algorithm.RR){
				System.out.println();
			}
			
			try{
				writeStatistics(args[1]);
			}catch(IOException e){
				System.err.println("ERROR: Could not write to output file.");
			}
		}
	}
	
	/**
	 * Returns the timestamp of the next point the simulation should advance to
	 */
	private static int queryNextEvent(){
		//(May be difficult for SRT...)
		int timeDelta=Integer.MAX_VALUE;
		//String reason="none";
		
		//Possible next events: Outside arrival, process finishing CPU, process
		//finishing I/O, others? ... SRT preemption, but that is weird.
		if(outside.size() > 0) {
			timeDelta = outside.peek().getArrivalTime() - currentTime;
			//reason = "outside entering ("+outside.peek().getArrivalTime()+"-"+currentTime+"="+timeDelta+").";
		}
		if(cooldown > 0 && cooldown < timeDelta) {
			timeDelta = cooldown;
			//reason = "cooldown finished";
		}
		else if(currentProcess != null && currentProcess.getRemainingCPUTime() < timeDelta) {
			timeDelta = currentProcess.getRemainingCPUTime();
			//reason = "current process finished";
		}
		if(io.size() > 0 && io.peek().getNextStateChange()-currentTime < timeDelta) {
			timeDelta = io.peek().getNextStateChange()-currentTime;
			//reason = "process exiting I/O";
		}
		/* If the processor is empty and there are more processes and there is no cooldown,
		 * next event is now, it is putting process in currentProcess. */
		if(currentProcess == null && queue.size() > 0 && cooldown == 0){
			timeDelta = 0;
			//reason = "process entering";
		}
		
		//System.out.println("Next process is occuring because: "+reason);
		return timeDelta;
	}
	
	/**
	 * Updates the current time variable. This really shouldn't be a method of
	 * it's own. We will discuss this tonight. 
	 */
	private static void updateTimestamps(int timeDelta){
		currentTime += timeDelta;
	}
	
	/**
	 * Parse process data from a file, create new processes with the data.
	 * Push new processes to the Outside queue.
	 * Expected file format: <proc-id>|<initial-arrival-time>|<cpu-burst-time>|<num-bursts>|<io-time>
	 * @param filename the file to parse
	 */
	private static boolean parseFile(String filename){
		try{
			outside.clear();
			Scanner input = new Scanner(new File(filename));
			while(input.hasNextLine()){
				String line = input.nextLine();
				Process p;
				if(line.length() > 0 && line.charAt(0)!='#'){
					p = parse(line);
					outside.add(p);
				}
			}
			input.close();
			return true;
		}catch(IOException e){
			System.err.println("ERROR: Cannot read file "+filename+" ("+e.getMessage()+")");
			return false;			
		}		
	}
	/* Helper function to parseFile(string).
	 * This will parse the particular line.
	 */
	private static Process parse(String in){
		String[] tokens = in.split("\\|");
		
		if(tokens.length != 5) 
			throw new IllegalArgumentException("Invalid process description ("+tokens.length+"): "+in);
		return new Process(tokens[0], Integer.parseInt(tokens[1]), Integer.parseInt(tokens[2]), Integer.parseInt(tokens[3]), Integer.parseInt(tokens[4]));
	}
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
		// If the processor is warming up/cooling down from a context switch.
		if (cooldown > 0) {
			cooldown = cooldown - elapsedTime;
		}
		if (cooldown < 0){
			throw new RuntimeException("ERROR:The cooldown is negative, this should not happen.");
		}
		//If there is something in the processor and the processor is not on cooldown
		if (currentProcess != null && cooldown == 0){
			
			// If the processor isn't working on a CS.
			if (preemptState == State.WAITING) {
				//decrement the remaining process time by the elapsed time
				currentProcess.decrementTime(elapsedTime);
				currentProcess.addToTurnTime(elapsedTime);
				
				// If the current process is done with its current CPU Burst...
				if (currentProcess.getRemainingCPUTime() <= 0) {
					//decrement the number of remaining bursts
					currentProcess.decrementBursts();
					//Print remaining bursts to output if any left
					if (currentProcess.getRemainingCPUBursts() > 0){
						System.out.print("time "+currentTime+"ms: Process "+currentProcess.getID()
							+" completed a CPU burst; ");
						if (currentProcess.getRemainingCPUBursts() == 1)
							System.out.println(currentProcess.getRemainingCPUBursts()
									+" burst to go ["+queueStatus()+"]");
						else
							System.out.println(currentProcess.getRemainingCPUBursts()
									+" bursts to go ["+queueStatus()+"]");
					}
					
					//Start a context switch to empty the CPU
					emptyCPU();
				}
			}
			//If the CPU is context switching the current process out (and the cooldown is 0)
			else if (preemptState == State.EMPTYING) {
				currentProcess.iterateWaitCounter();
				
				/* If the process is not finished with the current burst, return it to the queue.
				 * This should only be caused by a preemption. */
				if (currentProcess.getRemainingCPUTime() > 0) {
					queue.add(currentProcess);
				}
				/* Else, If the process has bursts remaining, move it to the I/O and reset the Burst time. */
				else if (currentProcess.getRemainingCPUBursts() > 0) {
					currentProcess.setStateChangeTime(currentTime+currentProcess.getIOTime());
					currentProcess.resetBurstTime();
					io.add(currentProcess);
				}
				/* Else, If it has finished all bursts, set nextStateChange variable
				 * appropriately, and move it to the finished queue. */
				else if (currentProcess.getRemainingCPUBursts() == 0) {
					currentProcess.setStateChangeTime(currentTime);
					finished.add(currentProcess);
				}
				//Error checking...
				else if (currentProcess.getRemainingCPUBursts() < 0){
					throw new RuntimeException("ERROR:The process has less than zero bursts remaining,"
							+ " this should not happen.");
				}
				//clear the CPU pointer and set the CPU to a waiting state
				preemptState = State.WAITING;
				currentProcess = null;
			}
			//If the CPU is currently context switching a new process in (and the cooldown is 0).
			else if (preemptState == State.INITIALIZING) {
				//Set the CPU to a WAITING state
				preemptState = State.WAITING;
				System.out.print("time "+ currentTime +"ms: Process "
					+currentProcess.getID()+" started using the CPU");
				if (currentProcess.getRemainingCPUTime() < currentProcess.getCPUBurstTime())
					System.out.print(" with "+ currentProcess.getRemainingCPUTime() 
						+"ms remaining");
				System.out.println(" ["+queueStatus()+"]");
			}
		}	
	}
	
	/**
	 * If there is something in the CPU, and the CPU is not busy, 
	 * 	initiate a context switch to empty the CPU.
	 */
	private static boolean emptyCPU() {
		if (currentProcess != null && preemptState == State.WAITING){
			//Start a context switch (first half):
			if (checkPreemption(currentAlg))
				/*do nothing*/ ;  //if the process has been preempted, don't print
			else if (currentProcess.getRemainingCPUBursts() > 0){
				System.out.println("time "+currentTime+"ms: Process " + currentProcess.getID()
					+ " switching out of CPU; will block on I/O until time "
					+ (currentTime + (contextSwitchTime/2) + currentProcess.getIOTime())
					+ "ms ["+queueStatus()+"]");
			}else{
				System.out.println("time "+currentTime+"ms: Process " + currentProcess.getID()
					+ " terminated ["+queueStatus()+"]");
			}
			//set the cooldown
			cooldown = (contextSwitchTime/2);
			//change the preemptionState to EMPTYING
			preemptState = State.EMPTYING;
			return true;
		}
		return false;
	}
	
	/**
	 * If the cpu is empty, move the current process to the cpu and
	 * 	initiate a context switch.
	 */
	private static boolean fillCPU(Process p) { 
		if (currentProcess == null && preemptState == State.WAITING){
			//Start a context switch (second half):
			//put the passed process in the CPU
			currentProcess = p;
			//set the cooldown
			cooldown = (contextSwitchTime/2);
			//change the preemptionState to INITIALIZING
			preemptState = State.INITIALIZING;
			return true;
		}
		return false;
	} 
	
	/**
	 * Move all processes that have completed I/O to the queue 
	 */
	private static void updateIO(){
		Iterator<Process> iter;
		
		if (debugging) {
			System.out.print(currentTime+": I/O List before update: ");
			iter = io.iterator();
			while (iter.hasNext()) {
				Process p = iter.next();
				System.out.print(p.getID()+":"+p.getNextStateChange()+", ");
			}
			System.out.println("");
		}
		
		while (io.size() > 0 && io.peek().getNextStateChange() <= currentTime) {
			Process p = io.poll();
			p.iterateWaitCounter();
			p.iterateTurnCounter();
			queue.add(p);
			//TODO change this message if p would preempt
			System.out.print("time "+currentTime+"ms: Process "+p.getID()+" completed I/O");
			if (checkPreemption(currentAlg)){
				System.out.print(" and will preempt " + currentProcess.getID());
				Process temp = queue.poll();
				System.out.println(" ["+queueStatus()+"]");
				queue.add(temp);
			}
				
			else
				System.out.println("; added to ready queue ["+queueStatus()+"]");
			
			
		}
		
		if (debugging) {
			System.out.print(currentTime+": I/O List after update: ");
			iter = io.iterator();
			while (iter.hasNext()) {
				Process p = iter.next();
				System.out.print(p.getID()+":"+p.getNextStateChange()+", ");
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
		
		if (debugging) {
			System.out.print(currentTime+": Outside List before update: ");
			iter = outside.iterator();
			while (iter.hasNext()) {
				Process p = iter.next();
				System.out.print(p.getArrivalTime()+", ");
			}
			System.out.println("");
		}
		
		iter = outside.iterator();
		while (iter.hasNext()) {
			Process p = iter.next();
			if (p.getArrivalTime() <= currentTime) {
				p.iterateWaitCounter();
				p.iterateTurnCounter();
				queue.add(p);
				System.out.print("time "+currentTime+"ms: Process "+p.getID()+" arrived");
				if (checkPreemption(currentAlg)){
					System.out.print(" and will preempt " + currentProcess.getID());
					Process temp = queue.poll();
					System.out.println(" ["+queueStatus()+"]");
					queue.add(temp);
				}	
				else
					System.out.println(" and added to ready queue ["+queueStatus()+"]");
				iter.remove();
			}
		}
		
		if (debugging) {
			System.out.print(currentTime+": Outside List after update: ");
			iter = outside.iterator();
			while (iter.hasNext()) {
				Process p = iter.next();
				System.out.print(p.getArrivalTime()+", ");
			}
			System.out.println("");
		}
		
	}
	
	/**
	 * If the CPU is empty push the next queued object into the CPU
	 */
	private static void updateQueue(int elapsedTime){
		Iterator<Process> iter = queue.iterator();
		while (iter.hasNext()) {
			Process p = iter.next();
			p.addToWaitTime(elapsedTime);
			p.addToTurnTime(elapsedTime);
		}
		
		//if the queue is empty, return
		if (queue.isEmpty())
			return;
		//If the CPU is empty, push the first process in the queue into the CPU
		if (currentProcess == null && preemptState == State.WAITING)
			fillCPU(queue.poll());
	}
	
	/**
	 * Checks for a preemption using the current algorithm
	 * @param alg the algorithm to check with
	 * @return true if there was a preemption, false otherwise
	 */
	private static boolean checkPreemption(Algorithm alg){
		//Can't preempt an empty CPU or an empty queue
		if (currentProcess == null || queue.isEmpty())
			return false;
		switch(alg){
		case FCFS:
			return false; //FCFS dosen't preempt
		case SRT:
			if (queue.peek().getRemainingCPUTime() < currentProcess.getRemainingCPUTime())
				return true;
		case RR: //TODO set up preemption for RR
			break; 
		}
		return false;
	}
	
	private static void writeStatistics(String filename) throws IOException{
		NumberFormat formatter = new DecimalFormat("#0.00");
		
		fileOut.write("Algorithm "+currentAlg+"\n");
		double avgBurst=0, avgWait=0, avgTurn=0;
		int totalBursts=0, totalWaits=0, totalTurns=0;
		Iterator<Process> iter = finished.iterator();
		while (iter.hasNext()) {
			Process p = iter.next();
			totalBursts += p.getNumberOfBursts();
			totalWaits += p.getWaitCount();
			totalTurns += p.getTurnCount();
			
			
			avgBurst += p.getCPUBurstTime()*p.getNumberOfBursts();
			avgTurn += p.getTurnTimer();
			avgWait += p.getWaitTimer();
		}
		
		avgBurst = avgBurst/totalBursts;
		avgTurn = avgTurn/totalTurns;
		avgWait = avgWait/totalWaits;
		
		fileOut.write("-- average CPU burst time: "+formatter.format(avgBurst)+"\n");
		fileOut.write("-- average wait time: "+formatter.format(avgWait)+"\n");
		fileOut.write("-- average turnaround time: "+formatter.format(avgTurn)+"\n");
		fileOut.write("-- total number of context switches: "+csCount+"\n");
		fileOut.write("-- total number of preemptions: "+preemptCount+"\n");
		fileOut.flush();
	}
	
	private static String queueStatus() {
		if (queue.size() == 0)
			return "Q <empty>";
		
		String retVal = "Q";
		Queue<Process> copyQueue;
		if (currentAlg == Algorithm.SRT)
			copyQueue = new PriorityQueue<Process>(queue);
		else
			copyQueue = new LinkedList<Process>(queue);
		while (!copyQueue.isEmpty()){
			Process p = copyQueue.poll();
			retVal += " "+p.getID();
		}
		return retVal;
	}
	
	
};
