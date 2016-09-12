package de.thingweb.repository;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URI;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;

import org.apache.jena.atlas.lib.StrUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.RDFDataMgr;

public class ThingDescriptionUtils {

	public static List<String> listThingDescriptions(String query) {
		List<String> tds = new ArrayList<>();
		Dataset dataset = Repository.get().dataset;
		dataset.begin(ReadWrite.READ);

		try {
			String q = "SELECT DISTINCT ?g WHERE { GRAPH ?g { " + query + " }}";
			try (QueryExecution qexec = QueryExecutionFactory.create(q, dataset)) {
				ResultSet result = qexec.execSelect();
				while (result.hasNext()) {
					QuerySolution td = result.next();
					if (td.get("g").toString().contains("td")) {
						tds.add(td.get("g").asResource().getURI());
					}
				}
			} catch (Exception e) {
				throw e;
			}
		} finally {
			dataset.end();
		}

		return tds;
	}

	public static List<String> listRuleApps(String query) {
		List<String> ruleApps = new ArrayList<>();
		Dataset dataset = Repository.get().dataset;
		dataset.begin(ReadWrite.READ);

		try {
			String q = "SELECT DISTINCT ?g WHERE { GRAPH ?g { " + query + " }}";
			try (QueryExecution qexec = QueryExecutionFactory.create(q, dataset)) {
				ResultSet result = qexec.execSelect();
				while (result.hasNext()) {
					QuerySolution ruleApp = result.next();
					if ((ruleApp.get("g").toString()).contains("ruleApp")) {
						ruleApps.add(ruleApp.get("g").asResource().getURI());
					}
				}
			} catch (Exception e) {
				throw e;
			}
		} finally {
			dataset.end();
		}

		return ruleApps;
	}

	public static String streamToString(InputStream s) throws IOException {
		StringWriter w = new StringWriter();
		InputStreamReader r = new InputStreamReader(s, "UTF-8");
		char[] buf = new char[512];
		int len;

		while ((len = r.read(buf)) > 0) {
			w.write(buf, 0, len);
		}
		s.close();

		return w.toString();
	}

	/**
	 * Returns the ID of a thing description stored in the database given its
	 * URI.
	 * 
	 * @param uri
	 *            URI of the thing description we want to return.
	 * @return the ID of the thing description.
	 */
	public static String getThingDescriptionIdFromUri(String uri) {

		String query = "?td <http://www.w3c.org/wot/td#associatedUri> <" + uri + ">";
		String id = "NOT FOUND";

		Dataset dataset = Repository.get().dataset;
		dataset.begin(ReadWrite.READ);

		try {
			String q = "SELECT ?g_id WHERE { GRAPH ?g_id { " + query + " }}";
			try (QueryExecution qexec = QueryExecutionFactory.create(q, dataset)) {
				ResultSet result = qexec.execSelect();
				while (result.hasNext()) {
					id = result.next().get("g_id").toString();
				}
			} catch (Exception e) {
				throw e;
			}
		} finally {
			dataset.end();
		}

		return id;
	}

	/**
	 * Returns a list of the thing descriptions URIs.
	 * 
	 * @return a list of URIs stored in the database.
	 */
	public static List<String> listThingDescriptionsUri() {

		List<String> tds = new ArrayList<>();
		String query = "?td <http://www.w3c.org/wot/td#associatedUri> ?uri";

		Dataset dataset = Repository.get().dataset;
		dataset.begin(ReadWrite.READ);

		try {
			String q = "SELECT ?uri WHERE { GRAPH ?g_id { " + query + " }}";
			try (QueryExecution qexec = QueryExecutionFactory.create(q, dataset)) {
				ResultSet result = qexec.execSelect();
				while (result.hasNext()) {
					tds.add(result.next().get("uri").toString());
				}
			} catch (Exception e) {
				throw e;
			}
		} finally {
			dataset.end();
		}

		return tds;
	}

	/**
	 * Returns a list of type values for the given property.
	 * 
	 * @param propertyURI
	 *            Complete URI of the property (baseUri + propertyName).
	 * @return List of values for the given property.
	 */
	public static String listRDFTypeValues(String propertyURI) {

		Model result = null;
		StringWriter out = new StringWriter();
		Dataset dataset = Repository.get().dataset;
		String prefix = StrUtils.strjoinNL("PREFIX td: <http://www.w3c.org/wot/td#>",
				"PREFIX qu: <http://purl.oclc.org/NET/ssnx/qu/qu#>",
				"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>",
				"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>", "PREFIX owl: <http://www.w3.org/2002/07/owl#>",
				"PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>",
				"PREFIX dim: <http://purl.oclc.org/NET/ssnx/qu/dim#>",
				"PREFIX quantity: <http://purl.oclc.org/NET/ssnx/qu/quantity#>");

		String queryProperty = prefix + "DESCRIBE ?interaction WHERE { " + "GRAPH ?g { "
				+ " ?td td:hasProperty ?interaction . " + "?interaction td:name ?name . " + "FILTER (?name = " + "\""
				+ propertyURI + "\"" + ")" + "}}";

		String queryEvent = prefix + "DESCRIBE ?interaction WHERE { " + "GRAPH ?g { "
				+ " ?td td:hasEvent ?interaction . " + "?interaction td:name ?name . " + "FILTER (?name = " + "\""
				+ propertyURI + "\"" + ")" + "}}";
		String queryAction = prefix + "DESCRIBE ?interaction WHERE { " + "GRAPH ?g { "
				+ " ?td td:hasAction ?interaction . " + "?interaction td:name ?name . " + "FILTER (?name = " + "\""
				+ propertyURI + "\"" + ")" + "}}";

		dataset.begin(ReadWrite.READ);
		System.out.println(queryProperty);
		try {

			if (((result = QueryExecutionFactory.create(queryProperty, dataset).execDescribe()).size() != 0)) {
				result.write(out, "Turtle");
			} else if (((result = QueryExecutionFactory.create(queryEvent, dataset).execDescribe()).size() != 0)) {

				result.write(out, "Turtle");
			} else if (((result = QueryExecutionFactory.create(queryAction, dataset).execDescribe()).size() != 0)) {

				result.write(out, "Turtle");
			}

		} finally {
			dataset.end();
		}

		return (out.toString());
	}

	/**
	 * Loads an ontology to the triple store, in the default graph.
	 * 
	 * @param fileName
	 *            File name with the ontology context.
	 */
	public static void loadOntology(String fileName) {

		List<String> ont = new ArrayList<>();

		// Check if the ontology is already there
		Dataset dataset = Repository.get().dataset;
		dataset.begin(ReadWrite.READ);
		try {
			String prefix = StrUtils.strjoinNL("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>",
					"PREFIX owl: <http://www.w3.org/2002/07/owl#>");
			String query = prefix + "SELECT ?s WHERE {?s rdf:type owl:Ontology}";

			try (QueryExecution qexec = QueryExecutionFactory.create(query, dataset)) {
				ResultSet result = qexec.execSelect();
				while (result.hasNext()) {
					ont.add(result.next().get("s").toString());
				}
			}

		} finally {
			dataset.end();
		}

		// Load QUDT ontology
		if (ont.isEmpty()) {
			dataset = Repository.get().dataset;
			dataset.begin(ReadWrite.WRITE);
			try {
				Model m = dataset.getDefaultModel();
				RDFDataMgr.read(m, fileName);
				dataset.commit();
			} finally {
				dataset.end();
			}
		}
	}

	// *********** FOR JENA-TEXT ************

	/**
	 * Extracts the key words from the model's statements. This includes the
	 * Literals and the localNames of Resources and Predicates.
	 * 
	 * @param model
	 *            Model to extract key words
	 * @return List of key word strings
	 */
	public List<String> getModelKeyWords(Model model) {

		List<String> keyWords = new ArrayList<String>();
		StmtIterator statementIter = model.listStatements();
		Statement s;
		Property predicate;
		RDFNode object;

		while (statementIter.hasNext()) {
			s = statementIter.nextStatement();
			predicate = s.getPredicate();
			object = s.getObject();

			keyWords.add(predicate.getLocalName());

			// local name of (non blank nodes) Resources
			if (object instanceof Resource && object.toString().contains("/")) {
				keyWords.add(object.asResource().getLocalName());

			} else if (object instanceof Literal) {
				// object is a Literal
				keyWords.add(object.toString());
			}
		}
		return keyWords;
	}

	/**
	 * Does a full text searc using jena-text. Returns the uris of the TDs that
	 * matched with the given key words.
	 * 
	 * @param keyWords
	 *            Words to find in a TD content.
	 * @return List of uris
	 */
	public static List<String> listThingDescriptionsFromTextSearch(String keyWords) {

		List<String> tds = new ArrayList<>();

		// Construct query
		String qMatch = "";
		String predicate, property;
		predicate = " text:query ";
		property = "rdfs:comment";
		qMatch += " ?s " + predicate + "(" + property + " " + keyWords + ") . ";

		String prefix = StrUtils.strjoinNL("PREFIX text: <http://jena.apache.org/text#>",
				"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>", "PREFIX td: <http://www.w3c.org/wot/td#>",
				"PREFIX qu: <http://purl.oclc.org/NET/ssnx/qu/qu#>",
				"PREFIX unit: <http://purl.oclc.org/NET/ssnx/qu/unit#>",
				"PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>");

		// Run the query
		Dataset dataset = Repository.get().dataset;
		dataset.begin(ReadWrite.READ);

		try {
			String query = "SELECT ?s WHERE { " + qMatch + " }";
			Query q = QueryFactory.create(prefix + "\n" + query);

			try {
				QueryExecution qexec = QueryExecutionFactory.create(q, dataset);
				ResultSet result = qexec.execSelect();
				while (result.hasNext()) {
					tds.add(result.next().get("s").asResource().getURI());
				}
			} catch (Exception e) {
				throw e;
			}
		} finally {
			dataset.end();
		}

		return tds;
	}

	// *********** FOR RESOURCE DIRECTORY ************

	/**
	 * Returns a list of registered end points
	 * 
	 * @return
	 */
	public static List<String> listEndpoints() {

		List<String> eps = new ArrayList<>();

		Dataset dataset = Repository.get().dataset;
		dataset.begin(ReadWrite.READ);

		String prefix = "PREFIX rdf-schema: <http://www.w3.org/2000/01/rdf-schema#>";

		try {
			String q = prefix + " SELECT ?endpoint WHERE { ?s rdf-schema:isDefinedBy ?endpoint . }";
			try (QueryExecution qexec = QueryExecutionFactory.create(q, dataset)) {
				ResultSet result = qexec.execSelect();
				while (result.hasNext()) {
					eps.add(result.next().get("endpoint").toString());
				}
			} catch (Exception e) {
				throw e;
			}
		} finally {
			dataset.end();
		}

		return eps;
	}

	/**
	 * Checks if lifetime is still valid (at least 10 seconds)
	 * 
	 * @param uri
	 *            Uri of the resource to check its lifetime
	 * @return true if time > 10 seconds, false otherwise
	 */
	public static Boolean checkLifeTime(URI uri) {

		List<String> dates = new ArrayList<String>();
		Boolean hasTime = true;

		Dataset dataset = Repository.get().dataset;
		dataset.begin(ReadWrite.READ);

		String prefix = "PREFIX purl: <http://purl.org/dc/terms/> ";
		String query = "SELECT ?modified ?lifetime WHERE { " + " <" + uri.toString() + "> purl:modified ?modified. "
				+ " <" + uri.toString() + "> purl:dateAccepted ?lifetime. }";

		String dateMod = "";
		String dateLife = "";

		try {

			try (QueryExecution qexec = QueryExecutionFactory.create(prefix + query, dataset)) {
				ResultSet result = qexec.execSelect();
				QuerySolution sol;
				while (result.hasNext()) {
					sol = result.next();
					dateMod = sol.get("modified").toString();
					dateLife = sol.get("lifetime").toString();
				}
			}

		} catch (Exception e) {
			throw e;
		} finally {
			dataset.end();
		}

		if (dateMod != null && !dateMod.isEmpty() && dateLife != null && !dateLife.isEmpty()) {
			DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
			Calendar modCal = Calendar.getInstance();
			Calendar lifeCal = Calendar.getInstance();
			long diff;
			try {
				lifeCal.setTime(dateFormat.parse(dateLife));
				diff = lifeCal.getTimeInMillis() - modCal.getTimeInMillis();
				// System.out.println("Remaining time " + Long.toString(diff));
				if (diff <= 0) {
					hasTime = false;
				}

			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

		return hasTime;
	}

	public String getCurrentDateTime(int plusTime) {

		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.SECOND, plusTime); // for the life time, else adds 0
											// sec
		return dateFormat.format(cal.getTime());
	}

	public static void printTDQueue() {

		Iterator<ThingDescription> iter = Repository.get().tdQueue.iterator();
		while (iter.hasNext()) {
			System.out.println(iter.next().getId());
		}
	}

	public static List<Entry<String, String>> listThingDescriptionsLifetime() {

		List<Entry<String, String>> tds = new ArrayList<>();
		Dataset dataset = Repository.get().dataset;

		String prefix = "PREFIX purl: <http://purl.org/dc/terms/> ";
		String query = prefix + "SELECT ?id ?lifetime WHERE {?id purl:dateAccepted ?lifetime.}";

		dataset.begin(ReadWrite.READ);
		try {
			try (QueryExecution qexec = QueryExecutionFactory.create(query, dataset)) {
				ResultSet result = qexec.execSelect();
				QuerySolution sol;
				while (result.hasNext()) {
					sol = result.next();
					tds.add(new SimpleEntry<String, String>(sol.get("id").toString(), sol.get("lifetime").toString()));
				}
			} catch (Exception e) {
				throw e;
			}

		} finally {
			dataset.end();
		}

		return tds;
	}

}