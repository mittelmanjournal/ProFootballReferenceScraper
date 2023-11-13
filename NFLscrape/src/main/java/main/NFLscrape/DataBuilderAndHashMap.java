package main.NFLscrape;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataBuilderAndHashMap {

	static long startTime;
	static long endTime;
	static long getSleepTime() {
		long ret = (long) (2000.0 - (endTime/1000000.0 - startTime/1000000.0));
		System.out.println(ret);
		if(ret < 0)	return 0; 
		else return ret; // 
	}	
	
	static HashMap<String, List<List<Double>>> returnerLinkToData = new HashMap<String, List<List<Double>>>();
	static HashMap<String, List<List<Double>>> playerLinkToData = new HashMap<String, List<List<Double>>>();
	// public static Document doc;
	static Document staticDoc;

	public static int year;
	// week 18 starts 2021
	public static int week;

	public int backAverageWeeks;

	public boolean weeksExtendPastCurrent;

	public int backAverageSeasons;

	public static void main(String[] args) throws IOException, InterruptedException {
		long startTime = System.nanoTime();

		//System.out.println("Weekday, Month, Day of Month, Year, Time, Roof, Field, O/U, Away Points, Home Points, ");
		
		for (year = 2002; year < 2021; year++) {
			for (week = 1; week <= 17; week++) {
				String url = "https://www.pro-football-reference.com/years/" + year + "/week_" + week + ".htm";
				// System.out.println(url);
				for (String game : getGameLinksGivenWeekAndYear(url)) {
					System.out.println(getSingleGameInfo(game));
				}
				System.out.println();
			}
		}
		long endTime = System.nanoTime();
		long elapsedTime = endTime - startTime;
		System.out.println("Program took " + elapsedTime + " nanoseconds to complete.");
	}

	// this doesnt use the same doc as getGameScore and getWLforGame
	public static List<String> getGameLinksGivenWeekAndYear(String url) {
		List<String> games = new ArrayList<String>();

		try {
			Thread.sleep(2000);
			staticDoc = Jsoup.connect(url).get();

			// Select the game summary div elements
			Elements gameSummaries = staticDoc.select("div.game_summary");// good

			for (Element gameSummary : gameSummaries) {

				// Find the game link element within each game summary
				Element gameLinkElement = gameSummary.selectFirst("td.gamelink a");

				// Check if the game link element is present
				if (gameLinkElement != null) {
					String gameLink = gameLinkElement.attr("href");
					// System.out.println("https://www.pro-football-reference.com" + gameLink);
					games.add("https://www.pro-football-reference.com" + gameLink);
				}
			}

		} catch (Exception IOE) {
			IOE.printStackTrace();
		}
		return games;

	}

	public static String getSingleGameInfo(String url) {
		String gameId = url.replace("https://www.pro-football-reference.com/boxscores/", "");
		gameId = gameId.replace(".htm", "");
		String out = gameId + ", " + week + ", ";
		try {
			Thread.sleep(2000);
			Document doc = Jsoup.connect(url).get();

			out += getDateAndTime(doc) + ", ";

			List<String> externalInfo = externalGameInfo(doc);
			out += "Roof Status: " + externalInfo.get(0) + ", Field Texture: " + externalInfo.get(1) + ", Over/Under: "
					+ externalInfo.get(2) + ", Vegas Line: " + externalInfo.get(3);

			List<Integer> gameScore = getGameScore(doc); // 2 value list 0 = away team score, 1 = home team score
			out += ", Away Game Score: " + gameScore.get(0) + ", Home Game Score: " + gameScore.get(1) + ", "; // stringify

			List<String> coachLinks = getCoachLinks(doc);
			out += "Away Coach: " + coachLinks.get(0) + ", Home Coach: " + coachLinks.get(1);

			int[] WLT = getWLTforGame(doc, gameScore.get(0), gameScore.get(1)); // get wlt for both teams in game given
																				// the awayScore and homeScore
			out += ", Away Team Record: " + WLT[0] + "-" + WLT[1] + "-" + WLT[2] + ", Home team Record: " + WLT[3] + "-"
					+ WLT[4] + "-" + WLT[5];

			String[] awayRoster = new String[25];
			String[] homeRoster = new String[25];

			int i = 0;
			for (String s : getStarterOffenseAndDefenseLinks(doc, "vis")) {
				awayRoster[i] = s;
				i++;
			}

			List<String> returners = getReturners(doc);
			List<String> kickersAndPunters = getKickersAndPunters(doc);

			awayRoster[i] = kickersAndPunters.get(0);
			awayRoster[i + 1] = kickersAndPunters.get(1);

			awayRoster[i + 2] = returners.get(0);

			i = 0;
			for (String s : getStarterOffenseAndDefenseLinks(doc, "home")) {
				homeRoster[i] = s;
				i++;
			}

			homeRoster[i] = kickersAndPunters.get(2);
			homeRoster[i + 1] = kickersAndPunters.get(3);

			homeRoster[i + 2] = returners.get(1);

			for(List<Double> playerDataList : updateLinkToDataMap(playerLinkToData, awayRoster))
				for(Double stat : playerDataList)
					out += ", " + stat;
			
			for(List<Double> playerDataList : updateLinkToDataMap(playerLinkToData, homeRoster))
				for(Double stat : playerDataList)
					out += ", " + stat;

		} catch (IOException ioe) {
			ioe.printStackTrace();
		} catch (InterruptedException ie) {
			ie.printStackTrace();
		}
		return out;

	}

	// getGameScore uses the same Document as getWLforGame, just pass a single
	// document between them to not have to delay
	public static List<Integer> getGameScore(Document doc) {
		List<Integer> scores = new ArrayList<Integer>();

		try {
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

	public static int[] getWLTforGame(Document doc, int awayScore, int homeScore) {
		int[] ret = new int[6];
		try {
			Elements divElements = doc.select("div.scorebox");
			String rawScoreboxText = divElements.text();
			String[] arr = Helpers.splitString(rawScoreboxText, " via Sports Logos.net About logos ");

			String awayRecord = Helpers.extractBetween(arr[0], awayScore + " ", " Prev");
			String homeRecord = Helpers.extractBetween(arr[1], homeScore + " ", " Prev");
			// if its week 1 there is no prev game option so must change it to next game
			if (week == 1) {
				awayRecord = Helpers.extractBetween(arr[0], awayScore + " ", " Next");
				homeRecord = Helpers.extractBetween(arr[1], homeScore + " ", " Next");

			}

			String[] awayWL = Helpers.splitString(awayRecord, "-");
			String[] homeWL = Helpers.splitString(homeRecord, "-");

			int awayWins = Integer.parseInt(awayWL[0]);
			int awayLosses;
			int awayTies;
			int homeWins = Integer.parseInt(homeWL[0]);
			int homeLosses;
			int homeTies;

			if (awayWL[1].length() >= 3) {
				String[] awayLT = Helpers.splitString(awayWL[1], "-");
				awayLosses = Integer.parseInt(awayLT[0]);
				awayTies = Integer.parseInt(awayLT[1]);
			} else {
				awayLosses = Integer.parseInt(awayWL[1]);
				awayTies = 0;
			}

			if (homeWL[1].length() >= 3) {
				String[] homeLT = Helpers.splitString(homeWL[1], "-");
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

	public static String getDateAndTime(Document doc) {
		// Select div elements with class 'scorebox_meta'
		Elements elements = doc.select(".scorebox_meta div");
		String date = elements.get(0).text();
		String time = elements.get(1).text();
		time = time.replace("Start Time: ", "");
		String ret = Helpers.dateToCSV(date) + ", " + Helpers.timeToCSV(time);
		return ret;
	}

	// returns String list 0th element is away coach, 1st element is home coach
	public static List<String> getCoachLinks(Document doc) {
		// Select all elements with class "datapoint" and extract coach links
		Elements coachElements = doc.select("div.datapoint strong:contains(Coach) + a");
		List<String> ret = new ArrayList<String>();
		for (Element coachElement : coachElements) {
			String coachName = coachElement.text();
			String coachLink = coachElement.attr("href");
			// System.out.println("Coach Name: " + coachName);
			// System.out.println("Coach Link: " + coachLink);
			ret.add(coachLink);
		}
		return ret;
	}

	public static List<String> getKickersAndPunters(Document doc) {
		String returnerTable = doc.selectFirst("div#all_kicking").html();
		return Helpers.extractStringsBetween(returnerTable, "data-stat=\"player\" ><a href=\"", "\">");
		// System.out.println(legList);
//			String awayK = legList.get(0);
//			String awayP = legList.get(1);
//			String homeK = legList.get(2);
//			String homeP = legList.get(3);
	}

	// returns list index values: 0 = roof (closed/open), 1 = field (grass/turf), 2
	// = over under line
	public static List<String> externalGameInfo(Document doc) {
		List<String> ret = new ArrayList<String>();
		Element gameInfoDiv = doc.selectFirst("div#all_game_info");
		String s = gameInfoDiv.outerHtml();

		String roof = Helpers.extractStringBetween(s,
				"<tr ><th scope=\"row\" class=\"center \" data-stat=\"info\" >Roof</th><td class=\"center \" data-stat=\"stat\" >",
				"</td></tr>");
		String surface = Helpers.extractStringBetween(s,
				"<tr ><th scope=\"row\" class=\"center \" data-stat=\"info\" >Surface</th><td class=\"center \" data-stat=\"stat\" >",
				"</td></tr>");
		String overUnder = Helpers.extractStringBetween(s,
				"<tr ><th scope=\"row\" class=\"center \" data-stat=\"info\" >Over/Under</th><td class=\"center \" data-stat=\"stat\" >",
				" <b>");

		String vegasLine = Helpers.extractStringBetween(s,
				"<th scope=\"row\" class=\"center \" data-stat=\"info\">Vegas Line</th><td class=\"center \" data-stat=\"stat\">",
				"/td></tr>");

		ret.add(roof);
		ret.add(surface);
		ret.add(overUnder);
		ret.add(vegasLine);

		return ret;
	}

	// each inner list represents the stats of a player up until the game date
	public static List<List<Double>> updateLinkToDataMap(HashMap<String, List<List<Double>>> hm, String[] playerLinks) throws InterruptedException {
		int i = 0;
		for (String playerLink : playerLinks) {
			if (i == 24) {
				if (returnerLinkToData.containsKey(playerLink) || playerLink.equals(null) || playerLink.equals(""))	continue;
				Thread.sleep(getSleepTime());
				startTime = System.nanoTime();
				endTime = 0;
				returnerLinkToData.put(playerLink, getReturnerData(playerLink));
				continue;
			}
			if (hm.containsKey(playerLink) || playerLink.equals(null) || playerLink.equals(""))	continue;
			Thread.sleep(getSleepTime());
			startTime = System.nanoTime();
			endTime = 0;
			hm.put(playerLink, getPlayerData(playerLink));
			i++;
		}

		// create the data point below
		// each list inside the list represents 1 accumulated players data
		List<List<Double>> rosterDataUpToGame = new ArrayList<List<Double>>();
		for (int x = 0; x < playerLinks.length; x++) {
			System.out.println(playerLinks[x]);
			if (returnerLinkToData.containsKey(playerLinks[x]) && x == playerLinks.length - 1) {
				rosterDataUpToGame.add(getReturnerDataUpToGame(returnerLinkToData.get(playerLinks[x])));
			} else {
				// add weekly data from weekly hashmap collections here as you have access to the 
				// playerLink and you can directly add the values to the output of getDataUpToGame output
				// make the weekly map static, map from linkString -> List<Double> accumulate the values of each game
				// for each player here based on the stat category in box score, maybe pass parameter of game Document
				// or make it static in order to access the box score stats
				rosterDataUpToGame.add(getDataUpToGame(hm.get(playerLinks[x])));
			}
		}

		return rosterDataUpToGame;

	}

	public static List<Double> getReturnerDataUpToGame(List<List<Double>> returnerData) {
		List<Double> ret = new ArrayList<Double>();
		ret.add(returnerData.get(0).get(1)); // add the height of the player
		ret.add(returnerData.get(0).get(2)); // add the weight of the player
		
		double age = -1;
		int gamesPlayed = 0;
		int KRs = 0;
		int KRyds = 0;
		int KRtds = 0;
		int PRs = 0;
		int PRyds = 0;
		int PRtds = 0;

		for(int i = 1; i < returnerData.size(); i++) {
			if(returnerData.get(i).get(0) < year) {
				//the values below may be -1 if null, meaning that if a player got no TDs in a year for whatever reason
				//instead of adding 0 touchdowns it will add -1 touchdown
				if(returnerData.get(i).get(0) == year - 1) age = returnerData.get(i).get(1);
				if(returnerData.get(i).get(2) >= 0) gamesPlayed += returnerData.get(i).get(2); //index of games played
				if(returnerData.get(i).get(3) >= 0) KRs += returnerData.get(i).get(3);
				if(returnerData.get(i).get(4) >= 0) KRyds += returnerData.get(i).get(4);
				if(returnerData.get(i).get(5) >= 0) KRtds += returnerData.get(i).get(5);
				if(returnerData.get(i).get(6) >= 0) PRs += returnerData.get(i).get(6);
				if(returnerData.get(i).get(7) >= 0) PRyds += returnerData.get(i).get(7);
				if(returnerData.get(i).get(8) >= 0) PRtds += returnerData.get(i).get(8);
			} else if (returnerData.get(1).get(0) == year) age = returnerData.get(1).get(1) - 1;
		}
		
		ret.add(age);
		ret.add((double) gamesPlayed);
		ret.add((double) KRs);
		ret.add((double) KRyds);
		ret.add((double) KRtds);
		ret.add((double) PRs);
		ret.add((double) PRyds);
		ret.add((double) PRtds);
		
		return ret;
	}

	//we take in a list of lists of yearly data and physical data
	public static List<Double> getDataUpToGame(List<List<Double>> playerData) {
		if (playerData == null) {
			List<Double> ret = new ArrayList<Double>();
			ret.add(-1.0);
			return ret;
		}
		//we know the year and the week this game occurred,
		//given: any non-returner player data, accumulate each of their categories until (including) 1 before the year of the current iterating season
		//also accumulate the weekly data if the week isn't week 1
		
		double position = playerData.get(0).get(0);
		
		switch((int)position) {
		  case 1:
			return getQBDataUpToGame(playerData);
		  case 2:
			return getRunRecDataUpToGame(playerData);
		  case 3:
			return getOLDataUpToGame(playerData);
		  case 4:
			return getDefenderDataUpToGame(playerData);
		  case 5:
			return getKickerDataUpToGame(playerData);
		  case 6:
			return getPunterDataUpToGame(playerData);
		  default:
		    return null;
		}
	}

	public static List<Double> getQBDataUpToGame(List<List<Double>> playerData) {
		List<Double> ret = new ArrayList<Double>();
		ret.add(playerData.get(0).get(1)); // add the height of the player
		ret.add(playerData.get(0).get(2)); // add the weight of the player

		double age = -1;
		int gamesPlayed = 0;
		double startingWinRateTotal = 0;
		int completions = 0;
		int attempts = 0;
		int passYards = 0;
		int passTDs = 0;
		int passInts = 0;
		int timePassedOnFirstDown = 0;
		int timesSacked = 0;
		int yardsLostOnSacks = 0;
		int gameWinningDrives = 0;
		int approximateValue = 0;
		int rushAttempts = 0;
		int rushYards = 0;
		int rushTDs = 0;
		int fumbles = 0;
		// calculate qbrating

		for (int i = 1; i < playerData.size(); i++) {
			if (playerData.get(i).get(0) < year) {
				if (playerData.get(i).get(0) == year - 1) age = playerData.get(i).get(1);
				if (playerData.get(i).get(2) >= 0) gamesPlayed += playerData.get(i).get(2);
				if (playerData.get(i).get(3) >= 0) startingWinRateTotal += playerData.get(i).get(3);
				if (playerData.get(i).get(4) >= 0) completions += playerData.get(i).get(4);
				if (playerData.get(i).get(5) >= 0) attempts += playerData.get(i).get(5);
				if (playerData.get(i).get(6) >= 0) passYards += playerData.get(i).get(6);
				if (playerData.get(i).get(7) >= 0) passTDs += playerData.get(i).get(7);
				if (playerData.get(i).get(8) >= 0) passInts += playerData.get(i).get(8);
				if (playerData.get(i).get(9) >= 0) timePassedOnFirstDown += playerData.get(i).get(9);
				if (playerData.get(i).get(11) >= 0) timesSacked += playerData.get(i).get(11); // 10 is qbRating, but we calculate it using formula
				if (playerData.get(i).get(12) >= 0) yardsLostOnSacks += playerData.get(i).get(12);
				if (playerData.get(i).get(13) >= 0) gameWinningDrives += playerData.get(i).get(13);
				if (playerData.get(i).get(14) >= 0) approximateValue += playerData.get(i).get(14);
				if (playerData.get(i).get(15) >= 0) rushAttempts += playerData.get(i).get(15);
				if (playerData.get(i).get(16) >= 0)	rushYards += playerData.get(i).get(16);
				if (playerData.get(i).get(17) >= 0)	rushTDs += playerData.get(i).get(17);
				if (playerData.get(i).get(18) >= 0) fumbles += playerData.get(i).get(18);
			} else if (playerData.get(1).get(0) == year) age = playerData.get(1).get(1) - 1;
		}
		ret.add(age);
		ret.add((double) gamesPlayed);
		ret.add(startingWinRateTotal / (playerData.size() - 1));
		ret.add((double) completions);
		ret.add((double) attempts);
		ret.add((double) passYards);
		ret.add((double) passTDs);
		ret.add((double) passInts);
		ret.add((double) timePassedOnFirstDown);
		ret.add((double) timesSacked);
		ret.add((double) yardsLostOnSacks);
		ret.add((double) gameWinningDrives);
		ret.add((double) approximateValue);
		ret.add((double) rushAttempts);
		ret.add((double) rushYards);
		ret.add((double) rushTDs);
		ret.add((double) fumbles);
		ret.add(Helpers.getPasserRating(completions, attempts, passYards, passTDs, passInts));

		return ret;

	}
	
	public static List<Double> getRunRecDataUpToGame(List<List<Double>> playerData){
		List<Double> ret = new ArrayList<Double>();
		ret.add(playerData.get(0).get(1)); // add the height of the player
		ret.add(playerData.get(0).get(2)); // add the weight of the player
		
		double age = -1;
		int gamesPlayed = 0;
		int targets = 0;
		int receptions = 0;
		int receivingYards = 0;
		int receivingTDs = 0;
		int firstDownReceptions = 0;
		int rushAttempts = 0;
		int rushYards = 0;
		int rushTDs = 0;
		int firstDownRushes = 0;
		int fumbles = 0;
		int approximateValue = 0;
		
		for(int i = 1; i < playerData.size(); i++) {
			if(playerData.get(i).get(0) < year) {
				//the values below may be -1 if null, meaning that if a player got no TDs in a year for whatever reason
				//instead of adding 0 touchdowns it will add -1 touchdown
				if(playerData.get(i).get(0) == year - 1) age = playerData.get(i).get(1);
				if(playerData.get(i).get(2) >= 0) gamesPlayed += playerData.get(i).get(2); //index of games played
				if(playerData.get(i).get(3) >= 0) targets += playerData.get(i).get(3);
				if(playerData.get(i).get(4) >= 0) receptions += playerData.get(i).get(4);
				if(playerData.get(i).get(5) >= 0) receivingYards += playerData.get(i).get(5);
				if(playerData.get(i).get(6) >= 0) receivingTDs += playerData.get(i).get(6);
				if(playerData.get(i).get(7) >= 0) firstDownReceptions += playerData.get(i).get(7);
				if(playerData.get(i).get(8) >= 0) rushAttempts += playerData.get(i).get(8);
				if(playerData.get(i).get(9) >= 0) rushYards += playerData.get(i).get(9);
				if(playerData.get(i).get(10) >= 0) rushTDs += playerData.get(i).get(10);
				if(playerData.get(i).get(11) >= 0) firstDownRushes += playerData.get(i).get(11);
				if(playerData.get(i).get(12) >= 0) fumbles += playerData.get(i).get(12);//for any of these values check if -1, if is -1 (meaning empty) turn into 0
				if(playerData.get(i).get(13) >= 0) approximateValue += playerData.get(i).get(13);//don't forget to add av
			} else if(playerData.get(1).get(0) == year) age = playerData.get(1).get(1) - 1;
		}
		
		ret.add(age);
		ret.add((double) gamesPlayed);
		ret.add((double) targets);
		ret.add((double) receptions);
		ret.add((double) receivingYards);
		ret.add((double) receivingTDs);
		ret.add((double) firstDownReceptions);
		ret.add((double) rushAttempts);
		ret.add((double) rushYards);
		ret.add((double) rushTDs);
		ret.add((double) firstDownRushes);
		ret.add((double) fumbles);
		ret.add((double) approximateValue);
		
		return ret;
		
	}
	
	public static List<Double> getOLDataUpToGame(List<List<Double>> playerData){
		List<Double> ret = new ArrayList<Double>();
		ret.add(playerData.get(0).get(1)); // add the height of the player
		ret.add(playerData.get(0).get(2)); // add the weight of the player
		
		double age = -1;
		int gamesPlayed = 0;
		int approximateValue = 0;
		
		for(int i = 1; i < playerData.size(); i++) {
			if(playerData.get(i).get(0) < year) {
				if(playerData.get(i).get(0) == year - 1) age = playerData.get(i).get(1);
				if(playerData.get(i).get(2) >= 0) gamesPlayed += playerData.get(i).get(2); //index of games played
				if(playerData.get(i).get(3) >= 0) approximateValue += playerData.get(i).get(3);
			} else if (playerData.get(1).get(0) == year) age = playerData.get(1).get(1) - 1;
		}
		
		ret.add(age);
		ret.add((double) gamesPlayed);
		ret.add((double) approximateValue);
		
		return ret;
	}
	
	public static List<Double> getDefenderDataUpToGame(List<List<Double>> playerData){
		List<Double> ret = new ArrayList<Double>();
		ret.add(playerData.get(0).get(1)); // add the height of the player
		ret.add(playerData.get(0).get(2)); // add the weight of the player
		
		double age = -1;
		int gamesPlayed = 0;
		int interceptions = 0;
		int fumblesForced = 0;
		int fumblesRecovered = 0; //qb passer rating
		int fumblesRecoveredTD = 0;
		double sacks = 0;
		int combinedTackles = 0;
		int soloTackles = 0;
		int assistTackles = 0;
		int tacklesForLoss = 0;
		//int qbHits = 0; //recorded since 2006
		int safetiesScored = 0;
		int approximateValue = 0;
		
		for(int i = 1; i < playerData.size(); i++) {
			if(playerData.get(i).get(0) < year) {
				//the values below may be -1 if null, meaning that if a player got no TDs in a year for whatever reason
				//instead of adding 0 touchdowns it will add -1 touchdown
				if(playerData.get(i).get(0) == year - 1) age = playerData.get(i).get(1);
				if(playerData.get(i).get(2) >= 0) gamesPlayed += playerData.get(i).get(2); //index of games played
				if(playerData.get(i).get(3) >= 0) interceptions += playerData.get(i).get(3);
				if(playerData.get(i).get(4) >= 0) fumblesForced += playerData.get(i).get(4);
				if(playerData.get(i).get(5) >= 0) fumblesRecovered += playerData.get(i).get(5);
				if(playerData.get(i).get(6) >= 0) fumblesRecoveredTD += playerData.get(i).get(6);
				if(playerData.get(i).get(7) >= 0) sacks += playerData.get(i).get(7);
				if(playerData.get(i).get(8) >= 0) combinedTackles += playerData.get(i).get(8);
				if(playerData.get(i).get(9) >= 0) soloTackles += playerData.get(i).get(9);
				if(playerData.get(i).get(10) >= 0) assistTackles += playerData.get(i).get(10);
				if(playerData.get(i).get(11) >= 0) tacklesForLoss += playerData.get(i).get(11);
				if(playerData.get(i).get(12) >= 0) safetiesScored += playerData.get(i).get(12);
				if(playerData.get(i).get(13) >= 0) approximateValue += playerData.get(i).get(13);
			} else if (playerData.get(1).get(0) == year) age = playerData.get(1).get(1) - 1;
		}
		
		ret.add(age);
		ret.add((double) gamesPlayed);
		ret.add((double) interceptions);
		ret.add((double) fumblesForced);
		ret.add((double) fumblesRecovered);
		ret.add((double) fumblesRecoveredTD);
		ret.add((double) sacks);
		ret.add((double) combinedTackles);
		ret.add((double) soloTackles);
		ret.add((double) assistTackles);
		ret.add((double) tacklesForLoss);
		ret.add((double) safetiesScored);
		ret.add((double) approximateValue);
		
		return ret;
	}
	
	public static List<Double> getKickerDataUpToGame(List<List<Double>> playerData){
		List<Double> ret = new ArrayList<Double>();
		ret.add(playerData.get(0).get(1)); // add the height of the player
		ret.add(playerData.get(0).get(2)); // add the weight of the player
		
		double age = -1;
		int gamesPlayed = 0;
		int fgAttempted = 0;
		int fgMade = 0;
		int xpAttempted= 0; //qb passer rating
		int xpMade = 0;
		int ko = 0;
		int koYards = 0;
		int koTB = 0;
		int approximateValue = 0;
		
		for(int i = 1; i < playerData.size(); i++) {
			if(playerData.get(i).get(0) < year) {
				//the values below may be -1 if null, meaning that if a player got no TDs in a year for whatever reason
				//instead of adding 0 touchdowns it will add -1 touchdown
				if(playerData.get(i).get(0) == year - 1) age = playerData.get(i).get(1);
				if(playerData.get(i).get(2) >= 0) gamesPlayed += playerData.get(i).get(2); //index of games played
				if(playerData.get(i).get(3) >= 0) fgAttempted += playerData.get(i).get(3);
				if(playerData.get(i).get(4) >= 0) fgMade += playerData.get(i).get(4);
				if(playerData.get(i).get(5) >= 0) xpAttempted += playerData.get(i).get(5);
				if(playerData.get(i).get(6) >= 0) xpMade += playerData.get(i).get(6);
				if(playerData.get(i).get(7) >= 0) ko += playerData.get(i).get(7);
				if(playerData.get(i).get(8) >= 0) koYards += playerData.get(i).get(8);
				if(playerData.get(i).get(9) >= 0) koTB += playerData.get(i).get(9);
				if(playerData.get(i).get(10) >= 0) approximateValue += playerData.get(i).get(10);
			} else if (playerData.get(1).get(0) == year) age = playerData.get(1).get(1) - 1;
		}
		
		ret.add(age);
		ret.add((double) gamesPlayed);
		ret.add((double) fgAttempted);
		ret.add((double) fgMade);
		ret.add((double) xpAttempted);
		ret.add((double) xpMade);
		ret.add((double) ko);
		ret.add((double) koYards);
		ret.add((double) koTB);
		ret.add((double) approximateValue);
		
		return ret;
	}
	
	public static List<Double> getPunterDataUpToGame(List<List<Double>> playerData){
		List<Double> ret = new ArrayList<Double>();
		ret.add(playerData.get(0).get(1)); // add the height of the player
		ret.add(playerData.get(0).get(2)); // add the weight of the player
		
		double age = -1;
		int gamesPlayed = 0;
		int punts = 0;
		int puntYards = 0;
		int puntTouchbacks = 0;
		int puntsWithin20 = 0;
		int approximateValue = 0;
		
		for(int i = 1; i < playerData.size(); i++) {
			if(playerData.get(i).get(0) < year) {
				//the values below may be -1 if null, meaning that if a player got no TDs in a year for whatever reason
				//instead of adding 0 touchdowns it will add -1 touchdown
				if(playerData.get(i).get(0) == year - 1) age = playerData.get(i).get(1);
				if(playerData.get(i).get(2) >= 0) gamesPlayed += playerData.get(i).get(2); //index of games played
				if(playerData.get(i).get(3) >= 0) punts += playerData.get(i).get(3);
				if(playerData.get(i).get(4) >= 0) puntYards += playerData.get(i).get(4);
				if(playerData.get(i).get(5) >= 0) puntTouchbacks += playerData.get(i).get(5);
				if(playerData.get(i).get(6) >= 0) puntsWithin20 += playerData.get(i).get(6);
				if(playerData.get(i).get(7) >= 0) approximateValue += playerData.get(i).get(7);
			} else if (playerData.get(1).get(0) == year) age = playerData.get(1).get(1) - 1;
		}
		
		ret.add(age);
		ret.add((double) gamesPlayed);
		ret.add((double) punts);
		ret.add((double) puntYards);
		ret.add((double) puntTouchbacks);
		ret.add((double) puntsWithin20);
		ret.add((double) approximateValue);
		
		return ret;
	}
	
	// list of yearly data, each year is a list of data with the columns/each
	// element representing a data category
	public static List<List<Double>> getPlayerData(String playerLink) throws InterruptedException {
		System.out.println(playerLink);
		String url = "https://www.pro-football-reference.com" + playerLink;
		try {
			Document document = Jsoup.connect(url).get();
			String positionText = document.selectFirst("p:has(strong:contains(Position))").text();
			positionText = positionText.replaceAll("Position: ", "");
			positionText = positionText.replaceAll(" Throws: Right", "");
			positionText = positionText.replaceAll(" Throws: Left", "");
			
			if (getPositionEncoded(positionText) == 1) return getQBData(document);
			else if (getPositionEncoded(positionText) == 2)	return getRunRecData(document);
			else if (getPositionEncoded(positionText) == 3)	return getOLData(document);
			else if (getPositionEncoded(positionText) == 4)	return getDefenderData(document);
			else if (getPositionEncoded(positionText) == 5)	return getKickerData(document);
			else if (getPositionEncoded(positionText) == 6)	return getPunterData(document);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public static List<List<Double>> getQBData(Document document) {
		Element passingTable = document.getElementById("all_passing");
		Elements tbody = passingTable.select("tbody");
		Elements trs = tbody.select("tr");
		trs = trs.removeClass("partial_table");

		Element runRecTable = document.getElementById("all_rushing_and_receiving");
		if (runRecTable == null) runRecTable = document.getElementById("all_receiving_and_rushing");
		Elements tbodyRunRec = runRecTable.select("tbody");
		Elements trsRunRec = tbodyRunRec.select("tr");
		trsRunRec = trsRunRec.removeClass("partial_table");
		
		List<List<Double>> qbData = new ArrayList<List<Double>>();
		for (Element tr : trs) {
			String ageText = Helpers.removeNonNumericCharacters(tr.selectFirst("[data-stat=age]").text());
			if (ageText != null && !ageText.equals("")) {
				ArrayList<Double> yearlyData = new ArrayList<Double>();
				addStat("[data-stat=year_id]", yearlyData, tr, false);
				addStat("[data-stat=age]", yearlyData, tr, false);
				addStat("[data-stat=g]", yearlyData, tr, false);

				Element startingWinRate = tr.selectFirst("[data-stat=qb_rec]");
				if (startingWinRate.attr("csk").equals(null) || startingWinRate.attr("csk").equals("")
						|| startingWinRate.text().equals(null) || startingWinRate.text().equals(""))
					yearlyData.add(-1.0);
				else {
					double startingWinRateAsNum = Double.parseDouble(startingWinRate.attr("csk"));
					yearlyData.add(startingWinRateAsNum);
				}

				addStat("[data-stat=pass_cmp]", yearlyData, tr, false);
				addStat("[data-stat=pass_att]", yearlyData, tr, false);
				addStat("[data-stat=pass_yds]", yearlyData, tr, false);
				addStat("[data-stat=pass_td]", yearlyData, tr, false);
				addStat("[data-stat=pass_int]", yearlyData, tr, false);
				addStat("[data-stat=pass_first_down]", yearlyData, tr, false);
				addStat("[data-stat=pass_rating]", yearlyData, tr, true);
				addStat("[data-stat=pass_sacked]", yearlyData, tr, false);
				addStat("[data-stat=pass_sacked_yds]", yearlyData, tr, false);
				addStat("[data-stat=gwd]", yearlyData, tr, false);
				addStat("[data-stat=av]", yearlyData, tr, false);

				qbData.add(yearlyData);
			}
		}

		int i = 0;
		for (Element tr : trsRunRec) {
			if (i > qbData.size() - 1) break;
			List<Double> dataList = qbData.get(i);
			
			String ageText = Helpers.removeNonNumericCharacters(tr.selectFirst("[data-stat=age]").text());
			if (ageText != null && !ageText.equals("")) {
				dataList.add(Double.parseDouble(tr.selectFirst("[data-stat=rush_att]").text()));
				dataList.add(Double.parseDouble(tr.selectFirst("[data-stat=rush_yds]").text()));
				dataList.add(Double.parseDouble(tr.selectFirst("[data-stat=rush_td]").text()));
				dataList.add(Double.parseDouble(tr.selectFirst("[data-stat=fumbles]").text()));
				i++;
			}
		}
		
		//make sure each list in the list of lists if correct size
		for(int x = 1; x < qbData.size(); x++) {
			while(qbData.get(x).size() < 19) {
				qbData.get(x).add(-1.0);
			}
		}

		qbData.add(0, Helpers.doubleArrayToList(getPlayerPhysical(document)));
		endTime = System.nanoTime();
		return qbData;
	}

	public static List<List<Double>> getRunRecData(Document document) {
		Element runRecTable = document.getElementById("all_receiving_and_rushing"); 
		if (runRecTable == null) runRecTable = document.getElementById("all_rushing_and_receiving");
		if(runRecTable == null) runRecTable = document.getElementById("all_defense");
		Elements tbodyRunRec = runRecTable.select("tbody");
		Elements trsRunRec = tbodyRunRec.select("tr");

		List<List<Double>> runRecData = new ArrayList<List<Double>>();
		for (Element tr : trsRunRec) {
			String ageText = Helpers.removeNonNumericCharacters(tr.selectFirst("[data-stat=age]").text());
			if (ageText != null && !ageText.equals("")) {
				ArrayList<Double> yearlyData = new ArrayList<Double>();
				addStat("[data-stat=year_id]", yearlyData, tr, false);
				addStat("[data-stat=age]", yearlyData, tr, false);
				addStat("[data-stat=g]", yearlyData, tr, false);
				addStat("[data-stat=targets]", yearlyData, tr, false);
				addStat("[data-stat=rec]", yearlyData, tr, false);
				addStat("[data-stat=rec_yds]", yearlyData, tr, false);
				addStat("[data-stat=rec_td]", yearlyData, tr, false);
				addStat("[data-stat=rec_first_down]", yearlyData, tr, false);
				addStat("[data-stat=rush_att]", yearlyData, tr, false);
				addStat("[data-stat=rush_yds]", yearlyData, tr, false);
				addStat("[data-stat=rush_td]", yearlyData, tr, false);
				addStat("[data-stat=rush_first_down]", yearlyData, tr, false);
				addStat("[data-stat=fumbles]", yearlyData, tr, false);
				addStat("[data-stat=av]", yearlyData, tr, false);
				runRecData.add(yearlyData);
			}
		}
		runRecData.add(0, Helpers.doubleArrayToList(getPlayerPhysical(document)));
		endTime = System.nanoTime();
		return runRecData;
	}

	public static List<List<Double>> getOLData(Document document) {
		List<List<Double>> OLData = new ArrayList<List<Double>>();

		Element OLTableContainingAV = document.getElementById("all_defense");
		if (OLTableContainingAV == null)
			OLTableContainingAV = document.getElementById("all_games_played");
		if (OLTableContainingAV == null)
			OLTableContainingAV = document.getElementById("all_returns");
		if(OLTableContainingAV == null)
			OLTableContainingAV = document.getElementById("all_receiving_and_rushing");
		if(OLTableContainingAV == null)
			OLTableContainingAV = document.getElementById("all_rushing_and_receiving");
		
		Elements tbodyOLAV = OLTableContainingAV.select("tbody");
		Elements trsOLAVIter = tbodyOLAV.select("tr");
//		if (trsOLAVIter.get(0).selectFirst("[data-stat=av]") == null) {
//			OLTableContainingAV = document.getElementById("all_receiving_and_rushing");
//			tbodyOLAV = OLTableContainingAV.select("tbody");
//			trsOLAVIter = tbodyOLAV.select("tr");
//		}
	
		for (Element tr : trsOLAVIter) {
			String ageText = Helpers.removeNonNumericCharacters(tr.selectFirst("[data-stat=age]").text());
			if (ageText != null && !ageText.equals("")) {
				ArrayList<Double> yearlyData = new ArrayList<Double>();
				addStat("[data-stat=year_id]", yearlyData, tr, false);
				addStat("[data-stat=age]", yearlyData, tr, false);
				addStat("[data-stat=g]", yearlyData, tr, false);
				addStat("[data-stat=av]", yearlyData, tr, false);
				OLData.add(yearlyData);
			}
		}

		OLData.add(0, Helpers.doubleArrayToList(getPlayerPhysical(document)));
		endTime = System.nanoTime();
		return OLData;
	}

	public static List<List<Double>> getDefenderData(Document document) {
		Element defenderTable = document.getElementById("all_defense");
		Elements tbodyDefense = defenderTable.select("tbody");
		Elements trsDefense = tbodyDefense.select("tr");

		List<List<Double>> defenderData = new ArrayList<List<Double>>();
		for (Element tr : trsDefense) {
			String ageText = Helpers.removeNonNumericCharacters(tr.selectFirst("[data-stat=age]").text());
			if (ageText != null && !ageText.equals("")) {
				ArrayList<Double> yearlyData = new ArrayList<Double>();
				addStat("[data-stat=year_id]", yearlyData, tr, false);
				addStat("[data-stat=age]", yearlyData, tr, false);
				addStat("[data-stat=g]", yearlyData, tr, false);
				addStat("[data-stat=def_int]", yearlyData, tr, false);
				addStat("[data-stat=pass_defended]", yearlyData, tr, false);
				addStat("[data-stat=fumbles_forced]", yearlyData, tr, false);
				addStat("[data-stat=fumbles_rec]", yearlyData, tr, false);
				addStat("[data-stat=fumbles_rec_td]", yearlyData, tr, false);
				addStat("[data-stat=sacks]", yearlyData, tr, true);
				addStat("[data-stat=tackles_combined]", yearlyData, tr, false);
				addStat("[data-stat=tackles_solo]", yearlyData, tr, false);
				addStat("[data-stat=tackles_assists]", yearlyData, tr, false);
				addStat("[data-stat=tackles_loss]", yearlyData, tr, false);
				// addStat("[data-stat=qb_hits]", yearlyData, tr, false); //recorded since 2006
				addStat("[data-stat=safety_md]", yearlyData, tr, false);
				addStat("[data-stat=av]", yearlyData, tr, false);
				defenderData.add(yearlyData);
			}
		}

		defenderData.add(0, Helpers.doubleArrayToList(getPlayerPhysical(document)));
		endTime = System.nanoTime();
		return defenderData;
	}

	public static List<List<Double>> getKickerData(Document document) {
		Element kickingTable = document.getElementById("all_kicking");
		Elements tbodyKicking = kickingTable.select("tbody");
		Elements trsKicking = tbodyKicking.select("tr");

		List<List<Double>> kickerData = new ArrayList<List<Double>>();
		for (Element tr : trsKicking) {
			String ageText = Helpers.removeNonNumericCharacters(tr.selectFirst("[data-stat=age]").text());
			if (ageText != null && !ageText.equals("")) {
				ArrayList<Double> yearlyData = new ArrayList<Double>();
				addStat("[data-stat=year_id]", yearlyData, tr, false);
				addStat("[data-stat=age]", yearlyData, tr, false);
				addStat("[data-stat=g]", yearlyData, tr, false);
				addStat("[data-stat=fga]", yearlyData, tr, false);
				addStat("[data-stat=fgm]", yearlyData, tr, false);
				addStat("[data-stat=xpa]", yearlyData, tr, false);
				addStat("[data-stat=xpm]", yearlyData, tr, false);
				addStat("[data-stat=kickoff]", yearlyData, tr, false);
				addStat("[data-stat=kickoff_yds]", yearlyData, tr, false);
				addStat("[data-stat=kickoff_tb]", yearlyData, tr, false);
				addStat("[data-stat=av]", yearlyData, tr, false);
				kickerData.add(yearlyData);
			}
		}

		kickerData.add(0, Helpers.doubleArrayToList(getPlayerPhysical(document)));
		endTime = System.nanoTime();
		return kickerData;
	}

	public static List<List<Double>> getPunterData(Document document) {
		Element puntingTable = document.getElementById("all_punting");
		Elements tbodyPunting = puntingTable.select("tbody");
		Elements trsPunting = tbodyPunting.select("tr");

		List<List<Double>> punterData = new ArrayList<List<Double>>();
		for (Element tr : trsPunting) {
			ArrayList<Double> yearlyData = new ArrayList<Double>();
			String ageText = Helpers.removeNonNumericCharacters(tr.selectFirst("[data-stat=age]").text());
			if (ageText != null && !ageText.equals("")) {
				addStat("[data-stat=year_id]", yearlyData, tr, false);
				addStat("[data-stat=age]", yearlyData, tr, false);
				addStat("[data-stat=g]", yearlyData, tr, false);
				addStat("[data-stat=punt]", yearlyData, tr, false);
				addStat("[data-stat=punt_yds]", yearlyData, tr, false);
				addStat("[data-stat=punt_tb]", yearlyData, tr, false);
				addStat("[data-stat=punt_in_20]", yearlyData, tr, false);
				addStat("[data-stat=av]", yearlyData, tr, false);
				punterData.add(yearlyData);
			}
		}

		punterData.add(0, Helpers.doubleArrayToList(getPlayerPhysical(document)));
		endTime = System.nanoTime();
		return punterData;
	}

	public static List<List<Double>> getReturnerData(String playerLink) {
		String url = "https://www.pro-football-reference.com" + playerLink;
		List<List<Double>> returnerData = new ArrayList<List<Double>>();

		try {
			Document document = Jsoup.connect(url).get();

			Element returningTable = document.getElementById("all_returns");
			Elements tbodyReturning = returningTable.select("tbody");
			Elements trsReturning = tbodyReturning.select("tr");

			for (Element tr : trsReturning) {
				String ageText = Helpers.removeNonNumericCharacters(tr.selectFirst("[data-stat=age]").text());
				if (ageText != null && !ageText.equals("")) {
					ArrayList<Double> yearlyData = new ArrayList<Double>();
					addStat("[data-stat=year_id]", yearlyData, tr, false);
					addStat("[data-stat=age]", yearlyData, tr, false);
					addStat("[data-stat=g]", yearlyData, tr, false);
					addStat("[data-stat=kick_ret]", yearlyData, tr, false);
					addStat("[data-stat=kick_ret_yds]", yearlyData, tr, false);
					addStat("[data-stat=kick_ret_td]", yearlyData, tr, false);
					addStat("[data-stat=punt_ret]", yearlyData, tr, false);
					addStat("[data-stat=punt_ret_yds]", yearlyData, tr, false);
					addStat("[data-stat=punt_ret_td]", yearlyData, tr, false);
					returnerData.add(yearlyData);
				}
			}

			returnerData.add(0, Helpers.doubleArrayToList(getPlayerPhysical(document)));
			endTime = System.nanoTime();
			return returnerData;

		} catch (Exception e) {
			e.printStackTrace();
		}
		endTime = System.nanoTime();
		return returnerData;
	}

	// 11 offense, 11 defense, 1 returner, 1 kicker, 1 punter
	public static String[] getStarterOffenseAndDefenseLinks(Document doc, String homeOrVis) {
		String[] roster = new String[22];
		String homeStartersHTML = doc.selectFirst("div#all_" + homeOrVis + "_starters").html();
		// extract body of table as html
		String innerStartersTableHTML = Helpers.extractSubstring(homeStartersHTML, "</thead>", "</table>");

		// gets each html row as an element in an array
		String[] starterRowsHTML = innerStartersTableHTML.split("\n");

		List<String> runnersReceiversAway = new ArrayList<String>();
		List<String> offensiveLinemenAway = new ArrayList<String>();
		List<String> defenseAway = new ArrayList<String>();

		List<String> runnersReceiversPositionAcronyms = new ArrayList<String>();
		for (String runnerReceiverPositionAcronym : "Z X RB HB TB FB LH RH BB B WB WR RWR LWR FL TE SE".split(" "))
			runnersReceiversPositionAcronyms.add(runnerReceiverPositionAcronym);

		List<String> offensiveLinemenPositionAcronyms = new ArrayList<String>();
		for (String offensiveLinemanPositionAcronym : "OL LT LOT T LG G C RG RT ROT".split(" "))
			offensiveLinemenPositionAcronyms.add(offensiveLinemanPositionAcronym);

		List<String> defensePositionAcronyms = new ArrayList<String>();
		for (String defensePositionAcronym : "RE LE DL LDE DE LDT DT NT MG DG RDT RDE LOLB RUSH OLB LLB LILB WILL ILB SLB MLB MIKE WLB RILB RLB ROLB SAM LB LCB CB RCB SS FS LDH RDH S RS DB"
				.split(" "))
			defensePositionAcronyms.add(defensePositionAcronym);

		boolean skipFirstRow = true;
		for (String starterRowHTML : starterRowsHTML) {
			if (skipFirstRow) {
				skipFirstRow = false;
				continue;
			}
			String curStarterPosition = Helpers.extractSubstring(starterRowHTML, "\" data-stat=\"pos\" >", "</td></tr>");
			// get the link for the player in the current traversed upon row
			String starterLink = Helpers.extractSubstring(starterRowHTML, " data-stat=\"player\" ><a href=\"", "\">");
			boolean keepLooping = true;
			while (keepLooping) {
				if (curStarterPosition.equals("QB"))
					roster[0] = starterLink;
				else if (Helpers.anyStringEquals(runnersReceiversPositionAcronyms, curStarterPosition))
					runnersReceiversAway.add(starterLink);
				else if (Helpers.anyStringEquals(offensiveLinemenPositionAcronyms, curStarterPosition))
					offensiveLinemenAway.add(starterLink);
				else if (Helpers.anyStringEquals(defensePositionAcronyms, curStarterPosition))
					defenseAway.add(starterLink);
				else {
					try {
						String playerOfUnknownPositionPage = "https://www.pro-football-reference.com" + starterLink;
						Thread.sleep(2000);
						Document document = Jsoup.connect(playerOfUnknownPositionPage).get();
						curStarterPosition = findPosition(document);
						// System.out.println(curStarterPosition);
						continue;
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				keepLooping = false;
			}

		}

		int index = 1; // Start index for adding elements in the roster array

		// Adding elements from runnersReceiversAway to the roster array
		for (int i = 0; i < runnersReceiversAway.size(); i++) {
			roster[index] = runnersReceiversAway.get(i);
			index++;
		}

		// Adding elements from offensiveLinemenAway to the roster array
		for (int i = 0; i < offensiveLinemenAway.size(); i++) {
			roster[index] = offensiveLinemenAway.get(i);
			index++;
		}

		// Adding elements from defenseAway to the roster array
		for (int i = 0; i < defenseAway.size(); i++) {
			roster[index] = defenseAway.get(i);
			index++;
		}

		// System.out.println(runnersReceiversAway);
		// System.out.println(offensiveLinemenAway);
		// System.out.println(defenseAway);
		return roster;
	}

	public static List<String> getReturners(Document doc) {
		String returnerTable = doc.selectFirst("div#all_returns").html();
		String cur = "_";
		List<String> returnersList = new ArrayList<String>();
		String curTeam;
		String prevTeam = null;
		while (cur != null) {
			cur = Helpers.extractStringBetween(returnerTable, " data-stat=\"player\" ><a href=\"", "</td><td");
			if (cur == null)
				break;

			curTeam = Helpers.getStringToRight(cur, "\"team\" >");
			if (!curTeam.equals(prevTeam)) {
				returnersList.add(Helpers.removeStringToRight(cur, "\">"));
			}

			returnerTable = Helpers.removeSubstring(returnerTable, cur);
			prevTeam = curTeam;

		}
		// System.out.println(returnersList);
		return returnersList;
	}

	// true if double false if int
	public static void addStat(String stat, List<Double> stats, Element e, boolean trueIfParseByDouble) {
		Element eStat = e.selectFirst(stat);
		double parsedStat;

		if (eStat == null) // if its element doesnt exist, make it -1 as a flag
			parsedStat = -1;
		else if (eStat.text().equals("") || eStat.text().equals(null)) // if its empty make it equal 0
			parsedStat = 0;
		else if (trueIfParseByDouble)
			parsedStat = Double.parseDouble(Helpers.removeNonNumericCharacters(eStat.text()));
		else
			parsedStat = Integer.parseInt(Helpers.removeNonNumericCharacters(eStat.text()));
		stats.add(parsedStat);
	}

	public static int getPositionEncoded(String position) {
		position = position.trim();
		if(position.contains("-")) position = position.split("-")[0];
		//System.out.println("position var in getPositionEncoded func: " + position);
		
		List<String> runnersReceiversPositionAcronyms = new ArrayList<String>();
		for (String runnerReceiverPositionAcronym : "Z X RB HB TB FB LH RH BB B WB WR RWR LWR FL TE SE".split(" "))
			runnersReceiversPositionAcronyms.add(runnerReceiverPositionAcronym);

		List<String> offensiveLinemenPositionAcronyms = new ArrayList<String>();
		for (String offensiveLinemanPositionAcronym : "OL LT LOT T LG G C RG RT ROT".split(" "))
			offensiveLinemenPositionAcronyms.add(offensiveLinemanPositionAcronym);

		List<String> defensePositionAcronyms = new ArrayList<String>();
		for (String defensePositionAcronym : "RE LE DL LDE DE LDT DT NT MG DG RDT RDE LOLB RUSH OLB LLB LILB WILL ILB SLB MLB MIKE WLB RILB RLB ROLB SAM LB LCB CB RCB SS FS LDH RDH S RS DB"
				.split(" "))
			defensePositionAcronyms.add(defensePositionAcronym);

		if (position.equals("QB"))
			return 1;
		else if (Helpers.anyStringEquals(runnersReceiversPositionAcronyms, position))
			return 2;
		else if (Helpers.anyStringEquals(offensiveLinemenPositionAcronyms, position))
			return 3;
		else if (Helpers.anyStringEquals(defensePositionAcronyms, position))
			return 4;
		else if (position.equals("K"))
			return 5; // 5 is reserved for returners, but no position is returners
		else if (position.equals("P"))
			return 6;
		// returner isnt a real position will be one of the positions above,
		return -1;
	}

	public static float getImperialHeightAsDecimal(String height) {
		String[] feetInches = height.split("-");
		if (feetInches.length > 2)
			return -1f;
		return Float.parseFloat(feetInches[0]) + (Float.parseFloat(feetInches[1]) / 12f);
	}

	public static double[] getPlayerPhysical(Document doc) {
		String positionText = doc.selectFirst("p:has(strong:contains(Position))").text();
		positionText = positionText.replaceAll("Position: ", "");
		positionText = positionText.replaceAll(" Throws: Right", "");
		positionText = positionText.replaceAll(" Throws: Left", "");
		String heightText = doc.selectFirst("p:has(span:contains(-)):not(:has(strong))").select("span").get(0).text(); 
		// what if a player has a name with a dash? /players/P/PeopDo00.htm
		String weightText = doc.selectFirst("p:has(span:contains(lb))").select("span").get(1).text();
		return new double[] { getPositionEncoded(positionText), getImperialHeightAsDecimal(heightText),
				Integer.parseInt(weightText.replaceAll("lb", "")) };
	}

	public static String findPosition(Document doc) {
		Elements paragraphs = doc.select("p:has(strong:contains(Position))");
		// Check if the element was found
		if (!paragraphs.isEmpty()) {
			Element paragraph = paragraphs.first();
			return paragraph.text().replace("Position: ", "");
		} else
			return null;
	}

}