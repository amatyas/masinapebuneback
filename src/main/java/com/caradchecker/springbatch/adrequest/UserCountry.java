package com.caradchecker.springbatch.adrequest;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public enum UserCountry {
	ROMANIA("Romania", "RON", """
			Prefer EU Safety Gate alerts for Romania, then other EU Safety Gate vehicle alerts.
			Use NHTSA recalls and complaints only as a foreign complement and label them as United States.
			"""),
	BULGARIA("Bulgaria", "BGN", """
			Prefer EU Safety Gate alerts for Bulgaria, then other EU Safety Gate vehicle alerts.
			Use NHTSA only as a foreign complement and label them as United States.
			"""),
	FRANCE("France", "EUR", """
			Prefer RappelConso automobile fiches, then EU Safety Gate alerts for France.
			Use NHTSA only as a foreign complement and label them as United States.
			"""),
	UK("UK", "GBP", """
			Prefer the official UK vehicle recall checker (DVSA / gov.uk), then historical EU Safety Gate alerts.
			Use NHTSA only as a foreign complement and label them as United States.
			""");

	private final String name;
	private final String currency;
	private final String sourcePriority;

	UserCountry(String name, String currency, String sourcePriority) {
		this.name = name;
		this.currency = currency;
		this.sourcePriority = sourcePriority;
	}

	public String countryName() {
		return name;
	}

	public String instruction() {
		return """
				# USER COUNTRY
				The buyer is in %s.
				Search technical defects, recall campaigns and repair costs for this country first.
				%s
				Guiding sources, which you must open yourself when a browsing tool is available:
				- EU Safety Gate: https://ec.europa.eu/safety-gate-alerts/
				- RappelConso: https://rappel.conso.gouv.fr/
				- NHTSA recalls: https://api.nhtsa.gov/recalls/recallsByVehicle
				- NHTSA complaints: https://api.nhtsa.gov/complaints/complaintsByVehicle
				- UK recall checker: https://www.check-vehicle-recalls.service.gov.uk/
				Produce technical findings, recalls and repair costs in your output from records you actually open.
				Every URL must be a page you opened. If you cannot open a source, leave that list empty or the amount null.
				Do not invent campaign numbers, bulletins or prices.
				Prefer repair costs in %s. A price from another country must name that country.
				""".formatted(name, sourcePriority, currency);
	}

	public String marketInstruction() {
		String sources = switch (this) {
			case ROMANIA -> """
					Search these Romanian marketplaces in this order, and open the listing pages yourself:
					1. AutoUncle Romania: https://www.autouncle.ro/
					2. AutoScanner: https://www.autoscanner.ro/
					3. AutoCauta: https://www.autocauta.ro/
					""";
			case BULGARIA -> """
					Search these Bulgarian marketplaces in this order, and open the listing pages yourself:
					1. AutoSearch: https://autosearch.bg/
					2. AUTOLA Bulgaria: https://autola.io/en/bulgaria/
					""";
			case FRANCE -> """
					Search these French marketplaces in this order, and open the listing pages yourself:
					1. Karoto Data: https://karoto.fr/data
					2. AutoUncle France: https://www.autouncle.fr/
					""";
			case UK -> """
					No guided UK marketplace was configured.
					Do not invent comparables or third-party valuations. Leave those lists empty and the price figures null.
					""";
		};
		return """
				# MARKET SEARCH
				The buyer is in %s.
				The application does not send comparables, pricing_calculations or third_party_valuations.
				Search the marketplaces one at a time, in the order listed. Do not open the next site while the current one already has ten comparable advertisements.
				Open the first site and collect up to ten comparable listings of the same model.
				If that site has ten, use only that site. If it has fewer than ten, continue with the next site and add only the missing listings.
				Stop as soon as you reach ten. Do not mix sites when one site is enough.
				A comparable URL may appear only if you opened that listing. Do not invent a listing, a price or an AutoUncle score.
				From the comparables you opened, compute the reference price, the low and high range, and the difference versus the asking price.
				If you opened fewer than three usable comparables, leave the numeric valuation null.
				Prices should be kept in %s when the source shows that currency. Do not silently convert.
				%s
				""".formatted(name, marketCurrency(), sources);
	}

	public String marketCurrency() {
		return switch (this) {
			case UK -> "GBP";
			case ROMANIA, BULGARIA, FRANCE -> "EUR";
		};
	}

	public static UserCountry parse(String raw) {
		if (raw == null || raw.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le pays est obligatoire : Romania, Bulgaria, France ou UK");
		}
		String value = raw.trim().toLowerCase();
		return switch (value) {
			case "romania", "ro" -> ROMANIA;
			case "bulgaria", "bg" -> BULGARIA;
			case "france", "fr" -> FRANCE;
			case "uk", "united kingdom", "gb" -> UK;
			default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Pays inconnu. Valeurs acceptées : Romania, Bulgaria, France, UK");
		};
	}
}
