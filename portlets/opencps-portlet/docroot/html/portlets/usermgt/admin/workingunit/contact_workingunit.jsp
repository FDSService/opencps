
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

<%@page import="org.opencps.usermgt.model.WorkingUnit"%>
<%@page import="org.opencps.usermgt.search.WorkingUnitDisplayTerms"%>
<%@page import="org.opencps.util.PortletPropsValues"%>
<%@page import="org.opencps.datamgt.service.DictItemLocalServiceUtil"%>
<%@page import="java.util.List"%>
<%@page import="java.util.ArrayList"%>
<%@page import="org.opencps.util.PortletUtil"%>
<%@page import="com.liferay.portal.kernel.log.Log"%>
<%@page import="com.liferay.portal.kernel.log.LogFactoryUtil"%>
<%@page import="org.opencps.util.WebKeys"%>

<%@ include file="../../init.jsp"%>


<%
	WorkingUnit workingUnit =
		(WorkingUnit) request.getAttribute(WebKeys.WORKING_UNIT_ENTRY);
    WorkingUnit workingUnitChild = null;
	long workingUnitId =
		workingUnit != null ? workingUnit.getWorkingunitId() : 0L;
	String isAddChild = ParamUtil.getString(request, "isAddChild");
	String dictItemCityId = "0";
	String dictItemDistrictId = "0";
	String dictItemWardId = "0";
	List<String> regions = new ArrayList<String>();
	try {
		if (workingUnit != null) {
			dictItemCityId = workingUnit.getCityCode();
			dictItemDistrictId = workingUnit.getDistrictCode();
			dictItemWardId = workingUnit.getWardCode();
		}

	}
	catch (Exception e) {

	}
	regions.add(dictItemCityId);
	regions.add(dictItemDistrictId);
	regions.add(dictItemWardId);
	String region = StringUtil.merge(regions);
%>
<c:choose>
    <c:when test="<%=Validator.isNotNull(isAddChild) %>">
        <aui:model-context bean="<%=workingUnitChild%>" model="<%=WorkingUnit.class%>" />
    </c:when>
    <c:otherwise>
        <aui:model-context bean="<%=workingUnit%>" model="<%=WorkingUnit.class%>" />
    </c:otherwise>
</c:choose>

<aui:row cssClass="nav-content-row-2">
	<aui:input name="<%=WorkingUnitDisplayTerms.WORKINGUNIT_ADDRESS%>" cssClass="input100">
		<aui:validator name="required" />
		<aui:validator name="maxLength">500</aui:validator>
	</aui:input>
	
</aui:row>

<aui:row cssClass="nav-content-row-2">
	<datamgt:ddr 
		depthLevel="3"
		dictCollectionCode="<%=PortletPropsValues.DATAMGT_MASTERDATA_ADMINISTRATIVE_REGION%>"
		itemNames="cityCode,districtCode,wardCode"
		itemsEmptyOption="true,true,true" 
		selectedItems="<%= region %>"
		cssClass="input100"
		showLabel="<%=true%>"
		emptyOptionLabels="cityId,districtId,wardId"
	>
	</datamgt:ddr>
</aui:row>

<aui:row cssClass="nav-content-row-2">
	<aui:col width="50">
		<aui:input name="<%=WorkingUnitDisplayTerms.WORKINGUNIT_TELNO%>" cssClass="input100">
			<aui:validator name="minLength">10</aui:validator>
			<aui:validator name="number"></aui:validator>
		</aui:input>
	</aui:col>
	<aui:col width="50">
		<aui:input name="<%=WorkingUnitDisplayTerms.WORKINGUNIT_FAXNO%>" cssClass="input100">
			<aui:validator name="minLength">5</aui:validator>
			<aui:validator name="number"></aui:validator>
		</aui:input>
	</aui:col>
</aui:row>

<aui:row cssClass="nav-content-row-2">
	<aui:col width="50">
		<aui:input name="<%=WorkingUnitDisplayTerms.WORKINGUNIT_EMAIL%>" cssClass="input100">
			<aui:validator name="email" />
			<aui:validator name="required" />
			<aui:validator name="maxLength">255</aui:validator>
		</aui:input>
	</aui:col>
	<aui:col width="50">
		<aui:input name="<%=WorkingUnitDisplayTerms.WORKINGUNIT_WEBSITE%>" cssClass="input100">
			<aui:validator name="url" />
			<aui:validator name="maxLength">1000</aui:validator>
		</aui:input>
	</aui:col>
</aui:row>

<%!
	Log _log =	LogFactoryUtil.getLog("html.portlets.usermgt.admin.workingunit.contact_workingunit.jsp");
%>