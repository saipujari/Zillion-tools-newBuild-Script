package config;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

public class Config {

	public class DefaultField {
		public DefaultField(JSONObject object) throws JSONException {
			to = object.getString("to");
			value = object.get("value");
		}

		public String getTo() {
			return to;
		}

		public Object getValue() {
			return value;
		}

		public String to;
		public Object value;
	}

	public class LinkField {
		public LinkField(JSONObject object) throws JSONException {
			to = object.getString("to");
			previousEntry = object.getInt("previousEntry");
		}

		public String getTo() {
			return to;
		}

		public int getPreviousEntry() {
			return previousEntry;
		}

		public String to;
		public int previousEntry;
	}

	public class ImageField {
		public ImageField(JSONObject object) throws JSONException {
			to = object.getString("to");
			from = object.getString("from");
		}

		public String getTo() {
			return to;
		}

		public String getFrom() {
			return from;
		}

		public String to;
		public String from;
	}

	public class MappingField {
		public MappingField(JSONObject object) throws JSONException {
			to = object.getString("to");

			JSONArray fromArray = object.getJSONArray("from");
			from = new ArrayList<String>();
			for(int i = 0; i < fromArray.length(); i++) {
				from.add(fromArray.getString(i));
			}

			JSONArray delimitersArray = object.getJSONArray("delimiters");
			delimiters = new ArrayList<String>();
			for(int i = 0; i < delimitersArray.length(); i++) {
				delimiters.add(delimitersArray.getString(i));
			}
			forceAsString = object.has("forceAsString") && object.getBoolean("forceAsString");

			formatAsDate = object.has("formatAsDate");
			if(formatAsDate) {
				incomingDateFormat = object.getJSONObject("formatAsDate").getString("incomingFormat");
				outgoingDateFormat = object.getJSONObject("formatAsDate").getString("outgoingFormat");
			}


			if(object.has("replaceThis") && object.has("withThis")) {
				replacement = true;
				replaceThis = object.getString("replaceThis");
				withThis = object.getString("withThis");
			} else {
				replacement = false;
			}
		}

		public String getTo() {
			return to;
		}

		public ArrayList<String> getFrom() {
			return from;
		}

		public ArrayList<String> getDelimiters() {
			return delimiters;
		}

		public boolean shouldForceAsString() {
			return forceAsString;
		}

		public boolean shouldFormatAsDate() {
			return formatAsDate;
		}

		public String formatAsDate(String incomingDate) {
			try {
				Date date = new SimpleDateFormat(incomingDateFormat).parse(incomingDate);
				return new SimpleDateFormat(outgoingDateFormat).format(date);
			} catch(Exception e) {
				return new SimpleDateFormat(outgoingDateFormat).format(new Date());
			}
		}

		public boolean hasReplacement() {
			return replacement;
		}

		public String doReplacement(String incomingString) {
			return incomingString.replaceAll(replaceThis, withThis);
		}

		public String to;
		public ArrayList<String> from;
		public ArrayList<String> delimiters;
		public boolean forceAsString;
		public boolean formatAsDate;

		public boolean replacement;
		public String replaceThis;
		public String withThis;

		public String incomingDateFormat;
		public String outgoingDateFormat;
	}

	public class ObjectMapping {
		ObjectMapping(JSONObject object) throws JSONException {
			entryTypeId = object.getString("entryTypeId");

			if(object.has("uniqueIdentifier")) {
				uniqueIdentifier = object.getString("uniqueIdentifier");
			}

			JSONArray defaultsArray = object.getJSONArray("defaults");
			defaults = new ArrayList<DefaultField>();
			for(int i = 0; i < defaultsArray.length(); i++) {
				defaults.add(new DefaultField(defaultsArray.getJSONObject(i)));
			}

			JSONArray linksArray = object.getJSONArray("links");
			links = new ArrayList<LinkField>();
			for(int i = 0; i < linksArray.length(); i++) {
				links.add(new LinkField(linksArray.getJSONObject(i)));
			}

			JSONArray imagesArray = object.getJSONArray("images");
			images = new ArrayList<ImageField>();
			for(int i = 0; i < imagesArray.length(); i++) {
				images.add(new ImageField(imagesArray.getJSONObject(i)));
			}

			JSONArray mappingsArray = object.getJSONArray("mapping");
			mappings = new ArrayList<MappingField>();
			for(int i = 0; i < mappingsArray.length(); i++) {
				mappings.add(new MappingField(mappingsArray.getJSONObject(i)));
			}
		}

		public String getEntryTypeId() {
			return entryTypeId;
		}

		public ArrayList<DefaultField> getDefaultFields() {
			return defaults;
		}

		public ArrayList<LinkField> getLinkFields() {
			return links;
		}

		public ArrayList<ImageField> getImageFields() {
			return images;
		}

		public ArrayList<MappingField> getMappingFields() {
			return mappings;
		}

		public boolean hasUniqueIdentifier() {
			return uniqueIdentifier != null;
		}

		public String getUniqueIdentifier() {
			return uniqueIdentifier;
		}

		String entryTypeId;
		String uniqueIdentifier;
		ArrayList<DefaultField> defaults;
		ArrayList<LinkField> links;
		ArrayList<ImageField> images;
		ArrayList<MappingField> mappings;
	}

	//oz: reading config file
	public String readConfigFile(String path) throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, "UTF-8");
	}

	public Config(String filename) throws Exception {
		FileReader reader = null;
		try { reader = new FileReader(filename); }
		catch(FileNotFoundException e) { e.printStackTrace(); System.exit(1); }

		//oz: I have old org.json version
		String configString = readConfigFile(filename);
		JSONObject configFile = new JSONObject(new JSONTokener(configString));

		//oz: uncomment the following code if you have latest library version that supports FileReader in JSONTokener
		//JSONObject configFile = new JSONObject(new JSONTokener(reader));

		spaceid = configFile.getString("spaceid");
		oauthtoken = configFile.getString("oauthtoken");
		innerredirect = configFile.getString("innerRedirect");

		JSONObject entryMappings = configFile.getJSONObject("entryMappings");
		Iterator<String> entryTypes = entryMappings.keys();

		mappings = new HashMap<String, ArrayList<ObjectMapping>>();
		idBuilders = new HashMap<String, ArrayList<String>>();

		while(entryTypes.hasNext()) {
			String entryType = entryTypes.next();
			JSONObject mappingObject = entryMappings.getJSONObject(entryType);

			JSONArray entryIds = mappingObject.getJSONArray("uniqueKey");
			ArrayList<String> entryIdBuilder = new ArrayList<String>();
			for(int i = 0; i < entryIds.length(); i++) {
				entryIdBuilder.add(entryIds.getString(i));
			}

			ArrayList<ObjectMapping> objectMappings = new ArrayList<ObjectMapping>();
			
			JSONArray objects = mappingObject.getJSONArray("mappings");
			for(int i = 0; i < objects.length(); i++) {
				JSONObject object = objects.getJSONObject(i);
				objectMappings.add(new ObjectMapping(object));
			}
			mappings.put(entryType, objectMappings);
			idBuilders.put(entryType, entryIdBuilder);
		}
	}

	public String getSpaceId() {
		return spaceid;
	}

	public String getOAuthToken() {
		return oauthtoken;
	}

	public HashMap<String, ArrayList<ObjectMapping>> getEntryMappings() {
		return mappings;
	}

	public HashMap<String, ArrayList<String>> getIdBuilders() {
		return idBuilders;
	}

	String spaceid, oauthtoken, innerredirect;
	HashMap<String, ArrayList<ObjectMapping>> mappings;
	HashMap<String, ArrayList<String>> idBuilders;
}
