/*
 * CSCI 4210 Operating Systems Project 1
 * 
 * Made by:
 * Kyle Fawcett fawcek
 * Peter Wood woodp
 * Gavin Petilli petilg
*/
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
	private static boolean debugging = false;
	
	private static int csCount = 0;
	private static int preemptCount = 0;
	private static int contextSwitchTime = 6;
	private static int timeToNextEvent;
	private static int currentTime;
	
	private static boolean shouldPreempt(){ return false; } //TODO
	private static void contextSwitch() {} //TODO
	
	public static void main(String[] args) {
		
		//initialize local variables
		finished = new LinkedList<Process>();
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
		if (args.length == 0) {
			System.out.println("ERROR: No input file.");
			return;
		}
		parseFile(args[0]);
		
		currentTime=0;
		
		//running loop for the program, loops until all processes have completed
		int i=0;
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
			//TODO Account for context switch!
			
			/* Removed the condition "currentProcess == null", because
			 * sometimes the processor will be empty for a while, while 
			 * everything is off doing IO, and it shouldn't switch. */
			if(currentProcess != null && currentProcess.getRemainingCPUTime() == 0 || shouldPreempt()){
				contextSwitch();
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
			i++;
		}
	}
	
	/**
	 * Returns the timestamp of the next point the simulation should advance to
	 */
	private static int queryNextEvent(){
		//(May be difficult for SRT...)
		int timeDelta=Integer.MAX_VALUE;
		String reason="none";
		
		//Possible next events: Outside arrival, process finishing CPU, process
		//finishing IO, others? ... SRT preemption, but that is weird.
		if(outside.size() > 0) {
			timeDelta = outside.peek().getArrivalTime() - currentTime;
			reason = "outside entering ("+outside.peek().getArrivalTime()+"-"+currentTime+"="+timeDelta+").";
		}
		if(cooldown > 0 && cooldown < timeDelta) {
			timeDelta = cooldown;
			reason = "cooldown finished";
		}
		else if(currentProcess != null && currentProcess.getRemainingCPUTime() < timeDelta) {
			timeDelta = currentProcess.getRemainingCPUTime();
			reason = "current process finished";
		}
		if(io.size() > 0 && io.peek().getNextStateChange()-currentTime < timeDelta) {
			timeDelta = io.peek().getNextStateChange()-currentTime;
			reason = "process exiting IO";
		}
		/* If the processor is empty and there are more processes and there is no cooldown,
		 * next event is now, it is putting process in currentProcess. */
		if(currentProcess == null && queue.size() > 0 && cooldown == 0){
			timeDelta = 0;
			reason = "process entering";
		}
		
		//System.out.println("Next process is occuring because: "+reason);
		return timeDelta;
	}
	
	/**
	 * Updates the current time variable. This really shouldn't be a method of
	 * it's own. We will discuss this tonight. 
	 */
	private static void updateTimestamps(int timeDelta){
		/* These line is getting removed, as it belongs to the CPU 
		 * specifically, not the timestamps in general. 
		 * if(currentProcess != null) currentProcess.decrementTime(timeDelta);
		 * if(cooldown >= timeDelta) cooldown -= timeDelta; */
		currentTime += timeDelta;
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
		
		
		// If there is something in the processor and it isn't working on a CS.
		if (currentProcess != null && cooldown == 0) {
			currentProcess.decrementTime(elapsedTime);
			
			// If the current process is done with it's current CPU Burst...
			if (currentProcess.getRemainingCPUTime() <= 0) {
				currentProcess.decrementBursts();
				
				/* If it has bursts remaining, move it to the IO. */
				if (currentProcess.getRemainingCPUBursts() != 0) {
					currentProcess.setStateChangeTime(currentTime+currentProcess.getIOTime());
					io.add(currentProcess);
					System.out.print("time "+currentTime+"ms: Process "+currentProcess.getID()+" completed a CPU burst; ");
					System.out.println(currentProcess.getRemainingCPUBursts()+" bursts to go ["+queueStatus()+"]");
				}
				
				/* If it has finished all bursts, set nextStateChange variable
				 * appropriately, and move it to the finished queue. */
				if (currentProcess.getRemainingCPUBursts() == 0) {
					System.out.println("time "+currentTime+"ms: Process "+currentProcess.getID()+" terminated ["+queueStatus()+"]");
					currentProcess.setStateChangeTime(currentTime+contextSwitchTime/2);
					finished.add(currentProcess);
				}
				
				/* Set the processor to null, and the cooldown to cs/2 + elapsedTime 
				 * The +elapsedTime is sort of a hack around the fact that 
				 * the next if statement decrements it, but it has to be in 
				 * this order. */
				currentProcess = null;
				cooldown = (contextSwitchTime/2)+elapsedTime;
			}
		}
		
		// If the processor is warming up/cooling down from a context switch.
		if (cooldown > 0) {
			cooldown = cooldown - elapsedTime;
		}
		
		/* If there is nothing in the processor, and the processor is done
		 * cooling down, and there is something waiting in the CPU. */
		if (currentProcess == null && cooldown == 0 && queue.size()!=0) {
			// If there is something in the queue, pop it in the processor, and
			// have half a cs switch to get it into the processor.
			cooldown = contextSwitchTime/2;
			currentProcess = queue.poll();
			currentProcess.resetBurstTime();
			
			System.out.println("time "+(currentTime+contextSwitchTime/2)+"ms: Process "+currentProcess.getID()+" started using the CPU ["+queueStatus()+"]");
		}
		
		
	}
	
	/**
	 * Check all processes to see if they are done doing IO. 
	 * --If any processes are finished with IO, move them to the queue
	 */
	private static void updateIO(){
		Iterator<Process> iter;
		
		if (debugging) {
			System.out.print(currentTime+": IO List before update: ");
			iter = io.iterator();
			while (iter.hasNext()) {
				Process p = iter.next();
				System.out.print(p.getNextStateChange()+", ");
			}
			System.out.println("");
		}
		
		while (io.size() > 0 && io.peek().getNextStateChange() <= currentTime) {
			Process p = io.poll();
			queue.add(p);
			System.out.println("time "+currentTime+"ms: Process "+p.getID()+" completed I/O; added to ready queue ["+queueStatus()+"]");
			
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
				queue.add(p);
				System.out.println("time "+currentTime+"ms: Process "+p.getID()+" arrived and added to ready queue ["+queueStatus()+"]");
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
	 * Check if cpu is empty
	 * Check for preemption
	 * -- If there was a preemption, increment the preemption counter.
	 * Initiate context switch if necessary
	 * -- If there was a context switch, increment the context switch counter.
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
	
	private static String queueStatus() {
		if (queue.size() == 0)
			return "Q <empty>";
		
		String retVal = "Q";
		Iterator<Process> iter = queue.iterator();
		while (iter.hasNext()) {
			Process p = iter.next();
			retVal += " "+p.getID();
		}
		return retVal;
	}
	
	
};
