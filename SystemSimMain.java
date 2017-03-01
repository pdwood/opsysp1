import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.File;
import java.util.Scanner;




public class SystemSimMain{

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


	public static void main(String[] args){
		/*do stuff*/
	}

}