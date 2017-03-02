import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.File;
import java.util.Scanner;

/* CSCI 4210 OpSys Project 1
 * 
 * Made By: 
 * Kyle Fawcett (fawcek)
 * Peter Wood (woodp)
 * Gavin Petilli (petilg)
 * 
 */


public class SystemSimMain{
	static int cs_t=6;
	static int t_milli=0;
	static Queue<Process> cpuQueue;
	static Queue<Event> eventQueue;

	public void parseFile(String filename){
		try{
			Scanner input = new Scanner(new File(filename));
			while(input.hasNextLine()){
				String line = input.nextLine();
				Process p;
				if(line.charAt(0)!='#'){
					p = parse(line);
				}
			}
		}catch(IOException e){
			System.err.println("ERROR: Cannot read file "+filename+" ("+e.getMessage()+")");
		}
	}


	public Process parse(String in){
		String[] tokens = in.split("|");
		if(tokens.length != 5) throw new IllegalArgumentException("Invalid process description: "+in);
		return new Process(tokens[0], Integer.parseInt(tokens[1]), Integer.parseInt(tokens[2]), Integer.parseInt(tokens[3]), Integer.parseInt(tokens[4]));
	}
	
	static int readFile(String filename) {
		// Read all the processes. 
		
		
		// For each process, make an event of processArrives. 
		Process p1 = new Process('A', 0, 168, 5, 287);
		Process p2 = new Process('B', 0, 385, 1, 0);
		Process p3 = new Process('C', 190, 97, 5, 2499);
		Process p4 = new Process('D', 250, 1770, 2, 822);
				
		Event e1 = new Event("Process Arrives", 0, p1);
		Event e2 = new Event("Process Arrives", 0, p2);
		Event e3 = new Event("Process Arrives", 190, p3);
		Event e4 = new Event("Process Arrives", 250, p4);
		
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
		
		if (e.getType().equals("Process Arrives"))
			processArrives(e);
		else if (e.getType().equals("Start CPU"))
			startCPU(e);
		else if (e.getType().equals("Complete CPU"))
			completeCPU(e);
		else if (e.getType().equals("Start IO"))
			startIO(e);
		else if (e.getType().equals("Complete IO"))
			completeIO(e);
		else if (e.getType().equals("Terminate Process"))
			processTerminates(e);
		
		return 0;
	}
	
	static int processArrives(Event e) {
		
		System.out.println("time "+t_milli+"ms: Process "+e.getProcess().getID()+" arrived [Q"+cpuQueueString()+"]");
		
		if (cpuQueue.isEmpty()) {
			Event newEvent = new Event("Start CPU", t_milli+cs_t/2, e.getProcess());
			eventQueue.add(newEvent);
		}

		cpuQueue.add(e.getProcess());
		
		return 0;
	}
	
	static int startCPU(Event e) {
		
		System.out.println("time "+t_milli+"ms: Process "+e.getProcess().getID()+" started using the CPU [Q"+cpuQueueString()+"]");
		
		if (e.getProcess().getRemainingCPUBursts() > 1) {
			Event newE = new Event("Complete CPU", t_milli+e.getProcess().getCPUBurstTime(), e.getProcess());
			eventQueue.add(newE);
		}
		else {
			Event newE = new Event("Terminate Process", t_milli+e.getProcess().getCPUBurstTime(), e.getProcess());
			eventQueue.add(newE);
		}
		
		return 0;
	}
	
	static int completeCPU(Event e) {
		
		e.getProcess().executeCPUBurst();
		
		System.out.println("time "+t_milli+"ms: Process "+e.getProcess().getID()+" completed a CPU burst; "+e.getProcess().getRemainingCPUBursts()+" to go [Q"+cpuQueueString()+"]");
		
		Event newIOEvent = new Event("Start IO", t_milli, e.getProcess());
		eventQueue.add(newIOEvent);
		
		if (!cpuQueue.isEmpty()) {
			Event newE = new Event("Start CPU", t_milli+cs_t, cpuQueue.remove());
			eventQueue.add(newE);
		}
		
		return 0;
	}
	
	static int startIO(Event e) {
		
		System.out.println("time "+t_milli+"ms: Process "+e.getProcess().getID()+" blocked on I/O until time "+(t_milli+e.getProcess().getIOTime())+"ms [Q"+cpuQueueString()+"]");
		
		Event newE = new Event("Complete IO", t_milli+e.getProcess().getIOTime(), e.getProcess());
		eventQueue.add(newE);
		
		return 0;
	}
	
	static int completeIO(Event e) {
		
		System.out.println("time "+t_milli+"ms: Process "+e.getProcess().getID()+" completed I/O [Q"+cpuQueueString()+"]");
		
		cpuQueue.add(e.getProcess());
		if (cpuQueue.isEmpty()) {
			Event newE = new Event("Start CPU", t_milli+cs_t/2, e.getProcess());
			eventQueue.add(newE);
		}
		
		return 0;
	}
	
	static int processTerminates(Event e) {
		
		System.out.println("time "+t_milli+"ms: Process "+e.getProcess().getID()+" terminated [Q"+cpuQueueString()+"]");
		
		return 0;
	}

	public static void main(String args[]) {
		eventQueue = new PriorityQueue<Event>();
		cpuQueue = new LinkedList<Process>();
		
		for (int i=0; i<args.length; i++) {
			System.out.println(i+": "+args[i]);
		}
		readFile(args[0]);
		
		while(!eventQueue.isEmpty()) {
			executeEvent();
		}
		
		System.out.println("Simulation finished. ");
	}

}
