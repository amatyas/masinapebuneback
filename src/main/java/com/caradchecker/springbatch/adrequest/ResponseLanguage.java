package com.caradchecker.springbatch.adrequest;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public enum ResponseLanguage {
	RO("ro", "Romanian"),
	FR("fr", "French"),
	EN("en", "English"),
	BG("bg", "Bulgarian");

	private final String code;
	private final String englishName;

	ResponseLanguage(String code, String englishName) {
		this.code = code;
		this.englishName = englishName;
	}

	public String code() {
		return code;
	}

	public String englishName() {
		return englishName;
	}

	public static ResponseLanguage parse(String raw) {
		if (raw == null || raw.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La langue est obligatoire : ro, fr, en ou bg");
		}
		String code = raw.trim().toLowerCase();
		for (ResponseLanguage language : values()) {
			if (language.code.equals(code)) {
				return language;
			}
		}
		throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Langue inconnue. Valeurs acceptées : ro, fr, en, bg");
	}

	public String instruction() {
		return """
				# RESPONSE LANGUAGE OVERRIDE
				The API caller selected language code "%s" (%s).
				This overrides every instruction above that asks for Romanian user-facing text.
				Write every user-facing string in %s.
				Keep JSON property names unchanged, including names that end in _ro.
				Keep enum values in English.
				Keep exact_quote as a verbatim excerpt in the source language.
				""".formatted(code, englishName, englishName);
	}
}
