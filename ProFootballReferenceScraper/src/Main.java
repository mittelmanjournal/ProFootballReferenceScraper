package main2;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class Main {
	public static long INTER_GAME_DELAY;
	
	public static void main(String[] args) {
		HashMap<String, HashMap<String, List<String>>> pidToGidsToStats = new HashMap<String, HashMap<String, List<String>>>();
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
		for (int i = 2; i <= 15; i++) {
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
						System.out.println("Invalid successfully entered, SHOULD BE WRITING: " + games.get(x).getID() + " to invalid text file");
						invalidWriter.write(games.get(x).getID());
						invalidWriter.newLine();
					}
					for(List<String> als : games.get(x).getAllPlayerData()) {
						
						if(!pidToGidsToStats.containsKey(als.get(0))) {
							pidToGidsToStats.put(als.get(0), new HashMap<String, List<String>>());
						}
						pidToGidsToStats.get(als.get(0)).put(games.get(x).getID(), als.subList(1, als.size()));//this line is only valid because we can assume that we will never see the same gameID in our traversal
					}
					//rewrite the updated hashmap to a file that tracks it
					
					BufferedWriter hashmapWriter = null;
					try {
						hashmapWriter = new BufferedWriter(new FileWriter("hashmapData.txt"));
					} catch (IOException e2) {
						e2.printStackTrace();
					}
					hashmapWriter.write(pidToGidsToStats.toString());
					hashmapWriter.close();
					
					System.out.println(games.get(x).toString(":%%:"));
					//System.out.println(pidToGidsToStats.toString());
					System.out.println();
					x++;	
				}

			} catch (IOException | InterruptedException | NullPointerException e) {
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
