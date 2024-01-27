package main2;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.ToIntFunction;

import org.jsoup.nodes.Element;

public class NeuralNetworkInput {
	// TODO solution for per game map could either be note the last game in each
	// season (superbowl) and go through every game in a player's submap, if the
	// game happened after the superbowl of the previous year (exclusive) and before
	// the current game you are collecting data for (exclusive) of the current year then accumulate the stats
	// accumulated in this game
	
	//ex of collecting data for one game for one player: iterate over all player's career games, if (end of last year) < curGameDate < (game you are collecting data for) accumulate data to already existing totals from yearly data (yearly data also considers previous years' playoffs performances)
	
	//A different solution could be adding another String layer to the map which would represent games by a map of {String year > {String playerID > {String gameID > List statlineThisGame}}}
	//this could be more potentially efficient but also a mindf*k to organize and compile, also due to the fact I don't have this data properly set up for kickers, I think the former solution is better
	
	public static final List<CoachData> COACH_YEARLY = getCoachData("FINAL_COACH_YEARLY_DATA.txt");
	
	public static final List<YearlyCareerData> PLAYER_YEARLY = getYearlyPlayerData("FINAL_CAREER_YEARLY_DATA.txt");
	public static final Map<String, Map<String,List<String>>> PLAYER_PER_GAME = getPerGameData("FINAL_PER-GAME_PLAYER_DATA_MAP.txt");
	
	public static final List<KickerCareerData> KICKER_YEARLY = getYearlyKickerData("FINAL_KICKER_YEARLY_DATA.txt");
	public static final Map<String, Map<String,List<String>>> KICKER_PER_GAME = getPerGameData("FINAL_PER-GAME_KICKER_DATA_MAP.txt");
	//2001 superbowl date to 2022 superbowl date
	public static final String[] SEASON_END_GAME = {"200202030nwe", "200301260rai", "200402010car", "200502060nwe", "200602050pit", "200702040chi", "200802030nwe", "200902010crd", "201002070clt", "201102060pit", "201202050nwe", "201302030sfo", "201402020den", "201502010sea", "201602070den", "201702050atl", "201802040nwe", "201902030ram", "202002020kan", "202102070tam", "202202130cin", "202302120phi"};
	
	// <0 if home wins, 1 if tie, 2 if away wins>, <week>, <year>, <weekday>,
	// <month>, <monthday>, <year>, <stadium status>, <field status>, <away score>,
	// <away record coming into game>, <per season coach averages coming into game>,
	// <linearly weighted person averages divided by games played to get game
	// averages for each player>
	private String gameID; //not nn input
	
	private String fullWeather; //maybe nn input
	private String spread; //not nn input
	private String overUnder; //not nn input

	private int winner;
	private int seasonWeek;
	private int seasonYear;

	private String dateInfo; //not nn input
	private int weekday;
	private int monthDay;
	private int month;
	private int year;

	private String timeStr; //not nn input
	private float time;
	private int militaryTime;

	private String stadiumStatus; //not nn input
	private int stadium;

	private String fieldStatus; //not nn input
	private int field;

	private int visScore;
	private String visRecordAfterGame;  //not nn input
	private int visSeasonWinsComingIntoGame;
	private int visSeasonTiesComingIntoGame;
	private int visSeasonLossesComingIntoGame;

	private double[] visCoachStats; // Coming into game
	private double[] visQBStats;
	private double[][] visRunceiverStats;
	private double[][] visDefendersStats;
	private double[] visKStats;

	private int homeScore;
	private String homeRecordAfterGame; //not nn input
	private int homeSeasonWinsComingIntoGame;
	private int homeSeasonTiesComingIntoGame;
	private int homeSeasonLossesComingIntoGame;

	private double[] homeCoachStats; // Coming into game
	private double[] homeQBStats;
	private double[][] homeRunceiverStats;
	private double[][] homeDefendersStats;
	private double[] homeKStats;

	public NeuralNetworkInput(Game game, String delim) {
		this(game.toString(delim), delim);
	}

	public NeuralNetworkInput(String game, String delim) {
		this(game.split(delim));
	}

	public NeuralNetworkInput(String[] game) {
		gameID = game[0];
		
		fullWeather = game[7];
		spread = game[8];
		overUnder = game[9];
		
		visScore = Integer.parseInt(game[11]);
		homeScore = Integer.parseInt(game[18]);
		winner = getGameOutcome(visScore, homeScore);
		
		seasonWeek = Integer.parseInt(game[1]);
		seasonYear = Integer.parseInt(game[2]);
		
		dateInfo = game[3];
		
		String[] dateInfoArr = dateInfo.replace(",", "").split(" ");
		weekday = getWeekdayAsInt(dateInfoArr[0]);
		month = getMonthAsInt(dateInfoArr[1]);
		monthDay = Integer.parseInt(dateInfoArr[2]);
		year = Integer.parseInt(dateInfoArr[3]);
		
		timeStr = game[4];
		time = parseTimeFromString(timeStr);
		militaryTime = convertToMilitaryTime(timeStr);
		
		stadiumStatus = game[5];
		stadium = getStadiumAsInt(stadiumStatus);
		
		fieldStatus = game[6];
		field = getFieldAsInt(fieldStatus);
		
		visRecordAfterGame = game[12];
		int[] visRecord = getRecord(false, visRecordAfterGame, winner); 
		visSeasonWinsComingIntoGame = visRecord[0];
		visSeasonTiesComingIntoGame = visRecord[1];
		visSeasonLossesComingIntoGame = visRecord[2];
		
		visCoachStats = getMeanCoachStatsUpToYear(game[13], seasonYear); //TODO get the average ranks when HC
		visQBStats = getMeanQBStatsUpToGame(game[14], seasonYear, gameID);
		visRunceiverStats = getMeanRunceiverStatsUpToGame(game[15], seasonYear, gameID); //difference between this and qb is that this one has multiple pids in game[15] [x,y,z]
		visDefendersStats = getMeanDefendersStatsUpToGame(game[16], seasonYear, gameID);
		visKStats = getMeanKStatsUpToGame(game[24], seasonYear, gameID);
		
		homeRecordAfterGame = game[19];
		int[] homeRecord = getRecord(true, homeRecordAfterGame, winner); 
		homeSeasonWinsComingIntoGame = homeRecord[0];
		homeSeasonTiesComingIntoGame = homeRecord[1];
		homeSeasonLossesComingIntoGame = homeRecord[2];
		
		homeCoachStats = getMeanCoachStatsUpToYear(game[20], seasonYear);
		visQBStats = getMeanQBStatsUpToGame(game[21], seasonYear, gameID);
		visRunceiverStats = getMeanRunceiverStatsUpToGame(game[22], seasonYear, gameID);
		visDefendersStats = getMeanDefendersStatsUpToGame(game[23], seasonYear, gameID);
		visKStats = getMeanKStatsUpToGame(game[25], seasonYear, gameID);
	}
	
	public String toString() {
		return getAllNonNNInputs() + "," + getAllNNInputs();
	}
	
	private String getAllNonNNInputs() {
		return gameID + "," + fullWeather + "," + spread + "," + overUnder + "," + dateInfo + "," + timeStr + "," + time + "," + stadiumStatus + "," + fieldStatus + "," + visRecordAfterGame + "," + homeRecordAfterGame;
	}
	
	private String getAllNNInputs() {
		return getAllExternalGameInfoAsString() + "," + getAllVisInfoAsString() + "," + getAllHomeInfoAsString();
	}
	
	private String getAllExternalGameInfoAsString() {
		return winner + "," + seasonWeek + "," + seasonYear + "," + weekday + "," + monthDay + "," + month + "," + year + "," + militaryTime + "," + stadium + "," + field;
	}
	
	private String getAllVisInfoAsString() {
		return visScore + "," + visSeasonWinsComingIntoGame + "," + visSeasonTiesComingIntoGame + "," + visSeasonLossesComingIntoGame + "," + arrayToString(visCoachStats) + "," + arrayToString(visQBStats) + "," + doubleArrayToString(visRunceiverStats) + "," + doubleArrayToString(visDefendersStats) + "," + arrayToString(visKStats);
	}
	
	private String getAllHomeInfoAsString() {
		return homeScore + "," + homeSeasonWinsComingIntoGame + "," + homeSeasonTiesComingIntoGame + "," + homeSeasonLossesComingIntoGame + "," + arrayToString(homeCoachStats) + "," + arrayToString(homeQBStats) + "," + doubleArrayToString(homeRunceiverStats) + "," + doubleArrayToString(homeDefendersStats) + "," + arrayToString(homeKStats);
	}
	
	public static String arrayToString(double[] array) {
        return Arrays.toString(array)
                .replace("[", "")
                .replace("]", "")
                .replace(" ", "");
    }

    public static String doubleArrayToString(double[][] doubleArray) {
    	if(doubleArray == null) return "null";
    	for(int i = 0; i < doubleArray.length; i++)
    		for(int j = 0; j < doubleArray[i].length; j++)
    			System.out.println(doubleArray[i][j]);
    	
        StringBuilder result = new StringBuilder();
        
        for (double[] row : doubleArray) {
            result.append(arrayToString(row)).append(",");
        }

        // Remove the trailing comma
        if (result.length() > 0) {
            result.setLength(result.length() - 1);
        }

        return result.toString();
    }
	
	private double[] getMeanQBStatsUpToGame(String pid, int seasonYear, String gameID) {
		YearlyCareerData player = null;
		for(YearlyCareerData ycd : PLAYER_YEARLY) {
			if(ycd.getId().equals(pid)) {
				player = ycd;
				break;
			}
		}
		
		double[] ret = new double[27];
		ret[0] = player.getHandednessAsInt(); //0 for left hand 1 for right hand
		ret[1] = Integer.parseInt(player.getHeightCm()); // height
		ret[2] = Integer.parseInt(player.getWeightKg()); // weight
		ret[3] = Integer.parseInt(player.yearToData.get(""+seasonYear).get(0)); // age
		
		int seasonCount = 0;
		//you could either get totals or get season averages plus seasons coached here
		for(Entry<String, List<String>> yrDataPair : player.yearToData.entrySet()) {
			if(Integer.parseInt(yrDataPair.getKey()) < seasonYear) {
				seasonCount++;
				//TODO you may need to debug a potential error below which sees the parsing fail because "" is the parameter
				ret[4] += getDouble(yrDataPair.getValue().get(1)); // av
				ret[5] += getDouble(yrDataPair.getValue().get(2)); // games
				System.out.println(ret[5]);
//				ret[6] += getDouble(yrDataPair.getValue().get(3)); // games started, unneccessary because w l and t below account for it
				System.out.println(yrDataPair.getValue().get(4));
				String[] recordArr = yrDataPair.getValue().get(4).split("-");
				if (recordArr.length == 3) {
					ret[6] += getDouble(recordArr[0]); // wins
					ret[7] += getDouble(recordArr[1]); // losses
					ret[8] += getDouble(recordArr[2]); // ties
				}
				ret[9] += getDouble(yrDataPair.getValue().get(5)); //cmp
				ret[10] += getDouble(yrDataPair.getValue().get(6)); //att
				ret[11] += getDouble(yrDataPair.getValue().get(7)); //yds
				ret[12] += getDouble(yrDataPair.getValue().get(8)); //tds
				ret[13] += getDouble(yrDataPair.getValue().get(9)); //int
				ret[14] += getDouble(yrDataPair.getValue().get(10)); //1st down passing (game average)
				ret[15] += getDouble(yrDataPair.getValue().get(11)); //passing success chance (season average)
				ret[16] += getDouble(yrDataPair.getValue().get(12));  //lng (season average)
				ret[17] += getDouble(yrDataPair.getValue().get(15)); //total sacks
				ret[18] += getDouble(yrDataPair.getValue().get(16)); //total sack yards
				ret[19] += getDouble(yrDataPair.getValue().get(18)); //game winning drives
				//sometimes qbs don't have these elements in the list at all, for this edge case, do if check
				if (yrDataPair.getValue().size() > 19) {
					ret[20] += getDouble(yrDataPair.getValue().get(19)); // rush att
					ret[21] += getDouble(yrDataPair.getValue().get(20)); // rush yards
					ret[22] += getDouble(yrDataPair.getValue().get(21)); // rush td
					ret[23] += getDouble(yrDataPair.getValue().get(22)); // rush 1st down
					ret[24] += getDouble(yrDataPair.getValue().get(23)); // rush success chance (season average)
					ret[25] += getDouble(yrDataPair.getValue().get(24)); // rush lng (season average)
					ret[26] += getDouble(yrDataPair.getValue().get(25)); // fmb
				} else {
					for (int i = 20; i <= 26; i++) {
						ret[i] += 0;
					}
				}
				
				//started rec is at index 4, you should parse out of this 3 vals, wins, losses ties, and accumulate them and input totals into nn, also maybe remove games started because the total of these 3 represent games started
				
			}
		}
		
		for(Entry<String, List<String>> yrDataPair : player.yearToDataPlayoffs.entrySet()) {
			if(Integer.parseInt(yrDataPair.getKey()) < seasonYear) {
				
				
//				2001=[ 24, 3,  3,   3-0,  60,  97,  572,  1,   1, 29, 52.4,  29,  77.3,   5,    36,   1,   2,    8,   22,   1,   3,  37.5,    6,   1] playoffs
//					  age, G, GS, QBRec, cmp, att, pYds, td, int, 1d, succ, pLng, rate, sks, skYds, 4QC, GWD, ratt, ryds, rTD, r1d, rSucc, rLng, fmb
//				      0    1  2   3      4    5    6     7   8    9   10    11    12    13   14     15   16   17    18    19   20   21     22    23
				
//				2001=[ 24, 12, 15, 14, 11-3-0, 264, 413, 2843, 18,  12, 145, 45.6,  91, 86.5,    , 41,   216,   3,   3,   36,   43,   0,   7, 25.0,   12,  12] regular season
//				      age, av,  G, GS,  QBrec, cmp, att,  yds, td, int,  1d, succ, lng, rate, QBR, sk, skYds, 4QC, GWD, ratt, ryds, rTD, r1d, succ, rLng, fmb
//				      0    1    2  3    4      5    6     7    8   9     10  11    12   13    14   15  16     17   18   19    20    21   22   23    24    25
				
				ret[5] += getDouble(yrDataPair.getValue().get(1)); // games
				String[] recordArr = yrDataPair.getValue().get(3).split("-"); //ignore .get(2) because thats gs
				ret[6] += getDouble(recordArr[0]); //wins
				ret[7] += getDouble(recordArr[1]); //losses
				//NO TIES IN PLAYOFFS
				ret[9] += getDouble(yrDataPair.getValue().get(4)); //cmp
				ret[10] += getDouble(yrDataPair.getValue().get(5)); //att
				ret[11] += getDouble(yrDataPair.getValue().get(6)); //yds
				ret[12] += getDouble(yrDataPair.getValue().get(7)); //tds
				ret[13] += getDouble(yrDataPair.getValue().get(8)); //int
				ret[14] += getDouble(yrDataPair.getValue().get(9)); //1st down passing (game average)
				//don't want to add pass success % because it will skew data weirdly
				// don't want to add pass long because it will skew data weirdly
				ret[17] += getDouble(yrDataPair.getValue().get(13)); //total sacks
				ret[18] += getDouble(yrDataPair.getValue().get(14)); //total sack yards
				ret[19] += getDouble(yrDataPair.getValue().get(16)); //game winning drives
				if (yrDataPair.getValue().size() > 17) {
					ret[20] += getDouble(yrDataPair.getValue().get(17)); // rush att
					ret[21] += getDouble(yrDataPair.getValue().get(18)); // rush yards
					ret[22] += getDouble(yrDataPair.getValue().get(19)); // rush td
					ret[23] += getDouble(yrDataPair.getValue().get(20)); // rush 1st down
					// don't want to add rush success % because it will skew data weirdly
					// don't want to add rush lng because it will skew data weirdly
					ret[26] += getDouble(yrDataPair.getValue().get(23)); // fmb
				} else {
					for (int i = 20; i <= 23; i++) {
						ret[i] += 0;
					}
					ret[26] += 0;
				}
			}
		}
		
		//accumulate gamely
		Map<String, List<String>> gidsToStats = PLAYER_PER_GAME.get(pid);
		//go through each entry, if the key in this entry is in between the start of the current season and the parameter game id then accumulate the appropriate categories
		for(Entry<String, List<String>> gidToStatline : gidsToStats.entrySet()) {
			//if(gidToStatline.getKey() > SEASON_END_GAME[seasonYear-2002] && gidToStatline.getKey() < gameID) {201802040nwe
			if(isDateOneAfterDateTwo(gidToStatline.getKey(), SEASON_END_GAME[seasonYear-2002]) && isDateOneAfterDateTwo(gameID, gidToStatline.getKey())) {
				ret[9] += getDouble(gidToStatline.getValue().get(0)); // cmp
				ret[10] += getDouble(gidToStatline.getValue().get(1)); //att
				ret[11] += getDouble(gidToStatline.getValue().get(2)); //yds
				ret[12] += getDouble(gidToStatline.getValue().get(3)); //tds
				ret[13] += getDouble(gidToStatline.getValue().get(4)); //int
				ret[17] += getDouble(gidToStatline.getValue().get(5)); //sacks
				ret[18] += getDouble(gidToStatline.getValue().get(6)); //sack yds
				ret[20] += getDouble(gidToStatline.getValue().get(9)); //rush att
				ret[21] += getDouble(gidToStatline.getValue().get(10)); //rush yds
				ret[22] += getDouble(gidToStatline.getValue().get(11)); //rush tds
				if(gidToStatline.getValue().size() >= 19) ret[26] += getDouble(gidToStatline.getValue().get(18)); //fmb
				else ret[26] += 0;
			}
		}
		
		ret[9] /= ret[5]; // cmps per game
		ret[10] /= ret[5]; //att per game
		ret[11] /= ret[5]; //yds per game
		ret[12] /= ret[5]; //tds per game
		ret[13] /= ret[5]; //ints per game
		ret[14] /= ret[5]; //1d pass attempts per game
		ret[15] /= seasonCount; //career (seasonal) average passing success chance
		ret[16] /= seasonCount; // average lng pass per season
		ret[17] /= ret[5]; // total sacks per game
		ret[18] /= ret[5]; // total sack yards lost per game
		ret[19] /= ret[5]; //do you want total gwd, gwd per season, or gwd per game? LAST ONE
		ret[20] /= ret[5]; //rush att per game
		ret[21] /= ret[5]; //rush yards per game
		ret[22] /= ret[5]; // rush td per game
		ret[23] /= ret[5]; // 1st down rushes per game
		ret[24] /= seasonCount; // rush success chance
		ret[25] /= seasonCount; // rush lng season avg
		ret[26] /= ret[5]; // fmb per game
		
		return ret;
		
	}
	
	private double[][] getMeanRunceiverStatsUpToGame(String pidList, int seasonYear, String gameID) {
		double[][] ret = new double[5][];
		
		String[] pids = pidList.replace("[", "").replace("]", "").split(",");
		
		int i = 0;
		for(String pid : pids) {
			ret[i] = getMeanRunceiverStatsUpToGameSingle(pid, seasonYear, gameID);
			i++;
		}
		
		return ret;
		
	}
	
	private double[] getMeanRunceiverStatsUpToGameSingle(String pid, int seasonYear, String gameID) {
		YearlyCareerData player = null;
		for(YearlyCareerData ycd : PLAYER_YEARLY) {
			if(ycd.getId().equals(pid)) {
				player = ycd;
				break;
			}
		}
		
		double[] ret = new double[20];
		ret[0] = Integer.parseInt(player.getHeightCm()); // height
		ret[1] = Integer.parseInt(player.getWeightKg()); // weight
		if(player.yearToData.get(""+seasonYear) != null) ret[2] = Integer.parseInt(player.yearToData.get(""+seasonYear).get(0)); // age
		
		int seasonCount = 0;
		//you could either get totals or get season averages plus seasons coached here
		for(Entry<String, List<String>> yrDataPair : player.yearToData.entrySet()) {
			if(Integer.parseInt(yrDataPair.getKey()) < seasonYear) {
				seasonCount++;
				//TODO you may need to debug a potential error below which sees the parsing fail because "" is the parameter
				
				ret[3] += getDouble(yrDataPair.getValue().get(1)); // av
				ret[4] += getDouble(yrDataPair.getValue().get(2)); // games
				ret[5] += getDouble(yrDataPair.getValue().get(3)); // games started, necessary for non-qb because w-l-t not saved
				
				//rec
				ret[6] += getDouble(yrDataPair.getValue().get(4)); //tgt want per game (per game average)
				ret[7] += getDouble(yrDataPair.getValue().get(5)); //rec g
				ret[8] += getDouble(yrDataPair.getValue().get(6)); //yds g
				ret[9] += getDouble(yrDataPair.getValue().get(7)); //td g
				ret[10] += getDouble(yrDataPair.getValue().get(8)); //1d g
				ret[11] += getDouble(yrDataPair.getValue().get(9)); //succ want per season (season average)
				ret[12] += getDouble(yrDataPair.getValue().get(10)); //lng s
				
				//rush
				ret[13] += getDouble(yrDataPair.getValue().get(11));  //ratt g
				ret[14] += getDouble(yrDataPair.getValue().get(12)); //ryds g
				ret[15] += getDouble(yrDataPair.getValue().get(13)); //rtd g
				ret[16] += getDouble(yrDataPair.getValue().get(14)); //r1d g
				ret[17] += getDouble(yrDataPair.getValue().get(15)); //rsucc s
				ret[18] += getDouble(yrDataPair.getValue().get(16)); //rlng s
				
				
				ret[19] += getDouble(yrDataPair.getValue().get(17)); //fmb g
			}
		}
		
		for(Entry<String, List<String>> yrDataPair : player.yearToDataPlayoffs.entrySet()) {
			if(Integer.parseInt(yrDataPair.getKey()) < seasonYear) {
				
//				2009=[ 23,  4, 11,  7,  54,  37, 359,  1, 19, 53.7,  29,    2,    5,   0,   0,  50.0,    5, 1  ]
//					  age, av,  g, gs, tgt, rec, yds, td, 1d, succ, lng, ratt, ryds, rtd, r1d, rsucc, rlng, fmb
//				      0    1   2    3   4    5    6    7   8   9     10   11    12    13   14   15     16    17
				
//				2009=[ 23, 1,  1,   8,   6,  44,  2,  4, 62.5,  24,     ,     ,   0,    ,      ,     ,   0]
//				      age, g, gs, tgt, rec, yds, td, 1d, succ, lng, ratt, ryds, rtd, r1d, rsucc, rlng, fmb
//					  0    1  2   3    4    5    6   7   8     9    10    11    12   13   14     15    16
				
				ret[4] += getDouble(yrDataPair.getValue().get(1)); // games
				ret[5] += getDouble(yrDataPair.getValue().get(2)); // games started, necessary for non-qb because w-l-t not saved
				
				//rec
				ret[6] += getDouble(yrDataPair.getValue().get(3)); //tgt want per game (per game average)
				ret[7] += getDouble(yrDataPair.getValue().get(4)); //rec g
				ret[8] += getDouble(yrDataPair.getValue().get(5)); //yds g
				ret[9] += getDouble(yrDataPair.getValue().get(6)); //td g
				ret[10] += getDouble(yrDataPair.getValue().get(7)); //1d g
				ret[11] += getDouble(yrDataPair.getValue().get(8)); //succ want per season (season average)
				ret[12] += getDouble(yrDataPair.getValue().get(9)); //lng s
				
				//rush
				ret[13] += getDouble(yrDataPair.getValue().get(10));  //ratt g
				ret[14] += getDouble(yrDataPair.getValue().get(11)); //ryds g
				ret[15] += getDouble(yrDataPair.getValue().get(12)); //rtd g
				ret[16] += getDouble(yrDataPair.getValue().get(13)); //r1d g
				ret[17] += getDouble(yrDataPair.getValue().get(14)); //rsucc s
				ret[18] += getDouble(yrDataPair.getValue().get(15)); //rlng s
				
				
				ret[19] += getDouble(yrDataPair.getValue().get(16)); //fmb g
			}
		}
		
		//accumulate gamely
		Map<String, List<String>> gidsToStats = PLAYER_PER_GAME.get(pid);
		//go through each entry, if the key in this entry is in between the start of the current season and the parameter game id then accumulate the appropriate categories
		for(Entry<String, List<String>> gidToStatline : gidsToStats.entrySet()) {
			//if(gidToStatline.getKey() > SEASON_END_GAME[seasonYear-2002] && gidToStatline.getKey() < gameID) {201802040nwe
			if(isDateOneAfterDateTwo(gidToStatline.getKey(), SEASON_END_GAME[seasonYear-2002]) && isDateOneAfterDateTwo(gameID, gidToStatline.getKey())) {
				//rec
				ret[6] += getDouble(gidToStatline.getValue().get(13)); // tgts g
				ret[7] += getDouble(gidToStatline.getValue().get(14)); //rec g
				ret[8] += getDouble(gidToStatline.getValue().get(15)); //yds g
				ret[9] += getDouble(gidToStatline.getValue().get(16)); //td g
				
				//rush
				ret[13] += getDouble(gidToStatline.getValue().get(9));  //ratt g
				ret[14] += getDouble(gidToStatline.getValue().get(10)); //ryds g 
				ret[15] += getDouble(gidToStatline.getValue().get(11)); //rtd g
				
				ret[19] += getDouble(gidToStatline.getValue().get(18)); //fmb g
				
			}
		}
		
		//rec
		ret[6] /= ret[4]; //tgt want per game (per game average)
		ret[7] /= ret[4]; //rec g
		ret[8] /= ret[4]; //yds g
		ret[9] /= ret[4]; //td g
		ret[10] /= ret[4]; //1d g
		ret[11] /= seasonCount; //succ want per season (season average)
		ret[12] /= seasonCount; //lng s
		
		//rush
		ret[13] /= ret[4];  //ratt g
		ret[14] /= ret[4]; //ryds g
		ret[15] /= ret[4]; //rtd g
		ret[16] /= ret[4]; //r1d g
		ret[17] /= seasonCount; //rsucc s
		ret[18] /= seasonCount; //rlng s
		
		ret[19] /= ret[4]; //fmb g
		
		return ret;
		
	}

	private double[][] getMeanDefendersStatsUpToGame(String pidList, int seasonYear, String gameID) {
		double[][] ret = new double[11][];
		
		String[] pids = pidList.replace("[", "").replace("]", "").split(",");
		
		int i = 0;
		for(String pid : pids) {
			ret[i] = getMeanDefenderStatsUpToGameSingle(pid, seasonYear, gameID);
			i++;
		}
		
		return ret;
		
	}
	
	private double[] getMeanDefenderStatsUpToGameSingle(String pid, int seasonYear, String gameID) {
		YearlyCareerData player = null;
		for(YearlyCareerData ycd : PLAYER_YEARLY) {
			if(ycd.getId().equals(pid)) {
				player = ycd;
				break;
			}
		}
		
		double[] ret = new double[17];
		ret[0] = Integer.parseInt(player.getHeightCm()); // height
		ret[1] = Integer.parseInt(player.getWeightKg()); // weight
		ret[2] = Integer.parseInt(player.yearToData.get(""+seasonYear).get(0)); // age
		//TODO FOR ALL OF THE ACCUMULATORS DON'T FORGET TO ACCUMULATE PLAYOFF STATS GIVEN BY YCD CLASS
		int seasonCount = 0;
		//you could either get totals or get season averages plus seasons coached here
		for(Entry<String, List<String>> yrDataPair : player.yearToData.entrySet()) {
			if(Integer.parseInt(yrDataPair.getKey()) < seasonYear) {
				seasonCount++;
				//TODO you may need to debug a potential error below which sees the parsing fail because "" is the parameter
				
				ret[3] += getDouble(yrDataPair.getValue().get(1)); // av
				ret[4] += getDouble(yrDataPair.getValue().get(2)); // games
				ret[5] += getDouble(yrDataPair.getValue().get(3)); // games started, necessary for non-qb because w-l-t not saved
				
				//pass defense
				ret[6] += getDouble(yrDataPair.getValue().get(4)); // ints (not implementing int yds in dataset) g
				ret[7] += getDouble(yrDataPair.getValue().get(6)); // int td/pick 6 g
				ret[8] += getDouble(yrDataPair.getValue().get(7)); //pass defended/deflected causing incomplete g
				
				//ground
				ret[9] += getDouble(yrDataPair.getValue().get(8)); //forced fumbles (ignoring FRs) g
				ret[10] += getDouble(yrDataPair.getValue().get(10)); //fumble touchdown g
				ret[11] += getDouble(yrDataPair.getValue().get(11)); //sacks g
				ret[12] += getDouble(yrDataPair.getValue().get(12)); //qb hits g
				ret[13] += getDouble(yrDataPair.getValue().get(14));  //solo tackles (ignoring combined tackles) g
				ret[14] += getDouble(yrDataPair.getValue().get(15)); //assist tackles (ignoring combined tackles) g
				ret[15] += getDouble(yrDataPair.getValue().get(16)); // tackles for loss g
				ret[16] += getDouble(yrDataPair.getValue().get(17)); // safety g
			}
		}
		
		//TODO RIGHT HERE SCRAPE AND ACCUMULATE ALL PLAYOFF DATAS BEFORE THE CURRENT SEASONYEAR
		//don't increment season counts (POTENTIAL EDGE CASE: PLAYER DOESN'T PLAY REG SEASON AND THEN PLAYS PLAYOFFS???)
		for(Entry<String, List<String>> yrDataPair : player.yearToDataPlayoffs.entrySet()) {
			if(Integer.parseInt(yrDataPair.getKey()) < seasonYear) {
				//TODO you may need to debug a potential error below which sees the parsing fail because "" is the parameter
				
				//TODO YOU COULD SEPARATE PLAYOFFS AND REGULAR STATS INSTEAD OF ADDING LIKE YOU DO BELOW, MAYBE IT WILL CHANGE RESULTS
				ret[4] += getDouble(yrDataPair.getValue().get(1)); // games
				ret[5] += getDouble(yrDataPair.getValue().get(2)); // games started, necessary for non-qb because w-l-t not saved
				
				//pass defense
				ret[6] += getDouble(yrDataPair.getValue().get(3)); // ints (not implementing int yds in dataset) g
				ret[7] += getDouble(yrDataPair.getValue().get(5)); // int td/pick 6 g
				ret[8] += getDouble(yrDataPair.getValue().get(6)); //pass defended/deflected causing incomplete g
				
				//ground
				ret[9] += getDouble(yrDataPair.getValue().get(7)); //forced fumbles (ignoring FRs) g
				ret[10] += getDouble(yrDataPair.getValue().get(9)); //fumble touchdown g
				ret[11] += getDouble(yrDataPair.getValue().get(10)); //sacks g
				ret[12] += getDouble(yrDataPair.getValue().get(11)); //qb hits g
				ret[13] += getDouble(yrDataPair.getValue().get(13));  //solo tackles (ignoring combined tackles) g
				ret[14] += getDouble(yrDataPair.getValue().get(14)); //assist tackles (ignoring combined tackles) g
				ret[15] += getDouble(yrDataPair.getValue().get(15)); // tackles for loss g
				ret[16] += getDouble(yrDataPair.getValue().get(16)); // safety g
			}
		}
		
		//accumulate gamely
		Map<String, List<String>> gidsToStats = PLAYER_PER_GAME.get(pid);
		//go through each entry, if the key in this entry is in between the start of the current season and the parameter game id then accumulate the appropriate categories
		if (gidsToStats != null)
			for(Entry<String, List<String>> gidToStatline : gidsToStats.entrySet()) {
			//if(gidToStatline.getKey() > SEASON_END_GAME[seasonYear-2002] && gidToStatline.getKey() < gameID) {201802040nwe
			if(isDateOneAfterDateTwo(gidToStatline.getKey(), SEASON_END_GAME[seasonYear-2002]) && isDateOneAfterDateTwo(gameID, gidToStatline.getKey())) {
				//pass defense
				ret[6] += getDouble(gidToStatline.getValue().get(0)); // ints (not implementing int yds in dataset) g
				ret[7] += getDouble(gidToStatline.getValue().get(2)); // int td/pick 6 g
				ret[8] += getDouble(gidToStatline.getValue().get(4)); //pass defended/deflected causing incomplete g
				
				//ground
				ret[9] += getDouble(gidToStatline.getValue().get(14)); //forced fumbles (ignoring FRs) g
				ret[10] += getDouble(gidToStatline.getValue().get(13)); //fumble touchdown g
				ret[11] += getDouble(gidToStatline.getValue().get(5)); //sacks g
				ret[12] += getDouble(gidToStatline.getValue().get(10)); //qb hits g
				ret[13] += getDouble(gidToStatline.getValue().get(7));  //solo tackles (ignoring combined tackles) g
				ret[14] += getDouble(gidToStatline.getValue().get(8)); //assist tackles (ignoring combined tackles) g
				ret[15] += getDouble(gidToStatline.getValue().get(9)); // tackles for loss g
				//safety not present as a per game (aka gamely) stat
			}
		}		
		
		//pass defense
		ret[6] /= ret[4]; // ints (not implementing int yds in dataset) g
		ret[7] /= ret[4]; // int td/pick 6 g
		ret[8] /= ret[4]; //pass defended/deflected causing incomplete g
		
		//ground
		ret[9] /= ret[4]; //forced fumbles (ignoring FRs) g
		ret[10] /= ret[4]; //fumble touchdown g
		ret[11] /= ret[4]; //sacks g
		ret[12] /= ret[4]; //qb hits g
		ret[13] /= ret[4]; //solo tackles (ignoring combined tackles) g
		ret[14] /= ret[4]; //assist tackles (ignoring combined tackles) g
		ret[15] /= ret[4]; // tackles for loss g
		ret[16] /= ret[4]; // safety g
		
		return ret;
		
	}
	
	private double[] getMeanKStatsUpToGame(String pid, int seasonYear, String gameID) {
		KickerCareerData player = null;
		for(KickerCareerData kcd : KICKER_YEARLY) {
			if(kcd.getId().equals(pid)) {
				player = kcd;
				break;
			}
		}
		
		double[] ret = new double[18];
		if(player.yearToData.get(""+seasonYear) != null) ret[0] = Integer.parseInt(player.yearToData.get(""+seasonYear).get(0)); // age
		//no height/weight because I feel like its irrelevant for kickers while age is slightly more relevant
		int seasonCount = 0;
		//you could either get totals or get season averages plus seasons coached here
		for(Entry<String, List<String>> yrDataPair : player.yearToData.entrySet()) {
			if(Integer.parseInt(yrDataPair.getKey()) < seasonYear) {
				// 2009=[ 25,  4, 16,  0,  31,  26,      ,      ,     7,     7,    13,    12,     8,     5,      ,      ,     3,     2,      53,  47, 47 ]
				//       age, av,  G, GS, FGA, FGM, fga19, fgm19, fga29, fgm29, fga39, fgm39, fga49, fgm49, fga19, fgm19, fga50, fgm50, fg_long, xpa, xpm
				//       0    1    2  3   4    5    6      7      8      9      10     11     12     13     14     15     16     17     18       19   20
				//ignore 14 and 15 because duplicates of 6 and 7
				
				seasonCount++;
				//TODO you may need to debug a potential error below which sees the parsing fail because "" is the parameter
				ret[1] += getDouble(yrDataPair.getValue().get(1)); // av
				ret[2] += getDouble(yrDataPair.getValue().get(2)); // G
				//exclude GS because this position not listed as started
				ret[3] += getDouble(yrDataPair.getValue().get(4)); // total fga
				ret[4] += getDouble(yrDataPair.getValue().get(5)); // total fgm
				ret[5] += getDouble(yrDataPair.getValue().get(6)); // fga 0-19
				ret[6] += getDouble(yrDataPair.getValue().get(7)); // fgm 0-19
				ret[7] += getDouble(yrDataPair.getValue().get(8)); // fga 20-29
				ret[8] += getDouble(yrDataPair.getValue().get(9)); // fgm 20-29
				ret[9] += getDouble(yrDataPair.getValue().get(10)); // fga 30-39
				ret[10] += getDouble(yrDataPair.getValue().get(11)); // fgm 30-39 
				ret[11] += getDouble(yrDataPair.getValue().get(12)); // fga 40-49
				ret[12] += getDouble(yrDataPair.getValue().get(13)); // fgm 40-49
				ret[13] += getDouble(yrDataPair.getValue().get(16)); // fga 50+
				ret[14] += getDouble(yrDataPair.getValue().get(17)); // fgm 50+
				ret[15] += getDouble(yrDataPair.getValue().get(18)); // fg_long
				ret[16] += getDouble(yrDataPair.getValue().get(19)); // xpa
				ret[17] += getDouble(yrDataPair.getValue().get(20)); // xpm
				
			}
		}
		
		for(Entry<String, List<String>> yrDataPair : player.yearToDataPlayoffs.entrySet()) {
			if(Integer.parseInt(yrDataPair.getKey()) < seasonYear) {
				
				//2009=[ 25, 1,  0,   1,    ,      ,      ,      ,      ,      ,      ,     1,      ,      ,     ,       ,      ,    null,   2,   2]
//						age, G, GS, fga, fgm, fga19, fgm19, fga29, fgm29, fga39, fgm39, fga49, fgm49, fga19, fgm19, fga50, fgm50, fg_long, xpa, xpm
				
				
				ret[2] += getDouble(yrDataPair.getValue().get(1)); // G
				//exclude GS because this position not listed as started
				ret[3] += getDouble(yrDataPair.getValue().get(3)); // total fga g
				ret[4] += getDouble(yrDataPair.getValue().get(4)); // total fgm g
				ret[5] += getDouble(yrDataPair.getValue().get(5)); // fga 0-19 g
				ret[6] += getDouble(yrDataPair.getValue().get(6)); // fgm 0-19 g 
				ret[7] += getDouble(yrDataPair.getValue().get(7)); // fga 20-29 g
				ret[8] += getDouble(yrDataPair.getValue().get(8)); // fgm 20-29 g
				ret[9] += getDouble(yrDataPair.getValue().get(9)); // fga 30-39 g
				ret[10] += getDouble(yrDataPair.getValue().get(10)); // fgm 30-39 g 
				ret[11] += getDouble(yrDataPair.getValue().get(11)); // fga 40-49 g
				ret[12] += getDouble(yrDataPair.getValue().get(12)); // fgm 40-49 g
				ret[13] += getDouble(yrDataPair.getValue().get(15)); // fga 50+ g
				ret[14] += getDouble(yrDataPair.getValue().get(16)); // fgm 50+ g
				// DON'T ADD PLAYOFF FG LONG BECAUSE IT WILL WEIRDLY SKEW DATA
				ret[16] += getDouble(yrDataPair.getValue().get(18)); // xpa g
				ret[17] += getDouble(yrDataPair.getValue().get(19)); // xpm g
			}
		}
		
		//accumulate gamely
		Map<String, List<String>> gidsToStats = KICKER_PER_GAME.get(pid);
		//go through each entry, if the key in this entry is in between the start of the current season and the parameter game id then accumulate the appropriate categories
		for(Entry<String, List<String>> gidToStatline : gidsToStats.entrySet()) {
			//if(gidToStatline.getKey() > SEASON_END_GAME[seasonYear-2002] && gidToStatline.getKey() < gameID) {201802040nwe
			if(isDateOneAfterDateTwo(gidToStatline.getKey(), SEASON_END_GAME[seasonYear-2002]) && isDateOneAfterDateTwo(gameID, gidToStatline.getKey())) {				
				//GostSt20={201812090mia=[3, 4, 2, 3],...} 
				//xpm xpa fgm fga
				ret[3] += getDouble(gidToStatline.getValue().get(3)); // fga
				ret[4] += getDouble(gidToStatline.getValue().get(2)); // fgm
				ret[16] += getDouble(gidToStatline.getValue().get(1)); // xpa
				ret[17] += getDouble(gidToStatline.getValue().get(0)); // xpm
				
			}
		}
		
		//exclude GS because this position not listed as started
		ret[3] /= ret[2]; // total fga g
		ret[4] /= ret[2]; // total fgm g
		ret[5] /= ret[2]; // fga 0-19 g
		ret[6] /= ret[2]; // fgm 0-19 g 
		ret[7] /= ret[2]; // fga 20-29 g
		ret[8] /= ret[2]; // fgm 20-29 g
		ret[9] /= ret[2]; // fga 30-39 g
		ret[10] /= ret[2]; // fgm 30-39 g 
		ret[11] /= ret[2]; // fga 40-49 g
		ret[12] /= ret[2]; // fgm 40-49 g
		ret[13] /= ret[2]; // fga 50+ g
		ret[14] /= ret[2]; // fgm 50+ g
		ret[15] /= seasonCount; // fg_long s
		ret[16] /= ret[2]; // xpa g
		ret[17] /= ret[2]; // xpm g
		
		return ret;
		
	}
	
	
	private static boolean isDateOneAfterDateTwo(String date1, String date2) {
		// TODO Auto-generated method stub
		//200602050pit yearmndy0htm -> <year, 4 chars><month 01-12, 2 chars><day 01-31, 2 chars>0<home team acronym, 3 chars>
		ToIntFunction<String> day = str -> Integer.parseInt(str.substring(6, 8));
		ToIntFunction<String> month = str -> Integer.parseInt(str.substring(4, 6));
		ToIntFunction<String> year = str -> Integer.parseInt(str.substring(0, 4));
		return isDateOneAfterDateTwo(day.applyAsInt(date1), month.applyAsInt(date1), year.applyAsInt(date1), day.applyAsInt(date2), month.applyAsInt(date2), year.applyAsInt(date2));
		
	}
	
	public static boolean isDateOneAfterDateTwo(int day1, int mth1, int yr1, int day2, int mth2, int yr2) {
        // Compare years
        if (yr1 > yr2) {
            return true;
        } else if (yr1 < yr2) {
            return false;
        }

        // If years are the same, compare months
        if (mth1 > mth2) {
            return true;
        } else if (mth1 < mth2) {
            return false;
        }

        // If years and months are the same, compare days
        return day1 > day2;
    }

	private static Map<String, Map<String, List<String>>> getPerGameData(String fileLocation) {
		HashMap<String, Map<String, List<String>>> pidToGidToStats = new HashMap<>();
		try (BufferedReader br = new BufferedReader(new FileReader(fileLocation))) {

			String line = br.readLine();
			br.close();
			
			String[] keyValPairs = line.split("\\}, ");
			
			for(String keyValPair : keyValPairs) {
				
				String[] keyValPairArr = keyValPair.split("=\\{");
				pidToGidToStats.put(keyValPairArr[0], CoachData.getMapFromString(keyValPairArr[1]));
			}
			
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return pidToGidToStats;
	}
	
	private static List<KickerCareerData> getYearlyKickerData(String fileLocation) {
		List<KickerCareerData> ret = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(new FileReader(fileLocation))) {

			String playerDataString;

			while ((playerDataString = br.readLine()) != null) {
				if (!playerDataString.isBlank())
					ret.add(new KickerCareerData(playerDataString.split(":%%:")));

			}
			br.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return ret;
	}
	
	private static List<YearlyCareerData> getYearlyPlayerData(String fileLocation) {
		List<YearlyCareerData> ret = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(new FileReader(fileLocation))) {

			String playerDataString;

			while ((playerDataString = br.readLine()) != null) {
				if (!playerDataString.isBlank())
					ret.add(new YearlyCareerData(playerDataString.split(":%%:")));

			}
			br.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return ret;
	}
	
	private static List<CoachData> getCoachData(String fileLocation) {
		List<CoachData> ret = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(new FileReader(fileLocation))) {

			String coachData;

			while ((coachData = br.readLine()) != null) {
				if (!coachData.isBlank()) {
					System.out.println("String that isn't blank which is being iterated upon from the text file " + coachData);
					ret.add(new CoachData(coachData.split(":%%:")));
				}
			}
			br.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return ret;
	}
	
	private Double getDouble(String str) {
	    return (str.isBlank() || str.equals("null") || str == null) ? 0.0 : Double.parseDouble(str);
	}

	
	private double[] getMeanCoachStatsUpToYear(String coachID, int seasonYear) {
		CoachData coach = null;
		for(CoachData c : COACH_YEARLY) {
			if(c.getId().equals(coachID)) {
				coach = c;
				break;
			}
		}
		
		double[] ret = new double[11];
		int seasonCount = 0;
		//you could either get totals or get season averages plus seasons coached here
		for(Entry<String, List<String>> e : coach.yearToCoachResults.entrySet()) {
			if(Integer.parseInt(e.getKey()) < seasonYear) {
				seasonCount++;
				//TODO you may need to debug a potential error below which sees the parsing fail because "" is the parameter
				ret[0] += getDouble(e.getValue().get(0)); // games
				ret[1] += getDouble(e.getValue().get(1)); // wins
				ret[2] += getDouble(e.getValue().get(2)); // losses
				ret[3] += getDouble(e.getValue().get(3)); // ties
				ret[4] += getDouble(e.getValue().get(5)); // SRS
				ret[5] += getDouble(e.getValue().get(6)); // OSRS
				ret[6] += getDouble(e.getValue().get(7)); // DSRS
				ret[7] += getDouble(e.getValue().get(8)); // po games
				ret[8] += getDouble(e.getValue().get(9)); // po wins
				ret[9] += getDouble(e.getValue().get(10)); //po losses
				ret[10] += getDouble(e.getValue().get(11)); // division placement
			}
		}
		
		ret[4] /= seasonCount;
		ret[5] /= seasonCount;
		ret[6] /= seasonCount;
		ret[10] /= seasonCount;
		//don't divide the others because I don't want the average wins per season, I want total because magnitude is relevant there
		
		return ret;
		
	}

	//bool isHome, String record, int gameOutcome: if isn't home and gameOutcome was vis wins, reduce the first int by 1 and return the values
	private int[] getRecord(boolean isHome, String recordAfterGame, int whoWon) {
		//0 is away 1 is tie 2 is home
		if(!isHome) {
			switch(whoWon) {
			case 0: return getRecord(recordAfterGame, 'w');
			case 1: return getRecord(recordAfterGame, 't');
			case 2: return getRecord(recordAfterGame, 'l');
			}
		} else {
			switch(whoWon) {
			case 0: return getRecord(recordAfterGame, 'l');
			case 1: return getRecord(recordAfterGame, 't');
			case 2: return getRecord(recordAfterGame, 'w');
			}
		}
		return new int[]{-1,-1,-1};
	}

	private int[] getRecord(String recordAfterGame, char winTieOrLoss) {
		if(recordAfterGame.length() <= 4) recordAfterGame += "-0"; // because 9-2-2.len >= 5, 9-8 or 12-5 are < 5
		String[] record = recordAfterGame.split("-");
		int wins = Integer.parseInt(record[0]);
		int losses = Integer.parseInt(record[1]);
		int ties = Integer.parseInt(record[2]);
		switch(winTieOrLoss) {
		case 'w': return new int[] {wins--, losses, ties};
		case 'l': return new int[] {wins, losses--, ties};
		case 't': return new int[] {wins, losses, ties--};
		}
		return new int[]{-1,-1,-1};
	}

	//astroplay, a_turf, grass, dessograss, matrixturf, astroturf, fieldturf, sportturf
	private int getFieldAsInt(String fieldStatus) {
		switch(fieldStatus) {
		case "grass": return 0;
		case "dessograss": return 1;
		case "astroplay": return 2;
		case "a_turf": return 3;
		case "matrix_turf": return 4;
		case "astroturf": return 5;
		case "fieldturf": return 6;
		case "sportturf": return 7;
		}
		return -1;
	}

	private int getStadiumAsInt(String stadiumStatus) {
		switch(stadiumStatus) {
		case "dome": return 0;
		case "retractable roof (closed)": return 1;
		case "retractable roof (open)": return 2;
		case "outdoors": return 3;
		}
		return -1;
	}

	private float parseTimeFromString(String time) {
		try {
            // Parse the input time string
            java.text.SimpleDateFormat timeFormat12 = new java.text.SimpleDateFormat("hh:mmaa");
            java.util.Date date = timeFormat12.parse(time);

            // Calculate the float time
            float floatTime = date.getHours() + (date.getMinutes() / 60.0f);

            // Adjust for "pm"
            if (time.toLowerCase().contains("pm")) {
                floatTime += 12.0f;
            }

            return floatTime;
        } catch (java.text.ParseException e) {
            // Handle parsing exceptions if any
            e.printStackTrace();
            return -1.0f; // Return -1 to indicate an error
        }
	}
	
    public static int convertToMilitaryTime(String timeString) {
        try {
            // Parse the input time string
            java.text.SimpleDateFormat timeFormat12 = new java.text.SimpleDateFormat("hh:mmaa");
            java.util.Date date = timeFormat12.parse(timeString);

            // Format the parsed time in 24-hour format
            java.text.SimpleDateFormat timeFormat24 = new java.text.SimpleDateFormat("HHmm");
            return Integer.parseInt(timeFormat24.format(date));
        } catch (java.text.ParseException e) {
            // Handle parsing exceptions if any
            e.printStackTrace();
            return -1; // Return -1 to indicate an error
        }
    }

	//these are the only months that game instances occur in
	private int getMonthAsInt(String month) {
		month = month.toLowerCase();
		switch(month) {
		case "sep": return 0;
		case "oct": return 1;
		case "nov": return 2;
		case "dec": return 3;
		case "jan": return 4;
		case "feb": return 5;
		}
		return -1;
	}

	private int getGameOutcome(int visPointsScored, int homePointsScored) {
		if(visPointsScored > homePointsScored) return 0;
		else if(visPointsScored == homePointsScored) return 1;
		else if(visPointsScored < homePointsScored) return 2;
		return -1;
	}
	
	private int getWeekdayAsInt(String weekday) {
		weekday = weekday.toLowerCase();
		switch(weekday) {
		case "sunday": return 0;
		case "monday": return 1;
		case "tuesday": return 2;
		case "wednesday": return 3;
		case "thursday": return 4;
		case "friday": return 5;
		case "saturday": return 6;
		}
		return -1;
	}

}
