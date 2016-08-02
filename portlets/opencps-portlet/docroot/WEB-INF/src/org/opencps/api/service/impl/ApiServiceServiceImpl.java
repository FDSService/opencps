/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package org.opencps.api.service.impl;

import org.opencps.api.model.ApiService;
import org.opencps.api.service.base.ApiServiceServiceBaseImpl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.opencps.api.service.ApiServiceLocalServiceUtil;
import org.opencps.backend.message.SendToEngineMsg;
import org.opencps.dossiermgt.NoSuchDossierPartException;
import org.opencps.dossiermgt.bean.ProcessOrderBean;
import org.opencps.dossiermgt.model.Dossier;
import org.opencps.dossiermgt.model.DossierFile;
import org.opencps.dossiermgt.model.DossierPart;
import org.opencps.dossiermgt.service.DossierFileLocalServiceUtil;
import org.opencps.dossiermgt.service.DossierLocalServiceUtil;
import org.opencps.dossiermgt.service.DossierPartLocalServiceUtil;
import org.opencps.processmgt.NoSuchProcessOrderException;
import org.opencps.processmgt.model.ProcessOrder;
import org.opencps.processmgt.model.ProcessWorkflow;
import org.opencps.processmgt.search.ProcessOrderDisplayTerms;
import org.opencps.processmgt.service.ProcessOrderLocalServiceUtil;
import org.opencps.processmgt.service.ProcessWorkflowLocalServiceUtil;
import org.opencps.servicemgt.model.ServiceInfo;
import org.opencps.servicemgt.service.ServiceInfoLocalServiceUtil;
import org.opencps.util.DLFolderUtil;
import org.opencps.util.DateTimeUtil;
import org.opencps.util.PortletConstants;

import com.liferay.portal.NoSuchUserException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.jsonwebservice.JSONWebService;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.messaging.Message;
import com.liferay.portal.kernel.messaging.MessageBusUtil;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.portal.security.auth.PrincipalException;
import com.liferay.portal.service.CompanyLocalServiceUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.service.ServiceContextThreadLocal;
import com.liferay.portal.service.UserLocalServiceUtil;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.documentlibrary.model.DLFolder;
import com.liferay.portlet.documentlibrary.service.DLAppLocalServiceUtil;
import com.liferay.portlet.documentlibrary.util.DLUtil;

/**
 * The implementation of the api service remote service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link org.opencps.api.service.ApiServiceService} interface.
 *
 * <p>
 * This is a remote service. Methods of this service are expected to have security checks based on the propagated JAAS credentials because this service can be accessed remotely.
 * </p>
 *
 * @author trungdk
 * @see org.opencps.api.service.base.ApiServiceServiceBaseImpl
 * @see org.opencps.api.service.ApiServiceServiceUtil
 */
public class ApiServiceServiceImpl extends ApiServiceServiceBaseImpl {
	
	@JSONWebService(value = "dossiers", method = "GET")
	public JSONObject searchDossierByUserAssignProcessOrder(String username)
			throws SystemException {
		
		_log.debug("searchDossierByUserAssignProcessOrder===username==============" + username);
		
		JSONObject resultObj = JSONFactoryUtil.createJSONObject();
		
		try {
			User user = UserLocalServiceUtil.fetchUserByEmailAddress(getUser().getCompanyId(), username);
			
			long userId = 0;
			
			if (user != null) {
				userId = user.getUserId();
			}
			
			SimpleDateFormat sdf = new SimpleDateFormat(DateTimeUtil._VN_DATE_TIME_FORMAT);
			int serviceInfoId = 0;
			int processStepId = 0;
			
			int count = ProcessOrderLocalServiceUtil.countProcessOrder(serviceInfoId, processStepId, userId, userId);
			List<ProcessOrderBean> processOrders = ProcessOrderLocalServiceUtil.searchProcessOrder(serviceInfoId, processStepId, userId, userId,
					0, count, 
					null);
			List<Dossier> dossiers = new ArrayList<Dossier>();
			for (ProcessOrderBean pd : processOrders) {
				try {
					dossiers.add(DossierLocalServiceUtil.getDossier(pd.getDossierId()));
				} catch (PortalException e) {
					_log.error(e);
				}
			}
			
			JSONArray resultArr = JSONFactoryUtil.createJSONArray();
			for (Dossier d : dossiers) {
				userId = d.getUserId();
				JSONObject dossierObj = JSONFactoryUtil.createJSONObject();
				dossierObj.put("oid", d.getOid());
				
				ServiceInfo serviceInfo = null;
				
				try {
					serviceInfo = ServiceInfoLocalServiceUtil.getServiceInfo(d
							.getServiceInfoId());
					dossierObj.put("serviceNo", serviceInfo.getServiceNo());
					dossierObj.put("serviceName", serviceInfo.getServiceName());
				} catch (Exception e) {
					_log.error(e);
					dossierObj.put("serviceNo", "");
					dossierObj.put("serviceName", "");
				}
				
				dossierObj.put("subjectName", d.getSubjectName());
				dossierObj.put("address", d.getAddress());
				if (d.getSubmitDatetime() != null) {
					dossierObj.put("submitDatetime",
							sdf.format(d.getSubmitDatetime()));
				}
				if (d.getReceiveDatetime() != null) {
					dossierObj.put("receiveDatetime",
							sdf.format(d.getReceiveDatetime()));
				}
				dossierObj.put("receptionNo", d.getReceptionNo());
				if (d.getEstimateDatetime() != null) {
					dossierObj.put("estimateDatetime", d.getEstimateDatetime());
				}
				dossierObj.put("dossierStatus", d.getDossierStatus());
				dossierObj.put("delayStatus", d.getDelayStatus());
				resultArr.put(dossierObj);
			}

			resultObj.put("statusCode", "Success");
			resultObj.put("data", resultArr);
			
			ServiceContext serviceContext = new ServiceContext();
			serviceContext.setUserId(getUser().getUserId());
			serviceContext.setScopeGroupId(getUser().getGroupId());
			serviceContext.setCompanyId(getUser().getCompanyId());

			String ipAddress = PortalUtil.getComputerAddress();

			ApiServiceLocalServiceUtil.addApiService(getUser().getUserId(),
					"02", ipAddress, "", "{ 'username': '" + username
							+ "' }", "success", serviceContext);
		}
		catch (Exception e) {
			_log.error(e);
		}	
			
		return resultObj;
	}

	@JSONWebService(value = "dossiers", method = "GET")
	public JSONObject searchDossierByProcessStepAndUser(String processno,
			String stepno, String username) {
		
		_log.debug("searchDossierByProcessStepAndUser===processno==============" + processno);
		_log.debug("searchDossierByProcessStepAndUser===stepno==============" + stepno);
		_log.debug("searchDossierByProcessStepAndUser===username==============" + username);
		
		JSONObject resultObj = JSONFactoryUtil.createJSONObject();
		SimpleDateFormat sdf = new SimpleDateFormat(
				DateTimeUtil._VN_DATE_TIME_FORMAT);
		
		try {
			int count = dossierLocalService.countDossierByP_PS_U(processno, stepno,
					username);
			List<Dossier> dossiers = dossierLocalService.searchDossierByP_PS_U(
					processno, stepno, username, 0, count);
			JSONArray resultArr = JSONFactoryUtil.createJSONArray();
			for (Dossier d : dossiers) {
				JSONObject dossierObj = JSONFactoryUtil.createJSONObject();
				dossierObj.put("oid", d.getOid());
				ServiceInfo serviceInfo = null;
				try {
					serviceInfo = ServiceInfoLocalServiceUtil.getServiceInfo(d
							.getServiceInfoId());
					dossierObj.put("serviceNo", serviceInfo.getServiceNo());
					dossierObj.put("serviceName", serviceInfo.getServiceName());
				} catch (Exception e) {
					dossierObj.put("serviceNo", "");
					dossierObj.put("serviceName", "");
				}
				
				dossierObj.put("subjectName", d.getSubjectName());
				dossierObj.put("address", d.getAddress());
				if (d.getSubmitDatetime() != null) {
					dossierObj.put("submitDatetime",
							sdf.format(d.getSubmitDatetime()));
				}
				if (d.getReceiveDatetime() != null) {
					dossierObj.put("receiveDatetime",
							sdf.format(d.getReceiveDatetime()));
				}
				dossierObj.put("receptionNo", d.getReceptionNo());
				if (d.getEstimateDatetime() != null) {
					dossierObj.put("estimateDatetime", d.getEstimateDatetime());
				}
				dossierObj.put("dossierStatus", d.getDossierStatus());
				dossierObj.put("delayStatus", d.getDelayStatus());
				resultArr.put(dossierObj);
			}

			resultObj.put("statusCode", "Success");
			resultObj.put("data", resultArr);

			try {
				ServiceContext serviceContext = new ServiceContext();
				serviceContext.setUserId(getUser().getUserId());
				serviceContext.setScopeGroupId(getUser().getGroupId());
				serviceContext.setCompanyId(getUser().getCompanyId());
				String ipAddress = PortalUtil.getComputerAddress();

				JSONObject params = JSONFactoryUtil.createJSONObject();
				params.put("processno", processno);
				params.put("stepno", stepno);
				params.put("username", username);

				ApiServiceLocalServiceUtil.addApiService(getUser().getUserId(),
						"01", ipAddress, "", params.toString(), "success",
						serviceContext);
			} catch (SystemException se) {

			} catch (PortalException pe) {

			}	
		}
		catch (NoSuchUserException e) {
			resultObj.put("statusCode", "UserNotFound");
		}
		

		return resultObj;
	}

	@JSONWebService(value = "dossiers", method = "GET")
	public JSONObject getByoid(String oid) {
		
		_log.debug("getByoid===oid==============" + oid);
		
		try {
			ServiceContext serviceContext = getServiceContext();
			
			_log.info("===getByoid===getRemoteAddr===" + serviceContext.getRemoteAddr());
		} catch(Exception e) {
			_log.error(e);
		}
		
		JSONObject jsonObject = JSONFactoryUtil.createJSONObject();
		Dossier dossier;
		try {
			SimpleDateFormat sdf = new SimpleDateFormat(DateTimeUtil._VN_DATE_TIME_FORMAT);
		
			dossier = dossierLocalService.getByoid(oid);
			
			if (dossier != null) {
				jsonObject.put("oid", dossier.getOid());
				ServiceInfo serviceInfo = null;
				
				try {
					serviceInfo = ServiceInfoLocalServiceUtil
							.getServiceInfo(dossier.getServiceInfoId());
					jsonObject.put("serviceNo", serviceInfo.getServiceNo());
					jsonObject.put("serviceName", serviceInfo.getServiceName());
				} catch (Exception e) {
					_log.error(e);
					jsonObject.put("serviceNo", "");
					jsonObject.put("serviceName", "");
				}
				
				jsonObject.put("govAgencyCode", dossier.getGovAgencyCode());
				jsonObject.put("govAgencyName", dossier.getGovAgencyName());
				jsonObject.put("subjectName", dossier.getSubjectName());
				jsonObject.put("address", dossier.getAddress());
				jsonObject.put("cityCode", dossier.getCityCode());
				jsonObject.put("cityName", dossier.getCityName());
				jsonObject.put("districtCode", dossier.getDistrictCode());
				jsonObject.put("districtName", dossier.getDistrictName());
				jsonObject.put("wardCode", dossier.getWardCode());
				jsonObject.put("wardName", dossier.getWardName());
				jsonObject.put("contactName", dossier.getContactName());
				jsonObject.put("contactTelNo", dossier.getContactTelNo());
				jsonObject.put("contactEmail", dossier.getContactEmail());
				jsonObject.put("note", dossier.getNote());
				
				if (dossier.getSubmitDatetime() != null) {
					jsonObject.put("submitDatetime",
							sdf.format(dossier.getSubmitDatetime()));
				}
				if (dossier.getReceiveDatetime() != null) {
					jsonObject.put("receiveDatetime",
							sdf.format(dossier.getReceiveDatetime()));
				}
				
				jsonObject.put("receptionNo", dossier.getReceptionNo());
				
				if (dossier.getEstimateDatetime() != null) {
					jsonObject.put("estimateDatetime", sdf.format(dossier.getEstimateDatetime()));
				}
				
				if (dossier.getFinishDatetime() != null) {
					jsonObject.put("finishDatetime",
							sdf.format(dossier.getFinishDatetime()));
				}
				
				jsonObject.put("dossierStatus", dossier.getDossierStatus());
				jsonObject.put("delayStatus", dossier.getDelayStatus());

				List<DossierFile> dossierFiles = DossierFileLocalServiceUtil
						.getDossierFileByDossierId(dossier.getDossierId());
				JSONArray dfArr = JSONFactoryUtil.createJSONArray();
				
				for (DossierFile df : dossierFiles) {
					JSONObject jsonDossierFile = JSONFactoryUtil.createJSONObject();
					
					jsonDossierFile.put("dossierFileOid", df.getOid());
					jsonDossierFile.put("dossierFileURL", "");
					
					try {
						DossierPart dpart = DossierPartLocalServiceUtil.getDossierPart(df.getDossierPartId());
						jsonDossierFile.put("dossierPartNo", dpart.getPartNo());
					} catch (Exception e) {
						_log.error(e);
					} 
					
					jsonDossierFile.put("dossierFileName", df.getDisplayName());
					jsonDossierFile.put("templateFileNo", df.getTemplateFileNo());
					jsonDossierFile.put("dossierFileNo", df.getDossierFileNo());
					
					if (df.getFileEntryId() > 0) {
						try {
							FileEntry fileEntry = DLAppLocalServiceUtil.getFileEntry(df.getFileEntryId());
							jsonDossierFile.put("dossierFullFileName", df.getDisplayName() + "." + fileEntry.getExtension());
							
							String url = getFileURL(fileEntry);

							jsonDossierFile.put("dossierFileURL", url);

						} catch (Exception e) {
							_log.error(e);
						}
					} 
					
					if (Validator.isNotNull(df.getFormData())) {
						jsonDossierFile.put("dossierFileContent", df.getFormData());
					}
					
					if (df.getDossierFileDate() != null) {
						jsonDossierFile.put("dossierFileDate", sdf.format(df.getDossierFileDate()));
					}
					
					dfArr.put(jsonDossierFile);
				}

				jsonObject.put("dossierFiles", dfArr);

				try {
					ServiceContext serviceContext = new ServiceContext();
					serviceContext.setUserId(getUser().getUserId());
					serviceContext.setScopeGroupId(getUser().getGroupId());
					serviceContext.setCompanyId(getUser().getCompanyId());
					String ipAddress = PortalUtil.getComputerAddress();

					JSONObject params = JSONFactoryUtil.createJSONObject();
					params.put("oid", oid);

					ApiServiceLocalServiceUtil.addApiService(getUser().getUserId(),
							"03", ipAddress, "", params.toString(), "success",
							serviceContext);
				} catch (Exception e) {
					_log.error(e);
				}			
			}
			else {
				jsonObject.put("statusCode", "DossierNotFound");
				try {
					ServiceContext serviceContext = new ServiceContext();
					serviceContext.setUserId(getUser().getUserId());
					serviceContext.setScopeGroupId(getUser().getGroupId());
					serviceContext.setCompanyId(getUser().getCompanyId());
					String ipAddress = PortalUtil.getComputerAddress();

					JSONObject params = JSONFactoryUtil.createJSONObject();
					params.put("oid", oid);

					ApiServiceLocalServiceUtil.addApiService(getUser().getUserId(),
							"03", ipAddress, "", params.toString(), "error",
							serviceContext);
				} catch (Exception e) {
					_log.error(e);
				}			
			}
		} catch (Exception e) {
			_log.error(e);
			
			jsonObject.put("statusCode", "DossierNotFound");
			
			try {
				ServiceContext serviceContext = new ServiceContext();
				serviceContext.setUserId(getUser().getUserId());
				serviceContext.setScopeGroupId(getUser().getGroupId());
				serviceContext.setCompanyId(getUser().getCompanyId());
				String ipAddress = PortalUtil.getComputerAddress();

				JSONObject params = JSONFactoryUtil.createJSONObject();
				params.put("oid", oid);

				ApiServiceLocalServiceUtil.addApiService(getUser().getUserId(),
						"03", ipAddress, "", params.toString(), "error",
						serviceContext);
			} catch (Exception e1) {
				_log.error(e1);
			}
		}

		return jsonObject;
	}

	@JSONWebService(value = "dossierfile", method = "POST")
	public JSONObject addDossierFile(String oid, String dossierfile) {
		
		_log.debug("addDossierFile===oid==============" + oid);
		_log.debug("addDossierFile===dossierfile==============" + dossierfile);
		
		JSONObject resultObj = JSONFactoryUtil.createJSONObject();
		
		try {
			JSONObject dossierfileObj = JSONFactoryUtil.createJSONObject(dossierfile);
			String dossierFileOid = dossierfileObj.getString("dossierFileOid");
			String dossierPartNo = dossierfileObj.getString("dossierPartNo");
			String dossierFileContent = dossierfileObj
					.getString("dossierFileContent");
			String dossierFileURL = dossierfileObj.getString("dossierFileURL");
			String templateFileNo = dossierfileObj.getString("templateFileNo");
			String dossierFileName = dossierfileObj
					.getString("dossierFileName");
			String dossierFileNo = dossierfileObj.getString("dossierFileNo");
			String dossierFileDate = dossierfileObj
					.getString("dossierFileDate");
			DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date fileDate = null;
			try {
				fileDate = formatter.parse(dossierFileDate);
			}
			catch (ParseException pe) {
				fileDate = new Date();
			}
			
			if (dossierFileOid.equals("")) {
				// Add new dossier file
				if (dossierFileURL.equals("")) {
					Dossier dossier = null;
					DossierFile dossierFile = null;
					DossierPart dossierPart = null;
					try {
						dossier = DossierLocalServiceUtil.getByoid(oid);
						if (dossier == null) {
							resultObj.put("statusCode", "DossierNotFound");
							return resultObj;
						}
						long dossierId = dossier.getDossierId();

						ServiceContext serviceContext = new ServiceContext();
						try {
							dossier = DossierLocalServiceUtil
									.getDossier(dossierId);

							dossierPart = DossierPartLocalServiceUtil.getDossierPartByTFN_PN(templateFileNo, dossierPartNo);

							serviceContext.setUserId(dossier.getUserId());
							serviceContext.setScopeGroupId(dossier.getGroupId());
							serviceContext.setCompanyId(dossier.getCompanyId());
							
							dossierFile = DossierFileLocalServiceUtil
									.addDossierFile(
											dossier.getUserId(),
											dossierId,
											dossierPart.getDossierpartId(),
											dossierPart.getTemplateFileNo(),
											StringPool.BLANK,
											0,
											0,
											dossier.getUserId(),
											dossier.getOwnerOrganizationId(),
											dossierFileName,
											dossierFileContent,
											0,
											PortletConstants.DOSSIER_FILE_MARK_UNKNOW,
											2,
											dossierFileNo,
											fileDate,
											1,
											PortletConstants.DOSSIER_FILE_SYNC_STATUS_NOSYNC,
											serviceContext);

							try {
								serviceContext = new ServiceContext();
								serviceContext.setUserId(getUser().getUserId());
								serviceContext.setScopeGroupId(getUser()
										.getGroupId());
								serviceContext.setCompanyId(getUser()
										.getCompanyId());

								String ipAddress = PortalUtil
										.getComputerAddress();
								JSONObject params = JSONFactoryUtil
										.createJSONObject();
								params.put("oid", oid);
								params.put("dossierfile", dossierfile);
								ApiServiceLocalServiceUtil.addApiService(
										getUser().getUserId(), "04", ipAddress,
										"", params.toString(), "success",
										serviceContext);
							} catch (SystemException se) {

							} catch (PortalException pe) {

							}
						} catch (NoSuchDossierPartException nsd) {
							resultObj.put("statusCode", "CanNotUpdate");
							return resultObj;
						} catch (Exception e) {
							resultObj.put("statusCode", "CanNotUpdate");
							return resultObj;
						}
					} catch (SystemException ee) {
						resultObj.put("statusCode", "DossierNotFound");
						return resultObj;

					}
				} else if (dossierFileContent.equals("") || dossierFileContent.equals("{}")) {
					try {
						URL fileURL = new URL(dossierFileURL);
						InputStream is = fileURL.openStream();
						long size = is.available();
						String mimeType = StringPool.BLANK;
						
						try {
							mimeType = URLConnection.guessContentTypeFromStream(is);
						}
						catch (IOException ioe) {
							_log.error(ioe);
						}
						
						ServiceContext sc = new ServiceContext();
		
						Dossier dossier = null;
						DossierFile dossierFile = null;
						DossierPart dossierPart = null;
						
						try {
							dossier = DossierLocalServiceUtil.getByoid(oid);
							
							if (dossier == null) {
								resultObj.put("statusCode", "DossierNotFound");
								return resultObj;
							}
							
							long dossierId = dossier.getDossierId();
							sc.setScopeGroupId(dossier.getGroupId());
							sc.setCompanyId(dossier.getCompanyId());
							sc.setUserId(dossier.getUserId());
							sc.setAddGroupPermissions(true);
							sc.setAddGuestPermissions(true);
							
							DLFolder dossierFolder = DLFolderUtil
									.getDossierFolder(
											sc.getScopeGroupId(),
											dossier.getUserId(),
											dossier.getCounter(),
											sc);
							dossierPart = DossierPartLocalServiceUtil
									.getDossierPartByTFN_PN(templateFileNo, dossierPartNo);
							DossierFileLocalServiceUtil
									.addDossierFile(
											dossier.getUserId(),
											dossierId,
											dossierPart.getDossierpartId(),
											dossierPart.getTemplateFileNo(),
											StringPool.BLANK,
											0L,
											0L,
											dossier.getUserId(),
											dossier.getOwnerOrganizationId(),
											dossierFileName,
											//StringPool.BLANK,
											mimeType,
											dossierFile != null ? dossierFile
													.getFileEntryId() : 0,
											PortletConstants.DOSSIER_FILE_MARK_UNKNOW,
											2,
											dossierFileNo,
											fileDate,
											1,
											PortletConstants.DOSSIER_FILE_SYNC_STATUS_SYNCSUCCESS,
											dossierFolder.getFolderId(),
											dossierFileNo, "", dossierFileName,
											StringPool.BLANK, StringPool.BLANK,
											is, size, sc);
							try {
								sc = new ServiceContext();
								sc.setUserId(getUser().getUserId());
								sc.setScopeGroupId(getUser()
										.getGroupId());
								sc.setCompanyId(getUser()
										.getCompanyId());

								String ipAddress = PortalUtil
										.getComputerAddress();
								JSONObject params = JSONFactoryUtil
										.createJSONObject();
								params.put("oid", oid);
								params.put("dossierfile", dossierfile);
								ApiServiceLocalServiceUtil.addApiService(
										getUser().getUserId(), "04", ipAddress,
										"", params.toString(), "success",
										sc);
							} catch (Exception e) {
								_log.error(e);
							}
						} catch (Exception e) {
							_log.error(e);
							resultObj.put("statusCode", "CanNotUpdate");
							return resultObj;
						}

					} catch (Exception e) {
						_log.error(e);
					}

				}
			} else {
				if (dossierFileURL.equals("")) {
					DossierFile dossierFile = null;
					
					try {
						dossierFile = DossierFileLocalServiceUtil
								.getByOid(dossierFileOid);
						DossierPart dossierPart = null;
						Dossier dossier = null;
						
						try {
							dossier = DossierLocalServiceUtil.getByoid(oid);
							long dossierId = dossier.getDossierId();

							ServiceContext serviceContext = new ServiceContext();
							try {
								dossier = DossierLocalServiceUtil
										.getDossier(dossierId);

								dossierPart = DossierPartLocalServiceUtil
										.getDossierPartByPartNo(dossierPartNo);

								serviceContext.setUserId(dossier.getUserId());

								DossierFileLocalServiceUtil
										.addDossierFile(
												dossier.getUserId(),
												dossierId,
												dossierPart.getDossierpartId(),
												dossierPart.getTemplateFileNo(),
												StringPool.BLANK,
												0,
												0,
												dossier.getUserId(),
												dossier.getOwnerOrganizationId(),
												dossierFileName,
												dossierFileContent,
												dossierFile != null ? dossierFile
														.getFileEntryId() : 0,
												PortletConstants.DOSSIER_FILE_MARK_UNKNOW,
												2,
												dossierFileNo,
												new Date(),
												1,
												PortletConstants.DOSSIER_FILE_SYNC_STATUS_NOSYNC,
												serviceContext);
								try {
									serviceContext = new ServiceContext();
									serviceContext.setUserId(getUser()
											.getUserId());
									serviceContext.setScopeGroupId(getUser()
											.getGroupId());
									serviceContext.setCompanyId(getUser()
											.getCompanyId());

									String ipAddress = PortalUtil
											.getComputerAddress();
									JSONObject params = JSONFactoryUtil
											.createJSONObject();
									params.put("oid", oid);
									params.put("dossierfile", dossierfile);
									ApiServiceLocalServiceUtil.addApiService(
											getUser().getUserId(), "04",
											ipAddress, "", params.toString(),
											"success", serviceContext);
								} catch (Exception e) {
									_log.error(e);
								}
							} catch (Exception e) {
								_log.error(e);
							}
						} catch (Exception e) {
							_log.error(e);
						}

					} catch (SystemException e) {
						_log.error(e);
					}

				} else {
					try {
						DossierFile dossierFile = null;
						dossierFile = DossierFileLocalServiceUtil
								.getByOid(dossierFileOid);
						URL fileURL = new URL(dossierFileURL);
						InputStream is = fileURL.openStream();
						long size = is.available();
						String mimeType = StringPool.BLANK;
						try {
							mimeType = URLConnection.guessContentTypeFromStream(is);
						}
						catch (IOException ioe) {
							_log.error(ioe);
						}

						Dossier dossier = null;
						DossierPart dossierPart = null;

						dossier = DossierLocalServiceUtil.getByoid(oid);
						long dossierId = dossier.getDossierId();
						ServiceContext serviceContext = new ServiceContext();
						serviceContext.setScopeGroupId(dossier.getGroupId());
						serviceContext.setCompanyId(dossier.getCompanyId());
						serviceContext.setUserId(dossier.getUserId());
						serviceContext.setAddGroupPermissions(true);
						serviceContext.setAddGuestPermissions(true);

						DLFolder dossierFolder = DLFolderUtil.getDossierFolder(
								serviceContext.getScopeGroupId(),
								dossier.getUserId(), dossier.getCounter(),
								serviceContext);
						dossierPart = DossierPartLocalServiceUtil
								.getDossierPartByPartNo(dossierPartNo);
						DossierFileLocalServiceUtil
								.addDossierFile(
										dossier.getUserId(),
										dossierId,
										dossierPart.getDossierpartId(),
										dossierPart.getTemplateFileNo(),
										StringPool.BLANK,
										0L,
										0L,
										dossier.getUserId(),
										dossier.getOwnerOrganizationId(),
										dossierFileName,
										//StringPool.BLANK,
										mimeType,
										dossierFile != null ? dossierFile
												.getFileEntryId() : 0,
										PortletConstants.DOSSIER_FILE_MARK_UNKNOW,
										2,
										dossierFileNo,
										new Date(),
										1,
										PortletConstants.DOSSIER_FILE_SYNC_STATUS_SYNCSUCCESS,
										dossierFolder.getFolderId(),
										dossierFileNo, "", dossierFileName,
										StringPool.BLANK, StringPool.BLANK, is,
										size, serviceContext);

						try {
							serviceContext = new ServiceContext();
							serviceContext.setUserId(getUser().getUserId());
							serviceContext.setScopeGroupId(getUser()
									.getGroupId());
							serviceContext.setCompanyId(getUser()
									.getCompanyId());

							String ipAddress = PortalUtil.getComputerAddress();
							JSONObject params = JSONFactoryUtil
									.createJSONObject();
							params.put("oid", oid);
							params.put("dossierfile", dossierfile);
							ApiServiceLocalServiceUtil.addApiService(getUser()
									.getUserId(), "04", ipAddress, "", params
									.toString(), "success", serviceContext);
						} catch (Exception e) {
							_log.error(e);
						}
					} catch (Exception e) {
						_log.error(e);
					}
				}
			}
		} catch (Exception e) {
			
			try {
				ServiceContext serviceContext = new ServiceContext();
				serviceContext.setUserId(getUser().getUserId());
				serviceContext.setScopeGroupId(getUser().getGroupId());
				serviceContext.setCompanyId(getUser().getCompanyId());

				String ipAddress = PortalUtil.getComputerAddress();
				JSONObject params = JSONFactoryUtil.createJSONObject();
				params.put("oid", oid);
				params.put("dossierfile", dossierfile);
				ApiServiceLocalServiceUtil.addApiService(getUser().getUserId(),
						"04", ipAddress, "", params.toString(), "error",
						serviceContext);
			} catch (SystemException se) {
				resultObj.put("statusCode", "CanNotUpdate");
				return resultObj;

			} catch (PortalException pe) {
				resultObj.put("statusCode", "CanNotUpdate");
				return resultObj;

			}
			
			resultObj.put("statusCode", "CanNotUpdate");
			return resultObj;
		}
		
		resultObj.put("statusCode", "Success");
		
		return resultObj;
	}

	@JSONWebService(value = "processorder", method = "POST")
	public JSONObject nextStep(String oid, String actioncode, String username) {
		
		_log.debug("nextStep======oid=======" + oid);
		_log.debug("nextStep======actioncode=======" + actioncode);
		_log.debug("nextStep======username=======" + username);
		
		JSONObject resultObj = JSONFactoryUtil.createJSONObject();
		
		Dossier dossier = null;
		long userId = 0;
		
		try {
			// insert log received
			
			
			// message = success
		} catch(Exception e) {
			// message = error
		}
		
		// insert log return
		
		//send json message 
		
		try {
			dossier = DossierLocalServiceUtil.getByoid(oid);
			userId = dossier.getUserId();
		} catch (SystemException e) {
			resultObj.put("statusCode", "DossierNotFound");
			return resultObj;
		}
		
		try {
			_log.debug("PROCESS ORDER============"+ dossier.getDossierId());
			
			ProcessOrder processOrder = ProcessOrderLocalServiceUtil
					.getProcessOrder(dossier.getDossierId(), 0);
			User user = UserLocalServiceUtil.getUserByScreenName(
					dossier.getCompanyId(), username);
			
			ProcessWorkflow processWorkflow = ProcessWorkflowLocalServiceUtil.getByActionCode(actioncode);
			
			Message message = new Message();
			
			_log.debug("AUTO EVENT=============" + processWorkflow.getAutoEvent());
			_log.debug("AUTO EVENT=============" + processWorkflow.getActionName());
			_log.debug("AUTO EVENT=============" + processWorkflow.getDeadlinePattern());
			
			if (Validator.isNotNull(processWorkflow.getAutoEvent())) {
				message.put(ProcessOrderDisplayTerms.EVENT, processWorkflow.getAutoEvent());
			}
			else {
				message.put(ProcessOrderDisplayTerms.PROCESS_WORKFLOW_ID,
						processWorkflow.getProcessWorkflowId());				
			}
			message.put(ProcessOrderDisplayTerms.ACTION_NOTE,
					"Chuyển trạng thái");
			message.put(ProcessOrderDisplayTerms.PROCESS_STEP_ID,
					processOrder.getProcessStepId());
			message.put(ProcessOrderDisplayTerms.ASSIGN_TO_USER_ID, 0);
			message.put(ProcessOrderDisplayTerms.SERVICE_PROCESS_ID,
					processOrder.getServiceProcessId());
			message.put(ProcessOrderDisplayTerms.PAYMENTVALUE, 0);
			/*
			message.put(ProcessOrderDisplayTerms.PROCESS_WORKFLOW_ID,
					processWorkflow.getProcessWorkflowId());
			*/
			message.put(ProcessOrderDisplayTerms.ACTION_USER_ID,
					user.getUserId());

			message.put(ProcessOrderDisplayTerms.PROCESS_ORDER_ID,
					processOrder.getProcessOrderId());
			message.put(ProcessOrderDisplayTerms.FILE_GROUP_ID, 0);
			message.put(ProcessOrderDisplayTerms.DOSSIER_ID,
					dossier.getDossierId());

			message.put(ProcessOrderDisplayTerms.GROUP_ID, dossier.getGroupId());

			message.put(ProcessOrderDisplayTerms.COMPANY_ID,
					dossier.getCompanyId());

			SendToEngineMsg sendToEngineMsg = new SendToEngineMsg();

			// sendToEngineMsg.setAction(WebKeys.ACTION);
			sendToEngineMsg.setCompanyId(dossier.getCompanyId());
			sendToEngineMsg.setActionNote("Chuyển trạng thái");
			sendToEngineMsg.setAssignToUserId(0);
			sendToEngineMsg.setActionUserId(user.getUserId());
			sendToEngineMsg.setDossierId(dossier.getDossierId());
			sendToEngineMsg.setFileGroupId(0);
			sendToEngineMsg.setPaymentValue(GetterUtil.getDouble(0));
			sendToEngineMsg.setProcessOrderId(processOrder.getProcessOrderId());
			/*
			sendToEngineMsg.setProcessWorkflowId(processWorkflow
					.getProcessWorkflowId());
			*/
			sendToEngineMsg.setReceptionNo(Validator.isNotNull(dossier
					.getReceptionNo()) ? dossier.getReceptionNo()
					: StringPool.BLANK);
			sendToEngineMsg.setSignature(0);
			if (Validator.isNotNull(processWorkflow.getAutoEvent())) {
				sendToEngineMsg.setEvent(processWorkflow.getAutoEvent());				
			}
			else {
				sendToEngineMsg.setProcessWorkflowId(processWorkflow
						.getProcessWorkflowId());				
			}
			message.put("msgToEngine", sendToEngineMsg);
			_log.debug("BEFORE SEND============" + message);
			MessageBusUtil.sendMessage("opencps/backoffice/engine/destination",
					message);
			resultObj.put("statusCode", "Success");
			ServiceContext serviceContext = getServiceContext();

			String ipAddress = serviceContext.getRemoteAddr();
			JSONObject params = JSONFactoryUtil.createJSONObject();
			params.put("oid", oid);
			params.put("actioncode", actioncode);
			params.put("username", username);

			ApiServiceLocalServiceUtil.addApiService(getUser().getUserId(),
					"05", ipAddress, "", params.toString(), "success",
					serviceContext);
		} catch (SystemException e) {
			resultObj.put("statusCode", "ActionNotFound");
			ServiceContext serviceContext = getServiceContext();
			String ipAddress = serviceContext.getRemoteAddr();
			JSONObject params = JSONFactoryUtil.createJSONObject();
			params.put("oid", oid);
			params.put("actioncode", actioncode);
			params.put("username", username);

			try {
				ApiServiceLocalServiceUtil.addApiService(userId, "05",
						ipAddress, "", params.toString(), "error",
						serviceContext);
			} catch (SystemException se) {

			}
		} catch (NoSuchProcessOrderException e) {
			// TODO Auto-generated catch block
			resultObj.put("statusCode", "ActionNotFound");

			try {
				ServiceContext serviceContext = getServiceContext();

				String ipAddress = serviceContext.getRemoteAddr();
				JSONObject params = JSONFactoryUtil.createJSONObject();
				params.put("oid", oid);
				params.put("actioncode", actioncode);
				params.put("username", username);
				ApiServiceLocalServiceUtil.addApiService(userId, "05",
						ipAddress, "", params.toString(), "error",
						serviceContext);
			} catch (SystemException se) {

			} catch (PortalException pe) {

			}
		} catch (PortalException e) {
			// TODO Auto-generated catch block
			resultObj.put("statusCode", "ActionNotFound");
			ServiceContext serviceContext = getServiceContext();
			String ipAddress = serviceContext.getRemoteAddr();
			JSONObject params = JSONFactoryUtil.createJSONObject();
			params.put("oid", oid);
			params.put("actioncode", actioncode);
			params.put("username", username);

			try {
				ApiServiceLocalServiceUtil.addApiService(userId, "05",
						ipAddress, "", params.toString(), "error",
						serviceContext);
			} catch (SystemException se) {

			}

		}
		return resultObj;
	}

	@JSONWebService(value = "processorder", method = "POST")
	public JSONObject nextStep(String oid, String actioncode,
			String actionnote, String username) {
		
		_log.debug("nextStep======oid=======" + oid);
		_log.debug("nextStep======actioncode=======" + actioncode);
		_log.debug("nextStep======actionnote=======" + actionnote);
		_log.debug("nextStep======username=======" + username);
		
		JSONObject resultObj = JSONFactoryUtil.createJSONObject();
		Dossier dossier = null;
		long userId = 0;
		
		try {
			dossier = DossierLocalServiceUtil.getByoid(oid);
			userId = dossier.getUserId();
		} catch (SystemException e) {
			resultObj.put("statusCode", "DossierNotFound");
			return resultObj;
		}
		
		try {
			_log.debug("PROCESS ORDER============"
					+ dossier.getDossierId());
			ProcessOrder processOrder = ProcessOrderLocalServiceUtil
					.getProcessOrder(dossier.getDossierId(), 0);
			User user = UserLocalServiceUtil.getUserByScreenName(
					dossier.getCompanyId(), username);

			ProcessWorkflow processWorkflow = ProcessWorkflowLocalServiceUtil
					.getByActionCode(actioncode);
			if (processWorkflow == null) {
				resultObj.put("statusCode", "ActionNotFound");
				return resultObj;
			}
			Message message = new Message();
			if (Validator.isNotNull(processWorkflow.getAutoEvent())) {
				message.put(ProcessOrderDisplayTerms.EVENT, processWorkflow.getAutoEvent());				
			}
			else {
				message.put(ProcessOrderDisplayTerms.PROCESS_WORKFLOW_ID,
						processWorkflow.getProcessWorkflowId());				
			}
			message.put(ProcessOrderDisplayTerms.ACTION_NOTE, actionnote);
			message.put(ProcessOrderDisplayTerms.PROCESS_STEP_ID,
					processOrder.getProcessStepId());
			message.put(ProcessOrderDisplayTerms.ASSIGN_TO_USER_ID, 0);
			message.put(ProcessOrderDisplayTerms.SERVICE_PROCESS_ID,
					processOrder.getServiceProcessId());
			message.put(ProcessOrderDisplayTerms.PAYMENTVALUE, 0);
			
			message.put(ProcessOrderDisplayTerms.ACTION_USER_ID,
					user.getUserId());

			message.put(ProcessOrderDisplayTerms.PROCESS_ORDER_ID,
					processOrder.getProcessOrderId());
			message.put(ProcessOrderDisplayTerms.FILE_GROUP_ID, 0);
			message.put(ProcessOrderDisplayTerms.DOSSIER_ID,
					dossier.getDossierId());

			message.put(ProcessOrderDisplayTerms.GROUP_ID, dossier.getGroupId());

			message.put(ProcessOrderDisplayTerms.COMPANY_ID,
					dossier.getCompanyId());

			SendToEngineMsg sendToEngineMsg = new SendToEngineMsg();

			// sendToEngineMsg.setAction(WebKeys.ACTION);
			sendToEngineMsg.setCompanyId(dossier.getCompanyId());
			sendToEngineMsg.setActionNote(actionnote);
			sendToEngineMsg.setAssignToUserId(0);
			sendToEngineMsg.setActionUserId(user.getUserId());
			sendToEngineMsg.setDossierId(dossier.getDossierId());
			sendToEngineMsg.setFileGroupId(0);
			sendToEngineMsg.setPaymentValue(GetterUtil.getDouble(0));
			sendToEngineMsg.setProcessOrderId(processOrder.getProcessOrderId());
			/*
			sendToEngineMsg.setProcessWorkflowId(processWorkflow
					.getProcessWorkflowId());
			*/
			sendToEngineMsg.setReceptionNo(Validator.isNotNull(dossier
					.getReceptionNo()) ? dossier.getReceptionNo()
					: StringPool.BLANK);
			sendToEngineMsg.setSignature(0);
			if (Validator.isNotNull(processWorkflow.getAutoEvent())) {
				sendToEngineMsg.setEvent(processWorkflow.getAutoEvent());				
			}
			else {
				sendToEngineMsg.setProcessWorkflowId(processWorkflow
						.getProcessWorkflowId());				
			}

			message.put("msgToEngine", sendToEngineMsg);
			_log.debug("BEFORE SEND============" + message);
			MessageBusUtil.sendMessage("opencps/backoffice/engine/destination",
					message);
			resultObj.put("statusCode", "Success");
			ServiceContext serviceContext = getServiceContext();

			String ipAddress = serviceContext.getRemoteAddr();
			JSONObject params = JSONFactoryUtil.createJSONObject();
			params.put("oid", oid);
			params.put("actioncode", actioncode);
			params.put("username", username);

			ApiServiceLocalServiceUtil.addApiService(getUser().getUserId(),
					"05", ipAddress, "", params.toString(), "success",
					serviceContext);
		} catch (SystemException e) {
			// TODO Auto-generated catch block
			resultObj.put("statusCode", "ActionNotFound");
			ServiceContext serviceContext = getServiceContext();
			String ipAddress = serviceContext.getRemoteAddr();
			JSONObject params = JSONFactoryUtil.createJSONObject();
			params.put("oid", oid);
			params.put("actioncode", actioncode);
			params.put("username", username);

			try {
				ApiServiceLocalServiceUtil.addApiService(userId, "05",
						ipAddress, "", params.toString(), "error",
						serviceContext);
			} catch (SystemException se) {

			}
		} catch (NoSuchProcessOrderException e) {
			// TODO Auto-generated catch block
			resultObj.put("statusCode", "ActionNotFound");

			try {
				ServiceContext serviceContext = getServiceContext();

				String ipAddress = serviceContext.getRemoteAddr();
				JSONObject params = JSONFactoryUtil.createJSONObject();
				params.put("oid", oid);
				params.put("actioncode", actioncode);
				params.put("username", username);
				ApiServiceLocalServiceUtil.addApiService(userId, "05",
						ipAddress, "", params.toString(), "error",
						serviceContext);
			} catch (SystemException se) {

			} catch (PortalException pe) {

			}
		} catch (PortalException e) {
			// TODO Auto-generated catch block
			resultObj.put("statusCode", "ActionNotFound");
			ServiceContext serviceContext = getServiceContext();
			String ipAddress = serviceContext.getRemoteAddr();
			JSONObject params = JSONFactoryUtil.createJSONObject();
			params.put("oid", oid);
			params.put("actioncode", actioncode);
			params.put("username", username);

			try {
				ApiServiceLocalServiceUtil.addApiService(userId, "05",
						ipAddress, "", params.toString(), "error",
						serviceContext);
			} catch (SystemException se) {

			}

		}
		return resultObj;
	}

	@JSONWebService(value = "dossiers", method = "GET")
	public JSONObject searchDossierByDS_RD_SN_U(String dossierstatus,
			String serviceno, String fromdate, String todate, String username)
			throws SystemException {
		
		_log.debug("searchDossierByDS_RD_SN_U===dossierstatus==============" + dossierstatus);
		_log.debug("searchDossierByDS_RD_SN_U===serviceno==============" + serviceno);
		_log.debug("searchDossierByDS_RD_SN_U===fromdate==============" + fromdate);
		_log.debug("searchDossierByDS_RD_SN_U===todate==============" + todate);
		_log.debug("searchDossierByDS_RD_SN_U===username==============" + username);
		
		JSONObject resultObj = JSONFactoryUtil.createJSONObject();
		long userId = 0;
		SimpleDateFormat sdf = new SimpleDateFormat(
				DateTimeUtil._VN_DATE_TIME_FORMAT);
		int count = dossierLocalService.countDossierByDS_RD_SN_U(dossierstatus,
				serviceno, fromdate, todate, username);
		List<Dossier> dossiers = dossierLocalService.searchDossierByDS_RD_SN_U(
				dossierstatus,serviceno, fromdate, todate, username, 0, count);
		JSONArray resultArr = JSONFactoryUtil.createJSONArray();
		for (Dossier d : dossiers) {
			userId = d.getUserId();
			JSONObject dossierObj = JSONFactoryUtil.createJSONObject();
			dossierObj.put("oid", d.getOid());
			ServiceInfo serviceInfo = null;
			try {
				serviceInfo = ServiceInfoLocalServiceUtil.getServiceInfo(d
						.getServiceInfoId());
				dossierObj.put("serviceNo", serviceInfo.getServiceNo());
				dossierObj.put("serviceName", serviceInfo.getServiceName());
			} catch (Exception e) {
				dossierObj.put("serviceNo", "");
				dossierObj.put("serviceName", "");
			} 
			
			dossierObj.put("subjectName", d.getSubjectName());
			dossierObj.put("address", d.getAddress());
			if (d.getSubmitDatetime() != null) {
				dossierObj.put("submitDatetime",
						sdf.format(d.getSubmitDatetime()));
			}
			if (d.getReceiveDatetime() != null) {
				dossierObj.put("receiveDatetime",
						sdf.format(d.getReceiveDatetime()));
			}
			dossierObj.put("receptionNo", d.getReceptionNo());
			if (d.getEstimateDatetime() != null) {
				dossierObj.put("estimateDatetime", d.getEstimateDatetime());
			}
			dossierObj.put("dossierStatus", d.getDossierStatus());
			dossierObj.put("delayStatus", d.getDelayStatus());
			resultArr.put(dossierObj);
		}

		resultObj.put("statusCode", "Success");
		resultObj.put("data", resultArr);
		try {
			ServiceContext serviceContext = getServiceContext();

			String ipAddress = serviceContext.getRemoteAddr();

			ApiServiceLocalServiceUtil.addApiService(getUser().getUserId(),
					"02", ipAddress, "", "{ 'username': '" + username + "' }",
					"success", serviceContext);
		} catch (PortalException pe) {

		}

		return resultObj;
	}
	
	@JSONWebService(value = "dossiers", method = "GET")
	public JSONObject getByoid(String oid, String filetype) {
		
		_log.debug("getByoid===oid==============" + oid);
		_log.debug("getByoid===filetype==============" + filetype);
		
		JSONObject jsonObject = JSONFactoryUtil.createJSONObject();
		Dossier dossier;
		try {
			SimpleDateFormat sdf = new SimpleDateFormat(
					DateTimeUtil._VN_DATE_TIME_FORMAT);
			dossier = dossierLocalService.getByoid(oid);
			if (dossier != null) {
				jsonObject.put("oid", dossier.getOid());
				ServiceInfo serviceInfo = null;
				try {
					serviceInfo = ServiceInfoLocalServiceUtil
							.getServiceInfo(dossier.getServiceInfoId());
					jsonObject.put("serviceNo", serviceInfo.getServiceNo());
					jsonObject.put("serviceName", serviceInfo.getServiceName());
				} catch (Exception e) {
					_log.error(e);
					jsonObject.put("serviceNo", "");
					jsonObject.put("serviceName", "");
				} 
				
				jsonObject.put("govAgencyCode", dossier.getGovAgencyCode());
				jsonObject.put("govAgencyName", dossier.getGovAgencyName());
				jsonObject.put("subjectName", dossier.getSubjectName());
				jsonObject.put("address", dossier.getAddress());
				jsonObject.put("cityCode", dossier.getCityCode());
				jsonObject.put("cityName", dossier.getCityName());
				jsonObject.put("districtCode", dossier.getDistrictCode());
				jsonObject.put("districtName", dossier.getDistrictName());
				jsonObject.put("wardCode", dossier.getWardCode());
				jsonObject.put("wardName", dossier.getWardName());
				jsonObject.put("contactName", dossier.getContactName());
				jsonObject.put("contactTelNo", dossier.getContactTelNo());
				jsonObject.put("contactEmail", dossier.getContactEmail());
				jsonObject.put("note", dossier.getNote());
				
				if (dossier.getSubmitDatetime() != null) {
					jsonObject.put("submitDatetime",
							sdf.format(dossier.getSubmitDatetime()));
				}
				if (dossier.getReceiveDatetime() != null) {
					jsonObject.put("receiveDatetime",
							sdf.format(dossier.getReceiveDatetime()));
				}
				jsonObject.put("receptionNo", dossier.getReceptionNo());
				if (dossier.getEstimateDatetime() != null) {
					jsonObject.put("estimateDatetime",
							sdf.format(dossier.getEstimateDatetime()));
				}
				if (dossier.getFinishDatetime() != null) {
					jsonObject.put("finishDatetime",
							sdf.format(dossier.getFinishDatetime()));
				}
				jsonObject.put("dossierStatus", dossier.getDossierStatus());
				jsonObject.put("delayStatus", dossier.getDelayStatus());

				List<DossierFile> dossierFiles = DossierFileLocalServiceUtil
						.getDossierFileByDossierId(dossier.getDossierId());
				JSONArray dfArr = JSONFactoryUtil.createJSONArray();
				int dossierFileType = 0;
				if (filetype.equals("1")) {
					dossierFileType = 1;
				}
				else if (filetype.equals("2")) {
					dossierFileType = 2;
				}
				for (DossierFile df : dossierFiles) {
					if (dossierFileType == 0 || (df.getDossierFileType() == dossierFileType)) {
						JSONObject jsonDossierFile = JSONFactoryUtil.createJSONObject();
						jsonDossierFile.put("dossierFileOid", df.getOid());
						try {
							DossierPart dpart = DossierPartLocalServiceUtil
									.getDossierPart(df.getDossierPartId());
							jsonDossierFile.put("dossierPartNo", dpart.getPartNo());
						} catch (Exception e) {
							_log.error(e);
						}
						
						jsonDossierFile.put("dossierFileName", df.getDisplayName());
						jsonDossierFile.put("templateFileNo", df.getTemplateFileNo());
						jsonDossierFile.put("dossierFileNo", df.getDossierFileNo());
						if (df.getFileEntryId() > 0) {
							_log.debug("FILE ENTRY==============" + df.getFileEntryId());
							FileEntry fileEntry;
							try {
								fileEntry = DLAppLocalServiceUtil.getFileEntry(df
										.getFileEntryId());
								jsonDossierFile.put(
										"dossierFullFileName",
										df.getDisplayName() + "."
												+ fileEntry.getExtension());

								String url = getFileURL(fileEntry);

								jsonDossierFile.put("dossierFileURL", url);

							} catch (PortalException e) {
								_log.error(e);
							}
						} 
						if (Validator.isNotNull(df.getFormData())) {
							jsonDossierFile.put("dossierFileContent", df.getFormData());
							jsonDossierFile.put("dossierFileURL", "");
						}
						
						if (df.getDossierFileDate() != null) {
							jsonDossierFile.put("dossierFileDate",
									sdf.format(df.getDossierFileDate()));
						}
						dfArr.put(jsonDossierFile);
					}
				}

				jsonObject.put("dossierFiles", dfArr);
				try {
					ServiceContext serviceContext = getServiceContext();
					
					String ipAddress = serviceContext.getRemoteAddr();

					JSONObject params = JSONFactoryUtil.createJSONObject();
					params.put("oid", oid);

					ApiServiceLocalServiceUtil.addApiService(getUser().getUserId(),
							"03", ipAddress, "", params.toString(), "success",
							serviceContext);
				} catch (SystemException se) {
					_log.error(se);
				} catch (PortalException pe) {
					_log.error(pe);
				}				
			}
			else {
				jsonObject.put("statusCode", "DossierNotFound");
				try {
					ServiceContext serviceContext = new ServiceContext();
					serviceContext.setUserId(getUser().getUserId());
					serviceContext.setScopeGroupId(getUser().getGroupId());
					serviceContext.setCompanyId(getUser().getCompanyId());
					String ipAddress = PortalUtil.getComputerAddress();

					JSONObject params = JSONFactoryUtil.createJSONObject();
					params.put("oid", oid);

					ApiServiceLocalServiceUtil.addApiService(getUser().getUserId(),
							"03", ipAddress, "", params.toString(), "error",
							serviceContext);
				} catch (SystemException se) {
					_log.error(se);
				} catch (PortalException pe) {
					_log.error(pe);
				}				
			}
		} catch (SystemException e) {
			jsonObject.put("statusCode", "DossierNotFound");
			try {
				ServiceContext serviceContext = new ServiceContext();
				serviceContext.setUserId(getUser().getUserId());
				serviceContext.setScopeGroupId(getUser().getGroupId());
				serviceContext.setCompanyId(getUser().getCompanyId());
				String ipAddress = PortalUtil.getComputerAddress();

				JSONObject params = JSONFactoryUtil.createJSONObject();
				params.put("oid", oid);

				ApiServiceLocalServiceUtil.addApiService(getUser().getUserId(),
						"03", ipAddress, "", params.toString(), "error",
						serviceContext);
			} catch (SystemException se) {
				_log.error(se);
			} catch (PortalException pe) {
				_log.error(pe);
			}

		}

		return jsonObject;
	}	
	
	private ServiceContext getServiceContext() 
			throws PortalException, SystemException {
		
		ServiceContext serviceContext = getServiceContext();
		
		if(serviceContext == null) {
			serviceContext = new ServiceContext();
			
			if(getUser() != null) {
				serviceContext.setUserId(getUser().getUserId());
				serviceContext.setScopeGroupId(getUser().getGroupId());
				serviceContext.setCompanyId(getUser().getCompanyId());
			}
		}
		
		return serviceContext;
	}
	
	private String getFileURL(FileEntry fileEntry) throws PortalException, SystemException {

		Company company = CompanyLocalServiceUtil.getCompany(getUser().getCompanyId());
		
		String portalURL = PortalUtil.getPortalURL(
				company.getVirtualHostname(), PortalUtil.getPortalPort(false), false);
		
		String fileURL = portalURL + DLUtil.getPreviewURL(fileEntry, fileEntry.getFileVersion(), null, "");
		
		return fileURL;
	}
	
	private static Log _log = LogFactoryUtil.getLog(ApiServiceServiceImpl.class.getName());	
}