package gov.nist.toolkit.xdstools2.client.tabs;

import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;
import gov.nist.toolkit.xdstools2.client.TabContainer;
import gov.nist.toolkit.xdstools2.client.Xdstools2;
import gov.nist.toolkit.xdstools2.client.command.command.GetImplementationVersionCommand;
import gov.nist.toolkit.xdstools2.client.inspector.HyperlinkFactory;
import gov.nist.toolkit.xdstools2.client.siteActorManagers.FindDocumentsSiteActorManager;
import gov.nist.toolkit.xdstools2.client.tabs.genericQueryTab.GenericQueryTab;
import gov.nist.toolkit.xdstools2.client.toolLauncher.ToolLauncher;
import gov.nist.toolkit.xdstools2.client.widgets.HorizontalFlowPanel;

public class HomeTab extends GenericQueryTab {
	private String aboutMessage = null;
	private HorizontalFlowPanel menubar = new HorizontalFlowPanel();

	public HomeTab() {
		super(new FindDocumentsSiteActorManager());
	}

	@Override
	protected Widget buildUI() {
		FlowPanel panel = new FlowPanel();
		panel.add(menubar);

//		menubar.addTest(
//				HyperlinkFactory.launchTool("&nbsp;&nbsp;[" + ToolLauncher.toolConfigTabLabel + "]&nbsp;&nbsp;", new ToolLauncher(ToolLauncher.toolConfigTabLabel))
//		);

		Frame frame = new Frame("site/index.html");
		frame.setSize("100em", "100em");
		panel.add(frame);
		return panel;
	}

	@Override
	protected void bindUI() {
		String th = "";

		try {
			th = Xdstools2.tkProps().get("toolkit.home","");
		} catch (Throwable t) {

		}

		mainGrid = new FlexTable();
		mainGrid.setCellSpacing(20);

		loadIHEGrid(0);
		loadVersion();
	}

	@Override
	protected void configureTabView() {
		addActorReloader();
	}

//	@Override
////	public void onTabLoad(final Xdstools2 container, boolean select, String eventName) {
//	public void onTabLoad(boolean select, String eventName) {
//
//
//
//		select = true;
//		registerTab(select, eventName);
////		tabTopPanel.addTest(new HTML("Menu Bar"));
////		tabTopPanel.
//
//
//	}

	boolean forDirect = false;
	boolean forIHE = false;
	boolean forNwHIN = false;

	class MainGridLoader {

		//@Override
		public void featuresLoadedCallback() {

//				tabTopPanel.addTest(mainGrid);


		}

	}

	public void loadIHEGrid(int startingColumn) {




		Xdstools2.clearMainMenu();

		Xdstools2.addtoMainMenu(addHTML("<h2>Toolkit</h2>"));

		Xdstools2.addtoMainMenu(HyperlinkFactory.launchTool(ToolLauncher.homeTabLabel, new ToolLauncher(ToolLauncher.homeTabLabel)));

		Xdstools2.addtoMainMenu(HyperlinkFactory.launchAdminTool(ToolLauncher.toolConfigTabLabel, new ToolLauncher(ToolLauncher.toolConfigTabLabel)));

		Xdstools2.addtoMainMenu(HyperlinkFactory.launchTool(ToolLauncher.sitesTabLabel, new ToolLauncher(ToolLauncher.sitesTabLabel)));

		Xdstools2.addtoMainMenu(HyperlinkFactory.launchTool(ToolLauncher.pidFavoritesLabel, new ToolLauncher(ToolLauncher.pidFavoritesLabel)));

		Xdstools2.addtoMainMenu(HyperlinkFactory.launchTool(ToolLauncher.simulatorControlTabLabel, new ToolLauncher(ToolLauncher.simulatorControlTabLabel)));

//		Xdstools2.addtoMainMenu(HyperlinkFactory.launchTool(ToolLauncher.simulatorMessageViewTabLabel, new ToolLauncher(ToolLauncher.simulatorMessageViewTabLabel)));

		Xdstools2.addtoMainMenu(HyperlinkFactory.launchTool(ToolLauncher.newSimulatorMessageViewTabLabel, new ToolLauncher(ToolLauncher.newSimulatorMessageViewTabLabel)));

		// **********************************************************************

		Xdstools2.addtoMainMenu(addHTML("<h3>Queries & Retrieves</h3>"));

		Xdstools2.addtoMainMenu(HyperlinkFactory.launchTool(ToolLauncher.findDocumentsTabLabel, new ToolLauncher(ToolLauncher.findDocumentsTabLabel)));

		Xdstools2.addtoMainMenu(HyperlinkFactory.launchTool(ToolLauncher.findDocumentsAllParametersTabLabel, new ToolLauncher(ToolLauncher.findDocumentsAllParametersTabLabel)));

		Xdstools2.addtoMainMenu(HyperlinkFactory.launchTool(ToolLauncher.findDocumentsByRefIdTabLabel, new ToolLauncher(ToolLauncher.findDocumentsByRefIdTabLabel)));

		Xdstools2.addtoMainMenu(HyperlinkFactory.launchTool(ToolLauncher.mpqFindDocumentsTabLabel, new ToolLauncher(ToolLauncher.mpqFindDocumentsTabLabel)));

		Xdstools2.addtoMainMenu(HyperlinkFactory.launchTool(ToolLauncher.getDocumentsTabLabel, new ToolLauncher(ToolLauncher.getDocumentsTabLabel)));

		Xdstools2.addtoMainMenu(HyperlinkFactory.launchTool(ToolLauncher.getRelatedTabLabel, new ToolLauncher(ToolLauncher.getRelatedTabLabel)));

		Xdstools2.addtoMainMenu(HyperlinkFactory.launchTool(ToolLauncher.findFoldersTabLabel, new ToolLauncher(ToolLauncher.findFoldersTabLabel)));

		Xdstools2.addtoMainMenu(HyperlinkFactory.launchTool(ToolLauncher.getFoldersTabLabel, new ToolLauncher(ToolLauncher.getFoldersTabLabel)));

		Xdstools2.addtoMainMenu(HyperlinkFactory.launchTool(ToolLauncher.getFolderAndContentsTabLabel, new ToolLauncher(ToolLauncher.getFolderAndContentsTabLabel)));

		Xdstools2.addtoMainMenu(HyperlinkFactory.launchTool(ToolLauncher.getSubmissionSetTabLabel, new ToolLauncher(ToolLauncher.getSubmissionSetTabLabel)));

		Xdstools2.addtoMainMenu(HyperlinkFactory.launchTool(ToolLauncher.getAllTabLabel, new ToolLauncher(ToolLauncher.getAllTabLabel)));

		Xdstools2.addtoMainMenu(HyperlinkFactory.launchTool(ToolLauncher.documentRetrieveTabLabel, new ToolLauncher(ToolLauncher.documentRetrieveTabLabel)));

//			mainGrid.setWidget(row, col, HyperlinkFactory.launchTool(ToolLauncher.imagingDocumentSetRetrieveTabLabel, new ToolLauncher(ToolLauncher.imagingDocumentSetRetrieveTabLabel)));
		Xdstools2.addtoMainMenu(HyperlinkFactory.launchTool(ToolLauncher.imagingDocumentSetRetrieveTabLabel, new ToolLauncher(ToolLauncher.imagingDocumentSetRetrieveTabLabel)));

		// FhirSearch is tool superseded by Asbestos
//		Xdstools2.addtoMainMenu(HyperlinkFactory.launchTool(ToolLauncher.fhirSearchTabLabel, new ToolLauncher(ToolLauncher.fhirSearchTabLabel)));


		// ***************************************************************************
		// Test data


		Xdstools2.addtoMainMenu(addHTML("<h3>Submit</h3>"));

		Xdstools2.addtoMainMenu(HyperlinkFactory.link(ToolLauncher.registryTestDataTabLabel, new ToolLauncher(ToolLauncher.registryTestDataTabLabel)));

		Xdstools2.addtoMainMenu(HyperlinkFactory.link(ToolLauncher.repositoryTestDataTabLabel, new ToolLauncher(ToolLauncher.repositoryTestDataTabLabel)));

		Xdstools2.addtoMainMenu(HyperlinkFactory.link(ToolLauncher.recipientTestDataTabLabel, new ToolLauncher(ToolLauncher.recipientTestDataTabLabel)));

//		Xdstools2.addtoMainMenu(HyperlinkFactory.link(ToolLauncher.submitResourceTabLabel, new ToolLauncher(ToolLauncher.submitResourceTabLabel)));


		// ***************************************************************************
		// Tools

		Xdstools2.addtoMainMenu(addHTML("<h3>Other Tools</h3>"));

		Xdstools2.addtoMainMenu(HyperlinkFactory.launchTool(ToolLauncher.repositoryTabLabel, new ToolLauncher(ToolLauncher.repositoryTabLabel)));

		Xdstools2.addtoMainMenu(HyperlinkFactory.launchTool(ToolLauncher.connectathonTabLabel, new ToolLauncher(ToolLauncher.connectathonTabLabel)));

//		Xdstools2.addtoMainMenu(HyperlinkFactory.launchTool(ToolLauncher.messageValidatorTabLabel, new ToolLauncher(ToolLauncher.messageValidatorTabLabel)));

		// ***************************************************************************
		// Tests

		Xdstools2.addtoMainMenu(addHTML("<h3>Testing</h3>"));

		Xdstools2.addtoMainMenu(HyperlinkFactory.launchTool(ToolLauncher.conformanceTestsLabel, new ToolLauncher(ToolLauncher.conformanceTestsLabel)));

//		Xdstools2.addtoMainMenu(HyperlinkFactory.launchTool(ToolLauncher.mesaTabLabel, new ToolLauncher(ToolLauncher.mesaTabLabel)));
//
//		Xdstools2.addtoMainMenu(HyperlinkFactory.launchTool(ToolLauncher.igTestsTabLabel, new ToolLauncher(ToolLauncher.igTestsTabLabel)));
//
//		Xdstools2.addtoMainMenu(HyperlinkFactory.launchTool(ToolLauncher.rgTestsTabLabel, new ToolLauncher(ToolLauncher.rgTestsTabLabel)));

//		Xdstools2.addtoMainMenu(HyperlinkFactory.launchTool(ToolLauncher.iigTestsTabLabel, new ToolLauncher(ToolLauncher.iigTestsTabLabel)));

//      Xdstools2.addtoMainMenu(HyperlinkFactory.launchTool(ToolLauncher.rigTestsTabLabel, new ToolLauncher(ToolLauncher.rigTestsTabLabel)));

//		Xdstools2.addtoMainMenu(HyperlinkFactory.launchTool(ToolLauncher.idsTestsTabLabel, new ToolLauncher(ToolLauncher.idsTestsTabLabel)));

//		Xdstools2.addtoMainMenu(HyperlinkFactory.launchTool(ToolLauncher.rsnaedgeTestsTabLabel, new ToolLauncher(ToolLauncher.rsnaedgeTestsTabLabel)));



	}

	int getIndex(ListBox lb, String value) {
		for (int i=0; i<lb.getItemCount(); i++) {
			String lbVal = lb.getItemText(i);
			if (value.equals(lbVal))
				return i;
		}
		return -1;
	}

	public void onTabLoad(TabContainer container, boolean select) {
	}


	public String getWindowShortName() {
		return "home";
	}

	void loadVersion() {

		new GetImplementationVersionCommand(){
			@Override
			public void onComplete(String result) {
				aboutMessage =  "XDS Toolkit\n" + result;
			}
		}.run(getCommandContext());
	}


	public void onTabLoad(TabContainer container, boolean select,
						  String eventName) {

	}




}
