package com.reflexit.magiccards.core.sync;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.reflexit.magiccards.core.MagicLogger;
import com.reflexit.magiccards.core.model.Edition;
import com.reflexit.magiccards.core.model.Editions;
import com.reflexit.magiccards.core.model.MagicCard;
import com.reflexit.magiccards.core.model.MagicCardField;
import com.reflexit.magiccards.core.sync.ParserHtmlHelper.ILoadCardHander;
import com.reflexit.magiccards.core.sync.ParserHtmlHelper.OutputHandler;

public class ParseScryFallChecklist extends AbstractParseJson {
	public static final String BASE_SEARCH_URL = "https://api.scryfall.com/cards/search?";

	public String processFromReader(BufferedReader st, ILoadCardHander handler) throws IOException {
		try {
			JSONObject top = (JSONObject) new JSONParser().parse(st);
			int c = getInt(top, "total_cards");
			handler.setCardCount(c);
			Boolean has_more = (Boolean) top.get("has_more");
			JSONArray data = (JSONArray) top.get("data");
			for (Object elem : data) {
				parseRecord((JSONObject) elem, handler);
			}
			if (has_more) {
				return getString(top, "next_page");
			}
		} catch (ParseException e) {
			MagicLogger.log(e);
			throw new RuntimeException("No results");
		}
		return null;
	}

	private void parseRecord(JSONObject elem, ILoadCardHander handler) {
		if (elem == null)
			return;
		MagicCard card = new MagicCard();
		card.setCollNumber(getString(elem, "collector_number"));
		String name = (String) elem.get("name");
		card.setName(name);
		String type = getString(elem, "type_line");
		card.setType(type);
		card.setPower(getString(elem, "power"));
		card.setToughness(getString(elem, "toughness"));
		String cost = getString(elem, "mana_cost");
		card.setCost(normalizeCost(cost));
		card.setRarity(getString(elem, "rarity"));
		card.setArtist(getString(elem, "artist"));
		String abbr = getString(elem, "set");
		String set = getString(elem, "set_name");
		Edition ed = new Edition(set, abbr.toUpperCase(Locale.ENGLISH));
		handler.handleEdition(ed);
		card.setSet(set);
		String regset = Editions.getInstance().getNameByAbbr(ed.getMainAbbreviation());
		if (regset != null) {
			card.setSet(regset);
		}
		JSONObject images = (JSONObject) elem.get("image_uris");
		String image = (String) images.get("png");
		card.set(MagicCardField.IMAGE_URL, image);
		card.setLanguage(getString(elem, "lang"));
		JSONArray gids = (JSONArray) elem.get("multiverse_ids");
		if (gids.size() > 0) {
			Object oneid = gids.get(0);
			card.setCardId(((Long) oneid).intValue());
		} else {
			card.setCardId(card.syntesizeId());
		}
		// print
		handler.handleCard(card);
	}

	private String normalizeCost(String cost) {
		int l = cost.length();
		if (cost.trim().length() == 0)
			return "";
		boolean inMana = false;
		StringBuilder res = new StringBuilder(l * 3);
		for (int i = 0; i < l; i++) {
			char c = cost.charAt(i);
			if (c == '{') {
				inMana = true;
				res.append(c);
				continue;
			}
			if (inMana) {
				res.append(c);
				if (c == '}')
					inMana = false;
				continue;
			}
			res.append('{');
			res.append(c);
			res.append('}');
		}
		return res.toString();
	}

	public URL getSearchQuery(String set) throws MalformedURLException {
		String url;
		if (set != null && set.startsWith("http")) {
			url = set;
		} else {
			String out = "&v=list&s=issue";
			String abbr = Editions.getInstance().getAbbrByName(set);
			if (abbr == null)
				abbr = set;
			String base = BASE_SEARCH_URL + "q=e%3A" + abbr + "+lang%3Aen" + out;
			url = base;
		}
		return new URL(url);
	}

	public static void main(String[] args) throws MalformedURLException, IOException {
		OutputHandler handler = new OutputHandler(System.out, true, true);
		// Editions.getInstance().addEdition("Duels of the Planeswalkers",
		// "dpa");
		// https://magiccards.info/query?q=Spite&v=list&s=cname
		// URL searchQuery = getSearchQuery("dpa");
		// new ParseMagicCardsInfoChecklist().loadSingleUrl(searchQuery,
		// handler);
		String german = "q=e%3Adtk+lang%3Ade&v=list&s=cname";
		String doubles = "q=Spite&v=list&s=cname";
		String wc = "q=e%3Awmcq+lang%3Aen&v=list&s=cname";
		String surl = BASE_SEARCH_URL + german;
		new ParseScryFallChecklist().loadSingleUrl(new URL(surl), handler);
		System.err.println("Total " + handler.getCardCount());
	}
}
