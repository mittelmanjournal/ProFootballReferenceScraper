package main2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class YearlyCareerData {
	
	static String forMain;
	Document page;
	private String id;
	private String name;
	private String position;
	private String handedness;
	private String heightCm;
	private String weightKg;
	private String[] combineMeasurements = new String[6];
	//40, bench, broad, shuttle, 3cone, vert
	
	//year to list of data including age, and stats (for QB stats are pass and rush, for Run/Rec stats are just rush and rec, and for defender stats are just defense
	public Map<String, List<String>> yearToData = new HashMap<String, List<String>>();
	public Map<String, List<String>> yearToDataPlayoffs = new HashMap<String, List<String>>();
	
	public int getRegSeasonMapSize() {
		return yearToData.size();
	}
	
	public YearlyCareerData(String link) {
		try {
			if(!link.contains("https://www.pro-football-reference.com/players/")) {
				link = "https://www.pro-football-reference.com/players/" + link.substring(0, 1).toUpperCase() + "/" + link;
			}
			
			if(!link.contains(".htm")) {
				link += ".htm";
			}
			id = link.substring("https://www.pro-football-reference.com/players/".length() + 2, link.indexOf(".htm"));
			Document playerPage = Jsoup.connect(link).get();
			
			Document content = Jsoup.parse(playerPage.select("#content").html().replaceAll("<!--", "").replaceAll("-->", ""));
			this.page = playerPage;
			
			
			long startTime = System.currentTimeMillis();
			
			name = getNameFromDoc(playerPage);
			position = getPositionFromDoc(playerPage);
			if(position.isBlank()) position = getPositionFromFirstTable(content);
			handedness = getHandednessFromDoc(playerPage);
			setHeightAndWeightFromDoc(playerPage);
			setMapData(content, yearToData, position, false);
			setMapData(content, yearToDataPlayoffs, position, true);
			combineMeasurements = getCombine(content);
					
			//get total time used
			long endTime = System.currentTimeMillis();
			
			long timeConsumed = endTime - startTime;
			
			//get time needed to delay to keep speed optimal
			Main.GAME_TIME_USED = timeConsumed;
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public YearlyCareerData(String[] ycdArr) {
		id = ycdArr[0];
		name = ycdArr[1];
		position = ycdArr[2];
		handedness = ycdArr[3];
		heightCm = ycdArr[4];
		weightKg = ycdArr[5];
		
		List<String> cM = CoachData.parseList(ycdArr[6].replace("[", "").replace("]", ""));
		if (cM == null) {
			combineMeasurements = null;
		} else {
//			combineMeasurements = (String[]) cM.toArray(); // because we don't care about combine measurements for this data, we can just ignore this
			combineMeasurements = null;
		}
		
		yearToData = CoachData.getMapFromString(ycdArr[7]); // because having extra elements isn't problematic, we just can have it be the highest
		yearToDataPlayoffs = CoachData.getMapFromString(ycdArr[8]);
	}
		
	
	
	public String getHandedness() {
		return handedness;
	}
	
	public int getHandednessAsInt() {
		return (handedness.equalsIgnoreCase("Right") || handedness.equalsIgnoreCase("Handedness absent")) ? 1 : 0;
	}

	public void setHandedness(String handedness) {
		this.handedness = handedness;
	}

	public String getHeightCm() {
		return heightCm;
	}

	public void setHeightCm(String heightCm) {
		this.heightCm = heightCm;
	}

	public String getWeightKg() {
		return weightKg;
	}

	public void setWeightKg(String weightKg) {
		this.weightKg = weightKg;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String toString(String delim) {
		List<String> combineList;
		if(combineMeasurements == null) combineList = null;
		else combineList = Arrays.asList(combineMeasurements);
		return id + delim + name + delim + position + delim + handedness + delim + heightCm + delim + weightKg + delim + combineList + delim + yearToData.toString() + delim + yearToDataPlayoffs.toString();
	}
	
	private String getNameFromDoc(Document doc) {
		String name = doc.select("#meta > div > h1").text();
		if(name != null) {
			return name;
		} else {
			return "Failed to collect name";
		}
	}
	
	
	private String getPositionFromFirstTable(Document doc) {
		//doc.select("div:containsOwn(all)");
		String statsID = doc.selectFirst(".table_wrapper.tabbed").id();
		statsID = statsID.replace("all_", "");
		return doc.selectFirst("#div_"+statsID+" table tbody tr td[data-stat=pos]").text();
	}
	
	private String getPositionFromDoc(Document doc) {
		String infoDiv = doc.select("#meta").select("p:has(strong:contains(Position))").text().replace("Position:", "").replace("Throws:", "").trim();
		
		String position = infoDiv.split("  ")[0];
		
		return position;
	}
	
	//ONLY QBs have handedness datapoint, if handedness not present, return handedness not present
	//ONLY RETURN NULL WHEN RELEVANT DATA FOR NN not present, otherwise just mark as absent
	private String getHandednessFromDoc(Document doc) {
		String infoDiv = doc.select("#meta").select("p:has(strong:contains(Position))").text();
		if(infoDiv.contains("Throws:")) {
			return infoDiv.replace("Position:", "").replace("Throws:", "").trim().split("  ")[1];
		} else {
			return "Handedness absent";
		}
	}
	
	private void setHeightAndWeightFromDoc(Document doc) {
		Element e = doc.selectFirst("#meta p:has(span:contains(-)):has(span:contains(lb))");
		e.select("span").remove();
		String[] arr = e.text().replaceAll("&nbsp;", "").replaceAll(",", "").trim().replace("(", "").replace(")", "").replace("cm", "").replace("kg", "").split(" ");
		heightCm = arr[0];
		weightKg = arr[1];
	}
	
//	private String getWeightFromDoc(Document doc) {
//		Element e = doc.selectFirst("#meta p:has(span)");
//		e.select("span").remove();
//		System.out.println(e);
//		return e.text().replaceAll("&nbsp;", "").replaceAll(",", "").trim().replace("(", "").replace(")", "").replace("cm", "").replace("kg", "").split(" ")[1];
//	}
	
	
	
	public static void setMapData(Document doc, Map<String, List<String>> map, String position, boolean isPlayoffs) {
		
		String playoffs = "";
		if(isPlayoffs) playoffs = "_playoffs";
		
		if(position.contains("-") && !position.equals("H-B")) position = position.substring(0, position.indexOf("-"));
		
		if(position.equals("QB")) {
			forMain = "passing";
			//go through both rushing and receiving and passing, do passing first and get av bc av not in rushing
			for(Element tr : doc.select("#div_passing"+playoffs+" table tbody tr")) { //d.select("#all_defense table tbody tr") use this for playoff and reg season intermix
				if (!tr.select("td[data-stat=age]").text().isBlank() && !tr.select("th[data-stat=year_id]").text().replaceAll("[^0-9]", "").equals("2023")) {
					String year = tr.select("th[data-stat=year_id]").text().replaceAll("[^0-9]", "");
					if (!map.containsKey(year) && year != null) {
						map.put(year, new ArrayList<String>());
					}
					setStandardDataMap(map, tr, isPlayoffs);
					setPassingDataMap(map, tr, isPlayoffs);
				}
			}
			
			for(Element tr : doc.select("#div_rushing_and_receiving"+playoffs+" table tbody tr")) {
				// right here check if age is a value
				if (!tr.select("td[data-stat=age]").text().isBlank() && !tr.select("th[data-stat=year_id]").text().replaceAll("[^0-9]", "").equals("2023")) {
					String year = tr.select("th[data-stat=year_id]").text().replaceAll("[^0-9]", "");
					//doing this because don't want to create a new year data row where there isn't a pass data
					if (map.containsKey(year)) {
						setRushDataMap(map, tr);
					}
					
				}
			}
			//TODO may have instance of passing data and rushing data not matching in years McCoJo01
			//a year row that was present for passing can be missing for rushing meaning that when iterating, it wont even
			//set the rushing data in the map to null, because we just iterate on any existing year row
			//everything would be fine if there are passing year rows that aren't present in rushing year rows,
			//but problems arise when rushing year rows are added when there is no passing year data already present for that year
			//so now we have rushing data in the place of passing data
			
			//solutions: track passing years in a hashset, if a rushing year is found that isn't in the hashset, skip adding this
			//year row data to the years map
			
			//you can do the solution above by removing the map doesnt contain key check and instead just get each year already
			//in the map added by the passing data and add the rushing data if the value at that year key isn't null
			//for each year, only if map already contains a key at this year > then add this row's rushing data, otherwise skip this...
			//year/row of rushing data as for QB, passing data is more relevant
			//then after this, go through map again and fill all missing rush values with null if size of list for this year isn't the right size
			
			//TODO !! NOTE THAT THE COLLECTED PLAYER DATAS may have QBs with some years missing Rush data leading to year list sizes
			//to not all be the same, if this is the case, just equalize the sizes of reg season and playoff data to their respective expected value
			
		} else if (anyStringEquals(position, "XR 87 X-WR TR FB' H-B F 0 Z X RB HB TB FB LH RH BB B WB WR RWR LWR FL TE SE".split(" "))) {
			String order = "rushing_and_receiving";
			Elements data =  doc.select("#div_"+order+playoffs);
			if(data.isEmpty()) {
				order = "receiving_and_rushing";
				data = doc.select("#div_"+order+playoffs);
			}
			forMain = order;
					 //if age isnt present this row is invalid
			for(Element tr : data.select("table tbody tr")) {
				// right here check if age is a value
				if (!tr.select("td[data-stat=age]").text().isBlank() && !tr.select("th[data-stat=year_id]").text().replaceAll("[^0-9]", "").equals("2023")) {
					String year = tr.select("th[data-stat=year_id]").text().replaceAll("[^0-9]", "");
					
					if (!map.containsKey(year) && year != null) {
						map.put(year, new ArrayList<String>());
					}
					setStandardDataMap(map, tr, isPlayoffs);
					setOffensiveDataMap(map, tr);
				}
			}
			
		} else if (anyStringEquals(position, "EDGE SCB 3CB ILN E ` NLB NCB WE CB-KR XDB OE XS 0LB XCB H D RDB LDB NE 33 MCB MOLB MILB TED MO FSW WIL ILBV 2 NB ROV DS WC NG RC JACK RE LE DL LDE DE LDT NDB DT NT UT MG DG RDT RDE END JLB LOLB RUSH BLB OLB LLB LILB WILL ILB SLB MLB MIKE WLB RILB RLB ROLB SAM LB LCB CB RCB SS FS LDH RDH S RS DB SAF".split(" "))) {
			forMain = "defense";
			for(Element tr : doc.select("#div_defense"+playoffs+" table tbody tr")) { //d.select("#all_defense table tbody tr") use this for playoff and reg season intermix
				if (!tr.select("td[data-stat=age]").text().isBlank() && !tr.select("th[data-stat=year_id]").text().replaceAll("[^0-9]", "").equals("2023")) {
					String year = tr.select("th[data-stat=year_id]").text().replaceAll("[^0-9]", "");
					if (!map.containsKey(year) && year != null) {
						map.put(year, new ArrayList<String>());
					}
					setStandardDataMap(map, tr, isPlayoffs);
					setDefensiveDataMap(map, tr);
				}
			}
			
		}
		
	}
	
	private static String[] getCombine(Document doc) {
		Element nullCheck = doc.selectFirst("#all_combine");
		if(nullCheck == null) return null;
		
		
		String tempString = nullCheck.html();
		Document tempDoc = Jsoup.parse(tempString.replace("<!--\n", "").replace("\n-->", ""));
		Element combine = tempDoc.selectFirst("#div_combine table tbody tr"); //TODO combine needs to get html then remove comments
		
		
		String[] ret = new String[6];
		
		int i = 0;
		for (String s : "forty_yd bench_reps broad_jump shuttle cone vertical".split(" ")) {
			Element e = combine.selectFirst("td[data-stat=" + s + "]");
			if (e != null) {
				ret[i] = e.text();
			} else {
				ret[i] = null;
			}
			i++;
		}
		
		return ret;
	}
	
	private static void setStandardDataMap(Map<String, List<String>> map, Element tr, boolean isPlayoffs) {
		addDataToMapList(map, tr, "age");	
		if(!isPlayoffs) addDataToMapList(map, tr, "av");
		addDataToMapList(map, tr, "g");
		addDataToMapList(map, tr, "gs");
	}
	
	private static void setPassingDataMap(Map<String, List<String>> map, Element tr, boolean isPlayoffs) {
		addDataToMapList(map, tr, "qb_rec");
		addDataToMapList(map, tr, "pass_cmp");
		addDataToMapList(map, tr, "pass_att");
		addDataToMapList(map, tr, "pass_yds");
		addDataToMapList(map, tr, "pass_td");
		addDataToMapList(map, tr, "pass_int");
		addDataToMapList(map, tr, "pass_first_down");
		addDataToMapList(map, tr, "pass_success");
		addDataToMapList(map, tr, "pass_long");
		addDataToMapList(map, tr, "pass_rating");
		if(!isPlayoffs) addDataToMapList(map, tr, "qbr");
		addDataToMapList(map, tr, "pass_sacked");
		addDataToMapList(map, tr, "pass_sacked_yds");
		addDataToMapList(map, tr, "comebacks");
		addDataToMapList(map, tr, "gwd");
	}
	
	private static void setDefensiveDataMap(Map<String, List<String>> map, Element tr) {
		addDataToMapList(map, tr, "def_int");
		addDataToMapList(map, tr, "def_int_yds");
		addDataToMapList(map, tr, "def_int_td");
		addDataToMapList(map, tr, "pass_defended");
		addDataToMapList(map, tr, "fumbles_forced");
		addDataToMapList(map, tr, "fumbles_rec");
		addDataToMapList(map, tr, "fumbles_rec_td");		addDataToMapList(map, tr, "sacks");
		addDataToMapList(map, tr, "qb_hits");
		addDataToMapList(map, tr, "tackles_combined");
		addDataToMapList(map, tr, "tackles_solo");
		addDataToMapList(map, tr, "tackles_assists");
		addDataToMapList(map, tr, "tackles_loss");
		addDataToMapList(map, tr, "safety_md");
	}
	
	private static void setOffensiveDataMap(Map<String, List<String>> map, Element tr) {	
		addDataToMapList(map, tr, "targets");
		addDataToMapList(map, tr, "rec");
		addDataToMapList(map, tr, "rec_yds");
		addDataToMapList(map, tr, "rec_td");
		addDataToMapList(map, tr, "rec_first_down");
		addDataToMapList(map, tr, "rec_success");
		addDataToMapList(map, tr, "rec_long");
		
		setRushDataMap(map,tr);
		
	}
	
	private static void setRushDataMap(Map<String, List<String>> map, Element tr) {
		addDataToMapList(map, tr, "rush_att");
		addDataToMapList(map, tr, "rush_yds");
		addDataToMapList(map, tr, "rush_td");
		addDataToMapList(map, tr, "rush_first_down");
		addDataToMapList(map, tr, "rush_success");
		addDataToMapList(map, tr, "rush_long");
		addDataToMapList(map, tr, "fumbles");
	}
	
	private static void addDataToMapList(Map<String, List<String>> map, Element tr, String data) {
		String year = tr.select("th[data-stat=year_id]").text().replaceAll("[^0-9]", "");
		Element e = tr.selectFirst("td[data-stat="+data+"]");
		if(e != null) {
			map.get(year).add(e.text());
		} else {
			map.get(year).add(null);
		}
	}
	
    private static boolean anyStringEquals(String word, String[] stringList) {
        for (String str : stringList) {
            if (word.equals(str)) {
                return true;
            }
        }
        return false;
    }
	
}
