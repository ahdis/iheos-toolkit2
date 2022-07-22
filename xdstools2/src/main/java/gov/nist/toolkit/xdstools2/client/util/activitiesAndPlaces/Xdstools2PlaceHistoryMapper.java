package gov.nist.toolkit.xdstools2.client.util.activitiesAndPlaces;

import com.google.gwt.place.shared.PlaceHistoryMapper;
import com.google.gwt.place.shared.WithTokenizers;
import gov.nist.toolkit.xdstools2.client.inspector.mvp.SimIndexInspector;
import gov.nist.toolkit.xdstools2.client.inspector.mvp.ResultInspector;
import gov.nist.toolkit.xdstools2.client.tabs.simMsgViewerTab.SimMsgViewer;

/**
 * Monitors PlaceChangeEvents and History events and keep them in sync.
 * It must know all the different Places used in the application.
 *
 * Created by onh2 on 12/10/2015.
 */
@WithTokenizers({
        TestInstance.Tokenizer.class,
        Tool.Tokenizer.class,
        ConfActor.Tokenizer.class,
        SimLog.Tokenizer.class,
        SimMsgViewer.Tokenizer.class,
        ResultInspector.Tokenizer.class,
        SimIndexInspector.Tokenizer.class

})
public interface Xdstools2PlaceHistoryMapper extends PlaceHistoryMapper{
}
