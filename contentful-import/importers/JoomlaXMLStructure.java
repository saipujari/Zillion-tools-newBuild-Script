package importers;

import java.io.FileReader;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.lang.reflect.Field;
import java.util.Locale;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.json.JSONException;
import org.json.JSONObject;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.Converter;

import com.thoughtworks.xstream.io.xml.DomDriver;


public class JoomlaXMLStructure {
	public JoomlaXMLStructure readXMLFile(String filename) {
		FileReader reader = null;
		
		try { reader = new FileReader(filename); }
		catch(FileNotFoundException e) { e.printStackTrace(); System.exit(1); }

		XStream xstream = new XStream(new DomDriver());
		xstream.ignoreUnknownElements();
		xstream.alias("j2xml", JoomlaXMLStructure.class);
		xstream.addImplicitCollection(JoomlaXMLStructure.class, "listItems", JoomlaXMLStructure.ListItem.class);
		xstream.alias("content", JoomlaXMLStructure.Content.class);
		// xstream.alias("category", JoomlaXMLStructure.Category.class);
		xstream.registerConverter(new ImageConverter());
		xstream.alias("img", JoomlaXMLStructure.Image.class);
		
		return (JoomlaXMLStructure)xstream.fromXML(reader);
	}

	public Map<String, ArrayList<JSONObject>> toJSONObjects() throws JSONException {
		Map<String, ArrayList<JSONObject>> jsonObjects = new HashMap<String, ArrayList<JSONObject>>();

		jsonObjects.put("Content", new ArrayList<JSONObject>());
		// jsonObjects.put("Category", new ArrayList<JSONObject>());
		jsonObjects.put("Image", new ArrayList<JSONObject>());

		for(ListItem item : listItems) {
			if(item.getType() == "Content") {
				((Content)item).formatDates();
			}
			jsonObjects.get(item.getType()).add(item.ToJsonObject());
		}

		return jsonObjects;
	}

	abstract public class ListItem {
		abstract public String getType();
		
		public JSONObject ToJsonObject() throws JSONException {
			JSONObject json = new JSONObject();

			for (Field field : this.getClass().getFields()) {
				try { json.put(field.getName(), field.get(this)); }
				catch(IllegalAccessException e) { e.printStackTrace(); System.exit(1); }
			}

			return json;
		}

		public int access;
		public String alias;
		public int hits;
		public int id;
		public String language;
		public String metadata;
		public String metadesc;
		public String metakey;
		public String title;
		public int version;
	}

	public class Content extends ListItem {
		public String getType() { return "Content"; }

		private String formatDate(String date) {
			return date.replace(" ", "T") + ".000Z";
		}

		public void formatDates() {
			// publish_down = formatDate(publish_down);
			publish_up = formatDate(publish_up);
		}

		public String attribs;
		public String catid;
		public String created;
		public String created_by_alias;
		public Boolean featured;
		public String fulltext;
		public String images;
		public String introtext;
		public String modified;
		public int ordering;
		public String publish_down;
		public String publish_up;
		public int rating_count;
		public int rating_sum;
		public int state;
		public String urls;
		public String xreference;
	}

	abstract public class SubListItem extends ListItem {

		public String created_time;
		public String description;
		public String extension;
		public String modified_time;
		public String note;
		public String params;
		public String path;
		public int published;
	}

	// public class Category extends SubListItem {
	// 	public String getType() { return "Category"; }

	// 	public String created_user_id;
	// 	public String modified_user_id;
	// }

	public class Image extends SubListItem {
		public String getType() { return "Image"; }

		public String imageData;
		public String src;
	}

	public class ImageConverter implements Converter {

		public void marshal(Object value, HierarchicalStreamWriter writer, MarshallingContext context) {}

		public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
			Image image = new Image();
			image.imageData = reader.getValue();
			image.src = reader.getAttribute("src");
			return image;
		}

		public boolean canConvert(Class clazz) {
			return clazz.equals(Image.class);
		}
	}


	ArrayList<ListItem> listItems;
}