package main2;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class Main {
	
	public static long GAME_TIME_USED;
	
	public static void main(String[] args) {
		HashMap<String, HashMap<String, List<String>>> pidToGidsToStats = new HashMap<String, HashMap<String, List<String>>>();
		BufferedWriter validWriter = null;
		BufferedWriter invalidWriter = null;
		BufferedWriter hashmapWriter = null;
		try {
			validWriter = new BufferedWriter(new FileWriter("valid_games.txt"));
			invalidWriter = new BufferedWriter(new FileWriter("invalid_games.txt"));
			hashmapWriter = new BufferedWriter(new FileWriter("hashmapData.txt"));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		// Specify the file path you want to read
		ArrayList<Game> games = new ArrayList<Game>();
		int x = 0;
		List<ArrayList<String>> listOfListOfLinks = new ArrayList<ArrayList<String>>();
		
		for (int i = 2; i <= 23; i++) {
			String filePath = "games_year/games_" + (2000 + i) + ".txt";
			
			ArrayList<String> curYearLinks = new ArrayList<String>();
			
			try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
				String link;
				
				while ((link = reader.readLine()) != null) {
					curYearLinks.add(link);
				}
				reader.close(); //REMOVE THIS IF FAILS TO READ WHEN CHANGING FILES
				listOfListOfLinks.add(curYearLinks);
			} catch (IOException | NullPointerException e) {
				e.printStackTrace();
			}
			
		}
		
		int year = 2002;
		for(ArrayList<String> links : listOfListOfLinks) {
			
			for(String link : links) {
				long startTime = 0;
				try {
					//TODO: TO COMBAT GETTING A READ TIMEOUT (which messes up the entire program because the write stream closes on exception catch)
					games.add(new Game(link));
					
					//to optimize, count time below GET TIME FROM HERE
					//get total time used
					startTime = System.currentTimeMillis();
					
					games.get(x).setYear(year); // this is necessary
					if(!games.get(x).toString(":%%:").contains("null")) {
						//write it to String as a line in the valid file
						validWriter.write(games.get(x).toString(":%%:"));
						validWriter.newLine();
					} else if (games.get(x).toString(":%%:").contains("null")){
						//write it's link to invalid file
						System.out.println("Invalid successfully entered, SHOULD BE WRITING: " + games.get(x).getID() + " to invalid text file");
						invalidWriter.write(games.get(x).getID() + " : " + games.get(x).invalidPosition);
						invalidWriter.newLine();
					}
					for(List<String> als : games.get(x).getAllPlayerData()) {
						
						if(!pidToGidsToStats.containsKey(als.get(0))) {
							pidToGidsToStats.put(als.get(0), new HashMap<String, List<String>>());
						}
						pidToGidsToStats.get(als.get(0)).put(games.get(x).getID(), als.subList(1, als.size()));//this line is only valid because we can assume that we will never see the same gameID in our traversal
					}
					//rewrite the updated hashmap to a file that tracks it
					hashmapWriter.write(pidToGidsToStats.toString());
					
					System.out.println(games.get(x).toString(":%%:"));
					//System.out.println(pidToGidsToStats.toString());
					System.out.println();
					x++;	
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				//get total time used
				long endTime = System.currentTimeMillis();
				
				long timeConsumed = endTime - startTime;
				
				long totalTimeUsed = GAME_TIME_USED + timeConsumed;
				try {
					if(2000 - totalTimeUsed <= 0) {
						Thread.sleep(2000);
					} else {
						Thread.sleep(2000 - totalTimeUsed);
					}
				} catch (InterruptedException ie) {
					ie.printStackTrace();
				}
				
				
			}
			year++;
		}
			
		
		
		try {
			validWriter.close();
			invalidWriter.close();
			hashmapWriter.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
	}
}
