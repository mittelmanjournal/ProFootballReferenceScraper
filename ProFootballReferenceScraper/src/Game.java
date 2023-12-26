package main2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/** All of the properties of this object are found in any game page */
public class Game {
	
	//Properties/Variables:
	
	//Below is game data that doesn't require any extra scraping on different pages
	
	/** the letters after the last '/' and before the ".htm" in the game link ("200209050nyg" 
	 * is the ID for this page https://www.pro-football-reference.com/boxscores/200209050nyg.htm)*/
	private String ID;
	
	/**the week in the season the game was played (1 to 20 something) 1-17 or 18 is reg season
	 * 18-22 or something is play-offs	 */
	private int week;
	
	/** The year of the season this game was played in. This is necessary because when getting  
	 * career data or when getting game box scores data for a player we need to know up to what season to get it */
	private int year; 
	
	/** The week and date game was played ("Thursday Sep 5, 2002") */
	private String date;
	
	/** The time game was played as a String ("8:38pm") */
	String time;
	
	/** The status of the roof, opened, closed, etc... ("outdoors") */
	String roof;
	
	/** The surface of the field, grass, concrete etc... ("grass") */
	String surface;
	
	/** The temperature, relative humidity, wind speed, and wind chill of the nearest airport to the game, separated by \
	 * commas ("73 degrees, relative humidity 49%, wind 7 mph, wind chill 0") TODO: note that sometimes relative humidity is excluded*/
	String weather;
	
	/** The vegas line for gambling of this game formatted like so <"team line"> or is "Pick" if empty ("San Francisco 49ers -4.0") */
	String vegas;
	
	/** The over under line for the total points scored in this game ("39.0 (under)") */
	String OU;
	
	//Below are all visitor and home team components, some require extra scraping others don't
	
	//Visitor info that doesn't need any extra page connections/scraping
	/** Name of visiting team ("San Francisco 49ers") */
	String visName;
	
	/** The points the visitors scored (16) */
	int visScore;
	
	/** the visitor's team current season record ("1-0") ("3-2-1" represents 3 wins, 2 losses, and 1 tie. 
	 * If we scrape "1-0" then "1-0-0" is implied) (*TODO: potential edge case may be that record restarts entering playoffs) */
	String visSeasonRec;
	
	//Visitor IDs that require extra scraping on their respective player or coach pages
	
	/** the letters after the last '/' and before the ".htm" in the coach link ("MariSt0" is the ID for this 
	 * coach's page https://www.pro-football-reference.com/coaches/MariSt0.htm) */
	String visCoachID;
	
	/** The ID of the starting visitor QB ("GarcJe00") */
	String visQBID;
	
	/** a list of 5 IDs of visitor starters that can be RBs, WRs, or TEs of any kind. These positions stats all fall into 
	 * the same 2 categories so I grouped them (["HearGa00", "OwenTe00", "SwifJu00", "StokJ.00", "JohnEr00"]) */
	String[] visRID = new String[5];
	
	/** a list of 11 IDs of visitor defenders that can be any defender position acronym. These positions only have defense stats. 
	 * (["OkeaCh20", "YounBr00", "StubDa00", "CartAn20", "PeteJu00", "SmitDe21", "WinbJa20", "PlumAh20", "WebsJa20", "ParrTo20", 
	 * "BronZa20"]) not including kickers, punters, kick returners, punt returners and O-Linemen because either there data is 
	 * hard to parse, or they have no relevant stat categories. */
	String[] visDID = new String[11];
	
	//Home team info that doesn't need any extra page connections/scraping
	
	/** Name of home team ("New York Giants") */
	String homeName;
	
	/** The points the home team scored (13) */
	int homeScore;
	
	/** The home team's current season record ("0-1") */
	String homeSeasonRec;
	
	//Home team IDs that require extra scraping on their respective player or coach pages
	
	/** The letters after the last '/' and before the ".htm" in the coach link  */
	String homeCoachID;
	
	/** The ID of the starting home QB */
	String homeQBID;
	
	/** A list of 5 IDs of home starters that can be RBs, WRs, or TEs of any kind */
	String[] homeRID = new String[5];
	
	/** A list of 11 IDs of home defenders that can be any defender position acronym */
	String[] homeDID = new String[11];
	
	List<List<String>> allPlayerGameData = new ArrayList<List<String>>();
		
	//Behaviors/Methods and Constructor(s)
	
	//validity methods:
	
	private boolean areStartersValid(Document doc) {
		boolean validity = true;
		
		Document checkVisStartersValidity = Jsoup.parse(doc.selectFirst("#all_vis_starters").html().replace("<!--\n", "").replace("\n-->", ""));
		if(checkVisStartersValidity.selectFirst("tbody").selectFirst("tr:has(td[data-stat=pos]:empty)") != null){
			validity = false;
		}
		
		Document checkHomeStartersValidity = Jsoup.parse(doc.selectFirst("#all_home_starters").html().replace("<!--\n", "").replace("\n-->", ""));
		if(checkHomeStartersValidity.selectFirst("tbody").selectFirst("tr:has(td[data-stat=pos]:empty)") != null){
			validity = false;
		}
		
		return validity;	
	}
	
	/**
	 * WILL ALSO return false if size isn't 44/roster isn't full
	 * @param doc
	 * @return
	 */
	private boolean arePosAcronymsValid(Document doc) {
		//go through each position acronym and if any exist that aren't part of the valid ones, return false
		Document visitor = Jsoup
				.parse(doc.selectFirst("#all_vis_starters").html().replace("<!--\n", "").replace("\n-->", ""));
		Document home = Jsoup
				.parse(doc.selectFirst("#all_home_starters").html().replace("<!--\n", "").replace("\n-->", ""));

		Elements startersVis = visitor.select("tbody > tr:has(td[data-stat=pos])");
		Elements startersHome = home.select("tbody > tr:has(td[data-stat=pos])");
		startersVis.addAll(startersHome);
		Elements allStarters = startersVis;

		String[] curPosAcronyms = allStarters.select("td").text().split(" "); //TODO maybe just in case add a .trim() after .text()
		
		HashSet<String> qbCheck = new HashSet<String>();
		qbCheck.addAll(Arrays.asList(curPosAcronyms));
		
		if (curPosAcronyms.length != 44 || !qbCheck.contains("QB"))
			return false;
		String[] validAcronyms = "QB Z X RB HB TB FB LH RH BB B WB WR RWR LWR FL TE SE RE LE DL LDE DE LDT DT NT UT MG DG RDT RDE JLB LOLB RUSH OLB BLB LLB LILB WILL ILB SLB MLB MIKE WLB RILB RLB ROLB SAM LB LCB CB RCB SS FS LDH RDH S RS DB END OL LT LOT T LG G C RG RT ROT"
				.split(" ");
		boolean isCurPosVal = false;
		for (String acr : curPosAcronyms) {
			if(acr.contains("/")) { // sometimes this String is formatted like this: "FB/TE"
				acr = acr.split("/")[0];
			}
			int i = 0;
			isCurPosVal = false;
			while (i < validAcronyms.length) {
				if (acr.equals(validAcronyms[i])) {
					isCurPosVal = true;
					break;
				}
				i++;
			}

			if (!isCurPosVal) {
				System.out.println("Invalid pos found "+acr);
				break;
			}

		}
		return isCurPosVal;
	}
	
	/** Constructor that takes in a String which is a link to an HTML document containing all of the data to fill the 
	 * variables of this class with. Also must delay at least 2 seconds before connecting to a new document.*/
	public Game(String info, String delim) {
		//0Game ID:%%:1week:%%:2year:%%:3date:%%:4time:%%:5roof:%%:6surface:%%:7weather:%%:8vegas:%%:9OU:%%:10visName:%%:11visScore:%%:12visSeasonRec:%%:13visCoachID:%%:14visQBID:%%:15[visRID as comma separated list NO SPACES]:%%:16[visDID as comma separated list]:%%:17homeName:%%:18homeScore:%%:19homeSeasonRec:%%:20homeCoachID:%%:21homeQBID:%%:22[homeRID as comma separated list]:%%:23[homeDID as comma separated list]
		String[] infoArr = info.split(delim);
		ID = infoArr[0]; //must correspond with date and home team abbrev
		week = Integer.parseInt(infoArr[1]); // *only doing regular season currently, only week 1 to 17 up until 2021, where 18
		year = Integer.parseInt(infoArr[2]); // any number > 2001 and < 2024
		date = infoArr[3]; //weekday monthAbbrev monthDay, year to be valid
		time = infoArr[4]; // must contain "am" or "pm" or ":" to be a valid input
		roof = infoArr[5];
		surface = infoArr[6];
		weather = infoArr[7]; //generally formatted like deg, rel humid, wind spd
		vegas = infoArr[8]; //when empty "Pick" 
		OU = infoArr[9];
		
		visName = infoArr[10];
		visScore = Integer.parseInt(infoArr[11]);
		visSeasonRec = infoArr[12];
		
		visCoachID = infoArr[13];
		visQBID = infoArr[14];
		visRID = infoArr[15].replace("[", "").replace("]", "").split(","); //make sure no spaces
		visDID = infoArr[16].replace("[", "").replace("]", "").split(",");
		
		homeName = infoArr[17];
		homeScore = Integer.parseInt(infoArr[18]);
		homeSeasonRec = infoArr[19];
		
		homeCoachID = infoArr[20];
		homeQBID = infoArr[21];
		homeRID = infoArr[22].replace("[", "").replace("]", "").split(",");;
		homeDID = infoArr[23].replace("[", "").replace("]", "").split(",");;
		//make a function to get the box score data and update main hashmap
	}
	
	public Game(String link) {
		ID = link.substring(49, 61);
		//connect to link	
		Document gamePage = null;
		try {
			gamePage = Jsoup.connect(link).get();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// start counting time
		long startTime = System.currentTimeMillis();

		// set all of the values above if this game is valid
		if (areStartersValid(gamePage) && arePosAcronymsValid(gamePage)) { //TODO the only things that need to be valid are parameters to the NN, meaning OU doesn't have to be valid
			
			week = getWeekFromDoc(gamePage);
			// year is set in Main class
			date = getDateFromDoc(gamePage);
			time = getTimeFromDoc(gamePage);
			roof = getGameInfoFromDoc(gamePage, "Roof");
			surface = getGameInfoFromDoc(gamePage, "Surface");
			weather = getGameInfoFromDoc(gamePage, "Weather");
			vegas = getGameInfoFromDoc(gamePage, "Vegas Line");
			OU = getGameInfoFromDoc(gamePage, "Over/Under");

			visName = getFullTeamNameFromDoc(gamePage, "vis");
			visScore = getScoreFromDoc(gamePage, "vis");
			// TODO: NOTE that the season record is accounting AFTER the game. When we build
			// the datapoint, we want the record to be before the game, make sure the
			// getRecord function works accordingly
			visSeasonRec = getRecordFromDoc(gamePage, "vis");

			visCoachID = getCoachIDFromDoc(gamePage, "vis");
			visQBID = getQBIDFromDoc(gamePage, "vis");
			visRID = getPosIDsFromDoc(gamePage, "vis", new String[] {"Z", "X", "RB", "HB", "TB", "FB", "LH", "RH", "BB", "B", "WB", "WR", "RWR", "LWR", "FL", "TE", "SE"}, visRID.length);
			visDID = getPosIDsFromDoc(gamePage,"vis", "RE LE DL LDE DE LDT DT NT UT MG DG RDT RDE END JLB LOLB RUSH BLB OLB LLB LILB WILL ILB SLB MLB MIKE WLB RILB RLB ROLB SAM LB LCB CB RCB SS FS LDH RDH S RS DB".split(" "), visDID.length);
			
			homeName = getFullTeamNameFromDoc(gamePage, "home");
			homeScore = getScoreFromDoc(gamePage, "home");
			homeSeasonRec = getRecordFromDoc(gamePage, "home");
			
			homeCoachID = getCoachIDFromDoc(gamePage, "home");
			homeQBID = getQBIDFromDoc(gamePage, "home");
			homeRID = getPosIDsFromDoc(gamePage, "home", new String[] {"Z", "X", "RB", "HB", "TB", "FB", "LH", "RH", "BB", "B", "WB", "WR", "RWR", "LWR", "FL", "TE", "SE"}, homeRID.length);
			homeDID = getPosIDsFromDoc(gamePage, "home", "RE LE DL LDE DE LDT DT NT UT MG DG RDT RDE END JLB LOLB RUSH BLB OLB LLB LILB WILL ILB SLB MLB MIKE WLB RILB RLB ROLB SAM LB LCB CB RCB SS FS LDH RDH S RS DB".split(" "), homeDID.length);
		
			allPlayerGameData = getAllStatlinesFromDoc(gamePage);
		}
		
		//get total time used
		long endTime = System.currentTimeMillis();
		
		long timeConsumed = endTime - startTime;
		
		//get time needed to delay to keep speed optimal
		if(2000 - timeConsumed <= 0) {
			Main.INTER_GAME_DELAY = 2000;
		} else {
			Main.INTER_GAME_DELAY = 2000 - timeConsumed;
		}
		
	}
	
	//Class getters and setters
	
	public List<List<String>> getAllPlayerData(){
		return this.allPlayerGameData;
	}
	
	//there is no setID() because I won't ever need to change the ID outside of this class
	public String getID() {
		return ID;
	}
	
	//there is no setWeek() because I won't ever need to change the week outside of this class
	public int getWeek() {
		return week;
	}
	
	public void setYear(int year) {
		this.year = year;
	}
	public int getYear() {
		return year;
	}
	
	public String getDate() {
		return date;
	}
		
	//Getters from HTML document to build object in constructor

	private static int getWeekFromDoc(Document doc) {
		// Assuming doc is a gamePage
		String weekHTML = doc.select("#all_other_scores").html();
		Document tempDoc = Jsoup.parse(weekHTML.replace("<!--     ", "\n").replace("-->", "").trim());
		String weekString = tempDoc.select("h2").text();
		String temp[] = weekString.split(" ");

		return Integer.parseInt(temp[temp.length - 1]);
	}
	
	private static String getDateFromDoc(Document doc) {
		Elements elements = doc.select(".scorebox_meta div");
		String date = elements.get(0).text();
		return date;
	}
	
	private static String getTimeFromDoc(Document doc) {
		Elements elements = doc.select(".scorebox_meta div");
		String time = elements.get(1).text().replace("Start Time: ", "");;
		return time;
	}
	
	private static String getGameInfoFromDoc(Document doc, String item) {
		try {
			Element gameInfoDiv = doc.selectFirst("div#all_game_info");
			String html = gameInfoDiv.outerHtml();
			Document tempDoc = Jsoup.parse(html.replace("<!--\n", "").replace("\n-->", ""));
			return tempDoc.selectFirst("tr:has(th:contains(" + item + "))").child(1).text();
		} catch (NullPointerException npe) {
			npe.printStackTrace();
			return "Failed to collect " + item; //if you want it to be sent to invalid category if any of these are invalid, return null
		}		
	}
	
	private static String getTeamNameFromDoc(Document doc, String visOrHome) {
		return doc.getElementById("all_"+visOrHome+"_starters").selectFirst("h2").text().replace(" Starters", "");		
	}
	
	private static String getFullTeamNameFromDoc(Document doc, String visOrHome) {
		return visOrHome.equalsIgnoreCase("home")
				? doc.selectFirst("div.scorebox").select("a[href*=/teams/]").get(1).text()
				: doc.selectFirst("div.scorebox").select("a[href*=/teams/]").get(0).text();
//		if(visOrHome.equalsIgnoreCase("home")) return doc.selectFirst("div.scorebox").select("a[href*=/teams/]").get(1).text();
//		else if (visOrHome.equalsIgnoreCase("vis") return doc.selectFirst("div.scorebox").select("a[href*=/teams/]").get(0).text();
	}
	
	private static int getScoreFromDoc(Document doc, String visOrHome) {
		return visOrHome.equalsIgnoreCase("home")
				? Integer.parseInt(doc.select(".score").get(1).text())
				: Integer.parseInt(doc.select(".score").get(0).text());
	}
	
	//TODO: somehow handle the ties in the getter (make getter private because only will be used in the build and compile data point method
	private static String getRecordFromDoc(Document doc, String visOrHome) {
		return visOrHome.equalsIgnoreCase("home")
				? doc.select(".scores").get(1).nextElementSibling().text()
				: doc.select(".scores").get(0).nextElementSibling().text();
	}
	
	private static String getCoachIDFromDoc(Document doc, String visOrHome) {
		return visOrHome.equalsIgnoreCase("home")
				? doc.select("div.datapoint > a[href*=/coaches/]").get(1).attr("href").replace("/coaches/", "").replace(".htm", "")
				: doc.select("div.datapoint > a[href*=/coaches/]").get(0).attr("href").replace("/coaches/", "").replace(".htm", "");
	}
	
	private static String getQBIDFromDoc(Document doc, String visOrHome) {
		Document revisedDoc = Jsoup.parse(doc.selectFirst("#all_"+visOrHome+"_starters").html().replace("<!--\n", "").replace("\n-->", ""));
		return revisedDoc.selectFirst("tbody > tr:has(td[data-stat=pos]:contains(QB)) > th").attr("data-append-csv");
	}
	
	/**
	 * TODO note that instead of iterating over an array of valid String position
	 * acronyms, you can probably use a HashSet and just check if contains (this
	 * will be constant speed). But it works for now so I'm good, maybe later if
	 * speed optimizations are needed, but as long as the Game object construction
	 * takes less than 2 seconds I'm fine.
	 * 
	 * @param doc
	 * @param visOrHome
	 * @param posAcronyms
	 * @param arrLength
	 * @return
	 */
	private static String[] getPosIDsFromDoc(Document doc, String visOrHome, String[] posAcronyms, int arrLength) {
		String[] ret = new String[arrLength];
		Document revisedDoc = Jsoup.parse(doc.selectFirst("#all_"+visOrHome+"_starters").html().replace("<!--\n", "").replace("\n-->", ""));
		Elements starters = revisedDoc.select("tbody > tr:has(td[data-stat=pos])");
		int j = 0;
		for (Element starter : starters) {
			if(j >= ret.length) {
				break;
			}
			String acr = starter.select("td[data-stat=pos]").text();
			if(acr.contains("/")) {
				acr = acr.split("/")[0];
			}
			for (int i = 0; i < posAcronyms.length; i++) {
				if (acr.equals(posAcronyms[i])) {
					ret[j] = starter.selectFirst("th").attr("data-append-csv");
					j++;
					break; 
				}
			}
		}
		return ret;
	}
	
    public static <T> String arrayToString(T[] array) {
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
	
	public String toStringConsole() {
		return "ID -> " + ID + "\n" + "week -> " + week + "\n" + "year -> " + year + "\n" + "date -> " + date + "\n" + "time -> " + time + "\n" + "roof -> " + roof + "\n" + "surface -> " + surface + "\n" + "weather -> " + weather + "\n" + "vegas -> " + vegas + "\n" + "OU -> " + OU + "\n" + "visName -> " + visName + "\n" + "visScore -> " + visScore + "\n" + "visSeasonRec -> " + visSeasonRec + "\n" + "visCoachID -> " + visCoachID + "\n" + "visQBID -> " + visQBID + "\n" + "visRID -> " + arrayToString(visRID) + "\n" + "visDID -> " + arrayToString(visDID) + "\n" + "homeName -> " + homeName + "\n" + "homeScore -> " + homeScore + "\n" + "homeSeasonRec -> " + homeSeasonRec + "\n" + "homeCoachID -> " + homeCoachID + "\n" + "homeQBID -> " + homeQBID + "\n" + "homeRID -> " + arrayToString(homeRID) + "\n" + "homeDID -> " + arrayToString(homeDID);
	}
	
	public String toString(String d) {
		//0Game ID:%%:1week:%%:2year:%%:3date:%%:4time:%%:5roof:%%:6surface:%%:7weather:%%:8vegas:%%:9OU:%%:10visName:%%:11visScore:%%:12visSeasonRec:%%:13visCoachID:%%:14visQBID:%%:15[visRID as comma separated list NO SPACES]:%%:16[visDID as comma separated list]:%%:17homeName:%%:18homeScore:%%:19homeSeasonRec:%%:20homeCoachID:%%:21homeQBID:%%:22[homeRID as comma separated list]:%%:23[homeDID as comma separated list]
		return ID + d + week + d + year + d + date + d + time + d + roof + d + surface + d + weather + d + vegas + d + OU + d + visName + d + visScore + d + visSeasonRec + d + visCoachID + d + visQBID + d + formatStringArr(visRID) + d + formatStringArr(visDID) + d + homeName + d + homeScore + d + homeSeasonRec + d + homeCoachID + d + homeQBID + d + formatStringArr(homeRID) + d + formatStringArr(homeDID);
		
	}
	
	private static String formatStringArr(String[] array) {
        StringBuilder result = new StringBuilder("[");
        for (int i = 0; i < array.length; i++) {
            result.append(array[i]);
            if (i < array.length - 1) {
                result.append(",");
            }
        }
        result.append("]");
        return result.toString();
    }

	
	//you will make the build function here.
	//you will pass in careerdata and gamestatline objects of the ids of the starters
	//you will make private getters that format the game object data the way you want to
		//for example the Season Records are stored like this "2-0", you will make a getter that returns them
		//all like this 1-0-0 so that if ties occur we can avoid confusion, also removing the most current game
	
	//make methods that collect box score data and return it for the gamedata statline hashmap
	public List<List<String>> getAllStatlinesFromDoc(Document gamePage) {
		List<List<String>> allBoxScores = new ArrayList<List<String>>();
		for(Element tr : gamePage.select("#div_player_offense > table > tbody > tr")) {
			if(!tr.classNames().contains("thead")) {
				// if size 21 = offense, if 16 = defense: System.out.println(tr.childrenSize());
				ArrayList<String> curPlayerStatline = new ArrayList<String>();
				
				String PID = tr.select("th").attr("data-append-csv");
				curPlayerStatline.add(PID);
				
				String pcmp = tr.select("td[data-stat=pass_cmp]").text();
				curPlayerStatline.add(pcmp);
				
				String patt = tr.select("td[data-stat=pass_att]").text();
				curPlayerStatline.add(patt);
				
				String pyds = tr.select("td[data-stat=pass_yds]").text();
				curPlayerStatline.add(pyds);
				
				String ptd = tr.select("td[data-stat=pass_td]").text();
				curPlayerStatline.add(ptd);
				
				String pint = tr.select("td[data-stat=pass_int]").text();
				curPlayerStatline.add(pint);
				
				String psk = tr.select("td[data-stat=pass_sacked]").text();
				curPlayerStatline.add(psk);
				
				String pskyds = tr.select("td[data-stat=pass_sacked_yds]").text();
				curPlayerStatline.add(pskyds);
				
				String plng = tr.select("td[data-stat=pass_long]").text();
				curPlayerStatline.add(plng);
				
				String prtg = tr.select("td[data-stat=pass_rating]").text();
				curPlayerStatline.add(prtg);
				
				String ratt = tr.select("td[data-stat=rush_att]").text();
				curPlayerStatline.add(ratt);
				
				String ryds = tr.select("td[data-stat=rush_yds]").text();
				curPlayerStatline.add(ryds);
				
				String rtd = tr.select("td[data-stat=rush_td]").text();
				curPlayerStatline.add(rtd);
				
				String rlng = tr.select("td[data-stat=rush_long]").text();
				curPlayerStatline.add(rlng);
				
				String tgts = tr.select("td[data-stat=targets]").text();
				curPlayerStatline.add(tgts);
				
				String rec = tr.select("td[data-stat=rec]").text();
				curPlayerStatline.add(rec);
				
				String recyds = tr.select("td[data-stat=rec_yds]").text();
				curPlayerStatline.add(recyds);
				
				String rectd = tr.select("td[data-stat=rec_td]").text();
				curPlayerStatline.add(rectd);
				
				String reclng = tr.select("td[data-stat=rec_long]").text();
				curPlayerStatline.add(reclng);
				
				String fmbs = tr.select("td[data-stat=fumbles]").text();
				curPlayerStatline.add(fmbs);

				String fls = tr.select("td[data-stat=fumbles_lost]").text();
				curPlayerStatline.add(fls);
				
				allBoxScores.add(curPlayerStatline);
			}
		}
		
		Document d = Jsoup.parse(gamePage.select("#all_player_defense").html().replace("<!--\n", "").replace("\n-->", ""));
		
		for(Element tr : d.select("#div_player_defense > table > tbody > tr")) {
			if (!tr.classNames().contains("thead")) {
				ArrayList<String> curPlayerStatline = new ArrayList<String>();
				//System.out.println(tr.childrenSize());
				
				String PID = tr.select("th").attr("data-append-csv");
				curPlayerStatline.add(PID);
				
				String dint = tr.select("td[data-stat=def_int]").text();
				curPlayerStatline.add(dint);
				
				String dintyds = tr.select("td[data-stat=def_int_yds]").text();
				curPlayerStatline.add(dintyds);
				
				String dinttd = tr.select("td[data-stat=def_int_td]").text();
				curPlayerStatline.add(dinttd);
				
				String dintlng = tr.select("td[data-stat=def_int_long]").text();
				curPlayerStatline.add(dintlng);
				
				String passDefended = tr.select("td[data-stat=pass_defended]").text();
				curPlayerStatline.add(passDefended);
				
				String sks = tr.select("td[data-stat=sacks]").text();
				curPlayerStatline.add(sks);
				
				String tcmb = tr.select("td[data-stat=tackles_combined]").text();
				curPlayerStatline.add(tcmb);
				
				String tsolo = tr.select("td[data-stat=tackles_solo]").text();
				curPlayerStatline.add(tsolo);
				
				String tast = tr.select("td[data-stat=tackles_assists]").text();
				curPlayerStatline.add(tast);
				
				String tloss = tr.select("td[data-stat=tackles_loss]").text();
				curPlayerStatline.add(tloss);
				
				String qbHits = tr.select("td[data-stat=qb_hits]").text();
				curPlayerStatline.add(qbHits);
				
				String frec = tr.select("td[data-stat=fumbles_rec]").text();
				curPlayerStatline.add(frec);
				
				String frecyds = tr.select("td[data-stat=fumbles_rec_yds]").text();
				curPlayerStatline.add(frecyds);
				
				String frectd = tr.select("td[data-stat=fumbles_rec_td]").text();
				curPlayerStatline.add(frectd);
				
				String ff = tr.select("td[data-stat=fumbles_forced]").text();
				curPlayerStatline.add(ff);
				
				allBoxScores.add(curPlayerStatline);
			}
			
		}
		return allBoxScores;
	}
}
