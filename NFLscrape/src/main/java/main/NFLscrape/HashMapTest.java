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

public class HashMapTest {	
	
	
	public static void main(String[] args) throws IOException {
		String url = "https://www.pro-football-reference.com/boxscores/202209180cle.htm";
		Document document = Jsoup.connect(url).get();
		
		String[] awayRoster = new String[25];
		String[] homeRoster = new String[25];
		
		int i = 0;
		for (String s : getStarterOffenseAndDefenseLinks(document, "vis")) {
			awayRoster[i] = s;
			i++;
		}
		
		List<String> returners = getReturners(document);
		List<String> kickersAndPunters = getKickersAndPunters(document);		
		
		awayRoster[i] = returners.get(0);
		
		awayRoster[i+1] = kickersAndPunters.get(0);
		awayRoster[i+2] = kickersAndPunters.get(1);
		
		i = 0;
		for (String s : getStarterOffenseAndDefenseLinks(document, "home")) {
			homeRoster[i] = s;
			i++;
		}
		
		homeRoster[i] = returners.get(1);
		
		homeRoster[i+1] = kickersAndPunters.get(2);
		homeRoster[i+2] = kickersAndPunters.get(3);
				
		System.out.println(arrayToString(awayRoster));
		System.out.println(arrayToString(homeRoster));
		
		
		HashMap<String, List<List<Double>>> playerLinkToData = new HashMap<String, List<List<Double>>>();
		//in: list of player links and a hashmap to manipulate | out: update the existing hashmap by changing (don't take up more memory) with any new links and map them to the data on their page
		//void
	}
	
	public static void updateLinkToDataMap(HashMap hm, String[] playerLinks){
		//because returners isn't an actual position https://www.pro-football-reference.com/players/H/HestDe99.htm, 
		//you must track the index so that when you get to that player, 
		for(String playerLink : playerLinks) {
			if(hm.containsKey(playerLink) || playerLink.equals(null) || playerLink.equals("")) continue; //this line is most likely where you will be updating weekly data
			hm.put(playerLink, getPlayerData(playerLink));
		}
		
	
	}
	
	//list of yearly data, each year is a list of data with the columns/each element representing a data category
	public static List<List<Integer>> getPlayerData(String playerLink){
		String url = "https://www.pro-football-reference.com" + playerLink;
		try {
			Document document = Jsoup.connect(url).get();
			
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	// 11 offense, 11 defense, 1 returner, 1 kicker, 1 punter
	public static String[] getStarterOffenseAndDefenseLinks(Document doc, String homeOrVis) {
		String[] roster = new String[22];
		String homeStartersHTML = doc.selectFirst("div#all_" + homeOrVis + "_starters").html();
		// extract body of table as html
		String innerStartersTableHTML = extractSubstring(homeStartersHTML, "</thead>", "</table>");

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

		// System.out.println(arrayToString(starterRowsHTML));
		boolean skipFirstRow = true;
		for (String starterRowHTML : starterRowsHTML) {
			if (skipFirstRow) {
				skipFirstRow = false;
				continue;
			}
			// System.out.println(starterRowHTML);
			String curStarterPosition = extractSubstring(starterRowHTML, "\" data-stat=\"pos\" >", "</td></tr>");
			// get the link for the player in the current traversed upon row
			String starterLink = extractSubstring(starterRowHTML, " data-stat=\"player\" ><a href=\"", "\">");
			// System.out.println(starterLink);
			boolean keepLooping = true;
			while (keepLooping) {
				if (curStarterPosition.equals("QB"))
					roster[0] = starterLink;
				else if (anyStringEquals(runnersReceiversPositionAcronyms, curStarterPosition))
					runnersReceiversAway.add(starterLink);
				else if (anyStringEquals(offensiveLinemenPositionAcronyms, curStarterPosition))
					offensiveLinemenAway.add(starterLink);
				else if (anyStringEquals(defensePositionAcronyms, curStarterPosition))
					defenseAway.add(starterLink);
				else {
					try {
						//technically this isn't max efficiency because doesn't makes extra document connection that if this method handled the hashmap storing would decrease its time consumeds
						TimeUnit.SECONDS.sleep(2);
						String playerOfUnknownPositionPage = "https://www.pro-football-reference.com" + starterLink;
						// System.out.println(playerOfUnknownPositionPage);
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
	
	public static String findPosition(Document doc) {
		Elements paragraphs = doc.select("p:has(strong:contains(Position))");
		// Check if the element was found
		if (!paragraphs.isEmpty()) {
			Element paragraph = paragraphs.first();
			return paragraph.text().replace("Position: ", "");
		} else
			return null;
	}
	public static boolean anyStringEquals(List<String> strings, String targetString) {
		for (String str : strings) {
			if (str.equals(targetString)) {
				return true;
			}
		}
		return false;
	}
	public static String arrayToString(Object[] array) {
		StringBuilder result = new StringBuilder("[");
		for (int i = 0; i < array.length; i++) {
			result.append(array[i]);
			if (i < array.length - 1) {
				result.append(", ");
			}
		}
		result.append("]");
		return result.toString();
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
	public static String extractSubstring(String first, String second, String third) {
		// Find the index of the second string in the first string
		int startIndex = first.indexOf(second);

		// If the second string is found in the first string
		if (startIndex != -1) {
			// Find the index of the third string in the first string after the second
			// string
			int endIndex = first.indexOf(third, startIndex + second.length());

			// If the third string is found after the second string
			if (endIndex != -1) {
				// Extract the substring between the second and third strings
				return first.substring(startIndex + second.length(), endIndex);
			}
		}
		// If nothing is found between the second and third strings, return an empty
		// string
		return "";
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
}