/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.drugorderexport.web.controller;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.drugorderexport.DrugOrderExportUtil;
import org.openmrs.module.drugorderexport.service.DrugOrderService;
import org.openmrs.util.OpenmrsUtil;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.ParameterizableViewController;

/**
 *
 */
public class StartTreatmentController extends ParameterizableViewController {
	
	protected final Log log = LogFactory.getLog(getClass());
	
	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#handleRequestInternal(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
		// TODO Auto-generated method stub
		Map<String, Object> map = new HashMap<String, Object>();
		List<Patient> patientsExport = new ArrayList<Patient>();
		List<Object[]> objectsList = new ArrayList<Object[]>();
		
		String checkedValueStr[] = null;
		int checkedValue = 0;
		if (request.getParameterValues("checkValue") != null) {
			checkedValueStr = request.getParameterValues("checkValue");
			
			checkedValue = Integer.parseInt(checkedValueStr[0]);
		}
		if (request.getMethod().equalsIgnoreCase("post")) {
			
			List<Integer> patientIds = new ArrayList<Integer>();
			
			Date startDate = null;
			Date endDate = null;
			
			DrugOrderService service = Context.getService(DrugOrderService.class);
			
			String startD = request.getParameter("startdate");
			String endD = request.getParameter("enddate");
			
			String gender = request.getParameter("gender"), minAge = request.getParameter("minAge"), maxAge = request
			        .getParameter("maxAge"), minBirthdate = request.getParameter("minBirthdate"), maxBirthdate = request
			        .getParameter("maxBirthdate");
			
			SimpleDateFormat df = OpenmrsUtil.getDateFormat();
			
			Date mnBirthdate = null;
			Date mxBirthdate = null;
			Date mnAge = null;
			Date mxAge = null;
			
			if (startD != null && startD.length() != 0) {
				startDate = df.parse(startD);
			}
			if (endD != null && endD.length() != 0) {
				endDate = df.parse(endD);
			}
			if (minBirthdate != null && minBirthdate.length() != 0) {
				mnBirthdate = df.parse(minBirthdate);
			}
			if (maxBirthdate != null && maxBirthdate.length() != 0) {
				mxBirthdate = df.parse(maxBirthdate);
			}
			
			if (maxAge != null && maxAge.length() != 0) {
				mxAge = service.getDateObjectFormAge(Integer.parseInt(maxAge));
			}
			if (minAge != null && minAge.length() != 0) {
				mnAge = service.getDateObjectFormAge(Integer.parseInt(minAge));
				
			}
			
			patientIds = service.getPatientWhoStartedOnDate(startDate, endDate, gender, mnAge, mxAge, mnBirthdate,
			    mxBirthdate);
			
			if (checkedValue == 1) {
				if(endDate==null)
				endDate = new Date();
				patientIds = service.getActivePatients(patientIds, endDate);
			}
			
			for (Integer patientId : patientIds) {
				Patient patient = new Patient();
				
				patient = Context.getPatientService().getPatient(patientId);
				if (!patient.getVoided())
					patientsExport.add(patient);
				
				Date startTreatmentDate = null;
				Date lastEncounterDate = null;
				Date lastVisitDate = null;
				String startTreatmentDateStr = "";
				String lastEncounterDateStr = "";
				String lastVisitDateStr = "";
				
				if (service.getPatientLastVisitDate(patientId) != null) {
					lastVisitDate = service.getPatientLastVisitDate(patientId);
					lastVisitDateStr = lastVisitDate.toString();
				}
				if (service.getWhenPatStartedXRegimen(patientId, DrugOrderExportUtil.gpGetARVDrugsIds()) != null) {
					startTreatmentDate = service.getWhenPatientStarted(patient);
					startTreatmentDateStr = startTreatmentDate.toString();
				}
				if (service.getPatientLastEncounterDate(patientId) != null) {
					lastEncounterDate = service.getPatientLastEncounterDate(patientId);
					lastEncounterDateStr = lastEncounterDate.toString();
				}
				
				objectsList.add(new Object[] { Context.getPersonService().getPerson(patientId),
				        startTreatmentDateStr, lastEncounterDateStr, lastVisitDateStr });
			}
			
			// for data exportation
			if (request.getParameter("export") != null && !request.getParameter("export").equals("")) {
	
					if (Context.getAuthenticatedUser().hasPrivilege("Export Collective Patient Data")) {
						if (request.getParameter("export").equals("excel"))
							DrugOrderExportUtil.exportData(request, response, patientsExport);
						if (request.getParameter("export").equals("pdf"))
							DrugOrderExportUtil.exportToPDF(request, response, patientsExport);
					}
					else
						map.put("msg", "Required Privilege : [Export Collective Patient Data]");
				
			}
			
				
			map.put("objectsList", objectsList);
			map.put("collectionSize", patientsExport.size());
			map.put("checkedValueExport", checkedValue);
			
			Date now = new Date();
			
			if (startDate != null)
				map.put("startdate", df.format(startDate));
			
			if (endDate != null)
				map.put("enddate", df.format(endDate));
			
			if (gender.equals("f"))
				map.put("gender", "Female");
			else if (gender.equals(""))
				map.put("gender", "Any");
			else
				map.put("gender", "Male");
			if (mnAge != null)
				map.put("minAge", now.getYear() - mnAge.getYear());
			if (mxAge != null)
				map.put("maxAge", now.getYear() - mxAge.getYear());
			if (mnBirthdate != null)
				map.put("minBirthdate", df.format(mnBirthdate));
			if (mxBirthdate != null)
				map.put("maxBirthdate", df.format(mxBirthdate));
			
		}
		return new ModelAndView(getViewName(), map);
	}
}
