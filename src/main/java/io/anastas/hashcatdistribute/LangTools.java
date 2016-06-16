package io.anastas.hashcatdistribute;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.*;

public class LangTools {
	public static String csSubstring(String s, int i, int len) {
		return s.substring(i, i+len);
	}

	public static Map<String, String> parsePOSTArguments(String raw) {
		try {
			String[] pairValues = raw.split("&");

			Map<String, String> args = new HashMap<String, String>();

			// Add pairs to hashmap
			for (String pair : pairValues) {
				String[] temp = pair.split("=");
				args.put(temp[0], terminateNullString(temp[1]));
			}

			if (args.keySet().size() == 0) return null;
			return args;
		} catch (Exception e) {
			return null;
		}
	}
	
	public static String printAsCharArray(String str) {
		for (int i=0; i<str.length(); i++) {
			System.out.print((int)str.charAt(i)+" ");
		}
		return str;
	}
	
	public static String terminateNullString(String str) {
		String terminatedString = "";
		for (int i=0; i<str.length(); i++) {
			if (str.charAt(i) != (char)0) {
				terminatedString += str.charAt(i);
			} else break;
		}
		return terminatedString;
	}

	public static <T> void printArray(T[] arr) {
		for (T e: arr) {
			System.err.print(e + " ");
		}
	}
	
	public static void writeFile(String toWrite, String path, Charset cs) {
		try {
			PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(path)), cs), true);
			Scanner br = new Scanner(toWrite);
			while (br.hasNextLine()) out.println(br.nextLine());
			
			out.flush();
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
}
