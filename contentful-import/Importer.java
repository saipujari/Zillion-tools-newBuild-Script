import com.sun.webui.jsf.component.Html;
import config.Config;
import contentful.Contentful;
import importers.JoomlaXMLStructure;
import importers.TabSeparatedStructure;
import org.apache.commons.lang3.StringEscapeUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Importer {

	public static class PushedEntries {
		public Map<String, String> entryHistory = new HashMap<String, String>();
		public ArrayList<String> entriesToUpdate = new ArrayList<String>();
		public Map<String, String> entryIdMappings = new HashMap<String, String>();
	}

	public static class PushedImage implements Serializable {
		public PushedImage(String contentfulURL, String contentfulId) {
			this.contentfulURL = contentfulURL;
			this.contentfulId = contentfulId;
		}
		String contentfulURL;
		String contentfulId;
		
		private static final long serialVersionUID = 7526422295622776147L;
	}

	public static class Entry {
		JSONObject jsonObj;
		boolean needsUpdating;
	}

	static Map<String, PushedImage> uploadAndMapImages(ArrayList<JSONObject> images, Map<String, PushedImage> previousImages, Contentful contentful) throws JSONException {
		HashMap<String, PushedImage> imageMappings = new HashMap<String, PushedImage>();

		for(Map.Entry<String, PushedImage> image : previousImages.entrySet()) {
			imageMappings.put(image.getKey(), image.getValue());
		}

		for(JSONObject image : images) {
			String filename = image.getString("src");

			if(imageMappings.containsKey(filename)) {
				continue;
			}

			String url = filename.contains("http://") ? filename : "http://orberacoach.com/"+filename;

			String title = filename.replaceAll("http://[^/]*/", "").replaceAll("/", "_");

			String imageId = contentful.uploadAsset(title, title, url);

			contentful.processAsset(imageId);

			String imageUrl = null;

			while(imageUrl == null) {

				contentful.publishAsset(imageId);

				JSONObject imageObject = contentful.getAsset(imageId);

				try {
					imageUrl = imageObject.getJSONObject("fields").getJSONObject("file").getJSONObject("en-US").getString("url");
				} catch(Exception e) {
					System.out.println("Asset not ready");
					try { Thread.sleep(500); }
					catch(InterruptedException e2) {}
				}
			}

			imageMappings.put(filename, new PushedImage(imageUrl, imageId));
		}

		return imageMappings;
	}

	static Entry buildEntry(JSONObject jsonObj, Config.ObjectMapping mapping, Map<String, PushedImage> imageMappings, ArrayList<String> entryIds) throws JSONException {
		JSONObject outJSON = new JSONObject();

		// Iterate over the defaults values
		for(Config.DefaultField field : mapping.getDefaultFields()) {
			outJSON.put(field.getTo(), new JSONObject().put("en-US", field.getValue()));
		}

		// Iterate over links
		for(Config.LinkField field : mapping.getLinkFields()) {
			String referenceID = entryIds.get(field.getPreviousEntry());

			outJSON.put(field.getTo(), new JSONObject()
					.put("en-US", new JSONObject()
							.put("sys", new JSONObject()
									.put("type", "Link")
									.put("linkType", "Entry")
									.put("id", referenceID))));
		}

		// Iterate over images
		for(Config.ImageField field : mapping.getImageFields()) {
			String from = field.getFrom();
			//oz: added .has check then go inside otherwise throwing exception here
			if (jsonObj.has(from)) {
				String key = jsonObj.getString(from);
				if (!imageMappings.containsKey(key)) continue;

				String value = imageMappings.get(key).contentfulId;

				outJSON.put(field.getTo(), new JSONObject()
					   .put("en-US", new JSONObject()
							   .put("sys", new JSONObject()
									   .put("type", "Link")
									   .put("linkType", "Asset")
									   .put("id", value))));
			}
		}

		boolean needsUpdating = false;

		// Iterate over mapped fields
		for(Config.MappingField field : mapping.getMappingFields()) {

			ArrayList<String> oldKeys = field.getFrom();
			ArrayList<String> delimiters = field.getDelimiters();

			if(oldKeys.size() == 0) continue;

			String oldKey = oldKeys.get(0);
			//oz: added .has check then go inside otherwise throwing exception here
			Object valueObject = jsonObj.has(oldKey) ? jsonObj.get(oldKey) : null;
			if(valueObject != null){
				boolean isSimpleSingleNonStringMapping = oldKeys.size() == 1 && !(valueObject instanceof String) && !field.shouldForceAsString();

				if(isSimpleSingleNonStringMapping) { //This is a singular mapping and it's *not* a string
					outJSON.put(field.getTo(), new JSONObject().put("en-US", valueObject));
				}
				else { //there is more than one key that is being mapped, so we must compile as a string
					String delimitedStringObj = delimiters.get(0);

					for(int i = 0; i < oldKeys.size(); i++) {
						oldKey = oldKeys.get(i);

						valueObject = jsonObj.get(oldKey);

						if(valueObject instanceof String) {

							String stringValue = jsonObj.getString(oldKey);

							if(stringValue.indexOf("index.php?") > 0) {
								needsUpdating = true;
							}

							for(Map.Entry<String, PushedImage> imageMapping : imageMappings.entrySet()) {
								stringValue = stringValue.replaceAll(imageMapping.getKey(), imageMapping.getValue().contentfulURL);
							}

							stringValue = StringEscapeUtils.unescapeHtml3(stringValue);

							try {
								stringValue = URLDecoder.decode(stringValue, "ISO-8859-1");
							} catch(Exception e) {
								// We tried our best...
							}

							delimitedStringObj += stringValue;

						} else {
							delimitedStringObj += valueObject.toString();
						}

						delimitedStringObj += delimiters.get(i+1);
					}

					if(field.shouldFormatAsDate()) {
						delimitedStringObj = field.formatAsDate(delimitedStringObj);
					}

					if(field.hasReplacement()) {
						delimitedStringObj = field.doReplacement(delimitedStringObj);
					}

					outJSON.put(field.getTo(), new JSONObject().put("en-US", delimitedStringObj));
				}
			}
		}
		Entry entry = new Entry();
		entry.jsonObj = outJSON;
		entry.needsUpdating = needsUpdating;

		return entry;
	}

	static PushedEntries createEntries(Map<String, ArrayList<JSONObject>> jsonObjects, HashMap<String, ArrayList<Config.ObjectMapping>> entryMappings,	Map<String, PushedImage> imageMappings, Map<String, String> entryHistory, HashMap<String, ArrayList<String>> entryIdBuilders, Contentful contentful) throws JSONException {
		PushedEntries pushed = new PushedEntries();

		for(Map.Entry<String, String> entry : entryHistory.entrySet()) {
			pushed.entryHistory.put(entry.getKey(), entry.getValue());
		}

		//For each of the entry types and list of those entries read in from the XML file
		for(Map.Entry<String, ArrayList<JSONObject>> inJSON : jsonObjects.entrySet()) {

			//What kind of item is it
			String itemType = inJSON.getKey();

			if(!entryMappings.containsKey(itemType)) continue;

			ArrayList<String> entryIdBuilder = entryIdBuilders.get(itemType);

			ArrayList<Config.ObjectMapping> mappings = entryMappings.get(itemType);

			for(JSONObject jsonObj : inJSON.getValue()) {

				ArrayList<String> entryIds = new ArrayList<String>();

				for(Config.ObjectMapping mapping : mappings) {

					String builtEntryId = "";

					for(String field : entryIdBuilder) {
						builtEntryId += jsonObj.get(field);
					}

					if(entryHistory.containsKey(builtEntryId)) continue;

					Entry entry = buildEntry(jsonObj, mapping, imageMappings, entryIds);

					//Send off the request
					String entryId = contentful.addEntry(new JSONObject().put("fields", entry.jsonObj).toString(), mapping.getEntryTypeId());

					// Save data for the second pass through
					if(entry.needsUpdating) {

						//oz: only save those entries in history which are to to be updated later
						pushed.entryHistory.put(builtEntryId, entryId);

						pushed.entriesToUpdate.add(entryId);
					}

					if(mapping.hasUniqueIdentifier()) {
						String id = jsonObj.get(mapping.getUniqueIdentifier()).toString();
						pushed.entryIdMappings.put(id, entryId);
					}

					entryIds.add(entryId);

					try { Thread.sleep(500); }
					catch(InterruptedException e2) {}

					//oz: publishing entry
					contentful.publishEntry(entryId);
				}
			}
		}
		return pushed;
	}

	static void updateEntries(PushedEntries entries, Contentful contentful) throws JSONException {
		final Pattern p = Pattern.compile("\\\"index\\.php\\?[^\"]*[;?]+id=([0-9]+)[^\"]*\\\"");

		//oz: if entry history contains data then map it to entriesToUpdate
		if(entries.entriesToUpdate.size() == 0 && !entries.entryHistory.isEmpty()){
			for(Map.Entry<String, String> entry : entries.entryHistory.entrySet()) {
				entries.entriesToUpdate.add(entry.getValue());
			}
		}

		for(String entryId : entries.entriesToUpdate) {
			JSONObject entryObject = contentful.getEntry(entryId);
			//oz: added .has check then go inside otherwise throwing exception here
			if(entryObject.has("fields")){
				String fields = entryObject.getJSONObject("fields").toString();

				/*Matcher m = p.matcher(fields);
				while(m.find()) {
					try {
						String match = m.group();
						String oldId = m.group(1);

						if(entries.entryIdMappings.containsKey(oldId)) {
							String newId = entries.entryIdMappings.get(oldId);
							fields = fields.replaceAll(match, "\\\"/member/#/library/viewArticle?id=" + newId + "\\\\\"");
							p.matcher(fields);
						} else {
							System.out.println("No mapping for found for: " + oldId + " original url: " + match);
						}
					} catch(Exception e) {
						e.printStackTrace();
						// System.out.println(fields);
					}
				}*/

				//oz: changed code and replaced index.php via contentful id of entry
				try {
					Document doc = Jsoup.parse(fields);
					Elements elements = doc.select("a");
					if(elements != null && elements.size() > 0){
                        for(Element element: elements){
                            String href = element.attr("href");
                            int recipeIdIndex = href.indexOf("r=");
                            if(recipeIdIndex > -1){
                                String oldId = href.substring(recipeIdIndex + 2, href.length() - 2) + ".0";
								if(entries.entryIdMappings.containsKey(oldId)) {
									String newId = entries.entryIdMappings.get(oldId);
									fields = fields.replace("&amp;", "&").replace("&quot;", "\"");
									fields = fields.replace(href, "\\\"/member/#/viewArticle/" + newId + "\\\"");
								}else{
									System.out.println("No mapping for found for: " + oldId + " original url: " + href);
								}
                            }
                        }
						contentful.updateEntry(new JSONObject().put("fields", new JSONObject(new JSONTokener(fields))).toString(), entryId, contentful.extractVersionNumber(entryObject));

						try { Thread.sleep(500); }
						catch(InterruptedException e2) {}

						//oz: publishing entry
						contentful.publishEntry(entryId);
                    }
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		}
	}

	public static void saveImageMappings(Map<String, PushedImage> imageMappings) {
		try {
			FileOutputStream fout = new FileOutputStream("imageMappings.map");
			ObjectOutputStream oos = new ObjectOutputStream(fout);
			oos.writeObject(imageMappings);
			oos.close();
		} catch(Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	public static Map<String, PushedImage> readPreviousImageMappings() {
		try {
			FileInputStream fis = new FileInputStream("imageMappings.map");
			ObjectInputStream ois = new ObjectInputStream(fis);
	      	Map<String, PushedImage> imageMappings = (Map<String, PushedImage>)ois.readObject();
			ois.close();
	 		return imageMappings;
		} catch(Exception e) {
			return new HashMap<String, PushedImage>();
		}
	}

	public static void saveEntryHistory(Map<String, String> entryHistory) {
		try {
			FileOutputStream fout = new FileOutputStream("entryHistory.map");
			ObjectOutputStream oos = new ObjectOutputStream(fout);
			oos.writeObject(entryHistory);
			oos.close();
		} catch(Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	public static Map<String, String> readPreviousEntryHistory() {
		try {
			FileInputStream fis = new FileInputStream("entryHistory.map");
			ObjectInputStream ois = new ObjectInputStream(fis);
	      	Map<String, String> entryHistory = (Map<String, String>)ois.readObject();
			ois.close();
	 		return entryHistory;
		} catch(Exception e) {
			return new HashMap<String, String>();
		}
	}

	public static void main(String[] args) throws Exception {
		Map<String, ArrayList<JSONObject>> jsonObjects  = null;

		//oz: for running locally, comment it before pushing		
		//args = new String[]{"articles_config.json", "E:\\ZIL-Work\\ZIL-5559\\all.xml"};
		//args = new String[]{"recipes_config.json", "E:\\ZIL-WORK\\ZIL-5559\\recipes_v3.tsv"};

		if(args[1].indexOf(".xml") >= 0) {
			//Read in the XML
			JoomlaXMLStructure structure = new JoomlaXMLStructure().readXMLFile(args[1]);

			//Format the XML->Object to a mapping of types to arrays of those object types as JSONObjects
			jsonObjects = structure.toJSONObjects();
		} else if(args[1].indexOf(".tsv") >= 0) {
			TabSeparatedStructure structure = new TabSeparatedStructure().readTSVFile(args[1]);
			jsonObjects = structure.toJSONObjects();
		} else {
			System.out.println("unrecognized input format: " + args[1]);
			System.exit(1);
		}

		Config config = new Config(args[0]);

		ArrayList<JSONObject> images = jsonObjects.get("Image");

		Contentful contentful = new Contentful(config.getSpaceId(), config.getOAuthToken());

		Map<String, PushedImage> previousImageMappings = readPreviousImageMappings();
		Map<String, PushedImage> imageMappings = uploadAndMapImages(images, previousImageMappings, contentful);
		saveImageMappings(imageMappings);

		Map<String, String> entryHistory = readPreviousEntryHistory();
		PushedEntries pushedEntries = createEntries(jsonObjects, config.getEntryMappings(), imageMappings, entryHistory, config.getIdBuilders(), contentful);
		saveEntryHistory(pushedEntries.entryHistory);

		updateEntries(pushedEntries, contentful);
	}
}