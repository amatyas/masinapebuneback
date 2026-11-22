package com.caradchecker.springbatch.util;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CiscoDateParser {
	
	private static final Logger log = LoggerFactory.getLogger(CiscoDateParser.class);

    // Format 1 (JS / Navigateur) : "Mon Jun 22 2026 10:44:13 GMT+0200"
    private static final DateTimeFormatter JS_GMT_FORMATTER = DateTimeFormatter.ofPattern("EEE MMM d yyyy HH:mm:ss 'GMT'Z", Locale.ENGLISH);

    // Format 2 (RFC-1123 / HTTP) : "Mon, 22 Jun 2026 09:51:32 GMT"
    // Le 'd' unique permet de parser "2 Jun" ou "22 Jun" de manière transparente
    private static final DateTimeFormatter RFC_1123_FORMATTER = DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm:ss z", Locale.ENGLISH);

    private static final DateTimeFormatter TARGET_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static String parseCiscoTimestamp(String uniqueTrackerId, String serialNumber, String rawTimestamp) {
    	        if (rawTimestamp == null || rawTimestamp.isBlank()) {
            return "";
        }

        String trimmed = rawTimestamp.trim();
        try {
            LocalDateTime ldt;

            // CAS A : Présence d'une virgule -> C'est le format RFC-1123 (ton cas d'erreur)
            if (trimmed.contains(",")) {
                ldt = ZonedDateTime.parse(trimmed, RFC_1123_FORMATTER).toLocalDateTime();
            } 
            // CAS B : Contient GMT mais pas de virgule -> Format JavaScript standard
            else if (trimmed.contains("GMT")) {
                ldt = ZonedDateTime.parse(trimmed, JS_GMT_FORMATTER).toLocalDateTime();
            } 
            // CAS C : Contient le séparateur "T" -> Format standard ISO-8601
            else if (trimmed.contains("T")) {
                if (trimmed.contains("Z") || trimmed.contains("+")) {
                    ldt = ZonedDateTime.parse(trimmed).toLocalDateTime();
                } else {
                    String isoString = trimmed.length() > 19 ? trimmed.substring(0, 19) : trimmed;
                    ldt = LocalDateTime.parse(isoString);
                }
            } 
            // Format non pris en charge
            else {
            	log.error("["+uniqueTrackerId+"]["+serialNumber+"] Unable to parse Cisco Date: "+rawTimestamp);
                return "";
            }

            // Retourne le format attendu par la BDD (ex: 2026-06-22 09:51)
            return ldt.format(TARGET_FORMATTER);

        } catch (Exception e) {
        	log.error("["+uniqueTrackerId+"]["+serialNumber+"] Unable to parse Cisco Date: "+rawTimestamp, e);
            // Loggez l'erreur ici (e.getMessage())
            return "";
        }
    }
}