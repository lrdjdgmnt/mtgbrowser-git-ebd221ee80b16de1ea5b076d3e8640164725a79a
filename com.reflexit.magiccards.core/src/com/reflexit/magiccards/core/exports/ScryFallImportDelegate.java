package com.reflexit.magiccards.core.exports;

import java.io.IOException;
import java.net.URL;

import com.reflexit.magiccards.core.model.MagicCard;
import com.reflexit.magiccards.core.model.MagicCardField;
import com.reflexit.magiccards.core.model.abs.ICardField;
import com.reflexit.magiccards.core.monitor.ICoreProgressMonitor;
import com.reflexit.magiccards.core.sync.ParseScryFallChecklist;
import com.reflexit.magiccards.core.sync.ParserHtmlHelper;

public class ScryFallImportDelegate extends AbstractImportDelegate {
	private ParseScryFallChecklist parser;

	@Override
	public void setReportType(ReportType reportType) {
		super.setReportType(reportType);
		// https://scryfall.com/search?q=e:pwcq+display:checklist+order:name
		reportType.setProperty("url_regex", "https://.*scryfall.com/.*");
	}

	@Override
	public void init(ImportData result) {
		super.init(result);
		importData.setFields(
				new ICardField[] { MagicCardField.NAME, MagicCardField.TYPE, MagicCardField.COST, MagicCardField.SET,
						MagicCardField.LANG, MagicCardField.RARITY, MagicCardField.COLLNUM, MagicCardField.ARTIST });
	}

	@Override
	protected void doRun(ICoreProgressMonitor monitor) throws IOException {
		ParserHtmlHelper.StashLoadHandler handler = new ParserHtmlHelper.StashLoadHandler() {
			@Override
			public void handleCard(MagicCard card) {
				super.handleCard(card);
				int id = card.syntesizeId();
				card.setCardId(id);
				importData.add(card);
			}
		};
		String property = importData.getProperty(ImportSource.URL.name());
		if (property == null)
			throw new IOException("URL is not defined");
		property = property.replace("https://scryfall.com/search?", ParseScryFallChecklist.BASE_SEARCH_URL);
		URL url = new URL(property);
		url = new URL(property + "&format=json");
		parser = new ParseScryFallChecklist();
		parser.loadSingleUrl(url, handler);
	}
}
