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

package org.openmrs.module.kenyaemr.api.db.hibernate;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.openmrs.Cohort;
import org.openmrs.Concept;
import org.openmrs.ConceptAnswer;
import org.openmrs.DrugOrder;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Obs;
import org.openmrs.OrderType;
import org.openmrs.Patient;
import org.openmrs.PatientProgram;
import org.openmrs.Person;
import org.openmrs.PersonAddress;
import org.openmrs.Program;
import org.openmrs.Visit;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.DAOException;
import org.openmrs.module.kenyaemr.api.db.KenyaEmrDAO;
import org.openmrs.module.kenyaemr.model.DrugInfo;
import org.openmrs.module.kenyaemr.model.DrugObsProcessed;
import org.openmrs.module.kenyaemr.model.DrugOrderProcessed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Hibernate specific data access functions. This class should not be used
 * directly.
 */
@SuppressWarnings("deprecation")
public class HibernateKenyaEmrDAO implements KenyaEmrDAO {

	SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	SimpleDateFormat formatterExt = new SimpleDateFormat("yyyy-MM-dd");

	private SessionFactory sessionFactory;

	@Autowired
	private DataSource dataSource;

	/**
	 * Sets the session factory
	 * 
	 * @param sessionFactory
	 *            the session factory
	 */
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	/**
	 * Convenience method to get current session
	 * 
	 * @return the session
	 */
	private Session getCurrentSession() {
		return sessionFactory.getCurrentSession();
	}

	public DataSource getDataSource() {
		return dataSource;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	@Override
	public List<Object> executeSqlQuery(String query,
			Map<String, Object> substitutions) {
		SQLQuery q = sessionFactory.getCurrentSession().createSQLQuery(query);

		for (Map.Entry<String, Object> e : substitutions.entrySet()) {
			if (e.getValue() instanceof Collection) {
				q.setParameterList(e.getKey(), (Collection) e.getValue());
			} else if (e.getValue() instanceof Object[]) {
				q.setParameterList(e.getKey(), (Object[]) e.getValue());
			} else if (e.getValue() instanceof Cohort) {
				q.setParameterList(e.getKey(),
						((Cohort) e.getValue()).getMemberIds());
			} else if (e.getValue() instanceof Date) {
				q.setDate(e.getKey(), (Date) e.getValue());
			} else {
				q.setParameter(e.getKey(), e.getValue());
			}

		}

		q.setReadOnly(true);

		List<Object> r = q.list();
		return r;
	}

	@Override
	public List<Object> executeHqlQuery(String query,
			Map<String, Object> substitutions) {
		Query q = sessionFactory.getCurrentSession().createQuery(query);

		applySubstitutions(q, substitutions);

		// optimizations go here
		q.setReadOnly(true);

		return q.list();
	}

	private void applySubstitutions(Query q, Map<String, Object> substitutions) {
		for (Map.Entry<String, Object> e : substitutions.entrySet()) {
			if (e.getValue() instanceof Collection) {
				q.setParameterList(e.getKey(), (Collection) e.getValue());
			} else if (e.getValue() instanceof Object[]) {
				q.setParameterList(e.getKey(), (Object[]) e.getValue());
			} else if (e.getValue() instanceof Cohort) {
				q.setParameterList(e.getKey(),
						((Cohort) e.getValue()).getMemberIds());
			} else if (e.getValue() instanceof Date) {
				q.setDate(e.getKey(), (Date) e.getValue());
			} else {
				q.setParameter(e.getKey(), e.getValue());
			}
		}
	}

	/*
	 * ENCOUNTER
	 */
	public Encounter getFirstEncounterByDateTime(Patient patient, Visit visit) {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(
				Encounter.class);
		criteria.add(Restrictions.eq("patient", patient));
		criteria.add(Restrictions.eq("visit", visit));
		criteria.addOrder(Order.asc("encounterDatetime"));
		criteria.setMaxResults(1);
		return (Encounter) criteria.uniqueResult();
	}

	public Encounter getFirstEncounterByCreatedDateTime(Patient patient,
			Visit visit) {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(
				Encounter.class);
		criteria.add(Restrictions.eq("patient", patient));
		criteria.add(Restrictions.eq("visit", visit));
		criteria.addOrder(Order.asc("dateCreated"));
		criteria.setMaxResults(1);
		return (Encounter) criteria.uniqueResult();
	}

	public Encounter getLastEncounterByDateTime(Patient patient, Visit visit) {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(
				Encounter.class);
		criteria.add(Restrictions.eq("patient", patient));
		criteria.add(Restrictions.eq("visit", visit));
		criteria.addOrder(Order.desc("encounterDatetime"));
		criteria.setMaxResults(1);
		return (Encounter) criteria.uniqueResult();
	}

	public Encounter getLastEncounterByCreatedDateTime(Patient patient,
			Visit visit) {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(
				Encounter.class);
		criteria.add(Restrictions.eq("patient", patient));
		criteria.add(Restrictions.eq("visit", visit));
		criteria.addOrder(Order.desc("dateCreated"));
		criteria.setMaxResults(1);
		return (Encounter) criteria.uniqueResult();
	}

	public Encounter getLastEncounterByDateTime(Patient patient,
			Set<EncounterType> encounterTypes) {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(
				Encounter.class);
		criteria.add(Restrictions.eq("patient", patient));
		criteria.add(Restrictions.in("encounterType", encounterTypes));
		criteria.addOrder(Order.desc("encounterDatetime"));
		criteria.setMaxResults(1);
		return (Encounter) criteria.uniqueResult();
	}

	public Encounter getLastEncounterByCreatedDateTime(Patient patient,
			Set<EncounterType> encounterTypes) {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(
				Encounter.class);
		criteria.add(Restrictions.eq("patient", patient));
		criteria.add(Restrictions.in("encounterType", encounterTypes));
		criteria.addOrder(Order.desc("dateCreated"));
		criteria.setMaxResults(1);
		return (Encounter) criteria.uniqueResult();
	}

	public List<org.openmrs.Order> getOrderByDateAndOrderType(Date date,
			OrderType orderType) {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(
				org.openmrs.Order.class, "order");
		criteria.add(Restrictions.eq("order.orderType", orderType));
		if (date != null) {
			String datee = formatterExt.format(date);
			String startFromDate = datee + " 00:00:00";
			String endFromDate = datee + " 23:59:59";
			try {
				criteria.add(Restrictions.and(
						Restrictions.ge("order.startDate",
								formatter.parse(startFromDate)),
						Restrictions.le("order.startDate",
								formatter.parse(endFromDate))));
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		return criteria.list();
	}

	public List<Obs> getObsGroupByDate(Date date) {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(
				Obs.class, "obs");
		if (date != null) {
			String dat = formatterExt.format(date);
			String startFromDate = dat + " 00:00:00";
			String endFromDate = dat + " 23:59:59";
			Concept concept1 = Context.getConceptService().getConceptByUuid(
					"163021AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
			Concept concept2 = Context.getConceptService().getConceptByUuid(
					"163022AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
			Concept concept3 = Context.getConceptService().getConceptByUuid(
					"163023AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
			List<Concept> obsGroupCollection = new LinkedList<Concept>();
			obsGroupCollection.add(concept1);
			obsGroupCollection.add(concept2);
			obsGroupCollection.add(concept3);
			try {
				criteria.add(Restrictions.and(
						Restrictions.ge("obs.dateCreated",
								formatter.parse(startFromDate)),
						Restrictions.le("obs.dateCreated",
								formatter.parse(endFromDate))));
			} catch (ParseException e) {
				e.printStackTrace();
			}
			criteria.add(Restrictions.in("obs.concept", obsGroupCollection));
		}
		criteria.add(Restrictions.isNull("comment"));
		return criteria.list();
	}

	public List<Obs> getObsGroupByDateAndPerson(Date date, Person person) {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(
				Obs.class, "obs");
		criteria.add(Restrictions.eq("obs.person", person));
		Concept concept1 = Context.getConceptService().getConceptByUuid(
				"163021AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
		Concept concept2 = Context.getConceptService().getConceptByUuid(
				"163022AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
		Concept concept3 = Context.getConceptService().getConceptByUuid(
				"163023AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
		List<Concept> obsGroupCollection = new LinkedList<Concept>();
		obsGroupCollection.add(concept1);
		obsGroupCollection.add(concept2);
		obsGroupCollection.add(concept3);
		if (date != null) {
			String dat = formatterExt.format(date);
			String startFromDate = dat + " 00:00:00";
			String endFromDate = dat + " 23:59:59";
			try {
				criteria.add(Restrictions.and(
						Restrictions.ge("obs.dateCreated",
								formatter.parse(startFromDate)),
						Restrictions.le("obs.dateCreated",
								formatter.parse(endFromDate))));
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		criteria.add(Restrictions.in("obs.concept", obsGroupCollection));
		criteria.add(Restrictions.isNull("comment"));
		return criteria.list();
	}

	public List<Obs> getObsByObsGroup(Obs obsGroup) {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(
				Obs.class, "obs");
		criteria.add(Restrictions.eq("obs.obsGroup", obsGroup));
		return criteria.list();
	}

	public Obs saveOrUpdateObs(Obs obs) throws DAOException {
		return (Obs) sessionFactory.getCurrentSession().merge(obs);
	}

	public DrugOrderProcessed saveDrugOrderProcessed(
			DrugOrderProcessed drugOrderProcessed) throws DAOException {
		return (DrugOrderProcessed) sessionFactory.getCurrentSession().merge(
				drugOrderProcessed);
	}

	public DrugObsProcessed saveDrugObsProcessed(
			DrugObsProcessed drugObsProcessed) throws DAOException {
		return (DrugObsProcessed) sessionFactory.getCurrentSession().merge(
				drugObsProcessed);
	}

	public DrugOrderProcessed getDrugOrderProcessed(DrugOrder drugOrder) {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(
				DrugOrderProcessed.class, "drugOrderProcessed");
		criteria.add(Restrictions.eq("drugOrder", drugOrder));
		criteria.add(Restrictions.eq("processedStatus", false));
		criteria.add(Restrictions.isNull("discontinuedDate"));
		return (DrugOrderProcessed) criteria.uniqueResult();
	}

	public DrugOrderProcessed getLastDrugOrderProcessed(DrugOrder drugOrder) {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(
				DrugOrderProcessed.class, "drugOrderProcessed");
		criteria.add(Restrictions.eq("drugOrder", drugOrder));
		criteria.addOrder(Order.desc("createdDate"));
		criteria.setMaxResults(1);
		return (DrugOrderProcessed) criteria.uniqueResult();
	}

	public DrugOrderProcessed getLastDrugOrderProcessedByPatient(Patient patient) {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(
				DrugOrderProcessed.class, "drugOrderProcessed");
		criteria.add(Restrictions.eq("patient", patient));
		criteria.addOrder(Order.desc("createdDate"));
		criteria.setMaxResults(1);
		return (DrugOrderProcessed) criteria.uniqueResult();
	}

	public DrugOrderProcessed getLastDrugOrderProcessedNotDiscontinued(
			DrugOrder drugOrder) {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(
				DrugOrderProcessed.class, "drugOrderProcessed");
		criteria.add(Restrictions.eq("drugOrder", drugOrder));
		criteria.add(Restrictions.isNull("discontinuedDate"));
		criteria.addOrder(Order.desc("createdDate"));
		criteria.setMaxResults(1);
		return (DrugOrderProcessed) criteria.uniqueResult();
	}

	public List<DrugOrderProcessed> getDrugOrderProcessedCompleted(
			DrugOrder drugOrder) {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(
				DrugOrderProcessed.class, "drugOrderProcessed");
		criteria.add(Restrictions.eq("drugOrder", drugOrder));
		criteria.add(Restrictions.eq("processedStatus", true));
		return criteria.list();
	}

	public DrugOrderProcessed getDrugOrderProcesedById(Integer id) {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(
				DrugOrderProcessed.class, "drugOrderProcessed");
		criteria.add(Restrictions.eq("id", id));
		return (DrugOrderProcessed) criteria.uniqueResult();
	}

	public List<DrugOrderProcessed> getDrugOrdersByProcessedDate(Date date) {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(
				DrugOrderProcessed.class, "drugOrderProcessed");
		String dat = formatterExt.format(date);
		String startFromDate = dat + " 00:00:00";
		String endFromDate = dat + " 23:59:59";
		try {
			criteria.add(Restrictions.and(
					Restrictions.ge("processedDate",
							formatter.parse(startFromDate)),
					Restrictions.le("processedDate",
							formatter.parse(endFromDate))));
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return criteria.list();
	}

	public List<DrugObsProcessed> getObsDrugOrdersByProcessedDate(Date date) {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(
				DrugObsProcessed.class, "DrugObsProcessed");
		String dat = formatterExt.format(date);
		String startFromDate = dat + " 00:00:00";
		String endFromDate = dat + " 23:59:59";
		try {
			criteria.add(Restrictions.and(
					Restrictions.ge("processedDate",
							formatter.parse(startFromDate)),
					Restrictions.le("processedDate",
							formatter.parse(endFromDate))));
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return criteria.list();
	}

	public List<DrugOrderProcessed> getDrugOrdersByPatientAndProcessedDate(
			Patient patient, Date processedDate) {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(
				DrugOrderProcessed.class, "drugOrderProcessed");
		criteria.add(Restrictions.eq("patient", patient));
		criteria.add(Restrictions.eq("processedStatus", true));
		if (processedDate != null) {
			String dat = formatterExt.format(processedDate);
			String startFromDate = dat + " 00:00:00";
			String endFromDate = dat + " 23:59:59";
			try {
				criteria.add(Restrictions.and(
						Restrictions.ge("processedDate",
								formatter.parse(startFromDate)),
						Restrictions.le("processedDate",
								formatter.parse(endFromDate))));
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		return criteria.list();
	}

	public List<DrugObsProcessed> getObsDrugOrdersByPatientAndProcessedDate(
			Patient patient, Date processedDate) {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(
				DrugObsProcessed.class, "DrugObsProcessed");
		criteria.add(Restrictions.eq("patient", patient));
		if (processedDate != null) {
			String dat = formatterExt.format(processedDate);
			String startFromDate = dat + " 00:00:00";
			String endFromDate = dat + " 23:59:59";
			try {
				criteria.add(Restrictions.and(
						Restrictions.ge("processedDate",
								formatter.parse(startFromDate)),
						Restrictions.le("processedDate",
								formatter.parse(endFromDate))));
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		return criteria.list();
	}

	public List<DrugInfo> getDrugInfo() {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(
				DrugInfo.class, "drugInfo");
		return criteria.list();
	}

	public DrugInfo getDrugInfo(String drugCode) {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(
				DrugInfo.class, "drugInfo");
		criteria.add(Restrictions.eq("drugCode", drugCode));
		return (DrugInfo) criteria.uniqueResult();
	}

	public DrugOrderProcessed getLastRegimenChangeType(Patient patient) {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(
				DrugOrderProcessed.class, "drugOrderProcessed");
		criteria.add(Restrictions.eq("patient", patient));
		// criteria.add(Restrictions.isNotNull("regimenChangeType"));
		// criteria.add(Restrictions.isNotNull("discontinuedDate"));
		// criteria.addOrder(Order.desc("discontinuedDate"));
		criteria.addOrder(Order.desc("createdDate"));
		criteria.setMaxResults(1);
		return (DrugOrderProcessed) criteria.uniqueResult();
	}

	public List<ConceptAnswer> getConceptAnswerByAnsweConcept(
			Concept answerConcept) {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(
				ConceptAnswer.class, "conceptAnswer");
		criteria.add(Restrictions.eq("answerConcept", answerConcept));
		return criteria.list();
	}

	@Override
	public List<DrugOrderProcessed> getAllfirstLine() {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(
				DrugOrderProcessed.class, "drugOrderProcessed");

		criteria.add(Restrictions.isNotNull("typeOfRegimen"));

		return criteria.list();
	}

	public List<PersonAddress> getPatientsByTownship(String township) {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(
				PersonAddress.class, "personAddress");
		criteria.add(Restrictions.ilike("countyDistrict", township + "%"));
		return criteria.list();
	}

	public List<Obs> getObsByScheduledDate(Date date) {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(
				Obs.class, "obs");
		Collection<Concept> conList = new ArrayList<Concept>();
		conList.add(Context.getConceptService().getConceptByUuid(
				"5096AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
		conList.add(Context.getConceptService().getConceptByUuid(
				"1879AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
		criteria.add(Restrictions.in("concept", conList));
		criteria.add(Restrictions.eq("valueDatetime", date));
		return criteria.list();
	}

	public Set<Patient> getPatientProgram(Program program, String startDate,
			String endDate) {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(
				PatientProgram.class, "patientProgram");
		criteria.add(Restrictions.eq("program", program));
		String startFromDate = startDate + " 00:00:00";
		String endFromDate = endDate + " 23:59:59";
		try {
			criteria.add(Restrictions.and(
					Restrictions.ge("dateEnrolled",
							formatter.parse(startFromDate)),
					Restrictions.le("dateEnrolled",
							formatter.parse(endFromDate))));
		} catch (ParseException e) {
			e.printStackTrace();
		}
		Set<Patient> patients = new HashSet<Patient>();
		List<PatientProgram> ppgms = criteria.list();
		for (PatientProgram ppgm : ppgms) {
			patients.add(ppgm.getPatient());
		}

		return patients;
	}

	public Set<Patient> getNoOfPatientTransferredIn(String startDate,
			String endDate) {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(
				Obs.class, "obs");
		Collection<Concept> conList = new ArrayList<Concept>();
		conList.add(Context.getConceptService().getConceptByUuid(
				"4b73234a-15db-49a0-b089-c26c239fe90d"));
		conList.add(Context.getConceptService().getConceptByUuid(
				"feee14d1-6cd6-4f5d-a3f6-056ed91526e5"));
		criteria.add(Restrictions.in("valueCoded", conList));
		String startFromDate = startDate + " 00:00:00";
		String endFromDate = endDate + " 23:59:59";
		try {
			criteria.add(Restrictions.and(Restrictions.ge("obsDatetime",
					formatter.parse(startFromDate)), Restrictions.le(
					"obsDatetime", formatter.parse(endFromDate))));
		} catch (ParseException e) {
			e.printStackTrace();
		}
		Set<Patient> patients = new HashSet<Patient>();
		List<Obs> obss = criteria.list();
		for (Obs obs : obss) {
			Patient patient = Context.getPatientService().getPatient(
					obs.getPersonId());
			if (patient != null) {
				patients.add(patient);
			}
		}
		return patients;
	}

	public Set<Patient> getNoOfPatientTransferredOut(String startDate,
			String endDate) {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(
				Obs.class, "obs");
		Concept conceptTransferredOut = Context.getConceptService()
				.getConceptByUuid("159492AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
		criteria.add(Restrictions.eq("valueCoded", conceptTransferredOut));
		String startFromDate = startDate + " 00:00:00";
		String endFromDate = endDate + " 23:59:59";
		try {
			criteria.add(Restrictions.and(Restrictions.ge("obsDatetime",
					formatter.parse(startFromDate)), Restrictions.le(
					"obsDatetime", formatter.parse(endFromDate))));
		} catch (ParseException e) {
			e.printStackTrace();
		}
		Set<Patient> patients = new HashSet<Patient>();
		List<Obs> obss = criteria.list();
		for (Obs obs : obss) {
			patients.add(obs.getPatient());
		}
		return patients;
	}

	public Visit getVisitsByPatient(Patient patient) {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(
				Visit.class, "visit");
		criteria.add(Restrictions.eq("patient", patient));
		criteria.addOrder(Order.asc("startDatetime"));
		criteria.setMaxResults(1);
		return (Visit) criteria.uniqueResult();
	}

	public Set<Patient> getTotalNoOfCohort(String startDate, String endDate) {
		Program program = Context.getProgramWorkflowService().getProgramByUuid(
				"96ec813f-aaf0-45b2-add6-e661d5bf79d6");
		Set<Patient> paientOnART = getPatientProgram(program, startDate,
				endDate);
		Set<Patient> paientTransferredOut = getNoOfPatientTransferredOut(
				startDate, endDate);
		Set<Patient> totalCohort = new LinkedHashSet<Patient>();
		for (Patient patient : paientOnART) {
			if (paientTransferredOut.contains(patient)) {

			} else {
				totalCohort.add(patient);
			}
		}
		return totalCohort;
	}

	public Set<Patient> getCohortBasedOnGender(String gender, String startDate,
			String endDate) {
		Program program = Context.getProgramWorkflowService().getProgramByUuid(
				"96ec813f-aaf0-45b2-add6-e661d5bf79d6");
		Set<Patient> patientOnCohort = getPatientProgram(program, startDate,
				endDate);
		Set<Patient> paientTransferredOut = getNoOfPatientTransferredOut(
				startDate, endDate);

		List<Person> personList = getListOfPatient(gender);
		List<Patient> patientList = new LinkedList<Patient>();
		for (Person person : personList) {
			Patient patient = Context.getPatientService().getPatient(
					person.getPersonId());
			if (patient != null) {
				patientList.add(patient);
			}
		}

		Set<Patient> cohortAfterTransferredOut = new LinkedHashSet<Patient>();
		Set<Patient> cohortBasedOnGender = new LinkedHashSet<Patient>();
		for (Patient patient : patientOnCohort) {
			if (paientTransferredOut.contains(patient)) {

			} else {
				cohortAfterTransferredOut.add(patient);
			}
		}

		for (Patient patient : cohortAfterTransferredOut) {
			if (patientList.contains(patient)) {
				cohortBasedOnGender.add(patient);
			}
		}
		return cohortBasedOnGender;
	}

	public Set<Patient> getCohortBasedOnAge(Integer age1, Integer age2,
			String startDate, String endDate) {
		Program program = Context.getProgramWorkflowService().getProgramByUuid(
				"96ec813f-aaf0-45b2-add6-e661d5bf79d6");
		Set<Patient> patientOnCohort = getPatientProgram(program, startDate,
				endDate);
		Set<Patient> paientTransferredOut = getNoOfPatientTransferredOut(
				startDate, endDate);

		List<Person> personList = getListOfPatient(age1, age2);
		List<Patient> patientList = new LinkedList<Patient>();
		for (Person person : personList) {
			if (person.getAge() != null && person.getAge() >= age1
					&& person.getAge() <= age2) {
				Patient patient = Context.getPatientService().getPatient(
						person.getPersonId());
				if (patient != null) {
					patientList.add(patient);
				}
			}
		}

		Set<Patient> cohortAfterTransferredOut = new LinkedHashSet<Patient>();
		Set<Patient> cohortBasedOnAge = new LinkedHashSet<Patient>();
		for (Patient patient : patientOnCohort) {
			if (paientTransferredOut.contains(patient)) {

			} else {
				cohortAfterTransferredOut.add(patient);
			}
		}

		for (Patient patient : cohortAfterTransferredOut) {
			if (patientList.contains(patient)) {
				cohortBasedOnAge.add(patient);
			}
		}
		return cohortBasedOnAge;
	}

	public Set<Patient> getNoOfCohortAliveAndOnArt(Program program,
			String startDate, String endDate) {
		Set<Patient> patients = new LinkedHashSet<Patient>();
		Set<Patient> noOfArtStoppedCohorts = getNoOfArtStoppedCohort(program,
				startDate, endDate);
		Set<Patient> noOfArtDiedCohorts = getNoOfArtDiedCohort(program,
				startDate, endDate);
		Set<Patient> noOfPatientLostToFollowUps = getNoOfPatientLostToFollowUp(
				startDate, endDate);
		Set<Patient> transferredOutPatient = getNoOfPatientTransferredOut(
				startDate, endDate);
		Set<Patient> noOfHIVStoppedCohorts = getNoOfHIVStoppedCohort(startDate,
				endDate);

		patients.addAll(noOfArtStoppedCohorts);
		patients.addAll(noOfArtDiedCohorts);
		patients.addAll(noOfPatientLostToFollowUps);
		patients.addAll(transferredOutPatient);
		patients.addAll(noOfHIVStoppedCohorts);

		Set<Patient> totalCohort = getTotalNoOfCohort(startDate, endDate);
		Set<Patient> patientSet = new LinkedHashSet<Patient>();

		for (Patient patient : totalCohort) {
			if (patients.contains(patient)) {

			} else {
				patientSet.add(patient);
			}
		}

		return patientSet;
	}

	public Set<Patient> getOriginalFirstLineRegimen(Program program,
			String startDate, String endDate) {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(
				DrugOrderProcessed.class, "drugOrderProcessed");
		List<String> typeOfRegimen = new ArrayList<String>();
		typeOfRegimen.add("First line Anti-retoviral drugs");
		typeOfRegimen.add("Fixed dose combinations (FDCs)");
		typeOfRegimen.add("ARV drugs for child");

		criteria.add(Restrictions.eq("regimenChangeType", "Start"));
		criteria.add(Restrictions.in("typeOfRegimen", typeOfRegimen));
		String startFromDate = startDate + " 00:00:00";
		String endFromDate = endDate + " 23:59:59";
		try {
			criteria.add(Restrictions.and(Restrictions.ge("startDate",
					formatter.parse(startFromDate)), Restrictions.le(
					"startDate", formatter.parse(endFromDate))));
		} catch (ParseException e) {
			e.printStackTrace();
		}
		List<DrugOrderProcessed> drugOrderProcesseds = criteria.list();
		Set<Patient> afasr = getAlternateFirstLineRegimen(program, startDate,
				endDate);
		afasr.addAll(getSecondLineRegimen(program, startDate, endDate));
		Set<Patient> dops = new LinkedHashSet<Patient>();
		for (DrugOrderProcessed drugOrderProcessed : drugOrderProcesseds) {

			if (afasr.contains(drugOrderProcessed.getPatient())) {

			} else {
				dops.add(drugOrderProcessed.getPatient());
			}
		}

		Set<Patient> patients = new LinkedHashSet<Patient>();
		Set<Patient> noOfArtStoppedCohorts = getNoOfArtStoppedCohort(program,
				startDate, endDate);
		Set<Patient> noOfArtDiedCohorts = getNoOfArtDiedCohort(program,
				startDate, endDate);
		Set<Patient> noOfPatientLostToFollowUps = getNoOfPatientLostToFollowUp(
				startDate, endDate);
		Set<Patient> transferredOutPatient = getNoOfPatientTransferredOut(
				startDate, endDate);
		Set<Patient> noOfHIVStoppedCohorts = getNoOfHIVStoppedCohort(startDate,
				endDate);

		patients.addAll(noOfArtStoppedCohorts);
		patients.addAll(noOfArtDiedCohorts);
		patients.addAll(noOfPatientLostToFollowUps);
		patients.addAll(transferredOutPatient);
		patients.addAll(noOfHIVStoppedCohorts);

		Set<Patient> patientSet = new LinkedHashSet<Patient>();

		for (Patient dop : dops) {
			if (patients.contains(dop)) {

			} else {
				patientSet.add(dop);
			}
		}
		return patientSet;
	}

	public Set<Patient> getAlternateFirstLineRegimen(Program program,
			String startDate, String endDate) {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(
				DrugOrderProcessed.class, "drugOrderProcessed");
		List<String> typeOfRegimen = new ArrayList<String>();
		typeOfRegimen.add("First line Anti-retoviral drugs");
		typeOfRegimen.add("Fixed dose combinations (FDCs)");

		criteria.add(Restrictions.eq("regimenChangeType", "Substitute"));
		criteria.add(Restrictions.in("typeOfRegimen", typeOfRegimen));
		String startFromDate = startDate + " 00:00:00";
		String endFromDate = endDate + " 23:59:59";
		try {
			criteria.add(Restrictions.and(Restrictions.ge("startDate",
					formatter.parse(startFromDate)), Restrictions.le(
					"startDate", formatter.parse(endFromDate))));
		} catch (ParseException e) {
			e.printStackTrace();
		}
		List<DrugOrderProcessed> drugOrderProcesseds = criteria.list();
		Set<Patient> dops = new LinkedHashSet<Patient>();
		for (DrugOrderProcessed drugOrderProcessed : drugOrderProcesseds) {
			DrugOrderProcessed dop = getLastDrugOrderProcessedByPatient(drugOrderProcessed
					.getPatient());
			if (dop.getRegimenChangeType().equals("Switch")) {

			} else {
				dops.add(drugOrderProcessed.getPatient());
			}
		}

		Set<Patient> patients = new LinkedHashSet<Patient>();
		Set<Patient> noOfArtStoppedCohorts = getNoOfArtStoppedCohort(program,
				startDate, endDate);
		Set<Patient> noOfArtDiedCohorts = getNoOfArtDiedCohort(program,
				startDate, endDate);
		Set<Patient> noOfPatientLostToFollowUps = getNoOfPatientLostToFollowUp(
				startDate, endDate);
		Set<Patient> transferredOutPatient = getNoOfPatientTransferredOut(
				startDate, endDate);
		Set<Patient> noOfHIVStoppedCohorts = getNoOfHIVStoppedCohort(startDate,
				endDate);

		patients.addAll(noOfArtStoppedCohorts);
		patients.addAll(noOfArtDiedCohorts);
		patients.addAll(noOfPatientLostToFollowUps);
		patients.addAll(transferredOutPatient);
		patients.addAll(noOfHIVStoppedCohorts);

		Set<Patient> patientSet = new LinkedHashSet<Patient>();

		for (Patient dop : dops) {
			if (patients.contains(dop)) {

			} else {
				patientSet.add(dop);
			}
		}
		return patientSet;
	}

	public Set<Patient> getSecondLineRegimen(Program program, String startDate,
			String endDate) {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(
				DrugOrderProcessed.class, "drugOrderProcessed");
		List<String> typeOfRegimen = new ArrayList<String>();
		typeOfRegimen.add("Second line ART");
		typeOfRegimen.add("Fixed dose combinations (FDCs)");

		criteria.add(Restrictions.eq("regimenChangeType", "Switch"));
		criteria.add(Restrictions.in("typeOfRegimen", typeOfRegimen));
		String startFromDate = startDate + " 00:00:00";
		String endFromDate = endDate + " 23:59:59";
		try {
			criteria.add(Restrictions.and(Restrictions.ge("startDate",
					formatter.parse(startFromDate)), Restrictions.le(
					"startDate", formatter.parse(endFromDate))));
		} catch (ParseException e) {
			e.printStackTrace();
		}
		List<DrugOrderProcessed> drugOrderProcesseds = criteria.list();
		Set<Patient> dops = new LinkedHashSet<Patient>();
		for (DrugOrderProcessed drugOrderProcessed : drugOrderProcesseds) {
			DrugOrderProcessed dop = getLastDrugOrderProcessedByPatient(drugOrderProcessed
					.getPatient());
			if (dop.getRegimenChangeType().equals("Substitute")) {

			} else {
				dops.add(drugOrderProcessed.getPatient());
			}
		}

		Set<Patient> patients = new LinkedHashSet<Patient>();
		Set<Patient> noOfArtStoppedCohorts = getNoOfArtStoppedCohort(program,
				startDate, endDate);
		Set<Patient> noOfArtDiedCohorts = getNoOfArtDiedCohort(program,
				startDate, endDate);
		Set<Patient> noOfPatientLostToFollowUps = getNoOfPatientLostToFollowUp(
				startDate, endDate);
		Set<Patient> transferredOutPatient = getNoOfPatientTransferredOut(
				startDate, endDate);
		Set<Patient> noOfHIVStoppedCohorts = getNoOfHIVStoppedCohort(startDate,
				endDate);

		patients.addAll(noOfArtStoppedCohorts);
		patients.addAll(noOfArtDiedCohorts);
		patients.addAll(noOfPatientLostToFollowUps);
		patients.addAll(transferredOutPatient);
		patients.addAll(noOfHIVStoppedCohorts);

		Set<Patient> patientSet = new LinkedHashSet<Patient>();

		for (Patient dop : dops) {
			if (patients.contains(dop)) {

			} else {
				patientSet.add(dop);
			}
		}
		return patientSet;
	}

	public Set<Patient> getNoOfArtStoppedCohort(Program program,
			String startDate, String endDate) {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(
				PatientProgram.class, "patientProgram");
		criteria.add(Restrictions.eq("program", program));
		criteria.add(Restrictions.isNotNull("dateCompleted"));
		String startFromDate = startDate + " 00:00:00";
		String endFromDate = endDate + " 23:59:59";
		try {
			criteria.add(Restrictions.and(
					Restrictions.ge("dateCompleted",
							formatter.parse(startFromDate)),
					Restrictions.le("dateCompleted",
							formatter.parse(endFromDate))));
		} catch (ParseException e) {
			e.printStackTrace();
		}
		List<PatientProgram> ppgms = criteria.list();
		Set<Patient> artStoppedCohort = new LinkedHashSet<Patient>();

		for (PatientProgram ppgm : ppgms) {
			Obs obs = getOutCome(ppgm.getPatient(), startFromDate, endFromDate);
			if (obs != null) {
				Date date1 = ppgm.getDateCompleted();
				Date date2 = obs.getObsDatetime();
				if (date1.compareTo(date2) > 0) {
					artStoppedCohort.add(ppgm.getPatient());
				}
			} else {
				artStoppedCohort.add(ppgm.getPatient());
			}
		}
		return artStoppedCohort;
	}

	public Set<Patient> getNoOfHIVStoppedCohort(String startDate, String endDate) {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(
				PatientProgram.class, "patientProgram");
		Program program = Context.getProgramWorkflowService().getProgramByUuid(
				"dfdc6d40-2f2f-463d-ba90-cc97350441a8");
		criteria.add(Restrictions.eq("program", program));
		criteria.add(Restrictions.isNotNull("dateCompleted"));
		String startFromDate = startDate + " 00:00:00";
		String endFromDate = endDate + " 23:59:59";
		try {
			criteria.add(Restrictions.and(
					Restrictions.ge("dateCompleted",
							formatter.parse(startFromDate)),
					Restrictions.le("dateCompleted",
							formatter.parse(endFromDate))));
		} catch (ParseException e) {
			e.printStackTrace();
		}
		List<PatientProgram> ppgms = criteria.list();
		Set<Patient> patients = new LinkedHashSet<Patient>();
		for (PatientProgram ppgm : ppgms) {
			patients.add(ppgm.getPatient());
		}
		return patients;
	}

	public Set<Patient> getNoOfArtDiedCohort(Program program, String startDate,
			String endDate) {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(
				PatientProgram.class, "patientProgram");
		criteria.add(Restrictions.eq("program", program));
		criteria.add(Restrictions.isNull("dateCompleted"));
		String startFromDate = startDate + " 00:00:00";
		String endFromDate = endDate + " 23:59:59";

		List<Person> personList = getListOfDiedPatient(startDate, endDate);
		List<Patient> patientList = new LinkedList<Patient>();
		for (Person person : personList) {
			Patient patient = Context.getPatientService().getPatient(
					person.getPersonId());
			if (patient != null) {
				patientList.add(patient);
			}
		}

		List<PatientProgram> ppgms = new ArrayList<PatientProgram>();
		Set<Patient> patients = new LinkedHashSet<Patient>();
		if (patientList.size() != 0) {
			criteria.add(Restrictions.in("patient", patientList));
			ppgms = criteria.list();
		}

		for (PatientProgram ppgm : ppgms) {
			patients.add(ppgm.getPatient());
		}

		return patients;
	}

	public Set<Patient> getNoOfPatientLostToFollowUp(String startDate,
			String endDate) {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(
				Obs.class, "obs");
		Concept conceptLostToFollowUp = Context.getConceptService()
				.getConceptByUuid("5240AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");

		criteria.add(Restrictions.eq("valueCoded", conceptLostToFollowUp));
		String startFromDate = startDate + " 00:00:00";
		String endFromDate = endDate + " 23:59:59";
		try {
			criteria.add(Restrictions.and(Restrictions.ge("obsDatetime",
					formatter.parse(startFromDate)), Restrictions.le(
					"obsDatetime", formatter.parse(endFromDate))));
		} catch (ParseException e) {
			e.printStackTrace();
		}
		criteria.add(Restrictions.eq("voided", false));
		List<Obs> obss = criteria.list();
		Set<Patient> patients = new LinkedHashSet<Patient>();
		for (Obs obs : obss) {
			patients.add(obs.getPatient());
		}
		return patients;
	}

	public List<Obs> getNoOfPatientWithCD4(String startDate, String endDate) {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(
				Obs.class, "obs");
		Concept cd4Concept = Context.getConceptService().getConceptByUuid(
				"5497AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
		Integer obj = new Integer(200);
		double doub = obj.doubleValue();

		criteria.add(Restrictions.and(Restrictions.eq("concept", cd4Concept),
				Restrictions.ge("valueNumeric", doub)));
		String startFromDate = startDate + " 00:00:00";
		String endFromDate = endDate + " 23:59:59";
		try {
			criteria.add(Restrictions.and(Restrictions.ge("obsDatetime",
					formatter.parse(startFromDate)), Restrictions.le(
					"obsDatetime", formatter.parse(endFromDate))));
		} catch (ParseException e) {
			e.printStackTrace();
		}
		criteria.add(Restrictions.eq("voided", false));
		return criteria.list();
	}

	public List<Obs> getNoOfPatientNormalActivity(String startDate,
			String endDate) {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(
				Obs.class, "obs");
		Concept scaleA = Context.getConceptService().getConceptByUuid(
				"e8a480a7-1f05-402c-9adf-9acbd6ff446f");

		criteria.add(Restrictions.eq("valueCoded", scaleA));
		String startFromDate = startDate + " 00:00:00";
		String endFromDate = endDate + " 23:59:59";
		try {
			criteria.add(Restrictions.and(Restrictions.ge("obsDatetime",
					formatter.parse(startFromDate)), Restrictions.le(
					"obsDatetime", formatter.parse(endFromDate))));
		} catch (ParseException e) {
			e.printStackTrace();
		}
		criteria.add(Restrictions.eq("voided", false));
		return criteria.list();
	}

	public List<Obs> getNoOfPatientBedriddenLessThanFifty(String startDate,
			String endDate) {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(
				Obs.class, "obs");
		Concept scaleB = Context.getConceptService().getConceptByUuid(
				"585dcf92-c42f-42af-ac44-fdd2fb66ae3a");

		criteria.add(Restrictions.eq("valueCoded", scaleB));
		String startFromDate = startDate + " 00:00:00";
		String endFromDate = endDate + " 23:59:59";
		try {
			criteria.add(Restrictions.and(Restrictions.ge("obsDatetime",
					formatter.parse(startFromDate)), Restrictions.le(
					"obsDatetime", formatter.parse(endFromDate))));
		} catch (ParseException e) {
			e.printStackTrace();
		}
		criteria.add(Restrictions.eq("voided", false));
		return criteria.list();
	}

	public List<Obs> getNoOfPatientBedriddenMoreThanFifty(String startDate,
			String endDate) {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(
				Obs.class, "obs");
		Concept scaleC = Context.getConceptService().getConceptByUuid(
				"a70cd549-aa63-4310-9a38-715dfc3ebbd2");

		criteria.add(Restrictions.eq("valueCoded", scaleC));
		String startFromDate = startDate + " 00:00:00";
		String endFromDate = endDate + " 23:59:59";
		try {
			criteria.add(Restrictions.and(Restrictions.ge("obsDatetime",
					formatter.parse(startFromDate)), Restrictions.le(
					"obsDatetime", formatter.parse(endFromDate))));
		} catch (ParseException e) {
			e.printStackTrace();
		}
		criteria.add(Restrictions.eq("voided", false));
		return criteria.list();
	}

	public Set<Patient> getNoOfPatientPickedUpArvForSixMonth(String startDate,
			String endDate) {
		Set<Patient> listOfVisitedPatients = getListOfVisitedPatient(startDate,
				endDate);

		Set<Patient> patients = new HashSet<Patient>();

		String endFromDate = endDate + " 23:59:59";
		String startFromDate = startDate + " 00:00:00";
		Date edate = null;
		Date sdate = null;

		try {
			edate = formatter.parse(endFromDate);
		} catch (ParseException e) {
			e.printStackTrace();
		}

		try {
			sdate = formatter.parse(startFromDate);
		} catch (ParseException e) {
			e.printStackTrace();
		}

		for (Patient patient : listOfVisitedPatients) {
			/* Get List of all visit for patients */
			List<Visit> visitsTotal = Context.getVisitService()
					.getVisitsByPatient(patient);

			/* Declare empty list of visit for visits before enddate */
			List<Visit> visits = new ArrayList<Visit>();
			int visitCount = 0;

			/*
			 * Get Date to form Period to look for all the lost to follow up
			 * cases
			 */
			Date startPeriodDate = null;
			Date endPeriodDate = null;
			if (visitsTotal.size() > 5) {
				for (Visit v : visitsTotal) {
					if (visitCount < 7) {
						if (v.getStopDatetime() != null
								&& v.getStopDatetime().before(edate)) {
							visits.add(v);
							visitCount++;
							/* add visit */

							if (visitCount == 0) {
								endPeriodDate = v.getStopDatetime();
							}
							if (visitCount == 6) {
								startPeriodDate = v.getStartDatetime();
							}
						} else if (v.getStopDatetime() == null
								&& edate.after(v.getStartDatetime())
								&& sdate.before(v.getStartDatetime())) {

							/* add visit */
							visits.add(v);
							visitCount++;
							if (visitCount == 6) {
								startPeriodDate = v.getStartDatetime();
							}
							if (visitCount == 0) {
								Date curDate = new Date();
								String modifiedDate = new SimpleDateFormat(
										"dd-MMM-yyyy").format(v
										.getStartDatetime());
								try {
									endPeriodDate = formatter
											.parse(modifiedDate + " "
													+ curDate.getHours() + ":"
													+ curDate.getMinutes()
													+ ":"
													+ curDate.getSeconds());
								} catch (ParseException e) {
									endPeriodDate = curDate;
									// e.printStackTrace();
								}
							}

						}
					}

				}
			}

			if (visits.size() > 5) {
				Visit seventhVisit = null;
				int extraVisit = 0;
				if (visits.size() > 6) {
					seventhVisit = visits.get(6);
					List<Obs> lostToFollowUp = getLostToFollowUpObs(
							seventhVisit.getPatient(),
							seventhVisit.getStartDatetime(),
							seventhVisit.getStopDatetime());
					List<DrugOrderProcessed> drugOrderProcesseds = getDrugOrderProcessedByVisit(seventhVisit);

					if (lostToFollowUp.size() == 0
							&& drugOrderProcesseds.size() > 0) {
						extraVisit = 1;
					}
				}

				int finalCount = 1;
				for (Visit visit : visits) {
					List<Obs> lostToFollowUp = getLostToFollowUpObs(
							visit.getPatient(), startPeriodDate, endPeriodDate);
					List<DrugOrderProcessed> drugOrderProcesseds = getDrugOrderProcessedByVisit(visit);
					if (lostToFollowUp.size() == 0
							&& drugOrderProcesseds.size() > 0
							&& finalCount == 6 && extraVisit == 0) {
						patients.add(visit.getPatient());
					}
					finalCount++;
				}
			}
		}
		return patients;
	}

	public Set<Patient> getNoOfPatientPickedUpArvForTwelveMonth(
			String startDate, String endDate) {
		Set<Patient> listOfVisitedPatients = getListOfVisitedPatient(startDate,
				endDate);

		Set<Patient> patients = new HashSet<Patient>();

		String endFromDate = endDate + " 23:59:59";
		String startFromDate = startDate + " 00:00:00";
		Date edate = null;
		Date sdate = null;

		try {
			edate = formatter.parse(endFromDate);
		} catch (ParseException e) {
			e.printStackTrace();
		}

		try {
			sdate = formatter.parse(startFromDate);
		} catch (ParseException e) {
			e.printStackTrace();
		}

		for (Patient patient : listOfVisitedPatients) {
			/* Get List of all visit for patients */
			List<Visit> visitsTotal = Context.getVisitService()
					.getVisitsByPatient(patient);

			/* Declare empty list of visit for visits before enddate */
			List<Visit> visits = new ArrayList<Visit>();
			int visitCount = 0;

			/*
			 * Get Date to form Period to look for all the lost to follow up
			 * cases
			 */
			Date startPeriodDate = null;
			Date endPeriodDate = null;
			if (visitsTotal.size() > 11) {
				for (Visit v : visitsTotal) {
					if (visitCount < 13) {
						if (v.getStopDatetime() != null
								&& v.getStopDatetime().before(edate)) {
							visits.add(v);
							visitCount++;
							/* add visit */

							if (visitCount == 0) {
								endPeriodDate = v.getStopDatetime();
							}
							if (visitCount == 12) {
								startPeriodDate = v.getStartDatetime();
							}
						} else if (v.getStopDatetime() == null
								&& edate.after(v.getStartDatetime())
								&& sdate.before(v.getStartDatetime())) {

							/* add visit */
							visits.add(v);
							visitCount++;
							if (visitCount == 12) {
								startPeriodDate = v.getStartDatetime();
							}
							if (visitCount == 0) {
								Date curDate = new Date();
								String modifiedDate = new SimpleDateFormat(
										"dd-MMM-yyyy").format(v
										.getStartDatetime());
								try {
									endPeriodDate = formatter
											.parse(modifiedDate + " "
													+ curDate.getHours() + ":"
													+ curDate.getMinutes()
													+ ":"
													+ curDate.getSeconds());
								} catch (ParseException e) {
									endPeriodDate = curDate;
									// e.printStackTrace();
								}
							}

						}
					}

				}
			}

			if (visits.size() > 11) {
				Visit seventhVisit = null;
				int extraVisit = 0;
				if (visits.size() > 12) {
					seventhVisit = visits.get(6);
					List<Obs> lostToFollowUp = getLostToFollowUpObs(
							seventhVisit.getPatient(),
							seventhVisit.getStartDatetime(),
							seventhVisit.getStopDatetime());
					List<DrugOrderProcessed> drugOrderProcesseds = getDrugOrderProcessedByVisit(seventhVisit);

					if (lostToFollowUp.size() == 0
							&& drugOrderProcesseds.size() > 0) {
						extraVisit = 1;
					}
				}

				int finalCount = 1;
				for (Visit visit : visits) {
					List<Obs> lostToFollowUp = getLostToFollowUpObs(
							visit.getPatient(), startPeriodDate, endPeriodDate);
					List<DrugOrderProcessed> drugOrderProcesseds = getDrugOrderProcessedByVisit(visit);
					if (lostToFollowUp.size() == 0
							&& drugOrderProcesseds.size() > 0
							&& finalCount == 12 && extraVisit == 0) {
						patients.add(visit.getPatient());
					}
					finalCount++;
				}
			}
		}
		return patients;
	}

	public List<DrugOrderProcessed> getDrugOrderProcessedByPatient(
			Patient patient) {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(
				DrugOrderProcessed.class);
		criteria.add(Restrictions.eq("patient", patient));
		return criteria.list();
	}

	public List<Person> getListOfDiedPatient(String startDate, String endDate) {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(
				Person.class, "person");
		String startFromDate = startDate + " 00:00:00";
		String endFromDate = endDate + " 23:59:59";

		try {
			criteria.add(Restrictions.and(Restrictions.ge("deathDate",
					formatter.parse(startFromDate)), Restrictions.le(
					"deathDate", formatter.parse(endFromDate))));
		} catch (ParseException e) {
			e.printStackTrace();
		}

		criteria.add(Restrictions.eq("dead", true));
		return criteria.list();
	}

	public List<Person> getListOfDiedPatient() {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(
				Person.class, "person");
		criteria.add(Restrictions.eq("dead", true));
		criteria.add(Restrictions.ge("personId", 38));
		return criteria.list();
	}

	public List<Person> getListOfAlivePatient() {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(
				Person.class, "person");
		criteria.add(Restrictions.eq("dead", false));
		criteria.add(Restrictions.ge("personId", 38));
		return criteria.list();
	}

	public List<Person> getListOfPatient(String gender) {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(
				Person.class, "person");
		criteria.add(Restrictions.eq("gender", gender));
		criteria.add(Restrictions.ge("personId", 38));
		return criteria.list();
	}

	public List<Person> getListOfPatient(Integer age1, Integer age2) {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(
				Person.class, "person");
		criteria.add(Restrictions.ge("personId", 38));
		return criteria.list();
	}

	public Set<Patient> getListOfVisitedPatient(String startDate, String endDate) {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(
				Visit.class, "visit");
		String startFromDate = startDate + " 00:00:00";
		String endFromDate = endDate + " 23:59:59";
		try {
			criteria.add(Restrictions.and(
					Restrictions.ge("startDatetime",
							formatter.parse(startFromDate)),
					Restrictions.le("startDatetime",
							formatter.parse(endFromDate))));
		} catch (ParseException e) {
			e.printStackTrace();
		}

		List<Visit> visits = criteria.list();
		Set<Patient> patients = new LinkedHashSet<Patient>();
		for (Visit visit : visits) {
			patients.add(visit.getPatient());
		}
		return patients;
	}

	public List<DrugOrderProcessed> getDrugOrderProcessedByVisit(Visit visit) {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(
				DrugOrderProcessed.class, "drugOrderProcessed");
		criteria.add(Restrictions.eq("visit", visit));
		// criteria.addOrder(Order.asc("createdDate"));
		return criteria.list();
	}

	public List<Obs> getLostToFollowUpObs(Patient patient, Date startVisitDate,
			Date endVisitDate) {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(
				Obs.class, "obs");
		Concept conceptLostToFollowUp = Context.getConceptService()
				.getConceptByUuid("5240AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
		Person person = patient;
		criteria.add(Restrictions.eq("person", person));
		criteria.add(Restrictions.eq("valueCoded", conceptLostToFollowUp));
		criteria.add(Restrictions.and(
				Restrictions.ge("obsDatetime", startVisitDate),
				Restrictions.le("obsDatetime", endVisitDate)));
		return criteria.list();
	}

	public Obs getOutCome(Patient patient, String startFromDate,
			String endFromDate) {
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(
				Obs.class, "obs");
		List<Concept> conceptList = new ArrayList<Concept>();
		Concept conceptDied = Context.getConceptService().getConceptByUuid(
				"160034AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
		Concept conceptLostToFollowUp = Context.getConceptService()
				.getConceptByUuid("5240AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
		Concept conceptTransferredOut = Context.getConceptService()
				.getConceptByUuid("159492AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
		conceptList.add(conceptDied);
		conceptList.add(conceptLostToFollowUp);
		conceptList.add(conceptTransferredOut);
		Person person = patient;
		criteria.add(Restrictions.eq("person", person));
		criteria.add(Restrictions.in("valueCoded", conceptList));
		try {
			criteria.add(Restrictions.and(Restrictions.ge("obsDatetime",
					formatter.parse(startFromDate)), Restrictions.le(
					"obsDatetime", formatter.parse(endFromDate))));
		} catch (ParseException e) {
			e.printStackTrace();
		}
		criteria.addOrder(Order.desc("obsDatetime"));
		criteria.setMaxResults(1);
		return (Obs) criteria.uniqueResult();
	}

	public Integer getPatientCount() {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String query = "SELECT COUNT(*) FROM patient;";
		/*
		 * SqlRowSet rs = jdbcTemplate.queryForRowSet( query ); while (
		 * rs.next() ) { String deValue = rs.getString( 1 ); }
		 */
		return jdbcTemplate.queryForInt(query);
	}

	// 1
	public Integer getNoOfNewPatientEnrolledInHivCare(String gender,
			String ageCategory, String startOfPeriod, String endOfPeriod) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String query = " select count(*)"
				+ " from ("
				+ " select e.encounter_id,e.encounter_datetime,e.patient_id,p.birthdate "
				+ " from encounter e "
				+ " inner join encounter_type et on et.encounter_type_id=e.encounter_type and et.name like 'HIV Enrollment'"
				+ " inner join person p on e.patient_id=p.person_id "
				+ " where e.form_id=8"
				+ " AND DATE(e.encounter_datetime) between "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " and gender like "
				+ "'"
				+ gender
				+ "'"
				+ " and TIMESTAMPDIFF(YEAR,(p.birthdate),(e.encounter_datetime))"
				+ ageCategory + " group by patient_id"
				+ " order by e.patient_id,e.encounter_datetime" + " )sag";
		return jdbcTemplate.queryForInt(query);
	}

	// 2
	public Integer getNoOfPatientTreatedForOpportunisticInfections(
			String gender, String ageCategory, String startOfPeriod,
			String endOfPeriod) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String query = " select count(*)" + " from(" + " select o.person_id"
				+ " from obs o"
				+ " inner join person p on p.person_id=o.person_id "
				+ " and gender like " + "'" + gender + "'"
				+ " and TIMESTAMPDIFF(YEAR,(p.birthdate),(o.obs_datetime))"
				+ ageCategory + " where o.concept_id=163079"
				+ " and o.obs_datetime between " + "'" + startOfPeriod + "'"
				+ " and " + "'" + endOfPeriod + "'" + " and  o.voided=0 "
				+ " group by o.person_id" + " )sag";
		return jdbcTemplate.queryForInt(query);
	}

	// 3
	public Integer getNoOfMedicallyEligiblePatientsWaitingForART(String gender,
			String ageCategory, String startOfPeriod, String endOfPeriod) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String query = " select count(*) tot"
				+ " from ("
				+ " select sag.patient_id"
				+ " from "
				+ " ( "
				+ " select e.patient_id,e.encounter_datetime"
				+ " from encounter e"
				+ " inner join encounter_type et on et.encounter_type_id=e.encounter_type and et.name like 'HIV Enrollment'"
				+ " inner join person p on p.person_id=e.patient_id and p.dead=0"
				+ " and gender like " + "'"
				+ gender
				+ "'"
				+ " and TIMESTAMPDIFF(YEAR,(p.birthdate),(e.encounter_datetime))"
				+ ageCategory
				+ " where  e.encounter_datetime between "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " and e.form_id=8"
				+ " )sag"
				+ " left join "
				+ " ("
				+ " select e.patient_id"
				+ " from encounter e"
				+ " inner join encounter_type et on et.encounter_type_id=e.encounter_type and et.name like 'ART'"
				+ " inner join person p on p.person_id=e.patient_id and p.dead=0"
				+ " and gender like "
				+ "'"
				+ gender
				+ "'"
				+ " and TIMESTAMPDIFF(YEAR,(p.birthdate),(e.encounter_datetime))"
				+ ageCategory
				+ " where  e.encounter_datetime between "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " and e.form_id=31"
				+ " )sag1"
				+ " on sag.patient_id=sag1.patient_id"
				+ " where sag1.patient_id is null"
				+ " group by sag.patient_id"
				+ " )sag3";
		return jdbcTemplate.queryForInt(query);
	}

	// 4.1
	public Integer getCumulativeNoOfActiveFollowUpPatientsStartedAtBegOfMonth(
			String gender, String ageCategory, String startOfPeriod,
			String endOfPeriod) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String query = " select count(*) tot"
				+ " from ("
				+ " select pp.patient_id"
				+ " from patient_program pp"
				+ " inner join program pr on pr.program_id=pp.program_id and pr.name like 'ART'"
				+ " inner join person p on p.person_id=pp.patient_id "
				+ " and gender like "
				+ "'"
				+ gender
				+ "'"
				+ " and TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled))"
				+ ageCategory
				+ " where date_enrolled between DATE_SUB("
				+ "'"
				+ startOfPeriod
				+ "'"
				+ ", INTERVAL 1 MONTH) and DATE_SUB("
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " , INTERVAL 1 MONTH)"
				+ " and case when date_completed is not null then date_completed > "
				+ "'" + startOfPeriod + "'" + " else 1=1 end"
				+ " group by pp.patient_id " + " )sag";

		return jdbcTemplate.queryForInt(query);
	}

	// 4.2
	public Integer getNoOfNewPatientsStartedOnART(String gender,
			String ageCategory, String startOfPeriod, String endOfPeriod) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String query = " SELECT COUNT(*) totb FROM( "
				+ "SELECT DISTINCT pp.patient_id "
				+ " from patient_program pp"
				+ " inner join program pr on pr.program_id=pp.program_id and pr.name like 'ART'"
				+ " left join obs o on o.person_id=pp.patient_id and o.concept_id=160540 "
				+ " inner join person p on p.person_id=pp.patient_id "
				+ " and gender like " + "'" + gender
				+ "'"
				+ " and TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled))"
				+ ageCategory
				+ " where date(date_enrolled) between "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " AND "
				+ "'"
				+ endOfPeriod
				+ "'"
				+
				// " and case when date_completed is not null then date_completed > "+"'"+endOfPeriod+"'"+
				// "else 1=1 end "+
				" and case when o.person_id is not null then o.value_coded not in (162870,162871) else 1=1 end ) a";

		return jdbcTemplate.queryForInt(query);
	}

	// 4.3
	public Integer getNoOfPatientsOnARTTransferredIn(String gender,
			String ageCategory, String startOfPeriod, String endOfPeriod) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String query = " SELECT COUNT(*) totb FROM( "
				+ "SELECT DISTINCT pp.patient_id"
				+ " from patient_program pp "
				+ " inner join program pr on pr.program_id=pp.program_id and pr.name like 'ART'"
				+ " inner join obs o on o.person_id=pp.patient_id and o.concept_id=160540 and o.value_coded in (162870,162871)"
				+ " inner join person p on p.person_id=pp.patient_id "
				+ " and gender like " + "'" + gender + "'"
				+ " and TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled))"
				+ ageCategory + " where date(date_enrolled) between " + "'"
				+ startOfPeriod + "'" + " AND " + "'" + endOfPeriod + "'"
				+ ") b";
		// " and case when date_completed is not null then date_completed > "+"'"+endOfPeriod+"'"+
		// " else 1=1 end ";

		return jdbcTemplate.queryForInt(query);
	}

	// 4.4
	public Integer getCumulativeNoOfActiveFollowUpPatientsStartedAtEndOfMonth(
			String gender, String ageCategory, String startOfPeriod,
			String endOfPeriod) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String query = " select count(*) tot "
				+ " from"
				+ " ("
				+ " select pp.patient_id,'initiate'"
				+ " from patient_program pp"
				+ " inner join program pr on pr.program_id=pp.program_id and pr.name like 'ART'"
				+ " inner join person p on p.person_id=pp.patient_id "
				+ " and gender like " + "'"
				+ gender
				+ "'"
				+ " and TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled)) "
				+ ageCategory
				+ " where date(date_enrolled) between "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " AND "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " group by pp.patient_id"
				+

				" union"
				+

				" select *"
				+ " from "
				+ " ("
				+ " select pp.patient_id,'before'"
				+ " from patient_program pp"
				+ " inner join program pr on pr.program_id=pp.program_id and pr.name like 'ART'"
				+ " inner join person p on p.person_id=pp.patient_id "
				+ " and gender like "
				+ "'"
				+ gender
				+ "'"
				+ " and TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled))"
				+ ageCategory
				+ " where date(date_enrolled) between DATE_SUB("
				+ "'"
				+ startOfPeriod
				+ "'"
				+ ", INTERVAL 1 MONTH) and DATE_SUB("
				+ "'"
				+ endOfPeriod
				+ "'"
				+ ", INTERVAL 1 MONTH)"
				+ " and case when date_completed is not null then date_completed > "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " else 1=1 end"
				+ " group by pp.patient_id" + " )sag" + " )sag1";

		return jdbcTemplate.queryForInt(query);
	}

	// 5.1
	public Integer getNoOfDeathReported(String gender, String ageCategory,
			String startOfPeriod, String endOfPeriod) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String query = " select count(*)  tot "
				+ " from"
				+ " ("
				+ " select pp.patient_id "
				+ " from patient_program pp "
				+ " inner join program pr on pr.program_id=pp.program_id and pr.name like 'ART' "
				+ " inner join person p on p.person_id=pp.patient_id "
				+ " and gender like " + "'"
				+ gender
				+ "'"
				+ " and TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled))"
				+ ageCategory
				+ " where date(date_enrolled) between "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " and date(p.death_date) between "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " group by pp.patient_id "
				+ " union "
				+ " select *"
				+ " from"
				+ " ("
				+ " select pp.patient_id "
				+ " from patient_program pp "
				+ " inner join program pr on pr.program_id=pp.program_id and pr.name like 'ART' "
				+ " inner join person p on p.person_id=pp.patient_id "
				+ " and gender like "
				+ "'"
				+ gender
				+ "'"
				+ " and TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled))"
				+ ageCategory
				+ " where date(date_enrolled) between DATE_SUB("
				+ "'"
				+ startOfPeriod
				+ "'"
				+ ", INTERVAL 1 MONTH) and DATE_SUB("
				+ "'"
				+ endOfPeriod
				+ "'"
				+ ", INTERVAL 1 MONTH) "
				+ " and case when date_completed is not null then date_completed >"
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " else 1=1 end "
				+ " group by pp.patient_id "
				+ " )sag"
				+ " )sag3 "
				+ " inner join "
				+ " ("
				+ " select patient_id "
				+ " from"
				+ " ("
				+ " select patient_id,case when max(ifnull(art,0))> max(ifnull(hiv,0)) then max(art) else max(hiv) end dd,"
				+ " case when max(ifnull(art,0))> max(ifnull(hiv,0)) then 'art' else 'hiv' end dd1,value_coded "
				+ " from"
				+ " ("
				+ " select pid, patient_id,case when sag.name like 'ART' then dd end 'art',"
				+ " case when sag.name like 'HIV' then dd end 'hiv',value_coded "
				+ " from "
				+ " ("
				+ " select s.* from "
				+ " ("
				+ " select pp.patient_id,pp.date_enrolled,pp.date_completed dd,pr.name,pp.patient_program_id pid,o.value_coded "
				+ " from patient_program pp "
				+ " inner join program pr on pr.program_id=pp.program_id "
				+ " inner join person p on p.person_id=pp.patient_id "
				+ " left join obs o on o.person_id = pp.patient_id and o.concept_id=161555 "
				+ " and gender like "
				+ "'"
				+ gender
				+ "'"
				+ " and TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled))"
				+ ageCategory
				+ " where pp.date_completed is not null "
				+ " and date(pp.date_completed) between "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " and case when o.concept_id is not null then o.obs_datetime between pp.date_enrolled and pp.date_completed else 1=1 end "
				+ " and case when o.concept_id is not null then o.value_coded=160034 else 1=1 end "
				+ " )s "
				+ " left join "
				+ " ("
				+ " select patient_id "
				+ " from patient_program pp "
				+ " inner join program pr on pr.program_id=pp.program_id and pr.name like 'HIV' "
				+ " where pp.date_completed is null "
				+ " group by pp.patient_id "
				+ " )s1 on s.patient_id=s1.patient_id "
				+ " where s1.patient_id is null"
				+ " )sag"
				+ " )sag1 "
				+ " group by patient_id "
				+ " )sag2 "
				+ " where case when dd1 like 'hiv' then sag2.value_coded is not null end "
				+ " )sag4 " + " on sag3.patient_id=sag4.patient_id";

		return jdbcTemplate.queryForInt(query);
	}

	// 5.2
	public Integer getNoOfPatientsTransferredOutUnderARV(String gender,
			String ageCategory, String startOfPeriod, String endOfPeriod) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

		String query = "select count(*)  tot "
				+ "from"
				+ "("
				+

				"select pp.patient_id"
				+ " from patient_program pp"
				+ " inner join program pr on pr.program_id=pp.program_id and pr.name like 'ART'"
				+ " inner join person p on p.person_id=pp.patient_id "
				+ " and gender like " + "'"
				+ gender
				+ "'"
				+ " and TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled)) "
				+ ageCategory
				+ " where date(date_enrolled) between "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " group by pp.patient_id"
				+

				" union"
				+

				" select *"
				+ " from "
				+ " ("
				+ " select pp.patient_id"
				+ " from patient_program pp"
				+ " inner join program pr on pr.program_id=pp.program_id and pr.name like 'ART'"
				+ " inner join person p on p.person_id=pp.patient_id "
				+ " and gender like "
				+ "'"
				+ gender
				+ "'"
				+ " and TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled))"
				+ ageCategory
				+ " where date(date_enrolled) between DATE_SUB("
				+ "'"
				+ startOfPeriod
				+ "'"
				+ ", INTERVAL 1 MONTH) and DATE_SUB("
				+ "'"
				+ endOfPeriod
				+ "'"
				+ ", INTERVAL 1 MONTH)"
				+ " and case when date_completed is not null then date_completed > "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " else 1=1 end"
				+ " group by pp.patient_id"
				+ " )sag"
				+

				")sag3"
				+ " inner join "
				+ "("
				+ " select patient_id "
				+ "from"
				+ "("
				+ " select patient_id,case when max(ifnull(art,0))> max(ifnull(hiv,0)) then max(art) else max(hiv) end dd,"
				+ " case when max(ifnull(art,0))> max(ifnull(hiv,0)) then 'art' else 'hiv' end dd1,value_coded"
				+ " from "
				+ " ("
				+ " select pid, patient_id,case when sag.name like 'ART' then dd end 'art',"
				+ " case when sag.name like 'HIV' then dd end 'hiv',value_coded"
				+ " from "
				+ " ("
				+ " select s.* from "
				+ " ("
				+ " select pp.patient_id,pp.date_enrolled,pp.date_completed dd,pr.name,pp.patient_program_id pid,o.value_coded"
				+ " from patient_program pp"
				+ " inner join program pr on pr.program_id=pp.program_id"
				+ " inner join person p on p.person_id=pp.patient_id "
				+ " left join obs o on o.person_id = pp.patient_id and o.concept_id=161555 "
				+ " and gender like "
				+ "'"
				+ gender
				+ "'"
				+ " and TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled))"
				+ ageCategory
				+ " where pp.date_completed is not null"
				+ " and date(pp.date_completed) between "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " and case when o.concept_id is not null then o.obs_datetime between pp.date_enrolled and pp.date_completed else 1=1 end"
				+ " and case when o.concept_id is not null then o.value_coded=159492 and o.voided=0 else 1=1 end"
				+ " )s"
				+ " left join "
				+

				" ("
				+ " select patient_id"
				+ " from patient_program pp"
				+ " inner join program pr on pr.program_id=pp.program_id and pr.name like 'HIV'"
				+ " where pp.date_completed is null"
				+ " group by pp.patient_id"
				+ " )s1 on s.patient_id=s1.patient_id"
				+ " where s1.patient_id is null"
				+ ")sag"
				+ " )sag1"
				+ " group by patient_id"
				+ ")sag2"
				+ " where case when dd1 like 'hiv' then sag2.value_coded is not null end"
				+ ")sag4" + " on sag3.patient_id=sag4.patient_id";

		return jdbcTemplate.queryForInt(query);
	}

	// 5.3
	public Integer getNoOfPatientsLostToFollowUp(String gender,
			String ageCategory, String startOfPeriod, String endOfPeriod) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String query = " select count(*)  tot"
				+ " from"
				+ "("
				+

				" select pp.patient_id"
				+ " from patient_program pp"
				+ " inner join program pr on pr.program_id=pp.program_id and pr.name like 'ART'"
				+ " inner join person p on p.person_id=pp.patient_id "
				+ " and gender like " + "'"
				+ gender
				+ "'"
				+ " and TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled))"
				+ ageCategory
				+ " where date(date_enrolled) between "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " group by pp.patient_id"
				+ " union"
				+ " select *"
				+ " from "
				+ " ("
				+ "  select pp.patient_id"
				+ "  from patient_program pp"
				+ "  inner join program pr on pr.program_id=pp.program_id and pr.name like 'ART'"
				+ "  inner join person p on p.person_id=pp.patient_id "
				+ "  and gender like "
				+ "'"
				+ gender
				+ "'"
				+ "  and TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled))"
				+ ageCategory
				+ "  where date(date_enrolled) between DATE_SUB("
				+ "'"
				+ startOfPeriod
				+ "'"
				+ ", INTERVAL 1 MONTH) and DATE_SUB("
				+ "'"
				+ endOfPeriod
				+ "'"
				+ ", INTERVAL 1 MONTH)"
				+ "  and case when date_completed is not null then date_completed > "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " else 1=1 end"
				+ "  group by pp.patient_id"
				+ " )sag"
				+

				")sag3"
				+ " inner join "
				+ "("
				+ " select patient_id "
				+ "from"
				+ "("
				+ " select patient_id,case when max(ifnull(art,0))> max(ifnull(hiv,0)) then max(art) else max(hiv) end dd,"
				+ " case when max(ifnull(art,0))> max(ifnull(hiv,0)) then 'art' else 'hiv' end dd1,value_coded"
				+ " from "
				+ " ("
				+ " select pid, patient_id,case when sag.name like 'ART' then dd end 'art',"
				+ " case when sag.name like 'HIV' then dd end 'hiv',value_coded"
				+ " from "
				+ " ("
				+ "   select s.* from "
				+ "   ("
				+ "           select pp.patient_id,pp.date_enrolled,pp.date_completed dd,pr.name,pp.patient_program_id pid,o.value_coded"
				+ "           from patient_program pp"
				+ "           inner join program pr on pr.program_id=pp.program_id "
				+ "           inner join person p on p.person_id=pp.patient_id "
				+ "           left join obs o on o.person_id = pp.patient_id and o.concept_id=161555 "
				+ "           and gender like "
				+ "'"
				+ gender
				+ "'"
				+ "           and TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled))"
				+ ageCategory
				+ "           where pp.date_completed is not null"
				+ "           and date(pp.date_completed) between "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ "           and case when o.concept_id is not null then o.obs_datetime between pp.date_enrolled and pp.date_completed else 1=1 end"
				+ "           and case when o.concept_id is not null then o.value_coded=5240 and o.voided=0 else 1=1 end"
				+ "   )s"
				+ "   left join "
				+

				"   ("
				+ "  select patient_id"
				+ " from patient_program pp"
				+ " inner join program pr on pr.program_id=pp.program_id and pr.name like 'HIV'"
				+ " where pp.date_completed is null"
				+ " group by pp.patient_id"
				+ "  )s1 on s.patient_id=s1.patient_id"
				+ "  where s1.patient_id is null"
				+ " )sag"
				+ "  )sag1"
				+ "  group by patient_id"
				+

				")sag2"
				+ " where case when dd1 like 'hiv' then sag2.value_coded is not null end"
				+ ")sag4" + " on sag3.patient_id=sag4.patient_id";

		return jdbcTemplate.queryForInt(query);
	}

	// 5.4
	public Integer getNoOfPatientsStopppedART(String gender,
			String ageCategory, String startOfPeriod, String endOfPeriod) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String query = " select count(* ) tot"
				+ " from"
				+ "("
				+ " select pp.patient_id"
				+ " from patient_program pp"
				+ " inner join program pr on pr.program_id=pp.program_id and pr.name like 'ART'"
				+ " inner join person p on p.person_id=pp.patient_id "
				+ " and gender like " + "'"
				+ gender
				+ "'"
				+ " and TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled))"
				+ ageCategory
				+ " where date(date_enrolled) between "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " group by pp.patient_id"
				+

				" union"
				+ " select *"
				+ " from "
				+ " ("
				+ " select pp.patient_id"
				+ " from patient_program pp"
				+ " inner join program pr on pr.program_id=pp.program_id and pr.name like 'ART'"
				+ " inner join person p on p.person_id=pp.patient_id "
				+ " and gender like "
				+ "'"
				+ gender
				+ "'"
				+ " and TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled))"
				+ ageCategory
				+ " where date(date_enrolled) between DATE_SUB("
				+ "'"
				+ startOfPeriod
				+ "'"
				+ ", INTERVAL 1 MONTH) and DATE_SUB("
				+ "'"
				+ endOfPeriod
				+ "'"
				+ ", INTERVAL 1 MONTH)"
				+ " and case when date_completed is not null then date_completed > "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " else 1=1 end"
				+ " group by pp.patient_id"
				+ ")sag"
				+ ")sag3"
				+ " inner join "
				+ "("
				+ " select sag2.patient_id from"
				+ " ("
				+ " select patient_id,case when max(ifnull(art,0))> max(ifnull(hiv,0)) then max(art) else max(hiv) end dd,"
				+ " case when max(ifnull(art,0))> max(ifnull(hiv,0)) then 'art' else 'hiv' end dd1"
				+ " from "
				+ "("
				+ " select pid, patient_id,case when sag.name like 'ART' then dd end 'art',"
				+ " case when sag.name like 'HIV' then dd end 'hiv'"
				+ " from "
				+ " ("
				+ " select pp.patient_id,pp.date_enrolled,pp.date_completed dd,pr.name,pp.patient_program_id pid"
				+ " from patient_program pp"
				+ " inner join program pr on pr.program_id=pp.program_id "
				+ " inner join person p on p.person_id=pp.patient_id "
				+ "  and gender like "
				+ "'"
				+ gender
				+ "'"
				+ " and TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled))"
				+ ageCategory
				+ " where pp.date_completed is not null"
				+ " and date(pp.date_completed) between "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+

				" )sag"
				+ " )sag1"
				+ " group by patient_id"
				+ " )sag2"
				+ " left join "
				+ " ("
				+ " select pp.patient_id,pp.date_enrolled"
				+ " from patient_program pp"
				+ " inner join program pr on pr.program_id=pp.program_id and pr.name like 'ART'"
				+ " inner join person p on p.person_id=pp.patient_id "
				+ " and gender like "
				+ "'"
				+ gender
				+ "'"
				+ " and TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled))"
				+ ageCategory
				+ " where pp.date_completed is null"
				+ " and date(pp.date_enrolled) between "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " )sag3"
				+ " on sag2.patient_id=sag3.patient_id"
				+ " where sag2.dd1 like 'art'"
				+ " and case when sag3.date_enrolled is not null then sag3.date_enrolled < sag2.dd else 1=1 end"
				+ " )sag4" + " on sag3.patient_id=sag4.patient_id";

		return jdbcTemplate.queryForInt(query);
	}

	// 5.5
	public Integer getNoOfPatientsOnART(String gender, String ageCategory,
			String startOfPeriod, String endOfPeriod) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String query = " select count(* ) tot"
				+ " from"
				+ " ("
				+ " select pp.patient_id"
				+ " from patient_program pp"
				+ " inner join program pr on pr.program_id=pp.program_id and pr.name like 'ART'"
				+ " inner join person p on p.person_id=pp.patient_id "
				+ " and gender like " + "'"
				+ gender
				+ "'"
				+ " and TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled)) "
				+ ageCategory
				+ " where date(date_enrolled) between "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " group by pp.patient_id"
				+

				" union"
				+

				" select *"
				+ " from "
				+ " ("
				+ " select pp.patient_id"
				+ " from patient_program pp"
				+ " inner join program pr on pr.program_id=pp.program_id and pr.name like 'ART'"
				+ " inner join person p on p.person_id=pp.patient_id "
				+ " and gender like "
				+ "'"
				+ gender
				+ "'"
				+ " and TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled))"
				+ ageCategory
				+ " where date(date_enrolled) between DATE_SUB("
				+ "'"
				+ startOfPeriod
				+ "'"
				+ ", INTERVAL 1 MONTH) and DATE_SUB("
				+ "'"
				+ endOfPeriod
				+ "'"
				+ ", INTERVAL 1 MONTH)"
				+ " and case when date_completed is not null then date_completed > "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " else 1=1 end"
				+ " group by pp.patient_id"
				+ " )sag"
				+ " )sag3"
				+ " left join "
				+ " ("
				+ " select sag2.patient_id from"
				+ " ("
				+ "  select patient_id,case when max(ifnull(art,0))> max(ifnull(hiv,0)) then max(art) else max(hiv) end dd,"
				+ "  case when max(ifnull(art,0))> max(ifnull(hiv,0)) then 'art' else 'hiv' end dd1"
				+ " from "
				+ " ("
				+ " select pid, patient_id,case when sag.name like 'ART' then dd end 'art',"
				+ " case when sag.name like 'HIV' then dd end 'hiv'"
				+ " from "
				+ " ("
				+ "  select pp.patient_id,pp.date_enrolled,pp.date_completed dd,pr.name,pp.patient_program_id pid"
				+ "  from patient_program pp"
				+ "  inner join program pr on pr.program_id=pp.program_id "
				+ "  inner join person p on p.person_id=pp.patient_id "
				+ " and gender like "
				+ "'"
				+ gender
				+ "'"
				+ " and TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled))"
				+ ageCategory
				+ " where pp.date_completed is not null"
				+ " and date(pp.date_completed) between "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " )sag"
				+ " )sag1"
				+ " group by patient_id"
				+ " )sag2"
				+ " left join "
				+ " ("
				+ " select patient_id,case when max(ifnull(art,0))> max(ifnull(hiv,0)) then max(art) else max(hiv) end dd,"
				+ " case when max(ifnull(art,0))> max(ifnull(hiv,0)) then 'art' else 'hiv' end dd1"
				+ " from "
				+ "  ("
				+ " select pid, patient_id,case when sag.name like 'ART' then dd end 'art',"
				+ " case when sag.name like 'HIV' then dd end 'hiv'"
				+ " from "
				+ " ("
				+ "  select pp.patient_id,pp.date_enrolled dd,pr.name,pp.patient_program_id pid"
				+ "  from patient_program pp"
				+ "  inner join program pr on pr.program_id=pp.program_id "
				+ "  inner join person p on p.person_id=pp.patient_id "
				+ " and gender like "
				+ "'"
				+ gender
				+ "'"
				+ " and TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled))"
				+ ageCategory
				+ " where pp.date_completed is null"
				+ " and date(pp.date_enrolled) between "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " )sag"
				+ " )sag1"
				+ " group by patient_id"
				+ " )sag3"
				+ " on sag2.patient_id=sag3.patient_id "
				+ " where case when sag3.patient_id is not null and sag2.dd1=sag3.dd1 then sag2.dd > sag3.dd else 1=1 end"
				+ " )sag4"
				+ " on sag3.patient_id=sag4.patient_id"
				+ " where sag4.patient_id is null";

		return jdbcTemplate.queryForInt(query);
	}

	// 5.5.1
	public Integer getNoOfPatientsOnOriginalFirstLineRegim(String gender,
			String ageCategory, String startOfPeriod, String endOfPeriod) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String query = "SELECT COUNT(*) FROM "
				+ "( "
				+ "SELECT sag3.patient_id"
				+ " FROM"
				+ "( "
				+ "  SELECT pp.patient_id"
				+ "  FROM patient_program pp"
				+ "  INNER JOIN program pr ON pr.program_id=pp.program_id AND pr.name LIKE 'ART'"
				+ "  INNER JOIN person p ON p.person_id=pp.patient_id "
				+ "  AND gender LIKE " + "'"
				+ gender
				+ "'"
				+ "  AND TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled)) "
				+ ageCategory
				+ "   WHERE DATE(date_enrolled) BETWEEN "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ "  GROUP BY pp.patient_id"
				+

				"  UNION"
				+

				" SELECT *"
				+ " FROM "
				+ " ("
				+ " SELECT pp.patient_id"
				+ "  FROM patient_program pp"
				+ "  INNER JOIN program pr ON pr.program_id=pp.program_id AND pr.name LIKE 'ART'"
				+ " INNER JOIN person p ON p.person_id=pp.patient_id "
				+ " AND gender LIKE "
				+ "'"
				+ gender
				+ "'"
				+ "   AND TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled))"
				+ ageCategory
				+ "   WHERE DATE(date_enrolled) BETWEEN DATE_SUB("
				+ "'"
				+ startOfPeriod
				+ "'"
				+ ", INTERVAL 1 MONTH) AND DATE_SUB("
				+ "'"
				+ endOfPeriod
				+ "'"
				+ ", INTERVAL 1 MONTH)"
				+ "   AND CASE WHEN date_completed IS NOT NULL THEN date_completed > "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ "ELSE 1=1 END"
				+ "   GROUP BY pp.patient_id"
				+ " )sag"
				+ ")sag3"
				+ " LEFT JOIN "
				+ "("
				+ "  SELECT sag2.patient_id FROM"
				+ " ( "
				+ " SELECT patient_id,CASE WHEN MAX(IFNULL(art,0))> MAX(IFNULL(hiv,0)) THEN MAX(art) ELSE MAX(hiv) END dd,"
				+ " CASE WHEN MAX(IFNULL(art,0))> MAX(IFNULL(hiv,0)) THEN 'art' ELSE 'hiv' END dd1"
				+ " FROM "
				+ " ("
				+ " SELECT pid, patient_id,CASE WHEN sag.name LIKE 'ART' THEN dd END 'art',"
				+ " CASE WHEN sag.name LIKE 'HIV' THEN dd END 'hiv'"
				+ " FROM "
				+ " ("
				+ " SELECT pp.patient_id,pp.date_enrolled,pp.date_completed dd,pr.name,pp.patient_program_id pid"
				+ " FROM patient_program pp"
				+ " INNER JOIN program pr ON pr.program_id=pp.program_id"
				+ " INNER JOIN person p ON p.person_id=pp.patient_id "
				+ " AND gender LIKE "
				+ "'"
				+ gender
				+ "'"
				+ " AND TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled))"
				+ ageCategory
				+ " WHERE pp.date_completed IS NOT NULL"
				+ " AND DATE(pp.date_completed) BETWEEN "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " )sag"
				+ " )sag1"
				+ " GROUP BY patient_id"
				+ " )sag2"
				+ " LEFT JOIN "
				+ " ("
				+ " SELECT patient_id,CASE WHEN MAX(IFNULL(art,0))> MAX(IFNULL(hiv,0)) THEN MAX(art) ELSE MAX(hiv) END dd,"
				+ " CASE WHEN MAX(IFNULL(art,0))> MAX(IFNULL(hiv,0)) THEN 'art' ELSE 'hiv' END dd1"
				+ " FROM "
				+ " ("
				+ " SELECT pid, patient_id,CASE WHEN sag.name LIKE 'ART' THEN dd END 'art',"
				+ " CASE WHEN sag.name LIKE 'HIV' THEN dd END 'hiv'"
				+ " FROM "
				+ " ("
				+ " SELECT pp.patient_id,pp.date_enrolled dd,pr.name,pp.patient_program_id pid"
				+ " FROM patient_program pp"
				+ " INNER JOIN program pr ON pr.program_id=pp.program_id "
				+ " INNER JOIN person p ON p.person_id=pp.patient_id "
				+ " AND gender LIKE "
				+ "'"
				+ gender
				+ "'"
				+ " AND TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled))"
				+ ageCategory
				+ "  WHERE pp.date_completed IS NULL"
				+ "  AND DATE(pp.date_enrolled) BETWEEN "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " )sag"
				+ " )sag1"
				+ " GROUP BY patient_id"
				+ " )sag3"
				+ " ON sag2.patient_id=sag3.patient_id"
				+ " WHERE CASE WHEN sag3.patient_id IS NOT NULL AND sag2.dd1=sag3.dd1 THEN sag2.dd>sag3.dd ELSE 1=1 END"
				+ " )sag4"
				+ " ON sag3.patient_id=sag4.patient_id"
				+ " WHERE sag4.patient_id IS NULL"
				+ " )sag5"
				+ " INNER JOIN"
				+ " ("
				+ " SELECT sag.patient_id  "
				+ " FROM "
				+ " ("
				+ " SELECT patient_id,start_date,regimen_change_type"
				+ " FROM drug_order_processed d"
				+ "  WHERE DATE(d.start_date) BETWEEN DATE_SUB("
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " , INTERVAL 1 MONTH) AND "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " AND d.discontinued_date IS NULL "
				+ "AND d.regimen_change_type IN ('Start','Restart') "
				+ "AND d.type_of_regimen IN ('First line Anti-retoviral drugs','Fixed dose combinations (FDCs)') "
				+ "GROUP BY patient_id,d.regimen_change_type"
				+ " )sag"
				+ " LEFT JOIN "
				+ "  ("
				+ " SELECT patient_id,start_date,regimen_change_type"
				+ "  FROM drug_order_processed d"
				+ "  WHERE DATE(d.start_date) BETWEEN DATE_SUB("
				+ "'"
				+ startOfPeriod
				+ "'"
				+ ", INTERVAL 1 MONTH) AND "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " AND d.discontinued_date IS NULL "
				+ "AND d.regimen_change_type IN ('Substitute','Switch') "
				+ "GROUP BY patient_id,d.regimen_change_type"
				+ " )sag1"
				+ "  ON sag.patient_id=sag1.patient_id"
				+ " WHERE CASE WHEN sag1.patient_id IS NOT NULL THEN sag1.start_date < sag.start_date ELSE sag1.patient_id IS NULL END "
				+ " )sag6" 
				+ " ON sag5.patient_id=sag6.patient_id";

		return jdbcTemplate.queryForInt(query);
	}

	// 5.5.2
	public Integer getNoOfPatientsSubstitutedFirstLineRegim(String gender,
			String ageCategory, String startOfPeriod, String endOfPeriod) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String query = " select count(*) tot "
				+ "from"
				+ "("
				+ "select sag3.patient_id "
				+ "from"
				+ "("
				+ "select pp.patient_id "
				+ "from patient_program pp "
				+ "inner join program pr on pr.program_id=pp.program_id and pr.name like 'ART' "
				+ "inner join person p on p.person_id=pp.patient_id "
				+ "and gender like" + "'"+ gender + "'"
				+ " and TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled))"+ ageCategory
				+ " where date(date_enrolled) between " + "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ "group by pp.patient_id "

				+ "union "

				+ "select * "
				+ "from "
				+ "("
				+ "select pp.patient_id "
				+ "from patient_program pp "
				+ "inner join program pr on pr.program_id=pp.program_id and pr.name like 'ART' "
				+ "inner join person p on p.person_id=pp.patient_id "
				+ "and gender like "  + "'"+ gender + "'"
				+ " and TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled)) "+ ageCategory
				+ " where date(date_enrolled) between DATE_SUB("
				+ "'"
				+ startOfPeriod
				+ "'"
				+ ", INTERVAL 1 MONTH) and DATE_SUB("
				+ "'"
				+ endOfPeriod
				+ "'"
				+ ", INTERVAL 1 MONTH) "
				+ "and case when date_completed is not null then date_completed > "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " else 1=1 end "
				+ "group by pp.patient_id"
				+ ")sag"
				+ ")sag3 "
				+ "left join"
				+ "("
				+ "select sag2.patient_id from"
				+ "("
				+ "select patient_id,case when max(ifnull(art,0))> max(ifnull(hiv,0)) then max(art) else max(hiv) end dd,"
				+ "case when max(ifnull(art,0))> max(ifnull(hiv,0)) then 'art' else 'hiv' end dd1 "
				+ "from"
				+ "("
				+ "select pid, patient_id,case when sag.name like 'ART' then dd end 'art',"
				+ "case when sag.name like 'HIV' then dd end 'hiv' "
				+ "from"
				+ "("
				+ "select pp.patient_id,pp.date_enrolled,pp.date_completed dd,pr.name,pp.patient_program_id pid "
				+ "from patient_program pp "
				+ "inner join program pr on pr.program_id=pp.program_id "
				+ "inner join person p on p.person_id=pp.patient_id "
				+ "and gender like "  + "'"+ gender + "'"
				+ " and TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled)) "+ ageCategory
				+ " where pp.date_completed is not null "
				+ " and date(pp.date_completed) between "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ ")sag"
				+ ")sag1 "
				+ "group by patient_id"
				+ ")sag2 "
				+ "left join"
				+ "("
				+ "select patient_id,case when max(ifnull(art,0))> max(ifnull(hiv,0)) then max(art) else max(hiv) end dd,"
				+ "case when max(ifnull(art,0))> max(ifnull(hiv,0)) then 'art' else 'hiv' end dd1 "
				+ "from"
				+ "("
				+ "select pid, patient_id,case when sag.name like 'ART' then dd end 'art',"
				+ "case when sag.name like 'HIV' then dd end 'hiv' "
				+ "from"
				+ "("
				+ "select pp.patient_id,pp.date_enrolled dd,pr.name,pp.patient_program_id pid "
				+ "from patient_program pp "
				+ "inner join program pr on pr.program_id=pp.program_id "
				+ "inner join person p on p.person_id=pp.patient_id "
				+ "and gender like "  + "'"+ gender + "'"
				+ "and TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled)) "+ ageCategory
				+ " where pp.date_completed is null "
				+ "and date(pp.date_enrolled) between "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ ")sag"
				+ ")sag1 "
				+ "group by patient_id"
				+ ")sag3 "
				+ "on sag2.patient_id=sag3.patient_id "
				+ "where case when sag3.patient_id is not null and sag2.dd1=sag3.dd1 then sag2.dd>sag3.dd else 1=1 end"
				+ ")sag4 "
				+ "on sag3.patient_id=sag4.patient_id "
				+ "where sag4.patient_id is null"

				+ ")sag5 "
				+ "inner join"
				+ "("

				+ "select patient_id,start_date,regimen_change_type "
				+ "from drug_order_processed d "
				+ "where DATE(d.start_date) BETWEEN DATE_SUB("
				+ "'"
				+ startOfPeriod
				+ "'"
				+ ", INTERVAL 1 MONTH) and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " AND d.discontinued_date IS NULL "
				+ "and d.regimen_change_type in ('Substitute')  and d.type_of_regimen in ('First line Anti-retoviral drugs','Fixed dose combinations (FDCs)') "
				+ "group by patient_id,d.regimen_change_type"
				+ ")sag6 "
				+ "on sag5.patient_id=sag6.patient_id";

		return jdbcTemplate.queryForInt(query);
	}

	// 5.5.3
	public Integer getNoOfPatientsSwitchedToSecondLineRegim(String gender,
			String ageCategory, String startOfPeriod, String endOfPeriod) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String query = " select count(*) tot "
				+ " from "
				+ " ( "
				+ " select sag3.patient_id"
				+ " from "
				+ " ( "
				+ " select pp.patient_id"
				+ " from patient_program pp "
				+ " inner join program pr on pr.program_id=pp.program_id and pr.name like 'ART'"
				+ " inner join person p on p.person_id=pp.patient_id "
				+ " and gender like " + "'"
				+ gender
				+ "'"
				+ " and TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled)) "
				+ ageCategory
				+ " where date(date_enrolled) between "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " group by pp.patient_id"
				+

				" union"
				+

				" select *"
				+ " from "
				+ " ("
				+ " select pp.patient_id"
				+ " from patient_program pp"
				+ " inner join program pr on pr.program_id=pp.program_id and pr.name like 'ART'"
				+ " inner join person p on p.person_id=pp.patient_id "
				+ " and gender like "
				+ "'"
				+ gender
				+ "'"
				+ " and TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled))"
				+ ageCategory
				+ " where date(date_enrolled) between DATE_SUB("
				+ "'"
				+ startOfPeriod
				+ "'"
				+ ", INTERVAL 1 MONTH) and DATE_SUB("
				+ "'"
				+ endOfPeriod
				+ "'"
				+ ", INTERVAL 1 MONTH)"
				+ " and case when date_completed is not null then date_completed > "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " else 1=1 end"
				+ " group by pp.patient_id"
				+ " )sag"
				+ " )sag3"
				+ " left join "
				+ " ( "
				+ " select sag2.patient_id from "
				+ " ("
				+ " select patient_id,case when max(ifnull(art,0))> max(ifnull(hiv,0)) then max(art) else max(hiv) end dd,"
				+ " case when max(ifnull(art,0))> max(ifnull(hiv,0)) then 'art' else 'hiv' end dd1"
				+ " from "
				+ " ( "
				+ " select pid, patient_id,case when sag.name like 'ART' then dd end 'art',"
				+ "  case when sag.name like 'HIV' then dd end 'hiv'"
				+ " from "
				+ " ("
				+ " select pp.patient_id,pp.date_enrolled,pp.date_completed dd,pr.name,pp.patient_program_id pid"
				+ " from patient_program pp"
				+ " inner join program pr on pr.program_id=pp.program_id "
				+ " inner join person p on p.person_id=pp.patient_id "
				+ " and gender like "
				+ "'"
				+ gender
				+ "'"
				+ " and TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled))"
				+ ageCategory
				+ " where pp.date_completed is not null"
				+ " and date(pp.date_completed) between "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " )sag"
				+ " )sag1 "
				+ "  group by patient_id"
				+ " )sag2"
				+ " left join "
				+ " ( "
				+ " select patient_id,case when max(ifnull(art,0))> max(ifnull(hiv,0)) then max(art) else max(hiv) end dd,"
				+ " case when max(ifnull(art,0))> max(ifnull(hiv,0)) then 'art' else 'hiv' end dd1"
				+ " from "
				+ " ("
				+ " select pid, patient_id,case when sag.name like 'ART' then dd end 'art',"
				+ " case when sag.name like 'HIV' then dd end 'hiv'"
				+ " from "
				+ " ("
				+ "  select pp.patient_id,pp.date_enrolled dd,pr.name,pp.patient_program_id pid"
				+ "  from patient_program pp"
				+ "  inner join program pr on pr.program_id=pp.program_id "
				+ "  inner join person p on p.person_id=pp.patient_id "
				+ "  and gender like "
				+ "'"
				+ gender
				+ "'"
				+ "  and TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled))"
				+ ageCategory
				+ " where pp.date_completed is null"
				+ " and date(pp.date_enrolled) between "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " )sag"
				+ " )sag1"
				+ " group by patient_id"
				+ " )sag3 "
				+ "  on sag2.patient_id=sag3.patient_id "
				+ " where case when sag3.patient_id is not null and sag2.dd1=sag3.dd1 then sag2.dd>sag3.dd else 1=1 end"
				+ " )sag4 "
				+ " on sag3.patient_id=sag4.patient_id "
				+ " where sag4.patient_id is null"
				+

				" )sag5"
				+ " inner join"
				+ " ("
				+

				" select patient_id,start_date,regimen_change_type"
				+ " from drug_order_processed d"
				+ " where DATE(d.start_date) BETWEEN DATE_SUB("
				+ "'"
				+ startOfPeriod
				+ "'"
				+ ", INTERVAL 1 MONTH) and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " AND d.discontinued_date IS NULL "
				+ "and d.regimen_change_type in ('Switch')  and d.type_of_regimen in ('Second line ART','Fixed dose combinations (FDCs)') "
				+ "AND drug_regimen!='AZT/3TC+TDF+LPV/r' "
				+ "group by patient_id,d.regimen_change_type"
				+ " )sag6"
				+ " on sag5.patient_id=sag6.patient_id";
		return jdbcTemplate.queryForInt(query);
	}

	// 5.5.4
	public Integer getNoOfPatientsSwitchedToThirdLineRegim(String gender,
			String ageCategory, String startOfPeriod, String endOfPeriod) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String query = "select count(*) tot"
				+ " from "
				+ " ("
				+ " select sag3.patient_id"
				+ " from"
				+ " ("
				+ " select pp.patient_id"
				+ " from patient_program pp"
				+ " inner join program pr on pr.program_id=pp.program_id and pr.name like 'ART'"
				+ " inner join person p on p.person_id=pp.patient_id "
				+ " and gender like " + "'"
				+ gender
				+ "'"
				+ " and TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled)) "
				+ ageCategory
				+ " where date(date_enrolled) between "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " group by pp.patient_id"
				+

				" union"
				+

				" select *"
				+ " from "
				+ " ("
				+ " select pp.patient_id"
				+ " from patient_program pp"
				+ " inner join program pr on pr.program_id=pp.program_id and pr.name like 'ART'"
				+ " inner join person p on p.person_id=pp.patient_id "
				+ " and gender like "
				+ "'"
				+ gender
				+ "'"
				+ " and TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled))"
				+ ageCategory
				+ " where date(date_enrolled) between DATE_SUB("
				+ "'"
				+ startOfPeriod
				+ "'"
				+ ", INTERVAL 1 MONTH) and DATE_SUB("
				+ "'"
				+ endOfPeriod
				+ "'"
				+ ", INTERVAL 1 MONTH)"
				+ " and case when date_completed is not null then date_completed > "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " else 1=1 end"
				+ " group by pp.patient_id"
				+ " )sag"
				+ " )sag3"
				+ " left join "
				+ " ("
				+ " select sag2.patient_id from"
				+ " ("
				+ " select patient_id,case when max(ifnull(art,0))> max(ifnull(hiv,0)) then max(art) else max(hiv) end dd,"
				+ " case when max(ifnull(art,0))> max(ifnull(hiv,0)) then 'art' else 'hiv' end dd1"
				+ " from "
				+ " ("
				+ " select pid, patient_id,case when sag.name like 'ART' then dd end 'art',"
				+ " case when sag.name like 'HIV' then dd end 'hiv'"
				+ " from "
				+ " ("
				+ " select pp.patient_id,pp.date_enrolled,pp.date_completed dd,pr.name,pp.patient_program_id pid"
				+ " from patient_program pp"
				+ " inner join program pr on pr.program_id=pp.program_id "
				+ " inner join person p on p.person_id=pp.patient_id "
				+ " and gender like "
				+ "'"
				+ gender
				+ "'"
				+ " and TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled))"
				+ ageCategory
				+ " where pp.date_completed is not null"
				+ " and date(pp.date_completed) between "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " )sag"
				+ " )sag1"
				+ " group by patient_id"
				+ " )sag2"
				+ " left join "
				+ " ("
				+ "  select patient_id,case when max(ifnull(art,0))> max(ifnull(hiv,0)) then max(art) else max(hiv) end dd,"
				+ "  case when max(ifnull(art,0))> max(ifnull(hiv,0)) then 'art' else 'hiv' end dd1"
				+ "  from "
				+ " ("
				+ " select pid, patient_id,case when sag.name like 'ART' then dd end 'art',"
				+ " case when sag.name like 'HIV' then dd end 'hiv'"
				+ " from "
				+ " ("
				+ " select pp.patient_id,pp.date_enrolled dd,pr.name,pp.patient_program_id pid"
				+ " from patient_program pp"
				+ " inner join program pr on pr.program_id=pp.program_id "
				+ " inner join person p on p.person_id=pp.patient_id "
				+ " and gender like "
				+ "'"
				+ gender
				+ "'"
				+ " and TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled))"
				+ ageCategory
				+ "  where pp.date_completed is null"
				+ "  and date(pp.date_enrolled) between "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " )sag"
				+ " )sag1"
				+ " group by patient_id"
				+ " )sag3"
				+ " on sag2.patient_id=sag3.patient_id"
				+ " where case when sag3.patient_id is not null and sag2.dd1=sag3.dd1 then sag2.dd>sag3.dd else 1=1 end"
				+ " )sag4"
				+ " on sag3.patient_id=sag4.patient_id"
				+ " where sag4.patient_id is null"
				+

				" )sag5"
				+ " inner join"
				+ " ("
				+ " select patient_id,start_date,regimen_change_type"
				+ " from drug_order_processed d"
				+ " where DATE(d.start_date) BETWEEN DATE_SUB("
				+ "'"
				+ startOfPeriod
				+ "'"
				+ ", INTERVAL 1 MONTH) and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " AND d.discontinued_date IS NULL "
				+ "and d.regimen_change_type in ('Switch')  and d.type_of_regimen in ('Fixed dose combinations (FDCs)') "
				+ "and d.drug_regimen like 'AZT/3TC+TDF+LPV/r'"
				+ " group by patient_id,d.regimen_change_type"
				+ " )sag6"
				+ " on sag5.patient_id=sag6.patient_id";
		return jdbcTemplate.queryForInt(query);
	}

	// 6.1
	public Integer getNoOfHIVPositiveTBPatients(String gender,
			String ageCategory, String startOfPeriod, String endOfPeriod) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String query = "SELECT COUNT(*) tot"
				+ " FROM"
				+ " ( SELECT tb1.patient_id"
				+ " FROM"
				+ " ("
				+ " SELECT pp.patient_id"
				+ " FROM patient_program pp"
				+ " INNER JOIN program pr ON pr.program_id=pp.program_id AND pr.name LIKE 'ART'"
				+ " INNER JOIN person p ON p.person_id=pp.patient_id "
				+ " INNER JOIN obs o  ON o.person_id=pp.patient_id"
				+ " AND o.concept_id LIKE '162872'"
				+ " AND o.value_coded LIKE '1663'" + " AND gender LIKE " + "'"
				+ gender
				+ "'"
				+ " AND TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled)) "
				+ ageCategory
				+ " WHERE DATE(date_enrolled) BETWEEN "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " AND "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " GROUP BY pp.patient_id"
				+ " )tb1"
				+ " LEFT JOIN "
				+ " ("
				+

				" SELECT p.person_id"
				+ " FROM person p"
				+ " INNER JOIN encounter e ON e.patient_id=p.person_id"
				+ " INNER JOIN encounter_type et ON et.encounter_type_id=e.encounter_type AND et.name LIKE 'ART'"
				+ " AND gender LIKE "
				+ "'"
				+ gender
				+ "'"
				+ " WHERE p.dead=1 "
				+ " AND death_date BETWEEN "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " AND "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " AND TIMESTAMPDIFF(YEAR,(p.birthdate),(p.death_date))"
				+ ageCategory
				+ " GROUP BY p.person_id"
				+

				" UNION"
				+

				" SELECT pp.patient_id"
				+ " FROM patient_program pp"
				+ " INNER JOIN program pr ON pr.program_id=pp.program_id AND pr.name LIKE 'HIV'"
				+ " INNER JOIN obs o ON o.person_id = pp.patient_id AND o.concept_id=161555 "
				+ " INNER JOIN person p ON p.person_id=pp.patient_id "
				+ " AND gender LIKE "
				+ "'"
				+ gender
				+ "'"
				+ " AND TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled)) "
				+ ageCategory
				+ " WHERE o.obs_datetime BETWEEN "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " AND "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " AND o.obs_datetime BETWEEN pp.date_enrolled AND pp.date_completed"
				+ " AND o.value_coded=5240"
				+ " UNION "
				+ " SELECT pp.patient_id"
				+ " FROM patient_program pp"
				+ " INNER JOIN program pr ON pr.program_id=pp.program_id AND pr.name LIKE 'ART'"
				+ " INNER JOIN person p ON p.person_id=pp.patient_id "
				+ " LEFT JOIN obs o ON o.person_id = pp.patient_id AND o.concept_id=161555 "
				+ " AND gender LIKE "
				+ "'"
				+ gender
				+ "'"
				+ " AND TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled))"
				+ ageCategory
				+ " WHERE pp.date_completed IS NOT NULL"
				+ " AND DATE(pp.date_completed) BETWEEN "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " AND "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " AND CASE WHEN o.concept_id IS NOT NULL THEN o.obs_datetime BETWEEN pp.date_enrolled AND pp.date_completed ELSE 1=1 END"
				+ " AND CASE WHEN o.concept_id IS NOT NULL THEN o.value_coded=159492 ELSE 1=1 END"
				+ " )tb2"
				+ " ON tb1.patient_id=tb2.person_id"
				+ " WHERE tb2.person_id IS  NULL"
				+ " GROUP BY tb1.patient_id"
				+ " )tb";
		return jdbcTemplate.queryForInt(query);
	}

	// 6.2
	public Integer getCumulativeNoOfHIVPositiveTBPatients(String gender,
			String ageCategory, String startOfPeriod, String endOfPeriod) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String query = "SELECT COUNT(*) tot"
				+ " FROM"
				+ " ( SELECT tb1.patient_id"
				+ " FROM"
				+ " ("
				+ " SELECT pp.patient_id"
				+ " FROM patient_program pp"
				+ " INNER JOIN program pr ON pr.program_id=pp.program_id AND pr.name LIKE 'ART'"
				+ " INNER JOIN person p ON p.person_id=pp.patient_id "
				+ " INNER JOIN obs o  ON o.person_id=pp.patient_id"
				+ " AND o.concept_id LIKE '162872'"
				+ " AND o.value_coded LIKE '1663'" + " AND gender LIKE " + "'"
				+ gender
				+ "'"
				+ " AND TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled))"
				+ ageCategory
				+ " WHERE DATE(date_enrolled) BETWEEN "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " AND "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " GROUP BY pp.patient_id"
				+ " UNION"
				+

				" SELECT *"
				+ " FROM "
				+ " ("
				+ " SELECT pp.patient_id"
				+ " FROM patient_program pp"
				+ " INNER JOIN program pr ON pr.program_id=pp.program_id AND pr.name LIKE 'ART'"
				+ " INNER JOIN person p ON p.person_id=pp.patient_id "
				+ " INNER JOIN obs o  ON o.person_id=pp.patient_id"
				+ " AND o.concept_id LIKE '162872'"
				+ " AND o.value_coded LIKE '1663'"
				+ " AND gender LIKE "
				+ "'"
				+ gender
				+ "'"
				+ " AND TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled))"
				+ ageCategory
				+ " WHERE DATE(date_enrolled) BETWEEN DATE_SUB("
				+ "'"
				+ startOfPeriod
				+ "'"
				+ ", INTERVAL 1 MONTH) AND DATE_SUB("
				+ "'"
				+ endOfPeriod
				+ "'"
				+ ", INTERVAL 1 MONTH)"
				+ " AND CASE WHEN date_completed IS NOT NULL THEN date_completed >"
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " ELSE 1=1 END"
				+ " GROUP BY pp.patient_id"
				+ " )sag"
				+ " )tb1"
				+ " LEFT JOIN "
				+ " ("
				+

				" SELECT p.person_id"
				+ " FROM person p"
				+ " INNER JOIN encounter e ON e.patient_id=p.person_id"
				+ " INNER JOIN encounter_type et ON et.encounter_type_id=e.encounter_type AND et.name LIKE 'ART'"
				+ " AND gender LIKE "
				+ "'"
				+ gender
				+ "'"
				+ " WHERE p.dead=1 "
				+ " AND death_date BETWEEN "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " AND "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " AND TIMESTAMPDIFF(YEAR,(p.birthdate),(p.death_date))"
				+ ageCategory
				+ " GROUP BY p.person_id"
				+

				" UNION"
				+

				" SELECT pp.patient_id"
				+ " FROM patient_program pp"
				+ " INNER JOIN program pr ON pr.program_id=pp.program_id AND pr.name LIKE 'HIV'"
				+ " INNER JOIN obs o ON o.person_id = pp.patient_id AND o.concept_id=161555 "
				+ " INNER JOIN person p ON p.person_id=pp.patient_id "
				+ " AND gender LIKE "
				+ "'"
				+ gender
				+ "'"
				+ " AND TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled)) "
				+ ageCategory
				+ " WHERE o.obs_datetime BETWEEN "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " AND "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " AND o.obs_datetime BETWEEN pp.date_enrolled AND pp.date_completed"
				+ " AND o.value_coded=5240"
				+ " UNION "
				+ " SELECT pp.patient_id"
				+ " FROM patient_program pp"
				+ " INNER JOIN program pr ON pr.program_id=pp.program_id AND pr.name LIKE 'ART'"
				+ " INNER JOIN person p ON p.person_id=pp.patient_id "
				+ " LEFT JOIN obs o ON o.person_id = pp.patient_id AND o.concept_id=161555 "
				+ " AND gender LIKE "
				+ "'"
				+ gender
				+ "'"
				+ " AND TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled))"
				+ ageCategory
				+ " WHERE pp.date_completed IS NOT NULL"
				+ " AND DATE(pp.date_completed) BETWEEN "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " AND "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " AND CASE WHEN o.concept_id IS NOT NULL THEN o.obs_datetime BETWEEN pp.date_enrolled AND pp.date_completed ELSE 1=1 END"
				+ " AND CASE WHEN o.concept_id IS NOT NULL THEN o.value_coded=159492 ELSE 1=1 END"
				+ " )tb2"
				+ " ON tb1.patient_id=tb2.person_id"
				+ " WHERE tb2.person_id IS  NULL"
				+ " GROUP BY tb1.patient_id"
				+ " )tb";
		return jdbcTemplate.queryForInt(query);
	}

	// 7.1
	public Integer getNoOfPatientsAssessedForAdherenceDuringThisMonth(
			String ageCategory, String startOfPeriod, String endOfPeriod) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String query = "SELECT COUNT(*) tot"
				+ " FROM"
				+ " ( SELECT adherence.patient_id"
				+ " FROM"
				+ " ("
				+ " SELECT pp.patient_id"
				+ " FROM patient_program pp"
				+ " INNER JOIN program pr ON pr.program_id=pp.program_id AND pr.name LIKE 'ART'"
				+ " INNER JOIN person p ON p.person_id=pp.patient_id "
				+ " INNER JOIN obs o ON o.person_id=pp.patient_id"
				+ " AND o.concept_id LIKE '162945'"
				+ " AND TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled)) "
				+ ageCategory
				+ " WHERE DATE(date_enrolled) BETWEEN "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " AND "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " GROUP BY pp.patient_id"
				+

				" UNION"
				+

				"  SELECT *"
				+ " FROM "
				+ " ("
				+ " SELECT pp.patient_id"
				+ " FROM patient_program pp"
				+ " INNER JOIN program pr ON pr.program_id=pp.program_id AND pr.name LIKE 'ART'"
				+ " INNER JOIN person p ON p.person_id=pp.patient_id "
				+ " INNER JOIN drug_order_processed d ON d.patient_id=pp.patient_id"
				+ " INNER JOIN obs o ON o.person_id=pp.patient_id"
				+ " AND o.concept_id LIKE '162945'"
				+ " AND TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled))"
				+ ageCategory
				+ " WHERE DATE(date_enrolled) BETWEEN DATE_SUB("
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " , INTERVAL 1 MONTH) AND DATE_SUB("
				+ "'"
				+ endOfPeriod
				+ "'"
				+ ", INTERVAL 1 MONTH)"
				+ " AND CASE WHEN date_completed IS NOT NULL THEN date_completed >"
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " ELSE 1=1 END"
				+ " GROUP BY pp.patient_id"
				+ " )sag"
				+ " )adherence"
				+ " LEFT JOIN "
				+ " ("
				+

				" SELECT p.person_id"
				+ " FROM person p"
				+ " INNER JOIN encounter e ON e.patient_id=p.person_id"
				+ " INNER JOIN encounter_type et ON et.encounter_type_id=e.encounter_type AND et.name LIKE 'ART'"
				+ " WHERE p.dead=1 "
				+ " AND death_date BETWEEN "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " AND "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " AND TIMESTAMPDIFF(YEAR,(p.birthdate),(p.death_date))"
				+ ageCategory
				+ " GROUP BY p.person_id"
				+ " UNION "
				+ " SELECT pp.patient_id"
				+ " FROM patient_program pp"
				+ " INNER JOIN program pr ON pr.program_id=pp.program_id AND pr.name LIKE 'ART'"
				+ " INNER JOIN person p ON p.person_id=pp.patient_id "
				+ " AND TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled))"
				+ ageCategory
				+ " WHERE pp.date_completed IS NOT NULL"
				+ " AND DATE(pp.date_completed) BETWEEN "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " AND "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " )adherence1"
				+ " ON adherence.patient_id=adherence1.person_id"
				+ " WHERE adherence1.person_id IS  NULL"
				+ " GROUP BY adherence.patient_id" + " )adherence2";
		return jdbcTemplate.queryForInt(query);
	}

	// 7.2.1
	public Integer getNoOfPatientsAssessedForAdherenceDuringTheLastMonthLevelOneTot(
			String ageCategory, String startOfPeriod, String endOfPeriod) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String query = "SELECT COUNT(*) tot"
				+ " FROM"
				+ " ( SELECT adherence.patient_id"
				+ " FROM"
				+ " ("
				+ " SELECT pp.patient_id"
				+ " FROM patient_program pp"
				+ " INNER JOIN program pr ON pr.program_id=pp.program_id AND pr.name LIKE 'ART'"
				+ " INNER JOIN person p ON p.person_id=pp.patient_id "
				+ " INNER JOIN obs o ON o.person_id=pp.patient_id"
				+ " AND o.concept_id LIKE '162945'"
				+ " AND o.value_text ='>95%' AND o.voided=0"
				+ " AND TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled)) "
				+ ageCategory
				+ " WHERE DATE(date_enrolled) BETWEEN "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " AND "
				+ "'"
				+ endOfPeriod
				+ "'"
				+

				" UNION"
				+

				" SELECT *"
				+ " FROM "
				+ " ("
				+ " SELECT pp.patient_id"
				+ " FROM patient_program pp"
				+ " INNER JOIN program pr ON pr.program_id=pp.program_id AND pr.name LIKE 'ART'"
				+ " INNER JOIN person p ON p.person_id=pp.patient_id "
				+ " INNER JOIN drug_order_processed d ON d.patient_id=pp.patient_id"
				+ "  INNER JOIN obs o ON o.person_id=pp.patient_id"
				+ " AND o.concept_id LIKE '162945'"
				+ " AND o.value_text ='>95%'  AND o.voided=0"
				+ "  AND TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled))"
				+ ageCategory
				+ " WHERE DATE(date_enrolled) BETWEEN DATE_SUB("
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " , INTERVAL 1 MONTH) AND DATE_SUB("
				+ "'"
				+ endOfPeriod
				+ "'"
				+ ", INTERVAL 1 MONTH)"
				+ "  AND CASE WHEN date_completed IS NOT NULL THEN date_completed >"
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " ELSE 1=1 END"
				+ "  GROUP BY pp.patient_id"
				+ " )sag"
				+ " )adherence"
				+ "  LEFT JOIN "
				+

				"("
				+

				" SELECT p.person_id"
				+ " FROM person p"
				+ " INNER JOIN encounter e ON e.patient_id=p.person_id"
				+ " INNER JOIN encounter_type et ON et.encounter_type_id=e.encounter_type AND et.name LIKE 'ART'"
				+ " WHERE p.dead=1 "
				+ " AND death_date BETWEEN "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " AND "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " AND TIMESTAMPDIFF(YEAR,(p.birthdate),(p.death_date))"
				+ ageCategory
				+ " GROUP BY p.person_id"
				+ " UNION "
				+ " SELECT pp.patient_id"
				+ " FROM patient_program pp"
				+ " INNER JOIN program pr ON pr.program_id=pp.program_id AND pr.name LIKE 'ART'"
				+ " INNER JOIN person p ON p.person_id=pp.patient_id "
				+ " AND TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled))"
				+ ageCategory
				+ " WHERE pp.date_completed IS NOT NULL"
				+ " AND DATE(pp.date_completed) BETWEEN "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " AND "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " UNION "
				+ " SELECT pp.patient_id"
				+ " FROM patient_program pp"
				+ " INNER JOIN program pr ON pr.program_id=pp.program_id AND pr.name LIKE 'HIV'"
				+ " INNER JOIN person p ON p.person_id=pp.patient_id "
				+ " AND TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled))"
				+ ageCategory
				+ " WHERE pp.date_completed IS NOT NULL"
				+ " AND DATE(pp.date_completed) < "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " )adherence1"
				+ " ON adherence.patient_id=adherence1.person_id"
				+ " WHERE adherence1.person_id IS  NULL"
				+ " GROUP BY adherence.patient_id" + ")adherence2";
		return jdbcTemplate.queryForInt(query);
	}

	// 7.2.2
	public Integer getNoOfPatientsAssessedForAdherenceDuringTheLastMonthLevelTwoTot(
			String ageCategory, String startOfPeriod, String endOfPeriod) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String query = "SELECT COUNT(*) tot"
				+ " FROM"
				+ " ( SELECT adherence.patient_id"
				+ " FROM"
				+ " ("
				+ " SELECT pp.patient_id"
				+ " FROM patient_program pp"
				+ " INNER JOIN program pr ON pr.program_id=pp.program_id AND pr.name LIKE 'ART'"
				+ " INNER JOIN person p ON p.person_id=pp.patient_id "
				+ " INNER JOIN obs o ON o.person_id=pp.patient_id"
				+ " AND o.concept_id LIKE '162945'"
				+ " AND o.value_text ='80-95 %' AND o.voided=0"
				+ " AND TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled)) "
				+ ageCategory
				+ " WHERE DATE(date_enrolled) BETWEEN "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " AND "
				+ "'"
				+ endOfPeriod
				+ "'"
				+

				" UNION"
				+

				" SELECT *"
				+ " FROM "
				+ " ("
				+ " SELECT pp.patient_id"
				+ " FROM patient_program pp"
				+ " INNER JOIN program pr ON pr.program_id=pp.program_id AND pr.name LIKE 'ART'"
				+ " INNER JOIN person p ON p.person_id=pp.patient_id "
				+ " INNER JOIN drug_order_processed d ON d.patient_id=pp.patient_id"
				+ "  INNER JOIN obs o ON o.person_id=pp.patient_id"
				+ " AND o.concept_id LIKE '162945'"
				+ " AND o.value_text ='80-95 %'  AND o.voided=0"
				+ "  AND TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled))"
				+ ageCategory
				+ " WHERE DATE(date_enrolled) BETWEEN DATE_SUB("
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " , INTERVAL 1 MONTH) AND DATE_SUB("
				+ "'"
				+ endOfPeriod
				+ "'"
				+ ", INTERVAL 1 MONTH)"
				+ "  AND CASE WHEN date_completed IS NOT NULL THEN date_completed >"
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " ELSE 1=1 END"
				+ "  GROUP BY pp.patient_id"
				+ " )sag"
				+ " )adherence"
				+ "  LEFT JOIN "
				+

				"("
				+

				" SELECT p.person_id"
				+ " FROM person p"
				+ " INNER JOIN encounter e ON e.patient_id=p.person_id"
				+ " INNER JOIN encounter_type et ON et.encounter_type_id=e.encounter_type AND et.name LIKE 'ART'"
				+ " WHERE p.dead=1 "
				+ " AND death_date BETWEEN "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " AND "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " AND TIMESTAMPDIFF(YEAR,(p.birthdate),(p.death_date))"
				+ ageCategory
				+ " GROUP BY p.person_id"
				+ " UNION "
				+ " SELECT pp.patient_id"
				+ " FROM patient_program pp"
				+ " INNER JOIN program pr ON pr.program_id=pp.program_id AND pr.name LIKE 'ART'"
				+ " INNER JOIN person p ON p.person_id=pp.patient_id "
				+ " AND TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled))"
				+ ageCategory
				+ " WHERE pp.date_completed IS NOT NULL"
				+ " AND DATE(pp.date_completed) BETWEEN "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " AND "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " UNION "
				+ " SELECT pp.patient_id"
				+ " FROM patient_program pp"
				+ " INNER JOIN program pr ON pr.program_id=pp.program_id AND pr.name LIKE 'HIV'"
				+ " INNER JOIN person p ON p.person_id=pp.patient_id "
				+ " AND TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled))"
				+ ageCategory
				+ " WHERE pp.date_completed IS NOT NULL"
				+ " AND DATE(pp.date_completed) < "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " )adherence1"
				+ " ON adherence.patient_id=adherence1.person_id"
				+ " WHERE adherence1.person_id IS  NULL"
				+ " GROUP BY adherence.patient_id" + ")adherence2";
		return jdbcTemplate.queryForInt(query);
	}

	// 7.2.3
	public Integer getNoOfPatientsAssessedForAdherenceDuringTheLastMonthLevelThreeTot(
			String ageCategory, String startOfPeriod, String endOfPeriod) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String query = "SELECT COUNT(*) tot"
				+ " FROM"
				+ " ( SELECT adherence.patient_id"
				+ " FROM"
				+ " ("
				+ " SELECT pp.patient_id"
				+ " FROM patient_program pp"
				+ " INNER JOIN program pr ON pr.program_id=pp.program_id AND pr.name LIKE 'ART'"
				+ " INNER JOIN person p ON p.person_id=pp.patient_id "
				+ " INNER JOIN obs o ON o.person_id=pp.patient_id"
				+ " AND o.concept_id LIKE '162945'"
				+ " AND o.value_text ='<80%' AND o.voided=0"
				+ " AND TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled)) "
				+ ageCategory
				+ " WHERE DATE(date_enrolled) BETWEEN "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " AND "
				+ "'"
				+ endOfPeriod
				+ "'"
				+

				" UNION"
				+

				" SELECT *"
				+ " FROM "
				+ " ("
				+ " SELECT pp.patient_id"
				+ " FROM patient_program pp"
				+ " INNER JOIN program pr ON pr.program_id=pp.program_id AND pr.name LIKE 'ART'"
				+ " INNER JOIN person p ON p.person_id=pp.patient_id "
				+ " INNER JOIN drug_order_processed d ON d.patient_id=pp.patient_id"
				+ "  INNER JOIN obs o ON o.person_id=pp.patient_id"
				+ " AND o.concept_id LIKE '162945'"
				+ " AND o.value_text ='<80%'  AND o.voided=0"
				+ "  AND TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled))"
				+ ageCategory
				+ " WHERE DATE(date_enrolled) BETWEEN DATE_SUB("
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " , INTERVAL 1 MONTH) AND DATE_SUB("
				+ "'"
				+ endOfPeriod
				+ "'"
				+ ", INTERVAL 1 MONTH)"
				+ "  AND CASE WHEN date_completed IS NOT NULL THEN date_completed >"
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " ELSE 1=1 END"
				+ "  GROUP BY pp.patient_id"
				+ " )sag"
				+ " )adherence"
				+ "  LEFT JOIN "
				+

				"("
				+

				" SELECT p.person_id"
				+ " FROM person p"
				+ " INNER JOIN encounter e ON e.patient_id=p.person_id"
				+ " INNER JOIN encounter_type et ON et.encounter_type_id=e.encounter_type AND et.name LIKE 'ART'"
				+ " WHERE p.dead=1 "
				+ " AND death_date BETWEEN "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " AND "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " AND TIMESTAMPDIFF(YEAR,(p.birthdate),(p.death_date))"
				+ ageCategory
				+ " GROUP BY p.person_id"
				+ " UNION "
				+ " SELECT pp.patient_id"
				+ " FROM patient_program pp"
				+ " INNER JOIN program pr ON pr.program_id=pp.program_id AND pr.name LIKE 'ART'"
				+ " INNER JOIN person p ON p.person_id=pp.patient_id "
				+ " AND TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled))"
				+ ageCategory
				+ " WHERE pp.date_completed IS NOT NULL"
				+ " AND DATE(pp.date_completed) BETWEEN "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " AND "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " UNION "
				+ " SELECT pp.patient_id"
				+ " FROM patient_program pp"
				+ " INNER JOIN program pr ON pr.program_id=pp.program_id AND pr.name LIKE 'HIV'"
				+ " INNER JOIN person p ON p.person_id=pp.patient_id "
				+ " AND TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled))"
				+ ageCategory
				+ " WHERE pp.date_completed IS NOT NULL"
				+ " AND DATE(pp.date_completed) < "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " )adherence1"
				+ " ON adherence.patient_id=adherence1.person_id"
				+ " WHERE adherence1.person_id IS  NULL"
				+ " GROUP BY adherence.patient_id" + ")adherence2";
		return jdbcTemplate.queryForInt(query);
	}

	// 8.1
	public Integer getNoOfPatientsOnPerformanceScaleA(String gender,
			String ageCategory, String startOfPeriod, String endOfPeriod) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String query = "SELECT COUNT(*)"
				+ " FROM("
				+ " SELECT DISTINCT o.person_id"
				+ " FROM obs o "
				+ " INNER JOIN concept_name cn ON o.value_coded = cn.concept_id "
				+ " INNER JOIN person p ON p.person_id = o.person_id"
				+ " WHERE o.concept_id IN (SELECT concept_id "
				+ "  FROM concept " + "  WHERE concept_id IN('162886') "
				+ "  AND concept_name_type = 'FULLY_SPECIFIED') "
				+ " AND cn.concept_id IN ('162887') AND o.voided=0"
				+ " AND p.gender Like " + "'" + gender + "'"
				+ " AND TIMESTAMPDIFF(YEAR,(p.birthdate),(o.obs_datetime))"
				+ ageCategory + " AND p.birthdate IS NOT NULL"
				+ " AND DATE(obs_datetime) BETWEEN " + "'" + startOfPeriod
				+ "'" + " AND " + "'" + endOfPeriod + "'" + " )performanceA";
		return jdbcTemplate.queryForInt(query);
	}

	// 8.2
	public Integer getNoOfPatientsOnPerformanceScaleB(String gender,
			String ageCategory, String startOfPeriod, String endOfPeriod) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String query = "SELECT COUNT(*)"
				+ " FROM("
				+ " SELECT DISTINCT o.person_id"
				+ " FROM obs o "
				+ " INNER JOIN concept_name cn ON o.value_coded = cn.concept_id "
				+ " INNER JOIN person p ON p.person_id = o.person_id"
				+ " WHERE o.concept_id IN (SELECT concept_id "
				+ " FROM concept " + " WHERE concept_id IN('162886') "
				+ " AND concept_name_type = 'FULLY_SPECIFIED') "
				+ " AND cn.concept_id IN ('162888') AND o.voided=0"
				+ " AND p.gender Like " + "'" + gender + "'"
				+ " AND TIMESTAMPDIFF(YEAR,(p.birthdate),(o.obs_datetime))"
				+ ageCategory + " AND p.birthdate IS NOT NULL"
				+ " AND DATE(obs_datetime) BETWEEN " + "'" + startOfPeriod
				+ "'" + " AND " + "'" + endOfPeriod + "'" + " )performanceB";
		return jdbcTemplate.queryForInt(query);
	}

	// 8.3
	public Integer getNoOfPatientsOnPerformanceScaleC(String gender,
			String ageCategory, String startOfPeriod, String endOfPeriod) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String query = "SELECT COUNT(*)"
				+ " FROM("
				+ " SELECT DISTINCT o.person_id"
				+ " FROM obs o "
				+ " INNER JOIN concept_name cn ON o.value_coded = cn.concept_id "
				+ " INNER JOIN person p ON p.person_id = o.person_id"
				+ " WHERE o.concept_id IN (SELECT concept_id "
				+ "  FROM concept " + " WHERE concept_id IN('162886') "
				+ " AND concept_name_type = 'FULLY_SPECIFIED') "
				+ " AND cn.concept_id IN ('162889')  AND o.voided=0"
				+ " AND p.gender Like " + "'" + gender + "'"
				+ " AND TIMESTAMPDIFF(YEAR,(p.birthdate),(o.obs_datetime))"
				+ ageCategory + " AND p.birthdate IS NOT NULL"
				+ " AND DATE(obs_datetime) BETWEEN " + "'" + startOfPeriod
				+ "'" + " AND " + "'" + endOfPeriod + "'" + " )performanceC";
		return jdbcTemplate.queryForInt(query);
	}

	// 9.1
	public Integer getNoOfPatientSWithRiskFactorsCodeOne(String gender,
			String ageCategory, String startOfPeriod, String endOfPeriod) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String query = "SELECT COUNT(*)"
				+ " FROM("
				+ " SELECT DISTINCT o.person_id"
				+ " FROM obs o "
				+ " INNER JOIN concept_name cn ON o.value_coded = cn.concept_id "
				+ " INNER JOIN person p ON p.person_id = o.person_id"
				+ " WHERE o.concept_id IN (SELECT concept_id "
				+ " FROM concept " + " WHERE concept_id IN('160581') "
				+ " AND concept_name_type = 'FULLY_SPECIFIED') "
				+ " AND cn.concept_id IN ('162914')  AND o.voided=0"
				+ " AND p.gender Like " + "'" + gender + "'"
				+ " AND TIMESTAMPDIFF(YEAR,(p.birthdate),(o.obs_datetime))"
				+ ageCategory + " AND p.birthdate IS NOT NULL"
				+ " AND DATE(obs_datetime) BETWEEN " + "'" + startOfPeriod
				+ "'" + " AND " + "'" + endOfPeriod + "'" + " )risk1";
		return jdbcTemplate.queryForInt(query);
	}

	// 9.2
	public Integer getNoOfPatientSWithRiskFactorsCodeTwo(String gender,
			String ageCategory, String startOfPeriod, String endOfPeriod) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String query = "SELECT COUNT(*)"
				+ " FROM("
				+ " SELECT DISTINCT o.person_id"
				+ " FROM obs o "
				+ " INNER JOIN concept_name cn ON o.value_coded = cn.concept_id "
				+ " INNER JOIN person p ON p.person_id = o.person_id"
				+ " WHERE o.concept_id IN (SELECT concept_id "
				+ " FROM concept " + " WHERE concept_id IN('160581') "
				+ " AND concept_name_type = 'FULLY_SPECIFIED') "
				+ " AND cn.concept_id IN ('160578')  AND o.voided=0"
				+ " AND p.gender Like " + "'" + gender + "'"
				+ " AND TIMESTAMPDIFF(YEAR,(p.birthdate),(o.obs_datetime))"
				+ ageCategory + " AND p.birthdate IS NOT NULL"
				+ " AND DATE(obs_datetime) BETWEEN " + "'" + startOfPeriod
				+ "'" + " AND " + "'" + endOfPeriod + "'" + " )risk2";
		return jdbcTemplate.queryForInt(query);
	}

	// 9.3
	public Integer getNoOfPatientSWithRiskFactorsCodeThree(String gender,
			String ageCategory, String startOfPeriod, String endOfPeriod) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String query = "SELECT COUNT(*)"
				+ " FROM("
				+ " SELECT DISTINCT o.person_id"
				+ " FROM obs o "
				+ " INNER JOIN concept_name cn ON o.value_coded = cn.concept_id "
				+ " INNER JOIN person p ON p.person_id = o.person_id"
				+ " WHERE o.concept_id IN (SELECT concept_id "
				+ " FROM concept " + " WHERE concept_id IN('160581') "
				+ " AND concept_name_type = 'FULLY_SPECIFIED') "
				+ " AND cn.concept_id IN ('160579')  AND o.voided=0"
				+ " AND p.gender Like " + "'" + gender + "'"
				+ " AND TIMESTAMPDIFF(YEAR,(p.birthdate),(o.obs_datetime))"
				+ ageCategory + " AND p.birthdate IS NOT NULL"
				+ " AND DATE(obs_datetime) BETWEEN " + "'" + startOfPeriod
				+ "'" + " AND " + "'" + endOfPeriod + "'" + " )risk3";
		return jdbcTemplate.queryForInt(query);
	}

	// 9.4
	public Integer getNoOfPatientSWithRiskFactorsCodeFour(String gender,
			String ageCategory, String startOfPeriod, String endOfPeriod) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String query = "SELECT COUNT(*)"
				+ " FROM("
				+ " SELECT DISTINCT o.person_id"
				+ " FROM obs o "
				+ " INNER JOIN concept_name cn ON o.value_coded = cn.concept_id "
				+ " INNER JOIN person p ON p.person_id = o.person_id"
				+ " WHERE o.concept_id IN (SELECT concept_id "
				+ " FROM concept " + " WHERE concept_id IN('160581') "
				+ " AND concept_name_type = 'FULLY_SPECIFIED') "
				+ " AND cn.concept_id IN ('162915')  AND o.voided=0"
				+ " AND p.gender Like " + "'" + gender + "'"
				+ " AND TIMESTAMPDIFF(YEAR,(p.birthdate),(o.obs_datetime))"
				+ ageCategory + " AND p.birthdate IS NOT NULL"
				+ " AND DATE(obs_datetime) BETWEEN " + "'" + startOfPeriod
				+ "'" + " AND " + "'" + endOfPeriod + "'" + " )risk4";
		return jdbcTemplate.queryForInt(query);
	}

	// 9.5
	public Integer getNoOfPatientSWithRiskFactorsCodeFive(String gender,
			String ageCategory, String startOfPeriod, String endOfPeriod) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String query = "SELECT COUNT(*)"
				+ " FROM("
				+ " SELECT DISTINCT o.person_id"
				+ " FROM obs o "
				+ " INNER JOIN concept_name cn ON o.value_coded = cn.concept_id "
				+ " INNER JOIN person p ON p.person_id = o.person_id"
				+ " WHERE o.concept_id IN (SELECT concept_id "
				+ " FROM concept " + " WHERE concept_id IN('160581') "
				+ " AND concept_name_type = 'FULLY_SPECIFIED') "
				+ " AND cn.concept_id IN ('1063')  AND o.voided=0"
				+ " AND p.gender Like " + "'" + gender + "'"
				+ " AND TIMESTAMPDIFF(YEAR,(p.birthdate),(o.obs_datetime))"
				+ ageCategory + " AND p.birthdate IS NOT NULL"
				+ " AND DATE(obs_datetime) BETWEEN " + "'" + startOfPeriod
				+ "'" + " AND " + "'" + endOfPeriod + "'" + " )risk5";
		return jdbcTemplate.queryForInt(query);
	}

	// 9.6
	public Integer getNoOfPatientSWithRiskFactorsCodeSix(String gender,
			String ageCategory, String startOfPeriod, String endOfPeriod) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String query = "SELECT COUNT(*)"
				+ " FROM("
				+ " SELECT DISTINCT o.person_id"
				+ " FROM obs o "
				+ " INNER JOIN concept_name cn ON o.value_coded = cn.concept_id "
				+ " INNER JOIN person p ON p.person_id = o.person_id"
				+ " WHERE o.concept_id IN (SELECT concept_id "
				+ " FROM concept " + " WHERE concept_id IN('160581') "
				+ " AND concept_name_type = 'FULLY_SPECIFIED') "
				+ " AND cn.concept_id IN ('162916')  AND o.voided=0"
				+ " AND p.gender Like " + "'" + gender + "'"
				+ " AND TIMESTAMPDIFF(YEAR,(p.birthdate),(o.obs_datetime))"
				+ ageCategory + " AND p.birthdate IS NOT NULL"
				+ " AND DATE(obs_datetime) BETWEEN " + "'" + startOfPeriod
				+ "'" + " AND " + "'" + endOfPeriod + "'" + " )risk6";
		return jdbcTemplate.queryForInt(query);
	}

	// 9.7
	public Integer getNoOfPatientSWithRiskFactorsCodeSeven(String gender,
			String ageCategory, String startOfPeriod, String endOfPeriod) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String query = "SELECT COUNT(*)"
				+ " FROM("
				+ " SELECT DISTINCT o.person_id"
				+ " FROM obs o "
				+ " INNER JOIN concept_name cn ON o.value_coded = cn.concept_id "
				+ " INNER JOIN person p ON p.person_id = o.person_id"
				+ " WHERE o.concept_id IN (SELECT concept_id "
				+ " FROM concept " + " WHERE concept_id IN('160581') "
				+ " AND concept_name_type = 'FULLY_SPECIFIED') "
				+ " AND cn.concept_id IN ('1067')  AND o.voided=0"
				+ " AND p.gender Like " + "'" + gender + "'"
				+ " AND TIMESTAMPDIFF(YEAR,(p.birthdate),(o.obs_datetime))"
				+ ageCategory + " AND p.birthdate IS NOT NULL"
				+ " AND DATE(obs_datetime) BETWEEN " + "'" + startOfPeriod
				+ "'" + " AND " + "'" + endOfPeriod + "'" + " )risk7";
		return jdbcTemplate.queryForInt(query);
	}

	// 10.1
	public Integer getNoOfPatientsTestedForCD4Count(String gender,
			String ageCategory, String startOfPeriod, String endOfPeriod) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String query = "SELECT COUNT(*)"
				+ " FROM("
				+ " SELECT DISTINCT o.person_id"
				+ " FROM obs o "
				+ " INNER JOIN concept_name cn ON o.value_coded = cn.concept_id "
				+ " INNER JOIN person p ON p.person_id = o.person_id"
				+ "  WHERE o.concept_id IN (SELECT concept_id "
				+ " FROM concept " + " WHERE concept_id IN('1283') "
				+ " AND concept_name_type = 'FULLY_SPECIFIED') "
				+ " AND cn.concept_id IN ('5497') " + " AND p.gender Like "
				+ "'" + gender + "'"
				+ " AND TIMESTAMPDIFF(YEAR,(p.birthdate),(o.obs_datetime))"
				+ ageCategory + " AND p.birthdate IS NOT NULL"
				+ " AND DATE(obs_datetime) BETWEEN  " + "'" + startOfPeriod
				+ "'" + " AND " + "'" + endOfPeriod + "'" + " )cd";
		return jdbcTemplate.queryForInt(query);
	}

	// 10.2
	public Integer getNoOfPatientsTestedForViralLoad(String gender,
			String ageCategory, String startOfPeriod, String endOfPeriod) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String query = "SELECT COUNT(*)"
				+ " FROM("
				+ " SELECT DISTINCT o.person_id"
				+ " FROM obs o "
				+ " INNER JOIN concept_name cn ON o.value_coded = cn.concept_id "
				+ " INNER JOIN person p ON p.person_id = o.person_id"
				+ " WHERE o.concept_id IN (SELECT concept_id "
				+ " FROM concept " + " WHERE concept_id IN('1283') "
				+ " AND concept_name_type = 'FULLY_SPECIFIED') "
				+ "  AND cn.concept_id IN ('856') " + " AND p.gender Like "
				+ "'" + gender + "'"
				+ "  AND TIMESTAMPDIFF(YEAR,(p.birthdate),(o.obs_datetime))"
				+ ageCategory + " AND p.birthdate IS NOT NULL"
				+ " AND DATE(obs_datetime) BETWEEN  " + "'" + startOfPeriod
				+ "'" + " AND " + "'" + endOfPeriod + "'" + " )viral";
		return jdbcTemplate.queryForInt(query);
	}

	public Integer getNoOfPatientsHavingFirstLineRegimen(String ageCategory,
			String startOfPeriod, String endOfPeriod, String drugRegimen,
			String doseRegimen) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String query = "SELECT COUNT(*) tot "
				+ "FROM"
				+ "(SELECT drug1.patient_id "
				+ "FROM "
				+ "("
				+ "SELECT pp.patient_id "
				+ "FROM patient_program pp "
				+ "INNER JOIN program pr ON pr.program_id=pp.program_id AND pr.name LIKE 'ART' "
				+ "INNER JOIN person p ON p.person_id=pp.patient_id "
				+ "INNER JOIN drug_order_processed d ON d.patient_id=pp.patient_id "
				+ "AND drug_regimen LIKE " + "'"
				+ drugRegimen
				+ "'"
				+ " AND dose_regimen LIKE "
				+ "'"
				+ doseRegimen
				+ "'"
				+ " AND discontinued_date IS NULL "
				+ "AND type_of_regimen LIKE 'First line Anti-retoviral drugs'"
				+ "AND TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled))"
				+ ageCategory
				+ " WHERE DATE(date_enrolled) BETWEEN "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " GROUP BY pp.patient_id "
				+ "UNION "
				+ "SELECT * "
				+ "FROM "
				+ "("
				+ "SELECT pp.patient_id "
				+ "FROM patient_program pp "
				+ "INNER JOIN program pr ON pr.program_id=pp.program_id AND pr.name LIKE 'ART' "
				+ "INNER JOIN person p ON p.person_id=pp.patient_id "
				+ "INNER JOIN drug_order_processed d ON d.patient_id=pp.patient_id "
				+ "AND drug_regimen LIKE "
				+ "'"
				+ drugRegimen
				+ "'"
				+ " AND dose_regimen LIKE "
				+ "'"
				+ doseRegimen
				+ "'"
				+ " AND discontinued_date IS NULL "
				+ " AND type_of_regimen LIKE 'First line Anti-retoviral drugs' "
				+ "AND TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled)) "
				+ ageCategory
				+ " WHERE DATE(date_enrolled) BETWEEN DATE_SUB("
				+ "'"
				+ startOfPeriod
				+ "'"
				+ ", INTERVAL 1 MONTH) and DATE_SUB("
				+ "'"
				+ endOfPeriod
				+ "'"
				+ ", INTERVAL 1 MONTH) "
				+ "AND CASE WHEN date_completed IS NOT NULL THEN date_completed > "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ "ELSE 1=1 END "
				+ "GROUP BY pp.patient_id"
				+ ")sag"
				+ ")drug1 "
				+ "LEFT JOIN"
				+ "("
				+ "SELECT p.person_id "
				+ "FROM person p "
				+ "INNER JOIN encounter e ON e.patient_id=p.person_id "
				+ "INNER JOIN encounter_type et ON et.encounter_type_id=e.encounter_type AND et.name LIKE 'ART' "
				+ "WHERE p.dead=1 "
				+ "AND death_date BETWEEN "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " AND TIMESTAMPDIFF(YEAR,(p.birthdate),(p.death_date)) "
				+ "GROUP BY p.person_id "
				+ "UNION "
				+ "SELECT pp.patient_id "
				+ "FROM patient_program pp "
				+ "INNER JOIN program pr ON pr.program_id=pp.program_id AND pr.name LIKE 'HIV' "
				+ "INNER JOIN obs o ON o.person_id = pp.patient_id AND o.concept_id=161555 "
				+ "INNER JOIN person p ON p.person_id=pp.patient_id "
				+ "AND TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled)) "
				+ ageCategory
				+ " WHERE o.obs_datetime BETWEEN "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " AND o.obs_datetime BETWEEN pp.date_enrolled AND pp.date_completed "
				+ "AND o.value_coded=5240 "
				+ "UNION "
				+ "SELECT pp.patient_id "
				+ "FROM patient_program pp "
				+ "INNER JOIN program pr ON pr.program_id=pp.program_id AND pr.name LIKE 'ART' "
				+ "INNER JOIN person p ON p.person_id=pp.patient_id "
				+ "LEFT JOIN obs o ON o.person_id = pp.patient_id AND o.concept_id=161555 "
				+ "AND TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled)) "
				+ ageCategory
				+ " WHERE pp.date_completed IS NOT NULL "
				+ "AND DATE(pp.date_completed) BETWEEN "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " AND CASE WHEN o.concept_id IS NOT NULL THEN o.obs_datetime BETWEEN pp.date_enrolled AND pp.date_completed ELSE 1=1 END "
				+ " AND CASE WHEN o.concept_id IS NOT NULL THEN o.value_coded=159492 ELSE 1=1 END "
				+ ")drug2 "
				+ "ON drug1.patient_id=drug2.person_id "
				+ "WHERE drug2.person_id IS  NULL "
				+ " GROUP BY drug1.patient_id " + ")regime";
		return jdbcTemplate.queryForInt(query);
	}

	public Integer getNoOfPatientsHavingFirstLineRegimenFromFixedDose(
			String ageCategory, String startOfPeriod, String endOfPeriod,
			String drugRegimen, String doseRegimen) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String query = "SELECT COUNT(*) tot "
				+ "FROM"
				+ "(SELECT drug1.patient_id "
				+ "FROM "
				+ "("
				+ "SELECT pp.patient_id "
				+ "FROM patient_program pp "
				+ "INNER JOIN program pr ON pr.program_id=pp.program_id AND pr.name LIKE 'ART' "
				+ "INNER JOIN person p ON p.person_id=pp.patient_id "
				+ "INNER JOIN drug_order_processed d ON d.patient_id=pp.patient_id "
				+ "AND drug_regimen LIKE " + "'"
				+ drugRegimen
				+ "'"
				+ " AND dose_regimen LIKE "
				+ "'"
				+ doseRegimen
				+ "'"
				+ " AND discontinued_date IS NULL "
				+ "AND type_of_regimen LIKE 'Fixed dose combinations (FDCs)'"
				+ "AND TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled))"
				+ ageCategory
				+ " WHERE DATE(date_enrolled) BETWEEN "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " GROUP BY pp.patient_id "
				+ "UNION "
				+ "SELECT * "
				+ "FROM "
				+ "("
				+ "SELECT pp.patient_id "
				+ "FROM patient_program pp "
				+ "INNER JOIN program pr ON pr.program_id=pp.program_id AND pr.name LIKE 'ART' "
				+ "INNER JOIN person p ON p.person_id=pp.patient_id "
				+ "INNER JOIN drug_order_processed d ON d.patient_id=pp.patient_id "
				+ "AND drug_regimen LIKE "
				+ "'"
				+ drugRegimen
				+ "'"
				+ " AND dose_regimen LIKE "
				+ "'"
				+ doseRegimen
				+ "'"
				+ " AND discontinued_date IS NULL "
				+ " AND type_of_regimen LIKE 'Fixed dose combinations (FDCs)' "
				+ "AND TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled)) "
				+ ageCategory
				+ " WHERE DATE(date_enrolled) BETWEEN DATE_SUB("
				+ "'"
				+ startOfPeriod
				+ "'"
				+ ", INTERVAL 1 MONTH) and DATE_SUB("
				+ "'"
				+ endOfPeriod
				+ "'"
				+ ", INTERVAL 1 MONTH) "
				+ "AND CASE WHEN date_completed IS NOT NULL THEN date_completed > "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ "ELSE 1=1 END "
				+ "GROUP BY pp.patient_id"
				+ ")sag"
				+ ")drug1 "
				+ "LEFT JOIN"
				+ "("
				+ "SELECT p.person_id "
				+ "FROM person p "
				+ "INNER JOIN encounter e ON e.patient_id=p.person_id "
				+ "INNER JOIN encounter_type et ON et.encounter_type_id=e.encounter_type AND et.name LIKE 'ART' "
				+ "WHERE p.dead=1 "
				+ "AND death_date BETWEEN "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " AND TIMESTAMPDIFF(YEAR,(p.birthdate),(p.death_date)) "
				+ "GROUP BY p.person_id "
				+ "UNION "
				+ "SELECT pp.patient_id "
				+ "FROM patient_program pp "
				+ "INNER JOIN program pr ON pr.program_id=pp.program_id AND pr.name LIKE 'HIV' "
				+ "INNER JOIN obs o ON o.person_id = pp.patient_id AND o.concept_id=161555 "
				+ "INNER JOIN person p ON p.person_id=pp.patient_id "
				+ "AND TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled)) "
				+ ageCategory
				+ " WHERE o.obs_datetime BETWEEN "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " AND o.obs_datetime BETWEEN pp.date_enrolled AND pp.date_completed "
				+ "AND o.value_coded=5240 "
				+ "UNION "
				+ "SELECT pp.patient_id "
				+ "FROM patient_program pp "
				+ "INNER JOIN program pr ON pr.program_id=pp.program_id AND pr.name LIKE 'ART' "
				+ "INNER JOIN person p ON p.person_id=pp.patient_id "
				+ "LEFT JOIN obs o ON o.person_id = pp.patient_id AND o.concept_id=161555 "
				+ "AND TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled)) "
				+ ageCategory
				+ " WHERE pp.date_completed IS NOT NULL "
				+ "AND DATE(pp.date_completed) BETWEEN "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " AND CASE WHEN o.concept_id IS NOT NULL THEN o.obs_datetime BETWEEN pp.date_enrolled AND pp.date_completed ELSE 1=1 END "
				+ " AND CASE WHEN o.concept_id IS NOT NULL THEN o.value_coded=159492 ELSE 1=1 END "
				+ ")drug2 "
				+ "ON drug1.patient_id=drug2.person_id "
				+ "WHERE drug2.person_id IS  NULL "
				+ " GROUP BY drug1.patient_id " + ")regime";
		return jdbcTemplate.queryForInt(query);
	}

	public Integer getNoOfPatientsHavingFirstLineRegimenWithoutDose(
			String ageCategory, String startOfPeriod, String endOfPeriod,
			String drugRegimen) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String query = "SELECT COUNT(*) tot "
				+ "FROM"
				+ "(SELECT drug1.patient_id "
				+ "FROM "
				+ "("
				+ "SELECT pp.patient_id "
				+ "FROM patient_program pp "
				+ "INNER JOIN program pr ON pr.program_id=pp.program_id AND pr.name LIKE 'ART' "
				+ "INNER JOIN person p ON p.person_id=pp.patient_id "
				+ "INNER JOIN drug_order_processed d ON d.patient_id=pp.patient_id "
				+ "AND drug_regimen LIKE " + "'"
				+ drugRegimen
				+ "'"
				+ " AND discontinued_date IS NULL "
				+ "AND type_of_regimen LIKE 'First line Anti-retoviral drugs'"
				+ "AND TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled))"
				+ ageCategory
				+ " WHERE DATE(date_enrolled) BETWEEN "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " GROUP BY pp.patient_id "
				+ "UNION "
				+ "SELECT * "
				+ "FROM "
				+ "("
				+ "SELECT pp.patient_id "
				+ "FROM patient_program pp "
				+ "INNER JOIN program pr ON pr.program_id=pp.program_id AND pr.name LIKE 'ART' "
				+ "INNER JOIN person p ON p.person_id=pp.patient_id "
				+ "INNER JOIN drug_order_processed d ON d.patient_id=pp.patient_id "
				+ "AND drug_regimen LIKE "
				+ "'"
				+ drugRegimen
				+ "'"
				+ " AND discontinued_date IS NULL "
				+ " AND type_of_regimen LIKE 'First line Anti-retoviral drugs' "
				+ "AND TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled)) "
				+ ageCategory
				+ " WHERE DATE(date_enrolled) BETWEEN DATE_SUB("
				+ "'"
				+ startOfPeriod
				+ "'"
				+ ", INTERVAL 1 MONTH) and DATE_SUB("
				+ "'"
				+ endOfPeriod
				+ "'"
				+ ", INTERVAL 1 MONTH) "
				+ "AND CASE WHEN date_completed IS NOT NULL THEN date_completed > "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ "ELSE 1=1 END "
				+ "GROUP BY pp.patient_id"
				+ ")sag"
				+ ")drug1 "
				+ "LEFT JOIN"
				+ "("
				+ "SELECT p.person_id "
				+ "FROM person p "
				+ "INNER JOIN encounter e ON e.patient_id=p.person_id "
				+ "INNER JOIN encounter_type et ON et.encounter_type_id=e.encounter_type AND et.name LIKE 'ART' "
				+ "WHERE p.dead=1 "
				+ "AND death_date BETWEEN "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " AND TIMESTAMPDIFF(YEAR,(p.birthdate),(p.death_date)) "
				+ "GROUP BY p.person_id "
				+ "UNION "
				+ "SELECT pp.patient_id "
				+ "FROM patient_program pp "
				+ "INNER JOIN program pr ON pr.program_id=pp.program_id AND pr.name LIKE 'HIV' "
				+ "INNER JOIN obs o ON o.person_id = pp.patient_id AND o.concept_id=161555 "
				+ "INNER JOIN person p ON p.person_id=pp.patient_id "
				+ "AND TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled)) "
				+ ageCategory
				+ " WHERE o.obs_datetime BETWEEN "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " AND o.obs_datetime BETWEEN pp.date_enrolled AND pp.date_completed "
				+ "AND o.value_coded=5240 "
				+ "UNION "
				+ "SELECT pp.patient_id "
				+ "FROM patient_program pp "
				+ "INNER JOIN program pr ON pr.program_id=pp.program_id AND pr.name LIKE 'ART' "
				+ "INNER JOIN person p ON p.person_id=pp.patient_id "
				+ "LEFT JOIN obs o ON o.person_id = pp.patient_id AND o.concept_id=161555 "
				+ "AND TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled)) "
				+ ageCategory
				+ " WHERE pp.date_completed IS NOT NULL "
				+ "AND DATE(pp.date_completed) BETWEEN "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " AND CASE WHEN o.concept_id IS NOT NULL THEN o.obs_datetime BETWEEN pp.date_enrolled AND pp.date_completed ELSE 1=1 END "
				+ " AND CASE WHEN o.concept_id IS NOT NULL THEN o.value_coded=159492 ELSE 1=1 END "
				+ ")drug2 "
				+ "ON drug1.patient_id=drug2.person_id "
				+ "WHERE drug2.person_id IS  NULL "
				+ " GROUP BY drug1.patient_id " + ")regime";
		return jdbcTemplate.queryForInt(query);
	}

	public Integer getNoOfPatientsHavingSecondLineRegimen(String ageCategory,
			String startOfPeriod, String endOfPeriod, String drugRegimen,
			String doseRegimen) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String query = "SELECT COUNT(*) tot "
				+ "FROM "
				+ "(SELECT drug1.patient_id "
				+ "FROM "
				+ "("
				+ "SELECT pp.patient_id "
				+ "FROM patient_program pp "
				+ "INNER JOIN program pr ON pr.program_id=pp.program_id AND pr.name LIKE 'ART' "
				+ "INNER JOIN person p ON p.person_id=pp.patient_id "
				+ "INNER JOIN drug_order_processed d ON d.patient_id=pp.patient_id "
				+ "AND drug_regimen LIKE " + "'"
				+ drugRegimen
				+ "'"
				+ " AND dose_regimen LIKE "
				+ "'"
				+ doseRegimen
				+ "'"
				+ " AND discontinued_date IS NULL "
				+ " AND type_of_regimen LIKE 'Second line ART' "
				+ "AND TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled)) "
				+ ageCategory
				+ " WHERE DATE(date_enrolled) BETWEEN "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " GROUP BY pp.patient_id "
				+ "UNION "
				+ "SELECT * "
				+ "FROM "
				+ "("
				+ "SELECT pp.patient_id "
				+ "FROM patient_program pp "
				+ "INNER JOIN program pr ON pr.program_id=pp.program_id AND pr.name LIKE 'ART' "
				+ "INNER JOIN person p ON p.person_id=pp.patient_id "
				+ "INNER JOIN drug_order_processed d ON d.patient_id=pp.patient_id "
				+ "AND drug_regimen LIKE "
				+ "'"
				+ drugRegimen
				+ "'"
				+ " AND dose_regimen LIKE "
				+ "'"
				+ doseRegimen
				+ "'"
				+ " AND discontinued_date IS NULL "
				+ "AND type_of_regimen LIKE 'Second line ART' "
				+ "AND TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled)) "
				+ ageCategory
				+ " WHERE DATE(date_enrolled) BETWEEN DATE_SUB("
				+ "'"
				+ startOfPeriod
				+ "'"
				+ ", INTERVAL 1 MONTH) and DATE_SUB("
				+ "'"
				+ endOfPeriod
				+ "'"
				+ ", INTERVAL 1 MONTH) "
				+ "AND CASE WHEN date_completed IS NOT NULL THEN date_completed > "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ "ELSE 1=1 END "
				+ "GROUP BY pp.patient_id "
				+ ")sag"
				+ ")drug1 "
				+ "LEFT JOIN "
				+ "("
				+ "SELECT p.person_id "
				+ "FROM person p "
				+ "INNER JOIN encounter e ON e.patient_id=p.person_id "
				+ " INNER JOIN encounter_type et ON et.encounter_type_id=e.encounter_type AND et.name LIKE 'ART' "
				+ "WHERE p.dead=1 "
				+ "AND death_date BETWEEN "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ "AND TIMESTAMPDIFF(YEAR,(p.birthdate),(p.death_date)) "
				+ ageCategory
				+ " GROUP BY p.person_id "
				+ "UNION "
				+ "SELECT pp.patient_id "
				+ "FROM patient_program pp "
				+ "INNER JOIN program pr ON pr.program_id=pp.program_id AND pr.name LIKE 'HIV' "
				+ "INNER JOIN obs o ON o.person_id = pp.patient_id AND o.concept_id=161555 "
				+ "INNER JOIN person p ON p.person_id=pp.patient_id "
				+ "AND TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled)) "
				+ "WHERE o.obs_datetime BETWEEN "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " AND o.obs_datetime BETWEEN pp.date_enrolled AND pp.date_completed "
				+ "AND o.value_coded=5240 "
				+ "UNION "
				+ "SELECT pp.patient_id "
				+ "FROM patient_program pp "
				+ "INNER JOIN program pr ON pr.program_id=pp.program_id AND pr.name LIKE 'ART' "
				+ "INNER JOIN person p ON p.person_id=pp.patient_id "
				+ "LEFT JOIN obs o ON o.person_id = pp.patient_id AND o.concept_id=161555 "
				+ "AND TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled)) "
				+ ageCategory
				+ " WHERE pp.date_completed IS NOT NULL "
				+ "AND DATE(pp.date_completed) BETWEEN "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " AND CASE WHEN o.concept_id IS NOT NULL THEN o.obs_datetime BETWEEN pp.date_enrolled AND pp.date_completed ELSE 1=1 END "
				+ "AND CASE WHEN o.concept_id IS NOT NULL THEN o.value_coded=159492 ELSE 1=1 END "
				+ ")drug2"
				+ " ON drug1.patient_id=drug2.person_id "
				+ "WHERE drug2.person_id IS  NULL "
				+ "GROUP BY drug1.patient_id " + ")regime";
		return jdbcTemplate.queryForInt(query);
	}

	public Integer getNoOfPatientsHavingSecondLineRegimenFromFixedDose(
			String ageCategory, String startOfPeriod, String endOfPeriod,
			String drugRegimen, String doseRegimen) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String query = "SELECT COUNT(*) tot "
				+ "FROM "
				+ "(SELECT drug1.patient_id "
				+ "FROM "
				+ "("
				+ "SELECT pp.patient_id "
				+ "FROM patient_program pp "
				+ "INNER JOIN program pr ON pr.program_id=pp.program_id AND pr.name LIKE 'ART' "
				+ "INNER JOIN person p ON p.person_id=pp.patient_id "
				+ "INNER JOIN drug_order_processed d ON d.patient_id=pp.patient_id "
				+ "AND drug_regimen LIKE " + "'"
				+ drugRegimen
				+ "'"
				+ " AND dose_regimen LIKE "
				+ "'"
				+ doseRegimen
				+ "'"
				+ " AND discontinued_date IS NULL "
				+ " AND type_of_regimen LIKE 'Fixed dose combinations (FDCs)' "
				+ "AND TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled)) "
				+ ageCategory
				+ " WHERE DATE(date_enrolled) BETWEEN "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " GROUP BY pp.patient_id "
				+ "UNION "
				+ "SELECT * "
				+ "FROM "
				+ "("
				+ "SELECT pp.patient_id "
				+ "FROM patient_program pp "
				+ "INNER JOIN program pr ON pr.program_id=pp.program_id AND pr.name LIKE 'ART' "
				+ "INNER JOIN person p ON p.person_id=pp.patient_id "
				+ "INNER JOIN drug_order_processed d ON d.patient_id=pp.patient_id "
				+ "AND drug_regimen LIKE "
				+ "'"
				+ drugRegimen
				+ "'"
				+ " AND dose_regimen LIKE "
				+ "'"
				+ doseRegimen
				+ "'"
				+ " AND discontinued_date IS NULL "
				+ "AND type_of_regimen LIKE 'Fixed dose combinations (FDCs)' "
				+ "AND TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled)) "
				+ ageCategory
				+ " WHERE DATE(date_enrolled) BETWEEN DATE_SUB("
				+ "'"
				+ startOfPeriod
				+ "'"
				+ ", INTERVAL 1 MONTH) and DATE_SUB("
				+ "'"
				+ endOfPeriod
				+ "'"
				+ ", INTERVAL 1 MONTH) "
				+ "AND CASE WHEN date_completed IS NOT NULL THEN date_completed > "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ "ELSE 1=1 END "
				+ "GROUP BY pp.patient_id "
				+ ")sag"
				+ ")drug1 "
				+ "LEFT JOIN "
				+ "("
				+ "SELECT p.person_id "
				+ "FROM person p "
				+ "INNER JOIN encounter e ON e.patient_id=p.person_id "
				+ " INNER JOIN encounter_type et ON et.encounter_type_id=e.encounter_type AND et.name LIKE 'ART' "
				+ "WHERE p.dead=1 "
				+ "AND death_date BETWEEN "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ "AND TIMESTAMPDIFF(YEAR,(p.birthdate),(p.death_date)) "
				+ ageCategory
				+ " GROUP BY p.person_id "
				+ "UNION "
				+ "SELECT pp.patient_id "
				+ "FROM patient_program pp "
				+ "INNER JOIN program pr ON pr.program_id=pp.program_id AND pr.name LIKE 'HIV' "
				+ "INNER JOIN obs o ON o.person_id = pp.patient_id AND o.concept_id=161555 "
				+ "INNER JOIN person p ON p.person_id=pp.patient_id "
				+ "AND TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled)) "
				+ "WHERE o.obs_datetime BETWEEN "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " AND o.obs_datetime BETWEEN pp.date_enrolled AND pp.date_completed "
				+ "AND o.value_coded=5240 "
				+ "UNION "
				+ "SELECT pp.patient_id "
				+ "FROM patient_program pp "
				+ "INNER JOIN program pr ON pr.program_id=pp.program_id AND pr.name LIKE 'ART' "
				+ "INNER JOIN person p ON p.person_id=pp.patient_id "
				+ "LEFT JOIN obs o ON o.person_id = pp.patient_id AND o.concept_id=161555 "
				+ "AND TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled)) "
				+ ageCategory
				+ " WHERE pp.date_completed IS NOT NULL "
				+ "AND DATE(pp.date_completed) BETWEEN "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " AND CASE WHEN o.concept_id IS NOT NULL THEN o.obs_datetime BETWEEN pp.date_enrolled AND pp.date_completed ELSE 1=1 END "
				+ "AND CASE WHEN o.concept_id IS NOT NULL THEN o.value_coded=159492 ELSE 1=1 END "
				+ ")drug2"
				+ " ON drug1.patient_id=drug2.person_id "
				+ "WHERE drug2.person_id IS  NULL "
				+ "GROUP BY drug1.patient_id " + ")regime";
		return jdbcTemplate.queryForInt(query);
	}

	public Integer getNoOfPatientsHavingSecondLineRegimenWithoutDose(
			String ageCategory, String startOfPeriod, String endOfPeriod,
			String drugRegimen) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String query = "SELECT COUNT(*) tot "
				+ "FROM "
				+ "(SELECT drug1.patient_id "
				+ "FROM "
				+ "("
				+ "SELECT pp.patient_id "
				+ "FROM patient_program pp "
				+ "INNER JOIN program pr ON pr.program_id=pp.program_id AND pr.name LIKE 'ART' "
				+ "INNER JOIN person p ON p.person_id=pp.patient_id "
				+ "INNER JOIN drug_order_processed d ON d.patient_id=pp.patient_id "
				+ "AND drug_regimen LIKE " + "'"
				+ drugRegimen
				+ "'"
				+ " AND discontinued_date IS NULL "
				+ " AND type_of_regimen LIKE 'Second line ART' "
				+ "AND TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled)) "
				+ ageCategory
				+ " WHERE DATE(date_enrolled) BETWEEN "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " GROUP BY pp.patient_id "
				+ "UNION "
				+ "SELECT * "
				+ "FROM "
				+ "("
				+ "SELECT pp.patient_id "
				+ "FROM patient_program pp "
				+ "INNER JOIN program pr ON pr.program_id=pp.program_id AND pr.name LIKE 'ART' "
				+ "INNER JOIN person p ON p.person_id=pp.patient_id "
				+ "INNER JOIN drug_order_processed d ON d.patient_id=pp.patient_id "
				+ "AND drug_regimen LIKE "
				+ "'"
				+ drugRegimen
				+ "'"
				+ " AND discontinued_date IS NULL "
				+ "AND type_of_regimen LIKE 'Second line ART' "
				+ "AND TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled)) "
				+ ageCategory
				+ " WHERE DATE(date_enrolled) BETWEEN DATE_SUB("
				+ "'"
				+ startOfPeriod
				+ "'"
				+ ", INTERVAL 1 MONTH) and DATE_SUB("
				+ "'"
				+ endOfPeriod
				+ "'"
				+ ", INTERVAL 1 MONTH) "
				+ " AND CASE WHEN date_completed IS NOT NULL THEN date_completed > "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ "ELSE 1=1 END "
				+ "GROUP BY pp.patient_id "
				+ ")sag"
				+ ")drug1 "
				+ "LEFT JOIN "
				+ "("
				+ "SELECT p.person_id "
				+ "FROM person p "
				+ "INNER JOIN encounter e ON e.patient_id=p.person_id "
				+ " INNER JOIN encounter_type et ON et.encounter_type_id=e.encounter_type AND et.name LIKE 'ART' "
				+ "WHERE p.dead=1 "
				+ "AND death_date BETWEEN "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ "AND TIMESTAMPDIFF(YEAR,(p.birthdate),(p.death_date)) "
				+ ageCategory
				+ " GROUP BY p.person_id "
				+ "UNION "
				+ "SELECT pp.patient_id "
				+ "FROM patient_program pp "
				+ "INNER JOIN program pr ON pr.program_id=pp.program_id AND pr.name LIKE 'HIV' "
				+ "INNER JOIN obs o ON o.person_id = pp.patient_id AND o.concept_id=161555 "
				+ "INNER JOIN person p ON p.person_id=pp.patient_id "
				+ "AND TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled)) "
				+ "WHERE o.obs_datetime BETWEEN "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " AND o.obs_datetime BETWEEN pp.date_enrolled AND pp.date_completed "
				+ "AND o.value_coded=5240 "
				+ "UNION "
				+ "SELECT pp.patient_id "
				+ "FROM patient_program pp "
				+ "INNER JOIN program pr ON pr.program_id=pp.program_id AND pr.name LIKE 'ART' "
				+ "INNER JOIN person p ON p.person_id=pp.patient_id "
				+ "LEFT JOIN obs o ON o.person_id = pp.patient_id AND o.concept_id=161555 "
				+ "AND TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled)) "
				+ ageCategory
				+ " WHERE pp.date_completed IS NOT NULL "
				+ "AND DATE(pp.date_completed) BETWEEN "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " AND CASE WHEN o.concept_id IS NOT NULL THEN o.obs_datetime BETWEEN pp.date_enrolled AND pp.date_completed ELSE 1=1 END "
				+ "AND CASE WHEN o.concept_id IS NOT NULL THEN o.value_coded=159492 ELSE 1=1 END "
				+ ")drug2"
				+ " ON drug1.patient_id=drug2.person_id "
				+ "WHERE drug2.person_id IS  NULL "
				+ "GROUP BY drug1.patient_id " + ")regime";
		return jdbcTemplate.queryForInt(query);
	}

	public Integer getNoOfPatientsHavingThirdLineRegimen(String ageCategory,
			String startOfPeriod, String endOfPeriod, String drugRegimen,
			String doseRegimen) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String query = "SELECT COUNT(*) tot "
				+ "FROM "
				+ "(SELECT drug1.patient_id "
				+ "FROM"
				+ "("
				+ "SELECT pp.patient_id "
				+ "FROM patient_program pp "
				+ "INNER JOIN program pr ON pr.program_id=pp.program_id AND pr.name LIKE 'ART' "
				+ "INNER JOIN person p ON p.person_id=pp.patient_id "
				+ "INNER JOIN drug_order_processed d ON d.patient_id=pp.patient_id "
				+ "AND drug_regimen LIKE " + "'"
				+ drugRegimen
				+ "'"
				+ " AND dose_regimen LIKE "
				+ "'"
				+ doseRegimen
				+ "'"
				+ "AND TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled)) "
				+ ageCategory
				+ " WHERE DATE(date_enrolled) BETWEEN "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " GROUP BY pp.patient_id "
				+ "UNION "
				+ "SELECT * "
				+ "FROM "
				+ "("
				+ "SELECT pp.patient_id "
				+ "FROM patient_program pp "
				+ "INNER JOIN program pr ON pr.program_id=pp.program_id AND pr.name LIKE 'ART' "
				+ "INNER JOIN person p ON p.person_id=pp.patient_id "
				+ "INNER JOIN drug_order_processed d ON d.patient_id=pp.patient_id "
				+ "AND drug_regimen LIKE "
				+ "'"
				+ drugRegimen
				+ "'"
				+ " AND dose_regimen LIKE "
				+ "'"
				+ doseRegimen
				+ "'"
				+ "AND TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled)) "
				+ ageCategory
				+ " WHERE DATE(date_enrolled) BETWEEN DATE_SUB("
				+ "'"
				+ startOfPeriod
				+ "'"
				+ ", INTERVAL 1 MONTH) and DATE_SUB("
				+ "'"
				+ endOfPeriod
				+ "'"
				+ ", INTERVAL 1 MONTH) "
				+ "AND CASE WHEN date_completed IS NOT NULL THEN date_completed >"
				+ "'"
				+ endOfPeriod
				+ "'"
				+ "ELSE 1=1 END "
				+ "GROUP BY pp.patient_id "
				+ ")sag"
				+ ")drug1 "
				+ "LEFT JOIN "
				+ "("
				+ "SELECT p.person_id "
				+ "FROM person p "
				+ "INNER JOIN encounter e ON e.patient_id=p.person_id "
				+ "INNER JOIN encounter_type et ON et.encounter_type_id=e.encounter_type AND et.name LIKE 'ART' "
				+ "WHERE p.dead=1 "
				+ " AND death_date BETWEEN "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " AND TIMESTAMPDIFF(YEAR,(p.birthdate),(p.death_date))>"
				+ ageCategory
				+ " GROUP BY p.person_id "
				+ ")drug2 "
				+ "ON drug1.patient_id=drug2.person_id "
				+ "WHERE drug2.person_id IS  NULL "
				+ "GROUP BY drug1.patient_id " + ")regime";
		return jdbcTemplate.queryForInt(query);
	}

	public Integer getNoOfChildPatientsHavingRegimen(String ageCategory,
			String startOfPeriod, String endOfPeriod, String drugRegimen,
			String doseRegimen) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String query = "SELECT COUNT(*) tot "
				+ "FROM "
				+ "( SELECT drug1.patient_id "
				+ "FROM "
				+ "("
				+ "SELECT pp.patient_id "
				+ "FROM patient_program pp "
				+ "INNER JOIN program pr ON pr.program_id=pp.program_id AND pr.name LIKE 'ART' "
				+ "INNER JOIN person p ON p.person_id=pp.patient_id "
				+ "INNER JOIN drug_order_processed d ON d.patient_id=pp.patient_id "
				+ "AND drug_regimen LIKE " + "'"
				+ drugRegimen
				+ "'"
				+ " AND dose_regimen LIKE "
				+ "'"
				+ doseRegimen
				+ "'"
				+ " AND type_of_regimen LIKE 'Fixed dose combinations (FDCs)' "
				+ " AND discontinued_date IS NULL "
				+ " AND TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled))"
				+ ageCategory
				+ " WHERE DATE(date_enrolled) BETWEEN "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " GROUP BY pp.patient_id "
				+ "UNION "
				+ "SELECT * "
				+ "FROM "
				+ "("
				+ "SELECT pp.patient_id "
				+ "FROM patient_program pp "
				+ "INNER JOIN program pr ON pr.program_id=pp.program_id AND pr.name LIKE 'ART' "
				+ "INNER JOIN person p ON p.person_id=pp.patient_id "
				+ "INNER JOIN drug_order_processed d ON d.patient_id=pp.patient_id "
				+ "AND drug_regimen LIKE "
				+ "'"
				+ drugRegimen
				+ "'"
				+ " AND dose_regimen LIKE "
				+ "'"
				+ doseRegimen
				+ "'"
				+ " AND type_of_regimen LIKE 'Fixed dose combinations (FDCs)' "
				+ " AND discontinued_date IS NULL "
				+ " AND TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled))"
				+ ageCategory
				+ " WHERE DATE(date_enrolled) BETWEEN DATE_SUB("
				+ "'"
				+ startOfPeriod
				+ "'"
				+ ", INTERVAL 1 MONTH) and DATE_SUB("
				+ "'"
				+ endOfPeriod
				+ "'"
				+ ", INTERVAL 1 MONTH) "
				+ " AND CASE WHEN date_completed IS NOT NULL THEN date_completed >"
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " else 1=1 end "
				+ "GROUP BY pp.patient_id "
				+ ")sag"
				+ ")drug1 "
				+ "LEFT JOIN"
				+ "("
				+ "SELECT p.person_id "
				+ "FROM person p "
				+ "INNER JOIN encounter e ON e.patient_id=p.person_id "
				+ "INNER JOIN encounter_type et ON et.encounter_type_id=e.encounter_type AND et.name LIKE 'ART' "
				+ "WHERE p.dead=1 "
				+ "AND death_date BETWEEN "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " AND TIMESTAMPDIFF(YEAR,(p.birthdate),(p.death_date))"
				+ ageCategory
				+ " GROUP BY p.person_id "
				+ "UNION "
				+ "SELECT pp.patient_id "
				+ "FROM patient_program pp "
				+ "INNER JOIN program pr ON pr.program_id=pp.program_id AND pr.name LIKE 'HIV' "
				+ "INNER JOIN obs o ON o.person_id = pp.patient_id AND o.concept_id=161555 "
				+ "INNER JOIN person p ON p.person_id=pp.patient_id "
				+ " and TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled))"
				+ ageCategory
				+ " WHERE DATE(o.obs_datetime) BETWEEN "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " AND DATE(o.obs_datetime) BETWEEN pp.date_enrolled AND pp.date_completed "
				+ " AND o.value_coded=5240 "
				+ "UNION "
				+ "SELECT pp.patient_id "
				+ "FROM patient_program pp "
				+ "INNER JOIN program pr ON pr.program_id=pp.program_id AND pr.name LIKE 'ART' "
				+ "INNER JOIN person p ON p.person_id=pp.patient_id "
				+ "LEFT JOIN obs o ON o.person_id = pp.patient_id AND o.concept_id=161555 "
				+ "AND TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled))"
				+ ageCategory
				+ " WHERE pp.date_completed IS NOT NULL "
				+ "AND DATE(pp.date_completed) BETWEEN "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " AND CASE WHEN o.concept_id IS NOT NULL THEN DATE(o.obs_datetime) BETWEEN pp.date_enrolled AND pp.date_completed ELSE 1=1 END "
				+ "AND CASE WHEN o.concept_id IS NOT NULL THEN o.value_coded=159492 ELSE 1=1 END "
				+ ")drug2 "
				+ "ON drug1.patient_id=drug2.person_id "
				+ "WHERE drug2.person_id IS  NULL "
				+ "GROUP BY drug1.patient_id " + ")regime";
		return jdbcTemplate.queryForInt(query);
	}

	public Integer getNoOfChildPatientsHavingRegimenWithoutDose(
			String ageCategory, String startOfPeriod, String endOfPeriod,
			String drugRegimen) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String query = "SELECT COUNT(*) tot "
				+ "FROM"
				+ "(SELECT drug1.patient_id "
				+ "FROM "
				+ "("
				+ "SELECT pp.patient_id "
				+ "FROM patient_program pp "
				+ "INNER JOIN program pr ON pr.program_id=pp.program_id AND pr.name LIKE 'ART' "
				+ "INNER JOIN person p ON p.person_id=pp.patient_id "
				+ "INNER JOIN drug_order_processed d ON d.patient_id=pp.patient_id "
				+ "AND drug_regimen LIKE " + "'"
				+ drugRegimen
				+ "'"
				+ " AND type_of_regimen LIKE 'Fixed dose combinations (FDCs)' "
				+ " AND discontinued_date IS NULL "
				+ " AND TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled))"
				+ ageCategory
				+ " WHERE DATE(date_enrolled) BETWEEN "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " GROUP BY pp.patient_id "
				+ "UNION "
				+ "SELECT * "
				+ "FROM"
				+ "("
				+ "SELECT pp.patient_id "
				+ "FROM patient_program pp "
				+ "INNER JOIN program pr ON pr.program_id=pp.program_id AND pr.name LIKE 'ART' "
				+ "INNER JOIN person p ON p.person_id=pp.patient_id "
				+ "INNER JOIN drug_order_processed d ON d.patient_id=pp.patient_id "
				+ "AND drug_regimen LIKE "
				+ "'"
				+ drugRegimen
				+ "'"
				+ " AND type_of_regimen LIKE 'Fixed dose combinations (FDCs)' "
				+ " AND discontinued_date IS NULL "
				+ " AND TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled))"
				+ ageCategory
				+ " WHERE DATE(date_enrolled) BETWEEN DATE_SUB("
				+ "'"
				+ startOfPeriod
				+ "'"
				+ ", INTERVAL 1 MONTH) and DATE_SUB("
				+ "'"
				+ endOfPeriod
				+ "'"
				+ ", INTERVAL 1 MONTH) "
				+ " AND CASE WHEN date_completed IS NOT NULL THEN date_completed >"
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " else 1=1 end "
				+ "GROUP BY pp.patient_id "
				+ ")sag"
				+ ")drug1 "
				+ "LEFT JOIN"
				+ "("
				+ "SELECT p.person_id "
				+ "FROM person p "
				+ "INNER JOIN encounter e ON e.patient_id=p.person_id "
				+ "INNER JOIN encounter_type et ON et.encounter_type_id=e.encounter_type AND et.name LIKE 'ART' "
				+ "WHERE p.dead=1 "
				+ "AND death_date BETWEEN "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " AND TIMESTAMPDIFF(YEAR,(p.birthdate),(p.death_date))"
				+ ageCategory
				+ " GROUP BY p.person_id "
				+ "UNION "
				+ "SELECT pp.patient_id "
				+ "FROM patient_program pp "
				+ "INNER JOIN program pr ON pr.program_id=pp.program_id AND pr.name LIKE 'HIV' "
				+ "INNER JOIN obs o ON o.person_id = pp.patient_id AND o.concept_id=161555 "
				+ "INNER JOIN person p ON p.person_id=pp.patient_id "
				+ "and TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled))"
				+ ageCategory
				+ " WHERE o.obs_datetime BETWEEN "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " AND o.obs_datetime BETWEEN pp.date_enrolled AND pp.date_completed "
				+ "AND o.value_coded=5240 "
				+ "UNION "
				+ "SELECT pp.patient_id "
				+ "FROM patient_program pp "
				+ "INNER JOIN program pr ON pr.program_id=pp.program_id AND pr.name LIKE 'ART' "
				+ "INNER JOIN person p ON p.person_id=pp.patient_id "
				+ "LEFT JOIN obs o ON o.person_id = pp.patient_id AND o.concept_id=161555 "
				+ " AND TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled))"
				+ ageCategory
				+ " WHERE pp.date_completed IS NOT NULL "
				+ " AND DATE(pp.date_completed) BETWEEN "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " AND CASE WHEN o.concept_id IS NOT NULL THEN o.obs_datetime BETWEEN pp.date_enrolled AND pp.date_completed ELSE 1=1 END "
				+ "AND CASE WHEN o.concept_id IS NOT NULL THEN o.value_coded=159492 ELSE 1=1 END "
				+ ")drug2 "
				+ "ON drug1.patient_id=drug2.person_id "
				+ "WHERE drug2.person_id IS  NULL "
				+ "GROUP BY drug1.patient_id " + ")regime";
		return jdbcTemplate.queryForInt(query);
	}

	// stock dispensed
	public Integer getNoOfPatientsstockdispensed(String ageCategory,
			String startOfPeriod, String endOfPeriod, String drugRegimen,
			String doseRegimen) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String query = "SELECT COUNT(*) tot"
				+ " FROM"
				+ " ( SELECT drug1.patient_id"
				+ " FROM "
				+ " ( "
				+ " SELECT pp.patient_id"
				+ " FROM patient_program pp"
				+ " INNER JOIN program pr ON pr.program_id=pp.program_id AND pr.name LIKE 'ART'"
				+ " INNER JOIN person p ON p.person_id=pp.patient_id "
				+ " INNER JOIN drug_order_processed d ON d.patient_id=pp.patient_id"
				+ " AND drug_regimen LIKE " + "'"
				+ drugRegimen
				+ "'"
				+ " AND dose_regimen LIKE "
				+ "'"
				+ doseRegimen
				+ "'"
				+ " AND processed_status =1"
				+ " AND TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled))"
				+ ageCategory
				+ " WHERE DATE(date_enrolled) BETWEEN "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " GROUP BY pp.patient_id"
				+

				" UNION"
				+

				" SELECT *"
				+ " FROM "
				+ " ("
				+ " SELECT pp.patient_id"
				+ " FROM patient_program pp"
				+ " INNER JOIN program pr ON pr.program_id=pp.program_id AND pr.name LIKE 'ART'"
				+ " INNER JOIN person p ON p.person_id=pp.patient_id "
				+ " INNER JOIN drug_order_processed d ON d.patient_id=pp.patient_id"
				+ " AND drug_regimen LIKE "
				+ "'"
				+ drugRegimen
				+ "'"
				+ " AND dose_regimen LIKE "
				+ "'"
				+ doseRegimen
				+ "'"
				+ " AND processed_status =1"
				+ " AND TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled))"
				+ ageCategory
				+ " WHERE DATE(date_enrolled) BETWEEN DATE_SUB("
				+ "'"
				+ startOfPeriod
				+ "'"
				+ ", INTERVAL 1 MONTH) AND DATE_SUB("
				+ "'"
				+ endOfPeriod
				+ "'"
				+ ", INTERVAL 1 MONTH)"
				+ " AND CASE WHEN date_completed IS NOT NULL THEN date_completed >"
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " ELSE 1=1 END"
				+ " GROUP BY pp.patient_id"
				+ " )sag"
				+ " )drug1"
				+ " LEFT JOIN "
				+ " ("
				+

				" SELECT p.person_id"
				+ " FROM person p"
				+ " INNER JOIN encounter e ON e.patient_id=p.person_id"
				+ " INNER JOIN encounter_type et ON et.encounter_type_id=e.encounter_type AND et.name LIKE 'ART'"
				+ " WHERE p.dead=1 "
				+ " AND death_date BETWEEN "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " AND TIMESTAMPDIFF(YEAR,(p.birthdate),(p.death_date))"
				+ ageCategory
				+ " GROUP BY p.person_id"
				+ " )drug2"
				+ " ON drug1.patient_id=drug2.person_id"
				+ " WHERE drug2.person_id IS  NULL"
				+ " GROUP BY drug1.patient_id" + " )regime";
		return jdbcTemplate.queryForInt(query);
	}

	public Integer getNoOfPatientsstockdispensedWithoutDose(String ageCategory,
			String startOfPeriod, String endOfPeriod, String drugRegimen) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String query = "SELECT COUNT(*) tot"
				+ " FROM"
				+ " ( SELECT drug1.patient_id"
				+ " FROM"
				+ " ("
				+ " SELECT pp.patient_id"
				+ " FROM patient_program pp"
				+ " INNER JOIN program pr ON pr.program_id=pp.program_id AND pr.name LIKE 'ART'"
				+ " INNER JOIN person p ON p.person_id=pp.patient_id "
				+ " INNER JOIN drug_order_processed d ON d.patient_id=pp.patient_id"
				+ " AND drug_regimen LIKE " + "'"
				+ drugRegimen
				+ "'"
				+ " AND processed_status =1"
				+ " AND TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled))"
				+ ageCategory
				+ " WHERE DATE(date_enrolled) BETWEEN "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " GROUP BY pp.patient_id"
				+

				" UNION"
				+

				" SELECT *"
				+ " FROM "
				+ " ("
				+ " SELECT pp.patient_id"
				+ " FROM patient_program pp"
				+ " INNER JOIN program pr ON pr.program_id=pp.program_id AND pr.name LIKE 'ART'"
				+ " INNER JOIN person p ON p.person_id=pp.patient_id "
				+ " INNER JOIN drug_order_processed d ON d.patient_id=pp.patient_id"
				+ "  AND drug_regimen LIKE "
				+ "'"
				+ drugRegimen
				+ "'"
				+ "  AND processed_status =1"
				+ "  AND TIMESTAMPDIFF(YEAR,(p.birthdate),(date_enrolled))"
				+ ageCategory
				+ "  WHERE DATE(date_enrolled) BETWEEN DATE_SUB("
				+ "'"
				+ startOfPeriod
				+ "'"
				+ ", INTERVAL 1 MONTH) AND DATE_SUB("
				+ "'"
				+ endOfPeriod
				+ "'"
				+ ", INTERVAL 1 MONTH)"
				+ "  AND CASE WHEN date_completed IS NOT NULL THEN date_completed >"
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " ELSE 1=1 END"
				+ "  GROUP BY pp.patient_id"
				+ " )sag"
				+ " )drug1"
				+ " LEFT JOIN "
				+ " ("
				+

				" SELECT p.person_id"
				+ " FROM person p"
				+ " INNER JOIN encounter e ON e.patient_id=p.person_id"
				+ " INNER JOIN encounter_type et ON et.encounter_type_id=e.encounter_type AND et.name LIKE 'ART'"
				+ " WHERE p.dead=1 "
				+ " AND death_date BETWEEN "
				+ "'"
				+ startOfPeriod
				+ "'"
				+ " and "
				+ "'"
				+ endOfPeriod
				+ "'"
				+ " AND TIMESTAMPDIFF(YEAR,(p.birthdate),(p.death_date))"
				+ ageCategory
				+ " GROUP BY p.person_id"
				+ " )drug2"
				+ " ON drug1.patient_id=drug2.person_id"
				+ " WHERE drug2.person_id IS  NULL"
				+ " GROUP BY drug1.patient_id" + " )regime";
		return jdbcTemplate.queryForInt(query);
	}

}