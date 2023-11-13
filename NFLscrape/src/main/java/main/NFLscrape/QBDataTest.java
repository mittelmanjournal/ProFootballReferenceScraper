package main.NFLscrape;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class QBDataTest {
	public static boolean anyStringEquals(List<String> strings, String targetString) {
		for (String str : strings) {
			if (str.equals(targetString)) {
				return true;
			}
		}
		return false;
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
	public static String extractStringBetween(String original, String beforeString, String afterString) {
		int startIndex = original.indexOf(beforeString);
		int endIndex = original.indexOf(afterString, startIndex + beforeString.length());

		if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
			return original.substring(startIndex + beforeString.length(), endIndex);
		}

		return null;
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
		String heightText = doc.selectFirst("span:contains(-)").text();
		String weightText = doc.selectFirst("span:contains(lb)").text();
		
		return new double[] {getPositionEncoded(positionText), getImperialHeightAsDecimal(heightText), Integer.parseInt(weightText.replaceAll("lb", ""))};
		//height, weight, position one hot encoded
		//return new int[]{doc.selectFirst("span:contains(-)").text(), doc.selectFirst("span:contains(lb)").text()};
	}

	public static List<List<Double>> getYearlyAndPhysicalData(Document document) {
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

			// this one isn't compatible with addStat function because the func gets text
			// within element
			// that has data-stat param attribute, this one wants the text within the
			// attribute csk
			Element startingWinRate = tr.selectFirst("[data-stat=qb_rec]");
			if(startingWinRate.attr("csk").equals(null) || startingWinRate.attr("csk").equals("")
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

		return qbData;
	}

	// true if double false if int
	public static void addStat(String stat, List<Double> stats, Element e, boolean trueIfParseByDouble) {
		Element eStat = e.selectFirst(stat);
		double parsedStat;

		if (eStat.text().equals("") || eStat.text().equals(null))
			parsedStat = 0;
		else if (trueIfParseByDouble)
			parsedStat = Double.parseDouble(eStat.text());
		else
			parsedStat = Integer.parseInt(eStat.text());
		stats.add(parsedStat);
	}

	public static void main(String[] args) {
		String url = "https://www.pro-football-reference.com/players/B/BrisJa00.htm";

		try {
			// Fetch the HTML of the website
			Document document = Jsoup.connect(url).get();
			List<List<Double>> physicalsList1AndYearlyStats = getYearlyAndPhysicalData(document);
			
			physicalsList1AndYearlyStats.add(0, doubleArrayToList(getPlayerPhysical(document)));
			
			System.out.println(physicalsList1AndYearlyStats);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
    public static List<Double> doubleArrayToList(double[] array) {
        List<Double> list = new ArrayList<>();
        for (double element : array) {
            list.add(element);
        }
        return list;
    }


}
