package com.example.ddmdemo.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TemplateParser {
	
	public static List<String> parseTemplate(String documentContent) {
	    List<String> fields = new ArrayList<>();
	
	    // Extract words after "Uprava za"
	    addField(fields, extractUprava(documentContent, "Uprava za\\s+([\\w\\s]+?)\\s*(?:\\n|$)"));
	
	    // Extract words after "Nivo uprave:"
	    addField(fields, extractField(documentContent, "Nivo uprave:\\s+([\\w\\s]+?)\\s*(?:\\n|$)"));
	
	    // Extract address
	    addField(fields, extractAddress(documentContent));
	
	    // Extract words before underscore
	    addWordsBeforeUnderscore(fields, documentContent);
	
	    // Print all words
	    fields.forEach(System.out::println);
	
	    return fields;
	}
	
	private static void addField(List<String> fields, String fieldValue) {
	    if (fieldValue != null && !fieldValue.isEmpty()) {
	        fields.add(fieldValue.trim());
	    }
	}
	
	private static String extractField(String documentContent, String regex) {
	    Pattern pattern = Pattern.compile(regex);
	    Matcher matcher = pattern.matcher(documentContent);
	    if (matcher.find()) {
	        if (matcher.groupCount() > 0) {
	            return matcher.group(1);
	        } else {
	            return matcher.group();
	        }
	    }
	    return null;
	}
	
	private static String extractAddress(String documentContent) {
	    Pattern emailPattern = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");
	    Matcher emailMatcher = emailPattern.matcher(documentContent);
	    if (emailMatcher.find()) {
	        int emailIndex = emailMatcher.start();
	        int nextLineBreak = documentContent.indexOf('\n', emailIndex);
	        if (nextLineBreak != -1) {
	        	extractCityFromAddress(documentContent.substring(emailIndex + emailMatcher.group().length(), nextLineBreak).trim());
	            return documentContent.substring(emailIndex + emailMatcher.group().length(), nextLineBreak).trim();
	        }
	    }
	    return null;
	}
	
	private static void addWordsBeforeUnderscore(List<String> fields, String documentContent) {
	    List<String> words = extractWordsBeforeUnderscore(documentContent);
	    if (words != null) {
	        fields.addAll(words);
	    }
	}
	
	private static List<String> extractWordsBeforeUnderscore(String documentContent) {
	    List<String> words = new ArrayList<>();
	    Pattern pattern = Pattern.compile("(\\b[A-Z][a-z]+\\b)\\s+(\\b[A-Z][a-z]+\\b)\\s+(?=_)");
	    Matcher matcher = pattern.matcher(documentContent);
	    if (matcher.find()) {
	        words.add(matcher.group(1));
	        words.add(matcher.group(2));
	    }
	    if (!words.isEmpty()) {
	        String fullName = String.join(" ", words);
	        log.info("STATISTIC-LOG zaposleni -> " + fullName);
	    }
	    return words;
	}
	
	private static void extractCityFromAddress(String address) {
        String[] parts = address.split(",");
        if (parts.length > 0) {
            String city = parts[parts.length - 1].trim();
            log.info("STATISTIC-LOG grad -> " + city);
        }
    }

	private static String extractUprava(String documentContent, String regex) {
	    Pattern pattern = Pattern.compile(regex);
	    Matcher matcher = pattern.matcher(documentContent);
	    if (matcher.find()) {
	        if (matcher.groupCount() > 0) {
	        	String uprava = matcher.group(1);
	        	log.info("STATISTIC-LOG uprava -> " + uprava);
	            return matcher.group(1);
	        } else {
	            return matcher.group();
	        }
	    }
	    return null;
	}
}
