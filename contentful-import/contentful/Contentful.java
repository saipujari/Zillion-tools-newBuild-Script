package contentful;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class Contentful {
	public Contentful(String spaceid, String oauthtoken) {
		this.spaceid = spaceid;
		this.oauthtoken = oauthtoken;
		this.client = ClientBuilder.newClient();
	}

	public String uploadAsset(String title, String filename, String url) throws JSONException {
		JSONObject titleObj = new JSONObject();
		titleObj.put("en-US", title);

		JSONObject localizedFileObj = new JSONObject();
		localizedFileObj.put("contentType", "image/jpeg");
		localizedFileObj.put("fileName", filename);
		localizedFileObj.put("upload", url);
		// localizedFileObj.put("upload", image.getString("imageData"));

		JSONObject fileObj = new JSONObject().put("en-US", localizedFileObj);

		JSONObject fields = new JSONObject().put("title", titleObj).put("file", fileObj);
		
		JSONObject outJSON = new JSONObject().put("fields", fields);

		Entity<String> payload = Entity.text(outJSON.toString());

		Response response = client.target("https://api.contentful.com/spaces/" + spaceid + "/assets")
			  .request(MediaType.TEXT_PLAIN_TYPE)
			  .header("Authorization", "Bearer " + oauthtoken)
			  .post(payload);

		String responseString = response.readEntity(String.class);
		JSONObject responseBody = new JSONObject(new JSONTokener(responseString));

		return responseBody.getJSONObject("sys").getString("id");
	}

	public void processAsset(String assetId) {
		Response response = client.target("https://api.contentful.com/spaces/" + spaceid + "/assets/" + assetId + "/files/en-US/process")
			  .request(MediaType.TEXT_PLAIN_TYPE)
			  .header("Authorization", "Bearer " + oauthtoken)
			  .put(Entity.text(""));

		String responseString = response.readEntity(String.class);
		System.out.println("processAsset response: " + responseString);
	}

	public void publishAsset(String assetId) {
		int versionNumber = 2;
		try {
			JSONObject getAssetObject = getAsset(assetId);
			versionNumber = extractVersionNumber(getAssetObject);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		Response response = client.target("https://api.contentful.com/spaces/" + spaceid + "/assets/" + assetId + "/published")
			  .request(MediaType.TEXT_PLAIN_TYPE)
			  .header("Authorization", "Bearer " + oauthtoken)
			  .header("X-Contentful-Version", versionNumber)
			  .put(Entity.text(""));

		String responseString = response.readEntity(String.class);
		System.out.println("publishAsset response: " + responseString);
	}

	public JSONObject getAsset(String assetId) throws JSONException {
		Response response = client.target("https://api.contentful.com/spaces/" + spaceid + "/assets/" + assetId)
			  .request(MediaType.TEXT_PLAIN_TYPE)
			  .header("Authorization", "Bearer " + oauthtoken)
			  .get();

		String responseString = response.readEntity(String.class);
		JSONObject responseBody = new JSONObject(new JSONTokener(responseString));

		return responseBody;
	}

	public String addEntry(String jsonPayload, String entryTypeId) throws JSONException {
		System.out.println("ADD");

		Entity<String> payload = Entity.text(jsonPayload);
		Response response = client.target("https://api.contentful.com/spaces/" + spaceid + "/entries")
		  .request(MediaType.TEXT_PLAIN_TYPE)
		  .header("Authorization", "Bearer " + oauthtoken)
		  .header("X-Contentful-Content-Type", entryTypeId)
		  .post(payload);

		String responseString = response.readEntity(String.class);
		JSONObject responseBody = new JSONObject(new JSONTokener(responseString));

		return responseBody.getJSONObject("sys").getString("id");
	}

	public void updateEntry(String jsonPayload, String entryId, int versionNumber) {
		System.out.println("UPDATE");

		Entity<String> payload = Entity.text(jsonPayload);
		Response response = client.target("https://api.contentful.com/spaces/" + spaceid + "/entries/" + entryId)
		  .request(MediaType.TEXT_PLAIN_TYPE)
		  .header("Authorization", "Bearer " + oauthtoken)
		  .header("X-Contentful-Version", versionNumber)
		  .put(payload);

		String responseString = response.readEntity(String.class);
		System.out.println("updateEntry response: " + responseString);
	}

	public JSONObject getEntry(String entryId) throws JSONException {
		System.out.println("GET");

		Response response = client.target("https://api.contentful.com/spaces/" + spaceid + "/entries/" + entryId)
			  .request(MediaType.TEXT_PLAIN_TYPE)
			  .header("Authorization", "Bearer " + oauthtoken)
			  .get();

		String responseString = response.readEntity(String.class);
		JSONObject responseBody = new JSONObject(new JSONTokener(responseString));

		return responseBody;
	}

	//oz: added publishEntry method
	public void publishEntry(String entryId) throws JSONException {
		System.out.println("PUBLISH");

		JSONObject getEntryObject = getEntry(entryId);
		int versionNumber = extractVersionNumber(getEntryObject);

		Entity<String> payload = Entity.text(getEntryObject.toString());

		Response response = client.target("https://api.contentful.com/spaces/" + spaceid + "/entries/" + entryId + "/published")
				.request(MediaType.TEXT_PLAIN_TYPE)
				.header("Authorization", "Bearer " + oauthtoken)
				.header("X-Contentful-Version", versionNumber)
				.put(payload);

		String responseString = response.readEntity(String.class);
		System.out.println("publishEntry response: " + responseString);

		//if we have this error notResolvable, then put some delay and then try publishing again
		if(responseString.contains("notResolvable")){
			try { Thread.sleep(500); }
			catch(InterruptedException e2) {}
			System.out.println("re-publishEntry response: " + responseString);
			publishEntry(entryId);
		}
	}

	//oz: added method to get version number from entry
	public int extractVersionNumber(JSONObject entryObject){
		int versionNumber = 1;
		try {
			JSONObject sysObject = entryObject.getJSONObject("sys");
			versionNumber = sysObject.getInt("version");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return versionNumber;
	}

	private String spaceid;
	private String oauthtoken;
	private Client client;
}