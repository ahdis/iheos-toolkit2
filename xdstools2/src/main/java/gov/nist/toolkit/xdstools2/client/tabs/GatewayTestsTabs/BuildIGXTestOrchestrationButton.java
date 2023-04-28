package gov.nist.toolkit.xdstools2.client.tabs.GatewayTestsTabs;

import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import gov.nist.toolkit.actortransaction.shared.ActorType;
import gov.nist.toolkit.actortransaction.shared.OptionType;
import gov.nist.toolkit.configDatatypes.client.Pid;
import gov.nist.toolkit.installation.shared.TestSession;
import gov.nist.toolkit.services.client.*;
import gov.nist.toolkit.sitemanagement.client.SiteSpec;
import gov.nist.toolkit.xdstools2.client.command.command.BuildIGXTestOrchestrationCommand;
import gov.nist.toolkit.xdstools2.client.tabs.FindDocumentsLauncher;
import gov.nist.toolkit.xdstools2.client.tabs.conformanceTest.ActorOptionConfig;
import gov.nist.toolkit.xdstools2.client.tabs.conformanceTest.ConformanceTestTab;
import gov.nist.toolkit.xdstools2.client.tabs.conformanceTest.SiteDisplay;
import gov.nist.toolkit.xdstools2.client.tabs.conformanceTest.TestContext;
import gov.nist.toolkit.xdstools2.client.tabs.conformanceTest.TestContextView;
import gov.nist.toolkit.xdstools2.client.tabs.conformanceTest.TestRunner;
import gov.nist.toolkit.xdstools2.client.tabs.genericQueryTab.GenericQueryTab;
import gov.nist.toolkit.xdstools2.client.util.ClientUtils;
import gov.nist.toolkit.xdstools2.client.widgets.OrchestrationSupportTestsDisplay;
import gov.nist.toolkit.xdstools2.client.widgets.PopupMessage;
import gov.nist.toolkit.xdstools2.client.widgets.buttons.AbstractOrchestrationButton;
import gov.nist.toolkit.xdstools2.shared.command.request.BuildIgxTestOrchestrationRequest;

/**
 * Build Orchestration for testing an Inititating Gateway.
 * This code id tied to the button that launches it.
 */
public class BuildIGXTestOrchestrationButton extends AbstractOrchestrationButton {
    private ConformanceTestTab testTab;
    private boolean includeIGX;
    private TestContext testContext;
    private TestContextView testContextView;
    private TestRunner testRunner;
    private ActorOptionConfig actorOption;
    private Panel initializationPanel;
    private FlowPanel initializationResultsPanel = new FlowPanel();

    public BuildIGXTestOrchestrationButton(ConformanceTestTab testTab, Panel initializationPanel, String label, TestContext testContext, TestContextView testContextView, TestRunner testRunner, boolean includeIGX, ActorOptionConfig actorOption
    ) {
        this.initializationPanel = initializationPanel;
        this.testTab = testTab;
        this.testContext = testContext;
        this.testContextView = testContextView;
        this.testRunner = testRunner;
        this.actorOption = actorOption;

        setParentPanel(initializationPanel);
        this.includeIGX = includeIGX;

        FlowPanel customPanel = new FlowPanel();

        HTML instructions = new HTML(
            "<p>The system under test is an Initiating Gateway. " +
            "Unlike the stock XDS test structure, this test environment does NOT assume that the Gateway under test is triggered by IHE transactions. " +
            "It is up to the person who owns the Initiating Gateway under test to trigger the transactions required by the test cases. " +
            "This test environment does not send Patient Identity Feed transactions to the system under test. " +
            "Test patients are documented externally or in README tests below." +
            "</p><br />"
        );

        customPanel.add(instructions);
        setCustomPanel(customPanel);

        setSystemDiagramUrl("diagrams/igx_testing_environment.png");

        build();
        panel().add(initializationResultsPanel);
    }


    private SiteSpec siteUnderTest(IgxOrchestrationResponse orchResponse) {
        if ((OptionType.AFFINITY_DOMAIN.equals(actorOption.getOptionId()) || OptionType.XUA.equals(actorOption.getOptionId()) && testContext.getSiteUnderTest() != null)) {
            return testContext.getSiteUnderTest().siteSpec();
        }
        return orchResponse.getSupportRG1().siteSpec();
    }

    public void orchestrate() {
        if (GenericQueryTab.empty(testTab.getCurrentTestSession())) {
            new PopupMessage("Must select test session first");
            return;
        }
        IgxOrchestrationRequest request = new IgxOrchestrationRequest();
        request.setUseExistingState(!isResetRequested());
        request.setUseTls(isTls());
        request.setTestSession(new TestSession(testTab.getCurrentTestSession()));
        request.setIncludeLinkedIGX(includeIGX);

        initializationResultsPanel.clear();

        new BuildIGXTestOrchestrationCommand(){
            @Override
            public void onComplete(RawResponse rawResponse) {
//                if (handleError(rawResponse, IgxOrchestrationResponse.class)) return;
                IgxOrchestrationResponse orchResponse = (IgxOrchestrationResponse) rawResponse;
                testTab.setOrchestrationResponse(orchResponse);
                SiteSpec siteUnderTest = siteUnderTest(orchResponse);
                testTab.setSiteToIssueTestAgainst(siteUnderTest);
                if (OptionType.AFFINITY_DOMAIN.equals(actorOption.getOptionId()))
                    orchResponse.setExternalStart(true);

                initializationResultsPanel.add(new HTML("Initialization complete"));

                if (testContext.getSiteUnderTest() != null) {
                    initializationResultsPanel.add(new SiteDisplay("System Under Test Configuration", testContext.getSiteUnderTest()));
                }

                if (orchResponse.getSupportRG1() != null) {
                    initializationResultsPanel.add(new SiteDisplay("Supporting Environment Configuration", orchResponse.getSupportRG1()));
                }
                if (orchResponse.getSupportRG2() != null) {
                    initializationResultsPanel.add(new SiteDisplay("", orchResponse.getSupportRG2()));
                }

                handleMessages(initializationResultsPanel, orchResponse);

                initializationResultsPanel.add(new OrchestrationSupportTestsDisplay(orchResponse, testContext, testContextView, testRunner, testTab ));

                initializationResultsPanel.add(new HTML("<br />"));

/*
                FlexTable table = new FlexTable();
                initializationResultsPanel.add(table);
                int row = 0;

                Widget w;

                table.setWidget(row, 0, new HTML("<h3>Test data pattern</h3>"));
                table.setWidget(row++, 1, new HTML("<h3>Patient ID</h3>"));

                // FindDocuments launcher need actor type
                SiteSpec siteSpecRg1 = orchResponse.getSupportRG1().siteSpec();
                siteSpecRg1.setActorType(ActorType.RESPONDING_GATEWAY);
                SiteSpec siteSpecRg2 = orchResponse.getSupportRG2().siteSpec();
                siteSpecRg2.setActorType(ActorType.RESPONDING_GATEWAY);

                table.setWidget(row, 0, buildFindDocumentsLauncher(siteSpecRg1, orchResponse.getOneDocPid(), "Single document in Community 1"));
                table.setWidget(row++, 1, new HTML(orchResponse.getOneDocPid().asString()));

                table.setWidget(row, 0, buildFindDocumentsLauncher(siteSpecRg1, orchResponse.getTwoDocPid(), "Two documents in Community 1"));
                table.setWidget(row++, 1, new HTML(orchResponse.getTwoDocPid().asString()));

                table.setWidget(row, 0, new HTML("Both Communities have single document"));
                table.setWidget(row++, 1, new HTML(orchResponse.getTwoRgPid().asString()));
                table.setWidget(row++, 0, buildFindDocumentsLauncher(siteSpecRg1, orchResponse.getTwoRgPid(), "Community 1"));
                table.setWidget(row++, 0, buildFindDocumentsLauncher(siteSpecRg2, orchResponse.getTwoRgPid(), "Community 2"));

                table.setWidget(row, 0, new HTML("Error management Patient ID"));
                table.setWidget(row++, 1, new HTML(orchResponse.getUnknownPid().asString()));
                table.setWidget(row++, 0, buildFindDocumentsLauncher(siteSpecRg1, orchResponse.getUnknownPid(), "Community 1 returns Registry Error"));
                table.setWidget(row++, 0, buildFindDocumentsLauncher(siteSpecRg2, orchResponse.getUnknownPid(), "Community 2 returns single document"));

                table.setWidget(row, 0, buildFindDocumentsLauncher(siteSpecRg1, orchResponse.getNoAdOptionPid(), "No XDS Affinity Domain Option"));
                table.setWidget(row++, 1, new HTML(orchResponse.getNoAdOptionPid().asString()));
*/
//                initializationResultsPanel.add(new HTML("<h3>Configure your Initiating Gateway to forward requests to both of the above Responding Gateways (listed under Supporting Environment Configuration).</h3><hr />"));

                testTab.displayTestCollection(testTab.getMainView().getTestsPanel());

            }
        }.run(new BuildIgxTestOrchestrationRequest(ClientUtils.INSTANCE.getCommandContext(),request));
    }



    Anchor buildFindDocumentsLauncher(SiteSpec siteSpec, Pid pid, String title) {
        Anchor a = new Anchor(title);
        try {
            a.addClickHandler(new FindDocumentsLauncher(pid, siteSpec, false));
        } catch (Exception e) {
            new PopupMessage(e.getMessage() + " for " + title);
        }
        return a;
    }

    Widget light(Widget w) {
        w.getElement().getStyle().setProperty("backgroundColor", "#f0f0f0");
        return w;
    }

    Widget dark(Widget w) {
        w.getElement().getStyle().setProperty("backgroundColor", "#d3d3d3");
        return w;
    }
}
