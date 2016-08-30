<%@page import="com.liferay.portal.kernel.language.LanguageUtil"%>
<%@page import="javax.portlet.PortletURL"%>
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
<%@ include file="/init.jsp"%>
<%
	PortletURL searchURL = renderResponse.createRenderURL();
	searchURL.setParameter("mvcPath", templatePath + "submitinstruction.jsp");
%>
<aui:nav-bar cssClass="opencps-toolbar custom-toolbar">
<div class="form-search">
	<aui:form action="<%= searchURL %>" method="post" name="fm">
		<aui:row>
			<aui:col width="25" cssClass="search-col">
				<datamgt:ddr
					depthLevel="1" 
					dictCollectionCode="GOVERNMENT_AGENCY"
					itemNames="administrationCode"
					itemsEmptyOption="true"
					cssClass="search-input select-box"
					emptyOptionLabels="administrationCode"
					showLabel="false"
				>
				</datamgt:ddr>

			</aui:col>
			<aui:col width="25" cssClass="search-col">
				<datamgt:ddr
					depthLevel="1" 
					dictCollectionCode="SERVICE_DOMAIN"
					itemNames="domainCode"
					itemsEmptyOption="true"	
					cssClass="search-input select-box"
					emptyOptionLabels="domainCode"
					showLabel="false"
				>
				</datamgt:ddr>

			</aui:col>
			<aui:col width="45" cssClass="search-col">
				<liferay-ui:input-search 
					id="keywords1"
					name="keywords"
					title='<%= LanguageUtil.get(locale, "keywords") %>'
					placeholder='<%= LanguageUtil.get(locale, "name") %>' 
					cssClass="search-input input-keyword"
				/>
			</aui:col>
		</aui:row>
	</aui:form>
</div>
</aui:nav-bar>
