/**
 * OpenCPS is the open source Core Public Services software Copyright (C)
 * 2016-present OpenCPS community This program is free software: you can
 * redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3
 * of the License, or any later version. This program is distributed in the hope
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details. You should have received a
 * copy of the GNU Affero General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>
 */

package org.opencps.servicemgt.service.persistence;

import java.util.Iterator;
import java.util.List;

import org.opencps.servicemgt.model.ServiceInfo;
import org.opencps.servicemgt.model.impl.ServiceInfoImpl;

import com.liferay.portal.kernel.dao.orm.QueryPos;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.dao.orm.SQLQuery;
import com.liferay.portal.kernel.dao.orm.Session;
import com.liferay.portal.kernel.dao.orm.Type;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.service.persistence.impl.BasePersistenceImpl;
import com.liferay.util.dao.orm.CustomSQLUtil;

/**
 * @author khoavd
 */
public class ServiceInfoFinderImpl extends BasePersistenceImpl<ServiceInfo>
    implements ServiceInfoFinder {

	public static final String SEARCH_SERVICE_SQL =
				ServiceInfoFinder.class.getName() + ".searchService";

	public static final String COUNT_SERVICE_SQL =
				ServiceInfoFinder.class.getName() + ".countService";
	
	public static final String SEARCH_SERVICE_DIRECTORY_SQL =
				ServiceInfoFinder.class.getName() + ".searchServiceDirectory";

	public static final String COUNT_SERVICE_DIRECTORY_SQL =
	    ServiceInfoFinder.class.getName() + ".countServiceDirectory";

	/**
	 * @param groupId
	 * @param keywords
	 * @param administrationCode
	 * @param domainCode
	 * @param activeStatus
	 * @return
	 */
	public int countServiceDirectory(
	    long groupId, String keywords, String administrationCode,
	    String domainCode, int activeStatus) {

		//String[] names = null;
		boolean andOperator = false;

		if (Validator.isNotNull(keywords)) {
			//names = CustomSQLUtil.keywords(keywords);
		}
		else {
			andOperator = true;
		}

		return _countServiceDirectory(
		    groupId, keywords, administrationCode, domainCode, andOperator, activeStatus);
	}
	
	/**
	 * @param groupId
	 * @param keywords
	 * @param administrationCode
	 * @param domainCode
	 * @return
	 */
	public int countService(
	    long groupId, String keywords, String administrationCode,
	    String domainCode) {

		//String[] names = null;
		boolean andOperator = false;

		if (Validator.isNotNull(keywords)) {
			//names = CustomSQLUtil.keywords(keywords);
		}
		else {
			andOperator = true;
		}

		return _countService(
		    groupId, keywords, administrationCode, domainCode, andOperator);
	}
	
	/**
	 * @param groupId
	 * @param keywords
	 * @param administrationCode
	 * @param domainCode
	 * @param activeStatus
	 * @param start
	 * @param end
	 * @return
	 */
	public List<ServiceInfo> searchServiceDirectory(
	    long groupId, String keywords, String administrationCode,
	    String domainCode, int activeStatus , int start, int end) throws SystemException {

		//String[] names = null;
		boolean andOperator = false;

		if (Validator.isNotNull(keywords)) {
			//names = CustomSQLUtil.keywords(keywords);
		}
		else {
			andOperator = true;
		}

		return _searchServiceDirectory(
		    groupId, keywords, administrationCode, domainCode,activeStatus, andOperator, start,
		    end);
	}

	/**
	 * @param groupId
	 * @param keyword
	 * @param adminCode
	 * @param domainCode
	 * @param activeStatus
	 * @param andOperator
	 * @param start
	 * @param end
	 * @return
	 * @throws SystemException
	 */
	private List<ServiceInfo> _searchServiceDirectory(
	    long groupId, String keyword, String adminCode, String domainCode,
	    int activeStatus, boolean andOperator, int start, int end) throws SystemException {

		//keywords = CustomSQLUtil.keywords(keywords);

		Session session = null;
		try {
			session = openSession();

			String sql = CustomSQLUtil.get(SEARCH_SERVICE_DIRECTORY_SQL);

			/*sql =
			    CustomSQLUtil.replaceKeywords(
			        sql, "lower(opencps_serviceinfo.serviceName)",
			        StringPool.LIKE, true, keywords);

			sql =
			    CustomSQLUtil.replaceKeywords(
			        sql, "lower(opencps_serviceinfo.fullName)",
			        StringPool.LIKE, true, keywords);

			sql = CustomSQLUtil.replaceAndOperator(sql, andOperator);*/
			
			if(Validator.isNull(keyword)){
				sql = StringUtil.replace(sql,
				        "AND ((lower(opencps_serviceinfo.serviceName) LIKE ?) OR (lower(opencps_serviceinfo.fullName) LIKE ?))",
				        StringPool.BLANK);
			}

			// remove condition query
			if (Validator.equals(adminCode, "0") || Validator.equals(adminCode, StringPool.BLANK)) {
				sql =
				    StringUtil.replace(
				        sql,
				        "AND (opencps_serviceinfo.administrationCode = ?)",
				        StringPool.BLANK);
			}

			if (Validator.equals(domainCode, "0") || Validator.equals(domainCode, StringPool.BLANK)) {
				sql =
				    StringUtil.replace(
				        sql, "AND (opencps_serviceinfo.domainCode = ?)",
				        StringPool.BLANK);
			}
			

			SQLQuery q = session.createSQLQuery(sql);

			q.setCacheable(false);

			q.addEntity("ServiceInfo", ServiceInfoImpl.class);

			QueryPos qPos = QueryPos.getInstance(q);

			qPos.add(groupId);
			
			if(Validator.isNotNull(keyword)){
				qPos.add("%"+keyword+"%");
				
				qPos.add("%"+keyword+"%");
			}

			
			if (!Validator.equals(adminCode, "0") && !Validator.equals(adminCode, StringPool.BLANK) ) {
				qPos.add(adminCode);
			}

			if (!Validator.equals(domainCode, "0") && !Validator.equals(domainCode, StringPool.BLANK)) {
				qPos.add(domainCode);
			}
			
			qPos.add(activeStatus);

			return (List<ServiceInfo>) QueryUtil.list(
			    q, getDialect(), start, end);
		}
		catch (Exception e) {
			throw new SystemException(e);
		}
		finally {
			closeSession(session);
		}

	}


	/**
	 * @param groupId
	 * @param keywords
	 * @param administrationCode
	 * @param domainCode
	 * @param start
	 * @param end
	 * @return
	 */
	public List<ServiceInfo> searchService(
	    long groupId, String keywords, String administrationCode,
	    String domainCode, int start, int end) {

		//String[] names = null;
		boolean andOperator = false;

		if (Validator.isNotNull(keywords)) {
			//names = CustomSQLUtil.keywords(keywords);
		}
		else {
			andOperator = true;
		}

		return _searchService(
		    groupId, keywords, administrationCode, domainCode, andOperator, start,
		    end);
	}

	/**
	 * @param groupId
	 * @param keywords
	 * @param adminCode
	 * @param domainCode
	 * @param andOperator
	 * @param start
	 * @param end
	 * @return
	 */
	private List<ServiceInfo> _searchService(
	    long groupId, String keyword, String adminCode, String domainCode,
	    boolean andOperator, int start, int end) {

		//keywords = CustomSQLUtil.keywords(keywords);

		Session session = null;
		try {
			session = openSession();

			String sql = CustomSQLUtil.get(SEARCH_SERVICE_SQL);

			/*sql =
			    CustomSQLUtil.replaceKeywords(
			        sql, "lower(opencps_serviceinfo.serviceName)",
			        StringPool.LIKE, true, keywords);

			sql =
			    CustomSQLUtil.replaceKeywords(
			        sql, "lower(opencps_serviceinfo.fullName)",
			        StringPool.LIKE, true, keywords);

			sql = CustomSQLUtil.replaceAndOperator(sql, andOperator);*/
			
			if(Validator.isNull(keyword)){
				sql = StringUtil.replace(sql,
				        "AND ((lower(opencps_serviceinfo.serviceName) LIKE ?) OR (lower(opencps_serviceinfo.fullName) LIKE ?))",
				        StringPool.BLANK);
			}

			// remove condition query
			if (Validator.equals(adminCode, "0") || Validator.equals(adminCode, StringPool.BLANK)) {
				sql =
				    StringUtil.replace(
				        sql,
				        "AND (opencps_serviceinfo.administrationCode = ?)",
				        StringPool.BLANK);
			}

			if (Validator.equals(domainCode, "0") || Validator.equals(domainCode, StringPool.BLANK)) {
				sql =
				    StringUtil.replace(
				        sql, "AND (opencps_serviceinfo.domainCode = ?)",
				        StringPool.BLANK);
			}
			

			SQLQuery q = session.createSQLQuery(sql);

			q.setCacheable(false);

			q.addEntity("ServiceInfo", ServiceInfoImpl.class);

			QueryPos qPos = QueryPos.getInstance(q);

			qPos.add(groupId);
			
			if(Validator.isNotNull(keyword)){
				qPos.add("%"+keyword+"%");
				
				qPos.add("%"+keyword+"%");
			}

			
			if (!Validator.equals(adminCode, "0") && !Validator.equals(adminCode, StringPool.BLANK) ) {
				qPos.add(adminCode);
			}

			if (!Validator.equals(domainCode, "0") && !Validator.equals(domainCode, StringPool.BLANK)) {
				qPos.add(domainCode);
			}

			return (List<ServiceInfo>) QueryUtil.list(
			    q, getDialect(), start, end);
		}
		catch (Exception e) {
			try {
				throw new SystemException(e);
			}
			catch (SystemException se) {
				se.printStackTrace();
			}
		}
		finally {
			closeSession(session);
		}

		return null;

	}

	/**
	 * @param groupId
	 * @param keywords
	 * @param adminCode
	 * @param domainCode
	 * @param andOperator
	 * @return
	 */
	private int _countService(
	    long groupId, String keyword, String adminCode, String domainCode,
	    boolean andOperator) {

		//keywords = CustomSQLUtil.keywords(keywords);

		Session session = null;
		try {
			session = openSession();

			String sql = CustomSQLUtil.get(COUNT_SERVICE_SQL);

			/*sql =
			    CustomSQLUtil.replaceKeywords(
			        sql, "lower(opencps_serviceinfo.serviceName)",
			        StringPool.LIKE, true, keywords);

			sql =
			    CustomSQLUtil.replaceKeywords(
			        sql, "lower(opencps_serviceinfo.fullName)",
			        StringPool.LIKE, true, keywords);

			sql = CustomSQLUtil.replaceAndOperator(sql, andOperator);*/
			
			if(Validator.isNull(keyword)){
				sql = StringUtil.replace(sql,
				        "AND ((lower(opencps_serviceinfo.serviceName) LIKE ?) OR (lower(opencps_serviceinfo.fullName) LIKE ?))",
				        StringPool.BLANK);
			}

			// remove condition query
			if (Validator.equals(adminCode, "0") || Validator.equals(adminCode, StringPool.BLANK)) {
				sql =
				    StringUtil.replace(
				        sql,
				        "AND (opencps_serviceinfo.administrationCode = ?)",
				        StringPool.BLANK);
			}

			if (Validator.equals(domainCode, "0") || Validator.equals(domainCode, StringPool.BLANK)) {
				sql =
				    StringUtil.replace(
				        sql, "AND (opencps_serviceinfo.domainCode = ?)",
				        StringPool.BLANK);
			}

			SQLQuery q = session.createSQLQuery(sql);

			q.setCacheable(false);

			q.addScalar(COUNT_COLUMN_NAME, Type.INTEGER);

			QueryPos qPos = QueryPos.getInstance(q);

			qPos.add(groupId);

			if(Validator.isNotNull(keyword)){
				qPos.add("%"+keyword+"%");
				
				qPos.add("%"+keyword+"%");
			}

			if (!Validator.equals(adminCode, "0") && !Validator.equals(adminCode, StringPool.BLANK) ) {
				qPos.add(adminCode);
			}

			if (!Validator.equals(domainCode, "0") && !Validator.equals(domainCode, StringPool.BLANK)) {
				qPos.add(domainCode);
			}

			Iterator<Integer> itr = q.iterate();

			if (itr.hasNext()) {
				Integer count = itr.next();

				if (count != null) {
					return count.intValue();
				}
			}

			return 0;

		}
		catch (Exception e) {
			try {
				throw new SystemException(e);
			}
			catch (SystemException se) {
				se.printStackTrace();
			}
		}
		finally {
			closeSession(session);
		}

		return 0;
	}
	
	private int _countServiceDirectory(
	    long groupId, String keyword, String adminCode, String domainCode,
	    boolean andOperator, int activeStatus) {

		//keywords = CustomSQLUtil.keywords(keywords);

		Session session = null;
		try {
			session = openSession();

			String sql = CustomSQLUtil.get(COUNT_SERVICE_DIRECTORY_SQL);

			/*sql =
			    CustomSQLUtil.replaceKeywords(
			        sql, "lower(opencps_serviceinfo.serviceName)",
			        StringPool.LIKE, true, keywords);

			sql =
			    CustomSQLUtil.replaceKeywords(
			        sql, "lower(opencps_serviceinfo.fullName)",
			        StringPool.LIKE, true, keywords);

			sql = CustomSQLUtil.replaceAndOperator(sql, andOperator);*/
			
			if(Validator.isNull(keyword)){
				sql = StringUtil.replace(sql,
				        "AND ((lower(opencps_serviceinfo.serviceName) LIKE ?) OR (lower(opencps_serviceinfo.fullName) LIKE ?))",
				        StringPool.BLANK);
			}

			// remove condition query
			if (Validator.equals(adminCode, "0") || Validator.equals(adminCode, StringPool.BLANK)) {
				sql =
				    StringUtil.replace(
				        sql,
				        "AND (opencps_serviceinfo.administrationCode = ?)",
				        StringPool.BLANK);
			}

			if (Validator.equals(domainCode, "0") || Validator.equals(domainCode, StringPool.BLANK)) {
				sql =
				    StringUtil.replace(
				        sql, "AND (opencps_serviceinfo.domainCode = ?)",
				        StringPool.BLANK);
			}

			SQLQuery q = session.createSQLQuery(sql);

			q.setCacheable(false);

			q.addScalar(COUNT_COLUMN_NAME, Type.INTEGER);

			QueryPos qPos = QueryPos.getInstance(q);

			qPos.add(groupId);

			if(Validator.isNotNull(keyword)){
				qPos.add("%"+keyword+"%");
				
				qPos.add("%"+keyword+"%");
			}

			if (!Validator.equals(adminCode, "0") && !Validator.equals(adminCode, StringPool.BLANK) ) {
				qPos.add(adminCode);
			}

			if (!Validator.equals(domainCode, "0") && !Validator.equals(domainCode, StringPool.BLANK)) {
				qPos.add(domainCode);
			}
			
			qPos.add(activeStatus);

			Iterator<Integer> itr = q.iterate();

			if (itr.hasNext()) {
				Integer count = itr.next();

				if (count != null) {
					return count.intValue();
				}
			}

			return 0;

		}
		catch (Exception e) {
			try {
				throw new SystemException(e);
			}
			catch (SystemException se) {
				se.printStackTrace();
			}
		}
		finally {
			closeSession(session);
		}

		return 0;
	}

}
