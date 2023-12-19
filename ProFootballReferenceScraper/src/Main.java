package main2;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class Main {
	public static long INTER_GAME_DELAY;
	
	public static void main(String[] args) {
		BufferedWriter validWriter = null;
		BufferedWriter invalidWriter = null;
		try {
			validWriter = new BufferedWriter(new FileWriter("valid_games.txt"));
			invalidWriter = new BufferedWriter(new FileWriter("invalid_games.txt"));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		// Specify the file path you want to read
		ArrayList<Game> games = new ArrayList<Game>();
		int x = 0;
		for (int i = 2; i <= 23; i++) {
			String filePath = "games_year/games_" + (2000 + i) + ".txt";
			
				
			try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
				String link;
				
				while ((link = reader.readLine()) != null) {
					
					games.add(new Game(link));
					Thread.sleep(INTER_GAME_DELAY);
					//to optimize, count time below GET TIME FROM HERE
					games.get(x).setYear(2000 + i); // this is necessary
					if(!games.get(x).toString(":%%:").contains("null")) {
						//write it to String as a line in the valid file
						validWriter.write(games.get(x).toString(":%%:"));
						validWriter.newLine();
					} else if (games.get(x).toString(":%%:").contains("null")){
						//write it's link to invalid file
						invalidWriter.write(games.get(x).getID());
						invalidWriter.newLine();
					}
					//update hashmap based on what this game object scraped whether or not its valid or invalid
					//rewrite the updated hashmap to a file that tracks it
					System.out.println(games.get(x).toString(":%%:"));
					System.out.println();
					x++;
					
					//TO GET TIME FROM HERE
					//and do the time to delay given by game constructor subtracted by time above
					
					//I want to have the ability to start at a specific game in the iteration
					
					//DONE - set the id before creating the document in the game class
					//- create the player stat line class 
					//- create the player id string to player stat line object hashmap *NOTE coaches will only have historical data included
					//UNNEEDED - whenever invalid position acronym seen take note of it
					//DONE - change the to string of a game object to something simpler on one line so that you can turn it into a game object
					//DONE - add a method that takes in a string and creates a game object from it
					//DONE- write all valid games to one file and all invalid games to another file
					//- write the hashmap of link to box score data object in a file after each game object construction
					//DONE - check to see that if the position string contains a "/" because it is formatted "FB/TE" likeso, just get the String before the "/"
					
					//after doing all above, you will have all of the game relevant data saved
					//meaning that you won't have to go over any of the pages any more for the valid Game objects.
					//This can allow us to construct from the notepad each game object and the only other data we need
					//beyond this point is each starter's historic seasonal data.
				}

			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
				try {
					validWriter.close();
					invalidWriter.close();
				} catch (IOException ioe) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		try {
			validWriter.close();
			invalidWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
