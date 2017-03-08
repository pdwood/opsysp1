package project1;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;


public class SimMain {
	
	private static Process CPU;
	private static int cooldown; //time until the cpu can be used (because of context switching)
	private static LinkedList<Process> queue;
	private static LinkedList<Process> IO;
	private static LinkedList<Process> outside;
	private static int contextSwitchTime = 8;
	private static int timeToNextEvent;
	private static int currentTime;

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		queue = new LinkedList<Process>();
		IO = new LinkedList<Process>();
		outside = new LinkedList<Process>();
		
		
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
			iter = IO.iterator();
			while (iter.hasNext()) {
				Process p = iter.next();
				System.out.print(p.getIOExitTime()+", ");
			}
			System.out.println("");
		}
		
		iter = IO.iterator();
		while (iter.hasNext()) {
			Process p = iter.next();
			if (p.getIOExitTime() <= currentTime) {
				queue.add(p);
				iter.remove();
			}
		}
		
		if (debugging) {
			System.out.print(currentTime+": IO List after update: ");
			iter = IO.iterator();
			while (iter.hasNext()) {
				Process p = iter.next();
				System.out.print(p.getIOExitTime()+", ");
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
		
		iter = IO.iterator();
		while (iter.hasNext()) {
			Process p = iter.next();
			if (p.getArrivalTime() <= currentTime) {
				queue.add(p);
				iter.remove();
			}
		}
		
		if (debugging) {
			System.out.print(currentTime+": outside List after update: ");
			iter = IO.iterator();
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
	 * --Initiate context switch if necessary
	 * @param elapsedTime the amount of time since the last cpu update
	 */
	private static void updateQueue(){
		
	}

};
