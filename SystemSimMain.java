import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.File;
import java.util.Scanner;
import java.util.Queue;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.PriorityQueue;

/* CSCI 4210 OpSys Project 1
 * 
 * Made By: 
 * Kyle Fawcett (fawcek)
 * Peter Wood (woodp)
 * Gavin Petilli (petilg)
 * 
 */


public class SystemSimMain{
	static boolean cpuInUse;
	static int cs_t=8;
	static int t_slice=84;
	static int t_milli=0;
	static Queue<Process> cpuQueue;
	static Queue<Event> eventQueue;
	static AlgorithmType algo;

	public static void parseFile(String filename){
		try{
			Scanner input = new Scanner(new File(filename));
			while(input.hasNextLine()){
				String line = input.nextLine();
				Process p;
				if(line.charAt(0)!='#'){
					p = parse(line);
					
					Event e = new Event(EventType.ARRIVE, p.getArrivalTime(), p);
				}
				
			}
		}catch(IOException e){
			System.err.println("ERROR: Cannot read file "+filename+" ("+e.getMessage()+")");
		}
	}


	public static Process parse(String in){
		String[] tokens = in.split("|");
		
		if(tokens.length != 5) throw new IllegalArgumentException("Invalid process description ("+tokens.length+"): "+in);
		return new Process(tokens[0], Integer.parseInt(tokens[1]), Integer.parseInt(tokens[2]), Integer.parseInt(tokens[3]), Integer.parseInt(tokens[4]));
	}
	
	static int readFile(String filename) {
		// Read all the processes. 
		
		
		// For each process, make an event of processArrives. 
		Process p1 = new Process("A", 0, 168, 5, 287);
		Process p2 = new Process("B", 0, 385, 1, 0);
		Process p3 = new Process("C", 250, 1770, 2, 822);
		Process p4 = new Process("D", 190, 97, 5, 2499);
		
		
		Event e1 = new Event(EventType.ARRIVE, 0, p1);
		Event e2 = new Event(EventType.ARRIVE, 0, p2);
		Event e3 = new Event(EventType.ARRIVE, 250, p3);
		Event e4 = new Event(EventType.ARRIVE, 190, p4);
		
		eventQueue.add(e1);
		eventQueue.add(e2);
		eventQueue.add(e3);
		eventQueue.add(e4);
		
		return 0;
	}

	static String cpuQueueString() {
		
		if (cpuQueue.size() == 0)
			return " empty";
		
		Iterator<Process> iter = cpuQueue.iterator();
		String retVal = "";
		while (iter.hasNext()) {
			retVal += " "+iter.next().getID();
			
		}
		
		return retVal;
	}
	
	static int executeEvent() {
		Event e = eventQueue.remove();
		t_milli = e.getTime();
		/*
		System.out.println("\n\n\n");
		
		System.out.println("EventQueue: "+eventQueue.size());
		Iterator<Event> iter=eventQueue.iterator();
		while (iter.hasNext()) {
			System.out.println("    "+iter.next());
		}
		Iterator<Process> iter2 = cpuQueue.iterator();
		System.out.println("ProcessQueue: "+cpuQueue.size());
		while (iter2.hasNext()) {
			System.out.println("    "+iter2.next());
		}
		System.out.println("Type "+e.getType());
		*/
		switch(e.getType()){
		case ARRIVE:
			return processArrives(e);
		case CPUSTART:
			return startCPU(e);
		case CPUDONE:
			return completeCPU(e);
		case IOSTART:
			return startIO(e);
		case IODONE:
			return completeIO(e);
		case TERMINATE:
			return processTerminates(e);
		default:
			return -1;
		}
	}
	
	static int processArrives(Event e) {
		
		if (cpuQueue.isEmpty() && !cpuInUse) {
			Event newEvent = new Event(EventType.CPUSTART, t_milli+cs_t/2, e.getProcess());
			eventQueue.add(newEvent);
		}
		
		cpuQueue.add(e.getProcess());
		
		System.out.println("time "+t_milli+"ms: Process "+e.getProcess().getID()+" arrived [Q"+cpuQueueString()+"]");
		
		return 0;
	}
	
	static int startCPU(Event e) {
		cpuInUse = true;
		
		cpuQueue.remove(); // This is the same process as stored in e.
		System.out.println("time "+t_milli+"ms: Process "+e.getProcess().getID()+" started using the CPU [Q"+cpuQueueString()+"]");
		
		if (e.getProcess().getRemainingCPUBursts() > 1) {
			Event newE = new Event(EventType.CPUDONE, t_milli+e.getProcess().getCPUBurstTime(), e.getProcess());
			eventQueue.add(newE);
		}else{
			Event newE = new Event(EventType.TERMINATE, t_milli+e.getProcess().getCPUBurstTime(), e.getProcess());
			eventQueue.add(newE);
		}
		
		return 0;
	}
	
	static intPreemptCPU(Event e) {
		
		if (algo = SRT) {
			// Pull the completeCPU out of event queue. 
			// Set that process's time remaining variable. 
			// Set completeCPU event for this event instead. 
		}
		
		else if (algo = RR) {
			// If there is nothing left if the queue: 
			if (cpuQueue.isEmpty()) {
				System.out.println("time "+t_milli+": Time slice expired; no preemption because ready queue is empty [Q "+cpuQueueString()+"]")
				return 0;
			}
			
			// Else:
			// Pull the completeCPU out of event queue. 
			// Set that process's time remaining variable. 
			// Set completeCPU event for this event instead. 
			// time 640ms: Time slice expired; process B preempted with 133ms to go [Q D C A B]
			
		}
		
		else { // Weird stuff, shouldn't happen. 
			System.out.println("ERRRRROROOOORRRRRRORORORRRRRRR!!!. Why are we calling Preempt?");
		}
		
		return 0;
	}
	
	static int completeCPU(Event e) {
		
		e.getProcess().executeCPUBurst();
		
		System.out.println("time "+t_milli+"ms: Process "+e.getProcess().getID()+" completed a CPU burst; "+e.getProcess().getRemainingCPUBursts()+" to go [Q"+cpuQueueString()+"]");
		
		Event newIOEvent = new Event(EventType.IOSTART, t_milli, e.getProcess());
		eventQueue.add(newIOEvent);
		
		if (!cpuQueue.isEmpty()) {
			Event newE = new Event(EventType.CPUSTART, t_milli+cs_t, cpuQueue.peek());
			eventQueue.add(newE);
		}
		else {
			cpuInUse = false;
		}
		
		return 0;
	}
	
	static int startIO(Event e) {
		
		System.out.println("time "+t_milli+"ms: Process "+e.getProcess().getID()+" blocked on I/O until time "+(t_milli+e.getProcess().getIOTime())+"ms [Q"+cpuQueueString()+"]");
		
		Event newE = new Event(EventType.IODONE, t_milli+e.getProcess().getIOTime(), e.getProcess());
		eventQueue.add(newE);
		
		return 0;
	}
	
	static int completeIO(Event e) {
		
		if (cpuQueue.isEmpty() && !cpuInUse) {
			Event newE = new Event(EventType.CPUSTART, t_milli+cs_t/2, e.getProcess());
			eventQueue.add(newE);
		}
		
		cpuQueue.add(e.getProcess());
		
		System.out.println("time "+t_milli+"ms: Process "+e.getProcess().getID()+" completed I/O [Q"+cpuQueueString()+"]");
		
		return 0;
	}
	
	static int processTerminates(Event e) {
		
		System.out.println("time "+t_milli+"ms: Process "+e.getProcess().getID()+" terminated [Q"+cpuQueueString()+"]");
		
		// If there is more in the queue, work on that.
		if (!cpuQueue.isEmpty()) {
			Event newE = new Event(EventType.CPUSTART, t_milli+cs_t, cpuQueue.peek());
			eventQueue.add(newE);
		}
		else {
			cpuInUse = false;
		}
		
		return 0;
	}

	public static void main(String args[]) {
		eventQueue = new PriorityQueue<Event>();
		cpuQueue = new LinkedList<Process>();
		cpuInUse = false;
		
		for (int i=0; i<args.length; i++) {
			System.out.println(i+": "+args[i]);
		}
		readFile(args[0]);
		
		algo = AlgorithmType.SRT;
		while(!eventQueue.isEmpty()) {
			executeEvent();
		}
		
		System.out.println("Simulation finished. ");
	}

}
