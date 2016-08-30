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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
%>

<%@page import="org.opencps.processmgt.permissions.ProcessPermission"%>

<%@ include file="../init.jsp"%>


<%
	PortletURL searchURL = renderResponse.createRenderURL();


	boolean isPermission =
	ProcessPermission.contains(
	    themeDisplay.getPermissionChecker(),
	    themeDisplay.getScopeGroupId(), ActionKeys.ADD_PROCESS);

%>
<aui:nav-bar cssClass="opencps-toolbar custom-toolbar">
	<aui:nav id="toolbarContainer" cssClass="nav-display-style-buttons pull-left" >
		<c:if test="<%= isPermission %>">
			<portlet:renderURL var="editProcessURL">
				<portlet:param name="mvcPath" value='<%= templatePath + "edit_process.jsp" %>'/>
				<portlet:param name="redirectURL" value="<%=currentURL %>"/>
				<portlet:param name="backURL" value="<%=currentURL %>"/>
			</portlet:renderURL>
			<aui:button icon="icon-plus" href="<%=editProcessURL %>" cssClass="action-button" value="add-process"/>
		</c:if>
	</aui:nav>
	<aui:nav-bar-search cssClass="pull-right">
		<div class="form-search">
			<aui:form action="<%= searchURL %>" method="post" name="fm">
				<div class="toolbar_search_input">
					<liferay-ui:input-search 
						id="keywords1"
						name="keywords"
						title='<%= LanguageUtil.get(locale, "keywords") %>'
						placeholder='<%= LanguageUtil.get(locale, "name") %>'
						cssClass="search-input input-keyword"
					/>
				</div>
			</aui:form>
		</div>
	</aui:nav-bar-search>
</aui:nav-bar>

<%!
	private Log _log = LogFactoryUtil.getLog("html.portlets.processmgt.admin.toolbar.jsp");
%>