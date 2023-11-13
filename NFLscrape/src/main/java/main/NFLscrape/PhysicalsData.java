package main.NFLscrape;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class PhysicalsData {
	
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
			return 6; //5 is reserved for returners, but no position is returners
		else if(position.equals("P"))
			return 7;
		
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

	
    public static void main(String[] args) throws IOException {
    	String url = "https://www.pro-football-reference.com/players/M/McCaCh01.htm";
		Document document = Jsoup.connect(url).get();
		
		for(double d : getPlayerPhysical(document)) {
			System.out.println(d);
		}
		
//		System.out.println(positionAndHandedness);
//    	System.out.println(extractStringBetween(positionAndHandedness, "Position: ", " Throws: "));
//    	System.out.println(getStringToRight(positionAndHandedness, " Throws: "));
    	
//        Scanner scanner = new Scanner(System.in);
//
//        System.out.print("Enter the file name (with extension): ");
//        String fileName = scanner.nextLine();
//
//        try {
//            // Create a FileWriter with the specified file name
//            FileWriter fileWriter = new FileWriter(fileName);
//
//            System.out.println("Enter text (type 'exit' to finish):");
//
//            // Read lines from the console and write them to the file
//            while (true) {
//                String line = scanner.nextLine();
//                if (line.equalsIgnoreCase("exit")) {
//                    break;
//                }
//                fileWriter.write(line + "\n");
//            }
//
//            // Close the FileWriter to save changes
//            fileWriter.close();
//
//            System.out.println("Text has been written to " + fileName);
//        } catch (IOException e) {
//            e.printStackTrace();
//            System.err.println("An error occurred while writing to the file.");
//        } finally {
//            scanner.close();
//        }
    }
}
