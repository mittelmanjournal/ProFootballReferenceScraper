package main.NFLscrape;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataBuilder2 {
	//public static Document doc;
	static Document staticDoc;
	
	public static int year;
	// week 18 starts 2021
	public static int week;

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
		long startTime = System.nanoTime();

		for(year = 2002; year < 2021; year++) {
			for(week = 1; week <= 17; week++) {
				String url = "https://www.pro-football-reference.com/years/" + year + "/week_" + week + ".htm";
				//System.out.println(url);
				for(String game : getGameLinksGivenWeekAndYear(url)) {
					System.out.println(getSingleGameInfo(game));
				}
				System.out.println();
			}
		}
		long endTime = System.nanoTime();
        long elapsedTime = endTime - startTime;
        System.out.println("Program took " + elapsedTime + " nanoseconds to complete.");


	}
	
	//this doesnt use the same doc as getGameScore and getWLforGame
	public static List<String> getGameLinksGivenWeekAndYear(String url) {
		List<String> games = new ArrayList<String>();

		try {
			TimeUnit.SECONDS.sleep(2); 
			staticDoc = Jsoup.connect(url).get();
			
			// Select the game summary div elements
			Elements gameSummaries = staticDoc.select("div.game_summary");//good

			for (Element gameSummary : gameSummaries) {
				
				// Find the game link element within each game summary
				Element gameLinkElement = gameSummary.selectFirst("td.gamelink a");

				// Check if the game link element is present
				if (gameLinkElement != null) { 
					String gameLink = gameLinkElement.attr("href");
					//System.out.println("https://www.pro-football-reference.com" + gameLink);
					games.add("https://www.pro-football-reference.com" + gameLink);
				}
			}

		} catch (Exception IOE) {
			IOE.printStackTrace();
		}
		return games;

	}
	
	public static String getSingleGameInfo(String url) {
		String out = "";
		try {
			TimeUnit.SECONDS.sleep(2); 
			Document doc = Jsoup.connect(url).get();
			
			out += getDateAndTime(doc) + " ";
			
			List<String> externalInfo = externalGameInfo(doc);
			out += "Roof Status: " + externalInfo.get(0) + " Field Texture: " + externalInfo.get(1) + " Over/Under: " + externalInfo.get(2) + " ";
			
			List<Integer> gameScore = getGameScore(doc); //2 value list 0 = away team score, 1 = home team score
			out += "Away Game Score: " + gameScore.get(0) + " - Home Game Score: " + gameScore.get(1) + " "; // stringify
			
			List<String> coachLinks = getCoachLinks(doc);
			out += "Away Coach: " + coachLinks.get(0) + " Home Coach: " + coachLinks.get(1);
			
			int[] WLT = getWLTforGame(doc, gameScore.get(0), gameScore.get(1)); //get wlt for both teams in game given the awayScore and homeScore
			out += " Away Team Record: " + WLT[0] + "-" + WLT[1] + "-" + WLT[2] + " Home team Record: " + WLT[3] + "-" + WLT[4] + "-" + WLT[5] + " :: " + url;

		}  catch (IOException ioe) {
			ioe.printStackTrace(); 
		} catch (InterruptedException ie) {
			ie.printStackTrace();
		}
		return out;
		
	}
	
	//getGameScore uses the same Document as getWLforGame, just pass a single document between them to not have to delay
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
	
	public static String getDateAndTime(Document doc) {
        // Select div elements with class 'scorebox_meta'
		Elements elements = doc.select(".scorebox_meta div");
		String date = elements.get(0).text();
		String time = elements.get(1).text();
		time = time.replace("Start Time: ", "");
		String ret = date + ", " + time;
		return ret;
	}
	
	
	//returns String list 0th element is away coach, 1st element is home coach
	public static List<String> getCoachLinks(Document doc) {
		// Select all elements with class "datapoint" and extract coach links
		Elements coachElements = doc.select("div.datapoint strong:contains(Coach) + a");
		List<String> ret = new ArrayList<String>();
		for (Element coachElement : coachElements) {
			String coachName = coachElement.text();
			String coachLink = coachElement.attr("href");
			//System.out.println("Coach Name: " + coachName);
			//System.out.println("Coach Link: " + coachLink);
			ret.add(coachLink);
		}		
		return ret;
	}
	
	public static List<String> getReturners(Document doc) {
		String returnerTable = doc.selectFirst("div#all_returns").html();
        String cur = "_";
        List<String> returnersList = new ArrayList<String>();
        String curTeam;
        String prevTeam = null;
        while(cur != null) {
        	cur = extractStringBetween(returnerTable, " data-stat=\"player\" ><a href=\"", "</td><td");
        	if(cur == null) break;
        	
        	curTeam = getStringToRight(cur, "\"team\" >");
        	if(!curTeam.equals(prevTeam)) {
        		returnersList.add(removeStringToRight(cur, "\">"));
        	}
        	
        	returnerTable = removeSubstring(returnerTable, cur);
        	prevTeam = curTeam;
	
        }      
        //System.out.println(returnersList);
        return returnersList;
	}

	public static List<String> getKickersAndPunters(Document doc){
		String returnerTable = doc.selectFirst("div#all_kicking").html();
		return extractStringsBetween(returnerTable, "data-stat=\"player\" ><a href=\"", "\">");
		//System.out.println(legList);
//		String awayK = legList.get(0);
//		String awayP = legList.get(1);
//		String homeK = legList.get(2);
//		String homeP = legList.get(3);
	}
	
	//returns list index values: 0 = roof (closed/open), 1 = field (grass/turf), 2 = over under line
	public static List<String> externalGameInfo(Document doc){
		List<String> ret = new ArrayList<String>();
		Element gameInfoDiv = doc.selectFirst("div#all_game_info");
		String s = gameInfoDiv.outerHtml();

		String roof = extractStringBetween(s,
				"<tr ><th scope=\"row\" class=\"center \" data-stat=\"info\" >Roof</th><td class=\"center \" data-stat=\"stat\" >",
				"</td></tr>");
		String surface = extractStringBetween(s,
				"<tr ><th scope=\"row\" class=\"center \" data-stat=\"info\" >Surface</th><td class=\"center \" data-stat=\"stat\" >",
				"</td></tr>");
		String overUnder = extractStringBetween(s,
				"<tr ><th scope=\"row\" class=\"center \" data-stat=\"info\" >Over/Under</th><td class=\"center \" data-stat=\"stat\" >",
				" <b>");
		
		ret.add(roof);
		ret.add(surface);
		ret.add(overUnder);
		
		return ret;
	}
	
	//helpers
	public static List<String> extractStringsBetween(String original, String beforeString, String afterString) {
        List<String> extractedStrings = new ArrayList<String>();
        
        String pattern = Pattern.quote(beforeString) + "(.*?)" + Pattern.quote(afterString);
        Pattern regexPattern = Pattern.compile(pattern);
        Matcher matcher = regexPattern.matcher(original);
        
        while (matcher.find()) {
            extractedStrings.add(matcher.group(1));
        }
        
        return extractedStrings;
    }
	public static String extractStringBetween(String original, String beforeString, String afterString) {
		int startIndex = original.indexOf(beforeString);
		int endIndex = original.indexOf(afterString, startIndex + beforeString.length());

		if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
			return original.substring(startIndex + beforeString.length(), endIndex);
		}

		return null;
	}
	public static String removeStringToRight(String originalString, String secondString) {
        int index = originalString.indexOf(secondString);
        if (index != -1) {
            // Remove everything to the right of and including the second string
            return originalString.substring(0, index).trim();
        } else {
            // Second string not found in original string
            return "Second string not found in the original string.";
        }
    }
	public static String getStringToRight(String originalString, String parameterString) {
        int index = originalString.indexOf(parameterString);
        if (index != -1) {
            // Add length of parameterString to get the substring to the right of parameterString
            return originalString.substring(index + parameterString.length()).trim();
        } else {
            // Parameter string not found in original string
            return "Parameter string not found in the original string.";
        }
    }
	public static String removeSubstring(String originalString, String substring) {
		if(substring == null) {
			return null;
		}
        int index = originalString.indexOf(substring);
        if (index != -1) {
            // Found the substring, remove everything before and including it
            return originalString.substring(index + substring.length()).trim();
        } else {
            // Substring not found, return the original string
            return originalString;
        }
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
