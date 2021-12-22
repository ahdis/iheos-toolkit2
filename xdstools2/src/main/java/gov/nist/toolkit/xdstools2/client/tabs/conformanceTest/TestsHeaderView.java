package gov.nist.toolkit.xdstools2.client.tabs.conformanceTest;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.*;
import gov.nist.toolkit.xdstools2.client.util.ClientFactoryImpl;

/**
 *
 */
class TestsHeaderView {
    private Controller controller;
    private FlowPanel testsHeader = new FlowPanel();
    private HTML selfTestBanner = new HTML();
    private HTML testsHeaderTitle = new HTML();
    private TestOverviewDisplayPanel bar = new TestOverviewDisplayPanel();
    private HTML title = new HTML();
    private HTML testCount = new HTML();
    private HTML successes = new HTML();
    private HTML failures = new HTML();
    private HTML notRun = new HTML();
    private FlexTable table = new FlexTable();
    private String headerText = "";
    private final HTML testsHeaderRunningMessage = new HTML();
    private boolean allowRun = true;

    TestsHeaderView(Controller controller) {
        this.controller = controller;

        testsHeader.add(bar);

        selfTestBanner.addStyleName("warningBanner");
        testsHeader.add(selfTestBanner);

        bar.add(testsHeaderTitle);
        testsHeaderRunningMessage.addStyleName("warningBanner");
        testsHeader.add(testsHeaderRunningMessage);

//        testsHeader.build(testsHeaderTitle);

        testCount.setWidth("12em");

        HTML testsLabel = new HTML("Tests:");
        testsLabel.setWidth("12em");
        table.setWidget(0, 0, testsLabel);
        table.setWidget(0, 1, testCount);
        testCount.setStyleName("testCount");

        table.setText(1, 0, "Successes:");
        table.setWidget(1, 1, successes);
        successes.setStyleName("testSuccess");

        table.setText(2, 0, "Failures: ");
        table.setWidget(2, 1, failures);
        failures.setStyleName("testFail");

        table.setText(3, 0, "Not Run:");
        table.setWidget(3, 1, notRun);
        notRun.setStyleName("testNotRun");

        table.setBorderWidth(2);
        testsHeader.add(table);

        testsHeader.add(new HTML("<hr />"));
    }

    void showRunningMessage(boolean running) {
        if (running)
            testsHeaderRunningMessage.setText("Running...");
        else
            testsHeaderRunningMessage.setText("");
    }

    Widget asWidget() {
        return testsHeader;
    }

    protected void update(TestStatistics testStatistics, String bodyText) {
        this.headerText = bodyText + " Tests";

        testsHeaderTitle.setHTML(headerText);

        bar.clear();
        bar.add(testsHeaderTitle);

        testCount.setHTML(String.valueOf(testStatistics.getTestCount()));

        successes.setHTML(String.valueOf(testStatistics.getSuccesses()));
        failures.setHTML(String.valueOf(testStatistics.getFailures()));
        notRun.setHTML(String.valueOf(testStatistics.getNotRun()));

        // Add controls
        if (allowRun) {
            Image play = new Image("icons2/play-32.png");
            play.setTitle("Run All");
            play.addClickHandler(controller.getRunAllClickHandler());
            play.addStyleName("iconStyle");
            bar.add(play);
        }

        Image refresh = new Image("icons2/refresh-32.png");
        refresh.setTitle("Reload");
        refresh.addClickHandler(controller.getRefreshTestCollectionClickHandler());
        refresh.addStyleName("iconStyle");
        bar.add(refresh);

        Image delete = new Image("icons2/garbage-32.png");
        delete.addStyleName("right");
        delete.addClickHandler(controller.getDeleteAllClickHandler());
        delete.setTitle("Delete All Logs");
        delete.addStyleName("right");
        delete.addStyleName("iconStyle");
        bar.add(delete);

        bar.addStyleName("test-summary");

        if (testStatistics.isAllRun()) {
            if (testStatistics.hasErrors()) {
                bar.labelFailure();
                bar.add(getStatusIcon(false));
            } else {
                bar.labelSuccess();
                bar.add(getStatusIcon(true));
            }
        }
        else if (testStatistics.hasErrors()) {
            bar.labelFailure();
            bar.add(getStatusIcon(false));
        }
        else {
            bar.labelNotRun();
        }

    }

    private Image getStatusIcon(boolean good) {
        Image status;
        if (good) {
            status = new Image("icons2/correct-32.png");
        } else {
            status = new Image(ClientFactoryImpl.getIconsResources().getWarningIcon());
        }
        status.addStyleName("right");
        status.addStyleName("iconStyle");
        return status;
    }

    public void allowRun(boolean allowRun) {
        this.allowRun = allowRun;
    }

    public void showSelfTestWarning(boolean show) {
        if (show)
            selfTestBanner.setText("SELF TEST");
        else
            selfTestBanner.setText("");
    }
}
