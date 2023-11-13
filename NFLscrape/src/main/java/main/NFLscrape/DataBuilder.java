package main.NFLscrape;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DataBuilder {
	public static Document doc;
	public static int year = 2002;
	// week 18 starts 2021
	public static int week = 16;

	// how far back do you want to get the player averages from
	// if you use a value here it must be less than 17 for 2002-2020 and less than
	// 18 for years after.
	// if weeksExtendPastCurrent is true, then get the average for the input of
	// weeks even if you must extend into previous seasons and mix partial seasons
	// data. If it is false, even if the value in backAverageWeeks is greater than
	// the amount of back iterable weeks in the current season from the point of
	// this current game, do not extend past this current season when collecting the
	// average, stop after collecting week 1 data of current season, this means that
	// even if we are observing a week 1 game our averages will be 0
	public int backAverageWeeks;

	public boolean weeksExtendPastCurrent;

	// if you use a number of seasons to get the average it will ignore the
	// backAverageWeeks value (the amount of seasons is additional
	// to getting the averages of the weeks in the same season this game was played
	// in) By default or if this val is -1 just get the max years average for each
	// player
	public int backAverageSeasons;

	// each data row will be formatted as follows: awayScore, homeScore, external
	// data, awayTeamData, homeTeamData
	// the goal is to predict the variables awayScore and homeScore given
	// externalData, awayTeamData, and homeTeamData

	public static void main(String[] args) throws IOException, InterruptedException {
//		String url = "https://www.pro-football-reference.com/years/" + year + "/week_" + week + ".htm";
//		int c = 1;
//		while (c <= 1000) {
//			doc = Jsoup.connect(url).get();
//			System.out.println(c);
//			TimeUnit.SECONDS.sleep(2);
//			c++;
//		}
		for (week = 1; week <=17; week++) {
			String url = "https://www.pro-football-reference.com/years/" + year + "/week_" + week + ".htm";
			//TimeUnit.SECONDS.sleep(2);
			//doc = Jsoup.connect(url).get();
			System.out.println(url + '\n');
			List<String> games = getGameLinksGivenWeekAndYear(url); //returns a list of game links /boxscores/201310100chi.htm
			for (String game : games) { //for each game link
				String out = "";
				// System.out.println(getGameScore(game) + ":::" + game);
				List<Integer> list = getGameScore(game); //2 value list 0 = away team score, 1 = home team score
				out += "Away Game Score: " + list.get(0) + " - Home Game Score: " + list.get(1) + " "; // stringify
				int[] WL = getWLforGame(game, list.get(0), list.get(1)); //get wlt for both teams in game given the awayScore and homeScore
				out += "Away Team Record: " + WL[0] + "-" + WL[1] + "-" + WL[2] + " Home team Record: " + WL[3] + "-" + WL[4] + "-" + WL[5] + " :: " + game;
				System.out.println(out);
			}
		}

	}
	
	//this doesnt use the same doc as getGameScore and getWLforGame
	public static List<String> getGameLinksGivenWeekAndYear(String url) {
		List<String> games = new ArrayList<String>();

		try {
			TimeUnit.SECONDS.sleep(2); 
			doc = Jsoup.connect(url).get();
			
			// Select the game summary div elements
			Elements gameSummaries = doc.select("div.game_summary");//good

			for (Element gameSummary : gameSummaries) {
				
				// Find the game link element within each game summary
				Element gameLinkElement = gameSummary.selectFirst("td.gamelink a");

				// Check if the game link element is present
				if (gameLinkElement != null) {
					TimeUnit.SECONDS.sleep(2); 
					String gameLink = gameLinkElement.attr("href");
					System.out.println("https://www.pro-football-reference.com" + gameLink);
					games.add("https://www.pro-football-reference.com" + gameLink);
				}
			}

		} catch (Exception IOE) {
			IOE.printStackTrace();
		}
		return games;

	}
	
	//getGameScore uses the same Document as getWLforGame, just pass a single document between them to not have to delay
	public static List<Integer> getGameScore(String url) {
		List<Integer> scores = new ArrayList<Integer>();

		try {
			// Fetch the HTML content from the URL
			TimeUnit.SECONDS.sleep(2); 
			doc = Jsoup.connect(url).get();
			
			// Find the score elements
			Elements scoreElements = doc.select(".score");

			// Extract the scores (assuming away team score is first and home team score is
			// second)
			for (Element scoreElement : scoreElements) {
				int score = Integer.parseInt(scoreElement.text());
				scores.add(score);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return scores;
	}

	// THIS TOTALLY IGNORES TIE HANDLING
	public static int[] getWLforGame(String url, int awayScore, int homeScore) {
		int[] ret = new int[6];
		try {
			TimeUnit.SECONDS.sleep(2); 
			doc = Jsoup.connect(url).get();
			Elements divElements = doc.select("div.scorebox");
			String rawScoreboxText = divElements.text();
			String[] arr = splitString(rawScoreboxText, " via Sports Logos.net About logos ");

			String awayRecord = extractBetween(arr[0], awayScore + " ", " Prev");
			String homeRecord = extractBetween(arr[1], homeScore + " ", " Prev");
			// if its week 1 there is no prev game option so must change it to next game
			if (week == 1) {
				awayRecord = extractBetween(arr[0], awayScore + " ", " Next");
				homeRecord = extractBetween(arr[1], homeScore + " ", " Next");

			}

			String[] awayWL = splitString(awayRecord, "-");
			String[] homeWL = splitString(homeRecord, "-");

			int awayWins = Integer.parseInt(awayWL[0]);
			int awayLosses;
			int awayTies;
			int homeWins = Integer.parseInt(homeWL[0]);
			int homeLosses;
			int homeTies;

			if (awayWL[1].length() >= 3) {
				String[] awayLT = splitString(awayWL[1], "-");
				awayLosses = Integer.parseInt(awayLT[0]);
				awayTies = Integer.parseInt(awayLT[1]);
			} else {
				awayLosses = Integer.parseInt(awayWL[1]);
				awayTies = 0;
			}

			if (homeWL[1].length() >= 3) {
				String[] homeLT = splitString(homeWL[1], "-");
				homeLosses = Integer.parseInt(homeLT[0]);
				homeTies = Integer.parseInt(homeLT[1]);
			} else {
				homeLosses = Integer.parseInt(homeWL[1]);
				homeTies = 0;
			}

			// here figure out who won and then subtract what must be subtracted
			if (awayScore > homeScore) {
				awayWins--;
				homeLosses--;
			} else if (awayScore < homeScore) {
				awayLosses--;
				homeWins--;
			} else if (homeScore == awayScore) {
				awayTies--;
				homeTies--;
			}
			// account for if score == here and subtract 1 from tie value

			ret[0] = awayWins;
			ret[1] = awayLosses;
			ret[2] = awayTies;
			ret[3] = homeWins;
			ret[4] = homeLosses;
			ret[5] = homeTies;

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// don't forget to update values accordingly with who won and lost
		return ret;
	}

	public static String[] splitString(String original, String input) {
		String[] result = new String[2];

		int index = original.indexOf(input);

		if (index != -1) {
			// Split the string based on the input string
			result[0] = original.substring(0, index);
			result[1] = original.substring(index + input.length());
		} else {
			// If the input string is not found, return the original string as the first
			// part
			result[0] = original;
			result[1] = "";
		}

		return result;
	}

	public static String extractBetween(String original, String firstPart, String lastPart) {
		int startIndex = original.indexOf(firstPart);
		int endIndex = original.lastIndexOf(lastPart);

		if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
			// Add the length of firstPart to startIndex to avoid including it
			startIndex += firstPart.length();

			// Extract the substring between startIndex and endIndex
			return original.substring(startIndex, endIndex);
		} else {
			// Return an empty string if firstPart or lastPart not found or in the wrong
			// order
			return "";
		}
	}

	
	
}
