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
package ro.nextreports.server.web.common.misc;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.attributes.AjaxCallListener;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.model.IModel;

/**
 * @author Decebal Suiu
 */
public abstract class AjaxConfirmLink<T> extends AjaxLink<T> {

	private static final long serialVersionUID = 1L;

	protected String message;
	
	public AjaxConfirmLink(String id) {
		super(id);
	}

	public AjaxConfirmLink(String id, String message) {
		super(id);
		
		this.message = message;
	}

	public AjaxConfirmLink(String id, IModel<T> object) {
		super(id, object);
	}

	public AjaxConfirmLink(String id, IModel<T> object, String message) {
		super(id, object);
		
		this.message = message;
	}

    public boolean showDialog() {
        return true;
    }	

	public String getMessage() {
		return message;
	}

	@Override
	protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
		super.updateAjaxAttributes(attributes);
		
		if (StringUtils.isNotEmpty(getMessage()) && showDialog()) {
			String message = getMessage().replaceAll("'", "\"");
			StringBuilder precondition = new StringBuilder("if(!confirm('").append(message).append("')) { return false; };");
			
			AjaxCallListener listener = new AjaxCallListener();
			listener.onPrecondition(precondition);
			
			attributes.getAjaxCallListeners().add(listener);
		}
	}
	
}
