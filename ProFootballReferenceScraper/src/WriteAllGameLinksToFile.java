package main2;


import main.NFLscrape.Getters;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class WriteAllGameLinksToFile {

    public static void main(String[] args) {
        for (int year = 2023; year < 2024; year++) {
            // Create a new file for each year
            String fileName = "games_" + year + ".txt";
            
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
                for (int week = 1; week <= 14; week++) {
                    String url = "https://www.pro-football-reference.com/years/" + year + "/week_" + week + ".htm";
                    System.out.println("Currently Scraping " + year + " week " + week + "\'s games from " + url);

                    for (String game : Getters.getGameLinksGivenWeekAndYear(url)) {
                        System.out.println(game);
                        
                        // Write each game to the file
                        writer.write(game);
                        writer.newLine();
                    }
                }
                System.out.println("Games for year " + year + " written to " + fileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
