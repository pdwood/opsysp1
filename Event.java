import java.util.Comparator;

/* CSCI 4210 OpSys Project 1
 * 
 * Made By: 
 * Kyle Fawcett (fawcek)
 * Peter Wood (woodp)
 * Gavin Petilli (petilg)
 * 
 */


public class Event implements Comparable<Event> {
		
	private	EventType type;
	private int time;
	private Process proc;

	public Event(EventType eventType, int eventTime, Process process) {
		type=eventType;
		time=eventTime;
		proc = process;
	}
	
	public EventType getType() {return type;}
	public int getTime() {return time;}
	public Process getProcess() {return proc;}
	public void setProcess(Process process) {proc = process;}

	/*public static int compare(Event arg0, Event arg1) {
		if (arg0.getTime() > arg1.getTime())
			return 1;
		if (arg0.getTime() < arg1.getTime())
			return -1;
		return 0;
	}*/
	
	public int compareTo(Event arg1) {
		if (this.getTime() > arg1.getTime())
			return 1;
		if (this.getTime() < arg1.getTime())
			return -1;
		return 0;
	}
}


// KRF