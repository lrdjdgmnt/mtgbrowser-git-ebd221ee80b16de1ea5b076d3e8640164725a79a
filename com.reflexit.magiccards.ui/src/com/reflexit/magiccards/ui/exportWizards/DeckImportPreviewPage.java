package com.reflexit.magiccards.ui.exportWizards;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Iterator;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;

import com.reflexit.magiccards.core.DataManager;
import com.reflexit.magiccards.core.MagicException;
import com.reflexit.magiccards.core.exports.ImportData;
import com.reflexit.magiccards.core.exports.ImportError;
import com.reflexit.magiccards.core.exports.ImportSource;
import com.reflexit.magiccards.core.model.IMagicCard;
import com.reflexit.magiccards.core.model.MagicCard;
import com.reflexit.magiccards.core.model.MagicCardField;
import com.reflexit.magiccards.core.model.MagicCardPhysical;
import com.reflexit.magiccards.core.model.abs.ICardField;
import com.reflexit.magiccards.core.model.nav.CardElement;
import com.reflexit.magiccards.ui.MagicUIActivator;
import com.reflexit.magiccards.ui.dialogs.NewSetDialog;
import com.reflexit.magiccards.ui.dnd.CopySupport;
import com.reflexit.magiccards.ui.utils.WaitUtils;
import com.reflexit.magiccards.ui.views.IMagicColumnViewer;
import com.reflexit.magiccards.ui.views.SimpleTableViewer;
import com.reflexit.magiccards.ui.views.columns.AbstractColumn;
import com.reflexit.magiccards.ui.views.columns.ColumnCollection;
import com.reflexit.magiccards.ui.views.columns.GroupColumn;
import com.reflexit.magiccards.ui.views.columns.MagicColumnCollection;
import com.reflexit.magiccards.ui.views.columns.SetColumn;

public class DeckImportPreviewPage extends WizardPage {
	private IMagicColumnViewer viewer;
	private Text text;
	protected ImportData importData;
	private Job thread = new Job("Modify") {
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			if (monitor.isCanceled())
				return Status.CANCEL_STATUS;
			WaitUtils.asyncExec(() -> reload());
			return Status.OK_STATUS;
		}
	};
	private ModifyListener modifyLister = new ModifyListener() {
		@Override
		public void modifyText(ModifyEvent e) {
			DeckImportPage startingPage = getMainPage();
			startingPage.setInputChoice(ImportSource.TEXT);
			CopySupport.runCopy(text.getText());
			importData.setText(text.getText());
			thread.cancel();
			thread.schedule(500);
		}
	};

	protected DeckImportPreviewPage(String pageName) {
		super(pageName);
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible == true) {
			reload();
		}
	}

	public void reload() {
		DeckImportPage startingPage = getMainPage();
		setTitle("Importing format " + startingPage.getReportType().getLabel());
		setErrorMessage(null);
		setDescription(getFirstDescription());
		importData = startingPage.getImportData();
		startingPage.performImport(true);
		safeSetText(importData.getText());
		updateColumns(importData.getFields());
		viewer.setInput(importData.getList());
		validate();
	}

	public void safeSetText(String text2) {
		text.removeModifyListener(modifyLister);
		int k = text2.indexOf('\n');
		if (k == -1)
			k = text2.indexOf('\r');
		text.setText(text2);
		int maxLine = 80 * 40;
		int len = text2.length();
		if (k == -1 && len > maxLine) {
			text.setText(text2.substring(0, maxLine));
			text.setEditable(false);
			return;
		}
		text.setEditable(true);
		text.addModifyListener(modifyLister);
	}

	public void validate() {
		setErrorMessage(null);
		int errorCount = importData.getErrorCount();
		Throwable e = importData.getError();
		if (e != null) {
			MagicUIActivator.log(e);
			setErrorMessage("Cannot parse data file: " + e.getMessage());
		} else if (!importData.isOk())
			setErrorMessage("Cannot parse data file: unknown reason");
		else if (errorCount == 0) {
			setDescription(getFirstDescription());
		} else {
			setErrorMessage(errorCount
					+ " errors during import. Review the cards and fix errors by editing set or name of the card using cell editor");
			// manager.setSortColumn(0, 1);
			setPageComplete(true);
			return;
		}
		setPageComplete(getErrorMessage() == null);
	}

	public void updateColumns(ICardField[] fields) {
		if (fields != null) {
			ColumnCollection colls = viewer.getColumnsCollection();
			AbstractColumn errColumn = colls.getColumn(MagicCardField.ERROR);
			String prefColumns = errColumn.getColumnFullName();
			for (int i = 0; i < fields.length; i++) {
				ICardField field = fields[i];
				AbstractColumn column = colls.getColumn(field);
				if (column != null)
					prefColumns += "," + column.getColumnFullName();
			}
			final String p = prefColumns;
			Display.getDefault().syncExec(() -> viewer.updateColumns(p));
		}
	}

	public String getTextOfFileAsString(InputStream st, int lines) throws FileNotFoundException, IOException {
		String textFile = "";
		if (st != null) {
			String line;
			int i = 0;
			BufferedReader b = new BufferedReader(new InputStreamReader(st));
			while ((line = b.readLine()) != null && i < lines) {
				textFile += line + "\n";
				i++;
			}
		}
		return textFile;
	}

	public DeckImportPage getMainPage() {
		IWizardPage[] pages = getWizard().getPages();
		for (IWizardPage wizardPage : pages) {
			if (wizardPage instanceof DeckImportPage)
				return (DeckImportPage) wizardPage;
		}
		return null;
	}

	public String getFirstDescription() {
		DeckImportPage startingPage = getMainPage();
		int choice = startingPage.getIntoChoice();
		switch (choice) {
		case 1:
			return "Importing into a new deck/collection";
		case 2:
			CardElement element = startingPage.getElement();
			String deckName = element == null ? "newdeck" : element.getName();
			String desc = "Importing into " + deckName + ".";
			return desc;
		case 3:
			return "Extending Magic Card Database";
		default:
			break;
		}
		return "";
	}

	@Override
	public void createControl(Composite parent) {
		setDescription("Import preview (10 rows)");
		Composite comp = new Composite(parent, SWT.NONE);
		setControl(comp);
		comp.setLayout(new GridLayout());
		text = new Text(comp, SWT.WRAP | SWT.BORDER | SWT.V_SCROLL | SWT.MULTI);
		GridData ld = new GridData(GridData.FILL_HORIZONTAL);
		ld.heightHint = text.getLineHeight() * 5;
		text.setLayoutData(ld);
		text.addModifyListener(modifyLister);
		viewer = new SimpleTableViewer(comp, columns);
		GridData tld = new GridData(GridData.FILL_BOTH);
		tld.widthHint = 100 * 5;
		viewer.getControl().setLayoutData(tld);
		Button button = new Button(comp, SWT.PUSH);
		button.setText("Attempt to Auto Fix Errors");
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				Collection<IMagicCard> result = (Collection<IMagicCard>) importData.getList();
				DeckImportPage mainPage = getMainPage();
				int choice = mainPage.getIntoChoice();
				final boolean dbImport = choice == 3;
				try {
					IRunnableWithProgress work = new IRunnableWithProgress() {
						@Override
						public void run(IProgressMonitor monitor)
								throws InvocationTargetException, InterruptedException {
							mainPage.fixErrors(result, dbImport, monitor);
						}
					};
					getRunnableContext().run(true, true, work);
				} catch (InvocationTargetException ite) {
					Throwable e = ite.getCause();
					if (e instanceof OperationCanceledException) {
						reload();
						return;
					}
					importData.setError(e);
					if (e instanceof RuntimeException && !(e instanceof MagicException))
						MagicUIActivator.log(e);
				} catch (InterruptedException e) {
					importData.setError(e);
					reload();
				}
			}
		});
	}

	protected IRunnableContext getRunnableContext() {
		return getContainer();
	}

	public ImportData getPreviewResult() {
		return importData;
	}

	private MagicColumnCollection columns = new MagicColumnCollection(null) {
		@Override
		protected GroupColumn createGroupColumn() {
			return new GroupColumn(true, true, true) {
				@Override
				public Color getForeground(Object element) {
					IMagicCard card = (IMagicCard) element;
					if (card != null && card.getCardId() == 0) {
						if (!card.getEdition().isUnknown())
							return Display.getDefault().getSystemColor(SWT.COLOR_RED);
					}
					return super.getForeground(element);
				}

				@Override
				protected void setElementValue(Object element, Object value) {
					super.setElementValue(element, value);
					validate();
				};
			};
		}

		@Override
		protected SetColumn createSetColumn() {
			return new SetColumn() {
				@Override
				public EditingSupport getEditingSupport(ColumnViewer viewer) {
					return new SetEditingSupport(viewer) {
						@Override
						protected void setValue(Object element, Object value) {
							if (element instanceof MagicCardPhysical) {
								MagicCardPhysical card = (MagicCardPhysical) element;
								String set = (String) value;
								// set
								Collection<IMagicCard> cards = DataManager.getCardHandler().getMagicDBStore()
										.getCandidates(card.getName());
								boolean found = false;
								for (Iterator iterator = cards.iterator(); iterator.hasNext();) {
									IMagicCard base = (IMagicCard) iterator.next();
									if (base.getSet().equals(set)) {
										card.setMagicCard((MagicCard) base);
										card.setError(null);
										found = true;
										break;
									}
								}
								if (!found) {
									NewSetDialog newdia = new NewSetDialog(getShell(), set);
									if (newdia.open() == Window.OK) {
										card.getBase().setSet(newdia.getSet().getName());
										card.setError(null);
									} else {
										card.getBase().setSet(set);
										card.setError(ImportError.SET_NOT_FOUND_ERROR);
									}
								}
								viewer.refresh(true);
								validate();
							}
						}
					};
				}
			};
		}
	};
}
