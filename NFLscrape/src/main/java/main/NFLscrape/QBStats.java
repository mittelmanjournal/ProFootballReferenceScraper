package main.NFLscrape;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;

public class QBStats {
	// true if double false if int
	public static void addStat(String stat, List<Double> stats, Element e, boolean trueIfParseByDouble) {
		Element eStat = e.selectFirst(stat);
		double parsedStat;
		if (trueIfParseByDouble)
			parsedStat = Double.parseDouble(eStat.text());
		else
			parsedStat = Integer.parseInt(eStat.text());
		stats.add(parsedStat);
	}

	public static void main(String[] args) {
		String url = "https://www.pro-football-reference.com/players/C/CoucTi00.htm";

		try {
			// Fetch the HTML of the website
			Document document = Jsoup.connect(url).get();

			// Find the element with id "all_passing" and print its ID
			Element passingTable = document.getElementById("all_passing");
			Elements tbody = passingTable.select("tbody");
			Elements trs = tbody.select("tr");

			Element runRecTable = document.getElementById("all_rushing_and_receiving");
			Elements tbodyRunRec = runRecTable.select("tbody");
			Elements trsRunRec = tbodyRunRec.select("tr");
			System.out.println(trsRunRec);
			
			List<List<Double>> qbData = new ArrayList<List<Double>>();
			for (Element tr : trs) {
				//System.out.println(tr);
				ArrayList<Double> yearlyData = new ArrayList<Double>();

				addStat("[data-stat=year_id]", yearlyData, tr, false);
				addStat("[data-stat=age]", yearlyData, tr, false);
				addStat("[data-stat=g]", yearlyData, tr, false);
				
				//this one isn't compatible with addStat function because the func gets text within element
				//that has data-stat param attribute, this one wants the text within the attribute csk
				Element startingWinRate = tr.selectFirst("[data-stat=qb_rec]");						
				double startingWinRateAsNum = Double.parseDouble(startingWinRate.attr("csk"));
				yearlyData.add(startingWinRateAsNum);
				
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
			for(Element tr : trsRunRec) {
				List<Double> dataList = qbData.get(i);
				if (Integer.parseInt(tr.selectFirst("[data-stat=year_id]").text()) == dataList.get(0)) {
					dataList.add(Double.parseDouble(tr.selectFirst("[data-stat=rush_att]").text()));
					dataList.add(Double.parseDouble(tr.selectFirst("[data-stat=rush_yds]").text()));
					dataList.add(Double.parseDouble(tr.selectFirst("[data-stat=rush_td]").text()));
					dataList.add(Double.parseDouble(tr.selectFirst("[data-stat=fumbles]").text()));
				}
				i++;
			}
			System.out.println("[ Year,   Age,  GmPl, Starting Winrate %]");
			System.out.println(qbData);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
