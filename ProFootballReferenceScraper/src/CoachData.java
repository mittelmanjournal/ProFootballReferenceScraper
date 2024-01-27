package main2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class CoachData {
	private String id;

	Map<String, List<String>> yearToCoachResults;
	Map<String, List<String>> yearToTeamResults; // ONLY FOR HC because coach results only include HC
	
	public CoachData(String[] elements) {
		id = elements[0];
		yearToCoachResults = getMapFromString(elements[1]);
		yearToTeamResults = null;//getMapFromString(elements[2]);
		
	}
	
	public static HashMap<String, List<String>> getMapFromString(String inputString) {
		
		HashMap<String, List<String>> resultMap = new HashMap<>();
		if (inputString == null || inputString.equals("null"))
			return resultMap;
		inputString = inputString.replace("{", "").replace("}", "");
		String[] arr = inputString.split("], ");
		for (String yearStats : arr) {
			yearStats = yearStats.replace("[", "").replace("]", "");
			if (!yearStats.isBlank()) {
				String[] keyValPair = yearStats.split("=");
				resultMap.put(keyValPair[0], parseList(keyValPair[1]));
			}
		}

		return resultMap;
	}

	public static List<String> standardize(List<String> inputList, int intendedLen){
		if(inputList == null) inputList = new ArrayList<String>();
		
		while(inputList.size() < intendedLen) {
			inputList.add("0");
		}
		return inputList;
	}
	
	public static List<String> parseList(String inputString) {
		
        List<String> resultList = new ArrayList<>();
        if(inputString.equals("null") || inputString == null) return resultList;
        // Split the input string by commas and trim spaces
        String[] valuesArray = inputString.split(",");

        for (String value : valuesArray) {
        	if(value == null || value.trim().equals("null")) {
        		value = "0";
        	}
            // Include empty strings in the list
            resultList.add(value.trim());
        }

        return resultList;
    }

	public CoachData(String id) {
		yearToCoachResults = new HashMap<String, List<String>>();
		yearToTeamResults = new HashMap<String, List<String>>();
		
		Document coachPage = null;
		this.id = id;
		long startTime = 0;
		try {
			coachPage = Jsoup.connect("https://www.pro-football-reference.com/coaches/"+id+".htm").get();
			startTime = System.currentTimeMillis();
		} catch(IOException e) {
			e.printStackTrace();
		}
		
		
		setMap(coachPage, yearToCoachResults, Jsoup.parse(coachPage.select("#content").html().replace("<!--\n", "").replace("\n-->", "")).select("#all_coaching_results #div_coaching_results #coaching_results tbody tr"), "g wins losses ties win_loss_perc srs_total srs_offense srs_defense g_playoffs wins_playoffs losses_playoffs rank_team".split(" "));
		setMap(coachPage, yearToTeamResults, Jsoup.parse(coachPage.select("#all_coaching_ranks").html().replace("<!--\n", "").replace("\n-->", "")).select("#div_coaching_ranks #coaching_ranks tbody tr"), "coordinator_type rank_win_percentage rank_takeaway_giveaway rank_points_diff rank_yds_diff rank_off_yds rank_off_pts rank_off_turnovers rank_off_rush_att rank_off_rush_yds rank_off_rush_td rank_off_rush_yds_per_att rank_off_fumbles_lost rank_off_pass_att rank_off_pass_yds rank_off_pass_td rank_off_pass_int rank_off_pass_net_yds_per_att rank_def_yds rank_def_pts rank_def_turnovers rank_def_rush_att rank_def_rush_yds rank_def_rush_td rank_def_rush_yds_per_att rank_def_fumbles_rec rank_def_pass_att rank_def_pass_yds rank_def_pass_td rank_def_pass_int rank_def_pass_net_yds_per_att".split(" "));
		
		long endTime = System.currentTimeMillis();
		
		Main.GAME_TIME_USED = endTime - startTime;
		
	}
	
	public String toString(String d) {
		return id + d + yearToCoachResults + d + yearToTeamResults;
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	
	Elements getYearRows(Document doc, String coachResultsOrTeamRanks) {
		if(coachResultsOrTeamRanks.equalsIgnoreCase("coach")) {
			return doc.select("#all_coaching_results #div_coaching_results #coaching_results tbody tr");
		} else if(coachResultsOrTeamRanks.equalsIgnoreCase("team")) {
			return Jsoup.parse(doc.select("#all_coaching_ranks").html().replace("<!--\n", "").replace("\n-->", "")).select("#div_coaching_ranks #coaching_ranks tbody tr");
		}
		return null;		
	}
	
	private static void setCoachMap(Document doc, Map<String, ArrayList<String>> map) {
		// assume map empty
		for (Element tr : doc.select("#all_coaching_results #div_coaching_results #coaching_results tbody tr")) {
			if (!tr.hasClass("thead")) {
				String year = tr.select("th[data-stat=year_id]").text();
				map.put(year, new ArrayList<String>());

				String[] elementsToCollect = "year_id g wins losses ties win_loss_perc srs_total srs_offense srs_defense g_playoffs wins_playoffs losses_playoffs rank_team".split(" ");

				for (String elem : elementsToCollect) {
					map.get(year).add(tr.select("td[data-stat=" + elem + "]").text());
				}
			}
		}
	}
	
	private static void setTeamMap(Document doc, Map<String, ArrayList<String>> map) {
		// assume map empty
		for (Element tr : Jsoup.parse(doc.select("#all_coaching_ranks").html().replace("<!--\n", "").replace("\n-->", "")).select("#div_coaching_ranks #coaching_ranks tbody tr")) {
			if(!tr.hasClass("thead")) {
				String year = tr.select("th[data-stat=year_id]").text();
				map.put(year, new ArrayList<String>());
				
				String[] elementsToCollect = "year_id coordinator_type rank_win_percentage rank_takeaway_giveaway rank_points_diff rank_yds_diff rank_off_yds rank_off_pts rank_off_turnovers rank_off_rush_att rank_off_rush_yds rank_off_rush_td rank_off_rush_yds_per_att rank_off_fumbles_lost rank_off_pass_att rank_off_pass_yds rank_off_pass_td rank_off_pass_int rank_off_pass_net_yds_per_att rank_def_yds rank_def_pts rank_def_turnovers rank_def_rush_att rank_def_rush_yds rank_def_rush_td rank_def_rush_yds_per_att rank_def_fumbles_rec rank_def_pass_att rank_def_pass_yds rank_def_pass_td rank_def_pass_int rank_def_pass_net_yds_per_att".split(" ");
				
				for(String elem : elementsToCollect) {
					map.get(year).add(tr.select("td[data-stat=" + elem + "]").text());
				}
			}
		}
	}
	
	private static void setMap(Document doc, Map<String, List<String>> yearToCoachResults2, Elements yearStatlines, String[] elementsToCollect) {
		// assume map empty
		for (Element tr : yearStatlines) {
			if(!tr.hasClass("thead")) {
				String year = tr.select("th[data-stat=year_id]").text();
				yearToCoachResults2.put(year, new ArrayList<String>());
				
				for(String elem : elementsToCollect) {
					yearToCoachResults2.get(year).add(tr.select("td[data-stat=" + elem + "]").text());
				}
			}
		}
	}
}
