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

public class FinalHashMapTest {	
	static HashMap<String, List<List<Double>>> returnerLinkToData = new HashMap<String, List<List<Double>>>();
	
	public static void main(String[] args) throws IOException, InterruptedException {
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
		
		awayRoster[i] = kickersAndPunters.get(0);
		awayRoster[i+1] = kickersAndPunters.get(1);
		
		awayRoster[i+2] = returners.get(0);
		
		i = 0;
		for (String s : getStarterOffenseAndDefenseLinks(document, "home")) {
			homeRoster[i] = s;
			i++;
		}
		
		homeRoster[i] = kickersAndPunters.get(2);
		homeRoster[i+1] = kickersAndPunters.get(3);
		
		homeRoster[i+2] = returners.get(1);
				
		//System.out.println(arrayToString(awayRoster));
		//System.out.println(arrayToString(homeRoster));
		HashMap<String, List<List<Double>>> playerLinkToData = new HashMap<String, List<List<Double>>>();
		//in: list of player links and a hashmap to manipulate | out: update the existing hashmap by changing (don't take up more memory) with any new links and map them to the data on their page
		//void
		
		updateLinkToDataMap(playerLinkToData,homeRoster);
		updateLinkToDataMap(playerLinkToData,awayRoster);
		
		System.out.println(playerLinkToData);
		System.out.println();
		System.out.println(returnerLinkToData);
	}
	
	public static void updateLinkToDataMap(HashMap hm, String[] playerLinks) throws InterruptedException{
		//because returners isn't an actual position https://www.pro-football-reference.com/players/H/HestDe99.htm, 
		//you must track the index so that when you get to that player, 
		int i = 0;
		for(String playerLink : playerLinks) {
			System.out.println(playerLink);
			if(i == 24) { //returner hashmap manipulation goes here
				if(returnerLinkToData.containsKey(playerLink) || playerLink.equals(null) || playerLink.equals("")) continue;
				returnerLinkToData.put(playerLink, getReturnerData(playerLink));
				continue;
			}
			if(hm.containsKey(playerLink) || playerLink.equals(null) || playerLink.equals("")) continue; //this line is most likely where you will be updating weekly data
			hm.put(playerLink, getPlayerData(playerLink));
			i++;
		}
		
	
	}

	//list of yearly data, each year is a list of data with the columns/each element representing a data category
	public static List<List<Double>> getPlayerData(String playerLink) throws InterruptedException{
		String url = "https://www.pro-football-reference.com" + playerLink;
		try {
			TimeUnit.SECONDS.sleep(2);
			Document document = Jsoup.connect(url).get();
			String positionText = document.selectFirst("p:has(strong:contains(Position))").text();
			positionText = positionText.replaceAll("Position: ", "");
			positionText = positionText.replaceAll(" Throws: Right", "");
			positionText = positionText.replaceAll(" Throws: Left", "");
			
			if(getPositionEncoded(positionText) == 1) return getQBData(document);
			else if (getPositionEncoded(positionText) == 2) return getRunRecData(document);
			else if (getPositionEncoded(positionText) == 3)	return getOLData(document);
			else if(getPositionEncoded(positionText) == 4) return getDefenderData(document);
			else if(getPositionEncoded(positionText) == 5) return getKickerData(document);
			else if(getPositionEncoded(positionText) == 6) return getPunterData(document);
			//because some players both return and start in positions, in order to make non conflicting data,
			//you must create a different punter hashmap so that a player can have starting data at their
			//respective position, as well as returner data.
			
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

		Element runRecTable = document.getElementById("all_rushing_and_receiving");
		Elements tbodyRunRec = runRecTable.select("tbody");
		Elements trsRunRec = tbodyRunRec.select("tr");
		//System.out.println(trsRunRec);

		List<List<Double>> qbData = new ArrayList<List<Double>>();
		for (Element tr : trs) {
			// System.out.println(tr);
			ArrayList<Double> yearlyData = new ArrayList<Double>();

			addStat("[data-stat=year_id]", yearlyData, tr, false);
			addStat("[data-stat=age]", yearlyData, tr, false);
			addStat("[data-stat=g]", yearlyData, tr, false);

			// incompatible with addStat func
			Element startingWinRate = tr.selectFirst("[data-stat=qb_rec]");
			if(startingWinRate.attr("csk").equals(null) || startingWinRate.attr("csk").equals("")
					|| startingWinRate.text().equals(null) || startingWinRate.text().equals(""))
				yearlyData.add(-1.0); //if the player had no starting games to create the csk attribute, his win percent wasn't 0, it didn't exist meaning its -1
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

		int i = 0;
		for (Element tr : trsRunRec) {
			if(i > qbData.size() - 1) break;
			List<Double> dataList = qbData.get(i);
			if (Integer.parseInt(tr.selectFirst("[data-stat=year_id]").text()) == dataList.get(0)) {
				dataList.add(Double.parseDouble(tr.selectFirst("[data-stat=rush_att]").text()));
				dataList.add(Double.parseDouble(tr.selectFirst("[data-stat=rush_yds]").text()));
				dataList.add(Double.parseDouble(tr.selectFirst("[data-stat=rush_td]").text()));
				dataList.add(Double.parseDouble(tr.selectFirst("[data-stat=fumbles]").text()));
			}
			i++;
		}
		
		qbData.add(0, doubleArrayToList(getPlayerPhysical(document))); //add to the front of list the physicals and position encoding
		return qbData;
	}
	public static List<List<Double>> getRunRecData(Document document){
		// the exception is caused by runners having the table IDed as rush_rec and receivers have table IDed as rec_run
		Element runRecTable = document.getElementById("all_receiving_and_rushing"); //rush and rec for RBs, rec and rush for receivers
		if (runRecTable == null) runRecTable = document.getElementById("all_rushing_and_receiving"); 
		Elements tbodyRunRec = runRecTable.select("tbody");
		Elements trsRunRec = tbodyRunRec.select("tr");
		
		List<List<Double>> runRecData = new ArrayList<List<Double>>();
		for(Element tr : trsRunRec) {
			//System.out.println(tr.text());
			ArrayList<Double> yearlyData = new ArrayList<Double>();
			
			String validYearCheck = removeNonNumericCharacters(tr.selectFirst("[data-stat=year_id]").text());
			String validAgeCheck = removeNonNumericCharacters(tr.selectFirst("[data-stat=age]").text());
			if(validYearCheck.equals("") || validYearCheck == null || validAgeCheck.equals("") || validAgeCheck == null)
				continue; //the year or age is empty, don't add any of the data because it is the split up data from the entire year /players/C/CoopAm00.htm
			
			addStat("[data-stat=year_id]", yearlyData, tr, false);
			addStat("[data-stat=age]", yearlyData, tr, false);
			addStat("[data-stat=g]", yearlyData, tr, false);
			
			runRecData.add(yearlyData);
		}
		
		runRecData.add(0, doubleArrayToList(getPlayerPhysical(document)));
		return runRecData;
	}
	public static List<List<Double>> getOLData(Document document){
		return null;
	}
	public static List<List<Double>> getDefenderData(Document document){
		return null;
	}
	public static List<List<Double>> getKickerData(Document document){
		return null;
	}
	public static List<List<Double>> getPunterData(Document document){
		return null;
	}
	
	public static List<List<Double>> getReturnerData(String playerLink) {
		String url = "https://www.pro-football-reference.com" + playerLink;
		try {
			TimeUnit.SECONDS.sleep(2);
			Document document = Jsoup.connect(url).get();
			
		} catch (Exception e) {
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
	
	// true if double false if int
	public static void addStat(String stat, List<Double> stats, Element e, boolean trueIfParseByDouble) {
		Element eStat = e.selectFirst(stat);
		double parsedStat;

		if (eStat == null) //if its element doesnt exist, make it -1 as a flag
			parsedStat = -1;
		else if (eStat.text().equals("") || eStat.text().equals(null)) // if its empty make it equal 0
			parsedStat = 0;
		else if (trueIfParseByDouble)
			parsedStat = Double.parseDouble(removeNonNumericCharacters(eStat.text()));
		else
			parsedStat = Integer.parseInt(removeNonNumericCharacters(eStat.text()));
		stats.add(parsedStat);
	}
	public static int getPositionEncoded(String position) {
		position = position.trim();
		
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
		
		if(position.equals("QB"))
			return 1;
		else if(anyStringEquals(runnersReceiversPositionAcronyms,position))
			return 2;
		else if(anyStringEquals(offensiveLinemenPositionAcronyms,position))
			return 3;
		else if(anyStringEquals(defensePositionAcronyms,position))
			return 4;
		else if(position.equals("K"))
			return 5; //5 is reserved for returners, but no position is returners
		else if(position.equals("P"))
			return 6;
		//returner isnt a real position will be one of the positions above,
		return -1;
	}
	public static float getImperialHeightAsDecimal(String height) {
		String[] feetInches = height.split("-");
		if(feetInches.length > 2) return -1f;
		return Float.parseFloat(feetInches[0]) + (Float.parseFloat(feetInches[1])/12f); 
	}
	public static double[] getPlayerPhysical(Document doc) {
		String positionText = doc.selectFirst("p:has(strong:contains(Position))").text();
		positionText = positionText.replaceAll("Position: ", "");
		positionText = positionText.replaceAll(" Throws: Right", "");
		positionText = positionText.replaceAll(" Throws: Left", "");
		String heightText = doc.selectFirst("p:has(span:contains(-))").select("span").get(0).text(); // what if a player has a name with a dash? /players/P/PeopDo00.htm
		String weightText = doc.selectFirst("p:has(span:contains(lb))").select("span").get(1).text();
		//System.out.println(doc.selectFirst("p:has(span:contains(lb))").select("span").get(1));
		//doc.selectFirst("p:has(span:contains(-))").select("span").get(0)
		//doc.selectFirst("p:has(span:contains(lb))").selectFirst("span:contains(lb)")
		return new double[] {getPositionEncoded(positionText), getImperialHeightAsDecimal(heightText), Integer.parseInt(weightText.replaceAll("lb", ""))};
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
	
	public static String removeNonNumericCharacters(String input) {
        // Regular expression to match any character that is not a digit (0-9) or a dot (.)
        String regex = "[^0-9.]";
        
        // Compile the regular expression pattern
        Pattern pattern = Pattern.compile(regex);
        
        // Create a matcher with the input string
        Matcher matcher = pattern.matcher(input);
        
        // Replace all non-numeric characters with an empty string
        String result = matcher.replaceAll("");
        
        // Return the cleaned string
        return result;
    }
	public static List<Double> doubleArrayToList(double[] array) {
        List<Double> list = new ArrayList<>();
        for (double element : array) {
            list.add(element);
        }
        return list;
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