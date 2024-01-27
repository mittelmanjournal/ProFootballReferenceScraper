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

public class NNI {

	public static final List<CoachData> COACH_YEARLY = getCoachData("FINAL_COACH_YEARLY_DATA.txt");
	
	public static final List<YearlyCareerData> PLAYER_YEARLY = getYearlyPlayerData("FINAL_CAREER_YEARLY_DATA.txt");
	public static final Map<String, Map<String,List<String>>> PLAYER_PER_GAME = getPerGameData("FINAL_PER-GAME_PLAYER_DATA_MAP.txt");
	
	public static final List<KickerCareerData> KICKER_YEARLY = getYearlyKickerData("FINAL_KICKER_YEARLY_DATA.txt");
	public static final Map<String, Map<String,List<String>>> KICKER_PER_GAME = getPerGameData("FINAL_PER-GAME_KICKER_DATA_MAP.txt");
	//2001 superbowl date to 2022 superbowl date
	public static final String[] SEASON_END_GAME = {"200202030nwe", "200301260rai", "200402010car", "200502060nwe", "200602050pit", "200702040chi", "200802030nwe", "200902010crd", "201002070clt", "201102060pit", "201202050nwe", "201302030sfo", "201402020den", "201502010sea", "201602070den", "201702050atl", "201802040nwe", "201902030ram", "202002020kan", "202102070tam", "202202130cin", "202302120phi"};
	
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

	public NNI(Game game, String delim) {
		this(game.toString(delim), delim);
	}

	public NNI(String game, String delim) {
		this(game.split(delim));
	}

	public NNI(String[] game) {
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
		return gameID + "," + fullWeather.replaceAll(",", "\'") + "," + spread + "," + overUnder + "," + dateInfo.replaceAll(",", "\'") + "," + timeStr + "," + time + "," + stadiumStatus + "," + fieldStatus + "," + visRecordAfterGame + "," + homeRecordAfterGame;
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
		for(Entry<String, List<String>> e : coach.yearToCoachResults.entrySet()) {
			if(Integer.parseInt(e.getKey()) < seasonYear) {
				seasonCount++;
				accumulateArrayInLoop(ret, e.getValue(), new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, new int[]{0, 1, 2, 3, 5, 6, 7, 8, 9, 10, 11}, 12);
			}
		}
	
		divideElements(ret, new int[] {4,5,6,10}, seasonCount);
		
		return ret;
		
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
		setHeightWeightAge(ret, new int[] {1,2,3}, player, seasonYear);
		
		int seasonCount = 0;
		
		for(Entry<String, List<String>> yrDataPair : player.yearToData.entrySet()) {
			if(Integer.parseInt(yrDataPair.getKey()) < seasonYear) {
				seasonCount++;
				
				String[] recordArr = yrDataPair.getValue().get(4).split("-");
				if (recordArr.length == 1) {
					ret[6] += 0; // wins
					ret[7] += 0; // losses
					ret[8] += 0; // ties

				} else {
					ret[6] += getDouble(recordArr[0]); // wins
					ret[7] += getDouble(recordArr[1]); // losses
					ret[8] += getDouble(recordArr[2]); // ties
				}
				
				accumulateArrayInLoop(ret, yrDataPair.getValue(), new int[] {4,5,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26}, new int[] {1,2,5,6,7,8,9,10,11,12,15,16,18,19,20,21,22,23,24,25}, 26);
				
			}
		}
		
		for(Entry<String, List<String>> yrDataPair : player.yearToDataPlayoffs.entrySet()) {
			if(Integer.parseInt(yrDataPair.getKey()) < seasonYear) {
				String[] recordArr = yrDataPair.getValue().get(3).split("-"); //ignore .get(2) because thats gs
				ret[6] += getDouble(recordArr[0]); //wins
				ret[7] += getDouble(recordArr[1]); //losses
				
				accumulateArrayInLoop(ret, yrDataPair.getValue(), new int[] {5,9,10,11,12,13,14,17,18,19,20,21,22,23,26}, new int[] {1,4,5,6,7,8,9,13,14,16,17,18,19,20,23}, 24);
			}
		}
		

		Map<String, List<String>> gidsToStats = PLAYER_PER_GAME.get(pid);
		
		for(Entry<String, List<String>> gidToStatline : gidsToStats.entrySet()) {
			if(isDateOneAfterDateTwo(gidToStatline.getKey(), SEASON_END_GAME[seasonYear-2002]) && isDateOneAfterDateTwo(gameID, gidToStatline.getKey())) {
				accumulateArrayInLoop(ret, gidToStatline.getValue(), new int[] {9,10,11,12,13,17,18,20,21,22,26}, new int[] {0,1,2,3,4,5,6,9,10,11,18}, 19);
			}
		}
		
		divideElements(ret, new int[] {9,10,11,12,13,14,17,18,19,20,21,22,23,26}, ret[5]);
		divideElements(ret, new int[] {24,25,15,16}, seasonCount);
		
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
		setHeightWeightAge(ret, new int[] {0,1,2}, player, seasonYear);
		
		int seasonCount = 0;

		for(Entry<String, List<String>> yrDataPair : player.yearToData.entrySet()) {
			if(Integer.parseInt(yrDataPair.getKey()) < seasonYear) {
				seasonCount++;
				
				accumulateArrayInLoop(ret, yrDataPair.getValue(), new int[] {3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19}, new int[] {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17}, 18);
			}
		}
		
		for(Entry<String, List<String>> yrDataPair : player.yearToDataPlayoffs.entrySet()) {
			if(Integer.parseInt(yrDataPair.getKey()) < seasonYear) {
				accumulateArrayInLoop(ret, yrDataPair.getValue(), new int[] {4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19}, new int[] {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16}, 17);
			}
		}
		
		Map<String, List<String>> gidsToStats = PLAYER_PER_GAME.get(pid);
		
		for(Entry<String, List<String>> gidToStatline : gidsToStats.entrySet()) {
			
			if(isDateOneAfterDateTwo(gidToStatline.getKey(), SEASON_END_GAME[seasonYear-2002]) && isDateOneAfterDateTwo(gameID, gidToStatline.getKey())) {
				accumulateArrayInLoop(ret, gidToStatline.getValue(), new int[] {6,7,8,9,13,14,15,19}, new int[] {13,14,15,16,9,10,11,18}, 19);
			}
		}
		
		divideElements(ret, new int[] {6,7,8,9,10,13,14,15,16,19}, ret[4]);
		divideElements(ret, new int[] {17,18,11,12}, seasonCount);
		
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
		
		setHeightWeightAge(ret, new int[] {0,1,2}, player, seasonYear);		
		
		int seasonCount = 0;
		for(Entry<String, List<String>> yrDataPair : player.yearToData.entrySet()) {
			if(Integer.parseInt(yrDataPair.getKey()) < seasonYear) {
				seasonCount++;
				accumulateArrayInLoop(ret, yrDataPair.getValue(), new int[] {3,4,5,6,7,8,9,10,11,12,13,14,15,16}, new int[] {1,2,3,4,6,7,8,10,11,12,14,15,16,17}, 18);
			}
		}
		
		for(Entry<String, List<String>> yrDataPair : player.yearToDataPlayoffs.entrySet()) {
			if(Integer.parseInt(yrDataPair.getKey()) < seasonYear) {
				accumulateArrayInLoop(ret, yrDataPair.getValue(), new int[] {4,5,6,7,8,9,10,11,12,13,14,15,16}, new int[] {1,2,3,5,6,7,9,10,11,13,14,15,16}, 17);
			}
		}
		
		Map<String, List<String>> gidsToStats = PLAYER_PER_GAME.get(pid);

		for (Entry<String, List<String>> gidToStatline : gidsToStats.entrySet()) {

			if (isDateOneAfterDateTwo(gidToStatline.getKey(), SEASON_END_GAME[seasonYear - 2002])
					&& isDateOneAfterDateTwo(gameID, gidToStatline.getKey())) {
				accumulateArrayInLoop(ret, gidToStatline.getValue(), new int[] {6,7,8,9,10,11,12,13,14,15}, new int[] {0,2,4,14,13,5,10,7,8,9}, 10);
			}
		}
		
		divideElements(ret, new int[] {6,7,8,9,10,11,12,13,14,15,16}, ret[4]);
		
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
		ret[0] = Integer.parseInt(player.yearToData.get(""+seasonYear).get(0)); // age
		
		int seasonCount = 0;
		
		for(Entry<String, List<String>> yrDataPair : player.yearToData.entrySet()) {
			if(Integer.parseInt(yrDataPair.getKey()) < seasonYear) {
				seasonCount++;
				accumulateArrayInLoop(ret, yrDataPair.getValue(), new int[] {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17}, new int[] {1,2,4,5,6,7,8,9,10,11,12,13,16,17,18,19,20}, 21);
			}
		}
		
		for(Entry<String, List<String>> yrDataPair : player.yearToDataPlayoffs.entrySet()) {
			if(Integer.parseInt(yrDataPair.getKey()) < seasonYear) {	
				accumulateArrayInLoop(ret, yrDataPair.getValue(), new int[] {2,3,4,5,6,7,8,9,10,11,12,13,14,16,17}, new int[] {1,3,4,5,6,7,8,9,10,11,12,15,16,18,19}, 20);
			}
		}
		
		Map<String, List<String>> gidsToStats = KICKER_PER_GAME.get(pid);
		
		for(Entry<String, List<String>> gidToStatline : gidsToStats.entrySet()) {
			if(isDateOneAfterDateTwo(gidToStatline.getKey(), SEASON_END_GAME[seasonYear-2002]) && isDateOneAfterDateTwo(gameID, gidToStatline.getKey())) {				
				accumulateArrayInLoop(ret, gidToStatline.getValue(), new int[] {3,4,16,17}, new int[] {3,2,1,0}, 4);
			}
		}
		
		divideElements(ret, new int[] {3,4,5,6,7,8,9,10,11,12,13,14,16,17}, ret[2]);
		ret[15] /= seasonCount;
		
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
	
	private static Double getDouble(String str) {
	    return (str.isBlank() || str.equals("null") || str == null) ? 0.0 : Double.parseDouble(str);
	}
	
	public static void accumulateArrayInLoop(double[] setValsInHere, List<String> getValsFromHere, int[] arrayIdxs, int[] dataIdxs, int standardizedSize) throws IndexOutOfBoundsException {
		List<String> standardizedList = CoachData.standardize(getValsFromHere, standardizedSize); 
		
		if (arrayIdxs.length != dataIdxs.length) // also do checks for if the array to set's length is less than arrayIdxs length
			throw new IndexOutOfBoundsException("The list of indexes for the array to be set is not the same length as the list of indexes from the List of String that you will get data from");
		else {
			for (int i = 0; i < arrayIdxs.length; i++) {
				setValsInHere[arrayIdxs[i]] += getDouble(standardizedList.get(dataIdxs[i])); 
				// this works assuming the
				// length of each string
				// list for each different
				// call of this function is
				// of a standardized length
				// (do this by adding 0
				// values to end of lists if
				// of not proper size,
				// either do this in
				// constructors of their own
				// data objects or in this
				// method at the start)
			}
		}
	}
	
	public static void divideElements(double[] arr, int[] arrIdxs, double divisor) {
		for(int i = 0; i < arrIdxs.length; i++) {
			arr[arrIdxs[i]] /= divisor;
		}
	}
	
	public static void setHeightWeightAge(double[] arrToSet, int[] idxsToSet, YearlyCareerData player, int year){
		arrToSet[idxsToSet[0]] = Integer.parseInt(player.getHeightCm());
		arrToSet[idxsToSet[1]] = Integer.parseInt(player.getWeightKg());
		arrToSet[idxsToSet[2]] = Integer.parseInt(player.yearToData.get(""+year).get(0));
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
