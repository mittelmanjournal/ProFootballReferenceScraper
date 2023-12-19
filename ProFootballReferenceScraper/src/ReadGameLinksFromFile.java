package main2;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class ReadGameLinksFromFile {

	public static void main(String[] args) {
		// Specify the file path you want to read
		int j = 1;
		for (int i = 2; i <= 23; i++) {
			String filePath;
			if(i<10) {
				filePath = "games_year/games_200" + i + ".txt";
			}
			else {
				filePath = "games_year/games_20" + i + ".txt";
			}
				

			try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
				String line;

				// Read and print each line from the file
				int x = 1;

				while ((line = reader.readLine()) != null) {
					System.out.println("All "+j+", This season " + x + ": " + line);
					x++;
					j++;
				}

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
