package main.NFLscrape;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class QB2TMEdgeCase {

	public static void main(String[] args) throws IOException {
		System.out.println(getRunRecDataUpToGame(
				getRunRecData(Jsoup.connect("https://www.pro-football-reference.com/players/B/BrewSe20.htm").get())));
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
	
	static int year = 2002;
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
		//endTime = System.nanoTime();
		return runRecData;
	}

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

	public static int getPositionEncoded(String position) {
		position = position.trim();
		if (position.contains("-"))
			position = position.split("-")[0];
		// System.out.println("position var in getPositionEncoded func: " + position);

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

}
