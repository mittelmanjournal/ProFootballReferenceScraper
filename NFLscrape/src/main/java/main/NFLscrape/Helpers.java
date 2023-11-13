package main.NFLscrape;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Helpers {

	public static double getPasserRating(double completions, double attempts, double yards, double touchdowns, double interceptions) {
		return (((completions/attempts - 0.3) * 5 + (yards/attempts - 3) * 0.25 + (touchdowns/attempts) * 20 + 2.375 - (interceptions/attempts * 25)) / 6) * 100;
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
		// Regular expression to match any character that is not a digit (0-9) or a dot
		// (.)
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
			// Add length of parameterString to get the substring to the right of
			// parameterString
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
		if (substring == null) {
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

	public static String extractBetween(String original, String firstPart, String lastPart) {
		int startIndex = original.indexOf(firstPart);
		int endIndex = original.lastIndexOf(lastPart);

		if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
			// Add the length of firstPart to startIndex to avoid including it
			startIndex += firstPart.length();

			// Extract the substring between startIndex and endIndex
			return original.substring(startIndex, endIndex);
		} else {
			// Return an empty string if firstPart or lastPart not found or in the wrong
			// order
			return "";
		}
	}

	public static String[] splitString(String original, String input) {
		String[] result = new String[2];

		int index = original.indexOf(input);

		if (index != -1) {
			// Split the string based on the input string
			result[0] = original.substring(0, index);
			result[1] = original.substring(index + input.length());
		} else {
			// If the input string is not found, return the original string as the first
			// part
			result[0] = original;
			result[1] = "";
		}

		return result;
	}
	//Thursday Sep 5, 2002
	public static String dateToCSV(String wkdyMDY) {
		String ret = "";
		wkdyMDY.replaceAll(",", "");
		String[] arr = wkdyMDY.split(" ");
		switch (arr[0]) {
		case "Sunday":
			ret += "1, ";
			break;
		case "Monday":
			ret += "2, ";
			break;
		case "Tuesday":
			ret += "3, ";
			break;
		case "Wednesday":
			ret += "4, ";
			break;
		case "Thursday":
			ret += "5, ";
			break;
		case "Friday":
			ret += "6, ";
			break;
		case "Saturday":
			ret += "7, ";
			break;
		default:
			ret += "-1, ";
			break;
		}
		
		switch (arr[1]) {
		case "Sep":
			ret += "1, ";
			break;
		case "Oct":
			ret += "2, ";
			break;
		case "Nov":
			ret += "3, ";
			break;
		case "Dec":
			ret += "4, ";
			break;
		case "Jan":
			ret += "5, ";
			break;
		case "Feb":
			ret += "6, ";
			break;
		case "Mar":
			ret += "7, ";
			break;
		case "Apr":
			ret += "8, ";
			break;
		case "May":
			ret += "9, ";
			break;
		case "Jun":
			ret += "10, ";
			break;
		case "Jul":
			ret += "11, ";
			break;
		case "Aug":
			ret += "12, ";
			break;
		default:
			ret += "-1, ";
			break;
		}
		
		ret += arr[2]; // add day of month
		ret += " " + arr[3]; // add year
		
		return ret;
	}
	
	//8:38pm
	public static String timeToCSV(String time) {
		time.trim();
		String[] arr = time.split(":");
		float hours;
		float minutes;
		float timeAsNum;
		if(time.contains("pm") && !arr[0].equals("12")) {
			hours = 12f + Integer.parseInt(arr[0]);
			minutes = Integer.parseInt(removeNonNumericCharacters(arr[1]));
		} else {
			hours = Integer.parseInt(arr[0]);
			minutes = Integer.parseInt(removeNonNumericCharacters(arr[1]));
		}
		
		timeAsNum = hours + minutes/60f;
		
		return ""+timeAsNum;
	}

}
