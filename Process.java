public class Process{
	private String name;
	private int arrival, burstTime, bursts, ioTime;

	public Process(String name, int arrival, int burstTime, int bursts, int ioTime){
		this.name=name;
		this.arrival=arrival;
		this.burstTime=burstTime;
		this.bursts=bursts;
		this.ioTime=ioTime;
	}

}