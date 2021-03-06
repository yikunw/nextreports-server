/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ro.nextreports.server.web.core;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.wicketstuff.push.AbstractPushEventHandler;
import org.wicketstuff.push.IPushEventContext;
import org.wicketstuff.push.IPushEventHandler;
import org.wicketstuff.push.IPushNode;
import org.wicketstuff.push.IPushService;
import org.wicketstuff.push.timer.TimerPushService;

import ro.nextreports.server.domain.ReportResultEvent;
import ro.nextreports.server.service.ReportListener;
import ro.nextreports.server.service.ReportService;
import ro.nextreports.server.service.StorageService;
import ro.nextreports.server.web.NextServerSession;
import ro.nextreports.server.web.common.jgrowl.JGrowlAjaxBehavior;
import ro.nextreports.server.web.core.section.SectionManager;
import ro.nextreports.server.web.core.section.tab.ImageTabbedPanel;
import ro.nextreports.server.web.core.section.tab.SectionTab;
import ro.nextreports.server.web.language.LanguageManager;

/**
 * @author Decebal Suiu
 */
public class HomePage extends BasePage {

	private static final long serialVersionUID = 1L;
	
	private transient IPushService pushService;
	private IPushNode<Message> pushNode;
	private Label growlLabel;
	
	@SpringBean
	private SectionManager sectionManager;
	
	@SpringBean
	private ReportService reportService;
	
	@SpringBean
	private StorageService storageService;

    public HomePage(PageParameters parameters) {
    	// clear search context
    	NextServerSession.get().setSearchContext(null);
    	
    	// add sections tab
    	List<ITab> tabs = new ArrayList<ITab>();
        for (String sectionId : sectionManager.getIds()) {
        	tabs.add(new SectionTab(sectionId));
        }        
    	
    	add(new ImageTabbedPanel("tabs", tabs));
    	
    	growlLabel = new Label("growl", "");
    	growlLabel.setOutputMarkupId(true);
    	growlLabel.add(new JGrowlAjaxBehavior());
    	add(growlLabel);    		    	    	
    }   
    
	@Override
	protected void onInitialize() {
		super.onInitialize();
		
    	// push
    	initPush();
    	
    	reportService.addReportListener(new ReportListener() {
			
			@Override
			public void onFinishRun(ReportResultEvent result) {
				if (pushService.isConnected(pushNode)) {
					// forward the Message event via the push service 
					// to the push event handler
					Message message = createMessage(result);
					pushService.publish(pushNode, message);
				}
			}
			
		});
	}

	private void initPush() {
	     // instantiate push event handler
	    IPushEventHandler<Message> handler = new AbstractPushEventHandler<Message>() {

			private static final long serialVersionUID = 1L;

			@Override
			public void onEvent(AjaxRequestTarget target, Message event, IPushNode<Message> node, IPushEventContext<Message> context) {
				int messageType = event.isError() ?	JGrowlAjaxBehavior.ERROR_STICKY : JGrowlAjaxBehavior.INFO_STICKY;		
				getSession().getFeedbackMessages().add(new FeedbackMessage(null, event.getText(), messageType));
				target.add(growlLabel);
			}
			
	    };

	     // obtain a reference to a Push service implementation
	    pushService = TimerPushService.get();

		// install push node into this panel
	    pushNode = pushService.installNode(this, handler);
	}
	
	private Message createMessage(ReportResultEvent event) {				
		StringBuilder sb = new StringBuilder();		
				
		Locale locale = LanguageManager.getInstance().getLocale(storageService.getSettings().getLanguage());
		ResourceBundle bundle = ResourceBundle.getBundle("ro.nextreports.server.web.NextServerApplication", locale);
		String s = bundle.getString("ActionContributor.Run.finish");		
		String message = MessageFormat.format(s, event.getReportName());		
				
		sb.append(message);
		sb.append("<br>");
		boolean error = false;
		if ("".equals(event.getReportUrl())) {
			error = true;
			sb.append(event.getResultMessage());
		} else if (!event.getReportUrl().endsWith("/report")) {
			// indicator and alarm schedule alerts do not have a resulting report (url ends with /report)
			sb.append("<a href=\"").
			   append(event.getReportUrl()).
			   append("\" target=\"_blank\">").
			   append(bundle.getString("ActionContributor.Run.result")).		
			   append("</a>");
		}		
		return new Message(sb.toString(), error);
	}
       
	private class Message  {
		
		private String text;
		private boolean error;

		public Message(String text, boolean error) {
			this.text = text;
			this.error = error;
		}

		public String getText() {
			return text;
		}

		public boolean isError() {
			return error;
		}
		
	}
	
}
