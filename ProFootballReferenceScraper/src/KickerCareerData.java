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

public class KickerCareerData {
	private String id;
	private String name;
	private String heightCm;
	private String weightKg;
	
	//year to list of data including age, and stats (for QB stats are pass and rush, for Run/Rec stats are just rush and rec, and for defender stats are just defense
	public Map<String, List<String>> yearToData = new HashMap<String, List<String>>();
	public Map<String, List<String>> yearToDataPlayoffs = new HashMap<String, List<String>>();
	
	public int getRegSeasonMapSize() {
		return yearToData.size();
	}
	
	public KickerCareerData(String[] kcdArr) {
		id = kcdArr[0];
		name = kcdArr[1];
		heightCm = kcdArr[2];
		weightKg = kcdArr[3];
		
		yearToData = CoachData.getMapFromString(kcdArr[4]);
		yearToDataPlayoffs = CoachData.getMapFromString(kcdArr[5]);
	}
	
	public KickerCareerData(String link) {
		try {
			if(!link.contains("https://www.pro-football-reference.com/players/")) {
				link = "https://www.pro-football-reference.com/players/" + link.substring(0, 1).toUpperCase() + "/" + link;
			}
			if(!link.contains(".htm")) {
				link += ".htm";
			}
			id = link.substring("https://www.pro-football-reference.com/players/".length() + 2, link.indexOf(".htm"));
			Document playerPage = Jsoup.connect(link).get();
			
			long startTime = System.currentTimeMillis();
			
			name = getNameFromDoc(playerPage);
			setHeightAndWeightFromDoc(playerPage);
			setMapData(playerPage, yearToData, false);
			setMapData(playerPage, yearToDataPlayoffs, true);
					
			//get total time used
			long endTime = System.currentTimeMillis();
			
			long timeConsumed = endTime - startTime;
			
			//get time needed to delay to keep speed optimal
			Main.GAME_TIME_USED = timeConsumed;
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public String toString(String delim) {
		return id + delim + name + delim + heightCm + delim + weightKg + delim + yearToData.toString() + delim + yearToDataPlayoffs.toString();
	}
	
	private String getNameFromDoc(Document doc) {
		String name = doc.select("#meta > div > h1").text();
		if(name != null) {
			return name;
		} else {
			return "Failed to collect name";
		}
	}
	
	private void setHeightAndWeightFromDoc(Document doc) {
		Element e = doc.selectFirst("#meta p:has(span:contains(-)):has(span:contains(lb))");
		if(e == null) {
			heightCm = "Failed to collect height";
			weightKg = "Failed to collect weight";
		} else {
			e.select("span").remove();
			String[] arr = e.text().replaceAll("&nbsp;", "").replaceAll(",", "").trim().replace("(", "").replace(")", "").replace("cm", "").replace("kg", "").split(" ");
			heightCm = arr[0];
			weightKg = arr[1];
		}		
	}
	
	public static void setMapData(Document doc, Map<String, List<String>> map, boolean isPlayoffs) {
		
		String playoffs = "";
		if(isPlayoffs) playoffs = "_playoffs";
		
		for(Element tr : doc.select("#div_kicking"+playoffs+" table tbody tr")) { //d.select("#all_defense table tbody tr") use this for playoff and reg season intermix
			if (!tr.select("td[data-stat=age]").text().isBlank() && !tr.select("th[data-stat=year_id]").text().replaceAll("[^0-9]", "").equals("2023")) {
				String year = tr.select("th[data-stat=year_id]").text().replaceAll("[^0-9]", "");
				if (!map.containsKey(year) && year != null) {
					map.put(year, new ArrayList<String>());
				}
				setStandardDataMap(map, tr, isPlayoffs);
				setKickerDataMap(map, tr);
			}
		}
		
	}
	
	private static void setStandardDataMap(Map<String, List<String>> map, Element tr, boolean isPlayoffs) {
		addDataToMapList(map, tr, "age");	
		if(!isPlayoffs) addDataToMapList(map, tr, "av");
		addDataToMapList(map, tr, "g");
		addDataToMapList(map, tr, "gs");
	}
	
	private static void setKickerDataMap(Map<String, List<String>> map, Element tr) {
		addDataToMapList(map, tr, "fga");
		addDataToMapList(map, tr, "fgm");
		addDataToMapList(map, tr, "fga1");
		addDataToMapList(map, tr, "fgm1");
		addDataToMapList(map, tr, "fga2");
		addDataToMapList(map, tr, "fgm2");
		addDataToMapList(map, tr, "fga3");
		addDataToMapList(map, tr, "fgm3");
		addDataToMapList(map, tr, "fga4");
		addDataToMapList(map, tr, "fgm4");
//		addDataToMapList(map, tr, "fga1");
//		addDataToMapList(map, tr, "fgm1"); //THIS WAS UNINTENTIONAL
		addDataToMapList(map, tr, "fga5");
		addDataToMapList(map, tr, "fgm5");
		addDataToMapList(map, tr, "fg_long");
		addDataToMapList(map, tr, "xpa");
		addDataToMapList(map, tr, "xpm");
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
	
	public String getId() {
		return id;
	}
}
