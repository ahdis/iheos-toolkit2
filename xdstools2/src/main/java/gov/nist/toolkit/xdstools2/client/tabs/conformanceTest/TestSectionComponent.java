package gov.nist.toolkit.xdstools2.client.tabs.conformanceTest;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.OpenHandler;
import com.google.gwt.user.client.ui.*;
import gov.nist.toolkit.results.client.TestInstance;
import gov.nist.toolkit.session.client.logtypes.SectionOverviewDTO;
import gov.nist.toolkit.session.client.logtypes.TestOverviewDTO;
import gov.nist.toolkit.session.client.logtypes.TestPartFileDTO;
import gov.nist.toolkit.testenginelogging.client.LogFileContentDTO;
import gov.nist.toolkit.testenginelogging.client.ReportDTO;
import gov.nist.toolkit.testenginelogging.client.TestStepLogContentDTO;
import gov.nist.toolkit.testenginelogging.client.UseReportDTO;
import gov.nist.toolkit.xdstools2.client.command.command.GetSectionTestPartFileCommand;
import gov.nist.toolkit.xdstools2.client.command.command.GetTestLogDetailsCommand;
import gov.nist.toolkit.xdstools2.client.command.command.LoadTestPartContentCommand;
import gov.nist.toolkit.xdstools2.client.sh.BrushFactory;
import gov.nist.toolkit.xdstools2.client.sh.SyntaxHighlighter;
import gov.nist.toolkit.xdstools2.client.util.ClientFactoryImpl;
import gov.nist.toolkit.xdstools2.client.util.ClientUtils;
import gov.nist.toolkit.xdstools2.client.widgets.HorizontalFlowPanel;
import gov.nist.toolkit.xdstools2.client.widgets.PopupMessage;
import gov.nist.toolkit.xdstools2.shared.command.request.GetSectionTestPartFileRequest;
import gov.nist.toolkit.xdstools2.shared.command.request.GetTestLogDetailsRequest;
import gov.nist.toolkit.xdstools2.shared.command.request.LoadTestPartContentRequest;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 *
 */
public class TestSectionComponent implements IsWidget {
    private final HorizontalFlowPanel header = new HorizontalFlowPanel();
    private final DisclosurePanel panel = new DisclosurePanel(header);
    private final String sessionName;
    private final TestInstance testInstance;
    private final FlowPanel body = new FlowPanel();
    private final FlowPanel sectionDescription = new FlowPanel();
    private final FlowPanel sectionResults = new FlowPanel();
    private TestInstance fullTestInstance;
    private SectionOverviewDTO sectionOverview;
    TestRunner testRunner;
    TestSectionComponent me;


    public TestSectionComponent(String sessionName, TestInstance testInstance, SectionOverviewDTO sectionOverview, TestRunner testRunner, boolean allowRun) {
        me = this;
//        this.toolkitService = toolkitService;
        this.sessionName = sessionName;
        this.testInstance = testInstance;
        this.testRunner = testRunner;
        this.sectionOverview = sectionOverview;
        fullTestInstance = new TestInstance(testInstance.getId(), sectionOverview.getName());

        HTML sectionLabel = new HTML("Section: " + sectionOverview.getName());
        sectionLabel.addStyleName("section-title");
        sectionLabel.setTitle("A section can be run independently although frequently a test section depends on the output of a previous section in the test.");
        if (sectionOverview.isRun()) {
            if (sectionOverview.isPass())
                header.addStyleName("testOverviewHeaderSuccess");
            else
                header.addStyleName("testOverviewHeaderFail");
        } else
            header.addStyleName("testOverviewHeaderNotRun");
        header.add(sectionLabel);
        header.add(new HTML(sectionOverview.getDisplayableTime()));
        if (sectionOverview.isRun()) {
            Image status = (sectionOverview.isPass()) ?
                    new Image("icons2/correct-16.png")
                    :
                    new Image(ClientFactoryImpl.getIconsResources().getWarningIcon());
            status.addStyleName("right");
            status.addStyleName("iconStyle");
            header.add(status);

            panel.addOpenHandler(new SectionOpenHandler(new TestInstance(testInstance.getId(), sectionOverview.getName())));
        } else {
            panel.addOpenHandler(new SectionNotRunOpenHandler(sessionName, testInstance, sectionOverview.getName()));
        }
        panel.add(body);

        if (allowRun) {
            Image play = getImg("icons2/play-16.png", "Run", "Play button");
            play.addClickHandler(new RunSection(fullTestInstance));
            header.add(play);
        }

        body.add(sectionDescription);
        sectionDescription.add(new HTML(sectionOverview.getDescription()));
        body.add(sectionResults);
    }

    private Image getImg(String iconFile, String tooltip, String altText) {
        Image imgIcon = new Image(iconFile);
        imgIcon.addStyleName("iconStyle");
        imgIcon.addStyleName("iconStyle_20x20");
        imgIcon.setTitle(tooltip);
        imgIcon.setAltText(altText);
        return imgIcon;
    }

    final static String viewTestplanLabel  = "&boxplus;View Testplan";
    final static String hideTestplanLabel = "&boxminus;Hide Testplan";
    final static String viewMetadataLabel = "&boxplus;View Metadata";
    final static String hideMetadataLabel = "&boxminus;Hide Metadata";


    private class ViewTestplanClickHandler implements ClickHandler {
        private ScrollPanel viewerPanel;
        private HTML ctl;
        private String htmlizedStr;

        public ViewTestplanClickHandler(ScrollPanel testplanViewerPanel, HTML testplanCtl, String secTestplanStr) {
            this.viewerPanel = testplanViewerPanel;
            this.ctl = testplanCtl;
            this.htmlizedStr = secTestplanStr;
        }

        @Override
        public void onClick(ClickEvent clickEvent) {
            viewerPanel.setVisible(!viewerPanel.isVisible());
            if (!viewerPanel.isVisible()) {
                ctl.setHTML(viewTestplanLabel);
            } else {
                ctl.setHTML(hideTestplanLabel);
            }
            if (viewerPanel.getWidget()==null) {
                viewerPanel.add(getShHtml(htmlizedStr));
            }
        }
    }

    private class ViewMetadataClickHandler implements ClickHandler {
        private ScrollPanel viewerPanel;
        private HTML ctl;
        private TestPartFileDTO metadataDTO;

        public ViewMetadataClickHandler(ScrollPanel viewerPanel, HTML ctl, TestPartFileDTO testPartFileDTO) {
            this.viewerPanel = viewerPanel;
            this.ctl = ctl;
            this.metadataDTO = testPartFileDTO;
        }
        @Override
        public void onClick(ClickEvent clickEvent) {
            viewerPanel.setVisible(!viewerPanel.isVisible());
            if (!viewerPanel.isVisible()) {
                ctl.setHTML(viewMetadataLabel);
            } else {
                ctl.setHTML(hideMetadataLabel);
            }
            if (viewerPanel.getWidget()==null) {
                new LoadTestPartContentCommand(){
                    @Override
                    public void onComplete(TestPartFileDTO result) {
                        String metadataStr = result.getHtlmizedContent().replace("<br/>", "\r\n");
                        viewerPanel.add(getShHtml(metadataStr));
                    }
                }.run(new LoadTestPartContentRequest(ClientUtils.INSTANCE.getCommandContext(),metadataDTO));
            }
        }
    }

    private class SectionNotRunOpenHandler implements OpenHandler<DisclosurePanel> {
        String sessionName;
        TestInstance testInstance;
        String section;

        public SectionNotRunOpenHandler(String sessionName, TestInstance testInstance, String section) {
            this.sessionName = sessionName;
            this.testInstance = testInstance;
            this.section = section;
        }

        @Override
        public void onOpen(OpenEvent<DisclosurePanel> openEvent) {
            new GetSectionTestPartFileCommand(){
                @Override
                public void onComplete(TestPartFileDTO sectionTp) {
                    sectionResults.clear();
                    final ScrollPanel testplanViewerPanel = new ScrollPanel();
                    testplanViewerPanel.setVisible(false);
                    final HTML testplanCtl = new HTML(viewTestplanLabel);
                    testplanCtl.addStyleName("iconStyle");
                    testplanCtl.addStyleName("inlineLink");
                    testplanCtl.addClickHandler(new ViewTestplanClickHandler(testplanViewerPanel,testplanCtl,sectionTp.getHtlmizedContent().replace("<br/>", "\r\n")));
                    sectionResults.add(testplanCtl);
                    sectionResults.add(testplanViewerPanel);

                    boolean singleStep = sectionTp.getStepList().size() == 1;
                    for (final String stepName : sectionTp.getStepList()) {
                        HorizontalFlowPanel stepHeader = new HorizontalFlowPanel();

                        HTML stepHeaderTitle = new HTML("Step: " + stepName);
                        stepHeaderTitle.addStyleName("testOverviewHeaderNotRun");
                        stepHeader.add(stepHeaderTitle);

                        DisclosurePanel stepPanel = new DisclosurePanel(stepHeader);
                        stepPanel.setOpen(singleStep);

                        final ScrollPanel metadataViewerPanel = new ScrollPanel();
                        metadataViewerPanel.setVisible(false);
                        String metadataCtlLabel = getMetadataCtlLabel(sectionTp, stepName);
                        final HTML metadataCtl = new HTML(metadataCtlLabel);
                        metadataCtl.addStyleName("iconStyle");
                        metadataCtl.addStyleName("inlineLink");
                        if (sectionTp.getStepTpfMap() != null && sectionTp.getStepTpfMap().get(stepName) != null) {
                            metadataCtl.addClickHandler(new ViewMetadataClickHandler(metadataViewerPanel, metadataCtl, sectionTp.getStepTpfMap().get(stepName)));
                        }

                        final FlowPanel stepResults = new FlowPanel();
                        stepResults.add(metadataCtl);
                        stepResults.add(metadataViewerPanel);

                        stepPanel.add(stepResults);
                        sectionResults.add(stepPanel);
                    }
                }
            }.run(new GetSectionTestPartFileRequest(ClientUtils.INSTANCE.getCommandContext(),testInstance,section));
        }
    }

    protected String getMetadataCtlLabel(TestPartFileDTO sectionTp, String stepName) {
        String metadataCtlLabel;
        if (sectionTp.getStepTpfMap()!=null && sectionTp.getStepTpfMap().get(stepName)!=null) {
            metadataCtlLabel = viewMetadataLabel;
        } else {
            metadataCtlLabel = "No Metadata.";
        }
        return metadataCtlLabel;
    }

    private HTML getShHtml(String xmlStr) {
        return new HTML(SyntaxHighlighter.highlight(xmlStr, BrushFactory.newXmlBrush(), false));
    }


    private class RunSection implements ClickHandler {
        TestInstance testInstance;

        RunSection(TestInstance testInstance) {
            this.testInstance = testInstance;
        }

        @Override
        public void onClick(ClickEvent clickEvent) {
            clickEvent.preventDefault();
            clickEvent.stopPropagation();

            me.testRunner.runTest(testInstance, null, null, new OnTestRunComplete() {
                @Override
                void updateDisplay(TestOverviewDTO testOverviewDTO, InteractionDiagramDisplay diagramDisplay) {
                   // This Class is not used!?
                }
            });
        }
    }

    private class SectionOpenHandler implements OpenHandler<DisclosurePanel> {
        TestInstance testInstance; // must include section name

        SectionOpenHandler(TestInstance testInstance) { this.testInstance = testInstance; }

        @Override
        public void onOpen(OpenEvent<DisclosurePanel> openEvent) {
            new GetTestLogDetailsCommand(){
                @Override
                public void onComplete(final LogFileContentDTO log) {
                    if (log == null) new PopupMessage("section is " + testInstance.getSection());
                    sectionResults.clear();

                    final ScrollPanel testplanViewerPanel = new ScrollPanel();
                    testplanViewerPanel.setVisible(false);
                    final HTML testplanCtl = new HTML(viewTestplanLabel);
                    testplanCtl.addStyleName("iconStyle");
                    testplanCtl.addStyleName("inlineLink");
                    sectionResults.add(testplanCtl);
                    sectionResults.add(testplanViewerPanel);
                    new GetSectionTestPartFileCommand(){
                        @Override
                        public void onComplete(TestPartFileDTO sectionTp) {
                            testplanCtl.addClickHandler(new ViewTestplanClickHandler(testplanViewerPanel,testplanCtl,sectionTp.getHtlmizedContent().replace("<br/>", "\r\n")));

                            int row;
                            if (log.hasFatalError()) body.add(new HTML("Fatal Error: " + log.getFatalError() + "<br />"));
                            boolean singleStep = log.getSteps().size() == 1;
                            for (TestStepLogContentDTO step : log.getSteps()) {
                                String stepName = step.getId();
                                HorizontalFlowPanel stepHeader = new HorizontalFlowPanel();


                                HTML stepHeaderTitle = new HTML("Step: " + step.getId());
                                if (step.isSuccess()) stepHeaderTitle.addStyleName("testOverviewHeaderSuccess");
                                else stepHeaderTitle.addStyleName("testOverviewHeaderFail");
                                stepHeader.add(stepHeaderTitle);

                                DisclosurePanel stepPanel = new DisclosurePanel(stepHeader);
                                stepPanel.setOpen(singleStep);

                                final ScrollPanel metadataViewerPanel = new ScrollPanel();
                                metadataViewerPanel.setVisible(false);
                                String metadataCtlLabel = getMetadataCtlLabel(sectionTp, stepName);
                                final HTML metadataCtl = new HTML(metadataCtlLabel);
                                metadataCtl.addStyleName("iconStyle");
                                metadataCtl.addStyleName("inlineLink");
                                if (sectionTp.getStepTpfMap().get(stepName)!=null) {
                                    metadataCtl.addClickHandler(new ViewMetadataClickHandler(metadataViewerPanel,metadataCtl,sectionTp.getStepTpfMap().get(stepName)));
                                }

                                FlowPanel stepResults = new FlowPanel();
                                stepResults.add(metadataCtl);
                                stepResults.add(metadataViewerPanel);

                                StringBuilder buf = new StringBuilder();
                                buf.append("Goals:<br />");
                                buf.append(sectionOverview.getStep(stepName).getGoals());
//                                List<String> goals = sectionOverview.getStep(stepName).getGoals();
//                                for (String goal : goals)  buf.append("&nbsp;&nbsp;&nbsp;&nbsp;").append(goal).append("<br />");


                                buf.append("Endpoint: " + step.getEndpoint()).append("<br />");
                                if (step.isExpectedSuccess())
                                    buf.append("Expected Status: Success").append("<br />");
                                else
                                    buf.append("Expected Status: Failure").append("<br />");

                                for (String fault : step.getSoapFaults()) {
                                    buf.append("Fault: " + fault).append("<br />");
                                }
                                for (String error : step.getErrors()) {
                                    buf.append("Error: " + error).append("<br />");
                                }
                                for (String assertion : step.getAssertionErrors()) {
                                    buf.append("Error: " + assertion).append("<br />");
                                }
                                stepResults.add(new HTML(buf.toString()));

                                // ******************************************************
                                // IDs
                                // ******************************************************
                                Map<String, String> assignedIds = step.getAssignedIds();
                                Map<String, String> assignedUids = step.getAssignedUids();
                                Set<String> idNames = new HashSet<String>();
                                idNames.addAll(assignedIds.keySet());
                                idNames.addAll(assignedUids.keySet());
                                FlexTable idTable = new FlexTable();
                                idTable.setCellPadding(3);
                                idTable.setStyleName("with-border");
                                row = 0;
                                idTable.setTitle("Assigned IDs");
                                idTable.setWidget(row, 0, new HTML("Object"));
                                idTable.setWidget(row, 1, new HTML("ID"));
                                idTable.setWidget(row, 2, new HTML("UID"));
                                row++;

                                for (String idName : idNames) {
                                    idTable.setWidget(row, 0, new HTML(idName));
                                    if (assignedIds.containsKey(idName))
                                        idTable.setWidget(row, 1, new HTML(assignedIds.get(idName)));
                                    if (assignedUids.containsKey(idName))
                                        idTable.setWidget(row, 2, new HTML(assignedUids.get(idName)));
                                    row++;
                                }
                                stepResults.add(new HTML("IDs"));
                                stepResults.add(idTable);

                                // ******************************************************
                                // UseReports
                                // ******************************************************
                                FlexTable useTable = new FlexTable();
                                useTable.setStyleName("with-border");
                                useTable.setCellPadding(3);
                                row = 0;
                                useTable.setWidget(row, 0, new HTML("Name"));
                                useTable.setWidget(row, 1, new HTML("Value"));
                                useTable.setWidget(row, 2, new HTML("Test"));
                                useTable.setWidget(row, 3, new HTML("Section"));
                                useTable.setWidget(row, 4, new HTML("Step"));
                                row++;
                                List<UseReportDTO> useReports = step.getUseReports();
                                for (UseReportDTO useReport : useReports) {
                                    useTable.setWidget(row, 0, new HTML(useReport.getName()));
                                    useTable.setWidget(row, 1, new HTML(useReport.getValue()));
                                    useTable.setWidget(row, 2, new HTML(useReport.getTest()));
                                    useTable.setWidget(row, 3, new HTML(useReport.getSection()));
                                    useTable.setWidget(row, 4, new HTML(useReport.getStep()));
                                    row++;
                                }
                                stepResults.add(new HTML("Use Reports"));
                                stepResults.add(useTable);

                                // ******************************************************
                                // Reports
                                // ******************************************************
                                FlexTable reportsTable = new FlexTable();
                                reportsTable.setStyleName("with-border");
                                reportsTable.setCellPadding(3);
                                List<ReportDTO> reports = step.getReportDTOs();
                                row = 0;
                                reportsTable.setWidget(row, 0, new HTML("Name"));
                                reportsTable.setWidget(row, 1, new HTML("Value"));
                                row++;
                                for (ReportDTO report : reports) {
                                    reportsTable.setWidget(row, 0, new HTML(report.getName()));
                                    reportsTable.setWidget(row, 1, new HTML(report.getValue()));
                                    row++;
                                }
                                stepResults.add(new HTML("Reports"));
                                stepResults.add(reportsTable);
                                stepPanel.add(stepResults);
                                sectionResults.add(stepPanel);
                            }
                        }
                    }.run(new GetSectionTestPartFileRequest(ClientUtils.INSTANCE.getCommandContext(),testInstance,testInstance.getSection()));
                }
            }.run(new GetTestLogDetailsRequest(ClientUtils.INSTANCE.getCommandContext(),testInstance));

        }
    }

    @Override
    public Widget asWidget() {
        return panel;
    }

}
