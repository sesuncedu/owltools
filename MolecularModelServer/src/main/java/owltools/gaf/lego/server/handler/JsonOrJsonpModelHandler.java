package owltools.gaf.lego.server.handler;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.glassfish.jersey.server.JSONP;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import owltools.gaf.lego.MolecularModelManager;
import owltools.gaf.lego.MolecularModelManager.OWLOperationResponse;
import owltools.graph.OWLGraphWrapper;

/**
 * Implementation of the {@link M3Handler}. Uses the build in function to render
 * generic object into JSON or JSONP.<br>
 * <br>
 * {@link Exception} are caught during the execution and reported in json
 * format. {@link Throwable} are not caught and will trigger a default error
 * page (and non standard status code for HTTP, i.e. 500).
 */
@Produces({MediaType.APPLICATION_JSON, "application/javascript"})
public class JsonOrJsonpModelHandler implements M3Handler {

	private static Logger LOG = Logger.getLogger(JsonOrJsonpModelHandler.class);

	static final String JSONP_DEFAULT_CALLBACK = "jsonp";
	public static final String JSONP_DEFAULT_OVERWRITE = "json.wrf";
	
	private final OWLGraphWrapper graph;
	private MolecularModelManager models = null;

	public JsonOrJsonpModelHandler(OWLGraphWrapper graph, MolecularModelManager models) {
		super();
		this.graph = graph;
		this.models = models;
	}

	private synchronized MolecularModelManager getMolecularModelManager() throws OWLOntologyCreationException {
		if (models == null) {
			LOG.info("Creating m3 object");
			models = new MolecularModelManager(graph);
		}
		return models;
	}

	/*
	 * Builder: {"id": <id>, "instances": [...]}
	 */
	@Override
	@JSONP(callback = JSONP_DEFAULT_CALLBACK, queryParam = JSONP_DEFAULT_OVERWRITE)
	public M3Response m3GetModel(String modelId, boolean help) {
		if (help) {
			return helpMsg("fetches molecular model json");
		}
		try {
			MolecularModelManager mmm = getMolecularModelManager();
			return bulk(modelId, mmm, M3Response.INSTANTIATE);
		} catch (Exception exception) {
			return errorMsg("Could not retrieve model", exception);
		}
	}

	/*
	 * Builder: {"id": <id>, "instances": [...]}
	 */
	@Override
	@JSONP(callback = JSONP_DEFAULT_CALLBACK, queryParam = JSONP_DEFAULT_OVERWRITE)
	public M3Response m3GenerateMolecularModel(String classId, String db, boolean help) {
		if (help) {
			return helpMsg("generates Minimal Model augmented with GO associations");
		}
		try {
			System.out.println("db: " + db);
			System.out.println("cls: " + classId);
			MolecularModelManager mmm = getMolecularModelManager();
			String mid = mmm.generateModel(classId, db);
			return bulk(mid, mmm, M3Response.INSTANTIATE);
		} catch (Exception exception) {
			return errorMsg("Could not generate model", exception);
		}
	}
	
	/*
	 * Builder: {"id": <id>, "instances": [...]}
	 */
	@Override
	@JSONP(callback = JSONP_DEFAULT_CALLBACK, queryParam = JSONP_DEFAULT_OVERWRITE)
	public M3Response m3GenerateBlankMolecularModel(String db, boolean help) {
		if (help) {
			return helpMsg("generates Minimal Model augmented with GO associations");
		}
		try {
			System.out.println("db: " + db);
			MolecularModelManager mmm = getMolecularModelManager();
			String mid = mmm.generateBlankModel(db);
			return bulk(mid, mmm, M3Response.INSTANTIATE);
		} catch (Exception exception) {
			return errorMsg("Could not generate model", exception);
		}
	}
	
	/*
	 * Info: {"message_type": "success", ..., "data: {"db": <db>}}
	 */
	@Override
	@JSONP(callback = JSONP_DEFAULT_CALLBACK, queryParam = JSONP_DEFAULT_OVERWRITE)
	public M3Response m3preloadGaf(String db, boolean help) {
		if (help) {
			return helpMsg("loads a GAF into memory (saves parsing time later on)");
		}
		try {
			MolecularModelManager mmm = getMolecularModelManager();
			mmm.loadGaf(db);
			return success(Collections.singletonMap("db", db), mmm);
		} catch (Exception exception) {
			return errorMsg("Could not preload gaf", exception);
		}
	}
	
	/*
	 * Individiuals: [...]
	 */
	@Override
	@JSONP(callback = JSONP_DEFAULT_CALLBACK, queryParam = JSONP_DEFAULT_OVERWRITE)
	public M3Response m3CreateIndividual(String modelId, String classId, boolean help) {
		if (help) {
			return helpMsg("generates a new individual");
		}
		try {
			MolecularModelManager mmm = getMolecularModelManager();
			OWLOperationResponse resp = mmm.createIndividual(modelId, classId);
			return response(resp, mmm, null);
		} catch (Exception exception) {
			return errorMsg("Could not create individual in model", exception);
		}
	}
	
	@Override
	@JSONP(callback = JSONP_DEFAULT_CALLBACK, queryParam = JSONP_DEFAULT_OVERWRITE)
	public M3Response m3DeleteIndividual(String modelId, String individualId, boolean help) {
		if (help) {
			return helpMsg("delete the given individual");
		}
		try {
			MolecularModelManager mmm = getMolecularModelManager();
			OWLOperationResponse resp = mmm.deleteIndividual(modelId, individualId);
			return bulk(modelId, mmm, M3Response.INCONSISTENT); // TODO for now return the whole thing
		} catch (Exception exception) {
			return errorMsg("Could not create individual in model", exception);
		}
	}
	
	/*
	 * Individiuals: [...]
	 */
	@Override
	@JSONP(callback = JSONP_DEFAULT_CALLBACK, queryParam = JSONP_DEFAULT_OVERWRITE)
	public M3Response m3AddType(String modelId, String individualId, String classId, boolean help) {
		if (help) {
			return helpMsg("generates ClassAssertion (named class)");
		}
		try {
			MolecularModelManager mmm = getMolecularModelManager();
			OWLOperationResponse resp = mmm.addType(modelId, individualId, classId);
			return response(resp, mmm, M3Response.MERGE);
		} catch (Exception exception) {
			return errorMsg("Could not add type to model", exception);
		}
	}
	
	/*
	 * Individiuals: [...]
	 */
	@Override
	@JSONP(callback = JSONP_DEFAULT_CALLBACK, queryParam = JSONP_DEFAULT_OVERWRITE)
	public M3Response m3AddTypeExpression(String modelId, String individualId, String propertyId,
					String classId, boolean help) {
		if (help) {
			return helpMsg("generates ClassAssertion (anon class expression)");
		}
		try {
			MolecularModelManager mmm = getMolecularModelManager();
			OWLOperationResponse resp = mmm.addType(modelId, individualId, propertyId, classId);
			return response(resp, mmm, M3Response.MERGE);
		} catch (Exception exception) {
			return errorMsg("Could not add type expression to model", exception);
		}
	}

	/*
	 * Individiuals: [...]
	 */
	@Override
	@JSONP(callback = JSONP_DEFAULT_CALLBACK, queryParam = JSONP_DEFAULT_OVERWRITE)
	public M3Response m3AddFact(String modelId, String propertyId, String individualId,
					String fillerId, boolean help) {
		if (help) {
			return helpMsg("generates ObjectPropertyAssertion");
		}
		try {
			//System.out.println("mod: " + modelId);
			//System.out.println("fil: " + fillerId);
			//System.out.println("ind: " + individualId);
			//System.out.println("rel: " + propertyId);
			MolecularModelManager mmm = getMolecularModelManager();
			OWLOperationResponse resp = mmm.addFact(modelId, propertyId, individualId, fillerId);
			M3Response response = response(resp, mmm, M3Response.MERGE);
			//printJsonResponse(response);
			return response;
		} catch (Exception exception) {
			return errorMsg("Could not add fact to model", exception);
		}
	}
	
	/*
	 * Individiuals: [...]
	 */
	@Override
	@JSONP(callback = JSONP_DEFAULT_CALLBACK, queryParam = JSONP_DEFAULT_OVERWRITE)
	public M3Response m3RemoveFact(String propertyId, String modelId, String individualId,
					String fillerId, boolean help) {
		if (help) {
			return helpMsg("generates ObjectPropertyAssertion");
		}
		try {
			System.out.println("mod: " + modelId);
			System.out.println("rel: " + propertyId);
			System.out.println("ind: " + individualId);
			System.out.println("fil: " + fillerId);
			MolecularModelManager mmm = getMolecularModelManager();
			OWLOperationResponse resp = mmm.removeFact(modelId, propertyId, individualId, fillerId);
			return response(resp, mmm, M3Response.MERGE);
		} catch (Exception exception) {
			return errorMsg("Could not remove fact from model", exception);
		}
	}

	/*
	 * Individiuals: [...]
	 */
	@Override
	@JSONP(callback = JSONP_DEFAULT_CALLBACK, queryParam = JSONP_DEFAULT_OVERWRITE)
	public M3Response m3CreateSimpleCompositeIndividual(String modelId, String classId, String enabledById, String occursInId, boolean help) {
		if (help) {
			return helpMsg("generates a new simple composite individual");
		}
		try {
			System.out.println("mod: " + modelId); // necessatry
			System.out.println("act: " + classId); // necessatry
			System.out.println("enb: " + enabledById); // optional
			System.out.println("occ: " + occursInId); // optional

			// Create base instance, along with any simples optionals that are along for the ride.
			MolecularModelManager mmm = getMolecularModelManager();
			OWLOperationResponse resp = mmm.addCompositeIndividual(modelId, classId,
																   StringUtils.stripToNull(enabledById),
																   StringUtils.stripToNull(occursInId));
			
			return response(resp, mmm, M3Response.MERGE);
		} catch (Exception exception) {
			return errorMsg("Could not create individual in model", exception);
		}
	}
	
	/*
	 * Other.
	 */
	@Override
	@JSONP(callback = JSONP_DEFAULT_CALLBACK, queryParam = JSONP_DEFAULT_OVERWRITE)
	public M3Response m3ExportModel(String modelId, boolean help) {
		if (help) {
			return helpMsg("Export the current content of the model");
		}
		try {
			MolecularModelManager mmm = getMolecularModelManager();
			String model = mmm.exportModel(modelId);
			return success(Collections.singletonMap("export", model), null);
		} catch (Exception exception) {
			return errorMsg("Could not export model", exception);
		}
	}
	
	/*
	 * @see owltools.gaf.lego.server.handler.M3Handler#m3ImportModel
	 */
	@Override
	@JSONP(callback = JSONP_DEFAULT_CALLBACK, queryParam = JSONP_DEFAULT_OVERWRITE)
	public M3Response m3ImportModel(String model, boolean help) {
		if (help) {
			return helpMsg("Import the model into the server.");
		}
		try {
			MolecularModelManager mmm = getMolecularModelManager();
			String modelId = mmm.importModel(model);
			return bulk(modelId, mmm, M3Response.INSTANTIATE);
		} catch (Exception exception) {
			return errorMsg("Could not import model", exception);
		}
	}

	/*
	 * Return all meta-infomation about models in a format that the client can pick apart to help build an interface.
	 * 
	 * @see owltools.gaf.lego.server.handler.M3Handler#m3GetAllModelIds(boolean)
	 */
	@Override
	@JSONP(callback = JSONP_DEFAULT_CALLBACK, queryParam = JSONP_DEFAULT_OVERWRITE)
	public M3Response m3GetAllModelIds(boolean help) {
		if (help) {
			return helpMsg("Get the current available model ids.");
		}
		try {
			// Get the different kinds of model IDs for the client.
			MolecularModelManager mmm = getMolecularModelManager();

			Set<String> allModelIds = mmm.getAvailableModelIds();
			//Set<String> scratchModelIds = mmm.getScratchModelIds();
			//Set<String> storedModelIds = mmm.getStoredModelIds();
			//Set<String> memoryModelIds = mmm.getCurrentModelIds();

			Map<String, Object> map = new HashMap<String, Object>();
			map.put("models_all", allModelIds);
			//map.put("models_memory", memoryModelIds);
			//map.put("models_stored", storedModelIds);
			//map.put("models_scratch", scratchModelIds);
			
			return information(map, mmm);
		} catch (Exception exception) {
			return errorMsg("Could not retrieve all available model ids", exception);
		}
	}
	
	/*
	 * @see owltools.gaf.lego.server.handler.M3Handler#m3SaveModel
	 */
	@Override
	@JSONP(callback = JSONP_DEFAULT_CALLBACK, queryParam = JSONP_DEFAULT_OVERWRITE)
	public M3Response m3StoreModel(String modelId, boolean help) {
		if (help) {
			return helpMsg("Persist the given model on the server.");
		}
		try {
			MolecularModelManager mmm = getMolecularModelManager();
			mmm.saveModel(modelId);
			M3Response response = new M3Response(M3Response.SUCCESS);
			response.message = "The model has been saved on the server.";
			return response;
		} catch (Exception exception) {
			return errorMsg("Could not save the model on the server.", exception);
		}
	}
	
	// ----------------------------------------
	// END OF COMMANDS
	// ----------------------------------------

	// ----------------------------------------
	// Reworked calls for batch operations 
	// ----------------------------------------

	static class Request {
		String entity;
		String operation;
		Argument arguments;
	}
	
	static class Argument {
		String modelId;
		String subject;
		String object;
		String predicate;
		String individual;
		
		Expression[] expressions;
		Pair[] values;
	}
	
	static class Pair {
		String key;
		String value;
	}
	
	static class Expression {
		String type;
		String onProp;
		String literal;
//		Expression expression;
	}
	
	static class BatchResponse {
		
	}
	
	public BatchResponse call(Request[] requests) {
		return null;
	}

	// ----------------------------------------
	// END OF COMMANDS
	// ----------------------------------------

	// UTIL
	private M3Response helpMsg(String msg) {
		M3Response response = new M3Response(M3Response.SUCCESS);
		response.message = msg;
		return response;
	}
	
	private M3Response warningMsg(String msg) {
		M3Response response = new M3Response(M3Response.WARNING);
		response.message = msg;
 		return response;
	}
	
	private M3Response errorMsg(String msg, Exception e) {
		M3Response response = new M3Response(M3Response.ERROR);
		response.message = msg;
		if (e != null) {
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("exception", e.getClass().getName());
			map.put("exceptionMsg", e.getMessage());
			StringWriter stacktrace = new StringWriter();
			e.printStackTrace(new PrintWriter(stacktrace));
			map.put("exceptionTrace", stacktrace.toString());
			response.commentary = map;
		}
		return response;
	}
	
	/**
	 * @param data
	 * @param mmm
	 * @return REST response, never null
	 */
	private M3Response success(Object data, MolecularModelManager mmm) {
		M3Response response = new M3Response(M3Response.SUCCESS);
		response.data = data;
		if (mmm != null) {
			// TODO add consistent m3 model to result ?
		}
		return response;
	}
	
	/**
	 * @param data
	 * @param mmm
	 * @return REST response, never null
	 */
	private M3Response information(Object data, MolecularModelManager mmm) {
		M3Response response = new M3Response(M3Response.INFORMATION);
		response.data = data;
		if (mmm != null) {
			// TODO add consistent m3 model to result ?
		}
		return response;
	}
	
	/**
	 * @param modelId
	 * @param mmm 
	 * @param respCat
	 * @return REST response, never null
	 */
	private M3Response bulk(String modelId, MolecularModelManager mmm, String respCat) {
		M3Response response = new M3Response(respCat);
		if (mmm != null) {
			// TODO add consistent m3 model to result ?
			Map<Object, Object> obj = mmm.getModelObject(modelId);
			obj.put("id", modelId);
			response.data = obj;
		}
		return response;
	}
	
	/**
	 * @param resp
	 * @param mmm
	 * @param intention
	 * @return REST response, never null
	 */
	private M3Response response(OWLOperationResponse resp, MolecularModelManager mmm, String intention) {
		M3Response response;
		if (resp.isResultsInInconsistency()) {
			response = new M3Response(M3Response.INCONSISTENT);
			response.message = "unintentional inconsistency";
		}
		else if ( ! resp.isSuccess()) {
			response = new M3Response(M3Response.ERROR);
		}
		else {
			//response = new M3Response(M3Response.SUCCESS);
			response = new M3Response(intention);
		}
		if (resp.getModelData() != null) {
			response.data = resp.getModelData();
		}
		return response;
	}
	
	/**
	 * Function for debugging: print JSON representation of a response to System.out.
	 * 
	 * @param response
	 */
	static void printJsonResponse(M3Response response) {
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.setPrettyPrinting();
		Gson gson = gsonBuilder.create();
		String json = gson.toJson(response);
		System.out.println("json: " + json);
	}
}
