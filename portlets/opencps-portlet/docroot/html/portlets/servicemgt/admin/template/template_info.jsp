
<%
/**
 * OpenCPS is the open source Core Public Services software
 * Copyright (C) 2016-present OpenCPS community
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>
 */
%>
<%@page import="com.liferay.portlet.documentlibrary.service.DLFileEntryLocalServiceUtil"%>
<%@page import="com.liferay.portlet.documentlibrary.model.DLFileEntry"%>
<%@page import="org.opencps.servicemgt.model.TemplateFile"%>

<%@ include file="../../init.jsp" %>

<%
	TemplateFile templateFile = (TemplateFile) request.getAttribute(WebKeys.SERVICE_TEMPLATE_ENTRY);
	String url = StringPool.BLANK;
	
	DLFileEntry dlFileEntry = null;
	if(Validator.isNotNull(templateFile)) {
		try {
			dlFileEntry = DLFileEntryLocalServiceUtil.getDLFileEntry(templateFile.getFileEntryId());
			url = themeDisplay.getPortalURL()+"/c/document_library/get_file?uuid="+dlFileEntry.getUuid()+"&groupId="+themeDisplay.getScopeGroupId();
		} catch (Exception e) {}
	}
%>

<aui:model-context bean="<%= templateFile %>" model="<%= TemplateFile.class %>"/>

<aui:row cssClass="nav-content-row">
	<aui:col>
		<aui:input name="fileNo"></aui:input>
	</aui:col>
</aui:row>

<aui:row cssClass="nav-content-row">
	<aui:col>
		<aui:input name="fileName"></aui:input>
	</aui:col>
</aui:row>

<c:if test="<%=Validator.isNotNull(url) %>">
	<a href="<%=url%>"><span style="color: blue"><liferay-ui:message key="url.file.entry" /></span></a>
</c:if>

<aui:row cssClass="nav-content-row">
	<aui:col>
		<aui:input name="uploadedFile" type="file"/>
	</aui:col>
</aui:row>




